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
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle.ComponentPlacement;

import com.redhat.thermostat.swing.HeaderPanel;

@SuppressWarnings("serial")
class ThreadMainPanel extends JPanel {

    private JToggleButton liveRecording;
    private JButton snapshot;
    private JSplitPane splitPane;
    
    public ThreadMainPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        
        HeaderPanel headerPanel = new HeaderPanel();
        headerPanel.setHeader("Thread Control Panel");
        
        JPanel content = new JPanel();
        headerPanel.setContent(content);
        
        JPanel controlPanel = new JPanel();
        
        splitPane = new JSplitPane();
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        
        GroupLayout gl_content = new GroupLayout(content);
        gl_content.setHorizontalGroup(
            gl_content.createParallelGroup(Alignment.TRAILING)
                .addGroup(gl_content.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(splitPane, GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(controlPanel, GroupLayout.PREFERRED_SIZE, 260, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap())
        );
        gl_content.setVerticalGroup(
            gl_content.createParallelGroup(Alignment.LEADING)
                .addGroup(Alignment.TRAILING, gl_content.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_content.createParallelGroup(Alignment.TRAILING)
                        .addComponent(splitPane, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 313, Short.MAX_VALUE)
                        .addComponent(controlPanel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE))
                    .addContainerGap())
        );
        
        snapshot = new JButton("Thread Dump");
        
        liveRecording = new JToggleButton();
        
        JScrollPane scrollPane = new JScrollPane();
        GroupLayout gl_controlPanel = new GroupLayout(controlPanel);
        gl_controlPanel.setHorizontalGroup(
            gl_controlPanel.createParallelGroup(Alignment.LEADING)
                .addComponent(liveRecording, GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                .addComponent(snapshot, GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
        );
        gl_controlPanel.setVerticalGroup(
            gl_controlPanel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_controlPanel.createSequentialGroup()
                    .addComponent(liveRecording)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(snapshot)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 251, Short.MAX_VALUE))
        );
        
        JList<String> list = new JList<>();
        scrollPane.setViewportView(list);
        controlPanel.setLayout(gl_controlPanel);
        
        content.setLayout(gl_content);
        
        add(headerPanel);
    }
    
    JToggleButton getLiveRecording() {
        return liveRecording;
    }
    
    JButton getSnapshot() {
        return snapshot;
    }
    public JSplitPane getSplitPane() {
        return splitPane;
    }
}
