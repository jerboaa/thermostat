/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.storage.dao;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.model.Pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.redhat.thermostat.storage.internal.dao.LoggingUtil.logDescriptorParsingException;
import static com.redhat.thermostat.storage.internal.dao.LoggingUtil.logStatementExecutionException;

public abstract class AbstractDao {

    final <T extends Pojo> PreparedStatement<T> getCustomizedPreparedStatement(DaoOperation<T> daoOperation) throws DescriptorParsingException {
        PreparedStatement<T> preparedStatement = daoOperation.getStorage()
                .prepareStatement(daoOperation.getStatementDescriptor());
        return daoOperation.customize(preparedStatement);
    }

    protected final <T extends Pojo> StatementResult<T> executeStatement(DaoStatement<T> daoOperation) {
        List<Exception> exceptions = new ArrayList<>();
        try {
            getCustomizedPreparedStatement(daoOperation).execute();
        } catch (DescriptorParsingException e) {
            logDescriptorParsingException(getLogger(), daoOperation.getStatementDescriptor(), e);
            exceptions.add(e);
        } catch (StatementExecutionException e) {
            logStatementExecutionException(getLogger(), daoOperation.getStatementDescriptor(), e);
            exceptions.add(e);
        }

        StatementResult<T> statementResult = new StatementResult<>();
        statementResult.addExceptions(exceptions);
        return statementResult;
    }

    protected final <T extends Pojo> QueryResult<T> executeQuery(DaoOperation<T> daoOperation) {
        List<Exception> exceptions = new ArrayList<>();
        Cursor<T> cursor = getEmptyResultCursor();
        try {
            cursor = getCustomizedPreparedStatement(daoOperation).executeQuery();
        } catch (DescriptorParsingException e) {
            logDescriptorParsingException(getLogger(), daoOperation.getStatementDescriptor(), e);
            exceptions.add(e);
        } catch (StatementExecutionException e) {
            logStatementExecutionException(getLogger(), daoOperation.getStatementDescriptor(), e);
            exceptions.add(e);
        }

        QueryResult<T> queryResult = new QueryResult<>(cursor);
        queryResult.addExceptions(exceptions);
        return queryResult;
    }

    protected abstract Logger getLogger();

    protected static <T extends Pojo> Cursor<T> getEmptyResultCursor() {
        return new Cursor<T>() {
            @Override
            public void setBatchSize(int n) throws IllegalArgumentException {
            }

            @Override
            public int getBatchSize() {
                return 0;
            }

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public T next() {
                return null;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
