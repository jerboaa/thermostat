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

package com.redhat.thermostat.web.client.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.common.ssl.SSLContextFactory;
import com.redhat.thermostat.common.ssl.SslInitException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.config.AuthenticationConfiguration;
import com.redhat.thermostat.storage.config.StartupConfiguration;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DataModifyingStatement;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.IllegalDescriptorException;
import com.redhat.thermostat.storage.core.IllegalPatchException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.SecureStorage;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.Operator;
import com.redhat.thermostat.web.common.ExpressionSerializer;
import com.redhat.thermostat.web.common.OperatorSerializer;
import com.redhat.thermostat.web.common.PreparedParameterSerializer;
import com.redhat.thermostat.web.common.ThermostatGSONConverter;
import com.redhat.thermostat.web.common.WebAdd;
import com.redhat.thermostat.web.common.WebPreparedStatement;
import com.redhat.thermostat.web.common.WebPreparedStatementResponse;
import com.redhat.thermostat.web.common.WebPreparedStatementSerializer;
import com.redhat.thermostat.web.common.WebQueryResponse;
import com.redhat.thermostat.web.common.WebQueryResponseSerializer;
import com.redhat.thermostat.web.common.WebRemove;
import com.redhat.thermostat.web.common.WebReplace;
import com.redhat.thermostat.web.common.WebUpdate;

public class WebStorage implements Storage, SecureStorage {

