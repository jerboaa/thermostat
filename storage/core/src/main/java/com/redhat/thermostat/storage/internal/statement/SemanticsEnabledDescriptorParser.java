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

package com.redhat.thermostat.storage.internal.statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.AggregateQuery;
import com.redhat.thermostat.storage.core.BackingStorage;
import com.redhat.thermostat.storage.core.DataModifyingStatement;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.ParsedStatement;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.Statement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.model.Pojo;

class SemanticsEnabledDescriptorParser<T extends Pojo> extends
        BasicDescriptorParser<T> {

    SemanticsEnabledDescriptorParser(BackingStorage storage, StatementDescriptor<T> desc) {
        super(storage, desc);
    }
    
    public ParsedStatement<T> parse() throws DescriptorParsingException {
        ParsedStatementImpl<T> parsed = (ParsedStatementImpl<T>)super.parse();
        doSemanticAnalysis(parsed);
        return parsed;
    }
    
    private void doSemanticAnalysis(ParsedStatementImpl<T> parsed) throws DescriptorParsingException {
        Statement<T> stmt = parsed.getRawStatement();
        // statement should never be null
        Objects.requireNonNull(stmt);
        if (stmt instanceof Add) {
            // ADD don't take a WHERE
            if (tree.getWhereExpn() != null) {
                String msg = "WHERE clause not allowed for ADD";
                throw new DescriptorParsingException(msg);
            }
            // ADD requires all keys to be specified in the desc
            ensureAllKeysSpecified();
        }
        if (stmt instanceof Replace) {
            // REPLACE requires a WHERE
            if (tree.getWhereExpn() == null) {
                String msg = "WHERE clause required for REPLACE";
                throw new DescriptorParsingException(msg);
            }
            // REPLACE requires all keys to be specified in the desc
            ensureAllKeysSpecified();
        }
        if (stmt instanceof Update) {
            // WHERE required for UPDATE
            if (tree.getWhereExpn() == null) {
                String msg = "WHERE clause required for UPDATE";
                throw new DescriptorParsingException(msg);
            }
            // SET required for UPDATE
            if (setList.getValues().size() == 0) {
                String msg = "SET list required for UPDATE";
                throw new DescriptorParsingException(msg);
            }
            ensureKeyInSetIsKnown();
        }
        if (stmt instanceof Remove && setList.getValues().size() > 0) {
            String msg = "SET not allowed for REMOVE";
            throw new DescriptorParsingException(msg);
        }
        // matches for QUERY/QUERY-COUNT/QUERY-DISTINCT
        if (stmt instanceof Query) {
            if (setList.getValues().size() > 0) {
                // Must not have SET for QUERYs
                String msg = "SET not allowed for QUERY/QUERY-COUNT";
                throw new DescriptorParsingException(msg);
            }
            if (stmt instanceof AggregateQuery) {
                AggregateQuery<T> aggQuery = (AggregateQuery<T>)stmt;
                switch (aggQuery.getAggregateFunction()) {
                case COUNT:
                    // count queries need a sane key param if present
                    performKeyParamChecksAllowNull(aggQuery);
                    break;
                case DISTINCT:
                    // distinct queries must have a known key
                    performKeyParamChecks(aggQuery);
                    break;
                default:
                    throw new IllegalStateException("Unknown aggregate function: " + aggQuery.getAggregateFunction());
                }
            }
        } else {
            assert(stmt instanceof DataModifyingStatement);
            // only queries can have sort/limit expressions
            if (this.tree.getLimitExpn() != null || this.tree.getSortExpn() != null) {
                String msg = "LIMIT/SORT only allowed for QUERY/QUERY-COUNT";
                throw new DescriptorParsingException(msg);
            }
        }
    }

    private void performKeyParamChecksAllowNull(AggregateQuery<T> aggQuery) throws DescriptorParsingException {
        if (aggQuery.getAggregateKey() != null) {
            performKeyParamChecks(aggQuery);
        }
    }

    private void performKeyParamChecks(AggregateQuery<T> aggQuery) throws DescriptorParsingException {
        Key<?> optionalKey = aggQuery.getAggregateKey();
        if (optionalKey == null) {
            throw new DescriptorParsingException("Aggregate key for " 
                       + aggQuery.getAggregateFunction() + " must not be null.");
        }
        // non-null case
        String name = optionalKey.getName();
        Key<?> aggKey = desc.getCategory().getKey(name);
        if (aggKey == null) {
            throw new DescriptorParsingException("Unknown aggregate key '" + name + "'");
        }
    }

    private void ensureKeyInSetIsKnown() throws DescriptorParsingException {
        // retrieve the expected keys list from the category
        Collection<Key<?>> keys = desc.getCategory().getKeys();
        Set<Key<?>> expectedSet = new HashSet<>(keys.size());
        expectedSet.addAll(keys);
        List<String> unknownKeys = new ArrayList<>();
        try {
            for (SetListValue val: setList.getValues()) {
                // this may throw CCE if LHS is a free parameter. That's not
                // allowed though.
                Key<?> key = (Key<?>)val.getKey().getValue();
                if (!expectedSet.contains(key)) {
                    unknownKeys.add(key.getName());
                }
            }
        } catch (ClassCastException e) {
            // LHS of set pair a free variable, which isn't allowed
            String msg = "LHS of set list pair must not be a free variable.";
            throw new DescriptorParsingException(msg);
        }
        if (!unknownKeys.isEmpty()) {
            String msg = "Unknown key(s) in SET: '" + unknownKeys + "'";
            throw new DescriptorParsingException(msg);
        }
    }

    private void ensureAllKeysSpecified() throws DescriptorParsingException {
        // retrieve the expected keys list from the category
        Collection<Key<?>> keys = desc.getCategory().getKeys();
        Set<Key<?>> expectedSet = new HashSet<>(keys.size());
        expectedSet.addAll(keys);
        Set<Key<?>> keysInSetList = new HashSet<>(keys.size());
        try {
            for (SetListValue val: setList.getValues()) {
                // this may throw CCE
                Key<?> key = (Key<?>)val.getKey().getValue();
                keysInSetList.add(key);
            }
        } catch (ClassCastException e) {
            // LHS of set pair a free variable, which isn't allowed
            String msg = "LHS of set list pair must not be a free variable.";
            throw new DescriptorParsingException(msg);
        }
        if (!keysInSetList.equals(expectedSet)) {
            String msg = "Keys don't match keys in category. Expected the following keys: " + expectedSet + " got " + keysInSetList;
            throw new DescriptorParsingException(msg);
        };
    }
}

