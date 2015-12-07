package com.dreamwing.serverville.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.dreamwing.serverville.util.FileUtil;

public class IndexedLogIndex {
	
	protected IndexedFileManager Manager;
	protected IndexWriter Writer;
	protected DirectoryReader Reader;
	protected SearcherManager SearcherMan;
	protected IndexSearcher Searcher;
	protected File IndexDir;
	
	protected ReadWriteLock CommitLock;
	protected RandomAccessFile LogFile;
	
	protected long CreatedTime;
	protected long LastModifiedTime;
	
	public IndexedLogIndex(String indexFilename, String logFilename, boolean readOnly, IndexedFileManager manager)
	{
		try
		{
			IndexDir = new File(indexFilename);
			Path indexPath = IndexDir.toPath();
			
			Directory dir = FSDirectory.open(indexPath);
			Manager = manager;
			if(readOnly)
			{
				Reader = DirectoryReader.open(dir);
			}
			else
			{
				IndexWriterConfig iwc = new IndexWriterConfig(Manager.getAnalyzer());
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
				iwc.setCommitOnClose(true);
				Writer = new IndexWriter(dir, iwc);
				
				Reader = DirectoryReader.open(Writer, false);
				
				CommitLock = new ReentrantReadWriteLock();
			}
			
			Searcher = new IndexSearcher(Reader);
			
			LogFile = new RandomAccessFile(logFilename, "r");
			
			Path logPath = new File(logFilename).toPath();
			BasicFileAttributes attr = Files.readAttributes(logPath, BasicFileAttributes.class);
			
			CreatedTime = attr.creationTime().toMillis();
			LastModifiedTime = attr.lastModifiedTime().toMillis();
			
		}
		catch(Exception e)
		{
			System.out.println("Error opening index:");
			e.printStackTrace();
		}
	}
	
	public long getCreatedTime() { return CreatedTime; }
	public long getLastModifiedTime() { return LastModifiedTime; }
	
	public void close()
	{
		try
		{
			if(SearcherMan != null)
				SearcherMan.close();
			if(Reader != null)
				Reader.close();
			if(Writer != null)
				Writer.close();
			if(LogFile != null)
				LogFile.close();
		}
		catch(Exception e)
		{
			System.out.println("Error closing index:");
			e.printStackTrace();
		}
		
		Searcher = null;
		SearcherMan = null;
		Reader = null;
		Writer = null;
		LogFile = null;
		CommitLock = null;
	}
	
	public void commit()
	{
		if(Writer == null)
			return;
		
		try
		{
			CommitLock.writeLock().lock();
			Writer.commit();
			DirectoryReader newReader = DirectoryReader.openIfChanged(Reader);
			if(newReader != null)
			{
				Reader.close();
				Reader = newReader;
				Searcher = new IndexSearcher(Reader);
			}
			//System.out.println("Committed log index, current docs: "+Reader.numDocs());
		} catch (IOException e) {
			System.out.println("Error committing index:");
			e.printStackTrace();
		}
		finally
		{
			CommitLock.writeLock().unlock();
		}
	}
	
	public void renameAndReopenReadOnly(String newName, String newLogFilename)
	{
		close();
		
		File newDir = new File(newName);
		File newParent = newDir.getParentFile();
		newParent.mkdirs();
		
		IndexDir.renameTo(newDir);
		IndexDir = newDir;
		
		try
		{
			Directory dir = FSDirectory.open(IndexDir.toPath());
			Reader = DirectoryReader.open(dir);
			Searcher = new IndexSearcher(Reader);
			
			LogFile = new RandomAccessFile(newLogFilename, "r");
		}
		catch(Exception e)
		{
			System.out.println("Error opening index for reading:");
			e.printStackTrace();
		}
		
	}
	
	public void delete()
	{
		close();
		try {
			FileUtil.deleteRecursive(IndexDir);
		} catch (FileNotFoundException e) {
			System.out.println("Error deleting index");
			e.printStackTrace();
		}
	}
	
	public void indexDocument(Document doc, long time) throws IOException
	{
		if(Writer == null)
			return;
		
		Writer.addDocument(doc);
		
		if(time > LastModifiedTime)
			LastModifiedTime = time;
	}
	
	
	public List<IndexedFileManager.LogSearchHit> query(Query query, int maxResults) throws IOException
	{
		List<IndexedFileManager.LogSearchHit> hits = new LinkedList<IndexedFileManager.LogSearchHit>();
		
		if(CommitLock != null)
			CommitLock.readLock().lock();
		
		try
		{
			TopFieldDocs results = Searcher.search(query, maxResults, Manager.GetTimestampSorter());
			for(ScoreDoc score : results.scoreDocs)
			{
				IndexedFileManager.LogSearchHit hit = getLogSearchHit(score.doc);
				hits.add(hit);
			}
		}
		finally
		{
			if(CommitLock != null)
				CommitLock.readLock().unlock();
		}
		
		return hits;
	}
	

	protected IndexedFileManager.LogSearchHit getLogSearchHit(int docID) throws IOException
	{
		Document doc = Searcher.doc(docID);
		if(doc == null)
			return null;
		
		IndexedFileManager.LogSearchHit hit = new IndexedFileManager.LogSearchHit();
		
		long filePos = doc.getField("filepos").numericValue().longValue();
		int length = doc.getField("length").numericValue().intValue();
		
		hit.timestamp = doc.getField("timestamp").numericValue().longValue();
		hit.message =  getLogLine(filePos, length);
		hit.file_pos = (int)filePos;
		
		return hit;
	}
	
	protected String getLogLine(long startPos, int length)
	{
		if(LogFile == null)
			return null;
		
		byte buf[] = new byte[length];
		try {
			synchronized(this)
			{
				LogFile.seek(startPos);
				LogFile.readFully(buf, 0, length);
			}
		} catch (IOException e) {
			return null;
		}
		
		return new String(buf, StandardCharsets.UTF_8);
	}
}
