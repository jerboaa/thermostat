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

package com.redhat.thermostat.common;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.common.internal.LocaleResources;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FrameworkUtil.class)
public class VersionTest {

    private Locale lang;
    private Version version;
    
    @After
    public void teardown() {
        version = null;
        Locale.setDefault(lang);
    }
    
    @Before
    public void setup() {
        this.lang = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }
    
    @Test
    public void canGetVersionNumber() {
        int major = 0;
        int minor = 3;
        int micro = 1;
        Bundle sysBundle = createBundle(major, minor, micro);
        
        version = new Version(sysBundle);
        assertEquals(String.format(Version.VERSION_NUMBER_FORMAT, major, minor,
                micro), version.getVersionNumber());
    }
    
    @Test
    public void canGetVersionInfo() {
        int major = 0;
        int minor = 4;
        int micro = 1;
        Bundle sysBundle = createBundle(major, minor, micro);
        
        version = new Version(sysBundle);
        String format = createFormat();
        // sanity check format
        assertEquals(format, "Thermostat version " + Version.VERSION_NUMBER_FORMAT);
        assertEquals(String.format(format, major, minor,
                micro), version.getVersionInfo());
    }

    @Test
    public void canGetCoreVersionInfo() throws Exception {
        int major = 0;
        int minor = 4;
        int micro = 1;

        Bundle sysBundle = createBundle(major, minor, micro);

        PowerMockito.mockStatic(FrameworkUtil.class);
        when(FrameworkUtil.getBundle(Version.class)).thenReturn(sysBundle);
        version = new Version();
        String format = createFormat();
        // sanity check format
        assertEquals(format, "Thermostat version " + Version.VERSION_NUMBER_FORMAT);
        assertEquals(String.format(format, major, minor,
                micro), version.getVersionInfo());
    }
    
    @Test
    public void canGetMajor() {
        int major = 0;
        int minor = 4;
        int micro = 1;
        Bundle sysBundle = createBundle(major, minor, micro);
        
        version = new Version(sysBundle);
        assertEquals(major, version.getMajor());
    }
    
    @Test
    public void canGetMinor() {
        int major = 0;
        int minor = 0xbeef;
        int micro = 1;
        Bundle sysBundle = createBundle(major, minor, micro);
        
        version = new Version(sysBundle);
        assertEquals(minor, version.getMinor());
    }
    
    @Test
    public void canGetMicro() {
        int major = 0;
        int minor = 0xbeef;
        int micro = 10;
        Bundle sysBundle = createBundle(major, minor, micro);
        
        version = new Version(sysBundle);
        assertEquals(micro, version.getMicro());
    }
    
    private Bundle createBundle(int major, int minor, int micro) {
        String qualifier = "201207241700";
        Bundle sysBundle = mock(Bundle.class);
        org.osgi.framework.Version ver = org.osgi.framework.Version
                .parseVersion(String.format(Version.VERSION_NUMBER_FORMAT,
                        major, minor, micro) + "." + qualifier);
        when(sysBundle.getVersion()).thenReturn(ver);
        return sysBundle;
    }
 
    private String createFormat() {
        ApplicationInfo appInfo = new ApplicationInfo();
        String format = MessageFormat.format(
                LocaleResources.createLocalizer().localize(LocaleResources.APPLICATION_VERSION_INFO).getContents(),
                appInfo.getName())
                + " " + Version.VERSION_NUMBER_FORMAT;
        return format;
    }
}

