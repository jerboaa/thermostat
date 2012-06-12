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

package com.redhat.swing.laf.dolphin.showcase.tree;

import javax.swing.JTree;

class JTreeView {
    
    private JTree tree;
    
    public JTreeView() {
        
        ThermostatTreeNode top =  new ThermostatTreeNode("Root ThermostatTreeNode");
        fillNodes(top);
        
        tree = new JTree(top);        
        tree.setRowHeight(25);
    }
    
    private void fillNodes(ThermostatTreeNode top) {
        ThermostatTreeNode category = null;
        ThermostatTreeNode book = null;
        
        category = new ThermostatTreeNode("Books for Java Programmers");
        top.add(category);
        
        //original Tutorial
        book = new ThermostatTreeNode(new VMInfo
            ("The Java Tutorial: A Short Course on the Basics",
            "tutorial.html"));
        category.add(book);
        
        //Tutorial Continued
        book = new ThermostatTreeNode(new VMInfo
            ("The Java Tutorial Continued: The Rest of the JDK",
            "tutorialcont.html"));
        category.add(book);
        
        //Swing Tutorial
        book = new ThermostatTreeNode(new VMInfo
            ("The Swing Tutorial: A Guide to Constructing GUIs",
            "swingtutorial.html"));
        category.add(book);

        //...add more books for programmers...

        category = new ThermostatTreeNode("Books for Java Implementers");
        top.add(category);

        //VM
        book = new ThermostatTreeNode(new VMInfo
            ("The Java Virtual Machine Specification",
             "vm.html"));
        category.add(book);

        //Language Spec
        book = new ThermostatTreeNode(new VMInfo
            ("The Java Language Specification",
             "jls.html"));
        category.add(book);
    }

    public JTree getTree() {
        return tree;
    }
}
