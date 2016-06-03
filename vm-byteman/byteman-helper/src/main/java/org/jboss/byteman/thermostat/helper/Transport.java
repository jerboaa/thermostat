/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package org.jboss.byteman.thermostat.helper;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.byteman.thermostat.Properties;

/**
 * @author akashche
 */
public abstract class Transport implements Closeable {
    
    public static final String SEND_THRESHOLD_PROPERTY = Properties.PREFIX + "sendThreshold";
    public static final String LOSE_THRESHOLD_PROPERTY = Properties.PREFIX + "loseThreshold";
    // settings
    private final int sendThreshold;
    private final int loseThreshold;
    // state
    private AtomicBoolean sending = new AtomicBoolean(false);
    // cache
    private ArrayList<BytemanMetric> cache = new ArrayList<BytemanMetric>();
    private final Object cacheLock = new Object();
    private long lostCount = 0;
    // executor
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThermostatThreadFactory("thermostat"));

    /**
     * Constructor for inheritors
     *
     * @param sendThreshold min number of records to cache before sending
     * @param loseThreshold max number of packages to cache
     */
    protected Transport(int sendThreshold, int loseThreshold) {
        this.sendThreshold = sendThreshold;
        this.loseThreshold = loseThreshold;
    }

    /**
     * This method should transfer specified records to Thermostat
     * It will be called from the background thread and no more than from
     * a single thread simultaneously
     *
     * @param records records to transfer
     */
    protected abstract void transferToPeer(ArrayList<BytemanMetric> records);

    public void send(BytemanMetric rec) {
        synchronized (cacheLock) {
            if (null != rec) {
                if (cache.size() < loseThreshold) {
                    cache.add(rec);
                    if (cache.size() >= sendThreshold && !sending.get()) {
                        ArrayList<BytemanMetric> records = cache;
                        cache = new ArrayList<BytemanMetric>();
                        TransferTask task = new TransferTask(records);
                        executor.execute(task);
                    }
                } else {
                    lostCount += 1;
                }
            }
        }
    }

    /**
     * Sends the remaining cached records to Thermostat
     */
    @Override
    public void close() {
        synchronized (cacheLock) {
            if (!sending.get() && cache.size() > 0) {
                TransferTask task = new TransferTask(cache);
                task.run();
            }
        }
        // Shut down executor and wait (with a timeout) for it to finish
        executor.shutdown();
        try {
            executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Getter for number of lost records
     *
     * @return number of lost records
     */
    public long getLostCount() {
        return lostCount;
    }

    private class TransferTask implements Runnable {
        private final ArrayList<BytemanMetric> records;

        private TransferTask(ArrayList<BytemanMetric> records) {
            this.records = records;
        }

        @Override
        public void run() {
            sending.set(true);
            try {
                transferToPeer(records);
            } catch (Exception e) {
                System.err.println("ERROR: Thermostat helper transfer data error:");
                e.printStackTrace();
            } finally {
                sending.set(false);
            }
        }
    }

    private static class ThermostatThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final String prefix;

        /**
         * Constructor
         *
         * @param prefix thread name prefix
         */
        public ThermostatThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        /**
         * Creates new thread
         *
         * @param runnable thread runnable
         * @return daemon thread with <code>prefix-counter</code> name
         */
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName(prefix + "-" + counter.incrementAndGet());
            return thread;
        }
    }
}
