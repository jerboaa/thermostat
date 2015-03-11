/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Lookup;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.common.ssl.SSLContextFactory;
import com.redhat.thermostat.common.ssl.SslInitException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.storage.core.Categories;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.IllegalDescriptorException;
import com.redhat.thermostat.storage.core.IllegalPatchException;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.SaveFileListener;
import com.redhat.thermostat.storage.core.SecureStorage;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.core.SaveFileListener.EventType;
import com.redhat.thermostat.storage.model.AggregateResult;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.web.common.PreparedStatementResponseCode;
import com.redhat.thermostat.web.common.SharedStateId;
import com.redhat.thermostat.web.common.WebPreparedStatement;
import com.redhat.thermostat.web.common.WebPreparedStatementResponse;
import com.redhat.thermostat.web.common.WebQueryResponse;
import com.redhat.thermostat.web.common.typeadapters.PojoTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.PreparedParameterTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.PreparedParametersTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.SharedStateIdTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.WebPreparedStatementResponseTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.WebPreparedStatementTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.WebQueryResponseTypeAdapterFactory;

public class WebStorage implements Storage, SecureStorage {

    private static final int STATUS_OK = 200;
    private static final int STATUS_NO_CONTENT = 204;

    private static final String HTTP_PREFIX = "http";
    private static final String HTTPS_PREFIX = "https";
    
    // Transition cache is valid for 30 seconds starting from the current time.
    private static final long TRANSITION_CACHE_OFFSET = TimeUnit.NANOSECONDS.convert(30, TimeUnit.SECONDS);
    
    static final Logger logger = LoggingUtils.getLogger(WebStorage.class);
    
    private static class CloseableHttpEntity implements Closeable, HttpEntity {

        private HttpEntity entity;
        private int responseCode;

        CloseableHttpEntity(HttpEntity entity, int responseCode) {
            this.entity = entity;
            this.responseCode = responseCode;
        }

        @Override
        public void consumeContent() throws IOException {
            EntityUtils.consume(entity);
        }

        @Override
        public InputStream getContent() throws IOException,
                IllegalStateException {
            return entity.getContent();
        }

        @Override
        public Header getContentEncoding() {
            return entity.getContentEncoding();
        }

        @Override
        public long getContentLength() {
            return entity.getContentLength();
        }

        @Override
        public Header getContentType() {
            return entity.getContentType();
        }

        @Override
        public boolean isChunked() {
            return entity.isChunked();
        }

        @Override
        public boolean isRepeatable() {
            return entity.isRepeatable();
        }

        @Override
        public boolean isStreaming() {
            return entity.isStreaming();
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            entity.writeTo(out);
        }

        @Override
        public void close() {
            try {
                EntityUtils.consume(entity);
            } catch (IOException ex) {
                throw new StorageException(ex);
            }
        }

        int getResponseCode() {
            return responseCode;
        }
    }

    private final class WebConnection extends Connection {
        WebConnection() {
            connected = true;
        }

        @Override
        public void disconnect() {
            connected = false;
            setUsername(Connection.UNSET_USERNAME);
            fireChanged(ConnectionStatus.DISCONNECTED);
        }

        @Override
        public void connect() {
            try {
                initAuthentication();
                ping();
                connected = true;
                logger.fine("Connected to storage");
                fireChanged(ConnectionStatus.CONNECTED);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not connect to storage!", ex);
                setUsername(Connection.UNSET_USERNAME);
                fireChanged(ConnectionStatus.FAILED_TO_CONNECT);
            }
        }
        
        private void initAuthentication()
                throws MalformedURLException {
            String username = creds.getUsername();
            setUsername(username);
            char[] password = creds.getPassword();
            if (username != null && password != null) {
                URL endpointURL = new URL(endpoint);
                BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
                // FIXME Password as string?  BAD.  Limited by apache API here however.
                Credentials creds = new UsernamePasswordCredentials(username,
                        new String(password));
                Arrays.fill(password, '\0');
                AuthScope scope = new AuthScope(endpointURL.getHost(),
                        endpointURL.getPort(), "Thermostat Realm");
                credsProvider.setCredentials(scope, creds);
                synchronized (httpClientContextLock) {
                    httpClientContext.setCredentialsProvider(credsProvider);
                }
            }
        }

