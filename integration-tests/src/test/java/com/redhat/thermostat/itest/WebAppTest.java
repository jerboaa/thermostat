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
import static com.redhat.thermostat.itest.IntegrationTest.spawnThermostat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.UUID;

import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
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

import expectj.Spawn;

public class WebAppTest {

    private static WebStorage webStorage;
    private static Server server;
    private static int port;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        Spawn storage = spawnThermostat("storage", "--start");
        storage.expect("pid:");
        storage.expectClose();

        assertNoExceptions(storage.getCurrentStandardOutContents(), storage.getCurrentStandardErrContents());

        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port);
            }
        });
        registerCategory();
    }

    @AfterClass
    public static void tearDownOnce() throws Exception {
        server.stop();
        server.join();

        Spawn storage = spawnThermostat("storage", "--stop");
        storage.expect("server shutdown complete");
        storage.expectClose();

        assertNoExceptions(storage.getCurrentStandardOutContents(), storage.getCurrentStandardErrContents());
    }

    private static void startServer(int port) throws Exception {
        server = new Server(port);
        ApplicationInfo appInfo = new ApplicationInfo();
        String version = appInfo.getMavenVersion();
        String warfile = "target/libs/thermostat-web-war-" + version + ".war";
        WebAppContext ctx = new WebAppContext(warfile, "/thermostat");
        ctx.getSecurityHandler().setAuthMethod("BASIC");
        ctx.getSecurityHandler().setLoginService(new MappedLoginService() {
            
            @Override
            protected void loadUsers() throws IOException {
                putUser("testname", new Password("testpasswd"), new String[] { "thermostat-agent" });
                putUser("test-no-role", new Password("testpasswd"), new String[] { "fluff" });
                putUser("test-cmd-channel", new Password("testpasswd"), new String[] { "thermostat-cmd-channel" });
            }
            
            @Override
            protected UserIdentity loadUser(String username) {
                if (username.equals("test-cmd-channel")) {
                    return new DefaultUserIdentity(null, null, new String[] { "thermostat-cmd-channel" });
                }
                return new DefaultUserIdentity(null, null, new String[] { "thermostat-agent" });
            }
        });
        server.setHandler(ctx);
        server.start();
    }

    private static void registerCategory() {
        String url = "http://localhost:" + port + "/thermostat/storage";
        StartupConfiguration config = new ConnectionConfiguration(url, "testname", "testpasswd");
        webStorage = new WebStorage(config);
        webStorage.setAgentId(new UUID(42, 24));
        webStorage.getConnection().connect();
        webStorage.registerCategory(VmClassStatDAO.vmClassStatsCategory);
    }

    @Test
    public void testPutFind() {
        Add add = webStorage.createAdd(VmClassStatDAO.vmClassStatsCategory);
        VmClassStat pojo = new VmClassStat();
        pojo.setAgentId("fluff");
        pojo.setLoadedClasses(12345);
        pojo.setTimeStamp(42);
        pojo.setVmId(987);
        add.setPojo(pojo);
        add.apply();

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
}
