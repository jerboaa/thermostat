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

package com.redhat.thermostat.vm.byteman.client.swing.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.StreamUtils;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.storage.model.VmInfo.AliveStatus;
import com.redhat.thermostat.vm.byteman.client.swing.internal.VmBytemanView.BytemanInjectState;
import com.redhat.thermostat.vm.byteman.client.swing.internal.VmBytemanView.GenerateAction;
import com.redhat.thermostat.vm.byteman.client.swing.internal.VmBytemanView.InjectAction;
import com.redhat.thermostat.vm.byteman.client.swing.internal.VmBytemanView.TabbedPaneAction;
import com.redhat.thermostat.vm.byteman.client.swing.internal.VmBytemanView.TabbedPaneContentAction;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanStatus;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest.RequestAction;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequestResponseListener;

public class VmBytemanInformationController implements InformationServiceController<VmRef> {
    
    private static final String BYTEMAN_RULE_TEMPLATE = "/byteman_rule_template.btm";
    private static final Logger logger = LoggingUtils.getLogger(VmBytemanInformationController.class);
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");
    static final String NO_RULES_LOADED = t.localize(LocaleResources.NO_RULES_LOADED).getContents();
    
    private final VmRef vm;
    private final AgentInfoDAO agentInfoDao;
    private final VmInfoDAO vmInfoDao;
    private final VmBytemanView view;
    private final VmBytemanDAO bytemanDao;
    private final RequestQueue requestQueue;
    