        @Override
        public void setUrl(String url) {
            super.setUrl(url);
            endpoint = url;
        }

        @Override
        public String getUrl() {
            return endpoint;
        }
    }

    private static class WebDataStream extends InputStream {

        private CloseableHttpEntity entity;
        private InputStream content;

        WebDataStream(CloseableHttpEntity entity) {
            this.entity = entity;
            try {
                content = entity.getContent();
            } catch (IllegalStateException | IOException e) {
                throw new StorageException(e);
            }
        }

        @Override
        public void close() throws IOException {
            content.close();
            entity.close();
        }

        @Override
        public int read() throws IOException {
            return content.read();
        }

        @Override
        public int available() throws IOException {
            return content.available();
        }

        @Override
        public void mark(int readlimit) {
            content.mark(readlimit);
        }

        @Override
        public boolean markSupported() {
            return content.markSupported();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return content.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return content.read(b, off, len);
        }

        @Override
        public void reset() throws IOException {
            content.reset();
        }

        @Override
        public long skip(long n) throws IOException {
            return content.skip(n);
        }

    }

    private class WebPreparedStatementImpl<T extends Pojo> extends WebPreparedStatement<T> {

        // The type of the query result objects we'd get back upon
        // statement execution
        private final transient Type parametrizedTypeToken;
        
        public WebPreparedStatementImpl(Type parametrizedTypeToken, int numParams, SharedStateId statementId) {
            super(numParams, statementId);
            this.parametrizedTypeToken = parametrizedTypeToken;
        }
        
        @Override
        public int execute() throws StatementExecutionException {
            return doWriteExecute(this, 0);
        }

        @Override
        public Cursor<T> executeQuery()
                throws StatementExecutionException {
            return doExecuteQuery(this, parametrizedTypeToken, 0);
        }
        
    }

    private String endpoint;

    private Map<Category<?>, SharedStateId> categoryIds;
    private Gson gson;
    // The shared http client we use for execution (uses the context below)
    private HttpClient httpClient;
    private Object httpClientContextLock = new Object();
    // Http client execution context. Protected via clientContext lock.
    private HttpClientContext httpClientContext;
    private StorageCredentials creds;
    private SecureRandom random;
    private WebConnection conn;
    private WebPreparedStatementCache stmtCache;
    // Temporary cache used for recovering after a server endpoint re-deployment.
    // Will only be valid for 30 seconds for any server endpoint re-deployment.
    private ExpirableWebPreparedStatementCache transitionStmtCache;
    
    // for testing
    WebStorage(String url, StorageCredentials creds, HttpClient client) {
        init(url, creds, client);
    }
    
    // for testing
    WebStorage(WebPreparedStatementCache stmtCache, ExpirableWebPreparedStatementCache transitionCache) {
        this.stmtCache = stmtCache;
        this.transitionStmtCache = transitionCache;
    }
    
    // for testing
    WebStorage(Map<Category<?>, SharedStateId> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public WebStorage(String url, StorageCredentials creds, SSLConfiguration sslConf) throws StorageException {
        PoolingHttpClientConnectionManager connManager = getPoolingHttpClientConnManager(sslConf, url);
        HttpClientBuilder builder = HttpClients.custom();
        Lookup<AuthSchemeProvider> authProviders = RegistryBuilder.<AuthSchemeProvider>create()
                .register(AuthSchemes.BASIC, new BasicSchemeFactory())
                .build();
        // Set up client with default basic-auth scheme and pooled
        // connection manager.
        HttpClient client = builder.setConnectionManager(connManager)
                .setDefaultAuthSchemeRegistry(authProviders)
                .build();
        init(url, creds, client);
    }
    
    private void init(String url, StorageCredentials creds, HttpClient client) {
        categoryIds = new HashMap<>();
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                .registerTypeAdapterFactory(new SharedStateIdTypeAdapterFactory())
                .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                .registerTypeAdapterFactory(new WebQueryResponseTypeAdapterFactory())
                .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                .registerTypeAdapterFactory(new PreparedParametersTypeAdapterFactory())
                .create();
        httpClient = client;
        synchronized (httpClientContextLock) {
            httpClientContext = HttpClientContext.create();
        }
        random = new SecureRandom();
        conn = new WebConnection();
        
        this.endpoint = url;
        this.creds = creds;
        this.stmtCache = new WebPreparedStatementCache();
    }

    // package private for testing
    PoolingHttpClientConnectionManager getPoolingHttpClientConnManager(SSLConfiguration sslConf, String url)
            throws StorageException {
        ConnectionSocketFactory plainsf = new PlainConnectionSocketFactory();
        RegistryBuilder<ConnectionSocketFactory> regBuilder = RegistryBuilder.<ConnectionSocketFactory>create()
                .register(HTTP_PREFIX, plainsf);
        try {
            // setup SSL if necessary
            if (url.startsWith(HTTPS_PREFIX)) {
                SSLContext sc = SSLContextFactory.getClientContext(sslConf);
                SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sc);
                regBuilder = regBuilder.register(HTTPS_PREFIX, socketFactory);
            }
        } catch ( SslInitException e) {
            throw new StorageException(e);
        }
        Registry<ConnectionSocketFactory> r = regBuilder.build();
        return new PoolingHttpClientConnectionManager(r);
    }

