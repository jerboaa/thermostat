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


package com.redhat.thermostat.web.cmd;

import java.io.IOException;
import java.util.List;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.webapp.WebAppContext;

import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.storage.MongoStorageProvider;
import com.redhat.thermostat.web.server.IpPortPair;
import com.redhat.thermostat.web.server.WebStorageEndPoint;

class WebServiceLauncher {

    private Server server;
    private String storageURL;
    // IP/Port pairs, keyed by IP
    private List<IpPortPair> ipsPorts;
    
    WebServiceLauncher() {
        server = new Server();
    }
    
    // Constructor for testing
    WebServiceLauncher(Server server) {
        this.server = server;
    }

    void start() throws Exception {
        checkConfig();
        Connector[] connectors = new Connector[ipsPorts.size()];
        for (int i = 0; i < ipsPorts.size(); i++) {
            IpPortPair pair = ipsPorts.get(i);
            connectors[i] = new SelectChannelConnector();
            connectors[i].setPort(pair.getPort());
            connectors[i].setHost(pair.getIp());
        }
        server.setConnectors( connectors );

        WebAppContext ctx = new WebAppContext();
        ctx.setContextPath("/");
        // This prevents useless classloading, which could fail in the face of OSGi.
        ctx.setConfigurations(new org.eclipse.jetty.webapp.Configuration[0]);

        ServletHolder servletHolder = new ServletHolder("rest-storage-end-point", new WebStorageEndPoint());
        servletHolder.setInitParameter(WebStorageEndPoint.STORAGE_ENDPOINT, storageURL);
        servletHolder.setInitParameter(WebStorageEndPoint.STORAGE_CLASS, MongoStorageProvider.class.getName());
        ctx.addServlet(servletHolder, "/");

        configureSecurity(ctx);

        server.setHandler(ctx);
        server.start();
        server.join();
    }

    private void configureSecurity(WebAppContext ctx) {
        ConstraintSecurityHandler secHandler = new ConstraintSecurityHandler();
        ConstraintMapping constraintMap = new ConstraintMapping();
        Constraint constraint = new Constraint();
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[] { "thermostat-client", "thermostat-agent" });
        constraint.setName("Entire Application");
        constraintMap.setPathSpec("/*");
        constraintMap.setConstraint(constraint);
        
        secHandler.setRealmName("Thermostat Realm");
        secHandler.setAuthMethod("BASIC");
        secHandler.addConstraintMapping(constraintMap);
        secHandler.addRole("thermostat-agent");
        secHandler.addRole("thermostat-client");
        secHandler.setLoginService(new MappedLoginService() {
            
            @Override
            protected void loadUsers() throws IOException {
                putUser("thermostat", new Password("thermostat"), new String[] { "thermostat-agent", "thermostat-client" });
            }

            @Override
            protected UserIdentity loadUser(String username) {
                return new DefaultUserIdentity(null, null, new String[] { "thermostat-agent", "thermostat-client" });
            }
        });
        ctx.setSecurityHandler(secHandler);
    }

    void stop() throws Exception {
        server.stop();
        server.join();
    }

    public void setStorageURL(String storageURL) {
        this.storageURL = storageURL;
    }

    public void setIpAddresses(List<IpPortPair> ipsPorts) {
        this.ipsPorts = ipsPorts;
    }

    /*
     * StorageURL, port must have been set
     */
    private void checkConfig() throws InvalidConfigurationException {
        if (storageURL == null) {
            throw new InvalidConfigurationException("Storage URL must be set");
        }
        if (ipsPorts == null) {
            throw new InvalidConfigurationException("IP adresses to bind to must be set");
        }
        for (IpPortPair pair: ipsPorts) {
            if (pair.getPort() <= 0) {
                throw new InvalidConfigurationException("Invalid port number " + pair.getPort());
            }
        }
    }
}
