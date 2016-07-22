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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.ExecutorService;

import com.redhat.thermostat.client.swing.internal.vmlist.controller.DecoratorListener;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.DecoratorManager;
import com.redhat.thermostat.client.ui.ReferenceFieldLabelDecorator;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.IssueDiagnoser;
import com.redhat.thermostat.client.core.Severity;
import com.redhat.thermostat.client.core.VmIssue;
import com.redhat.thermostat.client.core.views.IssueView;
import com.redhat.thermostat.client.core.views.IssueView.IssueAction;
import com.redhat.thermostat.client.core.views.IssueView.IssueDescription;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.testutils.StubBundleContext;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class IssueViewControllerTest {

    private StubBundleContext context;

    private ExecutorService executor;
    private VmInfoDAO vmInfoDao;
    private AgentInfoDAO agentInfoDao;
    private HostInfoDAO hostInfoDao;

    private IssueDiagnoser issueProvider;

    private DecoratorManager decoratorManager;
    private IssueView view;

    private IssueViewController controller;


    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        context = new StubBundleContext();

        ApplicationService appService = mock(ApplicationService.class);
        executor = mock(ExecutorService.class);
        when(appService.getApplicationExecutor()).thenReturn(executor);

        decoratorManager = mock(DecoratorManager.class);
        DecoratorListener listener = mock(DecoratorListener.class);
        ReferenceFieldLabelDecorator decorator = mock(ReferenceFieldLabelDecorator.class);
        when(decorator.getLabel(anyString(), any(Ref.class))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                return (String) invocationOnMock.getArguments()[0];
            }
        });
        when(listener.getDecorators()).thenReturn(Collections.singletonList(decorator));
        when(decoratorManager.getMainLabelDecoratorListener()).thenReturn(listener);
        view = mock(IssueView.class);

        agentInfoDao = mock(AgentInfoDAO.class);
        hostInfoDao = mock(HostInfoDAO.class);
        vmInfoDao = mock(VmInfoDAO.class);

        issueProvider = mock(IssueDiagnoser.class);

        controller = new IssueViewController(context, appService, decoratorManager, view);
    }

    @Test
    public void verifyDoesNothingWithoutStart() throws Exception {
        verify(view).setIssuesState(IssueView.IssueState.NOT_STARTED);
        verify(view, never()).addIssueActionListener(isA(ActionListener.class));
        verify(view, never()).clearIssues();
        verify(view, never()).addIssue(isA(IssueDescription.class));
    }

    @Test
    public void verifyDoesNothingWhenDaosAreNotAvailable() throws Exception {
        controller.start();

        ArgumentCaptor<ActionListener> captor = ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addIssueActionListener(captor.capture());

        ActionListener<IssueAction> listener = captor.getValue();

        listener.actionPerformed(new ActionEvent<>(view, IssueAction.SEARCH));

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submit(runnableCaptor.capture());

        runnableCaptor.getValue().run();

        verify(view).clearIssues();
        verify(view, never()).addIssue(isA(IssueDescription.class));

        controller.stop();
    }

    @Test
    public void verifySearchingForIssuesWithoutAnyResults() throws Exception {
        AgentId agentId = new AgentId("agent-id");
        VmId vmId = new VmId("vm-id");
        HostInfo hostInfo = new HostInfo(agentId.get(), "hostname", "osName", "osKernel", "cpuModel", 1, 1);
        VmInfo vmInfo = new VmInfo(agentId.get(), vmId.get(), 1, 2, 3,
                    "javaVersion", "javaHome",
                    "mainClass", "commandLine",
                    "vmName", "vmInfo", "vmVersion", "vmArguments",
                    Collections.<String,String>emptyMap(), Collections.<String,String>emptyMap(), new String[0],
                    1, "userName");
        VmIssue issue = new VmIssue(agentId, vmId, Severity.CRITICAL, "foobar");

        when(agentInfoDao.getAgentIds()).thenReturn(Collections.singleton(agentId));
        when(hostInfoDao.getHostInfo(agentId)).thenReturn(hostInfo);
        when(vmInfoDao.getVmIds(agentId)).thenReturn(Collections.singleton(vmId));
        when(vmInfoDao.getVmInfo(vmId)).thenReturn(vmInfo);
        when(issueProvider.diagnoseIssue(agentId, vmId)).thenReturn(Collections.singleton(issue));

        controller.start();

        context.registerService(AgentInfoDAO.class, agentInfoDao, null);
        context.registerService(VmInfoDAO.class, vmInfoDao, null);
        context.registerService(HostInfoDAO.class, hostInfoDao, null);

        context.registerService(IssueDiagnoser.class, issueProvider, null);

        ArgumentCaptor<ActionListener> captor = ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addIssueActionListener(captor.capture());
        verify(view).setIssuesState(IssueView.IssueState.NOT_STARTED);

        ActionListener<IssueAction> listener = captor.getValue();

        listener.actionPerformed(new ActionEvent<>(view, IssueAction.SEARCH));

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submit(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        ArgumentCaptor<IssueDescription> issueCaptor = ArgumentCaptor.forClass(IssueDescription.class);
        verify(view).clearIssues();
        verify(view).addIssue(issueCaptor.capture());
        verify(view).setIssuesState(IssueView.IssueState.ISSUES_FOUND);

        IssueDescription issueDescription = issueCaptor.getValue();
        assertEquals(String.format("%s (%s)", hostInfo.getHostname(), agentId.get()), issueDescription.agent);
        assertEquals(String.format("%s (PID: %s)", vmInfo.getMainClass(), Integer.toString(vmInfo.getVmPid())), issueDescription.vm);
        assertEquals(issue.getSeverity(), issueDescription.severity);
        assertEquals(issue.getDescription(), issueDescription.description);

        controller.stop();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIssueSelectionTriggersEvent() {
        AgentId agentId = new AgentId("agent-id");
        VmId vmId = new VmId("vm-id");
        HostInfo hostInfo = new HostInfo(agentId.get(), "hostname", "osName", "osKernel", "cpuModel", 1, 1);
        VmInfo vmInfo = new VmInfo(agentId.get(), vmId.get(), 1, 2, 3,
                "javaVersion", "javaHome",
                "mainClass", "commandLine",
                "vmName", "vmInfo", "vmVersion", "vmArguments",
                Collections.<String,String>emptyMap(), Collections.<String,String>emptyMap(), new String[0],
                1, "userName");
        VmIssue issue = new VmIssue(agentId, vmId, Severity.CRITICAL, "foobar");

        when(agentInfoDao.getAgentIds()).thenReturn(Collections.singleton(agentId));
        when(hostInfoDao.getHostInfo(agentId)).thenReturn(hostInfo);
        when(vmInfoDao.getVmIds(agentId)).thenReturn(Collections.singleton(vmId));
        when(vmInfoDao.getVmInfo(vmId)).thenReturn(vmInfo);
        when(issueProvider.diagnoseIssue(agentId, vmId)).thenReturn(Collections.singleton(issue));

        ArgumentCaptor<ActionListener> viewActionCaptor = ArgumentCaptor.forClass(ActionListener.class);
        controller.start();

        context.registerService(AgentInfoDAO.class, agentInfoDao, null);
        context.registerService(VmInfoDAO.class, vmInfoDao, null);
        context.registerService(HostInfoDAO.class, hostInfoDao, null);

        context.registerService(IssueDiagnoser.class, issueProvider, null);

        ArgumentCaptor<ActionListener> captor = ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addIssueActionListener(captor.capture());
        verify(view).setIssuesState(IssueView.IssueState.NOT_STARTED);

        ActionListener<IssueAction> listener = captor.getValue();

        listener.actionPerformed(new ActionEvent<>(view, IssueAction.SEARCH));

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submit(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        final boolean[] notified = {false};
        final Ref[] ref = new Ref[1];
        controller.addIssueSelectionListener(new ActionListener<IssueViewController.IssueSelectionAction>() {
            @Override
            public void actionPerformed(ActionEvent<IssueViewController.IssueSelectionAction> actionEvent) {
                notified[0] = true;
                ref[0] = (Ref) actionEvent.getPayload();
            }
        });

        verify(view).addIssueActionListener(viewActionCaptor.capture());
        ActionEvent event = new ActionEvent(view, IssueAction.SELECTION_CHANGED);
        event.setPayload(0);
        viewActionCaptor.getValue().actionPerformed(event);

        assertTrue(notified[0]);
        assertThat(ref[0], instanceOf(VmRef.class));
        VmRef vmRef = (VmRef) ref[0];
        assertThat(vmRef.getName(), is(vmInfo.getMainClass()));
        assertThat(vmRef.getPid(), is(vmInfo.getVmPid()));
        assertThat(vmRef.getStringID(), is(vmId.get()));
        assertThat(vmRef.getHostRef().getAgentId(), is(agentId.get()));
        assertThat(vmRef.getHostRef().getStringID(), is(agentId.get()));
        assertThat(vmRef.getHostRef().getHostName(), is(hostInfo.getHostname()));

        controller.stop();
    }
}
