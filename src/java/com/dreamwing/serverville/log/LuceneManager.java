package com.dreamwing.serverville.log;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.ConfigurationFactoryData;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.message.Message;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.util.SVID;


public class LuceneManager extends AbstractManager {

	private static final LuceneManagerFactory FACTORY = new LuceneManagerFactory();
	
	private LuceneIndexWrapper LogIndexer;
	private Path IndexBasePath;
	
	private int MaxDocumentsPerIndex;
	private int MaxIndexes;
	
	private List<LuceneIndexWrapper> AllIndexes;
	
	protected LuceneManager(LoggerContext loggerContext, final String indexPath, final int maxDocumentsPerIndex, final int maxIndexes) throws IOException
	{
		super(loggerContext, indexPath);
		
		MaxDocumentsPerIndex = maxDocumentsPerIndex;
		MaxIndexes = maxIndexes;
		
		File pathFile = new File(indexPath);
		IndexBasePath = pathFile.toPath();
		if(pathFile.exists())
		{
			if(!pathFile.isDirectory())
				throw new IOException("File already exists at index path");
			if(!pathFile.canRead() || !pathFile.canWrite())
				throw new IOException("Insufficient permissions to read/write index path");
		}
		else
		{
			pathFile.mkdirs();
		}
		
		List<String> indexes = new ArrayList<String>();
		
		File[] indexDirs = pathFile.listFiles();
		for(int f=0; f<indexDirs.length; f++)
		{
			File indexDir = indexDirs[f];
			if(!indexDir.isDirectory() || indexDir.getName().startsWith("."))
				continue;
			
			indexes.add(indexDir.getName());
		}
		
		AllIndexes = new LinkedList<LuceneIndexWrapper>();
		
		if(indexes.size() == 0)
		{
			String indexName = SVID.makeSVID();
			File indexFilename = IndexBasePath.resolve(indexName).toFile();
			
			LuceneIndexWrapper indexWrapper = new LuceneIndexWrapper(indexName, indexFilename, false);
			LogIndexManager.addIndex(indexWrapper);
			LogIndexer = indexWrapper;
			
			AllIndexes.add(LogIndexer);
		}
		else
		{
			indexes.sort(null);
			
			for(int i=0; i<indexes.size(); i++)
			{
				String indexName = indexes.get(i);
				
				File indexFilename = IndexBasePath.resolve(indexName).toFile();
				
				if(i < indexes.size()-1)
				{
					LuceneIndexWrapper indexWrapper = new LuceneIndexWrapper(indexName, indexFilename, true);
					LogIndexManager.addIndex(indexWrapper);
					AllIndexes.add(indexWrapper);
				}
				else
				{
					LuceneIndexWrapper indexWrapper = new LuceneIndexWrapper(indexName, indexFilename, false);
					LogIndexManager.addIndex(indexWrapper);
					AllIndexes.add(indexWrapper);
					LogIndexer = indexWrapper;
				}
			}
		}
		
	}
	
	public void append(LogEvent event, Layout<? extends Serializable> layout)
	{
		if(LogIndexer == null)
			return;
		
		checkRollover();
		
		Document doc = new Document();
    	doc.add(new NumericDocValuesField("timestamp", event.getTimeMillis()));
    	doc.add(new LongPoint("timestamp", event.getTimeMillis()));
    	doc.add(new StoredField("timestamp", event.getTimeMillis()));
    	doc.add(new StringField("level", event.getLevel().toString(), Field.Store.NO));
    	doc.add(new StringField("host", ServervilleMain.Hostname, Field.Store.NO));
    	
    	Message m = event.getMessage();
    	String message = m.getFormattedMessage();
    	doc.add(new TextField("message", message, Field.Store.NO));
    	
    	if(m instanceof IndexedLogMessage)
    	{
    		IndexedLogMessage logInfo = (IndexedLogMessage)m;
    		logInfo.addLuceneFields(doc);
    	}
    	
    	String formattedEvent = (String)layout.toSerializable(event);
    	doc.add(new StoredField("formatted", (String)formattedEvent));
 
    	
    	try {
			LogIndexer.indexDocument(doc, event.getTimeMillis());
		} catch (IOException e) {
			LOGGER.error("LuceneManager.append", e);
			return;
		}
	}
	
