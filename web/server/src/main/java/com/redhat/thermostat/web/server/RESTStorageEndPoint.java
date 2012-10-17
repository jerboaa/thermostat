package com.redhat.thermostat.web.server;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.redhat.thermostat.common.model.Pojo;
import com.redhat.thermostat.common.storage.Categories;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.web.common.Qualifier;
import com.redhat.thermostat.web.common.RESTQuery;
import com.redhat.thermostat.web.common.StorageWrapper;
import com.redhat.thermostat.web.common.WebInsert;

@SuppressWarnings("serial")
public class RESTStorageEndPoint extends HttpServlet {

    private Storage storage;
    private Gson gson;

    private int currentCategoryId;

    private Map<String, Integer> categoryIds;
    private Map<Integer, Category> categories;

    public void init() {
        gson = new Gson();
        categoryIds = new HashMap<>();
        categories = new HashMap<>();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (storage == null) {
            storage = StorageWrapper.getStorage();
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
        try {
            String insertParam = req.getParameter("insert");
            WebInsert insert = gson.fromJson(insertParam, WebInsert.class);
            Class<? extends Pojo> pojoCls = (Class<? extends Pojo>) Class.forName(insert.getPojoClass());
            String pojoParam = req.getParameter("pojo");
            Pojo pojo = gson.fromJson(pojoParam, pojoCls);
            int categoryId = insert.getCategoryId();
            Category category = getCategoryFromId(categoryId);
            storage.putPojo(category, insert.isReplace(), pojo);
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (ClassNotFoundException ex) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void findPojo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Reader in = req.getReader();
            RESTQuery query = gson.fromJson(in, RESTQuery.class);
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
            Reader in = req.getReader();
            RESTQuery query = gson.fromJson(in, RESTQuery.class);
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

    private Query constructTargetQuery(RESTQuery query) {
        Query targetQuery = storage.createQuery();
        int categoryId = query.getCategoryId();
        Category category = getCategoryFromId(categoryId);
        targetQuery = targetQuery.from(category);
        List<Qualifier<?>> qualifiers = query.getQualifiers();
        for (Qualifier q : qualifiers) {
            targetQuery = targetQuery.where(q.getKey(), q.getCriteria(), q.getValue());
        }
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

}
