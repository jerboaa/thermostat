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

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.model.Pojo;

public class QueuedStorage implements Storage {
    
    private static final Logger logger = LoggingUtils.getLogger(QueuedStorage.class);
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 3;

    protected final Storage delegate;
    protected final ExecutorService executor;
    protected final ExecutorService fileExecutor;

    /*
     * Decorates PreparedStatement.execute() so that executions of writes
     * are queued.
     */
    class QueuedPreparedStatement<T extends Pojo> implements PreparedStatement<T> {
        
        private final PreparedStatement<T> delegate;
        
        private QueuedPreparedStatement(PreparedStatement<T> delegate) {
            this.delegate = Objects.requireNonNull(delegate);
        }

        @Override
        public void setBooleanList(int paramIndex, boolean[] paramValue) {
            delegate.setBooleanList(paramIndex, paramValue);
        }

        @Override
        public void setLongList(int paramIndex, long[] paramValue) {
            delegate.setLongList(paramIndex, paramValue);
        }

        @Override
        public void setIntList(int paramIndex, int[] paramValue) {
            delegate.setIntList(paramIndex, paramValue);
        }

        @Override
        public void setDouble(int paramIndex, double paramValue) {
            delegate.setDouble(paramIndex, paramValue);
        }

        @Override
        public void setDoubleList(int paramIndex, double[] paramValue) {
            delegate.setDoubleList(paramIndex, paramValue);
        }

        @Override
        public void setPojo(int paramIndex, Pojo paramValue) {
            delegate.setPojo(paramIndex, paramValue);
        }

        @Override
        public void setPojoList(int paramIndex, Pojo[] paramValue) {
            delegate.setPojoList(paramIndex, paramValue);
        }

        @Override
        public void setBoolean(int paramIndex, boolean paramValue) {
            delegate.setBoolean(paramIndex, paramValue);
        }

        @Override
        public void setLong(int paramIndex, long paramValue) {
            delegate.setLong(paramIndex, paramValue);
        }

        @Override
        public void setInt(int paramIndex, int paramValue) {
            delegate.setInt(paramIndex, paramValue);
        }

        @Override
        public void setString(int paramIndex, String paramValue) {
            delegate.setString(paramIndex, paramValue);
        }

        @Override
        public void setStringList(int paramIndex, String[] paramValue) {
            delegate.setStringList(paramIndex, paramValue);
        }

        @Override
        public int execute() throws StatementExecutionException {
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        // TODO log return code of delegate, time execution?
                        delegate.execute();
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
            return delegate.executeQuery();
        }

        @Override
        public ParsedStatement<T> getParsedStatement() {
            return delegate.getParsedStatement();
        }
        
    }
    
    /*
     * NOTE: We intentially use single-thread executor. All updates are put into
     * a queue, from which a single dispatch thread calls the underlying
     * storage. Using multiple dispatch threads could cause out-of-order issues,
     * e.g. a VM death being reported before its VM start, which could confuse
     * the heck out of clients.
     */
    public QueuedStorage(Storage delegate) {
        this(delegate, Executors.newSingleThreadExecutor(), Executors.newSingleThreadExecutor());
    }

    /*
     * This is here solely for use by tests.
     */
    QueuedStorage(Storage delegate, ExecutorService executor, ExecutorService fileExecutor) {
        this.delegate = delegate;
        this.executor = executor;
        this.fileExecutor = fileExecutor;
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
        return new QueuedPreparedStatement<>(decoratee);
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

