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

package com.redhat.thermostat.thread.client.swing.impl;

import com.redhat.thermostat.client.swing.NonEditableTableModel;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ThermostatTable;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import com.redhat.thermostat.thread.client.common.locale.LocaleResources;
import com.redhat.thermostat.thread.client.common.view.ThreadTableView;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class SwingThreadTableView extends ThreadTableView implements SwingComponent {

    private boolean tableRepacked = false; 
    
    private int currentSelection = -1;
    
    private ThermostatTable table;
    private ThreadTable tablePanel;

    private Map<ThreadTableBean, Integer> beans;

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    
    public SwingThreadTableView() {

        beans = new HashMap<>();
        tablePanel = new ThreadTable();
        new ComponentVisibilityNotifier().initialize(tablePanel, notifier);
        
        table = new ThermostatTable(new ThreadViewTableModel());
        table.setName("threadBeansTable");
        table.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                // NOTE: The fireTableDataChanged executes this listener
                // before the internal listener, this means this update will
                // be overridden since the default listener resets the model.
                // So, although we are in the EDT, we need to ensure that
                // we schedule this operation for later, rather than do it
                // right away... isn't Swing fun?
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (currentSelection != -1) {
                            table.setRowSelectionInterval(currentSelection, currentSelection);
                        }
                    }
                });
            }
        });
        tablePanel.add(table.wrap());
        
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ThreadViewTableModel model = (ThreadViewTableModel) table.getModel();
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow != -1) {
                        selectedRow = table.convertRowIndexToModel(selectedRow);
                        final ThreadTableBean bean = model.infos.get(selectedRow);
                        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                            protected Void doInBackground() throws Exception {
                                threadTableNotifier.fireAction(ThreadSelectionAction.SHOW_THREAD_DETAILS, bean);
                                return null;
                            }
                        };
                        worker.execute();
                    }
                }
            }
        });
    }

    @Override
    public void clear() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ThreadViewTableModel model = (ThreadViewTableModel) table.getModel();
                model.setRowCount(0);
                beans.clear();
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return tablePanel;
    }
    
    @Override
    public void display(final ThreadTableBean tableBean) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                
                // reset the selection for the next iteration
                // everything is happening in one thread, so there's no fear
                currentSelection = -1;
                
                ThreadViewTableModel model = (ThreadViewTableModel) table.getModel();
                int selectedRow = table.getSelectedRow();
                
                ThreadTableBean info = null;
                if (selectedRow != -1) {
                    info = model.infos.get(selectedRow);
                }

                // update the infos
                Integer beanIndex = beans.get(tableBean);
                if (beanIndex == null) {
                    beanIndex = Integer.valueOf(model.infos.size());
                    beans.put(tableBean, beanIndex);
                    model.infos.add(tableBean);
                }

                if (info != null) {
                    int index = 0;
                    for (ThreadTableBean inModel : model.infos) {
                        if (info.equals(inModel)) {
                            currentSelection = index;
                            break;
                        }
                        index++;
                    }
                }
                
                // just repack once, or the user will see the table moving around
                if (!tableRepacked) {
                    table.repackCells();
                    tableRepacked = true;
                }
            }
        });
    }

    @Override
    public void submitChanges() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ThreadViewTableModel model =
                        (ThreadViewTableModel) table.getModel();
                model.fireTableDataChanged();
            }
        });
    }

    @SuppressWarnings("serial")
    private class ThreadViewTableModel extends NonEditableTableModel {

        private String [] columns = {
                t.localize(LocaleResources.NAME).getContents(),
                t.localize(LocaleResources.ID).getContents(),
                t.localize(LocaleResources.FIRST_SEEN).getContents(),
                t.localize(LocaleResources.LAST_SEEN).getContents(),
                t.localize(LocaleResources.WAIT_COUNT).getContents(),
                t.localize(LocaleResources.BLOCK_COUNT).getContents(),
                t.localize(LocaleResources.RUNNING).getContents(),
                t.localize(LocaleResources.WAITING).getContents(),
                t.localize(LocaleResources.SLEEPING).getContents(),
                t.localize(LocaleResources.MONITOR).getContents(), //, "Heap", "CPU Time", "User CPU Time"
        };

        private List<ThreadTableBean> infos;
        public ThreadViewTableModel() {
            this.infos = new ArrayList<>();
        }
    
        @Override
        public String getColumnName(int column) {
            return columns[column];
        }
        
        @Override
        public int getColumnCount() {
            return columns.length;
        }
        
        @Override
        public int getRowCount() {
            if (infos == null) {
                return 0;
            }
            return infos.size();
        }
        
        @Override
        public Class<?> getColumnClass(int column) {
            switch (column) {
            case 0:
            case 2:
            case 3:
            case 6:
            case 7:
            case 8:
            case 9:
                return String.class;
            default:
                return Long.class;
            }
        }
        
        @Override
        public Object getValueAt(int row, int column) {

            DecimalFormat format = new DecimalFormat("###.00");

            Object result = null;
            
            ThreadTableBean info = infos.get(row);
            switch (column) {
            case 0:
                result = info.getName();
                break;
            case 1:
                result = info.getId();
                break;
            case 2:
                result = new Date(info.getStartTimeStamp()).toString();
                break;
            case 3:
                if (info.getStopTimeStamp() != 0) {
                    result = new Date(info.getStopTimeStamp()).toString();
                } else {
                    result = "-";
                }
                break;
            case 4:
                result = info.getWaitedCount();
                break;
            case 5:
                result = info.getBlockedCount();
                break;
            case 6:
                result = format.format(info.getRunningPercent());
                break;
            case 7:
                result = format.format(info.getWaitingPercent());
                break;
            case 8:
                result = format.format(info.getSleepingPercent());
                break;
            case 9:
                result = format.format(info.getMonitorPercent());
                break;
             default:
                 result = "n/a";
                 break;
            }
            return result;
        }
    }
}

