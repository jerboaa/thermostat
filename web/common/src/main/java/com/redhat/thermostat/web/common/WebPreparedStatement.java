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

package com.redhat.thermostat.web.common;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.ParsedStatement;
import com.redhat.thermostat.storage.core.PreparedParameters;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.model.Pojo;

public class WebPreparedStatement<T extends Pojo> implements
        PreparedStatement<T> {
    
    private PreparedParameters params;
    private int statementId;
    
    public WebPreparedStatement(int numParams, int statementId) {
        this.params = new PreparedParameters(numParams);
        this.statementId = statementId;
    }
    
    public WebPreparedStatement() {
        // nothing. used for serialization
    }

    public int getStatementId() {
        return statementId;
    }

    public void setStatementId(int statementId) {
        this.statementId = statementId;
    }

    public PreparedParameters getParams() {
        return params;
    }

    public void setParams(PreparedParameters params) {
        this.params = params;
    }

    @Override
    public void setBoolean(int paramIndex, boolean paramValue) {
        params.setBoolean(paramIndex, paramValue);
    }

    @Override
    public void setBooleanList(int paramIndex, boolean[] paramValue) {
        params.setBooleanList(paramIndex, paramValue);
    }

    @Override
    public void setLong(int paramIndex, long paramValue) {
        params.setLong(paramIndex, paramValue);
    }

    @Override
    public void setLongList(int paramIndex, long[] paramValue) {
        params.setLongList(paramIndex, paramValue);
    }

    @Override
    public void setInt(int paramIndex, int paramValue) {
        params.setInt(paramIndex, paramValue);
    }

    @Override
    public void setIntList(int paramIndex, int[] paramValue) {
        params.setIntList(paramIndex, paramValue);
    }

    @Override
    public void setString(int paramIndex, String paramValue) {
        params.setString(paramIndex, paramValue);
    }

    @Override
    public void setStringList(int paramIndex, String[] paramValue) {
        params.setStringList(paramIndex, paramValue);
    }

    @Override
    public void setDouble(int paramIndex, double paramValue) {
        params.setDouble(paramIndex, paramValue);
    }

    @Override
    public void setDoubleList(int paramIndex, double[] paramValue) {
        params.setDoubleList(paramIndex, paramValue);
    }

    @Override
    public void setPojo(int paramIndex, Pojo paramValue) {
        params.setPojo(paramIndex, paramValue);
    }

    @Override
    public void setPojoList(int paramIndex, Pojo[] paramValue) {
        params.setPojoList(paramIndex, paramValue);
    }

    @Override
    public int execute() throws StatementExecutionException {
        // actual implementation should override this
        throw new IllegalStateException();
    }

    @Override
    public Cursor<T> executeQuery()
            throws StatementExecutionException {
        // actual implementation should override this
        throw new IllegalStateException();
    }

    @Override
    public ParsedStatement<T> getParsedStatement() {
        // Should never be called on WebPreparedStatement
        // It should use the implementation of the backing
        // storage implementation instead.
        throw new IllegalStateException();
    }

}

