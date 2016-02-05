package com.dreamwing.serverville.client;

import java.nio.ByteOrder;
import java.util.List;

import com.dreamwing.serverville.serialize.SerializeUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class ClientJsonProtocolDecoder extends ByteToMessageDecoder
{

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
	{
		in = in.order(ByteOrder.LITTLE_ENDIAN);
		
		while(in.readableBytes() >= 2)
		{	
			in.markReaderIndex();
			int messageSize = in.readUnsignedShort();
			if(in.readableBytes() < messageSize)	
			{
				in.resetReaderIndex();
				return;
			}
			
			try
			{
				ClientJsonMessageWrapper message = new ClientJsonMessageWrapper();
				
				message.Api = SerializeUtil.readUTF(in);
				message.MessageNum = SerializeUtil.readUTF(in);
				message.RequestJson = SerializeUtil.readUTF(in);
				
				out.add(message);
			}
			finally
			{
				// If we had an exception decoding the message, make sure we skip over the whole thing
				// so we don't get stuck on the same bad data
				
				in.resetReaderIndex();
				in.readerIndex(in.readerIndex() + messageSize + 2);
			}
		}
	}

}
