package com.dreamwing.serverville.log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.PatternProcessor;
import org.apache.logging.log4j.core.appender.rolling.action.FileRenameAction;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.message.Message;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
//import org.apache.lucene.document.FieldType;
//import org.apache.lucene.document.LegacyLongField;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
//import org.apache.lucene.index.DocValuesType;
//import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
//import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.BooleanClause;

public class IndexedFileManager implements Runnable {

	class LogEventBundle
	{
		public LogEvent Event;
		public Layout<? extends Serializable> Layout;
		boolean Flush;
	}
	
	protected static Map<String, IndexedFileManager> Managers = new HashMap<String, IndexedFileManager>();
	
	protected int UseCount=1;
	
	protected String FileName;
	protected PatternProcessor FileRolloverPattern;
	protected StrSubstitutor ConfigStringSub;
	protected long FlushTimeoutMS;
	protected boolean IsBuffered;
	protected int BufferSize;
	protected boolean IsAppend;
	protected long MaxFileSize;
	protected int MaxLogFiles;
	protected File LogArchiveDir;
	
	protected OutputStream FileOS=null;
	protected long LastFlushTime=0;
	protected boolean UnflushedWrites = false;
	protected long FilePos=0;
	
	protected ReadWriteLock RolloverLock;
	protected Thread LoggingThread;
	protected volatile boolean Running;
	protected BlockingQueue<LogEventBundle> WorkQueue;
	
	
	protected IndexedFileManager(String filename, String filePattern, boolean isAppend, boolean isBuffered,
			int bufferSize, int flushTimeout, long maxFileSize, int maxFiles, Configuration config)
	{
		FileName = filename;
		FileRolloverPattern = new PatternProcessor(filePattern);
		ConfigStringSub = config.getStrSubstitutor();
		FlushTimeoutMS = flushTimeout;
		IsBuffered = isBuffered;
		BufferSize = bufferSize;
		IsAppend = isAppend;
		MaxFileSize = maxFileSize;
		MaxLogFiles = maxFiles;
		
		// Find archive dir
		final StringBuilder tempBuf = new StringBuilder();
		FileRolloverPattern.formatFileName(ConfigStringSub, tempBuf, 0);
		File archiveFile = new File(tempBuf.toString());
		LogArchiveDir = archiveFile.getParentFile().toPath().normalize().toFile();
		
		createLogFile();
		
		initIndexes();
		
		Running = true;
		RolloverLock = new ReentrantReadWriteLock();
		WorkQueue = new LinkedBlockingQueue<LogEventBundle>();
		LoggingThread = new Thread(this, "IndexLoggingThread");
		LoggingThread.start();
	}
	
	public static boolean managerExists(String filename)
	{
		boolean contains=false;
		synchronized(IndexedFileManager.class)
		{
			contains = Managers.containsKey(filename);
		}
		return contains;
	}
	
	public static IndexedFileManager getManager(String filename)
	{
		IndexedFileManager manager = null;
		synchronized(IndexedFileManager.class)
		{
			manager = Managers.get(filename);
		}
		return manager;
	}
	
	public static IndexedFileManager getOrCreateFileManager(String filename, String filePattern, boolean isAppend, boolean isBuffered,
			int bufferSize, int flushTimeout, long maxFileSize, int maxFiles, Configuration config)
	{
		IndexedFileManager manager = null;
		synchronized(IndexedFileManager.class)
		{
			manager = Managers.get(filename);
			if(manager != null)
			{
				manager.UseCount++;
				return manager;
			}
			
			manager = new IndexedFileManager(filename, filePattern, isAppend, isBuffered, bufferSize, flushTimeout, maxFileSize, maxFiles, config);
			Managers.put(filename, manager);
		}
		return manager;
	}
	
	
	public void release()
	{
		synchronized(IndexedFileManager.class)
		{
			UseCount--;
			if(UseCount <= 0)
			{
				shutdown();
			}
		}
	}
	
