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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.command.ConfigurationServer;
import com.redhat.thermostat.agent.command.ConfigurationServerContext;
import com.redhat.thermostat.agent.command.RequestDecoder;
import com.redhat.thermostat.agent.command.ResponseEncoder;
import com.redhat.thermostat.agent.command.ServerHandler;

public class ConfigurationServerContextTest {

    ConfigurationServerContext ctx;

    @Before
    public void setUp() {
        ctx = new ConfigurationServerContext();
    }

    @Test
    public void testBootstrap() {
        Bootstrap bootstrap = ctx.getBootstrap();
        assertNotNull(bootstrap);

        assertTrue((Boolean) bootstrap.getOption("child.tcpNoDelay"));
        assertTrue((Boolean) bootstrap.getOption("child.keepAlive"));
        assertTrue((Boolean) bootstrap.getOption("child.reuseAddress"));
        assertEquals(100, bootstrap.getOption("child.connectTimeoutMillis"));
        assertTrue((Boolean) bootstrap.getOption("child.readWriteFair"));

        ChannelPipelineFactory pf = bootstrap.getPipelineFactory();
        assertNotNull(pf);

        ChannelPipeline p = null;
        try {
            p = pf.getPipeline();
        } catch (Exception e) {
            // Do nothing
        }
        assertNotNull(p);

        ChannelHandler encoder = p.get("encoder");
        assertNotNull(encoder);
        assertTrue(encoder instanceof ResponseEncoder);

        ChannelHandler decoder = p.get("decoder");
        assertNotNull(decoder);
        assertTrue(decoder instanceof RequestDecoder);

        ChannelHandler handler = p.get("handler");
        assertNotNull(handler);
        assertTrue(handler instanceof ServerHandler);
    }

    @Test
    public void testChannelGroup() {
        ChannelGroup cg = ctx.getChannelGroup();
        assertNotNull(cg);
        assertEquals(ConfigurationServer.class.getName(), cg.getName());
    }
}
