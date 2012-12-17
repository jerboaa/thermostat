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


package com.redhat.thermostat.web.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.redhat.thermostat.storage.config.StartupConfiguration;
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.storage.core.Categories;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.test.FreePortFinder;
import com.redhat.thermostat.test.FreePortFinder.TryPort;
import com.redhat.thermostat.web.common.Qualifier;
import com.redhat.thermostat.web.common.WebInsert;
import com.redhat.thermostat.web.common.WebQuery;
import com.redhat.thermostat.web.common.WebRemove;
import com.redhat.thermostat.web.common.WebUpdate;

public class WebStorageTest {

    private Server server;

    private int port;

    private String requestBody;

    private String responseBody;
    private Map<String,String> headers;
    private String method;
    private String requestURI;
    private int responseStatus;

    private static Category category;
    private static Key<String> key1;
    private static Key<Integer> key2;

    private WebStorage storage;

    @BeforeClass
    public static void setupCategory() {
        key1 = new Key<>("property1", true);
        key2 = new Key<>("property2", true);
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

        StartupConfiguration config = new StartupConfiguration() {
            
            @Override
            public String getDBConnectionString() {
                return "http://fluff.example.org";
            }
        };
        Executor exec = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        storage = new WebStorage(config, exec);
        storage.setEndpoint("http://localhost:" + port + "/");
        storage.setAgentId(new UUID(123, 456));
        headers = new HashMap<>();
        requestURI = null;
        method = null;
        responseStatus = HttpServletResponse.SC_OK;
        registerCategory();
    }

