package com.dreamwing.serverville.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConfigurationFactoryData;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.FileUtils;

/**
 * The Rolling File Manager.
 */
public class IndexedRollingFileManager extends RollingFileManager {

    private static IndexedRollingFileManagerFactory factory = new IndexedRollingFileManagerFactory();

    @Deprecated
    protected IndexedRollingFileManager(final String fileName, final String pattern, final OutputStream os,
            final boolean append, final long size, final long time, final TriggeringPolicy triggeringPolicy,
            final RolloverStrategy rolloverStrategy, final String advertiseURI,
            final Layout<? extends Serializable> layout, final int bufferSize, final boolean writeHeader) {
        super(fileName, pattern, os, append, size, time, triggeringPolicy, rolloverStrategy, advertiseURI, layout,
        		bufferSize, writeHeader);
    }

    @Deprecated
    protected IndexedRollingFileManager(final String fileName, final String pattern, final OutputStream os,
            final boolean append, final long size, final long time, final TriggeringPolicy triggeringPolicy,
            final RolloverStrategy rolloverStrategy, final String advertiseURI,
            final Layout<? extends Serializable> layout, final boolean writeHeader, final ByteBuffer buffer) {
        super(fileName, pattern, os, append, size, time, triggeringPolicy, rolloverStrategy, advertiseURI,
        		layout, writeHeader, buffer);
    }

    /**
     * @since 2.7
     */
    protected IndexedRollingFileManager(final LoggerContext loggerContext, final String fileName, final String pattern, final OutputStream os,
            final boolean append, final boolean createOnDemand, final long size, final long time,
            final TriggeringPolicy triggeringPolicy, final RolloverStrategy rolloverStrategy,
            final String advertiseURI, final Layout<? extends Serializable> layout, final boolean writeHeader, final ByteBuffer buffer) {
        super(loggerContext, fileName, pattern, os, append, createOnDemand, size, time,
        		triggeringPolicy, rolloverStrategy, advertiseURI, layout, writeHeader, buffer);
    }


    /**
     * Returns a IndexedRollingFileManager.
     * @param fileName The file name.
     * @param pattern The pattern for rolling file.
     * @param append true if the file should be appended to.
     * @param bufferedIO true if data should be buffered.
     * @param policy The TriggeringPolicy.
     * @param strategy The RolloverStrategy.
     * @param advertiseURI the URI to use when advertising the file
     * @param layout The Layout.
     * @param bufferSize buffer size to use if bufferedIO is true
     * @param immediateFlush flush on every write or not
     * @param createOnDemand true if you want to lazy-create the file (a.k.a. on-demand.)
     * @param configuration The configuration.
     * @return A IndexedRollingFileManager.
     */
    public static IndexedRollingFileManager getFileManager(final String fileName, final String pattern, final boolean append,
            final boolean bufferedIO, final TriggeringPolicy policy, final RolloverStrategy strategy,
            final String advertiseURI, final Layout<? extends Serializable> layout, final int bufferSize,
            final boolean createOnDemand, final Configuration configuration) {
        String name = fileName == null ? pattern : fileName;
        return (IndexedRollingFileManager) getManager(name, new FactoryData(fileName, pattern, append,
            bufferedIO, policy, strategy, advertiseURI, layout, bufferSize, createOnDemand, configuration), factory);
    }

    public synchronized void rollover() {
    	super.rollover();
    }

    /**
     * Factory data.
     */
    private static class FactoryData extends ConfigurationFactoryData {
        private final String fileName;
        private final String pattern;
        private final boolean append;
        private final boolean bufferedIO;
        private final int bufferSize;
        private final boolean createOnDemand;
        private final TriggeringPolicy policy;
        private final RolloverStrategy strategy;
        private final String advertiseURI;
        private final Layout<? extends Serializable> layout;

