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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.redhat.thermostat.vm.heap.analysis.client.core.HeapView.DumpDisabledReason;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;

@SuppressWarnings("serial")
public class StatsPanel extends JPanel {

    private HeapChartPanel heapPanel;
    private HeapDumperPopup popup;
    
    private ExportDumpPopup export;
    private List<OverlayComponent> overlays;

    private boolean canDump;
    
    private HeapDump selectedDump;
    
    public StatsPanel() {
        
        setName(StatsPanel.class.getName());
        
        canDump = true;
        
        overlays = new ArrayList<>();
        
        popup = new HeapDumperPopup();
        
        export = new ExportDumpPopup();
        export.addExportListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedDump != null) {
                    fireExportDumpEvent(selectedDump);
                }
            }
        });
        
        setLayout(new BorderLayout());
    }
    
    public void setChartPanel(HeapChartPanel panel) {
        if (heapPanel != null) {
            remove(heapPanel);
        }
        
        heapPanel = panel;
        for (OverlayComponent overlay : overlays) {
            heapPanel.add(overlay);
        }
        
        heapPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e)  {
                checkPopup(e);
            }  

            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }  

            private void checkPopup(MouseEvent e) {
                if (canDump && e.isPopupTrigger()) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        
        add(heapPanel, BorderLayout.CENTER);
    }

    public void setMax(String capacity) {
        //max.setText(capacity);
    }

    public void setUsed(String used) {
        //current.setText(used);
    }
    
    public void addHeapDumperListener(ActionListener listener) {
        popup.addDumperListener(listener);
    }

    public void addDumpListListener(HeapDumpListener listener) {
        listenerList.add(HeapDumpListener.class, listener);
    }

    public void addExportDumpListener(ExportDumpListener listener) {
        listenerList.add(ExportDumpListener.class, listener);
    }
    
    public void disableHeapDumperControl(DumpDisabledReason reason) {
        canDump = false;
    }

    public void enableHeapDumperControl() {
        canDump = true;
    }

    private void fireExportDumpEvent(HeapDump source) {
        Object[] listeners = listenerList.getListenerList();

        ExportDumpEvent event = new ExportDumpEvent(source);
        
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ExportDumpListener.class) {
                ((ExportDumpListener) listeners[i + 1]).actionPerformed(event);
            }
        }
    }
    
    private void fireHeapDumpClicked(OverlayComponent component) {
        Object[] listeners = listenerList.getListenerList();

        HeapSelectionEvent event = new HeapSelectionEvent(component);
        
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == HeapDumpListener.class) {
                ((HeapDumpListener) listeners[i + 1]).actionPerformed(event);
            }
        }
    }
    
    public void addDump(final HeapDump dump) {
        OverlayComponent dumpOverlay = new OverlayComponent(dump);
        if (!overlays.contains(dumpOverlay)) {
            dumpOverlay.setName(String.valueOf(dump.getTimestamp()));
            overlays.add(dumpOverlay);
            dumpOverlay.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() > 1) {
                        OverlayComponent sourceOverlay = (OverlayComponent) e.getSource();
                        for (OverlayComponent overlay : overlays) {
                            overlay.setSelected(false);
                        }
                        sourceOverlay.setSelected(true);
                        fireHeapDumpClicked(sourceOverlay);
                    }
                }
                
                public void mousePressed(MouseEvent e)  {
                    checkPopup(e);
                }  

                public void mouseReleased(MouseEvent e) {
                    checkPopup(e);
                }
                
                private void checkPopup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        selectedDump = dump;
                        export.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
            
            if (heapPanel != null) {
                heapPanel.add(dumpOverlay);
            }
        }    
    }

    public void clearDumpList() {
        if (heapPanel != null) {
            for (OverlayComponent overlay : overlays) {
                heapPanel.remove(overlay);
            }
        }
        overlays.clear();
    }
    
    public void selectOverlay(HeapDump heapDump) {
        OverlayComponent dumpOverlay = new OverlayComponent(heapDump);
        
        for (OverlayComponent overlay : overlays) {
            if (overlay.equals(dumpOverlay)) {
                overlay.setSelected(true);
            } else {
                overlay.setSelected(false);
            }
        }
    }

    public void updateHeapDumpList(List<HeapDump> heapDumps) {
        for (HeapDump heapDump : heapDumps) {
            addDump(heapDump);
        }
    }
}

