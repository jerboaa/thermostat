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

package com.redhat.thermostat.dev.populator.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.dev.populator.dependencies.Relationship;

public class PopulationConfigTest {
    
    private static final String NETWORK_INFO_NAME = "network-info";
    private static final String AGENT_CONFIG_NAME = "agent-config";
    private static final String HOST_INFO_NAME = "host-info";
    private static final String VM_INFO_NAME = "vm-info";
    private static final String VM_GC_STAT_NAME = "vm-gc-stats";
    private static final String AGENT_KEY = "foo-agent-key";
    private static final String VM_KEY = "bar-vm-key";

    @Test
    public void canParseFromFile() throws IOException {
        String json = new String(Files.readAllBytes(new File(getClass().getResource("/testconfig").getFile()).toPath()));
        PopulationConfig config = PopulationConfig.parseFromJsonString(json);
        ConfigItem item = config.getConfig("agent-config");
        assertNotNull(item);
        assertEquals(100, item.getNumber());
        assertEquals(20, item.getAliveItems());
        List<ConfigItem> items = config.getConfigsTopologicallySorted();
        assertEquals(7, items.size());
        item = config.getConfig("vm-info");
        assertEquals(50, item.getNumber());
        assertEquals(40, item.getAliveItems());
        item = config.getConfig("vm-gc-stats");
        assertNotNull(item);
    }
    
    /**
     * Tests topological sorting of a simple DAG
     * 
     * <pre>
     * agent-config -> network-info
     *    |----------> host-info
     *    |--> vm-info
     *    |      `--> vm-gc-stat
     *    `------------^
     * </pre>
     * 
     * @throws IOException
     */
    @Test
    public void testTopogicalSort() throws IOException {
        List<ConfigItem> records = buildGoodRecords();
        List<Relationship> rels = buildGoodRels();
        PopulationConfig pc = PopulationConfig.createFromLists(records, rels);
        List<ConfigItem> topSortedList = pc.getConfigsTopologicallySorted();
        assertAgentConfigBeforeNetworkInfo(topSortedList);
        assertAgentConfigBeforeHostInfo(topSortedList);
        assertAgentConfigBeforeVmInfo(topSortedList);
        assertVmInfoBeforeVmGcStat(topSortedList);
        assertAgentConfigBeforeVmGcStat(topSortedList);
    }
    
    private void assertAgentConfigBeforeVmGcStat(List<ConfigItem> topSortedList) {
        assertTrue(isABeforeB(AGENT_CONFIG_NAME, VM_GC_STAT_NAME, topSortedList));
    }

    private void assertVmInfoBeforeVmGcStat(List<ConfigItem> topSortedList) {
        assertTrue(isABeforeB(VM_INFO_NAME, VM_GC_STAT_NAME, topSortedList));
    }

    private void assertAgentConfigBeforeVmInfo(List<ConfigItem> topSortedList) {
        assertTrue(isABeforeB(AGENT_CONFIG_NAME, VM_INFO_NAME, topSortedList));
    }

    private void assertAgentConfigBeforeHostInfo(List<ConfigItem> topSortedList) {
        assertTrue(isABeforeB(AGENT_CONFIG_NAME, HOST_INFO_NAME, topSortedList));
    }

    private void assertAgentConfigBeforeNetworkInfo(List<ConfigItem> topSortedList) {
        assertTrue(isABeforeB(AGENT_CONFIG_NAME, NETWORK_INFO_NAME, topSortedList));
    }
    
    private boolean isABeforeB(String a, String b, List<ConfigItem> list) {
        int idxA = -1;
        int idxB = -1;
        for (int i = 0; i < list.size(); i++) {
            ConfigItem item = list.get(i);
            if (item.getName().equals(a)) {
                if (idxA >= 0) {
                    throw new AssertionError("Illegal state. Re-assigning idxA!");
                }
                idxA = i;
            }
            if (item.getName().equals(b)) {
                if (idxB >= 0) {
                    throw new AssertionError("Illegal state. Re-assigning idxB!");
                }
                idxB = i;
            }
        }
        return idxA < idxB;
    }

