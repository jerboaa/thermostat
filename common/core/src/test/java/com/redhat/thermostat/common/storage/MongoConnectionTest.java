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

package com.redhat.thermostat.common.storage;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoURI;
import com.redhat.thermostat.common.config.StartupConfiguration;
import com.redhat.thermostat.common.storage.Connection.ConnectionListener;
import com.redhat.thermostat.common.storage.Connection.ConnectionStatus;

@PrepareForTest(MongoConnection.class)
@RunWith(PowerMockRunner.class)
public class MongoConnectionTest {

    private MongoConnection conn;
    private ConnectionListener listener;

    @Before
    public void setUp() {
        StartupConfiguration conf = mock(StartupConfiguration.class);
        when(conf.getDBConnectionString()).thenReturn("mongodb://127.0.0.1:27518");
        conn = new MongoConnection(conf);
        listener = mock(ConnectionListener.class);
        conn.addListener(listener);
    }

    @After
    public void tearDown() {
        conn = null;
    }

    @Test
    public void testConnectSuccess() throws Exception {
        DBCollection collection = mock(DBCollection.class);
        DB db = mock(DB.class);
        when(db.getCollection("agent-config")).thenReturn(collection);
        Mongo m = mock(Mongo.class);
        when(m.getDB(StorageConstants.THERMOSTAT_DB_NAME)).thenReturn(db);
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        conn.connect();

        verify(listener).changed(ConnectionStatus.CONNECTED);
    }

    @Test
    public void testConnectIOException() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenThrow(new IOException());
        boolean exceptionThrown = false;
        try {
            conn.connect();
        } catch (ConnectionException ex) {
            exceptionThrown = true;
        }
        verify(listener).changed(ConnectionStatus.FAILED_TO_CONNECT);
        assertTrue(exceptionThrown);
    }

    @Test
    public void testConnectMongoException() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenThrow(new MongoException("fluff"));
        boolean exceptionThrown = false;
        try {
            conn.connect();
        } catch (ConnectionException ex) {
            exceptionThrown = true;
        }

        verify(listener).changed(ConnectionStatus.FAILED_TO_CONNECT);
        assertTrue(exceptionThrown);
    }
}
