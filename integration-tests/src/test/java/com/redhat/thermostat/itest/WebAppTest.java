/*
 * Copyright 2013 Red Hat, Inc.
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
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import com.redhat.thermostat.host.cpu.common.CpuStatDAO;
import com.redhat.thermostat.host.cpu.common.model.CpuStat;
import com.redhat.thermostat.storage.config.ConnectionConfiguration;
import com.redhat.thermostat.storage.config.StartupConfiguration;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.IllegalDescriptorException;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.test.FreePortFinder;
import com.redhat.thermostat.test.FreePortFinder.TryPort;
import com.redhat.thermostat.vm.classstat.common.VmClassStatDAO;
import com.redhat.thermostat.vm.classstat.common.model.VmClassStat;
import com.redhat.thermostat.vm.cpu.common.VmCpuStatDAO;
import com.redhat.thermostat.vm.cpu.common.model.VmCpuStat;
import com.redhat.thermostat.web.client.internal.WebStorage;
import com.redhat.thermostat.web.server.auth.Roles;

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
public class WebAppTest extends IntegrationTest {

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
    private static final String KEY_SET_DEFAULT_AGENT_ID = "setDefaultAgentID";
    
    static {
        Map<String, String> descMap = new HashMap<>();
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
        descMap.put(KEY_SET_DEFAULT_AGENT_ID, "QUERY vm-cpu-stats");
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
    private static final String PREP_USER = "prepuser";
    private static final String PREP_PASSWORD = "preppassword";
    private static final double EQUALS_DELTA = 0.00000000000001;
    private static final String THERMOSTAT_USERS_FILE = getConfigurationDir() + "/thermostat-users.properties";
    private static final String THERMOSTAT_ROLES_FILE = getConfigurationDir() + "/thermostat-roles.properties";
    private static final String VM_ID1 = "vmId1";
    private static final String VM_ID2 = "vmId2";
    private static final String VM_ID3 = "vmId3";

    private static Server server;
    private static int port;
    private static Path backupUsers;
    private static Path backupRoles;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        startStorage();

        backupUsers = Files.createTempFile("itest-backup-thermostat-users", "");
        backupRoles = Files.createTempFile("itest-backup-thermostat-roles", "");
        backupRoles.toFile().deleteOnExit();
        backupUsers.toFile().deleteOnExit();
        Files.copy(new File(THERMOSTAT_USERS_FILE).toPath(), backupUsers, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(new File(THERMOSTAT_ROLES_FILE).toPath(), backupRoles, StandardCopyOption.REPLACE_EXISTING);


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
        deleteCpuData();

        server.stop();
        server.join();
        
        stopStorage();

        Files.copy(backupUsers, new File(THERMOSTAT_USERS_FILE).toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(backupRoles, new File(THERMOSTAT_ROLES_FILE).toPath(), StandardCopyOption.REPLACE_EXISTING);
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
    private static Storage getAndConnectStorage(String username, String password,
                                                String[] roleNames,
                                                ConnectionListener listener) throws IOException {
        setupJAASForUser(roleNames, username, password);
        String url = "http://localhost:" + port + "/thermostat/storage";
        StartupConfiguration config = new ConnectionConfiguration(url, username, password);
        Storage storage = new WebStorage(config);
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
        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.ACCESS_REALM,
                Roles.LOGIN,
                Roles.APPEND
        };
        Storage storage = getAndConnectStorage(PREP_USER, PREP_PASSWORD, roleNames);
        storage.registerCategory(CpuStatDAO.cpuStatCategory);

        for (int i = 0; i < numberOfItems; i++) {
            CpuStat pojo = new CpuStat(i, new double[] {i, i*2});
            pojo.setAgentId("test-agent-id");
            Add add = storage.createAdd(CpuStatDAO.cpuStatCategory);
            add.setPojo(pojo);
            add.apply();
        }

        storage.getConnection().disconnect();
    }

    private static void deleteCpuData() throws IOException {
        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.ACCESS_REALM,
                Roles.LOGIN,
                Roles.PURGE
        };
        Storage storage = getAndConnectStorage(PREP_USER, PREP_PASSWORD, roleNames);
        storage.registerCategory(CpuStatDAO.cpuStatCategory);

        storage.purge("test-agent-id");

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
    public void authorizedAdd() throws Exception {
        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.ACCESS_REALM,
                Roles.LOGIN,
                Roles.APPEND
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        webStorage.registerCategory(VmClassStatDAO.vmClassStatsCategory);
        
        Add add = webStorage.createAdd(VmClassStatDAO.vmClassStatsCategory);
        VmClassStat pojo = new VmClassStat();
        pojo.setAgentId("fluff");
        pojo.setLoadedClasses(12345);
        pojo.setTimeStamp(42);
        pojo.setVmId(VM_ID1);
        add.setPojo(pojo);
        add.apply();
        
        // Add another couple of entries
        add = webStorage.createAdd(VmClassStatDAO.vmClassStatsCategory);
        pojo = new VmClassStat();
        pojo.setAgentId("fluff");
        pojo.setLoadedClasses(67890);
        pojo.setTimeStamp(42);
        pojo.setVmId(VM_ID2);
        add.setPojo(pojo);
        add.apply();
        
        add = webStorage.createAdd(VmClassStatDAO.vmClassStatsCategory);
        pojo = new VmClassStat();
        pojo.setAgentId("fluff");
        pojo.setLoadedClasses(34567);
        pojo.setTimeStamp(42);
        pojo.setVmId(VM_ID3);
        add.setPojo(pojo);
        add.apply();

        webStorage.getConnection().disconnect();
    }
    
    @Test
    public void authorizedQuery() throws Exception {

        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT
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
    public void authorizedQueryEqualTo() throws Exception {

        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.LOGIN,
                Roles.ACCESS_REALM,
                Roles.PREPARE_STATEMENT
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
                Roles.PREPARE_STATEMENT
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
                Roles.PREPARE_STATEMENT
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
                Roles.PREPARE_STATEMENT
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
                Roles.PREPARE_STATEMENT
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
                Roles.PREPARE_STATEMENT
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
                Roles.PREPARE_STATEMENT
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
                Roles.PREPARE_STATEMENT
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
                Roles.PREPARE_STATEMENT
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
                Roles.LOGIN
        };
        Storage webStorage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        
        byte[] data = "Hello World".getBytes();
        webStorage.saveFile("test", new ByteArrayInputStream(data));
        // Note: On the server side, the file is saved into mongodb
        // via GridFS.  The save operation returns before write is
        // complete, and there is no callback mechanism to find out
        // when the write is complete.  So, we try a few times to
        // load it before considering it a failure.
        InputStream loadStream = null;
        int loadAttempts = 0;
        while (loadStream == null && loadAttempts < 3) {
            Thread.sleep(300);
            loadStream = webStorage.loadFile("test");
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
    public void setDefaultAgentID() throws Exception {
        String[] roleNames = new String[] {
                Roles.ACCESS_REALM,
                Roles.LOGIN,
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.APPEND,
                Roles.PURGE,
                Roles.PREPARE_STATEMENT
        };
        Storage storage = getAndConnectStorage(TEST_USER, TEST_PASSWORD, roleNames);
        UUID uuid = new UUID(42, 24);
        storage.setAgentId(uuid);

        storage.registerCategory(VmCpuStatDAO.vmCpuStatCategory);
        long timeStamp = 5;
        double cpuLoad = 0.15;
        VmCpuStat pojo = new VmCpuStat(timeStamp, VM_ID1, cpuLoad);
        // Note: agentId not set on pojo
        Add add = storage.createAdd(VmCpuStatDAO.vmCpuStatCategory);
        add.setPojo(pojo);
        add.apply();

        String strDesc = DESCRIPTOR_MAP.get(KEY_SET_DEFAULT_AGENT_ID);
        StatementDescriptor<VmCpuStat> queryDesc = new StatementDescriptor<>(VmCpuStatDAO.vmCpuStatCategory, strDesc);
        PreparedStatement<VmCpuStat> query = storage.prepareStatement(queryDesc);
        Cursor<VmCpuStat> cursor = query.executeQuery();
        assertTrue(cursor.hasNext());
        pojo = cursor.next();
        assertFalse(cursor.hasNext());

        assertEquals(timeStamp, pojo.getTimeStamp());
        assertEquals(VM_ID1, pojo.getVmId());
        assertEquals(cpuLoad, pojo.getCpuLoad(), EQUALS_DELTA);
        assertEquals(uuid.toString(), pojo.getAgentId());

        storage.purge(uuid.toString());
    }
}
