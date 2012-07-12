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

package com.redhat.swing.laf.dolphin.showcase.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GradientPaint;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.JTable;

import com.redhat.swing.laf.dolphin.DolphinLookAndFeel;

public class TableDemo extends JFrame {

    private JPanel contentPane;
    private JTable table;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    
                    UIManager.setLookAndFeel(new DolphinLookAndFeel());
                    
                    TableDemo frame = new TableDemo();
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
    public TableDemo() {
        
        String[] columnNames = {"First Name",
                "Last Name",
                "Sport",
                "# of Years",
                "Vegetarian"};
        
        Object[][] data = {
                { "Kathy", "Smith", "Snowboarding", new Integer(5),
                    new Boolean(false) },
            { "John", "Doe", "Rowing", new Integer(3), new Boolean(true) },
            { "Sue", "Black", "Knitting", new Integer(2),
                    new Boolean(false) },
            { "Jane", "White", "Speed reading", new Integer(20),
                    new Boolean(true) },
            { "Jane", "White", "Speed reading", new Integer(20),
                    new Boolean(true) },
            { "Jane", "White", "Speed reading", new Integer(20),
                    new Boolean(true) },
            { "Jane", "White", "Speed reading", new Integer(20),
                    new Boolean(true) },
            { "Jane", "White", "Speed reading", new Integer(20),
                    new Boolean(true) },
            { "Jane", "White", "Speed reading", new Integer(20),
                    new Boolean(true) },
            { "Jane", "White", "Speed reading", new Integer(20),
                    new Boolean(true) },
            { "Jane", "White", "Speed reading", new Integer(20),
                    new Boolean(true) },
            { "Jane", "White", "Speed reading", new Integer(20),
                    new Boolean(true) },
            { "Jane", "White", "Speed reading", new Integer(20),
                    new Boolean(true) },
                { "Kathy", "Smith", "Snowboarding", new Integer(5),
                        new Boolean(false) },
                { "John", "Doe", "Rowing", new Integer(3), new Boolean(true) },
                { "Sue", "Black", "Knitting", new Integer(2),
                        new Boolean(false) },
                { "Jane", "White", "Speed reading", new Integer(20),
                        new Boolean(true) },
                { "Jane", "White", "Speed reading", new Integer(20),
                        new Boolean(true) },
                { "Jane", "White", "Speed reading", new Integer(20),
                        new Boolean(true) },
                { "Jane", "White", "Speed reading", new Integer(20),
                        new Boolean(true) },
                { "Jane", "White", "Speed reading", new Integer(20),
                        new Boolean(true) },
                { "Jane", "White", "Speed reading", new Integer(20),
                        new Boolean(true) },
                { "Jane", "White", "Speed reading", new Integer(20),
                        new Boolean(true) },
                { "Jane", "White", "Speed reading", new Integer(20),
                        new Boolean(true) },
                { "Jane", "White", "Speed reading", new Integer(20),
                        new Boolean(true) },
                { "Jane", "White", "Speed reading", new Integer(20),
                        new Boolean(true) },
                { "Joe", "Brown",
                 "Pool", new Integer(10), new Boolean(false)}
            };

        table = new JTable(data, columnNames);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        
        JScrollPane scrollPane = new JScrollPane(table);
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 450, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);
        
        contentPane.add(scrollPane, BorderLayout.CENTER);
    }

}
