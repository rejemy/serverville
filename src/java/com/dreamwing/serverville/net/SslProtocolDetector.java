package com.dreamwing.serverville.net;

import java.io.File;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.dreamwing.serverville.ServervilleMain;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

public class SslProtocolDetector extends ChannelInboundHandlerAdapter {

	private static final Logger l = LogManager.getLogger(SslProtocolDetector.class);
	
	public static SslContext SharedSslContext = null;
	public static boolean AdminSSLOnly = false;
	public static boolean AgentSSLOnly = false;
	public static boolean ClientSSLOnly = false;
	
	public static void init() throws Exception
	{
		String keyFileName = ServervilleMain.ServerProperties.getProperty("ssl_key_file");
        String certChainFileName = ServervilleMain.ServerProperties.getProperty("ssl_cert_chain_file");
        if(keyFileName.length() > 0 && certChainFileName.length() > 0)
        {
        	File keyFile = new File(keyFileName);
        	if(!keyFile.canRead())
        		throw new Exception("SSL key file "+keyFileName+" not readable");
        	File certChainFile = new File(certChainFileName);
        	if(!certChainFile.canRead())
        		throw new Exception("SSL cert chain file "+certChainFile+" not readable");
        	
        	SslContextBuilder builder = SslContextBuilder.forServer(new File(certChainFileName), new File(keyFileName));
        	SharedSslContext = builder.build();
        	
        	AdminSSLOnly = Boolean.parseBoolean(ServervilleMain.ServerProperties.getProperty("admin_ssl_only"));
        	AgentSSLOnly = Boolean.parseBoolean(ServervilleMain.ServerProperties.getProperty("agent_ssl_only"));
        	ClientSSLOnly = Boolean.parseBoolean(ServervilleMain.ServerProperties.getProperty("client_ssl_only"));
        	
        	l.info("Using SSL key "+keyFileName);
        }
        else if(keyFileName.length() > 0 || certChainFileName.length() > 0)
        {
        	throw new Exception("Must set both ssl_key_file and ssl_cert_chain_file for ssl");
        }
 
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		if (msg instanceof ByteBuf) {
			ByteBuf buf = (ByteBuf)msg;
			
			if(buf.getByte(0) == 22)
			{
				httpsDetected(ctx);
			}
			else
			{
				httpDetected(ctx);
			}

		}
		
		ctx.fireChannelRead(msg);
	}
	
	private void httpDetected(ChannelHandlerContext ctx)
	{
		ChannelPipeline pipe = ctx.channel().pipeline();
		pipe.remove(this);
	}
	
	private void httpsDetected(ChannelHandlerContext ctx)
	{
		ChannelPipeline pipe = ctx.channel().pipeline();
		pipe.replace(this, "ssl", SharedSslContext.newHandler(ctx.channel().alloc()));
	}
	


}
