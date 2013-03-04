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

package com.redhat.thermostat.client.swing.internal;

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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleException;

import com.redhat.thermostat.client.core.Filter;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.HostInformationView;
import com.redhat.thermostat.client.core.views.HostInformationViewProvider;
import com.redhat.thermostat.client.core.views.SummaryView;
import com.redhat.thermostat.client.core.views.SummaryViewProvider;
import com.redhat.thermostat.client.core.views.VmInformationView;
import com.redhat.thermostat.client.core.views.VmInformationViewProvider;
import com.redhat.thermostat.client.osgi.service.ContextAction;
import com.redhat.thermostat.client.osgi.service.DecoratorProvider;
import com.redhat.thermostat.client.osgi.service.HostContextAction;
import com.redhat.thermostat.client.osgi.service.MenuAction;
import com.redhat.thermostat.client.osgi.service.VMContextAction;
import com.redhat.thermostat.client.ui.SummaryController;
import com.redhat.thermostat.client.ui.VmInformationController;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.common.ThermostatExtensionRegistry.Action;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.HostsVMsLoader;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.test.Bug;
import com.redhat.thermostat.testutils.StubBundleContext;

public class MainWindowControllerImplTest {

    private ActionListener<MainView.Action> l;

    private MainWindowControllerImpl controller;

    private MainView view;

    private Timer mainWindowTimer;

    private HostInfoDAO mockHostsDAO;
    private VmInfoDAO mockVmsDAO;

    private HostContextAction hostContextAction1;
    private VMContextAction vmContextAction1;
    private VMContextAction vmContextAction2;

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

    private StubBundleContext context;
    private CountDownLatch shutdown;

    private VmInformationView vmInfoView;
    private VmInformationViewProvider vmInfoViewProvider;
    
