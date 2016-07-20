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

package com.redhat.thermostat.itest;

import static com.redhat.thermostat.common.utils.IteratorUtils.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.common.ApplicationInfo;
import com.redhat.thermostat.common.internal.test.FreePortFinder;
import com.redhat.thermostat.common.internal.test.FreePortFinder.TryPort;
import com.redhat.thermostat.host.cpu.common.CpuStatDAO;
import com.redhat.thermostat.host.cpu.common.model.CpuStat;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.shared.config.internal.SSLConfigurationImpl;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.BackingStorage;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.IllegalDescriptorException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.mongodb.internal.MongoStorage;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;
import com.redhat.thermostat.vm.classstat.common.VmClassStatDAO;
import com.redhat.thermostat.vm.classstat.common.model.VmClassStat;
import com.redhat.thermostat.vm.cpu.common.VmCpuStatDAO;
import com.redhat.thermostat.vm.cpu.common.model.VmCpuStat;
import com.redhat.thermostat.web.client.internal.WebStorage;
import com.redhat.thermostat.web.server.auth.Roles;

import expectj.ExpectJ;
import expectj.Spawn;

/**
 * This test class starts up a mongod instance and a web storage instance
 * (in jetty container) in front of that.  Tests should make their own
 * connection to the web storage, probably by making use of one of the
 * getAndConnectStorage() method variants.
 * 
 * Because the storage instance is shared among all of the tests, it is
 * necessary to take precautions to avoid introducing data dependencies
 * between tests.  Such precautions could include: using a different
 * category (ie mongod collection) than any other existing test; setting
 * a unique agent-id for all data written and then deleting the data
 * at the end of the test; <insert other clever idea here>.
 * 
 * Please don't introduce any more sporadic test failures to this
 * integration test!!!
 */
public class WebAppTest extends WebStorageUsingIntegrationTest {

    /*
     * Registry of descriptors this test needs to allow in order to avoid
     * illegal statement descriptor exceptions being thrown. See also:
     * WebAppTestStatementDescriptorRegistration
     */
    public static final Set<String> TRUSTED_DESCRIPTORS;
    // descriptive name -> descriptor mapping
    private static final Map<String, String> DESCRIPTOR_MAP;
    
    private static final String KEY_AUTHORIZED_QUERY = "authorizedQuery";
    private static final String KEY_AUTHORIZED_QUERY_EQUAL_TO = "authorizedQueryEqualTo";
    private static final String KEY_AUTHORIZED_QUERY_NOT_EQUAL_TO = "authorizedQueryNotEqualTo";
    private static final String KEY_AUTHORIZED_QUERY_GREATER_THAN = "authorizedQueryGreaterThan";
    private static final String KEY_AUTHORIZED_QUERY_GREATER_THAN_OR_EQUAL_TO = "authorizedQueryGreaterThanOrEqualTo";
    private static final String KEY_AUTHORIZED_QUERY_LESS_THAN = "authorizedQueryLessThan";
    private static final String KEY_AUTHORIZED_QUERY_LESS_THAN_OR_EQUAL_TO = "authorizedQueryLessThanOrEqualTo";
    private static final String KEY_AUTHORIZED_QUERY_NOT = "authorizedQueryNot";
    private static final String KEY_AUTHORIZED_QUERY_AND = "authorizedQueryAnd";
    private static final String KEY_AUTHORIZED_QUERY_OR = "authorizedQueryOr";
    private static final String KEY_STORAGE_PURGE = "storagePurge";
    private static final String KEY_AUTHORIZED_FILTERED_QUERY = "authorizedFilteredQuerySubset";
    
    static {
        Map<String, String> descMap = new HashMap<>();
        descMap.put(KEY_AUTHORIZED_FILTERED_QUERY, "QUERY agent-config");
        descMap.put(KEY_AUTHORIZED_QUERY, "QUERY cpu-stats SORT ?s ASC");
        descMap.put(KEY_AUTHORIZED_QUERY_EQUAL_TO, "QUERY cpu-stats WHERE 'timeStamp' = ?l SORT 'timeStamp' ASC");
        descMap.put(KEY_AUTHORIZED_QUERY_NOT_EQUAL_TO, "QUERY cpu-stats WHERE 'timeStamp' != ?l SORT 'timeStamp' ASC");
        descMap.put(KEY_AUTHORIZED_QUERY_GREATER_THAN, "QUERY cpu-stats WHERE 'timeStamp' > ?l SORT 'timeStamp' ASC");
        descMap.put(KEY_AUTHORIZED_QUERY_GREATER_THAN_OR_EQUAL_TO, "QUERY cpu-stats WHERE 'timeStamp' >= ?l SORT 'timeStamp' ASC");
        descMap.put(KEY_AUTHORIZED_QUERY_LESS_THAN, "QUERY cpu-stats WHERE 'timeStamp' < ?l SORT 'timeStamp' ASC");
        descMap.put(KEY_AUTHORIZED_QUERY_LESS_THAN_OR_EQUAL_TO, "QUERY cpu-stats WHERE 'timeStamp' <= ?l SORT 'timeStamp' ASC");
        descMap.put(KEY_AUTHORIZED_QUERY_NOT, "QUERY cpu-stats WHERE NOT 'timeStamp' > ?l SORT 'timeStamp' ASC");
        descMap.put(KEY_AUTHORIZED_QUERY_AND, "QUERY cpu-stats WHERE 'timeStamp' > 0 AND 'timeStamp' < ?l SORT 'timeStamp' ASC");
        descMap.put(KEY_AUTHORIZED_QUERY_OR, "QUERY cpu-stats WHERE 'timeStamp' > ?l OR 'timeStamp' < ?l SORT 'timeStamp' ASC");
        descMap.put(KEY_STORAGE_PURGE, "QUERY vm-cpu-stats");
        Set<String> trustedDescriptors = new HashSet<>();
        for (String val: descMap.values()) {
            trustedDescriptors.add(val);
        }
        TRUSTED_DESCRIPTORS = trustedDescriptors;
        DESCRIPTOR_MAP = descMap;
    }
    
    
    private static class CountdownConnectionListener implements ConnectionListener {

