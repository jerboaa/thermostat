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

package com.redhat.thermostat.collections.graph;

import java.util.HashMap;
import java.util.Map;

/**
 * A class abstracting a connection between two {@link Node}s, such as an Edge
 * in a Graph.
 */
public class Relationship {
    
    public static final String NAME = "name";
    
    private Node from;
    private Node to;
    
    private Map<String, Object> properties;
    
    public Relationship(Node from, String name, Node to) {
        
        this.from = from;
        this.to = to;
        
        properties = new HashMap<>();
        properties.put(NAME, name);
    }
    
    public void setProperty(String propertyName, Object propertyValue) {
        if (propertyName.equals(NAME)) {
            throw new IllegalArgumentException("\"" + NAME + "\" is an immutable property");
        }
        properties.put(propertyName, propertyValue);
    }
    
    public final String getName() {
        return (String) properties.get(NAME);
    }

    @SuppressWarnings("unchecked")
    public <E> E getProperty(String propertyName) {
        return (E) properties.get(propertyName);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Node getFrom() {
        return from;
    }
    
    public Node getTo() {
        return to;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((from == null) ? 0 : from.hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((to == null) ? 0 : to.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Relationship other = (Relationship) obj;
        if (from == null) {
            if (other.from != null)
                return false;
        } else if (!from.equals(other.from))
            return false;
        if (getName() == null) {
            if (other.getName() != null)
                return false;
        } else if (!getName().equals(other.getName()))
            return false;
        if (to == null) {
            if (other.to != null)
                return false;
        } else if (!to.equals(other.to))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        builder.append(from).append(")-");
        builder.append("[:").append(getName()).append("]");
        builder.append("->(").append(to).append(")");
        
        return builder.toString();
    }
}
