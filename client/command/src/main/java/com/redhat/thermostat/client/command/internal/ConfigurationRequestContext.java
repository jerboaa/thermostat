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

package com.redhat.thermostat.client.command.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.redhat.thermostat.common.command.noapi.ConfigurationCommandContext;
import com.redhat.thermostat.common.command.noapi.RequestEncoder;
import com.redhat.thermostat.common.ssl.SSLContextFactory;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.SSLConfiguration;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;

public class ConfigurationRequestContext implements ConfigurationCommandContext {
    
    private static final Logger logger = LoggingUtils.getLogger(ConfigurationRequestContext.class);

    private final Bootstrap bootstrap;
    private final SSLConfiguration sslConf;

    ConfigurationRequestContext(SSLConfiguration sslConf) {
        this(sslConf, new ClientPipelineInitializerCreator());
    }
    
    ConfigurationRequestContext(SSLConfiguration sslConf, ClientPipelineInitializerCreator creator) {
        this.sslConf = sslConf;
        this.bootstrap = createBootstrap(creator);
    }

    @Override
    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    @Override
    public SSLConfiguration getSSLConfiguration() {
        return sslConf;
    }

    private Bootstrap createBootstrap(ClientPipelineInitializerCreator initCreator) {
        // Configure the client.
        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        bootstrap.channel(NioSocketChannel.class)
            .group(workerGroup)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .handler(initCreator.createInitializer(sslConf));
        return bootstrap;
    }

    static class ClientPipelineInitializer extends ChannelInitializer<SocketChannel> {

        private final SSLConfiguration sslConf;
        
        ClientPipelineInitializer(SSLConfiguration sslConf) {
            this.sslConf = sslConf;
        }
        
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            if (sslConf.enableForCmdChannel()) {
                SSLContext ctxt = SSLContextFactory.getClientContext(sslConf);
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
        }
        
    }
    
    // Testing hook
    static class ClientPipelineInitializerCreator {
        
        ClientPipelineInitializer createInitializer(SSLConfiguration sslConf) {
            return new ClientPipelineInitializer(sslConf);
        }
    }
}

