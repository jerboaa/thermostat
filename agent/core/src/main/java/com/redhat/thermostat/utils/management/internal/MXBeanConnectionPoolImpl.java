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

package com.redhat.thermostat.utils.management.internal;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.RMIRegistry;
import com.redhat.thermostat.agent.internal.RMIRegistryImpl;
import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.tools.ApplicationException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.utils.management.MXBeanConnection;
import com.redhat.thermostat.utils.management.MXBeanConnectionPool;

public class MXBeanConnectionPoolImpl implements MXBeanConnectionPool {

    private static final Logger logger = LoggingUtils.getLogger(MXBeanConnectionPoolImpl.class);
    
    // pid -> (usageCount, actualObject)
    private Map<Integer, Pair<Integer, MXBeanConnectionImpl>> pool = new HashMap<>();

    private final ConnectorCreator creator;
    private final RMIRegistryImpl registry;
    private final File binPath;

    public MXBeanConnectionPoolImpl(RMIRegistryImpl registry, File binPath) {
        this(new ConnectorCreator(), registry, binPath);
    }

    MXBeanConnectionPoolImpl(ConnectorCreator connectorCreator, RMIRegistryImpl registry, File binPath) {
        this.creator = connectorCreator;
        this.registry = registry;
        this.binPath = binPath;
        
        // Start RMI registry
        try {
            registry.start();
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, "Unable to start RMI registry", e);
        }
    }

    @Override
    public synchronized MXBeanConnection acquire(int pid) throws Exception {
        Pair<Integer, MXBeanConnectionImpl> data = pool.get(pid);
        if (data == null) {
            MXBeanConnector connector = null;
            try {
                connector = creator.create(registry, pid, binPath);
                connector.attach();
                MXBeanConnectionImpl connection = connector.connect();
                data = new Pair<Integer, MXBeanConnectionImpl>(1, connection);
            } finally {
                if (connector != null) {
                    connector.close();
                }
            }
        } else {
            data = new Pair<>(data.getFirst() + 1, data.getSecond());
        }
        pool.put(pid, data);
        return data.getSecond();
    }

    @Override
    public synchronized void release(int pid, MXBeanConnection toRelese) throws Exception {
        Pair<Integer, MXBeanConnectionImpl> data = pool.get(pid);
        MXBeanConnectionImpl connection = data.getSecond();
        int usageCount = data.getFirst();
        usageCount--;
        if (usageCount == 0) {
            connection.close();
            pool.remove(pid);
        } else {
            data = new Pair<>(usageCount, connection);
            pool.put(pid, data);
        }
    }
    
    public void shutdown() {
        try {
            registry.stop();
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, "Unable to stop RMI registry", e);
        }
    }

    static class ConnectorCreator {
        public MXBeanConnector create(RMIRegistry registry, int pid, File binPath) throws IOException, ApplicationException {
            MXBeanConnector connector = new MXBeanConnector(registry, pid, binPath);
            return connector;
        }
    }
}