    private void ping() throws StorageException {
        post(endpoint + "/ping", (HttpEntity) null).close();
    }

    private CloseableHttpEntity post(String url, List<NameValuePair> formparams)
            throws StorageException {
        try {
            return postImpl(url, formparams);
        } catch (IOException ex) {
            throw new StorageException(ex);
        }
    }

    private CloseableHttpEntity postImpl(String url,
            List<NameValuePair> formparams) throws IOException {
        HttpEntity entity;
        if (formparams != null) {
            entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        } else {
            entity = null;
        }
        return postImpl(url, entity);
    }

    private CloseableHttpEntity post(String url, HttpEntity entity)
            throws StorageException {
        try {
            return postImpl(url, entity, null);
        } catch (IOException ex) {
            throw new StorageException(ex);
        }
    }
    
    private CloseableHttpEntity post(String url, HttpEntity entity, RequestConfig config)
            throws StorageException {
        try {
            return postImpl(url, entity, config);
        } catch (IOException ex) {
            throw new StorageException(ex);
        }
    }
    
    private CloseableHttpEntity postImpl(String url, HttpEntity entity, RequestConfig config)
            throws IOException {
        HttpPost httpPost = new HttpPost(url);
        if (entity != null) {
            httpPost.setEntity(entity);
        }
        HttpResponse response = null;
        // The client context is not thread-safe. Thus protect execution
        // via the client context lock.
        synchronized(httpClientContextLock) {
            RequestConfig oldConfig = httpClientContext.getRequestConfig();
            if (config != null) {
                httpClientContext.setRequestConfig(config);
            }
            try {
                response = httpClient.execute(httpPost, httpClientContext);
            } catch (Throwable e) {
                throw e;
            } finally {
                if (config != null) {
                    httpClientContext.setRequestConfig(oldConfig);
                }
            }
        }
        StatusLine status = response.getStatusLine();
        int responseCode = status.getStatusCode();
        switch (responseCode) {
        case (STATUS_NO_CONTENT):
            // Let calling code handle STATUS_NO_CONTENT
            break;
        case (STATUS_OK):
            // Let calling code handle STATUS_OK
            break;
        default:
            // Properly consume the entity, thus closing the content stream,
            // by throwing this IOException sub-class. This is important for the
            // 403 and 500 status code cases. See:
            // http://hc.apache.org/httpcomponents-core-4.3.x/httpcore/apidocs/org/apache/http/util/EntityUtils.html#consume%28org.apache.http.HttpEntity%29
            throw new EntityConsumingIOException(response.getEntity(), 
                    "Server returned status: " + status);
        }
        
        return new CloseableHttpEntity(response.getEntity(), responseCode);
    }

    private CloseableHttpEntity postImpl(String url, HttpEntity entity)
            throws StorageException {
        try {
            return postImpl(url, entity, null);
        } catch (IOException ex) {
            throw new StorageException(ex);
        }
    }

    private static InputStream getContent(HttpEntity entity) {
        try {
            return entity.getContent();
        } catch (IOException ex) {
            throw new StorageException(ex);
        }
    }

