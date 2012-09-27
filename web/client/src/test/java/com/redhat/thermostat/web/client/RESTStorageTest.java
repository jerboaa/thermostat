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


package com.redhat.thermostat.web.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.redhat.thermostat.common.storage.Categories;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Query.Criteria;
import com.redhat.thermostat.test.FreePortFinder;
import com.redhat.thermostat.test.FreePortFinder.TryPort;
import com.redhat.thermostat.web.common.Qualifier;
import com.redhat.thermostat.web.common.RESTQuery;
import com.redhat.thermostat.web.common.WebInsert;

public class RESTStorageTest {

    private Server server;

    private int port;

    private String requestBody;

    private String responseBody;

    private static Category category;
    private static Key<String> key1;

    @BeforeClass
    public static void setupCategory() {
        key1 = new Key<>("property1", true);
        category = new Category("test", key1);
    }

    @AfterClass
    public static void cleanupCategory() {
        Categories.remove(category);
        category = null;
        key1 = null;
    }

    @Before
    public void setUp() throws Exception {
        port = FreePortFinder.findFreePort(new TryPort() {
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port);
            }
        });
    }

    private void startServer(int port) throws Exception {
        server = new Server(port);
        server.setHandler(new AbstractHandler() {
            
            @Override
            public void handle(String target, Request baseRequest,
                    HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                // Read request body.
                StringBuilder body = new StringBuilder();
                Reader reader = request.getReader();
                while (true) {
                    int read = reader.read();
                    if (read == -1) {
                        break;
                    }
                    body.append((char) read);
                }
                requestBody = body.toString();
                // Send response body.
                response.setStatus(HttpServletResponse.SC_OK);
                if (responseBody != null) {
                    response.getWriter().write(responseBody);
                }
                baseRequest.setHandled(true);
            }
        });
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        server.join();
    }

    @Test
    public void testFindPojo() {
        RESTStorage storage = new RESTStorage();
        storage.setEndpoint("http://localhost:" + port + "/");

        TestObj obj = new TestObj();
        obj.setProperty1("fluffor");
        Gson gson = new Gson();
        responseBody = gson.toJson(obj);

        Query query = storage.createQuery().from(category).where(key1, Criteria.EQUALS, "fluff");

        TestObj result = storage.findPojo(query, TestObj.class);
        RESTQuery restQuery = gson.fromJson(requestBody, RESTQuery.class);

        Category actualCategory = restQuery.getCategory();
        assertEquals("test", actualCategory.getName());
        Collection<Key<?>> keys = actualCategory.getKeys();
        assertEquals(1, keys.size());
        assertTrue(keys.contains(new Key<String>("property1", true)));
        List<Qualifier<?>> qualifiers = restQuery.getQualifiers();
        assertEquals(1, qualifiers.size());
        Qualifier<?> qual = qualifiers.get(0);
        assertEquals(new Key<String>("property1", true), qual.getKey());
        assertEquals(Criteria.EQUALS, qual.getCriteria());
        assertEquals("fluff", qual.getValue());

        assertEquals("fluffor", result.getProperty1());
    }

    @Test
    public void testFindAllPojos() {
        RESTStorage storage = new RESTStorage();
        storage.setEndpoint("http://localhost:" + port + "/");

        TestObj obj1 = new TestObj();
        obj1.setProperty1("fluffor1");
        TestObj obj2 = new TestObj();
        obj2.setProperty1("fluffor2");
        Gson gson = new Gson();
        responseBody = gson.toJson(Arrays.asList(obj1, obj2));

        Key<String> key1 = new Key<>("property1", true);
        Query query = storage.createQuery().from(category).where(key1, Criteria.EQUALS, "fluff");

        Cursor<TestObj> results = storage.findAllPojos(query, TestObj.class);
        RESTQuery restQuery = gson.fromJson(requestBody, RESTQuery.class);

        Category actualCategory = restQuery.getCategory();
        assertEquals("test", actualCategory.getName());
        Collection<Key<?>> keys = actualCategory.getKeys();
        assertEquals(1, keys.size());
        assertTrue(keys.contains(new Key<String>("property1", true)));
        List<Qualifier<?>> qualifiers = restQuery.getQualifiers();
        assertEquals(1, qualifiers.size());
        Qualifier<?> qual = qualifiers.get(0);
        assertEquals(new Key<String>("property1", true), qual.getKey());
        assertEquals(Criteria.EQUALS, qual.getCriteria());
        assertEquals("fluff", qual.getValue());

        assertTrue(results.hasNext());
        assertEquals("fluffor1", results.next().getProperty1());
        assertTrue(results.hasNext());
        assertEquals("fluffor2", results.next().getProperty1());
        assertFalse(results.hasNext());
    }

    @Test
    public void testPut() throws IOException, JsonSyntaxException, ClassNotFoundException {
        RESTStorage storage = new RESTStorage();
        storage.setEndpoint("http://localhost:" + port + "/");
        TestObj obj = new TestObj();
        obj.setProperty1("fluff");

        storage.putPojo(category, true, obj);

        Gson gson = new Gson();
        StringReader reader = new StringReader(requestBody);
        BufferedReader bufRead = new BufferedReader(reader);
        String line = bufRead.readLine();
        String [] params = line.split("&");
        assertEquals(2, params.length);
        String[] parts = params[0].split("=");
        assertEquals("insert", parts[0]);
        WebInsert insert = gson.fromJson(parts[1], WebInsert.class);
        assertEquals(category, insert.getCategory());
        assertEquals(true, insert.isReplace());
        assertEquals(TestObj.class.getName(), insert.getPojoClass());

        parts = params[1].split("=");
        assertEquals(2, parts.length);
        assertEquals("pojo", parts[0]);
        Object resultObj = gson.fromJson(parts[1], Class.forName(insert.getPojoClass()));
        assertEquals(obj, resultObj);
    }

}
