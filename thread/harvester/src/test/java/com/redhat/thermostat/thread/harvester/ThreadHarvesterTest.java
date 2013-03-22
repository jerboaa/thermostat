/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.thread.harvester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.thread.collector.HarvesterCommand;
import com.redhat.thermostat.thread.dao.ThreadDao;

public class ThreadHarvesterTest {

    @Test
    public void testStart() {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ThreadDao dao = mock(ThreadDao.class);
        Request request = mock(Request.class);
        
        final boolean[] getHarvesterCalled = new boolean[1];
        final Harvester harverster = mock(Harvester.class);
        
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        
        when(request.getParameter(captor.capture())).
            thenReturn(HarvesterCommand.START.name()).
            thenReturn("42").
            thenReturn("0xcafe");
        
        ThreadHarvester threadHarvester = new ThreadHarvester(executor) {
            @Override
            Harvester getHarvester(String vmId) {
                
                getHarvesterCalled[0] = true;
                assertEquals("42", vmId);
                
                return harverster;
            }
        };
        threadHarvester.setThreadDao(dao);
        threadHarvester.receive(request);
        
        List<String> values = captor.getAllValues();
        assertEquals(2, values.size());
        
        assertEquals(HarvesterCommand.class.getName(), values.get(0));
        assertEquals(HarvesterCommand.VM_ID.name(), values.get(1));
        
        assertTrue(getHarvesterCalled[0]);
        
        verify(harverster).start();
    }

    @Test
    public void testStop() {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ThreadDao dao = mock(ThreadDao.class);
        Request request = mock(Request.class);
        
        final Harvester harverster = mock(Harvester.class);
        
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        
        when(request.getParameter(captor.capture())).
            thenReturn(HarvesterCommand.STOP.name()).
            thenReturn("42");
        
        ThreadHarvester threadHarvester = new ThreadHarvester(executor) {
            { connectors.put("42", harverster); }
        };
        threadHarvester.setThreadDao(dao);
        threadHarvester.receive(request);
        
        List<String> values = captor.getAllValues();
        assertEquals(2, values.size());
        
        assertEquals(HarvesterCommand.class.getName(), values.get(0));
        assertEquals(HarvesterCommand.VM_ID.name(), values.get(1));
                
        verify(harverster).stop();        
    }
    
    @Test
    public void testSaveVmCaps() {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ThreadDao dao = mock(ThreadDao.class);
        
        final boolean[] getHarvesterCalled = new boolean[1];
        final Harvester harverster = mock(Harvester.class);
        
        ThreadHarvester threadHarvester = new ThreadHarvester(executor) {
            @Override
            Harvester getHarvester(String vmId) {
                
                getHarvesterCalled[0] = true;
                assertEquals("42", vmId);
                
                return harverster;
            }
        };
        threadHarvester.setThreadDao(dao);
        threadHarvester.saveVmCaps("42");
        
        assertTrue(getHarvesterCalled[0]);
        
        verify(harverster).saveVmCaps();
    }    

    @Test
    public void testRecieveWithoutDaosFails() {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

        ThreadHarvester harvester = new ThreadHarvester(executor);
        Response response = harvester.receive(mock(Request.class));

        assertEquals(ResponseType.ERROR, response.getType());
    }
}

