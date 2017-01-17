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

import java.util.List;
import java.util.Objects;

import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.populator.internal.BasePopulator;
import com.redhat.thermostat.storage.populator.internal.config.ConfigItem;
import com.redhat.thermostat.storage.populator.internal.dependencies.SharedState;
import com.redhat.thermostat.storage.populator.internal.LocaleResources;

public class HostInfoPopulator extends BasePopulator {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String[] HOSTS_FORMAT = new String[] {
            "vm-host-%06d", "prometheus-%06d", "saturn-%06d"
    };
    private static final String DOMAIN_FORMAT = "domain-%06d";
    private static final String HOSTNAME_SUFFIX = "example.com";
    private static final long MB = 1024 * 1024;

    private HostInfoDAO dao;
    
    public HostInfoPopulator() {
        this(null);
    }

    public HostInfoPopulator(HostInfoDAO dao) {
        this.dao = dao;
    }

    @Override
    public SharedState addPojos(ConfigItem item, SharedState relState, Console console) {
        Objects.requireNonNull(dao,
                translator.localize(LocaleResources.DAO_NOT_INITIALIZED).getContents());
        List<String> agentIds = relState.getProcessedRecordsFor("agentId").getAll();
        int totalItems = agentIds.size();
        int currCount = 0;
        long countBefore = getCount();
        console.getOutput().println("\n" + translator.localize(LocaleResources.POPULATING_RECORDS,
                Integer.toString(totalItems), item.getName()).getContents());
        // There is a 1-to-1 correspondence between host-info and agents
        for (int i = agentIds.size() - 1; i >= 0; i--) {
            String agentId = agentIds.get(i);
            HostInfo info = new HostInfo();
            info.setAgentId(agentId);
            info.setCpuCount(getRandomInt(50 + i));
            info.setCpuModel("x86_64, data populator");
            info.setHostname(getRandomHostName(i));
            info.setOsKernel("4.2.3 data populator");
            info.setOsName("Linux Random Flavor");
            int memory = getRandomInt(500 + i);
            info.setTotalMemory(memory * MB);
            dao.putHostInfo(info);
            currCount++;
        }
        reportSubmitted(item, currCount, console);
        doWaitUntilCount(countBefore + totalItems, console, 200);
        return relState;
    }

    private String getRandomHostName(int i) {
        int index = getRandomInt(HOSTS_FORMAT.length);
        String hostFormat = HOSTS_FORMAT[index] + "." + DOMAIN_FORMAT + "." + HOSTNAME_SUFFIX;
        return String.format(hostFormat, i, i);
    }

    private int getRandomInt(int i) {
        return (int)(Math.random() * i);
    }

    @Override
    long getCount() {
        return dao.getCount();
    }

    @Override
    public String getHandledCollection() {
        return HostInfoDAO.hostInfoCategory.getName();
    }

    public void setDAO(HostInfoDAO dao) {
        this.dao = dao;
    }
}
