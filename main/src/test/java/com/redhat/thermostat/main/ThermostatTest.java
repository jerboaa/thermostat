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

package com.redhat.thermostat.main;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.main.impl.FrameworkProvider;
import com.redhat.thermostat.shared.config.CommonPaths;

public class ThermostatTest {

    private FrameworkProvider provider;
    private CommonPaths paths;
    private ArgumentCaptor<Boolean> printOsgiInfoCaptor;
    private ArgumentCaptor<Boolean> ignoreBundleVersionsCaptor;
    private Thermostat thermostat;

    @Before
    public void setUp() {
        provider = mock(FrameworkProvider.class);
        paths = mock(CommonPaths.class);

        printOsgiInfoCaptor = ArgumentCaptor.forClass(Boolean.class);
        ignoreBundleVersionsCaptor = ArgumentCaptor.forClass(Boolean.class);

        thermostat = mock(Thermostat.class);

        when(thermostat
                .createFrameworkProvider(eq(paths), printOsgiInfoCaptor.capture(), ignoreBundleVersionsCaptor.capture()))
                .thenReturn(provider);
        doCallRealMethod().when(thermostat).start(eq(paths), any(String[].class));

    }

    @Test
    public void verifyNoArgDoesNotPrintOsgiInfo() {
        String[] args = {};
        thermostat.start(paths, args);

        verify(provider).start(eq(new String[]{}));
        assertEquals(false, printOsgiInfoCaptor.getValue());
    }

    @Test
    public void verifyArgPrintsOsgiInfo() {
        String[] args = {"--print-osgi-info"};
        thermostat.start(paths, args);

        verify(provider).start(eq(new String[]{}));
        assertEquals(true, printOsgiInfoCaptor.getValue());
    }

    @Test
    public void verifyNoArgDoesNotIgnoreOsgiVersions() {
        String[] args = {};
        thermostat.start(paths, args);

        verify(provider).start(eq(new String[]{}));
        assertEquals(false, ignoreBundleVersionsCaptor.getValue());
    }

    @Test
    public void verifyArgIgnoresBundleVersions() {
        String[] args = {"--ignore-bundle-versions"};
        thermostat.start(paths, args);

        verify(provider).start(eq(new String[]{}));
        assertEquals(true, ignoreBundleVersionsCaptor.getValue());
    }

    @Test
    public void verifyUnrecognizedArgsArePassedAlong() {
        String[] args = {"--new-super-arg", "unknown"};
        thermostat.start(paths, args);

        verify(provider).start(eq(args));
    }
}

