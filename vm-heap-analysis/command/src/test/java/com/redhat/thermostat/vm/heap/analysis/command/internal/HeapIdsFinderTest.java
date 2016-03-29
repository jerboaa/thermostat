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

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.cli.CompletionInfo;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HeapIdsFinderTest {

    private BundleContext context;
    private DependencyServices dependencyServices;
    private HeapDAO heapDao;
    private VmInfoDAO vmDao;
    private HeapIdsFinder finder;

    @Before
    public void setup() {
        context = mock(BundleContext.class);

        dependencyServices = mock(DependencyServices.class);
        heapDao = mock(HeapDAO.class);
        vmDao = mock(VmInfoDAO.class);
        when(dependencyServices.hasService(HeapDAO.class)).thenReturn(true);
        when(dependencyServices.getService(HeapDAO.class)).thenReturn(heapDao);
        when(dependencyServices.hasService(VmInfoDAO.class)).thenReturn(true);
        when(dependencyServices.getService(VmInfoDAO.class)).thenReturn(vmDao);

        HeapInfo heapInfo1 = new HeapInfo();
        heapInfo1.setTimeStamp(100L);
        heapInfo1.setVmId("foo-vm");
        heapInfo1.setAgentId("foo-agent");
        heapInfo1.setHeapDumpId("foo-heapdump");
        heapInfo1.setHistogramId("foo-histogram");
        heapInfo1.setHeapId("foo-heap");

        HeapInfo heapInfo2 = new HeapInfo();
        heapInfo2.setTimeStamp(200L);
        heapInfo2.setVmId("bar-vm");
        heapInfo2.setAgentId("bar-agent");
        heapInfo2.setHeapDumpId("bar-heapdump");
        heapInfo2.setHistogramId("bar-histogram");
        heapInfo2.setHeapId("bar-heap");

        when(heapDao.getAllHeapInfo()).thenReturn(Arrays.asList(heapInfo1, heapInfo2));

        VmInfo vmInfo1 = new VmInfo();
        vmInfo1.setAgentId("foo-agent");
        vmInfo1.setVmId("foo-vm");
        vmInfo1.setMainClass("foo-mainclass");
        vmInfo1.setStartTimeStamp(50L);
        vmInfo1.setStopTimeStamp(-1L);

        VmInfo vmInfo2 = new VmInfo();
        vmInfo2.setAgentId("bar-agent");
        vmInfo2.setVmId("bar-vm");
        vmInfo2.setMainClass("bar-mainclass");
        vmInfo2.setStartTimeStamp(75L);
        vmInfo2.setStopTimeStamp(-1L);

        when(vmDao.getVmInfo(new VmId("foo-vm"))).thenReturn(vmInfo1);
        when(vmDao.getVmInfo(new VmId("bar-vm"))).thenReturn(vmInfo2);

        finder = new HeapIdsFinder(dependencyServices);
    }

    @Test
    public void testNumberOfResults() {
        List<CompletionInfo> infos = finder.findCompletions();
        assertThat(infos.size(), is(2));
    }

    @Test
    public void testReturnsEmptyListWhenNoInfoFromDao() {
        when(heapDao.getAllHeapInfo()).thenReturn(Collections.<HeapInfo>emptyList());
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
        CompletionInfo barInfo = infos.get(0);
        CompletionInfo fooInfo = infos.get(1);

        assertThat(barInfo.getActualCompletion(), is("bar-heap"));
        assertThat(barInfo.getUserVisibleText(), is("bar-mainclass @ " + Clock.DEFAULT_DATE_FORMAT.format(new Date(200L))));

        assertThat(fooInfo.getActualCompletion(), is("foo-heap"));
        assertThat(fooInfo.getUserVisibleText(), is("foo-mainclass @ " + Clock.DEFAULT_DATE_FORMAT.format(new Date(100L))));
    }

}
