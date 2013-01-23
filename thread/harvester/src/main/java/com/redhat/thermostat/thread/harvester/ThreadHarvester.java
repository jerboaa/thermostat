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

package com.redhat.thermostat.thread.harvester;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;

import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;

import com.redhat.thermostat.thread.collector.HarvesterCommand;
import com.redhat.thermostat.thread.dao.ThreadDao;

public class ThreadHarvester implements RequestReceiver {

    private ScheduledExecutorService executor;
    Map<String, Harvester> connectors;

    private ThreadDao dao;
    
    public ThreadHarvester(ScheduledExecutorService executor) {
        this.executor = executor;
        connectors = new HashMap<>();
    }

    public void setThreadDao(ThreadDao dao) {
        // a new ThreadDao has appeared, stop everything using the old implementation
        removeAllHarvesters();
        this.dao = dao;
        // TODO maybe bring back the old stuff using the new ThreadDao?
    }
    
    @Override
    public Response receive(Request request) {
        if (!allRequirementsAvailable()) {
            return new Response(ResponseType.ERROR);
        }
        
        boolean result = false;
        
        String command = request.getParameter(HarvesterCommand.class.getName());
        switch (HarvesterCommand.valueOf(command)) {
        case START: {
            String vmId = request.getParameter(HarvesterCommand.VM_ID.name());
            result = startHarvester(vmId);
            break;
        }   
        case STOP: {
            String vmId = request.getParameter(HarvesterCommand.VM_ID.name());
            result = stopHarvester(vmId);
            break;
        }
        case VM_CAPS: {
            // this is blocking
            String vmId = request.getParameter(HarvesterCommand.VM_ID.name());
            result = saveVmCaps(vmId);
            break;
        }
        case IS_COLLECTING: {
            // this is blocking too
            String vmId = request.getParameter(HarvesterCommand.VM_ID.name());
            // FIXME: this need to be replaced when we support response parameters
            return isCollecting(vmId) ? new Response(ResponseType.OK) : new Response(ResponseType.NOK);
        }        
        default:
            result = false;
            break;
        }
        
        if (result) {
            return new Response(ResponseType.OK);            
        } else {
            return new Response(ResponseType.ERROR);
        }
    }
    
    private boolean isCollecting(String vmId) {
        Harvester harvester = connectors.get(vmId);
        if (harvester == null) {
            return false;
        }
        return harvester.isConnected();
    }
    
    private boolean startHarvester(String vmId) {
        Harvester harvester = getHarvester(vmId);
        return harvester.start();
    }
    
    private boolean saveVmCaps(String vmId) {
        Harvester harvester = getHarvester(vmId);
        return harvester.saveVmCaps();
    }
    
    private boolean stopHarvester(String vmId) {
        Harvester harvester = connectors.get(vmId);
        if (harvester != null) {
            return harvester.stop();
        }
        return true;
    }
    
    Harvester getHarvester(String vmId) {
        Harvester harvester = connectors.get(vmId);
        if (harvester == null) {
            harvester = new Harvester(dao, executor, vmId);
            connectors.put(vmId, harvester);
        }
        
        return harvester;
    }

    private void removeAllHarvesters() {
        Iterator<Entry<String, Harvester>> iter = connectors.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, Harvester> entry = iter.next();
            entry.getValue().stop();
            iter.remove();
        }
    }

    private boolean allRequirementsAvailable() {
        return dao != null;
    }

}

