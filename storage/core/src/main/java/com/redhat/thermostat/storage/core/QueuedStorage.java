/*
 * Copyright 2012-2014 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */


package com.redhat.thermostat.storage.core;

import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.perflog.PerformanceLogFormatter;
import com.redhat.thermostat.shared.perflog.PerformanceLogFormatter.LogTag;
import com.redhat.thermostat.storage.internal.CountingDecorator;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.Expression;

public class QueuedStorage implements Storage {
    
    private static final Logger logger = LoggingUtils.getLogger(QueuedStorage.class);
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 3;

    /*
     * True if and only if the delegate is a backing storage (such as mongodb)
     * and it is used as a backing storage for proxied storage (such as web).
     */
    protected final boolean isBackingStorageInProxy;
    /*
     * True if and only if statements should get timed and logged.
     */
    protected final boolean isTimedStatements;
    // performance logger. may be null
    protected final PerformanceLogFormatter perfLogFormatter;
    protected final Storage delegate;
    protected final ExecutorService executor;
    protected final ExecutorService fileExecutor;
    
    private abstract static class SettingDecorator<T extends Pojo> implements PreparedStatementSetter, PreparedStatement<T> {
        
        protected final PreparedStatement<T> stmtDelegate;
        
        private SettingDecorator(PreparedStatement<T> decoratee) {
            this.stmtDelegate = decoratee;
        }
        
        @Override
        public void setBooleanList(int paramIndex, boolean[] paramValue) {
            stmtDelegate.setBooleanList(paramIndex, paramValue);
        }

        @Override
        public void setLongList(int paramIndex, long[] paramValue) {
            stmtDelegate.setLongList(paramIndex, paramValue);
        }

        @Override
        public void setIntList(int paramIndex, int[] paramValue) {
            stmtDelegate.setIntList(paramIndex, paramValue);
        }

        @Override
        public void setDouble(int paramIndex, double paramValue) {
            stmtDelegate.setDouble(paramIndex, paramValue);
        }

        @Override
        public void setDoubleList(int paramIndex, double[] paramValue) {
            stmtDelegate.setDoubleList(paramIndex, paramValue);
        }

        @Override
        public void setPojo(int paramIndex, Pojo paramValue) {
            stmtDelegate.setPojo(paramIndex, paramValue);
        }

        @Override
        public void setPojoList(int paramIndex, Pojo[] paramValue) {
            stmtDelegate.setPojoList(paramIndex, paramValue);
        }

        @Override
        public void setBoolean(int paramIndex, boolean paramValue) {
            stmtDelegate.setBoolean(paramIndex, paramValue);
        }

        @Override
        public void setLong(int paramIndex, long paramValue) {
            stmtDelegate.setLong(paramIndex, paramValue);
        }

        @Override
        public void setInt(int paramIndex, int paramValue) {
            stmtDelegate.setInt(paramIndex, paramValue);
        }

        @Override
        public void setString(int paramIndex, String paramValue) {
            stmtDelegate.setString(paramIndex, paramValue);
        }

        @Override
        public void setStringList(int paramIndex, String[] paramValue) {
            stmtDelegate.setStringList(paramIndex, paramValue);
        }
        
    }
    
    class QueuedStatementDecorator<T extends Pojo> implements Statement<T> {
        
        private final Statement<T> stmt;
        
        private QueuedStatementDecorator(Statement<T> delegate) {
            this.stmt = delegate;
        }

        @Override
        public Statement<T> getRawDuplicate() {
            return stmt.getRawDuplicate();
        }
    }
    
    abstract class QueuedQueryDecorator<T extends Pojo> extends QueuedStatementDecorator<T> implements Query<T> {

        protected final Query<T> query;
        private QueuedQueryDecorator(Query<T> delegate) {
            super(delegate);
            this.query = delegate;
        }
        
        @Override
        public void where(Expression expr) {
            query.where(expr);
        }

        @Override
        public void sort(Key<?> key,
                com.redhat.thermostat.storage.core.Query.SortDirection direction) {
            query.sort(key, direction);
        }

        @Override
        public void limit(int n) {
            query.limit(n);
        }

        public abstract Cursor<T> execute();

        @Override
        public Expression getWhereExpression() {
            return query.getWhereExpression();
        }
        
    }
    
    class QueuedWrite<T extends Pojo> extends QueuedStatementDecorator<T> implements DataModifyingStatement<T> {

        protected final DataModifyingStatement<T> write;
        
        private QueuedWrite(DataModifyingStatement<T> delegate) {
            super(delegate);
            this.write = delegate;
        }
        
