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

package com.redhat.thermostat.web.endpoint.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.redhat.thermostat.common.ssl.SSLContextFactory;
import com.redhat.thermostat.common.ssl.SslInitException;
import com.redhat.thermostat.common.utils.HostPortPair;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.config.SSLConfiguration;

class JettyContainerLauncher {
    
    private static Logger logger = LoggingUtils.getLogger(JettyContainerLauncher.class);
    static final String JAAS_CONFIG_PROP = "java.security.auth.login.config";
    
    private final EmbeddedServletContainerConfiguration config;
    private final SSLConfiguration sslConfig;
    private Server server;
    private Thread serverThread;
    private boolean isStartupSuccessFul = true;
    
    JettyContainerLauncher(EmbeddedServletContainerConfiguration config, SSLConfiguration sslConfig) {
        this.config = config;
        this.sslConfig = sslConfig;
    }
    
    
    void startContainer(final CountDownLatch contextStartedLatch) {
        serverThread = new Thread(new Runnable() {

            @Override
            public void run() {
                startContainerAndDeployWar(contextStartedLatch);
            }
            
        });
        serverThread.start();
    }
    
    boolean isStartupSuccessFul() {
        return isStartupSuccessFul;
    }
    
    void stopContainer() {
        try {
            server.stop();
            server.join();
            serverThread.join();
        } catch (Exception e) {
            logger.log(Level.INFO, e.getMessage(), e);
        }
    }

    private void startContainerAndDeployWar(final CountDownLatch contextStartedLatch) {
        // Since we call this in a thread and we wait for a countDown() on the
        // latch be sure to always call it in the exception case. Otherwise
        // the thread won't exit.
        try {
            doStartContainerAndDeployWar(contextStartedLatch);
        } catch (Exception e) {
            isStartupSuccessFul = false;
            logger.log(Level.WARNING, e.getMessage(), e);
            contextStartedLatch.countDown();
        }
    }
    
    private void doStartContainerAndDeployWar(final CountDownLatch contextStartedLatch) throws Exception {
        HostPortPair ipPort = null;
        try {
            ipPort = config.getHostsPortsConfig();
        } catch (InvalidConfigurationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            isStartupSuccessFul = false;
            contextStartedLatch.countDown();
            return;
        }
        server = new Server();
        // Set up the connector with SSL enabled if so configured
        ServerConnector connector = getServerConnector();
        
        // Set host and port
        connector.setHost(ipPort.getHost());
        connector.setPort(ipPort.getPort());
        server.addConnector(connector);
            
        File webArchiveDir = config.getAbsolutePathToExplodedWebArchive();
        if (!webArchiveDir.exists()) {
            String msg = String.format("Exploded web archive '%s' not found, exitting!",
                    webArchiveDir.getCanonicalFile().getAbsolutePath());
            logger.log(Level.SEVERE, msg);
            isStartupSuccessFul = false;
            contextStartedLatch.countDown();
            return;
        }
        WebAppContext ctx = new WebAppContext(webArchiveDir.getAbsolutePath(), config.getContextPath());
        // Jetty insists on a webdefault.xml file. If the file is not found,
        // it fails to boot the embedded server. We work around this by writing
        // a temp file and convincing it to use that. The file content comes
        // from jetty itself, so it should always be there.
        Bundle bundle = FrameworkUtil.getBundle(WebAppContext.class);
        URL uri = bundle.getResource("/org/eclipse/jetty/webapp/webdefault.xml");
        logger.log(Level.INFO, 
                uri == null ?
                        "webdefault.xml file not found!" :
                            "found file in: " + uri.toExternalForm());
        File tempWebDefaults = File.createTempFile("jetty-webdefault", ".xml");
        tempWebDefaults.deleteOnExit();
        
        writeWebDefaults(tempWebDefaults, uri);
        ctx.setDefaultsDescriptor(tempWebDefaults.getAbsolutePath());
        
        // Make the class loader aware of the osgi env. By default it uses
        // WebAppClassLoader which is not sufficient. Mainly because of classes
        // jetty itself is using (from the servlet API, other bundles etc). 
        // Thermostat itself should be happy with just WebAppClassLoader.
        // 
        // It's a simple delegating class loader. If WebappClassLoader doesn't
        // find the class, we delegate to the class loader of this bundle (which
        // in turn should delegate to the right loader).
        // 
        // Note that this also assumes proper wiring of bundles. That should be
        // the case by using explicit instructions for the maven-bundle-plugin
        // and starting all required jetty bundles on boot.
        ctx.setClassLoader(new DelegatingWebappClassLoader(getClass().getClassLoader(), ctx));
        
        // Make server startup fail if context cannot be deployed.
        // Please don't change this.
        ctx.setThrowUnavailableOnStartupException(true);
        
        // Wait for the context to be up and running
        ctx.addLifeCycleListener(new DoNothingLifecycleListener() {
            @Override
            public void lifeCycleStarted(LifeCycle arg0) {
                contextStartedLatch.countDown();
            }
            
            @Override
            public void lifeCycleFailure(LifeCycle arg0, Throwable arg1) {
                isStartupSuccessFul = false;
                contextStartedLatch.countDown();
            }
        });
        configureJaas();
        // Configure the context handler with request logging if 
        // so desired.
        configureRequestLog(ctx);
        
        server.start();
    }

