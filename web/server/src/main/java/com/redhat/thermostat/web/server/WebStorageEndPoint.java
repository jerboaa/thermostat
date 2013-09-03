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

package com.redhat.thermostat.web.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.servlet.ServletConfig;
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
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.Configuration;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Categories;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.IllegalPatchException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.ParsedStatement;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.PreparedParameters;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.core.auth.DescriptorMetadata;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorMetadataFactory;
import com.redhat.thermostat.storage.model.AggregateResult;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.BinaryLogicalExpression;
import com.redhat.thermostat.storage.query.BinaryLogicalOperator;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.Operator;
import com.redhat.thermostat.web.common.ExpressionSerializer;
import com.redhat.thermostat.web.common.OperatorSerializer;
import com.redhat.thermostat.web.common.PreparedParameterSerializer;
import com.redhat.thermostat.web.common.StorageWrapper;
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
import com.redhat.thermostat.web.server.auth.FilterResult;
import com.redhat.thermostat.web.server.auth.Roles;
import com.redhat.thermostat.web.server.auth.UserPrincipal;
import com.redhat.thermostat.web.server.auth.WebStoragePathHandler;

@SuppressWarnings("serial")
public class WebStorageEndPoint extends HttpServlet {

    static final String CMDC_AUTHORIZATION_GRANT_ROLE_PREFIX = "thermostat-cmdc-grant-";
    static final String FILES_READ_GRANT_ROLE_PREFIX = "thermostat-files-grant-read-filename-";
    static final String FILES_WRITE_GRANT_ROLE_PREFIX = "thermostat-files-grant-write-filename-";
    private static final String TOKEN_MANAGER_TIMEOUT_PARAM = "token-manager-timeout";
    private static final String TOKEN_MANAGER_KEY = "token-manager";
    private static final String JETTY_JAAS_USER_PRINCIPAL_CLASS_NAME = "org.eclipse.jetty.plus.jaas.JAASUserPrincipal";
    private static final String CATEGORY_KEY_FORMAT = "%s|%s";

    // our strings can contain non-ASCII characters. Use UTF-8
    // see also PR 1344
    private static final String RESPONSE_JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    private static final Logger logger = LoggingUtils.getLogger(WebStorageEndPoint.class);

    private Storage storage;
    private Gson gson;

    public static final String STORAGE_ENDPOINT = "storage.endpoint";
    public static final String STORAGE_USERNAME = "storage.username";
    public static final String STORAGE_PASSWORD = "storage.password";
    public static final String STORAGE_CLASS = "storage.class";
    
    private int currentCategoryId;

    private Map<String, Integer> categoryIds;
    private Map<Integer, Category<?>> categories;
    
    private Map<StatementDescriptor<?>, PreparedStatementHolder<?>> preparedStmts;
    private Map<Integer, PreparedStatementHolder<?>> preparedStatementIds;
    // Lock to be held for setting/getting prepared queries in the above maps
    private Object preparedStmtLock = new Object();
    private int currentPreparedStmtId;
    
    // read-only set of all known statement descriptors we trust and allow
    private Set<String> knownStatementDescriptors;
    // read-only map of known descriptors => descriptor metadata
    private Map<String, StatementDescriptorMetadataFactory> descMetadataFactories;
    // read-only set of all known categories which we allow to get registered.
    private Set<String> knownCategoryNames;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        logger.log(Level.INFO, "Initializing web service");
        
        // check if thermostat home is set and readable
        checkThermostatHome();
        
        gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Pojo.class,
                        new ThermostatGSONConverter())
                .registerTypeHierarchyAdapter(Expression.class,
                        new ExpressionSerializer())
                .registerTypeHierarchyAdapter(Operator.class,
                        new OperatorSerializer())
                .registerTypeAdapter(WebQueryResponse.class, new WebQueryResponseSerializer<>())
                .registerTypeAdapter(PreparedParameter.class, new PreparedParameterSerializer())
                .registerTypeAdapter(WebPreparedStatement.class, new WebPreparedStatementSerializer())
                .create();
        categoryIds = new HashMap<>();
        categories = new HashMap<>();
        preparedStatementIds = new HashMap<>();
        preparedStmts = new HashMap<>();
        TokenManager tokenManager = new TokenManager();
        String timeoutParam = getInitParameter(TOKEN_MANAGER_TIMEOUT_PARAM);
        if (timeoutParam != null) {
            tokenManager.setTimeout(Integer.parseInt(timeoutParam));
        }
        getServletContext().setAttribute(TOKEN_MANAGER_KEY, tokenManager);
        
        // Set the set of statement descriptors which we trust
        KnownDescriptorRegistry descRegistry = KnownDescriptorRegistryFactory.getInstance();
        knownStatementDescriptors = descRegistry.getRegisteredDescriptors();
        descMetadataFactories = descRegistry.getDescriptorMetadataFactories();
        // Set the set of category names which we allow to get registered
        KnownCategoryRegistry categoryRegistry = KnownCategoryRegistryFactory.getInstance();
        knownCategoryNames = categoryRegistry.getRegisteredCategoryNames();
    }
    
    @Override
    public void destroy() {
        logger.log(Level.INFO, "Going to shut down web service");
        if (storage != null) {
            // See IcedTea BZ#1315. Shut down storage in order
            // to avoid further memory leaks.
            Connection connection = storage.getConnection();
            try {
                // Tests have null connections
                if (connection != null) {
                    connection.disconnect();
                }
            } finally {
                storage.shutdown();
            }
        }
        logger.log(Level.INFO, "Web service shut down finished");
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
        if (cmd.equals("prepare-statement")) {
            prepareStatement(req, resp);
        } else if (cmd.equals("query-execute")) {
            queryExecute(req, resp);
        } else if (cmd.equals("add-pojo")) {
            addPojo(req, resp);
        } else if (cmd.equals("replace-pojo")) {
            replacePojo(req, resp);
        } else if (cmd.equals("register-category")) {
            registerCategory(req, resp);
        } else if (cmd.equals("remove-pojo")) {
            removePojo(req, resp);
        } else if (cmd.equals("update-pojo")) {
            updatePojo(req, resp);
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
    
    private boolean isThermostatHomeSet() {
        try {
            // this throws config exception if neither the property
            // nor the env var is set
            new Configuration();
            return true;
        } catch (InvalidConfigurationException e) {
            return false;
        }
    }
    
    private void checkThermostatHome() {
        if (!isThermostatHomeSet()) {
            String msg = "THERMOSTAT_HOME context parameter not set!";
            logger.log(Level.SEVERE, msg);
            throw new RuntimeException(msg);
        }
        File thermostatHomeFile = getThermostatHome();
        if (!thermostatHomeFile.canRead()) {
            // This is bad news. If we can't at least read THERMOSTAT_HOME
            // we are bound to fail in some weird ways at some later point.
            String msg = "THERMOSTAT_HOME = "
                    + thermostatHomeFile.getAbsolutePath()
                    + " is not readable or does not exist!";
            logger.log(Level.SEVERE, msg);
            throw new RuntimeException(msg);
        }
        logger.log(Level.FINEST, "THERMOSTAT_HOME == "
                + thermostatHomeFile.getAbsolutePath());
    }

    private File getThermostatHome() {
        try {
            Configuration config = new Configuration();
            return config.getSystemThermostatHome();
        } catch (InvalidConfigurationException e) {
            // we should have just checked if this throws any exception
            logger.log(Level.SEVERE, "Illegal configuration!", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @WebStoragePathHandler( path = "prepare-statement" )
    private <T extends Pojo> void prepareStatement(HttpServletRequest req,
            HttpServletResponse resp) throws IOException {
        if (! isAuthorized(req, resp, Roles.PREPARE_STATEMENT)) {
            return;
        }
        String queryDescrParam = req.getParameter("query-descriptor");
        String categoryIdParam = req.getParameter("category-id");
        Integer catId = gson.fromJson(categoryIdParam, Integer.class);
        Category<T> cat = (Category<T>)getCategoryFromId(catId);
        WebPreparedStatementResponse response = new WebPreparedStatementResponse();
        if (cat == null) {
            // bad category? we refuse to accept this
            logger.log(Level.WARNING, "Attepted to prepare a statement with an illegal category id");
            response.setStatementId(WebPreparedStatementResponse.ILLEGAL_STATEMENT);
            writeResponse(resp, response, WebPreparedStatementResponse.class);
            return;
        }
        StatementDescriptor<T> desc = new StatementDescriptor<>(cat, queryDescrParam);
        // Check if descriptor is trusted (i.e. known)
        if (!knownStatementDescriptors.contains(desc.getDescriptor())) {
            String msg = "Attempted to prepare a statement descriptor which we " +
            		"don't trust! Descriptor was: ->" + desc.getDescriptor() + "<-";
            logger.log(Level.WARNING, msg);
            response.setStatementId(WebPreparedStatementResponse.ILLEGAL_STATEMENT);
            writeResponse(resp, response, WebPreparedStatementResponse.class);
            return;
        }
        
        synchronized (preparedStmtLock) {
            // see if we've prepared this query already
            if (preparedStmts.containsKey(desc)) {
                PreparedStatementHolder<T> holder = (PreparedStatementHolder<T>) preparedStmts
                        .get(desc);
                ParsedStatement<T> parsed = holder.getStmt()
                        .getParsedStatement();
                int freeVars = parsed.getNumParams();
                response.setNumFreeVariables(freeVars);
                response.setStatementId(holder.getId());
                writeResponse(resp, response,
                        WebPreparedStatementResponse.class);
                return;
            }
            
            // Prepare the target statement and put it into our prepared statement
            // maps.
            PreparedStatement<T> targetPreparedStatement;
            try {
                targetPreparedStatement = (PreparedStatement<T>) storage
                        .prepareStatement(desc);
            } catch (DescriptorParsingException e) {
                logger.log(Level.WARNING, "Descriptor parse error!", e);
                response.setStatementId(WebPreparedStatementResponse.DESCRIPTOR_PARSE_FAILED);
                writeResponse(resp, response,
                        WebPreparedStatementResponse.class);
                return;
            }
            PreparedStatementHolder<T> holder = new PreparedStatementHolder<T>(
                    currentPreparedStmtId, targetPreparedStatement,
                    (Class<T>) cat.getDataClass(), desc);
            preparedStmts.put(desc, holder);
            preparedStatementIds.put(currentPreparedStmtId, holder);
            ParsedStatement<?> parsed = targetPreparedStatement
                    .getParsedStatement();
            response.setNumFreeVariables(parsed.getNumParams());
            response.setStatementId(currentPreparedStmtId);
            writeResponse(resp, response, WebPreparedStatementResponse.class);
            currentPreparedStmtId++;
        }
    }

    @WebStoragePathHandler( path = "ping" )
    private void ping(HttpServletRequest req, HttpServletResponse resp) {
        if (! isAuthorized(req, resp, Roles.LOGIN)) {
            return;
        }
        
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @WebStoragePathHandler( path = "purge" )
    private void purge(HttpServletRequest req, HttpServletResponse resp) {
        if (! isAuthorized(req, resp, Roles.PURGE)) {
            return;
        }
        
        String agentId = req.getParameter("agentId");
        storage.purge(agentId);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @WebStoragePathHandler( path = "load-file" )
    private void loadFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (! isAuthorized(req, resp, Roles.LOAD_FILE)) {
            return;
        }
        
        String name = req.getParameter("file");
        if (! isAllowedToLoadFile(req, resp, name)) {
            return;
        }
        try (InputStream data = storage.loadFile(name)) {
            if (data == null) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }
            OutputStream out = resp.getOutputStream();
            byte[] buffer = new byte[512];
            int read = 0;
            while (read >= 0) {
                read = data.read(buffer);
                if (read > 0) {
                    out.write(buffer, 0, read);
                }
            }
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    @WebStoragePathHandler( path = "save-file" )
    private void saveFile(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (! isAuthorized(req, resp, Roles.SAVE_FILE)) {
            return;
        }
        
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
                    if (! isAllowedToSaveFile(req, resp, name)) {
                        return;
                    }
                    InputStream in = item.getInputStream();
                    storage.saveFile(name, in);
                }
            }
        } catch (FileUploadException ex) {
            throw new ServletException(ex);
        }
        
    }

    private boolean isAllowedToLoadFile(HttpServletRequest req,
            HttpServletResponse resp, String filename) {
        String fileRole = FILES_READ_GRANT_ROLE_PREFIX + filename;
        return isAllowed(req, resp, filename, Roles.GRANT_FILES_READ_ALL, fileRole);
        
    }

    private boolean isAllowedToSaveFile(HttpServletRequest req,
            HttpServletResponse resp, String filename) {
        String fileRole = FILES_WRITE_GRANT_ROLE_PREFIX + filename;
        return isAllowed(req, resp, filename, Roles.GRANT_FILES_WRITE_ALL, fileRole);
    }

    private boolean isAllowed(HttpServletRequest req, HttpServletResponse resp,
            String filename, String grantAllRole, String specificFileRole) {
        if (req.isUserInRole(grantAllRole) || req.isUserInRole(specificFileRole)) {
            return true;
        } else {
            String detailMsg = "User '" + req.getRemoteUser() +
                    "' does not belong to any of the following roles: [ " + 
                    grantAllRole + ", " + 
                    specificFileRole + " ]";
            logger.log(Level.INFO, "Permission denied for file '" +
                    filename + "'. " + detailMsg);
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
    }

    @SuppressWarnings("unchecked") // need to adapt categories
    @WebStoragePathHandler( path = "register-category" )
    private synchronized void registerCategory(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (! isAuthorized(req, resp, Roles.REGISTER_CATEGORY)) {
            return;
        }
        
        String categoryName = req.getParameter("name");
        String dataClassName = req.getParameter("data-class");
        // We need to index into the category map using name + data class since
        // we have a different category for aggregate queries. For them the
        // category name will be the same, but the data class will be different.
        String categoryKey = String.format(CATEGORY_KEY_FORMAT, categoryName, dataClassName);
        String categoryParam = req.getParameter("category");
        int id;
        if (categoryIds.containsKey(categoryKey)) {
            id = categoryIds.get(categoryKey);
        } else {
            Class<?> dataClass = getDataClassFromName(dataClassName);
            Category<?> category = null;
            if ((AggregateResult.class.isAssignableFrom(dataClass))) {
                // Aggregate category case
                Category<?> original = Categories.getByName(categoryName);
                if (original == null) {
                    // DAOs register categories when they are constructed. If we
                    // end up triggering this we are in deep water. An aggregate
                    // query was attempted before the underlying category is
                    // registered at all? Not good!
                    throw new IllegalStateException("Original category of aggregate not registered!");
                }
                // Adapt the original category to the one we want
                @SuppressWarnings({ "rawtypes" })
                CategoryAdapter adapter = new CategoryAdapter(original);
                category = adapter.getAdapted(dataClass);
                logger.log(Level.FINEST, "(id: " + currentCategoryId + ") not registering aggregate category " + category );
            } else {
                // Regular, non-aggregate category. Those categories we actually
                // need to register with backing storage.
                //
                // Make sure we only register known categories
                if (! knownCategoryNames.contains(categoryName)) {
                    logger.log(Level.WARNING,
                        "Attempt to register category which we don't know of! Name was '"
                                + categoryName + "'");
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                // The following has the side effect of registering the newly
                // deserialized Category in the Categories class.
                category = gson.fromJson(categoryParam, Category.class);
                storage.registerCategory(category);
                logger.log(Level.FINEST, "(id: " + currentCategoryId + ") registered non-aggreate category: " + category);
            }
            id = currentCategoryId;
            categoryIds.put(categoryKey, id);
            categories.put(id, category);
            currentCategoryId++;
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(RESPONSE_JSON_CONTENT_TYPE);
        Writer writer = resp.getWriter();
        gson.toJson(id, writer);
        writer.flush();
    }

    private Class<?> getDataClassFromName(String dataClassName) {
        try {
            Class<?> clazz = Class.forName(dataClassName);
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unknown data class!");
        }
    }

    @WebStoragePathHandler( path = "add-pojo" )
    private void addPojo(HttpServletRequest req, HttpServletResponse resp) {
        if (! isAuthorized(req, resp, Roles.APPEND)) {
            return;
        }
        String addParam = req.getParameter("add");
        WebAdd<?> add = gson.fromJson(addParam, WebAdd.class);
        int categoryId = add.getCategoryId();
        Category<?> category = getCategoryFromId(categoryId);
        Add<?> targetAdd = storage.createAdd(category);
        Class<? extends Pojo> pojoCls = category.getDataClass();
        String pojoParam = req.getParameter("pojo");
        Pojo pojo = gson.fromJson(pojoParam, pojoCls);
        targetAdd.setPojo(pojo);
        targetAdd.apply();
        resp.setStatus(HttpServletResponse.SC_OK);
    }
    
    @WebStoragePathHandler( path = "replace-pojo" )
    private void replacePojo(HttpServletRequest req, HttpServletResponse resp) {
        if (! isAuthorized(req, resp, Roles.REPLACE)) {
            return;
        }
        String replaceParam = req.getParameter("replace");
        WebReplace<?> replace = gson.fromJson(replaceParam, WebReplace.class);
        int categoryId = replace.getCategoryId();
        Category<?> category = getCategoryFromId(categoryId);
        Replace<?> targetReplace = storage.createReplace(category);
        Class<? extends Pojo> pojoCls = category.getDataClass();
        String pojoParam = req.getParameter("pojo");
        Pojo pojo = gson.fromJson(pojoParam, pojoCls);
        targetReplace.setPojo(pojo);
        Expression expr = replace.getWhereExpression();
        targetReplace.where(expr);
        targetReplace.apply();
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @WebStoragePathHandler( path = "remove-pojo" )
    private void removePojo(HttpServletRequest req, HttpServletResponse resp) {
        if (! isAuthorized(req, resp, Roles.DELETE)) {
            return;
        }
        
        String removeParam = req.getParameter("remove");
        WebRemove<?> remove = gson.fromJson(removeParam, WebRemove.class);
        Category<?> targetCategory = getCategoryFromId(remove.getCategoryId());
        Remove<?> targetRemove = storage.createRemove(targetCategory);
        Expression expr = remove.getWhereExpression();
        if (expr != null) {
            targetRemove.where(expr);
        }
        targetRemove.apply();
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @WebStoragePathHandler( path = "update-pojo" )
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void updatePojo(HttpServletRequest req, HttpServletResponse resp) {
        if (! isAuthorized(req, resp, Roles.UPDATE)) {
            return;
        }
        
        try {
            String updateParam = req.getParameter("update");
            WebUpdate update = gson.fromJson(updateParam, WebUpdate.class);
            Update targetUpdate = storage.createUpdate(getCategoryFromId(update.getCategoryId()));
            Expression expr = update.getWhereExpression();
            if (expr != null) {
                targetUpdate.where(expr);
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

    @SuppressWarnings("unchecked")
    @WebStoragePathHandler( path = "query-execute" )
    private <T extends Pojo> void queryExecute(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (! isAuthorized(req, resp, Roles.READ)) {
            return;
        }
        String queryParam = req.getParameter("prepared-stmt");
        WebPreparedStatement<T> stmt = gson.fromJson(queryParam, WebPreparedStatement.class);
        
        PreparedParameters p = stmt.getParams();
        PreparedParameter[] params = p.getParams();
        PreparedStatementHolder<T> targetStmtHolder = getStatementHolderFromId(stmt.getStatementId());
        PreparedStatement<T> targetStmt = targetStmtHolder.getStmt();
        ParsedStatement<T> parsed = targetStmt.getParsedStatement();
        Query<T> targetQuery = null;
        ArrayList<T> resultList = new ArrayList<>();
        WebQueryResponse<T> response = new WebQueryResponse<>();
        try {
            targetQuery = (Query<T>)parsed.patchStatement(params);
            response.setResponseCode(WebQueryResponse.SUCCESS);
        } catch (IllegalPatchException e) {
            response.setResponseCode(WebQueryResponse.ILLEGAL_PATCH);
            writeResponse(resp, response, WebQueryResponse.class);
            return;
        }
        
        StatementDescriptor<T> desc = targetStmtHolder.getStatementDescriptor();
        StatementDescriptorMetadataFactory factory = descMetadataFactories.get(desc.getDescriptor());
        DescriptorMetadata actualMetadata = factory.getDescriptorMetadata(desc.getDescriptor(), params);
        
        UserPrincipal userPrincipal = getUserPrincipal(req);
        targetQuery = getQueryForPrincipal(userPrincipal, targetQuery, desc, actualMetadata);
        Cursor<T> cursor = targetQuery.execute();
        while (cursor.hasNext()) {
            resultList.add(cursor.next());
        }
        T[] results = (T[])Array.newInstance(targetStmtHolder.getDataClass(), resultList.size());
        for (int i = 0; i < resultList.size(); i++) {
            results[i] = resultList.get(i);
        }
        response.setResultList(results);
        writeResponse(resp, response, WebQueryResponse.class);
    }
    
    private UserPrincipal getUserPrincipal(HttpServletRequest req) {
        // Since we use our own JAAS based auth module this cast is safe
        // for Tomcat and JBoss AS 7 since they allow for configuration of
        // user principal classes. As for Jetty, it always uses
        // JAASUserPrincipal, but that principal has access to the subject
        // which in turn has our user principal added.
        Principal principal = req.getUserPrincipal();
        
        if (principal.getClass().getName().equals(JETTY_JAAS_USER_PRINCIPAL_CLASS_NAME)) {
            // Jetty has our principal on the accessible subject.
            Subject subject = null;
            try {
                // Do this via reflection in order to avoid a hard dependency
                // on jetty-plus.
                Class<?> jassUserPrincipal = Class.forName(JETTY_JAAS_USER_PRINCIPAL_CLASS_NAME);
                Method method = jassUserPrincipal.getDeclaredMethod("getSubject");
                subject = (Subject)method.invoke(principal);
            } catch (ClassNotFoundException | NoSuchMethodException
                    | SecurityException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
            if (subject == null) {
                throw new IllegalStateException(
                        "Could not retrieve subject from principal of type "
                                + JETTY_JAAS_USER_PRINCIPAL_CLASS_NAME);
            }
            Set<UserPrincipal> userPrincipals = subject.getPrincipals(UserPrincipal.class);
            if (userPrincipals.size() != 1) {
                throw new IllegalStateException("More than one thermostat user principals!");
            }
            return userPrincipals.iterator().next();
        } else {
            // A simple cast should succeed for the non-jetty case. At the very
            // least this should work for Tomcat and JBoss AS 7.
            return (UserPrincipal)principal;
        }
        
    }

    /*
     * Performs the heavy lifting of query filtering. It adds a where expression
     * and uses conjunction to the original, unfilterered, query.
     */
    private <T extends Pojo> Query<T> getQueryForPrincipal(
            UserPrincipal userPrincipal, Query<T> patchedQuery,
            StatementDescriptor<T> desc, DescriptorMetadata metaData) {
        Expression whereExpression = patchedQuery.getWhereExpression();
        FilterResult result = userPrincipal.getReadFilter(desc, metaData);
        Expression authorizationExpression = null;
        switch (result.getType()) {
        case ALL: // fall-through. same as next case.
        case QUERY_EXPRESSION:
            authorizationExpression = result.getFilterExpression();
            break;
        case EMPTY:
            return getEmptyQuery();
        default:
            throw new IllegalStateException("Unknown type!");
        }
        // Handled empty already
        if (whereExpression == null) {
            // no where, use auth expression only
            if (authorizationExpression != null) {
                patchedQuery.where(authorizationExpression);
                return patchedQuery;
            }
        } else {
            if (authorizationExpression != null) {
                Expression andExpression = new BinaryLogicalExpression<Expression, Expression>(
                        authorizationExpression, BinaryLogicalOperator.AND,
                        whereExpression);
                patchedQuery.where(andExpression);
                return patchedQuery;
            }
        }
        assert( (whereExpression != null && authorizationExpression == null) ||
                (whereExpression == null && authorizationExpression == null));
        // nothing to tag on
        return patchedQuery;
    }

    private <T extends Pojo> PreparedStatementHolder<T> getStatementHolderFromId(int statementId) {
        @SuppressWarnings("unchecked") // we are the only ones adding them
        PreparedStatementHolder<T> holder = (PreparedStatementHolder<T>)preparedStatementIds.get(statementId);
        return holder;
    }

    private Category<?> getCategoryFromId(int categoryId) {
        Category<?> category = categories.get(categoryId);
        return category;
    }

    private void writeResponse(HttpServletResponse resp,
            Object responseObj, Class<?> typeOfResponseObj) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(RESPONSE_JSON_CONTENT_TYPE);
        gson.toJson(responseObj, typeOfResponseObj, resp.getWriter());
        resp.flushBuffer();
    }

    @WebStoragePathHandler( path = "generate-token" )
    private void generateToken(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (! isAuthorized(req, resp, Roles.CMD_CHANNEL_GENERATE) ) {
            return;
        }
        TokenManager tokenManager = (TokenManager) getServletContext().getAttribute(TOKEN_MANAGER_KEY);
        assert tokenManager != null;
        String clientTokenParam = req.getParameter("client-token");
        byte[] clientToken = Base64.decodeBase64(clientTokenParam);
        String actionName = req.getParameter("action-name");
        // Perform pre-authorization: Since it's the client user which issues
        // generate-token we have the correct user for which we can check role
        // membership - and, thus, determine if this action is allowed to be
        // performed. If the action is not allowed to be performed for this user
        // a 403 will get returned and further verify-token would also fail,
        // since no token gets added into the map. Trustworthiness of the
        // action name will be implicitly checked by verify-token.
        //
        // We authorize based on role membership of this user. I.e. in order
        // for a ping request (action-name == "ping") to properly authorize
        // the user needs to be a member of role 
        // "thermostat-cmdc-grant-ping". More generally, membership of role
        // "thermostat-cmdc-grant-<actionName>" grants the authenticated
        // user the <actionName> command channel action.
        String requiredRole = CMDC_AUTHORIZATION_GRANT_ROLE_PREFIX + actionName;
        if (! isAuthorized(req, resp, requiredRole)) {
            return;
        }
        // authorization succeeded at this point
        byte[] token = tokenManager.generateToken(clientToken, actionName);
        resp.setContentType("application/octet-stream");
        resp.setContentLength(token.length);
        resp.getOutputStream().write(token);
    }

    @WebStoragePathHandler( path = "verify-token" )
    private void verifyToken(HttpServletRequest req, HttpServletResponse resp) {
        if (! isAuthorized(req, resp, Roles.CMD_CHANNEL_VERIFY) ) {
            return;
        }
        TokenManager tokenManager = (TokenManager) getServletContext().getAttribute(TOKEN_MANAGER_KEY);
        assert tokenManager != null;
        String clientTokenParam = req.getParameter("client-token");
        byte[] clientToken = Base64.decodeBase64(clientTokenParam);
        String actionName = req.getParameter("action-name");
        byte[] token = Base64.decodeBase64(req.getParameter("token"));
        // Perform authentication of the request. We can't do authorization for
        // the originating client request here, since the only user info we have
        // in verify-token is the identity of the agent which the client asked
        // to perform the action for. Hence looking up role membership is not
        // what we want here in order to limit privileges of the client.
        //
        // Note that we achieve this by performing authorization checks during
        // generate-token. This is something the client user initiates and hence
        // there we have the required user information. The entire command
        // channel interaction can only succeed if and only if generate-token
        // AND verify-token succeeded for the same token. Thus it's OK to only
        // verify the token here - which in would only verify successfully if
        // generate-token worked properly as a first step.
        boolean verified = tokenManager.verifyToken(clientToken, token, actionName);
        if (! verified) {
            logger.log(Level.INFO, "Command channel action " + actionName + " from remote host " +
                                   req.getRemoteAddr() + " FAILED to authenticate!");
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else {
            logger.log(Level.FINEST, "Command channel action " + actionName + " from remote host " +
                    req.getRemoteAddr() + " PASSED authentication.");
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }
    
    private boolean isAuthorized(HttpServletRequest req, HttpServletResponse resp, String role) {
        if (req.isUserInRole(role)) {
            return true;
        } else {
            logger.log(Level.INFO, "Not permitting access to " + req.getPathInfo() + ". User '" + req.getRemoteUser() + "' not in role " + role);
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
    }
    
    private <T extends Pojo> Query<T> getEmptyQuery() {
        final Query<T> empty = new Query<T>() {

            @Override
            public void where(Expression expr) {
                // must not be called.
                throw new IllegalStateException();
            }

            @Override
            public void sort(Key<?> key,
                    com.redhat.thermostat.storage.core.Query.SortDirection direction) {
                // must not be called.
                throw new IllegalStateException();
            }

            @Override
            public void limit(int n) {
                // must not be called.
                throw new IllegalStateException();
            }

            @Override
            public Cursor<T> execute() {
                return getEmptyCursor();
            }

            @Override
            public Expression getWhereExpression() {
                // must not be called.
                throw new IllegalStateException();
            }
            
        };
        return empty;
    }
    
    private <T extends Pojo> Cursor<T> getEmptyCursor() {
        final Cursor<T> empty = new Cursor<T>() {

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public T next() {
                // must not be called.
                throw new IllegalStateException();
            }
            
        };
        return empty;
    }


}

