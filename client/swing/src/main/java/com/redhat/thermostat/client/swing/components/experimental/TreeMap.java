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

import java.util.List;

/**
 * This class represents a treemap and facilitates its construction from information available in
 * the {@link S} dataset.
 *
 * @param <S> a dataset that aggregates elements of type {@link T}
 * @param <T> an element, holding some weight and labelled by some key (e.g. a pathname) that
 *           expresses a delimited set of (hierarchical) nodes
 */
public class TreeMap<S, T> {
    
    private final TreeMapNode root;
    
    public TreeMap(S data, NodeDataExtractor<S, T> extractor) {
        root = new TreeMapNode("", 0);
        buildTree(data, root, extractor);
        // calculates weights for inner nodes
        fillWeights(root);
        // collapse nodes with only one child
        packTree(root);
    }
    
    public TreeMapNode getRoot() {
        return root;
    }

    private static <S, T> void buildTree(S data, TreeMapNode root,
                                         NodeDataExtractor<S, T> extractor) {

        for (T element: extractor.getAsCollection(data)) {
            TreeMapNode lastProcessed = root;
            for (String node : extractor.getNodes(element)) {
                TreeMapNode child = searchNode(lastProcessed, node);

                if (child == null) {
                    child = new TreeMapNode(node, 0);
                    lastProcessed.addChild(child);
                }

                lastProcessed = child;
            }
            lastProcessed.setRealWeight(extractor.getWeight(element));
        }
    }
    
    /**
     * This method calculates the real weights using a bottom-up traversal.  The weight of a
     * parent node is the sum of its children's weights.
     *
     * Package-private for testing purposes.
     *
     * @param node the root of the subtree
     * @return the real weight of the root
     */
    static double fillWeights(TreeMapNode node) {
        if (node.getChildren().size() == 0) {
            return node.getRealWeight();
        }

        double sum = 0;
        for (TreeMapNode child : node.getChildren()) {
            sum += fillWeights(child);
        }
        node.setRealWeight(sum);
        return node.getRealWeight();
    }
    
    /**
     * This method allows the collapse of a series of nodes, each with at most one child, into a
     * single node placed at the root of the series.
     *
     * Package-private for testing purposes.
     *
     * @param node the root of the subtree
     */
    static void packTree(TreeMapNode node) {
        List<TreeMapNode> children = node.getChildren();
        if (children.size() == 1) {
            TreeMapNode child = children.get(0);
            node.setLabel(node.getLabel() + "." + child.getLabel());
            node.setChildren(child.getChildren());
            packTree(node);
        } else {
            for (TreeMapNode child : children) {
                packTree(child);
            }
        }
    }
    
    public static TreeMapNode searchNode(TreeMapNode startingPoint, String nodeId) {
        List<TreeMapNode> children = startingPoint.getChildren();
        for (TreeMapNode node : children) {
            if (node.getLabel().equals(nodeId)) {
                return node;
            }
        }
        return null;
    }
}
