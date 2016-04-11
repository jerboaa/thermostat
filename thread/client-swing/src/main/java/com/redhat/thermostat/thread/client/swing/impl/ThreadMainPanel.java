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

import com.redhat.thermostat.client.swing.IconResource;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.ActionToggleButton;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.OverlayPanel;
import com.redhat.thermostat.client.swing.components.ShadowLabel;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.client.swing.components.ThermostatThinScrollBar;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.thread.client.common.locale.LocaleResources;
import com.redhat.thermostat.thread.model.ThreadSession;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.OverlayLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.List;

@SuppressWarnings("serial")
class ThreadMainPanel extends JPanel {

    private static final Icon START_ICON = IconResource.SAMPLE.getIcon();
    private final Icon stopIcon;

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private JSplitPane splitPane;
    
    private ActionToggleButton toggleButton;
    private ActionToggleButton showRecordedSessionsButton;

    private OverlayPanel overlay;

    private UIDefaults uiDefaults;
    private ThreadSessionList sessionsPanel;
    private DefaultListModel<ThreadSession> sessionsModel;

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    public ThreadMainPanel(UIDefaults uiDefaults) {
        this.uiDefaults = uiDefaults;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        HeaderPanel headerPanel = new HeaderPanel();
        headerPanel.setHeader(t.localize(LocaleResources.THREAD_CONTROL_PANEL));

        stopIcon = new FontAwesomeIcon('\uf28e', START_ICON.getIconHeight(), uiDefaults.getIconColor());

        toggleButton = new ActionToggleButton(START_ICON, stopIcon, t.localize(LocaleResources.THREAD_MONITOR_SWITCH));
        toggleButton.setName("recordButton");
        headerPanel.addToolBarButton(toggleButton);

        Icon listSessionsIcon = IconResource.HISTORY.getIcon();
        showRecordedSessionsButton = new ActionToggleButton(listSessionsIcon, t.localize(LocaleResources.THREAD_MONITOR_DISPLAY_SESSIONS));
        showRecordedSessionsButton.setName("showRecordedSessionsButton");
        headerPanel.addToolBarButton(showRecordedSessionsButton);

        overlay = new OverlayPanel(t.localize(LocaleResources.RECORDING_LIST), true, true);
        overlay.setName("threadOverlayPanel");
        overlay.addCloseEventListener(new OverlayPanel.CloseEventListener() {
            @Override
            public void closeRequested(OverlayPanel.CloseEvent event) {
                showRecordedSessionsButton.doClick();
            }
        });

        JPanel stack = new JPanel();
        stack.setName("threadStackPanel");
        stack.setOpaque(true);
        stack.setLayout(new OverlayLayout(stack));

        splitPane = new JSplitPane();
        splitPane.setName("threadMainPanelSplitPane");
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOneTouchExpandable(true);

        JPanel content = new JPanel();
        GroupLayout gl_content = new GroupLayout(content);
        gl_content.setHorizontalGroup(
            gl_content.createParallelGroup(Alignment.TRAILING)
                .addGroup(Alignment.LEADING, gl_content.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(splitPane, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap())
        );
        gl_content.setVerticalGroup(
            gl_content.createParallelGroup(Alignment.TRAILING)
                .addGroup(Alignment.LEADING, gl_content.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(splitPane, 0, 240, Short.MAX_VALUE)
                    .addContainerGap())
        );

        content.setLayout(gl_content);

        stack.add(overlay);
        stack.add(content);
        stack.setOpaque(false);

        headerPanel.setContent(stack);

        add(headerPanel);

        sessionsModel = new DefaultListModel<>();

        sessionsPanel = new ThreadSessionList(sessionsModel);
        sessionsPanel.setSelectionMode(JList.VERTICAL);
        sessionsPanel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sessionsPanel.setOpaque(false);
        sessionsPanel.setCellRenderer(new ThreadSessionRenderer());
        sessionsPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point where = new Point(e.getX(), e.getY());
                int index = sessionsPanel.locationToIndex(where);
                int hoveredIndex = sessionsPanel.getHoveredIndex();
                if (index != hoveredIndex) {
                    sessionsPanel.setHoveredIndex(index);
                    sessionsPanel.repaint();
                }
            }
        });
        ThermostatScrollPane scrollPane = new ThermostatScrollPane(sessionsPanel);
        scrollPane.setVerticalScrollBar(new ThermostatThinScrollBar(ThermostatThinScrollBar.VERTICAL));
        overlay.add(scrollPane);
    }

    public ThreadSessionList getSessionsPanel() {
        return sessionsPanel;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }
    
    public ActionToggleButton getRecordingToggleButton() {
        return toggleButton;
    }

    public ActionToggleButton getShowRecordedSessionsButton() {
        return showRecordedSessionsButton;
    }

    public void toggleOverlayPanel(boolean visible) {
        overlay.setOverlayVisible(visible);
    }

    public class ThreadSessionRenderer implements ListCellRenderer<ThreadSession> {

        @Override
        public Component getListCellRendererComponent(JList<? extends ThreadSession> list,
                                                      ThreadSession value, int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.setOpaque(false);
            ShadowLabel label = new ShadowLabel();
            label.setText("[" + new Date(value.getTimeStamp()) +"]");
            label.setOpaque(false);

            if (isSelected || cellHasFocus) {
                panel.setOpaque(true);
                panel.setBackground((Color) uiDefaults.getSelectedComponentBGColor());
                label.setForeground((Color) uiDefaults.getSelectedComponentFGColor());

            } else if ((sessionsPanel.getHoveredIndex() == index)) {
                panel.setOpaque(true);
                panel.setBackground(Palette.ELEGANT_CYAN.getColor());
                label.setForeground((Color) uiDefaults.getSelectedComponentFGColor());

            } else {
                label.setForeground((Color) uiDefaults.getComponentFGColor());
            }

            panel.add(label);
            return panel;
        }
    }

    public void setOverlayContent(List<ThreadSession> threadSessions) {
        sessionsModel.clear();

        for (ThreadSession session : threadSessions) {
            sessionsModel.addElement(session);
        }
        sessionsPanel.setHoveredIndex(-1);

        overlay.revalidate();
        overlay.repaint();

    }

    public class ThreadSessionList extends JList<ThreadSession> {
        private int hoveredIndex;
        public ThreadSessionList(ListModel<ThreadSession> dataModel) {
            super(dataModel);
            hoveredIndex = -1;
        }

        public int getHoveredIndex() {
            return hoveredIndex;
        }

        public void setHoveredIndex(int hoveredIndex) {
            this.hoveredIndex = hoveredIndex;
        }
    }
}

