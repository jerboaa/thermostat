/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.components.models;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;

/**
 * Makes a JList non-selectable.
 */
public class NullSelectionModel implements ListSelectionModel {

    @Override
    public void setSelectionInterval(int index0, int index1) {
    }

    @Override
    public void addSelectionInterval(int index0, int index1) {
    }

    @Override
    public void removeSelectionInterval(int index0, int index1) {        
    }

    @Override
    public int getMinSelectionIndex() {
        return 0;
    }

    @Override
    public int getMaxSelectionIndex() {
        return 0;
    }

    @Override
    public boolean isSelectedIndex(int index) {
        return false;
    }

    @Override
    public int getAnchorSelectionIndex() {
        return 0;
    }

    @Override
    public void setAnchorSelectionIndex(int index) {        
    }

    @Override
    public int getLeadSelectionIndex() {
        return -1;
    }

    @Override
    public void setLeadSelectionIndex(int index) {
    }

    @Override
    public void clearSelection() {
    }

    @Override
    public boolean isSelectionEmpty() {
        return true;
    }

    @Override
    public void insertIndexInterval(int index, int length, boolean before) {
    }

    @Override
    public void removeIndexInterval(int index0, int index1) {
    }

    @Override
    public void setValueIsAdjusting(boolean valueIsAdjusting) {
    }

    @Override
    public boolean getValueIsAdjusting() {
        return false;
    }

    @Override
    public void setSelectionMode(int selectionMode) {
    }

    @Override
    public int getSelectionMode() {
        return -1;
    }

    @Override
    public void addListSelectionListener(ListSelectionListener x) {        
    }

    @Override
    public void removeListSelectionListener(ListSelectionListener x) {
    }

}