    VmBytemanInformationController(final VmBytemanView view, VmRef vm,
                                   AgentInfoDAO agentInfoDao, VmInfoDAO vmInfoDao,
                                   VmBytemanDAO bytemanDao, RequestQueue requestQueue) {
        this.view = view;
        this.vm = vm;
        this.agentInfoDao = agentInfoDao;
        this.vmInfoDao = vmInfoDao;
        this.bytemanDao = bytemanDao;
        this.requestQueue = requestQueue;
        view.addActionListener(new ActionListener<Action>() {
            
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                Action id = actionEvent.getActionId();
                switch(id) {
                case HIDDEN:
                    // nothing
                    break;
                case VISIBLE:
                    updateRuleAndDetermineState();
                    break;
                default:
                    throw new AssertionError("Invalid action event: " + id);
                }
            }
        });
        view.addRuleChangeListener(new ActionListener<InjectAction>() {
            
            @Override
            public void actionPerformed(ActionEvent<InjectAction> actionEvent) {
                InjectAction id = actionEvent.getActionId();
                switch(id) {
                case INJECT_RULE:
                    loadRule();
                    break;
                case UNLOAD_RULE:
                    unloadRule();
                    break;
                default:
                    throw new AssertionError("Invalid action event: " + id);
                }
            }

        });
        view.addTabbedPaneChangeListener(new ActionListener<TabbedPaneAction>() {

            @Override
            public void actionPerformed(ActionEvent<TabbedPaneAction> actionEvent) {
                TabbedPaneAction id = actionEvent.getActionId();
                switch(id) {
                case METRICS_TAB_SELECTED:
                    updateMetrics();
                    break;
                case RULES_TAB_SELECTED:
                    updateRule(getVmBytemanStatus());
                    break;
                default:
                    throw new AssertionError("Invalid action event: " + id);
                }
            }
        });
        view.addGenerateActionListener(new ActionListener<VmBytemanView.GenerateAction>() {

            @Override
            public void actionPerformed(ActionEvent<GenerateAction> actionEvent) {
                GenerateAction id = actionEvent.getActionId();
                switch(id) {
                case GENERATE_TEMPLATE:
                    generateTemplate();
                    break;
                default:
                    throw new AssertionError("Unknown action event: " + id);
                }
            }
        });
        view.setViewControlsEnabled(isAlive());
    }
    
    // main constructor
    VmBytemanInformationController(VmRef ref, AgentInfoDAO agentInfoDao,
                                   VmInfoDAO vmInfo, VmBytemanDAO vmBytemanDao,
                                   RequestQueue requestQueue) {
        this(new SwingVmBytemanView(), ref, agentInfoDao, vmInfo, vmBytemanDao, requestQueue);
    }

    static String generateTemplateForVM(String mainClass) {
        try(InputStream stream = VmBytemanInformationController.class.getResourceAsStream(BYTEMAN_RULE_TEMPLATE)) {
            byte[] allBytes = StreamUtils.readAll(stream);
            String format = new String(allBytes, UTF_8_CHARSET);
            return String.format(format, mainClass, mainClass, mainClass);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to read byteman rule template file", e);
            return null;
        }
    }
    
    private void generateTemplate() {
        VmInfo info = vmInfoDao.getVmInfo(new VmId(vm.getVmId()));
        String ruleTemplate = generateTemplateForVM(info.getMainClass());
        ActionEvent<TabbedPaneContentAction> event = new ActionEvent<>(this, TabbedPaneContentAction.RULES_CHANGED);
        event.setPayload(ruleTemplate);
        view.contentChanged(event);
    }

    private void updateRuleAndDetermineState() {
        VmBytemanStatus status = getVmBytemanStatus();
        setInitialInstrumentationState(status);
        updateRule(status);
    }

    private VmBytemanStatus getVmBytemanStatus() {
        VmId vmId = new VmId(vm.getVmId());
        return bytemanDao.findBytemanStatus(vmId);
    }

    private void setInitialInstrumentationState(VmBytemanStatus status) {
        if (status != null) {
            String rule = status.getRule();
            if (rule != null && !rule.isEmpty()) {
                view.setInjectState(BytemanInjectState.INJECTED);
            }
        }
        // Everything else means there is no rule injected
    }

    // Package-private for testing
    void updateRule(VmBytemanStatus status) {
        String rule = status.getRule();
        if (rule == null || rule.isEmpty()) {
            rule = NO_RULES_LOADED;
        }
        ActionEvent<TabbedPaneContentAction> event = new ActionEvent<>(this, TabbedPaneContentAction.RULES_CHANGED);
        event.setPayload(rule);
        view.contentChanged(event);
    }

    // Package-private for testing
    void updateMetrics() {
        VmId vmId = new VmId(vm.getVmId());
        AgentId agentId = new AgentId(vm.getHostRef().getAgentId());
        // TODO: Make this query configurable
        long now = System.currentTimeMillis();
        long from = now - TimeUnit.MINUTES.toMillis(5);
        long to = now;
        Range<Long> timeRange = new Range<Long>(from, to);
        List<BytemanMetric> metrics = bytemanDao.findBytemanMetrics(timeRange, vmId, agentId);
        ActionEvent<TabbedPaneContentAction> event = new ActionEvent<>(this, TabbedPaneContentAction.METRICS_CHANGED);
        event.setPayload(metrics);
        view.contentChanged(event);
    }

    // Package-private for testing
    void loadRule() {
        // 1. Validate rule input (basic validation, i.e. rule != EMPTY)
        String rule = view.getRuleContent();
        if (isEmpty(rule)) {
            // unselect the toggle button. This is needed for repeated error
            // handling.
            view.setInjectState(BytemanInjectState.UNLOADED);
            view.handleError(t.localize(LocaleResources.RULE_EMPTY));
            return;
        }
        view.setInjectState(BytemanInjectState.INJECTING);
        view.setViewControlsEnabled(false);
        // 2. Load rule into agent, handle errors.
        BytemanRequestResponseListener listener = doLoadRule(rule);
        view.setViewControlsEnabled(true);
        boolean success = !listener.isError();
        if (success) {
            view.setInjectState(BytemanInjectState.INJECTED);
        } else {
            view.setInjectState(BytemanInjectState.UNLOADED);
            // Error message is already localized.
            view.handleError(new LocalizedString(listener.getErrorMessage()));
        }
    }
    
    // Package-private for testing
    void unloadRule() {
        view.setInjectState(BytemanInjectState.UNLOADING);
        view.setViewControlsEnabled(false);
        BytemanRequestResponseListener listener = doUnloadRule();
        boolean success = !listener.isError();
        view.setViewControlsEnabled(true);
        if (success) {
            // Update the status in gui
            updateRule(getVmBytemanStatus());
            view.setInjectState(BytemanInjectState.UNLOADED);
        } else {
            view.setInjectState(BytemanInjectState.INJECTED);
            // error message is already localized
            view.handleError(new LocalizedString(listener.getErrorMessage()));
        }
    }

    private BytemanRequestResponseListener doLoadRule(String rule) {
        Request request = createRequest(rule, RequestAction.LOAD_RULES);
        return submitRequest(request);
    }

    private Request createRequest(String rule, RequestAction action) {
        VmId vmId = new VmId(vm.getVmId());
        VmBytemanStatus status = bytemanDao.findBytemanStatus(vmId);
        int listenPort = status.getListenPort();
        AgentInformation agentInfo = agentInfoDao.getAgentInformation(new AgentId(vm.getHostRef().getAgentId()));
        InetSocketAddress address = agentInfo.getRequestQueueAddress();
        Request request = null;
        if (action == RequestAction.LOAD_RULES) {
            request = BytemanRequest.create(address, vmId, RequestAction.LOAD_RULES, listenPort, rule);
        } else if (action == RequestAction.UNLOAD_RULES) {
            request = BytemanRequest.create(address, vmId, action, listenPort);
        } else {
            throw new AssertionError("Unknown action: " + action);
        }
        return request;
    }
    
    private BytemanRequestResponseListener submitRequest(Request request) {
        CountDownLatch latch = new CountDownLatch(1);
        BytemanRequestResponseListener listener = new BytemanRequestResponseListener(latch);
        request.addListener(listener);
        requestQueue.putRequest(request);
        waitWithTimeOut(latch);
        return listener;
    }

    void waitWithTimeOut(CountDownLatch latch) {
        try {
            // wait for request to finish
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private boolean isEmpty(String rule) {
        if (rule == null) {
            return true;
        }
        return NO_RULES_LOADED.equals(rule);
    }

    private BytemanRequestResponseListener doUnloadRule() {
        Request request = createRequest(null, RequestAction.UNLOAD_RULES);
        return submitRequest(request);
    }

    private boolean isAlive() {
        AgentId agent = new AgentId(vm.getHostRef().getAgentId());
        AgentInformation agentInfo = agentInfoDao.getAgentInformation(agent);
        if (!agentInfo.isAlive()) {
            return false;
        }
        return vmInfoDao.getVmInfo(vm).isAlive(agentInfo) == AliveStatus.RUNNING;
    }

    @Override
    public UIComponent getView() {
        return view;
    }

    @Override
    public LocalizedString getLocalizedName() {
        return t.localize(LocaleResources.VM_BYTEMAN_TAB_NAME);
    }

}
