package com.redhat.thermostat.web.server;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.web.common.Qualifier;
import com.redhat.thermostat.web.common.RESTQuery;
import com.redhat.thermostat.web.common.StorageWrapper;

@SuppressWarnings("serial")
public class RESTStorageEndPoint extends HttpServlet {

    private Storage storage;
    private Gson gson;

    public void init() {
        storage = StorageWrapper.getStorage();
        gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uri = req.getRequestURI();
        System.err.println("request uri: " + uri);
        int lastPartIdx = uri.lastIndexOf("/");
        String cmd = uri.substring(lastPartIdx + 1);
        if (cmd.equals("find-pojo")) {
            findPojo(req, resp);
        } else if (cmd.equals("find-all")) {
            findAll(req, resp);
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
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "result class not found");
        }
    }

    private Query constructTargetQuery(RESTQuery query) {
        Query targetQuery = storage.createQuery();
        targetQuery = targetQuery.from(query.getCategory());
        List<Qualifier<?>> qualifiers = query.getQualifiers();
        for (Qualifier q : qualifiers) {
            targetQuery = targetQuery.where(q.getKey(), q.getCriteria(), q.getValue());
        }
        return targetQuery;
    }

    private void writeResponse(HttpServletResponse resp, Object result) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        gson.toJson(result, resp.getWriter());
        resp.flushBuffer();
    }

}
