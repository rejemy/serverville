package com.dreamwing.serverville.client;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.net.HttpDispatcher;
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

public class ClientSocketInitializer extends ChannelInitializer<SocketChannel> {

	private static EventLoopGroup BossGroup;
	private static EventLoopGroup WorkerGroup;
	
	protected ClientDispatcher JsonDispatcher;
	protected HttpDispatcher FormDispatcher;
	
	public static String URL;
	
	public ClientSocketInitializer() throws Exception
	{
		JsonDispatcher = new ClientDispatcher();
		JsonDispatcher.addAllMethods(ClientAPI.class);
		
		FormDispatcher = new HttpDispatcher(new ClientFormAuthenticator());
		FormDispatcher.addAllStaticMethods("/form/", ClientFormAPI.class);
	}
	
	@Override
	public void initChannel(SocketChannel ch) throws Exception
	{
		ChannelPipeline pipeline = ch.pipeline();
        
		if(SslProtocolDetector.SharedSslContext != null)
        {
			if(SslProtocolDetector.ClientSSLOnly)
	        	pipeline.addLast(SslProtocolDetector.SharedSslContext.newHandler(ch.alloc()));
	        else
	        	pipeline.addLast(new SslProtocolDetector());
        }
		
		// Disabling this thing for now, not used and needs more work
		//pipeline.addLast("detector", new ClientProtocolDetector());
      
		pipeline.addLast("httpServer", new HttpServerCodec());
        pipeline.addLast("httpAggregator", new HttpObjectAggregator(ServervilleMain.MaxRequestSize));
        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
        pipeline.addLast(new IdleStateHandler(0, 0, 120));
        pipeline.addLast(new ClientConnectionHandler(JsonDispatcher, FormDispatcher));
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
        
        String protocol = SslProtocolDetector.AdminSSLOnly ? "https" : "http";
        URL = protocol+"://"+ServervilleMain.Hostname+":"+port+"/";
    }
    
    public static void shutdown()
    {
    	BossGroup.shutdownGracefully();
    	WorkerGroup.shutdownGracefully();
    }
}