    private void startServer(int port) throws Exception {
        server = new Server(port);
        server.setHandler(new AbstractHandler() {
            
            @Override
            public void handle(String target, Request baseRequest,
                    HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    headers.put(headerName, request.getHeader(headerName));
                }

                method = request.getMethod();
                requestURI = request.getRequestURI();

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
                response.setStatus(responseStatus);
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

        headers = null;
        requestURI = null;
        method = null;
        storage = null;

        server.stop();
        server.join();
    }

    private void registerCategory() {

        // Return 42 for categoryId.
        Gson gson = new Gson();
        responseBody = gson.toJson(42);

        storage.registerCategory(category);
    }

    @Test
    public void testFindPojo() throws UnsupportedEncodingException, IOException {

        TestObj obj = new TestObj();
        obj.setProperty1("fluffor");
        Gson gson = new Gson();
        responseBody = gson.toJson(obj);

        Query query = storage.createQuery().from(category).where(key1, Criteria.EQUALS, "fluff");

        TestObj result = storage.findPojo(query, TestObj.class);
        StringReader reader = new StringReader(requestBody);
        BufferedReader bufRead = new BufferedReader(reader);
        String line = URLDecoder.decode(bufRead.readLine(), "UTF-8");
        String[] parts = line.split("=");
        assertEquals("query", parts[0]);
        WebQuery restQuery = gson.fromJson(parts[1], WebQuery.class);

        assertEquals(42, restQuery.getCategoryId());
        List<Qualifier<?>> qualifiers = restQuery.getQualifiers();
        assertEquals(1, qualifiers.size());
        Qualifier<?> qual = qualifiers.get(0);
        assertEquals(new Key<String>("property1", true), qual.getKey());
        assertEquals(Criteria.EQUALS, qual.getCriteria());
        assertEquals("fluff", qual.getValue());

        assertEquals("fluffor", result.getProperty1());
    }

    @Test
    public void testFindAllPojos() throws UnsupportedEncodingException, IOException {

        TestObj obj1 = new TestObj();
        obj1.setProperty1("fluffor1");
        TestObj obj2 = new TestObj();
        obj2.setProperty1("fluffor2");
        Gson gson = new Gson();
        responseBody = gson.toJson(Arrays.asList(obj1, obj2));

        Key<String> key1 = new Key<>("property1", true);
        Query query = storage.createQuery().from(category).where(key1, Criteria.EQUALS, "fluff");

        Cursor<TestObj> results = storage.findAllPojos(query, TestObj.class);
        StringReader reader = new StringReader(requestBody);
        BufferedReader bufRead = new BufferedReader(reader);
        String line = URLDecoder.decode(bufRead.readLine(), "UTF-8");
        String[] parts = line.split("=");
        assertEquals("query", parts[0]);
        WebQuery restQuery = gson.fromJson(parts[1], WebQuery.class);

        assertEquals(42, restQuery.getCategoryId());
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

        TestObj obj = new TestObj();
        obj.setProperty1("fluff");

        storage.putPojo(category, true, obj);

        Gson gson = new Gson();
        StringReader reader = new StringReader(requestBody);
        BufferedReader bufRead = new BufferedReader(reader);
        String line = URLDecoder.decode(bufRead.readLine(), "UTF-8");
        String [] params = line.split("&");
        assertEquals(2, params.length);
        String[] parts = params[0].split("=");
        assertEquals("insert", parts[0]);
        WebInsert insert = gson.fromJson(parts[1], WebInsert.class);
        assertEquals(42, insert.getCategoryId());
        assertEquals(true, insert.isReplace());
        assertEquals(TestObj.class.getName(), insert.getPojoClass());

        parts = params[1].split("=");
        assertEquals(2, parts.length);
        assertEquals("pojo", parts[0]);
        Object resultObj = gson.fromJson(parts[1], Class.forName(insert.getPojoClass()));
        assertEquals(obj, resultObj);
    }

    @Test
    public void testCreateRemove() {
        WebRemove remove = (WebRemove) storage.createRemove();
        assertNotNull(remove);
        remove = remove.from(category);
        assertEquals(42, remove.getCategoryId());
        assertNotNull(remove);
        remove = remove.where(key1, "test");
        assertNotNull(remove);
        List<Qualifier<?>> qualifiers = remove.getQualifiers();
        assertEquals(1, qualifiers.size());
        Qualifier<?> qualifier = qualifiers.get(0);
        assertEquals(key1, qualifier.getKey());
        assertEquals(Criteria.EQUALS, qualifier.getCriteria());
        assertEquals("test", qualifier.getValue());
    }

    @Test
    public void testRemovePojo() throws UnsupportedEncodingException, IOException {
        Remove remove = storage.createRemove().from(category).where(key1, "test");
        storage.removePojo(remove);

        Gson gson = new Gson();
        StringReader reader = new StringReader(requestBody);
        BufferedReader bufRead = new BufferedReader(reader);
        String line = URLDecoder.decode(bufRead.readLine(), "UTF-8");
        String[] parts = line.split("=");
        assertEquals("remove", parts[0]);
        WebRemove actualRemove = gson.fromJson(parts[1], WebRemove.class);
        
        assertEquals(42, actualRemove.getCategoryId());
        List<Qualifier<?>> qualifiers = actualRemove.getQualifiers();
        assertEquals(1, qualifiers.size());
        Qualifier<?> qualifier = qualifiers.get(0);
        assertEquals(key1, qualifier.getKey());
        assertEquals(Criteria.EQUALS, qualifier.getCriteria());
        assertEquals("test", qualifier.getValue());
    }

    @Test
    public void testCreateUpdate() {
        WebUpdate update = (WebUpdate) storage.createUpdate();
        assertNotNull(update);
        update = update.from(category);
        assertEquals(42, update.getCategoryId());
        assertNotNull(update);
        update = update.where(key1, "test");
        assertNotNull(update);
        List<Qualifier<?>> qualifiers = update.getQualifiers();
        assertEquals(1, qualifiers.size());
        Qualifier<?> qualifier = qualifiers.get(0);
        assertEquals(key1, qualifier.getKey());
        assertEquals(Criteria.EQUALS, qualifier.getCriteria());
        assertEquals("test", qualifier.getValue());
        update = update.set(key1, "fluff");
        assertNotNull(update);
        List<WebUpdate.UpdateValue> updates = update.getUpdates();
        assertEquals(1, updates.size());
        assertEquals("fluff", updates.get(0).getValue());
        assertEquals(key1, updates.get(0).getKey());
        assertEquals("java.lang.String", updates.get(0).getValueClass());
    }

    @Test
    public void testUpdate() throws UnsupportedEncodingException, IOException, JsonSyntaxException, ClassNotFoundException {

        WebUpdate update = storage.createUpdate().from(category).where(key1, "test").set(key1, "fluff").set(key2, 42);
        storage.updatePojo(update);

        Gson gson = new Gson();
        StringReader reader = new StringReader(requestBody);
        BufferedReader bufRead = new BufferedReader(reader);
        String line = URLDecoder.decode(bufRead.readLine(), "UTF-8");
        String [] params = line.split("&");
        assertEquals(2, params.length);
        String[] parts = params[0].split("=");
        assertEquals("update", parts[0]);
        WebUpdate receivedUpdate = gson.fromJson(parts[1], WebUpdate.class);
        assertEquals(42, receivedUpdate.getCategoryId());

        List<WebUpdate.UpdateValue> updates = receivedUpdate.getUpdates();
        assertEquals(2, updates.size());

        WebUpdate.UpdateValue update1 = updates.get(0);
        assertEquals(key1, update1.getKey());
        assertEquals("java.lang.String", update1.getValueClass());
        assertNull(update1.getValue());

        WebUpdate.UpdateValue update2 = updates.get(1);
        assertEquals(key2, update2.getKey());
        assertEquals("java.lang.Integer", update2.getValueClass());
        assertNull(update2.getValue());

        List<Qualifier<?>> qualifiers = receivedUpdate.getQualifiers();
        assertEquals(1, qualifiers.size());
        Qualifier<?> qualifier = qualifiers.get(0);
        assertEquals(key1, qualifier.getKey());
        assertEquals(Criteria.EQUALS, qualifier.getCriteria());
        assertEquals("test", qualifier.getValue());

        parts = params[1].split("=");
        assertEquals(2, parts.length);
        assertEquals("values", parts[0]);
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(parts[1]).getAsJsonArray();
        String value1 = gson.fromJson(jsonArray.get(0), String.class);
        assertEquals("fluff", value1);
        int value2 = gson.fromJson(jsonArray.get(1), Integer.class);
        assertEquals(42, value2);
    }

    @Test
    public void testGetCount() throws UnsupportedEncodingException, IOException {

        Gson gson = new Gson();
        responseBody = gson.toJson(12345);

        long result = storage.getCount(category);

        StringReader reader = new StringReader(requestBody);
        BufferedReader bufRead = new BufferedReader(reader);
        String line = URLDecoder.decode(bufRead.readLine(), "UTF-8");
        String[] parts = line.split("=");
        assertEquals("category", parts[0]);
        assertEquals("42", parts[1]);
        assertEquals(12345, result);
    }

    @Test
    public void testSaveFile() {
        String data = "Hello World";
        ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes());
        storage.saveFile("fluff", in);
        assertEquals("chunked", headers.get("Transfer-Encoding"));
        String contentType = headers.get("Content-Type");
        assertTrue(contentType.startsWith("multipart/form-data; boundary="));
        String boundary = contentType.split("boundary=")[1];
        String[] lines = requestBody.split("\n");
        assertEquals("--" + boundary, lines[0].trim());
        assertEquals("Content-Disposition: form-data; name=\"file\"; filename=\"fluff\"", lines[1].trim());
        assertEquals("Content-Type: application/octet-stream", lines[2].trim());
        assertEquals("Content-Transfer-Encoding: binary", lines[3].trim());
        assertEquals("", lines[4].trim());
        assertEquals("Hello World", lines[5].trim());
        assertEquals("--" + boundary + "--", lines[6].trim());
        
    }

    @Test
    public void testLoadFile() throws IOException {
        responseBody = "Hello World";
        InputStream in = storage.loadFile("fluff");
        assertEquals("file=fluff", requestBody.trim());
        byte[] data = new byte[11];
        int totalRead = 0;
        while (totalRead < 11) {
            int read = in.read(data, totalRead, 11 - totalRead);
            if (read < 0) {
                fail();
            }
            totalRead += read;
        }
        assertEquals("Hello World", new String(data));

    }

    @Test
    public void testPurge() {
        storage.purge();
        assertEquals("POST", method);
        assertTrue(requestURI.endsWith("/purge"));
    }

    @Test
    public void testGenerateToken() throws UnsupportedEncodingException {
        responseBody = "flufftoken";

        AuthToken authToken = storage.generateToken();

        assertTrue(requestURI.endsWith("/generate-token"));
        assertEquals("POST", method);

        String[] requestParts = requestBody.split("=");
        assertEquals("client-token", requestParts[0]);
        String clientTokenParam = URLDecoder.decode(requestParts[1], "UTF-8");
        byte[] clientToken = Base64.decodeBase64(clientTokenParam);
        assertEquals(256, clientToken.length);

        assertTrue(authToken instanceof AuthToken);
        AuthToken token = (AuthToken) authToken;
        byte[] tokenBytes = token.getToken();
        assertEquals("flufftoken", new String(tokenBytes));

        assertTrue(Arrays.equals(clientToken, token.getClientToken()));

        // Send another request and verify that we send a different client-token every time.
        storage.generateToken();

        requestParts = requestBody.split("=");
        assertEquals("client-token", requestParts[0]);
        clientTokenParam = URLDecoder.decode(requestParts[1], "UTF-8");
        byte[] clientToken2 = Base64.decodeBase64(clientTokenParam);
        assertFalse(Arrays.equals(clientToken, clientToken2));

    }

    @Test
    public void canSSLEnableClient() {
        StartupConfiguration config = new StartupConfiguration() {
            
            @Override
            public String getDBConnectionString() {
                return "https://onlyHttpsPrefixUsed.example.com";
            }
        };
        storage = new WebStorage(config);
        HttpClient client = storage.httpClient;
        SchemeRegistry schemeReg = client.getConnectionManager().getSchemeRegistry();
        Scheme scheme = schemeReg.getScheme("https");
        assertNotNull(scheme);
        assertEquals(443, scheme.getDefaultPort());
    }

    @Test
    public void testVerifyToken() throws UnsupportedEncodingException {
        byte[] token = "stuff".getBytes();
        byte[] clientToken = "fluff".getBytes();
        AuthToken authToken = new AuthToken(token, clientToken);

        responseStatus = 200;
        boolean ok = storage.verifyToken(authToken);
        assertTrue(requestURI.endsWith("/verify-token"));
        assertEquals("POST", method);
        String[] requestParts = requestBody.split("&");
        assertEquals(2, requestParts.length);
        String[] clientTokenParts = requestParts[0].split("=");
        assertEquals(2, clientTokenParts.length);
        assertEquals("client-token", clientTokenParts[0]);
        String urlDecoded = URLDecoder.decode(clientTokenParts[1], "UTF-8");
        String base64decoded = new String(Base64.decodeBase64(urlDecoded));
        assertEquals("fluff", base64decoded);
        String[] authTokenParts = requestParts[1].split("=");
        assertEquals(2, authTokenParts.length);
        assertEquals("token", authTokenParts[0]);
        urlDecoded = URLDecoder.decode(authTokenParts[1], "UTF-8");
        base64decoded = new String(Base64.decodeBase64(urlDecoded));
        assertEquals("stuff", base64decoded);
        assertTrue(ok);

        // Try another one in which verification fails.
        responseStatus = 401;
        ok = storage.verifyToken(authToken);
        assertFalse(ok);
        
    }
}
