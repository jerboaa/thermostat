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

package com.redhat.thermostat.vm.byteman.client.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.cli.AbstractCompleterCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CliCommandOption;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.common.cli.FileNameTabCompleter;
import com.redhat.thermostat.common.cli.TabCompleter;
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
import com.redhat.thermostat.vm.byteman.client.cli.internal.LocaleResources;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanStatus;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequestResponseListener;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest.RequestAction;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;

@Component
@Service
@Property(name = Command.NAME, value = "byteman")
@References({
        @Reference(name = "agentInfoDao", referenceInterface = AgentInfoDAO.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY),
        @Reference(name = "vmInfoDao", referenceInterface = VmInfoDAO.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY),
        @Reference(name = "vmBytemanDao", referenceInterface = VmBytemanDAO.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY),
        @Reference(name = "requestQueue", referenceInterface = RequestQueue.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY),
        @Reference(name = "fileNameTabCompleter", referenceInterface = FileNameTabCompleter.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
})
public class BytemanControlCommand extends AbstractCompleterCommand {

    static final CliCommandOption RULES_OPTION = new CliCommandOption("r", "rules", true,
            "a file with Byteman rules to load into a VM", false);
    
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    static final String INJECT_RULE_ACTION = "load";
    static final String UNLOAD_RULE_ACTION = "unload";
    static final String STATUS_ACTION = "status";
    static final String SHOW_ACTION = "show-metrics";
    private static final String RULES_FILE_OPTION = "rules";
    private static final String NO_RULES_LOADED = "<no-loaded-rules>";
    private static final String UNSET_PORT = "<unset>";
    private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

    
    private final DependencyServices depServices = new DependencyServices();

    @Override
    public Map<CliCommandOption, ? extends TabCompleter> getOptionCompleters() {
        if (!depServices.hasService(FileNameTabCompleter.class)) {
            return Collections.emptyMap();
        }
        return Collections.singletonMap(RULES_OPTION, depServices.getService(FileNameTabCompleter.class));
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        VmArgument vmArgument = VmArgument.required(ctx.getArguments());
        VmId vmId = vmArgument.getVmId();
        
        VmInfoDAO vmInfoDAO = depServices.getRequiredService(VmInfoDAO.class);
        
        final VmInfo vmInfo = vmInfoDAO.getVmInfo(vmId);
        requireNonNull(vmInfo, translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));

        AgentInfoDAO agentInfoDAO = depServices.getRequiredService(AgentInfoDAO.class);
        final AgentId agentId = new AgentId(vmInfo.getAgentId());

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
        VmBytemanDAO bytemanDao = depServices.getRequiredService(VmBytemanDAO.class);

        String command = nonOptionargs.get(0);

        switch (command) {
        case INJECT_RULE_ACTION:
            injectRules(target, vmInfo, ctx, bytemanDao);
            break;
        case UNLOAD_RULE_ACTION:
            unloadRules(target, vmInfo, ctx, bytemanDao);
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
    private void unloadRules(InetSocketAddress target, VmInfo vmInfo, CommandContext ctx, VmBytemanDAO bytemanDao) throws CommandException {
        VmId vmId = new VmId(vmInfo.getVmId());
        RequestQueue requestQueue = depServices.getRequiredService(RequestQueue.class);
        VmBytemanStatus status = getVmBytemanStatus(vmId, bytemanDao);
        int listenPort = status.getListenPort();
        Request unloadRequest = BytemanRequest.create(target, vmInfo, RequestAction.UNLOAD_RULES, listenPort);
        submitRequest(ctx, requestQueue, unloadRequest);
    }

    
    /* Injects byteman rules */
    private void injectRules(InetSocketAddress target, VmInfo vmInfo, CommandContext ctx, VmBytemanDAO bytemanDao) throws CommandException {
        VmId vmId = new VmId(vmInfo.getVmId());
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
        VmBytemanStatus status = bytemanDao.findBytemanStatus(vmId);
        int listenPort = BytemanRequest.NOT_ATTACHED_PORT;
        if (status != null) {
            listenPort = status.getListenPort();
        }
        RequestQueue requestQueue = depServices.getRequiredService(RequestQueue.class);
        Request request = BytemanRequest.create(target, vmInfo, RequestAction.LOAD_RULES, listenPort, rulesContent);
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
        // Byteman status might be null if no agent has been attached yet. Treat
        // this similar to no-rules loaded
        VmBytemanStatus status = bytemanDao.findBytemanStatus(new VmId(vmInfo.getVmId()));
        PrintStream out = ctx.getConsole().getOutput();
        String rules;
        if (status == null || status.getRule() == null || status.getRule().isEmpty()) {
            rules = NO_RULES_LOADED;
        } else {
            rules = status.getRule();
        }
        String listenPort;
        if (status == null) {
            listenPort = UNSET_PORT;
        } else {
            listenPort = Integer.toString(status.getListenPort());
        }
        out.println(translator.localize(LocaleResources.BYTEMAN_STATUS_MSG,
                                        vmInfo.getMainClass(),
                                        listenPort,
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

    private VmBytemanStatus getVmBytemanStatus(VmId vmId, VmBytemanDAO bytemanDao) throws CommandException {
        VmBytemanStatus status = bytemanDao.findBytemanStatus(vmId);
        if (status == null) {
            throw new CommandException(translator.localize(LocaleResources.ERROR_NO_STATUS, vmId.get()));
        }
        return status;
    }
    
    void bindAgentInfoDao(AgentInfoDAO agentDao) {
        depServices.addService(AgentInfoDAO.class, agentDao);
    }
    
    void unbindAgentInfoDao(AgentInfoDAO agentDao) {
        depServices.removeService(AgentInfoDAO.class);
    }
    
    void bindVmInfoDao(VmInfoDAO vmDao) {
        depServices.addService(VmInfoDAO.class, vmDao);
    }
    
    void unbindVmInfoDao(VmInfoDAO vmDao) {
        depServices.removeService(VmInfoDAO.class);
    }
    
    void bindVmBytemanDao(VmBytemanDAO metricDao) {
        depServices.addService(VmBytemanDAO.class, metricDao);
    }
    
    void unbindVmBytemanDao(VmBytemanDAO metricDao) {
        depServices.removeService(VmBytemanDAO.class);
    }
    
    void bindRequestQueue(RequestQueue queue) {
        depServices.addService(RequestQueue.class, queue);
    }
    
    void unbindRequestQueue(RequestQueue queue) {
        depServices.removeService(RequestQueue.class);
    }

    void bindFileNameTabCompleter(FileNameTabCompleter fileNameTabCompleter) {
        depServices.addService(FileNameTabCompleter.class, fileNameTabCompleter);
    }

    void unbindFileNameTabCompleter(FileNameTabCompleter fileNameTabCompleter) {
        depServices.removeService(FileNameTabCompleter.class);
    }

}
