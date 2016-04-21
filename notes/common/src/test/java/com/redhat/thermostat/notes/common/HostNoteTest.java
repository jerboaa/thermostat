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

package com.redhat.thermostat.notes.common;

import com.redhat.thermostat.notes.common.internal.AbstractNoteTest;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class HostNoteTest extends AbstractNoteTest<HostNote> {

    @Override
    protected HostNote createNewNote() {
        return new HostNote();
    }

    @Test
    public void testEquals() {
        HostNote note2 = new HostNote();
        assertThat(note, is(equalTo(note2)));
    }

    @Test
    public void testEquals2() {
        note.setId("id");
        note.setTimeStamp(100L);
        note.setContent("content");
        HostNote note2 = new HostNote();
        assertThat(note, is(not(equalTo(note2))));
    }

    @Test
    public void testEquals3() {
        note.setId("id");
        note.setTimeStamp(100L);
        note.setContent("content");
        HostNote note2 = new HostNote();
        note.setId(note.getId());
        assertThat(note, is(not(equalTo(note2))));
    }

    @Test
    public void testEquals4() {
        note.setId("id");
        note.setTimeStamp(100L);
        note.setContent("content");
        HostNote note2 = new HostNote();
        note2.setTimeStamp(note.getTimeStamp());
        assertThat(note, is(not(equalTo(note2))));
    }

    @Test
    public void testEquals5() {
        note.setId("id");
        note.setTimeStamp(100L);
        note.setContent("content");
        HostNote note2 = new HostNote();
        note2.setContent(note.getContent());
        assertThat(note, is(not(equalTo(note2))));
    }

    @Test
    public void testEquals6() {
        note.setId("id");
        note.setTimeStamp(100L);
        note.setContent("content");
        HostNote note2 = new HostNote();
        note2.setId(note.getId());
        note2.setTimeStamp(note.getTimeStamp());
        assertThat(note, is(not(equalTo(note2))));
    }

    @Test
    public void testEquals7() {
        note.setId("id");
        note.setTimeStamp(100L);
        note.setContent("content");
        HostNote note2 = new HostNote();
        note2.setId(note.getId());
        note2.setContent(note.getContent());
        assertThat(note, is(not(equalTo(note2))));
    }

    @Test
    public void testEquals8() {
        note.setId("id");
        note.setTimeStamp(100L);
        note.setContent("content");
        HostNote note2 = new HostNote();
        note2.setTimeStamp(note.getTimeStamp());
        note2.setContent(note.getContent());
        assertThat(note, is(not(equalTo(note2))));
    }

    @Test
    public void testEquals9() {
        note.setId("id");
        note.setTimeStamp(100L);
        note.setContent("content");
        HostNote note2 = new HostNote();
        note2.setId(note.getId());
        note2.setTimeStamp(note.getTimeStamp());
        note2.setContent(note.getContent());
        assertThat(note, is(equalTo(note2)));
        assertThat(note.hashCode(), is(note2.hashCode()));
    }

}
