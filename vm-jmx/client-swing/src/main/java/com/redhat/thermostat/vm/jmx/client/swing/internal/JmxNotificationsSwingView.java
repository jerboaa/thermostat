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

package com.redhat.thermostat.vm.jmx.client.swing.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.redhat.thermostat.client.swing.IconResource;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ActionToggleButton;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.LocalizedLabel;
import com.redhat.thermostat.client.swing.components.experimental.EventTimeline;
import com.redhat.thermostat.client.swing.components.experimental.EventTimelineRangeChangeListener;
import com.redhat.thermostat.client.swing.components.experimental.Timeline;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.model.LongRangeNormalizer;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsView;
import com.redhat.thermostat.vm.jmx.client.core.LocaleResources;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;

public class JmxNotificationsSwingView extends JmxNotificationsView implements SwingComponent {

    private static final Translate<LocaleResources> translate = LocaleResources.createLocalizer();
    private List<ActionListener<NotificationAction>> listeners = new CopyOnWriteArrayList<>();

    private static final Icon START_ICON = IconResource.SAMPLE.getIcon();
    private static final Icon STOP_ICON = new FontAwesomeIcon('\uf28e', START_ICON.getIconHeight());

    private final HeaderPanel visiblePanel;

    private ActionToggleButton toolbarButton;

    private List<JmxNotification> notifications = new ArrayList<>();

    private DetailPanel timelineDetails;
    private Timeline ruler;
    private EventTimeline timeline;
    private boolean viewControlsEnabled = true;

