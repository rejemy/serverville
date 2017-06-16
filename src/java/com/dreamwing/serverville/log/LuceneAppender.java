package com.dreamwing.serverville.log;


import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.util.Booleans;

/**
 * Lucene Appender.
 */
@Plugin(name = LuceneAppender.PLUGIN_NAME, category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class LuceneAppender extends AbstractAppender {

    public static final String PLUGIN_NAME = "Lucene";

    /**
     * Builds LuceneAppender instances.
     * 
     * @param <B>
     *            The type to build
     */
    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<LuceneAppender> {

    	@PluginBuilderAttribute
        @Required
        private String indexPath;
    	
    	@PluginBuilderAttribute
        private int maxDocumentsPerIndex=10000;
    	
    	@PluginBuilderAttribute
        private int maxIndexes=10;
        
        @Override
        public LuceneAppender build() {
            
            final Layout<? extends Serializable> layout = getOrCreateLayout();
            if(!(layout instanceof AbstractStringLayout))
            {
            	LOGGER.error("Unsupported layout type for Lucene Appender");
            	return null;
            }
            final LuceneManager manager = LuceneManager.getLuceneManager(indexPath, maxDocumentsPerIndex, maxIndexes, getConfiguration());
            if (manager == null) {
                return null;
            }
            
            return new LuceneAppender(getName(), indexPath, getFilter(), layout, isIgnoreExceptions(), manager);
        }
        
        public String getIndexPath() {
            return indexPath;
        }
        
        public int getMaxDocumentsPerIndex() {
        	return maxDocumentsPerIndex;
        }
        
        public int getMaxIndexes() {
        	return maxIndexes;
        }
        
        public B withIndexPath(final String indexPath) {
            this.indexPath = indexPath;
            return asBuilder();
        }
        
        public B withMaxDocumentsPerIndex(final int maxDocumentsPerIndex) {
            this.maxDocumentsPerIndex = maxDocumentsPerIndex;
            return asBuilder();
        }
        
        public B withMaxIndexes(final int maxIndexes) {
            this.maxIndexes = maxIndexes;
            return asBuilder();
        }
        

    }

    /**
     * Create a LuceneAppender.
     * @param fileName The name and path of the file.
     * @param append "True" if the file should be appended to, "false" if it should be overwritten.
     * The default is "true".
     * @param locking "True" if the file should be locked. The default is "false".
     * @param name The name of the Appender.
     * @param immediateFlush "true" if the contents should be flushed on every write, "false" otherwise. The default
     * is "true".
     * @param ignoreExceptions If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param bufferedIo "true" if I/O should be buffered, "false" otherwise. The default is "true".
     * @param bufferSizeStr buffer size for buffered IO (default is 8192).
     * @param layout The layout to use to format the event. If no layout is provided the default PatternLayout
     * will be used.
     * @param filter The filter, if any, to use.
     * @param advertise "true" if the appender configuration should be advertised, "false" otherwise.
     * @param advertiseUri The advertised URI which can be used to retrieve the file contents.
     * @param config The Configuration
     * @return The FileAppender.
     * @deprecated Use {@link #newBuilder()}
     */
    @Deprecated
    public static <B extends Builder<B>> LuceneAppender createAppender(
            // @formatter:off
            final String name,
            final String indexPath,
            final int maxDocumentsPerIndex,
            final int maxIndexes,
            final String ignoreExceptions,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final Configuration config) {
        return LuceneAppender.<B>newBuilder()
            .setConfiguration(config)
            .withFilter(filter)
            .withIgnoreExceptions(Booleans.parseBoolean(ignoreExceptions, true))
            .withLayout(layout)
            .withName(name)
            .withIndexPath(indexPath)
            .withMaxDocumentsPerIndex(maxDocumentsPerIndex)
            .withMaxIndexes(maxIndexes)
            .build();
        // @formatter:on
    }
    
    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }
    
    private final String indexPath;
    
    private LuceneManager manager;
    
    private LuceneAppender(final String name, final String indexPath, final Filter filter, final Layout<? extends Serializable> layout,
    		final boolean ignoreExceptions, final LuceneManager manager) {

        super(name, filter, layout, ignoreExceptions);
        this.manager = manager;
        this.indexPath = indexPath;
    }


    @Override
    public void start() {
        if (getLayout() == null) {
            LOGGER.error("No layout set for the appender named [" + getName() + "].");
        }
        if (manager == null) {
            LOGGER.error("No LuceneManager set for the appender named [" + getName() + "].");
        }
        super.start();
    }


    @Override
    protected boolean stop(final long timeout, final TimeUnit timeUnit, final boolean changeLifeCycleState) {
    	boolean stopped = super.stop(timeout, timeUnit, changeLifeCycleState);
        stopped &= manager.stop(timeout, timeUnit);
        if (changeLifeCycleState) {
            setStopped();
        }
        LOGGER.debug("Appender {} stopped with status {}", getName(), stopped);
        return stopped;
    }
    
    
    /**
     * Gets the index path setting.
     *
     * @return index name.
     */
    public String getIndexPath() {
        return indexPath;
    }
    
    
	@Override
	public void append(LogEvent event)
	{
		manager.append(event, getLayout());
	}
}