        @Override
        public int apply() {
            executor.execute(new Runnable() {
                
                @Override
                public void run() {
                    doApply();
                }
            });
            return DataModifyingStatement.DEFAULT_STATUS_SUCCESS;
        }
        
        // Allows for timed decoration
        protected int doApply() {
            return write.apply();
        }
        
    }
    
    abstract class QueuedParsedStatementDecorator<T extends Pojo> implements ParsedStatement<T> {

        protected final ParsedStatement<T> parsedDelegate;
        
        private QueuedParsedStatementDecorator(ParsedStatement<T> delegate) {
            this.parsedDelegate = delegate;
        }
        
        @Override
        public abstract Statement<T> patchStatement(PreparedParameter[] params) throws IllegalPatchException;

        @Override
        public int getNumParams() {
            return parsedDelegate.getNumParams();
        }
        
    }
    
    class QueuedParsedStatement<T extends Pojo> extends QueuedParsedStatementDecorator<T> {
        
        private QueuedParsedStatement(ParsedStatement<T> delegate) {
            super(delegate);
        }

        @Override
        public Statement<T> patchStatement(PreparedParameter[] params)
                throws IllegalPatchException {
            Statement<T> stmt = parsedDelegate.patchStatement(params);
            if (stmt instanceof DataModifyingStatement) {
                DataModifyingStatement<T> target = (DataModifyingStatement<T>)stmt;
                return new QueuedWrite<>(target);
            } else if (stmt instanceof Query) {
                // Queries are not queued
                return stmt;
            } else {
                // We only have two statement types: reads and writes.
                throw new IllegalStateException("Should not reach here"); 
            }
        }
        
    }
    
    static class TimedStatement {
        private static final String DB_READ_PREFIX = "DB_READ";
        private static final String DB_WRITE_PREFIX = "DB_WRITE";
        private static final String DB_WRITE_FORMAT = DB_WRITE_PREFIX + "(%s) %s";
        private static final String DB_READ_FORMAT = DB_READ_PREFIX + " %s";
    }
    
    class TimedQuery<T extends Pojo> extends QueuedQueryDecorator<T> {

        private final Query<T> queryDelegate;
        private final PerformanceLogFormatter perfLogFormatter;
        private final String descriptor;
        
        private TimedQuery(Query<T> delegate, PerformanceLogFormatter perfLogFormatter, String descriptor) {
            super(delegate);
            this.queryDelegate = delegate;
            this.perfLogFormatter = perfLogFormatter;
            this.descriptor = descriptor;
        }

        @Override
        public Cursor<T> execute() {
            long start = System.nanoTime();
            Cursor<T> result = queryDelegate.execute();
            long end = System.nanoTime();
            String msg = String.format(TimedStatement.DB_READ_FORMAT, descriptor);
            logger.log(LoggingUtils.PERFLOG, perfLogFormatter.format(LogTag.STORAGE_BACKING_PROXIED, msg, (end - start)));
            return result;
        }
        
    }
    
    class TimedWrite<T extends Pojo> extends QueuedWrite<T> implements DataModifyingStatement<T> {

        private final QueuedWrite<T> write;
        private final PerformanceLogFormatter perfLogFormatter;
        private final String descriptor;
        
        private TimedWrite(QueuedWrite<T> delegate, PerformanceLogFormatter perfLogFormatter, String descriptor) {
            super(delegate);
            this.write = delegate;
            this.perfLogFormatter = perfLogFormatter;
            this.descriptor = descriptor;
        }

        @Override
        public int apply() {
            executor.execute(new Runnable() {
                
                @Override
                public void run() {
                    long start = System.nanoTime();
                    int retval = write.doApply();
                    long end = System.nanoTime();
                    String msg = String.format(TimedStatement.DB_WRITE_FORMAT, retval, descriptor);
                    logger.log(LoggingUtils.PERFLOG, perfLogFormatter.format(LogTag.STORAGE_BACKING_PROXIED, msg, (end - start)));
                }
                
            });
            return DataModifyingStatement.DEFAULT_STATUS_SUCCESS;
        }
        
    }
    
    class TimedParsedStatement<T extends Pojo> extends QueuedParsedStatementDecorator<T> {

        private final PerformanceLogFormatter perfLogFormatter;
        private final String descriptor;
        
        private TimedParsedStatement(ParsedStatement<T> delegate, PerformanceLogFormatter perfLogFormatter, String descriptor) {
            super(delegate);
            this.perfLogFormatter = perfLogFormatter;
            this.descriptor = descriptor;
        }
        
