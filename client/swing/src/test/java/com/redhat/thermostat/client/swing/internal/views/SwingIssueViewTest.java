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

package com.redhat.thermostat.client.swing.internal.views;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.core.Severity;
import com.redhat.thermostat.client.core.views.IssueView;
import com.redhat.thermostat.client.core.views.IssueView.IssueAction;
import com.redhat.thermostat.client.core.views.IssueView.IssueDescription;
import com.redhat.thermostat.client.swing.internal.views.SwingIssueView;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;

public class SwingIssueViewTest {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final SwingIssueView view = new SwingIssueView();
                view.showInitialView();

                JFrame mainWindow = new JFrame();
                mainWindow.add(view.getUiComponent());

                view.addIssueActionListener(new ActionListener<IssueView.IssueAction>() {
                    @Override
                    public void actionPerformed(ActionEvent<IssueAction> actionEvent) {
                        view.showIssues();
                        view.clearIssues();
                        view.addIssue(new IssueDescription(Severity.CRITICAL, "agent-id", "vm-id", "CRITICAL!"));
                        view.addIssue(new IssueDescription(Severity.WARNING, "agent-id", "vm-id", "WARNING!"));
                        view.addIssue(new IssueDescription(Severity.LOW, "agent-id", "vm-id", "LOW!"));
                    }
                });

                mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainWindow.pack();
                mainWindow.setVisible(true);
            }
        });
    }

}