	public void append(LogEvent event, Layout<? extends Serializable> layout, final boolean flush)
	{
		LogEventBundle bundle = new LogEventBundle();
		bundle.Event = event;
		bundle.Layout = layout;
		bundle.Flush = flush;
		
		try {
			WorkQueue.put(bundle);
		} catch (InterruptedException e) {
			System.out.println("Adding log item to log queue threw exception, should not happen");
			e.printStackTrace();
		}
	}

	protected void createLogFile() {
		try
		{
			File logFile = new File(FileName);
			File parent = logFile.getParentFile();
			parent.mkdirs();
	        final FileOutputStream os = new FileOutputStream(FileName, IsAppend);
	        if (IsBuffered)
	        {
	        	FileOS = new BufferedOutputStream(os, BufferSize);
	        } else {
	        	FileOS = os;
	        }
	        if(logFile.exists())
	        	FilePos = logFile.length();
	        else
	        	FilePos = 0;
		}
		catch(IOException e)
		{
			System.out.println("Unable to open log file: "+FileName);
			e.printStackTrace();
		}
    }
	
	
	protected void shutdown()
	{
		Running = false;
		Managers.remove(FileName);
		LoggingThread.interrupt();
	}
	
	protected void closeFile()
	{
		try {
			FileOS.flush();
			FileOS.close();
		} catch (IOException e) {
			System.out.println("Exception closing log file "+FileName);
			e.printStackTrace();
		}
		FileOS = null;
	}
	
	public void flush()
	{
		try {
			FileOS.flush();
			LastFlushTime = System.currentTimeMillis();
			UnflushedWrites = false;
		} catch (IOException e) {
			System.out.println("Exception flushing log file "+FileName);
			e.printStackTrace();
		}
		
		ActiveWriteIndex.commit();
	}

	protected void checkRollover(final LogEvent event)
	{
        if(FilePos >= MaxFileSize)
        {
        	doRollover();
        }
    }
	
	protected File findNextFilename()
	{
		int i = 1;
		while(MaxLogFiles == 0 || i <= MaxLogFiles)
		{
			final StringBuilder newFilenameBuf = new StringBuilder();
			FileRolloverPattern.formatFileName(ConfigStringSub, newFilenameBuf, i);
			i++;
			
			File nextFile = new File(newFilenameBuf.toString());
			if(nextFile.exists())
			{	
				continue;
			}
			
			return nextFile;
		}
		
		return null;
	}
	
	protected void doRollover()
	{
		closeFile();
		
		File rolloverToFile = findNextFilename();
		
		RolloverLock.writeLock().lock();
		try
		{
			if(rolloverToFile != null)
			{
				File currFile = new File(FileName);
				FileRenameAction.execute(currFile, rolloverToFile, true);
			}
			else
			{
				System.out.println("Couldn't find a new name for the log file to rollover to that wasn't already taken!");
			}
			
			createLogFile();
	    	
			if(rolloverToFile != null)
			{
				rolloveActiveIndex(rolloverToFile.getPath());
			}
			
			doOldFileCleanup();
		}
		finally
		{
			RolloverLock.writeLock().unlock();
		}
		
	}
	
	protected void doOldFileCleanup()
	{
		if(MaxLogFiles == 0)
			return;
		
		File archiveFiles[] = LogArchiveDir.listFiles();
		
		List<File> logFileList = new ArrayList<File>(archiveFiles.length);
		
		for(File archive : archiveFiles)
		{
			if(archive.isFile() && archive.getName().endsWith(".log"))
			{
				logFileList.add(archive);
			}
		}
		
		if(logFileList.size() <= MaxLogFiles)
			return;
		
		logFileList.sort((f1, f2) -> (int)(f2.lastModified() - f1.lastModified()));
		while(logFileList.size() > MaxLogFiles)
		{
			File toDelete = logFileList.remove(logFileList.size()-1);
			toDelete.delete();
			deleteIndex(toDelete.getPath());
		}
	}
	
