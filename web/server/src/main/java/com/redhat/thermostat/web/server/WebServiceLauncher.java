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


package com.redhat.thermostat.web.server;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;

import com.redhat.thermostat.common.config.InvalidConfigurationException;

class WebServiceLauncher {

    private Server server;
    private String storageURL;
    private int port = -1;
    
    WebServiceLauncher() {
        server = new Server();
    }
    
    // Constructor for testing
    WebServiceLauncher(Server server) {
        this.server = server;
    }

    void start() throws Exception {
        checkConfig();
        Connector connector = new SelectChannelConnector();
        connector.setPort(port);
        server.setConnectors(new Connector[] { connector } );
        ServletHandler handler = new ServletHandler();
        ServletHolder servletHolder = new ServletHolder("rest-storage-end-point", new WebStorageEndPoint());
        servletHolder.setInitParameter(WebStorageEndPoint.STORAGE_ENDPOINT, storageURL);
        handler.setServlets(new ServletHolder[] { servletHolder });
        ServletMapping mapping = new ServletMapping();
        mapping.setPathSpec("/");
        mapping.setServletName("rest-storage-end-point");
        handler.setServletMappings(new ServletMapping[] { mapping });
        server.setHandler(handler);
        server.start();
        server.join();
    }

    void stop() throws Exception {
        server.stop();
        server.join();
    }

    public void setStorageURL(String storageURL) {
        this.storageURL = storageURL;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /*
     * StorageURL, port must have been set
     */
    private void checkConfig() throws InvalidConfigurationException {
        if (port <= 0) {
            throw new InvalidConfigurationException("Invalid port number: " + port);
        }
        if (storageURL == null) {
            throw new InvalidConfigurationException("Storage URL must be set");
        }
    }
}
