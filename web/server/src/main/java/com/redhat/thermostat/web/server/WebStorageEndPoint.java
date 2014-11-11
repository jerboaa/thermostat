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

package com.redhat.thermostat.web.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.config.internal.CommonPathsImpl;
import com.redhat.thermostat.storage.core.Categories;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DataModifyingStatement;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.IllegalPatchException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.ParsedStatement;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.PreparedParameters;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Statement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.experimental.BatchCursor;
import com.redhat.thermostat.storage.model.AggregateResult;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.BinaryLogicalExpression;
import com.redhat.thermostat.storage.query.BinaryLogicalOperator;
import com.redhat.thermostat.storage.query.Expression;
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
import com.redhat.thermostat.web.server.auth.FilterResult;
import com.redhat.thermostat.web.server.auth.PrincipalCallback;
import com.redhat.thermostat.web.server.auth.PrincipalCallbackFactory;
import com.redhat.thermostat.web.server.auth.Roles;
import com.redhat.thermostat.web.server.auth.UserPrincipal;
import com.redhat.thermostat.web.server.auth.WebStoragePathHandler;
import com.redhat.thermostat.web.server.containers.ServletContainerInfo;
import com.redhat.thermostat.web.server.containers.ServletContainerInfoFactory;

@SuppressWarnings("serial")
public class WebStorageEndPoint extends HttpServlet {

    // This is an ugly hack in order to allow for testing of batched querying.
    static int DEFAULT_QUERY_BATCH_SIZE = BatchCursor.DEFAULT_BATCH_SIZE;
    
    static final String CMDC_AUTHORIZATION_GRANT_ROLE_PREFIX = "thermostat-cmdc-grant-";
    static final String FILES_READ_GRANT_ROLE_PREFIX = "thermostat-files-grant-read-filename-";
    static final String FILES_WRITE_GRANT_ROLE_PREFIX = "thermostat-files-grant-write-filename-";
    private static final String TOKEN_MANAGER_TIMEOUT_PARAM = "token-manager-timeout";
    private static final String TOKEN_MANAGER_KEY = "token-manager";
    private static final String USER_PRINCIPAL_CALLBACK_KEY = "user-principal-callback";
    private static final String CURSOR_MANAGER_KEY = "cursor-manager";
    private static final String PREPARED_STMT_MANAGER_KEY = "prepared-stmt-manager";
    private static final int UNKNOWN_CURSOR_ID = -0xdeadbeef;
    private static final String CATEGORY_KEY_FORMAT = "%s|%s";

    // our strings can contain non-ASCII characters. Use UTF-8
    // see also PR 1344
    private static final String RESPONSE_JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    private static final Logger logger = LoggingUtils.getLogger(WebStorageEndPoint.class);

    private Storage storage;
    private Gson gson;
    private CommonPaths paths;

    public static final String STORAGE_ENDPOINT = "storage.endpoint";
    public static final String STORAGE_USERNAME = "storage.username";
    public static final String STORAGE_PASSWORD = "storage.password";
    public static final String STORAGE_CLASS = "storage.class";
    
    private int currentCategoryId;

    private Map<String, Integer> categoryIds;
    private Map<Integer, Category<?>> categories;
    // A unique server token, which gets reset on every Servlet.init() call.
    // I.e. every reload/redeployment.
    private UUID serverToken;
    
    // read-only set of all known statement descriptors we trust and allow
    private Set<String> knownStatementDescriptors;
    // read-only set of all known categories which we allow to get registered.
    private Set<String> knownCategoryNames;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        logger.log(Level.INFO, "Initializing web service");
        
        // check if thermostat home is set and readable
        // Side effect: sets this.paths
        checkThermostatHome();
        
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                .registerTypeAdapterFactory(new SharedStateIdTypeAdapterFactory())
                .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                .registerTypeAdapterFactory(new WebQueryResponseTypeAdapterFactory())
                .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                .registerTypeAdapterFactory(new PreparedParametersTypeAdapterFactory())
                .create();
        categoryIds = new HashMap<>();
        categories = new HashMap<>();
        TokenManager tokenManager = new TokenManager();
        String timeoutParam = getInitParameter(TOKEN_MANAGER_TIMEOUT_PARAM);
        if (timeoutParam != null) {
            tokenManager.setTimeout(Integer.parseInt(timeoutParam));
        }
        ServletContext servletContext = getServletContext();
        servletContext.setAttribute(TOKEN_MANAGER_KEY, tokenManager);
        
