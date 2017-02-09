/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.shared.config.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import com.redhat.thermostat.testutils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.shared.config.CommonPaths;

public class SSLConfigurationImplTest {

    private SSLConfigurationImpl sslConf;

    @Before
    public void setUp() throws UnsupportedEncodingException {
        sslConf = new SSLConfigurationImpl(null);
        File clientProps = new File(decodeFilePath(this.getClass().getResource("/client.properties")));
        sslConf.initProperties(clientProps);
    }

    @Test
    public void canGetKeystoreFileFromProps() throws Exception {
        String keystorePath = "/path/to/thermostat.keystore";
        String keystorePwd = "some password";
        String absPath = sslConf.getKeystoreFile().getAbsolutePath();

        // on windows the path returned may be (probably will be) prefixed by a drive name.
        // for example "e:\arbitrary\path\foo.dll"
        // for the purposes of testing, ignore the drive letter and convert backslashes to '/'.
        absPath = TestUtils.convertWinPathToUnixPath(absPath);

        assertEquals(keystorePath, absPath);
        assertEquals(keystorePwd, sslConf.getKeyStorePassword());
    }
    
    @Test
    public void notExistingPropertiesFileReturnsNull() throws Exception {
        SSLConfigurationImpl badSSLConf = new SSLConfigurationImpl(null);
        File clientProps = new File("i/am/not/there/file.txt");
        badSSLConf.initProperties(clientProps);
        assertTrue(badSSLConf.getKeystoreFile() == null);
        assertEquals(null, badSSLConf.getKeyStorePassword());
    }
    
    @Test
    public void canGetSSLEnabledConfigs() {
        assertTrue(sslConf.enableForCmdChannel());
        assertTrue(sslConf.enableForBackingStorage());
        assertTrue(sslConf.disableHostnameVerification());
        File disabledSSLProps = new File(this.getClass().getResource("/ssl.properties").getFile());
        SSLConfigurationImpl disabledSSLConf = new SSLConfigurationImpl(null);
        disabledSSLConf.initProperties(disabledSSLProps);
        assertFalse(disabledSSLConf.enableForCmdChannel());
        assertFalse(disabledSSLConf.enableForBackingStorage());
        assertFalse(disabledSSLConf.disableHostnameVerification());
    }
    
    /*
     * $THERMOSTAT_HOME/etc/ssl.properties is specified,
     * $USER_THERMOSTAT_HOME/etc/ssl.properties not specified.
     * 
     * Thus, system ssl.properties should get used.
     */
    @Test
    public void canInitFromSystemHomeConfig() {
        File systemEtc = new File(decodeFilePath(this.getClass().getResource("/system_th_home")));
        CommonPaths paths = mock(CommonPaths.class);
        when(paths.getSystemConfigurationDirectory()).thenReturn(systemEtc);
        File userEtc = new File("/thermostat/not-existing-foo");
        
        // assert preconditions
        assertTrue("ssl.properties in system home expected to exist",
                new File(systemEtc, "ssl.properties").exists());
        assertFalse("ssl.properties in user home must not exist",
                new File(userEtc, "ssl.properties").exists());
        
        SSLConfigurationImpl config = new SSLConfigurationImpl(paths);
        // This should use system ssl.properties and use values defined there
        config.loadProperties();
        
        // Both config location should have been checked.
        verify(paths).getSystemConfigurationDirectory();
        verify(paths).getUserConfigurationDirectory();
        
        // use this assertion in order to avoid false positives if loading of
        // ssl.properties did not work, but boolean matches default values.
        assertEquals("system thermostat home", config.getKeyStorePassword());
        assertTrue(config.enableForBackingStorage());
        assertTrue(config.disableHostnameVerification());
    }
    
