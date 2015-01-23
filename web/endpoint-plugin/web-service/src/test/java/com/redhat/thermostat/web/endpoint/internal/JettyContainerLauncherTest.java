/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.web.endpoint.internal;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.redhat.thermostat.common.utils.HostPortPair;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.web.endpoint.internal.EmbeddedServletContainerConfiguration;
import com.redhat.thermostat.web.endpoint.internal.JettyContainerLauncher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

public class JettyContainerLauncherTest {

    @Test
    public void startFailsIfWebArchivePathDoesNotExist() throws InterruptedException {
        EmbeddedServletContainerConfiguration config = mock(EmbeddedServletContainerConfiguration.class);
        File notThere = new File("/path/not-here-go-away");
        when(config.getAbsolutePathToExplodedWebArchive()).thenReturn(notThere);
        HostPortPair hostPort = new HostPortPair("foo", 1111);
        when(config.getHostsPortsConfig()).thenReturn(hostPort);
        assertFalse(notThere.exists());
        JettyContainerLauncher launcher = new JettyContainerLauncher(config, mock(SSLConfiguration.class));
        CountDownLatch latch = new CountDownLatch(1);
        launcher.startContainer(latch);
        latch.await();
        assertFalse(launcher.isStartupSuccessFul());
    }
    
    @Test
    public void canConfigureJaasNoPropSet() {
        try {
            assertNull("Precondition failed!", System.getProperty(JettyContainerLauncher.JAAS_CONFIG_PROP));
            EmbeddedServletContainerConfiguration config = mock(EmbeddedServletContainerConfiguration.class);
            when(config.getAbsolutePathToJaasConfig()).thenReturn("/foo/jaas.conf");
            JettyContainerLauncher launcher = new JettyContainerLauncher(config, mock(SSLConfiguration.class));
            launcher.configureJaas();
            assertEquals("/foo/jaas.conf", System.getProperty(JettyContainerLauncher.JAAS_CONFIG_PROP));
        } finally {
            System.clearProperty(JettyContainerLauncher.JAAS_CONFIG_PROP);
        }
    }
    
    // A preconfigured jaas property should be left untouched.
    @Test
    public void canConfigureJaasWithPropSet() {
        try {
            EmbeddedServletContainerConfiguration config = mock(EmbeddedServletContainerConfiguration.class);
            System.setProperty(JettyContainerLauncher.JAAS_CONFIG_PROP, "foo_jaas.conf");
            JettyContainerLauncher launcher = new JettyContainerLauncher(config, mock(SSLConfiguration.class));
            launcher.configureJaas();
            verify(config, times(0)).getAbsolutePathToJaasConfig();
            assertEquals("foo_jaas.conf", System.getProperty(JettyContainerLauncher.JAAS_CONFIG_PROP));
        } finally {
            System.clearProperty(JettyContainerLauncher.JAAS_CONFIG_PROP);
        }
    }
}
