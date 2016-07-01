package com.dreamwing.serverville.client;

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

public class ClientSocketInitializer extends ChannelInitializer<SocketChannel> {

	private static EventLoopGroup BossGroup;
	private static EventLoopGroup WorkerGroup;
	
	private ClientDispatcher Dispatcher;
	
	public static String URL;
	
	public ClientSocketInitializer() throws Exception
	{
		Dispatcher = new ClientDispatcher();
		Dispatcher.addAllMethods(ClientAPI.class);
	}
	
	@Override
	public void initChannel(SocketChannel ch) throws Exception
	{
		ChannelPipeline pipeline = ch.pipeline();
        
		pipeline.addLast("detector", new ClientProtocolDetector());
        pipeline.addLast("httpServer", new HttpServerCodec());
        pipeline.addLast("httpAggregator", new HttpObjectAggregator(65536));
        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
        pipeline.addLast(new IdleStateHandler(0, 0, 120));
        pipeline.addLast(new ClientConnectionHandler(Dispatcher));
	}

	public static void startListener(int port) throws Exception
    {
    	BossGroup = new NioEventLoopGroup(1);
    	WorkerGroup = new NioEventLoopGroup();

    	ServerBootstrap b = new ServerBootstrap();
        b.group(BossGroup, WorkerGroup)
         .channel(NioServerSocketChannel.class)
         .childHandler(new ClientSocketInitializer());

        b.bind(port).sync();
        
        URL = "http://localhost:"+port+"/";
    }
    
    public static void shutdown()
    {
    	BossGroup.shutdownGracefully();
    	WorkerGroup.shutdownGracefully();
    }
}
