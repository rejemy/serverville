package com.dreamwing.serverville.log;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.dreamwing.serverville.log.LogIndexManager.LogSearchHit;
import com.dreamwing.serverville.util.FileUtil;
import com.dreamwing.serverville.util.RandomUtil;

// Wrapper that provides a simplified API to a single Lucene index
public class LuceneIndexWrapper {
	
	protected String Id;
	protected boolean ReadOnly;
	protected IndexWriter Writer;
	protected SearcherManager SearchManager;
	protected File IndexFilePath;
	protected Directory IndexDir;
	protected File TimestampFile;
	protected int NumDocuments;
	
	protected ScheduledFuture<?> CommitTask;
	protected volatile boolean Closing;
	
	protected long OldestTimestamp=0;
	protected long NewestTimestamp=0;
	
	protected long LastRefresh;
	final static long REFRESH_RATE = 1000;
	final static long COMMIT_RATE = 2000;
	
	protected static ScheduledThreadPoolExecutor ServiceScheduler;
	{
		ServiceScheduler = new ScheduledThreadPoolExecutor(1);
		ServiceScheduler.setRemoveOnCancelPolicy(true);
	}
	
	public LuceneIndexWrapper(String id, File indexFilePath, boolean readOnly) throws IOException
	{
		Id = id;
		ReadOnly = readOnly;
		IndexFilePath = indexFilePath;

		IndexDir = FSDirectory.open(IndexFilePath.toPath());
		Closing = false;
		
		if(readOnly)
		{
			SearchManager = new SearcherManager(IndexDir, null);
			
			IndexSearcher s = SearchManager.acquire();
			NumDocuments = s.getIndexReader().numDocs();
			SearchManager.release(s);
		}
		else
		{
			IndexWriterConfig iwc = new IndexWriterConfig(LogIndexManager.getAnalyzer());
			iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			iwc.setCommitOnClose(true);
			Writer = new IndexWriter(IndexDir, iwc);
			NumDocuments = Writer.numDocs();
			
			LastRefresh = System.currentTimeMillis();
			SearchManager = new SearcherManager(Writer, false, false, null);
		
			CommitTask = ServiceScheduler.scheduleWithFixedDelay(new CommitThread(), RandomUtil.randInt((int)COMMIT_RATE), COMMIT_RATE, TimeUnit.MILLISECONDS);
		}
		
		TimestampFile = IndexFilePath.toPath().resolve("timewindow").toFile();
		if(TimestampFile.exists() && TimestampFile.canRead())
		{
			DataInputStream read = new DataInputStream(new FileInputStream(TimestampFile));
			OldestTimestamp = read.readLong();
			NewestTimestamp = read.readLong();
			read.close();
		}
	}

	public String getId()
	{
		return Id;
	}
	
	public String getFilename()
	{
		return IndexFilePath.toString();
	}
	
	public boolean overlapsTimePeriod(long lowerTime, long upperTime)
	{
		return OldestTimestamp == 0 || lowerTime <= NewestTimestamp && upperTime >= OldestTimestamp;
	}
	
	public synchronized void close()
	{
		Closing = true;
		
		writeTimestamps();
		
		try
		{
			if(CommitTask != null)
				CommitTask.cancel(false);
			if(SearchManager != null)
				SearchManager.close();
			if(Writer != null)
				Writer.close();
			if(IndexDir != null)
				IndexDir.close();
		}
		catch(Exception e)
		{
			System.out.println("Error closing index:");
			e.printStackTrace();
		}
		
		CommitTask = null;
		SearchManager = null;
		Writer = null;
		IndexDir = null;
	}
	