    private static Reader getContentAsReader(HttpEntity entity) {
        InputStream in = getContent(entity);
        return new InputStreamReader(in);
    }

    @Override
    public void registerCategory(Category<?> category) throws StorageException {
        NameValuePair nameParam = new BasicNameValuePair("name",
                category.getName());
        NameValuePair dataClassParam = new BasicNameValuePair("data-class",
                category.getDataClass().getName());
        
        NameValuePair categoryParam = new BasicNameValuePair("category",
                gson.toJson(category));
        List<NameValuePair> formparams = Arrays
                .asList(nameParam, categoryParam, dataClassParam);
        try (CloseableHttpEntity entity = post(endpoint + "/register-category",
                formparams)) {
            Reader reader = getContentAsReader(entity);
            SharedStateId id = gson.fromJson(reader, SharedStateId.class);
            categoryIds.put(category, id);
        }
    }

    /**
     * Executes a prepared query
     * 
     * @param stmt
     *            The prepared statement to execute
     * @param parametrizedTypeToken
     *            The parametrized type token to use for deserialization.
     *            Example as to how this was created:
     *            <pre>
     *            Type parametrizedTypeToken = new
     *            TypeToken&lt;WebQueryResponse&lt;AgentInformation&gt;&gt;().getType();
     *            </pre>
     * @param invocationCount The number of recursive invocations performed so far.
     * @return A cursor for the generic type.
     * @throws StatementExecutionException
     *             If execution of the statement failed. In particular, if
     *             the state got out of sync, it tried to recover and then
     *             failed again.
     */
    <T extends Pojo> Cursor<T> doExecuteQuery(final WebPreparedStatement<T> stmt, Type parametrizedTypeToken, final int invocationCount) throws StatementExecutionException {
        checkRecursiveInvocationCount(invocationCount);
        NameValuePair queryParam = new BasicNameValuePair("prepared-stmt", gson.toJson(stmt, WebPreparedStatement.class));
        List<NameValuePair> formparams = Arrays.asList(queryParam);
        WebQueryResponse<T> qResp = null;
        try (CloseableHttpEntity entity = post(endpoint + "/query-execute", formparams)) {
            Reader reader = getContentAsReader(entity);
            qResp = gson.fromJson(reader, parametrizedTypeToken);
        } catch (Exception e) {
            throw new StatementExecutionException(e);
        }
        switch(qResp.getResponseCode()) {
        case PreparedStatementResponseCode.QUERY_SUCCESS:
            return new WebCursor<T>(this, qResp.getResultList(),
                    qResp.hasMoreBatches(),
                    qResp.getCursorId(), parametrizedTypeToken, stmt);
        case PreparedStatementResponseCode.ILLEGAL_PATCH: {
            String msg = "Illegal statement argument. See server logs for details.";
            IllegalArgumentException iae = new IllegalArgumentException(msg);
            IllegalPatchException e = new IllegalPatchException(iae);
            throw new StatementExecutionException(e);
        }
        case PreparedStatementResponseCode.PREP_STMT_BAD_STOKEN: {
            // Try to recover from this situation. If this path is
            // entered more than once than we'll fail on method entry.
            try {
                WebPreparedStatement<T> newStmt = handlePreparedStmtStateOutOfSync(stmt);
                return doExecuteQuery(newStmt, parametrizedTypeToken, invocationCount + 1);
            } catch (DescriptorParsingException e) {
                throw new StatementExecutionException(e);
            }
        }
        default: {
            String msg = "[query-execute] Unknown response from storage endpoint!";
            IllegalStateException ise = new IllegalStateException(msg);
            throw new StatementExecutionException(ise);
        }
        }
    }
    
    private void checkRecursiveInvocationCount(int invocationCount) throws StatementExecutionException {
        if (invocationCount > 1) {
            // Initial invokation == 0, potential recovery-invocation == 1
            String msg = "Failed to recover from out-of-sync state with server";
            logger.log(Level.WARNING, msg);
            throw new StatementExecutionException(new IllegalStateException(msg));
        }
    }
    
