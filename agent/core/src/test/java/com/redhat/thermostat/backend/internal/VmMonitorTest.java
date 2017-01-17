/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.backend.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.agent.utils.ProcessChecker;
import com.redhat.thermostat.backend.VmUpdateListener;

import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;
import sun.jvmstat.monitor.event.VmListener;

public class VmMonitorTest {

    private static final String PROCESS_NOT_FOUND = "Process not found";
    private static final int MONITOR_EXCEPTION_THROWING_PID = 999;
    private VmMonitor monitor;
    private HostIdentifier hostIdentifier;
    private MonitoredHost host;
    private MonitoredVm monitoredVm;
    private ProcessChecker checker;
    private TestLogHandler handler;
    private Logger logger;
    private Level savedLoggingLevel;

    @After
    public void tearDown() {
        if (handler != null) {
            logger.removeHandler(handler);
            handler = null;
        }
        logger.setLevel(savedLoggingLevel);
    }

    @Before
    public void setUp() throws Exception {
        savedLoggingLevel = setupTestLoggerAndReturnOriginalLevel();

        hostIdentifier = mock(HostIdentifier.class);
        when(hostIdentifier.resolve(isA(VmIdentifier.class))).then(new Answer<VmIdentifier>() {
            @Override
            public VmIdentifier answer(InvocationOnMock invocation) throws Throwable {
                return (VmIdentifier) invocation.getArguments()[0];
            }
        });
        host = mock(MonitoredHost.class);
        when(host.getHostIdentifier()).thenReturn(hostIdentifier);
        
        checker = mock(ProcessChecker.class);
        when(checker.exists(isA(Integer.class))).thenReturn(false);

        monitoredVm = mock(MonitoredVm.class);

        monitor = new VmMonitor(checker);
        monitor.setHost(host);
    }
    
