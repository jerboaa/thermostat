/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.client.internal;

import static com.redhat.thermostat.client.locale.Translate.localize;

import java.awt.EventQueue;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.swing.laf.dolphin.DolphinLookAndFeel;
import com.redhat.thermostat.client.internal.config.ConnectionConfiguration;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.osgi.service.ApplicationService;
import com.redhat.thermostat.client.ui.ClientConfigurationController;
import com.redhat.thermostat.client.ui.ClientConfigurationSwing;
import com.redhat.thermostat.client.ui.ClientConfigurationView;
import com.redhat.thermostat.client.ui.MainWindowController;
import com.redhat.thermostat.client.ui.UiFacadeFactory;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.ThreadPoolTimerFactory;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.config.StartupConfiguration;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.MongoDAOFactory;
import com.redhat.thermostat.common.storage.Connection;
import com.redhat.thermostat.common.storage.Connection.ConnectionListener;
import com.redhat.thermostat.common.storage.Connection.ConnectionStatus;
import com.redhat.thermostat.common.storage.Connection.ConnectionType;
import com.redhat.thermostat.common.storage.MongoStorageProvider;
import com.redhat.thermostat.common.storage.StorageProvider;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.utils.keyring.Keyring;

public class Main {
    
    private static final Logger logger = LoggingUtils.getLogger(Main.class);

    private UiFacadeFactory uiFacadeFactory;
    
    public Main(Keyring keyring, UiFacadeFactory uiFacadeFactory, String[] args) {
        this.uiFacadeFactory = uiFacadeFactory;
        setUp(keyring);
    }

    void run() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {

                // check if the user has other preferences...
                // not that there is any reason!
                String laf = System.getProperty("swing.defaultlaf");
                if (laf == null) {
                    try {
                        UIManager.setLookAndFeel(new DolphinLookAndFeel());
                    } catch (UnsupportedLookAndFeelException e) {
                        logger.log(Level.WARNING, "cannot use DolphinLookAndFeel");
                    }
                }

                // TODO: move them in an appropriate place
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);
                UIManager.getDefaults().put("OptionPane.buttonOrientation", SwingConstants.RIGHT);
                UIManager.getDefaults().put("OptionPane.isYesLast", true);
                UIManager.getDefaults().put("OptionPane.sameSizeButtons", true);
                
