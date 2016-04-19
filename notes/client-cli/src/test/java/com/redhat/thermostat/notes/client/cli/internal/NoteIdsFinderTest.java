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

package com.redhat.thermostat.notes.client.cli.internal;

import com.redhat.thermostat.common.cli.CompletionInfo;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.notes.common.HostNote;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.notes.common.Note;
import com.redhat.thermostat.notes.common.VmNote;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NoteIdsFinderTest {

    private DependencyServices dependencyServices;
    private HostNoteDAO hostDao;
    private VmNoteDAO vmDao;
    private NoteIdsFinder finder;
    private HostNote hostNote1;
    private HostNote hostNote2;
    private VmNote vmNote1;
    private VmNote vmNote2;

    @Before
    public void setup() {
        dependencyServices = mock(DependencyServices.class);
        hostDao = mock(HostNoteDAO.class);
        vmDao = mock(VmNoteDAO.class);
        when(dependencyServices.hasService(HostNoteDAO.class)).thenReturn(true);
        when(dependencyServices.getService(HostNoteDAO.class)).thenReturn(hostDao);
        when(dependencyServices.hasService(VmNoteDAO.class)).thenReturn(true);
        when(dependencyServices.getService(VmNoteDAO.class)).thenReturn(vmDao);

        hostNote1 = new HostNote();
        hostNote1.setId("host-note-id-01");
        hostNote1.setContent("host note content 1");
        hostNote1.setAgentId("foo-agent");
        hostNote1.setTimeStamp(100L);

        hostNote2 = new HostNote();
        hostNote2.setId("host-note-id-02");
        hostNote2.setContent("host note content 2");
        hostNote2.setAgentId("foo-agent");
        hostNote2.setTimeStamp(150L);

        when(hostDao.getAll()).thenReturn(Arrays.asList(hostNote1, hostNote2));

        vmNote1 = new VmNote();
        vmNote1.setId("vm-note-id-01");
        vmNote1.setContent("vm note content 1");
        vmNote1.setAgentId("foo-agent");
        vmNote1.setVmId("foo-vm");
        vmNote1.setTimeStamp(200L);

        vmNote2 = new VmNote();
        vmNote2.setId("vm-note-id-02");
        vmNote2.setContent("vm note content 2");
        vmNote2.setAgentId("foo-agent");
        vmNote2.setVmId("foo-vm");
        vmNote2.setTimeStamp(250L);

        when(vmDao.getAll()).thenReturn(Arrays.asList(vmNote1, vmNote2));

        finder = new NoteIdsFinder(dependencyServices);
    }

    @Test
    public void testNumberOfResults() {
        List<CompletionInfo> infos = finder.findCompletions();
        assertThat(infos.size(), is(4));
    }

    @Test
    public void testReturnsVmListWhenNoInfoFromHostDao() {
        when(hostDao.getAll()).thenReturn(Collections.<HostNote>emptyList());
        List<CompletionInfo> infos = finder.findCompletions();
        assertThat(infos, is(equalTo(convertNotesToInfos(vmDao.getAll()))));
    }

    @Test
    public void testReturnsHostListWhenNoInfoFromVmDao() {
        when(vmDao.getAll()).thenReturn(Collections.<VmNote>emptyList());
        List<CompletionInfo> infos = finder.findCompletions();
        assertThat(infos, is(equalTo(convertNotesToInfos(hostDao.getAll()))));
    }

    @Test
    public void testReturnsEmptyListWhenNoInfoFromEitherDao() {
        when(vmDao.getAll()).thenReturn(Collections.<VmNote>emptyList());
        when(hostDao.getAll()).thenReturn(Collections.<HostNote>emptyList());
        List<CompletionInfo> infos = finder.findCompletions();
        assertThat(infos, is(equalTo(Collections.<CompletionInfo>emptyList())));
    }

    @Test
    public void testResultFormat() {
        List<CompletionInfo> infos = new ArrayList<>(finder.findCompletions());
        Collections.sort(infos, new Comparator<CompletionInfo>() {
            @Override
            public int compare(CompletionInfo a, CompletionInfo b) {
                return String.CASE_INSENSITIVE_ORDER.compare(a.getCompletionWithUserVisibleText(),
                        b.getCompletionWithUserVisibleText());
            }
        });
        CompletionInfo info = infos.get(0);

        assertThat(info.getActualCompletion(), is("host-note-id-01"));
        assertThat(info.getUserVisibleText(), is("host note content 1"));
        assertThat(info.getCompletionWithUserVisibleText(), is("host-note-id-01 [host note content 1]"));
    }

    @Test
    public void testToCompletionInfoReturnsNonNull() {
        assertThat(NoteIdsFinder.toCompletionInfo(hostNote1), is(not(nullValue())));
    }

    @Test(expected = NullPointerException.class)
    public void testToCompletionInfoWithNullArg() {
        NoteIdsFinder.toCompletionInfo(null);
    }

    @Test
    public void testToCompletionInfoContents() {
        CompletionInfo result = NoteIdsFinder.toCompletionInfo(hostNote1);
        assertThat(result.getActualCompletion(), is(hostNote1.getId()));
        assertThat(result.getUserVisibleText(), is(NoteIdsFinder.trimContent(hostNote1.getContent())));
    }

    @Test
    public void testTrimContentWithShortInput() {
        String in = "short input";
        String trimmed = NoteIdsFinder.trimContent(in);
        assertThat(trimmed, is(in));
    }

    @Test
    public void testTrimContentWithLongInput() {
        String in = "this is a long input string which is expected to be trimmed short";
        String trimmed = NoteIdsFinder.trimContent(in);
        String expected = "this is a long input string...";
        assertThat(trimmed.length(), is(30));
        assertThat(trimmed, is(expected));
    }

    @Test
    public void testTrimContentWithLongInput2() {
        String in = "this is a long input string";
        String trimmed = NoteIdsFinder.trimContent(in);
        String expected = "this is a long input string";
        assertThat(trimmed, is(expected));
    }

    @Test
    public void testTrimContentWithLongInput3() {
        String in = "this is a long input string w";
        String trimmed = NoteIdsFinder.trimContent(in);
        String expected = "this is a long input string w";
        assertThat(trimmed, is(expected));
    }

    @Test
    public void testTrimContentWithLongInput4() {
        String in = "this is a long input string wh";
        String trimmed = NoteIdsFinder.trimContent(in);
        String expected = "this is a long input string...";
        assertThat(trimmed, is(expected));
        assertThat(in.length(), is(expected.length()));
    }

    @Test
    public void testTrimContentWithNullInput() {
        String in = null;
        String trimmed = NoteIdsFinder.trimContent(in);
        assertThat(trimmed, is(""));
    }

    private static <T extends Note> List<CompletionInfo> convertNotesToInfos(Iterable<T> notes) {
        List<CompletionInfo> result = new ArrayList<>();
        for (T note : notes) {
            result.add(NoteIdsFinder.toCompletionInfo(note));
        }
        return result;
    }

}
