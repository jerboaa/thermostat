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

/**
 * Model class as returned upon preparing statements.
 */
public class WebPreparedStatementResponse {
    
    /**
     * Response code for untrusted/unknown descriptors.
     */
    public static final int ILLEGAL_STATEMENT = -1;
    
    /**
     * Response code for descriptor parsing exceptions.
     */
    public static final int DESCRIPTOR_PARSE_FAILED = -2;
    
    /**
     * Response code indicating that the server token
     * of the client and the token the server is using
     * did not match.
     */
    public static final int CATEGORY_OUT_OF_SYNC = -3;
    
    public WebPreparedStatementResponse() {
        // Should always be set using the setter before it
        // is retrieved. Since 0 is a bad default for this,
        // we set it to -1 in order to make this an invalid
        // value right away.
        this.numFreeVariables = -1;
    }
    
    private int numFreeVariables;
    private SharedStateId statementId;
    
    public SharedStateId getStatementId() {
        return statementId;
    }

    public void setStatementId(SharedStateId statementId) {
        this.statementId = statementId;
    }

    public int getNumFreeVariables() {
        return numFreeVariables;
    }

    public void setNumFreeVariables(int freeVars) {
        this.numFreeVariables = freeVars;
    }
}

