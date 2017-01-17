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

package com.redhat.thermostat.storage.populator.internal.config;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.collections.graph.DepthFirstSearch;
import com.redhat.thermostat.collections.graph.HashGraph;
import com.redhat.thermostat.storage.populator.internal.config.typeadapter.ConfigItemTypeAdapter;
import com.redhat.thermostat.storage.populator.internal.config.typeadapter.PopulationConfigTypeAdapterFactory;
import com.redhat.thermostat.storage.populator.internal.config.typeadapter.RelationShipTypeAdapter;
import com.redhat.thermostat.collections.graph.Relationship;
import com.redhat.thermostat.collections.graph.TopologicalSort;
import com.redhat.thermostat.collections.graph.Graph;
import com.redhat.thermostat.collections.graph.Node;

public class PopulationConfig {
    
    public static final String RECORDS = "records";
    public static final String RELATIONSHIPS = "relationships";
    public static final String ITEM = "item";
    private final Map<String, Node> configs;
    private final Map<Node, Set<Relationship>> relationshipMap;
    private final Graph graph;
    
    private PopulationConfig() {
        configs = new HashMap<>();
        graph = new HashGraph();
        relationshipMap = new HashMap<>();
    }

    public Node getConfig(String configItem) {
        Node cfg = configs.get(configItem);
        if (cfg == null) {
            throw new InvalidConfigurationException("Config is missing from collection!");
        }
        return cfg;
    }
    
    public List<ConfigItem> getConfigsTopologicallySorted() {
        return topologicallySort();
    }

    /**
     * Sort config items topologically. That is, get them in an order so as
     * to process collections with no incoming links to other collections.
     *
     * It uses the standard DFS algorithm to do so.
     *
     * @return A topologically sorted list.
     */
    private List<ConfigItem> topologicallySort() {
        DepthFirstSearch dfs = new DepthFirstSearch(graph);
        TopologicalSort tsort = new TopologicalSort();
        List<Node> roots = getRoots();
        Set<ConfigItem> cfgs = new LinkedHashSet<>();
        // If no roots were found to start from, the graph must be cyclic or empty.
        if (roots.size() == 0) {
            throw new InvalidConfigurationException("Relationships form cycle!");
        }

        for (Node root : roots) {
            dfs.search(getConfig(root.getName()), tsort);
            for (Node n : tsort.getOrdered()) {
                Node cfg = getConfig(n.getName());
                cfgs.add((ConfigItem) cfg.getProperty(ITEM));
            }
            // Isolated nodes will return empty from DFS
            if (tsort.getOrdered().size() == 0) {
                cfgs.add((ConfigItem) root.getProperty(ITEM));
            }
            dfs.reset();
            tsort.reset();
        }

        List<ConfigItem> ordered = new ArrayList<>(cfgs);
        Collections.reverse(ordered);
        return ordered;
    }

    private List<Node> getRoots() {
        List<Node> roots = new ArrayList<>();
        for (Node n : relationshipMap.keySet()) {
            if (relationshipMap.get(n).size() == 0) {
                roots.add(n);
            }
        }
        return roots;
    }

    private void addConfig(ConfigItem configItem) {
        Node item = new Node(configItem.getName());
        item.setProperty(ITEM, configItem);
        configs.put(item.getName(), item);
        relationshipMap.put(item, new HashSet<Relationship>());
    }
    
    private void addRelationShip(Relationship relationship) {
        graph.addRelationship(relationship);
        if (relationshipMap.get(relationship.getTo()) == null) {
            Set<Relationship> rels = new HashSet<>();
            rels.add(relationship);
            relationshipMap.put(relationship.getTo(), rels);
        }
        else {
            relationshipMap.get(relationship.getTo()).add(relationship);
        }
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
                pc.addRelationShip(r);
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
