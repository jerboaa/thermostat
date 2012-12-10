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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.config.StartupConfiguration;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.SecureStorage;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.model.AgentIdPojo;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.web.common.ThermostatGSONConverter;
import com.redhat.thermostat.web.common.WebInsert;
import com.redhat.thermostat.web.common.WebQuery;
import com.redhat.thermostat.web.common.WebRemove;
import com.redhat.thermostat.web.common.WebUpdate;

public class WebStorage implements Storage, SecureStorage {

    private static final String HTTPS_PREFIX = "https";
    final Logger logger = LoggingUtils.getLogger(WebStorage.class);

    private static class CloseableHttpEntity implements Closeable, HttpEntity {

        private HttpEntity entity;

        CloseableHttpEntity(HttpEntity entity) {
            this.entity = entity;
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

    private String endpoint;
    private UUID agentId;

    private Map<Category, Integer> categoryIds;
    private Gson gson;
    // package private for testing
    DefaultHttpClient httpClient;
    private String username;
    private String password;
    private SecureRandom random;

    public WebStorage(StartupConfiguration config) throws StorageException {
        categoryIds = new HashMap<>();
        gson = new GsonBuilder().registerTypeHierarchyAdapter(Pojo.class,
                new ThermostatGSONConverter()).create();
        ClientConnectionManager connManager = new ThreadSafeClientConnManager();
        DefaultHttpClient client = new DefaultHttpClient(connManager);
        httpClient = client;
        random = new SecureRandom();

        // setup SSL if necessary
        if (config.getDBConnectionString().startsWith(HTTPS_PREFIX)) {
            registerSSLScheme(connManager);
        }
    }

    private void registerSSLScheme(ClientConnectionManager conManager)
            throws StorageException {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            X509TrustManager ourTm;
            try {
                ourTm = new CustomX509TrustManager();
            } catch (Exception e) {
                throw new StorageException(e);
            }
            TrustManager[] tms = new TrustManager[] { ourTm };
            sc.init(null, tms, new SecureRandom());
            SSLSocketFactory socketFactory = new SSLSocketFactory(sc);
            Scheme sch = new Scheme("https", 443, socketFactory);
            conManager.getSchemeRegistry().register(sch);
        } catch ( GeneralSecurityException e) {
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
        if (status.getStatusCode() != 200) {
            throw new IOException("Server returned status: " + status);
        }

        return new CloseableHttpEntity(response.getEntity());
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
    public void registerCategory(Category category) throws StorageException {
        NameValuePair nameParam = new BasicNameValuePair("name",
                category.getName());
        NameValuePair categoryParam = new BasicNameValuePair("category",
                gson.toJson(category));
        List<NameValuePair> formparams = Arrays
                .asList(nameParam, categoryParam);
        try (CloseableHttpEntity entity = post(endpoint + "/register-category",
                formparams)) {
            Reader reader = getContentAsReader(entity);
            Integer id = gson.fromJson(reader, Integer.class);
            categoryIds.put(category, id);
        }
    }

    @Override
    public Query createQuery() {
        return new WebQuery(categoryIds);
    }

    @Override
    public Remove createRemove() {
        return new WebRemove(categoryIds);
    }

    @Override
    public WebUpdate createUpdate() {
        return new WebUpdate(categoryIds);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Pojo> Cursor<T> findAllPojos(Query query,
            Class<T> resultClass) throws StorageException {
        ((WebQuery) query).setResultClassName(resultClass.getName());
        NameValuePair queryParam = new BasicNameValuePair("query",
                gson.toJson(query));
        List<NameValuePair> formparams = Arrays.asList(queryParam);
        try (CloseableHttpEntity entity = post(endpoint + "/find-all",
                formparams)) {
            Reader reader = getContentAsReader(entity);
            T[] result = (T[]) gson.fromJson(reader,
                    Array.newInstance(resultClass, 0).getClass());
            return new WebCursor<T>(result);
        }
    }

    @Override
    public <T extends Pojo> T findPojo(Query query, Class<T> resultClass)
            throws StorageException {
        ((WebQuery) query).setResultClassName(resultClass.getName());
        NameValuePair queryParam = new BasicNameValuePair("query",
                gson.toJson(query));
        List<NameValuePair> formparams = Arrays.asList(queryParam);
        try (CloseableHttpEntity entity = post(endpoint + "/find-pojo",
                formparams)) {
            Reader reader = getContentAsReader(entity);
            T result = gson.fromJson(reader, resultClass);
            return result;
        }
    }

    @Override
    public String getAgentId() {
        return agentId.toString();
    }

    @Override
    public Connection getConnection() {
        return new WebConnection();
    }

    @Override
    public long getCount(Category category) throws StorageException {
        NameValuePair categoryParam = new BasicNameValuePair("category",
                gson.toJson(categoryIds.get(category)));
        List<NameValuePair> formparams = Arrays.asList(categoryParam);
        try (CloseableHttpEntity entity = post(endpoint + "/get-count",
                formparams)) {
            Reader reader = getContentAsReader(entity);
            long result = gson.fromJson(reader, Long.class);
            return result;
        }
    }

    @Override
    public InputStream loadFile(String name) throws StorageException {
        NameValuePair fileParam = new BasicNameValuePair("file", name);
        List<NameValuePair> formparams = Arrays.asList(fileParam);
        CloseableHttpEntity entity = post(endpoint + "/load-file", formparams);
        return new WebDataStream(entity);
    }

    @Override
    public void purge() throws StorageException {
        post(endpoint + "/purge", (HttpEntity) null).close();
    }

    @Override
    public void putPojo(Category category, boolean replace, AgentIdPojo pojo)
            throws StorageException {
        // TODO: This logic should probably be moved elsewhere. I.e. out of the
        // Storage API.
        if (pojo.getAgentId() == null) {
            pojo.setAgentId(getAgentId());
        }

        int categoryId = categoryIds.get(category);
        WebInsert insert = new WebInsert(categoryId, replace, pojo.getClass()
                .getName());
        NameValuePair insertParam = new BasicNameValuePair("insert",
                gson.toJson(insert));
        NameValuePair pojoParam = new BasicNameValuePair("pojo",
                gson.toJson(pojo));
        List<NameValuePair> formparams = Arrays.asList(insertParam, pojoParam);
        post(endpoint + "/put-pojo", formparams).close();
    }

    @Override
    public void removePojo(Remove remove) throws StorageException {
        NameValuePair removeParam = new BasicNameValuePair("remove",
                gson.toJson(remove));
        List<NameValuePair> formparams = Arrays.asList(removeParam);
        post(endpoint + "/remove-pojo", formparams).close();
    }

    @Override
    public void saveFile(String name, InputStream in) throws StorageException {
        InputStreamBody body = new InputStreamBody(in, name);
        MultipartEntity mpEntity = new MultipartEntity();
        mpEntity.addPart("file", body);
        post(endpoint + "/save-file", mpEntity).close();
    }

    @Override
    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    @Override
    public void updatePojo(Update update) throws StorageException {
        WebUpdate webUp = (WebUpdate) update;
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
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setAuthConfig(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public AuthToken generateToken() throws StorageException {
        byte[] token = new byte[256];
        random.nextBytes(token);
        NameValuePair clientTokenParam = new BasicNameValuePair("client-token", Base64.encodeBase64String(token));
        List<NameValuePair> formparams = Arrays.asList(clientTokenParam);
        try (CloseableHttpEntity entity = post(endpoint + "/generate-token", formparams)) {
            byte[] authToken = EntityUtils.toByteArray(entity);
            return new AuthToken(authToken, token);
        } catch (IOException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public boolean verifyToken(AuthToken authToken) {
        byte[] clientToken = authToken.getClientToken();
        byte[] token = authToken.getToken();
        NameValuePair clientTokenParam = new BasicNameValuePair("client-token", Base64.encodeBase64String(clientToken));
        NameValuePair tokenParam = new BasicNameValuePair("token", Base64.encodeBase64String(token));
        List<NameValuePair> formparams = Arrays.asList(clientTokenParam, tokenParam);
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

}