	private void checkRollover()
	{
		if(LogIndexer.getNumDocuments() < MaxDocumentsPerIndex)
			return;
		
		rolloverIndex();
	}
	
	private synchronized void rolloverIndex()
	{
		// Check again in case multiple threads made it through checkRollover at the same time
		if(LogIndexer.getNumDocuments() < MaxDocumentsPerIndex)
			return;
		
		String indexName = SVID.makeSVID();
		File indexFilename = IndexBasePath.resolve(indexName).toFile();
		
		try {
			LuceneIndexWrapper indexWrapper = new LuceneIndexWrapper(indexName, indexFilename, false);
			LogIndexManager.addIndex(indexWrapper);
			
			LuceneIndexWrapper previousIndex = LogIndexer;
			
			AllIndexes.add(indexWrapper);
			LogIndexer = indexWrapper;
			
			previousIndex.reopenReadOnly();
			
			while(AllIndexes.size() > MaxIndexes)
			{
				LuceneIndexWrapper oldIndex = AllIndexes.remove(0);
				LogIndexManager.removeIndex(oldIndex);
				oldIndex.delete();
			}
			
		} catch (IOException e) {
			LOGGER.error("LuceneManager.rolloverIndex", e);
		}
		
	}
	
	@Override
	public boolean stop(final long timeout, final TimeUnit timeUnit)
	{
		if(LogIndexer != null)
		{
			LogIndexManager.removeIndex(LogIndexer);
			LogIndexer.close();
			LogIndexer = null;
		}
		
		return super.stop(timeout,  timeUnit);
	}
	 
	/**
     * Returns the LuceneManager.
     * @param name The name of the lucene index to manage.
     * @param configuration The configuration.
     * @return A FileManager for the File.
     */
    public static LuceneManager getLuceneManager(final String indexPath, final int maxDocumentsPerIndex, final int maxIndexes, final Configuration configuration) {
    	return AbstractManager.getManager(indexPath, FACTORY, new FactoryData(maxDocumentsPerIndex, maxIndexes, configuration));
    }
    
    /**
     * Factory Data.
     */
    private static class FactoryData extends ConfigurationFactoryData
    {
    	
    	private final int maxDocumentsPerIndex;
    	private final int maxIndexes;
    	
        /**
         * Constructor.
         * @param maxDocumentsPerIndex Number of documents to before the appender rolls over into a new index.
         * @param maxIndexes Number of indexes to keep before old ones are deleted.
         * @param configuration the configuration
         */
        public FactoryData(final int maxDocumentsPerIndex, final int maxIndexes, final Configuration configuration) {
            super(configuration);
            this.maxDocumentsPerIndex = maxDocumentsPerIndex;
            this.maxIndexes = maxIndexes;
        }
        
        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append(super.toString());
            builder.append("[maxDocumentsPerIndex=");
            builder.append(maxDocumentsPerIndex);
            builder.append(", maxIndexes=");
            builder.append(maxIndexes);
            builder.append("]");
            return builder.toString();
        }
    }
    
    /**
     * Factory to create a LuceneManager.
     */
    private static class LuceneManagerFactory implements ManagerFactory<LuceneManager, FactoryData> {

        /**
         * Creates a FileManager.
         * @param name The name of the File.
         * @param data The FactoryData
         * @return The FileManager for the File.
         */
        @Override
        public LuceneManager createManager(final String indexPath, final FactoryData data) {
        	try {
				return new LuceneManager(data.getLoggerContext(), indexPath, data.maxDocumentsPerIndex, data.maxIndexes);
			} catch (IOException ex) {
				LOGGER.error("LuceneManager (" + indexPath + ") " + ex, ex);
			}
        	return null;
        }
    }
    
    @Override
    public void updateData(final Object data)
    {
    	//final FactoryData factoryData = (FactoryData) data;
    }

}
