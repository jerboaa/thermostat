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

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.swing.ActionToggleButton;
import com.redhat.thermostat.swing.HeaderPanel;
import com.redhat.thermostat.thread.client.common.IconResources;
import com.redhat.thermostat.thread.client.common.locale.LocaleResources;

@SuppressWarnings("serial")
class ThreadMainPanel extends JPanel {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private JSplitPane splitPane;
    
    private ActionToggleButton toggleButton;
    
    public ThreadMainPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        
        HeaderPanel headerPanel = new HeaderPanel();
        headerPanel.setHeader(t.localize(LocaleResources.THREAD_CONTROL_PANEL));
        
        JPanel content = new JPanel();
        headerPanel.setContent(content);
        
        toggleButton = new ActionToggleButton(new ImageIcon(IconResources.getRecordIcon().getData().array()));
        toggleButton.setName("recordButton");
        headerPanel.addToolBarButton(toggleButton);
        
        splitPane = new JSplitPane();
        splitPane.setName("threadMainPanelSplitPane");
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        
        GroupLayout gl_content = new GroupLayout(content);
        gl_content.setHorizontalGroup(
            gl_content.createParallelGroup(Alignment.TRAILING)
                .addGroup(Alignment.LEADING, gl_content.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(splitPane, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap())
        );
        gl_content.setVerticalGroup(
            gl_content.createParallelGroup(Alignment.TRAILING)
                .addGroup(Alignment.LEADING, gl_content.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(splitPane, 0, 240, Short.MAX_VALUE)
                    .addContainerGap())
        );
        
        content.setLayout(gl_content);
        
        add(headerPanel);
    }
    
    public JSplitPane getSplitPane() {
        return splitPane;
    }
    
    public ActionToggleButton getToggleButton() {
        return toggleButton;
    }
}
