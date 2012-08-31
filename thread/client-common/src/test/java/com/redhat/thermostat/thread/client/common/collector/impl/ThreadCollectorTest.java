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

package com.redhat.thermostat.thread.client.common.collector.impl;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.collector.impl.ThreadMXBeanCollector;
import com.redhat.thermostat.thread.collector.HarvesterCommand;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;

public class ThreadCollectorTest {

    @Test
    public void testVMCapabilitiesNotInDAO() throws Exception {
        
        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("42");
        
        VmRef reference = mock(VmRef.class);
        when(reference.getIdString()).thenReturn("00101010");
        when(reference.getAgent()).thenReturn(agent);
        ThreadDao threadDao = mock(ThreadDao.class);
        
        VMThreadCapabilities resCaps = mock(VMThreadCapabilities.class);
        when(threadDao.loadCapabilities(reference)).thenReturn(null).thenReturn(resCaps);
        
        final Request request = mock(Request.class);
        final RequestQueue requestQueue = mock(RequestQueue.class);
        
        final ArgumentCaptor<RequestResponseListener> captor = ArgumentCaptor.forClass(RequestResponseListener.class);
        doNothing().when(request).addListener(captor.capture());
        
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Request req = (Request) invocation.getArguments()[0];
                assertSame(request, req);
                
                RequestResponseListener listener = captor.getValue();
                listener.fireComplete(null, null);
                
                return null;
            }

        }).when(requestQueue).putRequest(request);
                
        /* ************* */
        
        ThreadCollector collector = new ThreadMXBeanCollector(threadDao, reference) {
            @Override
            Request createRequest() {
                return request;
            }
            @Override
            RequestQueue getRequestQueue() {
                return requestQueue;
            }
        };
        
        VMThreadCapabilities caps = collector.getVMThreadCapabilities();

        verify(request).setParameter(HarvesterCommand.class.getName(), HarvesterCommand.VM_CAPS.name());
        verify(request).setParameter(HarvesterCommand.VM_ID.name(), "00101010");
        verify(request).setParameter(HarvesterCommand.AGENT_ID.name(), "42");
        
        verify(requestQueue).putRequest(request);
        
        verify(threadDao, times(2)).loadCapabilities(reference);
        assertSame(resCaps, caps);
    }
    
    @Test
    public void testVMCapabilitiesInDAO() throws Exception {
        
        VmRef reference = mock(VmRef.class);
        ThreadDao threadDao = mock(ThreadDao.class);
        
        VMThreadCapabilities resCaps = mock(VMThreadCapabilities.class);
        when(threadDao.loadCapabilities(reference)).thenReturn(resCaps);
        
        ThreadCollector collector = new ThreadMXBeanCollector(threadDao, reference) {
            @Override
            Request createRequest() {
                fail();
                return null;
            }
            @Override
            RequestQueue getRequestQueue() {
                fail();
                return null;
            }
        };
        
        VMThreadCapabilities caps = collector.getVMThreadCapabilities();
 
        verify(threadDao, times(1)).loadCapabilities(reference);
        assertSame(resCaps, caps);
    }
    
    @Test
    public void testStart() {
        
        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("42");
        
        final Request request = mock(Request.class);
        final RequestQueue requestQueue = mock(RequestQueue.class);
        ThreadDao threadDao = mock(ThreadDao.class);
        VmRef reference = mock(VmRef.class);
        when(reference.getIdString()).thenReturn("00101010");
        when(reference.getAgent()).thenReturn(agent);
        
        final Response response = mock(Response.class);
        when(response.getType()).thenReturn(ResponseType.OK);
        
        final ArgumentCaptor<RequestResponseListener> captor = ArgumentCaptor.forClass(RequestResponseListener.class);
        doNothing().when(request).addListener(captor.capture());
        
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Request req = (Request) invocation.getArguments()[0];
                assertSame(request, req);
                
                RequestResponseListener listener = captor.getValue();
                listener.fireComplete(request, response);
                
                return null;
            }

        }).when(requestQueue).putRequest(request);
        
        ThreadCollector collector = new ThreadMXBeanCollector(threadDao, reference) {
            @Override
            Request createRequest() {
                return request;
            }
            @Override
            RequestQueue getRequestQueue() {
                return requestQueue;
            }
        };
        
        collector.startHarvester();
        
        verify(request).setParameter(HarvesterCommand.class.getName(), HarvesterCommand.START.name());
        verify(request).setParameter(HarvesterCommand.VM_ID.name(), "00101010");
        verify(request).setParameter(HarvesterCommand.AGENT_ID.name(), "42");
        
        verify(requestQueue).putRequest(request);
    }
    
    
    @Test
    public void testStop() {

        final Request request = mock(Request.class);
        final RequestQueue requestQueue = mock(RequestQueue.class);
        ThreadDao threadDao = mock(ThreadDao.class);
        VmRef reference = mock(VmRef.class);
        when(reference.getIdString()).thenReturn("00101010");
        
        final Response response = mock(Response.class);
        when(response.getType()).thenReturn(ResponseType.OK);
        
        final ArgumentCaptor<RequestResponseListener> captor = ArgumentCaptor.forClass(RequestResponseListener.class);
        doNothing().when(request).addListener(captor.capture());
        
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Request req = (Request) invocation.getArguments()[0];
                assertSame(request, req);
                
                RequestResponseListener listener = captor.getValue();
                listener.fireComplete(request, response);
                
                return null;
            }

        }).when(requestQueue).putRequest(request);
        
        ThreadCollector collector = new ThreadMXBeanCollector(threadDao, reference) {
            @Override
            Request createRequest() {
                return request;
            }
            @Override
            RequestQueue getRequestQueue() {
                return requestQueue;
            }
        };
        collector.stopHarvester();
        
        verify(request).setParameter(HarvesterCommand.class.getName(), HarvesterCommand.STOP.name());
        verify(request).setParameter(HarvesterCommand.VM_ID.name(), "00101010");
        
        verify(requestQueue).putRequest(request);
    }    
}
