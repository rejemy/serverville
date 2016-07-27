package com.dreamwing.serverville.client;
import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;

public class ClientProtocolDetector extends ChannelInboundHandlerAdapter {

	public static byte[] MagicHeaderJson = "SV/JSON\n".getBytes(StandardCharsets.UTF_8);
	
	private int ReadSoFar = 0;
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		if (msg instanceof ByteBuf) {
			ByteBuf buf = (ByteBuf)msg;
			
			
			byte[] message = new byte[MagicHeaderJson.length];
			buf.getBytes(buf.readerIndex(), message);

			for(int i=0; i<buf.readableBytes(); i++)
			{
				byte b = message[i];
				int pos = ReadSoFar+i;
				if(b != MagicHeaderJson[pos])
				{
					ctx.fireChannelRead(msg);
					httpDetected(ctx);
					return;
				}
				
				if(pos == MagicHeaderJson.length-1)
				{
					customProtocolDetected(ctx);
					
					if(buf.readableBytes() > buf.readerIndex()+MagicHeaderJson.length)
					{
						buf.readerIndex(buf.readerIndex()+MagicHeaderJson.length);
						ctx.fireChannelRead(msg);
					}
					
					return;
				}
			}
			
			ReadSoFar += buf.readableBytes();

		}
		
		ctx.fireChannelRead(msg);
	}
	
	private void httpDetected(ChannelHandlerContext ctx)
	{
		ChannelPipeline pipe = ctx.channel().pipeline();
		pipe.remove(this);
	}
	
	private void customProtocolDetected(ChannelHandlerContext ctx)
	{
		ChannelPipeline pipe = ctx.channel().pipeline();
		
		pipe.remove(this);
		
		pipe.remove("httpServer");
		pipe.remove("httpAggregator");
		pipe.remove("chunkedWriter");
		
		pipe.addFirst("jsonDecoder", new ClientJsonProtocolDecoder());
	}
	


}
