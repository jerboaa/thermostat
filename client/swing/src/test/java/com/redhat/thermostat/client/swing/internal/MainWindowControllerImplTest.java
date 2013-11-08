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

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;

import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleException;

import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.client.core.views.AgentInformationViewProvider;
import com.redhat.thermostat.client.core.views.ClientConfigViewProvider;
import com.redhat.thermostat.client.core.views.HostInformationView;
import com.redhat.thermostat.client.core.views.HostInformationViewProvider;
import com.redhat.thermostat.client.core.views.SummaryView;
import com.redhat.thermostat.client.core.views.SummaryViewProvider;
import com.redhat.thermostat.client.core.views.VmInformationView;
import com.redhat.thermostat.client.core.views.VmInformationViewProvider;
import com.redhat.thermostat.client.swing.internal.registry.decorator.DecoratorRegistryController;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.ContextActionController;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.HostTreeController;
import com.redhat.thermostat.client.ui.MenuAction;
import com.redhat.thermostat.client.ui.MenuRegistry;
import com.redhat.thermostat.client.ui.ReferenceContextAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.common.ThermostatExtensionRegistry.Action;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.monitor.HostMonitor;
import com.redhat.thermostat.storage.monitor.NetworkMonitor;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.utils.keyring.Keyring;

public class MainWindowControllerImplTest {

    private ActionListener<MainView.Action> l;
    
    private MainWindowControllerImpl controller;

    private MainView view;

    private Timer mainWindowTimer;

    private HostInfoDAO mockHostsDAO;
    private VmInfoDAO mockVmsDAO;

    private HostFilterRegistry hostFilterRegistry;
    private VmFilterRegistry vmFilterRegistry;

    private VMInformationRegistry vmInfoRegistry;
    private MenuRegistry menus;

    private StubBundleContext context;
    private CountDownLatch shutdown;

    private VmInformationView vmInfoView;
    private VmInformationViewProvider vmInfoViewProvider;
    
    private HostTreeController treeController;
    private DecoratorRegistryController decoratorController;
    
    private ContextActionController contextController;
    
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

        Keyring keyring = mock(Keyring.class);
        context.registerService(Keyring.class, keyring, null);
        
        mockHostsDAO = mock(HostInfoDAO.class);
        context.registerService(HostInfoDAO.class, mockHostsDAO, null);
        mockVmsDAO = mock(VmInfoDAO.class);
        context.registerService(VmInfoDAO.class, mockVmsDAO, null);
        
        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        context.registerService(AgentInfoDAO.class, agentInfoDAO, null);
        BackendInfoDAO backendInfoDAO = mock(BackendInfoDAO.class);
        context.registerService(BackendInfoDAO.class, backendInfoDAO, null);
        
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
        
        AgentInformationViewProvider agentInfoViewProvider = mock(AgentInformationViewProvider.class);
        context.registerService(AgentInformationViewProvider.class, agentInfoViewProvider, null);
        ClientConfigViewProvider clientConfigViewProvider = mock(ClientConfigViewProvider.class);
        context.registerService(ClientConfigViewProvider.class, clientConfigViewProvider, null);

        HostMonitor hostMonitor = mock(HostMonitor.class);
        context.registerService(HostMonitor.class, hostMonitor, null);
        NetworkMonitor networkMonitor = mock(NetworkMonitor.class);
        context.registerService(NetworkMonitor.class, networkMonitor, null);

        // Setup View
        view = mock(MainView.class);
        ArgumentCaptor<ActionListener> grabListener = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(grabListener.capture());
        
        contextController = mock(ContextActionController.class);
        when(view.getContextActionController()).thenReturn(contextController);
        
        treeController = mock(HostTreeController.class);
        ArgumentCaptor<ActionListener> hostTreeCaptor = ArgumentCaptor.forClass(ActionListener.class);
        when(view.getHostTreeController()).thenReturn(treeController);
        
        doNothing().when(treeController).addReferenceSelectionChangeListener(hostTreeCaptor.capture());

        ProgressNotifier notifier = mock(ProgressNotifier.class);
        when(view.getNotifier()).thenReturn(notifier);
        
        RegistryFactory registryFactory = mock(RegistryFactory.class);
        hostFilterRegistry = mock(HostFilterRegistry.class);
        vmFilterRegistry = mock(VmFilterRegistry.class);

        vmInfoRegistry = mock(VMInformationRegistry.class);
        menus = mock(MenuRegistry.class);
        shutdown = mock(CountDownLatch.class);

        decoratorController = mock(DecoratorRegistryController.class);
        
        when(registryFactory.createMenuRegistry()).thenReturn(menus);
        when(registryFactory.createHostFilterRegistry()).thenReturn(hostFilterRegistry);
        when(registryFactory.createVmFilterRegistry()).thenReturn(vmFilterRegistry);
        when(registryFactory.createVMInformationRegistry()).thenReturn(vmInfoRegistry);
        when(registryFactory.createDecoratorController()).thenReturn(decoratorController);
        
        ArgumentCaptor<ActionListener> grabHostFiltersListener = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(hostFilterRegistry).addActionListener(grabHostFiltersListener.capture());

        ArgumentCaptor<ActionListener> grabVmFiltersListener = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(vmFilterRegistry).addActionListener(grabVmFiltersListener.capture());
        
        ArgumentCaptor<ActionListener> grabInfoRegistry = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(vmInfoRegistry).addActionListener(grabInfoRegistry.capture());

        controller = new MainWindowControllerImpl(context, appSvc, view, registryFactory, shutdown);
        