    /**
     * This method gets called from WebCursor in order to fetch more results
     * or refresh the result set since parameters like limit or skip have
     * changed since the original result set was fetched.
     * 
     * @param cursorId
     * @param parametrizedTypeToken The type token for the data class (Pojo).
     * @param batchSize The desired batchSize or null. null means that the user
     *                  did not set an explicit batch size.
     * @param limit The desired limit for this cursor or null. null means that
     *              a user did not set an explicit limit.
     * @param skip The desired skip value or null. null means no skip value has
     *             been specified by the user.
     * @return
     */
    <T extends Pojo> WebQueryResponse<T> getMore(int cursorId, Type parametrizedTypeToken, Integer batchSize, WebPreparedStatement<T> stmt) {
        String stmtId = gson.toJson(stmt.getStatementId());
        NameValuePair preparedStmtIdParam = new BasicNameValuePair("prepared-stmt-id", stmtId);
        NameValuePair cursorIdParam = new BasicNameValuePair("cursor-id", Integer.toString(cursorId));
        NameValuePair batchSizeParam = new BasicNameValuePair("batch-size", batchSize.toString());
        
        List<NameValuePair> formparams = Arrays.asList(preparedStmtIdParam,
                                                       cursorIdParam,
                                                       batchSizeParam);
        WebQueryResponse<T> qResp = null;
        try (CloseableHttpEntity entity = post(endpoint + "/get-more", formparams)) {
            Reader reader = getContentAsReader(entity);
            qResp = gson.fromJson(reader, parametrizedTypeToken);
        } catch (Exception e) {
            throw new StorageException(e);
        }
        return qResp;
    }
    
    /**
     * Executes a prepared write
     * 
     * @param stmt
     *            The prepared statement to execute
     * @param invocationCount
     *            The number of times this method has been recursively called,
     *            starting at 0.
     * @return The response code of executing the underlying data modifying
     *         statement.
     * @throws StatementExecutionException
     *             If execution of the statement failed. For example if the
     *             values set as prepared parameters did not work or were
     *             partially missing for the prepared statement.
     */
    <T extends Pojo> int doWriteExecute(final WebPreparedStatement<T> stmt, final int invocationCount)
            throws StatementExecutionException {
        checkRecursiveInvocationCount(invocationCount);
        NameValuePair queryParam = new BasicNameValuePair("prepared-stmt", gson.toJson(stmt, WebPreparedStatement.class));
        List<NameValuePair> formparams = Arrays.asList(queryParam);
        int responseCode = PreparedStatementResponseCode.WRITE_GENERIC_FAILURE;
        try (CloseableHttpEntity entity = post(endpoint + "/write-execute", formparams)) {
            Reader reader = getContentAsReader(entity);
            responseCode = gson.fromJson(reader, int.class);
        } catch (Exception e) {
            throw new StatementExecutionException(e);
        }
        if (responseCode == PreparedStatementResponseCode.ILLEGAL_PATCH) {
            String msg = "Illegal statement argument. See server logs for details. Invokation count: " + invocationCount;
            IllegalArgumentException iae = new IllegalArgumentException(msg);
            IllegalPatchException e = new IllegalPatchException(iae);
            throw new StatementExecutionException(e);
        } else if (responseCode == PreparedStatementResponseCode.PREP_STMT_BAD_STOKEN) {
            // Try to recover from this situation. If this path is
            // entered more than once than we'll fail on method entry.
            try {
                WebPreparedStatement<T> newStmt = handlePreparedStmtStateOutOfSync(stmt);
                return doWriteExecute(newStmt, invocationCount + 1);
            } catch (DescriptorParsingException e) {
                throw new StatementExecutionException(e);
            }
        }
        return responseCode;
    }

    @Override
    public Connection getConnection() {
        return conn;
    }

    @Override
    public InputStream loadFile(String name) throws StorageException {
        NameValuePair fileParam = new BasicNameValuePair("file", name);
        List<NameValuePair> formparams = Arrays.asList(fileParam);
        CloseableHttpEntity entity = post(endpoint + "/load-file", formparams);
        if (entity.getResponseCode() == STATUS_NO_CONTENT) {
            return null;
        }
        return new WebDataStream(entity);
    }

