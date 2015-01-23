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

package com.redhat.thermostat.thread.client.swing.impl;

import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.thread.client.common.model.timeline.ThreadInfo;
import com.redhat.thermostat.thread.client.common.model.timeline.TimelineProbe;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;
import com.redhat.thermostat.thread.client.swing.experimental.components.ContentPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Same as demo 2, except that it insert 10 minutes worth of timelines.
 * Startup time is also configurable.
 */
public class SwingThreadViewDemo3 {

    private static final int INTERVAL = 100; // ms
    private static final long STARTUP = 999l; // ms

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                UIDefaults defaults = createUIDefaults();

                ContentPane contentPane = new ContentPane();

                SwingThreadTimelineView view = new SwingThreadTimelineView(defaults);
                JFrame frame = new JFrame();
                frame.add(contentPane);

                contentPane.add(view.getUiComponent());

                JButton addDate = new JButton("Add Timeline Data");

                addDate.addActionListener(new DataFiller(view));
                contentPane.add(addDate, BorderLayout.SOUTH);

                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(new Dimension(800, 800));
                frame.setVisible(true);
            }
        });
    }

    private static class DataFiller implements ActionListener {

        private ThreadTimelineView view;
        private long startTime;
        private long lastUpdate;

        private boolean threadAdded;
        private int swap;
        private boolean swapped;

        private int added;

        DataFiller(ThreadTimelineView view) {
            this.view = view;
            startTime = STARTUP;
            lastUpdate = STARTUP;
            swap = 0;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SwingWorker<Void, Void> worker  = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    for (long l = 0; l < 10_000; l+=INTERVAL) {
                        lastUpdate += INTERVAL;
                        final Range<Long> range = new Range<>(startTime, lastUpdate);
                        view.setTotalRange(range);

                        if (!threadAdded) {
                            ThreadInfo info = new ThreadInfo();
                            info.setName("test1");
                            info.setId(0l);
                            view.addThread(info);

                            info = new ThreadInfo();
                            info.setName("test2");
                            info.setId(1l);
                            view.addThread(info);

                            threadAdded = true;

                        }

                        Palette color1 = Palette.THERMOSTAT_BLU;
                        Palette color2 = Palette.THERMOSTAT_RED;

                        if (swapped) {
                            color1 = Palette.THERMOSTAT_RED;
                            color2 = Palette.THERMOSTAT_BLU;
                        }

                        swap++;
                        if (swap % 10 == 0) {
                            swapped = !swapped;
                            swap = 0;
                        }

                        ThreadInfo info = new ThreadInfo();
                        info.setName("test1");
                        info.setId(0l);
                        TimelineProbe probe = new TimelineProbe(color1, "test1", lastUpdate);
                        view.addProbe(info, probe);

                        info = new ThreadInfo();
                        info.setName("test2");
                        info.setId(1l);
                        probe = new TimelineProbe(color2, "test2", lastUpdate);
                        view.addProbe(info, probe);

                        added++;
                    }
                    return null;
                }
            };
            worker.execute();
        }
    }

    private static UIDefaults createUIDefaults() {
        UIDefaults defaults = mock(UIDefaults.class);
        when(defaults.getDefaultFont()).thenReturn(new JLabel().getFont());
        when(defaults.getIconColor()).thenReturn(Palette.EARL_GRAY.getColor());
        when(defaults.getSelectedComponentBGColor()).thenReturn(Palette.ADWAITA_BLU.getColor());

        return defaults;
    }
}
