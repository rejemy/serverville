package com.dreamwing.serverville.cluster;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.cluster.ClusterMessages.CachedDataUpdateMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.ClusterMessageFactory;
import com.dreamwing.serverville.cluster.ClusterMessages.DisconnectUserMessage;
import com.dreamwing.serverville.cluster.DistributedData.DistributedDataFactory;
import com.dreamwing.serverville.cluster.DistributedData.OnlineUserLocator;
import com.dreamwing.serverville.data.UserSession;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.util.StringUtil;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.ListenerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class ClusterManager
{
	private static final Logger l = LogManager.getLogger(ClusterManager.class);
	
	public static HazelcastInstance Cluster;
	private static ITopic<CachedDataUpdateMessage> CachedDataUpdateTopic;
	private static IMap<String, OnlineUserLocator> OnlineUsers;
	private static Map<String,Member> ClusterMembers;
	
	
	private static IExecutorService RemoteExecutor;
	
	public static void init()
	{
		List<String> hostList = null;
		
		String clusterString = ServervilleMain.ServerProperties.getProperty("cluster_members");
		if(!StringUtil.isNullOrEmpty(clusterString))
		{
			String[] hostnames = clusterString.split(",");
			hostList = new ArrayList<String>(hostnames.length);
			for(String hostname : hostnames)
			{
				hostname = hostname.trim();
				hostList.add(hostname);
			}
		}
		
		ClusterMembers = new ConcurrentHashMap<String,Member>();
		
		Config cfg = new Config();
		cfg.setProperty("hazelcast.logging.type", "log4j");
		
		cfg.getSerializationConfig().addDataSerializableFactoryClass(ClusterMessageFactory.FACTORY_ID, ClusterMessageFactory.class);
		cfg.getSerializationConfig().addDataSerializableFactoryClass(DistributedDataFactory.FACTORY_ID, DistributedDataFactory.class);
		
		cfg.getNetworkConfig().setPortAutoIncrement(true);
		JoinConfig joinCfg = cfg.getNetworkConfig().getJoin();
		
		joinCfg.getMulticastConfig().setEnabled(false);
		joinCfg.getAwsConfig().setEnabled(false);
		joinCfg.getTcpIpConfig().setEnabled(true);
		if(hostList != null)
			joinCfg.getTcpIpConfig().setMembers(hostList);
		
		cfg.getTopicConfig("CachedDataUpdate").addMessageListenerConfig(new ListenerConfig("com.dreamwing.serverville.cluster.CachedDataUpdateListener"));
		
		Cluster = Hazelcast.newHazelcastInstance(cfg);
		
		CachedDataUpdateTopic = Cluster.getTopic("CachedDataUpdate");
		
		Cluster.getCluster().addMembershipListener(new ClusterMemberListener());
		
		RemoteExecutor = Cluster.getExecutorService("RemoteExecutor");
		
		Set<Member> clusterMembers = Cluster.getCluster().getMembers();
		for(Member m : clusterMembers)
		{
			ClusterMembers.put(m.getUuid(), m);
		}
		
		OnlineUsers = Cluster.getMap("OnlineUsers");
	}
	
	public static void shutdown()
	{
		Cluster.shutdown();
	}
	
	public static String getLocalMemberUUID()
	{
		return Cluster.getCluster().getLocalMember().getUuid();
	}
	
	public static void sendCachedDataUpdateMessage(CachedDataUpdateMessage event)
	{
		CachedDataUpdateTopic.publish(event);
	}
	
	
	public static void addOnlineUser(OnlineUserLocator locator)
	{
		locator.MemberUUID = getLocalMemberUUID();
		OnlineUserLocator oldUser = OnlineUsers.put(locator.UserId, locator);
		if(oldUser != null)
		{
			// We have a previous connection
			
			if(!oldUser.SessionId.equals(locator.SessionId))
			{
				// Old connection was on a different session, expire the old session
				try {
					UserSession oldSession = UserSession.findById(oldUser.SessionId);
					if(oldSession != null)
					{
						oldSession.Expired = true;
						oldSession.Connected = false;
						oldSession.update();
					}
					
				} catch (SQLException | JsonApiException e) {
					l.error("Db error trying to expire old session", e);
				}
			}
			
			// Find the member the old connection is on, and tell them to disconnect
			Member oldConnectionMember = locateUserClusterMember(oldUser.MemberUUID);
			if(oldConnectionMember != null)
			{
				DisconnectUserMessage message = new DisconnectUserMessage();
				message.SessionId = oldUser.SessionId;
				sendToMember(message, oldConnectionMember);
			}
		}
	}
	
	public static void removeOnlineUser(String userId)
	{
		OnlineUsers.remove(userId);
	}
	
	public static OnlineUserLocator locateOnlineUser(String userId)
	{
		return OnlineUsers.get(userId);
	}
	
	public static Member locateUserClusterMember(String userId)
	{
		OnlineUserLocator locator = OnlineUsers.get(userId);
		if(locator == null)
			return null;
		
		return ClusterMembers.get(locator.MemberUUID);
	}
	
	public static class ClusterMemberListener implements MembershipListener
	{

		@Override
		public void memberAdded(MembershipEvent membershipEvent)
		{
			Member m = membershipEvent.getMember();
			ClusterMembers.put(m.getUuid(), m);
		}

		@Override
		public void memberRemoved(MembershipEvent membershipEvent)
		{
			ClusterMembers.remove(membershipEvent.getMember().getUuid());
		}

		@Override
		public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent)
		{
			
		}
		
	}
	
	public static void sendToMember(IdentifiedDataSerializable message, Member clusterMember)
	{
		ClusterMemberMessageRunnable runner = new ClusterMemberMessageRunnable();
		runner.Message = message;
		
		if(clusterMember.localMember())
		{
			ServervilleMain.ServiceScheduler.execute(runner);
		}
		else
		{
			RemoteExecutor.executeOnMember(runner, clusterMember);
		}
	}
}
