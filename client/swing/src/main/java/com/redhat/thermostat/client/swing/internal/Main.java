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

package com.redhat.thermostat.client.swing.internal;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.osgi.framework.BundleContext;

import com.redhat.thermostat.client.core.views.ClientConfigurationView;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.swing.EdtHelper;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.internal.views.ClientConfigurationSwing;
import com.redhat.thermostat.client.swing.internal.vmlist.UIDefaultsImpl;
import com.redhat.thermostat.client.ui.ClientConfigReconnector;
import com.redhat.thermostat.client.ui.ClientConfigurationController;
import com.redhat.thermostat.client.ui.ClientPreferencesModel;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.internal.utils.laf.ThemeManager;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.DbServiceFactory;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.utils.keyring.Keyring;

public class Main implements ClientConfigReconnector, ConnectionListener {
    
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final Logger logger = LoggingUtils.getLogger(Main.class);

    private BundleContext context;
    private ApplicationService appSvc;
    private DbServiceFactory dbServiceFactory;
    private Keyring keyring;
    private CountDownLatch shutdown;
    private ClientPreferencesCreator prefsCreator;
    private GUIInteractions interactions;
    private SSLConfiguration sslConf;

    private MainWindowControllerImpl mainController;

    private boolean retryConnecting = true;

    public Main(BundleContext context, Keyring keyring, CommonPaths paths, ApplicationService appSvc, SSLConfiguration sslConf) {

        DbServiceFactory dbServiceFactory = new DbServiceFactory();
        CountDownLatch shutdown = new CountDownLatch(1);
        ShowMainWindow mainWindowRunnable = new ShowMainWindow();
        ClientPreferencesCreator prefsCreator = new ClientPreferencesCreator(paths);
        GUIInteractions interactions = new GUIInteractions(context, keyring, prefsCreator, this, mainWindowRunnable);

        init(context, appSvc, dbServiceFactory, keyring, shutdown, prefsCreator, interactions, sslConf);
    }

    Main(BundleContext context, ApplicationService appSvc,
            DbServiceFactory dbServiceFactory, Keyring keyring,
            CountDownLatch shutdown, ClientPreferencesCreator prefsCreator, GUIInteractions interactions) {
        init(context, appSvc, dbServiceFactory, keyring, shutdown, prefsCreator, interactions, sslConf);
    }

    private void init(BundleContext context, ApplicationService appSvc,
            DbServiceFactory dbServiceFactory, Keyring keyring,
            CountDownLatch shutdown, ClientPreferencesCreator prefsCreator,
            GUIInteractions interactions, SSLConfiguration sslConf) {
        this.context = context;
        this.appSvc = appSvc;
        this.dbServiceFactory = dbServiceFactory;
        this.keyring = keyring;
        this.shutdown = shutdown;
        this.prefsCreator = prefsCreator;
        this.interactions = interactions;
        this.sslConf = sslConf;
    }
    
