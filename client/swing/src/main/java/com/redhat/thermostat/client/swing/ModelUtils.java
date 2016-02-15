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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.DefaultListModel;

public final class ModelUtils {

    private ModelUtils() {
        // do not instantiate
    }

    /**
     * Update the swing model based on the provided list, adding new items to
     * the swing model and removing no-longer-present items from the swing
     * model.
     */
    public static <T> void updateListModel(final List<T> list, DefaultListModel<T> model) {
        for (T item : list) {
            if (!model.contains(item)) {
                model.addElement(item);
            }
        }

        List<T> toRemove = new ArrayList<>();
        Enumeration<T> e = model.elements();
        while (e.hasMoreElements()) {
            T item = e.nextElement();
            if (!list.contains(item)) {
                toRemove.add(item);
            }
        }

        for (T item : toRemove) {
            model.removeElement(item);
        }

    }

}
