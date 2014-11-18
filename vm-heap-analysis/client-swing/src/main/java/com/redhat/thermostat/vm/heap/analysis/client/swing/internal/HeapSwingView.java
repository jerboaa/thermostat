/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.swing.IconResource;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ActionButton;
import com.redhat.thermostat.client.swing.components.ActionToggleButton;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.OverlayPanel;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpListView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapIconResources;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapView;
import com.redhat.thermostat.vm.heap.analysis.client.core.chart.OverviewChart;
import com.redhat.thermostat.vm.heap.analysis.client.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats.ExportDumpEvent;
import com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats.ExportDumpListener;
import com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats.HeapChartPanel;
import com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats.HeapDumpListener;
import com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats.HeapSelectionEvent;
import com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats.StatsPanel;
import com.redhat.thermostat.vm.heap.analysis.common.DumpFile;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;

public class HeapSwingView extends HeapView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private StatsPanel stats;

    private HeapPanel heapDetailPanel;
    private HeaderPanel overview;
    
    private JPanel visiblePane;
    private OverlayPanel overlay;
    
    private ActionToggleButton showHeapListButton;
    private ActionButton takeDumpIconButton;
    
    private JPanel stack;
    
    private JFileChooser fileChooser;

    public HeapSwingView() {
        stats = new StatsPanel();
        stats.addHeapDumperListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                heapDumperNotifier.fireAction(HeapDumperAction.DUMP_REQUESTED);
            }
        });

        stats.addDumpListListener(new HeapDumpListener() {
            @Override
            public void actionPerformed(HeapSelectionEvent e) {
                HeapDump dump = e.getSource().getHeapDump();
                heapDumperNotifier.fireAction(HeapDumperAction.ANALYSE, dump);
            }
        });
        
        stats.addExportDumpListener(new ExportDumpListener() {
            @Override
            public void actionPerformed(ExportDumpEvent e) {
                HeapDump dump = e.getSource();
                heapDumperNotifier.fireAction(HeapDumperAction.REQUEST_EXPORT, dump);
            }
        });
        
        visiblePane = new JPanel();
        visiblePane.setLayout(new BoxLayout(visiblePane, BoxLayout.X_AXIS));
        visiblePane.setName(HeapSwingView.class.getName());
        
        heapDetailPanel = new HeapPanel();
        
        overview = new HeaderPanel(translator.localize(LocaleResources.HEAP_OVERVIEW_TITLE));

        stack = new JPanel();
        stack.setLayout(new OverlayLayout(stack));
        
        overlay = new OverlayPanel(translator.localize(LocaleResources.DUMPS_LIST));
        stack.add(overlay);
        stack.add(stats);
        stats.setOpaque(false);
        
        overlay.setAlignmentX(-1.f);
        overlay.setAlignmentY(1.f);

        overview.setContent(stack);
        new ComponentVisibilityNotifier().initialize(overview, notifier);

        Icon takeDumpIcon = new Icon(HeapIconResources.getIcon(HeapIconResources.TRIGGER_HEAP_DUMP));
        takeDumpIconButton = new ActionButton(takeDumpIcon, translator.localize(LocaleResources.TRIGGER_HEAP_DUMP));
        takeDumpIconButton.setToolTipText(translator.localize(LocaleResources.TRIGGER_HEAP_DUMP).getContents());
        takeDumpIconButton.setName("TRIGGER_HEAP_DUMP");
        takeDumpIconButton.getToolbarButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                heapDumperNotifier.fireAction(HeapDumperAction.DUMP_REQUESTED);
            }
        });
        overview.addToolBarButton(takeDumpIconButton);
                
        Icon listDumpIcon = IconResource.HISTORY.getIcon();
        showHeapListButton = new ActionToggleButton(listDumpIcon, translator.localize(LocaleResources.LIST_DUMPS_ACTION));
        showHeapListButton.setToolTipText(translator.localize(LocaleResources.LIST_DUMPS_ACTION).getContents());
        showHeapListButton.setName("LIST_DUMPS_ACTION");

        showHeapListButton.getToolbarButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (overlay.isVisible()) {
                    overlay.setOverlayVisible(false);
                } else {
                    heapDumperNotifier.fireAction(HeapDumperAction.REQUEST_DISPLAY_DUMP_LIST);
                }
            }
        });
        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        @SuppressWarnings("serial")
        javax.swing.Action closeOverlay = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeDumpListView();
            }
        };
        overlay.getActionMap().put("close", closeOverlay);
        overlay.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, "close");
        
        overview.addToolBarButton(showHeapListButton);
        
        // at the beginning, only the overview is visible
        visiblePane.add(overview);
        
        fileChooser = new JFileChooser();
        fileChooser.setName("EXPORT_HEAP_DUMP_FILE_CHOOSER");
    }

    @Override
    public void setModel(final OverviewChart model) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                // TODO, move to controller
                model.createChart(stats.getWidth(), stats.getBackground());
                
                final HeapChartPanel charts = new HeapChartPanel(model.getChart());
                
                /*
                 * By default, ChartPanel scales itself instead of redrawing things when
                 * it's re-sized. To have it resize automatically, we need to set minimum
                 * and maximum sizes. Lets constrain the minimum, but not the maximum
                 * size.
                 */
                final int MINIMUM_DRAW_SIZE = 100;

                charts.setMinimumDrawHeight(MINIMUM_DRAW_SIZE);
                charts.setMinimumDrawWidth(MINIMUM_DRAW_SIZE);

                charts.setMaximumDrawHeight(Integer.MAX_VALUE);
                charts.setMaximumDrawWidth(Integer.MAX_VALUE);
                
                charts.setOpaque(false);
                
                stats.setChartPanel(charts);
            }
        });
    }

    @Override
    public void updateUsedAndCapacity(final String used, final String capacity) {
        
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                stats.setMax(capacity);
                stats.setUsed(used);
            }
        });
    }

    @Override
    public void enableHeapDumping() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stats.enableHeapDumperControl();
            }
        });
    }

    @Override
    public void disableHeapDumping(final DumpDisabledReason reason) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stats.disableHeapDumperControl(reason);
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
    public void openDumpView() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                visiblePane.removeAll();
                heapDetailPanel.divideView();
                heapDetailPanel.setTop(overview);

                visiblePane.add(heapDetailPanel);
                visiblePane.revalidate();
            }
        });
    }

    @Override
    public void setChildView(BasicView childView) {
        if (childView instanceof HeapDetailsSwing) {
            final HeapDetailsSwing view = (HeapDetailsSwing)childView;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    heapDetailPanel.setBottom(view.getUiComponent());
                    visiblePane.revalidate();
                }
            });
        }
    }

    @Override
    public void setActiveDump(final HeapDump dump) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stats.selectOverlay(dump);
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return visiblePane;
    }

    @Override
    public void notifyHeapDumpComplete() {
        enableHeapDumping();
    }

    @Override
    public void updateHeapDumpList(final List<HeapDump> heapDumps) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                stats.updateHeapDumpList(heapDumps);
            }
        });
    }

    @Override
    public void displayWarning(LocalizedString string) {
        JOptionPane.showMessageDialog(visiblePane, string.getContents(), "Warning", JOptionPane.WARNING_MESSAGE);
    }
    
    private void closeDumpListView() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (overview.isVisible()) {
                    showHeapListButton.getToolbarButton().doClick();
                }
            }
        });
    }
    
    @Override
    public void openDumpListView(HeapDumpListView view) {
        if (view instanceof SwingHeapDumpListView) {
            SwingHeapDumpListView swingView = (SwingHeapDumpListView) view;
            view.addListListener(new com.redhat.thermostat.common.ActionListener<HeapDumpListView.ListAction>() {
                @Override
                public void actionPerformed(com.redhat.thermostat.common.ActionEvent<HeapDumpListView.ListAction> actionEvent) {
                    switch (actionEvent.getActionId()) {
                        case OPEN_DUMP_DETAILS:
                            closeDumpListView();
                            break;

                        default:
                            break;
                    }
                }
            });
            
            final Component dumpListView = swingView.getUiComponent();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    overlay.removeAll();
                    overlay.add(dumpListView);                    
                    overlay.setOverlayVisible(true);
                }
            });
        }
    }
    
    @Override
    public void openExportDialog(final DumpFile heapDump) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fileChooser.setSelectedFile(heapDump.getFile());
                int result = fileChooser.showSaveDialog(HeapSwingView.this.getUiComponent());
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    heapDump.setFile(file);
                    heapDumperNotifier.fireAction(HeapDumperAction.SAVE_HEAP_DUMP, heapDump);
                }
            }
        });
    }
}

