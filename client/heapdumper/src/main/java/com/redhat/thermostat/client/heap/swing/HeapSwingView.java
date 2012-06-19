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

package com.redhat.thermostat.client.heap.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.redhat.thermostat.client.heap.HeapDump;
import com.redhat.thermostat.client.heap.HeapView;
import com.redhat.thermostat.client.heap.chart.OverviewChart;
import com.redhat.thermostat.client.ui.ComponentVisibleListener;

public class HeapSwingView extends HeapView<JComponent> {

    private boolean heapDetailIsShowing;
    
    private StatsPanel stats;

    private HeapPanel heapDetailPanel;
    private HeaderPanel overview;
    
    private JPanel visiblePane;
    
    public HeapSwingView() {
        
        stats = new StatsPanel();
        stats.addHeapDumperListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                stats.disableHeapDumperControl();
                
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        heapDumperNotifier.fireAction(HeadDumperAction.DUMP_REQUESTED);
                        return null;
                    }
                    
                    @Override
                    protected void done() {
                        stats.enableHeapDumperControl();
                    }
                };
                worker.execute();
            }
        });
        
        stats.addDumpListListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { 
                    HeapDump dump = stats.getSelectedHeapDump();
                    heapDumperNotifier.fireAction(HeadDumperAction.ANALYSE, dump);
                }
            }
        });
        
        visiblePane = new JPanel();
        visiblePane.setLayout(new BoxLayout(visiblePane, BoxLayout.X_AXIS));
        
        heapDetailPanel = new HeapPanel();
        
        overview = new HeaderPanel("Heap Usage Overview");
        overview.setContent(stats);
        overview.addHierarchyListener(new ViewVisibleListener());

        // at the beginning, only the overview is visible
        visiblePane.add(overview);
    }
    
    @Override
    public JComponent getComponent() {
        return visiblePane;
    }
    
    private class ViewVisibleListener extends ComponentVisibleListener {
        @Override
        public void componentShown(Component component) {
            HeapSwingView.this.notify(Action.VISIBLE);
        }

        @Override
        public void componentHidden(Component component) {
            HeapSwingView.this.notify(Action.HIDDEN);
        }
    }

    @Override
    public void updateOverview(final OverviewChart model, final String used, final String capacity) {
        
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {

                ChartPanel charts = new ChartPanel(model);
                stats.setChartPanel(charts);
                
                stats.setMax(capacity);
                stats.setUsed(used);
            }
        });
    }

    @Override
    public void addHeapDump(final HeapDump dump) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stats.addDump(dump);
            }
        });
    }
    
    @Override
    public void clearHeapDumpList() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stats.clearDumpList();
            }
        });
    }
    
    @Override
    public void openDumpView(final HeapDump dump) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!heapDetailIsShowing) {
                    visiblePane.removeAll();
                    
                    heapDetailIsShowing = true;
                    
                    heapDetailPanel.divideView();
                    heapDetailPanel.setTop(overview);
                    
                    String[] columnNames = {"First Name",
                            "Last Name",
                            "Sport",
                            "# of Years",
                            "Vegetarian"};
                    Object[][] data = {
                            {"Kathy", "Smith",
                             "Snowboarding", new Integer(5), new Boolean(false)},
                            {"John", "Doe",
                             "Rowing", new Integer(3), new Boolean(true)},
                            {"Sue", "Black",
                             "Knitting", new Integer(2), new Boolean(false)},
                            {"Jane", "White",
                             "Speed reading", new Integer(20), new Boolean(true)},
                            {"Joe", "Brown",
                             "Pool", new Integer(10), new Boolean(false)}
                        };
                    JTable table = new JTable(data, columnNames);
                    JPanel bottom = new JPanel();
                    bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
                    bottom.add(table);
                    heapDetailPanel.setBottom(bottom);
                    
                    visiblePane.add(heapDetailPanel);
                    
                    visiblePane.revalidate();                    
                }
            }
        });
    }
}
