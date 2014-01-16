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

package com.redhat.thermostat.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.host.cpu.common.CpuStatDAO;
import com.redhat.thermostat.host.cpu.common.model.CpuStat;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.shared.config.internal.SSLConfigurationImpl;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.BackingStorage;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.Query.SortDirection;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.mongodb.internal.MongoStorage;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;
import com.redhat.thermostat.vm.classstat.common.VmClassStatDAO;
import com.redhat.thermostat.vm.classstat.common.model.VmClassStat;
import com.redhat.thermostat.vm.cpu.common.VmCpuStatDAO;
import com.redhat.thermostat.vm.cpu.common.model.VmCpuStat;

/*
 * This test class starts up a mongod instance and tests if thermostat
 * queries with expressions return what they supposed to return. 
 * 
 * Tests should make their own connection to storage, probably by making use of
 * one of the getAndConnectStorage() method variants.
 * 
 * Because the storage instance is shared among all of the tests, it is
 * necessary to take precautions to avoid introducing data dependencies
 * between tests.  Such precautions could include: using a different
 * category (ie mongod collection) than any other existing test; setting
 * a unique agent-id for all data written and then deleting the data
 * at the end of the test; <insert other clever idea here>.
 * 
 */
public class MongoQueriesTest extends IntegrationTest {

    private static class CountdownConnectionListener implements ConnectionListener {

        private final ConnectionStatus target;
        private final CountDownLatch latch;

        private CountdownConnectionListener(ConnectionStatus target, CountDownLatch latch) {
            this.target = target;
            this.latch = latch;
        }

        @Override
        public void changed(ConnectionStatus newStatus) {
            assertEquals(target, newStatus);
            latch.countDown();
        }
    }

    private static final double EQUALS_DELTA = 0.00000000000001;
    private static final String VM_ID1 = "vmId1";
    private static final String VM_ID2 = "vmId2";
    private static final String VM_ID3 = "vmId3";

    private ExpressionFactory factory = new ExpressionFactory();

    @BeforeClass
    public static void setUpOnce() throws Exception {
        startStorage();

        addCpuData(4);
    }

    @AfterClass
    public static void tearDownOnce() throws Exception {
        deleteCpuData();

        stopStorage();
    }

    /*
     * Make a connection to mongo storage (returning the Storage object). Before
     * initiating the connection, add the ConnectionListener to Storage.
     */
    private static BackingStorage getAndConnectStorage(ConnectionListener listener) {
        final String url = "mongodb://127.0.0.1:27518";
        StorageCredentials creds = new StorageCredentials() {

            @Override
            public String getUsername() {
                return null;
            }

            @Override
            public char[] getPassword() {
                return null;
            }
            
        };
        SSLConfiguration sslConf = new SSLConfigurationImpl(new CommonPaths() {

            @Override
            public File getSystemThermostatHome() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getUserThermostatHome() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getSystemPluginRoot() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getSystemLibRoot() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getSystemNativeLibsRoot() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getSystemBinRoot() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getSystemConfigurationDirectory() throws InvalidConfigurationException { 
                return null;
            }

            @Override
            public File getUserConfigurationDirectory() throws InvalidConfigurationException {
                return new File("/tmp");
            }

            @Override
            public File getUserPersistentDataDirectory() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getUserRuntimeDataDirectory() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getUserLogDirectory() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getUserCacheDirectory() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getUserPluginRoot() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getUserStorageDirectory() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getSystemStorageConfigurationFile() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getUserStorageConfigurationFile() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getUserStorageLogFile() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getUserStoragePidFile() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getSystemAgentConfigurationFile() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getUserAgentConfigurationFile() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getSystemAgentAuthConfigFile() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getUserAgentAuthConfigFile() throws InvalidConfigurationException {
                return null;
            }

            @Override
            public File getUserClientConfigurationFile() throws InvalidConfigurationException { 
                return null;
            }

            @Override
            public File getUserHistoryFile() throws InvalidConfigurationException {
                return null;
            }
            
        });
        BackingStorage storage = new MongoStorage(url, creds, sslConf);
        if (listener != null) {
            storage.getConnection().addListener(listener);
        }
        storage.getConnection().connect();
        return storage;
    }

