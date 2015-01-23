/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.web.common.SharedStateId;

/**
 * Manager for {@link PreparedStatement}s which get prepared/executed via
 * {@link WebStorage}.
 *
 */
class PreparedStatementManager {

    // We have one map per server token.
    private final Map<SharedStateId, PreparedStatementHolder<?>> preparedStatementIds;
    private final Map<StatementDescriptor<?>, PreparedStatementHolder<?>> preparedStmts;
    private int currentPreparedStmtId = 0;
    
    PreparedStatementManager() {
        preparedStatementIds = new HashMap<>();
        preparedStmts = new HashMap<>();
    }
    
    // Test only constructor
    PreparedStatementManager(int initialValue) {
        this();
        currentPreparedStmtId = initialValue;
    }
    
    @SuppressWarnings("unchecked") // we are the only ones adding them
    synchronized <T extends Pojo> PreparedStatementHolder<T> getStatementHolder(SharedStateId id) {
        return (PreparedStatementHolder<T>)preparedStatementIds.get(Objects.requireNonNull(id));
    }
    
    @SuppressWarnings("unchecked") // we are the only ones adding them
    synchronized <T extends Pojo> PreparedStatementHolder<T> getStatementHolder(StatementDescriptor<T> desc) {
        return (PreparedStatementHolder<T>)preparedStmts.get(Objects.requireNonNull(desc));
    }
    
    /**
     * Adds a new {@link PreparedStatementHolder} into this
     * {@link PreparedStatementManager}. Adding an equal {@code targetDesc}
     * statement twice will yield the same returned id.
     * 
     * @param serverToken
     *            A server token used for creating a new {@link SharedStateId}
     *            if the target statement is not already tracked.
     * @param targetStmt
     *            The target statement to keep track of.
     * @param dataClass
     *            The data class of the target statement.
     * @param targetDesc
     *            The {@link StatementDescriptor} which was used for creating
     *            the target statement {@code targetStmt}
     * @return A unique ID identifying this statement. It's suitable to be
     *         shared between server and client.
     */
    synchronized <T extends Pojo> SharedStateId createAndPutHolder(
                                                             UUID serverToken,
                                                             PreparedStatement<T> targetStmt,
                                                             Class<T> dataClass,
                                                             StatementDescriptor<T> targetDesc) {
        // check if we have this descriptor already added
        @SuppressWarnings("unchecked")
        PreparedStatementHolder<T> holder = (PreparedStatementHolder<T>)preparedStmts.get(Objects.requireNonNull(targetDesc));
        if (holder != null) {
            // nothing to do
            assert( preparedStatementIds.get(holder.getId()) != null );
            return holder.getId();
        }
        // OK, must be a new statement we don't yet track
        SharedStateId id = new SharedStateId(currentPreparedStmtId, Objects.requireNonNull(serverToken));
        currentPreparedStmtId++;
        // There is nothing we can do other than using a long rather than an int
        // for the ID. That being said, having more than 2 billion *different* queries
        // seems more than unlikely. It may very well be a bug. Either way it
        // seems like a good idea to fail hard in order to be in the know about
        // this situation.
        if (currentPreparedStmtId == Integer.MAX_VALUE) {
            throw new IllegalStateException("Too many different statements!");
        }
        holder = new PreparedStatementHolder<>(id,
                                               Objects.requireNonNull(targetStmt),
                                               Objects.requireNonNull(dataClass),
                                               targetDesc);
        preparedStmts.put(targetDesc, holder);
        preparedStatementIds.put(id, holder);
        return id;
    }
}