    private Level setupTestLoggerAndReturnOriginalLevel() {
        logger = Logger.getLogger("com.redhat.thermostat");
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.FINEST);
        handler = new TestLogHandler(MONITOR_EXCEPTION_THROWING_PID);
        logger.addHandler(handler);
        return originalLevel;
    }

    @Test
    public void testNewVM() throws MonitorException, URISyntaxException {
        final int VM_PID = 1;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm);

        VmUpdateListener listener = mock(VmUpdateListener.class);
        monitor.handleNewVm(listener, VM_PID);
        
        // Check listener registered
        ArgumentCaptor<VmListenerWrapper> captor = ArgumentCaptor.forClass(VmListenerWrapper.class);
        verify(monitoredVm).addVmListener(captor.capture());
        VmListenerWrapper wrapper = captor.getValue();
        assertEquals(listener, wrapper.getVmUpdateListener());
        
        // Check pid map
        assertTrue(monitor.getPidToDataMap().containsKey(VM_PID));
        assertEquals(wrapper, monitor.getPidToDataMap().get(VM_PID).getSecond());
    }
    
    /*
     * English locale "Process not found". It must not matter really for this
     * test. See the next test which verifies this.
     */
    @Test
    public void testNewVMWithProcessNotFoundDoesNotLogWarning() throws Exception {
        String exceptionMsg = PROCESS_NOT_FOUND;
        basicProcNotFoundTest(exceptionMsg);
    }
    
    /*
     * Random exception message as the exact message will be locale dependent.
     */
    @Test
    public void testNewVMWithProcessNotFoundDoesNotLogWarning2() throws Exception {
        String exceptionMsg = "foo but not bar";
        basicProcNotFoundTest(exceptionMsg);
    }

    private void basicProcNotFoundTest(String exceptionMsg) throws Exception {
        IllegalArgumentException iae = new IllegalArgumentException(exceptionMsg);
        MonitorException procNotFound = new MonitorException(iae);
        assertEquals(iae, procNotFound.getCause());
        
        VmIdentifier vmID = new VmIdentifier(String.valueOf(MONITOR_EXCEPTION_THROWING_PID));
        when(host.getMonitoredVm(vmID)).thenThrow(procNotFound);
        VmUpdateListener listener = mock(VmUpdateListener.class);

        monitor.handleNewVm(listener, MONITOR_EXCEPTION_THROWING_PID);
        assertFalse(handler.isUnableToAttachLoggedAsWarning());
        assertTrue(handler.isUnableToAttachLoggedAsFinest());
        assertFalse(handler.isUnableToAttachLoggedAsWarningUnrelated());
    }

    @Test
    public void testNewVMUnrelatedCausedMonitorExceptionLogWarning() throws Exception {
        MonitorException procNotFound = new MonitorException("unknown");
        
        VmIdentifier vmID = new VmIdentifier(String.valueOf(MONITOR_EXCEPTION_THROWING_PID));
        when(host.getMonitoredVm(vmID)).thenThrow(procNotFound);
        VmUpdateListener listener = mock(VmUpdateListener.class);
        
        monitor.handleNewVm(listener, MONITOR_EXCEPTION_THROWING_PID);
        assertFalse(handler.isUnableToAttachLoggedAsWarning());
        assertFalse(handler.isUnableToAttachLoggedAsFinest());
        assertTrue(handler.isUnableToAttachLoggedAsWarningUnrelated());
    }
    
    @Test
    public void testStatVMGetMonitoredVmFails() throws MonitorException {
        final int VM_PID = 1;
        MonitorException monitorException = new MonitorException();
        when(host.getMonitoredVm(isA(VmIdentifier.class))).thenThrow(monitorException);

        VmUpdateListener listener = mock(VmUpdateListener.class);
        monitor.handleNewVm(listener, VM_PID);

        assertFalse(monitor.getPidToDataMap().containsKey(VM_PID));
    }

    @Test
    public void testStoppedVM() throws MonitorException, URISyntaxException {
        final int VM_PID = 1;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm);

        VmUpdateListener listener = mock(VmUpdateListener.class);
        
        monitor.handleNewVm(listener, VM_PID);
        monitor.handleStoppedVm(VM_PID);

        // Check listener unregistered
        ArgumentCaptor<VmListenerWrapper> captor = ArgumentCaptor.forClass(VmListenerWrapper.class);
        verify(monitoredVm).removeVmListener(captor.capture());
        VmListenerWrapper wrapper = captor.getValue();
        assertEquals(listener, wrapper.getVmUpdateListener());
        
        assertFalse(monitor.getPidToDataMap().containsKey(VM_PID));
    }

    @Test
    public void testUnknownVMStopped() throws URISyntaxException, MonitorException {
        final int VM_PID = 1;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm);

        monitor.handleStoppedVm(VM_PID);

        verifyNoMoreInteractions(monitoredVm);
    }

    @Test
    public void testErrorRemovingVmListener() throws URISyntaxException, MonitorException {
        final int VM_PID = 1;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm);
        
        MonitorException monitorException = new MonitorException();
        doThrow(monitorException).when(monitoredVm).removeVmListener(any(VmListener.class));

        VmUpdateListener listener = mock(VmUpdateListener.class);
        monitor.handleNewVm(listener, VM_PID);
        monitor.handleStoppedVm(VM_PID);

        verify(monitoredVm).detach();
    }
    
    @Test
    public void testRemoveAllListeners() throws URISyntaxException, MonitorException {
        final int VM_PID1 = 1;
        final int VM_PID2 = 2;
        
        VmIdentifier VM_ID1 = new VmIdentifier(String.valueOf(VM_PID1));
        when(host.getMonitoredVm(VM_ID1)).thenReturn(monitoredVm);
        
        MonitoredVm monitoredVm2 = mock(MonitoredVm.class);
        VmIdentifier VM_ID2 = new VmIdentifier(String.valueOf(VM_PID2));
        when(host.getMonitoredVm(VM_ID2)).thenReturn(monitoredVm2);

        VmUpdateListener listener1 = mock(VmUpdateListener.class);
        VmUpdateListener listener2 = mock(VmUpdateListener.class);
        monitor.handleNewVm(listener1, VM_PID1);
        monitor.handleNewVm(listener2, VM_PID2);
        
        monitor.removeVmListeners();
        
        ArgumentCaptor<VmListenerWrapper> captor1 = ArgumentCaptor.forClass(VmListenerWrapper.class);
        verify(monitoredVm).removeVmListener(captor1.capture());
        VmListenerWrapper wrapper1 = captor1.getValue();
        assertEquals(listener1, wrapper1.getVmUpdateListener());
        
        ArgumentCaptor<VmListenerWrapper> captor2 = ArgumentCaptor.forClass(VmListenerWrapper.class);
        verify(monitoredVm2).removeVmListener(captor2.capture());
        VmListenerWrapper wrapper2 = captor2.getValue();
        assertEquals(listener2, wrapper2.getVmUpdateListener());
        
        assertEquals(0, monitor.getPidToDataMap().size());
    }

}

