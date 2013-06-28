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

import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Statement;
import com.redhat.thermostat.storage.query.Expression;

/**
 * Result object as returned by {@link StatementDescriptorParser#parse()}.
 * An instance of this plus an {@link PreparedStatementImpl} instance should
 * be sufficient to patch up a prepared statement with its real values.
 *
 * @see PreparedStatementImpl#executeQuery()
 */
class ParsedStatement {

    private int numParams;
    private SuffixExpression suffixExpn;
    private final Statement statement;
    
    ParsedStatement(Statement statement) {
        this.statement = statement;
    }
    
    public int getNumParams() {
        return numParams;
    }

    public Statement getRawStatement() {
        return statement;
    }
    
    public Statement patchQuery(PreparedStatementImpl stmt) throws IllegalPatchException {
        if (suffixExpn == null) {
            String msg = "Suffix expression must be set before patching!";
            IllegalStateException expn = new IllegalStateException(msg);
            throw new IllegalPatchException(expn);
        }
        PreparedParameter[] params = stmt.getParams();
        patchWhere(params);
        patchSort(params);
        patchLimit(params);
        // TODO count actual patches and throw an exception if not all vars
        // have been patched up.
        return statement;
    }

    private void patchLimit(PreparedParameter[] params) throws IllegalPatchException {
        LimitExpression expn = suffixExpn.getLimitExpn();
        if (expn == null) {
            // no limit expn, nothing to do
            return;
        }
        PatchedLimitExpression patchedExp = expn.patch(params);
        if (statement instanceof Query<?>) {
            Query<?> query = (Query<?>)statement;
            query.limit(patchedExp.getLimitValue());
        } else {
            String msg = "Patching of non-query types not (yet) supported! Class was:"
                    + statement.getClass().getName();
            IllegalStateException invalid = new IllegalStateException(msg);
            throw new IllegalPatchException(invalid);
        }
    }

    private void patchSort(PreparedParameter[] params) throws IllegalPatchException {
        SortExpression expn = suffixExpn.getSortExpn();
        if (expn == null) {
            // no sort expn, nothing to do
            return;
        }
        PatchedSortExpression patchedExp = expn.patch(params);
        if (statement instanceof Query<?>) {
            Query<?> query = (Query<?>)statement;
            PatchedSortMember[] members = patchedExp.getSortMembers();
            for (int i = 0; i < members.length; i++) {
                query.sort(members[i].getSortKey(), members[i].getDirection());
            }
        } else {
            String msg = "Patching of non-query types not (yet) supported! Class was:"
                    + statement.getClass().getName();
            IllegalStateException invalid = new IllegalStateException(msg);
            throw new IllegalPatchException(invalid);
        }
    }

    private void patchWhere(PreparedParameter[] params) throws IllegalPatchException {
        WhereExpression expn = suffixExpn.getWhereExpn();
        if (expn == null) {
            // no where, nothing to do
            return;
        }
        // walk the tree, create actual expressions and patch values along
        // the way.
        PatchedWhereExpression patchedExp = expn.patch(params);
        Expression whereClause = patchedExp.getExpression();
        if (statement instanceof Query<?>) {
            Query<?> query = (Query<?>)statement;
            query.where(whereClause);
        } else {
            String msg = "Patching of non-query types not (yet) supported! Class was:"
                    + statement.getClass().getName();
            IllegalStateException invalid = new IllegalStateException(msg);
            throw new IllegalPatchException(invalid);
        }
    }

    public void setNumFreeParams(int num) {
        this.numParams = num;
    }

    public void setSuffixExpression(SuffixExpression tree) {
        this.suffixExpn = tree;
    }

    public SuffixExpression getSuffixExpression() {
        return suffixExpn;
    }

}
