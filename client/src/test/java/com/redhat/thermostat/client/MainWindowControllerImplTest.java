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

package com.redhat.thermostat.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;

public class MainWindowControllerImplTest {

    private ActionListener<MainView.Action> l;

    private MainWindowControllerImpl controller;

    private MainView view;

    private Timer mainWindowTimer;

    private HostInfoDAO mockHostsDAO;
    private VmInfoDAO mockVmsDAO;

    @SuppressWarnings({ "unchecked", "rawtypes" }) // ActionListener fluff
    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();
        mainWindowTimer = mock(Timer.class);
        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(mainWindowTimer);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);

        setupDAOs();

        view = mock(MainView.class);
        ArgumentCaptor<ActionListener> grabListener = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(grabListener.capture());
        controller = new MainWindowControllerImpl(view);
        l = grabListener.getValue();

    }

    private void setupDAOs() {
        mockHostsDAO = mock(HostInfoDAO.class);

        mockVmsDAO = mock(VmInfoDAO.class);

        DAOFactory daoFactory = mock(DAOFactory.class);
        when(daoFactory.getHostInfoDAO()).thenReturn(mockHostsDAO);
        when(daoFactory.getVmInfoDAO()).thenReturn(mockVmsDAO);
        ApplicationContext.getInstance().setDAOFactory(daoFactory);

    }

    @After
    public void tearDown() {
        view = null;
        controller = null;
        mockHostsDAO = null;
        mockVmsDAO = null;
        l = null;
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void verifyThatShutdownEventStopsController() {

        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.SHUTDOWN));

        verify(mainWindowTimer).stop();

    }

    @Test
    public void verifyThatHostsVmsFilterChangeUpdatesTree() {

        when(view.getHostVmTreeFilter()).thenReturn("test");

        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_TREE_FILTER));

        verify(view).updateTree(eq("test"), any(HostsVMsLoader.class));

    }

    @Test
    public void verifyTimerGetsStartedOnConstruction() {
        verify(mainWindowTimer).start();
    }

    @Test
    public void verifyShowMainWindowActuallyCallsView() {
        controller.showMainMainWindow();
        verify(view).showMainWindow();
    }

    @Test
    public void verifyUpdateHostsVMsLoadsCorrectHosts() {

        Collection<HostRef> expectedHosts = new ArrayList<>();
        expectedHosts.add(new HostRef("123", "fluffhost1"));
        expectedHosts.add(new HostRef("456", "fluffhost2"));

        when(mockHostsDAO.getAliveHosts()).thenReturn(expectedHosts);
        
        controller.doUpdateTreeAsync();

        ArgumentCaptor<HostsVMsLoader> arg = ArgumentCaptor.forClass(HostsVMsLoader.class);
        verify(view).updateTree(anyString(), arg.capture());
        HostsVMsLoader loader = arg.getValue();

        Collection<HostRef> actualHosts = loader.getHosts();
        assertEqualCollection(expectedHosts, actualHosts);
    }

    @Test
    public void verifyUpdateHostsVMsLoadsCorrectVMs() {

        Collection<VmRef> expectedVMs = new ArrayList<>();
        HostRef host = new HostRef("123", "fluffhost1");
        expectedVMs.add(new VmRef(host, 123, "vm1"));
        expectedVMs.add(new VmRef(host, 456, "vm2"));

        when(mockVmsDAO.getVMs(any(HostRef.class))).thenReturn(expectedVMs);

        controller.doUpdateTreeAsync();

        ArgumentCaptor<HostsVMsLoader> arg = ArgumentCaptor.forClass(HostsVMsLoader.class);
        verify(view).updateTree(anyString(), arg.capture());
        HostsVMsLoader loader = arg.getValue();

        Collection<VmRef> actualVMs = loader.getVMs(host);
        assertEqualCollection(expectedVMs, actualVMs);
    }

    private void assertEqualCollection(Collection<?> expected, Collection<?> actual) {
        assertEquals(expected.size(), actual.size());
        assertTrue(expected.containsAll(actual));
    }
}
