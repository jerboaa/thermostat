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

package com.redhat.thermostat.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleException;

import com.redhat.thermostat.client.core.VmFilter;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.osgi.service.MenuAction;
import com.redhat.thermostat.client.osgi.service.VMContextAction;
import com.redhat.thermostat.client.osgi.service.VmDecorator;
import com.redhat.thermostat.client.ui.HostVmFilter;
import com.redhat.thermostat.client.ui.SummaryController;
import com.redhat.thermostat.client.ui.UiFacadeFactory;
import com.redhat.thermostat.client.ui.VmInformationController;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.HostsVMsLoader;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.ThermostatExtensionRegistry.Action;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmInfo;
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

    private HostFilterRegistry hostFilterRegistry;
    private VmFilterRegistry vmFilterRegistry;
    private HostTreeDecoratorRegistry hostDecoratorRegistry;
    private VMTreeDecoratorRegistry vmDecoratorRegistry;
    private VMInformationRegistry vmInfoRegistry;
    private MenuRegistry menus;
    
    @SuppressWarnings("unused")
    private ActionListener<ThermostatExtensionRegistry.Action> hostFiltersListener;
    @SuppressWarnings("unused")
    private ActionListener<ThermostatExtensionRegistry.Action> vmFiltersListener;
    private ActionListener<ThermostatExtensionRegistry.Action> decoratorsListener;
    
    @BeforeClass
    public static void setUpOnce() {
        // TODO remove when controller uses mocked objects rather than real swing objects
        FailOnThreadViolationRepaintManager.install();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" }) // ActionListener fluff
    @Before
    public void setUp() throws Exception {
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

        mockHostsDAO = mock(HostInfoDAO.class);
        mockVmsDAO = mock(VmInfoDAO.class);

        // Setup View
        view = mock(MainView.class);
        ArgumentCaptor<ActionListener> grabListener = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(grabListener.capture());
        
        RegistryFactory registryFactory = mock(RegistryFactory.class);
        hostFilterRegistry = mock(HostFilterRegistry.class);
        vmFilterRegistry = mock(VmFilterRegistry.class);
        hostDecoratorRegistry = mock(HostTreeDecoratorRegistry.class);
        vmDecoratorRegistry = mock(VMTreeDecoratorRegistry.class);
        vmInfoRegistry = mock(VMInformationRegistry.class);
        menus = mock(MenuRegistry.class);

        when(registryFactory.createMenuRegistry()).thenReturn(menus);
        when(registryFactory.createHostTreeDecoratorRegistry()).thenReturn(hostDecoratorRegistry);
        when(registryFactory.createVMTreeDecoratorRegistry()).thenReturn(vmDecoratorRegistry);
        when(registryFactory.createHostFilterRegistry()).thenReturn(hostFilterRegistry);
        when(registryFactory.createVmFilterRegistry()).thenReturn(vmFilterRegistry);
        when(registryFactory.createVMInformationRegistry()).thenReturn(vmInfoRegistry);
        
        ArgumentCaptor<ActionListener> grabHostFiltersListener = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(hostFilterRegistry).addActionListener(grabHostFiltersListener.capture());

        ArgumentCaptor<ActionListener> grabVmFiltersListener = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(vmFilterRegistry).addActionListener(grabVmFiltersListener.capture());

        ArgumentCaptor<ActionListener> grabDecoratorsListener = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(vmDecoratorRegistry).addActionListener(grabDecoratorsListener.capture());
        
        ArgumentCaptor<ActionListener> grabInfoRegistry = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(vmInfoRegistry).addActionListener(grabInfoRegistry.capture());

        setUpVMContextActions();

        controller = new MainWindowControllerImpl(uiFacadeFactory, view, registryFactory, mockHostsDAO, mockVmsDAO);
        l = grabListener.getValue();
        
        hostFiltersListener = grabHostFiltersListener.getValue();
        vmFiltersListener = grabVmFiltersListener.getValue();
        decoratorsListener = grabDecoratorsListener.getValue();
    }

    private void setUpVMContextActions() {
        action1 = mock(VMContextAction.class);
        VmFilter action1Filter = mock(VmFilter.class);
        when(action1Filter.matches(isA(VmRef.class))).thenReturn(true);

        when(action1.getName()).thenReturn("action1");
        when(action1.getDescription()).thenReturn("action1desc");
        when(action1.getFilter()).thenReturn(action1Filter);
        
        action2 = mock(VMContextAction.class);
        VmFilter action2Filter = mock(VmFilter.class);
        when(action2Filter.matches(isA(VmRef.class))).thenReturn(false);

        when(action2.getName()).thenReturn("action2");
        when(action2.getDescription()).thenReturn("action2desc");
        when(action2.getFilter()).thenReturn(action2Filter);
        
        Collection<VMContextAction> actions = new ArrayList<>();
        actions.add(action1);
        actions.add(action2);
        
        when(uiFacadeFactory.getVMContextActions()).thenReturn(actions);
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

    @SuppressWarnings("unchecked")
    @Test
    public void verifyDecoratorsAdded() {

        List<VmDecorator> currentDecoratros = controller.getVmTreeDecorators();
        assertEquals(0, currentDecoratros.size());
        
        ActionEvent<ThermostatExtensionRegistry.Action> event =
                new ActionEvent<ThermostatExtensionRegistry.Action>(vmDecoratorRegistry,
                        ThermostatExtensionRegistry.Action.SERVICE_ADDED);
        
        VmDecorator payload = mock(VmDecorator.class);
        event.setPayload(payload);
        
        decoratorsListener.actionPerformed(event);

        currentDecoratros = controller.getVmTreeDecorators();
        assertEquals(1, currentDecoratros.size());
        assertEquals(payload, currentDecoratros.get(0));
        
        verify(view).updateTree(any(List.class), isA(List.class), isA(List.class), any(List.class), any(HostsVMsLoader.class));
    }
    
    @Test
    public void verifyThatHiddenEventStopsController() {

        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HIDDEN));

        verify(mainWindowTimer).stop();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyThatHostsVmsFilterChangeUpdatesTree() {

        when(view.getHostVmTreeFilterText()).thenReturn("test");

        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_TREE_FILTER));

        verify(view).updateTree(isA(List.class), isA(List.class), isA(List.class), isA(List.class), isA(HostsVMsLoader.class));
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
        verify(view).setSubView(any(BasicView.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyUpdateHostsVMsLoadsCorrectHosts() {

        Collection<HostRef> expectedHosts = new ArrayList<>();
        expectedHosts.add(new HostRef("123", "fluffhost1"));
        expectedHosts.add(new HostRef("456", "fluffhost2"));

        when(mockHostsDAO.getAliveHosts()).thenReturn(expectedHosts);

        controller.doUpdateTreeAsync();

        ArgumentCaptor<HostsVMsLoader> arg = ArgumentCaptor.forClass(HostsVMsLoader.class);
        verify(view).updateTree(isA(List.class), isA(List.class), isA(List.class), isA(List.class), arg.capture());
        HostsVMsLoader loader = arg.getValue();

        Collection<HostRef> actualHosts = loader.getHosts();
        assertEqualCollection(expectedHosts, actualHosts);
    }
    
    @SuppressWarnings("unchecked")
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
        verify(view).updateTree(isA(List.class), isA(List.class), isA(List.class), isA(List.class), arg.capture());
        HostsVMsLoader loader = arg.getValue();

        Collection<HostRef> actualHosts = loader.getHosts();
        assertEqualCollection(liveHost, actualHosts);

        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.SWITCH_HISTORY_MODE));
        ArgumentCaptor<HostsVMsLoader> argCaptor = ArgumentCaptor.forClass(HostsVMsLoader.class);
        // actionPerformed triggers updateTree
        verify(view, times(2)).updateTree(isA(List.class), isA(List.class), isA(List.class), isA(List.class), argCaptor.capture());
        loader = argCaptor.getValue();

        actualHosts = loader.getHosts();
        assertEqualCollection(allHosts, actualHosts);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyUpdateHostsVMsLoadsCorrectVMs() {

        Collection<VmRef> expectedVMs = new ArrayList<>();
        HostRef host = new HostRef("123", "fluffhost1");
        expectedVMs.add(new VmRef(host, 123, "vm1"));
        expectedVMs.add(new VmRef(host, 456, "vm2"));

        when(mockVmsDAO.getVMs(any(HostRef.class))).thenReturn(expectedVMs);

        controller.doUpdateTreeAsync();

        ArgumentCaptor<HostsVMsLoader> arg = ArgumentCaptor.forClass(HostsVMsLoader.class);
        verify(view).updateTree(isA(List.class), isA(List.class), isA(List.class), isA(List.class), arg.capture());
        HostsVMsLoader loader = arg.getValue();

        Collection<VmRef> actualVMs = loader.getVMs(host);
        assertEqualCollection(expectedVMs, actualVMs);
    }

    @Test
    public void verifyUpdateHostsVMsLoadsCorrectVMWithFilter() {

        VmRef ref1 = mock(VmRef.class);
        when(ref1.getStringID()).thenReturn("test1");
        when(ref1.getName()).thenReturn("test1");
        
        VmRef ref2 = mock(VmRef.class);
        when(ref2.getStringID()).thenReturn("test2");
        when(ref2.getName()).thenReturn("test2");
        
        controller.setHostVmTreeFilter("test1");
                
        HostVmFilter filter = controller.getSearchFilter();
        assertTrue(filter.matches(ref1));
        assertFalse(filter.matches(ref2));
    }
    
    private void assertEqualCollection(Collection<?> expected, Collection<?> actual) {
        assertEquals(expected.size(), actual.size());
        assertTrue(expected.containsAll(actual));
    }

    @Test
    @Bug(id="954",
         summary="Thermostat GUI client should remember my last panel selected",
         url="http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=954")
    public void verifyOpenSameHostVMTab() throws Exception {

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getName()).thenReturn("testvm");
        when(vmRef.getIdString()).thenReturn("testvmid");
        HostRef ref = mock(HostRef.class);
        when(ref.getAgentId()).thenReturn("agentId");
        when(vmRef.getAgent()).thenReturn(ref);
        
        when(view.getSelectedHostOrVm()).thenReturn(vmRef);

        VmInformationController vmInformationController = mock(VmInformationController.class);
        when(vmInformationController.getSelectedChildID()).thenReturn(3);
        when(uiFacadeFactory.getVmController(any(VmRef.class))).thenReturn(vmInformationController);
        when(vmInformationController.selectChildID(anyInt())).thenReturn(true);

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
    public void verifyOpenSameHostVMTab2() {
        
        VmRef vmRef1 = mock(VmRef.class);
        VmRef vmRef2 = mock(VmRef.class);
        when(view.getSelectedHostOrVm()).thenReturn(vmRef1).thenReturn(vmRef1).thenReturn(vmRef2).thenReturn(vmRef1);

        when(vmRef1.getName()).thenReturn("testvm");
        when(vmRef1.getIdString()).thenReturn("testvmid");
        HostRef ref = mock(HostRef.class);
        when(ref.getAgentId()).thenReturn("agentId");
        when(vmRef1.getAgent()).thenReturn(ref);
        
        when(vmRef2.getName()).thenReturn("testvm");
        when(vmRef2.getIdString()).thenReturn("testvmid");
        when(vmRef2.getAgent()).thenReturn(ref);
        
        VmInformationController vmInformationController1 = mock(VmInformationController.class);
        VmInformationController vmInformationController2 = mock(VmInformationController.class);
        
        when(vmInformationController1.getSelectedChildID()).thenReturn(2).thenReturn(2);
        when(vmInformationController2.getSelectedChildID()).thenReturn(3);
        
        when(vmInformationController1.selectChildID(0)).thenReturn(true);
        when(vmInformationController1.selectChildID(2)).thenReturn(true);
        when(vmInformationController1.selectChildID(3)).thenReturn(false);
        
        when(vmInformationController2.selectChildID(0)).thenReturn(true);
        when(vmInformationController2.selectChildID(2)).thenReturn(true);
        when(vmInformationController2.selectChildID(3)).thenReturn(true);
        
        when(uiFacadeFactory.getVmController(any(VmRef.class))).
                             thenReturn(vmInformationController1).
                             thenReturn(vmInformationController2).
                             thenReturn(vmInformationController2).
                             thenReturn(vmInformationController1);

        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));

        ArgumentCaptor<Integer> arg = ArgumentCaptor.forClass(Integer.class);
        verify(vmInformationController1).selectChildID(arg.capture());
        verify(vmInformationController1, times(0)).getSelectedChildID();

        int id = arg.getValue();

        assertEquals(0, id);

        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));

        arg = ArgumentCaptor.forClass(Integer.class);
        verify(vmInformationController1).getSelectedChildID();
        verify(vmInformationController2, times(1)).selectChildID(arg.capture());
        id = arg.getValue();

        assertEquals(2, id);
        
        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));

        arg = ArgumentCaptor.forClass(Integer.class);
        verify(vmInformationController2, times(1)).getSelectedChildID();
        verify(vmInformationController2, times(2)).selectChildID(arg.capture());
        id = arg.getValue();

        assertEquals(3, id);
        
        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));

        arg = ArgumentCaptor.forClass(Integer.class);
        verify(vmInformationController2, times(2)).getSelectedChildID();
        verify(vmInformationController1, times(3)).selectChildID(arg.capture());
        id = arg.getValue();

        assertEquals(2, id);
    }
    
    @Test
    public void verityVMActionsAreShown() {
        VmInfo vmInfo = new VmInfo(0, 1, 2, null, null, null, null, null, null, null, null, null, null, null);
        when(mockVmsDAO.getVmInfo(isA(VmRef.class))).thenReturn(vmInfo);

        VmRef ref = mock(VmRef.class);
        when(view.getSelectedHostOrVm()).thenReturn(ref);

        MouseEvent uiEvent = mock(MouseEvent.class);
        ActionEvent<MainView.Action> viewEvent = new ActionEvent<>(view, MainView.Action.SHOW_VM_CONTEXT_MENU);
        viewEvent.setPayload(uiEvent);

        l.actionPerformed(viewEvent);

        verify(view).showVMContextActions(Arrays.asList(action1), uiEvent);
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
    public void verifyMenuItems() {
        
        ActionListener<ThermostatExtensionRegistry.Action> menuListener = controller.getMenuListener();

        MenuAction action = mock(MenuAction.class);
        when(action.getName()).thenReturn("Test1");

        ActionEvent<Action> addEvent = new ActionEvent<ThermostatExtensionRegistry.Action>(
        		menus, ThermostatExtensionRegistry.Action.SERVICE_ADDED);
        addEvent.setPayload(action);
        menuListener.actionPerformed(addEvent);
        verify(view).addMenu(action);

        ActionEvent<Action> removeEvent = new ActionEvent<ThermostatExtensionRegistry.Action>(menus, ThermostatExtensionRegistry.Action.SERVICE_REMOVED);
        removeEvent.setPayload(action);
        menuListener.actionPerformed(removeEvent);
        verify(view).removeMenu(action);
    }

   @Test
   public void testOSGiFrameworkShutdown() throws BundleException {

       l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.SHUTDOWN));

       verify(uiFacadeFactory).shutdown();
   }
}
