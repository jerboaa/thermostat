/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.client.command.cli;

import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleCommand;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.utils.OSGIUtils;

public class PingCommand extends SimpleCommand {

    private static final String NAME = "ping";
    private static final String USAGE = "ping <agentId>";
    private static final String DESC = "Using the Command Channel, send a ping to a running agent.";

    private class PongListener implements RequestResponseListener {

        private PrintStream out;
        private final Semaphore responseBarrier;

        public PongListener(PrintStream out, Semaphore responseBarrier) {
            this.out = out;
            this.responseBarrier = responseBarrier;
        }

        @Override
        public void fireComplete(Request request, Response response) {
            switch (response.getType()) {
            case ERROR:
                out.println("Error received from:" + request.getTarget().toString());
                break;
            case PONG:
            case OK:
            case NOOP:
                out.println("Response received from: " + request.getTarget().toString());
                break;
            case EXCEPTION:
                out.println("The tubes, they are probably broken.");
                break;
            case NOK:
                out.println("The server refused to PONG our PING?");
                break;
            default:
                out.println("Unknown result from ping command.");
                break;
            }
            responseBarrier.release();
        }
        
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        PrintStream out = ctx.getConsole().getOutput();
        String agentId = getAgentIDArgument(ctx.getArguments());
        if (agentId == null) {
            printCustomMessageWithUsage(out, "Ping command accepts one and only one argument.");
            return;
        }
        DAOFactory df = ApplicationContext.getInstance().getDAOFactory();
        HostInfoDAO dao = df.getHostInfoDAO();
        HostRef targetHostRef = getHostRef(dao, agentId);
        if (targetHostRef == null) {
            printCustomMessageWithUsage(out, "Invalid host ID or agent no longer running.  See \'help list-vms to obtain a valid host ID.");
            return;
        }
        int port = df.getStorage().getConfigListenPort(targetHostRef);
        InetSocketAddress target = new InetSocketAddress(targetHostRef.getHostName(), port);
        Request ping = new Request(RequestType.PING, target);
        final Semaphore responseBarrier = new Semaphore(0);
        ping.addListener(new PongListener(out, responseBarrier));

        RequestQueue queue = OSGIUtils.getInstance().getService(RequestQueue.class);
        out.println("Queuing ping request.  Destination: " + target.toString());
        queue.putRequest(ping);
        try {
            responseBarrier.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String getAgentIDArgument(Arguments arguments) {
        List<String> args = arguments.getNonOptionArguments();
        if (args.size() != 1) {
            return null;
        }
        return args.get(0);
    }

    private HostRef getHostRef(HostInfoDAO dao, String agentId) {
        HostRef targetHostRef = null;
        for (HostRef hostref : dao.getAliveHosts()) {
            if (agentId.equals(hostref.getAgentId())) {
                targetHostRef = hostref;
                break;
            }
        }
        return targetHostRef;
    }

    private void printCustomMessageWithUsage(PrintStream out, String message) {
        out.println(message);
        out.println(getUsage());
        return;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESC;
    }

    @Override
    public String getUsage() {
        return USAGE;
    }

    @Override
    public Collection<ArgumentSpec> getAcceptedArguments() {
        List<ArgumentSpec> args = new ArrayList<>();
        return args;
    }

}
