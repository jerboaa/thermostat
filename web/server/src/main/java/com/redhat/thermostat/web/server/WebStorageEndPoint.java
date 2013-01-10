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

package com.redhat.thermostat.web.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.redhat.thermostat.storage.core.AbstractQuery.Sort;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Put;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.web.common.Qualifier;
import com.redhat.thermostat.web.common.StorageWrapper;
import com.redhat.thermostat.web.common.ThermostatGSONConverter;
import com.redhat.thermostat.web.common.WebInsert;
import com.redhat.thermostat.web.common.WebQuery;
import com.redhat.thermostat.web.common.WebRemove;
import com.redhat.thermostat.web.common.WebUpdate;

@SuppressWarnings("serial")
public class WebStorageEndPoint extends HttpServlet {

    private static final String TOKEN_MANAGER_TIMEOUT_PARAM = "token-manager-timeout";
    private static final String TOKEN_MANAGER_KEY = "token-manager";
    private static final String ROLE_THERMOSTAT_AGENT = "thermostat-agent";
    private static final String ROLE_THERMOSTAT_CLIENT = "thermostat-client";
    private static final String ROLE_CMD_CHANNEL = "thermostat-cmd-channel";

    private Storage storage;
    private Gson gson;

    public static final String STORAGE_ENDPOINT = "storage.endpoint";
    public static final String STORAGE_USERNAME = "storage.username";
    public static final String STORAGE_PASSWORD = "storage.password";
    public static final String STORAGE_CLASS = "storage.class";
    
    private int currentCategoryId;

    private Map<String, Integer> categoryIds;
    private Map<Integer, Category> categories;

    public void init() {
        gson = new GsonBuilder().registerTypeHierarchyAdapter(Pojo.class, new ThermostatGSONConverter()).create();
        categoryIds = new HashMap<>();
        categories = new HashMap<>();
        TokenManager tokenManager = new TokenManager();
        String timeoutParam = getInitParameter(TOKEN_MANAGER_TIMEOUT_PARAM);
        if (timeoutParam != null) {
            tokenManager.setTimeout(Integer.parseInt(timeoutParam));
        }
        getServletContext().setAttribute(TOKEN_MANAGER_KEY, tokenManager);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        if (storage == null) {
            String storageClass = getServletConfig().getInitParameter(STORAGE_CLASS);
            String storageEndpoint = getServletConfig().getInitParameter(STORAGE_ENDPOINT);
            String username = getServletConfig().getInitParameter(STORAGE_USERNAME);
            String password = getServletConfig().getInitParameter(STORAGE_PASSWORD);
            storage = StorageWrapper.getStorage(storageClass, storageEndpoint, username, password);
        }
        String uri = req.getRequestURI();
        int lastPartIdx = uri.lastIndexOf("/");
        String cmd = uri.substring(lastPartIdx + 1);
        if (cmd.equals("find-pojo")) {
            findPojo(req, resp);
        } else if (cmd.equals("find-all")) {
            findAll(req, resp);
        } else if (cmd.equals("put-pojo")) {
            putPojo(req, resp);
        } else if (cmd.equals("register-category")) {
            registerCategory(req, resp);
        } else if (cmd.equals("remove-pojo")) {
            removePojo(req, resp);
        } else if (cmd.equals("update-pojo")) {
            updatePojo(req, resp);
        } else if (cmd.equals("get-count")) {
            getCount(req, resp);
        } else if (cmd.equals("save-file")) {
            saveFile(req, resp);
        } else if (cmd.equals("load-file")) {
            loadFile(req, resp);
        } else if (cmd.equals("purge")) {
            purge(req, resp);
        } else if (cmd.equals("ping")) {
            ping(req, resp);
        } else if (cmd.equals("generate-token")) {
            generateToken(req, resp);
        } else if (cmd.equals("verify-token")) {
            verifyToken(req, resp);
        }
    }

