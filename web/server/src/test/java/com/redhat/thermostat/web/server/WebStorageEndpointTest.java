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

package com.redhat.thermostat.web.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.AggregateQuery;
import com.redhat.thermostat.storage.core.AggregateQuery.AggregateFunction;
import com.redhat.thermostat.storage.core.BackingStorage;
import com.redhat.thermostat.storage.core.Categories;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.ParsedStatement;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.auth.CategoryRegistration;
import com.redhat.thermostat.storage.core.auth.DescriptorMetadata;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration;
import com.redhat.thermostat.storage.core.experimental.BatchCursor;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.query.BinarySetMembershipExpression;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;
import com.redhat.thermostat.test.FreePortFinder;
import com.redhat.thermostat.test.FreePortFinder.TryPort;
import com.redhat.thermostat.web.common.PreparedStatementResponseCode;
import com.redhat.thermostat.web.common.WebPreparedStatement;
import com.redhat.thermostat.web.common.WebPreparedStatementResponse;
import com.redhat.thermostat.web.common.WebQueryResponse;
import com.redhat.thermostat.web.common.typeadapters.PojoTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.PreparedParameterTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.PreparedParametersTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.WebPreparedStatementResponseTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.WebPreparedStatementTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.WebQueryResponseTypeAdapterFactory;
import com.redhat.thermostat.web.server.auth.BasicRole;
import com.redhat.thermostat.web.server.auth.RolePrincipal;
import com.redhat.thermostat.web.server.auth.Roles;
import com.redhat.thermostat.web.server.auth.UserPrincipal;

public class WebStorageEndpointTest {

    @Entity
    public static class TestClass extends BasePojo {
        
        public TestClass() {
            super(null);
        }
        
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
    private BackingStorage mockStorage;
    private Integer categoryId;

    private static Key<String> key1;
    private static Key<Integer> key2;
    private static Category<TestClass> category;
    private static String categoryName = "test";
    private static File testThermostatHome;

    @BeforeClass
    public static void setupCategory() {
        key1 = new Key<>("key1");
        key2 = new Key<>("key2");
        category = new Category<>(categoryName, TestClass.class, key1, key2);
    }

