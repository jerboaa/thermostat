package com.redhat.thermostat.client;

import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoURI;
import com.redhat.thermostat.agent.storage.StorageConstants;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class MongoConnection extends Connection {
    private static final Logger logger = LoggingUtils.getLogger(MongoConnection.class);

    private Mongo m = null;
    private DB db = null;
    private boolean isLocal = false;

    @Override
    public void connect() {
        switch (getType()) {
            case LOCAL:
                try {
                    m = new Mongo(getMongoURI());
                    db = m.getDB(StorageConstants.THERMOSTAT_DB_NAME);
                    /* the mongo java driver does not ensure this connection is actually working */
                    testConnnection(db);
                } catch (MongoException e) {
                    fireChanged(ConnectionStatus.FAILED_TO_CONNECT);
                    return;
                } catch (UnknownHostException e) {
                    fireChanged(ConnectionStatus.FAILED_TO_CONNECT);
                    return;
                }
                isLocal = true;
                break;
            case REMOTE:
                // TODO connect to a remote machine
                break;
            case CLUSTER:
                // TODO connect to a cluster
                break;
            case NONE:
                throw new IllegalArgumentException();
        }
        fireChanged(ConnectionStatus.CONNECTED);
        connected = true;
    }

    private static void testConnnection(DB db) {
        db.getCollection("agent-config").getCount();
    }

    private MongoURI getMongoURI() {
        // FIXME hardcorded address
        startMongoAndAgent();
        return new MongoURI("mongodb://127.0.0.1:27017");
    }

    private void startMongoAndAgent() {
        // TODO implement this
        logger.log(Level.WARNING, "startMongoAndAgent not implemented");
        logger.log(Level.WARNING, "please start mongodb and agent yourself");
    }

    public DB getDB() {
        return db;
    }

    @Override
    public void disconnect() {
        if (m != null) {
            m.close();
        }
        if (isLocal) {
            stopMongoAndAgent();
        }
        connected = false;
    }

    private void stopMongoAndAgent() {
        // TODO implement this
        logger.log(Level.WARNING, "startMongoAndAgent not implemented");
        logger.log(Level.WARNING, "please stop mongodb and agent yourself");
    }

}
