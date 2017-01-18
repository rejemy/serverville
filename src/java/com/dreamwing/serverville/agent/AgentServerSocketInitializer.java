package com.dreamwing.serverville.agent;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.net.SslProtocolDetector;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class AgentServerSocketInitializer extends ChannelInitializer<SocketChannel> {

	private static EventLoopGroup BossGroup;
	private static EventLoopGroup WorkerGroup;
	
	private AgentDispatcher Dispatcher;
	
	public static String URL;
	
    public AgentServerSocketInitializer() throws Exception
    {
    	Dispatcher = new AgentDispatcher();
    	Dispatcher.addAllMethods(AgentAPI.class);
    }

    @Override
    public void initChannel(SocketChannel ch)
    {
        ChannelPipeline pipeline = ch.pipeline();
        
        if(SslProtocolDetector.SharedSslContext != null)
        {
	        if(SslProtocolDetector.AgentSSLOnly)
	        	pipeline.addLast(SslProtocolDetector.SharedSslContext.newHandler(ch.alloc()));
	        else
	        	pipeline.addLast(new SslProtocolDetector());
        }
        
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new IdleStateHandler(0, 0, 60));
        pipeline.addLast(new AgentServerConnectionHandler(Dispatcher));
    }
    
    public static void startListener(int port) throws Exception
    {
    	BossGroup = new NioEventLoopGroup(1);
    	WorkerGroup = new NioEventLoopGroup();
    	
    	ServerBootstrap b = new ServerBootstrap();
        b.group(BossGroup, WorkerGroup)
         .channel(NioServerSocketChannel.class)
         .childHandler(new AgentServerSocketInitializer());

        b.bind(port).sync();
        
        String protocol = SslProtocolDetector.AdminSSLOnly ? "https" : "http";
        URL = protocol+"://"+ServervilleMain.Hostname+":"+port+"/";
    }
    
    public static void shutdown()
    {
    	BossGroup.shutdownGracefully();
    	WorkerGroup.shutdownGracefully();
    }
}