    private void ping(HttpServletRequest req, HttpServletResponse resp) {
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void purge(HttpServletRequest req, HttpServletResponse resp) {
        storage.purge();
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void loadFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter("file");
        try (InputStream data = storage.loadFile(name)) {
            OutputStream out = resp.getOutputStream();
            byte[] buffer = new byte[512];
            int read = 0;
            while (read >= 0) {
                read = data.read(buffer);
                if (read > 0) {
                    out.write(buffer, 0, read);
                }
            }
        }
    }

    private void saveFile(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean isMultipart = ServletFileUpload.isMultipartContent(req);
        if (! isMultipart) {
            throw new ServletException("expected multipart message");
        }
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        try {
            @SuppressWarnings("unchecked")
            List<FileItem> items = upload.parseRequest(req);
            for (FileItem item : items) {
                String fieldName = item.getFieldName();
                if (fieldName.equals("file")) {
                    String name = item.getName();
                    InputStream in = item.getInputStream();
                    storage.saveFile(name, in);
                }
            }
        } catch (FileUploadException ex) {
            throw new ServletException(ex);
        }
        
    }

    private void getCount(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String categoryParam = req.getParameter("category");
            int categoryId = gson.fromJson(categoryParam, Integer.class);
            Category category = categories.get(categoryId);
            long result = storage.getCount(category);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            gson.toJson(result, resp.getWriter());
            resp.flushBuffer();
        } catch (IOException ex) {
            ex.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private synchronized void registerCategory(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String categoryName = req.getParameter("name");
        String categoryParam = req.getParameter("category");
        int id;
        if (categoryIds.containsKey(categoryName)) {
            id = categoryIds.get(categoryName);
        } else {
            // The following has the side effect of registering the newly deserialized Category in the Categories clas.
            Category category = gson.fromJson(categoryParam, Category.class);
            storage.registerCategory(category);

            id = currentCategoryId;
            categoryIds.put(categoryName, id);
            categories.put(id, category);
            currentCategoryId++;
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        Writer writer = resp.getWriter();
        gson.toJson(id, writer);
        writer.flush();
    }

    private void putPojo(HttpServletRequest req, HttpServletResponse resp) {
        if (! req.isUserInRole(ROLE_THERMOSTAT_AGENT)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            String insertParam = req.getParameter("insert");
            WebInsert insert = gson.fromJson(insertParam, WebInsert.class);
            Class<? extends Pojo> pojoCls = (Class<? extends Pojo>) Class.forName(insert.getPojoClass());
            String pojoParam = req.getParameter("pojo");
            Pojo pojo = gson.fromJson(pojoParam, pojoCls);
            int categoryId = insert.getCategoryId();
            Category category = getCategoryFromId(categoryId);
            Put targetPut = insert.isReplace() ? storage.createReplace(category) : storage.createAdd(category);
            targetPut.setPojo(pojo);
            targetPut.apply();
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (ClassNotFoundException ex) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void removePojo(HttpServletRequest req, HttpServletResponse resp) {
        String removeParam = req.getParameter("remove");
        WebRemove remove = gson.fromJson(removeParam, WebRemove.class);
        Remove targetRemove = storage.createRemove();
        targetRemove = targetRemove.from(getCategoryFromId(remove.getCategoryId()));
        List<Qualifier<?>> qualifiers = remove.getQualifiers();
        for (Qualifier qualifier : qualifiers) {
            assert (qualifier.getCriteria() == Criteria.EQUALS);
            targetRemove = targetRemove.where(qualifier.getKey(), qualifier.getValue());
        }
        storage.removePojo(targetRemove);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void updatePojo(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String updateParam = req.getParameter("update");
            WebUpdate update = gson.fromJson(updateParam, WebUpdate.class);
            Update targetUpdate = storage.createUpdate(getCategoryFromId(update.getCategoryId()));
            List<Qualifier<?>> qualifiers = update.getQualifiers();
            for (Qualifier qualifier : qualifiers) {
                assert (qualifier.getCriteria() == Criteria.EQUALS);
                targetUpdate.where(qualifier.getKey(), qualifier.getValue());
            }
            List<WebUpdate.UpdateValue> updates = update.getUpdates();
            if (updates != null) {
                String valuesParam = req.getParameter("values");
                JsonParser parser = new JsonParser();
                JsonArray jsonArray = parser.parse(valuesParam)
                        .getAsJsonArray();
                int index = 0;
                for (WebUpdate.UpdateValue updateValue : updates) {
                    Class valueClass = Class.forName(updateValue
                            .getValueClass());
                    Object value = gson.fromJson(jsonArray.get(index),
                            valueClass);
                    index++;
                    Key key = updateValue.getKey();
                    targetUpdate.set(key, value);
                }
            }
            targetUpdate.apply();
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void findPojo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String queryParam = req.getParameter("query");
            WebQuery query = gson.fromJson(queryParam, WebQuery.class);
            Class resultClass = Class.forName(query.getResultClassName());
            Query targetQuery = constructTargetQuery(query);
            Object result = storage.findPojo(targetQuery, resultClass);
            writeResponse(resp, result);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "result class not found");
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void findAll(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String queryParam = req.getParameter("query");
            WebQuery query = gson.fromJson(queryParam, WebQuery.class);
            Class resultClass = Class.forName(query.getResultClassName());
            Query targetQuery = constructTargetQuery(query);
            ArrayList resultList = new ArrayList();
            Cursor result = storage.findAllPojos(targetQuery, resultClass);
            while (result.hasNext()) {
                resultList.add(result.next());
            }
            writeResponse(resp, resultList.toArray());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "result class not found");
        }
    }

    private Query constructTargetQuery(WebQuery query) {
        Query targetQuery = storage.createQuery();
        int categoryId = query.getCategoryId();
        Category category = getCategoryFromId(categoryId);
        targetQuery = targetQuery.from(category);
        List<Qualifier<?>> qualifiers = query.getQualifiers();
        for (Qualifier q : qualifiers) {
            targetQuery = targetQuery.where(q.getKey(), q.getCriteria(), q.getValue());
        }
        for (Sort s : query.getSorts()) {
            targetQuery = targetQuery.sort(s.getKey(), s.getDirection());
        }
        targetQuery = targetQuery.limit(query.getLimit());
        return targetQuery;
    }

    private Category getCategoryFromId(int categoryId) {
        Category category = categories.get(categoryId);
        return category;
    }

    private void writeResponse(HttpServletResponse resp, Object result) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        gson.toJson(result, resp.getWriter());
        resp.flushBuffer();
    }

    private void generateToken(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (! req.isUserInRole(ROLE_CMD_CHANNEL)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        TokenManager tokenManager = (TokenManager) getServletContext().getAttribute(TOKEN_MANAGER_KEY);
        assert tokenManager != null;
        String clientToken = req.getParameter("client-token");
        byte[] token = tokenManager.generateToken(clientToken);
        resp.setContentType("application/octet-stream");
        resp.setContentLength(token.length);
        resp.getOutputStream().write(token);
    }

    private void verifyToken(HttpServletRequest req, HttpServletResponse resp) {
        if (! req.isUserInRole(ROLE_CMD_CHANNEL)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        TokenManager tokenManager = (TokenManager) getServletContext().getAttribute(TOKEN_MANAGER_KEY);
        assert tokenManager != null;
        String clientToken = req.getParameter("client-token");
        byte[] token = Base64.decodeBase64(req.getParameter("token"));
        boolean verified = tokenManager.verifyToken(clientToken, token);
        if (! verified) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }


}
