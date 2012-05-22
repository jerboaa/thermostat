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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.redhat.thermostat.client.osgi.service.VMContextAction;
import com.redhat.thermostat.client.ui.SummaryController;
import com.redhat.thermostat.client.ui.SummaryView;
import com.redhat.thermostat.client.ui.VmInformationController;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.ViewFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.test.Bug;

public class MainWindowControllerImplTest {

    private ActionListener<MainView.Action> l;

    private MainWindowControllerImpl controller;

    private UiFacadeFactory uiFacadeFactory;

    private MainView view;

    private Timer mainWindowTimer;

    private HostInfoDAO mockHostsDAO;
    private VmInfoDAO mockVmsDAO;

    private VMContextAction action1;
    private VMContextAction action2;
    
    @BeforeClass
    public static void setUpOnce() {
        // TODO remove when controller uses mocked objects rather than real swing objects
        FailOnThreadViolationRepaintManager.install();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" }) // ActionListener fluff
    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();

        // Setup timers
        mainWindowTimer = mock(Timer.class);
        Timer otherTimer = mock(Timer.class); // FIXME needed for SummaryView; remove later
        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(mainWindowTimer).thenReturn(otherTimer);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);

        SummaryController summaryController = mock(SummaryController.class);

        uiFacadeFactory = mock(UiFacadeFactory.class);
        when(uiFacadeFactory.getSummary()).thenReturn(summaryController);
        setupDAOs();

        // Setup View
        view = mock(MainView.class);
        ArgumentCaptor<ActionListener> grabListener = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(grabListener.capture());

        // TODO remove this asap. the main window has a hard dependency on summary controller/view
        ViewFactory viewFactory = mock(ViewFactory.class);
        SummaryView summaryView = mock(SummaryView.class);
        when(viewFactory.getView(SummaryView.class)).thenReturn(summaryView);
        ApplicationContext.getInstance().setViewFactory(viewFactory);

        setUpVMContextActions();
        
        controller = new MainWindowControllerImpl(uiFacadeFactory, view);
        l = grabListener.getValue();

    }

    private void setUpVMContextActions() {
        action1 = mock(VMContextAction.class);
        when(action1.getName()).thenReturn("action1");
        when(action1.getDescription()).thenReturn("action1desc");
        
        action2 = mock(VMContextAction.class);
        when(action2.getName()).thenReturn("action2");
        when(action2.getDescription()).thenReturn("action2desc");
        
        Collection<VMContextAction> actions = new ArrayList<>();
        actions.add(action1);
        actions.add(action2);
        
        when(uiFacadeFactory.getVMContextActions()).thenReturn(actions);
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
    public void verifyThatHiddenEventStopsController() {

        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HIDDEN));

        verify(mainWindowTimer).stop();

    }

    @Test
    public void verifyThatHostsVmsFilterChangeUpdatesTree() {

        when(view.getHostVmTreeFilter()).thenReturn("test");

        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_TREE_FILTER));

        verify(view).updateTree(eq("test"), any(HostsVMsLoader.class));

    }

    @Test
    public void verifyTimerGetsStartedOnBecomingVisible() {
        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.VISIBLE));

        verify(mainWindowTimer).setDelay(3);
        verify(mainWindowTimer).setTimeUnit(TimeUnit.SECONDS);
        verify(mainWindowTimer).setSchedulingType(SchedulingType.FIXED_RATE);
        verify(mainWindowTimer).start();
    }

    @Test
    public void verifyShowMainWindowActuallyCallsView() {
        controller.showMainMainWindow();
        verify(view).showMainWindow();
    }

    @Test
    public void verifySubViewIsSetByDefault() throws InvocationTargetException, InterruptedException {
        verify(view).setSubView(any(Component.class));
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
    public void verifyHistoryModeUpdateHostsVMCorrectly() {

        Collection<HostRef> liveHost = new ArrayList<>();
        liveHost.add(new HostRef("123", "fluffhost1"));
        liveHost.add(new HostRef("456", "fluffhost2"));

        Collection<HostRef> allHosts = new ArrayList<>();
        allHosts.addAll(liveHost);
        allHosts.add(new HostRef("789", "fluffhost3"));

        when(mockHostsDAO.getAliveHosts()).thenReturn(liveHost);
        when(mockHostsDAO.getHosts()).thenReturn(allHosts);

        controller.doUpdateTreeAsync();

        ArgumentCaptor<HostsVMsLoader> arg = ArgumentCaptor.forClass(HostsVMsLoader.class);
        verify(view).updateTree(anyString(), arg.capture());
        HostsVMsLoader loader = arg.getValue();

        Collection<HostRef> actualHosts = loader.getHosts();
        assertEqualCollection(liveHost, actualHosts);

        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.SWITCH_HISTORY_MODE));

        actualHosts = loader.getHosts();
        assertEqualCollection(allHosts, actualHosts);
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

    @Test
    @Bug(id="954",
         summary="Thermostat GUI client should remember my last panel selected",
         url="http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=954")
    public void verifyOpenSameHostVMTab() {

        VmRef vmRef = mock(VmRef.class);
        when(view.getSelectedHostOrVm()).thenReturn(vmRef);

        VmInformationController vmInformationController = mock(VmInformationController.class);
        when(vmInformationController.getSelectedChildID()).thenReturn(3);
        when(uiFacadeFactory.getVmController(any(VmRef.class))).thenReturn(vmInformationController);

        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));

        ArgumentCaptor<Integer> arg = ArgumentCaptor.forClass(Integer.class);
        verify(vmInformationController).selectChildID(arg.capture());
        verify(vmInformationController, times(0)).getSelectedChildID();

        int id = arg.getValue();

        assertEquals(0, id);

        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));

        arg = ArgumentCaptor.forClass(Integer.class);
        verify(vmInformationController, times(1)).getSelectedChildID();
        verify(vmInformationController, times(2)).selectChildID(arg.capture());
        id = arg.getValue();

        assertEquals(3, id);
    }
    
    @Test
    public void verityVMActionsAreRegistered() {

        verify(view).registerVMContextAction(action1);
        verify(view).registerVMContextAction(action2);
    }
    
    @Test
    public void verityVMActionsAreExecuted() {

        VmRef vmRef = mock(VmRef.class);
        when(view.getSelectedHostOrVm()).thenReturn(vmRef);

        ActionEvent<MainView.Action> event = new ActionEvent<>(view, MainView.Action.VM_CONTEXT_ACTION);
        event.setPayload(action1);
        l.actionPerformed(event);
        
        verify(action1, times(1)).execute(any(VmRef.class));
        verify(action2, times(0)).execute(any(VmRef.class));
    }

   @Test
   public void testOSGiFrameworkShutdown() throws BundleException {

       l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.SHUTDOWN));

       verify(uiFacadeFactory).shutdown();
   }
}