    @Override
    public void saveFile(String filename, InputStream data, SaveFileListener listener) {
        Objects.requireNonNull(listener);
        StorageException exceptionIfAny = null;

        try {
            doSave(filename, data);
        } catch (StorageException e) {
            exceptionIfAny = e;
        }

        if (exceptionIfAny != null) {
            listener.notify(EventType.EXCEPTION_OCCURRED, exceptionIfAny);
        } else {
            listener.notify(EventType.SAVE_COMPLETE, null);
        }
    }

    private void doSave(String name, InputStream in) throws StorageException {
        InputStreamBody body = new InputStreamBody(in, name);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        HttpEntity mpEntity = builder.addPart("file", body).build();
        // See IcedTea bug #1314. For safe-file we need to do this:
        // setExcpectContinueEnabled. However,
        // doing this for other actions messes up authentication when using
        // jetty (and possibly others). Hence, do this expect-continue thingy
        // only for save-file. We achieve this via a single request configuration.
        RequestConfig config = RequestConfig.custom().setExpectContinueEnabled(true).build();
        post(endpoint + "/save-file", mpEntity, config).close();
    }

    @Override
    public void purge(String agentId) throws StorageException {
        NameValuePair agentIdParam = new BasicNameValuePair("agentId", agentId);
        List<NameValuePair> agentIdParams = Arrays.asList(agentIdParam);
        post(endpoint + "/purge", agentIdParams).close();
    }

    @Override
    public AuthToken generateToken(String actionName) throws StorageException {
        byte[] clientToken = new byte[256];
        random.nextBytes(clientToken);
        NameValuePair clientTokenParam = new BasicNameValuePair("client-token", Base64.encodeBase64String(clientToken));
        NameValuePair actionNameParam = new BasicNameValuePair("action-name",
                Objects.requireNonNull(actionName));
        List<NameValuePair> formparams = Arrays.asList(clientTokenParam, actionNameParam);
        try (CloseableHttpEntity entity = post(endpoint + "/generate-token", formparams)) {
            byte[] authToken = EntityUtils.toByteArray(entity);
            return new AuthToken(authToken, clientToken);
        } catch (IOException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public boolean verifyToken(AuthToken authToken, String actionName) {
        byte[] clientToken = authToken.getClientToken();
        byte[] token = authToken.getToken();
        NameValuePair clientTokenParam = new BasicNameValuePair("client-token", Base64.encodeBase64String(clientToken));
        NameValuePair tokenParam = new BasicNameValuePair("token", Base64.encodeBase64String(token));
        NameValuePair actionNameParam = new BasicNameValuePair("action-name",
                Objects.requireNonNull(actionName));
        List<NameValuePair> formparams = Arrays.asList(clientTokenParam,
                tokenParam, actionNameParam);
        HttpResponse response = null;
        try {
            HttpEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            HttpPost httpPost = new HttpPost(endpoint + "/verify-token");
            httpPost.setEntity(entity);
            synchronized (httpClientContextLock) {
                response = httpClient.execute(httpPost, httpClientContext);
            }
            StatusLine status = response.getStatusLine();
            return status.getStatusCode() == STATUS_OK;
        } catch (IOException ex) {
            throw new StorageException(ex);
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException ex) {
                    throw new StorageException(ex);
                }
            }
        }
    }

    @Override
    public void shutdown() {
        // Nothing to do here.
    }

    SharedStateId getCategoryId(Category<?> category) {
        return categoryIds.get(category);
    }
    
