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

package com.redhat.thermostat.vm.byteman.client.cli.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.common.utils.StreamUtils;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanStatus;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequestResponseListener;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest.RequestAction;

public class BytemanControlCommand extends AbstractCommand {
    
    public static final String COMMAND_NAME = "byteman";
    
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final String INJECT_RULE_ACTION = "load";
    private static final String UNLOAD_RULE_ACTION = "unload";
    private static final String STATUS_ACTION = "status";
    private static final String SHOW_ACTION = "show-metrics";
    private static final String RULES_FILE_OPTION = "rules";
    private static final String NO_RULES_LOADED = "<no-loaded-rules>";
    private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");
    
    private final DependencyServices depServices = new DependencyServices();

    @Override
    public void run(CommandContext ctx) throws CommandException {
        VmArgument vmArgument = VmArgument.required(ctx.getArguments());
        VmId vmId = vmArgument.getVmId();
        
        VmInfoDAO vmInfoDAO = depServices.getService(VmInfoDAO.class);
        
        requireNonNull(vmInfoDAO, translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));
        final VmInfo vmInfo = vmInfoDAO.getVmInfo(vmId);
        requireNonNull(vmInfo, translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));

        AgentInfoDAO agentInfoDAO = depServices.getService(AgentInfoDAO.class);
        final AgentId agentId = new AgentId(vmInfo.getAgentId());
        requireNonNull(agentInfoDAO, translator.localize(LocaleResources.AGENT_SERVICE_UNAVAILABLE));

        AgentInformation agentInfo = agentInfoDAO.getAgentInformation(agentId);
        if (agentInfo == null) {
            throw new CommandException(translator.localize(LocaleResources.AGENT_NOT_FOUND, agentId.get()));
        }
        if (!agentInfo.isAlive()) {
            throw new CommandException(translator.localize(LocaleResources.AGENT_DEAD, agentId.get()));
        }

        InetSocketAddress target = agentInfo.getRequestQueueAddress();

        List<String> nonOptionargs = ctx.getArguments().getNonOptionArguments();
        if (nonOptionargs.size() != 1) {
            throw new CommandException(translator.localize(LocaleResources.COMMAND_EXPECTED));
        }
        VmBytemanDAO bytemanDao = depServices.getService(VmBytemanDAO.class);
        requireNonNull(bytemanDao, translator.localize(LocaleResources.BYTEMAN_METRICS_SERVICE_UNAVAILABLE));

        String command = nonOptionargs.get(0);

        switch (command) {
        case INJECT_RULE_ACTION:
            injectRules(target, vmId, ctx, bytemanDao);
            break;
        case UNLOAD_RULE_ACTION:
            unloadRules(target, vmId, ctx, bytemanDao);
            break;
        case STATUS_ACTION:
            showStatus(ctx, vmInfo, bytemanDao);
            break;
        case SHOW_ACTION:
            showMetrics(ctx, vmId, agentId, bytemanDao);
            break;
        default:
            throw new CommandException(translator.localize(LocaleResources.UNKNOWN_COMMAND, command));
        }
    }
    
    /* Unloads byteman rules */
    private void unloadRules(InetSocketAddress target, VmId vmId, CommandContext ctx, VmBytemanDAO bytemanDao) throws CommandException {
        RequestQueue requestQueue = getRequestQueue();
        VmBytemanStatus status = getVmBytemanStatus(vmId, bytemanDao);
        int listenPort = status.getListenPort();
        Request unloadRequest = BytemanRequest.create(target, vmId, RequestAction.UNLOAD_RULES, listenPort);
        submitRequest(ctx, requestQueue, unloadRequest);
    }

    
    /* Injects byteman rules */
    private void injectRules(InetSocketAddress target, VmId vmId, CommandContext ctx, VmBytemanDAO bytemanDao) throws CommandException {
        Arguments args = ctx.getArguments();
        if (!args.hasArgument(RULES_FILE_OPTION)) {
            throw new CommandException(translator.localize(LocaleResources.NO_RULE_OPTION));
        }
        String ruleFile = args.getArgument(RULES_FILE_OPTION);
        
        byte[] rulesBytes;
        try {
            rulesBytes = StreamUtils.readAll(new FileInputStream(new File(ruleFile)));
        } catch (FileNotFoundException e) {
            throw new CommandException(translator.localize(LocaleResources.RULE_FILE_NOT_FOUND, ruleFile));
        } catch (IOException e) {
            throw new CommandException(translator.localize(LocaleResources.ERROR_READING_RULE_FILE, ruleFile));
        }
        String rulesContent = new String(rulesBytes, UTF_8_CHARSET);
        VmBytemanStatus status = getVmBytemanStatus(vmId, bytemanDao);
        int listenPort = status.getListenPort();
        RequestQueue requestQueue = getRequestQueue();
        Request request = BytemanRequest.create(target, vmId, RequestAction.LOAD_RULES, listenPort, rulesContent);
        submitRequest(ctx, requestQueue, request);
    }

    /* Show metrics retrieved via byteman rules */
    private void showMetrics(CommandContext ctx, VmId vmId, AgentId agentId, VmBytemanDAO bytemanDao) throws CommandException {
        // TODO: Make this query configurable with arguments
        long now = System.currentTimeMillis();
        long from = now - TimeUnit.MINUTES.toMillis(5);
        long to = now;
        Range<Long> timeRange = new Range<Long>(from, to);
        List<BytemanMetric> metrics = bytemanDao.findBytemanMetrics(timeRange, vmId, agentId);
        PrintStream output = ctx.getConsole().getOutput();
        PrintStream out = ctx.getConsole().getOutput();
        if (metrics.isEmpty()) {
            out.println(translator.localize(LocaleResources.NO_METRICS_AVAILABLE, vmId.get()).getContents());
        } else {
            for (BytemanMetric m: metrics) {
                output.println(m.getDataAsJson());
            }
        }
    }

    /* Show status of loaded byteman rules */
    private void showStatus(CommandContext ctx, VmInfo vmInfo, VmBytemanDAO bytemanDao) throws CommandException {
        VmBytemanStatus status = getVmBytemanStatus(new VmId(vmInfo.getVmId()), bytemanDao);
        PrintStream out = ctx.getConsole().getOutput();
        String rules = status.getRule();
        if (rules == null || rules.isEmpty()) {
            rules = NO_RULES_LOADED;
        }
        out.println(translator.localize(LocaleResources.BYTEMAN_STATUS_MSG,
                                        vmInfo.getMainClass(),
                                        Integer.toString(status.getListenPort()),
                                        rules).getContents());
    }

    private void submitRequest(CommandContext ctx, RequestQueue requestQueue, Request request) {
        CountDownLatch latch = new CountDownLatch(1);
        BytemanRequestResponseListener listener = new BytemanRequestResponseListener(latch);
        request.addListener(listener);
        requestQueue.putRequest(request);
        waitWithTimeout(latch);
        printResponse(listener, ctx);
    }
    
    private void printResponse(BytemanRequestResponseListener listener, CommandContext ctx) {
        if (listener.isError()) {
            PrintStream err = ctx.getConsole().getError();
            err.println(listener.getErrorMessage());
        } else {
            PrintStream out = ctx.getConsole().getOutput();
            out.println(translator.localize(LocaleResources.REQUEST_SUCCESS)
                    .getContents());
        }
    }
    
    // package-private for testing
    void waitWithTimeout(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
    }
    
    private RequestQueue getRequestQueue() throws CommandException {
        RequestQueue requestQueue = depServices.getService(RequestQueue.class);
        requireNonNull(requestQueue, translator.localize(LocaleResources.QUEUE_SERVICE_UNAVAILABLE));
        return requestQueue;
    }
    
    private VmBytemanStatus getVmBytemanStatus(VmId vmId, VmBytemanDAO bytemanDao) throws CommandException {
        VmBytemanStatus status = bytemanDao.findBytemanStatus(vmId);
        if (status == null) {
            throw new CommandException(translator.localize(LocaleResources.ERROR_NO_STATUS, vmId.get()));
        }
        return status;
    }
    
    void setAgentInfoDao(AgentInfoDAO agentDao) {
        depServices.addService(AgentInfoDAO.class, agentDao);
    }
    
    void unsetAgentInfoDao() {
        depServices.removeService(AgentInfoDAO.class);
    }
    
    void setVmInfoDao(VmInfoDAO vmDao) {
        depServices.addService(VmInfoDAO.class, vmDao);
    }
    
    void unsetVmInfoDao() {
        depServices.removeService(VmInfoDAO.class);
    }
    
    void setVmBytemanDao(VmBytemanDAO metricDao) {
        depServices.addService(VmBytemanDAO.class, metricDao);
    }
    
    void unsetVmBytemanDao() {
        depServices.removeService(VmBytemanDAO.class);
    }
    
    void setRequestQueue(RequestQueue queue) {
        depServices.addService(RequestQueue.class, queue);
    }
    
    void unsetRequestQueue() {
        depServices.removeService(RequestQueue.class);
    }

}
