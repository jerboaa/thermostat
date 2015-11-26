/*
 * Copyright 2012-2015 Red Hat, Inc.
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
import java.util.regex.Pattern;

public abstract class AbstractTreeAssembler<T> implements TreeAssembler<T> {

    @Override
    public abstract void buildTree(T data, TreeMapNode root);

    public static TreeMapNode processRecord(String name, String token, TreeMapNode lastProcessed) {
        while (!name.equals("")) {

            String nodeId = name.split(Pattern.quote(token))[0];

            TreeMapNode child = searchNode(lastProcessed.getChildren(), nodeId);
            if (child == null) {
                child = new TreeMapNode(nodeId, 0);
                lastProcessed.addChild(child);
            }

            lastProcessed = child;

            name = name.substring(nodeId.length());
            if (name.startsWith(".")) {
                name = name.substring(1);
            }
        }
        return lastProcessed;
    }

    public static TreeMapNode searchNode(List<TreeMapNode> nodes, String nodeId) {
        for (TreeMapNode node : nodes) {
            if (node.getLabel().equals(nodeId)) {
                return node;
            }
        }
        return null;
    }
}
