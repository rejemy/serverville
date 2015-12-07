package com.dreamwing.serverville.log;

import java.io.Serializable;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.Integers;

@Plugin(name = "IndexedFile", category = "Core", elementType = "appender", printObject = true)
public class IndexedFileAppender extends AbstractAppender
{

	private static final long serialVersionUID = 1L;
	
	protected static final int DEFAULT_BUFFER_SIZE = 8192;
	protected static final int DEFAULT_MAX_FLUSH_MS = 1000;
	protected static final int DEFAULT_MAX_FILES = 0;
	
	private static final long KB = 1024;
    private static final long MB = KB * KB;
    private static final long GB = KB * MB;

    private static final Pattern FILE_SIZE_VALUE_PATTERN =
            Pattern.compile("([0-9]+([\\.,][0-9]+)?)\\s*(|K|M|G)B?", Pattern.CASE_INSENSITIVE);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // let 10 MB the default max size
	
    private String Filename;
    
	protected boolean ImmediateFlush;
	protected IndexedFileManager Manager;
	
	public IndexedFileAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
            final boolean immediateFlush, IndexedFileManager manager, final String fileName)
	{
		super(name, filter, layout, true);
		
		Filename = fileName;
		ImmediateFlush = immediateFlush;
		Manager = manager;

	}
	
	/**
     * Converts a string to a number of bytes. Strings consist of a floating point value followed by
     * K, M, or G for kilobytes, megabytes, gigabytes, respectively. The
     * abbreviations KB, MB, and GB are also accepted. Matching is case insensitive.
     *
     * @param string The string to convert
     * @return The Bytes value for the string
     */
    private static long parseFileSize(final String string) {
    	if(string == null)
    		return MAX_FILE_SIZE;
    	
        final Matcher matcher = FILE_SIZE_VALUE_PATTERN.matcher(string);

        // Valid input?
        if (matcher.matches()) {
            try {
                // Get double precision value
                final long value = NumberFormat.getNumberInstance(Locale.getDefault()).parse(
                    matcher.group(1)).longValue();

                // Get units specified
                final String units = matcher.group(3);

                if (units.isEmpty()) {
                    return value;
                } else if (units.equalsIgnoreCase("K")) {
                    return value * KB;
                } else if (units.equalsIgnoreCase("M")) {
                    return value * MB;
                } else if (units.equalsIgnoreCase("G")) {
                    return value * GB;
                } else {
                    LOGGER.error("Units not recognized: " + string);
                    return MAX_FILE_SIZE;
                }
            } catch (final ParseException e) {
                LOGGER.error("Unable to parse numeric part: " + string, e);
                return MAX_FILE_SIZE;
            }
        }
        LOGGER.error("Unable to parse bytes: " + string);
        return MAX_FILE_SIZE;
    }
    
	/**
     * Create a IndexedFileAppender.
     * @param fileName The name of the file that is actively written to. (required).
     * @param filePattern The pattern of the file name to use on rollover. (required).
     * @param append If true, events are appended to the file. If false, the file
     * is overwritten when opened. Defaults to "true"
     * @param name The name of the Appender (required).
     * @param bufferedIO When true, I/O will be buffered. Defaults to "true".
     * @param bufferSizeStr buffer size for buffered IO (default is 8192).
     * @param flushTimerStr maximum milliseconds to wait before flushing (default is 1000).
     * @param immediateFlush When true, events are immediately flushed. Defaults to "true".
     * @param maxSizeStr maximum size of file before rollover occurs (default is 100MB).
     * @param layout The layout to use (defaults to the default PatternLayout).
     * @param filter The Filter or null.
     * @param config The Configuration.
     * @return A IndexedFileAppender.
     */
    @PluginFactory
    public static IndexedFileAppender createAppender(
            @PluginAttribute("fileName") final String fileName,
            @PluginAttribute("filePattern") final String filePattern,
            @PluginAttribute("append") final String append,
            @PluginAttribute("name") final String name,
            @PluginAttribute("bufferedIO") final String bufferedIO,
            @PluginAttribute("bufferSize") final String bufferSizeStr,
            @PluginAttribute("flushTimer") final String flushTimerStr,
            @PluginAttribute("immediateFlush") final String immediateFlush,
            @PluginAttribute("maxSize") final String maxSizeStr,
            @PluginAttribute("maxFiles") final String maxFilesStr,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginConfiguration final Configuration config) {

        final boolean isAppend = Booleans.parseBoolean(append, true);
        final boolean isBuffered = Booleans.parseBoolean(bufferedIO, true);
        final boolean isFlush = Booleans.parseBoolean(immediateFlush, false);
        final int bufferSize = Integers.parseInt(bufferSizeStr, DEFAULT_BUFFER_SIZE);
        final int flushTimeout = Integers.parseInt(flushTimerStr, DEFAULT_MAX_FLUSH_MS);
        final int maxFiles = Integers.parseInt(maxFilesStr, DEFAULT_MAX_FILES);
        
        if (!isBuffered && bufferSize > 0) {
            LOGGER.warn("The bufferSize is set to {} but bufferedIO is not true: {}", bufferSize, bufferedIO);
        }
        if (name == null) {
            LOGGER.error("No name provided for FileAppender");
            return null;
        }

        if (fileName == null) {
            LOGGER.error("No filename was provided for FileAppender with name "  + name);
            return null;
        }
        
        boolean managerAlreadyExists = IndexedFileManager.managerExists(fileName);
        if (filePattern == null && !managerAlreadyExists) {
            LOGGER.error("No filename pattern provided for FileAppender with name "  + name);
            return null;
        }

        long maxFileSize = parseFileSize(maxSizeStr);
        
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        final IndexedFileManager manager = IndexedFileManager.getOrCreateFileManager(fileName, filePattern, isAppend,
            isBuffered, bufferSize, flushTimeout, maxFileSize, maxFiles, config);
        if (manager == null) {
            return null;
        }
        
        return new IndexedFileAppender(name, layout, filter, isFlush, manager, fileName);
    }

	@Override
	public void append(LogEvent event)
	{
		Manager.append(event, getLayout(), ImmediateFlush);
	}
	
	/**
     * Returns the File name for the Appender.
     * @return The file name.
     */
    public String getFileName() {
        return Filename;
    }


}