    private void configureRequestLog(WebAppContext ctx) {
        if (config.hasRequestLogConfig()) {
            HandlerCollection handlers = new HandlerCollection();
            ContextHandlerCollection contexts = new ContextHandlerCollection();
            RequestLogHandler requestLogHandler = new RequestLogHandler();
            handlers.setHandlers(new Handler[] { contexts, ctx,
                    requestLogHandler });
            server.setHandler(handlers);
    
            String logPath = config.getAbsolutePathToRequestLog();
            NCSARequestLog requestLog = new NCSARequestLog(logPath);
            requestLog.setRetainDays(90);
            requestLog.setAppend(true);
            requestLog.setExtended(false);
            TimeZone tz = Calendar.getInstance().getTimeZone();
            requestLog.setLogTimeZone(tz.getID());
            requestLogHandler.setRequestLog(requestLog);
            logger.log(Level.FINEST, "Using jetty request log: " + logPath);
        } else {
            // no request logging just use the context as handler
            server.setHandler(ctx);
        }
    }


    private ServerConnector getServerConnector()
            throws InvalidConfigurationException, SslInitException {
        ServerConnector connector;
        if (config.isEnableTLS()) {
            logger.log(Level.FINEST, "Enabling TLS enabled web storage endpoint");
            
            // HTTP Configuration
            HttpConfiguration http_config = new HttpConfiguration();
            http_config.setSecureScheme("https");
            
            // SSL HTTP Configuration
            HttpConfiguration https_config = new HttpConfiguration(http_config);
            https_config.addCustomizer(new SecureRequestCustomizer());

            SslContextFactory sslContextFactory = new SslContextFactory();
            SSLContext serverContext = SSLContextFactory.getServerContext(sslConfig);
            sslContextFactory.setSslContext(serverContext);
            // SSL Connector
            connector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory,"http/1.1"),
                new HttpConnectionFactory(https_config));
        } else {
            // non-SSL
            connector = new ServerConnector(server);
        }
        return connector;
    }


    private void writeWebDefaults(File tempWebDefaults, URL uri) {
        try (FileOutputStream fout = new FileOutputStream(tempWebDefaults);
                InputStream in = uri.openStream();
                BufferedInputStream bin = new BufferedInputStream(in)) {
            byte[] buf = new byte[256];
            int len = -1;
            while ((len = bin.read(buf)) > 0) {
                fout.write(buf, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     * Equivalent of -Djava.security.auth.login.config=$THERMOSTAT_HOME/etc/thermostat_jaas.conf
     * 
     * package-private for testing.
     */
    void configureJaas() {
        String propVal = System.getProperty(JAAS_CONFIG_PROP);
        // Only set JAAS config property if not already set
        if (propVal == null) {
            propVal = config.getAbsolutePathToJaasConfig();
            System.setProperty(JAAS_CONFIG_PROP, propVal);
        }
        logger.log(Level.FINE, "Using JAAS config '" + propVal + "'");
    }
    
}
