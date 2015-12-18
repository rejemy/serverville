package com.dreamwing.serverville.net;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

import javax.activation.MimetypesFileTypeMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;


public class HttpFileServer
{
	private static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
	private static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
	private static final int HTTP_CACHE_SECONDS = 60;
    
    private static MimetypesFileTypeMap MimeTypeMap;
    
    private static String MimeTypes = 
    		"application/font-woff			woff\n"+
    		"application/font-woff2			woff2\n"+
    		"application/javascript			js\n"+
    		"application/json				json\n"+
    		"application/xml				xml\n"+
    		"image/bmp						bmp\n"+
    		"image/gif						gif\n"+
    		"image/jpeg						jpeg jpg\n"+
    		"image/png						png\n"+
    		"text/css						css\n"+
    		"text/html						html htm\n"+
    		"text/plain						txt log\n";
    
    private class CachedFile
    {
    	public long ModifiedAt;
    	public ByteBuf Contents;
    	public boolean Compressed;
    }
    
    private static ConcurrentHashMap<String,CachedFile> FileCache;
    private static Set<String> CompressedTypes;
    
    static
    {
    	MimeTypeMap = new MimetypesFileTypeMap();
    	MimeTypeMap.addMimeTypes(MimeTypes);
    	
    	FileCache = new ConcurrentHashMap<String,CachedFile>();
    	
    	CompressedTypes = new HashSet<String>();
    	CompressedTypes.add("application/javascript");
    	CompressedTypes.add("application/json");
    	CompressedTypes.add("application/xml");
    	CompressedTypes.add("text/css");
    	CompressedTypes.add("text/html");
    }
    
    
    
    public String BaseURL;
    public Path BasePath; 
    public int CacheLessThan;
    
    public HttpFileServer(Path basePath, int cacheLessThan)
    {
    	BasePath = basePath; 
    	CacheLessThan = cacheLessThan;
    }
    
    @HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public ChannelFuture getFile(HttpRequestInfo req) throws ParseException, IOException
	{
		ChannelHandlerContext ctx = req.Connection.Ctx;
		FullHttpRequest request = req.Request;
		URI uri = req.RequestURI;
		
        
        Path filePath = BasePath.resolve(req.PathRemainder).normalize();
        if(!filePath.startsWith(BasePath))
        {
        	return HttpUtil.sendError(req, ApiErrors.FORBIDDEN);
        }
        
        File file = filePath.toFile();
        
        if (file.isDirectory()) {
        	if(!req.RequestURI.getPath().endsWith("/"))
        	{
        		return HttpUtil.sendRedirect(req, uri.getRawPath() + '/');
        	}
        	file = filePath.resolve("index.html").toFile();
        }
        
        
        if (file.isHidden() || !file.exists()) {
            return HttpUtil.sendError(req, ApiErrors.NOT_FOUND);
        }
        
        if (!file.canRead() || !file.isFile()) {
        	return HttpUtil.sendError(req, ApiErrors.FORBIDDEN);
        }
        
        long fileModifiedAt = file.lastModified();

        // Cache Validation
        String ifModifiedSince = request.headers().get(Names.IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

            // Only compare up to the second because the datetime format we send to the client
            // does not have milliseconds
            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
            long fileLastModifiedSeconds = fileModifiedAt / 1000;
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                return sendNotModified(ctx);
            }
        }

        ChannelFuture lastContentFuture;
        long longFileSize = file.length();
        
        String contentType = MimeTypeMap.getContentType(file.getName());
        
