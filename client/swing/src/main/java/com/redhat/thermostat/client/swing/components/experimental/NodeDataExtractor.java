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

package com.redhat.thermostat.client.swing.components.experimental;

import java.util.Collection;

/**
 * This interface facilitates the extraction of a collection of elements from a dataset and
 * provides methods for reading information from these elements.
 *
 * @param <S> a dataset that aggregates elements of type {@link T}
 * @param <T> an element, holding some weight and labelled by some key (e.g. a pathname) that
 *           expresses a delimited set of (hierarchical) nodes
 */
public interface NodeDataExtractor<S, T> {

    /**
     * This method extracts and returns the nodes from the key of the {@link T} element parameter.
     *
     * @param element a unit, holding some weight and labelled by some key (e.g. a pathname) that
     *           expresses a delimited set of (hierarchical) nodes
     * @return a String array, with an individual node at each index
     */
    String[] getNodes(T element);

    /**
     * This method returns the weight of the {@link T} element parameter.
     */
    double getWeight(T element);

    /**
     * This method extracts and returns the set of {@link T} elements which are aggregated in the
     * {@link S} data parameter.
     */
    Collection<T> getAsCollection(S data);
}
