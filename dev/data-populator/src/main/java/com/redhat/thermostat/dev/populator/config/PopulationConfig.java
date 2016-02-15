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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.dev.populator.config.typeadapter.ConfigItemTypeAdapter;
import com.redhat.thermostat.dev.populator.config.typeadapter.PopulationConfigTypeAdapterFactory;
import com.redhat.thermostat.dev.populator.config.typeadapter.RelationShipTypeAdapter;
import com.redhat.thermostat.dev.populator.dependencies.Relationship;

public class PopulationConfig {
    
    public static final String RECORDS = "records";
    public static final String RELATIONSHIPS = "relationships";
    private final Map<String, ConfigItem> configs;
    private final List<ConfigItem> allConfigs;
    private final Map<String, List<Relationship>> incomingRelationShips;
    private final Map<String, List<Relationship>> outgoingRelationShips;
    
    private PopulationConfig() {
        configs = new HashMap<>();
        allConfigs = new ArrayList<>();
        incomingRelationShips = new HashMap<>();
        outgoingRelationShips = new HashMap<>();
    }

    public ConfigItem getConfig(String name) {
        return configs.get(name);
    }
    
    public List<ConfigItem> getConfigsTopologicallySorted() {
        return topologicallySort(allConfigs);
    }
    
    /**
     * Sort config items topologically. That is, get them in an order so as
     * to process collections with no incoming links to other collections.
     * 
     * It uses Kahn's algorithm to do so.
     * 
     * @param configs unsorted configs.
     * @return A topologically sorted list.
     */
    private List<ConfigItem> topologicallySort(List<ConfigItem> configs) {
        LinkedList<ConfigItem> sortedList = new LinkedList<>();
        LinkedList<ConfigItem> queue = getConfigItemsWithNoIncomingEdge(configs);
        while (!queue.isEmpty()) {
            ConfigItem n = queue.remove(0);
            sortedList.addLast(n);
            List<Relationship> source = getOutgoingRelationShipsForConfig(n);
            List<Relationship> outgoingRels = new LinkedList<>(source);
            for (Relationship edge: outgoingRels) {
                ConfigItem m = getConfig(edge.getTo());
                removeEdge(edge, n, m);
                if (hasNoIncomingEdge(m)) {
                    queue.addLast(m);
                }
            }
        }
        if (hasRelationShips(configs)) {
            throw new AssertionError("Relationships form at least one cycle! Excpected an acyclic directed graph.");
        }
        return sortedList;
    }

    private boolean hasRelationShips(List<ConfigItem> configs) {
        for (ConfigItem i: configs) {
            List<Relationship> incoming = getIncomingRelationShipsForConfig(i);
            List<Relationship> outgoing = getOutgoingRelationShipsForConfig(i);
            if (!incoming.isEmpty() ||
                    !outgoing.isEmpty()) {
                // We are going to bomb, print some debug info.
                System.err.println("outgoings for " + i.getName() + " " + outgoing);
                System.err.println("incomings for " + i.getName() + " " + incoming);
                return true;
            }
        }
        return false;
    }

    private void removeEdge(Relationship toRemove, ConfigItem from, ConfigItem to) {
        if (from == null || to == null) {
            throw new InvalidConfigurationException("Found relationship [" +
                                                    toRemove +
                    "] but either incoming or outgoing records config is missing");
        }
        List<Relationship> incoming = getIncomingRelationShipsForConfig(to);
        List<Relationship> outgoing = getOutgoingRelationShipsForConfig(from);
        incoming.remove(toRemove);
        outgoing.remove(toRemove);
    }

    private LinkedList<ConfigItem> getConfigItemsWithNoIncomingEdge(List<ConfigItem> allConfigs) {
        LinkedList<ConfigItem> startNodes = new LinkedList<>();
        for (ConfigItem item: allConfigs) {
            if (hasNoIncomingEdge(item)) {
                startNodes.add(item);
            }
        }
        return startNodes;
    }

    private boolean hasNoIncomingEdge(ConfigItem item) {
        List<Relationship> rels = getIncomingRelationShipsForConfig(item);
        if (rels == null || rels.isEmpty()) {
            return true;
        }
        return false;
    }

    private List<Relationship> getIncomingRelationShipsForConfig(ConfigItem item) {
        List<Relationship> result = incomingRelationShips.get(item.getName());
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }
    
    private List<Relationship> getOutgoingRelationShipsForConfig(ConfigItem item) {
        List<Relationship> result = outgoingRelationShips.get(item.getName());
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }
    
    private void addConfig(ConfigItem item) {
        configs.put(item.getName(), item);
        allConfigs.add(item);
    }
    
    private void addIncomingRelationShipForConfig(ConfigItem item, Relationship relationship) {
        addRelationShip(item, relationship, incomingRelationShips);
    }
    
    private void addOutgoingRelationShipForConfig(ConfigItem item, Relationship relationship) {
        addRelationShip(item, relationship, outgoingRelationShips);
    }
    
    private void addRelationShip(ConfigItem item, Relationship relationship, Map<String, List<Relationship>> map) {
        List<Relationship> existingRels = map.get(item.getName());
        if (existingRels == null) {
            existingRels = new LinkedList<>();
            map.put(item.getName(), existingRels);
        }
        existingRels.add(relationship);
    }
    
    public static PopulationConfig parseFromJsonString(String json) throws IOException {
        Gson gson= new GsonBuilder()
                        .registerTypeAdapter(ConfigItem.class, new ConfigItemTypeAdapter())
                        .registerTypeAdapter(Relationship.class, new RelationShipTypeAdapter())
                        .registerTypeAdapterFactory(new PopulationConfigTypeAdapterFactory())
                        .create();
        PopulationConfig pc = gson.fromJson(json, PopulationConfig.class);
        return pc;
    }
    
    public static PopulationConfig createFromLists(List<ConfigItem> items, List<Relationship> rels) {
        PopulationConfig pc = new PopulationConfig();
        for (ConfigItem item: items) {
            pc.addConfig(item);
            for (Relationship r: rels) {
                // keep track of incoming relationships (incoming edge)
                if (r.getTo().equals(item.getName())) {
                    pc.addIncomingRelationShipForConfig(item, r);
                }
                // keep track of outgoing relationships (outgoing edge)
                if (r.getFrom().equals(item.getName())) {
                    pc.addOutgoingRelationShipForConfig(item, r);
                }
            }
        }
        return pc;
    }
    
    @SuppressWarnings("serial")
    static class InvalidConfigurationException extends RuntimeException {
        
        private InvalidConfigurationException(String msg) {
            super(msg);
        }

    }
}