    /*
     * $THERMOSTAT_HOME/etc/ssl.properties is specified,
     * $USER_THERMOSTAT_HOME/etc/ssl.properties also specified.
     * 
     * Thus, user ssl.properties should get used.
     */
    @Test
    public void userHomeConfigOverridesSystem() {
        File systemEtc = new File(decodeFilePath(this.getClass().getResource("/system_th_home")));
        CommonPaths paths = mock(CommonPaths.class);
        when(paths.getSystemConfigurationDirectory()).thenReturn(systemEtc);
        File userEtc = new File(decodeFilePath(this.getClass().getResource("/user_th_home")));
        when(paths.getUserConfigurationDirectory()).thenReturn(userEtc);
        
        // assert preconditions
        assertTrue("ssl.properties in system home expected to exist",
                new File(systemEtc, "ssl.properties").exists());
        assertTrue("ssl.properties in user home expected to exist",
                new File(userEtc, "ssl.properties").exists());
        
        SSLConfigurationImpl config = new SSLConfigurationImpl(paths);
        // This should use system ssl.properties and use values defined there
        config.loadProperties();
        
        // Both config location should have been checked.
        verify(paths).getSystemConfigurationDirectory();
        verify(paths).getUserConfigurationDirectory();
        
        // use this assertion in order to avoid false positives if loading of
        // ssl.properties did not work, but boolean matches default values.
        assertEquals("user thermostat home", config.getKeyStorePassword());
        assertFalse(config.enableForBackingStorage());
        assertFalse(config.disableHostnameVerification());
    }
    
    /*
     * $THERMOSTAT_HOME/etc/ssl.properties is missing,
     * $USER_THERMOSTAT_HOME/etc/ssl.properties is specified.
     * 
     * Thus, user ssl.properties should get used and it should not fail in some
     * weird way since system ssl.properties is not existing.
     */
    @Test
    public void userHomeConfigCanBeUsedWithSystemMissing() {
        File systemEtc = new File("/thermostat/not-existing-foo");
        CommonPaths paths = mock(CommonPaths.class);
        when(paths.getSystemConfigurationDirectory()).thenReturn(systemEtc);
        File userEtc = new File(decodeFilePath(this.getClass().getResource("/user_th_home")));
        when(paths.getUserConfigurationDirectory()).thenReturn(userEtc);
        
        // assert preconditions
        assertFalse("system ssl.properties does not exist",
                new File(systemEtc, "ssl.properties").exists());
        assertTrue("ssl.properties in user home exists",
                new File(userEtc, "ssl.properties").exists());
        
        SSLConfigurationImpl config = new SSLConfigurationImpl(paths);
        // This should use system ssl.properties and use values defined there
        config.loadProperties();
        
        // Both config location should have been checked.
        verify(paths).getSystemConfigurationDirectory();
        verify(paths).getUserConfigurationDirectory();
        
        // use this assertion in order to avoid false positives if loading of
        // ssl.properties did not work, but boolean matches default values.
        assertEquals("user thermostat home", config.getKeyStorePassword());
        assertFalse(config.enableForBackingStorage());
        assertFalse(config.disableHostnameVerification());
    }
    
    /*
     * Neither ssl.properties file exist. It is expected to initialize and
     * provide reasonable default values.
     */
    @Test
    public void noSSLConfigProvidesReasonableDefaults() {
        File systemEtc = new File("/thermostat-home-system/not-existing-foo");
        CommonPaths paths = mock(CommonPaths.class);
        when(paths.getSystemConfigurationDirectory()).thenReturn(systemEtc);
        File userEtc = new File("/thermostat-home-user/not-existing-bar");
        when(paths.getUserConfigurationDirectory()).thenReturn(userEtc);
        
        // assert preconditions
        assertFalse("system ssl.properties does not exist",
                new File(systemEtc, "ssl.properties").exists());
        assertFalse("user ssl.properties does not exist",
                new File(userEtc, "ssl.properties").exists());
        
        SSLConfigurationImpl config = new SSLConfigurationImpl(paths);
        // This should use system ssl.properties and use values defined there
        config.loadProperties();
        
        // Both config location should have been checked.
        verify(paths).getSystemConfigurationDirectory();
        verify(paths).getUserConfigurationDirectory();

        // assert default values
        assertNull(config.getKeyStorePassword());
        assertNull(config.getKeystoreFile());
        assertFalse(config.enableForBackingStorage());
        assertFalse(config.enableForCmdChannel());
        assertFalse(config.disableHostnameVerification());
    }
    
    private static String decodeFilePath(URL url) {
        try {
            // Spaces are encoded as %20 in URLs. Use URLDecoder.decode() so
            // as to handle cases like that.
            return URLDecoder.decode(url.getFile(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 not supported, huh?");
        }
    }
}