        private final ConnectionStatus target;
        private final CountDownLatch latch;
        private final AtomicBoolean indicator;

        private CountdownConnectionListener(ConnectionStatus target, CountDownLatch latch, AtomicBoolean indicator) {
            this.target = target;
            this.latch = latch;
            this.indicator = indicator;
        }

        @Override
        public void changed(ConnectionStatus newStatus) {
            indicator.set(true);
            assertEquals(target, newStatus);
            latch.countDown();
        }
    }

    private static class WebAppContextListener implements Listener {
        private Throwable cause;
        private boolean failed = false;
        private final CountDownLatch contextStartedLatch;
        private WebAppContextListener(CountDownLatch latch) {
            this.contextStartedLatch = latch;
        }

        @Override
        public void lifeCycleStarting(LifeCycle event) {
            // nothing
        }

        @Override
        public void lifeCycleStarted(LifeCycle event) {
            contextStartedLatch.countDown();
        }

        @Override
        public void lifeCycleFailure(LifeCycle event, Throwable cause) {
            this.failed = true;
            this.cause = cause;
            contextStartedLatch.countDown();
        }

        @Override
        public void lifeCycleStopping(LifeCycle event) {
            // nothing
        }

        @Override
        public void lifeCycleStopped(LifeCycle event) {
            // nothing
        }
    }

    private static final String TEST_USER = "testuser";
    private static final String TEST_PASSWORD = "testpassword";
    private static final double EQUALS_DELTA = 0.00000000000001;
    
    private static final String VM_ID1 = "vmId1";
    private static final String VM_ID2 = "vmId2";
    private static final String VM_ID3 = "vmId3";

    private static Server server;
    private static int port;
    

