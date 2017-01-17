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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ActionButton;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.ShadowLabel;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.client.swing.components.ThermostatThinScrollBar;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpListView;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;

public class SwingHeapDumpListView extends HeapDumpListView implements SwingComponent {

    private JPanel container;
    private JScrollPane scrollPane;
    private HeapDumpPanel table;

    public SwingHeapDumpListView() {
        container = new JPanel();
        container.setLayout(new BorderLayout());
        container.setOpaque(false);

        table = new HeapDumpPanel();
        BoxLayout layout = new BoxLayout(table, BoxLayout.Y_AXIS);
        table.setLayout(layout);
        table.setBorder(new EmptyBorder(Constants.THIN_INSETS));
        table.setOpaque(false);

        scrollPane = new ThermostatScrollPane(table);
        scrollPane.setVerticalScrollBar(new ThermostatThinScrollBar(ThermostatThinScrollBar.VERTICAL));

        container.add(scrollPane, BorderLayout.CENTER);

        JPanel invisibleFixture = new JPanel();
        invisibleFixture.setOpaque(false);
        invisibleFixture.setMaximumSize(Constants.MINIMUM_SIZE);

        container.add(invisibleFixture, BorderLayout.SOUTH);
    }

    @Override
    public Component getUiComponent() {
        return container;
    }
    
    @Override
    public void setDumps(List<HeapDump> dumps) {
        setDumps(dumps, null);
    }
    
    // package-private for testing
    void setDumps(final List<HeapDump> dumps, final Runnable callback) {
        final List<HeapDump> dumpsSortedByTimestamp = new ArrayList<>(dumps);
        Collections.sort(dumpsSortedByTimestamp, new DumpsComparator());
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                table.setName("_heapdump_table_list");
                table.clear();
                for (final HeapDump  dump : dumpsSortedByTimestamp) {
                    final HeapDumpItem item = new HeapDumpItem(dump);
                    table.add(item);
                }
                container.revalidate();
                container.repaint();
                if (callback != null) {
                    callback.run();
                }
            }
        });
    }

    private class DumpsComparator implements Comparator<HeapDump> {
        @Override
        public int compare(HeapDump o1, HeapDump o2) {
            // TODO: descending order only for now, we should allow the users
            // to sort this via the UI though
            int result = Long.compare(o1.getTimestamp(), o2.getTimestamp());
            return -result;
        }
    }

    private class HeapDumpPanel extends JPanel implements Scrollable {
        private List<HeapDumpItem> heapdumpList = new ArrayList<>();

        public HeapDumpPanel() {
            setDoubleBuffered(true);
        }

        public void heapDumpItemMouseOver(final HeapDumpItem item) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (HeapDumpItem i : heapdumpList) {
                        i.toggleDisplayButtonOff();
                    }
                    item.toggleDisplayButtonOn();
                }
            });
        }


        @Override
        public java.awt.Component add(java.awt.Component comp) {
            super.add(comp);
            heapdumpList.add((HeapDumpItem) comp);
            return comp;
        }

        public void clear() {
            for (HeapDumpItem item : heapdumpList) {
                super.remove(item);
            }
            heapdumpList.clear();
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return super.getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            //Unit scroll 1/10th of the panel's height.
            //Used when scrolling with scrollbar arrows.
            return (int)(this.getPreferredSize().getHeight() / 10);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            //Block scroll 1/5th of the panel's height.
            //Used when scrolling with mouse-wheel clicks.
            return (int)(this.getPreferredSize().getHeight() / 5);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

    }

    private class HeapDumpItem extends JPanel {
        private HeapDump heapDump;
        private JButton button;

        public HeapDumpItem(final HeapDump heapDump) {
            super();

            this.setName(heapDump.toString() + "_panel");
            this.heapDump = heapDump;

            ShadowLabel label = new ShadowLabel(new LocalizedString(heapDump.toString()));
            label.setForeground(Palette.ROYAL_BLUE.getColor());
            label.setName(heapDump.toString() + "_label");

            char iconId = '\uF019';
            Icon icon = new FontAwesomeIcon(iconId, (int) label.getMinimumSize().getHeight());
            button = new ActionButton(icon);

            button.setToolTipText("Export heap dump.");
            button.setName(heapDump.toString() + "_button");
            button.setBackground(null);

            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    listNotifier.fireAction(ListAction.EXPORT_DUMP, HeapDumpItem.this.heapDump);
                }
            });

            button.setVisible(false);

            GridBagLayout gbl = new GridBagLayout();
            setLayout(gbl);

            GridBagConstraints gbc = new GridBagConstraints();

            gbc.gridy = 0;
            gbc.gridx = GridBagConstraints.RELATIVE;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weighty = 1;

            gbc.weightx = 1;
            add(label, gbc);

            gbc.weightx = 0;
            add(button, gbc);

            setOpaque(false);
            setBorder(new EmptyBorder(Constants.THIN_INSETS));
            setPreferredSize(label.getPreferredSize());

            this.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    if (evt.getClickCount() == 2) {
                        listNotifier.fireAction(ListAction.OPEN_DUMP_DETAILS, HeapDumpItem.this.heapDump);
                    }

                }

                public void mouseEntered(MouseEvent evt) {
                    table.heapDumpItemMouseOver(HeapDumpItem.this);
                }
            });
        }

        private void toggleDisplayButtonOn() {
            this.setBackground(Palette.ELEGANT_CYAN.getColor());
            setOpaque(true);
            button.setVisible(true);
        }

        private void toggleDisplayButtonOff() {
            this.setBackground(UIManager.getColor("Panel.background"));
            setOpaque(false);
            button.setVisible(false);
        }

        @Override
        public Dimension getMaximumSize() {
            int width = (int) super.getMaximumSize().getWidth();
            return new Dimension(width, (int) this.getPreferredSize().getHeight());
        }
    }
}

