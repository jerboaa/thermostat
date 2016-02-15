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

package com.redhat.thermostat.utils.management.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.redhat.thermostat.agent.utils.ProcDataSource;
import com.redhat.thermostat.agent.utils.management.MXBeanConnection;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionException;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.agent.utils.username.UserNameUtil;
import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.utils.management.internal.ProcessUserInfoBuilder.ProcessUserInfo;

public class MXBeanConnectionPoolImpl implements MXBeanConnectionPool {

    // pid -> (usageCount, actualObject)
    private Map<Integer, Pair<Integer, MXBeanConnectionImpl>> pool = new HashMap<>();

    private final ConnectorCreator creator;
    private final File binPath;
    private final ProcessUserInfoBuilder userInfoBuilder;

    public MXBeanConnectionPoolImpl(File binPath, UserNameUtil userNameUtil) {
        this(new ConnectorCreator(), binPath, new ProcessUserInfoBuilder(new ProcDataSource(), userNameUtil));
    }

    MXBeanConnectionPoolImpl(ConnectorCreator connectorCreator, File binPath, ProcessUserInfoBuilder userInfoBuilder) {
        this.creator = connectorCreator;
        this.binPath = binPath;
        this.userInfoBuilder = userInfoBuilder;
    }

    @Override
    public synchronized MXBeanConnection acquire(int pid) throws MXBeanConnectionException {
        Pair<Integer, MXBeanConnectionImpl> data = pool.get(pid);
        if (data == null) {
            MXBeanConnector connector = null;
            ProcessUserInfo info = userInfoBuilder.build(pid);
            String username = info.getUsername();
            if (username == null) {
                throw new MXBeanConnectionException("Unable to determine owner of " + pid);
            }
            try {
                connector = creator.create(pid, username, binPath);
                MXBeanConnectionImpl connection = connector.connect();
                data = new Pair<Integer, MXBeanConnectionImpl>(1, connection);
            } catch (IOException e) {
                throw new MXBeanConnectionException(e);
            }
        } else {
            data = new Pair<>(data.getFirst() + 1, data.getSecond());
        }
        pool.put(pid, data);
        return data.getSecond();
    }

    @Override
    public synchronized void release(int pid, MXBeanConnection toRelease) throws MXBeanConnectionException {
        Pair<Integer, MXBeanConnectionImpl> data = pool.get(pid);
        MXBeanConnectionImpl connection = data.getSecond();
        int usageCount = data.getFirst();
        usageCount--;
        if (usageCount == 0) {
            try {
                connection.close();
            } catch (IOException e) {
                throw new MXBeanConnectionException(e);
            }
            pool.remove(pid);
        } else {
            data = new Pair<>(usageCount, connection);
            pool.put(pid, data);
        }
    }
    
    static class ConnectorCreator {
        public MXBeanConnector create(int pid, String user, File binPath) throws IOException {
            MXBeanConnector connector = new MXBeanConnector(pid, user, binPath);
            return connector;
        }
    }
}

