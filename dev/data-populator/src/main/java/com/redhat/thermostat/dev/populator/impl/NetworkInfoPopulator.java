/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.dev.populator.impl;

import java.util.List;

import com.redhat.thermostat.dev.populator.config.ConfigItem;
import com.redhat.thermostat.dev.populator.dependencies.ProcessedRecords;
import com.redhat.thermostat.dev.populator.dependencies.SharedState;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.Countable;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.internal.dao.NetworkInterfaceInfoDAOImpl;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.NetworkInterfaceInfo;

public class NetworkInfoPopulator extends BasePopulator {
    
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
    
    private final NetworkInterfaceDAOCountable dao;
    
    public NetworkInfoPopulator() {
        this(null);
    }
    
    NetworkInfoPopulator(NetworkInterfaceDAOCountable dao) {
        this.dao = dao;
    }

    @Override
    public SharedState addPojos(Storage storage, ConfigItem item, SharedState relState) {
        ProcessedRecords<String> procAgents = relState.getProcessedRecordsFor("agentId");
        NetworkInterfaceDAOCountable dao = getDao(storage);
        List<String> allAgents = procAgents.getAll();
        long countBefore = dao.getCount();
        int totalItems = item.getNumber() * allAgents.size();
        // populate network info records per agentID
        System.out.println("Populating "+ totalItems  + " " + item.getName() + " records");
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
                reportProgress(item, currVal);
                currVal++;
            }
        }
        doWaitUntilCount(dao, countBefore + totalItems);
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

    private NetworkInterfaceDAOCountable getDao(Storage storage) {
        if (this.dao == null) {
            return new NetworkInterfaceDAOCountable(storage);
        }
        return dao;
    }

    @Override
    public String getHandledCollection() {
        return NetworkInterfaceInfoDAO.networkInfoCategory.getName();
    }
    
    static class NetworkInterfaceDAOCountable extends NetworkInterfaceInfoDAOImpl implements Countable {

        private final Category<AggregateCount> aggregateCategory;
        private final Storage storage;
        
        NetworkInterfaceDAOCountable(Storage storage) {
            super(storage);
            CategoryAdapter<NetworkInterfaceInfo, AggregateCount> adapter = new CategoryAdapter<>(networkInfoCategory);
            aggregateCategory = adapter.getAdapted(AggregateCount.class);
            storage.registerCategory(aggregateCategory);
            this.storage = storage;
        }

        @Override
        public long getCount() {
            String descriptor = "QUERY-COUNT " + networkInfoCategory.getName();
            try {
                PreparedStatement<AggregateCount> stmt = storage.prepareStatement(new StatementDescriptor<>(aggregateCategory, descriptor));
                Cursor<AggregateCount> cursor = stmt.executeQuery();
                AggregateCount count = cursor.next();
                return count.getCount();
            } catch (DescriptorParsingException | StatementExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        
    }

}