        // If the file is small, just cache it in memory instead of reloading it all the dang time
        if(longFileSize < CacheLessThan && acceptsEncoding(request, "gzip"))
        {

        	CachedFile cached = FileCache.get(file.getPath());
        	if(cached == null || cached.ModifiedAt < fileModifiedAt)
        	{
            	int fileSize=0;
            	
        		if(cached != null)
        			cached.Contents.release();
        		
        		cached = new CachedFile();
        		
        		boolean compress = CompressedTypes.contains(contentType);
        		
        		if(compress)
        		{
	        		ByteBuf temp = Unpooled.buffer();
	        		
	        		GZIPOutputStream os = new GZIPOutputStream(new ByteBufOutputStream(temp));
	        		Files.copy(file.toPath(), os);
	        		os.finish();
	        		
	        		fileSize = temp.readableBytes();
	        		cached.Contents = Unpooled.buffer(fileSize);
	        		cached.Contents.writeBytes(temp);
	        		temp.release();
	        		
	        		cached.Compressed = true;
        		}
        		else
        		{
        			fileSize = (int)longFileSize;
        			cached.Contents = Unpooled.buffer(fileSize, fileSize);
            		FileInputStream is = new FileInputStream(file);
            		cached.Contents.writeBytes(is, fileSize);
            		is.close();
            		
            		cached.Compressed = false;
        		}
        		
        		
        		cached.ModifiedAt = fileModifiedAt;
        		
        		FileCache.put(file.getPath(), cached);
        	}
        	

        	HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            HttpHeaders.setContentLength(response, cached.Contents.readableBytes());
            HttpUtil.setContentTypeHeader(response, contentType);
            setDateAndCacheHeaders(response, file);
            if(cached.Compressed)
            	response.headers().set(Names.CONTENT_ENCODING, HttpHeaders.Values.GZIP);
            if (HttpHeaders.isKeepAlive(request)) {
                response.headers().set(Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            }

            // Write the initial line and the header.
            ctx.write(response);
            
            cached.Contents.retain(); // Retain because the write releases it
            ctx.write(cached.Contents);
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        }
        else
        {
	        RandomAccessFile raf;
	        try {
	            raf = new RandomAccessFile(file, "r");
	        } catch (FileNotFoundException ignore) {
	        	return HttpUtil.sendError(req, ApiErrors.NOT_FOUND);
	        }
	
	        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
	        HttpHeaders.setContentLength(response, longFileSize);
	        HttpUtil.setContentTypeHeader(response, contentType);
	        setDateAndCacheHeaders(response, file);
	        if (HttpHeaders.isKeepAlive(request)) {
	            response.headers().set(Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
	        }
	
	        // Write the initial line and the header.
	        ctx.write(response);
	
	        // Write the content.
	        ChannelFuture sendFileFuture;
	        
	        if (ctx.pipeline().get(SslHandler.class) == null) {
	            sendFileFuture =
	                    ctx.write(new DefaultFileRegion(raf.getChannel(), 0, longFileSize), ctx.newProgressivePromise());
	            // Write the end marker.
	            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
	        } else {
	            sendFileFuture =
	                    ctx.write(new HttpChunkedInput(new ChunkedFile(raf, 0, longFileSize, 8192)),
	                            ctx.newProgressivePromise());
	            // HttpChunkedInput will write the end marker (LastHttpContent) for us.
	            lastContentFuture = sendFileFuture;
	        }
	        
	        /*sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                if (total < 0) { // total unknown
                    System.err.println(future.channel() + " Transfer progress: " + progress);
                } else {
                    System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) {
                System.err.println(future.channel() + " Transfer complete.");
            }
        });*/
        }
 
        return lastContentFuture;
	}
	


    private static ChannelFuture sendNotModified(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED);
        setDateHeader(response);

        // Close the connection as soon as the error message is sent.
        return ctx.writeAndFlush(response);
    }


    private static void setDateHeader(FullHttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.headers().set(Names.DATE, dateFormatter.format(time.getTime()));
    }


    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(Names.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(Names.EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(Names.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(
        		Names.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }


    private static boolean acceptsEncoding(FullHttpRequest request, String encoding) {
    	
    	String encodings = request.headers().get(Names.ACCEPT_ENCODING);
    	if(encodings == null)
    		return false;
    	
    	if(encodings.indexOf(encoding) >= 0)
    		return true;
    	
    	return false;
    }
}
