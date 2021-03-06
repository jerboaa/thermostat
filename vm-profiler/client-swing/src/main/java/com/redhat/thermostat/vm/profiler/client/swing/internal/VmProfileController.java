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

package com.redhat.thermostat.vm.profiler.client.swing.internal;

import static com.redhat.thermostat.vm.profiler.client.swing.internal.VmProfileView.TabbedPaneAction.TABLE_TAB_SELECTED;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.progress.ProgressHandle;
import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo.AliveStatus;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult.MethodInfo;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResultParser;
import com.redhat.thermostat.vm.profiler.client.swing.internal.VmProfileView.TabbedPaneAction;
import com.redhat.thermostat.vm.profiler.client.swing.internal.VmProfileView.Profile;
import com.redhat.thermostat.vm.profiler.client.swing.internal.VmProfileView.ProfileAction;
import com.redhat.thermostat.vm.profiler.client.swing.internal.VmProfileView.ProfilingState;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileInfo;
import com.redhat.thermostat.vm.profiler.common.ProfileRequest;
import com.redhat.thermostat.vm.profiler.common.ProfileStatusChange;

public class VmProfileController implements InformationServiceController<VmRef> {

    static final String STATE_MAP_KEY = "vmProfileControllerRestoreBundle";
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final Map<VmRef, SaveState> STATE_BUNDLE_MAP = new HashMap<>();

    private final ApplicationService service;
    private final ProgressNotifier notifier;
    private final ProfileDAO profileDao;
    private final AgentInfoDAO agentInfoDao;
    private final VmInfoDAO vmInfoDao;
    private final RequestQueue queue;
    private final VmRef vm;

    private VmProfileView view;
    private VmProfileTreeMapView treeMapView;

    private TabbedPaneAction lastTabSelection;
    private ProfilingResult selectedResult;

    private Timer updater;

    private Clock clock;

    private boolean profilingStartOrStopRequested = false;
    private ProfileStatusChange previousStatus;
    private ProfilingState profilingState = ProfilingState.STOPPED;

    private ProgressHandle progressDisplay;


    public VmProfileController(ApplicationService service, ProgressNotifier notifier,
            AgentInfoDAO agentInfoDao, VmInfoDAO vmInfoDao, ProfileDAO dao,
            RequestQueue queue, VmProfileTreeMapViewProvider treeMapViewProvider, VmRef vm, UIDefaults uiDefaults) {
        this(service, notifier, agentInfoDao, vmInfoDao, dao, queue, new SystemClock(),
                new SwingVmProfileView(uiDefaults), vm, treeMapViewProvider);
    }

