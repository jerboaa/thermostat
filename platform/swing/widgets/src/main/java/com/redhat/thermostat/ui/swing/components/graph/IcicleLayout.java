/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.ui.swing.components.graph;

import com.redhat.thermostat.collections.graph.Node;
import com.redhat.thermostat.common.model.LongRangeNormalizer;
import com.redhat.thermostat.common.model.Range;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.redhat.thermostat.ui.swing.components.graph.GraphContainer.PREFERRED_HEIGHT;
import static com.redhat.thermostat.ui.swing.model.graph.GraphModel.SAMPLE_COUNT_PROPERTY;

class IcicleLayout extends GraphLayout {

    private static final boolean PRINT_LOOP_STATS = false;
    private boolean invertedIcicle;

    public IcicleLayout() {
        invertedIcicle = false;
    }

    public IcicleLayout(boolean invertedIcicle) {
        this.invertedIcicle = invertedIcicle;
    }

    public void setInvertedIcicle(boolean invertedIcicle) {
        this.invertedIcicle = invertedIcicle;
    }

    public boolean isInvertedIcicle() {
        return invertedIcicle;
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        GraphContainer graphContainer = (GraphContainer) parent;
        Dimension size = graphContainer.getParent().getSize();

        if (!graphContainer.getAdjacencyTree().isEmpty()) {
            int prefHeight = graphContainer.getAdjacencyTree().size() * PREFERRED_HEIGHT;
            if (prefHeight > size.height) {
                size.height = prefHeight;
            }
        }

        return size;
    }

    @Override
    protected void doLayout(GraphContainer graphContainer) {

        long startTime = 0l;
        if (PRINT_LOOP_STATS) {
            startTime = System.nanoTime()/1_000_000;
        }

        final Node root = graphContainer.getRoot();

        long sampleCount = root.getProperty(SAMPLE_COUNT_PROPERTY);
        Range<Long> range = new Range<>(0l, sampleCount);
        final LongRangeNormalizer normalizer = new LongRangeNormalizer(range, 0, graphContainer.getWidth());

        Map<Node, Node> parents = graphContainer.getParents();
        List<List<Node>> adjacencyTree = graphContainer.getAdjacencyTree();

        final Map<Node, Integer> boundsCache = new HashMap<>();

        int totalHeight = graphContainer.getHeight();

        int level = 0;
        for (List<Node> adjacent : adjacencyTree) {
            for (Node node : adjacent) {

                long samples = node.getProperty(SAMPLE_COUNT_PROPERTY);

                int x = 0;
                int y = (level * PREFERRED_HEIGHT) + 1;
                int w = (int) normalizer.getValueNormalized(samples);
                int h = PREFERRED_HEIGHT;

                if (invertedIcicle) {
                    y = totalHeight - y - h + 1;
                }

                Tile component = node.getProperty(GraphContainer.COMPONENT_PROPERTY);
                Node parent = parents.get(node);
                if (parent != null) {
                    if (boundsCache.containsKey(parent)) {
                        x = boundsCache.get(parent);
                    }
                }

                Rectangle bounds = new Rectangle(x, y, w, h);
                component.setBounds(bounds);
                if (parent != null) {
                    boundsCache.put(parent, (x + w));
                }
                // record where we started
                boundsCache.put(node, x);
            }
            level++;
        }
        if (PRINT_LOOP_STATS) {
            long stopTime = System.nanoTime()/1_000_000;
            long totalTime = (stopTime - startTime);
            System.err.println("loop time: " + totalTime + "ms");
        }
    }
}
