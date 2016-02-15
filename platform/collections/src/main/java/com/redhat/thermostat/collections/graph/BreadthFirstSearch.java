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


package com.redhat.thermostat.collections.graph;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class BreadthFirstSearch {

    protected Graph graph;
    protected Queue<Node> queue;
    protected BFSPayload payload;

    public BreadthFirstSearch(Graph graph) {
        this.graph = graph;
        reset();
    }
    
    public BFSPayload getPayload() {
        return payload;
    }
    
    public void reset() {
        queue = new LinkedList<>();
        payload = new BFSPayload();
    }
    
    public void search(Node root, TraversalListener listener) {

        Set<Node> discovered = payload.getDiscovered();
        Map<Node, Node> parents = payload.getParents();
        Set<Node> processed = payload.getProcessed();
        
        queue.offer(root);
        discovered.add(root);
        
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            TraversalListener.Status status = listener.preProcessNode(node, payload);
            if (status.equals(TraversalListener.Status.BREAK)) {
                return;
            }
            
            processed.add(node);
            
            Set<Relationship> relationships = graph.getRelationships(node);
            if (relationships == null) {
                continue;
            }
            
            for (Relationship relationship : relationships) {
                Node endPoint = relationship.getTo();
                
                // the graph is always directed
                status = listener.processRelationship(relationship, payload);
                if (status.equals(TraversalListener.Status.BREAK)) {
                    return;
                }
                
                if (!discovered.contains(endPoint)) {
                    queue.offer(endPoint);
                    discovered.add(endPoint);
                    
                    parents.put(endPoint, node);
                }
            }
            status = listener.postProcessNode(node, payload);
            if (status.equals(TraversalListener.Status.BREAK)) {
                return;
            }
        }
    }
}
