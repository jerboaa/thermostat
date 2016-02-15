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

package com.redhat.thermostat.web.client.internal;

import java.lang.reflect.Type;

import com.redhat.thermostat.web.common.SharedStateId;

/**
 * Container used for parameter caching in order to avoid unneccessary
 * network overhead.
 *
 * @see {@link WebStorage}
 * @see {@link WebPreparedStatementCache}
 *
 */
class WebPreparedStatementHolder {
    
    private final Type typeToken;
    private final int numParams;
    private final SharedStateId statementId;
    
    WebPreparedStatementHolder(Type typeToken, int numParams, SharedStateId statementId) {
        this.typeToken = typeToken;
        this.numParams = numParams;
        this.statementId = statementId;
    }

    Type getTypeToken() {
        return typeToken;
    }

    int getNumParams() {
        return numParams;
    }

    SharedStateId getStatementId() {
        return statementId;
    }
}