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

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.LayoutStyle.ComponentPlacement;

import com.redhat.swing.laf.dolphin.DolphinLookAndFeel;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;

public class ButtonsDemo extends JFrame {

    private JPanel contentPane;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(new DolphinLookAndFeel());
                    
                    ButtonsDemo frame = new ButtonsDemo();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public ButtonsDemo() {
        setTitle("Button Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 355, 436);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        
        JPanel panel = new JPanel();
        GroupLayout gl_contentPane = new GroupLayout(contentPane);
        gl_contentPane.setHorizontalGroup(
            gl_contentPane.createParallelGroup(Alignment.LEADING)
                .addComponent(panel, GroupLayout.DEFAULT_SIZE, 379, Short.MAX_VALUE)
        );
        gl_contentPane.setVerticalGroup(
            gl_contentPane.createParallelGroup(Alignment.LEADING)
                .addComponent(panel, GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
        );
        
        JButton btnNewButton = new JButton("Test");
        
        JButton button = new JButton("Test");
        button.setEnabled(false);
        
        JLabel lblNewLabel = new JLabel("Toggle: ");
        lblNewLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        JToggleButton tglbtnNewToggleButton = new JToggleButton("New toggle button");

        JRadioButton rdbtnNewRadioButton = new JRadioButton("Radio");

        JCheckBox chckbxCheckbox = new JCheckBox("CheckBox");

        JCheckBox chckbxDisabled = new JCheckBox("Disabled");
        chckbxDisabled.setEnabled(false);
        chckbxDisabled.setSelected(true);

        JRadioButton rdbtnDisabled = new JRadioButton("Disabled");
        rdbtnDisabled.setEnabled(false);
        GroupLayout gl_panel = new GroupLayout(panel);
        gl_panel.setHorizontalGroup(
            gl_panel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_panel.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblNewLabel, GroupLayout.PREFERRED_SIZE, 83, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
                        .addComponent(tglbtnNewToggleButton, 0, 0, Short.MAX_VALUE)
                        .addComponent(button, GroupLayout.PREFERRED_SIZE, 113, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnNewButton, GroupLayout.PREFERRED_SIZE, 113, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(115, Short.MAX_VALUE))
                .addGroup(gl_panel.createSequentialGroup()
                    .addGap(135)
                    .addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
                        .addGroup(gl_panel.createSequentialGroup()
                            .addComponent(rdbtnDisabled)
                            .addContainerGap())
                        .addGroup(gl_panel.createSequentialGroup()
                            .addComponent(rdbtnNewRadioButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGap(135))))
                .addGroup(gl_panel.createSequentialGroup()
                    .addGap(121)
                    .addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
                        .addGroup(gl_panel.createSequentialGroup()
                            .addComponent(chckbxDisabled, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addContainerGap())
                        .addGroup(gl_panel.createSequentialGroup()
                            .addComponent(chckbxCheckbox, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addGap(122))))
        );
        gl_panel.setVerticalGroup(
            gl_panel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_panel.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(btnNewButton)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(button)
                    .addGap(55)
                    .addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
                        .addComponent(tglbtnNewToggleButton)
                        .addComponent(lblNewLabel, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addComponent(rdbtnNewRadioButton)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(rdbtnDisabled)
                    .addGap(18)
                    .addComponent(chckbxCheckbox)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(chckbxDisabled)
                    .addContainerGap(114, Short.MAX_VALUE))
        );
        panel.setLayout(gl_panel);
        contentPane.setLayout(gl_contentPane);
    }
}
