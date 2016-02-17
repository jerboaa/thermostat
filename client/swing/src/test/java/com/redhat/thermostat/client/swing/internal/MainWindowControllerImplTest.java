/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.redhat.thermostat.client.swing.internal.search.ReferenceFieldSearchFilter;
import com.redhat.thermostat.common.Filter;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
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
import com.redhat.thermostat.client.core.views.IssueView;
import com.redhat.thermostat.client.core.views.IssueViewProvider;
import com.redhat.thermostat.client.core.views.VersionAndInfoView;
import com.redhat.thermostat.client.core.views.VersionAndInfoViewProvider;
import com.redhat.thermostat.client.core.views.VmInformationView;
import com.redhat.thermostat.client.core.views.VmInformationViewProvider;
import com.redhat.thermostat.client.swing.internal.MainWindowControllerImpl.UriOpener;
import com.redhat.thermostat.client.swing.internal.registry.decorator.DecoratorRegistryController;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.ContextActionController;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.HostTreeController;
import com.redhat.thermostat.client.ui.MenuAction;
import com.redhat.thermostat.client.ui.MenuRegistry;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.common.ThermostatExtensionRegistry.Action;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.shared.config.CommonPaths;
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

    private UriOpener uriOpener;

    private HostInfoDAO mockHostsDAO;
    private VmInfoDAO mockVmsDAO;

    private ReferenceFilterRegistry hostFilterRegistry;

    private MenuRegistry menus;

    private StubBundleContext context;
    private CountDownLatch shutdown;

    private VmInformationView vmInfoView;
    private VmInformationViewProvider vmInfoViewProvider;
    
    private HostTreeController treeController;
    private DecoratorRegistryController decoratorController;
    
    private ContextActionController contextController;
    private ReferenceFieldSearchFilter referenceFieldSearchFilter;
    private HostMonitor hostMonitor;
    private NetworkMonitor networkMonitor;
    private HostRef host;
    private List<HostRef> hosts;
    private VmRef vm1;
    private VmRef vm2;
    private List<VmRef> vms;

    @SuppressWarnings({ "unchecked", "rawtypes" }) // ActionListener fluff
    @Before
    public void setUp() throws Exception {
        context = new StubBundleContext();

        uriOpener = mock(UriOpener.class);
                
        // Setup timers
        TimerFactory timerFactory = mock(TimerFactory.class);
        ApplicationService appSvc = mock(ApplicationService.class);
        when (appSvc.getTimerFactory()).thenReturn(timerFactory);

        Keyring keyring = mock(Keyring.class);
        context.registerService(Keyring.class, keyring, null);
        CommonPaths paths = mock(CommonPaths.class);
        when(paths.getUserClientConfigurationFile()).thenReturn(mock(File.class));
        context.registerService(CommonPaths.class, paths, null);
        
        mockHostsDAO = mock(HostInfoDAO.class);
        context.registerService(HostInfoDAO.class, mockHostsDAO, null);
        mockVmsDAO = mock(VmInfoDAO.class);
        context.registerService(VmInfoDAO.class, mockVmsDAO, null);
        
        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        context.registerService(AgentInfoDAO.class, agentInfoDAO, null);
        BackendInfoDAO backendInfoDAO = mock(BackendInfoDAO.class);
        context.registerService(BackendInfoDAO.class, backendInfoDAO, null);
        
        VersionAndInfoViewProvider summaryViewProvider = mock(VersionAndInfoViewProvider.class);
        context.registerService(VersionAndInfoViewProvider.class, summaryViewProvider, null);
        VersionAndInfoView versionAndInfoView = mock(VersionAndInfoView.class);
        when(summaryViewProvider.createView()).thenReturn(versionAndInfoView);

        IssueViewProvider issueViewProvider = mock(IssueViewProvider.class);
        context.registerService(IssueViewProvider.class, issueViewProvider, null);
        IssueView issueView = mock(IssueView.class);
        when(issueViewProvider.createView()).thenReturn(issueView);
        
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

        hostMonitor = mock(HostMonitor.class);
        context.registerService(HostMonitor.class, hostMonitor, null);
        networkMonitor = mock(NetworkMonitor.class);
        context.registerService(NetworkMonitor.class, networkMonitor, null);

        host = mock(HostRef.class);
        hosts = Collections.singletonList(host);
        when(networkMonitor.getHosts(any(Filter.class))).thenReturn(hosts);

        vm1 = mock(VmRef.class);
        vm2 = mock(VmRef.class);
        vms = Arrays.asList(vm1, vm2);
        when(hostMonitor.getVirtualMachines(any(HostRef.class), any(Filter.class))).thenReturn(vms);

        // Setup View
        view = mock(MainView.class);
        ArgumentCaptor<ActionListener> grabListener = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(grabListener.capture());
        
        contextController = mock(ContextActionController.class);
        when(view.getContextActionController()).thenReturn(contextController);

        referenceFieldSearchFilter = mock(ReferenceFieldSearchFilter.class);
        when(view.getSearchFilter()).thenReturn(referenceFieldSearchFilter);
        
        treeController = mock(HostTreeController.class);
        ArgumentCaptor<ActionListener> hostTreeCaptor = ArgumentCaptor.forClass(ActionListener.class);
        when(view.getHostTreeController()).thenReturn(treeController);
        
        doNothing().when(treeController).addReferenceSelectionChangeListener(hostTreeCaptor.capture());

        ProgressNotifier notifier = mock(ProgressNotifier.class);
        when(view.getNotifier()).thenReturn(notifier);
        
        RegistryFactory registryFactory = mock(RegistryFactory.class);
        hostFilterRegistry = mock(ReferenceFilterRegistry.class);

        menus = mock(MenuRegistry.class);
        shutdown = mock(CountDownLatch.class);

        decoratorController = mock(DecoratorRegistryController.class);
        
        when(registryFactory.createMenuRegistry()).thenReturn(menus);
        when(registryFactory.createFilterRegistry()).thenReturn(hostFilterRegistry);
        when(registryFactory.createDecoratorController()).thenReturn(decoratorController);
        
        ArgumentCaptor<ActionListener> grabHostFiltersListener = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(hostFilterRegistry).addActionListener(grabHostFiltersListener.capture());

        controller = new MainWindowControllerImpl(context, appSvc, view, registryFactory, shutdown, uriOpener);
        
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

    @Test @SuppressWarnings("unchecked")
    public void verifyShowMainWindowActuallyCallsView() {
        controller.showMainMainWindow();
        verify(view).showMainWindow();
        verify(referenceFieldSearchFilter).addHosts(any(Collection.class));
        verify(referenceFieldSearchFilter).addVMs(eq(host), any(Collection.class));
        verify(treeController).registerHost(host);
        verify(treeController).registerVM(vm1);
        verify(treeController).registerVM(vm2);
    }

    @Test
    public void verifyShowUserGuideEvent() throws Exception {
        l.actionPerformed(new ActionEvent<MainView.Action>(view, MainView.Action.SHOW_USER_GUIDE));

        verify(uriOpener).open(isA(URI.class));
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

