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

package com.redhat.thermostat.storage.internal.statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.redhat.thermostat.storage.internal.statement.BinaryExpressionNode;
import com.redhat.thermostat.storage.internal.statement.Node;
import com.redhat.thermostat.storage.internal.statement.NotBooleanExpressionNode;
import com.redhat.thermostat.storage.internal.statement.TerminalNode;
import com.redhat.thermostat.storage.internal.statement.WhereExpression;

/**
 * Helper class for comparing where expression trees.
 *
 * @see BasicDescriptorParserTest
 */
public class WhereExpressions {

    /**
     * Compares two where expression parse trees.
     * 
     * @param a
     * @param b
     * @return true, if all nodes/values were equal. false otherwise.
     */
    public static boolean equals(WhereExpression a, WhereExpression b) {
        Node aRoot = a.getRoot();
        Node bRoot = b.getRoot();
        return recEquals(aRoot, bRoot);
    }
    
    private static boolean recEquals(Node a, Node b) {
        boolean retval = Objects.equals(a, b);
        if (!retval) {
            return false;
        }
        Node[] nextNodesA = getNextNodes(a);
        Node[] nextNodesB = getNextNodes(b);
        if (nextNodesA != null && nextNodesB != null) {
            retval = retval && (nextNodesA.length == nextNodesB.length);
            if (!retval) {
                return false;
            }
            // verify nodes at this level are equal
            for (int i = 0; i < nextNodesA.length; i++) {
                retval = retval && Objects.equals(nextNodesA[i], nextNodesB[i]);
                if (!retval) {
                    return false;
                }
            }
            // recursively check child nodes
            for (int i = 0; i < nextNodesA.length; i++) {
                retval = retval && recEquals(nextNodesA[i], nextNodesB[i]);
                if (!retval) {
                    return false;
                }
            }
            return retval;
        }
        if (nextNodesA != null || nextNodesB != null) {
            return false;
        }
        // all pass
        return true;
    }

    private static Node[] getNextNodes(Node node) {
        if (node instanceof TerminalNode) {
            return null;
        } else if (node instanceof BinaryExpressionNode) {
            List<Node> nodes = new ArrayList<>(2);
            BinaryExpressionNode binNode = (BinaryExpressionNode)node;
            if (binNode.getLeftChild() != null) {
                nodes.add(binNode.getLeftChild());
            }
            if (binNode.getRightChild() != null) {
                nodes.add(binNode.getRightChild());
            }
            if (nodes.size() == 0) {
                return null;
            }
            return nodes.toArray(new Node[0]);
        } else if (node instanceof NotBooleanExpressionNode || node instanceof Node) {
            Node next = (Node)node.getValue();
            if (next == null) {
                return null;
            }
            return new Node[] { next };
        } else {
            return null;
        }
    }
}