    private static final String HTTPS_PREFIX = "https";
    final Logger logger = LoggingUtils.getLogger(WebStorage.class);

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
            fireChanged(ConnectionStatus.DISCONNECTED);
        }

        @Override
        public void connect() {
            try {
                initAuthentication(httpClient);
                ping();
                connected = true;
                logger.fine("Connected to storage");
                fireChanged(ConnectionStatus.CONNECTED);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not connect to storage!", ex);
                fireChanged(ConnectionStatus.FAILED_TO_CONNECT);
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

    private class WebAddImpl<T extends Pojo> extends WebAdd<T> {

        private WebAddImpl(int categoryId) {
            super(categoryId);
        }
        
        @Override
        public int apply() {
            return addImpl(this);
        }
        
    }

    private class WebReplaceImpl<T extends Pojo> extends WebReplace<T> {
        
        private WebReplaceImpl(int categoryId) {
            super(categoryId);
        }
        
        @Override
        public int apply() {
            return replaceImpl(this);
        }
        
    }

    private class WebUpdateImpl<T extends Pojo> extends WebUpdate<T> {
    
        @Override
        public int apply() {
            return updatePojo(this);
        }
    }
    
    private class WebRemoveImpl<T extends Pojo> extends WebRemove<T> {
        
        private WebRemoveImpl(int categoryId) {
            super(categoryId);
        }
        
        @Override
        public int apply() {
            return removePojo(this);
        }
        
    }
    
    private class WebPreparedStatementImpl<T extends Pojo> extends WebPreparedStatement<T> {

        // The type of the query result objects we'd get back upon
        // statement execution
        private final transient Type parametrizedTypeToken;
        
        public WebPreparedStatementImpl(Type parametrizedTypeToken, int numParams, int statementId) {
            super(numParams, statementId);
            this.parametrizedTypeToken = parametrizedTypeToken;
        }
        
        @Override
        public int execute() throws StatementExecutionException {
            throw new IllegalStateException("Not yet implemented!");
        }

        @Override
        public Cursor<T> executeQuery()
                throws StatementExecutionException {
            return doExecuteQuery(this, parametrizedTypeToken);
        }
        
    }

    private String endpoint;

    private Map<Category<?>, Integer> categoryIds;
    private Gson gson;
    // package private for testing
    DefaultHttpClient httpClient;
    private String username;
    private String password;
    private SecureRandom random;
    private WebConnection conn;
    
    // for testing
    WebStorage(StartupConfiguration config, DefaultHttpClient client, ClientConnectionManager connManager) {
        init(config, client, connManager);
    }

    public WebStorage(StartupConfiguration config) throws StorageException {
        ClientConnectionManager connManager = new ThreadSafeClientConnManager();
        DefaultHttpClient client = new DefaultHttpClient(connManager);
        init(config, client, connManager);
    }
    
    private void init(StartupConfiguration config, DefaultHttpClient client, ClientConnectionManager connManager) {
        categoryIds = new HashMap<>();
        gson = new GsonBuilder().registerTypeHierarchyAdapter(Pojo.class,
                        new ThermostatGSONConverter())
                .registerTypeHierarchyAdapter(Expression.class,
                        new ExpressionSerializer())
                .registerTypeHierarchyAdapter(Operator.class, new OperatorSerializer())
                .registerTypeAdapter(WebPreparedStatement.class, new WebPreparedStatementSerializer())
                .registerTypeAdapter(WebQueryResponse.class, new WebQueryResponseSerializer<>())
                .registerTypeAdapter(PreparedParameter.class, new PreparedParameterSerializer())
                .create();
        httpClient = client;
        random = new SecureRandom();
        conn = new WebConnection();
        
        setEndpoint(config.getDBConnectionString());
        if (config instanceof AuthenticationConfiguration) {
            AuthenticationConfiguration authConfig = (AuthenticationConfiguration) config;
            setAuthConfig(authConfig.getUsername(), authConfig.getPassword());
        }
        // setup SSL if necessary
        if (config.getDBConnectionString().startsWith(HTTPS_PREFIX)) {
            registerSSLScheme(connManager);
        }
    }

    private void registerSSLScheme(ClientConnectionManager conManager)
            throws StorageException {
        try {
            SSLContext sc = SSLContextFactory.getClientContext();
            SSLSocketFactory socketFactory = new SSLSocketFactory(sc);
            Scheme sch = new Scheme("https", 443, socketFactory);
            conManager.getSchemeRegistry().register(sch);
        } catch ( SslInitException e) {
            throw new StorageException(e);
        }
    }

    private void initAuthentication(DefaultHttpClient client)
            throws MalformedURLException {
        if (username != null && password != null) {
            URL endpointURL = new URL(endpoint);
            // TODO: Maybe also limit to realm like 'Thermostat Realm' or such?
            AuthScope scope = new AuthScope(endpointURL.getHost(),
                    endpointURL.getPort());
            Credentials creds = new UsernamePasswordCredentials(username,
                    password);
            client.getCredentialsProvider().setCredentials(scope, creds);
        }
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
            return postImpl(url, entity);
        } catch (IOException ex) {
            throw new StorageException(ex);
        }
    }

    private CloseableHttpEntity postImpl(String url, HttpEntity entity)
            throws IOException {
        HttpPost httpPost = new HttpPost(url);
        if (entity != null) {
            httpPost.setEntity(entity);
        }
        HttpResponse response = httpClient.execute(httpPost);
        StatusLine status = response.getStatusLine();
        int responseCode = status.getStatusCode();
        switch (responseCode) {
        case (HttpServletResponse.SC_NO_CONTENT):
            // Let calling code handle SC_NO_CONTENT
            break;
        case (HttpServletResponse.SC_OK):
            // Let calling code handle SC_OK
            break;
        default:
            throw new IOException("Server returned status: " + status);
        }

        return new CloseableHttpEntity(response.getEntity(), responseCode);
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
            Integer id = gson.fromJson(reader, Integer.class);
            categoryIds.put(category, id);
        }
    }

    @Override
    public <T extends Pojo> Remove<T> createRemove(Category<T> category) {
        return new WebRemoveImpl<>(categoryIds.get(category));
    }

    @Override
    public <T extends Pojo> Update<T> createUpdate(Category<T> category) {
        WebUpdateImpl<T> updateImpl = new WebUpdateImpl<>();
        updateImpl.setCategoryId(categoryIds.get(category));
        return updateImpl;
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
     * @return A cursor for the generic type.
     * @throws StatementExecutionException
     *             If execution of the statement failed.
     */
    private <T extends Pojo> Cursor<T> doExecuteQuery(WebPreparedStatement<T> stmt, Type parametrizedTypeToken) throws StatementExecutionException {
        NameValuePair queryParam = new BasicNameValuePair("prepared-stmt", gson.toJson(stmt, WebPreparedStatement.class));
        List<NameValuePair> formparams = Arrays.asList(queryParam);
        WebQueryResponse<T> qResp = null;
        try (CloseableHttpEntity entity = post(endpoint + "/query-execute", formparams)) {
            Reader reader = getContentAsReader(entity);
            qResp = gson.fromJson(reader, parametrizedTypeToken);
        } catch (Exception e) {
            throw new StatementExecutionException(e);
        }
        if (qResp.getResponseCode() == WebQueryResponse.SUCCESS) {
            T[] result = qResp.getResultList();
            return new WebCursor<T>(result);
        } else if (qResp.getResponseCode() == WebQueryResponse.ILLEGAL_PATCH) {
            String msg = "Illegal statement argument. See server logs for details.";
            IllegalArgumentException iae = new IllegalArgumentException(msg);
            IllegalPatchException e = new IllegalPatchException(iae);
            throw new StatementExecutionException(e);
        } else {
            // We only handle success responses and illegal patches, like
            // we do for other storages. This is just a defensive measure in
            // order to fail early in case something unexpected comes back.
            String msg = "Unknown response from storage endpoint!";
            IllegalStateException ise = new IllegalStateException(msg);
            throw new StatementExecutionException(ise);
        }
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
        if (entity.getResponseCode() == HttpServletResponse.SC_NO_CONTENT) {
            return null;
        }
        return new WebDataStream(entity);
    }

    @Override
    public void saveFile(String name, InputStream in) throws StorageException {
        InputStreamBody body = new InputStreamBody(in, name);
        MultipartEntity mpEntity = new MultipartEntity();
        mpEntity.addPart("file", body);
        // See IcedTea bug #1314. For safe-file we need to do this. However,
        // doing this for other actions messes up authentication when using
        // jetty (and possibly others). Hence, do this expect-continue thingy
        // only for save-file.
        httpClient.getParams().setParameter("http.protocol.expect-continue", Boolean.TRUE);
        try {
            post(endpoint + "/save-file", mpEntity).close();
        } finally {
            // FIXME: Not sure if we need this :/
            httpClient.getParams().removeParameter("http.protocol.expect-continue");
        }
    }

    @Override
    public void purge(String agentId) throws StorageException {
        NameValuePair agentIdParam = new BasicNameValuePair("agentId", agentId);
        List<NameValuePair> agentIdParams = Arrays.asList(agentIdParam);
        post(endpoint + "/purge", agentIdParams).close();
    }

    @Override
    public <T extends Pojo> Add<T> createAdd(Category<T> into) {
        int categoryId = getCategoryId(into);
        WebAdd<T> add = new WebAddImpl<>(categoryId);
        return add;
    }

    @Override
    public <T extends Pojo> Replace<T> createReplace(Category<T> into) {
        int categoryId = getCategoryId(into);
        WebReplace<T> replace = new WebReplaceImpl<>(categoryId);
        return replace;
    }
    
    private int addImpl(final WebAdd<?> add) throws StorageException {
        Pojo pojo = add.getPojo();
        checkAgentIdIsSet(pojo);
        NameValuePair pojoParam = new BasicNameValuePair("pojo",
                gson.toJson(pojo));
        NameValuePair addParam = new BasicNameValuePair("add",
                gson.toJson(add));
        List<NameValuePair> formParams = Arrays.asList(addParam, pojoParam);
        post(endpoint + "/add-pojo", formParams).close();
        return DataModifyingStatement.DEFAULT_STATUS_SUCCESS;
    }

    private int replaceImpl(final WebReplace<?> replace) throws StorageException {
        Pojo pojo = replace.getPojo();
        checkAgentIdIsSet(pojo);
        NameValuePair replaceParam = new BasicNameValuePair("replace",
                gson.toJson(replace));
        NameValuePair pojoParam = new BasicNameValuePair("pojo",
                gson.toJson(pojo));
        List<NameValuePair> formParams = Arrays.asList(replaceParam, pojoParam);
        post(endpoint + "/replace-pojo", formParams).close();
        return DataModifyingStatement.DEFAULT_STATUS_SUCCESS;
    }

    private void checkAgentIdIsSet(final Pojo pojo) throws AssertionError {
        try {
            if (BeanUtils.getProperty(pojo, Key.AGENT_ID.getName()) == null) {
                throw new AssertionError("agentId must be set!");
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new AssertionError("Pojo needs to have an agentId property");
        }
    }

    private int removePojo(Remove<?> remove) throws StorageException {
        NameValuePair removeParam = new BasicNameValuePair("remove",
                gson.toJson(remove));
        List<NameValuePair> formparams = Arrays.asList(removeParam);
        post(endpoint + "/remove-pojo", formparams).close();
        return DataModifyingStatement.DEFAULT_STATUS_SUCCESS;
    }

    private int updatePojo(Update<?> update) throws StorageException {
        WebUpdate<?> webUp = (WebUpdate<?>) update;
        List<WebUpdate.UpdateValue> updateValues = webUp.getUpdates();
        List<Object> values = new ArrayList<>(updateValues.size());
        for (WebUpdate.UpdateValue updateValue : updateValues) {
            values.add(updateValue.getValue());
        }

        NameValuePair updateParam = new BasicNameValuePair("update",
                gson.toJson(update));
        NameValuePair valuesParam = new BasicNameValuePair("values",
                gson.toJson(values));
        List<NameValuePair> formparams = Arrays
                .asList(updateParam, valuesParam);
        post(endpoint + "/update-pojo", formparams).close();
        return DataModifyingStatement.DEFAULT_STATUS_SUCCESS;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setAuthConfig(String username, String password) {
        this.username = username;
        this.password = password;
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
            response = httpClient.execute(httpPost);
            StatusLine status = response.getStatusLine();
            return status.getStatusCode() == 200;
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

    int getCategoryId(Category<?> category) {
        return categoryIds.get(category);
    }

    @Override
    public <T extends Pojo> PreparedStatement<T> prepareStatement(StatementDescriptor<T> desc)
            throws DescriptorParsingException {
        String strDesc = desc.getDescriptor();
        int categoryId = getCategoryId(desc.getCategory());
        NameValuePair nameParam = new BasicNameValuePair("query-descriptor",
                strDesc);
        NameValuePair categoryParam = new BasicNameValuePair("category-id",
                gson.toJson(categoryId, Integer.class));
        List<NameValuePair> formparams = Arrays
                .asList(nameParam, categoryParam);
        try (CloseableHttpEntity entity = post(endpoint + "/prepare-statement",
                formparams)) {
            Reader reader = getContentAsReader(entity);
            WebPreparedStatementResponse result = gson.fromJson(reader, WebPreparedStatementResponse.class);
            int numParams = result.getNumFreeVariables();
            int statementId = result.getStatementId();
            if (statementId == WebPreparedStatementResponse.ILLEGAL_STATEMENT) {
                // we've got a descriptor the endpoint doesn't know about or
                // refuses to accept for security reasons.
                String msg = "Unknown query descriptor which endpoint of " + WebStorage.class.getName() + " refused to accept!";
                throw new IllegalDescriptorException(msg, desc.getDescriptor());
            } else if (statementId == WebPreparedStatementResponse.DESCRIPTOR_PARSE_FAILED) {
                String msg = "Statement descriptor failed to parse. " +
                             "Please check server logs for details!";
                throw new DescriptorParsingException(msg);
            } else {
                // We need this ugly trick in order for WebQueryResponse
                // deserialization to work properly. I.e. GSON needs this type
                // info hint.
                Class<T> dataClass = desc.getCategory().getDataClass();
                Type typeToken = new WebQueryResponse<T>().getRuntimeParametrizedType(dataClass);
                return new WebPreparedStatementImpl<T>(typeToken, numParams, statementId);
            }
        }
    }

}