        l = grabListener.getValue();
    }

    @After
    public void tearDown() {
        view = null;
        controller = null;
        mockHostsDAO = null;
        mockVmsDAO = null;
        l = null;
    }

    @Test
    public void verifyDecoratorsControllerRegisteredAndStarted() {
        
        controller.shutdownApplication();

        verify(view, atLeastOnce()).getHostTreeController();
        verify(decoratorController, times(1)).init(treeController);
        verify(decoratorController, times(1)).start();
        verify(decoratorController, times(1)).stop();
    }
    
    @Test
    public void verifyShowMainWindowActuallyCallsView() {
        controller.showMainMainWindow();
        verify(view).showMainWindow();
    }

//    @Test
//    @Bug(id="954",
//         summary="Thermostat GUI client should remember my last panel selected",
//         url="http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=954")
//    public void verifyOpenSameHostVMTab() throws Exception {
//
//        VmRef vmRef = mock(VmRef.class);
//        when(vmRef.getName()).thenReturn("testvm");
//        when(vmRef.getVmId()).thenReturn("testvmid");
//        HostRef ref = mock(HostRef.class);
//        when(ref.getAgentId()).thenReturn("agentId");
//        when(vmRef.getHostRef()).thenReturn(ref);
//        
//        when(view.getSelectedHostOrVm()).thenReturn(vmRef);
//        
//        when(vmInfoView.getSelectedChildID()).thenReturn(3);
//        when(vmInfoView.selectChildID(anyInt())).thenReturn(true);
//        
//        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));
//        
//        ArgumentCaptor<Integer> arg = ArgumentCaptor.forClass(Integer.class);
//        verify(vmInfoView).selectChildID(arg.capture());
//        verify(vmInfoView, times(0)).getSelectedChildID();
//
//        int id = arg.getValue();
//
//        assertEquals(0, id);
//
//        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));
//
//        arg = ArgumentCaptor.forClass(Integer.class);
//        verify(vmInfoView, times(1)).getSelectedChildID();
//        verify(vmInfoView, times(2)).selectChildID(arg.capture());
//        id = arg.getValue();
//
//        assertEquals(3, id);
//    }
//    
//    @Test
//    public void verifyOpenSameHostVMTab2() {
//        
//        VmRef vmRef1 = mock(VmRef.class);
//        VmRef vmRef2 = mock(VmRef.class);
//        when(view.getSelectedHostOrVm()).thenReturn(vmRef1).thenReturn(vmRef1).thenReturn(vmRef2).thenReturn(vmRef1);
//
//        when(vmRef1.getName()).thenReturn("testvm");
//        when(vmRef1.getVmId()).thenReturn("testvmid");
//        HostRef ref = mock(HostRef.class);
//        when(ref.getAgentId()).thenReturn("agentId");
//        when(vmRef1.getHostRef()).thenReturn(ref);
//        
//        when(vmRef2.getName()).thenReturn("testvm");
//        when(vmRef2.getVmId()).thenReturn("testvmid");
//        when(vmRef2.getHostRef()).thenReturn(ref);
//        
//        VmInformationView vmInfoView2 = mock(VmInformationView.class);
//        
//        when(vmInfoView.getSelectedChildID()).thenReturn(2).thenReturn(2);
//        when(vmInfoView2.getSelectedChildID()).thenReturn(3);
//        
//        when(vmInfoView.selectChildID(0)).thenReturn(true);
//        when(vmInfoView.selectChildID(2)).thenReturn(true);
//        when(vmInfoView.selectChildID(3)).thenReturn(false);
//        
//        when(vmInfoView2.selectChildID(0)).thenReturn(true);
//        when(vmInfoView2.selectChildID(2)).thenReturn(true);
//        when(vmInfoView2.selectChildID(3)).thenReturn(true);
//        
//        when(vmInfoViewProvider.createView()).thenReturn(vmInfoView)
//                .thenReturn(vmInfoView2).thenReturn(vmInfoView2)
//                .thenReturn(vmInfoView);
//        
//        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));
//
//        ArgumentCaptor<Integer> arg = ArgumentCaptor.forClass(Integer.class);
//        verify(vmInfoView).selectChildID(arg.capture());
//        verify(vmInfoView, times(0)).getSelectedChildID();
//
//        int id = arg.getValue();
//
//        assertEquals(0, id);
//
//        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));
//
//        arg = ArgumentCaptor.forClass(Integer.class);
//        verify(vmInfoView).getSelectedChildID();
//        verify(vmInfoView2, times(1)).selectChildID(arg.capture());
//        id = arg.getValue();
//
//        assertEquals(2, id);
//        
//        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));
//
//        arg = ArgumentCaptor.forClass(Integer.class);
//        verify(vmInfoView2, times(1)).getSelectedChildID();
//        verify(vmInfoView2, times(2)).selectChildID(arg.capture());
//        id = arg.getValue();
//
//        assertEquals(3, id);
//        
//        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.HOST_VM_SELECTION_CHANGED));
//
//        arg = ArgumentCaptor.forClass(Integer.class);
//        verify(vmInfoView2, times(2)).getSelectedChildID();
//        verify(vmInfoView, times(3)).selectChildID(arg.capture());
//        id = arg.getValue();
//
//        assertEquals(2, id);
//    }

    @Test
    public void verifyMenuItems() {
        
        ActionListener<ThermostatExtensionRegistry.Action> menuListener = controller.getMenuListener();

        MenuAction action = mock(MenuAction.class);
        when(action.getName()).thenReturn(new LocalizedString("Test1"));

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

