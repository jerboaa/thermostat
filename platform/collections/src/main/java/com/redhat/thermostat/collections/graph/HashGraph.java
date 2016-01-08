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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 */
public class HashGraph implements Graph {

    private Map<Node, Set<Relationship>> relationships;
    private int size;

    public HashGraph() {
        this.relationships = new HashMap<>();
    }

    @Override
    public Relationship addRelationship(Node source, Node destination, String id) {
        Relationship rel = new Relationship(id, source, destination);
        if (!addRelationship(rel)) {
            return null;
        }
        return rel;
    }

    @Override
    public boolean addRelationship(Relationship relationship) {

        boolean result = false;

        Node root = relationship.getFrom();

        Set<Relationship> rel = relationships.get(root);
        if (rel == null) {
            rel = new HashSet<>();
            relationships.put(root, rel);
        }

        boolean added = rel.add(relationship);
        if (added) {
            size++;
            result = true;
        }

        Node endPoint = relationship.getTo();
        if (!relationships.containsKey(endPoint)) {
            rel = new HashSet<>();
            relationships.put(endPoint, rel);
        }

        return result;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int order() {
        return relationships.size();
    }

    @Override
    public Set<Relationship> getRelationships(Node node) {
        return relationships.get(node);
    }

    /**
     * The iterator is not ordered.
     */
    @Override
    public Iterator<Node> iterator() {
        return relationships.keySet().iterator();
    }
}
