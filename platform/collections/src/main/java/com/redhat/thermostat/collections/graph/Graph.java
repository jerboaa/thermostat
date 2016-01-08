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

import java.util.Iterator;
import java.util.Set;

/**
 * A simple interface defining methods for a general purpose Graph.
 */
public interface Graph extends Iterable<Node> {

    /**
     * Creates a new {@link Relationship} to this Graph
     * from the given {@code node}s and {@code id}.
     * Returns the {@link Relationship} just created or {@code null} if the
     * {@link Relationship} was not added to the Graph.
     */
    Relationship addRelationship(Node source, Node destination, String id);

    /**
     * Adds the given {@link Relationship} to this Graph.
     * Returns {@code true} if the operation was successful, {@code false}
     * otherwise.
     */
    boolean addRelationship(Relationship relationship);

    /**
     * Returns the size of this Graph. The size is intended as the number of
     * {@link Relationship}s contained in the Graph.
     */
    int size();

    /**
     * Returns the order, or the number of {@link Node}s, of this Graph.
     */
    int order();

    /**
     * Returns a {@link Set<Relationship>} containing all the relationship
     * in this Graph.
     */
    Set<Relationship> getRelationships(Node node);
    
    /**
     * Returns an iterator over this graph nodes. The order of the iterator
     * is not defined and is implementation dependent.
     */
    @Override
    Iterator<Node> iterator();
}
