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


package com.redhat.thermostat.web.cmd;

import java.util.List;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.utils.HostPortPair;
import com.redhat.thermostat.common.utils.HostPortsParser;

public class WebServiceCommand extends AbstractCommand {

    private WebServiceLauncher serviceLauncher;
    
    public WebServiceCommand() {
        this.serviceLauncher = new WebServiceLauncher();
    }
    
    // Constructor for testing
    WebServiceCommand(WebServiceLauncher launcher) {
        this.serviceLauncher = launcher;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        String storageURL = ctx.getArguments().getArgument("dbUrl");
        String username = ctx.getArguments().getArgument("username");
        String password = ctx.getArguments().getArgument("password");
        serviceLauncher.setIpAddresses(parseIPsPorts(ctx.getArguments().getArgument("bindAddrs")));
        serviceLauncher.setStorageURL(storageURL);
        serviceLauncher.setStorageUsername(username);
        serviceLauncher.setStoragePassword(password);
        try {
            // this blocks
            serviceLauncher.start();
        } catch (InterruptedException e) {
            // just shut down cleanly
            try {
                serviceLauncher.stop();
            } catch (Exception ex) {
                ex.printStackTrace(ctx.getConsole().getError());
                throw new CommandException(ex);
            }
        } catch (Exception ex) {
            ex.printStackTrace(ctx.getConsole().getError());
            throw new CommandException(ex);
        }
    }

    @Override
    public boolean isStorageRequired() {
        return false;
    }

    @Override
    public boolean isAvailableInShell() {
    	return false;
    }

    private List<HostPortPair> parseIPsPorts(String rawIpsPorts) throws CommandException {
        HostPortsParser parser = new HostPortsParser(rawIpsPorts);
        try {
           parser.parse(); 
        } catch (IllegalArgumentException e) {
            throw new CommandException(e);
        }
        return parser.getHostsPorts();
    }

}

