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

package com.redhat.thermostat.storage.dao;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.model.VmInfo;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractDaoTest {

    private static final List<VmInfo> MOCK_RESULTS = Arrays.asList(mock(VmInfo.class), mock(VmInfo.class));

    @Test
    public void testGetEmptyResultCursor() {
        Cursor<VmInfo> cursor = AbstractDao.getEmptyResultCursor();
        assertThat(cursor.hasNext(), is(false));
        assertThat(cursor.next(), is(equalTo(null)));
        assertThat(cursor.getBatchSize(), is(0));
    }

    @Test
    public void testGetCustomizedPreparedStatement() throws DescriptorParsingException {
        Storage storage = mock(Storage.class);
        Category<VmInfo> category = VmInfoDAO.vmInfoCategory;
        String descriptor = "descriptor";
        @SuppressWarnings("unchecked")
        final PreparedStatement<VmInfo> DUMMY_STATEMENT = (PreparedStatement<VmInfo>) mock(PreparedStatement.class);
        DaoOperation<VmInfo> daoOperation = new AbstractDaoStatement<VmInfo>(storage, category, descriptor) {
            @Override
            public PreparedStatement<VmInfo> customize(PreparedStatement<VmInfo> preparedStatement) {
                return DUMMY_STATEMENT;
            }
        };
        AbstractDao abstractDao = getAbstractDao();

        assertThat(abstractDao.getCustomizedPreparedStatement(daoOperation), is(equalTo(DUMMY_STATEMENT)));
    }

    @Test @SuppressWarnings("unchecked")
    public void testExecuteStatement() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        PreparedStatement<VmInfo> statement = (PreparedStatement<VmInfo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(statement);
        Cursor<VmInfo> sentinelCursor = getSentinelCursor();
        when(statement.executeQuery()).thenReturn(sentinelCursor);

        Category<VmInfo> category = VmInfoDAO.vmInfoCategory;
        String descriptor = "descriptor";
        DaoStatement<VmInfo> daoOperation = new SimpleDaoStatement<>(storage, category, descriptor);
        AbstractDao abstractDao = getAbstractDao();
        StatementResult<VmInfo> statementResult = abstractDao.executeStatement(daoOperation);
        assertThat(statementResult.hasExceptions(), is(false));
    }

    @Test @SuppressWarnings("unchecked")
    public void testExecuteQuery() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        PreparedStatement<VmInfo> statement = (PreparedStatement<VmInfo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(statement);
        Cursor<VmInfo> sentinelCursor = getSentinelCursor();
        when(statement.executeQuery()).thenReturn(sentinelCursor);

        Category<VmInfo> category = VmInfoDAO.vmInfoCategory;
        String descriptor = "descriptor";
        DaoQuery<VmInfo> daoOperation = new SimpleDaoQuery<>(storage, category, descriptor);
        AbstractDao abstractDao = getAbstractDao();
        QueryResult<VmInfo> queryResult = abstractDao.executeQuery(daoOperation);
        assertThat(queryResult.hasExceptions(), is(false));
        assertEquals(MOCK_RESULTS, queryResult.asList());
    }

    private Cursor<VmInfo> getSentinelCursor() {
        return new Cursor<VmInfo>() {

            private final Iterator<VmInfo> iterator = MOCK_RESULTS.iterator();

            @Override
            public void setBatchSize(int n) throws IllegalArgumentException {
            }

            @Override
            public int getBatchSize() {
                return 0;
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public VmInfo next() {
                return iterator.next();
            }
        };
    }

    private AbstractDao getAbstractDao() {
        return new AbstractDao() {
            @Override
            protected Logger getLogger() {
                return LoggingUtils.getLogger(AbstractDaoTest.this.getClass());
            }
        };
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<VmInfo> anyDescriptor() {
        return (StatementDescriptor<VmInfo>) any(StatementDescriptor.class);
    }
}
