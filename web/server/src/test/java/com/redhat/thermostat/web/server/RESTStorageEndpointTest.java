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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.redhat.thermostat.common.model.Pojo;
import com.redhat.thermostat.common.storage.Categories;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Query.Criteria;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.test.FreePortFinder;
import com.redhat.thermostat.test.FreePortFinder.TryPort;
import com.redhat.thermostat.web.client.RESTStorage;
import com.redhat.thermostat.web.common.RESTQuery;
import com.redhat.thermostat.web.common.StorageWrapper;

public class RESTStorageEndpointTest {

    public static class TestClass implements Pojo {
        private String key1;
        private int key2;
        public String getKey1() {
            return key1;
        }
        public void setKey1(String key1) {
            this.key1 = key1;
        }
        public int getKey2() {
            return key2;
        }
        public void setKey2(int key2) {
            this.key2 = key2;
        }
    }

    private Server server;
    private int port;
    private Storage mockStorage;

    private static Key<String> key1;
    private static Key<Integer> key2;
    private static Category category;

    @BeforeClass
    public static void setupCategory() {
        key1 = new Key<>("key1", true);
        key2 = new Key<>("key2", false);
        category = new Category("test", key1, key2);
    }

    @AfterClass
    public static void cleanupCategory() {
        Categories.remove(category);
        category = null;
        key2 = null;
        key1 = null;
    }

    @Before
    public void setUp() throws Exception {

        mockStorage = mock(Storage.class);
        StorageWrapper.setStorage(mockStorage);

        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port);
            }
        });
    }

    private void startServer(int port) throws Exception {
        server = new Server(port);
        server.setHandler(new WebAppContext("src/main/webapp", "/"));
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        server.join();
    }

    @Test
    public void testFind() {
        // Configure mock storage.
        TestClass expected = new TestClass();
        expected.setKey1("fluff");
        expected.setKey2(42);
        when(mockStorage.findPojo(any(Query.class), same(TestClass.class))).thenReturn(expected);

        Query mockQuery = QueryTestHelper.createMockQuery();
        when(mockStorage.createQuery()).thenReturn(mockQuery);

        RESTStorage restStorage = new RESTStorage();
        restStorage.setEndpoint(getEndpoint());
        Query query = restStorage.createQuery();
        query.from(category).where(key1, Criteria.EQUALS, "fluff");

        TestClass result = restStorage.findPojo(query, TestClass.class);

        assertEquals("fluff", result.getKey1());
        assertEquals(42, result.getKey2());
        verify(mockStorage).createQuery();
        verify(mockStorage).findPojo(any(Query.class), same(TestClass.class));
    }

    @Test
    public void testFindAllPojos() throws IOException {
        TestClass expected1 = new TestClass();
        expected1.setKey1("fluff1");
        expected1.setKey2(42);
        TestClass expected2 = new TestClass();
        expected2.setKey1("fluff2");
        expected2.setKey2(43);
        @SuppressWarnings("unchecked")
        Cursor<TestClass> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(expected1).thenReturn(expected2);

        when(mockStorage.findAllPojos(any(Query.class), same(TestClass.class))).thenReturn(cursor);
        Query mockQuery = QueryTestHelper.createMockQuery();
        when(mockStorage.createQuery()).thenReturn(mockQuery);

        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/find-all");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        RESTQuery query = (RESTQuery) new RESTQuery().from(category).where(key1, Criteria.EQUALS, "fluff");
        query.setResultClassName(TestClass.class.getName());
        Gson gson = new Gson();
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        gson.toJson(query, out);
        out.flush();

        Reader in = new InputStreamReader(conn.getInputStream());
        TestClass[] results = gson.fromJson(in, TestClass[].class);
        assertEquals(2, results.length);
        assertEquals("fluff1", results[0].getKey1());
        assertEquals(42, results[0].getKey2());
        assertEquals("fluff2", results[1].getKey1());
        assertEquals(43, results[1].getKey2());
    }

    private String getEndpoint() {
        return "http://localhost:" + port + "/storage";
    }
}
