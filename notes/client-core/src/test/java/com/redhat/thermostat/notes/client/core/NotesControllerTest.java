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

package com.redhat.thermostat.notes.client.core;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.notes.common.VmNote;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.testutils.StubExecutor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotesControllerTest {

    private Clock clock;
    private VmRef ref;
    private VmNoteDAO dao;
    private NotesView view;

    private StubNotesController controller;

    private ActionListener<NotesView.NoteAction> noteActionActionListener;
    private ActionListener<BasicView.Action> visibilityActionListener;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        clock = mock(Clock.class);
        ApplicationService appSvc = mock(ApplicationService.class);
        when(appSvc.getApplicationExecutor()).thenReturn(new StubExecutor());
        ref = mock(VmRef.class);
        TimerFactory timerFactory = mock(TimerFactory.class);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);
        // Implementation detail: NotesController creates two timers; the first for sync() and the second for autorefresh
        when(timerFactory.createTimer()).thenReturn(new SyncTimer()).thenReturn(new AutoRefreshTimer());
        dao = mock(VmNoteDAO.class);
        NotesViewProvider viewProvider = mock(NotesViewProvider.class);
        view = mock(NotesView.class);

        when(viewProvider.createView()).thenReturn(view);

        ArgumentCaptor<ActionListener> noteActionCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addNoteActionListener(noteActionCaptor.capture());

        ArgumentCaptor<ActionListener> visibilityActionCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(visibilityActionCaptor.capture());

        controller = new StubNotesController(clock, appSvc, ref, dao, viewProvider);

        noteActionActionListener = noteActionCaptor.getValue();
        visibilityActionListener = visibilityActionCaptor.getValue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void verifyControllerAddsNoteActionListener() {
        verify(view).addNoteActionListener(any(ActionListener.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void verifyControllerAddsVisibilityActionListener() {
        verify(view).addActionListener(any(ActionListener.class));
    }

    @Test
    public void testGetView() {
        assertThat((NotesView) controller.getView(), is(view));
    }

    @Test
    public void verifyRemoteRefreshNoteActionCausesDaoRead() {
        noteActionActionListener.actionPerformed(new ActionEvent<>(this, NotesView.NoteAction.REMOTE_REFRESH));
        InOrder inOrder = inOrder(view, dao, view);
        inOrder.verify(view).setBusy(true);
        inOrder.verify(dao).getFor(ref);
        // TODO: verify that the view is updated with expected mock values from DAO
        inOrder.verify(view).setBusy(false);
    }

    @Test
    public void verifyLocalAddSavesNotesThenAddsNewLocal() {
        noteActionActionListener.actionPerformed(new ActionEvent<>(this, NotesView.NoteAction.LOCAL_ADD));
        InOrder inOrder = inOrder(view, dao, view, clock, view);

        // remote save
        inOrder.verify(view).setBusy(true);
        // TODO: add mock note data to DAO and controller, verify correct sync
        inOrder.verify(dao).getFor(ref);
        inOrder.verify(view).setBusy(false);
        inOrder.verify(view).setBusy(true);

        // local add new
        inOrder.verify(clock, times(3)).getRealTimeMillis();
        inOrder.verify(view).add(controller.mockNote);
    }

    @Test
    public void verifyLocalSaveUpdatesLocalModel() {
        when(controller.mockNote.getTimeStamp()).thenReturn(100L);
        when(controller.mockNote.getId()).thenReturn("foo-noteid");
        when(controller.mockNote.getContent()).thenReturn("content");
        when(clock.getRealTimeMillis()).thenReturn(150L);
        controller.localAddNewNote();
        ActionEvent<NotesView.NoteAction> actionEvent = new ActionEvent<>(this, NotesView.NoteAction.LOCAL_SAVE);
        actionEvent.setPayload(controller.mockNote.getId());
        noteActionActionListener.actionPerformed(actionEvent);
        verify(clock, times(2)).getRealTimeMillis();
        verify(controller.mockNote).setTimeStamp(clock.getRealTimeMillis());
    }

    @Test
    public void verifyLocalDeleteRemovesLocalModelThenSyncsDao() {
        when(controller.mockNote.getId()).thenReturn("1");
        ActionEvent<NotesView.NoteAction> actionEvent = new ActionEvent<>(this, NotesView.NoteAction.LOCAL_DELETE);
        actionEvent.setPayload(controller.mockNote.getId());
        controller.localAddNewNote();
        noteActionActionListener.actionPerformed(actionEvent);

        InOrder inOrder = inOrder(view, dao, view, view);
        // remote save
        inOrder.verify(view).setBusy(true);
        // TODO: add mock note data to DAO and controller, verify correct sync
        inOrder.verify(dao).getFor(ref);
        inOrder.verify(view).setBusy(false);
    }

    @Test
    public void verifyVisibilityHiddenTriggersRemoteSave() {
        visibilityActionListener.actionPerformed(new ActionEvent<>(this, BasicView.Action.HIDDEN));
        InOrder inOrder = inOrder(view, dao, view);
        // remote save
        inOrder.verify(view).setBusy(true);
        // TODO: add mock note data to DAO and controller, verify correct sync
        inOrder.verify(dao).getFor(ref);
        inOrder.verify(view).setBusy(false);
    }

    @Test
    public void verifyVisibilityVisibleTriggersRemoteRefresh() {
        visibilityActionListener.actionPerformed(new ActionEvent<>(this, BasicView.Action.VISIBLE));
        InOrder inOrder = inOrder(view, dao, view);
        inOrder.verify(view).setBusy(true);
        inOrder.verify(dao).getFor(ref);
        // TODO: verify that the view is updated with expected mock values from DAO
        inOrder.verify(view).setBusy(false);
    }

    @Test
    public void testCreatesCopyIfStorageAlreadyHasNewNote() {
        when(dao.getFor(ref)).thenReturn(Collections.singletonList(controller.mockNote));
        controller.localAddNewNote();
        assertThat(controller.createCount, is(1));
        assertThat(controller.copyCount, is(0));
        controller.sendLocalChangesToStorage();
        assertThat(controller.createCount, is(1));
        assertThat(controller.copyCount, is(1));
    }

    @Test
    public void testCreatesCopyIfInFlightUpdateOccurrs() {
        when(dao.getFor(ref)).thenReturn(Collections.singletonList(controller.mockNote));

        controller.localAddNewNote();
        controller.localSaveNote(controller.mockNote.getId());
        assertThat(controller.createCount, is(1));
        assertThat(controller.copyCount, is(0));

        controller.sendLocalChangesToStorage();

        assertThat(controller.createCount, is(1));
        assertThat(controller.copyCount, is(1));

    }

    private static class StubNotesController extends NotesController<VmRef, VmNote, VmNoteDAO> {

        int createCount = 0;
        VmNote mockNote;
        int copyCount = 0;
        VmNote copyNote;

        public StubNotesController(Clock clock, ApplicationService appSvc, VmRef ref, VmNoteDAO dao, NotesViewProvider viewProvider) {
            super(clock, appSvc, ref, dao, viewProvider);
            mockNote = mock(VmNote.class);
            when(mockNote.getId()).thenReturn("mock");
            when(mockNote.getContent()).thenReturn("mock");
            when(mockNote.getTimeStamp()).thenReturn(100L);
            when(mockNote.getVmId()).thenReturn("mockvm");
            copyNote = mock(VmNote.class);
            when(copyNote.getId()).thenReturn("copy");
            when(copyNote.getContent()).thenReturn("copy");
            when(copyNote.getTimeStamp()).thenReturn(200L);
            when(copyNote.getVmId()).thenReturn("copyvm");
        }

        @Override
        protected VmNote createNewNote(long timeStamp, String content) {
            createCount++;
            return mockNote;
        }

        @Override
        protected VmNote copyNote(VmNote note) {
            copyCount++;
            return copyNote;
        }
    }

    private static abstract class AbstractTimer implements Timer {

        protected Runnable action;

        @Override
        public void stop() {
        }

        @Override
        public void setAction(Runnable action) {
            this.action = action;
        }

        @Override
        public void setInitialDelay(long initialDelay) {
        }

        @Override
        public void setDelay(long period) {
        }

        @Override
        public void setSchedulingType(SchedulingType schedulingType) {
        }

        @Override
        public void setTimeUnit(TimeUnit timeUnit) {
        }
    }

    private static class SyncTimer extends AbstractTimer {
        @Override
        public void start() {
            // Implementation detail: run twice because the NotesController SyncTask expects to run at least twice
            if (action != null) {
                action.run();
                action.run();
            }
        }
    }

    private static class AutoRefreshTimer extends AbstractTimer {
        @Override
        public void start() {
            if (action != null) {
                action.run();
            }
        }
    }

}