    @BeforeClass
    public static void setUpOnce() {
        // TODO remove when controller uses mocked objects rather than real swing objects
        FailOnThreadViolationRepaintManager.install();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" }) // ActionListener fluff
    @Before
    public void setUp() throws Exception {
        context = new StubBundleContext();
        
        // Setup timers
        mainWindowTimer = mock(Timer.class);
        Timer otherTimer = mock(Timer.class); // FIXME needed for SummaryView; remove later
        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(mainWindowTimer).thenReturn(otherTimer);
        ApplicationService appSvc = mock(ApplicationService.class);
        when (appSvc.getTimerFactory()).thenReturn(timerFactory);

        mockHostsDAO = mock(HostInfoDAO.class);
        context.registerService(HostInfoDAO.class, mockHostsDAO, null);
        mockVmsDAO = mock(VmInfoDAO.class);
        context.registerService(VmInfoDAO.class, mockVmsDAO, null);
        
        SummaryViewProvider summaryViewProvider = mock(SummaryViewProvider.class);
        context.registerService(SummaryViewProvider.class, summaryViewProvider, null);
        SummaryView summaryView = mock(SummaryView.class);
        when(summaryViewProvider.createView()).thenReturn(summaryView);
        
        HostInformationViewProvider hostInfoViewProvider = mock(HostInformationViewProvider.class);
        context.registerService(HostInformationViewProvider.class, hostInfoViewProvider, null);
        HostInformationView hostInfoView = mock(HostInformationView.class);
        when(hostInfoViewProvider.createView()).thenReturn(hostInfoView);
        
        vmInfoViewProvider = mock(VmInformationViewProvider.class);
        context.registerService(VmInformationViewProvider.class, vmInfoViewProvider, null);
        vmInfoView = mock(VmInformationView.class);
        when(vmInfoViewProvider.createView()).thenReturn(vmInfoView);

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
        shutdown = mock(CountDownLatch.class);

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

        setUpHostContextActions();
        setUpVMContextActions();

        controller = new MainWindowControllerImpl(context, appSvc, view, registryFactory, shutdown);
        l = grabListener.getValue();
        
        hostFiltersListener = grabHostFiltersListener.getValue();
        vmFiltersListener = grabVmFiltersListener.getValue();
        decoratorsListener = grabDecoratorsListener.getValue();
    }

    private void setUpHostContextActions() {
        hostContextAction1 = mock(HostContextAction.class);
        Filter<HostRef> hostFilter1 = mock(Filter.class);
        when(hostFilter1.matches(isA(HostRef.class))).thenReturn(true);

        when(hostContextAction1.getName()).thenReturn("action1");
        when(hostContextAction1.getDescription()).thenReturn("action1desc");
        when(hostContextAction1.getFilter()).thenReturn(hostFilter1);

        context.registerService(HostContextAction.class, hostContextAction1, null);
    }

    private void setUpVMContextActions() {
        vmContextAction1 = mock(VMContextAction.class);
        Filter action1Filter = mock(Filter.class);
        when(action1Filter.matches(isA(VmRef.class))).thenReturn(true);

        when(vmContextAction1.getName()).thenReturn("action1");
        when(vmContextAction1.getDescription()).thenReturn("action1desc");
        when(vmContextAction1.getFilter()).thenReturn(action1Filter);
        
        context.registerService(VMContextAction.class, vmContextAction1, null);
        
        vmContextAction2 = mock(VMContextAction.class);
        Filter action2Filter = mock(Filter.class);
        when(action2Filter.matches(isA(VmRef.class))).thenReturn(false);

        when(vmContextAction2.getName()).thenReturn("action2");
        when(vmContextAction2.getDescription()).thenReturn("action2desc");
        when(vmContextAction2.getFilter()).thenReturn(action2Filter);
        
        context.registerService(VMContextAction.class, vmContextAction2, null);
    }

    @After
    public void tearDown() {
        view = null;
        controller = null;
        mockHostsDAO = null;
        mockVmsDAO = null;
        l = null;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyDecoratorsAdded() {

        List<DecoratorProvider<VmRef>> currentDecoratros = controller.getVmTreeDecorators();
        assertEquals(0, currentDecoratros.size());
        
        ActionEvent<ThermostatExtensionRegistry.Action> event =
                new ActionEvent<ThermostatExtensionRegistry.Action>(vmDecoratorRegistry,
                        ThermostatExtensionRegistry.Action.SERVICE_ADDED);
        
        DecoratorProvider<VmRef> payload = mock(DecoratorProvider.class);
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
                
        Filter<VmRef> filter = controller.getVmFilter();
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
        
        when(vmInfoView.getSelectedChildID()).thenReturn(3);
        when(vmInfoView.selectChildID(anyInt())).thenReturn(true);
        
        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));
        
        ArgumentCaptor<Integer> arg = ArgumentCaptor.forClass(Integer.class);
        verify(vmInfoView).selectChildID(arg.capture());
        verify(vmInfoView, times(0)).getSelectedChildID();

        int id = arg.getValue();

        assertEquals(0, id);

        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));

        arg = ArgumentCaptor.forClass(Integer.class);
        verify(vmInfoView, times(1)).getSelectedChildID();
        verify(vmInfoView, times(2)).selectChildID(arg.capture());
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
        
        VmInformationView vmInfoView2 = mock(VmInformationView.class);
        
        when(vmInfoView.getSelectedChildID()).thenReturn(2).thenReturn(2);
        when(vmInfoView2.getSelectedChildID()).thenReturn(3);
        
        when(vmInfoView.selectChildID(0)).thenReturn(true);
        when(vmInfoView.selectChildID(2)).thenReturn(true);
        when(vmInfoView.selectChildID(3)).thenReturn(false);
        
        when(vmInfoView2.selectChildID(0)).thenReturn(true);
        when(vmInfoView2.selectChildID(2)).thenReturn(true);
        when(vmInfoView2.selectChildID(3)).thenReturn(true);
        
        when(vmInfoViewProvider.createView()).thenReturn(vmInfoView)
                .thenReturn(vmInfoView2).thenReturn(vmInfoView2)
                .thenReturn(vmInfoView);
        
        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));

        ArgumentCaptor<Integer> arg = ArgumentCaptor.forClass(Integer.class);
        verify(vmInfoView).selectChildID(arg.capture());
        verify(vmInfoView, times(0)).getSelectedChildID();

        int id = arg.getValue();

        assertEquals(0, id);

        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));

        arg = ArgumentCaptor.forClass(Integer.class);
        verify(vmInfoView).getSelectedChildID();
        verify(vmInfoView2, times(1)).selectChildID(arg.capture());
        id = arg.getValue();

        assertEquals(2, id);
        
        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));

        arg = ArgumentCaptor.forClass(Integer.class);
        verify(vmInfoView2, times(1)).getSelectedChildID();
        verify(vmInfoView2, times(2)).selectChildID(arg.capture());
        id = arg.getValue();

        assertEquals(3, id);
        
        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));

        arg = ArgumentCaptor.forClass(Integer.class);
        verify(vmInfoView2, times(2)).getSelectedChildID();
        verify(vmInfoView, times(3)).selectChildID(arg.capture());
        id = arg.getValue();

        assertEquals(2, id);
    }

    @Test
    public void verifyHostActionsAreShown() {
        HostRef host = mock(HostRef.class);
        when(view.getSelectedHostOrVm()).thenReturn(host);

        MouseEvent uiEvent = mock(MouseEvent.class);
        ActionEvent<MainView.Action> viewEvent = new ActionEvent<>(view, MainView.Action.SHOW_HOST_VM_CONTEXT_MENU);
        viewEvent.setPayload(uiEvent);

        l.actionPerformed(viewEvent);

        List<ContextAction> actions = new ArrayList<>();
        actions.add(hostContextAction1);

        verify(view).showContextActions(actions, uiEvent);
    }

    @Test
    public void verityVMActionsAreShown() {
        VmInfo vmInfo = new VmInfo(0, 1, 2, null, null, null, null, null, null, null, null, null, null, null);
        when(mockVmsDAO.getVmInfo(isA(VmRef.class))).thenReturn(vmInfo);

        VmRef ref = mock(VmRef.class);
        when(view.getSelectedHostOrVm()).thenReturn(ref);

        MouseEvent uiEvent = mock(MouseEvent.class);
        ActionEvent<MainView.Action> viewEvent = new ActionEvent<>(view, MainView.Action.SHOW_HOST_VM_CONTEXT_MENU);
        viewEvent.setPayload(uiEvent);

        l.actionPerformed(viewEvent);

        List<ContextAction> actions = new ArrayList<>();
        actions.add(vmContextAction1);

        verify(view).showContextActions(actions, uiEvent);
    }

    @Test
    public void verifyHostActionsAreExecuted() {
        HostRef hostRef = mock(HostRef.class);
        when(view.getSelectedHostOrVm()).thenReturn(hostRef);

        ActionEvent<MainView.Action> event = new ActionEvent<>(view, MainView.Action.HOST_VM_CONTEXT_ACTION);
        event.setPayload(hostContextAction1);
        l.actionPerformed(event);

        verify(hostContextAction1, times(1)).execute(hostRef);
    }

    @Test
    public void verityVMActionsAreExecuted() {

        VmRef vmRef = mock(VmRef.class);
        when(view.getSelectedHostOrVm()).thenReturn(vmRef);

        ActionEvent<MainView.Action> event = new ActionEvent<>(view, MainView.Action.HOST_VM_CONTEXT_ACTION);
        event.setPayload(vmContextAction1);
        l.actionPerformed(event);
        
        verify(vmContextAction1, times(1)).execute(any(VmRef.class));
        verify(vmContextAction2, times(0)).execute(any(VmRef.class));
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

       verify(shutdown).countDown();
   }
}

