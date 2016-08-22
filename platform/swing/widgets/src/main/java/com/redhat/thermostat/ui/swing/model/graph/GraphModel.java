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

package com.redhat.thermostat.ui.swing.model.graph;

import com.redhat.thermostat.collections.graph.Graph;
import com.redhat.thermostat.collections.graph.HashGraph;
import com.redhat.thermostat.collections.graph.Node;
import com.redhat.thermostat.ui.swing.model.Model;
import com.redhat.thermostat.ui.swing.model.Trace;
import com.redhat.thermostat.ui.swing.model.TraceElement;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class GraphModel extends Model<Graph> {

    /**
     * Long property
     */
    public static final String SAMPLE_COUNT_PROPERTY = "sampleCount";

    /**
     * Long property
     */
    public static final String SAMPLE_WEIGHT_PROPERTY = "weight";

    /**
     * String property
     */
    public static final String TRACE_REAL_ID_PROPERTY = "traceID";

    private int maxDepth;

    private Graph graph;
    private String rootID;
    private Map<String, Node> cache;
    
    public GraphModel(String id) {
        this(id, new HashGraph());
    }

    // For testing
    GraphModel(String id, Graph graph) {
        this.graph = graph;

        rootID = id;
        maxDepth = 0;
        cache = new ConcurrentHashMap<>();
    }

    public Node getRoot() {
        return getCache().get(rootID);
    }

    @Override
    protected Graph getData() {
        return graph;
    }

    public void clearImpl() {
        graph.clear();
        cache.clear();
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    // For testing, so we can study the internal state of the graph
    Map<String, Node> getCache() {
        return cache;
    }

    private Node getOrCreateNode(String id, String realID, long weight) {
        Node node = null;
        if (cache.containsKey(id)) {
            node = cache.get(id);
            long sampleCount = node.getProperty(SAMPLE_COUNT_PROPERTY);
            node.setProperty(SAMPLE_COUNT_PROPERTY, ++sampleCount);
            long sampleWeight = node.getProperty(SAMPLE_WEIGHT_PROPERTY);
            node.setProperty(SAMPLE_WEIGHT_PROPERTY, sampleWeight + weight);
        } else {
            node = new Node(id);
            node.setProperty(TRACE_REAL_ID_PROPERTY, realID);
            node.setProperty(SAMPLE_COUNT_PROPERTY, 1l);
            node.setProperty(SAMPLE_WEIGHT_PROPERTY, weight);

            cache.put(id, node);
        }
        return node;
    }

    public void addTrace(final Trace trace) {
        Node parent = getOrCreateNode(rootID, rootID, 1l);
        int n = 0;
        for (TraceElement element : trace) {
            String id = element.getName() + "#" + n + "." + parent.getName();
            n++;
            Node node = getOrCreateNode(id, element.getName(), element.getWeight());
            graph.addRelationship(parent, "->", node);
            parent = node;
        }
        if (maxDepth < n) {
            maxDepth = n;
        }
    }
}
