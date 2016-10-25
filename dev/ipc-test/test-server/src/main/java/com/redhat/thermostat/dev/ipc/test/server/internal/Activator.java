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

package com.redhat.thermostat.dev.ipc.test.server.internal;

import java.io.IOException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.MultipleServiceTracker.DependencyProvider;
import com.redhat.thermostat.shared.config.CommonPaths;

public class Activator implements BundleActivator {
    
    private MultipleServiceTracker tracker;
    private UnixSocketTestServer server;
    private boolean finished = false;
    
    @Override
    public void start(final BundleContext ctx) throws Exception {
        Class<?>[] deps = { CommonPaths.class, AgentIPCService.class };
        tracker = new MultipleServiceTracker(ctx, deps, new Action() {
            
            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                AgentIPCService service = services.get(AgentIPCService.class);
                CommonPaths paths = services.get(CommonPaths.class);
                server = new UnixSocketTestServer(service, paths);
                try {
                    server.start();
                    //finished = true;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // Test finished, so tell the framework to shutdown
                    shutdown(ctx.getBundle());
                }
            }

            @Override
            public void dependenciesUnavailable() {
                if (!finished && server != null) {
                    try {
                        server.cleanup();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    server = null;
                }
            }
        });
        tracker.open();
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
        tracker.close();
    }
    
    private void shutdown(final Bundle bundle) {
        // Shutdown in a separate thread, otherwise context is invalidated early
        // and we get exceptions
        Thread shutdownThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    bundle.stop();
                } catch (BundleException e) {
                    e.printStackTrace();
                }
            }
        });
        shutdownThread.start();
    }

}
