/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.agent.heapdumper.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.heap.HistogramLoader;
import com.redhat.thermostat.common.heap.ObjectHistogram;
import com.redhat.thermostat.common.model.HeapInfo;

public class HeapDumpReceiverTest {

    private HeapDAO heapDAO;
    private Request request;
    private JMXHeapDumper jmxDumper;
    private HistogramLoader histogramLoader;

    private HeapDumpReceiver receiver;
    private JMapHeapDumper jmapDumper;

    @Before
    public void setUp() {
        heapDAO = mock(HeapDAO.class);

        request = mock(Request.class);
        when(request.getParameter("vmId")).thenReturn("123");
        jmxDumper = mock(JMXHeapDumper.class);
        jmapDumper = mock(JMapHeapDumper.class);
        histogramLoader = mock(HistogramLoader.class);
        receiver = new HeapDumpReceiver(heapDAO, jmxDumper, jmapDumper, histogramLoader);
    }

    @After
    public void tearDown() {
        histogramLoader = null;
        jmapDumper = null;
        jmxDumper = null;
        request = null;
        heapDAO = null;
    }

    @Test
    public void testJMXHeapDump() throws Exception {

        ObjectHistogram expectedHistogramData = mock(ObjectHistogram.class);
        when(histogramLoader.load(anyString())).thenReturn(expectedHistogramData);
        
        Response response = receiver.receive(request);

        assertEquals(ResponseType.OK, response.getType());
        ArgumentCaptor<String> filename = ArgumentCaptor.forClass(String.class);
        verify(jmxDumper).dumpHeap(eq("123"), filename.capture());
        verify(histogramLoader).load(filename.getValue());
        ArgumentCaptor<HeapInfo> heapInfo = ArgumentCaptor.forClass(HeapInfo.class);
        verify(heapDAO).putHeapInfo(heapInfo.capture(), eq(new File(filename.getValue())), same(expectedHistogramData));
        assertEquals(123, heapInfo.getValue().getVmId());
    }

    @Test
    public void verifyFallbackWhenJMXFails() throws HeapDumpException {

        doThrow(new HeapDumpException()).when(jmxDumper).dumpHeap(anyString(), anyString());

        Response response = receiver.receive(request);

        assertEquals(ResponseType.OK, response.getType());
        verify(jmxDumper).dumpHeap(eq("123"), anyString());
        
    }

    @Test
    public void verifyResponseTypeWhenAllFails() throws HeapDumpException {
        doThrow(new HeapDumpException()).when(jmxDumper).dumpHeap(anyString(), anyString());
        doThrow(new HeapDumpException()).when(jmapDumper).dumpHeap(anyString(), anyString());
        Response response = receiver.receive(request);

        assertEquals(ResponseType.EXCEPTION, response.getType());
        
    }

    @Test
    public void verifyResponseTypeWhenIOFails() throws HeapDumpException, IOException {
        doThrow(new IOException()).when(histogramLoader).load(anyString());

        Response response = receiver.receive(request);

        assertEquals(ResponseType.EXCEPTION, response.getType());
        
    }
}