    private List<Relationship> buildGoodRels() {
        List<Relationship> relationships = new LinkedList<>();
        Relationship agentConfigToNetworkInfo = new Relationship(AGENT_CONFIG_NAME, NETWORK_INFO_NAME, AGENT_KEY);
        relationships.add(agentConfigToNetworkInfo);
        Relationship agentConfigToVmGcStat = new Relationship(AGENT_CONFIG_NAME, VM_GC_STAT_NAME, AGENT_KEY);
        relationships.add(agentConfigToVmGcStat);
        Relationship agentConfigToHostInfo = new Relationship(AGENT_CONFIG_NAME, HOST_INFO_NAME, AGENT_KEY);
        relationships.add(agentConfigToHostInfo);
        Relationship agentConfigToVmInfo = new Relationship(AGENT_CONFIG_NAME, VM_INFO_NAME, AGENT_KEY);
        relationships.add(agentConfigToVmInfo);
        Relationship vmInfoToVmGcStat = new Relationship(VM_INFO_NAME, VM_GC_STAT_NAME, VM_KEY);
        relationships.add(vmInfoToVmGcStat);
        return relationships;
    }

    private List<ConfigItem> buildGoodRecords() {
        List<ConfigItem> records = new ArrayList<>();
        ConfigItem agentConfig = new ConfigItem(10, 3, AGENT_CONFIG_NAME);
        records.add(agentConfig);
        ConfigItem hostInfo = new ConfigItem(3, 0, HOST_INFO_NAME);
        records.add(hostInfo);
        ConfigItem networkInfo = new ConfigItem(100, 0, NETWORK_INFO_NAME);
        records.add(networkInfo);
        ConfigItem vmGcStat = new ConfigItem(23, 0, VM_GC_STAT_NAME);
        records.add(vmGcStat);
        ConfigItem vmInfo = new ConfigItem(10, 0, VM_INFO_NAME);
        records.add(vmInfo);
        return records;
    }

    /**
     * Tests topological sorting with a cycle. Expected to fail.
     */
    @Test(expected = AssertionError.class)
    public void testTopogicalSortCycle() throws IOException {
        List<ConfigItem> records = buildGoodRecords();
        List<Relationship> rels = buildGoodRels();
        // add a cycle between vm-info -> host-info -> agent-config
        Relationship cyclePartOne = new Relationship(VM_INFO_NAME, HOST_INFO_NAME, VM_KEY);
        rels.add(cyclePartOne);
        Relationship cyclePartTwo = new Relationship(HOST_INFO_NAME, AGENT_CONFIG_NAME, AGENT_KEY);
        rels.add(cyclePartTwo);
        PopulationConfig pc = PopulationConfig.createFromLists(records, rels);
        // This is expected to fail
        pc.getConfigsTopologicallySorted();
    }
    
    @Test(expected = PopulationConfig.InvalidConfigurationException.class)
    public void testTopogicalSortIncomingCollectionMissing() throws IOException {
        List<ConfigItem> records = buildGoodRecords();
        // remove vm-gc-stat records config which has an incoming edge
        // from agent-config and vm-info
        int deleteIdx = -1;
        for (int i = 0; i < records.size(); i++) {
            ConfigItem item = records.get(i);
            if (item.getName().equals(VM_GC_STAT_NAME)) {
                deleteIdx = i;
                break;
            }
        }
        assertTrue("Expected vm-gc-stat to be in records config", deleteIdx >= 0);
        records.remove(deleteIdx);
        List<Relationship> rels = buildGoodRels();
        PopulationConfig pc = PopulationConfig.createFromLists(records, rels);
        pc.getConfigsTopologicallySorted();
    }
}
