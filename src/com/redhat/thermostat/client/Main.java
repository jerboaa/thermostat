package com.redhat.thermostat.client;

import static com.redhat.thermostat.client.Translate._;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.Connection.ConnectionListener;
import com.redhat.thermostat.client.Connection.ConnectionStatus;
import com.redhat.thermostat.client.Connection.ConnectionType;
import com.redhat.thermostat.client.ui.ConnectionSelectionDialog;
import com.redhat.thermostat.client.ui.MainWindow;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class Main {

    private static final Logger logger = LoggingUtils.getLogger(Main.class);

    private ClientArgs arguments;
    private Connection connection;
    private UiFacadeFactory uiFacadeFactory;

    private Main(String[] args) {
        this.arguments = new ClientArgs(args);

        if (arguments.useDummyDataSource()) {
            logger.log(Level.CONFIG, "using dummy data");
            connection = new Connection() {
                @Override
                public void disconnect() {
                    /* no op */
                }

                @Override
                public void connect() {
                    /* no op */
                }
            };
        } else {
            connection = new MongoConnection();
        }
    }

    private void showGui() {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        ConnectionSelectionDialog dialog = new ConnectionSelectionDialog((JFrame) null, connection);
        dialog.pack();
        dialog.setModal(true);
        dialog.setVisible(true);

        if (connection.getType() == ConnectionType.NONE) {
            return;
        }

        ConnectionListener connectionListener = new ConnectionListener() {
            @Override
            public void changed(ConnectionStatus newStatus) {
                if (newStatus == ConnectionStatus.FAILED_TO_CONNECT) {
                    JOptionPane.showMessageDialog(
                            null,
                            _("CONNECTION_FAILED_TO_CONNECT_DESCRIPTION"),
                            _("CONNECTION_FAILED_TO_CONNECT_TITLE"),
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
                }
            }
        };

        connection.addListener(connectionListener);
        connection.connect();
        connection.removeListener(connectionListener);

        if (arguments.useDummyDataSource()) {
            uiFacadeFactory = new DummyUiFacadeFactory();
        } else {
            uiFacadeFactory = new UiFacadeFactoryImpl((MongoConnection) connection);
        }

        MainWindow gui = new MainWindow(uiFacadeFactory);
        gui.pack();
        gui.setVisible(true);
    }

    public static void main(String[] args) {
        LoggingUtils.setGlobalLogLevel(Level.ALL);

        final Main main = new Main(args);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                main.showGui();
            }
        });
    }

}
