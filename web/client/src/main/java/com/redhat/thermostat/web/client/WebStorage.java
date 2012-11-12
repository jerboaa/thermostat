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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.common.model.AgentIdPojo;
import com.redhat.thermostat.common.model.Pojo;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Connection;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Remove;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.storage.Update;
import com.redhat.thermostat.web.common.WebQuery;
import com.redhat.thermostat.web.common.ThermostatGSONConverter;
import com.redhat.thermostat.web.common.WebInsert;
import com.redhat.thermostat.web.common.WebRemove;
import com.redhat.thermostat.web.common.WebUpdate;

public class WebStorage extends Storage {

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
                ping();
                connected = true;
                fireChanged(ConnectionStatus.CONNECTED);
            } catch (IOException ex) {
                fireChanged(ConnectionStatus.FAILED_TO_CONNECT);
            }
        }
        @Override
        public String getUrl() {
            return endpoint;
        }
    }

    private String endpoint;
    private UUID agentId;

    private Map<Category, Integer> categoryIds;
    private Gson gson;

    public WebStorage() {
        categoryIds = new HashMap<>();
        gson = new GsonBuilder().registerTypeHierarchyAdapter(Pojo.class, new ThermostatGSONConverter()).create();
    }

    private void ping() throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(endpoint + "/ping");
        HttpResponse response = httpClient.execute(httpPost);
        StatusLine status = response.getStatusLine();
        if (status.getStatusCode() != 200) {
            throw new IOException("Server returned status: " + status);
        }
    }

    @Override
    public void registerCategory(Category category) {
        try {
            URL url = new URL(endpoint + "/register-category");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String enc = "UTF-8";
            conn.setRequestProperty("Content-Encoding", enc);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            OutputStream out = conn.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.write("name=");
            writer.write(URLEncoder.encode(category.getName(), enc));
            writer.write("&category=");
            writer.write(URLEncoder.encode(gson.toJson(category), enc));
            writer.flush();

            InputStream in = conn.getInputStream();
            Reader reader = new InputStreamReader(in);
            Integer id = gson.fromJson(reader, Integer.class);
            categoryIds.put(category, id);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
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

    @Override
    public <T extends Pojo> Cursor<T> findAllPojos(Query query, Class<T> resultClass) {
        try {
            URL url = new URL(endpoint + "/find-all");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStream out = conn.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            ((WebQuery) query).setResultClassName(resultClass.getName());
            gson.toJson(query, writer);
            writer.flush();

            InputStream in = conn.getInputStream();
            T[] result = (T[]) gson.fromJson(new InputStreamReader(in), Array.newInstance(resultClass, 0).getClass());
            return new WebCursor<T>(result);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public <T extends Pojo> T findPojo(Query query, Class<T> resultClass) {
        try {
            ((WebQuery) query).setResultClassName(resultClass.getName());
            URL url = new URL(endpoint + "/find-pojo");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStream out = conn.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            gson.toJson(query, writer);
            writer.flush();

            InputStream in = conn.getInputStream();
            T result = gson.fromJson(new InputStreamReader(in), resultClass);
            return result;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
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
    public long getCount(Category category) {
        try {
            URL url = new URL(endpoint + "/get-count");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStream out = conn.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.write("category=");
            gson.toJson(categoryIds.get(category), writer);
            writer.write("\n");
            writer.flush();

            InputStream in = conn.getInputStream();
            long result = gson.fromJson(new InputStreamReader(in), Long.class);
            return result;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public InputStream loadFile(String name) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(endpoint + "/load-file");
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("file", name));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);
            return response.getEntity().getContent();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void purge() {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(endpoint + "/purge");
            HttpResponse response = httpClient.execute(httpPost);
            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                throw new IOException("Server returned status: " + status);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void putPojo(Category category, boolean replace, AgentIdPojo pojo) {
        // TODO: This logic should probably be moved elsewhere. I.e. out of the Storage API.
        if (pojo.getAgentId() == null) {
            pojo.setAgentId(getAgentId());
        }
        try {
            int categoryId = categoryIds.get(category);
            WebInsert insert = new WebInsert(categoryId, replace, pojo.getClass().getName());
            URL url = new URL(endpoint + "/put-pojo");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            OutputStream out = conn.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.write("insert=");
            writer.write(URLEncoder.encode(gson.toJson(insert), "UTF-8"));
            writer.write("&pojo=");
            writer.write(URLEncoder.encode(gson.toJson(pojo), "UTF-8"));
            writer.flush();
            checkResponseCode(conn);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void checkResponseCode(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Web server returned HTTP code: " + responseCode);
        }
    }

    @Override
    public void removePojo(Remove remove) {
        try {
            URL url = new URL(endpoint + "/remove-pojo");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStream out = conn.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.write("remove=");
            writer.write(URLEncoder.encode(gson.toJson(remove), "UTF-8"));
            writer.flush();
            checkResponseCode(conn);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void saveFile(String name, InputStream in) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(endpoint + "/save-file");
            InputStreamBody body = new InputStreamBody(in, name);
            MultipartEntity entity = new MultipartEntity();
            entity.addPart("file", body);
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != 200) {
                throw new IOException("Server returned status: " + status);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    @Override
    public void updatePojo(Update update) {
        WebUpdate webUp = (WebUpdate) update;
        try {
            URL url = new URL(endpoint + "/update-pojo");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            OutputStream out = conn.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.write("update=");
            writer.write(URLEncoder.encode(gson.toJson(webUp), "UTF-8"));
            List<WebUpdate.UpdateValue> updateValues = webUp.getUpdates();
            List<Object> values = new ArrayList<>(updateValues.size());
            for (WebUpdate.UpdateValue updateValue : updateValues) {
                values.add(updateValue.getValue());
            }
            writer.write("&values=");
            writer.write(URLEncoder.encode(gson.toJson(values), "UTF-8"));
            writer.flush();
            checkResponseCode(conn);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

}
