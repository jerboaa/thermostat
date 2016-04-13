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

package com.redhat.thermostat.notes.common.internal;

import com.redhat.thermostat.notes.common.HostNote;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.notes.common.NoteDAO;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.testutils.StatementDescriptorTester;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HostNoteDAOImplTest {

    private HostNoteDAOImpl dao;
    private Storage storage;
    private HostNote note;
    private HostRef ref;
    private PreparedStatement<HostNote> statement;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        storage = mock(Storage.class);
        dao = new HostNoteDAOImpl(storage);

        note = new HostNote();
        note.setAgentId("foo-agent");
        note.setTimeStamp(100L);
        note.setContent("note content");
        note.setId("host-note-01");

        ref = mock(HostRef.class);
        when(ref.getAgentId()).thenReturn("foo-agent");

        statement = mock(PreparedStatement.class);
        when(storage.prepareStatement(isA(StatementDescriptor.class))).thenReturn(statement);

        Cursor<HostNote> cursor = mock(Cursor.class);
        when(cursor.getBatchSize()).thenReturn(1);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(note).thenReturn(null);
        when(statement.executeQuery()).thenReturn(cursor);
    }

    @Test
    public void testLoggerIsProvided() {
        assertThat(dao.getLogger(), is(not(equalTo(null))));
    }

    @Test
    public void verifyCategoryIsRegistered() {
        verify(storage).registerCategory(HostNoteDAO.hostNotesCategory);
    }

    @Test
    public void verifyAggregateCategoryIsRegistered() {
        verify(storage).registerCategory(dao.aggregateCountCategory);
    }

    @Test
    public void testCategory() {
        Category<HostNote> category = HostNoteDAO.hostNotesCategory;
        assertNotNull(category);
        assertEquals("host-notes", category.getName());
        Collection<Key<?>> keys = category.getKeys();
        assertTrue(keys.contains(Key.AGENT_ID));
        assertTrue(keys.contains(NoteDAO.KEY_ID));
        assertTrue(keys.contains(Key.TIMESTAMP));
        assertTrue(keys.contains(NoteDAO.KEY_CONTENT));
        assertThat(keys.size(), is(4));
    }

    private void doParseTest(String descriptor) {
        StatementDescriptorTester<HostNote> tester = new StatementDescriptorTester<>();
        StatementDescriptor<HostNote> desc = new StatementDescriptor<>(HostNoteDAO.hostNotesCategory, descriptor);
        try {
            tester.testParseBasic(desc);
            tester.testParseSemantic(desc);
            // pass
        } catch (DescriptorParsingException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testCanParseAddHostNoteDescriptor() {
        doParseTest(HostNoteDAOImpl.ADD_HOST_NOTE);
    }

    @Test
    public void testCanParseQueryCountByAgentIdDescriptor() {
        doParseTest(HostNoteDAOImpl.QUERY_COUNT_HOST_NOTES_BY_AGENT_ID);
    }

    @Test
    public void testCanParseQueryNotesByAgentIdDescriptor() {
        doParseTest(HostNoteDAOImpl.QUERY_HOST_NOTES_BY_AGENT_ID);
    }

    @Test
    public void testCanParseQueryNoteByIdDescriptor() {
        doParseTest(HostNoteDAOImpl.QUERY_HOST_NOTE_BY_ID);
    }

    @Test
    public void testCanParseUpdateNoteDescriptor() {
        doParseTest(HostNoteDAOImpl.UPDATE_HOST_NOTE);
    }

    @Test
    public void testCanParseRemoveNoteByIdDescriptor() {
        doParseTest(HostNoteDAOImpl.REMOVE_HOST_NOTE_BY_ID);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void verifyAddNoteStorageAccess() throws Exception {
        dao.add(note);
        ArgumentCaptor<StatementDescriptor> statementCaptor = ArgumentCaptor.forClass(StatementDescriptor.class);
        verify(storage).prepareStatement(statementCaptor.capture());

        verify(statement).setString(0, note.getAgentId());
        verify(statement).setString(1, note.getId());
        verify(statement).setLong(2, note.getTimeStamp());
        verify(statement).setString(3, note.getContent());

        verify(statement).execute();
        StatementDescriptor desc = statementCaptor.getValue();
        assertThat(desc.getCategory(), is(equalTo(HostNoteDAO.hostNotesCategory)));
        assertThat(desc.getDescriptor(), is(HostNoteDAOImpl.ADD_HOST_NOTE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void verifyGetCountStorageAccess() throws Exception {
        PreparedStatement<AggregateCount> statement = mock(PreparedStatement.class);
        when(storage.prepareStatement(isA(StatementDescriptor.class))).thenReturn(statement);
        Cursor<AggregateCount> cursor = mock(Cursor.class);
        AggregateCount count = new AggregateCount();
        count.setCount(1);
        when(cursor.getBatchSize()).thenReturn(1);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(count).thenReturn(null);
        when(statement.executeQuery()).thenReturn(cursor);

        dao.getCount(ref);
        ArgumentCaptor<StatementDescriptor> statementCaptor = ArgumentCaptor.forClass(StatementDescriptor.class);
        verify(storage).prepareStatement(statementCaptor.capture());

        verify(statement).setString(0, ref.getAgentId());

        verify(statement).executeQuery();
        StatementDescriptor desc = statementCaptor.getValue();
        assertThat(desc.getCategory(), is(equalTo(dao.aggregateCountCategory)));
        assertThat(desc.getDescriptor(), is(HostNoteDAOImpl.QUERY_COUNT_HOST_NOTES_BY_AGENT_ID));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void verifyGetForStorageAccess() throws Exception {
        dao.getFor(ref);
        ArgumentCaptor<StatementDescriptor> statementCaptor = ArgumentCaptor.forClass(StatementDescriptor.class);
        verify(storage).prepareStatement(statementCaptor.capture());

        verify(statement).setString(0, ref.getAgentId());

        verify(statement).executeQuery();
        StatementDescriptor desc = statementCaptor.getValue();
        assertThat(desc.getCategory(), is(equalTo(HostNoteDAO.hostNotesCategory)));
        assertThat(desc.getDescriptor(), is(HostNoteDAOImpl.QUERY_HOST_NOTES_BY_AGENT_ID));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void verifyGetByIdStorageAccess() throws Exception {
        dao.getById(ref, note.getId());
        ArgumentCaptor<StatementDescriptor> statementCaptor = ArgumentCaptor.forClass(StatementDescriptor.class);
        verify(storage).prepareStatement(statementCaptor.capture());

        verify(statement).setString(0, ref.getAgentId());
        verify(statement).setString(1, note.getId());

        verify(statement).executeQuery();
        StatementDescriptor desc = statementCaptor.getValue();
        assertThat(desc.getCategory(), is(equalTo(HostNoteDAO.hostNotesCategory)));
        assertThat(desc.getDescriptor(), is(HostNoteDAOImpl.QUERY_HOST_NOTE_BY_ID));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void verifyUpdateStorageAccess() throws Exception {
        dao.update(note);
        ArgumentCaptor<StatementDescriptor> statementCaptor = ArgumentCaptor.forClass(StatementDescriptor.class);
        verify(storage).prepareStatement(statementCaptor.capture());

        verify(statement).setLong(0, note.getTimeStamp());
        verify(statement).setString(1, note.getContent());
        verify(statement).setString(2, note.getAgentId());
        verify(statement).setString(3, note.getId());

        verify(statement).execute();
        StatementDescriptor desc = statementCaptor.getValue();
        assertThat(desc.getCategory(), is(equalTo(HostNoteDAO.hostNotesCategory)));
        assertThat(desc.getDescriptor(), is(HostNoteDAOImpl.UPDATE_HOST_NOTE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void verifyRemoveByIdStorageAccess() throws Exception {
        dao.removeById(ref, note.getId());
        ArgumentCaptor<StatementDescriptor> statementCaptor = ArgumentCaptor.forClass(StatementDescriptor.class);
        verify(storage).prepareStatement(statementCaptor.capture());

        verify(statement).setString(0, ref.getAgentId());
        verify(statement).setString(1, note.getId());

        verify(statement).execute();
        StatementDescriptor desc = statementCaptor.getValue();
        assertThat(desc.getCategory(), is(equalTo(HostNoteDAO.hostNotesCategory)));
        assertThat(desc.getDescriptor(), is(HostNoteDAOImpl.REMOVE_HOST_NOTE_BY_ID));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void verifyRemoveStorageAccess() throws Exception {
        dao.remove(note);
        ArgumentCaptor<StatementDescriptor> statementCaptor = ArgumentCaptor.forClass(StatementDescriptor.class);
        verify(storage).prepareStatement(statementCaptor.capture());

        verify(statement).setString(0, ref.getAgentId());
        verify(statement).setString(1, note.getId());

        verify(statement).execute();
        StatementDescriptor desc = statementCaptor.getValue();
        assertThat(desc.getCategory(), is(equalTo(HostNoteDAO.hostNotesCategory)));
        assertThat(desc.getDescriptor(), is(HostNoteDAOImpl.REMOVE_HOST_NOTE_BY_ID));
    }

}
