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

package com.redhat.thermostat.beans.property;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class BasePropertyTest {
    @Test
    public void testListeners() throws Exception {
        final int [] result = new int[2];

        BaseProperty property = new BaseProperty(Boolean.FALSE);
        property.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                result[0]++;
            }
        });
        property.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
                                Boolean oldValue, Boolean newValue)
            {
                result[1]++;
            }
        });

        // no event should be fired
        property.setValue(false);
        assertEquals(0, result[0]);
        assertEquals(0, result[1]);

        property.setValue(true);
        assertEquals(1, result[0]);
        assertEquals(1, result[0]);

        property.setValue(Boolean.FALSE);
        assertEquals(2, result[0]);
        assertEquals(2, result[0]);
    }

    @Test
    public void testBinding() throws Exception {
        BaseProperty<String> source = new BaseProperty<>("Test");
        BaseProperty<String> bound = new BaseProperty<>("BoundProperty");

        bound.bind(source);
        assertTrue(bound.isBound());

        source.setValue("The property has changed");
        assertEquals(source.getValue(), bound.getValue());
        assertEquals("The property has changed", bound.getValue());
    }

    @Test(expected = BindException.class)
    public void testSetOnBoundThrowException() throws Exception {
        BaseProperty<String> source = new BaseProperty<>("Test");
        BaseProperty<String> bound = new BaseProperty<>("BoundProperty");

        bound.bind(source);

        bound.setValue("Crash me!");
    }

    @Test
    public void testBindingListener() throws Exception {

        final int [] result = new int[1];

        BaseProperty<String> source = new BaseProperty<>("Test");
        BaseProperty<String> bound = new BaseProperty<>("BoundProperty");

        bound.addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (newValue.equals("The property has changed") && oldValue.equals("Test")) {
                    result[0]++;
                } else if (newValue.equals("Test") && oldValue.equals("BoundProperty")) {
                    result[0]++;
                }
            }
        });

        bound.bind(source);
        source.setValue("The property has changed");

        assertEquals(2, result[0]);
    }
}
