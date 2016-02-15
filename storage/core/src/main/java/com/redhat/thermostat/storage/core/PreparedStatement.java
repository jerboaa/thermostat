/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.storage.core;

import com.redhat.thermostat.storage.model.Pojo;


/**
 * A prepared statement.
 * 
 * @see Statement
 * @see DataModifyingStatement
 * @see Storage#prepareStatement(StatementDescriptor)
 *
 */
public interface PreparedStatement<T extends Pojo> extends PreparedStatementSetter {
    
    void setBoolean(int paramIndex, boolean paramValue);
    
    void setLong(int paramIndex, long paramValue);
    
    void setInt(int paramIndex, int paramValue);
    
    void setString(int paramIndex, String paramValue);
    
    void setStringList(int paramIndex, String[] paramValue);

    /**
     * Executes a predefined {@link DataModifyingStatement}.
     * 
     * @return a non-zero error code on failure. Zero otherwise.
     * 
     * @throws StatementExecutionException
     *             If the prepared statement wasn't valid for execution.
     * @throws StorageException
     *             If the underlying storage throws an exception while executing
     *             this statement
     */
    int execute() throws StatementExecutionException;

    /**
     * Executes a predefined {@link Query}.
     * 
     * @return a {@link Cursor} as a result to the underlying {@link Query}
     * 
     * @throws StatementExecutionException
     *             If the prepared statement wasn't valid for execution.
     * @throws StorageException
     *             If the underlying storage throws an exception while executing
     *             this query
     */
    Cursor<T> executeQuery() throws StatementExecutionException;
    
    /**
     * @return An intermediary representation of this prepared statement.
     */
    ParsedStatement<T> getParsedStatement();

}
