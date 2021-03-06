/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.storage.populator.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.storage.populator.internal.BasePopulator;
import com.redhat.thermostat.storage.populator.internal.config.ConfigItem;
import com.redhat.thermostat.storage.populator.internal.dependencies.ProcessedRecords;
import com.redhat.thermostat.storage.populator.internal.dependencies.SharedState;
import com.redhat.thermostat.storage.populator.internal.LocaleResources;

public class VmInfoPopulator extends BasePopulator {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private VmInfoDAO dao;
    
    public VmInfoPopulator() {
        this(null);
    }
    
    public VmInfoPopulator(VmInfoDAO dao) {
        this.dao = dao;
    }

    @Override
    public SharedState addPojos(ConfigItem item, SharedState relState, Console console) {
        Objects.requireNonNull(dao,
                translator.localize(LocaleResources.DAO_NOT_INITIALIZED).getContents());
        // Default to all alive, if unset
        int aliveItems = item.getAliveItems() == ConfigItem.UNSET ? item.getNumber() : item.getAliveItems();
        ProcessedRecords<String> procAgents = relState.getProcessedRecordsFor("agentId");
        List<String> vmIds = new ArrayList<>();
        List<String> allAgents = procAgents.getAll();
        long countBefore = getCount();
        int totalItems = item.getNumber() * allAgents.size();
        // populate VM records per agentID
        console.getOutput().println("\n" + translator.localize(LocaleResources.POPULATING_RECORDS,
                Integer.toString(totalItems), item.getName()).getContents());
        int currVal = 0;
        for (String agentId: allAgents) {
            for (int i = 0; i < item.getNumber(); i++) {
                VmInfo info = new VmInfo();
                info.setAgentId(agentId);
                info.setJavaCommandLine("java DataPopulatorProducedVM --vm " + i  + " --version");
                info.setJavaHome("/opt/foo/bar/java-1.8.0-openjdk/jre");
                info.setJavaVersion("1.8.0");
                info.setMainClass("com.redhat.thermostat.TestDataPopulator");
                info.setLoadedNativeLibraries(new String[] { "glibc.so", "something.so" });
                info.setProperties(getFakeJavaProperties(i, agentId));
                StartStopTimeStamp ts = getFakeStartStopTimeStamp(i, aliveItems);
                info.setStartTimeStamp(ts.startTS);
                info.setStopTimeStamp(ts.stopTS);
                info.setUsername("vm-user-" + i);
                info.setUid(i);
                info.setVmArguments("-XX:+UseG1Gc");
                String vmId = UUID.randomUUID().toString();
                info.setVmId(vmId);
                vmIds.add(vmId);
                info.setVmInfo("OpenJDK 8");
                info.setVmPid(34 + i);
                info.setVmVersion("hs 25");
                info.setVmName("Hotspot");
                dao.putVmInfo(info);
                currVal++;
            }
        }
        reportSubmitted(item, currVal, console);
        doWaitUntilCount(countBefore + totalItems, console, 200);
        relState.addProcessedRecords("vmId", new ProcessedRecords<>(vmIds));
        return relState;
    }
    
    private StartStopTimeStamp getFakeStartStopTimeStamp(int num, int aliveItems) {
        long currTime = System.currentTimeMillis();
        if (num < aliveItems) {
            // create alive timestamp pair
            long startTs = Long.MAX_VALUE;
            long stopTs = currTime;
            return new StartStopTimeStamp(startTs, stopTs);
        }
        // create dead timestamp pair
        return new StartStopTimeStamp(currTime - (1000 + num), currTime - (500 + num));
    }

    private Map<String, String> getFakeJavaProperties(int i, String agentId) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("foo", "bar");
        props.put("vm#", Integer.toString(i));
        props.put("agentId", agentId);
        return props;
    }

    @Override
    long getCount() {
        return dao.getCount();
    }

    @Override
    public String getHandledCollection() {
        return VmInfoDAO.vmInfoCategory.getName();
    }
    
    private static class StartStopTimeStamp {
        private final long startTS;
        private final long stopTS;
        
        private StartStopTimeStamp(long start, long stop) {
            this.startTS = start;
            this.stopTS = stop;
        }
    }

    public void setDAO(VmInfoDAO dao) {
        this.dao = dao;
    }
}