    private static void addCpuData(int numberOfItems) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage storage = getAndConnectStorage(listener);
        latch.await();
        storage.getConnection().removeListener(listener);
        
        storage.registerCategory(CpuStatDAO.cpuStatCategory);

        for (int i = 0; i < numberOfItems; i++) {
            CpuStat pojo = new CpuStat("test-agent-id", i, new double[] {i, i*2});
            Add<CpuStat> add = storage.createAdd(CpuStatDAO.cpuStatCategory);
            add.set(Key.AGENT_ID.getName(), pojo.getAgentId());
            add.set(CpuStatDAO.cpuLoadKey.getName(), pojo.getPerProcessorUsage());
            add.set(Key.TIMESTAMP.getName(), pojo.getTimeStamp());
            add.apply();
        }

        storage.getConnection().disconnect();
    }

    private static void deleteCpuData() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        Storage storage = getAndConnectStorage(listener);
        latch.await();
        storage.getConnection().removeListener(listener);
        storage.registerCategory(CpuStatDAO.cpuStatCategory);

        storage.purge("test-agent-id");

        storage.getConnection().disconnect();
    }

    private void executeAndVerifyQuery(Query<CpuStat> query, List<Long> expectedTimestamps) {
        Cursor<CpuStat> cursor = query.execute();

        for (Long time : expectedTimestamps) {
            assertTrue(cursor.hasNext());
            CpuStat pojo = cursor.next();
            assertEquals("test-agent-id", pojo.getAgentId());
            assertEquals(time.longValue(), pojo.getTimeStamp());
            double[] data = pojo.getPerProcessorUsage();
            assertEquals(time, data[0], EQUALS_DELTA);
            assertEquals(time*2, data[1], EQUALS_DELTA);
        }
        assertFalse(cursor.hasNext());
    }

    @Test
    public void testMongoAdd() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        mongoStorage.registerCategory(VmClassStatDAO.vmClassStatsCategory);
        
        Add<VmClassStat> add = mongoStorage.createAdd(VmClassStatDAO.vmClassStatsCategory);
        VmClassStat pojo = new VmClassStat();
        pojo.setAgentId("fluff");
        pojo.setLoadedClasses(12345);
        pojo.setTimeStamp(42);
        pojo.setVmId(VM_ID1);
        addVmClassStat(add, pojo);
        
        // Add another couple of entries
        add = mongoStorage.createAdd(VmClassStatDAO.vmClassStatsCategory);
        pojo = new VmClassStat();
        pojo.setAgentId("fluff");
        pojo.setLoadedClasses(67890);
        pojo.setTimeStamp(42);
        pojo.setVmId(VM_ID2);
        addVmClassStat(add, pojo);
        
        add = mongoStorage.createAdd(VmClassStatDAO.vmClassStatsCategory);
        pojo = new VmClassStat();
        pojo.setAgentId("fluff");
        pojo.setLoadedClasses(34567);
        pojo.setTimeStamp(42);
        pojo.setVmId(VM_ID3);
        addVmClassStat(add, pojo);

        mongoStorage.getConnection().disconnect();
    }

    private void addVmClassStat(Add<VmClassStat> add, VmClassStat pojo) {
        add.set(Key.AGENT_ID.getName(), pojo.getAgentId());
        add.set(Key.VM_ID.getName(), pojo.getVmId());
        add.set(Key.TIMESTAMP.getName(), pojo.getTimeStamp());
        add.set(VmClassStatDAO.loadedClassesKey.getName(), pojo.getLoadedClasses());
        add.apply();
    }

    @Test
    public void canQueryNoWhere() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        mongoStorage.registerCategory(CpuStatDAO.cpuStatCategory);
        
        Query<CpuStat> query = mongoStorage.createQuery(CpuStatDAO.cpuStatCategory);
        query.sort(Key.TIMESTAMP, SortDirection.ASCENDING);

        executeAndVerifyQuery(query, Arrays.asList(0l, 1l, 2l, 3l));

        mongoStorage.getConnection().disconnect();
    }
    
    @Test
    public void canQueryEqualTo() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        mongoStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        Query<CpuStat> query = mongoStorage.createQuery(CpuStatDAO.cpuStatCategory);
        Expression expr = factory.equalTo(Key.TIMESTAMP, 2l);
        query.where(expr);
        query.sort(Key.TIMESTAMP, SortDirection.ASCENDING);

        executeAndVerifyQuery(query, Arrays.asList(2l));

        mongoStorage.getConnection().disconnect();
    }
    
    @Test
    public void canQueryNotEqualTo() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        mongoStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        Query<CpuStat> query = mongoStorage.createQuery(CpuStatDAO.cpuStatCategory);
        Expression expr = factory.notEqualTo(Key.TIMESTAMP, 2l);
        query.where(expr);
        query.sort(Key.TIMESTAMP, SortDirection.ASCENDING);

        executeAndVerifyQuery(query, Arrays.asList(0l, 1l, 3l));

        mongoStorage.getConnection().disconnect();
    }
    
    @Test
    public void canQueryGreaterThan() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        mongoStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        Query<CpuStat> query = mongoStorage.createQuery(CpuStatDAO.cpuStatCategory);
        Expression expr = factory.greaterThan(Key.TIMESTAMP, 2l);
        query.where(expr);
        query.sort(Key.TIMESTAMP, SortDirection.ASCENDING);

        executeAndVerifyQuery(query, Arrays.asList(3l));

        mongoStorage.getConnection().disconnect();
    }
    
    @Test
    public void canQueryGreaterThanOrEqualTo() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        mongoStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        Query<CpuStat> query = mongoStorage.createQuery(CpuStatDAO.cpuStatCategory);
        Expression expr = factory.greaterThanOrEqualTo(Key.TIMESTAMP, 2l);
        query.where(expr);
        query.sort(Key.TIMESTAMP, SortDirection.ASCENDING);

        executeAndVerifyQuery(query, Arrays.asList(2l, 3l));

        mongoStorage.getConnection().disconnect();
    }
    
    @Test
    public void canQueryLessThan() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        mongoStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        Query<CpuStat> query = mongoStorage.createQuery(CpuStatDAO.cpuStatCategory);
        Expression expr = factory.lessThan(Key.TIMESTAMP, 2l);
        query.where(expr);
        query.sort(Key.TIMESTAMP, SortDirection.ASCENDING);

        executeAndVerifyQuery(query, Arrays.asList(0l, 1l));

        mongoStorage.getConnection().disconnect();
    }
    
    @Test
    public void canQueryLessThanOrEqualTo() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        mongoStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        Query<CpuStat> query = mongoStorage.createQuery(CpuStatDAO.cpuStatCategory);
        Expression expr = factory.lessThanOrEqualTo(Key.TIMESTAMP, 2l);
        query.where(expr);
        query.sort(Key.TIMESTAMP, SortDirection.ASCENDING);

        executeAndVerifyQuery(query, Arrays.asList(0l, 1l, 2l));

        mongoStorage.getConnection().disconnect();
    }
    
    @Test
    public void canQueryIn() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        mongoStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        List<Long> times = Arrays.asList(0l, 2l);
        Query<CpuStat> query = mongoStorage.createQuery(CpuStatDAO.cpuStatCategory);
        Expression expr = factory.in(Key.TIMESTAMP, new HashSet<>(times), Long.class);
        query.where(expr);
        query.sort(Key.TIMESTAMP, SortDirection.ASCENDING);

        executeAndVerifyQuery(query, times);

        mongoStorage.getConnection().disconnect();
    }
    
    @Test
    public void canQueryNotIn() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        mongoStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        Query<CpuStat> query = mongoStorage.createQuery(CpuStatDAO.cpuStatCategory);
        Expression expr = factory.notIn(Key.TIMESTAMP, new HashSet<>(Arrays.asList(0l, 2l)), Long.class);
        query.where(expr);
        query.sort(Key.TIMESTAMP, SortDirection.ASCENDING);

        executeAndVerifyQuery(query, Arrays.asList(1l, 3l));

        mongoStorage.getConnection().disconnect();
    }
    
    @Test
    public void canQueryNot() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        mongoStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        Query<CpuStat> query = mongoStorage.createQuery(CpuStatDAO.cpuStatCategory);
        Expression expr = factory.not(factory.greaterThan(Key.TIMESTAMP, 2l));
        query.where(expr);
        query.sort(Key.TIMESTAMP, SortDirection.ASCENDING);

        executeAndVerifyQuery(query, Arrays.asList(0l, 1l, 2l));

        mongoStorage.getConnection().disconnect();
    }
    
    @Test
    public void canQueryAnd() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        mongoStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        Query<CpuStat> query = mongoStorage.createQuery(CpuStatDAO.cpuStatCategory);
        Expression expr = factory.and(factory.greaterThan(Key.TIMESTAMP, 0l),
                                      factory.lessThan(Key.TIMESTAMP, 2l));
        query.where(expr);
        query.sort(Key.TIMESTAMP, SortDirection.ASCENDING);

        executeAndVerifyQuery(query, Arrays.asList(1l));

        mongoStorage.getConnection().disconnect();
    }
    
    @Test
    public void canQueryOr() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        mongoStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        Query<CpuStat> query = mongoStorage.createQuery(CpuStatDAO.cpuStatCategory);
        Expression expr = factory.or(factory.greaterThan(Key.TIMESTAMP, 2l),
                                      factory.lessThan(Key.TIMESTAMP, 1l));
        query.where(expr);
        query.sort(Key.TIMESTAMP, SortDirection.ASCENDING);

        executeAndVerifyQuery(query, Arrays.asList(0l, 3l));

        mongoStorage.getConnection().disconnect();
    }

    @Test
    public void canLoadSave() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        Storage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        
        byte[] data = "Hello World".getBytes();
        mongoStorage.saveFile("test", new ByteArrayInputStream(data));
        // Note: On the server side, the file is saved into mongodb
        // via GridFS.  The save operation returns before write is
        // complete, and there is no callback mechanism to find out
        // when the write is complete.  So, we try a few times to
        // load it before considering it a failure.
        InputStream loadStream = null;
        int loadAttempts = 0;
        while (loadStream == null && loadAttempts < 3) {
            Thread.sleep(300);
            loadStream = mongoStorage.loadFile("test");
            loadAttempts++;
        }
        assertNotNull(loadStream);
        StringBuilder str = new StringBuilder();
        int i = loadStream.read();
        while (i != -1) {
            str.append((char) i);
            i = loadStream.read();
        }
        assertEquals("Hello World", str.toString());

        mongoStorage.getConnection().disconnect();
    }

    @Test
    public void storagePurge() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        UUID uuid = new UUID(42, 24);

        mongoStorage.registerCategory(VmCpuStatDAO.vmCpuStatCategory);
        long timeStamp = 5;
        double cpuLoad = 0.15;
        VmCpuStat pojo = new VmCpuStat(uuid.toString(), timeStamp, VM_ID1, cpuLoad);
        Add<VmCpuStat> add = mongoStorage.createAdd(VmCpuStatDAO.vmCpuStatCategory);
        add.set(Key.AGENT_ID.getName(), pojo.getAgentId());
        add.set(Key.VM_ID.getName(), pojo.getVmId());
        add.set(Key.TIMESTAMP.getName(), pojo.getTimeStamp());
        add.set(VmCpuStatDAO.vmCpuLoadKey.getName(), pojo.getCpuLoad());
        add.apply();

        Query<VmCpuStat> query = mongoStorage.createQuery(VmCpuStatDAO.vmCpuStatCategory);
        Cursor<VmCpuStat> cursor = query.execute();
        assertTrue(cursor.hasNext());
        pojo = cursor.next();
        assertFalse(cursor.hasNext());

        assertEquals(timeStamp, pojo.getTimeStamp());
        assertEquals(VM_ID1, pojo.getVmId());
        assertEquals(cpuLoad, pojo.getCpuLoad(), EQUALS_DELTA);
        assertEquals(uuid.toString(), pojo.getAgentId());

        mongoStorage.purge(uuid.toString());
    }
}

