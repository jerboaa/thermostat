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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ThermostatTabbedPane;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpDetailsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapTreeMapView;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsView;
import com.redhat.thermostat.vm.heap.analysis.client.locale.LocaleResources;

public class HeapDetailsSwing extends HeapDumpDetailsView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    /** For TESTING only! */
    static final String TAB_NAME = "tabs";

    private JPanel visiblePane;
    private JPanel labelPane;

    private JTabbedPane tabPane = new ThermostatTabbedPane();

    private JLabel dumpLabel;

    public HeapDetailsSwing() {
        labelPane = new JPanel();
        dumpLabel = new JLabel();
        labelPane.add(dumpLabel);
        visiblePane = new JPanel();
        visiblePane.setLayout(new BorderLayout());
        visiblePane.add(tabPane, BorderLayout.CENTER);
        visiblePane.add(labelPane, BorderLayout.NORTH);

        tabPane.setName(TAB_NAME);
        tabPane.addTab(
                translator.localize(LocaleResources.HEAP_DUMP_SECTION_TREEMAP).getContents(), null);
        tabPane.addTab(
                translator.localize(LocaleResources.HEAP_DUMP_SECTION_OBJECT_BROWSER).getContents(), null);
        tabPane.addTab(
                translator.localize(LocaleResources.HEAP_DUMP_SECTION_HISTOGRAM).getContents(), null);
        tabPane.setSelectedIndex(0);
    }

    public void setDumpName(LocalizedString heapDump) {
        dumpLabel.setText(heapDump.getContents());
        visiblePane.revalidate();
        visiblePane.repaint();
    }

    protected JLabel getLabel() {
        return dumpLabel;
    }

    @Override
    public void addSubView(final LocalizedString title, final HeapHistogramView view) {
        verifyIsSwingComponent(view);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tabPane.insertTab(title.getContents(), null, ((SwingComponent)view).getUiComponent(), null, 0);
            }
        });
    }

    @Override
    public void addSubView(final LocalizedString title, final ObjectDetailsView view) {
        verifyIsSwingComponent(view);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tabPane.insertTab(title.getContents(), null, ((SwingComponent)view).getUiComponent(), null, 1);
            }
        });
    }

    @Override
    public void removeSubView(final LocalizedString title) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int tabCount = tabPane.getTabCount();
                for (int i = 0; i < tabCount; i++) {
                    String tabTitle = tabPane.getTitleAt(i);
                    if (tabTitle.equals(title.getContents())) {
                        tabPane.removeTabAt(i);
                        return;
                    }
                }
            }
        });
    }

    @Override
    public JPanel getUiComponent() {
        return visiblePane;
    }

    private void verifyIsSwingComponent(Object obj) {
        if (!(obj instanceof SwingComponent)) {
            throw new IllegalArgumentException("component is not swing");
        }
    }

    @Override
    public void addSubView(final LocalizedString title, final HeapTreeMapView view) {
        verifyIsSwingComponent(view);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tabPane.insertTab(title.getContents(), null, ((SwingComponent)view).getUiComponent(), null, 0);
            }
        });
    }

    @Override
    public void updateView(final HeapHistogramView histogramView,
                           final ObjectDetailsView detailsView,
                           final HeapTreeMapView treeMapView) {

        verifyIsSwingComponent(histogramView);
        verifyIsSwingComponent(detailsView);
        verifyIsSwingComponent(treeMapView);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tabPane.setComponentAt(0, ((SwingComponent) treeMapView).getUiComponent());
                tabPane.setComponentAt(1, ((SwingComponent) detailsView).getUiComponent());
                tabPane.setComponentAt(2, ((SwingComponent) histogramView).getUiComponent());
                tabPane.revalidate();
                tabPane.repaint();
            }
        });
    }

}