        @Override
        public Statement<T> patchStatement(PreparedParameter[] params)
                throws IllegalPatchException {
            Statement<T> stmt = parsedDelegate.patchStatement(params);
            if (stmt instanceof DataModifyingStatement) {
                QueuedWrite<T> target = (QueuedWrite<T>)stmt;
                return new TimedWrite<>(target, perfLogFormatter, descriptor);
            } else if (stmt instanceof Query) {
                Query<T> target = (Query<T>)stmt;
                return new TimedQuery<>(target, perfLogFormatter, descriptor);
            } else {
                // We only have two statement types: reads and writes.
                throw new IllegalStateException("Should not reach here");
            }
        }
        
    }
    
    /*
     * Decorates statement executions so that they get timed and results logged
     * via the provided PerformanceLogger.
     */
    class TimedPreparedStatement<T extends Pojo> extends SettingDecorator<T> implements PreparedStatement<T> {
        
        private static final String DB_READ_PREFIX = "DB_READ";
        private static final String DB_WRITE_PREFIX = "DB_WRITE";
        private static final String DB_WRITE_FORMAT = DB_WRITE_PREFIX + "(%s) %s";
        private static final String DB_READ_FORMAT = DB_READ_PREFIX + " %s";
        private final PerformanceLogFormatter perfLogFormatter;
        private final String descriptor;
        
        private TimedPreparedStatement(QueuedPreparedStatement<T> decoratee, PerformanceLogFormatter perfLogFormatter, String descriptor) {
            super(decoratee);
            this.perfLogFormatter = perfLogFormatter;
            this.descriptor = descriptor;
        }

        @Override
        public Cursor<T> executeQuery() throws StatementExecutionException {
            long start = System.nanoTime();
            Cursor<T> result = stmtDelegate.executeQuery();
            long end = System.nanoTime();
            String msg = String.format(DB_READ_FORMAT, descriptor);
            logger.log(LoggingUtils.PERFLOG, perfLogFormatter.format(LogTag.STORAGE_FRONT_END, msg, (end - start)));
            return result;
        }
        
        @Override
        public int execute() throws StatementExecutionException {
            executor.execute(new Runnable() {
                
                @Override
                public void run() {
                    try {
                        long start = System.nanoTime();
                        QueuedPreparedStatement<T> d = (QueuedPreparedStatement<T>)stmtDelegate;
                        int retval = d.doExecute();
                        long end = System.nanoTime();
                        String msg = String.format(DB_WRITE_FORMAT, retval, descriptor);
                        logger.log(LoggingUtils.PERFLOG, perfLogFormatter.format(LogTag.STORAGE_FRONT_END, msg, (end - start)));
                    } catch (StatementExecutionException e) {
                        // There isn't much we can do in case of invalid
                        // patch or the likes. Log it and move on.
                        logger.log(Level.WARNING, "Failed to execute statement", e);
                    }
                }
            });
            return DataModifyingStatement.DEFAULT_STATUS_SUCCESS;
        }

        /*
         * For proxied prepared statements we never actually call the underlying
         * execute() and executeQuery() methods for performance reasons. We
         * simply use previously prepared statements and use the parsed result
         * of them. Thus, in order to make backing storages queued too, we
         * need to decorate them this way.
         */
        @Override
        public ParsedStatement<T> getParsedStatement() {
            if (isBackingStorageInProxy) {
                ParsedStatement<T> target = stmtDelegate.getParsedStatement();
                return new TimedParsedStatement<>(target, perfLogFormatter, descriptor);
            } else {
                return stmtDelegate.getParsedStatement();
            }
        }
        
    }

    /*
     * Decorates PreparedStatement.execute() so that executions of writes
     * are queued.
     */
    class QueuedPreparedStatement<T extends Pojo> extends SettingDecorator<T> implements PreparedStatement<T> {
        
        private QueuedPreparedStatement(PreparedStatement<T> delegate) {
            super(Objects.requireNonNull(delegate));
        }

        @Override
        public int execute() throws StatementExecutionException {
            if (isBackingStorageInProxy) {
                String msg = "Did not expect to get called for backing storage in proxied setup.";
                throw new AssertionError(msg);
            }
            executor.execute(new Runnable() {
                
                @Override
                public void run() {
                    try {
                        doExecute();
                    } catch (StatementExecutionException e) {
                        // There isn't much we can do in case of invalid
                        // patch or the likes. Log it and move on.
                        logger.log(Level.WARNING, "Failed to execute statement", e);
                    }
                }
            });
            return DataModifyingStatement.DEFAULT_STATUS_SUCCESS;
        }

        @Override
        public Cursor<T> executeQuery() throws StatementExecutionException {
            if (isBackingStorageInProxy) {
                String msg = "Did not expect to get called for backing storage in proxied setup.";
                throw new AssertionError(msg);
            }
            return stmtDelegate.executeQuery();
        }
        
