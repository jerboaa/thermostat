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

package com.redhat.thermostat.web.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.gson.Gson;
import com.redhat.thermostat.storage.core.Categories;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.core.Query.SortDirection;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.test.FreePortFinder;
import com.redhat.thermostat.test.FreePortFinder.TryPort;
import com.redhat.thermostat.web.common.StorageWrapper;
import com.redhat.thermostat.web.common.WebInsert;
import com.redhat.thermostat.web.common.WebQuery;
import com.redhat.thermostat.web.common.WebRemove;
import com.redhat.thermostat.web.common.WebUpdate;

public class WebStorageEndpointTest {

    @Entity
    public static class TestClass extends BasePojo {
        private String key1;
        private int key2;
        @Persist
        public String getKey1() {
            return key1;
        }
        @Persist
        public void setKey1(String key1) {
            this.key1 = key1;
        }
        @Persist
        public int getKey2() {
            return key2;
        }
        @Persist
        public void setKey2(int key2) {
            this.key2 = key2;
        }
        public boolean equals(Object o) {
            if (! (o instanceof TestClass)) {
                return false;
            }
            TestClass other = (TestClass) o;
            return key1.equals(other.key1) && key2 == other.key2;
        }
    }

    private Server server;
    private int port;
    private Storage mockStorage;
    private Integer categoryId;

    private static Key<String> key1;
    private static Key<Integer> key2;
    private static Category<TestClass> category;

    @BeforeClass
    public static void setupCategory() {
        key1 = new Key<>("key1", true);
        key2 = new Key<>("key2", false);
        category = new Category<>("test", TestClass.class, key1, key2);
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
        registerCategory();
    }

    private void startServer(int port) throws Exception {
        server = new Server(port);
        WebAppContext ctx = new WebAppContext("src/main/webapp", "/");
        ctx.getSecurityHandler().setAuthMethod("BASIC");
        ctx.getSecurityHandler().setLoginService(new MappedLoginService() {
            
            @Override
            protected void loadUsers() throws IOException {
                putUser("testname", new Password("testpasswd"), new String[] { "thermostat-agent" });
                putUser("test-no-role", new Password("testpasswd"), new String[] { "fluff" });
                putUser("test-cmd-channel", new Password("testpasswd"), new String[] { "thermostat-cmd-channel" });
            }
            
            @Override
            protected UserIdentity loadUser(String username) {
                if (username.equals("test-cmd-channel")) {
                    return new DefaultUserIdentity(null, null, new String[] { "thermostat-cmd-channel" });
                }
                return new DefaultUserIdentity(null, null, new String[] { "thermostat-agent" });
            }
        });
        server.setHandler(ctx);
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        server.join();
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

        Query mockQuery = mock(Query.class);
        when(mockStorage.createQuery(any(Category.class))).thenReturn(mockQuery);
        when(mockQuery.execute()).thenReturn(cursor);

        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/find-all");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        Map<Category,Integer> categoryIdMap = new HashMap<>();
        categoryIdMap.put(category, categoryId);
        WebQuery query = new WebQuery(categoryId);
        query.where(key1, Criteria.EQUALS, "fluff");
        query.sort(key1, SortDirection.DESCENDING);
        query.limit(42);
        Gson gson = new Gson();
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        String body = "query=" + URLEncoder.encode(gson.toJson(query), "UTF-8");
        out.write(body + "\n");
        out.flush();

        Reader in = new InputStreamReader(conn.getInputStream());
        TestClass[] results = gson.fromJson(in, TestClass[].class);
        assertEquals(2, results.length);
        assertEquals("fluff1", results[0].getKey1());
        assertEquals(42, results[0].getKey2());
        assertEquals("fluff2", results[1].getKey1());
        assertEquals(43, results[1].getKey2());

        verify(mockQuery).where(key1, Criteria.EQUALS, "fluff");
        verify(mockQuery).sort(key1, SortDirection.DESCENDING);
        verify(mockQuery).limit(42);
        verify(mockQuery).execute();
        verifyNoMoreInteractions(mockQuery);
    }

