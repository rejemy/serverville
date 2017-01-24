package com.dreamwing.serverville.cluster;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.cluster.ClusterMessages.ClusterMessageFactory;
import com.dreamwing.serverville.cluster.ClusterMessages.DisconnectUserMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.MemberShuttingDownMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.ClusterMemberMessageRunnable;
import com.dreamwing.serverville.cluster.DistributedData.BaseResidentClusterData;
import com.dreamwing.serverville.cluster.DistributedData.ChannelClusterData;
import com.dreamwing.serverville.cluster.DistributedData.DistributedDataFactory;
import com.dreamwing.serverville.cluster.DistributedData.OnlineUserLocator;
import com.dreamwing.serverville.cluster.DistributedData.ResidentLocator;
import com.dreamwing.serverville.data.UserSession;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.residents.BaseResident;
import com.dreamwing.serverville.residents.Channel;
import com.dreamwing.serverville.util.StringUtil;
import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.Partition;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class ClusterManager
{
	private static final Logger l = LogManager.getLogger(ClusterManager.class);
	
	public static HazelcastInstance Cluster;
	private static IMap<String, OnlineUserLocator> OnlineUsers;
	private static IMap<String, ResidentLocator> ResidentLocations;
	private static IMap<ResidentLocator, BaseResidentClusterData> Residents;
	private static IExecutorService RemoteExecutor;
	
	private static Member LocalMember;
	private static Map<String,Member> ClusterMembers;
	
	public static void init() throws Exception
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

		NearCacheConfig residentCache = new NearCacheConfig();
		residentCache.setEvictionPolicy("LRU");
		residentCache.setMaxSize(10000);
		cfg.getMapConfig("ResidentLocations").setNearCacheConfig(residentCache);
		
		cfg.getMapConfig("OnlineUsers").setNearCacheConfig(residentCache);
		
		//PartitioningStrategyConfig stringParition = new PartitioningStrategyConfig().setPartitionStrategy(new StringPartitioningStrategy());
		
		MapConfig residentsMapConfig = cfg.getMapConfig("Residents");
		//residentsMapConfig.setPartitioningStrategyConfig(stringParition);
		residentsMapConfig.setInMemoryFormat(InMemoryFormat.OBJECT);
		residentsMapConfig.setBackupCount(0);
		
		Cluster = Hazelcast.newHazelcastInstance(cfg);
		
		LocalMember = Cluster.getCluster().getLocalMember();
		LocalMember.setStringAttribute("hostname", ServervilleMain.Hostname+":"+ServervilleMain.ClientPort);
		LocalMember.setShortAttribute("servernum", ServervilleMain.getServerNumber());
		
		Cluster.getCluster().addMembershipListener(new ClusterMemberListener());
		//Cluster.getPartitionService().addPartitionLostListener(new PartitionLostHandler());
		
		RemoteExecutor = Cluster.getExecutorService("RemoteExecutor");
		
		Set<Member> clusterMembers = Cluster.getCluster().getMembers();
		for(Member m : clusterMembers)
		{
			ClusterMembers.put(m.getUuid(), m);
			
			if(!m.localMember())
			{
				Short memberNumber = m.getShortAttribute("servernum");
				if(memberNumber != null && memberNumber == ServervilleMain.getServerNumber())
				{
					shutdown();
					throw new Exception("Cluster members have duplicate member number: "+memberNumber);
				}
			}
		}
		
		OnlineUsers = Cluster.getMap("OnlineUsers");
		ResidentLocations = Cluster.getMap("ResidentLocations");
		
		Residents = Cluster.getMap("Residents");
	}
	
	
	public static void shutdown()
	{
		MemberShuttingDownMessage message = new MemberShuttingDownMessage();
		message.MemberId = LocalMember.getUuid();
		sendToAll(message);
		Cluster.shutdown();
	}
	
	public static String getLocalMemberUUID()
	{
		return Cluster.getCluster().getLocalMember().getUuid();
	}
	
	/*
	public static void sendCachedDataUpdateMessage(CachedDataUpdateMessage event)
	{
		CachedDataUpdateTopic.publish(event);
	}
	*/
	
	public static void addOnlineUser(String userId, OnlineUserLocator locator)
	{
		locator.MemberUUID = getLocalMemberUUID();
		OnlineUserLocator oldUser = OnlineUsers.put(userId, locator);
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
	
	public static void onMemberGracefulExit(String memberId)
	{
		ClusterMembers.remove(memberId);
	}
	
	public static class ClusterMemberListener implements MembershipListener
	{

		@Override
		public void memberAdded(MembershipEvent membershipEvent)
		{
			Member m = membershipEvent.getMember();
			l.info("Cluster member joined: "+m.getUuid());
			
			ClusterMembers.put(m.getUuid(), m);
		}

		@Override
		public void memberRemoved(MembershipEvent membershipEvent)
		{
			Member m = membershipEvent.getMember();
			l.info("Cluster member removed: "+m.getUuid());
			
			if(ClusterMembers.remove(m.getUuid()) != null)
			{
				l.warn("Cluster member died: "+m.getUuid());
				// Member wasn't cleaned up gracefully, we have to check our data
				cleanupDataAfterMemberCrash();
			}
			else
			{
				l.info("Cluster member removed: "+m.getUuid());
			}
		}

		@Override
		public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent)
		{
			
		}
		
	}
	
	/*
	public static class PartitionLostHandler implements PartitionLostListener
	{

		@Override
		public void partitionLost(PartitionLostEvent event)
		{
			l.warn("Lost partition "+event.getPartitionId());
		}	
	}
	*/
	
	public static Member getMemberForId(String id)
	{
		Partition part = Cluster.getPartitionService().getPartition(id);
		return part.getOwner();
	}
	
	public static Member getMemberForResident(ResidentLocator locator)
	{
		Partition part = Cluster.getPartitionService().getPartition(locator);
		return part.getOwner();
	}
	
	public static void sendToAll(IdentifiedDataSerializable message)
	{
		ClusterMemberMessageRunnable runner = new ClusterMemberMessageRunnable();
		runner.Message = message;
		
		RemoteExecutor.executeOnAllMembers(runner);	
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
	
	public static IdentifiedDataSerializable runOnMemberOwningId(IdentifiedDataSerializable request, String id) throws JsonApiException
	{
		Member member = getMemberForId(id);
		return runOnMember(request, member);
	}
	
	public static IdentifiedDataSerializable runOnMember(IdentifiedDataSerializable request, Member clusterMember) throws JsonApiException
	{
		ClusterMemberMessageRunnable runner = new ClusterMemberMessageRunnable();
		runner.Message = request;
		
		Future<IdentifiedDataSerializable> future;
		if(clusterMember.localMember())
		{
			future = ServervilleMain.ServiceScheduler.submit((Callable<IdentifiedDataSerializable>)runner);
		}
		else
		{
			future = RemoteExecutor.submitToMember((Callable<IdentifiedDataSerializable>)runner, clusterMember);
		}
		
		try {
			return future.get();
		} catch (InterruptedException e) {
			throw new JsonApiException(ApiErrors.INTERRUPTED, e.getMessage());
		} catch (ExecutionException e) {
			if(e.getCause() instanceof JsonApiException)
				throw (JsonApiException)(e.getCause());
			throw new JsonApiException(ApiErrors.INTERNAL_SERVER_ERROR, e.getMessage());
		}
		
	}
	
	
	public static void registerLocalResident(BaseResident res) throws JsonApiException
	{
		ResidentLocator locator = res.getLocator();
		
		ResidentLocations.set(res.getId(), locator);
		
		// Not enforcing resident uniqueness until I get it a little more stable
		/*
		if(ResidentLocations.putIfAbsent(res.getId(), locator) != null)
		{
			// Already resident with that id
			throw new JsonApiException(ApiErrors.RESIDENT_ID_TAKEN, res.getId());
		}*/
	}
	
	public static void unregisterLocalResident(BaseResident res)
	{
		ResidentLocations.delete(res.getId());
	}
	
	public static void createChannelInCluster(Channel res)
	{
		ChannelClusterData clusterData = new ChannelClusterData();
		clusterData.LiveChannel = res;
		clusterData.IsTempObject = true;
		
		ResidentLocator locator = res.getLocator();
		Residents.set(locator, clusterData);
		ResidentLocations.set(res.getId(), locator);
	}
	
	/*
	public static void createResidentInCluster(Resident res)
	{
		ResidentClusterData clusterData = new ResidentClusterData();
		clusterData.LiveResident = res;
		
		ResidentLocator locator = res.getLocator();
		Residents.set(locator, clusterData);
		ResidentLocations.set(res.getId(), locator);
	}*/
	
	public static void cleanupDataAfterMemberCrash()
	{
		for(String key : ResidentLocations.localKeySet())
		{
			ResidentLocator locator = ResidentLocations.get(key);
			if(!Residents.containsKey(locator))
			{
				ResidentLocations.remove(key);
			}
			
		}
	}
	
	public static String getHostnameForResident(String residentId)
	{
		Partition part = Cluster.getPartitionService().getPartition(residentId);
		Member memb = part.getOwner();
		return memb.getStringAttribute("hostname");
	}
}
