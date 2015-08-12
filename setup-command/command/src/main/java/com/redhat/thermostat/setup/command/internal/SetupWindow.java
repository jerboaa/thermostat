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

package com.redhat.thermostat.setup.command.internal;

import com.redhat.thermostat.setup.command.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.Arrays;

public class SetupWindow {
    private CountDownLatch shutdown;
    private JFrame frame;
    private JPanel mainView;
    private JPanel topPanel;
    private JLabel title;
    private JLabel progress;
    private StartView startView;
    private MongoUserSetupView mongoUserSetupView;
    private UserPropertiesView userPropertiesView;
    private String storageUsername = null;
    private char[] storagePassword = null;
    private boolean showDetailedBlurb = false;
    private ThermostatSetup thermostatSetup;

    private static final String DEFAULT_AGENT_USER = "agent-tester";
    private static final String DEFAULT_CLIENT_USER = "client-tester";
    private static final String DEFAULT_USER_PASSWORD = "tester";
    private static final String DEFAULT_STORAGE_USER = "mongodevuser";
    private static final String DEFAULT_STORAGE_PASSWORD = "mongodevpassword";
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private PrintStream out;

    public SetupWindow(PrintStream out, ThermostatSetup thermostatSetup) {
        this.out = out;
        this.thermostatSetup = thermostatSetup;
        shutdown = new CountDownLatch(1);
    }

    public void run() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initialize();
                addViewListeners();
                showView(startView);
                frame.setVisible(true);
            }
        });

        try {
            shutdown.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        if (storagePassword != null) {
            Arrays.fill(storagePassword, '\0');
        }
    }

    private void initialize() {
        frame = new JFrame(translator.localize(LocaleResources.WINDOW_TITLE).getContents());
        setLargeFrame(false);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
        mainView = new JPanel(new BorderLayout());
        createTopPanel();
        mainView.add(topPanel, BorderLayout.NORTH);
        frame.add(mainView);

        startView = new StartView(new BorderLayout());
        mongoUserSetupView = new MongoUserSetupView(new BorderLayout());
        userPropertiesView = new UserPropertiesView(new BorderLayout());
    }

    private void createTopPanel() {
        title = new JLabel();
        title.setFont(new Font("Liberation Sans", Font.BOLD, 16));
        progress = new JLabel();

        topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets.left = 10;
        c.insets.top = 10;
        c.gridx = 0;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.LINE_START;
        topPanel.add(progress, c);
        c.gridx = 1;
        c.weightx = 1;
        c.gridwidth = 2;
        topPanel.add(title, c);
    }

    private void addViewListeners() {
        startView.getNextBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                showDetailedBlurb = false;
                startView.showMoreInfo(showDetailedBlurb);
                setLargeFrame(showDetailedBlurb);

                mainView.remove(startView);
                showView(mongoUserSetupView);
            }
        });
        startView.getCancelBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                shutdown();
            }
        });
        startView.getShowMoreInfoBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                showDetailedBlurb = !showDetailedBlurb;
                startView.showMoreInfo(showDetailedBlurb);
                setLargeFrame(showDetailedBlurb);
            }
        });
        mongoUserSetupView.getBackBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mainView.remove(mongoUserSetupView);
                showView(startView);
            }
        });
        mongoUserSetupView.getDefaultSetupBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mongoUserSetupView.setUsername(DEFAULT_STORAGE_USER);
                mongoUserSetupView.setPassword(DEFAULT_STORAGE_PASSWORD);
            }
        });
        mongoUserSetupView.getNextBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                storageUsername = mongoUserSetupView.getUsername();
                storagePassword = mongoUserSetupView.getPassword();
                runMongoSetup();

                if (thermostatSetup.isWebAppInstalled()) {
                    mainView.remove(mongoUserSetupView);
                    showView(userPropertiesView);
                }
            }
        });
        mongoUserSetupView.getCancelBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                shutdown();
            }
        });
        userPropertiesView.getFinishBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                runPropertiesSetup();
            }
        });
        userPropertiesView.getCancelBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                shutdown();
            }
        });
    }

    private void setLargeFrame(boolean setLarge) {
        if (setLarge) {
            frame.setSize(600, 600);
        } else {
            frame.setSize(600, 350);
        }
    }

    private void runMongoSetup() {
        SwingWorker worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                mongoUserSetupView.disableButtons();
                userPropertiesView.disableButtons();
                try {
                    thermostatSetup.createMongodbUser(storageUsername, storagePassword);
                } catch (MongodbUserSetupException e) {
                    e.printStackTrace();
                    shutdown();
                }
                return null;
            }

            @Override
            public void done() {
                mongoUserSetupView.enableButtons();
                userPropertiesView.enableButtons();
                if (!thermostatSetup.isWebAppInstalled()) {
                    shutdown();
                }
            }
        };
        worker.execute();
    }

    private void runPropertiesSetup() {
        SwingWorker worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                userPropertiesView.disableButtons();
                try {
                    String[] agentRoles = null;
                    String[] clientRoles = null;
                    if (userPropertiesView.makeAgentUserSelected()) {
                        agentRoles = new String[] {
                                UserRoles.CMD_CHANNEL_VERIFY,
                                UserRoles.LOGIN,
                                UserRoles.PREPARE_STATEMENT,
                                UserRoles.PURGE,
                                UserRoles.REGISTER_CATEGORY,
                                UserRoles.ACCESS_REALM,
                                UserRoles.SAVE_FILE,
                                UserRoles.WRITE,
                                UserRoles.GRANT_FILES_WRITE_ALL,
                        };
                    }
                    if (userPropertiesView.makeClientAdminSelected()) {
                        clientRoles = new String[] {
                                UserRoles.GRANT_AGENTS_READ_ALL,
                                UserRoles.CMD_CHANNEL_GENERATE,
                                UserRoles.GRANT_HOSTS_READ_ALL,
                                UserRoles.LOAD_FILE,
                                UserRoles.LOGIN,
                                UserRoles.PREPARE_STATEMENT,
                                UserRoles.READ,
                                UserRoles.ACCESS_REALM,
                                UserRoles.REGISTER_CATEGORY,
                                UserRoles.GRANT_VMS_READ_BY_USERNAME_ALL,
                                UserRoles.GRANT_VMS_READ_BY_VM_ID_ALL,
                                UserRoles.GRANT_FILES_READ_ALL,
                                UserRoles.WRITE,
                        };
                    }
                    thermostatSetup.createThermostatUser(DEFAULT_AGENT_USER, DEFAULT_USER_PASSWORD.toCharArray(), agentRoles);
                    thermostatSetup.createThermostatUser(DEFAULT_CLIENT_USER, DEFAULT_USER_PASSWORD.toCharArray(), clientRoles);

                } catch (IOException e) {
                    e.printStackTrace();
                    shutdown();
                }
                return null;
            }

            @Override
            public void done() {
                userPropertiesView.enableButtons();
                shutdown();
            }
        };
        worker.execute();
    }

    private void showView(SetupView view) {
        mainView.add(view.getUiComponent(), BorderLayout.CENTER);
        view.setTitleAndProgress(title, progress);
        mainView.revalidate();
        mainView.repaint();
    }

    private void shutdown() {
        shutdown.countDown();
    }
}
