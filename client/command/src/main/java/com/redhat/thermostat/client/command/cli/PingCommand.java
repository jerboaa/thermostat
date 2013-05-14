/*
 * Copyright 2012, 2013 Red Hat, Inc.
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
import java.util.List;
import java.util.concurrent.Semaphore;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.command.internal.LocaleResources;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.locale.LocalizedString;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;

public class PingCommand extends AbstractCommand {
    
    private static final String PING_ACTION_NAME = "ping";

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

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
                out.println(translator.localize(LocaleResources.COMMAND_PING_RESPONSE_ERROR, request.getTarget().toString()).getContents());
                break;
            case AUTH_FAILED:
                out.println(translator.localize(LocaleResources.COMMAND_PING_RESPONSE_AUTH_FAILED, request.getTarget().toString()));
                break;
            case OK:
                // fallthrough
            case NOOP:
                out.println(translator.localize(LocaleResources.COMMAND_PING_RESPONSE_OK, request.getTarget().toString()).getContents());
                break;
            case NOK:
                out.println(translator.localize(LocaleResources.COMMAND_PING_RESPONSE_REFUSED).getContents());
                break;
            default:
                out.println(translator.localize(LocaleResources.COMMAND_PING_RESPONSE_UNKNOWN).getContents());
                break;
            }
            responseBarrier.release();
        }
        
    }

    private final BundleContext context;

    public PingCommand() {
        this(FrameworkUtil.getBundle(PingCommand.class).getBundleContext());
    }

    public PingCommand(BundleContext context) {
        this.context = context;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        PrintStream out = ctx.getConsole().getOutput();
        String agentId = getAgentIDArgument(ctx.getArguments());
        if (agentId == null) {
            printCustomMessageWithUsage(out, translator.localize(LocaleResources.COMMAND_PING_ARGUMENT));
            return;
        }

        ServiceReference hostInfoDaoRef = context.getServiceReference(HostInfoDAO.class.getName());
        if (hostInfoDaoRef == null) {
            throw new CommandException(translator.localize(LocaleResources.COMMAND_PING_NO_HOST_INFO_DAO));
        }
        HostInfoDAO hostInfoDao = (HostInfoDAO) context.getService(hostInfoDaoRef);
        HostRef targetHostRef = getHostRef(hostInfoDao, agentId);
        context.ungetService(hostInfoDaoRef);

        if (targetHostRef == null) {
            printCustomMessageWithUsage(out, translator.localize(LocaleResources.COMMAND_PING_INVALID_HOST_ID));
            return;
        }
        ServiceReference agentInfoDaoRef = context.getServiceReference(AgentInfoDAO.class.getName());
        if (agentInfoDaoRef == null) {
            throw new CommandException(translator.localize(LocaleResources.COMMAND_PING_NO_AGENT_INFO_DAO));
        }
        AgentInfoDAO agentInfoDao = (AgentInfoDAO) context.getService(agentInfoDaoRef);
        String address = agentInfoDao.getAgentInformation(targetHostRef).getConfigListenAddress();
        context.ungetService(agentInfoDaoRef);
        
        String [] host = address.split(":");
        InetSocketAddress target = new InetSocketAddress(host[0], Integer.parseInt(host[1]));
        Request ping = new Request(RequestType.RESPONSE_EXPECTED, target);
        ping.setParameter(Request.ACTION, PING_ACTION_NAME);
        ping.setReceiver("com.redhat.thermostat.agent.command.internal.PingReceiver");
        final Semaphore responseBarrier = new Semaphore(0);
        ping.addListener(new PongListener(out, responseBarrier));

        ServiceReference queueRef = context.getServiceReference(RequestQueue.class.getName());
        if (queueRef == null) {
            throw new CommandException(translator.localize(LocaleResources.COMMAND_PING_NO_REQUEST_QUEUE));
        }
        RequestQueue queue = (RequestQueue) context.getService(queueRef);
        out.println(translator.localize(LocaleResources.COMMAND_PING_QUEUING_REQUEST, target.toString()).getContents());
        queue.putRequest(ping);
        context.ungetService(queueRef);
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

    private void printCustomMessageWithUsage(PrintStream out, LocalizedString message) {
        out.println(message.getContents());
        // FIXME add usage back out.println(getUsage());
        return;
    }

}