    @AfterClass
    public static void cleanupCategory() {
        Categories.remove(category);
        category = null;
        key2 = null;
        key1 = null;
        
        if (testThermostatHome != null) {
            Path testTh = testThermostatHome.toPath();
            try {
                WebstorageEndpointTestUtils.deleteDirectoryRecursive(testTh);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        
        mockStorage = mock(BackingStorage.class);
        StorageFactory.setStorage(mockStorage);
    }

    private void startServer(int port, LoginService loginService) throws Exception {
        server = new Server(port);
        // This makes the test use the web.xml in src/main/webapp. That's
        // also where the test's thermostat home is set up via 
        // the context listener. THERMOSTAT_HOME == /tmp/test-thermostat-home
        // See src/main/webapp/WEB-INF/web.xml
        final WebAppContext ctx = new WebAppContext("src/main/webapp", "/");
        ctx.getSecurityHandler().setLoginService(loginService);
        // We get access to the THERMOSTAT_HOME value once the context has
        // been started.
        ctx.addLifeCycleListener(new Listener() {

            @Override
            public void lifeCycleFailure(LifeCycle arg0, Throwable arg1) {
                // nothing
            }

            @Override
            public void lifeCycleStarted(LifeCycle arg0) {
                // In Servlet.init() we check if ssl.properties exists. We need
                // to create that file in order for the tests to get past this
                // check.
                File thermostatHome = new File(ctx.getInitParameter("THERMOSTAT_HOME"));
                if (testThermostatHome == null) {
                    testThermostatHome = thermostatHome;
                }
                if (!thermostatHome.exists()) {
                    thermostatHome.mkdir();
                }
                File configDirectory = new File(thermostatHome, "etc");
                if (!configDirectory.exists()) {
                    configDirectory.mkdir();
                }
                File sslProperties = new File(configDirectory, "ssl.properties");
                // only creates file if it doesn't exist yet
                try {
                    sslProperties.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void lifeCycleStarting(LifeCycle arg0) {
                // nothing
            }

            @Override
            public void lifeCycleStopped(LifeCycle arg0) {
                // nothing
            }

            @Override
            public void lifeCycleStopping(LifeCycle arg0) {
                // nothing
            }
            
        });
        server.setHandler(ctx);
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        
        // some tests don't use server
        if (server != null) {
            server.stop();
            server.join();
        }
        KnownCategoryRegistryFactory.setInstance(null);
        KnownDescriptorRegistryFactory.setKnownDescriptorRegistry(null);
    }

    @Test
    public void authorizedPrepareQueryWithUnTrustedDescriptor() throws Exception {
        String strDescriptor = "QUERY " + category.getName() + " WHERE '" + key1.getName() + "' = ?s SORT '" + key1.getName() + "' DSC LIMIT 42";
        // setup a statement descriptor set so as to mimic a not trusted desc
        String wrongDescriptor = "QUERY something-other WHERE 'a' = true";
        setupTrustedStatementRegistry(wrongDescriptor);
        
        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.PREPARE_STATEMENT,
                Roles.ACCESS_REALM,
        };
        String testuser = "testuser";
        String password = "testpassword";
        final LoginService loginService = new TestLoginService(testuser, password, roleNames); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        // This makes register category work for the "test" category.
        // Undone via @After
        setupTrustedCategory(categoryName);
        registerCategory(testuser, password);
        
        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/prepare-statement");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        sendAuthentication(conn, testuser, password);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        Gson gson = new GsonBuilder()
                        .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                        .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                        .registerTypeAdapterFactory(new WebQueryResponseTypeAdapterFactory())
                        .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                        .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                        .create();
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        String body = "query-descriptor=" + URLEncoder.encode(strDescriptor, "UTF-8") + "&category-id=" + categoryId;
        out.write(body + "\n");
        out.flush();

        Reader in = new InputStreamReader(conn.getInputStream());
        WebPreparedStatementResponse response = gson.fromJson(in, WebPreparedStatementResponse.class);
        assertEquals("descriptor not trusted, so expected number should be negative!", -1, response.getNumFreeVariables());
        assertEquals(WebPreparedStatementResponse.ILLEGAL_STATEMENT, response.getStatementId());
        assertEquals("application/json; charset=UTF-8", conn.getContentType());
    }

    /**
     * Tests a successful query execution, starting from preparing the query,
     * then executing the query and finally getting more results for the query.
     * 
     * @throws Exception
     * 
     * @see {@link #authorizedPrepareQueryWithTrustedDescriptorGetMoreFail()}
     */
    @SuppressWarnings({ "rawtypes" })
    @Test
    public void authorizedPrepareQueryWithTrustedDescriptorSuccessfulGetMore() throws Exception {
        // Get the trusted descriptor
        String strDescriptor = setupPreparedQueryWithTrustedDescriptor();
        
        // Prepare the query
        boolean moreBatches = true;
        TrustedPreparedQueryTestResult prepareQueryResult = prepareQuery(strDescriptor, moreBatches);
        
        Type typeToken = new TypeToken<WebQueryResponse<TestClass>>(){}.getType();
        // now execute the query we've just prepared
        WebPreparedStatement<TestClass> stmt = new WebPreparedStatement<>(1, 0);
        stmt.setString(0, "fluff");
        
        // Execute the query, preserver the cookie
        String cookieValue = executeQuery(prepareQueryResult.gson, prepareQueryResult.mockMongoQuery, typeToken, stmt, moreBatches);
        
        // Simulate getting more elements
        int cursorId = 0;
        int batchSize = 3;
        // stub the underlying cursor so that it can fill up a single batch (3)
        // and leaves one element more => 3 + 1.
        when(prepareQueryResult.cursor.hasNext()).thenReturn(true)  // 1
                              .thenReturn(true)  // 2
                              .thenReturn(true)  // 3
                              .thenReturn(true)  // 4
                              .thenReturn(false);
                              
                              
        when(prepareQueryResult.cursor.next())
                    .thenReturn(new TestClass()) // 1
                    .thenReturn(new TestClass()) // 2
                    .thenReturn(new TestClass()) // 3
                    .thenReturn(new TestClass()) // 4
                    .thenReturn(null);
        when(prepareQueryResult.cursor.getBatchSize()).thenReturn(batchSize);
        URL url = new URL(getEndpoint() + "/get-more");
        HttpURLConnection getMoreConn = (HttpURLConnection) url.openConnection();
        getMoreConn.setRequestMethod("POST");
        setCookie(getMoreConn, cookieValue);
        sendAuthentication(getMoreConn, "ignored1", "ignored2");
        getMoreConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        getMoreConn.setDoInput(true);
        getMoreConn.setDoOutput(true);
        
        OutputStreamWriter out = new OutputStreamWriter(getMoreConn.getOutputStream());
        String body = "prepared-stmt-id=" + stmt.getStatementId() + "&";
        body += "cursor-id=" + cursorId + "&";
        body += "batch-size=" + batchSize;
        out.write(body);
        out.flush();
        
        InputStreamReader in = new InputStreamReader(getMoreConn.getInputStream());
        WebQueryResponse result = prepareQueryResult.gson.fromJson(in, typeToken);
        assertEquals(3, result.getResultList().length);
        assertEquals(cursorId, result.getCursorId());
        assertTrue("There was one more result than the batch size", result.hasMoreBatches());
        assertEquals("application/json; charset=UTF-8", getMoreConn.getContentType());
        // expected setBatchSize to be called via get-more request
        verify(prepareQueryResult.cursor).setBatchSize(batchSize);
    }
    
    /**
     * Tests authorized prepared query which attempts to do a get-more but does
     * not have a cursor in the user's cursor manager.
     * 
     * @throws Exception
     * 
     * @see {@link #authorizedPrepareQueryWithTrustedDescriptorSuccessfulGetMore}
     */
    @SuppressWarnings({ "rawtypes" })
    @Test
    public void authorizedPrepareQueryWithTrustedDescriptorGetMoreFail() throws Exception {
        // Get the trusted descriptor
        String strDescriptor = setupPreparedQueryWithTrustedDescriptor();
        
        // Prepare the query
        boolean moreBatches = false;
        TrustedPreparedQueryTestResult prepareQueryResult = prepareQuery(strDescriptor, moreBatches);
        
        Type typeToken = new TypeToken<WebQueryResponse<TestClass>>(){}.getType();
        // now execute the query we've just prepared
        WebPreparedStatement<TestClass> stmt = new WebPreparedStatement<>(1, 0);
        stmt.setString(0, "fluff");
        
        // Execute the query, preserver the cookie
        String cookieValue = executeQuery(prepareQueryResult.gson, prepareQueryResult.mockMongoQuery, typeToken, stmt, moreBatches);
        
        // Simulate getting more elements
        int cursorId = 0;
        int batchSize = 3;
        // stub the underlying cursor so that it can fill up a single batch (3)
        // and leaves one element more => 3 + 1.
        when(prepareQueryResult.cursor.hasNext()).thenReturn(true)  // 1
                              .thenReturn(true)  // 2
                              .thenReturn(true)  // 3
                              .thenReturn(false)  // 4
                              .thenReturn(false);
                              
                              
        when(prepareQueryResult.cursor.next())
                    .thenReturn(new TestClass()) // 1
                    .thenReturn(new TestClass()) // 2
                    .thenReturn(new TestClass()) // 3
                    .thenReturn(null)            // 4
                    .thenReturn(null);
        when(prepareQueryResult.cursor.getBatchSize()).thenReturn(batchSize);
        URL url = new URL(getEndpoint() + "/get-more");
        HttpURLConnection getMoreConn = (HttpURLConnection) url.openConnection();
        getMoreConn.setRequestMethod("POST");
        setCookie(getMoreConn, cookieValue);
        sendAuthentication(getMoreConn, "ignored1", "ignored2");
        getMoreConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        getMoreConn.setDoInput(true);
        getMoreConn.setDoOutput(true);
        
        OutputStreamWriter out = new OutputStreamWriter(getMoreConn.getOutputStream());
        String body = "prepared-stmt-id=" + stmt.getStatementId() + "&";
        body += "cursor-id=" + cursorId + "&";
        body += "batch-size=" + batchSize;
        out.write(body);
        out.flush();
        
        InputStreamReader in = new InputStreamReader(getMoreConn.getInputStream());
        WebQueryResponse result = prepareQueryResult.gson.fromJson(in, typeToken);
        assertEquals(PreparedStatementResponseCode.GET_MORE_NULL_CURSOR, result.getResponseCode());
        assertNotNull(result.getResultList());
        assertEquals(0, result.getResultList().length);
        assertEquals(cursorId, result.getCursorId());
        assertFalse("This is a failure response, no more batches", result.hasMoreBatches());
        assertEquals("application/json; charset=UTF-8", getMoreConn.getContentType());
    }

    private String executeQuery(Gson gson, Query<TestClass> mockMongoQuery,
            Type typeToken, WebPreparedStatement<TestClass> stmt, boolean moreBatches)
            throws MalformedURLException, IOException, ProtocolException {
        URL url = new URL(getEndpoint() + "/query-execute");
        HttpURLConnection queryExecuteConn = (HttpURLConnection) url.openConnection();
        queryExecuteConn.setRequestMethod("POST");
        sendAuthentication(queryExecuteConn, "ignored1", "ignored2");
        queryExecuteConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        queryExecuteConn.setDoInput(true);
        queryExecuteConn.setDoOutput(true);
        
        OutputStreamWriter out = new OutputStreamWriter(queryExecuteConn.getOutputStream());
        String body = "prepared-stmt=" + gson.toJson(stmt, WebPreparedStatement.class);
        out.write(body + "\n");
        out.flush();

        String cookieValue = queryExecuteConn.getHeaderField("Set-Cookie");
        InputStreamReader in = new InputStreamReader(queryExecuteConn.getInputStream());
        WebQueryResponse<TestClass> result = gson.fromJson(in, typeToken);
        assertEquals("Expected more batches", moreBatches, result.hasMoreBatches());
        TestClass[] results = result.getResultList();
        assertEquals(2, results.length);
        assertEquals("fluff1", results[0].getKey1());
        assertEquals(42, results[0].getKey2());
        assertEquals("fluff2", results[1].getKey1());
        assertEquals(43, results[1].getKey2());

        assertEquals("application/json; charset=UTF-8", queryExecuteConn.getContentType());
        verify(mockMongoQuery).execute();
        verify(mockMongoQuery).getWhereExpression();
        verifyNoMoreInteractions(mockMongoQuery);
        return cookieValue;
    }
    
    private static class TrustedPreparedQueryTestResult {
        
        private final Gson gson;
        private final Query<TestClass> mockMongoQuery;
        private final BatchCursor<TestClass> cursor;
        
        private TrustedPreparedQueryTestResult(Gson gson, Query<TestClass> mockMongoQuery, BatchCursor<TestClass> cursor) {
            this.cursor = cursor;
            this.gson = gson;
            this.mockMongoQuery = mockMongoQuery;
        }
    }
    
    @SuppressWarnings("unchecked")
    private TrustedPreparedQueryTestResult prepareQuery(String strDescriptor, boolean moreBatches) throws Exception {
        TestClass expected1 = new TestClass();
        expected1.setKey1("fluff1");
        expected1.setKey2(42);
        TestClass expected2 = new TestClass();
        expected2.setKey1("fluff2");
        expected2.setKey2(43);
        // prepare-statement does this under the hood
        Query<TestClass> mockMongoQuery = mock(Query.class);
        when(mockStorage.createQuery(eq(category))).thenReturn(mockMongoQuery);

        BatchCursor<TestClass> cursor = mock(BatchCursor.class);
        WebStorageEndPoint.DEFAULT_QUERY_BATCH_SIZE = 2;
        // Assuming: moreBatches == true then we have
        // WebStorageEndpoint.getBatchFromCursor() method calls hasNext() twice,
        // CursorManager.put() calls it once and WebStorageEndpoint.queryExecute()
        // calls it once. Thus, 2 + 1 + 1 = 4 x true, then return false;
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(moreBatches).thenReturn(moreBatches).thenReturn(false);
        when(cursor.next()).thenReturn(expected1).thenReturn(expected2);
        
        PreparedStatement<TestClass> mockPreparedQuery = mock(PreparedStatement.class);
        when(mockStorage.prepareStatement(any(StatementDescriptor.class))).thenReturn(mockPreparedQuery);
        
        ParsedStatement<TestClass> mockParsedStatement = mock(ParsedStatement.class);
        when(mockParsedStatement.getNumParams()).thenReturn(1);
        when(mockParsedStatement.patchStatement(any(PreparedParameter[].class))).thenReturn(mockMongoQuery);
        when(mockPreparedQuery.getParsedStatement()).thenReturn(mockParsedStatement);
        
        // The web layer
        when(mockPreparedQuery.executeQuery()).thenReturn(cursor);
        // And the mongo layer
        when(mockMongoQuery.execute()).thenReturn(cursor);

        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/prepare-statement");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        sendAuthentication(conn, "ignored1", "ignored2");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        Gson gson = new GsonBuilder()
                            .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebQueryResponseTypeAdapterFactory())
                            .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                            .registerTypeAdapterFactory(new PreparedParametersTypeAdapterFactory())
                            .create();
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        String body = "query-descriptor=" + URLEncoder.encode(strDescriptor, "UTF-8") + "&category-id=" + categoryId;
        out.write(body + "\n");
        out.flush();

        Reader in = new InputStreamReader(conn.getInputStream());
        WebPreparedStatementResponse response = gson.fromJson(in, WebPreparedStatementResponse.class);
        assertEquals(1, response.getNumFreeVariables());
        assertEquals(0, response.getStatementId());
        assertEquals("application/json; charset=UTF-8", conn.getContentType());
        
        return new TrustedPreparedQueryTestResult(gson, mockMongoQuery, cursor);
    }

    private String setupPreparedQueryWithTrustedDescriptor() throws Exception {
        String strDescriptor = "QUERY " + category.getName() + " WHERE '" + key1.getName() + "' = ?s SORT '" + key1.getName() + "' DSC LIMIT 42";
        // metadata which basically does no filtering. There's another test which
        // asserts only allowed data (via ACL) gets returned.
        setupTrustedStatementRegistry(strDescriptor);
        
        Set<BasicRole> roles = new HashSet<>();
        roles.add(new RolePrincipal(Roles.REGISTER_CATEGORY));
        roles.add(new RolePrincipal(Roles.PREPARE_STATEMENT));
        roles.add(new RolePrincipal(Roles.READ));
        roles.add(new RolePrincipal(Roles.ACCESS_REALM));
        UserPrincipal testUser = new UserPrincipal("ignored1");
        testUser.setRoles(roles);
        
        final JAASLoginService loginService = getConfiguredLoginService(testUser, roles);
        
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        // This makes register category work for the "test" category.
        // Undone via @After
        setupTrustedCategory(categoryName);
        registerCategory("ignored1", "ignored2");
        return strDescriptor;
    }
    
    /*
     * 
     * This test simulates a case where the mongo query would return more than
     * a user can see. In this case only records matching agentIds which are
     * allowed via roles should get returned.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void authorizedFilteredQuery() throws Exception {
        Category oldCategory = category;
        String categoryName = "test-authorizedFilteredQuery";
        // redefine category to include the agentId key in the category.
        // undone via a the try-finally block.
        category = new Category(categoryName, TestClass.class, key1, key2, Key.AGENT_ID);
        try {
            String strDescriptor = "QUERY " + category.getName() + " WHERE '" +
                    key1.getName() + "' = ?s SORT '" + key1.getName() + "' DSC LIMIT 42";
            setupTrustedStatementRegistry(strDescriptor);
            
            Set<BasicRole> roles = new HashSet<>();
            roles.add(new RolePrincipal(Roles.REGISTER_CATEGORY));
            roles.add(new RolePrincipal(Roles.PREPARE_STATEMENT));
            roles.add(new RolePrincipal(Roles.READ));
            roles.add(new RolePrincipal(Roles.ACCESS_REALM));
            String fakeAgentId = "someAgentId";
            roles.add(new RolePrincipal("thermostat-agents-grant-read-agentId-" + fakeAgentId));
            UserPrincipal testUser = new UserPrincipal("ignored1");
            testUser.setRoles(roles);
            
            final JAASLoginService loginService = getConfiguredLoginService(testUser, roles);
            port = FreePortFinder.findFreePort(new TryPort() {
                
                @Override
                public void tryPort(int port) throws Exception {
                    startServer(port, loginService);
                }
            });
            // This makes register category work for the "test" category.
            // Undone via @After
            setupTrustedCategory(categoryName);
            registerCategory("ignored1", "ignored2");
            
            TestClass expected1 = new TestClass();
            expected1.setKey1("fluff1");
            expected1.setKey2(42);
            TestClass expected2 = new TestClass();
            expected2.setKey1("fluff2");
            expected2.setKey2(43);
            // prepare-statement does this under the hood
            Query<TestClass> mockMongoQuery = mock(Query.class);
            
            when(mockStorage.createQuery(eq(category))).thenReturn(mockMongoQuery);
    
            Cursor<TestClass> cursor = mock(Cursor.class);
            when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
            when(cursor.next()).thenReturn(expected1).thenReturn(expected2);
            
            PreparedStatement mockPreparedQuery = mock(PreparedStatement.class);
            when(mockStorage.prepareStatement(any(StatementDescriptor.class))).thenReturn(mockPreparedQuery);
            
            ParsedStatement mockParsedStatement = mock(ParsedStatement.class);
            when(mockParsedStatement.getNumParams()).thenReturn(1);
            when(mockParsedStatement.patchStatement(any(PreparedParameter[].class))).thenReturn(mockMongoQuery);
            when(mockPreparedQuery.getParsedStatement()).thenReturn(mockParsedStatement);
            
            // The web layer
            when(mockPreparedQuery.executeQuery()).thenReturn(cursor);
            // And the mongo layer
            when(mockMongoQuery.execute()).thenReturn(cursor);
    
            String endpoint = getEndpoint();
            URL url = new URL(endpoint + "/prepare-statement");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            sendAuthentication(conn, "ignored1", "ignored2");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            Gson gson = new GsonBuilder()
                                .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                                .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                                .registerTypeAdapterFactory(new WebQueryResponseTypeAdapterFactory())
                                .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                                .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                                .registerTypeAdapterFactory(new PreparedParametersTypeAdapterFactory())
                                .create();
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            String body = "query-descriptor=" + URLEncoder.encode(strDescriptor, "UTF-8") + "&category-id=" + categoryId;
            out.write(body + "\n");
            out.flush();
    
            Reader in = new InputStreamReader(conn.getInputStream());
            WebPreparedStatementResponse response = gson.fromJson(in, WebPreparedStatementResponse.class);
            assertEquals(1, response.getNumFreeVariables());
            assertEquals(0, response.getStatementId());
            assertEquals("application/json; charset=UTF-8", conn.getContentType());
            
            
            
            // now execute the query we've just prepared
            WebPreparedStatement<TestClass> stmt = new WebPreparedStatement<>(1, 0);
            stmt.setString(0, "fluff");
            
            url = new URL(endpoint + "/query-execute");
            HttpURLConnection conn2 = (HttpURLConnection) url.openConnection();
            conn2.setRequestMethod("POST");
            sendAuthentication(conn2, "ignored1", "ignored2");
            conn2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn2.setDoInput(true);
            conn2.setDoOutput(true);
            
            out = new OutputStreamWriter(conn2.getOutputStream());
            body = "prepared-stmt=" + gson.toJson(stmt, WebPreparedStatement.class);
            out.write(body + "\n");
            out.flush();
    
            in = new InputStreamReader(conn2.getInputStream());
            Type typeToken = new TypeToken<WebQueryResponse<TestClass>>(){}.getType();
            WebQueryResponse<TestClass> result = gson.fromJson(in, typeToken);
            TestClass[] results = result.getResultList();
            assertEquals(2, results.length);
            assertEquals("fluff1", results[0].getKey1());
            assertEquals(42, results[0].getKey2());
            assertEquals("fluff2", results[1].getKey1());
            assertEquals(43, results[1].getKey2());
    
            assertEquals("application/json; charset=UTF-8", conn2.getContentType());
            verify(mockMongoQuery).execute();
            verify(mockMongoQuery).getWhereExpression();
            ArgumentCaptor<Expression> expressionCaptor = ArgumentCaptor.forClass(Expression.class);
            verify(mockMongoQuery).where(expressionCaptor.capture());
            verifyNoMoreInteractions(mockMongoQuery);
            
            Expression capturedExpression = expressionCaptor.getValue();
            assertTrue(capturedExpression instanceof BinarySetMembershipExpression);
            Set<String> agentIds = new HashSet<>();
            agentIds.add(fakeAgentId);
            Expression expectedExpression = new ExpressionFactory().in(Key.AGENT_ID, agentIds, String.class);
            assertEquals(expectedExpression, capturedExpression);
        } finally {
            category = oldCategory; 
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void authorizedPreparedAggregateQuery() throws Exception {
        String strDescriptor = "QUERY-COUNT " + category.getName();
        setupTrustedStatementRegistry(strDescriptor);
        
        Set<BasicRole> roles = new HashSet<>();
        roles.add(new RolePrincipal(Roles.REGISTER_CATEGORY));
        roles.add(new RolePrincipal(Roles.PREPARE_STATEMENT));
        roles.add(new RolePrincipal(Roles.READ));
        roles.add(new RolePrincipal(Roles.ACCESS_REALM));
        UserPrincipal testUser = new UserPrincipal("ignored1");
        testUser.setRoles(roles);
        
        final JAASLoginService loginService = getConfiguredLoginService(testUser, roles);
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        
        AggregateCount count = new AggregateCount();
        count.setCount(500);
        // prepare-statement does this under the hood
        Query<AggregateCount> mockMongoQuery = mock(AggregateQuery.class);
        Category<AggregateCount> adapted = new CategoryAdapter(category).getAdapted(AggregateCount.class);
        registerCategory(adapted, "no-matter", "no-matter");
        when(mockStorage.createAggregateQuery(eq(AggregateFunction.COUNT), eq(adapted))).thenReturn(mockMongoQuery);

        Cursor<AggregateCount> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(count);
        
        PreparedStatement mockPreparedQuery = mock(PreparedStatement.class);
        when(mockStorage.prepareStatement(any(StatementDescriptor.class))).thenReturn(mockPreparedQuery);
        
        ParsedStatement mockParsedStatement = mock(ParsedStatement.class);
        when(mockParsedStatement.getNumParams()).thenReturn(0);
        when(mockParsedStatement.patchStatement(any(PreparedParameter[].class))).thenReturn(mockMongoQuery);
        when(mockPreparedQuery.getParsedStatement()).thenReturn(mockParsedStatement);
        
        // The web layer
        when(mockPreparedQuery.executeQuery()).thenReturn(cursor);
        // And the mongo layer
        when(mockMongoQuery.execute()).thenReturn(cursor);

        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/prepare-statement");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        sendAuthentication(conn, "no-matter", "no-matter");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        Gson gson = new GsonBuilder()
                            .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebQueryResponseTypeAdapterFactory())
                            .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                            .registerTypeAdapterFactory(new PreparedParametersTypeAdapterFactory())
                            .create();
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        String body = "query-descriptor=" + URLEncoder.encode(strDescriptor, "UTF-8") + "&category-id=" + categoryId;
        out.write(body + "\n");
        out.flush();

        Reader in = new InputStreamReader(conn.getInputStream());
        WebPreparedStatementResponse response = gson.fromJson(in, WebPreparedStatementResponse.class);
        assertEquals(0, response.getNumFreeVariables());
        assertEquals(0, response.getStatementId());
        assertEquals("application/json; charset=UTF-8", conn.getContentType());
        
        
        
        // now execute the query we've just prepared
        WebPreparedStatement<AggregateCount> stmt = new WebPreparedStatement<>(0, 0);
        
        url = new URL(endpoint + "/query-execute");
        HttpURLConnection conn2 = (HttpURLConnection) url.openConnection();
        conn2.setRequestMethod("POST");
        sendAuthentication(conn2, "no-matter", "no-matter");
        conn2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn2.setDoInput(true);
        conn2.setDoOutput(true);
        
        out = new OutputStreamWriter(conn2.getOutputStream());
        body = "prepared-stmt=" + gson.toJson(stmt, WebPreparedStatement.class);
        out.write(body + "\n");
        out.flush();

        in = new InputStreamReader(conn2.getInputStream());
        Type typeToken = new TypeToken<WebQueryResponse<AggregateCount>>(){}.getType();
        WebQueryResponse<AggregateCount> result = gson.fromJson(in, typeToken);
        AggregateCount[] results = result.getResultList();
        assertEquals(1, results.length);
        assertEquals(500, results[0].getCount());

        assertEquals("application/json; charset=UTF-8", conn2.getContentType());
        verify(mockMongoQuery).execute();
        verify(mockMongoQuery).getWhereExpression();
        verifyNoMoreInteractions(mockMongoQuery);
    }
    
    private void setupTrustedCategory(String categoryName) {
        Set<String> descs = new HashSet<>();
        descs.add(categoryName);
        CategoryRegistration reg = mock(CategoryRegistration.class);
        when(reg.getCategoryNames()).thenReturn(descs);
        List<CategoryRegistration> regs = new ArrayList<>(1);
        regs.add(reg);
        KnownCategoryRegistry registry = new KnownCategoryRegistry(regs);
        KnownCategoryRegistryFactory.setInstance(registry);
    }
    
    private void setupTrustedStatementRegistry(String strDescriptor) {
        Set<String> descs = new HashSet<>();
        descs.add(strDescriptor);
        StatementDescriptorRegistration reg = new TestStatementDescriptorRegistration(descs);
        List<StatementDescriptorRegistration> regs = new ArrayList<>(1);
        regs.add(reg);
        KnownDescriptorRegistry registry = new KnownDescriptorRegistry(regs);
        KnownDescriptorRegistryFactory.setKnownDescriptorRegistry(registry);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void authorizedPreparedWrite() throws Exception {
        Category<TestClass> oldCategory = category;
        String categoryName = "test-authorizedPreparedWrite";
        // redefine category to include the agentId key in the category.
        // undone via a the try-finally block.
        category = new Category<>(categoryName, TestClass.class, key1, key2, Key.AGENT_ID);
        try {
            String strDescriptor = "ADD " + category.getName() + " SET '" +
                    key1.getName() + "' = ?s , '" + key2.getName() + "' = ?s";
            setupTrustedStatementRegistry(strDescriptor);
            
            Set<BasicRole> roles = new HashSet<>();
            roles.add(new RolePrincipal(Roles.REGISTER_CATEGORY));
            roles.add(new RolePrincipal(Roles.PREPARE_STATEMENT));
            roles.add(new RolePrincipal(Roles.WRITE));
            roles.add(new RolePrincipal(Roles.ACCESS_REALM));
            UserPrincipal testUser = new UserPrincipal("ignored1");
            testUser.setRoles(roles);
            
            final JAASLoginService loginService = getConfiguredLoginService(testUser, roles);
            port = FreePortFinder.findFreePort(new TryPort() {
                
                @Override
                public void tryPort(int port) throws Exception {
                    startServer(port, loginService);
                }
            });
            // This makes register category work for the "test" category.
            // Undone via @After
            setupTrustedCategory(categoryName);
            registerCategory("ignored1", "ignored2");
            
            // prepare-statement does this under the hood
            Add<TestClass> mockMongoAdd = mock(Add.class);
            
            when(mockStorage.createAdd(eq(category))).thenReturn(mockMongoAdd);
    
            PreparedStatement<TestClass> mockPreparedQuery = mock(PreparedStatement.class);
            when(mockStorage.prepareStatement(any(StatementDescriptor.class))).thenReturn(mockPreparedQuery);
            
            ParsedStatement<TestClass> mockParsedStatement = mock(ParsedStatement.class);
            when(mockParsedStatement.getNumParams()).thenReturn(2);
            when(mockParsedStatement.patchStatement(any(PreparedParameter[].class))).thenReturn(mockMongoAdd);
            when(mockPreparedQuery.getParsedStatement()).thenReturn(mockParsedStatement);
            
            // The web layer
            when(mockPreparedQuery.execute()).thenReturn(PreparedStatementResponseCode.WRITE_GENERIC_FAILURE);
            // And the mongo layer
            when(mockMongoAdd.apply()).thenReturn(PreparedStatementResponseCode.WRITE_GENERIC_FAILURE);
    
            String endpoint = getEndpoint();
            URL url = new URL(endpoint + "/prepare-statement");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            sendAuthentication(conn, "ignored1", "ignored2");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                .registerTypeAdapterFactory(new PreparedParametersTypeAdapterFactory())
                .create();
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            String body = "query-descriptor=" + URLEncoder.encode(strDescriptor, "UTF-8") + "&category-id=" + categoryId;
            out.write(body + "\n");
            out.flush();
    
            Reader in = new InputStreamReader(conn.getInputStream());
            WebPreparedStatementResponse response = gson.fromJson(in, WebPreparedStatementResponse.class);
            assertEquals(2, response.getNumFreeVariables());
            assertEquals(0, response.getStatementId());
            assertEquals("application/json; charset=UTF-8", conn.getContentType());
            
            
            
            // now execute the ADD we've just prepared
            WebPreparedStatement<TestClass> stmt = new WebPreparedStatement<>(2, 0);
            stmt.setString(0, "fluff");
            stmt.setString(1, "test2");
            
            url = new URL(endpoint + "/write-execute");
            HttpURLConnection conn2 = (HttpURLConnection) url.openConnection();
            conn2.setRequestMethod("POST");
            sendAuthentication(conn2, "ignored1", "ignored2");
            conn2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn2.setDoInput(true);
            conn2.setDoOutput(true);
            
            out = new OutputStreamWriter(conn2.getOutputStream());
            body = "prepared-stmt=" + gson.toJson(stmt, WebPreparedStatement.class);
            out.write(body + "\n");
            out.flush();
    
            in = new InputStreamReader(conn2.getInputStream());
            int result = gson.fromJson(in, int.class);
            assertEquals(PreparedStatementResponseCode.WRITE_GENERIC_FAILURE, result);
        } finally {
            category = oldCategory; 
        }
    }
    
    @Test
    public void cannotRegisterCategoryWithoutRegistrationOnInit() throws Exception {
        // need this in order to pass basic permissions.
        String[] roleNames = new String[] {
                Roles.REGISTER_CATEGORY
        };
        String username = "testuser";
        String password = "testpassword";
        final LoginService loginService = new TestLoginService(username, password, roleNames); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        try {
            String endpoint = getEndpoint();
            URL url = new URL(endpoint + "/register-category");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String enc = "UTF-8";
            conn.setRequestProperty("Content-Encoding", enc);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            sendAuthentication(conn, username, password);
            OutputStream out = conn.getOutputStream();
            Gson gson = new Gson();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.write("name=");
            writer.write(URLEncoder.encode(category.getName(), enc));
            writer.write("&category=");
            writer.write(URLEncoder.encode(gson.toJson(category), enc));
            writer.flush();

            // "test" category name not registered, expecting forbidden.
            assertEquals(HttpServletResponse.SC_FORBIDDEN, conn.getResponseCode());
        } catch (IOException e) {
            fail("Should not throw exception! " + e.getMessage());
        }
    }

    @Test
    public void unauthorizedPrepareStmt() throws Exception {
        String failMsg = "thermostat-prepare-statement role missing, expected Forbidden!";
        doUnauthorizedTest("prepare-statement", failMsg);
    }
    
    @Test
    public void unauthorizedExecutePreparedQuery() throws Exception {
        String failMsg = "thermostat-read role missing, expected Forbidden!";
        doUnauthorizedTest("query-execute", failMsg);
    }
    
    private void doUnauthorizedTest(String pathForEndPoint, String failMessage) throws Exception {
        String[] insufficientRoleNames = new String[] {
                Roles.REGISTER_CATEGORY,
                Roles.ACCESS_REALM
        };
        doUnauthorizedTest(pathForEndPoint, failMessage, insufficientRoleNames, true);
    }

    private void doUnauthorizedTest(String pathForEndPoint, String failMessage,
            String[] insufficientRoles, boolean doRegisterCategory) throws Exception,
            MalformedURLException, IOException, ProtocolException {
        String testuser = "testuser";
        String password = "testpassword";
        final LoginService loginService = new TestLoginService(testuser, password, insufficientRoles); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        if (doRegisterCategory) {
            // This makes register category work for the "test" category.
            // Undone via @After
            setupTrustedCategory(categoryName);
            registerCategory(testuser, password);
        }
        
        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/" + pathForEndPoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        sendAuthentication(conn, testuser, password);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        
        assertEquals(failMessage, HttpServletResponse.SC_FORBIDDEN, conn.getResponseCode());
    }
    
    @Test
    public void authorizedRegisterCategoryTest() throws Exception {
        Set<BasicRole> roles = new HashSet<>();
        roles.add(new RolePrincipal(Roles.REGISTER_CATEGORY));
        roles.add(new RolePrincipal(Roles.ACCESS_REALM));
        UserPrincipal testUser = new UserPrincipal("ignored1");
        testUser.setRoles(roles);
        
        final JAASLoginService loginService = getConfiguredLoginService(testUser, roles);
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        Category<HostInfo> wantedCategory = HostInfoDAO.hostInfoCategory;
        Category<AggregateCount> aggregate = new CategoryAdapter<HostInfo, AggregateCount>(wantedCategory).getAdapted(AggregateCount.class);
        
        // First the originating category has to be registered, then the adapted
        // one.
        Integer realId = registerCategoryAndGetId(wantedCategory, "no-matter", "no-matter");
        Integer aggregateId = registerCategoryAndGetId(aggregate, "no-matter", "no-matter");
        
        assertTrue("Aggregate categories need their own ID", aggregateId != realId);
        
        verify(mockStorage).registerCategory(eq(wantedCategory));
        verifyNoMoreInteractions(mockStorage);
    }
    
    private Integer registerCategoryAndGetId(Category<?> cat, String username, String password) throws Exception {
        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/register-category");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        sendAuthentication(conn, username, password);

        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        Gson gson = new Gson();
        out.write("name=" + cat.getName() + "&data-class=" + cat.getDataClass().getName() + "&category=" + gson.toJson(cat));
        out.flush();
        assertEquals(200, conn.getResponseCode());
        Reader reader = new InputStreamReader(conn.getInputStream());
        Integer id = gson.fromJson(reader, Integer.class);
        return id;
    }
    
    private void setCookie(HttpURLConnection conn, String cookieVal) {
        conn.setRequestProperty("Cookie", cookieVal);
    }

    private void sendAuthentication(HttpURLConnection conn, String username, String passwd) {
        String userpassword = username + ":" + passwd;
        String encodedAuthorization = Base64.encodeBase64String(userpassword.getBytes());
        conn.setRequestProperty("Authorization", "Basic "+ encodedAuthorization);
    }

    @Test
    public void authorizedSaveFile() throws Exception {
        String filename = "fluff";
        String[] roleNames = new String[] {
                Roles.SAVE_FILE,
                Roles.ACCESS_REALM,
                // User also needs permission for specific file to be saved.
                WebStorageEndPoint.FILES_WRITE_GRANT_ROLE_PREFIX + filename
        };
        String testuser = "testuser";
        String password = "testpassword";
        final LoginService loginService = new TestLoginService(testuser, password, roleNames); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        String endpoint = getEndpoint();

        URL url = new URL(endpoint + "/save-file");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        sendAuthentication(conn, testuser, password);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=fluff");
        conn.setRequestProperty("Transfer-Encoding", "chunked");
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("--fluff\r\n");
        out.write("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n");
        out.write("Content-Type: application/octet-stream\r\n");
        out.write("Content-Transfer-Encoding: binary\r\n");
        out.write("\r\n");
        out.write("Hello World\r\n");
        out.write("--fluff--\r\n");
        out.flush();
        // needed in order to trigger inCaptor interaction with mock
        int respCode = conn.getResponseCode();
        assertEquals(HttpServletResponse.SC_OK, respCode);
        ArgumentCaptor<InputStream> inCaptor = ArgumentCaptor.forClass(InputStream.class);
        verify(mockStorage).saveFile(eq(filename), inCaptor.capture());
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
    public void unauthorizedSaveFile() throws Exception {
        String failMsg = "thermostat-save-file role missing, expected Forbidden!";
        String[] insufficientRoles = new String[] {
                Roles.ACCESS_REALM
        };
        doUnauthorizedTest("save-file", failMsg, insufficientRoles, false);
    }
    
    @Test
    public void unauthorizedSaveFileMissingSpecificRole() throws Exception {
        String filename = "foo.txt";
        String[] insufficientRoles = new String[] {
                Roles.SAVE_FILE,
                Roles.ACCESS_REALM
        };
        String testuser = "testuser";
        String password = "testpassword";
        final LoginService loginService = new TestLoginService(testuser, password, insufficientRoles); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        String endpoint = getEndpoint();

        URL url = new URL(endpoint + "/save-file");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        sendAuthentication(conn, testuser, password);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=fluff");
        conn.setRequestProperty("Transfer-Encoding", "chunked");
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("--fluff\r\n");
        out.write("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n");
        out.write("Content-Type: application/octet-stream\r\n");
        out.write("Content-Transfer-Encoding: binary\r\n");
        out.write("\r\n");
        out.write("Hello World\r\n");
        out.write("--fluff--\r\n");
        out.flush();
        int respCode = conn.getResponseCode();
        assertEquals(HttpServletResponse.SC_FORBIDDEN, respCode);
        verifyNoMoreInteractions(mockStorage);
    }

    @Test
    public void authorizedLoadFile() throws Exception {
        String filename = "fluff";
        String[] roleNames = new String[] {
                Roles.LOAD_FILE,
                Roles.ACCESS_REALM,
                // Grant the specific read file permission
                WebStorageEndPoint.FILES_READ_GRANT_ROLE_PREFIX + filename
        };
        String testuser = "testuser";
        String password = "testpassword";
        final LoginService loginService = new TestLoginService(testuser, password, roleNames); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        
        byte[] data = "Hello World".getBytes();
        InputStream in = new ByteArrayInputStream(data);
        when(mockStorage.loadFile(filename)).thenReturn(in);

        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/load-file");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        sendAuthentication(conn, testuser, password);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("file=" + filename);
        out.flush();
        int respCode = conn.getResponseCode();
        assertEquals(HttpServletResponse.SC_OK, respCode);
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
        verify(mockStorage).loadFile(filename);
    }
    
    @Test
    public void unauthorizedLoadFile() throws Exception {
        String failMsg = "thermostat-load-file role missing, expected Forbidden!";
        String[] insufficientRoles = new String[] {
                Roles.ACCESS_REALM
        };
        doUnauthorizedTest("load-file", failMsg, insufficientRoles, false);
    }
    
    @Test
    public void unauthorizedLoadFileMissingSpecificRole() throws Exception {
        String filename = "foo.txt";
        String[] insufficientRoles = new String[] {
                Roles.LOAD_FILE,
                Roles.ACCESS_REALM
        };
        String testuser = "testuser";
        String password = "testpassword";
        final LoginService loginService = new TestLoginService(testuser, password, insufficientRoles); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        
        byte[] data = "Hello World".getBytes();
        InputStream in = new ByteArrayInputStream(data);
        when(mockStorage.loadFile(filename)).thenReturn(in);

        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/load-file");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        sendAuthentication(conn, testuser, password);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("file=" + filename);
        out.flush();
        int respCode = conn.getResponseCode();
        assertEquals(HttpServletResponse.SC_FORBIDDEN, respCode);
        verifyNoMoreInteractions(mockStorage);
    }

    @Test
    public void authorizedPurge() throws Exception {
        String[] roleNames = new String[] {
                Roles.PURGE,
                Roles.ACCESS_REALM
        };
        String testuser = "testuser";
        String password = "testpassword";
        final LoginService loginService = new TestLoginService(testuser, password, roleNames); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/purge");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        sendAuthentication(conn, testuser, password);
        conn.getOutputStream().write("agentId=fluff".getBytes());
        int status = conn.getResponseCode();
        assertEquals(HttpServletResponse.SC_OK, status);
        verify(mockStorage).purge("fluff");
    }
    
    @Test
    public void unauthorizedAccessRealm() throws Exception {
        String failMsg = Roles.ACCESS_REALM + " role missing, expected Forbidden!";
        String[] insufficientRoles = new String[0];
        // entry point for this test doesn't matter. Use '/'. 
        doUnauthorizedTest("", failMsg, insufficientRoles, false);
    }
    
    @Test
    public void authorizedAccessRealm() throws Exception {
        String[] roles = new String[] {
                Roles.ACCESS_REALM
        };
        String testuser = "testuser";
        String password = "testpassword";
        final LoginService loginService = new TestLoginService(testuser, password, roles); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        
        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/"); // Testing the realm, nothing else.
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        sendAuthentication(conn, testuser, password);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        
        assertEquals(HttpServletResponse.SC_OK, conn.getResponseCode());
    }
    
    @Test
    public void unauthorizedPurge() throws Exception {
        String failMsg = "thermostat-purge role missing, expected Forbidden!";
        String[] insufficientRoles = new String[] {
                Roles.ACCESS_REALM
        };
        doUnauthorizedTest("purge", failMsg, insufficientRoles, false);
    }

    private void registerCategory(String username, String password) {
        registerCategory(category, username, password);
    }
    
    private void registerCategory(Category<?> category, String username, String password) {
        try {
            String endpoint = getEndpoint();
            URL url = new URL(endpoint + "/register-category");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String enc = "UTF-8";
            conn.setRequestProperty("Content-Encoding", enc);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            sendAuthentication(conn, username, password);
            OutputStream out = conn.getOutputStream();
            Gson gson = new Gson();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.write("name=");
            writer.write(URLEncoder.encode(category.getName(), enc));
            writer.write("&category=");
            writer.write(URLEncoder.encode(gson.toJson(category), enc));
            writer.write("&data-class=");
            writer.write(URLEncoder.encode(category.getDataClass().getName(), enc));
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
    public void authorizedGenerateToken() throws Exception {
        String actionName = "testing";
        String[] roleNames = new String[] {
                Roles.CMD_CHANNEL_GENERATE,
                Roles.ACCESS_REALM,
                // grant the "testing" action
                WebStorageEndPoint.CMDC_AUTHORIZATION_GRANT_ROLE_PREFIX + actionName
        };
        String testuser = "testuser";
        String password = "testpassword";
        final LoginService loginService = new TestLoginService(testuser, password, roleNames); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        verifyAuthorizedGenerateToken(testuser, password, actionName);
    }

    @Test
    public void unauthorizedGenerateToken() throws Exception {
        String failMsg = "thermostat-cmdc-generate role missing, expected Forbidden!";
        String[] insufficientRoles = new String[] {
                Roles.ACCESS_REALM
        };
        doUnauthorizedTest("generate-token", failMsg, insufficientRoles, false);
    }

    @Test
    public void authorizedGenerateVerifyToken() throws Exception {
        String actionName = "someAction";
        String[] roleNames = new String[] {
                Roles.CMD_CHANNEL_GENERATE,
                Roles.CMD_CHANNEL_VERIFY,
                Roles.ACCESS_REALM,
                // grant "someAction" to be performed
                WebStorageEndPoint.CMDC_AUTHORIZATION_GRANT_ROLE_PREFIX + actionName,
        };
        String testuser = "testuser";
        String password = "testpassword";
        final LoginService loginService = new TestLoginService(testuser, password, roleNames); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        byte[] token = verifyAuthorizedGenerateToken(testuser, password, actionName);
        
        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/verify-token");
        
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        sendAuthentication(conn, testuser, password);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("client-token=fluff&action-name=" + actionName + "&token=" +
                URLEncoder.encode(Base64.encodeBase64String(token), "UTF-8"));
        out.flush();
        assertEquals(200, conn.getResponseCode());
    }
    
    @Test
    public void unAuthorizedGenerateVerifyToken() throws Exception {
        String testuser = "testuser";
        String password = "testpassword";
        String actionName = "someAction";
        String[] roleNames = new String[] {
                Roles.CMD_CHANNEL_GENERATE,
                Roles.CMD_CHANNEL_VERIFY,
                Roles.ACCESS_REALM,
                // missing the thermostat-cmdc-grant-someAction role
        };
        final LoginService loginService = new TestLoginService(testuser, password, roleNames); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        
        byte[] result = verifyAuthorizedGenerateToken(testuser, password, actionName, 403);
        assertNull(result);
    }
    
    @Test
    public void authenticatedGenerateVerifyTokenWithActionNameMismatch() throws Exception {
        String actionName = "someAction";
        String[] roleNames = new String[] {
                Roles.CMD_CHANNEL_GENERATE,
                WebStorageEndPoint.CMDC_AUTHORIZATION_GRANT_ROLE_PREFIX + actionName,
                Roles.CMD_CHANNEL_VERIFY,
                Roles.ACCESS_REALM
        };
        String testuser = "testuser";
        String password = "testpassword";
        final LoginService loginService = new TestLoginService(testuser, password, roleNames); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        byte[] token = verifyAuthorizedGenerateToken(testuser, password, actionName);

        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/verify-token");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        sendAuthentication(conn, testuser, password);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        // expected action-name parameter is "someAction". This should not
        // verify => 403.
        out.write("client-token=fluff&action-name=wrongAction&token=" + URLEncoder.encode(Base64.encodeBase64String(token), "UTF-8"));
        out.flush();
        assertEquals(403, conn.getResponseCode());
    }
    

    @Test
    public void authenticatedTokenTimeout() throws Exception {
        String actionName = "someAction";
        String[] roleNames = new String[] {
                Roles.CMD_CHANNEL_GENERATE,
                Roles.CMD_CHANNEL_VERIFY,
                Roles.ACCESS_REALM,
                // Grant "someAction", this test tests the time-out
                WebStorageEndPoint.CMDC_AUTHORIZATION_GRANT_ROLE_PREFIX + actionName
        };
        String testuser = "testuser";
        String password = "testpassword";
        final LoginService loginService = new TestLoginService(testuser, password, roleNames); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        byte[] token = verifyAuthorizedGenerateToken(testuser, password, actionName);

        Thread.sleep(700); // Timeout is set to 500ms for tests, 700ms should be enough for everybody. ;-)

        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/verify-token");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        sendAuthentication(conn, testuser, password);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("client-token=fluff&action-name=" + actionName + "&token=" +
                  URLEncoder.encode(Base64.encodeBase64String(token), "UTF-8"));
        out.flush();
        assertEquals(403, conn.getResponseCode());
    }

    @Test
    public void authenticatedVerifyNonExistentToken() throws Exception {
        String[] roleNames = new String[] {
                Roles.CMD_CHANNEL_VERIFY,
                Roles.ACCESS_REALM
        };
        String testuser = "testuser";
        String password = "testpassword";
        final LoginService loginService = new TestLoginService(testuser, password, roleNames); 
        port = FreePortFinder.findFreePort(new TryPort() {
            
            @Override
            public void tryPort(int port) throws Exception {
                startServer(port, loginService);
            }
        });
        
        byte[] token = "fluff".getBytes();

        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/verify-token");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        sendAuthentication(conn, testuser, password);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("client-token=fluff&action-name=someAction&token=" + URLEncoder.encode(Base64.encodeBase64String(token), "UTF-8"));
        out.flush();
        assertEquals(403, conn.getResponseCode());
    }
    
    @Test
    public void unauthorizedVerifyToken() throws Exception {
        String failMsg = "thermostat-cmdc-verify role missing, expected Forbidden!";
        String[] insufficientRoles = new String[] {
                Roles.ACCESS_REALM
        };
        doUnauthorizedTest("verify-token", failMsg, insufficientRoles, false);
    }
    
    private byte[] verifyAuthorizedGenerateToken(String username, String password, String actionName) throws IOException {
        return verifyAuthorizedGenerateToken(username, password, actionName, 200);
    }
    
    private byte[] verifyAuthorizedGenerateToken(String username, String password, String actionName, int expectedResponseCode) throws IOException {
        return verifyAuthorizedGenerateToken(username, password, expectedResponseCode, actionName);
    }
    
    private JAASLoginService getConfiguredLoginService(UserPrincipal user, Set<BasicRole> roles) {
        Configuration config = new TestConfiguration(user, roles);
        Configuration.setConfiguration(config);
        JAASLoginService loginService = new JAASLoginService("foo");
        loginService.setRoleClassNames(new String[] { RolePrincipal.class.getName() });
        return loginService;
    }
    
    private byte[] verifyAuthorizedGenerateToken(String username,
            String password, int expectedResponseCode, String actionName)
            throws IOException {
        String endpoint = getEndpoint();
        URL url = new URL(endpoint + "/generate-token");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        sendAuthentication(conn, username, password);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("client-token=fluff&action-name=" + actionName);
        out.flush();
        int actualResponseCode = conn.getResponseCode();
        assertEquals(expectedResponseCode, actualResponseCode);
        if (actualResponseCode == 200) {
            InputStream in = conn.getInputStream();
            int length = conn.getContentLength();
            byte[] token = new byte[length];
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
        } else {
            return null;
        }
    }
    
    private static class TestLoginService extends MappedLoginService {

        private final String[] roleNames;
        private final String username;
        private final String password;

        private TestLoginService(String username, String password,
                String[] roleNames) {
            this.username = username;
            this.password = password;
            this.roleNames = roleNames;
        }

        @Override
        protected void loadUsers() throws IOException {
            putUser(username, new Password(password),
                    roleNames);
        }

        @Override
        protected UserIdentity loadUser(String username) {
            return new DefaultUserIdentity(null, null,
                    roleNames);
        }
    }
    
    private static class TestStatementDescriptorRegistration implements StatementDescriptorRegistration {

        private final Set<String> descriptorSet;
        private TestStatementDescriptorRegistration(Set<String> descriptorSet) {
            assertEquals(1, descriptorSet.size());
            this.descriptorSet = descriptorSet;
        }
        
        @Override
        public DescriptorMetadata getDescriptorMetadata(String descriptor,
                PreparedParameter[] params) {
            throw new AssertionError("should not be used");
        }

        @Override
        public Set<String> getStatementDescriptors() {
            return descriptorSet;
        }
        
    }
    
    private class TestConfiguration extends Configuration {
        
        private final UserPrincipal uPrincipal;
        private final Set<BasicRole> roles;
        
        TestConfiguration(UserPrincipal uPrincipal, Set<BasicRole> roles) {
            this.uPrincipal = uPrincipal;
            this.roles = roles;
        }

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            Map<String, Object> state = new HashMap<>();
            // TestLoginModule uses those options
            state.put("user", uPrincipal);
            state.put("roles", roles);
            AppConfigurationEntry entry = new AppConfigurationEntry(TestLoginModule.class.getName(), LoginModuleControlFlag.REQUIRED, state);
            return new AppConfigurationEntry[] { entry };
        }
        
    }
    
    public static class TestLoginModule implements LoginModule {
        
        private Subject subject;
        private UserPrincipal user;
        private Set<BasicRole> roles;

        @SuppressWarnings("unchecked")
        @Override
        public void initialize(Subject subject,
                CallbackHandler callbackHandler, Map<String, ?> sharedState,
                Map<String, ?> options) {
            this.subject = subject;
            this.user = (UserPrincipal)options.get("user");
            this.roles = (Set<BasicRole>)options.get("roles");
        }

        @Override
        public boolean login() throws LoginException {
            Set<Principal> principals = subject.getPrincipals();
            principals.add(user);
            principals.addAll(roles);
            return true;
        }

        @Override
        public boolean commit() throws LoginException {
            return true;
        }

        @Override
        public boolean abort() throws LoginException {
            return true;
        }

        @Override
        public boolean logout() throws LoginException {
            return true;
        }
        
    }
    
}

