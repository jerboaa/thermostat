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

package com.redhat.thermostat.web.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Test;

import com.redhat.thermostat.web.server.auth.WebStoragePathHandler;

/**
 * A test class for {@link WebStorageEndPoint}. It should contain tests for
 * which we don't need the servlet deployed in a container. I.e. which are
 * more of the unit-test nature (rather than of the functional test nature).
 *
 */
public class WebStorageEndPointUnitTest {

    @After
    public void tearDown() {
        System.clearProperty("THERMOSTAT_HOME");
    }
    
    /*
     * Tests whether isThermostatHomeSet() works as expected.
     * 
     * In particular, this tests sets thermostat home to a known-to-be-read-only
     * for non-root users. That will make isThermostatHomeSet() return false
     * due to creating paths failing.
     * 
     * Regression test for the following bug:
     * http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=1637
     */
    @Test
    public void testCheckThermostatHome() {
        System.setProperty("THERMOSTAT_HOME", "/root");
        WebStorageEndPoint endpoint = new WebStorageEndPoint();
        assertTrue("THERMOSTAT_HOME clearly set, do we create paths where we shouldn't?",
                endpoint.isThermostatHomeSet());
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
                "save-file", "load-file", "purge", "ping", "generate-token", "verify-token"
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
            assertTrue(e.getMessage().contains("THERMOSTAT_HOME"));
        } catch (ServletException e) {
            fail(e.getMessage());
        }
        // set config with non-existing dir
        when(config.getInitParameter("THERMOSTAT_HOME")).thenReturn("not-existing");
        try {
            endpoint.init(config);
            fail("Thermostat home was set in config but file does not exist, should have died!");
        } catch (RuntimeException e) {
            // pass
            assertTrue(e.getMessage().contains("THERMOSTAT_HOME"));
        } catch (ServletException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void initThrowsRuntimeExceptionIfSSLPropertiesNotReadable() throws Exception {
        Path testThermostatHome = null;
        File etcDir = null;
        try {
            testThermostatHome = Files.createTempDirectory(
                    "foo-thermostat-home-", new FileAttribute[] {});
            File thFile = testThermostatHome.toFile();
            etcDir = new File(thFile, "etc");
            etcDir.mkdir();
            assertTrue(etcDir.exists());
            assertTrue(etcDir.canWrite());
            File sslProperties = new File(etcDir, "ssl.properties");
            sslProperties.createNewFile();
            assertTrue(sslProperties.canRead());
            // explicitly remove read perms from etc directory
            etcDir.setExecutable(false);
            assertFalse(sslProperties.canRead());

            WebStorageEndPoint endpoint = new WebStorageEndPoint();
            System.setProperty("THERMOSTAT_HOME", thFile.getAbsolutePath());
            try {
                endpoint.init(mock(ServletConfig.class));
                fail("should have failed to initialize! can't read ssl.properties");
            } catch (RuntimeException e) {
                assertTrue(e.getMessage().contains("ssl.properties"));
            }
        } finally {
            etcDir.setExecutable(true);
            if (testThermostatHome != null) {
                deleteDirectoryRecursive(testThermostatHome);
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
            System.setProperty("THERMOSTAT_HOME", thFile.getAbsolutePath());
            try {
                endpoint.init(mock(ServletConfig.class));
                fail("should have failed to initialize, ssl.properties not existing!");
            } catch (RuntimeException e) {
                assertTrue(e.getMessage().contains("ssl.properties"));
            }
        } finally {
            if (testThermostatHome != null) {
                deleteDirectoryRecursive(testThermostatHome);
            }
        }
    }
    
    private void deleteDirectoryRecursive(Path dir) throws IOException {
        Files.walkFileTree(dir, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
                // nothing
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
                exc.printStackTrace();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                // All files have been visitated before that.
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
            
        });
    }
}
