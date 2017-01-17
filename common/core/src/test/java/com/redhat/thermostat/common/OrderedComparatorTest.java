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

package com.redhat.thermostat.common;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class OrderedComparatorTest {

    @Test
    public void testServiceOrderTie() {
        int[] orderValues = { 45, 20, 0, 90, 20 };

        final List<Ordered> services = mockServices(orderValues);
        
        // Override the getName method to give predetermined class names to
        // the services with equal order value
        OrderedComparator<Ordered> comparator = new OrderedComparator<Ordered>() {
            @Override
            String getName(Ordered object) {
                String result;
                if (object.equals(services.get(1))) {
                    result = "TheirService";
                }
                else if (object.equals(services.get(4))) {
                    result = "MyService";
                }
                else {
                    result = super.getName(object); 
                }
                return result;
            }
        };
        
        List<Ordered> sorted = new ArrayList<>(services);
        Collections.sort(sorted, comparator);
        
        // Ensure MyService comes before TheirService
        assertEquals(services.get(2), sorted.get(0));
        assertEquals(services.get(4), sorted.get(1));
        assertEquals(services.get(1), sorted.get(2));
        assertEquals(services.get(0), sorted.get(3));
        assertEquals(services.get(3), sorted.get(4));
    }

    private List<Ordered> mockServices(int[] orderValues) {
        List<Ordered> services = new ArrayList<>();
        for (int value : orderValues) {
            Ordered service = mock(Ordered.class);
            when(service.getOrderValue()).thenReturn(value);
            services.add(service);
        }
        return services;
    }

}

