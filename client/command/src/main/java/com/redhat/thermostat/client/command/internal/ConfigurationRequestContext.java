/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.client.command.internal;

import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.ssl.SslHandler;

import com.redhat.thermostat.common.command.ConfigurationCommandContext;
import com.redhat.thermostat.common.ssl.SSLContextFactory;
import com.redhat.thermostat.common.ssl.SSLConfiguration;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class ConfigurationRequestContext implements ConfigurationCommandContext {
    
    private static final Logger logger = LoggingUtils.getLogger(ConfigurationRequestContext.class);

    private final ClientBootstrap bootstrap;

    ConfigurationRequestContext() {
        this.bootstrap = createBootstrap();
    }

    @Override
    public
    Bootstrap getBootstrap() {
        return bootstrap;
    }

    private ClientBootstrap createBootstrap() {
        // Configure the client.
        ClientBootstrap bootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        // Set up the pipeline factory.
        bootstrap.setPipelineFactory(new ClientPipelineFactory());

        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
        bootstrap.setOption("reuseAddress", true);
        bootstrap.setOption("connectTimeoutMillis", 100);
        bootstrap.setOption("readWriteFair", true);
        return bootstrap;
    }

    private class ClientPipelineFactory implements ChannelPipelineFactory {

        @Override
        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = Channels.pipeline();
            if (SSLConfiguration.enableForCmdChannel()) {
                SSLContext ctxt = SSLContextFactory.getClientContext();
                SSLEngine engine = ctxt.createSSLEngine();
                engine.setUseClientMode(true);
                // intentionally don't set the endpoint identification algo,
                // since this doesn't seem to work for SSLEngine and nio.
                // we do this manually once the hanshake finishes.
                engine.setSSLParameters(SSLContextFactory.getSSLParameters(ctxt));
                pipeline.addLast("ssl", new SslHandler(engine));
                logger.log(Level.FINE, "Added ssl handler for command channel client");
            }
            pipeline.addLast("decoder", new ResponseDecoder());
            pipeline.addLast("encoder", new RequestEncoder());
            return pipeline;
        }
        
    }
}

