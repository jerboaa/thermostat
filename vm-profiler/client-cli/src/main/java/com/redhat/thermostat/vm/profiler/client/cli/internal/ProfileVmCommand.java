/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.redhat.thermostat.client.cli.HostVMArguments;
import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.utils.StreamUtils;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileRequest;

public class ProfileVmCommand extends AbstractCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String START_ARGUMENT = "start";
    private static final String STOP_ARGUMENT = "stop";
    private static final String SHOW_ARGUMENT = "show";

    @Override
    public void run(CommandContext ctx) throws CommandException {

        HostVMArguments args = new HostVMArguments(ctx.getArguments(), true, true);

        AgentInfoDAO agentInfoDAO = getService(AgentInfoDAO.class);
        VmInfoDAO vmInfoDAO = getService(VmInfoDAO.class);

        requireNonNull(agentInfoDAO, translator.localize(LocaleResources.AGENT_SERVICE_UNAVAILABLE));
        requireNonNull(vmInfoDAO, translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));

        RequestQueue requestQueue = getService(RequestQueue.class);
        requireNonNull(requestQueue, translator.localize(LocaleResources.QUEUE_SERVICE_UNAVAILABLE));

        AgentInformation agentInfo = agentInfoDAO.getAgentInformation(args.getHost());
        if (agentInfo == null) {
            ctx.getConsole().getError().println("error: agent '" + args.getHost().getAgentId() + "' not found'");
            return;
        }

        InetSocketAddress target = agentInfo.getRequestQueueAddress();

        List<String> arguments = ctx.getArguments().getNonOptionArguments();
        if (arguments.size() != 1) {
            throw new CommandException(translator.localize(LocaleResources.COMMAND_EXPECTED));
        }

        String command = arguments.get(0);

        switch (command) {
        case START_ARGUMENT:
            sendStartProfilingRequest(ctx.getConsole(), requestQueue, target, args.getVM().getVmId());
            break;
        case STOP_ARGUMENT:
            sendStopProfilingRequest(ctx.getConsole(), requestQueue, target, args.getVM().getVmId());
            break;
        case SHOW_ARGUMENT:
            showProfilingResults(ctx.getConsole(), args.getVM());
            break;
        default:
            throw new CommandException(translator.localize(LocaleResources.UNKNOWN_COMMAND, command));
        }
    }

    public void sendStartProfilingRequest(Console console, RequestQueue queue, InetSocketAddress target, String vmId) throws CommandException {
        Response response = sendProfilingRequestAndGetResponse(
                console, queue, target, ProfileRequest.START_PROFILING, vmId);

        if (response.getType() == ResponseType.OK) {
            console.getOutput().println("Started profiling " + vmId);
        } else if (response.getType() == ResponseType.NOOP) {
            console.getOutput().println("Profiling already active for " + vmId);
        } else if (response.getType() == ResponseType.NOK) {
            console.getError().println("Unable to start profiling " + vmId);
        } else {
            console.getError().println("Unable to use profiling");
        }
    }

    public void sendStopProfilingRequest(Console console, RequestQueue queue, InetSocketAddress target, String vmId) throws CommandException {
        Response response = sendProfilingRequestAndGetResponse(
                console, queue, target, ProfileRequest.STOP_PROFILING, vmId);

        if (response.getType() == ResponseType.OK) {
            console.getOutput().println("Stopped profiling " + vmId);
        } else if (response.getType() == ResponseType.NOOP) {
            console.getOutput().println("Profiling was *not* active for " + vmId + ". No action taken");
        } else if (response.getType() == ResponseType.NOK) {
            console.getError().println("Unable to stop profiling " + vmId);
        } else {
            console.getError().println("Unable to use profiling");
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

    private void showProfilingResults(Console console, VmRef vm) {
        ProfileDAO dao = getService(ProfileDAO.class);
        InputStream data = dao.loadLatestProfileData(vm);
        displayProfilingData(console, data);
    }

    private void displayProfilingData(Console console, InputStream data) {
        try {
            StreamUtils.copyStream(new BufferedInputStream(data), new BufferedOutputStream(console.getOutput()));
        } catch (IOException e) {
            console.getError().println("Error displaying data");
            e.printStackTrace();
        }
    }

    void setAgentInfoDAO(AgentInfoDAO dao) {
        addService(AgentInfoDAO.class, dao);
    }

    void unsetAgentInfoDAO() {
        removeService(AgentInfoDAO.class);
    }

    void setVmInfoDAO(VmInfoDAO dao) {
        addService(VmInfoDAO.class, dao);
    }

    void unsetVmInfoDAO() {
        removeService(VmInfoDAO.class);
    }

    void setRequestQueue(RequestQueue queue) {
        addService(RequestQueue.class, queue);
    }

    void unsetRequestQueue() {
        removeService(RequestQueue.class);
    }

    void setProfileDAO(ProfileDAO dao) {
        addService(ProfileDAO.class, dao);
    }

    void unsetProfileDAO() {
        removeService(ProfileDAO.class);
    }
}
