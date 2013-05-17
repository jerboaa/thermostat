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

import static com.redhat.thermostat.itest.IntegrationTest.assertNoExceptions;
import static com.redhat.thermostat.itest.IntegrationTest.deleteFilesUnder;
import static com.redhat.thermostat.itest.IntegrationTest.getStorageDataDirectory;
import static com.redhat.thermostat.itest.IntegrationTest.getThermostatHome;
import static com.redhat.thermostat.itest.IntegrationTest.spawnThermostat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

import javax.security.auth.login.LoginException;

import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.common.ApplicationInfo;
import com.redhat.thermostat.storage.config.ConnectionConfiguration;
import com.redhat.thermostat.storage.config.StartupConfiguration;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.test.FreePortFinder;
import com.redhat.thermostat.test.FreePortFinder.TryPort;
import com.redhat.thermostat.vm.classstat.common.VmClassStatDAO;
import com.redhat.thermostat.vm.classstat.common.model.VmClassStat;
import com.redhat.thermostat.web.client.internal.WebStorage;
import com.redhat.thermostat.web.server.auth.Roles;

import expectj.Spawn;

public class WebAppTest {

    private static final String THERMOSTAT_USERS_FILE = getThermostatHome() +
            "/etc/thermostat-users.properties";
    private static final String THERMOSTAT_ROLES_FILE = getThermostatHome() +
            "/etc/thermostat-roles.properties";
    private static WebStorage webStorage;
    private static Server server;
    private static int port;
    private static Path backupUsers;
    private static Path backupRoles;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        String staleDataDir = getStorageDataDirectory();
        deleteFilesUnder(staleDataDir);

        Spawn storage = spawnThermostat("storage", "--start");
        storage.expect("pid:");
        storage.expectClose();

        assertNoExceptions(storage.getCurrentStandardOutContents(), storage.getCurrentStandardErrContents());
        backupUsers = Files.createTempFile("itest-backup-thermostat-users", "");
        backupRoles = Files.createTempFile("itest-backup-thermostat-roles", "");
        backupRoles.toFile().deleteOnExit();
        backupUsers.toFile().deleteOnExit();
        Files.copy(new File(THERMOSTAT_USERS_FILE).toPath(), backupUsers, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(new File(THERMOSTAT_ROLES_FILE).toPath(), backupRoles, StandardCopyOption.REPLACE_EXISTING);
    }
    
    private void writeThermostatUsersRolesFile(Properties usersContent, Properties rolesContent) throws IOException {
        File thermostatUsers = new File(THERMOSTAT_USERS_FILE);
        File thermostatRoles = new File(THERMOSTAT_ROLES_FILE);
        try (FileOutputStream usersStream = new FileOutputStream(thermostatUsers)) {
            usersContent.store(usersStream, "integration-test users");
        }
        try (FileOutputStream rolesStream = new FileOutputStream(thermostatRoles)) {
            rolesContent.store(rolesStream, "integration-test roles");
        }
    }
    
