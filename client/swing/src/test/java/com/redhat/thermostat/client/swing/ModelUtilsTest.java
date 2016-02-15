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

package com.redhat.thermostat.client.swing;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;

import org.junit.Test;

public class ModelUtilsTest {

    @Test
    public void verifyNewEntriesAreAdded() throws Exception {
        DefaultListModel<Integer> model = new DefaultListModel<>();

        List<Integer> data = new ArrayList<>();
        data.add(0);
        data.add(1);
        data.add(2);

        ModelUtils.updateListModel(data, model);

        assertEquals(3, model.size());
        assertEquals((Integer) 0, model.get(0));
        assertEquals((Integer) 1, model.get(1));
        assertEquals((Integer) 2, model.get(2));
    }

    @Test
    public void verifyMissingEntriesAreRemoved() throws Exception {
        DefaultListModel<Integer> model = new DefaultListModel<>();
        model.addElement(0);
        model.addElement(1);
        model.addElement(2);

        List<Integer> data = new ArrayList<>();

        ModelUtils.updateListModel(data, model);

        assertEquals(0, model.size());
    }

    @Test
    public void verifyEntriesAreReplacedCorrectly() throws Exception {
        DefaultListModel<Integer> model = new DefaultListModel<>();
        model.addElement(0);
        model.addElement(1);
        model.addElement(2);
        model.addElement(3);

        List<Integer> data = new ArrayList<>();
        data.add(1);
        data.add(3);

        ModelUtils.updateListModel(data, model);

        assertEquals(2, model.size());
        assertEquals((Integer) 1, model.get(0));
        assertEquals((Integer) 3, model.get(1));
    }

}
