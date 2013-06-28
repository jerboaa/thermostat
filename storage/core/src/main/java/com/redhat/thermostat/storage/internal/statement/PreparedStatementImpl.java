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

package com.redhat.thermostat.storage.internal.statement;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DataModifyingStatement;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Statement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.model.Pojo;

/**
 * Main implementation of {@link PreparedStatement}s.
 *
 */
final public class PreparedStatementImpl implements PreparedStatement {
    
    private Query<? extends Pojo> query;
    private DataModifyingStatement dmlStatement;
    private final PreparedParameter[] params;
    private final ParsedStatement parsedStatement;
    
    public PreparedStatementImpl(Storage storage, StatementDescriptor desc) throws DescriptorParsingException {
        StatementDescriptorParser parser = new StatementDescriptorParser(storage, desc);
        this.parsedStatement = parser.parse();
        int numParams = parsedStatement.getNumParams();
        params = new PreparedParameter[numParams];
        Statement statement = parsedStatement.getRawStatement();
        if (statement instanceof DataModifyingStatement) {
            this.dmlStatement = (DataModifyingStatement)statement;
        } else if (statement instanceof Query<?>) {
            this.query = (Query<?>)statement;
        }
    }
    
    // used for testing ParsedStatements
    PreparedStatementImpl(int numParams) {
        params = new PreparedParameter[numParams];
        this.parsedStatement = null;
    }
    
    @Override
    public void setLong(int paramIndex, long paramValue) {
        setType(paramIndex, paramValue, Long.class);
    }

    @Override
    public void setInt(int paramIndex, int paramValue) {
        setType(paramIndex, paramValue, Integer.class);
    }

    @Override
    public void setStringList(int paramIndex, String[] paramValue) {
        setType(paramIndex, paramValue, String[].class);
    }
    
    @Override
    public void setBoolean(int paramIndex, boolean paramValue) {
        setType(paramIndex, paramValue, boolean.class);
    }

    private void setType(int paramIndex, Object paramValue, Class<?> paramType) {
        if (paramIndex >= params.length) {
            throw new IllegalArgumentException("Parameter index '" + paramIndex + "' out of range.");
        }
        PreparedParameter param = new PreparedParameter(paramValue, paramType);
        params[paramIndex] = param;
    }

    @Override
    public int execute() throws StatementExecutionException {
        if (dmlStatement == null) {
            throw new IllegalStateException(
                    "Can't execute statement which isn't an instance of "
                            + DataModifyingStatement.class.getName());
        }
        try {
            dmlStatement = (DataModifyingStatement)parsedStatement.patchQuery(this);
        } catch (Exception e) {
            throw new StatementExecutionException(e);
        }
        return dmlStatement.execute();
    }

    // unchecked since query has no knowledge of type
    @SuppressWarnings("unchecked")
    @Override
    public Cursor<?> executeQuery() throws StatementExecutionException{
        if (query == null) {
            throw new IllegalStateException(
                    "Can't execute statement which isn't an instance of "
                            + Query.class.getName());
        }
        try {
            // FIXME: I'm sure we can improve on this. We should avoid walking the
            // tree each time. Some cache with unfinished nodes and a reference
            // to the matching expression should be sufficient.
            query = (Query<?>)parsedStatement.patchQuery(this);
        } catch (IllegalPatchException e) {
            throw new StatementExecutionException(e);
        }
        return query.execute();
    }

    @Override
    public int getId() {
        // not implemented for Mongo
        return -1;
    }

    @Override
    public void setString(int paramIndex, String paramValue) {
        setType(paramIndex, paramValue, String.class);
    }

    PreparedParameter[] getParams() {
        return params;
    }
}