    VmProfileController(final ApplicationService service, ProgressNotifier notifier,
            AgentInfoDAO agentInfoDao, VmInfoDAO vmInfoDao, ProfileDAO dao,
            RequestQueue queue, Clock clock, final VmProfileView view, VmRef vm,
            VmProfileTreeMapViewProvider treeMapViewProvider) {
        this.service = service;
        this.notifier = notifier;
        this.agentInfoDao = agentInfoDao;
        this.vmInfoDao = vmInfoDao;
        this.profileDao = dao;
        this.queue = queue;
        this.clock = clock;
        this.view = view;
        this.vm = vm;

        if (service.getApplicationCache().getAttribute(STATE_MAP_KEY) == null) {
            service.getApplicationCache().addAttribute(STATE_MAP_KEY, STATE_BUNDLE_MAP);
        }

        this.treeMapView = Objects.requireNonNull(treeMapViewProvider.createView());
        view.addTabToTabbedPane(translator.localize(LocaleResources.PROFILER_RESULTS_TREEMAP),
                ((SwingComponent) this.treeMapView).getUiComponent());
        this.lastTabSelection = TABLE_TAB_SELECTED;

        // TODO dispose the timer when done
        updater = service.getTimerFactory().createTimer();
        updater.setSchedulingType(SchedulingType.FIXED_DELAY);
        updater.setInitialDelay(0);
        updater.setDelay(5);
        updater.setTimeUnit(TimeUnit.SECONDS);
        updater.setAction(new Runnable() {
            @Override
            public void run() {
                updateViewWithCurrentProfilingStatus();
            }

        });

        view.addActionListener(new ActionListener<BasicView.Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case HIDDEN:
                        saveState();
                        updater.stop();
                        break;
                    case VISIBLE:
                        restoreState();
                        boolean isAlive = isAlive();
                        view.setViewControlsEnabled(isAlive);
                        // When the VM we've selected is dead then the overlay would
                        // be empty and the table wouldn't show past results 
                        // if we didn't load latest profiling data. For live
                        // JVMs we load them after each stopped profiling event.
                        if (!isAlive) {
                            loadLatestProfileData();
                        }
                        updater.start();
                        break;
                    default:
                        throw new AssertionError("Unknown action event: " + actionEvent);
                }
            }
        });

        view.addProfileActionListener(new ActionListener<VmProfileView.ProfileAction>() {
            @Override
            public void actionPerformed(ActionEvent<ProfileAction> actionEvent) {
                ProfileAction id = actionEvent.getActionId();
                switch (id) {
                    case START_PROFILING:
                        startProfiling();
                        break;
                    case STOP_PROFILING:
                        stopProfiling();
                        break;
                    case PROFILE_SELECTED:
                        loadSelectedProfileRunData();
                        updateViewWithSelectedProfileRunData();
                        break;
                    case DISPLAY_PROFILING_SESSIONS:
                        displayProfiledRuns();
                        break;
                    case PROFILE_TABLE_FILTER_CHANGED:
                        if (selectedResult != null) {
                            String filterForTable = view.getProfilingDataFilter();
                            updateViewWithSelectedProfileRunData(filterForTable);
                        }
                        break;
                    default:
                        throw new AssertionError("Unknown event: " + id);
                }
            }
        });

        view.setTabbedPaneActionListener(new ActionListener<TabbedPaneAction>() {
            @Override
            public void actionPerformed(ActionEvent<TabbedPaneAction> actionEvent) {
                TabbedPaneAction id = actionEvent.getActionId();
                boolean validTabSelection = Arrays.asList(VmProfileView.TabbedPaneAction.values()).contains(id);

                if (validTabSelection && selectedResult != null) {
                    lastTabSelection = id;
                    updateViewWithSelectedProfileRunData();
                } else if (validTabSelection) {
                    lastTabSelection = id;
                } else {
                    throw new AssertionError("Unknown event: " + id);
                }
            }
        });

        view.setViewControlsEnabled(isAlive());
    }

    private void saveState() {
        SaveState bundle = new SaveState(previousStatus, profilingStartOrStopRequested, profilingState, selectedResult);
        Map<VmRef, SaveState> map = ((Map<VmRef, SaveState>) service.getApplicationCache().getAttribute(STATE_MAP_KEY));
        map.put(vm, bundle);
        service.getApplicationCache().addAttribute(STATE_MAP_KEY, map);
    }

    private void restoreState() {
        SaveState bundle = ((Map<VmRef, SaveState>) service.getApplicationCache().getAttribute(STATE_MAP_KEY)).get(vm);
        profilingStartOrStopRequested = bundle != null && bundle.isProfilingStartOrStopRequested();
        previousStatus = bundle == null ? null : bundle.getProfileStatusChange();
        profilingState = bundle == null ? ProfilingState.STOPPED : bundle.getProfilingState();
        selectedResult = bundle == null ? null : bundle.getProfilingResult();
        if (previousStatus != null) {
            profilingState = getProfilingState(previousStatus, profilingStartOrStopRequested);
        }
        view.setProfilingState(profilingState);

        if (selectedResult != null) {
            view.setProfilingDetailData(selectedResult);
        }
    }

    private void startProfiling() {
        profilingState = ProfilingState.STARTING;
        view.setProfilingState(profilingState);
        setProgressNotificationAndSendRequest(true);
    }

    private void stopProfiling() {
        profilingState = ProfilingState.STOPPING;
        view.setProfilingState(profilingState);
        setProgressNotificationAndSendRequest(false);
    }

    private void setProgressNotificationAndSendRequest(boolean start) {
        showProgressNotification(start);
        sendProfilingRequest(start);
    }

    private void sendProfilingRequest(final boolean start) {
        AgentId agentId = new AgentId(vm.getHostRef().getAgentId());
        InetSocketAddress address = agentInfoDao.getAgentInformation(agentId).getRequestQueueAddress();
        final String action = start ? ProfileRequest.START_PROFILING : ProfileRequest.STOP_PROFILING;
        Request req = ProfileRequest.create(address, vm.getVmId(), action);
        req.addListener(new RequestResponseListener() {
            @Override
            public void fireComplete(Request request, Response response) {
                switch (response.getType()) {
                    case OK:
                        updateViewWithCurrentProfilingStatus();
                        if (action == ProfileRequest.STOP_PROFILING) {
                            loadLatestProfileData();
                        }
                        break;
                    default:
                        view.displayErrorMessage(translator.localize(LocaleResources.PROFILER_ERROR));
                        hideProgressNotificationIfVisible();
                        profilingStartOrStopRequested = false;
                        profilingState = ProfilingState.DISABLED;
                        view.setProfilingState(profilingState);
                        view.setViewControlsEnabled(false);
                        break;
                }
            }
        });
        queue.putRequest(req);
        profilingStartOrStopRequested = true;
    }

    private void updateViewWithCurrentProfilingStatus() {
        ProfileStatusChange currentStatus = profileDao.getLatestStatus(vm);
        if (currentStatus != null) {
            profilingState = getProfilingState(currentStatus, profilingStartOrStopRequested);
        }

        if (!isAlive()) {
            view.setProfilingState(ProfilingState.DISABLED);
        } else if (profilingStartOrStopRequested) {
            boolean statusChanged = (previousStatus == null && currentStatus != null)
                    || (currentStatus != null && !(currentStatus.equals(previousStatus)));
            if (statusChanged) {
                view.setProfilingState(profilingState);
                profilingStartOrStopRequested = false;
                hideProgressNotificationIfVisible();
            }
        } else {
            view.setProfilingState(profilingState);
        }

        previousStatus = currentStatus;
    }

    private ProfilingState getProfilingState(ProfileStatusChange profileStatusChange, boolean profilingStartOrStopRequested) {
        ProfilingState profilingState;
        boolean currentlyActive = profileStatusChange.isStarted();
        if (currentlyActive && profilingStartOrStopRequested) {
            profilingState = ProfilingState.STARTING;
        } else if (currentlyActive) {
            profilingState = ProfilingState.STARTED;
        } else if (profilingStartOrStopRequested) {
            profilingState = ProfilingState.STOPPING;
        } else {
            profilingState = ProfilingState.STOPPED;
        }
        return profilingState;
    }

    private void showProgressNotification(boolean start) {
        if (start) {
            progressDisplay = new ProgressHandle(translator.localize(LocaleResources.STARTING_PROFILING));
        } else {
            progressDisplay = new ProgressHandle(translator.localize(LocaleResources.STOPPING_PROFILING));
        }
        progressDisplay.setIndeterminate(true);
        notifier.register(progressDisplay);
        progressDisplay.start();
    }

    private void hideProgressNotificationIfVisible() {
        if (progressDisplay != null) {
            progressDisplay.stop();
            progressDisplay = null;
        }
    }

    private boolean isAlive() {
        AgentId agent = new AgentId(vm.getHostRef().getAgentId());
        AgentInformation agentInfo = agentInfoDao.getAgentInformation(agent);
        if (!agentInfo.isAlive()) {
            return false;
        }
        return vmInfoDao.getVmInfo(vm).isAlive(agentInfo) == AliveStatus.RUNNING;
    }

    private void updateViewWithProfiledRuns() {
        long end = clock.getRealTimeMillis();
        long start = end - TimeUnit.DAYS.toMillis(1); // FIXME hardcoded 1 day

        List<ProfileInfo> profileInfos = profileDao.getAllProfileInfo(vm, new Range<>(start, end));
        List<Profile> profiles = new ArrayList<>();
        for (ProfileInfo profileInfo : profileInfos) {
            Profile profile = new Profile(profileInfo.getProfileId(),
                    profileInfo.getStartTimeStamp(), profileInfo.getStopTimeStamp());
            profiles.add(profile);
        }

        Collections.sort(profiles, new ByTimeStamp());

        view.setAvailableProfilingRuns(profiles);
    }

    private void displayProfiledRuns() {
        view.setDisplayProfilingRuns(true);
    }

    private void loadSelectedProfileRunData() {
        String profileId = view.getSelectedProfile().name;
        InputStream in = profileDao.loadProfileDataById(vm, profileId);
        selectedResult = new ProfilingResultParser().parse(in);
    }

    private void loadLatestProfileData() {
        service.getApplicationExecutor().submit(new Runnable() {
            @Override
            public void run() {
                AgentId agentId = new AgentId(vm.getHostRef().getAgentId());
                VmId vmId = new VmId(vm.getVmId());
                InputStream in = profileDao.loadLatestProfileData(agentId, vmId);
                if (in == null) {
                    // this method is called after a profiling stop event,
                    // there may be some elapsed time between the data is available in
                    // the database for retrieval, so the method is called again if
                    // there data is still not around, and then again
                    loadLatestProfileData();
                    return;
                }
                updateViewWithProfiledRuns();
                selectedResult = new ProfilingResultParser().parse(in);
                updateViewWithSelectedProfileRunData();
            }
        });
    }

    private void updateViewWithSelectedProfileRunData() {
        updateViewWithSelectedProfileRunData(null);
    }

    private void updateViewWithSelectedProfileRunData(String filterForTable) {
        switch (lastTabSelection) {
            case TABLE_TAB_SELECTED:
                ProfilingResult result;
                if (filterForTable == null) {
                    result = selectedResult;
                } else {
                    result = filterResult(filterForTable, selectedResult);
                }
                view.setProfilingDetailData(result);
                break;
            case TREEMAP_TAB_SELECTED:
                treeMapView.display(selectedResult);
                break;
            default:
                throw new AssertionError("Unknown selection: " + lastTabSelection);
        }
    }

    private ProfilingResult filterResult(String filter, ProfilingResult result) {
        List<MethodInfo> completeList = result.getMethodInfo();
        List<MethodInfo> filteredList = new ArrayList<>();
        for (MethodInfo method : completeList) {
            if (method.decl.getName().contains(filter)) {
                filteredList.add(method);
            }
        }
        return new ProfilingResult(filteredList);
    }

    @Override
    public UIComponent getView() {
        return view;
    }

    @Override
    public LocalizedString getLocalizedName() {
        return translator.localize(LocaleResources.PROFILER_TAB_NAME);
    }

    static class ByTimeStamp implements Comparator<Profile> {
        @Override
        public int compare(Profile o1, Profile o2) {
            return Long.compare(o1.startTimeStamp, o2.startTimeStamp);
        }
    }

    static class SaveState {
        private final ProfileStatusChange profileStatusChange;
        private final boolean profilingStartOrStopRequested;
        private final ProfilingState profilingState;
        private final ProfilingResult result;

        public SaveState(ProfileStatusChange profileStatusChange,
                         boolean profilingStartOrStopRequested,
                         ProfilingState profilingState,
                         ProfilingResult result)
        {
            this.profileStatusChange = profileStatusChange;
            this.profilingStartOrStopRequested = profilingStartOrStopRequested;
            this.profilingState = profilingState;
            this.result = result;
        }

        public ProfileStatusChange getProfileStatusChange() {
            return profileStatusChange;
        }

        public boolean isProfilingStartOrStopRequested() {
            return profilingStartOrStopRequested;
        }

        public ProfilingState getProfilingState() {
            return profilingState;
        }

        public ProfilingResult getProfilingResult() {
            return result;
        }
    }
}