    @BeforeClass
    public static void setUpOnce() throws Exception {
        clearStorageDataDirectory();

        backupOriginalCredentialsFiles();

        createFakeSetupCompleteFile();
        createFakeUserSetupDoneFile();

        addUserToStorage(getMongodbUsername(), getMongodbPassword());

        startStorage();

        ExpectJ mongo = new ExpectJ(TIMEOUT_IN_SECONDS);
        Spawn mongoSpawn = mongo.spawn("mongo 127.0.0.1:27518");
        mongoSpawn.send("use thermostat\n");
        mongoSpawn.expect("switched to db thermostat");
        mongoSpawn.send(String.format("db.auth(\"%s\", \"%s\")\n", getMongodbUsername(), getMongodbPassword()));
        mongoSpawn.expect("1");
        mongoSpawn.send("db[\"fake\"].insert({foo:\"bar\", baz: 1})\n");
        mongoSpawn.send("db[\"fake\"].findOne()\n");
        mongoSpawn.send("show collections\n");

        createWebAuthFile();

        // start the server, deploy the war
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port);
            }
        });

        addCpuData(4);
    }

    @AfterClass
    public static void tearDownOnce() throws Exception {
        try {
            deleteCpuData();
        } catch (Exception e) {
            System.out.println("AN ERROR OCCURRED DELETING CPU DATA!");
            e.printStackTrace();
            throw e;
        } finally {
            try {
                // Avoid NPEs during teardown when server failed to
                // start for some reason.
                if (server != null) {
                    server.stop();
                    server.join();
                }
            } catch (Exception e) {
                System.out.println("AN ERROR OCCURRED STOPPING JETTY!");
                e.printStackTrace();
                throw e;
            } finally {
                try {
                    stopStorage();
                } catch (Exception e) {
                    System.out.println("AN ERROR OCCURRED STOPPING STORAGE!");
                    e.printStackTrace();
                    throw e;
                } finally {
                    removeSetupCompleteStampFiles();
                    restoreBackedUpCredentialsFiles();
                    System.out.println("RESTORED backed-up files!");
                    clearStorageDataDirectory();
                }
            }
        }
    }
    
    private static long countAllData(BackingStorage storage, Category<AggregateCount> cat) {
        try {
            String countAllDataDesc = "QUERY-COUNT " + cat.getName();
            StatementDescriptor<AggregateCount> desc = new StatementDescriptor<>(cat, countAllDataDesc);
            PreparedStatement<AggregateCount> statement = storage.prepareStatement(desc);
            Cursor<AggregateCount> cursor = statement.executeQuery();
            assert cursor.hasNext();
            AggregateCount aggregate = cursor.next();
            long count = aggregate.getCount();
            return count;
        } catch (StatementExecutionException | DescriptorParsingException e) {
            throw new AssertionError(e);
        }
    }

    private static void createWebAuthFile() throws IOException {
        System.out.println("WRITING auth file: " + getMongodbUsername() + "/" + getMongodbPassword());
        List<String> lines = new ArrayList<String>();
        lines.add("username = " + getMongodbUsername());
        lines.add("password = " + getMongodbPassword());
        Files.write(new File(THERMOSTAT_WEB_AUTH_FILE).toPath(), lines, StandardCharsets.US_ASCII);
    }

    private static String getMongodbUsername() {
        // Define this default in order for IDE based runs to require fewer
        // properties to be set.
        String defaultDevUser = "mongodevuser";
        String devUsername = System.getProperty("mongodb.dev.username", defaultDevUser);
        return devUsername;
    }

    private static String getMongodbPassword() {
        // Define this default in order for IDE based runs to require fewer
        // properties to be set.
        String defaultDevPassword = "mongodevpassword";
        String devPassword = System.getProperty("mongodb.dev.password", defaultDevPassword);
        return devPassword;
    }
    
    /*
     * Queries tests use write operations to put things into storage. For them
     * we don't want to go through the hassles of using prepared writes. Instead
     * use mongo-storage directly (which is a BackingStorage). 
     */
    private static BackingStorage getAndConnectBackingStorage() {
        final String url = "mongodb://127.0.0.1:27518";
        StorageCredentials creds = new StorageCredentials() {

            @Override
            public String getUsername() {
                return getMongodbUsername();
            }

            @Override
            public char[] getPassword() {
                return getMongodbPassword().toCharArray();
            }
            
        };
        SSLConfiguration sslConfig = new SSLConfiguration() {

            @Override
            public File getKeystoreFile() {
                return null;
            }

            @Override
            public String getKeyStorePassword() {
                return null;
            }

            @Override
            public boolean enableForCmdChannel() {
                // Simple case.
                return false;
            }

            @Override
            public boolean enableForBackingStorage() {
                return false;
            }

            @Override
            public boolean disableHostnameVerification() {
                return false;
            }
        };
        BackingStorage storage = new MongoStorage(url, creds, sslConfig);
        storage.getConnection().connect();
        return storage;
    }

    /*
     * Using the given username and password, set up a user for JAAS in the web app,
     * with the given roles, and make a storage connection to the web app (returning
     * the Storage object).
     */
    private static Storage getAndConnectStorage(String username, String password,
                                                String[] roleNames) throws IOException {
        return getAndConnectStorage(username, password, roleNames, null);
    }

    /*
     * Using the given username and password, set up a user for JAAS in the web app,
     * with the given roles, and make a connection to the web app (returning the
     * Storage object).  Before initiating the connection, add the ConnectionListener
     * to Storage.
     */
    private static Storage getAndConnectStorage(final String username, final String password,
                                                String[] roleNames,
                                                ConnectionListener listener) throws IOException {
        setupJAASForUser(roleNames, username, password);
        final String url = "http://localhost:" + port + "/thermostat/storage";
        StorageCredentials creds = new StorageCredentials() {

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public char[] getPassword() {
                return password.toCharArray();
            }
            
        };
        CommonPaths paths = mock(CommonPaths.class);
        when(paths.getSystemConfigurationDirectory()).thenReturn(new File(getConfigurationDir()));
        when(paths.getUserConfigurationDirectory()).thenReturn(new File(getUserThermostatHome(), "etc"));
        SSLConfiguration sslConf = new SSLConfigurationImpl(paths);
        Storage storage = new WebStorage(url, creds, sslConf);
        if (listener != null) {
            storage.getConnection().addListener(listener);
        }
        storage.getConnection().connect();
        return storage;
    }

    private static void setupJAASForUser(String[] roleNames, String user,
            String password) throws IOException {
        Properties userProps = new Properties();
        userProps.put(user, password);
        Properties roleProps = new Properties();
        StringBuffer roles = new StringBuffer();
        for (int i = 0; i < roleNames.length - 1; i++) {
            roles.append(roleNames[i] + ", ");
        }
        roles.append(roleNames[roleNames.length - 1]);
        roleProps.put(user, roles.toString());
        writeThermostatUsersRolesFile(userProps, roleProps);
    }
    
    private static void writeThermostatUsersRolesFile(Properties usersContent, Properties rolesContent) throws IOException {
        File thermostatUsers = new File(THERMOSTAT_USERS_FILE);
        File thermostatRoles = new File(THERMOSTAT_ROLES_FILE);
        try (FileOutputStream usersStream = new FileOutputStream(thermostatUsers)) {
            usersContent.store(usersStream, "integration-test users");
        }
        try (FileOutputStream rolesStream = new FileOutputStream(thermostatRoles)) {
            rolesContent.store(rolesStream, "integration-test roles");
        }
    }

    private static void startServer(int port) throws Exception {
        final CountDownLatch contextStartedLatch = new CountDownLatch(1);
        server = new Server(port);
        ApplicationInfo appInfo = new ApplicationInfo();
        String version = appInfo.getMavenVersion();
        String warfile = "target/libs/thermostat-web-war-" + version + ".war";
        WebAppContext ctx = new WebAppContext(warfile, "/thermostat");
        
        // We need to set this to true in order for WebStorageEndPoint to pick
        // up the descriptor registrations from WebAppTestStatementDescriptorRegistration
        // which would result in 
        // "java.util.ServiceConfigurationError: com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration: Provider com.redhat.thermostat.itest.WebAppTestStatementDescriptorRegistration not a subtype"
        // errors.
        ctx.setParentLoaderPriority(true);
        
        WebAppContextListener listener = new WebAppContextListener(contextStartedLatch);
        ctx.addLifeCycleListener(listener);
        /* The web archive has a jetty-web.xml config file which sets up the
         * JAAS config. If done in code, this would look like this:
         *
         * JAASLoginService loginS = new JAASLoginService();
         * loginS.setLoginModuleName("ThermostatJAASLogin");
         * loginS.setName("Thermostat Realm");
         * loginS.setRoleClassNames(new String[] {
         * WrappedRolePrincipal.class.getName(),
         *       RolePrincipal.class.getName(),
         *       UserPrincipal.class.getName()
         * });
         * ctx.getSecurityHandler().setLoginService(loginS);
         * 
         */
        server.setHandler(ctx);
        server.start();
        // wait for context to start
        contextStartedLatch.await();
        if (listener.failed) {
            throw new IllegalStateException(listener.cause);
        }
    }

    private static void addCpuData(int numberOfItems) throws IOException {
        BackingStorage storage = getAndConnectBackingStorage();
        Category<AggregateCount> cat = registerCatoriesForWrite(storage, CpuStatDAO.cpuStatCategory);

        for (int i = 0; i < numberOfItems; i++) {
            CpuStat pojo = new CpuStat("test-agent-id", i, new double[] {i, i*2});
            Add<CpuStat> add = storage.createAdd(CpuStatDAO.cpuStatCategory);
            add.set(Key.AGENT_ID.getName(), pojo.getAgentId());
            add.set(CpuStatDAO.cpuLoadKey.getName(), pojo.getPerProcessorUsage());
            add.set(Key.TIMESTAMP.getName(), pojo.getTimeStamp());
            add.apply();
        }
        waitForDataCount(numberOfItems, storage, cat);
        storage.getConnection().disconnect();
    }
    
    private static void waitForDataCount(int expectedCount, BackingStorage storage, Category<AggregateCount> cat) {
        long count = countAllData(storage, cat);
        int currCount = 0;
        final int MAX_CYCLES = 5;
        while (count != expectedCount && currCount < MAX_CYCLES) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ignored) {}
            count = countAllData(storage, cat);
        }
        
    }

    private static <T extends Pojo> Category<AggregateCount> registerCatoriesForWrite(BackingStorage storage, Category<T> cat) {
        storage.registerCategory(cat);
        Category<AggregateCount> adaptedCategory = new CategoryAdapter<T, AggregateCount>(cat).getAdapted(AggregateCount.class);
        storage.registerCategory(adaptedCategory);
        return adaptedCategory;
    }
    
    private static void addHostInfoData(int numberOfItems) throws IOException {
        BackingStorage storage = getAndConnectBackingStorage();
        Category<AggregateCount> cat = registerCatoriesForWrite(storage, HostInfoDAO.hostInfoCategory);

        for (int i = 0; i < numberOfItems; i++) {
            HostInfo hostInfo = new HostInfo("test-host-agent-id", "foo " + i, "linux " + i, "kernel", "t8", i, i * 1000);
            Add<HostInfo> add = storage.createAdd(HostInfoDAO.hostInfoCategory);
            add.set(Key.AGENT_ID.getName(), hostInfo.getAgentId());
            add.set(HostInfoDAO.hostNameKey.getName(), hostInfo.getHostname());
            add.set(HostInfoDAO.cpuCountKey.getName(), hostInfo.getCpuCount());
            add.set(HostInfoDAO.cpuModelKey.getName(), hostInfo.getCpuModel());
            add.set(HostInfoDAO.hostMemoryTotalKey.getName(), hostInfo.getTotalMemory());
            add.set(HostInfoDAO.osKernelKey.getName(), hostInfo.getOsKernel());
            add.set(HostInfoDAO.osNameKey.getName(), hostInfo.getOsName());
            add.apply();
        }
        waitForDataCount(numberOfItems, storage, cat);
        storage.getConnection().disconnect();
    }
    
    private static void addAgentConfigData(List<AgentInformation> items) throws IOException {
        BackingStorage storage = getAndConnectBackingStorage();
        Category<AggregateCount> cat = registerCatoriesForWrite(storage, AgentInfoDAO.CATEGORY);

        for (AgentInformation info: items) {
            Add<AgentInformation> add = storage.createAdd(AgentInfoDAO.CATEGORY);
            add.set(Key.AGENT_ID.getName(), info.getAgentId());
            add.set(AgentInfoDAO.ALIVE_KEY.getName(), info.isAlive());
            add.set(AgentInfoDAO.CONFIG_LISTEN_ADDRESS.getName(), info.getConfigListenAddress());
            add.set(AgentInfoDAO.START_TIME_KEY.getName(), info.getStartTime());
            add.set(AgentInfoDAO.STOP_TIME_KEY.getName(), info.getStopTime());
            add.apply();
        }
        waitForDataCount(items.size(), storage, cat);
        storage.getConnection().disconnect();
    }

    private static void deleteCpuData() throws IOException {
        doDeleteData(CpuStatDAO.cpuStatCategory, "test-agent-id");
    }
    
    private static void deleteHostInfoData() throws IOException {
        doDeleteData(HostInfoDAO.hostInfoCategory, "test-host-agent-id");
    }
    
    private static <T extends Pojo> void doDeleteData(Category<T> category, String agentId) throws IOException {
        BackingStorage storage = getAndConnectBackingStorage();
        Category<AggregateCount> cat = registerCatoriesForWrite(storage, category);
        // FIXME: The method signature suggests it deletes data for the given agent
        //        in the given category. But it actually deletes any records in
        //        any category matching the agentId parameter. This should get
        //        changed from purge to a remove operation.
        storage.purge(agentId);
        waitForDataCount(0, storage, cat);
        storage.getConnection().disconnect();
    }
    
    private static void deleteAgentConfigData(List<AgentInformation> items) throws IOException {
        BackingStorage storage = getAndConnectBackingStorage();
        Category<AggregateCount> cat = registerCatoriesForWrite(storage, AgentInfoDAO.CATEGORY);
        
        long countPriorRemove = countAllData(storage, cat);
        ExpressionFactory factory = new ExpressionFactory();
        Remove<AgentInformation> remove = storage.createRemove(AgentInfoDAO.CATEGORY);
        Set<String> agentIds = new HashSet<>();
        for (AgentInformation info: items) {
            agentIds.add(info.getAgentId());
        }
        Expression expression = factory.in(Key.AGENT_ID, agentIds, String.class);
        remove.where(expression);
        remove.apply();
        waitForDataCount((int)(countPriorRemove - items.size()), storage, cat);

        storage.getConnection().disconnect();
    }

    private void executeAndVerifyQuery(PreparedStatement<CpuStat> query, List<Long> expectedTimestamps) throws StatementExecutionException {
        Cursor<CpuStat> cursor = query.executeQuery();

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
    public void authorizedPreparedAdd() throws Exception {
        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.WRITE,
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT,
        };
        
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(VmClassStatDAO.vmClassStatsCategory);
        
        // This is the same descriptor as VmClassStatDAOImpl uses. It also
        // gets registered automatically for that reason, no need to do it
        // manually for this test.
        String strDesc = "ADD vm-class-stats SET 'agentId' = ?s , " +
                                "'vmId' = ?s , " +
                                "'timeStamp' = ?l , " +
                                "'loadedClasses' = ?l , " +
                                "'loadedBytes' = ?l , " +
                                "'unloadedClasses' = ?l , " +
                                "'unloadedBytes' = ?l , " +
                                "'classLoadTime' = ?l";
        StatementDescriptor<VmClassStat> desc = new StatementDescriptor<>(VmClassStatDAO.vmClassStatsCategory, strDesc);
        VmClassStat pojo = new VmClassStat();
        pojo.setAgentId("fluff");
        pojo.setLoadedClasses(12345);
        pojo.setTimeStamp(42);
        pojo.setVmId(VM_ID1);
        PreparedStatement<VmClassStat> add;
        add = webStorage.prepareStatement(desc);
        addPreparedVmClassStat(pojo, add);
        
        // Add another couple of entries
        pojo = new VmClassStat();
        pojo.setAgentId("fluff");
        pojo.setLoadedClasses(67890);
        pojo.setTimeStamp(42);
        pojo.setVmId(VM_ID2);
        addPreparedVmClassStat(pojo, add);
        
        pojo = new VmClassStat();
        pojo.setAgentId("fluff");
        pojo.setLoadedClasses(34567);
        pojo.setTimeStamp(42);
        pojo.setVmId(VM_ID3);
        addPreparedVmClassStat(pojo, add);
        
        webStorage.getConnection().disconnect();
    }
    
    private void addPreparedVmClassStat(VmClassStat pojo,
            PreparedStatement<VmClassStat> add)
            throws StatementExecutionException {
        add.setString(0, pojo.getAgentId());
        add.setString(1, pojo.getVmId());
        add.setLong(2, pojo.getTimeStamp());
        add.setLong(3, pojo.getLoadedClasses());
        add.setLong(4, pojo.getLoadedBytes());
        add.setLong(5, pojo.getUnloadedClasses());
        add.setLong(6, pojo.getUnloadedBytes());
        add.setLong(7, pojo.getClassLoadTime());
        add.execute();
    }
    
    /*
     * Tests whether a query only returns results which a user is allowed to see.
     * 
     * In particular, multiple agent-config records available in the DB, but
     * only a subset are allowed to be seen by the user.
     */
    @Test
    public void authorizedFilteredQuerySubset() throws Exception {
        // add agent records into the DB
        List<AgentInformation> items = Collections.emptyList();
        try {
            String agentIdGrantPrefix = "thermostat-agents-grant-read-agentId-";
            String agent1Id = "agent1";
            String agent2Id = "agent2";
            items = getAgentInformationItemsIncluding(new String[] { agent1Id, agent2Id });
            // assert pre-condition. records in db need to be more than expected
            // result set size.
            assertTrue(items.size() > 2);
            addAgentConfigData(items);
            String[] roleNames = new String[] {
                    Roles.REGISTER_CATEGORY,
                    Roles.READ,
                    Roles.LOGIN,
                    Roles.ACCESS_REALM,
                    Roles.PREPARE_STATEMENT,
                    // Grant read access only for "agent1" and "agent2" agend IDs
                    agentIdGrantPrefix + agent1Id,
                    agentIdGrantPrefix + agent2Id
            };
            
            Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
            webStorage.registerCategory(AgentInfoDAO.CATEGORY);
            
            String strDesc = DESCRIPTOR_MAP.get(KEY_AUTHORIZED_FILTERED_QUERY);
            StatementDescriptor<AgentInformation> queryDesc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, strDesc);
            PreparedStatement<AgentInformation> query = webStorage.prepareStatement(queryDesc);
            Cursor<AgentInformation> cursor = query.executeQuery();
            assertTrue(cursor.hasNext());
            List<AgentInformation> actual = asList(cursor);
            assertEquals(2, actual.size());
            assertFalse("Returned agentIds should be different!", actual.get(0).getAgentId().equals(actual.get(1).getAgentId()));
            for (AgentInformation info: actual) {
                assertTrue(info.getAgentId().equals(agent1Id) || info.getAgentId().equals(agent2Id));
            }
        } finally {
           deleteAgentConfigData(items); 
        }
    }
    
    
    private List<AgentInformation> getAgentInformationItemsIncluding(
            String[] includeItems) {
        List<AgentInformation> infos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String agentId = UUID.randomUUID().toString() + "--" + i;
            if (i < includeItems.length) {
                agentId = includeItems[i];
            }
            AgentInformation info = new AgentInformation();
            info.setAgentId(agentId);
            info.setAlive((i % 2) == 0);
            info.setConfigListenAddress("127.0.0." + i + ":88888");
            info.setStartTime(i * 300);
            info.setStopTime((i + 1) * 400);
            infos.add(info);
        }
        return infos;
    }

    /*
     * Tests whether no query results are returned for a user which lacks *any*
     * granting roles for reads.
     */
    @Test
    public void authorizedFilteredQueryNone() throws Exception {
        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ, // this is just the stop-gap role.
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT,
                // lacking read grant roles
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(CpuStatDAO.cpuStatCategory);
        
        String strDesc = DESCRIPTOR_MAP.get(KEY_AUTHORIZED_QUERY);
        StatementDescriptor<CpuStat> queryDesc = new StatementDescriptor<>(CpuStatDAO.cpuStatCategory, strDesc);
        PreparedStatement<CpuStat> query = webStorage.prepareStatement(queryDesc);

        query.setString(0, "timeStamp");
        // Note: with read-all granted, this returns 4 records. See authorized
        //       query test.
        // For this test, however, it should come back empty.
        
        Cursor<CpuStat> cursor = query.executeQuery();
        assertFalse(cursor.hasNext());
        try {
            cursor.next();
            fail("Cursor should throw a NoSuchElementException!");
        } catch (NoSuchElementException e) {
            // pass
        }

        webStorage.getConnection().disconnect();
    }
    
    @Test
    public void authorizedQuery() throws Exception {

        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT,
                Roles.GRANT_READ_ALL
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(CpuStatDAO.cpuStatCategory);
        
        String strDesc = DESCRIPTOR_MAP.get(KEY_AUTHORIZED_QUERY);
        StatementDescriptor<CpuStat> queryDesc = new StatementDescriptor<>(CpuStatDAO.cpuStatCategory, strDesc);
        PreparedStatement<CpuStat> query = webStorage.prepareStatement(queryDesc);

        query.setString(0, "timeStamp");
        
        executeAndVerifyQuery(query, Arrays.asList(0l, 1l, 2l, 3l));

        webStorage.getConnection().disconnect();
    }
    
    @Test
    public void authorizedAggregateCount() throws Exception {
        try {
            int count = 2;
            // registers host info category
            addHostInfoData(count);
            
            String[] roleNames = new String[] {
                    Roles.REGISTER_CATEGORY,
                    Roles.READ,
                    Roles.LOGIN,
                    Roles.ACCESS_REALM,
                    Roles.PREPARE_STATEMENT,
                    Roles.GRANT_READ_ALL // don't want to test filtered results
            };
            Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
            Category<AggregateCount> adapted = new CategoryAdapter<HostInfo, AggregateCount>(HostInfoDAO.hostInfoCategory).getAdapted(AggregateCount.class);
            // register non-adapted + adapted category in that order. Adapted
            // category needs to be registered, since it gets it's own mapped id
            webStorage.registerCategory(HostInfoDAO.hostInfoCategory);
            webStorage.registerCategory(adapted);
            
            // storage-core registers this descriptor. no need to do it in this
            // test.
            String strDesc = "QUERY-COUNT host-info";
            StatementDescriptor<AggregateCount> queryDesc = new StatementDescriptor<>(adapted, strDesc);
            PreparedStatement<AggregateCount> query = webStorage.prepareStatement(queryDesc);
    
            Cursor<AggregateCount> cursor = query.executeQuery();
            assertTrue(cursor.hasNext());
            AggregateCount c = cursor.next();
            assertFalse(cursor.hasNext());
            assertEquals(count, c.getCount());
    
            webStorage.getConnection().disconnect();
        } finally {
            deleteHostInfoData();
        }
    }
    
    @Test
    public void authorizedQueryEqualTo() throws Exception {

        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT,
                Roles.GRANT_READ_ALL
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        String strDesc = DESCRIPTOR_MAP.get(KEY_AUTHORIZED_QUERY_EQUAL_TO);
        StatementDescriptor<CpuStat> queryDesc = new StatementDescriptor<>(CpuStatDAO.cpuStatCategory, strDesc);
        PreparedStatement<CpuStat> query = webStorage.prepareStatement(queryDesc);
        query.setLong(0, 2l);
        
        executeAndVerifyQuery(query, Arrays.asList(2l));

        webStorage.getConnection().disconnect();
    }
    
    @Test
    public void authorizedQueryNotEqualTo() throws Exception {

        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT,
                Roles.GRANT_READ_ALL
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        String strDesc = DESCRIPTOR_MAP.get(KEY_AUTHORIZED_QUERY_NOT_EQUAL_TO);
        StatementDescriptor<CpuStat> queryDesc = new StatementDescriptor<>(CpuStatDAO.cpuStatCategory, strDesc);
        PreparedStatement<CpuStat> query = webStorage.prepareStatement(queryDesc);
        query.setLong(0, 2l);
        
        executeAndVerifyQuery(query, Arrays.asList(0l, 1l, 3l));

        webStorage.getConnection().disconnect();
    }
    
    @Test
    public void authorizedQueryGreaterThan() throws Exception {

        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT,
                Roles.GRANT_READ_ALL
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        String strDesc = DESCRIPTOR_MAP.get(KEY_AUTHORIZED_QUERY_GREATER_THAN);
        StatementDescriptor<CpuStat> queryDesc = new StatementDescriptor<>(CpuStatDAO.cpuStatCategory, strDesc);
        PreparedStatement<CpuStat> query = webStorage.prepareStatement(queryDesc);
        query.setLong(0, 2l);
        
        executeAndVerifyQuery(query, Arrays.asList(3l));

        webStorage.getConnection().disconnect();
    }
    
    @Test
    public void authorizedQueryGreaterThanOrEqualTo() throws Exception {

        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT,
                Roles.GRANT_READ_ALL
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        String strDesc = DESCRIPTOR_MAP.get(KEY_AUTHORIZED_QUERY_GREATER_THAN_OR_EQUAL_TO);
        StatementDescriptor<CpuStat> queryDesc = new StatementDescriptor<>(CpuStatDAO.cpuStatCategory, strDesc);
        PreparedStatement<CpuStat> query = webStorage.prepareStatement(queryDesc);
        query.setLong(0, 2l);
        
        executeAndVerifyQuery(query, Arrays.asList(2l, 3l));

        webStorage.getConnection().disconnect();
    }
    
    @Test
    public void authorizedQueryLessThan() throws Exception {

        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT,
                Roles.GRANT_READ_ALL
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        String strDesc = DESCRIPTOR_MAP.get(KEY_AUTHORIZED_QUERY_LESS_THAN);
        StatementDescriptor<CpuStat> queryDesc = new StatementDescriptor<>(CpuStatDAO.cpuStatCategory, strDesc);
        PreparedStatement<CpuStat> query = webStorage.prepareStatement(queryDesc);
        query.setLong(0, 2l);
        
        executeAndVerifyQuery(query, Arrays.asList(0l, 1l));

        webStorage.getConnection().disconnect();
    }
    
    @Test
    public void authorizedQueryLessThanOrEqualTo() throws Exception {

        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT,
                Roles.GRANT_READ_ALL
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        String strDesc = DESCRIPTOR_MAP.get(KEY_AUTHORIZED_QUERY_LESS_THAN_OR_EQUAL_TO);
        StatementDescriptor<CpuStat> queryDesc = new StatementDescriptor<>(CpuStatDAO.cpuStatCategory, strDesc);
        PreparedStatement<CpuStat> query = webStorage.prepareStatement(queryDesc);
        query.setLong(0, 2l);

        executeAndVerifyQuery(query, Arrays.asList(0l, 1l, 2l));

        webStorage.getConnection().disconnect();
    }
    
    @Test
    public void authorizedQueryNot() throws Exception {

        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT,
                Roles.GRANT_READ_ALL
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        String strDesc = DESCRIPTOR_MAP.get(KEY_AUTHORIZED_QUERY_NOT);
        StatementDescriptor<CpuStat> queryDesc = new StatementDescriptor<>(CpuStatDAO.cpuStatCategory, strDesc);
        PreparedStatement<CpuStat> query = webStorage.prepareStatement(queryDesc);
        query.setLong(0, 2l);
        
        executeAndVerifyQuery(query, Arrays.asList(0l, 1l, 2l));

        webStorage.getConnection().disconnect();
    }
    
    @Test
    public void authorizedQueryAnd() throws Exception {

        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT,
                Roles.GRANT_READ_ALL
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        String strDesc = DESCRIPTOR_MAP.get(KEY_AUTHORIZED_QUERY_AND);
        StatementDescriptor<CpuStat> queryDesc = new StatementDescriptor<>(CpuStatDAO.cpuStatCategory, strDesc);
        PreparedStatement<CpuStat> query = webStorage.prepareStatement(queryDesc);
        query.setLong(0, 2l);
        
        executeAndVerifyQuery(query, Arrays.asList(1l));

        webStorage.getConnection().disconnect();
    }
    
    @Test
    public void authorizedQueryOr() throws Exception {

        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT,
                Roles.GRANT_READ_ALL
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        String strDesc = DESCRIPTOR_MAP.get(KEY_AUTHORIZED_QUERY_OR);
        StatementDescriptor<CpuStat> queryDesc = new StatementDescriptor<>(CpuStatDAO.cpuStatCategory, strDesc);
        PreparedStatement<CpuStat> query = webStorage.prepareStatement(queryDesc);
        query.setLong(0, 2);
        query.setLong(1, 1);
        
        executeAndVerifyQuery(query, Arrays.asList(0l, 3l));

        webStorage.getConnection().disconnect();
    }
    
    @Test
    public void refuseUnknownQueryDescriptor() throws IOException {

        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(CpuStatDAO.cpuStatCategory);

        String strDesc = "QUERY cpu-stats WHERE 'fooBarTest' = ?s";
        assertFalse("wanted this descriptor to be untrusted!", TRUSTED_DESCRIPTORS.contains(strDesc));
        StatementDescriptor<CpuStat> queryDesc = new StatementDescriptor<>(CpuStatDAO.cpuStatCategory, strDesc);
        
        try {
            webStorage.prepareStatement(queryDesc);
        } catch (IllegalDescriptorException e) {
            // pass
            String expectedMsg = "Unknown query descriptor which endpoint of com.redhat.thermostat.web.client.internal.WebStorage refused to accept!";
            assertEquals(expectedMsg, e.getMessage());
        } catch (DescriptorParsingException e) {
            // should have been able to parse the descriptor
            fail(e.getMessage());
        }
        
        webStorage.getConnection().disconnect();
    }

    @Test
    public void authorizedLoadSave() throws Exception {
        String[] roleNames = new String[] {
                Roles.LOAD_FILE,
                Roles.SAVE_FILE,
                Roles.ACCESS_REALM,
                Roles.LOGIN,
                Roles.GRANT_FILES_WRITE_ALL,
                Roles.GRANT_FILES_READ_ALL
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        
        byte[] data = "Hello World".getBytes();
        webStorage.saveFile("test", new ByteArrayInputStream(data), new DoNothing());
        // Note: On the server side, the file is saved into mongodb
        // via GridFS.  The save operation returns before write is
        // complete, and there is no callback mechanism to find out
        // when the write is complete.  So, we try a few times to
        // load it before considering it a failure.
        InputStream loadStream = null;
        int loadAttempts = 0;
        while (loadStream == null && loadAttempts < 3) {
            Thread.sleep(300);
            try {
                loadStream = webStorage.loadFile("test");
            } catch (StorageException e) {
                /**
                 * Exceptions can occur if the load fails
                 * Ignore and retry.
                 */
            }
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

        webStorage.getConnection().disconnect();
    }

    @Test
    public void authorizedLoadNotExistingFile() throws Exception {
        String[] roleNames = new String[] {
                Roles.LOAD_FILE,
                Roles.ACCESS_REALM,
                Roles.LOGIN,
                Roles.GRANT_FILES_WRITE_ALL,
                Roles.GRANT_FILES_READ_ALL
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        InputStream loadStream = webStorage.loadFile("not-existing");

        assertNull(loadStream);

        webStorage.getConnection().disconnect();
    }

    @Test
    public void unauthorizedLogin() throws Exception {
        String[] roleNames = new String[] {
                Roles.ACCESS_REALM
        };

        CountDownLatch statusLatch = new CountDownLatch(1);
        AtomicBoolean listenerTriggered = new AtomicBoolean(false);
        ConnectionListener listener = new CountdownConnectionListener(ConnectionStatus.FAILED_TO_CONNECT, statusLatch, listenerTriggered);
        @SuppressWarnings("unused")
        Storage storage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames, listener);
        statusLatch.await();
        assertTrue(listenerTriggered.get());
    }

    @Test
    public void storagePurge() throws Exception {
        // Add some data to purge (uses backing storage)
        UUID uuid = new UUID(42, 24);
        long timeStamp = 5;
        double cpuLoad = 0.15;
        VmCpuStat pojo = new VmCpuStat(uuid.toString(), timeStamp, VM_ID1, cpuLoad);
        addVmCpuStat(pojo);

        String[] roleNames = new String[] {
                Roles.ACCESS_REALM,
                Roles.LOGIN,
                Roles.PURGE,
                Roles.PREPARE_STATEMENT,
                Roles.READ,
                Roles.GRANT_READ_ALL,
                Roles.REGISTER_CATEGORY
        };
        
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(VmCpuStatDAO.vmCpuStatCategory);
        
        String strDesc = DESCRIPTOR_MAP.get(KEY_STORAGE_PURGE);
        StatementDescriptor<VmCpuStat> queryDesc = new StatementDescriptor<>(VmCpuStatDAO.vmCpuStatCategory, strDesc);
        PreparedStatement<VmCpuStat> query = webStorage.prepareStatement(queryDesc);
        Cursor<VmCpuStat> cursor = query.executeQuery();
        assertTrue(cursor.hasNext());
        pojo = cursor.next();
        assertFalse(cursor.hasNext());

        assertEquals(timeStamp, pojo.getTimeStamp());
        assertEquals(VM_ID1, pojo.getVmId());
        assertEquals(cpuLoad, pojo.getCpuLoad(), EQUALS_DELTA);
        assertEquals(uuid.toString(), pojo.getAgentId());

        webStorage.purge(uuid.toString());
    }

    private void addVmCpuStat(VmCpuStat pojo) {
        BackingStorage storage = getAndConnectBackingStorage();
        storage.registerCategory(VmCpuStatDAO.vmCpuStatCategory);
        Add<VmCpuStat> add = storage.createAdd(VmCpuStatDAO.vmCpuStatCategory);
        add.set(Key.AGENT_ID.getName(), pojo.getAgentId());
        add.set(Key.VM_ID.getName(), pojo.getVmId());
        add.set(Key.TIMESTAMP.getName(), pojo.getTimeStamp());
        add.set(VmCpuStatDAO.vmCpuLoadKey.getName(), pojo.getCpuLoad());
        add.apply();
        storage.getConnection().disconnect();        
    }
}

