package com.redhat.thermostat.client;

import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.ConnectionInfo.ConnectionType;
import com.redhat.thermostat.client.ui.ConnectionSelectionDialog;
import com.redhat.thermostat.client.ui.MainWindow;

public class Main {

    private static void showGui() {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        ConnectionInfo model = new ConnectionInfo();

        ConnectionSelectionDialog dialog = new ConnectionSelectionDialog((JFrame) null, model);
        dialog.pack();
        dialog.setModal(true);
        dialog.setVisible(true);

        if (model.getType() == ConnectionType.NONE) {
            return;
        }

        ThermostatFacade facade = new DummyFacade();

        MainWindow gui = new MainWindow(facade);
        gui.setStartupMode(model.getType());
        gui.pack();
        gui.setVisible(true);
    }

    public static void main(String[] args) {
        ClientArgs arguments = new ClientArgs(args);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                showGui();
            }
        });
    }
}