        /*
         * For proxied prepared statements we never actually call the underlying
         * execute() and executeQuery() methods for performance reasons. We
         * simply use previously prepared statements and use the parsed result
         * of them. Thus, in order to make backing storages queued too, we
         * need to decorate them this way.
         */
        @Override
        public ParsedStatement<T> getParsedStatement() {
            if (isBackingStorageInProxy) {
                ParsedStatement<T> target = stmtDelegate.getParsedStatement();
                return new QueuedParsedStatement<>(target);
            } else {
                return stmtDelegate.getParsedStatement();
            }
        }
        
        // This is to allow for proper decoration of it if timing is turned on.
        private int doExecute() throws StatementExecutionException {
            return stmtDelegate.execute();
        }
        
    }
    
    public QueuedStorage(Storage delegate) {
        this(delegate, null);
    }
    
    /*
     * NOTE: We intentionally use single-thread executor. All updates are put into
     * a queue, from which a single dispatch thread calls the underlying
     * storage. Using multiple dispatch threads could cause out-of-order issues,
     * e.g. a VM death being reported before its VM start, which could confuse
     * the heck out of clients.
     */
    public QueuedStorage(Storage delegate, PerformanceLogFormatter perfLogFormatter) {
        this(delegate, Executors.newSingleThreadExecutor(), Executors.newSingleThreadExecutor(), perfLogFormatter);
    }

    QueuedStorage(Storage delegate, ExecutorService executor, ExecutorService fileExecutor) {
        this(delegate, executor, fileExecutor, null);
    }
    
    QueuedStorage(Storage delegate, ExecutorService executor, ExecutorService fileExecutor, PerformanceLogFormatter perfLogFormatter) {
        this.delegate = delegate;
        this.fileExecutor = fileExecutor;
        this.isBackingStorageInProxy = !(delegate instanceof SecureStorage) && Boolean.getBoolean(Constants.IS_PROXIED_STORAGE);
        this.isTimedStatements = LoggingUtils.getEffectiveLogLevel(logger).intValue() <= LoggingUtils.PERFLOG.intValue();
        // set up queue counting executor if so requested
        if (isTimedStatements) {
            LogTag logTag = (isBackingStorageInProxy ? LogTag.STORAGE_BACKING_PROXIED : LogTag.STORAGE_FRONT_END);
            this.executor = new CountingDecorator(executor, perfLogFormatter, logTag);
        } else {
            this.executor = executor;
        }
        this.perfLogFormatter = perfLogFormatter;
    }

    ExecutorService getExecutor() {
        return executor;
    }

    ExecutorService getFileExecutor() {
        return fileExecutor;
    }

    @Override
    public void purge(final String agentId) {

        executor.execute(new Runnable() {
            
            @Override
            public void run() {
                delegate.purge(agentId);
            }

        });

    }

    @Override
    public void saveFile(final String filename, final InputStream data) {

        fileExecutor.execute(new Runnable() {
            
            @Override
            public void run() {
                delegate.saveFile(filename, data);
            }

        });

    }

    @Override
    public InputStream loadFile(String filename) {
        return delegate.loadFile(filename);
    }

    @Override
    public void registerCategory(final Category<?> category) {
        delegate.registerCategory(category);
    }
    
    @Override
    public <T extends Pojo> PreparedStatement<T> prepareStatement(final StatementDescriptor<T> desc)
            throws DescriptorParsingException {
        PreparedStatement<T> decoratee = delegate.prepareStatement(desc);
        QueuedPreparedStatement<T> queuedPreparedStatement = new QueuedPreparedStatement<>(decoratee);
        return decorateWithTimingLoggerIfNecessary(queuedPreparedStatement, desc);
    }
    
    private <T extends Pojo> PreparedStatement<T> decorateWithTimingLoggerIfNecessary(QueuedPreparedStatement<T> decoratee, StatementDescriptor<T> desc) {
        if (isTimedStatements) {
            return new TimedPreparedStatement<>(decoratee, this.perfLogFormatter, desc.getDescriptor());
        }
        return decoratee;
    }
    
    @Override
    public Connection getConnection() {
        return delegate.getConnection();
    }

    @Override
    public void shutdown() {
        /*
         * First shut down executors. This may trigger some pushes to the
         * storage implementation (a.k.a. delegate). Hence, this should get
         * shut down last as this closes the connection etc.
         */
        try {
            executor.shutdown();
            executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            // Fall through. 
        }
        try {
            fileExecutor.shutdown();
            fileExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            // Fall through. 
        }
        delegate.shutdown();
    }

}

