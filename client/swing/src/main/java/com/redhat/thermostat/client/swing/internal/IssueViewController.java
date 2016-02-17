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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.client.core.AgentIssue;
import com.redhat.thermostat.client.core.Issue;
import com.redhat.thermostat.client.core.IssueDiagnoser;
import com.redhat.thermostat.client.core.VmIssue;
import com.redhat.thermostat.client.core.views.IssueView;
import com.redhat.thermostat.client.core.views.IssueView.IssueAction;
import com.redhat.thermostat.client.core.views.IssueView.IssueDescription;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;

public class IssueViewController {

    private static final Logger logger = LoggingUtils.getLogger(IssueViewController.IssueDiagnoserTracker.class);

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    static class IssueDiagnoserTracker extends ServiceTracker {

        private final CopyOnWriteArrayList<IssueDiagnoser> diagnosers = new CopyOnWriteArrayList<>();

        @SuppressWarnings("unchecked")
        public IssueDiagnoserTracker(BundleContext context) {
            super(context, IssueDiagnoser.class.getName(), null);
        }

        @Override
        public Object addingService(ServiceReference reference) {
            IssueDiagnoser service = (IssueDiagnoser) super.addingService(reference);
            diagnosers.add(service);
            return service;
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            diagnosers.remove(service);
            super.removedService(reference, service);
        }

        public Collection<IssueDiagnoser> getDiagnosers() {
            return Collections.unmodifiableList(diagnosers);
        }
    }

    static class DaoTracker<T> extends ServiceTracker {

        private AtomicReference<T> dao = new AtomicReference<>();

        public DaoTracker(BundleContext context, Class<T> klass) {
            super(context, klass.getName(), null);
        }

        @Override
        public T addingService(ServiceReference reference) {
            T service = (T) super.addingService(reference);
            if (dao.get() != null) {
                throw new AssertionError("this code is not robust if more than one dao ever appears");
            }
            dao.set(service);
            return service;
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            dao.set(null);
            super.removedService(reference, service);
        }

        public T getDao() {
            return dao.get();
        }

    }

    private final IssueView view;

    private IssueDiagnoserTracker issueTracker;

    private DaoTracker<AgentInfoDAO> agentInfoDaoTracker;
    private DaoTracker<HostInfoDAO> hostInfoDaoTracker;
    private DaoTracker<VmInfoDAO> vmInfoDaoTracker;

    private ActionListener<IssueAction> actionListener;

    private ExecutorService executor;

    public IssueViewController(BundleContext context, ApplicationService appService, final IssueView view) {
        this.executor = appService.getApplicationExecutor();
        this.view = view;

        view.showInitialView();

        issueTracker = new IssueDiagnoserTracker(context);

        agentInfoDaoTracker = new DaoTracker<>(context, AgentInfoDAO.class);
        hostInfoDaoTracker = new DaoTracker<>(context, HostInfoDAO.class);
        vmInfoDaoTracker = new DaoTracker<>(context, VmInfoDAO.class);

        actionListener = new ActionListener<IssueAction>() {
            @Override
            public void actionPerformed(com.redhat.thermostat.common.ActionEvent<IssueAction> actionEvent) {
                if (view.isInitialView()) {
                    view.showIssues();
                }

                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        Collection<Issue> issues = findIssues();
                        Collection<IssueDescription> descriptions = makeDescriptions(issues);

                        view.clearIssues();
                        for (IssueDescription description: descriptions) {
                            view.addIssue(description);
                        }
                    }
                };
                executor.submit(task);
            }
        };
    }

    private Collection<Issue> findIssues() {
        AgentInfoDAO agentInfoDao = agentInfoDaoTracker.getDao();
        VmInfoDAO vmInfoDao = vmInfoDaoTracker.getDao();
        List<Issue> result = new ArrayList<>();
        if (agentInfoDao != null && vmInfoDao != null) {
            for (IssueDiagnoser diagnoser : issueTracker.getDiagnosers()) {
                Set<AgentId> agentIds = agentInfoDao.getAgentIds();
                for (AgentId agentId : agentIds) {
                    Set<VmId> vmIds = vmInfoDao.getVmIds(agentId);
                    result.addAll(diagnoser.diagnoseIssue(agentId));
                    for (VmId vmId : vmIds) {
                        result.addAll(diagnoser.diagnoseIssue(agentId, vmId));
                    }
                }
            }
        } else {
            logger.warning("no dao available");
        }
        return result;
    }

    private Collection<IssueDescription> makeDescriptions(Collection<Issue> issues) {
        HostInfoDAO hostDao = hostInfoDaoTracker.getDao();
        AgentInfoDAO agentInfoDao = agentInfoDaoTracker.getDao();
        VmInfoDAO vmInfoDao = vmInfoDaoTracker.getDao();
        List<IssueDescription> result = new ArrayList<>(issues.size());
        for (Issue issue : issues) {
            if (issue instanceof AgentIssue) {
                AgentIssue agentIssue = (AgentIssue) issue;
                String prettyAgentName = createPrettyAgentName(agentInfoDao, hostDao, agentIssue.getAgentId()).getContents();
                IssueDescription description = new IssueDescription(
                        issue.getSeverity(),
                        prettyAgentName,
                        "",
                        issue.getDescription());
                result.add(description);
            } else if (issue instanceof VmIssue) {
                VmIssue vmIssue = (VmIssue) issue;
                String prettyAgentName = createPrettyAgentName(agentInfoDao, hostDao, vmIssue.getAgentId()).getContents();
                IssueDescription description = new IssueDescription(
                        issue.getSeverity(),
                        prettyAgentName,
                        createPrettyVmName(vmInfoDao, vmIssue.getAgentId(), vmIssue.getVmId()).getContents(),
                        issue.getDescription());
                result.add(description);
            }
        }
        return result;
    }


    private LocalizedString createPrettyAgentName(AgentInfoDAO agentDao, HostInfoDAO hostDao, AgentId agentId) {
        return translator.localize(LocaleResources.ISSUES_AGENT_FORMAT,
                hostDao.getHostInfo(agentId).getHostname(), agentId.get());
    }

    private LocalizedString createPrettyVmName(VmInfoDAO dao, AgentId agentId, VmId vmId) {
        // FIXME we need a generic name here. ideally same as that in the vm tree
        return translator.localize(LocaleResources.ISSUES_VM_FORMAT,
                dao.getVmInfo(vmId).getMainClass(), vmId.get());
    }

    public void start() {
        vmInfoDaoTracker.open();
        agentInfoDaoTracker.open();
        hostInfoDaoTracker.open();
        issueTracker.open();

        view.addIssueActionListener(actionListener);

    }

    public void stop() {
        view.removeIssueActionListener(actionListener);

        issueTracker.close();
        hostInfoDaoTracker.close();
        agentInfoDaoTracker.close();
        vmInfoDaoTracker.close();
    }

    public IssueView getView() {
        return view;
    }

}
