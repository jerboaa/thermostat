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

package com.redhat.thermostat.web.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.eq;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.redhat.thermostat.common.internal.test.Bug;
import com.redhat.thermostat.storage.core.Storage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.web.server.auth.WebStoragePathHandler;

/**
 * A test class for {@link WebStorageEndPoint}. It should contain tests for
 * which we don't need the servlet deployed in a container. I.e. which are
 * more of the unit-test nature (rather than of the functional test nature).
 *
 */
public class WebStorageEndPointUnitTest {

    private static final String TH_HOME_PROP_NAME = "THERMOSTAT_HOME";

    private StorageFactoryProvider storageFactoryProvider;
    private StorageFactory storageFactory;
    private Storage storage;

    @Before
    public void setup() {
        storage = mock(Storage.class);
        storageFactory = mock(StorageFactory.class);
        storageFactoryProvider = mock(StorageFactoryProvider.class);

        when(storageFactoryProvider.createStorageFactory()).thenReturn(storageFactory);
        when(storageFactory.getStorage(anyString(), anyString(), any(CommonPaths.class), any(StorageCredentials.class)))
                .thenReturn(storage);
    }

    @After
    public void tearDown() {
        System.clearProperty(TH_HOME_PROP_NAME);
    }
    
    /**
     * Makes sure that all paths we dispatch to, dispatch to
     * {@link WebStoragePathHandler} annotated methods.
     * 
     * @throws Exception
     */
    @Test
    public void ensureAuthorizationCovered() throws Exception {
        // manually maintained list of path handlers which should include
        // authorization checks
        final String[] authPaths = new String[] {
                "prepare-statement", "query-execute", "write-execute", "register-category",
                "save-file", "load-file", "purge", "ping", "generate-token", "verify-token",
                "get-more"
        };
        Map<String, Boolean> checkedAutPaths = new HashMap<>();
        for (String path: authPaths) {
            checkedAutPaths.put(path, false);
        }
        int methodsReqAuthorization = 0;
        for (Method method: WebStorageEndPoint.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(WebStoragePathHandler.class)) {
                methodsReqAuthorization++;
                WebStoragePathHandler annot = method.getAnnotation(WebStoragePathHandler.class);
                try {
                    // this may NPE if there is something funny going on in
                    // WebStorageEndPoint (e.g. one method annotated but this
                    // reference list has not been updated).
                    if (!checkedAutPaths.get(annot.path())) {
                        // mark path as covered
                        checkedAutPaths.put(annot.path(), true);
                    } else {
                        throw new AssertionError(
                                "method "
                                        + method
                                        + " annotated as web storage path handler (path '"
                                        + annot.path()
                                        + "'), but not in reference list we know about!");
                    }
                } catch (NullPointerException e) {
                    throw new AssertionError("Don't know about path '"
                            + annot.path() + "'");
                }
            }
        }
        // at this point we should have all dispatched paths covered
        for (String path: authPaths) {
            assertTrue(
                    "Is " + path
                          + " marked with @WebStoragePathHandler and have proper authorization checks been included?",
                    checkedAutPaths.get(path));
        }
        assertEquals(authPaths.length, methodsReqAuthorization);
    }
    
    @Test
    public void initThrowsRuntimeExceptionIfThermostatHomeNotSet() {
        WebStorageEndPoint endpoint = new WebStorageEndPoint();
        ServletConfig config = mock(ServletConfig.class);
        try {
            endpoint.init(config);
            fail("Thermostat home was not set in config, should not get here!");
        } catch (RuntimeException e) {
            // pass
            assertTrue(e.getMessage().contains(TH_HOME_PROP_NAME));
        } catch (ServletException e) {
            fail(e.getMessage());
        }
        // set config with non-existing dir
        when(config.getInitParameter(TH_HOME_PROP_NAME)).thenReturn("not-existing");
        try {
            endpoint.init(config);
            fail("Thermostat home was set in config but file does not exist, should have died!");
        } catch (RuntimeException e) {
            // pass
            assertTrue(e.getMessage().contains(TH_HOME_PROP_NAME));
        } catch (ServletException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void initThrowsRuntimeExceptionIfSSLPropertiesNotReadable() throws Exception {
        ThCreatorResult result = null;
        try {
            result = creatWorkingThermostatHome();
            // explicitly remove read perms from etc directory
            result.etcDir.setExecutable(false);
            assertFalse(result.sslProperties.canRead());

            WebStorageEndPoint endpoint = new WebStorageEndPoint();
            System.setProperty(TH_HOME_PROP_NAME, result.thermostatHome.toFile().getAbsolutePath());
            try {
                endpoint.init(mock(ServletConfig.class));
                fail("should have failed to initialize! can't read ssl.properties");
            } catch (RuntimeException e) {
                assertTrue(e.getMessage().contains("ssl.properties"));
            }
        } finally {
            result.etcDir.setExecutable(true);
            if (result.thermostatHome != null) {
                WebstorageEndpointTestUtils.deleteDirectoryRecursive(result.thermostatHome);
            }
        }
    }
    
    @Test
    public void initThrowsRuntimeExceptionIfSSLPropertiesDoesnotExist() throws Exception {
        Path testThermostatHome = null;
        try {
            testThermostatHome = Files.createTempDirectory(
                    "bar-thermostat-home-", new FileAttribute[] {});
            File thFile = testThermostatHome.toFile();
            WebStorageEndPoint endpoint = new WebStorageEndPoint();
            System.setProperty(TH_HOME_PROP_NAME, thFile.getAbsolutePath());
            try {
                endpoint.init(mock(ServletConfig.class));
                fail("should have failed to initialize, ssl.properties not existing!");
            } catch (RuntimeException e) {
                assertTrue(e.getMessage().contains("ssl.properties"));
            }
        } finally {
            if (testThermostatHome != null) {
                WebstorageEndpointTestUtils.deleteDirectoryRecursive(testThermostatHome);
            }
        }
    }
    
    /**
     * Verifies that Servlet.init() sets servlet context attributes correctly.
     * @throws ServletException 
     * @throws IOException 
     */
    @Test
    public void testSetServletAttribute() throws ServletException, IOException {
        final ServletContext mockContext = mock(ServletContext.class);
        when(mockContext.getServerInfo()).thenReturn("jetty/9.1.0.v20131115");
        ConfigurationFinder finder = mock(ConfigurationFinder.class);
        when(finder.getConfiguration(anyString())).thenReturn(mock(File.class));
        @SuppressWarnings("serial")
        WebStorageEndPoint endpoint = new WebStorageEndPoint(null, null, finder, storageFactoryProvider) {
            @Override
            public ServletContext getServletContext() {
                return mockContext;
            }
        };
        ServletConfig config = mock(ServletConfig.class);
        when(config.getInitParameter(WebStorageEndPoint.STORAGE_CLASS)).thenReturn("fooKlazz"); // let it fail through
        when(config.getInitParameter(WebStorageEndPoint.STORAGE_ENDPOINT)).thenReturn("fooEndPoint");
        ThCreatorResult result = creatWorkingThermostatHome();
        System.setProperty(TH_HOME_PROP_NAME, result.thermostatHome.toFile().getAbsolutePath());
        endpoint.init(config);
        ArgumentCaptor<CategoryManager> categoryManagerCaptor = ArgumentCaptor.forClass(CategoryManager.class);
        ArgumentCaptor<PreparedStatementManager> prepStmtManagerCaptor = ArgumentCaptor.forClass(PreparedStatementManager.class);
        ArgumentCaptor<TokenManager> tokenManagerCaptor = ArgumentCaptor.forClass(TokenManager.class);
        ArgumentCaptor<UUID> serverTokenCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(mockContext).setAttribute(eq("category-manager"), categoryManagerCaptor.capture());
        verify(mockContext).setAttribute(eq("prepared-stmt-manager"), prepStmtManagerCaptor.capture());
        verify(mockContext).setAttribute(eq("token-manager"), tokenManagerCaptor.capture());
        verify(mockContext).setAttribute(eq("server-token"), serverTokenCaptor.capture());
        assertNotNull(categoryManagerCaptor.getValue());
        assertNotNull(prepStmtManagerCaptor.getValue());
        assertNotNull(tokenManagerCaptor.getValue());
        assertNotNull(serverTokenCaptor.getValue());
    }
    
    @Test
    public void testShutDownCancelsTimers() {
        TimerRegistry registry = mock(TimerRegistry.class);
        WebStorageEndPoint endpoint = new WebStorageEndPoint(registry, null, null, storageFactoryProvider);
        endpoint.destroy();
        verify(registry).shutDown();
    }
    
    /**
     * If storage credentials are not found then null is expected to get returned.
     */
    @Test
    public void storageCredentialsNull() throws IOException {
        CommonPaths paths = mock(CommonPaths.class);
        TimerRegistry registry = mock(TimerRegistry.class);
        ConfigurationFinder finder = mock(ConfigurationFinder.class);
        when(finder.getConfiguration("web.auth")).thenReturn(null);

        WebStorageEndPoint endpoint = new WebStorageEndPoint(registry, paths, finder, storageFactoryProvider);
        StorageCredentials creds = endpoint.getStorageCredentials();

        assertNull(creds);
    }

    @Test
    @Bug(id = "PR2941",
            url = "http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=2941",
            summary = "Concurrent webstorage connections may cause storage exceptions")
    public void testStorageIsCreatedOnceOnInit() throws Exception {
        final ServletContext mockContext = mock(ServletContext.class);
        when(mockContext.getServerInfo()).thenReturn("jetty/9.1.0.v20131115");
        ConfigurationFinder finder = mock(ConfigurationFinder.class);
        when(finder.getConfiguration(anyString())).thenReturn(mock(File.class));
        @SuppressWarnings("serial")
        WebStorageEndPoint endpoint = new WebStorageEndPoint(null, null, finder, storageFactoryProvider) {
            @Override
            public ServletContext getServletContext() {
                return mockContext;
            }
        };
        ServletConfig config = mock(ServletConfig.class);
        when(config.getInitParameter(WebStorageEndPoint.STORAGE_CLASS)).thenReturn("fooKlazz"); // let it fail through
        when(config.getInitParameter(WebStorageEndPoint.STORAGE_ENDPOINT)).thenReturn("fooEndPoint");
        ThCreatorResult result = creatWorkingThermostatHome();
        System.setProperty(TH_HOME_PROP_NAME, result.thermostatHome.toFile().getAbsolutePath());

        // not created yet
        verifyZeroInteractions(storageFactoryProvider);
        verifyZeroInteractions(storageFactory);

        endpoint.init(mock(ServletConfig.class));

        // created once
        verify(storageFactoryProvider).createStorageFactory();
        verify(storageFactory).getStorage(anyString(), anyString(), any(CommonPaths.class), any(StorageCredentials.class));

        endpoint.init(mock(ServletConfig.class));

        // still only once
        verify(storageFactoryProvider).createStorageFactory();
        verify(storageFactory).getStorage(anyString(), anyString(), any(CommonPaths.class), any(StorageCredentials.class));
    }

    private ThCreatorResult creatWorkingThermostatHome() throws IOException {
        Path testThermostatHome = Files.createTempDirectory(
                "foo-thermostat-home-", new FileAttribute[] {});
        File thFile = testThermostatHome.toFile();
        File etcDir = new File(thFile, "etc");
        etcDir.mkdir();
        assertTrue(etcDir.exists());
        assertTrue(etcDir.canWrite());
        File sslProperties = new File(etcDir, "ssl.properties");
        sslProperties.createNewFile();
        assertTrue(sslProperties.canRead());
        return new ThCreatorResult(testThermostatHome, etcDir, sslProperties);
    }
    
    private static class ThCreatorResult {
        private final Path thermostatHome;
        private final File etcDir;
        private final File sslProperties;
        
        ThCreatorResult(Path thermostatHome, File etcFile, File sslProperties) {
            this.thermostatHome = thermostatHome;
            this.etcDir = etcFile;
            this.sslProperties = sslProperties;
        }
    }
    
}
