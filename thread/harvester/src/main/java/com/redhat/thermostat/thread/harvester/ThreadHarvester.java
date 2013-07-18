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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.thread.collector.HarvesterCommand;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.utils.management.MXBeanConnectionPool;

public class ThreadHarvester implements RequestReceiver {

    private static final Logger logger = LoggingUtils.getLogger(ThreadHarvester.class);
    
    private ScheduledExecutorService executor;
    Map<String, Harvester> connectors;

    private ThreadDao dao;
    private Clock clock;
    private MXBeanConnectionPool connectionPool;

    public ThreadHarvester(ScheduledExecutorService executor, MXBeanConnectionPool pool) {
        this(executor, new SystemClock(), pool);
    }
    
    public ThreadHarvester(ScheduledExecutorService executor, Clock clock, MXBeanConnectionPool connectionPool) {
        this.executor = executor;
        this.connectors = new HashMap<>();
        this.clock = clock;
        this.connectionPool = connectionPool;
    }
    
    /**
     * Set the new implementation of thread DAO to be used as storage
     * @param dao
     */
    public void setThreadDao(ThreadDao dao) {
        // stop everything using the old implementation
        List<Pair<String, Integer>> allSaved = new ArrayList<>();
        if (this.dao != null) {
            allSaved.addAll(stopAndRemoveAllHarvesters());
        }

        this.dao = dao;
        // re-enable all existing harvesters
        for (Pair<String, Integer> saved : allSaved) {
            startHarvester(saved.getFirst(), saved.getSecond());
        }
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
            String strPid = request.getParameter(HarvesterCommand.VM_PID.name());
            try {
                Integer pid = Integer.parseInt(strPid);
                result = startHarvester(vmId, pid);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Invalid pid: " + strPid, e);
            }
            break;
        }   
        case STOP: {
            String vmId = request.getParameter(HarvesterCommand.VM_ID.name());
            result = stopHarvester(vmId);
            break;
        }
        case FIND_DEADLOCKS:
            String vmId = request.getParameter(HarvesterCommand.VM_ID.name());
            String strPid = request.getParameter(HarvesterCommand.VM_PID.name());
            try {
                Integer pid = Integer.parseInt(strPid);
                result = findAndSaveDeadLockInformation(vmId, pid);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Invalid pid: " + strPid, e);
            }
            break;
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

    /**
     * Attaches and starts a harvester to the given PID.
     * <p>
     * Saves current harvesting status to storage.
     */
    public boolean startHarvester(String vmId, int pid) {
        Harvester harvester = getHarvester(vmId, pid);
        boolean result = harvester.start();
        if (result) {
            updateHarvestingStatus(vmId, result);
        }
        return result;
    }
    
    boolean saveVmCaps(String vmId, int pid) {
        Harvester harvester = getHarvester(vmId, pid);
        return harvester.saveVmCaps();
    }

    /**
     * Stops and detaches a harvester from the given PID.
     * <p>
     * Saves current harvesting status to storage.
     */
    public boolean stopHarvester(String vmId) {
        Harvester harvester = connectors.get(vmId);
        boolean result = true;
        if (harvester != null) {
            result = harvester.stop();
        }
        updateHarvestingStatus(vmId, false);
        return result;
    }

    /** Save current status to storage */
    public void addThreadHarvestingStatus(String vmId) {
        boolean harvesting = false;
        Harvester harvester = connectors.get(vmId);
        if (harvester != null) {
            harvesting = harvester.isConnected();
        }
        updateHarvestingStatus(vmId, harvesting);
    }

    private void updateHarvestingStatus(String vmId, boolean harvesting) {
        ThreadHarvestingStatus status = new ThreadHarvestingStatus();
        status.setTimeStamp(clock.getRealTimeMillis());
        status.setVmId(vmId);
        status.setHarvesting(harvesting);
        dao.saveHarvestingStatus(status);
    }

    private Harvester getHarvester(String vmId, int pid) {
        Harvester harvester = connectors.get(vmId);
        if (harvester == null) {
            harvester = createHarvester(vmId, pid);
            connectors.put(vmId, harvester);
        }
        
        return harvester;
    }

    Harvester createHarvester(String vmId, int pid) {
        return new Harvester(dao, executor, vmId, pid, connectionPool);
    }

    /**
     * Returns a list of VM ID, PID pairs which can be used to resume
     * the harvesters at a later time.
     */
    public List<Pair<String, Integer>> stopAndRemoveAllHarvesters() {
        List<Pair<String, Integer>> result = new ArrayList<>();
        Iterator<Entry<String, Harvester>> iter = connectors.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, Harvester> entry = iter.next();
            String vmId = entry.getKey();
            Harvester harvester = entry.getValue();
            harvester.stop();
            updateHarvestingStatus(vmId, false);
            iter.remove();
            result.add(new Pair<>(vmId, harvester.getPid()));
        }
        return result;
    }

    private boolean allRequirementsAvailable() {
        return dao != null;
    }

    public boolean findAndSaveDeadLockInformation(String vmId, int pid) {
        Harvester harvester = getHarvester(vmId, pid);
        boolean result = harvester.saveDeadLockData();
        return result;
    }
    
}

