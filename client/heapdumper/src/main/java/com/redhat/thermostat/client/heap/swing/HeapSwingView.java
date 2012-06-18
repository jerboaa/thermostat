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

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.redhat.thermostat.client.heap.HeapDump;
import com.redhat.thermostat.client.heap.HeapView;
import com.redhat.thermostat.client.heap.chart.OverviewChart;
import com.redhat.thermostat.client.ui.ComponentVisibleListener;

public class HeapSwingView extends HeapView<JComponent> {
    
    private StatsPanel stats;

    private HeapPanel panel;
    private HeaderPanel overview;
    
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
        
        panel = new HeapPanel();
        
        overview = new HeaderPanel("Heap Usage Overview");
        overview.setContent(stats);
        overview.addHierarchyListener(new ViewVisibleListener());

        panel.setTop(overview);        
    }
    
    @Override
    public JComponent getComponent() {
        return overview;
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
}
