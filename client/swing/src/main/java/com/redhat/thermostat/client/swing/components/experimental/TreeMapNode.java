/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.components.experimental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class represents a single tree node, and serves as the fundamental unit of a TreeMap
 * model.  Each node is labelled by an arbitrary String and has an associated weight.
 *
 * <p>When an instance of this class is created, it will automatically be
 * assigned a unique id.
 * 
 * <p>A static Quick Sort algorithm implementation is also provided by this class.
 */
public class TreeMapNode {
        
    /**
     * Counter for assign unique id to nodes.
     */
    private static int idCounter = 0;

    /**
     * This node's id.
     */
    private int id;
    
    /**
     * A Map in which store information for this node.
     */
    private Map<String, String> info;
    
    /**
     * Reference to the parent.
     */
    private TreeMapNode parent;
    
    /**
     * Reference to children.
     */
    private List<TreeMapNode> children;
    
    /**
     * The node's label. It can be the same of another node.
     */
    private String label;
    
    /**
     * The node's weight.
     */
    private double realWeight;
    
    /**
     * 
     * Constructor that allow to set the nodes' real weight. Others fields are 
     * initialized to their default value.
     * It automatically set the node's id.
     *
     * <p>
     * @param realWeight the nodes real weight, which will be not affected 
     * during node processing.
     * 
     */
    public TreeMapNode(double realWeight) {
        this("", realWeight);
    }

    /**
     * 
     * Constructor that allow to set the nodes' real weight and the label. 
     * Others fields are initialized to their default value.
     * It automatically set the node's id.
     *
     * <p>
     * @param label the node's label.
     * @param realWeight the nodes real weight, which will be not affected 
     * during node processing.
     * 
     */
    public TreeMapNode(String label, double realWeight) {
        this.id = idCounter++;
        this.label = label;
        this.parent = null;
        this.children = new ArrayList<TreeMapNode>();
        this.info = new HashMap<String, String>();
        this.realWeight = realWeight;
    }

    /**
     * Return the id of this object.
     * @return the id automatically assigned at this object initialization.
     */
    public int getId() {
        return this.id;
    }
    
    /**
     * Set this node's label.
     * @param newLabel the new label to set.
     */
    public void setLabel(String newLabel) {
        this.label = newLabel;
    }
    
    /**
     * Return the label of this object.
     * @return the label assigned at instantiation time to this object.
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Return the reference to the node parent of this object.
     * @return the parent of this node. It can be null.
     */
    public TreeMapNode getParent() {
        return this.parent;
    }

    /**
     * Set as parent of this object the node given in input.
     * @param parent the new parent of this object. No checks are made for null
     * value.
     */
    public void setParent(TreeMapNode parent) {
        this.parent = parent;
    }

    /**
     * Return the list of nodes representing this node's children.
     * @return a list of {@link TreeMapNode} objects.
     */
    public List<TreeMapNode> getChildren() {
        return this.children;
    }

    public boolean isLeaf() {
        return getChildren().isEmpty();
    }
    
    /**
     * Set as children list of this object the list given in input.
     * @param children the new list of children for this node.
     */
    public void setChildren(List<TreeMapNode> children) {
        this.children = children;
        for (TreeMapNode child : this.children) {
            child.setParent(this);
        }
    }

    /**
     * Return the {@link Map} object containing all information of this node.
     * @return a {@link Map} object.
     */
    public Map<String, String> getInfo() {
        return this.info;
    }
    
    /**
     * Store the given information into this object.
     * @param key the key searching value for the information to store.
     * @param value the information to store into this object.
     * @return the old value for the given key.
     */
    public String addInfo(String key, String value) {
        return this.info.put(key, value);
    }

    /**
     * Return the information stored in this object, corresponding to the key 
     * given in input.
     * @param key the key value for the search information.
     * @return the corresponding value for the given key.
     */
    public String getInfo(String key) {
        return this.info.get(key);
    }

    /**
     * Add the object given in input to the children list of this object. It 
     * also add this object as its parent.
     * @param child the new child to add at this object.
     */
    public void addChild(TreeMapNode child) {
        if (child != null) {
            this.children.add(child);
            child.setParent(this);
        }
    }

    @Override
    public String toString() {
        return  getClass().getSimpleName() + " [" + "label = " + getLabel() + 
                "; weight =" + getRealWeight() + "]";
    }

    /**
     * Use this method to retrieve the real weight assigned to this node.
     * @return the weight corresponding to this node.
     */
    public double getRealWeight() {
        return this.realWeight;
    }
    
    /**
     * Use this method to set the real weight of this node.
     */
    public void setRealWeight(double w) {
        this.realWeight = w;
    }

    public int getDepth() {
        if (this.parent == null) {
            return 0;
        } else {
            return 1 + parent.getDepth();
        }
    }

    /**
     * This method sorts the given list in <b>descending<b> way.
     * 
     * @param nodes the list of {@link TreeMapNode} to sort.
     */
    public static void sort(List<TreeMapNode> nodes) {
        Comparator<TreeMapNode> c = new Comparator<TreeMapNode>() {
          @Override
          public int compare(TreeMapNode o1, TreeMapNode o2) {
              // inverting the result to descending sort the list
              return -(Double.compare(o1.getRealWeight(), o2.getRealWeight()));
          }
      };
      Collections.sort(nodes, c);
    }
    
    /**
     * Return the list of ancestors node of this object. The first one is this 
     * node itself, the last one the root.
     * @return a list of ancestors nodes.
     */
    public LinkedList<TreeMapNode> getAncestors() {
        LinkedList<TreeMapNode> toReturn = new LinkedList<TreeMapNode>();
        TreeMapNode tmp = this;
        do {
            toReturn.add(tmp);
        } while ((tmp = tmp.getParent()) != null);
        return toReturn;
    }
}
