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

package com.redhat.thermostat.thread.client.swing.impl.timeline;

import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.thread.client.common.model.timeline.ThreadInfo;
import com.redhat.thermostat.thread.client.swing.experimental.components.ContentPane;
import com.redhat.thermostat.thread.client.swing.experimental.components.Separator;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.TimelineModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 */
public class TimelineComponent extends RulerComponent {

    private static final int MIN_HEIGHT = 50;
    private final ThreadInfo threadInfo;

    private ContentPane labelPane;

    private TimelineLabel label;

    private ThermostatScrollPane scrollPane;
    private TimelineContainer timelineContainer;

    public TimelineComponent(UIDefaults uiDefaults, ThreadInfo threadInfo,
                             TimelineModel model)
    {
        super(uiDefaults, model);
        this.threadInfo = threadInfo;
    }

    public void initComponents() {
        setName(threadInfo.getName());

        initModel();

        initLabelPane();
        initThreadPane();

        setBorder(new Separator(uiDefaults, Separator.Side.BOTTOM,
                                Separator.Type.SOLID));

        Hover hover = new Hover();
        addMouseListener(hover);
    }

    private void initModel() {
        model.getScrollBarModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                repaint();
            }
        });
    }

    private void initThreadPane() {

        timelineContainer = new TimelineContainer(model);
        timelineContainer.setName(threadInfo.getName());

        scrollPane = new ThermostatScrollPane(timelineContainer);
        scrollPane.setHorizontalScrollBarPolicy(ThermostatScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ThermostatScrollPane.VERTICAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);

        scrollPane.getHorizontalScrollBar().setModel(model.getScrollBarModel());
    }

    public TimelineContainer getTimelineContainer() {
        return timelineContainer;
    }

    private void initLabelPane() {
        label = new TimelineLabel(uiDefaults, getName());
        labelPane = new ContentPane();

        labelPane.setOpaque(false);
        labelPane.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.f;

        labelPane.add(label, gbc);
        add(labelPane, BorderLayout.NORTH);
    }

    @Override
    public int getHeight() {
        return MIN_HEIGHT;
    }

    private class Hover extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            onMouseHover(true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            onMouseHover(false);
        }

        public void onMouseHover(boolean hover) {
            label.onMouseHover(hover);
            repaint();
        }
    }
}
