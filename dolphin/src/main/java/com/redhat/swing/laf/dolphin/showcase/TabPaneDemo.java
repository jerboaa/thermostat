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

package com.redhat.swing.laf.dolphin.showcase;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import java.awt.BorderLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JTabbedPane;

import com.redhat.swing.laf.dolphin.DolphinLookAndFeel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;

public class TabPaneDemo {

    private JFrame frame;
    private JTextField textField;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    
                    UIManager.setLookAndFeel(new DolphinLookAndFeel());
                    
                    TabPaneDemo window = new TabPaneDemo();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public TabPaneDemo() {
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 450, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        GroupLayout gl_panel = new GroupLayout(panel);
        gl_panel.setHorizontalGroup(
            gl_panel.createParallelGroup(Alignment.LEADING)
                .addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
        );
        gl_panel.setVerticalGroup(
            gl_panel.createParallelGroup(Alignment.LEADING)
                .addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
        );
        
        JPanel panel_1 = new JPanel();
        tabbedPane.addTab("New tab", null, panel_1, null);
        
        JLabel lblNewLabel = new JLabel("New label");
        
        JButton btnNewButton = new JButton("New button");
        GroupLayout gl_panel_1 = new GroupLayout(panel_1);
        gl_panel_1.setHorizontalGroup(
            gl_panel_1.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_panel_1.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblNewLabel)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(btnNewButton)
                    .addContainerGap(228, Short.MAX_VALUE))
        );
        gl_panel_1.setVerticalGroup(
            gl_panel_1.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_panel_1.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_panel_1.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblNewLabel)
                        .addComponent(btnNewButton))
                    .addContainerGap(223, Short.MAX_VALUE))
        );
        panel_1.setLayout(gl_panel_1);
        
        JPanel panel_2 = new JPanel();
        tabbedPane.addTab("New tab", null, panel_2, null);
        
        textField = new JTextField();
        textField.setColumns(10);
        GroupLayout gl_panel_2 = new GroupLayout(panel_2);
        gl_panel_2.setHorizontalGroup(
            gl_panel_2.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_panel_2.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(textField, GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
                    .addContainerGap())
        );
        gl_panel_2.setVerticalGroup(
            gl_panel_2.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_panel_2.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(textField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(219, Short.MAX_VALUE))
        );
        panel_2.setLayout(gl_panel_2);
        
        JPanel panel_3 = new JPanel();
        tabbedPane.addTab("New tab", null, panel_3, null);
        
        JTextArea textArea = new JTextArea();
        GroupLayout gl_panel_3 = new GroupLayout(panel_3);
        gl_panel_3.setHorizontalGroup(
            gl_panel_3.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_panel_3.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(textArea, GroupLayout.PREFERRED_SIZE, 421, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap())
        );
        gl_panel_3.setVerticalGroup(
            gl_panel_3.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_panel_3.createSequentialGroup()
                    .addGap(5)
                    .addComponent(textArea, GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE)
                    .addContainerGap())
        );
        panel_3.setLayout(gl_panel_3);
        panel.setLayout(gl_panel);
    }
}
