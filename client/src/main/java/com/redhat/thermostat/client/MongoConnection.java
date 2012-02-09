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

package com.redhat.thermostat.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Logger;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoURI;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.storage.StorageConstants;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class MongoConnection extends Connection {
    private static final Logger logger = LoggingUtils.getLogger(MongoConnection.class);

    private Mongo m = null;
    private DB db = null;
    private boolean hasLocalAgent = false;
    private Process localAgentProcess = null;
    private Properties props;

    public MongoConnection(Properties props) {
        this.props = props;
    }

    @Override
    public void connect() {
        try {
            m = new Mongo(getMongoURI());
            db = m.getDB(StorageConstants.THERMOSTAT_DB_NAME);
            /* the mongo java driver does not ensure this connection is actually working */
            testConnection(db);
        } catch (MongoException e) {
            fireChanged(ConnectionStatus.FAILED_TO_CONNECT);
            return;
        } catch (UnknownHostException e) {
            fireChanged(ConnectionStatus.FAILED_TO_CONNECT);
            return;
        } catch (LocalAgentException e) {
            fireChanged(ConnectionStatus.FAILED_TO_CONNECT);
            return;
        } catch (NotImplementedException e) {
            fireChanged(ConnectionStatus.FAILED_TO_CONNECT);
            return;
        }
        fireChanged(ConnectionStatus.CONNECTED);
        connected = true;
    }

    private static void testConnection(DB db) {
        db.getCollection("agent-config").getCount();
    }

    private MongoURI getMongoURI() {
        MongoURI uri = null;
        switch (getType()) {
            case LOCAL:
                startLocalAgent();
                uri = new MongoURI("mongodb://127.0.0.1:"
                        + props.getProperty(Constants.AGENT_PROPERTY_MONGOD_PORT));
                break;
            case REMOTE:
                throw new NotImplementedException("No mongo URI implemented for REMOTE.");
            case CLUSTER:
                throw new NotImplementedException("No mongo URI implemented for CLUSTER.");
        }
        return uri;
    }

    private void startLocalAgent() throws LocalAgentException {
        int status = 0;
        try {
            String agentCommand = props.getProperty(Constants.CLIENT_PROPERTY_AGENT_LAUNCH_SCRIPT) + " --local";
            localAgentProcess = Runtime.getRuntime().exec(agentCommand);
            // Allow some time for things to get started.
            try {
                // TODO provide some UI feedback here instead of just seeming dead.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // ignore
            }
        } catch (IOException e) {
            throw new LocalAgentException();
        }
        if (status != 0) {
            throw new LocalAgentException();
        }
        hasLocalAgent = true;
    }

    public DB getDB() {
        return db;
    }

    @Override
    public void disconnect() {
        if (m != null) {
            m.close();
        }
        if (hasLocalAgent) {
            stopLocalAgent();
        }
        connected = false;
    }

    private void stopLocalAgent() {
        // TODO this is currently using Agent's 'run until some data avail on stdin' hack.
        // That hack will go away, at which point we will need another way to shut down.
        OutputStream agentIn = localAgentProcess.getOutputStream();
        byte[] anything = { 0x04 };
        try {
            agentIn.write(anything);
            agentIn.flush();
            localAgentProcess.waitFor();
        } catch (IOException e) {
            logger.warning("Error shutting down local agent.");
        } catch (InterruptedException e) {
            logger.warning("Interrupted waiting for local agent to shut down.");
        }
    }

    private class LocalAgentException extends RuntimeException {
    }
}
