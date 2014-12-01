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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
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
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.storage.core.BackingStorage;
import com.redhat.thermostat.storage.core.Categories;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.IllegalDescriptorException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedParameters;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.core.experimental.BatchCursor;
import com.redhat.thermostat.storage.model.AggregateResult;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.test.FreePortFinder;
import com.redhat.thermostat.test.FreePortFinder.TryPort;
import com.redhat.thermostat.web.common.PreparedStatementResponseCode;
import com.redhat.thermostat.web.common.SharedStateId;
import com.redhat.thermostat.web.common.WebPreparedStatement;
import com.redhat.thermostat.web.common.WebPreparedStatementResponse;
import com.redhat.thermostat.web.common.WebQueryResponse;
import com.redhat.thermostat.web.common.typeadapters.PojoTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.PreparedParameterTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.SharedStateIdTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.WebPreparedStatementResponseTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.WebPreparedStatementTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.WebQueryResponseTypeAdapterFactory;

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
    private UUID serverNonce;

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
        serverNonce = UUID.randomUUID();
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
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new SharedStateIdTypeAdapterFactory()).create();
        responseBody = gson.toJson(new SharedStateId(42, serverNonce));

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
                .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                .registerTypeAdapterFactory(new SharedStateIdTypeAdapterFactory())
                .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                .create();

        // missing quotes for LHS key
        String strDesc = "QUERY test WHERE a = ?s";
        StatementDescriptor<TestObj> desc = new StatementDescriptor<>(category, strDesc);
        
        WebPreparedStatementResponse fakeResponse = new WebPreparedStatementResponse();
        SharedStateId id = new SharedStateId(WebPreparedStatementResponse.DESCRIPTOR_PARSE_FAILED, UUID.randomUUID());
        fakeResponse.setStatementId(id);
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
                .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                .registerTypeAdapterFactory(new SharedStateIdTypeAdapterFactory())
                .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                .create();

        String strDesc = "QUERY test WHERE 'property1' = ?s";
        StatementDescriptor<TestObj> desc = new StatementDescriptor<>(category, strDesc);
        
        WebPreparedStatementResponse fakeResponse = new WebPreparedStatementResponse();
        SharedStateId id = new SharedStateId(WebPreparedStatementResponse.ILLEGAL_STATEMENT, UUID.randomUUID());
        fakeResponse.setStatementId(id);
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
    public void forbiddenExecuteQueryThrowsConsumingExcptn() throws UnsupportedEncodingException, IOException {
        Gson gson = new GsonBuilder()
                            .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                            .registerTypeAdapterFactory(new SharedStateIdTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebQueryResponseTypeAdapterFactory())
                            .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                            .create();

        String strDesc = "QUERY test WHERE 'property1' = ?s";
        StatementDescriptor<TestObj> desc = new StatementDescriptor<>(category, strDesc);
        PreparedStatement<TestObj> stmt = null;
        
        int fakePrepStmtId = 5;
        SharedStateId id = new SharedStateId(fakePrepStmtId, UUID.randomUUID());
        WebPreparedStatementResponse fakeResponse = new WebPreparedStatementResponse();
        fakeResponse.setNumFreeVariables(1);
        fakeResponse.setStatementId(id);
        prepareServer(gson.toJson(fakeResponse));
        try {
            stmt = storage.prepareStatement(desc);
        } catch (DescriptorParsingException e) {
            // descriptor should parse fine and is trusted
            fail(e.getMessage());
        }
        assertTrue(stmt instanceof WebPreparedStatement);
        WebPreparedStatement<TestObj> webStmt = (WebPreparedStatement<TestObj>)stmt;
        assertEquals(id, webStmt.getStatementId());
        PreparedParameters params = webStmt.getParams();
        assertEquals(1, params.getParams().length);
        assertNull(params.getParams()[0]);
        
        // now set a parameter
        stmt.setString(0, "fluff");
        assertEquals("fluff", params.getParams()[0].getValue());
        assertEquals(String.class, params.getParams()[0].getType());
        
        prepareServer(HttpServletResponse.SC_FORBIDDEN);
        try {
            stmt.executeQuery();
            fail("Forbidden should have thrown an exception!");
        } catch (StatementExecutionException e) {
            Throwable t = e.getCause();
            assertTrue(t instanceof StorageException);
            t = t.getCause();
            assertTrue("Wanted EntityConsumingIOException as root cause", t instanceof EntityConsumingIOException);
        }
    }
    
    @Test
    public void prepareStatementCachesStatements() throws DescriptorParsingException {
        String strDesc = "QUERY test WHERE 'property1' = ?s";
        StatementDescriptor<TestObj> desc = new StatementDescriptor<>(category, strDesc);
        PrepareStatementWebStorage testStorage = new PrepareStatementWebStorage("foo", mock(StorageCredentials.class), mock(SSLConfiguration.class));
        // should fill the cache
        WebPreparedStatement<TestObj> stmt = (WebPreparedStatement<TestObj>)testStorage.prepareStatement(desc);
        int numParams = stmt.getParams().getParams().length;
        SharedStateId stmtId = stmt.getStatementId();
        assertEquals(0, numParams);
        assertEquals(1, stmtId.getId());
        // this one should be cached, same stmtId and numParams as previous
        // one.
        stmt = (WebPreparedStatement<TestObj>)testStorage.prepareStatement(desc);
        numParams = stmt.getParams().getParams().length;
        stmtId = stmt.getStatementId();
        assertEquals("PreparedStatementWebStorge increments a counter" +
                     " if it wasn't cached. Was it 2? Bad!",
                     0, numParams);
        assertEquals("PreparedStatementWebStorge increments a counter" +
                     " if it wasn't cached. Was it 3? Bad!", 1, stmtId.getId());
        // preparing a different descriptor should not be cached.
        strDesc = "QUERY test WHERE 'foo' = ?l";
        desc = new StatementDescriptor<>(category, strDesc);
        stmt = (WebPreparedStatement<TestObj>)testStorage.prepareStatement(desc);
        numParams = stmt.getParams().getParams().length;
        stmtId = stmt.getStatementId();
        assertEquals("PreparedStatementWebStorge increments a counter" +
                     " if it wasn't cached. Triggers e.g. if it was erronously cached!",
                     2, numParams);
        assertEquals("PreparedStatementWebStorge increments a counter" +
                     " if it wasn't cached. Triggers e.g. if it was erronously cached!",
                     3, stmtId.getId());
    }
    
    @Test
    public void prepareStatementCachesStatements2() throws DescriptorParsingException {
        WebPreparedStatementCache cache = mock(WebPreparedStatementCache.class);
        final WebPreparedStatementHolder holder = mock(WebPreparedStatementHolder.class);
        WebStorage testStorage = new WebStorage(cache, null) {
            <T extends Pojo> WebPreparedStatementHolder sendPrepareStmtRequest(StatementDescriptor<T> desc, final int invokationCount)
                    throws DescriptorParsingException {
                if (invokationCount != 0) {
                    throw new AssertionError("expected invokation count 0 but was: " + invokationCount);
                }
                return holder;
            }
        };
        String strDesc = "QUERY test WHERE 'property1' = ?s";
        StatementDescriptor<TestObj> desc = new StatementDescriptor<>(category, strDesc);
        testStorage.prepareStatement(desc);
        verify(cache).get(desc);
        verify(cache).put(desc, holder);
    }
    
    @Test
    public void verifySendCategoryReRegistrationRequest() {
        @SuppressWarnings("unchecked")
        Map<Category<?>, SharedStateId> categoryMap = mock(Map.class);
        final List<Category<?>> interceptedCategories = new ArrayList<>();
        WebStorage testStorage = new WebStorage(categoryMap) {
            @Override
            public void registerCategory(Category<?> category) throws StorageException {
                interceptedCategories.add(category);
            }
        };
        // verify aggregate categories
        Category<TestAggregate> aggregateCategory = new CategoryAdapter<TestObj, TestAggregate>(category).getAdapted(TestAggregate.class);
        testStorage.sendCategoryReRegistrationRequest(aggregateCategory);
        verify(categoryMap).remove(aggregateCategory);
        verify(categoryMap).remove(category);
        assertEquals(2, interceptedCategories.size());
        assertEquals("Expected actual category to be re-registered first", category, interceptedCategories.get(0));
        assertEquals("Expected aggregate category to be re-registered second", aggregateCategory, interceptedCategories.get(1));
        
        interceptedCategories.clear();
        
        // verify regular categories
        testStorage.sendCategoryReRegistrationRequest(category);
        // earlier test above did invoke it already once
        verify(categoryMap, times(2)).remove(category);
        assertEquals(1, interceptedCategories.size());
        assertEquals(category, interceptedCategories.get(0));
        verifyNoMoreInteractions(categoryMap);
    }
    
    /**
     * Tests a query which returns results in a single batch.
     * 
     * By setting hasMoreBatches to false in WebQueryResponse we signal that
     * there are no more batches available via getMore().
     * 
     * @see {@link #canPrepareAndExecuteQueryMultiBatchFailure()}
     * @see {@link #canPrepareAndExecuteQueryMultiBatchSuccess()}
     */
    @Test
    public void canPrepareAndExecuteQuerySingleBatch() {
        WebQueryResponse<TestObj> fakeQueryResponse = new WebQueryResponse<>();
        fakeQueryResponse.setResponseCode(PreparedStatementResponseCode.QUERY_SUCCESS);
        fakeQueryResponse.setResultList(getTwoTestObjects());
        fakeQueryResponse.setCursorId(444);
        // Setting this to false makes Cursor.hasNext() return false after the
        // current result list is exhausted.
        fakeQueryResponse.setHasMoreBatches(false);
        Cursor<TestObj> results = doBasicPrepareAndExecuteQueryTest(fakeQueryResponse);
        assertFalse(results.hasNext());
        assertNull(results.next());
    }
    
    /**
     * Tests a query which returns results in multiple batches. The get-more
     * call is successful in this test.
     * 
     * By setting hasMoreBatches to true in WebQueryResponse we signal that
     * there are more batches available via getMore().
     * @throws IOException 
     * @throws UnsupportedEncodingException 
     * 
     * @see {@link #canPrepareAndExecuteQueryMultiBatchFailure()}
     */
    @Test
    public void canPrepareAndExecuteQueryMultiBatchSuccess() throws UnsupportedEncodingException, IOException {
        WebQueryResponse<TestObj> fakeQueryResponse = new WebQueryResponse<>();
        fakeQueryResponse.setResponseCode(PreparedStatementResponseCode.QUERY_SUCCESS);
        fakeQueryResponse.setResultList(getTwoTestObjects());
        fakeQueryResponse.setCursorId(444);
        // Setting this to true makes Cursor.hasNext() return true after the
        // current result list is exhausted.
        fakeQueryResponse.setHasMoreBatches(true);
        // doBasicPrepareAndExecuteQueryTest performs two hasNext() and
        // next() calls on the cursor.
        Cursor<TestObj> results = doBasicPrepareAndExecuteQueryTest(fakeQueryResponse);
        assertTrue("Expected cursor to return true, since there are more batches", results.hasNext());
        assertEquals("POST", method);
        String path = requestURI.substring(requestURI.lastIndexOf('/'));
        assertEquals("/query-execute", path);

        TestObj more = new TestObj();
        more.setProperty1("get-more-result");
        WebQueryResponse<TestObj> getMoreResults = new WebQueryResponse<>();
        getMoreResults.setResponseCode(PreparedStatementResponseCode.QUERY_SUCCESS);
        getMoreResults.setCursorId(444);
        getMoreResults.setHasMoreBatches(true); // one more batch
        getMoreResults.setResultList(new TestObj[] {more});
        final Gson gson = getQueryGson();
        prepareServer(gson.toJson(getMoreResults));
        // the following next() call performs the get-more request
        // for which we had to prepare the server
        TestObj returnedGetMore = results.next();
        assertEquals("POST", method);
        path = requestURI.substring(requestURI.lastIndexOf('/'));
        assertEquals("/get-more", path);
        // Verify correctly passed parameters
        StringReader reader = new StringReader(requestBody);
        BufferedReader bufRead = new BufferedReader(reader);
        String line = URLDecoder.decode(bufRead.readLine(), "UTF-8");
        String[] requestParams = line.split("&");
        String prepStmtIdParam = requestParams[0];
        String cursorIdParam = requestParams[1];
        String batchSizeParam = requestParams[2];
        String[] prStmtArray = prepStmtIdParam.split("=");
        String[] cursorIdArray = cursorIdParam.split("=");
        String[] batchSizeArray = batchSizeParam.split("=");
        assertEquals("prepared-stmt-id", prStmtArray[0]);
        SharedStateId prStmtId = gson.fromJson(prStmtArray[1], SharedStateId.class);
        assertEquals(5, prStmtId.getId());
        assertEquals("cursor-id", cursorIdArray[0]);
        assertEquals("444", cursorIdArray[1]);
        assertEquals("batch-size", batchSizeArray[0]);
        assertEquals(Integer.toString(BatchCursor.DEFAULT_BATCH_SIZE), batchSizeArray[1]);

        assertEquals("get-more-result", returnedGetMore.getProperty1());
        
        
        // Do it again, this time with a non-default batch size: 5
        
        assertTrue(results instanceof BatchCursor);
        BatchCursor<TestObj> advCursor = (BatchCursor<TestObj>)results;
        advCursor.setBatchSize(5);
        
        WebQueryResponse<TestObj> getMoreResults2 = new WebQueryResponse<>();
        getMoreResults2.setResponseCode(PreparedStatementResponseCode.QUERY_SUCCESS);
        getMoreResults2.setCursorId(444);
        getMoreResults2.setHasMoreBatches(false); // no more batches this time
        getMoreResults2.setResultList(new TestObj[] { more });
        prepareServer(gson.toJson(getMoreResults2));
        advCursor.next();
        
        path = requestURI.substring(requestURI.lastIndexOf('/'));
        assertEquals("/get-more", path);
        
        String[] batchSizeParamPair = requestBody.split("&")[2].split("=");
        assertEquals("batch-size", batchSizeParamPair[0]);
        assertEquals("5", batchSizeParamPair[1]);
    }
    
    /**
     * Tests a query which returns results in multiple batches. The get-more
     * call fails on the server side, though.
     *
     * @see {@link #canPrepareAndExecuteQueryMultiBatchSuccess()}
     */
    @Test
    public void canPrepareAndExecuteQueryMultiBatchFailure() {
        WebQueryResponse<TestObj> fakeQueryResponse = new WebQueryResponse<>();
        fakeQueryResponse.setResponseCode(PreparedStatementResponseCode.QUERY_SUCCESS);
        fakeQueryResponse.setResultList(getTwoTestObjects());
        fakeQueryResponse.setCursorId(444);
        fakeQueryResponse.setHasMoreBatches(true);
        Cursor<TestObj> results = doBasicPrepareAndExecuteQueryTest(fakeQueryResponse);
        assertTrue("Expected cursor to return true, since there are more batches", results.hasNext());
        
        WebQueryResponse<TestObj> getMoreResults = new WebQueryResponse<>();
        getMoreResults.setResponseCode(PreparedStatementResponseCode.QUERY_FAILURE);
        final Gson gson = getQueryGson();
        prepareServer(gson.toJson(getMoreResults));
        try {
            results.next();
            fail(); // Expected storage exception
        } catch (StorageException e) {
            assertEquals("[get-more] Failed to get more results for cursorId: 444. See server logs for details.", e.getMessage());
        }
        // Do it again with a generic failure code
        getMoreResults.setResponseCode(-0xcafeBabe); // this should be unknown
        prepareServer(gson.toJson(getMoreResults));
        try {
            results.next();
            fail(); // Expected storage exception
        } catch (StorageException e) {
            assertEquals("[get-more] Failed to get more results for cursorId: 444. See server logs for details.", e.getMessage());
        }
    }
    
    private TestObj[] getTwoTestObjects() {
        TestObj obj1 = new TestObj();
        obj1.setProperty1("fluffor1");
        TestObj obj2 = new TestObj();
        obj2.setProperty1("fluffor2");
        return new TestObj[] { obj1, obj2 };
    }

    private Cursor<TestObj> doBasicPrepareAndExecuteQueryTest(WebQueryResponse<TestObj> fakeQueryResponse) {
        Gson gson = getQueryGson();

        String strDesc = "QUERY test WHERE 'property1' = ?s";
        StatementDescriptor<TestObj> desc = new StatementDescriptor<>(category, strDesc);
        PreparedStatement<TestObj> stmt = null;
        
        int fakePrepStmtId = 5;
        WebPreparedStatementResponse fakeResponse = new WebPreparedStatementResponse();
        fakeResponse.setNumFreeVariables(1);
        SharedStateId id = new SharedStateId(fakePrepStmtId, UUID.randomUUID());
        fakeResponse.setStatementId(id);
        prepareServer(gson.toJson(fakeResponse));
        try {
            stmt = storage.prepareStatement(desc);
        } catch (DescriptorParsingException e) {
            // descriptor should parse fine and is trusted
            fail(e.getMessage());
        }
        assertTrue(stmt instanceof WebPreparedStatement);
        WebPreparedStatement<TestObj> webStmt = (WebPreparedStatement<TestObj>)stmt;
        assertEquals(fakePrepStmtId, webStmt.getStatementId().getId());
        PreparedParameters params = webStmt.getParams();
        assertEquals(1, params.getParams().length);
        assertNull(params.getParams()[0]);
        
        // now set a parameter
        stmt.setString(0, "fluff");
        assertEquals("fluff", params.getParams()[0].getValue());
        assertEquals(String.class, params.getParams()[0].getType());
        
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
        assertTrue("Expected WebCursor to be an AdvancedCursor", results instanceof BatchCursor);
        assertTrue(results.hasNext());
        assertEquals("fluffor1", results.next().getProperty1());
        assertTrue(results.hasNext());
        assertEquals("fluffor2", results.next().getProperty1());
        return results;
    }
    
    private Gson getQueryGson() {
        return new GsonBuilder()
                    .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                    .registerTypeAdapterFactory(new SharedStateIdTypeAdapterFactory())
                    .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                    .registerTypeAdapterFactory(new WebQueryResponseTypeAdapterFactory())
                    .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                    .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                    .create();
    }
    
    @Test
    public void canPrepareAndExecuteWrite() {
        TestObj obj1 = new TestObj();
        obj1.setProperty1("fluffor1");
        TestObj obj2 = new TestObj();
        obj2.setProperty1("fluffor2");
        Gson gson = new GsonBuilder()
                            .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                            .registerTypeAdapterFactory(new SharedStateIdTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebQueryResponseTypeAdapterFactory())
                            .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                            .create();

        String strDesc = "ADD test SET 'property1' = ?s";
        StatementDescriptor<TestObj> desc = new StatementDescriptor<>(category, strDesc);
        PreparedStatement<TestObj> stmt = null;
        
        int fakePrepStmtId = 3;
        WebPreparedStatementResponse fakeResponse = new WebPreparedStatementResponse();
        fakeResponse.setNumFreeVariables(1);
        SharedStateId id = new SharedStateId(fakePrepStmtId, UUID.randomUUID());
        fakeResponse.setStatementId(id);
        prepareServer(gson.toJson(fakeResponse));
        try {
            stmt = storage.prepareStatement(desc);
        } catch (DescriptorParsingException e) {
            // descriptor should parse fine and is trusted
            fail(e.getMessage());
        }
        assertTrue(stmt instanceof WebPreparedStatement);
        WebPreparedStatement<TestObj> webStmt = (WebPreparedStatement<TestObj>)stmt;
        assertEquals(fakePrepStmtId, webStmt.getStatementId().getId());
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
    public void forbiddenExecuteWriteReturnsGenericWriteFailure() {
        Gson gson = new GsonBuilder()
                            .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                            .registerTypeAdapterFactory(new SharedStateIdTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebQueryResponseTypeAdapterFactory())
                            .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                            .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                            .create();

        String strDesc = "ADD test SET 'property1' = ?s";
        StatementDescriptor<TestObj> desc = new StatementDescriptor<>(category, strDesc);
        PreparedStatement<TestObj> stmt = null;
        
        int fakePrepStmtId = 3;
        WebPreparedStatementResponse fakeResponse = new WebPreparedStatementResponse();
        fakeResponse.setNumFreeVariables(1);
        SharedStateId id = new SharedStateId(fakePrepStmtId, UUID.randomUUID());
        fakeResponse.setStatementId(id);
        prepareServer(gson.toJson(fakeResponse));
        try {
            stmt = storage.prepareStatement(desc);
        } catch (DescriptorParsingException e) {
            // descriptor should parse fine and is trusted
            fail(e.getMessage());
        }
        assertTrue(stmt instanceof WebPreparedStatement);
        WebPreparedStatement<TestObj> webStmt = (WebPreparedStatement<TestObj>)stmt;
        assertEquals(fakePrepStmtId, webStmt.getStatementId().getId());
        PreparedParameters params = webStmt.getParams();
        assertEquals(1, params.getParams().length);
        assertNull(params.getParams()[0]);
        
        // now set a parameter
        stmt.setString(0, "fluff");
        assertEquals("fluff", params.getParams()[0].getValue());
        assertEquals(String.class, params.getParams()[0].getType());
        
        prepareServer(HttpServletResponse.SC_FORBIDDEN);
        try {
            stmt.execute();
            fail("Forbidden should have thrown an exception!");
        } catch (StatementExecutionException e) {
            Throwable t = e.getCause();
            assertTrue(t instanceof StorageException);
            t = t.getCause();
            assertTrue("Wanted EntityConsumingIOException as root cause", t instanceof EntityConsumingIOException);
        }
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
    public void enablesTLSforHttps() {
        SSLConfiguration sslConf = mock(SSLConfiguration.class);
        String httpsUrl = "https://onlyHttpsPrefixUsed.example.com";
        new WebStorage(httpsUrl,
                new TrivialStorageCredentials(null, null), sslConf);
        // should get called for HTTPS URLs
        verify(sslConf).getKeystoreFile();
        verify(sslConf).getKeyStorePassword();
    }
    
    @Test
    public void doesnotEnableTLSForHttp() {
        SSLConfiguration sslConf = mock(SSLConfiguration.class);
        String httpsUrl = "http://foo.example.com";
        new WebStorage(httpsUrl,
                new TrivialStorageCredentials(null, null), sslConf);
        verifyNoMoreInteractions(sslConf);
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
    public void verifyConnectFiresEventOnConnectionFailure() throws Exception {
        HttpClient client = mock(HttpClient.class);
        // Execution of ping() will fail
        Mockito.doThrow(RuntimeException.class).when(client).execute(any(HttpUriRequest.class), any(HttpContext.class));
        storage = new WebStorage("http://localhost:" + port + "/", new TrivialStorageCredentials(null, null),
                client);
        
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
    
    @Test
    public void verifyDoWriteExecuteMorethanOne() {
        try {
            storage.doWriteExecute(null, 2);
            fail("Expected excecution exception since invoked count > 1");
        } catch (StatementExecutionException e) {
            Throwable cause = e.getCause();
            // pass
            assertEquals("Failed to recover from out-of-sync state with server", cause.getMessage());
        }
    }
    
    @Test
    public void verifyDoExecuteQueryMorethanOne() {
        try {
            storage.doExecuteQuery(null, null, 2);
            fail("Expected descriptor parsing exception since invoked count > 1");
        } catch (StatementExecutionException e) {
            Throwable cause = e.getCause();
            // pass
            assertEquals("Failed to recover from out-of-sync state with server", cause.getMessage());
        }
    }
    
    @Test
    public void verifySendPreparedStatementRequestMoreThanOne() {
        try {
            storage.sendPrepareStmtRequest(null, 2);
            fail("Expected descriptor parsing exception since invoked count > 1");
        } catch (DescriptorParsingException e) {
            // pass
            assertEquals("Failed to recover from out-of-sync state with server", e.getMessage());
        }
    }
    
    /**
     * Test the base case in {@link WebStorage#handlePreparedStmtStateOutOfSync(WebPreparedStatement)}.
     * with null transition cache and non-null statement cache. This simulates the
     * case where state got out of sync and handlePreparedStmtStateOutOfSync() is
     * called the first time after that out-of-sync-event happened. In that case
     * it is expected for prepareStatement() and sendCategoryReRegistrationRequest()
     * to be called.
     * 
     * @throws DescriptorParsingException 
     * 
     */
    @Test
    public void testHandleStatementStateOutOfSyncBaseCase() throws DescriptorParsingException {
        WebPreparedStatementHolder mockHolder = mock(WebPreparedStatementHolder.class);
        SharedStateId id = new SharedStateId(300, UUID.randomUUID());
        when(mockHolder.getStatementId()).thenReturn(id);
        @SuppressWarnings("unchecked")
        Category<Pojo> foo = mock(Category.class);
        StatementDescriptor<Pojo> desc = new StatementDescriptor<>(foo, "testing");
        WebPreparedStatementCache stmtCache = mock(WebPreparedStatementCache.class);
        // this is called twice. Once for creating the snapshot and another time
        // for getting the descriptor.
        when(stmtCache.get(id)).thenReturn(desc).thenReturn(desc);
        final boolean[] sendCategoryReRegistrationRequest = new boolean[1];
        final boolean[] prepareStatement = new boolean[1];
        final WebPreparedStatement<?> newStmt = mock(WebPreparedStatement.class);
        WebStorage webStorage = new WebStorage(stmtCache, null) {
            
            @Override
            protected synchronized <T extends Pojo> void sendCategoryReRegistrationRequest(Category<T> category) {
                sendCategoryReRegistrationRequest[0] = true;
            }
            
            @SuppressWarnings("unchecked")
            @Override
            public <T extends Pojo> PreparedStatement<T> prepareStatement(StatementDescriptor<T> desc)
                    throws DescriptorParsingException {
                prepareStatement[0] = true;
                return (PreparedStatement<T>)newStmt;
            }
        };
        @SuppressWarnings("unchecked")
        WebPreparedStatement<Pojo> mockStmt = mock(WebPreparedStatement.class);
        PreparedParameters mockParams = new PreparedParameters(3);
        when(mockStmt.getParams()).thenReturn(mockParams);
        when(mockStmt.getStatementId()).thenReturn(id);
        
        WebPreparedStatementCache stmtCacheSnapshot = mock(WebPreparedStatementCache.class);
        when(stmtCache.createSnapshot()).thenReturn(stmtCacheSnapshot);
        webStorage.handlePreparedStmtStateOutOfSync(mockStmt);
        verify(stmtCache).createSnapshot();
        verify(stmtCache).get(id);
        verify(stmtCache).remove(id);
        assertTrue("expected sendCategoryReRegistrationRequest() to be called", sendCategoryReRegistrationRequest[0]);
        assertTrue("expected prepareStatement() to be called", prepareStatement[0]);
        verify(newStmt).setParams(mockParams);
        verifyNoMoreInteractions(stmtCache);
    }
    
    /**
     * Test the transition case in {@link WebStorage#handlePreparedStmtStateOutOfSync(WebPreparedStatement)}.
     * with non-null transition cache and non-null statement cache.
     * 
     * This simulates the case where state got out of sync and handlePreparedStmtStateOutOfSync() is
     * called <strong>not</strong> the first time after an out-of-sync-event happened. I.e.
     * the base-case path has been entered first when a similar statement tried
     * to execute, in turn, getting the statement id removed from the main
     * statement cache.
     * 
     * In that case it is expected for the transition cache to become active
     * allowing the statement to execute successfully. 
     * 
     * @throws DescriptorParsingException 
     * 
     */
    @Test
    public void testHandleStatementStateOutOfSyncTransitionCase() throws DescriptorParsingException {
        WebPreparedStatementHolder mockHolder = mock(WebPreparedStatementHolder.class);
        SharedStateId id = new SharedStateId(300, UUID.randomUUID());
        when(mockHolder.getStatementId()).thenReturn(id);
        @SuppressWarnings("unchecked")
        Category<Pojo> foo = mock(Category.class);
        StatementDescriptor<Pojo> desc = new StatementDescriptor<>(foo, "testing");
        WebPreparedStatementCache stmtCache = mock(WebPreparedStatementCache.class);
        // no setup for the id in stmtCache, however the transitionCache,
        // a snapshot cache - created when the first call to handlePreparedStatementStateOutOfSync()
        // came in - still "knows" about this record.
        ExpirableWebPreparedStatementCache transitionCache = mock(ExpirableWebPreparedStatementCache.class);
        when(transitionCache.isExpired()).thenReturn(false);
        when(transitionCache.get(id)).thenReturn(desc);
        when(transitionCache.get(desc)).thenReturn(mockHolder);
        SharedStateId updatedId = new SharedStateId(301, UUID.randomUUID());
        WebPreparedStatementHolder newHolder = mock(WebPreparedStatementHolder.class);
        when(mockHolder.getStatementId()).thenReturn(id);
        // called twice. once for equality check. once for getting the id and
        // using it to update the prepared statement id.
        when(newHolder.getStatementId()).thenReturn(updatedId).thenReturn(updatedId);
        when(stmtCache.get(desc)).thenReturn(newHolder);
        WebStorage webStorage = new WebStorage(stmtCache, transitionCache);
        @SuppressWarnings("unchecked")
        WebPreparedStatement<Pojo> mockStmt = mock(WebPreparedStatement.class);
        when(mockStmt.getStatementId()).thenReturn(id);
        WebPreparedStatement<Pojo> result = webStorage.handlePreparedStmtStateOutOfSync(mockStmt);
        verify(mockStmt).setStatementId(updatedId);
        assertSame(mockStmt, result);
        verify(stmtCache).get(id);
        verify(stmtCache).get(desc);
        verify(transitionCache).isExpired();
        verify(transitionCache).get(id);
        verify(transitionCache).get(desc);
        verifyNoMoreInteractions(stmtCache);
        verifyNoMoreInteractions(transitionCache);
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
    
    private static class PrepareStatementWebStorage extends WebStorage {

        private int counter;
        
        public PrepareStatementWebStorage(String url, StorageCredentials creds,
                SSLConfiguration sslConf) throws StorageException {
            super(url, creds, sslConf);
        }
        
        @Override
        <T extends Pojo> WebPreparedStatementHolder sendPrepareStmtRequest(StatementDescriptor<T> desc, int invokationCounter)
                throws DescriptorParsingException {
            int numParams = counter++;
            int stmtId = counter++;
            SharedStateId id = new SharedStateId(stmtId, UUID.randomUUID());
            return new WebPreparedStatementHolder(TestObj.class, numParams, id); 
        }
    }
    
    private static class TestAggregate implements AggregateResult {
        // nothing
    }
}

