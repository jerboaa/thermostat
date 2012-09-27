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
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import com.google.gson.Gson;
import com.redhat.thermostat.common.model.Pojo;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Connection;
import com.redhat.thermostat.common.storage.ConnectionKey;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Remove;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.storage.Update;
import com.redhat.thermostat.web.common.RESTQuery;
import com.redhat.thermostat.web.common.WebInsert;

public class RESTStorage extends Storage {

    private String endpoint;

    @Override
    public ConnectionKey createConnectionKey(Category arg0) {
        return new ConnectionKey() {};
    }

    @Override
    public Query createQuery() {
        return new RESTQuery();
    }

    @Override
    public Remove createRemove() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Update createUpdate() {
        // TODO Auto-generated method stub
        return null;
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
            Gson gson = new Gson();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            ((RESTQuery) query).setResultClassName(resultClass.getName());
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
            ((RESTQuery) query).setResultClassName(resultClass.getName());
            URL url = new URL(endpoint + "/find-pojo");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStream out = conn.getOutputStream();
            Gson gson = new Gson();
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Connection getConnection() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getCount(Category arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public InputStream loadFile(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void purge() {
        // TODO Auto-generated method stub

    }

    @Override
    public void putPojo(Category category, boolean replace, Pojo pojo) {
        try {
            WebInsert insert = new WebInsert(category, replace, pojo.getClass().getName());
            URL url = new URL(endpoint + "/put-pojo");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            OutputStream out = conn.getOutputStream();
            Gson gson = new Gson();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.write("insert=");
            gson.toJson(insert, writer);
            writer.write("&pojo=");
            gson.toJson(pojo, writer);
            writer.flush();

            InputStream in = conn.getInputStream();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void removePojo(Remove arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void saveFile(String arg0, InputStream arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setAgentId(UUID arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updatePojo(Update arg0) {
        // TODO Auto-generated method stub

    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

}
