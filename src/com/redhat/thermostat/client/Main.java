package com.redhat.thermostat.client;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.Connection.ConnectionType;
import com.redhat.thermostat.client.ui.ConnectionSelectionDialog;
import com.redhat.thermostat.client.ui.MainWindow;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class Main {

    private static final Logger logger = LoggingUtils.getLogger(Main.class);

    private ClientArgs arguments;

    private void showGui() {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        Connection connection;

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
            connection = null; // TODO replace with actual connection object
        }

        ConnectionSelectionDialog dialog = new ConnectionSelectionDialog((JFrame) null, connection);
        dialog.pack();
        dialog.setModal(true);
        dialog.setVisible(true);

        if (connection.getType() == ConnectionType.NONE) {
            return;
        }

        connection.connect();
        UiFacadeFactory.setConnection(connection);

        MainWindow gui = new MainWindow(UiFacadeFactory.getMainWindow());
        gui.pack();
        gui.setVisible(true);
    }

    public static void main(String[] args) {
        LoggingUtils.setGlobalLogLevel(Level.ALL);

        final Main main = new Main();
        main.initArgs(args);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                main.showGui();
            }
        });
    }

    public void initArgs(String[] args) {
        this.arguments = new ClientArgs(args);
    }
}