    @Test
    public void testPutPojo() throws IOException {

        Replace replace = mock(Replace.class);
        when(mockStorage.createReplace(any(Category.class))).thenReturn(replace);

        TestClass expected1 = new TestClass();
        expected1.setKey1("fluff1");
        expected1.setKey2(42);

        String endpoint = getEndpoint();

        URL url = new URL(endpoint + "/put-pojo");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        sendAuthorization(conn, "testname", "testpasswd");

        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        WebInsert insert = new WebInsert(categoryId, true);
        Gson gson = new Gson();
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("insert=");
        gson.toJson(insert, out);
        out.flush();
        out.write("&pojo=");
        gson.toJson(expected1, out);
        out.write("\n");
        out.flush();
        assertEquals(200, conn.getResponseCode());
        verify(mockStorage).createReplace(category);
        verify(replace).setPojo(expected1);
        verify(replace).apply();
    }

    private void sendAuthorization(HttpURLConnection conn, String username, String passwd) {
        String userpassword = username + ":" + passwd;
        String encodedAuthorization = Base64.encodeBase64String(userpassword.getBytes());
        conn.setRequestProperty("Authorization", "Basic "+ encodedAuthorization);
    }

    @Test
    public void testRemovePojo() throws IOException {

        Remove mockRemove = mock(Remove.class);
        when(mockRemove.from(any(Category.class))).thenReturn(mockRemove);
        when(mockRemove.where(any(Key.class), any())).thenReturn(mockRemove);

        when(mockStorage.createRemove()).thenReturn(mockRemove);

        String endpoint = getEndpoint();

        URL url = new URL(endpoint + "/remove-pojo");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        Map<Category<?>,Integer> categoryIds = new HashMap<>();
        categoryIds.put(category, categoryId);
        WebRemove remove = new WebRemove(categoryIds).from(category).where(key1, "test");
        Gson gson = new Gson();
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("remove=");
        gson.toJson(remove, out);
        out.write("\n");
        out.flush();

        assertEquals(200, conn.getResponseCode());
        verify(mockStorage).createRemove();
        verify(mockRemove).from(category);
        verify(mockRemove).where(key1, "test");
        verify(mockStorage).removePojo(mockRemove);
    }

    @Test
    public void testUpdatePojo() throws IOException {

        Update mockUpdate = mock(Update.class);
        when(mockStorage.createUpdate(any(Category.class))).thenReturn(mockUpdate);

        String endpoint = getEndpoint();

        URL url = new URL(endpoint + "/update-pojo");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        WebUpdate update = new WebUpdate();
        update.setCategoryId(categoryId);
        update.where(key1, "test");
        update.set(key1, "fluff");
        update.set(key2, 42);

        Gson gson = new Gson();
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("update=");
        gson.toJson(update, out);
        out.write("&values=");
        gson.toJson(new Object[] {"fluff", 42 }, out);
        out.write("\n");
        out.flush();

        assertEquals(200, conn.getResponseCode());
        verify(mockStorage).createUpdate(category);
        verify(mockUpdate).where(key1, "test");
        verify(mockUpdate).set(key1, "fluff");
        verify(mockUpdate).set(key2, 42);
        verify(mockUpdate).apply();
        verifyNoMoreInteractions(mockUpdate);
    }


    @Test
    public void testGetCount() throws IOException {

        when(mockStorage.getCount(category)).thenReturn(12345L);
        String endpoint = getEndpoint();

        URL url = new URL(endpoint + "/get-count");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        Gson gson = new Gson();
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("category=" + categoryId);
        out.flush();

        InputStream in = conn.getInputStream();
        Reader reader = new InputStreamReader(in);
        long result = gson.fromJson(reader, Long.class);
        assertEquals(200, conn.getResponseCode());
        assertEquals(12345, result);
        verify(mockStorage).getCount(category);
        
    }