        /**
         * Creates the data for the factory.
         * @param pattern The pattern.
         * @param append The append flag.
         * @param bufferedIO The bufferedIO flag.
         * @param advertiseURI
         * @param layout The Layout.
         * @param bufferSize the buffer size
         * @param immediateFlush flush on every write or not
         * @param createOnDemand true if you want to lazy-create the file (a.k.a. on-demand.)
         * @param configuration The configuration
         */
        public FactoryData(final String fileName, final String pattern, final boolean append, final boolean bufferedIO,
                final TriggeringPolicy policy, final RolloverStrategy strategy, final String advertiseURI,
                final Layout<? extends Serializable> layout, final int bufferSize,
                final boolean createOnDemand, final Configuration configuration) {
            super(configuration);
            this.fileName = fileName;
            this.pattern = pattern;
            this.append = append;
            this.bufferedIO = bufferedIO;
            this.bufferSize = bufferSize;
            this.policy = policy;
            this.strategy = strategy;
            this.advertiseURI = advertiseURI;
            this.layout = layout;
            this.createOnDemand = createOnDemand;
        }

        public TriggeringPolicy getTriggeringPolicy()
        {
            return this.policy;
        }

        public RolloverStrategy getRolloverStrategy()
        {
            return this.strategy;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append(super.toString());
            builder.append("[pattern=");
            builder.append(pattern);
            builder.append(", append=");
            builder.append(append);
            builder.append(", bufferedIO=");
            builder.append(bufferedIO);
            builder.append(", bufferSize=");
            builder.append(bufferSize);
            builder.append(", policy=");
            builder.append(policy);
            builder.append(", strategy=");
            builder.append(strategy);
            builder.append(", advertiseURI=");
            builder.append(advertiseURI);
            builder.append(", layout=");
            builder.append(layout);
            builder.append("]");
            return builder.toString();
        }
    }

    @Override
    public void updateData(final Object data)
    {
        final FactoryData factoryData = (FactoryData) data;
        setRolloverStrategy(factoryData.getRolloverStrategy());
        setTriggeringPolicy(factoryData.getTriggeringPolicy());
    }

    /**
     * Factory to create a IndexedRollingFileManager.
     */
    private static class IndexedRollingFileManagerFactory implements ManagerFactory<IndexedRollingFileManager, FactoryData> {

        /**
         * Creates a IndexedRollingFileManager.
         * @param name The name of the entity to manage.
         * @param data The data required to create the entity.
         * @return a IndexedRollingFileManager.
         */
        @Override
        public IndexedRollingFileManager createManager(final String name, final FactoryData data) {
            long size = 0;
            boolean writeHeader = !data.append;
            File file = null;
            if (data.fileName != null) {
                file = new File(data.fileName);
                // LOG4J2-1140: check writeHeader before creating the file
                writeHeader = !data.append || !file.exists();

                try {
                    FileUtils.makeParentDirs(file);
                    final boolean created = data.createOnDemand ? false : file.createNewFile();
                    LOGGER.trace("New file '{}' created = {}", name, created);
                } catch (final IOException ioe) {
                    LOGGER.error("Unable to create file " + name, ioe);
                    return null;
                }
                size = data.append ? file.length() : 0;
            }

            try {
                final int actualSize = data.bufferedIO ? data.bufferSize : Constants.ENCODER_BYTE_BUFFER_SIZE;
                final ByteBuffer buffer = ByteBuffer.wrap(new byte[actualSize]);
                final OutputStream os = data.createOnDemand  || data.fileName == null ? null :
                        new FileOutputStream(data.fileName, data.append);
                final long time = data.createOnDemand || file == null ?
                        System.currentTimeMillis() : file.lastModified(); // LOG4J2-531 create file first so time has valid value

                return new IndexedRollingFileManager(data.getLoggerContext(), data.fileName, data.pattern, os,
                        data.append, data.createOnDemand, size, time, data.policy, data.strategy, data.advertiseURI,
                        data.layout, writeHeader, buffer);
            } catch (final IOException ex) {
                LOGGER.error("IndexedRollingFileManager (" + name + ") " + ex, ex);
            }
            return null;
        }
    }


}