	public void refresh()
	{
		try {
			SearchManager.maybeRefreshBlocking();
			LastRefresh = System.currentTimeMillis();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected class CommitThread implements Runnable
	{

		@Override
		public void run()
		{
			commit();
		}
		
	}
	
	protected void writeTimestamps()
	{
		try
		{
			DataOutputStream tsFile = new DataOutputStream(new FileOutputStream(TimestampFile));
			tsFile.writeLong(OldestTimestamp);
			tsFile.writeLong(NewestTimestamp);
			tsFile.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public synchronized void commit()
	{
		if(ReadOnly || Closing)
			return;
		
		try
		{
			writeTimestamps();
			Writer.commit();
			SearchManager.maybeRefreshBlocking();
			LastRefresh = System.currentTimeMillis();
			
		} catch (IOException e) {
			System.out.println("Error committing index:");
			e.printStackTrace();
		}

	}
	
	
	public synchronized void reopenReadOnly() throws IOException
	{
		if(ReadOnly)
			return;
		
		close();
		
		ReadOnly = true;
		
		IndexDir = FSDirectory.open(IndexFilePath.toPath());
		SearchManager = new SearcherManager(IndexDir, null);
		
	}
	
	public void delete()
	{
		close();
		
		try {
			FileUtil.deleteRecursive(IndexFilePath);
		} catch (FileNotFoundException e) {
			System.out.println("Error deleting index");
			e.printStackTrace();
		}
	}
	
	public void indexDocument(Document doc, long timestamp) throws IOException
	{
		if(ReadOnly)
			return;
		if(OldestTimestamp == 0)
			OldestTimestamp = timestamp;
		if(timestamp > NewestTimestamp)
			NewestTimestamp = timestamp;
		
		Writer.addDocument(doc);
		NumDocuments++;
	}
	
	public int getNumDocuments()
	{
		return NumDocuments;
	}
	
	public int query(List<LogSearchHit> hits, Query query, int maxResults, Sort sorter) throws IOException
	{
		if(!ReadOnly && System.currentTimeMillis() - LastRefresh > REFRESH_RATE)
			refresh();
		
		IndexSearcher s = SearchManager.acquire();
		
		TopFieldDocs results = null;
		try
		{
			
			results = s.search(query, maxResults, sorter);
			for(ScoreDoc score : results.scoreDocs)
			{
				LogSearchHit hit = getLogSearchHit(s, score.doc);
				hits.add(hit);
			}
		}
		finally
		{
			SearchManager.release(s);
		}
		
		return results.scoreDocs.length;

	}
	
	public int query(List<LogSearchHit> hits, Query query, int maxResults, int startAfterDocId, Sort sorter) throws IOException
	{
		if(!ReadOnly && System.currentTimeMillis() - LastRefresh > REFRESH_RATE)
			refresh();
		
		IndexSearcher s = SearchManager.acquire();
		
		TopDocs results = null;
		try
		{
			ScoreDoc after = new ScoreDoc(startAfterDocId, 0.0f);
			
			results = s.searchAfter(after, query, maxResults, sorter);
			for(ScoreDoc score : results.scoreDocs)
			{
				LogSearchHit hit = getLogSearchHit(s, score.doc);
				hits.add(hit);
			}
		}
		finally
		{
			SearchManager.release(s);
		}
		
		return results.scoreDocs.length;
	}

	protected LogSearchHit getLogSearchHit(IndexSearcher s, int docID) throws IOException
	{
		Document doc = s.doc(docID);
		if(doc == null)
			return null;
		
		LogSearchHit hit = new LogSearchHit();

		hit.timestamp = doc.getField("timestamp").numericValue().longValue();
		hit.log_line =  doc.getField("formatted").stringValue();
		
		return hit;
	}
	
	/*
	private String addTestDocument() throws IOException
	{
		String svid = SVID.makeSVID();
		long time = System.currentTimeMillis();
		Document doc = new Document();
    	doc.add(new NumericDocValuesField("timestamp", time));
    	doc.add(new LongPoint("timestamp", time));
    	doc.add(new StoredField("timestamp", time));
    	doc.add(new StringField("level", "info", Field.Store.YES));
    	doc.add(new StringField("host", "localhost", Field.Store.NO));
    	doc.add(new TextField("message", "It's message "+svid, Field.Store.YES));
    	
    	indexDocument(doc);
    	
    	return svid;
	}
	
	public static void test()
	{
		File dir = ServervilleMain.DataRoot.resolve("test-index").toFile();
		if(dir.exists())
		{
			try {
				FileUtil.deleteRecursive(dir);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		System.out.println("Starting test");
		
		StandardQueryParser Parser = new StandardQueryParser(LogIndexManager.getAnalyzer());
		
		try {
			LuceneIndexWrapper index = new LuceneIndexWrapper(dir.toString(), false);
			
			System.out.println("Adding documents");
			String firstDoc = null;
			for(int i=0; i< 2000; i++)
			{
				String docId = index.addTestDocument();
				if(firstDoc == null)
					firstDoc = docId;
				Thread.sleep(2);
			}
			
			System.out.println("Done adding documents");
			
			
			Query query = Parser.parse(firstDoc, "message");
			
			//Thread.sleep(1010);
			System.out.println("Searching for doc "+firstDoc);
			List<LogSearchHit> hits = index.query(query, 50);
			System.out.println("Got "+hits.size()+" hits");
			

	
			Thread.sleep(100);
			System.out.println("Trying to close");
			index.close();
			
		} catch (Exception e) {
			System.err.println("Error testing index: "+e.getMessage());
			e.printStackTrace();
		}
	}
	*/
}
