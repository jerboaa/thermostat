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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import com.redhat.thermostat.common.cli.CliCommandOption;
import com.redhat.thermostat.common.cli.TabCompleter;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class HeapIdCompleterServiceTest {

    private HeapDAO heapDao;
    private VmInfoDAO vmInfoDao;
    private HeapIdCompleterService service;

    @Before
    public void setup() {
        heapDao = mock(HeapDAO.class);
        vmInfoDao = mock(VmInfoDAO.class);
        service = new HeapIdCompleterService();
        service.setHeapDAO(heapDao);
        service.setVmInfoDAO(vmInfoDao);
    }

    @Test
    public void testGetCommands() {
        List<String> commands = new ArrayList<>(service.getCommands());
        List<String> expected = new ArrayList<>(Arrays.asList(
                FindObjectsCommand.COMMAND_NAME,
                FindRootCommand.COMMAND_NAME,
                ObjectInfoCommand.COMMAND_NAME,
                SaveHeapDumpToFileCommand.COMMAND_NAME,
                ShowHeapHistogramCommand.COMMAND_NAME
        ));
        Collections.sort(commands);
        Collections.sort(expected);
        assertThat(commands, is(equalTo(expected)));
    }

    @Test
    public void testGetOptionCompleters() {
        Map<CliCommandOption, ? extends TabCompleter> map = service.getOptionCompleters();
        assertThat(map.size(), is(1));
        for (Map.Entry<CliCommandOption, ? extends TabCompleter> entry : map.entrySet()) {
            CliCommandOption opt = entry.getKey();
            assertThat(opt.getOpt(), is("h"));
            assertThat(opt.getLongOpt(), is("heapId"));
            assertThat(opt.hasArg(), is(true));
            assertThat(opt.isRequired(), is(false));
            assertThat(opt.getDescription(), is(not(nullValue())));
            assertThat(opt.getDescription().length(), is(atLeast(1)));
        }
    }

    private static Matcher<Integer> atLeast(final Integer i) {
        return new BaseMatcher<Integer>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof Integer && ((int) o) >= i;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("int greater than or equal to ")
                        .appendValue(i);
            }
        };
    }

}
