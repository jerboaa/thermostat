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

package com.redhat.thermostat.dev.populator.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.dev.populator.config.ConfigItem;
import com.redhat.thermostat.dev.populator.dependencies.SharedState;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.Countable;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.dao.impl.ThreadDaoImpl;
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.thread.model.ThreadSession;
import com.redhat.thermostat.thread.model.ThreadState;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VmDeadLockData;

public class ThreadPopulator extends BasePopulator {

    /**
     * Package-private and static for testing.
     */
    static final int NUM_SAMPLES = 10;

    public final String SESSION = "session";
    private final int NUM_THREADS = 3;
    private final long SEED = 5;
    Random generator = new Random(SEED);
    private static final String[] THREAD_NAMES = new String[] {
            "Spencer", "Sheldon", "Alice", "Bob", "Julie", "Phoebe", "Alpha", "Beta", "Gamma",
            "Theta", "Zeta",
    };
    static final String DEADLOCK_DESC_FORMAT = "" +
            "\"%s\" Id=%d WAITING on java.util.concurrent.locks.ReentrantLock$NonfairSync@52de95c7 owned by \"%s\" Id=%d\n" +
            "\tat sun.misc.Unsafe.park(Native Method)\n" +
            "\t-  waiting on java.util.concurrent.locks.ReentrantLock$NonfairSync@52de95c7\n" +
            "\tat java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\n" +
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\n" +
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\n" +
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\n" +
            "\tat java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\n" +
            "\tat java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\n" +
            "\tat com.redhat.thermostat.tests.DeadLock$Philosopher.run(DeadLock.java:57)\n" +
            "\t...\n\n" +
            "\tNumber of locked synchronizers = 1\n" +
            "\t- java.util.concurrent.locks.ReentrantLock$NonfairSync@441634c2\n" +
            "\n\n" +
            "\"%s\" Id=%d WAITING on java.util.concurrent.locks.ReentrantLock$NonfairSync@105ff84e owned by \"%s\" Id=%d\n" +
            "\tat sun.misc.Unsafe.park(Native Method)\n" +
            "\t-  waiting on java.util.concurrent.locks.ReentrantLock$NonfairSync@105ff84e\n" + 
            "\tat java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\n" + 
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\n" +
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\n" +
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\n" +
            "\tat java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\n" +
            "\tat java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\n" + 
            "\tat com.redhat.thermostat.tests.DeadLock$Philosopher.run(DeadLock.java:57)\n" +
            "\t...\n\n" + 
            "\tNumber of locked synchronizers = 1\n" +
            "\t- java.util.concurrent.locks.ReentrantLock$NonfairSync@52de95c7\n" +
            "\n\n" +
            "\"%s\" Id=%d WAITING on java.util.concurrent.locks.ReentrantLock$NonfairSync@441634c2 owned by \"%s\" Id=%d\n" +
            "\tat sun.misc.Unsafe.park(Native Method)\n" +
            "\t-  waiting on java.util.concurrent.locks.ReentrantLock$NonfairSync@441634c2\n" +
            "\tat java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\n" + 
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\n" +
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\n" + 
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\n" + 
            "\tat java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\n" +
            "\tat java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\n" +
            "\tat com.redhat.thermostat.tests.DeadLock$Philosopher.run(DeadLock.java:57)\n" + 
            "\t...\n\n" +
            "\tNumber of locked synchronizers = 1\n" +
            "\t- java.util.concurrent.locks.ReentrantLock$NonfairSync@105ff84e\n\n\n";
            
    
    private ThreadDaoCountable dao;

    public ThreadPopulator() {
        this(null);
    }

    ThreadPopulator(ThreadDaoCountable dao) {
        this.dao = dao;
    }

    // Testing hook
    private void createDao(Storage storage) {
        if (dao == null) {
            dao = new ThreadDaoCountable(storage);
        }
    }