    @Test
    public void testSaveFile() throws IOException {
        String endpoint = getEndpoint();

        URL url = new URL(endpoint + "/save-file");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=fluff");
        conn.setRequestProperty("Transfer-Encoding", "chunked");
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("--fluff\r\n");
        out.write("Content-Disposition: form-data; name=\"file\"; filename=\"fluff\"\r\n");
        out.write("Content-Type: application/octet-stream\r\n");
        out.write("Content-Transfer-Encoding: binary\r\n");
        out.write("\r\n");
        out.write("Hello World\r\n");
        out.write("--fluff--\r\n");
        out.flush();
        int status = conn.getResponseCode();
        ArgumentCaptor<InputStream> inCaptor = ArgumentCaptor.forClass(InputStream.class);
        verify(mockStorage).saveFile(eq("fluff"), inCaptor.capture());
        InputStream in = inCaptor.getValue();
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
    public void testLoadFile() throws IOException {

        byte[] data = "Hello World".getBytes();
        InputStream in = new ByteArrayInputStream(data);
        when(mockStorage.loadFile("fluff")).thenReturn(in);

        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/load-file");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("file=fluff");
        out.flush();
        in = conn.getInputStream();
        data = new byte[11];
        int totalRead = 0;
        while (totalRead < 11) {
            int read = in.read(data, totalRead, 11 - totalRead);
            if (read < 0) {
                fail();
            }
            totalRead += read;
        }
        assertEquals("Hello World", new String(data));
        verify(mockStorage).loadFile("fluff");
    }

    @Test
    public void testPurge() throws IOException {
        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/purge");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        int status = conn.getResponseCode();
        assertEquals(200, status);
        verify(mockStorage).purge();
    }

    private void registerCategory() {
        try {
            String endpoint = getEndpoint();
            URL url = new URL(endpoint + "/register-category");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String enc = "UTF-8";
            conn.setRequestProperty("Content-Encoding", enc);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            OutputStream out = conn.getOutputStream();
            Gson gson = new Gson();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.write("name=");
            writer.write(URLEncoder.encode(category.getName(), enc));
            writer.write("&category=");
            writer.write(URLEncoder.encode(gson.toJson(category), enc));
            writer.flush();

            InputStream in = conn.getInputStream();
            Reader reader = new InputStreamReader(in);
            Integer id = gson.fromJson(reader, Integer.class);
            categoryId = id;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getEndpoint() {
        return "http://localhost:" + port + "/storage";
    }

    @Test
    public void testBasicGenerateToken() throws IOException {
        
        verifyGenerateToken();
    }

    @Test
    public void testGenerateTokenWithoutAuth() throws IOException {
        
        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/generate-token");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        sendAuthorization(conn, "test-no-role", "testpasswd");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("client-token=fluff");
        out.flush();
        assertEquals(401, conn.getResponseCode());
    }

    @Test
    public void testBasicGenerateVerifyToken() throws IOException {
        
        byte[] token = verifyGenerateToken();

        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/verify-token");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        sendAuthorization(conn, "test-cmd-channel", "testpasswd");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("client-token=fluff&token=" + URLEncoder.encode(Base64.encodeBase64String(token), "UTF-8"));
        out.flush();
        assertEquals(200, conn.getResponseCode());
    }

    @Test
    public void testTokenTimeout() throws IOException, InterruptedException {
        
        byte[] token = verifyGenerateToken();

        Thread.sleep(700); // Timeout is set to 500ms for tests, 700ms should be enough for everybody. ;-)

        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/verify-token");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        sendAuthorization(conn, "test-cmd-channel", "testpasswd");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("client-token=fluff&token=" + URLEncoder.encode(Base64.encodeBase64String(token), "UTF-8"));
        out.flush();
        assertEquals(401, conn.getResponseCode());
    }

    @Test
    public void testVerifyNonExistentToken() throws IOException {
        
        byte[] token = "fluff".getBytes();

        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/verify-token");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        sendAuthorization(conn, "test-cmd-channel", "testpasswd");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("client-token=fluff&token=" + URLEncoder.encode(Base64.encodeBase64String(token), "UTF-8"));
        out.flush();
        assertEquals(401, conn.getResponseCode());
    }

    private byte[] verifyGenerateToken() throws IOException {
        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/generate-token");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        sendAuthorization(conn, "test-cmd-channel", "testpasswd");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("client-token=fluff");
        out.flush();
        InputStream in = conn.getInputStream();
        int length = conn.getContentLength();
        byte[] token  = new byte[length];
        assertEquals(256, length);
        int totalRead = 0;
        while (totalRead < length) {
            int read = in.read(token, totalRead, length - totalRead);
            if (read < 0) {
                fail();
            }
            totalRead += read;
        }
        return token;
    }
}

