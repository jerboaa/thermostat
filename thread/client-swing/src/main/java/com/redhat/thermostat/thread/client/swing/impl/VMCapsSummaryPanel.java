/*
 * Copyright 2012 Red Hat, Inc.
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
package com.redhat.thermostat.thread.client.swing.impl;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.redhat.thermostat.swing.models.NullSelectionModel;

@SuppressWarnings("serial")
class VMCapsSummaryPanel extends JPanel {

    private DefaultListModel<String> listModel;

    /**
     * Create the panel.
     */
    public VMCapsSummaryPanel() {
        setLayout(new GridLayout(0, 1, 0, 0));
        setOpaque(false);
        
        listModel = new DefaultListModel<>();
        
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1, 0, 0));
        panel.setBorder(new TitledBorder(null, "VM Capabilities", TitledBorder.RIGHT, TitledBorder.TOP, null, null));
        panel.setOpaque(false);
        
        add(panel);
        
        JList<String> list = new JList<String>();
        list.setPreferredSize(new Dimension(0, 0));
        list.setOpaque(false);
        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        list.setSelectionModel(new NullSelectionModel());
        
        list.setModel(listModel);
        
        panel.add(list);
    }
    
    void addInfo(List<String> infos) {
        listModel.removeAllElements();
        for (String info : infos) {
            listModel.addElement(info);
        }
    }
}