    @Override
    public SharedState addPojos(Storage storage, ConfigItem item, SharedState relState) {
        createDao(storage);
        // creates records per agent *and* vm
        List<String> agentIds = relState.getProcessedRecordsFor("agentId").getAll();
        List<String> vmIds = relState.getProcessedRecordsFor("vmId").getAll();
        int perVmNumber = item.getNumber();
        int totalCount = perVmNumber * agentIds.size() * vmIds.size();
        int currCount = 0;
        long countBefore = dao.getCount();
        System.out.println("Populating "+ totalCount  + " " + item.getName() + " records");
        long currTime = System.currentTimeMillis();
        for (String agentId: agentIds) {
            for (String vmId: vmIds) {
                for (int i = 0; i < perVmNumber; i++) {
                    ThreadSummary summary = new ThreadSummary();
                    summary.setAgentId(agentId);
                    summary.setVmId(vmId);
                    int liveThreads = generator.nextInt(NUM_THREADS);
                    summary.setCurrentLiveThreads(liveThreads);
                    int daemonThreads = NUM_THREADS - liveThreads;
                    summary.setCurrentDaemonThreads(daemonThreads);
                    long timeStamp = currTime + i * 1000;
                    summary.setTimeStamp(timeStamp);
                    dao.saveSummary(summary);

                    ThreadSession session = new ThreadSession();
                    session.setSession(SESSION);
                    session.setTimeStamp(timeStamp);
                    session.setAgentId(agentId);
                    session.setVmId(vmId);
                    dao.saveSession(session);

                    final Thread.State[] states = Thread.State.values();
                    String state = states[generator.nextInt(states.length)].toString();
                    String name = THREAD_NAMES[generator.nextInt(THREAD_NAMES.length)];

                    for(int j = 0; j < NUM_SAMPLES; j++) {
                        ThreadState threadState = new ThreadState();
                        threadState.setTimeStamp(currTime + j * 1000);
                        threadState.setVmId(vmId);
                        threadState.setAgentId(agentId);
                        threadState.setState(state);
                        threadState.setId(i);
                        threadState.setName(name);
                        threadState.setSession(SESSION);
                        dao.addThreadState(threadState);
                    }

                    ThreadHarvestingStatus status = new ThreadHarvestingStatus(agentId);
                    status.setVmId(vmId);
                    status.setTimeStamp(timeStamp);
                    status.setHarvesting(generator.nextBoolean());
                    dao.saveHarvestingStatus(status);

                    VmDeadLockData data = new VmDeadLockData();
                    data.setAgentId(agentId);
                    data.setVmId(vmId);
                    data.setTimeStamp(currTime + i * 10);
                    data.setDeadLockDescription(getDeadlockedDescription(currCount));
                    dao.saveDeadLockStatus(data);
                    currCount++;
                    reportProgress(item, currCount);
                }
            }
        }
        doWaitUntilCount(dao, countBefore + totalCount);
        return relState;
    }

    private String getDeadlockedDescription(int currCount) {
        return String.format(DEADLOCK_DESC_FORMAT, getFormatStringArgs(currCount));
    }

    Object[] getFormatStringArgs(int currCount) {
        List<Pair<String, Integer>> nameIdMapping = new ArrayList<>();
        int[] randomIds = getIdValues(currCount, NUM_THREADS);
        int randomEntry = getRandomInt(THREAD_NAMES.length);
        for (int i = 0, idx; i < NUM_THREADS; i++) {
            idx = (randomEntry + i) % THREAD_NAMES.length;
            String name = THREAD_NAMES[idx];
            int tid = randomIds[i];
            nameIdMapping.add(new Pair<>(name, tid));
        }
        Object[] formatArgs = new Object[NUM_THREADS * 2 * 2];
        formatArgs[0] = nameIdMapping.get(0).getFirst();
        formatArgs[1] = nameIdMapping.get(0).getSecond();
        for (int i = 2, j = 1; i < formatArgs.length - 2; i+= 4, j++) {
            formatArgs[i] = nameIdMapping.get(j).getFirst();
            formatArgs[i + 1] = nameIdMapping.get(j).getSecond();
            formatArgs[i + 2] = nameIdMapping.get(j).getFirst();
            formatArgs[i + 3] = nameIdMapping.get(j).getSecond();
        }
        formatArgs[formatArgs.length - 2] = nameIdMapping.get(0).getFirst();
        formatArgs[formatArgs.length - 1] = nameIdMapping.get(0).getSecond();
        return formatArgs;
    }

    private int getRandomInt(int len) {
        return (int)(Math.random() * len);
    }

    private int[] getIdValues(int currCount, int length) {
        int[] values = new int[length];
        for (int i = 0; i < values.length; i++) {
            values[i] = currCount + i;
        }
        return values;
    }

    @Override
    public String getHandledCollection() {
        return ThreadDao.THREAD_HARVESTING_STATUS.getName();
    }

    static class ThreadDaoCountable extends ThreadDaoImpl implements Countable {

        private final Category<AggregateCount> aggregateCategory;
        private final Storage storage;

        public ThreadDaoCountable(Storage storage) {
            super(storage);
            CategoryAdapter<VmDeadLockData, AggregateCount> adapter = new CategoryAdapter<>(DEADLOCK_INFO);
            aggregateCategory = adapter.getAdapted(AggregateCount.class);
            storage.registerCategory(aggregateCategory);
            this.storage = storage;
        }

        @Override
        public long getCount() {
            String descriptor = "QUERY-COUNT " + DEADLOCK_INFO.getName();
            try {
                PreparedStatement<AggregateCount> stmt = storage.prepareStatement(new StatementDescriptor<>(aggregateCategory, descriptor));
                Cursor<AggregateCount> cursor = stmt.executeQuery();
                AggregateCount count = cursor.next();
                return count.getCount();
            } catch (DescriptorParsingException | StatementExecutionException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