    /**
     * Package private for testing
     * 
     * This method handles the recovery mechanism which needs to be done before
     * an already failed {@link WebPreparedStatement} can be re-submitted
     * because some state maintained in the client (here) and on the server
     * need to be in agreement.
     * 
     * Here is how the recovery mechanism works:
     * 
     * Pre: client and server agree on an ID for every statement. Any single
     *      statement is uniquely identifiable via the (server-token, int-id)
     *      pair. When this method is called we already know that when we first
     *      tried to execute the statement we had an out-dated server-token in
     *      record. Thus, we need to refresh the local cache with updated
     *      statement IDs.
     * 
     * Getting the local cache back in sync can be done by:
     * 1. Removing the old values from the current statement cache and 
     * 2. Re-registering the underlying category and re-preparing the statement
     *  
     * The above two steps will be done once per statement descriptor. This will
     * update the statement cache accordingly. However, since there may be other
     * statements in the local queue waiting to be executed. Those pending
     * statements still have old statement IDs in record. This is where the
     * transition cache comes into play. There is no need to re-register categories
     * and re-prepare statements for the same descriptor. It was already done
     * once and the main statement cache updated accordingly. The transition cache is then used
     * to get the descriptor from an old statement ID. I.e. whenever the transition
     * cache is used it is no longer equal to the main statement cache. In a way
     * the transition cache is a tool to get a descriptor for an now out-dated
     * statement ID. Once we have the descriptor again we can look it up in the
     * regular statement cache in (which has been updated previously) in order
     * to get the updated values for the statement ID.
     *  
     * @param origStmt The original statement that failed to execute.
     * @return A fixed-up statement which should succeed to execute if tried
     *         again.
     * @throws DescriptorParsingException If re-preparing a statement failed.
     */
    synchronized <T extends Pojo> WebPreparedStatement<T> handlePreparedStmtStateOutOfSync(final WebPreparedStatement<T> origStmt) throws DescriptorParsingException {
        SharedStateId id = origStmt.getStatementId();
        String msg = "Prepared statement failed to execute. Server changed token. Trying to recover stmt with id: " + id;
        logger.log(Level.FINE, msg);
        // Transition stmt cache needs to be created in 2 cases:
        // 1. It might be null if it was the first time the server
        //    re-deployed.
        // 2. The server did re-deploy at least once and the time it happened
        //    is more than TRANSITION_CACHE_OFFSET in the past. Case for
        //    multiple re-deployments of the server parts.
        if (transitionStmtCache == null || transitionStmtCache.isExpired()) {
            // Create a transition cache which expires soon in the future
            // in order to allow successful executions of queued statements which
            // did not yet run and have old statement IDs in record.
            logger.log(Level.FINE, "Re-creating transition cache");
            WebPreparedStatementCache cacheSnapshot = stmtCache.createSnapshot();
            long timeExpires = System.nanoTime() + TRANSITION_CACHE_OFFSET;
            transitionStmtCache = new ExpirableWebPreparedStatementCache(cacheSnapshot, timeExpires);
        }
        StatementDescriptor<T> desc = stmtCache.get(id);
        // If the above returned null we most likely tried to execute a statement
        // with an old server token. Attempt to use the transition cache in order
        // to still be able to execute it successfully.
        if (desc == null) {
            desc = transitionStmtCache.get(id);
            if (desc == null) {
                throw new IllegalStateException("Irrecoverable error. GC happened or transition cache expired.");
            }
            WebPreparedStatementHolder transCacheHolder = transitionStmtCache.get(desc);
            WebPreparedStatementHolder cacheHolder = stmtCache.get(desc);
            if (transCacheHolder.getStatementId().equals(cacheHolder.getStatementId())) {
                throw new IllegalStateException("Should not happen!");
            }
            // Transition case:
            //
            // Fetch the new mapping from the stmt cache since the statement id
            // must have changed but category-registration and preparing the
            // updated statement was done already.
            SharedStateId stmtId = cacheHolder.getStatementId();
            logger.log(Level.FINE, "Returning fixed-up statement using updated statement id: " + stmtId);
            origStmt.setStatementId(stmtId);
            return origStmt;
        }
        // Base case: re-register category and re-prepare statement. This will
        //            be done *once* for every statement.
        logger.log(Level.FINE, "Re-register category + prepareStatement + setting params: " + desc);
        sendCategoryReRegistrationRequest(desc.getCategory());
        stmtCache.remove(id);
        // prepareStatement() will return a raw statement (no parameters will be
        // set in this new datastructure). In order to make it executable right
        // away we need to set the params via the params we have in record in the
        // original stmt.
        WebPreparedStatement<T> newStmt = (WebPreparedStatement<T>)prepareStatement(desc);
        newStmt.setParams(origStmt.getParams());
        return newStmt;
    }

