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

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AbstractNoteTest<T extends AbstractNote> {

    protected T note;

    @Before
    public void setup() {
        note = createNewNote();
    }

    protected T createNewNote() {
        return (T) new TestNote();
    }

    @Test(expected = NullPointerException.class)
    public void testNullContentNotAccepted() {
        note.setContent(null);
    }

    @Test
    public void testInitialContentIsEmptyString() {
        assertThat(note.getContent(), is(""));
    }

    @Test
    public void testSetGetContent() {
        String content = "content";
        note.setContent(content);
        assertThat(note.getContent(), is(content));
    }

    @Test(expected = NullPointerException.class)
    public void testNullIdNotAccepted() {
        note.setId(null);
    }

    @Test
    public void testInitialIdIsEmptyString() {
        assertThat(note.getId(), is(""));
    }

    @Test
    public void testSetGetId() {
        String id = "id";
        note.setId(id);
        assertThat(note.getId(), is(id));
    }

    @Test
    public void testInitialTimestampIsZero() {
        assertThat(note.getTimeStamp(), is(0L));
    }

    @Test
    public void testSetGetTimestamp() {
        long timestmap = 100L;
        note.setTimeStamp(timestmap);
        assertThat(note.getTimeStamp(), is(timestmap));
    }

    private static class TestNote extends AbstractNote {
    }

}
