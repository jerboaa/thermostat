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

package com.redhat.thermostat.storage.internal.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.SchemaInfo;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.model.SchemaInformation;

public class SchemaInfoDAOTest {

    private SchemaInformation schemaInfo;
    private SchemaInformation schemaInfo2;
    
    @Before
    public void setUp() {
        schemaInfo = new SchemaInformation();
        schemaInfo.setCategoryName("test SchemaInfo 1");
        
        schemaInfo2 = new SchemaInformation();
        schemaInfo2.setCategoryName("test SchemaInfo 2");
    }

    @Test
    public void preparedQueryDescriptorsAreSane() {
        String expectedAllCollections = "QUERY schema-info";
        assertEquals(expectedAllCollections, SchemaInfoDAOImpl.QUERY_ALL_COLLECTIONS);
    }
    
    @Test
    public void verifyCategoryName() {
        Category<SchemaInformation> category = SchemaInfo.CATEGORY;
        assertEquals("schema-info", category.getName());
    }

    @Test
    public void verifyKeyNames() {
        assertEquals("timeStamp", Key.TIMESTAMP.getName());
        assertEquals("categoryName", SchemaInfo.NAME.getName());
    }
    
    @Test
    public void verifyCategoryHasAllKeys() {
        Collection<Key<?>> keys = SchemaInfo.CATEGORY.getKeys();
        
        assertEquals(2, keys.size());
        assertTrue(keys.contains(SchemaInfo.NAME));
        assertTrue(keys.contains(Key.TIMESTAMP));

    } 

    @Test
    public void verifyGetSchemaInfos()
            throws StatementExecutionException, DescriptorParsingException{
        @SuppressWarnings("unchecked")
        Cursor<SchemaInformation> schemaCursor = (Cursor<SchemaInformation>) mock(Cursor.class);
        Storage storage = mock(Storage.class);
        
        @SuppressWarnings("unchecked")
        PreparedStatement<SchemaInformation> stmt = (PreparedStatement<SchemaInformation>) mock(PreparedStatement.class);
        ArgumentCaptor<StatementDescriptor> messageCaptor = ArgumentCaptor.forClass(StatementDescriptor.class);
        SchemaInfoDAOImpl dao = new SchemaInfoDAOImpl(storage);
        when(storage.prepareStatement(messageCaptor.capture())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(schemaCursor);
        
        // 0 elements into DB
        when(schemaCursor.hasNext()).thenReturn(false);
        when(schemaCursor.next()).thenReturn(null);
        List<SchemaInformation> listSchemaInfo = dao.getSchemaInfos();
        assertEquals(0, listSchemaInfo.size());
        
        @SuppressWarnings("unchecked")
        StatementDescriptor<SchemaInformation> arg = messageCaptor.getValue();
        
        assertEquals(SchemaInfoDAOImpl.QUERY_ALL_COLLECTIONS, arg.getDescriptor());
        
        // One element into DB
        when(schemaCursor.hasNext()).thenReturn(true).thenReturn(false);
        when(schemaCursor.next()).thenReturn(schemaInfo).thenReturn(null);
        listSchemaInfo = dao.getSchemaInfos();
        assertEquals(1, listSchemaInfo.size());
        
        SchemaInformation result = listSchemaInfo.get(0);
        assertEquals(schemaInfo, result);
        
        // More elements into DB
        when(schemaCursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(schemaCursor.next()).thenReturn(schemaInfo).thenReturn(schemaInfo).thenReturn(schemaInfo2).thenReturn(null);

        listSchemaInfo = dao.getSchemaInfos();
        assertEquals(3, listSchemaInfo.size());
        
        result = listSchemaInfo.get(2);
        assertEquals(schemaInfo2, result);
    }
    
}