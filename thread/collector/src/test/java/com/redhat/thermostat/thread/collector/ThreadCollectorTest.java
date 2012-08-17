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

package com.redhat.thermostat.thread.collector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.MalformedObjectNameException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.thread.collector.impl.ThreadMXBeanCollector;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.utils.management.MXBeanConnection;
import com.redhat.thermostat.utils.management.MXBeanConnector;

public class ThreadCollectorTest {

    @Test
    public void testVMCapabilitiesNotInDAO() throws Exception {
        
        VMThreadCapabilities referenceCaps = mock(VMThreadCapabilities.class);
        when(referenceCaps.supportContentionMonitor()).thenReturn(true);
        when(referenceCaps.supportCPUTime()).thenReturn(false);
        
        VmRef reference = mock(VmRef.class);
        
        ThreadDao threadDao = mock(ThreadDao.class);
        when(threadDao.loadCapabilities(reference)).thenReturn(null);
        
        MXBeanConnector connector = mock(MXBeanConnector.class);
        when(connector.isAttached()).thenReturn(false).thenReturn(true);
        
        MXBeanConnection connection = mock(MXBeanConnection.class);
        when(connector.connect()).thenReturn(connection);
                
        ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> captor2 = ArgumentCaptor.forClass(Class.class);
        
        ThreadMXBean bean = mock(ThreadMXBean.class);
        when(bean.isThreadCpuTimeSupported()).thenReturn(false);
        when(bean.isThreadContentionMonitoringSupported()).thenReturn(true);
        
        when(connection.createProxy(captor1.capture(), captor2.capture())).thenThrow(new MalformedObjectNameException()).thenReturn(bean);
        
        ScheduledExecutorService threadPool = mock(ScheduledExecutorService.class);
        
        /* ************* */
        
        ThreadCollector collector = new ThreadMXBeanCollector(threadDao, reference, connector, threadPool);
        VMThreadCapabilities caps = collector.getVMThreadCapabilities();

        String beanName = captor1.getValue();
        assertEquals(ManagementFactory.THREAD_MXBEAN_NAME, beanName);
        
        Class clazz = captor2.getValue();
        assertEquals(ThreadMXBean.class.getName(), clazz.getName());
        
        verify(threadDao).loadCapabilities(reference);
        
        verify(connector).attach();
        verify(connector).connect();
        verify(connector).close();
        
        verify(threadDao).saveCapabilities(reference, caps);
        
        assertTrue(caps.supportContentionMonitor());
        assertFalse(caps.supportCPUTime());
        assertFalse(caps.supportThreadAllocatedMemory());
    }
    
    @SuppressWarnings("restriction")
    @Test
    public void testHasThreadMemorySupport() throws Exception {
        
        VMThreadCapabilities referenceCaps = mock(VMThreadCapabilities.class);
        when(referenceCaps.supportContentionMonitor()).thenReturn(true);
        when(referenceCaps.supportCPUTime()).thenReturn(false);
        
        VmRef reference = mock(VmRef.class);
        
        ThreadDao threadDao = mock(ThreadDao.class);
        when(threadDao.loadCapabilities(reference)).thenReturn(null);
        
        MXBeanConnector connector = mock(MXBeanConnector.class);
        when(connector.isAttached()).thenReturn(false).thenReturn(true);
        
        MXBeanConnection connection = mock(MXBeanConnection.class);
        when(connector.connect()).thenReturn(connection);
                
        ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> captor2 = ArgumentCaptor.forClass(Class.class);
        
        com.sun.management.ThreadMXBean bean = mock(com.sun.management.ThreadMXBean.class);
        when(bean.isThreadCpuTimeSupported()).thenReturn(false);
        when(bean.isThreadContentionMonitoringSupported()).thenReturn(true);
        when(bean.isThreadAllocatedMemorySupported()).thenReturn(true);
        
        when(connection.createProxy(captor1.capture(), captor2.capture())).thenReturn(bean);
        
        ScheduledExecutorService threadPool = mock(ScheduledExecutorService.class);
        
        /* ************* */
        
        ThreadCollector collector = new ThreadMXBeanCollector(threadDao, reference, connector, threadPool);
        VMThreadCapabilities caps = collector.getVMThreadCapabilities();

        String beanName = captor1.getValue();
        assertEquals(ManagementFactory.THREAD_MXBEAN_NAME, beanName);
        
        Class clazz = captor2.getValue();
        assertEquals(com.sun.management.ThreadMXBean.class.getName(), clazz.getName());
        
        verify(threadDao).loadCapabilities(reference);
        
        verify(connector).attach();
        verify(connector).connect();
        verify(connector).close();
        
        verify(threadDao).saveCapabilities(reference, caps);
        
        assertTrue(caps.supportContentionMonitor());
        assertFalse(caps.supportCPUTime());
        assertTrue(caps.supportThreadAllocatedMemory());
    }
    
    @Test
    public void testVMCapabilitiesInDAO() throws Exception {
        
        VMThreadCapabilities referenceCaps = mock(VMThreadCapabilities.class);
        when(referenceCaps.supportContentionMonitor()).thenReturn(true);
        when(referenceCaps.supportCPUTime()).thenReturn(false);
        
        VmRef reference = mock(VmRef.class);
        
        ThreadDao threadDao = mock(ThreadDao.class);
        when(threadDao.loadCapabilities(reference)).thenReturn(referenceCaps);
        
        MXBeanConnector connector = mock(MXBeanConnector.class);
        
        ScheduledExecutorService threadPool = mock(ScheduledExecutorService.class);
        
        /* ************* */
        
        ThreadCollector collector = new ThreadMXBeanCollector(threadDao, reference, connector, threadPool);
        VMThreadCapabilities caps = collector.getVMThreadCapabilities();

        verify(threadDao).loadCapabilities(reference);
        
        verify(connector, times(0)).attach();
        verify(connector, times(0)).connect();
        verify(connector, times(0)).close();
        
        verify(threadDao, times(0)).saveCapabilities(reference, caps);
        
        assertTrue(caps.supportContentionMonitor());
        assertFalse(caps.supportCPUTime());
    }
}
