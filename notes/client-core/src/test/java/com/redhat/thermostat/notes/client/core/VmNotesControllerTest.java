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

package com.redhat.thermostat.notes.client.core;

import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.notes.common.VmNote;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VmNotesControllerTest {

    private Clock clock;
    private ApplicationService appSvc;
    private VmNoteDAO dao;
    private NotesViewProvider viewProvider;

    private VmRef ref;

    @Before
    public void setup() {
        clock = mock(Clock.class);
        appSvc = mock(ApplicationService.class);
        TimerFactory timerFactory = mock(TimerFactory.class);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);
        Timer timer = mock(Timer.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        dao = mock(VmNoteDAO.class);
        viewProvider = mock(NotesViewProvider.class);
        when(viewProvider.createView()).thenReturn(mock(NotesView.class));

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("foo-agent");
        ref = mock(VmRef.class);
        when(ref.getHostRef()).thenReturn(hostRef);
        when(ref.getVmId()).thenReturn("vmId");
    }

    @Test
    public void testTimestamp() {
        VmNotesController controller = new VmNotesController(clock, appSvc, dao, ref, viewProvider);
        VmNote note = controller.createNewNote(100l, "foo");
        assertThat(note.getTimeStamp(), is(equalTo(100l)));
    }

    @Test
    public void testContent() {
        VmNotesController controller = new VmNotesController(clock, appSvc, dao, ref, viewProvider);
        VmNote note = controller.createNewNote(100l, "foo");
        assertThat(note.getContent(), is(equalTo("foo")));
    }

    @Test
    public void testAgentId() {
        VmNotesController controller = new VmNotesController(clock, appSvc, dao, ref, viewProvider);
        VmNote note = controller.createNewNote(100l, "foo");
        assertThat(note.getAgentId(), is(ref.getHostRef().getAgentId()));
    }

    @Test
    public void verifyIdIsValidUuid() {
        VmNotesController controller = new VmNotesController(clock, appSvc, dao, ref, viewProvider);
        VmNote note = controller.createNewNote(100l, "foo");
        try {
            UUID.fromString(note.getId());
        } catch (IllegalArgumentException e) {
            fail();
        }
    }

}
