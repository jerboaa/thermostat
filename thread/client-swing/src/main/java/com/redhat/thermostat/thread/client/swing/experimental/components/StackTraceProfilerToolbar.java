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

package com.redhat.thermostat.thread.client.swing.experimental.components;

import com.redhat.thermostat.beans.property.BooleanProperty;
import com.redhat.thermostat.beans.property.ChangeListener;
import com.redhat.thermostat.beans.property.ObservableValue;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.thread.client.common.Toggle;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 */
public class StackTraceProfilerToolbar extends DataPane {

    private Icon mergeTraces;
    private Icon expandeTraces;
    private Icon mergeTracesHover;
    private Icon expandeTracesHover;

    private JLabel expandTracesLabel;
    private Toggle mergeRecursiveTracesProperty;

    public StackTraceProfilerToolbar(UIDefaults uiDefaults, Toggle mergeRecursiveTracesProperty) {

        DataPane controls = new DataPane();
        controls.setLayout(new GridLayout(1, 0, 5, 5));

        this.mergeRecursiveTracesProperty = mergeRecursiveTracesProperty;

        mergeTraces = new FontAwesomeIcon('\uf0dc', 15, uiDefaults.getIconColor());
        expandeTraces = new FontAwesomeIcon('\uf160', 15, uiDefaults.getIconColor());
        mergeTracesHover = new FontAwesomeIcon('\uf0dc', 15, uiDefaults.getSelectedComponentBGColor());
        expandeTracesHover = new FontAwesomeIcon('\uf160', 15, uiDefaults.getSelectedComponentBGColor());

        expandTracesLabel = new JLabel();
        expandTracesLabel.setBorder(new EmptyBorder(1, 1, 1, 1));

        setMergeTraceIconState(false);

        mergeRecursiveTracesProperty.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                setMergeTraceIconState(true);
            }
        });

        expandTracesLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                StackTraceProfilerToolbar.this.mergeRecursiveTracesProperty.toggle();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setMergeTraceIconState(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setMergeTraceIconState(false);
            }
        });

        controls.add(expandTracesLabel);
        add(controls, BorderLayout.EAST);
    }

    private void setMergeTraceIconState(boolean hover) {
        Icon state = mergeRecursiveTracesProperty.get() ? mergeTraces : expandeTraces;
        if (hover) {
            state = mergeRecursiveTracesProperty.get() ? mergeTracesHover : expandeTracesHover;
        }
        expandTracesLabel.setIcon(state);
    }
}
