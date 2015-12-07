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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.experimental.TreeMap;
import com.redhat.thermostat.client.swing.components.experimental.TreeMapComponent;
import com.redhat.thermostat.client.swing.components.experimental.TreeMapNode;
import com.redhat.thermostat.client.swing.components.experimental.TreeMapToolbar;
import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapTreeMapView;
import com.redhat.thermostat.vm.heap.analysis.common.HistogramRecord;
import com.redhat.thermostat.vm.heap.analysis.common.ObjectHistogram;

public class SwingHeapTreeMapView extends HeapTreeMapView implements SwingComponent {
    
    private TreeMapComponent treeMap;
    private JPanel panel;

    public SwingHeapTreeMapView() {
        treeMap = new TreeMapComponent();
        treeMap.setToolTipRenderer(new WeightAsSizeRenderer());

        panel = new JPanel();
        panel.setLayout(new BorderLayout());
    }

    @Override
    public void display(final ObjectHistogram histogram) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TreeMap<ObjectHistogram, HistogramRecord> map = new TreeMap<>(histogram, new ObjectHistogramNodeDataExtractor());
                TreeMapNode model = map.getRoot();
                treeMap.setModel(model);
                panel.removeAll();
                panel.add(treeMap, BorderLayout.CENTER);
                panel.add(new TreeMapToolbar(treeMap), BorderLayout.NORTH);
                panel.revalidate();
                panel.repaint();
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return panel;
    }

    public static class WeightAsSizeRenderer implements TreeMapComponent.ToolTipRenderer {
        @Override
        public String render(TreeMapNode node) {
            Size size = new Size(node.getRealWeight(), Size.Unit.B);
            return node.getLabel() + " - " + size.toString("%.2f");
        }
    }
}
