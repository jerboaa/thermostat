/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import java.util.Map;
import java.util.Set;

import com.redhat.thermostat.collections.graph.TraversalListener.Status;

/**
 *
 */
public class DepthFirstSearch {
    protected Graph graph;
    
    protected Status status;
    protected Set<Node> discovered;
    protected Map<Node, Node> parents;
    
    protected DFSPayload payload;
    protected Map<Node, Integer> entryTimes;
    protected Map<Node, Integer> exitTimes;
    
    protected Set<Node> processed;
    
    protected int time;
    
    public DepthFirstSearch(Graph graph) {
        this.graph = graph;
        reset();           
    }

    public DFSPayload getPayload() {
        return payload;
    }
    
    public void reset() {
        payload = new DFSPayload();

        processed = payload.getProcessed();
        discovered = payload.getDiscovered();
        parents = payload.getParents();
        
        entryTimes = payload.getEntryTimes();
        exitTimes = payload.getExitTimes();
        
        time = 0;
        
        status = Status.CONTINUE; 
    }
    
    public void search(Node node, TraversalListener listener) {
        
        if (status.equals(Status.BREAK)) {
            return;
        }

        discovered.add(node);
        time++;
        entryTimes.put(node, new Integer(time));

        status = listener.preProcessNode(node, payload);
        if (status.equals(Status.BREAK)) {
            return;
        }
        
        Set<Relationship> relationships = graph.getRelationships(node);
        if (relationships == null) {
            return;
        }
        
        for (Relationship relationship : relationships) {
            Node endPoint = relationship.getTo();
            if (!discovered.contains(endPoint)) {
                parents.put(endPoint, node);
                status = listener.processRelationship(relationship, payload);
                if (status.equals(TraversalListener.Status.BREAK)) {
                    return;
                }
                
                search(endPoint, listener);
                
            } else {
                status = listener.processRelationship(relationship, payload);
                if (status.equals(TraversalListener.Status.BREAK)) {
                    return;
                }
            }
        }
        
        status = listener.postProcessNode(node, payload);
        if (status.equals(TraversalListener.Status.BREAK)) {
            return;
        }
        
        time++;
        exitTimes.put(node, new Integer(time));
        
        processed.add(node);
    }
    
    public static RelationshipType classify(Relationship relationship, DFSPayload payload) {
        
        Node source = relationship.getFrom();
        Node destination = relationship.getTo();
        
        Node parent = payload.getParents().get(destination);
        if (parent != null && parent.equals(source)) {
            return RelationshipType.TREE;
        }
        
        if (payload.getDiscovered().contains(destination)) {
            parent = payload.getParents().get(source);
            if (parent != null && !parent.equals(destination)) {
                return RelationshipType.BACK;
            }
        }
        
        if (payload.getProcessed().contains(destination)) {
            Map<Node, Integer> entryTimes = payload.getEntryTimes();
            if (!entryTimes.containsKey(source) || !entryTimes.containsKey(destination)) {
                return RelationshipType.UNKNOWN;
            }
            
            int sourceEntryTime = entryTimes.get(source).intValue();
            int destinationEntryTime = entryTimes.get(destination).intValue();
            if (destinationEntryTime > sourceEntryTime) {
                return RelationshipType.FORWARD;
            }
            
            if (destinationEntryTime < sourceEntryTime) {
                return RelationshipType.CROSS;
            }
        }
        
        return RelationshipType.UNKNOWN;
    }
}
