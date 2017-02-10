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
import com.dreamwing.serverville.cluster.ClusterMessages.RemoveGlobalChannelMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.ReplicateGlobalChannelMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.StartupCompleteMessage;
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
import com.dreamwing.serverville.residents.GlobalChannel;
import com.dreamwing.serverville.residents.ResidentManager;
import com.dreamwing.serverville.scripting.ScriptManager;
import com.dreamwing.serverville.util.StringUtil;
import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicReference;
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
	private static IAtomicReference<String> SeniorMemberUuid;
	
	private static Member LocalMember;
	private static Map<String,Member> ClusterMembers;
	private static boolean IsSeniorMember = false;
	private static boolean ScriptsInitted = false;
	
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
		
		MapConfig residentsMapConfig = cfg.getMapConfig("Residents");
		residentsMapConfig.setInMemoryFormat(InMemoryFormat.OBJECT);
		residentsMapConfig.setBackupCount(0);
		
		Cluster = Hazelcast.newHazelcastInstance(cfg); // <---------------------------- Join cluster
		
		LocalMember = Cluster.getCluster().getLocalMember();
		LocalMember.setStringAttribute("hostname", ServervilleMain.Hostname+":"+ServervilleMain.ClientPort);
		LocalMember.setShortAttribute("servernum", ServervilleMain.getServerNumber());
		LocalMember.setLongAttribute("joined", System.currentTimeMillis());
		
		Cluster.getCluster().addMembershipListener(new ClusterMemberListener());
		RemoteExecutor = Cluster.getExecutorService("RemoteExecutor");
		
		SeniorMemberUuid = Cluster.getAtomicReference("SeniorUuid");
		
		if(SeniorMemberUuid.compareAndSet(null, LocalMember.getUuid()))
		{
			IsSeniorMember = true;
			LocalMember.setBooleanAttribute("senior", true);
		}
		
		OnlineUsers = Cluster.getMap("OnlineUsers");
		ResidentLocations = Cluster.getMap("ResidentLocations");
		
		Residents = Cluster.getMap("Residents");
		
		if(IsSeniorMember)
		{
			ScriptManager.doGlobalInit();
			ScriptsInitted = true;
		}
		
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
				
				if(IsSeniorMember)
				{
					sendGlobalsToMember(m);
				}
			}
		}
		
		if(IsSeniorMember)
		{
			onClusterReady();
		}
		
	}
	
	public static void onClusterReady()
	{
		LocalMember.setBooleanAttribute("ready", true);
		ScriptsInitted = true;
		ServervilleMain.onClusterStarted();
	}
	
	public static void shutdown()
	{
		MemberShuttingDownMessage message = new MemberShuttingDownMessage();
		message.MemberId = LocalMember.getUuid();
		sendToAll(message);
		Cluster.shutdown();
	}
	
	public static boolean isSeniorMember()
	{
		return IsSeniorMember;
	}
	
	public static String getLocalMemberUUID()
	{
		return Cluster.getCluster().getLocalMember().getUuid();
	}
	

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
			
			if(!m.localMember() && IsSeniorMember && ScriptsInitted)
			{
				sendGlobalsToMember(m);
			}
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
			
			if(Boolean.TRUE.equals(m.getBooleanAttribute("senior")))
			{
				senioritySearch();
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
			future = ServervilleMain.MainExecutor.submit((Callable<IdentifiedDataSerializable>)runner);
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
	
	public static void createGlobalChannel(String id, String residentType, Map<String,Object> values)
	{
		ReplicateGlobalChannelMessage message = new ReplicateGlobalChannelMessage();
		message.ChannelId = id;
		message.ResidentType = residentType;
		
		if(values != null)
		{
			GlobalChannel channel = new GlobalChannel(id, residentType);
			channel.setTransientValues(values);
			message.Values = channel.getValues();
		}
		
		sendToAll(message);
	}
	
	public static void destroyGlobalChannel(GlobalChannel channel)
	{
		RemoveGlobalChannelMessage message = new RemoveGlobalChannelMessage();
		message.ChannelId = channel.getId();
		
		sendToAll(message);
	}
	
	
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
	
	private static void senioritySearch()
	{
		String oldSeniorMember = SeniorMemberUuid.get();
		long myJoinDate = LocalMember.getLongAttribute("joined");
		
		for(Member m : ClusterMembers.values())
		{
			long joined = m.getLongAttribute("joined");
			if(joined < myJoinDate)
			{
				return;
			}
			else if(joined == myJoinDate)
			{
				if(LocalMember.getUuid().compareTo(m.getUuid()) >= 0)
					return;
			}
		}
		
		// I guess it's us
		if(SeniorMemberUuid.compareAndSet(oldSeniorMember, LocalMember.getUuid()))
		{
			becomeSenior();
		}
		
	}
	
	private static void becomeSenior()
	{
		IsSeniorMember = true;
		LocalMember.setBooleanAttribute("senior", true);
		
		// Check to see if anly cluster members are not ready - they might have joined
		// during the seniority search while there was nobody to send them the globals
		for(Member m : ClusterMembers.values())
		{
			if(!Boolean.TRUE.equals(m.getBooleanAttribute("ready")))
			{
				sendGlobalsToMember(m);
			}
		}
	}
	
	private static void sendGlobalsToMember(Member m)
	{
		for(GlobalChannel channel : ResidentManager.GlobalResidents.values())
		{
			ReplicateGlobalChannelMessage message = new ReplicateGlobalChannelMessage();
			message.ChannelId = channel.getId();
			message.ResidentType = channel.getType();
			message.Values = channel.getValues();
			
			sendToMember(message, m);
		}
		
		StartupCompleteMessage message = new StartupCompleteMessage();
		sendToMember(message, m);
	}
}
