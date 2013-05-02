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

package com.redhat.thermostat.gc.remote.command;

import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.gc.remote.command.internal.GC;
import com.redhat.thermostat.gc.remote.command.internal.GCException;
import com.redhat.thermostat.gc.remote.common.command.GCCommand;
import com.redhat.thermostat.utils.management.MXBeanConnection;
import com.redhat.thermostat.utils.management.MXBeanConnectionPool;

public class GCCommandReceiver implements RequestReceiver {

    private MXBeanConnectionPool pool;

    public GCCommandReceiver(MXBeanConnectionPool pool) {
        this.pool = pool;
    }

    @Override
    public Response receive(Request request) {
        Response response = new Response(ResponseType.OK);
        
        String command = request.getParameter(GCCommand.class.getName());
        switch (GCCommand.valueOf(command)) {
        case REQUEST_GC:
            String vmId = request.getParameter(GCCommand.VM_ID);
            try {
                new GC(pool, vmId).gc();
            } catch (GCException gce) {
                response = new Response(ResponseType.ERROR);
            }
            break;
        default:
            break;
        }
        return response;
    }
}

