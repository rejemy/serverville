package com.dreamwing.serverville.log;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import com.dreamwing.serverville.net.HttpRequestInfo;
import com.dreamwing.serverville.net.HttpConnectionInfo;

public class SVLog implements Message, StringBuilderFormattable, IndexedLogMessage
{
	private static final long serialVersionUID = 1L;
	
	private String Msg;
	private HttpConnectionInfo Con;
	private HttpRequestInfo Req;
	private String Formatted;
	
	public SVLog(String message)
	{
		Msg = message;
	}
	
	public SVLog(String message, HttpConnectionInfo con)
	{
		Msg = message;
		Con = con;
	}
	
	public SVLog(String message, HttpRequestInfo req)
	{
		Msg = message;
		Req = req;
		Con = req.Connection;
	}
	
	@Override
	public void formatTo(StringBuilder buffer)
	{
		buffer.append(Msg);
		if(Con != null)
		{
			buffer.append(" Connection(ConnectionId:"+Con.ConnectionId+" RemoteHost:"+Con.Ctx.channel().remoteAddress().toString()+")");
		}
		if(Req != null)
		{
			buffer.append(" Request(RequestId:"+Req.RequestId+" URI:"+Req.Request.uri()+")");
		}
	}
	
	@Override
	public String getFormattedMessage() {
		if(Formatted != null)
			return Formatted;
		
		StringBuilder str = new StringBuilder();
		
		formatTo(str);
		
		Formatted = str.toString();
		
		return Formatted;
	}
	
	public void addLuceneFields(Document doc)
	{
		if(Con != null)
		{
			InetSocketAddress addr = (InetSocketAddress)Con.Ctx.channel().remoteAddress();
			
			doc.add(new StringField("connectionId", Con.ConnectionId.toLowerCase(), Field.Store.NO));
			doc.add(new StringField("remoteHost", addr.getHostString().toLowerCase(), Field.Store.NO));
		}
		if(Req != null)
		{
			doc.add(new StringField("requestId", Req.RequestId.toLowerCase(), Field.Store.NO));
			doc.add(new TextField("uri", Req.Request.uri(), Field.Store.NO));
		}
	}
	
	@Override
	public String getFormat() {
		return null;
	}

	@Override
	public Object[] getParameters() {
		return null;
	}

	@Override
	public Throwable getThrowable() {
		return null;
	}

	

}