                showGui();
            }

        });

        try {
            uiFacadeFactory.awaitShutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    void setUp(Keyring keyring) {
        ClientPreferences prefs = new ClientPreferences(keyring);
        StartupConfiguration config = new ConnectionConfiguration(prefs);

        StorageProvider connProv = new MongoStorageProvider(config);
        DAOFactory daoFactory = new MongoDAOFactory(connProv);
        ApplicationContext.getInstance().setDAOFactory(daoFactory);
        TimerFactory timerFactory = new ThreadPoolTimerFactory(1);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);
        SwingViewFactory viewFactory = new SwingViewFactory();
        ApplicationContext.getInstance().setViewFactory(viewFactory);
    }
        
    private void showGui() {
        
        ApplicationService appSrv = OSGIUtils.getInstance().getService(ApplicationService.class);
        final ExecutorService service = appSrv.getApplicationExecutor();
        
        service.execute(new ConnectorSetup(service));
    }
    
    private class ConnectorSetup implements Runnable {
        
        private ExecutorService service;
        public ConnectorSetup(ExecutorService service) {
            this.service = service;
        }
        
        @Override
        public void run() {
            
            Connection connection = ApplicationContext.getInstance().getDAOFactory().getConnection();
            connection.setType(ConnectionType.LOCAL);
            ConnectionListener connectionListener = new ConnectionHandler(connection, service);
            connection.addListener(connectionListener);
            try {
                connection.connect();
            } catch (Throwable t) {
                logger.log(Level.WARNING, "connection attempt failed: ", t);
            }
        }
    }
    
    private class Connector implements Runnable {
        private Connection connection;
        Connector(Connection connection) {
            this.connection = connection;
        }
        
        @Override
        public void run() {
            try {
                connection.connect();
            } catch (Throwable t) {
                logger.log(Level.WARNING, "connection attempt failed: ", t);
            }
        }
    }
    
    private class ConnectionAttemp implements Runnable {
        private Connection connection;
        private ExecutorService service;
        public ConnectionAttemp(Connection connection, ExecutorService service) {
            this.connection = connection;
            this.service = service;
        }
        
        @Override
        public void run() {
            Object[] options = {
                    localize(LocaleResources.CONNECTION_WIZARD),
                    localize(LocaleResources.CONNECTION_QUIT),
            };
            int n = JOptionPane
                    .showOptionDialog(
                            null,
                            localize(LocaleResources.CONNECTION_FAILED_TO_CONNECT_DESCRIPTION),
                            localize(LocaleResources.CONNECTION_FAILED_TO_CONNECT_TITLE),
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.ERROR_MESSAGE, null, options,
                            options[0]);

            switch (n) {

            case JOptionPane.CANCEL_OPTION:
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.NO_OPTION:
                uiFacadeFactory
                        .shutdown(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
                break;

            case JOptionPane.YES_OPTION:
            default:
                createPreferencesDialog(connection, service);
                break;
            }
        }
    }
    
    private void connect(Connection connection, ExecutorService service) {
        service.execute(new Connector(connection));
    }
    
    private class ConfigDialogListener implements ActionListener<ClientConfigurationView.Action> {
        private Connection connection;
        private ExecutorService service;
        public ConfigDialogListener(Connection connection, ExecutorService service) {
            this.connection = connection;
            this.service = service;
        }
        
        @Override
        public void actionPerformed(ActionEvent<ClientConfigurationView.Action> actionEvent) {
            switch (actionEvent.getActionId()) {
            case CLOSE_CANCEL:
                uiFacadeFactory.shutdown(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
                break;
            
            case CLOSE_ACCEPT:
            default:
                connect(connection, service);
                break;
            }
        }
    }
    
    private void createPreferencesDialog(final Connection connection, final ExecutorService service) {

        ClientPreferences prefs = new ClientPreferences(OSGIUtils.getInstance().getService(Keyring.class));
        ClientConfigurationView configDialog = new ClientConfigurationSwing();
        ClientConfigurationController controller =
                new ClientConfigurationController(prefs, configDialog);
        
        configDialog.addListener(new ConfigDialogListener(connection, service));
        controller.showDialog();
    }
    
    private class ConnectionHandler implements ConnectionListener {
        private boolean retry;
        private Connection connection;
        private ExecutorService service;
        public ConnectionHandler(Connection connection, ExecutorService service) {
            this.connection = connection;
            this.retry = true;
            this.service = service;
        }
        
        private void showConnectionAttemptWarning() {
            SwingUtilities.invokeLater(new ConnectionAttemp(connection, service));
        }
        
        @Override
        public void changed(ConnectionStatus newStatus) {
            if (newStatus == ConnectionStatus.CONNECTED) {
                showMainWindow();
            } else if (newStatus == ConnectionStatus.FAILED_TO_CONNECT) {
                if (retry) {
                    retry = false;
                    showConnectionAttemptWarning();
                } else {
                    JOptionPane.showMessageDialog(
                            null,
                            localize(LocaleResources.CONNECTION_FAILED_TO_CONNECT_DESCRIPTION),
                            localize(LocaleResources.CONNECTION_FAILED_TO_CONNECT_TITLE),
                            JOptionPane.ERROR_MESSAGE);
                    uiFacadeFactory.shutdown(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
                }
            }
        }
    }
    
    private void showMainWindow() {
        SwingUtilities.invokeLater(new ShowMainWindow());
    }
    
    private class ShowMainWindow implements Runnable {
        @Override
        public void run() {
            MainWindowController mainController = uiFacadeFactory.getMainWindow();
            mainController.showMainMainWindow();                        
        }
    }
}
