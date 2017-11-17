package com.dreamwing.serverville.admin;

import java.nio.file.Path;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.net.HttpDispatcher;
import com.dreamwing.serverville.net.HttpFileServer;
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

public class AdminServerSocketInitializer extends ChannelInitializer<SocketChannel>
{

	private static EventLoopGroup BossGroup;
	private static EventLoopGroup WorkerGroup;
	
	private static HttpDispatcher Dispatcher;
	
	public static String URL;
	

    public AdminServerSocketInitializer() throws Exception
    {
		Path adminFileRoot = ServervilleMain.ResRoot.resolve("admin/webroot");
		int cacheSize = Integer.parseInt(ServervilleMain.ServerProperties.getProperty("cache_files_under"));
		HttpFileServer fileServer = new HttpFileServer(adminFileRoot, cacheSize);
		
		HttpFileServer logFileServer = new HttpFileServer(KeyDataManager.getLogDir().toPath(), 0);
		
		Dispatcher = new HttpDispatcher(new AdminAuthentiator());
		Dispatcher.addRedirect			("/admin", "/admin/");
		Dispatcher.addMethod				("/admin/*", fileServer, "getFile");
		Dispatcher.addAllStaticMethods	("/api/", AdminAPI.class);
		Dispatcher.addMethod				("/logs/*", logFileServer, "getFile");
    }

    @Override
    public void initChannel(SocketChannel ch)
    {
        ChannelPipeline pipeline = ch.pipeline();
       
        if(SslProtocolDetector.SharedSslContext != null)
        {
	        if(SslProtocolDetector.AdminSSLOnly)
	        		pipeline.addLast(SslProtocolDetector.SharedSslContext.newHandler(ch.alloc()));
	        else
	        		pipeline.addLast(new SslProtocolDetector());
        }
        
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(ServervilleMain.MaxRequestSize));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new IdleStateHandler(0, 0, 60));
        pipeline.addLast(new AdminServerConnectionHandler(Dispatcher));
    }
    
    public static void startListener(int port) throws Exception
    {
	    	BossGroup = new NioEventLoopGroup(1);
	    	WorkerGroup = new NioEventLoopGroup();
	    	
	    	ServerBootstrap b = new ServerBootstrap();
	    b.group(BossGroup, WorkerGroup)
	         .channel(NioServerSocketChannel.class)
	         .childHandler(new AdminServerSocketInitializer());

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