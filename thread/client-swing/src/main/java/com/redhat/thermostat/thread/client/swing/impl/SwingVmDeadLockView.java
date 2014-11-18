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

package com.redhat.thermostat.thread.client.swing.impl;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.thread.client.common.locale.LocaleResources;
import com.redhat.thermostat.thread.client.common.view.VmDeadLockView;

public class SwingVmDeadLockView extends VmDeadLockView implements SwingComponent {

    private static final Translate<LocaleResources> translate = LocaleResources.createLocalizer();

    private final JPanel actualComponent = new JPanel();
    private final JTextArea description = new JTextArea();

    public SwingVmDeadLockView() {
        actualComponent.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_END;
        JButton recheckButton = new JButton(translate.localize(LocaleResources.CHECK_FOR_DEADLOCKS).getContents());
        recheckButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deadLockNotifier.fireAction(VmDeadLockViewAction.CHECK_FOR_DEADLOCK);
            }
        });

        actualComponent.add(recheckButton, c);

        c.anchor = GridBagConstraints.LINE_START;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;

        JScrollPane scrollPane = new JScrollPane(description);
        actualComponent.add(scrollPane, c);

        new ComponentVisibilityNotifier().initialize(actualComponent, notifier);
    }

    @Override
    public void setDeadLockInformation(final String info) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                description.setText(info);
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return actualComponent;
    }

}

