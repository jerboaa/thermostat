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
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.model.NetworkInterfaceInfo;
import com.redhat.thermostat.storage.populator.internal.config.ConfigItem;
import com.redhat.thermostat.storage.populator.internal.dependencies.ProcessedRecords;
import com.redhat.thermostat.storage.populator.internal.dependencies.SharedState;

public class NetworkInfoPopulator extends BasePopulator {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    static final String[] IPV4_FORMATS = new String[] {
            "192.168.0.%d", "10.33.4.%d", "89.15.93.%d"
    };
    static final String[] IPV6_FORMATS = new String[] {
            "e8:b1:fc:d2:e3:%s%%%s", "fe80::56ee:75ff:fe35:%s%%%s",
            "0:0:0:0:0:0:0:%s%%%s"
    };
    private final String[] IFACE_NAMES = new String[] {
            "lo", "enp0s25", "tun0", "virbr0", "wlp4s0"
    };
    private static int IPv6Mod = Integer.parseInt("ffff", 16);
    private static int IPv4Mod = 255;

    private NetworkInterfaceInfoDAO dao;

    public NetworkInfoPopulator() {
        this(null);
    }

    public NetworkInfoPopulator(NetworkInterfaceInfoDAO dao) {
        this.dao = dao;
    }

    @Override
    public SharedState addPojos(ConfigItem item, SharedState relState, Console console) {
        Objects.requireNonNull(dao,
                translator.localize(LocaleResources.DAO_NOT_INITIALIZED).getContents());
        ProcessedRecords<String> procAgents = relState.getProcessedRecordsFor("agentId");
        List<String> allAgents = procAgents.getAll();
        long countBefore = getCount();
        int totalItems = item.getNumber() * allAgents.size();
        // populate network info records per agentID
        console.getOutput().println("\n" + translator.localize(LocaleResources.POPULATING_RECORDS,
                Integer.toString(totalItems), item.getName()).getContents());
        int currVal = 0;
        for (String agentId: allAgents) {
            for (int i = 0; i < item.getNumber(); i++) {
                NetworkInterfaceInfo info = new NetworkInterfaceInfo();
                info.setAgentId(agentId);
                String name = getRandomInterfaceName(i);
                info.setInterfaceName(name);
                info.setIp6Addr(getRandomIpv6Address(i, name));
                info.setIp4Addr(getRandomIpv4Addrees(i));
                dao.putNetworkInterfaceInfo(info);
                currVal++;
            }
        }
        reportSubmitted(item, currVal, console);
        doWaitUntilCount(countBefore + totalItems, console, 200);
        return relState;
    }

    private String getRandomIpv4Addrees(int i) {
        int idx = getRandomInt(IPV4_FORMATS.length);
        return String.format(IPV4_FORMATS[idx], getIpv4Octet(i));
    }

    private int getRandomInt(int upperBound) {
        return (int)(Math.random() * upperBound);
    }

    private String getRandomIpv6Address(int i, String name) {
        int idx = getRandomInt(IPV6_FORMATS.length);
        return String.format(IPV6_FORMATS[idx], getIpv6Hextet(i), name);
    }
    
    // package-private for testing
    String getIpv6Hextet(int i) {
        int remainder = i % IPv6Mod;
        if (remainder == 0) {
            // 0 isn't really an IP, make it 1 instead.
            remainder = 1;
        }
        return Integer.toHexString(remainder);
    }
    
    // package-private for testing
    int getIpv4Octet(int i) {
        int remainder = i % IPv4Mod;
        if (remainder == 0) {
            // 0 is network address, switch to IP
            remainder = 1;
        }
        return remainder;
    }

    private String getRandomInterfaceName(int i) {
        int idx = getRandomInt(IFACE_NAMES.length);
        return IFACE_NAMES[idx] + i;
    }

    @Override
    long getCount() {
        return dao.getCount();
    }

    @Override
    public String getHandledCollection() {
        return NetworkInterfaceInfoDAO.networkInfoCategory.getName();
    }

    public void setDAO(NetworkInterfaceInfoDAO dao) {
        this.dao = dao;
    }
}