    public JmxNotificationsSwingView() {

        LocalizedLabel description = new LocalizedLabel(translate.localize(LocaleResources.NOTIFICATIONS_DESCRIPTION));

        JPanel contents = new JPanel();
        contents.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.FIRST_LINE_START;

        contents.add(description, c);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.FIRST_LINE_START;

        c.weightx = 1;
        c.weighty = 1;

        timelineDetails = new DetailPanel();
        contents.add(timelineDetails, c);

        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;

        ruler = new Timeline(new Range<Long>(1l, 2l));

        contents.add(ruler, c);

        c.gridy++;
        c.weightx = 0.25;
        c.weighty = 0.25;

        timeline = new EventTimeline();
        Color edgeColor = Palette.THERMOSTAT_BLU.getColor();
        Color fillColor = new Color(edgeColor.getRed(), edgeColor.getGreen(), edgeColor.getBlue(), 25);
        Color eventColor = Palette.THERMOSTAT_RED.getColor();
        timeline.setSelectionEdgePaint(edgeColor);
        timeline.setSelectionFillPaint(fillColor);
        timeline.setEventPaint(eventColor);

        timeline.getModel().addRangeChangeListener(new EventTimelineRangeChangeListener() {
            @Override
            public void rangeChanged(Range<Long> overview, Range<Long> detail) {
                if (detail != null) {
                    plotDetails(detail.getMin(), detail.getMax());
                }
            }
        });

        contents.add(timeline, c);

        new ComponentVisibilityNotifier().initialize(contents, notifier);

        toolbarButton = new ActionToggleButton(START_ICON, STOP_ICON, translate.localize(LocaleResources.NOTIFICATIONS_ENABLE));
        toolbarButton.setName("toggleNotifications");
        toolbarButton.setToolTipText(translate.localize(LocaleResources.NOTIFICATIONS_ENABLE_DESCRIPTION).getContents());
        toolbarButton.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fireNotificationAction(NotificationAction.TOGGLE_NOTIFICATIONS);
            }
        });
        toolbarButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                ButtonModel model = ((AbstractButton) e.getSource()).getModel();
                if (model.isSelected()) {
                    toolbarButton.setText(translate.localize(LocaleResources.NOTIFICATIONS_DISABLE).getContents());
                    toolbarButton.setToolTipText(translate.localize(LocaleResources.NOTIFICATIONS_DISABLE_DESCRIPTION).getContents());
                } else {
                    toolbarButton.setText(translate.localize(LocaleResources.NOTIFICATIONS_ENABLE).getContents());
                    toolbarButton.setToolTipText(translate.localize(LocaleResources.NOTIFICATIONS_ENABLE_DESCRIPTION).getContents());
                }
            }
        });

        visiblePanel = new HeaderPanel(translate.localize(LocaleResources.NOTIFICATIONS_HEADER));
        visiblePanel.addToolBarButton(toolbarButton);
        visiblePanel.setContent(contents);
    }

    protected void plotDetails(long start, long end) {
        ruler.setRange(new Range<Long>(start, end));
        timelineDetails.setDisplayRange(start, end);
    }

    @Override
    public void addNotificationActionListener(ActionListener<NotificationAction> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeNotificationActionListener(ActionListener<NotificationAction> listener) {
        listeners.remove(listener);
    }

    @Override
    public void setMonitoringState(final MonitoringState monitoringState) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!viewControlsEnabled) {
                    toolbarButton.setToggleActionState(MonitoringState.DISABLED);
                } else {
                    toolbarButton.setToggleActionState(monitoringState);
                }
            }
        });
    }

    @Override
    public void setViewControlsEnabled(boolean enabled) {
        this.viewControlsEnabled = enabled;
        if (!enabled) {
            setMonitoringState(MonitoringState.DISABLED);
        }
    }

    private void fireNotificationAction(NotificationAction action) {
        for (ActionListener<NotificationAction> listener : listeners) {
            listener.actionPerformed(new ActionEvent<>(this, action));
        }
    }

    @Override
    public void clearNotifications() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                notifications.clear();
                timeline.getModel().clearEvents();
            }
        });
    }

    @Override
    public void addNotification(final JmxNotification data) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                notifications.add(data);
                timeline.getModel().addEvent(data.getTimeStamp(), data.getSourceDetails());
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

    @Override
    public void displayWarning(final LocalizedString warning) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(visiblePanel.getParent(), warning.getContents(), "", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private class DetailPanel extends JPanel {

        private long start = 0;
        private long end = 0;

        public void setDisplayRange(long start, long end) {
            this.start = start;
            this.end = end;

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            long startTimeStamp = start;
            long endTimeStamp = end;

            int step = 0;

            for (JmxNotification not : notifications) {
                step++;
                if ((not.getTimeStamp() >= startTimeStamp) && (not.getTimeStamp() <= endTimeStamp)) {
                    paintNotification((Graphics2D) g, not, step);
                }
            }
        }

        private void paintNotification(Graphics2D g, JmxNotification not, int step) {
            long startTimeStamp = start;
            long endTimeStamp = end;
            LongRangeNormalizer normalizer = new LongRangeNormalizer(new Range<>(startTimeStamp, endTimeStamp), 0, getWidth());
            int xPos = (int) normalizer.getValueNormalized(not.getTimeStamp());

            paintNotificationDetails(not, g, xPos, getYBandPosition(step));
        }

        private int getYBandPosition(int step) {
            // TODO can we do better in determining the number of 'bands' ?
            int TOTAL_STEPS = 10;
            step = step % TOTAL_STEPS;

            return Math.round(1.0f * step * getHeight() / TOTAL_STEPS);
        }

        private void paintNotificationDetails(JmxNotification notification, Graphics2D g, int x, int y) {
            g = (Graphics2D) g.create();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            FontMetrics metrics = g.getFontMetrics();
            double textWidth = 0;

            textWidth = Math.max(textWidth, metrics.getStringBounds(notification.getSourceDetails(), g).getWidth());
            textWidth = Math.max(textWidth, metrics.getStringBounds(notification.getContents(), g).getWidth());

            int lines = 3;
            int lineHeight = metrics.getHeight();

            int textHeight = lineHeight * lines;

            final int TEXT_PADDING = 10;

            g.setColor(Palette.PALE_RED.getColor());

            g.drawLine(x, getHeight(), x, y);
            g.fillRect(x, y, (int) textWidth + (2 * TEXT_PADDING), textHeight + (lines * TEXT_PADDING));

            g.setColor(Color.WHITE);
            DateFormat df = new SimpleDateFormat("HH:mm:ss");
            g.drawString(df.format(new Date(notification.getTimeStamp())), x + TEXT_PADDING, y + TEXT_PADDING + (lineHeight * 1));
            g.drawString(notification.getSourceDetails(), x + TEXT_PADDING, y + TEXT_PADDING + (lineHeight * 2));
            g.drawString(notification.getContents(), x + TEXT_PADDING, y + TEXT_PADDING + (lineHeight * 3));

            g.dispose();
        }

    }

    public static void main(String[] args) {
        final JmxNotificationsSwingView view = new JmxNotificationsSwingView();

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(view.getUiComponent());

                frame.setVisible(true);
            }
        });

        final long time = System.currentTimeMillis();

        final AtomicInteger i = new AtomicInteger(0);
        Timer t = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                int c = i.incrementAndGet();
                JmxNotification data = new JmxNotification("foo-agent");
                data.setTimeStamp(time + (c * TimeUnit.MINUTES.toMillis(1)));
                data.setSourceBackend("foo");
                data.setSourceDetails("GarbageCollection " + c);
                data.setContents(c + ": PS Scavenge on 'old' gen. 10 s");
                view.addNotification(data);
            }
        };

        t.schedule(task, 0, TimeUnit.SECONDS.toMillis(1));
    }

}