	@Override
	public void run()
	{
		while(Running)
		{
			try {
				LogEventBundle bundle = WorkQueue.poll(FlushTimeoutMS, TimeUnit.MILLISECONDS);
				long currTime = System.currentTimeMillis();
				if(bundle == null)
				{
					if(UnflushedWrites && currTime - LastFlushTime >= FlushTimeoutMS)
						flush();
					continue;
				}
				
				checkRollover(bundle.Event);
				
				byte bytes[] = bundle.Layout.toByteArray(bundle.Event);
				
				long startPos = FilePos;
				int bufferLength = bytes.length;
				
				FileOS.write(bytes, 0, bufferLength);
				FilePos += bytes.length;
				
				indexLogEvent(bundle.Event, startPos, bufferLength);
				
				if(bundle.Flush || currTime - LastFlushTime >= FlushTimeoutMS)
					flush();
				else
					UnflushedWrites = true;
				
				
				
			} catch (InterruptedException e) {
				// Probably just a shutdown
			} catch (IOException e) {
				System.out.println("Exception writing to logfile: "+FileName);
				e.printStackTrace();
			}
		}
		
		closeFile();
		shutdownIndexes();
	}
	
	// Index stuff
	
	protected Map<String, IndexedLogIndex> Indexes;
	protected IndexedLogIndex ActiveWriteIndex;
	protected String Hostname;
	protected Analyzer TextAnalyzer;
	protected Sort TimestampSorter;
	
	  /** 
	   * Type for a stored sorted LongField:
	   */
	  /*public static final FieldType LONG_STORED_SORTED = new FieldType();
	  static {
		LONG_STORED_SORTED.setTokenized(true);
	    LONG_STORED_SORTED.setOmitNorms(true);
	    LONG_STORED_SORTED.setIndexOptions(IndexOptions.DOCS);
	    LONG_STORED_SORTED.setNumericType(FieldType.LegacyNumericType.LONG);
	    LONG_STORED_SORTED.setStored(true);
	    LONG_STORED_SORTED.setDocValuesType(DocValuesType.NUMERIC);
	    LONG_STORED_SORTED.freeze();
	  }*/
	
	protected void indexLogEvent(LogEvent event, long filePos, int eventLength)
	{
		if(ActiveWriteIndex == null)
			return;
		
		try
		{
			Document doc = new Document();
	    	doc.add(new StoredField("filepos", filePos));
	    	doc.add(new StoredField("length", eventLength-1)); // remove newline
	    	doc.add(new NumericDocValuesField("timestamp", event.getTimeMillis()));
	    	doc.add(new LongPoint("timestamp", event.getTimeMillis()));
	    	doc.add(new StoredField("timestamp", event.getTimeMillis()));
	    	//doc.add(new LegacyLongField("timestamp", event.getTimeMillis(), LONG_STORED_SORTED));
	    	doc.add(new StringField("level", event.getLevel().toString().toLowerCase(), Field.Store.YES));
	    	doc.add(new StringField("host", Hostname.toLowerCase(), Field.Store.NO));
	    	Message m = event.getMessage();
	    	String entireMessage = m.getFormattedMessage();
	    	doc.add(new TextField("all", entireMessage, Field.Store.NO));
	    	if(m instanceof IndexedLogMessage)
	    	{
	    		IndexedLogMessage logInfo = (IndexedLogMessage)m;
	    		logInfo.toLuceneDocument(doc);
	    	}
	    	else
	    	{
	    		doc.add(new TextField("message", entireMessage, Field.Store.NO));
	    	}
	    	
	    	ActiveWriteIndex.indexDocument(doc, event.getTimeMillis());
		}
    	catch(Exception e)
    	{
    		System.out.println("Exception indexing a log event");
			e.printStackTrace();
    	}
	}
	
	protected void initIndexes()
	{
		try {
			Hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			System.out.println("Can't get local hostname");
			e.printStackTrace();
			Hostname = "localhost";
		}
		
		TextAnalyzer = new StandardAnalyzer();
		TimestampSorter = new Sort(new SortField("timestamp", SortField.Type.LONG));
		
		Indexes = new HashMap<String, IndexedLogIndex>();
		String activeIndexName = filenameToIndexName(FileName);
		ActiveWriteIndex = new IndexedLogIndex(activeIndexName, FileName, false, this);
		
		File archiveFiles[] = LogArchiveDir.listFiles();
		
		for(File archive : archiveFiles)
		{
			if(archive.isFile() && archive.getName().endsWith(".log"))
			{
				if(archive.getPath().equals(FileName))
					continue;
				
				String indexName = filenameToIndexName(archive.getPath());
				if(new File(indexName).exists())
				{
					IndexedLogIndex idx = new IndexedLogIndex(indexName, archive.getPath(), true, this);
					Indexes.put(archive.getPath(), idx);
				}
			}
		}
	}
	
