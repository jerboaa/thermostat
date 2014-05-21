/*
 * Copyright 2012-2014 Red Hat, Inc.
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
import static org.mockito.Mockito.mock;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.config.StartupConfiguration;
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.storage.core.Categories;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.BackingStorage;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.IllegalDescriptorException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.PreparedParameters;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.test.FreePortFinder;
import com.redhat.thermostat.test.FreePortFinder.TryPort;
import com.redhat.thermostat.web.common.PreparedParameterSerializer;
import com.redhat.thermostat.web.common.PreparedStatementResponseCode;
import com.redhat.thermostat.web.common.ThermostatGSONConverter;
import com.redhat.thermostat.web.common.WebPreparedStatement;
import com.redhat.thermostat.web.common.WebPreparedStatementResponse;
import com.redhat.thermostat.web.common.WebPreparedStatementSerializer;
import com.redhat.thermostat.web.common.WebQueryResponse;
import com.redhat.thermostat.web.common.WebQueryResponseSerializer;

public class WebStorageTest {

    private Server server;

    private int port;

    // Set these in prepareServer() to determine server behaviour
    private int responseStatus = HttpServletResponse.SC_OK;
    private String responseBody;

    // These get set by test server handler (anonymous class in startServer())
    // Check them after WebStorage method call that should interact with server.
    private String requestBody;
    private Map<String,String> headers;
    private String method;
    private String requestURI;

    private static Category<TestObj> category;
    private static Key<String> key1;

    private WebStorage storage;


    @BeforeClass
    public static void setupCategory() {
        key1 = new Key<>("property1");
        category = new Category<>("test", TestObj.class, key1);
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

        SSLConfiguration sslConf = mock(SSLConfiguration.class);
        storage = new WebStorage("http://localhost:" + port + "/", 
                new TrivialStorageCredentials(null, null), sslConf);
        headers = new HashMap<>();
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

    // Specified status and response body.
    private void prepareServer(int responseStatus, String responseBody) {
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;

        requestBody = null;
        requestURI = null;
        method = null;
        headers.clear();
    }

    // Specified status and null response body.
    private void prepareServer(int responseStatus) {
        prepareServer(responseStatus, null);
    }

    // OK status and specified response body.
    private void prepareServer(String responseBody) {
        prepareServer(HttpServletResponse.SC_OK, responseBody);
    }

    // OK status and null response body.
    private void prepareServer() {
        prepareServer(HttpServletResponse.SC_OK);
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
    
    // WebStorage is a proxy storage, no backing storage
    @Test
    public void isNoBackingStorage() {
        assertFalse(storage instanceof BackingStorage);
    }
    
    @Test
    public void preparingFaultyDescriptorThrowsException() throws UnsupportedEncodingException, IOException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(WebPreparedStatement.class, new WebPreparedStatementSerializer())
                .create();

        // missing quotes for LHS key
        String strDesc = "QUERY test WHERE a = ?s";
        StatementDescriptor<TestObj> desc = new StatementDescriptor<>(category, strDesc);
        
        WebPreparedStatementResponse fakeResponse = new WebPreparedStatementResponse();
        fakeResponse.setStatementId(WebPreparedStatementResponse.DESCRIPTOR_PARSE_FAILED);
        prepareServer(gson.toJson(fakeResponse));
        try {
            storage.prepareStatement(desc);
            fail("Should have refused to prepare the statement");
        } catch (IllegalDescriptorException e) {
            // should have thrown superclass DescriptorParsingException
            fail(e.getMessage());
        } catch (DescriptorParsingException e) {
            // pass
        }
    }
    
    @Test
    public void preparingUnknownDescriptorThrowsException() throws UnsupportedEncodingException, IOException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(WebPreparedStatement.class, new WebPreparedStatementSerializer())
                .create();

        String strDesc = "QUERY test WHERE 'property1' = ?s";
        StatementDescriptor<TestObj> desc = new StatementDescriptor<>(category, strDesc);
        
        WebPreparedStatementResponse fakeResponse = new WebPreparedStatementResponse();
        fakeResponse.setStatementId(WebPreparedStatementResponse.ILLEGAL_STATEMENT);
        prepareServer(gson.toJson(fakeResponse));
        try {
            storage.prepareStatement(desc);
            fail("Should have refused to prepare the statement");
        } catch (IllegalDescriptorException e) {
            // pass
            assertEquals(strDesc, e.getFailedDescriptor());
        } catch (DescriptorParsingException e) {
            // should have thrown IllegalDescriptorException
            fail(e.getMessage());
        }
    }
    
    @Test
    public void canPrepareAndExecuteQuery() throws UnsupportedEncodingException, IOException {
        TestObj obj1 = new TestObj();
        obj1.setProperty1("fluffor1");
        TestObj obj2 = new TestObj();
        obj2.setProperty1("fluffor2");
        Gson gson = new GsonBuilder().registerTypeHierarchyAdapter(PreparedParameter.class, new PreparedParameterSerializer())
                .registerTypeAdapter(WebPreparedStatement.class, new WebPreparedStatementSerializer())
                .registerTypeAdapter(WebQueryResponse.class, new WebQueryResponseSerializer<>())
                .registerTypeAdapter(Pojo.class, new ThermostatGSONConverter())
                .create();

        String strDesc = "QUERY test WHERE 'property1' = ?s";
        StatementDescriptor<TestObj> desc = new StatementDescriptor<>(category, strDesc);
        PreparedStatement<TestObj> stmt = null;
        
        int fakePrepStmtId = 5;
        WebPreparedStatementResponse fakeResponse = new WebPreparedStatementResponse();
        fakeResponse.setNumFreeVariables(1);
        fakeResponse.setStatementId(fakePrepStmtId);
        prepareServer(gson.toJson(fakeResponse));
        try {
            stmt = storage.prepareStatement(desc);
        } catch (DescriptorParsingException e) {
            // descriptor should parse fine and is trusted
            fail(e.getMessage());
        }
        assertTrue(stmt instanceof WebPreparedStatement);
        WebPreparedStatement<TestObj> webStmt = (WebPreparedStatement<TestObj>)stmt;
        assertEquals(fakePrepStmtId, webStmt.getStatementId());
        PreparedParameters params = webStmt.getParams();
        assertEquals(1, params.getParams().length);
        assertNull(params.getParams()[0]);
        
        // now set a parameter
        stmt.setString(0, "fluff");
        assertEquals("fluff", params.getParams()[0].getValue());
        assertEquals(String.class, params.getParams()[0].getType());
        
        WebQueryResponse<TestObj> fakeQueryResponse = new WebQueryResponse<>();
        fakeQueryResponse.setResponseCode(PreparedStatementResponseCode.QUERY_SUCCESS);
        fakeQueryResponse.setResultList(new TestObj[] { obj1, obj2 });
        prepareServer(gson.toJson(fakeQueryResponse));
        Cursor<TestObj> results = null;
        try {
            results = stmt.executeQuery();
        } catch (StatementExecutionException e) {
            // should execute fine
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertNotNull(results);
        assertTrue(results instanceof WebCursor);
        assertTrue(results.hasNext());
        assertEquals("fluffor1", results.next().getProperty1());
        assertTrue(results.hasNext());
        assertEquals("fluffor2", results.next().getProperty1());
        assertFalse(results.hasNext());
        try {
            results.next();
            fail();
        } catch (NoSuchElementException ex) {
            // Pass.
        }
    }
    
    @Test
    public void canPrepareAndExecuteWrite() {
        TestObj obj1 = new TestObj();
        obj1.setProperty1("fluffor1");
        TestObj obj2 = new TestObj();
        obj2.setProperty1("fluffor2");
        Gson gson = new GsonBuilder().registerTypeAdapter(PreparedParameter.class, new PreparedParameterSerializer())
                .registerTypeAdapter(WebPreparedStatement.class, new WebPreparedStatementSerializer())
                .registerTypeHierarchyAdapter(Pojo.class, new ThermostatGSONConverter())
                .create();

        String strDesc = "ADD test SET 'property1' = ?s";
        StatementDescriptor<TestObj> desc = new StatementDescriptor<>(category, strDesc);
        PreparedStatement<TestObj> stmt = null;
        
        int fakePrepStmtId = 3;
        WebPreparedStatementResponse fakeResponse = new WebPreparedStatementResponse();
        fakeResponse.setNumFreeVariables(1);
        fakeResponse.setStatementId(fakePrepStmtId);
        prepareServer(gson.toJson(fakeResponse));
        try {
            stmt = storage.prepareStatement(desc);
        } catch (DescriptorParsingException e) {
            // descriptor should parse fine and is trusted
            fail(e.getMessage());
        }
        assertTrue(stmt instanceof WebPreparedStatement);
        WebPreparedStatement<TestObj> webStmt = (WebPreparedStatement<TestObj>)stmt;
        assertEquals(fakePrepStmtId, webStmt.getStatementId());
        PreparedParameters params = webStmt.getParams();
        assertEquals(1, params.getParams().length);
        assertNull(params.getParams()[0]);
        
        // now set a parameter
        stmt.setString(0, "fluff");
        assertEquals("fluff", params.getParams()[0].getValue());
        assertEquals(String.class, params.getParams()[0].getType());
        
        prepareServer(gson.toJson(PreparedStatementResponseCode.WRITE_GENERIC_FAILURE));
        
        int response = Integer.MAX_VALUE;
        try {
            response = stmt.execute();
        } catch (StatementExecutionException e) {
            // should execute fine
            e.printStackTrace();
            fail(e.getMessage());
        }
        
        assertEquals(PreparedStatementResponseCode.WRITE_GENERIC_FAILURE, response);
    }

    @Test
    public void testSaveFile() {
        String data = "Hello World";
        ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes());

        prepareServer();
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
        prepareServer(HttpServletResponse.SC_NO_CONTENT);
        InputStream in = storage.loadFile("no_file_here");
        assertNull(in);

        prepareServer("Hello World");
        in = storage.loadFile("fluff");
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
    public void testPurge() throws UnsupportedEncodingException, IOException {

        prepareServer();
        storage.purge("fluff");

        assertEquals("POST", method);
        assertTrue(requestURI.endsWith("/purge"));
        StringReader reader = new StringReader(requestBody);
        BufferedReader bufRead = new BufferedReader(reader);
        String line = URLDecoder.decode(bufRead.readLine(), "UTF-8");
        String[] parts = line.split("=");
        assertEquals("agentId", parts[0]);
        assertEquals("fluff", parts[1]);
    }

    @Test
    public void testGenerateToken() throws UnsupportedEncodingException {

        final String actionName = "some action";

        prepareServer("flufftoken");
        AuthToken authToken = storage.generateToken(actionName);

        assertTrue(requestURI.endsWith("/generate-token"));
        assertEquals("POST", method);

        String[] requestParts = requestBody.split("=");
        assertEquals("client-token", requestParts[0]);
        String clientTokenParam = URLDecoder.decode(requestParts[1], "UTF-8");
        byte[] clientToken = Base64.decodeBase64(clientTokenParam);
        assertEquals(256, clientToken.length);

        byte[] tokenBytes = authToken.getToken();
        assertEquals("flufftoken", new String(tokenBytes));

        assertTrue(Arrays.equals(clientToken, authToken.getClientToken()));

        // Send another request and verify that we send a different client-token every time.
        prepareServer("flufftoken");
        storage.generateToken(actionName);

        requestParts = requestBody.split("=");
        assertEquals("client-token", requestParts[0]);
        clientTokenParam = URLDecoder.decode(requestParts[1], "UTF-8");
        byte[] clientToken2 = Base64.decodeBase64(clientTokenParam);
        assertFalse(Arrays.equals(clientToken, clientToken2));

    }

    @Test
    public void canSSLEnableClient() {
        // This test doesn't use the class-wide storage+server setup.
        StartupConfiguration config = new StartupConfiguration() {
            
            @Override
            public String getDBConnectionString() {
                return "https://onlyHttpsPrefixUsed.example.com";
            }
        };
        SSLConfiguration sslConf = mock(SSLConfiguration.class);
        WebStorage storage = new WebStorage("https://onlyHttpsPrefixUsed.example.com",
                new TrivialStorageCredentials(null, null), sslConf);
        HttpClient client = storage.httpClient;
        SchemeRegistry schemeReg = client.getConnectionManager().getSchemeRegistry();
        Scheme scheme = schemeReg.getScheme("https");
        assertNotNull(scheme);
        assertEquals(443, scheme.getDefaultPort());
    }

    @Test
    public void testVerifyToken() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] token = "stuff".getBytes();
        String clientToken = "fluff";
        String someAction = "someAction";
        byte[] tokenDigest = getShaBytes(clientToken, someAction);
        
        AuthToken authToken = new AuthToken(token, tokenDigest);

        prepareServer();
        boolean ok = storage.verifyToken(authToken, someAction);

        assertTrue(ok);
        assertTrue(requestURI.endsWith("/verify-token"));
        assertEquals("POST", method);
        String[] requestParts = requestBody.split("&");
        assertEquals(3, requestParts.length);
        String[] clientTokenParts = requestParts[0].split("=");
        assertEquals(2, clientTokenParts.length);
        assertEquals("client-token", clientTokenParts[0]);
        String urlDecoded = URLDecoder.decode(clientTokenParts[1], "UTF-8");
        assertTrue(Arrays.equals(tokenDigest, Base64.decodeBase64(urlDecoded)));
        String[] authTokenParts = requestParts[1].split("=");
        assertEquals(2, authTokenParts.length);
        assertEquals("token", authTokenParts[0]);
        String[] actionParts = requestParts[2].split("=");
        assertEquals(2, actionParts.length);
        assertEquals("action-name", actionParts[0]);
        urlDecoded = URLDecoder.decode(authTokenParts[1], "UTF-8");
        String base64decoded = new String(Base64.decodeBase64(urlDecoded));
        assertEquals("stuff", base64decoded);

        // Try another one in which verification fails.
        prepareServer(HttpServletResponse.SC_UNAUTHORIZED);
        ok = storage.verifyToken(authToken, someAction);
        assertFalse(ok);
        
    }

    private byte[] getShaBytes(String clientToken, String someAction)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(clientToken.getBytes());
        digest.update(someAction.getBytes("UTF-8"));
        byte[] tokenDigest = digest.digest();
        return tokenDigest;
    }
    
    @Test
    public void verifyConnectFiresEventOnConnectionFailure() {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        ClientConnectionManager connManager = mock(ClientConnectionManager.class);
        // this should make connect fail
        Mockito.doThrow(RuntimeException.class).when(client).getCredentialsProvider();
        SSLConfiguration sslConf = mock(SSLConfiguration.class);
        storage = new WebStorage("http://localhost:" + port + "/", new TrivialStorageCredentials(null, null),
                client, connManager, sslConf);
        
        CountDownLatch latch = new CountDownLatch(1);
        MyListener listener = new MyListener(latch);
        storage.getConnection().addListener(listener);
        storage.getConnection().connect();
        // wait for connection to fail
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse(listener.connectEvent);
        assertTrue(listener.failedToConnectEvent);
    }
    
    @Test
    public void verifyConnectFiresEventOnSuccessfulConnect() {
        CountDownLatch latch = new CountDownLatch(1);
        MyListener listener = new MyListener(latch);
        storage.getConnection().addListener(listener);
        storage.getConnection().connect();
        // wait for connection to happen
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(listener.connectEvent);
        assertFalse(listener.failedToConnectEvent);
    }
    
    @Test
    public void verifyDisconnectFiresDisconnectEvent() {
        CountDownLatch latch = new CountDownLatch(1);
        MyListener listener = new MyListener(latch);
        storage.getConnection().addListener(listener);
        storage.getConnection().disconnect();
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse(listener.connectEvent);
        assertFalse(listener.failedToConnectEvent);
        assertTrue(listener.disconnectEvent);
    }
    
    static class MyListener implements ConnectionListener {

        private CountDownLatch latch;
        boolean failedToConnectEvent = false;
        boolean connectEvent = false;
        boolean disconnectEvent = false;
        
        MyListener(CountDownLatch latch) {
            this.latch = latch;
        }
        
        @Override
        public void changed(ConnectionStatus newStatus) {
            if (newStatus == ConnectionStatus.CONNECTED) {
                connectEvent = true;
                latch.countDown();
            }
            if (newStatus == ConnectionStatus.FAILED_TO_CONNECT) {
                failedToConnectEvent = true;
                latch.countDown();
            }
            if (newStatus == ConnectionStatus.DISCONNECTED) {
                disconnectEvent = true;
                latch.countDown();
            }
        }
    }

    private class TrivialStorageCredentials implements StorageCredentials {
        private String user;
        private char[] pw;
        private TrivialStorageCredentials(String user, char[] password) {
            this.user = user;
            this.pw = password;
        }
        @Override
        public String getUsername() {
            return user;
        }
        @Override
        public char[] getPassword() {
            return pw;
        }
    }
}