    public void run() {
        interactions.initializeLookAndFeel();

        tryConnecting();

        try {
            shutdown.await();

            // Shutdown MainController
            if (mainController != null) {
                mainController.shutdownApplication();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void tryConnecting() {
        final ExecutorService service = appSvc.getApplicationExecutor();
        ClientPreferences prefs = prefsCreator.create();
        StorageCredentials creds = new KeyringStorageCredentials(keyring, prefs);
        connect(prefs, creds, service);
    }

    @Override
    public void reconnect(ClientPreferences prefs, StorageCredentials creds) {
        connect(prefs, creds, appSvc.getApplicationExecutor());
    }

    private void connect(ClientPreferences prefs, StorageCredentials creds, ExecutorService service) {
        try {
            // create DbService with potentially modified parameters
            final DbService dbService = dbServiceFactory.createDbService(prefs.getConnectionUrl(), creds, sslConf);
            dbService.addConnectionListener(this);
            service.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        dbService.connect();
                    } catch (Throwable t) {
                        // Note: DbService fires a ConnectionListener event when it
                        // fails to connect. No need to notify our handler manually.
                        logger.log(Level.FINE, "connection attempt failed: ", t);
                    }
                }
            });
        } catch (StorageException se) {
            logger.log(Level.WARNING, "Unable to find appropriate storage provider", se);
            // Prevent Icedtea BZ#1294, where no matching StorageProvider
            // could potentially be found for the given connection URL.
            // Indicate connection failure immediately.
            changed(ConnectionStatus.FAILED_TO_CONNECT);
        }
    }

    @Override
    public void changed(ConnectionStatus newStatus) {
        if (newStatus == ConnectionStatus.CONNECTED) {
            interactions.showMainWindow();
        } else if (newStatus == ConnectionStatus.FAILED_TO_CONNECT) {
            if (retryConnecting) {
                retryConnecting = false;
                connectionAttemptFailed();
            } else {
                interactions.showFailedToConnectDialog();
                shutdown.countDown();
            }
        }
    }

    private void connectionAttemptFailed() {
        int userAction = interactions.showFailedToConnectDialogWithRetryOption();

        switch (userAction) {

        case JOptionPane.CANCEL_OPTION:
        case JOptionPane.CLOSED_OPTION:
        case JOptionPane.NO_OPTION:
            shutdown();
            break;

        case JOptionPane.YES_OPTION:
        default:
            interactions.showPreferencesDialog(appSvc.getApplicationExecutor());
            break;
        }
    }

    @Override
    public void abort() {
        shutdown();
    }

    public void shutdown() {
        shutdown.countDown();
    }

    /*
     * This Runnable is extracted to a class for testing purposes.
     */
    class ShowMainWindow implements Runnable {

        @Override
        public void run() {
            mainController = new MainWindowControllerImpl(context, appSvc, shutdown);
            mainController.showMainMainWindow();
        }
    }

    static class KeyringStorageCredentials implements StorageCredentials {

        private Keyring keyring;
        private ClientPreferences prefs;

        KeyringStorageCredentials(Keyring keyring, ClientPreferences prefs) {
            this.keyring = keyring;
            this.prefs = prefs;
        }

        @Override
        public String getUsername() {
            return prefs.getUserName();
        }

        @Override
        public char[] getPassword() {
            return keyring.getPassword(prefs.getConnectionUrl(), prefs.getUserName());
        }

    }

    static class GUIInteractions {

        private ClientConfigReconnector reconnector;
        private Keyring keyring;
        private ClientPreferencesCreator prefsCreator;
        private ShowMainWindow showMainWindow;
        private BundleContext context;

        public GUIInteractions(BundleContext context, Keyring keyring, ClientPreferencesCreator prefsCreator,
                ClientConfigReconnector reconnector, ShowMainWindow showMainWindow) {
            this.context = context;
            this.keyring = keyring;
            this.prefsCreator = prefsCreator;
            this.reconnector = reconnector;
            this.showMainWindow = showMainWindow;
        }

        // FIXME some methods in this class are synchronous (and so need to block)
        // others are async and call the appropriate thing when done (these dont have to block)

        void initializeLookAndFeel() {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ThemeManager themeManager = ThemeManager.getInstance();
                    themeManager.setLAF();

                    // this needs to be done after setting the laf, otherwise
                    // we will not get consistent colours
                    UIDefaults uiDefaults = UIDefaultsImpl.getInstance();
                    context.registerService(UIDefaults.class, uiDefaults, null);
                }
            });
        }
        void showPreferencesDialog(final ExecutorService service) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ClientPreferences prefs = prefsCreator.create();
                    ClientConfigurationView configDialog = new ClientConfigurationSwing();
                    ClientConfigurationController controller =
                            new ClientConfigurationController(new ClientPreferencesModel(keyring, prefs), configDialog, reconnector);
                    controller.showDialog();
                }
            });
        }

        /** Returns an _OPTION value from {@link JOptionPane} */
        int showFailedToConnectDialogWithRetryOption() {
            try {
                return (int) new EdtHelper().callAndWait(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        Object[] options = {
                                translator.localize(LocaleResources.CONNECTION_WIZARD).getContents(),
                                translator.localize(LocaleResources.CONNECTION_QUIT).getContents(),
                        };
                        int n = JOptionPane
                                .showOptionDialog(
                                        null,
                                        translator.localize(LocaleResources.CONNECTION_FAILED_TO_CONNECT_DESCRIPTION).getContents(),
                                        translator.localize(LocaleResources.CONNECTION_FAILED_TO_CONNECT_TITLE).getContents(),
                                        JOptionPane.OK_CANCEL_OPTION,
                                        JOptionPane.ERROR_MESSAGE, null, options,
                                        options[0]);
                        return n;
                    }
                });
            } catch (InvocationTargetException | InterruptedException e) {
                logger.log(Level.WARNING, "Error", e);
                return JOptionPane.CANCEL_OPTION;
            }
        }

        void showMainWindow() {
            SwingUtilities.invokeLater(showMainWindow);
        }

        void showFailedToConnectDialog() {
            try {
                new EdtHelper().callAndWait(new Runnable() {
                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(
                                null,
                                translator.localize(LocaleResources.CONNECTION_FAILED_TO_CONNECT_DESCRIPTION).getContents(),
                                translator.localize(LocaleResources.CONNECTION_FAILED_TO_CONNECT_TITLE).getContents(),
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (InvocationTargetException | InterruptedException e) {
                logger.log(Level.WARNING, "Error while waiting for user to dismiss dialog", e);
            }
        }
    }

    static class ClientPreferencesCreator {
        private CommonPaths paths;

        public ClientPreferencesCreator(CommonPaths paths) {
            this.paths = paths;
        }

        public ClientPreferences create() {
            return new ClientPreferences(paths);
        }
    }
}

