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

package com.redhat.thermostat.agent.command.server.internal;

import java.nio.channels.ByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.redhat.thermostat.common.command.ConfigurationCommandContext;
import com.redhat.thermostat.common.ssl.SSLContextFactory;
import com.redhat.thermostat.common.ssl.SslInitException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.config.SSLConfiguration;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslHandler;

class CommandChannelServerContext implements ConfigurationCommandContext {

    private static final Logger logger = LoggingUtils.getLogger(CommandChannelServerContext.class);
    
    private final ServerBootstrap bootstrap;
    private final SSLConfiguration sslConf;


    CommandChannelServerContext(SSLConfiguration sslConf, ByteChannel agentChannel) {
        this(sslConf, agentChannel, new ServerChannelPipelineInitializerCreator());
    }
    
    CommandChannelServerContext(SSLConfiguration sslConf, ByteChannel agentChannel, ServerChannelPipelineInitializerCreator initCreator) {
        this.sslConf = sslConf;
        bootstrap = createBootstrap(sslConf, agentChannel, initCreator);
    }

    @Override
    public AbstractBootstrap<?, ?> getBootstrap() {
        return bootstrap;
    }

    @Override
    public SSLConfiguration getSSLConfiguration() {
        return sslConf;
    }

    private ServerBootstrap createBootstrap(SSLConfiguration conf, ByteChannel agentChannel, ServerChannelPipelineInitializerCreator initCreator) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(initCreator.createInitializer(conf, agentChannel))
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.SO_REUSEADDR, true);
        
        return bootstrap;
    }

    static class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

        private final SSLConfiguration sslConf;
        private final ByteChannel agentChannel;
        
        ServerChannelInitializer(SSLConfiguration sslConf, ByteChannel agentChannel) {
            this.sslConf = sslConf;
            this.agentChannel = agentChannel;
        }
        
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            if (sslConf.enableForCmdChannel()) {
                SSLEngine engine = null;
                try {
                    SSLContext ctxt = SSLContextFactory.getServerContext(sslConf);
                    engine = ctxt.createSSLEngine();
                    engine.setUseClientMode(false);
                } catch (SslInitException | InvalidConfigurationException e) {
                    logger.log(Level.SEVERE,
                            "Failed to initiate command channel endpoint", e);
                }
                pipeline.addLast("ssl", new SslHandler(engine));
                logger.log(Level.FINE, "Added SSL handler for command channel endpoint");
            }
            pipeline.addLast("decoder", new CommandChannelRequestDecoder());
            pipeline.addLast("encoder", new ResponseEncoder());
            pipeline.addLast("handler", new ServerHandler(sslConf, agentChannel));
        }
        
    }
    
    // Testing hook
    static class ServerChannelPipelineInitializerCreator {
        
        ServerChannelInitializer createInitializer(SSLConfiguration sslConf, ByteChannel agentChannel) {
            return new ServerChannelInitializer(sslConf, agentChannel);
        }
    }

}

