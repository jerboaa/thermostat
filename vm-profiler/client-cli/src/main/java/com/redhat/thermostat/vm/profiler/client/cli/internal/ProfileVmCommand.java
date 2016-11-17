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

package com.redhat.thermostat.vm.profiler.client.cli.internal;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult.MethodInfo;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResultParser;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileRequest;
import com.redhat.thermostat.vm.profiler.common.ProfileStatusChange;

public class ProfileVmCommand extends AbstractCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    static final String START_ARGUMENT = "start";
    static final String STOP_ARGUMENT = "stop";
    static final String STATUS_ARGUMENT = "status";
    static final String SHOW_ARGUMENT = "show";

    private final DependencyServices myServices = new DependencyServices();

    @Override
    public void run(CommandContext ctx) throws CommandException {

        AgentInfoDAO agentInfoDAO = myServices.getRequiredService(AgentInfoDAO.class);
        VmInfoDAO vmInfoDAO = myServices.getRequiredService(VmInfoDAO.class);

        VmArgument vmArgument = VmArgument.required(ctx.getArguments());
        VmId vmId = vmArgument.getVmId();
        final VmInfo vmInfo = vmInfoDAO.getVmInfo(vmId);
        final AgentId agentId = new AgentId(vmInfo.getAgentId());

        RequestQueue requestQueue = myServices.getRequiredService(RequestQueue.class);

        AgentInformation agentInfo = agentInfoDAO.getAgentInformation(agentId);
        if (agentInfo == null) {
            throw new CommandException(translator.localize(LocaleResources.AGENT_NOT_FOUND, agentId.get()));
        }

        InetSocketAddress target = agentInfo.getRequestQueueAddress();

        String command = ctx.getArguments().getSubcommand();
        switch (command) {
        case START_ARGUMENT:
            sendStartProfilingRequest(ctx.getConsole(), requestQueue, target, vmId.get());
            break;
        case STOP_ARGUMENT:
            sendStopProfilingRequest(ctx.getConsole(), requestQueue, target, vmId.get());
            break;
        case STATUS_ARGUMENT:
            showProfilingStatus(ctx.getConsole(), agentId, vmId);
            break;
        case SHOW_ARGUMENT:
            showProfilingResults(ctx.getConsole(), agentId, vmId);
            break;
        }
    }

    public void sendStartProfilingRequest(Console console, RequestQueue queue, InetSocketAddress target, String vmId) throws CommandException {
        Response response = sendProfilingRequestAndGetResponse(
                console, queue, target, ProfileRequest.START_PROFILING, vmId);

        if (response.getType() == ResponseType.OK) {
            console.getOutput().println(translator.localize(LocaleResources.STARTED_PROFILING, vmId).getContents());
        } else if (response.getType() == ResponseType.NOOP) {
            console.getOutput().println(translator.localize(LocaleResources.ALREADY_PROFILING, vmId).getContents());
        } else if (response.getType() == ResponseType.NOK) {
            console.getError().println(translator.localize(LocaleResources.UNABLE_TO_START_PROFILING, vmId).getContents());
        } else {
            console.getError().println(translator.localize(LocaleResources.UNABLE_TO_USE_PROFILING).getContents());
        }
    }

    public void sendStopProfilingRequest(Console console, RequestQueue queue, InetSocketAddress target, String vmId) throws CommandException {
        Response response = sendProfilingRequestAndGetResponse(
                console, queue, target, ProfileRequest.STOP_PROFILING, vmId);

        if (response.getType() == ResponseType.OK) {
            console.getOutput().println(translator.localize(LocaleResources.STOPPED_PROFILING, vmId).getContents());
        } else if (response.getType() == ResponseType.NOOP) {
            console.getOutput().println(translator.localize(LocaleResources.NOT_PROFILING, vmId).getContents());
        } else if (response.getType() == ResponseType.NOK) {
            console.getError().println(translator.localize(LocaleResources.UNABLE_TO_STOP_PROFILING, vmId).getContents());
        } else {
            console.getError().println(translator.localize(LocaleResources.UNABLE_TO_USE_PROFILING).getContents());
        }
    }

    public Response sendProfilingRequestAndGetResponse(Console console, RequestQueue queue, InetSocketAddress target, String action, String vmId) throws CommandException {
        final Response[] responses = new Response[1];
        final Semaphore responseReceived = new Semaphore(0);
        Request request = ProfileRequest.create(target, vmId, action);

        request.addListener(new RequestResponseListener() {
            @Override
            public void fireComplete(Request request, Response response) {
                responses[0] = response;
                responseReceived.release(1);
            }
        });

        queue.putRequest(request);

        try {
            responseReceived.acquire();
            Response response = responses[0];
            return response;
        } catch (InterruptedException e) {
            throw new CommandException(translator.localize(LocaleResources.INTERRUPTED_WAITING_FOR_RESPONSE));
        }

    }

    private void showProfilingStatus(Console console, AgentId agentId, VmId vmId) {
        ProfileDAO dao = myServices.getService(ProfileDAO.class);
        ProfileStatusChange latest = dao.getLatestStatus(agentId, vmId);
        boolean profiling = false;
        if (latest != null) {
            profiling = latest.isStarted();
        }
        String message;
        if (profiling) {
            message = translator.localize(LocaleResources.STATUS_CURRENTLY_PROFILING).getContents();
        } else {
            message = translator.localize(LocaleResources.STATUS_CURRENTLY_NOT_PROFILING).getContents();
        }
        console.getOutput().println(message);
    }

    private void showProfilingResults(Console console, AgentId agentId, VmId vmId) {
        ProfileDAO dao = myServices.getService(ProfileDAO.class);
        InputStream data = dao.loadLatestProfileData(agentId, vmId);
        if (data == null) {
            console.getError().println(translator.localize(LocaleResources.PROFILING_DATA_NOT_AVAILABLE).getContents());
            return;
        }
        parseAndDisplayProfilingData(console, data);
    }

    private void parseAndDisplayProfilingData(Console console, InputStream data) {
        ProfilingResultParser parser = new ProfilingResultParser();
        ProfilingResult results = parser.parse(data);

        List<MethodInfo> methodInfos = new ArrayList<>(results.getMethodInfo());

        Collections.sort(methodInfos, new Comparator<MethodInfo>() {
            @Override
            public int compare(MethodInfo o1, MethodInfo o2) {
                return Double.compare(o2.percentageTime, o1.percentageTime);
            }
        });

        ProfileResultFormatter formatter = new ProfileResultFormatter();

        formatter.addHeader();

        for (MethodInfo method : methodInfos) {
            formatter.addMethodInfo(method);
        }

        formatter.format(console.getOutput());
    }

    void setAgentInfoDAO(AgentInfoDAO dao) {
        myServices.addService(AgentInfoDAO.class, dao);
    }

    void unsetAgentInfoDAO() {
        myServices.removeService(AgentInfoDAO.class);
    }

    void setVmInfoDAO(VmInfoDAO dao) {
        myServices.addService(VmInfoDAO.class, dao);
    }

    void unsetVmInfoDAO() {
        myServices.removeService(VmInfoDAO.class);
    }

    void setRequestQueue(RequestQueue queue) {
        myServices.addService(RequestQueue.class, queue);
    }

    void unsetRequestQueue() {
        myServices.removeService(RequestQueue.class);
    }

    void setProfileDAO(ProfileDAO dao) {
        myServices.addService(ProfileDAO.class, dao);
    }

    void unsetProfileDAO() {
        myServices.removeService(ProfileDAO.class);
    }
}
