package com.dreamwing.serverville.cluster;

import java.util.ArrayList;
import java.util.List;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.cluster.ClusterMessages.CachedDataUpdateMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.ClusterMessageFactory;
import com.dreamwing.serverville.util.StringUtil;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.ListenerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;

public class ClusterManager
{
	public static HazelcastInstance Cluster;
	private static ITopic<CachedDataUpdateMessage> CachedDataUpdateTopic;
	
	//private static Member LocalMember;
	
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
		
		Config cfg = new Config();
		cfg.setProperty("hazelcast.logging.type", "log4j");
		
		cfg.getSerializationConfig().addDataSerializableFactoryClass(ClusterMessageFactory.FACTORY_ID, ClusterMessageFactory.class);
		
		cfg.getNetworkConfig().setPortAutoIncrement(true);
		JoinConfig joinCfg = cfg.getNetworkConfig().getJoin();
		
		joinCfg.getMulticastConfig().setEnabled(false);
		joinCfg.getAwsConfig().setEnabled(false);
		joinCfg.getTcpIpConfig().setEnabled(true);
		if(hostList != null)
			joinCfg.getTcpIpConfig().setMembers(hostList);
		
		cfg.getTopicConfig("CachedDataUpdate").addMessageListenerConfig(new ListenerConfig("com.dreamwing.serverville.cluster.CachedDataUpdateListener"));
		
		Cluster = Hazelcast.newHazelcastInstance(cfg);
		//LocalMember = Cluster.getCluster().getLocalMember();
		
		CachedDataUpdateTopic = Cluster.getTopic("CachedDataUpdate");
		
	}
	
	public static void shutdown()
	{
		Cluster.shutdown();
	}
	
	public static void sendCachedDataUpdateMessage(CachedDataUpdateMessage event)
	{
		CachedDataUpdateTopic.publish(event);
	}
}
