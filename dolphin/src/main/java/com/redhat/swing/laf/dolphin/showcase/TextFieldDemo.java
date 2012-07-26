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
import javax.swing.SpringLayout;
import javax.swing.JPanel;
import javax.swing.UIManager;

import java.awt.BorderLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JTextArea;

import com.redhat.swing.laf.dolphin.DolphinLookAndFeel;
import javax.swing.JPasswordField;
import javax.swing.JFormattedTextField;
import javax.swing.JTextPane;

public class TextFieldDemo {

    private JFrame frame;
    private JTextField textField;
    private JPasswordField passwordField;
    private JFormattedTextField formattedTextField;
    private JTextPane textPane;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(new DolphinLookAndFeel());

                    TextFieldDemo window = new TextFieldDemo();
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
    public TextFieldDemo() {
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

        JLabel lblNewLabel = new JLabel("text field:");

        textField = new JTextField();
        textField.setColumns(10);

        JLabel lblNewLabel_1 = new JLabel("text area:");

        JTextArea textArea = new JTextArea();

        JLabel lblPassswordField = new JLabel("passsword field");

        passwordField = new JPasswordField();

        JLabel lblFormattedField = new JLabel("formatted field");

        formattedTextField = new JFormattedTextField();

        JLabel lblTextpane = new JLabel("textpane");

        textPane = new JTextPane();
        GroupLayout gl_panel = new GroupLayout(panel);
        gl_panel.setHorizontalGroup(
            gl_panel.createParallelGroup(Alignment.LEADING)
                .addGroup(Alignment.TRAILING, gl_panel.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
                        .addComponent(textPane, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 549, Short.MAX_VALUE)
                        .addComponent(textArea, GroupLayout.DEFAULT_SIZE, 416, Short.MAX_VALUE)
                        .addGroup(Alignment.LEADING, gl_panel.createSequentialGroup()
                            .addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
                                .addComponent(lblPassswordField)
                                .addComponent(lblNewLabel)
                                .addComponent(lblFormattedField))
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
                                .addComponent(formattedTextField, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE)
                                .addComponent(textField, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE)
                                .addComponent(passwordField, GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)))
                        .addComponent(lblNewLabel_1, Alignment.LEADING)
                        .addComponent(lblTextpane, Alignment.LEADING))
                    .addContainerGap())
        );
        gl_panel.setVerticalGroup(
            gl_panel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_panel.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
                        .addComponent(lblNewLabel)
                        .addComponent(textField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
                        .addComponent(lblPassswordField)
                        .addGroup(gl_panel.createSequentialGroup()
                            .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
                                .addComponent(formattedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblFormattedField))))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(lblNewLabel_1)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(textArea, GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(lblTextpane)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(textPane, GroupLayout.DEFAULT_SIZE, 222, Short.MAX_VALUE)
                    .addContainerGap())
        );
        panel.setLayout(gl_panel);
    }
}
