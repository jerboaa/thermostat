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

package com.redhat.thermostat.web.common;

import com.redhat.thermostat.storage.core.PreparedStatement;

/**
 * Common response codes for prepared statement responses.
 *
 */
public interface PreparedStatementResponseCode {

    /**
     * Response code for successful prepared queries.
     */
    public static final int QUERY_SUCCESS = 0;
    
    /**
     * Generic error code for failed queries. Usually
     * returned if get-more failed for an unknown reason.
     */
    public static final int QUERY_FAILURE = -100;
    
    
    /**
     * Failure code for expired cursors. Usually returned if
     * get-more requests failed because the underlying cursor
     * was null.
     */
    public static final int GET_MORE_NULL_CURSOR = -151;
    
    /**
     * Response code if patching of a {@link PreparedStatement} failed during
     * statement execution.
     * <p>
     * For example a patching failure could happen if there was a type mismatch
     * between the descriptor and the parameter provided. Providing not all
     * parameters and attempting execution of a {@link PreparedStatement} would
     * be another example.
     */
    public static final int ILLEGAL_PATCH = -1;
    
    /**
     * Failure code for mismatching server tokens. This is usually happening if
     * client and server get out of sync due to re-deployment or the like.
     * Client should recover from this automatically by clearing the client
     * cache and preparing statements again.
     */
    public static final int PREP_STMT_BAD_STOKEN = -2;
    
    /**
     * Failure to execute a prepared write statement for some unknown reason.
     */
    public static final int WRITE_GENERIC_FAILURE = -200;
    
}