    @Before
    public void setup() throws Exception {
        // start the server, deploy the war
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port);
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        server.join();
    }

    @AfterClass
    public static void tearDownOnce() throws Exception {

        Spawn storage = spawnThermostat("storage", "--stop");
        storage.expect("server shutdown complete");
        storage.expectClose();

        assertNoExceptions(storage.getCurrentStandardOutContents(), storage.getCurrentStandardErrContents());
        Files.copy(backupUsers, new File(THERMOSTAT_USERS_FILE).toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(backupRoles, new File(THERMOSTAT_ROLES_FILE).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void startServer(int port) throws Exception {
        server = new Server(port);
        ApplicationInfo appInfo = new ApplicationInfo();
        String version = appInfo.getMavenVersion();
        String warfile = "target/libs/thermostat-web-war-" + version + ".war";
        WebAppContext ctx = new WebAppContext(warfile, "/thermostat");
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
    }

    private static void connectStorage(String username, String password) {
        String url = "http://localhost:" + port + "/thermostat/storage";
        StartupConfiguration config = new ConnectionConfiguration(url, username, password);
        webStorage = new WebStorage(config);
        webStorage.setAgentId(new UUID(42, 24));
        webStorage.getConnection().connect();
    }

    private void setupJAASForUser(String[] roleNames, String testuser,
            String password) throws IOException {
        Properties userProps = new Properties();
        userProps.put(testuser, password);
        Properties roleProps = new Properties();
        StringBuffer roles = new StringBuffer();
        for (int i = 0; i < roleNames.length - 1; i++) {
            roles.append(roleNames[i] + ", ");
        }
        roles.append(roleNames[roleNames.length - 1]);
        roleProps.put(testuser, roles.toString());
        writeThermostatUsersRolesFile(userProps, roleProps);
    }

    @Test
    public void authorizedAdd() throws Exception {
        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.ACCESS_REALM,
                Roles.LOGIN,
                Roles.APPEND
        };
        String testuser = "testuser";
        String password = "testpassword";
        setupJAASForUser(roleNames, testuser, password);
        
        connectStorage(testuser, password);
        webStorage.registerCategory(VmClassStatDAO.vmClassStatsCategory);
        
        Add add = webStorage.createAdd(VmClassStatDAO.vmClassStatsCategory);
        VmClassStat pojo = new VmClassStat();
        pojo.setAgentId("fluff");
        pojo.setLoadedClasses(12345);
        pojo.setTimeStamp(42);
        pojo.setVmId(987);
        add.setPojo(pojo);
        add.apply();
    }

    @Test
    public void authorizedQuery() throws Exception {
        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.READ,
                Roles.LOGIN,
                Roles.ACCESS_REALM
        };
        String testuser = "testuser";
        String password = "testpassword";
        setupJAASForUser(roleNames, testuser, password);
        connectStorage(testuser, password);
        webStorage.registerCategory(VmClassStatDAO.vmClassStatsCategory);
        
        Query<VmClassStat> query = webStorage.createQuery(VmClassStatDAO.vmClassStatsCategory);
        Cursor<VmClassStat> cursor = query.execute();
        assertTrue(cursor.hasNext());
        VmClassStat foundPojo = cursor.next();
        assertEquals("fluff", foundPojo.getAgentId());
        assertEquals(42, foundPojo.getTimeStamp());
        assertEquals(987, foundPojo.getVmId());
        assertEquals(12345, foundPojo.getLoadedClasses());
        assertFalse(cursor.hasNext());
    }

    @Test
    public void authorizedLoadSave() throws Exception {
        String[] roleNames = new String[] {
                Roles.LOAD_FILE,
                Roles.SAVE_FILE,
                Roles.ACCESS_REALM,
                Roles.LOGIN
        };
        String testuser = "testuser";
        String password = "testpassword";
        setupJAASForUser(roleNames, testuser, password);
        connectStorage(testuser, password);
        
        byte[] data = "Hello World".getBytes();
        webStorage.saveFile("test", new ByteArrayInputStream(data));
        // Note: Apparently, saving the file takes a bit. Without this
        // waiting, we sometimes get problems on loadFile. There seems
        // to be no way to synchronize on the operation in Mongo.
        Thread.sleep(300);
        InputStream loadStream = webStorage.loadFile("test");
        StringBuilder str = new StringBuilder();
        int i = loadStream.read();
        while (i != -1) {
            str.append((char) i);
            i = loadStream.read();
        }
        assertEquals("Hello World", str.toString());
    }
    
    private static class TestLoginService extends MappedLoginService {

        private final String[] roleNames;
        private final String username;
        private final String password;

        private TestLoginService(String username, String password,
                String[] roleNames) {
            this.username = username;
            this.password = password;
            this.roleNames = roleNames;
        }

        @Override
        protected void loadUsers() throws IOException {
            putUser(username, new Password(password),
                    roleNames);
        }

        @Override
        protected UserIdentity loadUser(String username) {
            return new DefaultUserIdentity(null, null,
                    roleNames);
        }
    }
}