	protected void shutdownIndexes()
	{
		ActiveWriteIndex.close();
		ActiveWriteIndex = null;
		for(IndexedLogIndex index : Indexes.values())
		{
			index.close();
		}
		Indexes.clear();
	}
	
	protected void deleteIndex(String logFileName)
	{
		String indexName = filenameToIndexName(logFileName);
		IndexedLogIndex idx = Indexes.get(indexName);
		if(idx != null)
		{
			idx.delete();
			Indexes.remove(indexName);
		}
	}
	
	protected void rolloveActiveIndex(String newLogFilename)
	{
		String newIndexName = filenameToIndexName(newLogFilename);
		
		ActiveWriteIndex.renameAndReopenReadOnly(newIndexName, newLogFilename);
		
		Indexes.put(newIndexName, ActiveWriteIndex);
		
		String activeIndexName = filenameToIndexName(FileName);
		ActiveWriteIndex = new IndexedLogIndex(activeIndexName, FileName, false, this);
	}
	
	protected static String filenameToIndexName(String filename)
	{
		filename = new File(filename).toPath().normalize().toString();
		if(filename.endsWith(".log"))
		{
			filename = filename.substring(0, filename.length()-4);
		}
		
		
		return filename+"-index";
	}
	
	public static class LogSearchHit
	{
		public double timestamp;
		public String message;
		public int file_pos;
	}
	
	public static class LogSearchHits
	{
		public List<LogSearchHit> hits;
	}
	
	
	public LogSearchHits query(String queryStr, long lowerTime, long upperTime) throws ParseException
	{
		QueryParser parser = new QueryParser("all", TextAnalyzer);
		Query query = parser.parse(queryStr);
		
		return query(query, lowerTime, upperTime);
	}
	
	public LogSearchHits query(Query query, long lowerTime, long upperTime)
	{

		int maxResults = 100;
		
		if(lowerTime != Long.MIN_VALUE || upperTime != Long.MAX_VALUE)
		{
			//Query rangeQuery = LegacyNumericRangeQuery.newLongRange("timestamp", lowerTime != 0 ? lowerTime : null, upperTime != 0 ? upperTime : null, false, true);
			Query rangeQuery = LongPoint.newRangeQuery("timestamp", lowerTime, upperTime);
			BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
			booleanQueryBuilder.add(query, BooleanClause.Occur.MUST);
			booleanQueryBuilder.add(rangeQuery, BooleanClause.Occur.FILTER);
			query = booleanQueryBuilder.build();
		}

		RolloverLock.readLock().lock();
		try
		{
			try
			{
				LogSearchHits hits = new LogSearchHits();
				hits.hits = new LinkedList<LogSearchHit>();
				
				if(lowerTime == 0 || ActiveWriteIndex.getLastModifiedTime() >= lowerTime &&
					upperTime == 0 || ActiveWriteIndex.getCreatedTime() <= upperTime)
				{
					List<LogSearchHit> logHits = ActiveWriteIndex.query(query, maxResults);
					hits.hits.addAll(logHits);
				}
				
				for(IndexedLogIndex index : Indexes.values())
				{
					if(lowerTime == 0 || index.getLastModifiedTime() >= lowerTime &&
							upperTime == 0 || index.getCreatedTime() <= upperTime)
						{
							List<LogSearchHit> logHits = index.query(query, maxResults);
							hits.hits.addAll(logHits);
						}
				}
				
				return hits;
				
			} catch (IOException e) {
				System.out.println("Exception quering an index:");
				e.printStackTrace();
			}
			
		}
		finally
		{
			RolloverLock.readLock().unlock();
		}
		
		return null;
		
	}
	
	public Analyzer getAnalyzer()
	{
		return TextAnalyzer;
	}
	
	public Sort GetTimestampSorter()
	{
		return TimestampSorter;
	}
	
}
