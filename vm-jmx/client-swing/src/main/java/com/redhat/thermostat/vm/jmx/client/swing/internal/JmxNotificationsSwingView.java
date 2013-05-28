/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.redhat.thermostat.client.swing.ComponentVisibleListener;
import com.redhat.thermostat.client.swing.IconResource;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ActionToggleButton;
import com.redhat.thermostat.client.swing.components.LocalizedLabel;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsView;
import com.redhat.thermostat.vm.jmx.client.core.LocaleResources;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;

public class JmxNotificationsSwingView extends JmxNotificationsView implements SwingComponent {

    private static final Translate<LocaleResources> translate = LocaleResources.createLocalizer();
    private List<ActionListener<NotificationAction>> listeners = new CopyOnWriteArrayList<>();

    private final HeaderPanel visiblePanel;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();

    private ActionToggleButton toolbarButton;

    public JmxNotificationsSwingView() {

        LocalizedLabel description = new LocalizedLabel(translate.localize(LocaleResources.NOTIFICATIONS_DESCRIPTION));
        description.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JList<String> issuesList = new JList<>(listModel);

        JPanel contents = new JPanel();
        contents.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.FIRST_LINE_START;

        contents.add(description, c);

        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        contents.add(new JScrollPane(issuesList), c);

        contents.addHierarchyListener(new ComponentVisibleListener() {
            @Override
            public void componentShown(Component component) {
                notifier.fireAction(Action.VISIBLE);
            }

            @Override
            public void componentHidden(Component component) {
                notifier.fireAction(Action.HIDDEN);
            }
        });

        toolbarButton = new ActionToggleButton(IconResource.SAMPLE.getIcon(), translate.localize(LocaleResources.NOTIFICATIONS_ENABLE));
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

    @Override
    public void addNotificationActionListener(ActionListener<NotificationAction> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeNotificationActionListener(ActionListener<NotificationAction> listener) {
        listeners.remove(listener);
    }

    private void fireNotificationAction(NotificationAction action) {
        for (ActionListener<NotificationAction> listener : listeners) {
            listener.actionPerformed(new ActionEvent<>(this, action));
        }
    }

    @Override
    public void setNotificationsEnabled(final boolean enabled) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                toolbarButton.setSelected(enabled);
            }
        });
    }

    @Override
    public void clearNotifications() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                listModel.clear();
            }
        });
    }

    @Override
    public void addNotification(final JmxNotification data) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                listModel.add(listModel.size(), data.getContents());
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

}