    @Override
    public <T extends Pojo> PreparedStatement<T> prepareStatement(StatementDescriptor<T> desc)
            throws DescriptorParsingException {
        /*
         * Avoid two network round-trips for statements which have already
         * been prepared. Note that this makes preparing statements not entirely
         * stateless, since the prepared statement ID might change if the
         * web endpoint reloads. If those IDs get out-of-sync we do our best
         * to correct this situation by clearing the relevant cache entry and
         * preparing the statement again.
         */
        WebPreparedStatementHolder holder = stmtCache.get(desc);
        // note this is a WeakHashMap-backed cache and may return null
        if (holder == null) {
            // Cache-miss, send request over the wire and cache result.
            holder = sendPrepareStmtRequest(desc, 0);
            stmtCache.put(desc, holder);
        }
        return new WebPreparedStatementImpl<>(holder.getTypeToken(), holder.getNumParams(), holder.getStatementId());
    }
    
    // package-private for testing
    <T extends Pojo> WebPreparedStatementHolder sendPrepareStmtRequest(StatementDescriptor<T> desc, final int invokationCount)
            throws DescriptorParsingException {
        if (invokationCount > 1) {
            // Initial invokation == 0, potential recovery-invocation == 1
            String msg = "Failed to recover from out-of-sync state with server";
            logger.log(Level.WARNING, msg);
            throw new DescriptorParsingException(msg);
        }
        String strDesc = desc.getDescriptor();
        SharedStateId categoryId = getCategoryId(desc.getCategory());
        NameValuePair nameParam = new BasicNameValuePair("query-descriptor",
                strDesc);
        NameValuePair categoryParam = new BasicNameValuePair("category-id",
                gson.toJson(categoryId, SharedStateId.class));
        List<NameValuePair> formparams = Arrays
                .asList(nameParam, categoryParam);
        try (CloseableHttpEntity entity = post(endpoint + "/prepare-statement",
                formparams)) {
            Reader reader = getContentAsReader(entity);
            WebPreparedStatementResponse result = gson.fromJson(reader, WebPreparedStatementResponse.class);
            int numParams = result.getNumFreeVariables();
            SharedStateId statementId = result.getStatementId();
            int stmtId = statementId.getId();
            switch (stmtId) {
                case WebPreparedStatementResponse.ILLEGAL_STATEMENT: {
                    // we've got a descriptor the endpoint doesn't know about or
                    // refuses to accept for security reasons.
                    String msg = "Unknown query descriptor which endpoint of " + WebStorage.class.getName() + " refused to accept!";
                    throw new IllegalDescriptorException(msg, desc.getDescriptor());
                }
                case WebPreparedStatementResponse.DESCRIPTOR_PARSE_FAILED: {
                    String msg = "Statement descriptor failed to parse. " +
                            "Please check server logs for details!";
                    throw new DescriptorParsingException(msg);
                }
                case WebPreparedStatementResponse.CATEGORY_OUT_OF_SYNC: {
                    // We tried to prepare a statement and the server's
                    // representation of category IDs changed. Thus, be sure to
                    // clear the category state and get their new IDs.
                    String msg = "Preparing statement failed. Server changed category state. Clearing category ID for statement: " +
                                    desc.getDescriptor() + " and trying to recover.";
                    logger.log(Level.FINE, msg);
                    sendCategoryReRegistrationRequest(desc.getCategory());
                    return sendPrepareStmtRequest(desc, invokationCount + 1);
                }
                default: {
                    // Common case where stmtId is the actual ID of the statement
                    // and not an error code.
                    assert(stmtId >= 0); // negative values are error codes
                    // We need this ugly trick in order for WebQueryResponse
                    // deserialization to work properly. I.e. GSON needs this type
                    // info hint.
                    Class<T> dataClass = desc.getCategory().getDataClass();
                    Type typeToken = new WebQueryResponse<T>().getRuntimeParametrizedType(dataClass);
                    return new WebPreparedStatementHolder(typeToken, numParams, statementId);
                }
            }
        }
    }
    
    // package private for testing
    synchronized <T extends Pojo> void sendCategoryReRegistrationRequest(Category<T> category) {
        // There are two possible cases. Category is an aggregate category or
        // it is not. For aggregate categories we need to re-register the
        // original first and then the aggregate category.
        Class<T> dataClass = category.getDataClass();
        if (AggregateResult.class.isAssignableFrom(dataClass)) {
            Category<?> nonAggregateCategory = Categories.getByName(category.getName());
            categoryIds.remove(nonAggregateCategory);
            registerCategory(nonAggregateCategory);
        }
        categoryIds.remove(category);
        registerCategory(category);
    }


}

