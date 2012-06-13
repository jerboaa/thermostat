/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.agent.command;

import java.net.InetSocketAddress;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.command.ConfigurationServer;
import com.redhat.thermostat.agent.command.ConfigurationServerContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigurationServerTest {

    private ConfigurationServerContext ctx;
    ChannelGroup cg;
    ServerBootstrap bootstrap;
    InetSocketAddress addr;

    @Before
    public void setUp() {
        cg = mock(ChannelGroup.class);
        ChannelGroupFuture future = mock(ChannelGroupFuture.class);
        when(cg.close()).thenReturn(future);
        bootstrap = mock(ServerBootstrap.class);
        addr = new InetSocketAddress(123);
        ctx = mock(ConfigurationServerContext.class);
        when(ctx.getBootstrap()).thenReturn(bootstrap);
        when(ctx.getChannelGroup()).thenReturn(cg);
        when(ctx.getAddress()).thenReturn(addr);
    }

    @Test
    public void testStartup() {
        ConfigurationServer server = new ConfigurationServer(ctx);
        server.startup();


        ArgumentCaptor<InetSocketAddress> argument = ArgumentCaptor.forClass(InetSocketAddress.class);
        verify(bootstrap).bind(argument.capture());
        assertEquals(addr, argument.getValue());
    }

    @Test
    public void testShutdown() {
        ConfigurationServer server = new ConfigurationServer(ctx);
        server.shutdown();

        verify(cg).close();
    }
    
}