        // Set the set of statement descriptors which we trust
        KnownDescriptorRegistry descRegistry = KnownDescriptorRegistryFactory.getInstance();
        knownStatementDescriptors = descRegistry.getRegisteredDescriptors();
        // Set the set of category names which we allow to get registered
        KnownCategoryRegistry categoryRegistry = KnownCategoryRegistryFactory.getInstance();
        knownCategoryNames = categoryRegistry.getRegisteredCategoryNames();
        
        // finally set callback for retrieving our JAAS user principal
        String serverInfo = servletContext.getServerInfo();
        ServletContainerInfoFactory factory = new ServletContainerInfoFactory(serverInfo);
        ServletContainerInfo info = factory.getInfo();
        PrincipalCallbackFactory cbFactory = new PrincipalCallbackFactory(info);
        PrincipalCallback callback = Objects.requireNonNull(cbFactory.getCallback());
        servletContext.setAttribute(USER_PRINCIPAL_CALLBACK_KEY, callback);
        synchronized(servletContext) {
            servletContext.setAttribute(PREPARED_STMT_MANAGER_KEY, new PreparedStatementManager());
        }
        serverToken = UUID.randomUUID();
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
            final String username = getServletConfig().getInitParameter(STORAGE_USERNAME);
            // FIXME Password as string?  bad.
            final String password = getServletConfig().getInitParameter(STORAGE_PASSWORD);
            StorageCredentials creds = new StorageCredentials() {

                @Override
                public String getUsername() {
                    return username;
                }

                @Override
                public char[] getPassword() {
                    return password == null ? null : password.toCharArray();
                }
                
            };
            storage = StorageFactory.getStorage(storageClass, storageEndpoint, paths, creds);
        }
        String uri = req.getRequestURI();
        int lastPartIdx = uri.lastIndexOf("/");
        String cmd = uri.substring(lastPartIdx + 1);
        if (cmd.equals("prepare-statement")) {
            prepareStatement(req, resp);
        } else if (cmd.equals("query-execute")) {
            queryExecute(req, resp);
        } else if (cmd.equals("write-execute")) {
            writeExecute(req, resp);
        } else if (cmd.equals("register-category")) {
            registerCategory(req, resp);
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
        } else if (cmd.equals("get-more")) {
            getMore(req, resp);
        }
    }

    // Side effect: sets this.paths
    // package-private for testing
    boolean isThermostatHomeSet() {
        try {
            // this throws config exception if neither the property
            // nor the env var is set
            paths = new CommonPathsImpl();
            return true;
        } catch (InvalidConfigurationException e) {
            return false;
        }
    }

    // Side effect: sets this.paths
    private void checkThermostatHome() {
        if (!isThermostatHomeSet()) {
            String msg = "THERMOSTAT_HOME context parameter not set!";
            logger.log(Level.SEVERE, msg);
            throw new RuntimeException(msg);
        }
        File thermostatHomeFile = getThermostatHome();
        String notReadableMsg = " is not readable or does not exist!";
        if (!thermostatHomeFile.canRead()) {
            // This is bad news. If we can't at least read THERMOSTAT_HOME
            // we are bound to fail in some weird ways at some later point.
            String msg = "THERMOSTAT_HOME = "
                    + thermostatHomeFile.getAbsolutePath()
                    + notReadableMsg;
            logger.log(Level.SEVERE, msg);
            throw new RuntimeException(msg);
        }
        // we need to be able to read ssl config for backing storage
        // paths got set in isThermostatHomeSet()
        File sslProperties = new File(paths.getSystemConfigurationDirectory(), "ssl.properties");
        if (!sslProperties.canRead()) {
            String msg = "File " + sslProperties.getAbsolutePath() +
                    notReadableMsg;
            logger.log(Level.SEVERE, msg);
            throw new RuntimeException(msg);
        }
        // Thermost home looks OK and seems usable
        logger.log(Level.FINEST, "THERMOSTAT_HOME == "
                + thermostatHomeFile.getAbsolutePath());
    }

    private File getThermostatHome() {
        try {
            return paths.getSystemThermostatHome();
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
            SharedStateId id = new SharedStateId(WebPreparedStatementResponse.ILLEGAL_STATEMENT, serverToken);
            response.setStatementId(id);
            writeResponse(resp, response, WebPreparedStatementResponse.class);
            return;
        }
        StatementDescriptor<T> desc = new StatementDescriptor<>(cat, queryDescrParam);
        // Check if descriptor is trusted (i.e. known)
        if (!knownStatementDescriptors.contains(desc.getDescriptor())) {
            String msg = "Attempted to prepare a statement descriptor which we " +
            		"don't trust! Descriptor was: ->" + desc.getDescriptor() + "<-";
            logger.log(Level.WARNING, msg);
            SharedStateId id = new SharedStateId(WebPreparedStatementResponse.ILLEGAL_STATEMENT, serverToken);
            response.setStatementId(id);
            writeResponse(resp, response, WebPreparedStatementResponse.class);
            return;
        }
        
        PreparedStatementManager prepStmtManager = getPreparedStmtManager();
        // see if we've prepared this query already
        PreparedStatementHolder<T> holder = prepStmtManager.getStatementHolder(desc);
        if (holder != null) {
            ParsedStatement<T> parsed = holder.getStmt().getParsedStatement();
            int freeVars = parsed.getNumParams();
            response.setNumFreeVariables(freeVars);
            SharedStateId id = new SharedStateId(holder.getId().getId(), serverToken);
            response.setStatementId(id);
            writeResponse(resp, response, WebPreparedStatementResponse.class);
            return;
        } else {
            // Prepare the target statement and track it via
            // PreparedStatementManager
            PreparedStatement<T> targetPreparedStatement;
            try {
                targetPreparedStatement = (PreparedStatement<T>) storage
                        .prepareStatement(desc);
            } catch (DescriptorParsingException e) {
                logger.log(Level.WARNING, "Descriptor parse error!", e);
                SharedStateId id = new SharedStateId(WebPreparedStatementResponse.DESCRIPTOR_PARSE_FAILED, serverToken);
                response.setStatementId(id);
                writeResponse(resp, response,
                        WebPreparedStatementResponse.class);
                return;
            }
            SharedStateId stmtId = prepStmtManager.createAndPutHolder(
                    serverToken, targetPreparedStatement, cat.getDataClass(),
                    desc);
            ParsedStatement<?> parsed = targetPreparedStatement
                    .getParsedStatement();
            response.setNumFreeVariables(parsed.getNumParams());
            response.setStatementId(stmtId);
            writeResponse(resp, response, WebPreparedStatementResponse.class);
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
            throw new IllegalStateException("Unknown data class: '" + dataClassName + "'");
        }
    }

    /**
     * Handler for query executions (except for getting more results). See
     * {@link #getMore(HttpServletRequest, HttpServletResponse)}.
     * 
     * @param req
     * @param resp
     * @throws IOException
     */
    @WebStoragePathHandler( path = "query-execute" )
    private <T extends Pojo> void queryExecute(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (! isAuthorized(req, resp, Roles.READ)) {
            return;
        }
        String queryParam = req.getParameter("prepared-stmt");
        @SuppressWarnings("unchecked")
        WebPreparedStatement<T> stmt = gson.fromJson(queryParam, WebPreparedStatement.class);
        
        // Check if the server token the client knows about still matches.
        // Bail out early otherwise.
        SharedStateId stmtId = stmt.getStatementId();
        if (!serverToken.equals(stmtId.getServerToken())) {
            logger.log(Level.INFO, "Server token: '" + serverToken +
                                   "' and client token '" + stmtId.getServerToken() +
                                   "' out of sync.");
            WebQueryResponse<T> response = new WebQueryResponse<>();
            response.setResponseCode(PreparedStatementResponseCode.PREP_STMT_BAD_STOKEN);
            writeResponse(resp, response, WebQueryResponse.class);
            return;
        }
        PreparedParameters p = stmt.getParams();
        PreparedParameter[] params = p.getParams();
        PreparedStatementManager prepStmtManager = getPreparedStmtManager();
        PreparedStatementHolder<T> targetStmtHolder = prepStmtManager.getStatementHolder(stmtId);
        PreparedStatement<T> targetStmt = targetStmtHolder.getStmt();
        ParsedStatement<T> parsed = targetStmt.getParsedStatement();
        Query<T> targetQuery = null;
        WebQueryResponse<T> response = new WebQueryResponse<>();
        try {
            targetQuery = (Query<T>)parsed.patchStatement(params);
            response.setResponseCode(PreparedStatementResponseCode.QUERY_SUCCESS);
        } catch (IllegalPatchException e) {
            logger.log(Level.INFO, "Failed to execute query", e);
            response.setResponseCode(PreparedStatementResponseCode.ILLEGAL_PATCH);
            writeResponse(resp, response, WebQueryResponse.class);
            return;
        }
        
        StatementDescriptor<T> desc = targetStmtHolder.getStatementDescriptor();
        
        UserPrincipal userPrincipal = getUserPrincipal(req);
        targetQuery = getQueryForPrincipal(userPrincipal, targetQuery, desc);
        // While the signature still says the retval of query execute is
        // cursor, we return an instance of AdvancedCursor instead for new code.
        // This is the case for MongoStorage. However, in order to work
        // around potential third-party implementations perform the check
        // and fall back to legacy behaviour.
        Cursor<T> cursor = targetQuery.execute();
        List<T> resultsList = null;
        if (cursor instanceof BatchCursor) {
            BatchCursor<T> batchCursor = (BatchCursor<T>)cursor;
            resultsList = getBatchFromCursor(batchCursor, DEFAULT_QUERY_BATCH_SIZE);
            assert(resultsList.size() <= DEFAULT_QUERY_BATCH_SIZE);
            CursorManager cursorManager = null;
            HttpSession userSession = req.getSession();
            synchronized(userSession) {
                cursorManager = (CursorManager)userSession.getAttribute(CURSOR_MANAGER_KEY);
                if (cursorManager == null) {
                    // Not yet set for this user, create a new cursor manager
                    // and start the sweeper timer so as to prevent memory
                    // leaks due to cursors kept as a reference in cursor manager
                    cursorManager = new CursorManager();
                    cursorManager.startSweeperTimer();
                    userSession.setAttribute(CURSOR_MANAGER_KEY, cursorManager);
                }
            }
            // Only record cursor if there are more results to return than the
            // first batch size.
            int cursorId = cursorManager.put(batchCursor);
            response.setCursorId(cursorId);
            response.setHasMoreBatches(batchCursor.hasNext());
        } else {
            // fallback to old behaviour
            resultsList = getLegacyResultList(cursor);
            response.setHasMoreBatches(false); // only one batch
            response.setCursorId(UNKNOWN_CURSOR_ID);
        }
        writeQueryResponse(resp, response, resultsList, targetStmtHolder);
    }

    private PreparedStatementManager getPreparedStmtManager() {
        ServletContext servletContext = getServletContext();
        PreparedStatementManager prepStmtManager = null;
        synchronized(servletContext) {
            prepStmtManager = (PreparedStatementManager)servletContext.getAttribute(PREPARED_STMT_MANAGER_KEY);
        }
        // If this throws a NPE this is certainly a bug.
        return Objects.requireNonNull(prepStmtManager);
    }
    
    private <T extends Pojo> void writeQueryResponse(HttpServletResponse resp, WebQueryResponse<T> response, List<T> resultsList, PreparedStatementHolder<T> targetStmtHolder) throws IOException {
        @SuppressWarnings("unchecked")
        T[] results = (T[])Array.newInstance(targetStmtHolder.getDataClass(), resultsList.size());
        for (int i = 0; i < resultsList.size(); i++) {
            results[i] = resultsList.get(i);
        }
        response.setResultList(results);
        writeResponse(resp, response, WebQueryResponse.class);
    }
    
    /**
     * Handler for getting more results for a query. Queries return results
     * in batches. The first batch is returned via {@link #queryExecute(HttpServletRequest, HttpServletResponse)}. Subsequent results will get returned using
     * this path.
     * 
     * @param req
     * @param resp
     * @throws IOException
     */
    @WebStoragePathHandler( path = "get-more" )
    private <T extends Pojo> void getMore(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (! isAuthorized(req, resp, Roles.READ)) {
            return;
        }
        String stmtIdParam = req.getParameter("prepared-stmt-id");
        String cursorIdParam = req.getParameter("cursor-id");
        String batchSizeParam = req.getParameter("batch-size");
        
        // Statement Id is JSON encoded.
        SharedStateId id = gson.fromJson(URLDecoder.decode(stmtIdParam, "UTF-8"), SharedStateId.class);
        int cursorId = Integer.parseInt(cursorIdParam);
        int batchSize = Integer.parseInt(batchSizeParam);
        
        HttpSession userSession = req.getSession();
        CursorManager cursorManager = null; 
        synchronized(userSession) {
            cursorManager = (CursorManager)userSession.getAttribute(CURSOR_MANAGER_KEY);
        }
        if (cursorManager == null) {
            // Trying to get a cursorManager for a user which does not
            // have it in the session as an attribute? Perhaps a cluster
            // deployment problem?
            throw new IllegalStateException("[get-more] No cursor manager available in session for " + req.getRemoteUser());
        }
        @SuppressWarnings("unchecked")
        BatchCursor<T> batchCursor = (BatchCursor<T>)cursorManager.get(cursorId);
        
        PreparedStatementManager prepStmtManager = getPreparedStmtManager();
        PreparedStatementHolder<T> targetStmtHolder = prepStmtManager.getStatementHolder(id);
        if (batchCursor == null) {
            // This either means:
            // 1. The underlying (backing-storage) cursor didn't have
            //    more results, thus WebQueryResponse.hasMoreBatches() == false,
            //    when queryExecute() returned its WebQueryResponse. Still,
            //    the client requested more elements anyway and ended up here.
            //    That's really a bug in the client which performed this request.
            // 2. The cursor expired via the sweeper timer in CursorManager,
            //    before the client actually managed to request more results. In
            //    that case the client is advised to re-issue the query in order
            //    to get a new cursor, since the underlying data in the DB might
            //    have changed anyway and results returned would be surprising.
            //    See http://docs.mongodb.org/manual/core/cursors/
            //    (section "Cursor Isolation")
            String msg = "No cursor found for user " +
                            req.getRemoteUser() + " and cursor id: " + cursorId +
                         ". Query was: " + targetStmtHolder.getStatementDescriptor();
            logger.log(Level.WARNING, msg);
            WebQueryResponse<T> response = new WebQueryResponse<>();
            response.setResponseCode(PreparedStatementResponseCode.GET_MORE_NULL_CURSOR);
            response.setHasMoreBatches(false);
            response.setCursorId(cursorId);
            List<T> empty = Collections.emptyList();
            writeQueryResponse(resp, response, empty, targetStmtHolder);
            return;
        }
        // Update backing storage cursor with (possibly) changed params.
        // This will validate batchSize input
        batchCursor.setBatchSize(batchSize);
        
        List<T> nextBatch = getBatchFromCursor(batchCursor, batchCursor.getBatchSize());
        boolean stillMoreResults = batchCursor.hasNext();
        if (stillMoreResults) {
            // Refresh timestamp of a live cursor so that it won't expire.
            cursorManager.updateCursorTimeStamp(cursorId);
        } else {
            // no more results, remove cursor
            cursorManager.removeCursor(cursorId);
        }
        logger.log(Level.FINEST, "Fetched more results (" + nextBatch.size() + ") for user '" + req.getRemoteUser() + "' cursorId " + cursorId +
                                 ". Statement: " + targetStmtHolder.getStatementDescriptor());
        WebQueryResponse<T> response = new WebQueryResponse<>();
        response.setResponseCode(PreparedStatementResponseCode.QUERY_SUCCESS);
        response.setHasMoreBatches(stillMoreResults);
        response.setCursorId(cursorId);
        writeQueryResponse(resp, response, nextBatch, targetStmtHolder);
    }
    
    // Fetches the first batch of results. Number of results are determined
    // by the default batch size in AdvancedCursor
    private <T extends Pojo> List<T> getBatchFromCursor(final BatchCursor<T> cursor, final int batchSize) {
        ArrayList<T> resultList = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize && cursor.hasNext(); i++) {
            resultList.add(cursor.next());
        }
        return resultList;
    }
    
    // Fetches all results imposing no bound on the result set if the underlying
    // query was unbounded.
    private <T extends Pojo> ArrayList<T> getLegacyResultList(Cursor<T> cursor) {
        ArrayList<T> resultList = new ArrayList<>();
        while (cursor.hasNext()) {
            resultList.add(cursor.next());
        }
        return resultList;
    }
    
    @SuppressWarnings("unchecked")
    @WebStoragePathHandler( path = "write-execute" )
    private <T extends Pojo> void writeExecute(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (! isAuthorized(req, resp, Roles.WRITE)) {
            return;
        }
        String queryParam = req.getParameter("prepared-stmt");
        WebPreparedStatement<T> stmt = gson.fromJson(queryParam, WebPreparedStatement.class);
        
        // Check if the server token the client knows about still matches.
        // Bail out early otherwise.
        SharedStateId stmtId = stmt.getStatementId();
        if (!serverToken.equals(stmtId.getServerToken())) {
            logger.log(Level.INFO, "Server token: '" + serverToken +
                                   "' and client token '" + stmtId.getServerToken() +
                                   "' out of sync.");
            writeResponse(resp, PreparedStatementResponseCode.PREP_STMT_BAD_STOKEN, int.class);
            return;
        }
        PreparedParameters p = stmt.getParams();
        PreparedParameter[] params = p.getParams();
        PreparedStatementManager prepStmtManager = getPreparedStmtManager();
        PreparedStatementHolder<T> targetStmtHolder = prepStmtManager.getStatementHolder(stmt.getStatementId());
        PreparedStatement<T> targetStmt = targetStmtHolder.getStmt();
        ParsedStatement<T> parsed = targetStmt.getParsedStatement();
        
        DataModifyingStatement<T> targetStatement = null;
        try {
            // perform the patching of the target statement.
            targetStatement = (DataModifyingStatement<T>)parsed.patchStatement(params);
        } catch (IllegalPatchException e) {
            logger.log(Level.INFO, "Failed to execute write", e);
            writeResponse(resp, PreparedStatementResponseCode.ILLEGAL_PATCH, int.class);
            return;
        }
        
        // executes statement
        int response = targetStatement.apply();
        writeResponse(resp, response, int.class);
    }
    
    private UserPrincipal getUserPrincipal(HttpServletRequest req) {
        Principal principal = req.getUserPrincipal();
        
        ServletContext context = getServletContext();
        PrincipalCallback callback = (PrincipalCallback)context.getAttribute(USER_PRINCIPAL_CALLBACK_KEY);
        return callback.getUserPrincipal(principal);
    }

    /*
     * Performs the heavy lifting of query filtering. It adds a where expression
     * and uses conjunction to the original, unfilterered, query.
     */
    private <T extends Pojo> Query<T> getQueryForPrincipal(
            UserPrincipal userPrincipal, Query<T> patchedQuery,
            StatementDescriptor<T> desc) {
        Expression whereExpression = patchedQuery.getWhereExpression();
        FilterResult result = userPrincipal.getReadFilter(desc);
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
        assert(authorizationExpression == null);
        // nothing to tag on
        return patchedQuery;
    }

    private Category<?> getCategoryFromId(int categoryId) {
        Category<?> category = categories.get(categoryId);
        return category;
    }

    private void writeResponse(HttpServletResponse resp,
            Object responseObj, Class<?> typeOfResponseObj) throws IOException {
        String json = null;
        try {
            json = gson.toJson(responseObj, typeOfResponseObj);
        } catch (Exception e) {
            logger.log(Level.WARNING, "JSON serialization failed for " + typeOfResponseObj, e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(RESPONSE_JSON_CONTENT_TYPE);
        try (PrintWriter pw = resp.getWriter()) {
            pw.write(json);
        }
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

            @Override
            public Statement<T> getRawDuplicate() {
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

