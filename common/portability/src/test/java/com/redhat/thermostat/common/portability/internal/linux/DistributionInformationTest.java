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

package com.redhat.thermostat.common.portability.internal.linux;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Logger;

import com.redhat.thermostat.shared.config.OS;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DistributionInformationTest {
    
    private Logger logger;
    private TestLogHandler handler;
    
    @Before
    public void setup() {
        setupTestLogger();
    }
    
    @After
    public void tearDown() {
        if (handler != null) {
            logger.removeHandler(handler);
            handler = null;
        }
    }
    
    private void setupTestLogger() {
        logger = Logger.getLogger("com.redhat.thermostat");
        handler = new TestLogHandler();
        logger.addHandler(handler);
    }
    
    /*
     * Verifies that no warning gets logged if EtcOsRelease fails, but
     * LsbRelease works. Since LsbRelease is the fallback, all is well.
     * 
     * see: http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=1628
     */
    @Test
    public void testNoWarningLoggedOnFallback() {
        // verify preconditions
        assertFalse(handler.isEtcOsReleaseLogged());
        assertTestHandlerRegistered();
        
        // Default LSB release
        LsbRelease lsbRelease = new LsbRelease();
        // EtcOsRelease with non existent file
        EtcOsRelease etcOsRelease = new EtcOsRelease(EtcOsReleaseTest.NOT_EXISTING_OS_RELEASE_FILE);
        DistributionInformation.get(etcOsRelease, lsbRelease);
        assertFalse(handler.isEtcOsReleaseLogged());
    }
    
    /*
     * Verifies that a warning gets logged if os-release and lsb_release both
     * fail.
     * 
     * see: http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=1628
     */
    @Test
    public void testWarningLoggedIfBothFail() {
        // verify preconditions
        assertFalse(handler.isEtcOsReleaseLogged());
        assertFalse(handler.isLsbReleaseLogged());
        assertTestHandlerRegistered();
        
        // both etc-os-release and lsb-release don't exist for this test
        EtcOsRelease etcOsRelease = new EtcOsRelease(EtcOsReleaseTest.NOT_EXISTING_OS_RELEASE_FILE);
        LsbRelease lsbRelease = new LsbRelease(LsbReleaseTest.NOT_EXISTING_LSB_RELEASE);
        
        DistributionInformation info = DistributionInformation.get(etcOsRelease, lsbRelease);
        assertFalse(handler.isEtcOsReleaseLogged());
        assertTrue(handler.isLsbReleaseLogged());
        assertNotNull(info);
        assertEquals(DistributionInformation.UNKNOWN_NAME, info.getName());
        assertEquals(DistributionInformation.UNKNOWN_VERSION, info.getVersion());
    }
    
    @Test
    public void verifyFallbackToLsbWhenEtcOsReturnsUnknown() throws IOException {
        EtcOsRelease mockEtcOsRelease = mock(EtcOsRelease.class);
        DistributionInformation mockDistro = mock(DistributionInformation.class);
        when(mockEtcOsRelease.getDistributionInformation()).thenReturn(mockDistro);
        when(mockDistro.getName()).thenReturn(DistributionInformation.UNKNOWN_NAME);
        when(mockDistro.getVersion()).thenReturn(DistributionInformation.UNKNOWN_VERSION);
        
        LsbRelease mockLsbRelease = mock(LsbRelease.class);
        DistributionInformation mockLsbDistro = mock(DistributionInformation.class);
        when(mockLsbRelease.getDistributionInformation()).thenReturn(mockLsbDistro);
        
        DistributionInformation info = DistributionInformation.get(mockEtcOsRelease, mockLsbRelease);
        assertSame("Expected lsb info to be used since etc returns unknown",
                   mockLsbDistro, info);
    }

    private void assertTestHandlerRegistered() {
        assertNotNull(logger);
        boolean testLogHandlerRegistered = false;
        for (Handler h: logger.getHandlers()) {
            if (h instanceof TestLogHandler) {
                testLogHandlerRegistered = true;
            }
        }
        assertTrue(testLogHandlerRegistered);
    }

    @Test
    public void testName() {
        if (OS.IS_LINUX) {
            DistributionInformation info = DistributionInformation.get();
            String name = info.getName();
            assertNotNull(name);
            assertTrue(name.length() > 0);
            assertFalse(name.startsWith(":"));
            assertFalse(name.equals(DistributionInformation.UNKNOWN_NAME));
        }
    }

    @Test
    public void testVersion() {
        if (OS.IS_LINUX) {
            DistributionInformation info = DistributionInformation.get();
            String version = info.getVersion();
            assertNotNull(version);
            assertTrue(version.length()> 0);
            assertFalse(version.startsWith(":"));
            assertFalse(version.equals(DistributionInformation.UNKNOWN_VERSION));
        }
    }

}

