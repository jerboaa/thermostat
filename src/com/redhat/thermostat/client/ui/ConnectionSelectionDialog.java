package com.redhat.thermostat.client.ui;

import static com.redhat.thermostat.client.Translate._;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.redhat.thermostat.client.Connection;
import com.redhat.thermostat.client.Connection.ConnectionType;

public class ConnectionSelectionDialog extends JDialog {

    private static final long serialVersionUID = -3149845673473434408L;

    private static final int ICON_LABEL_GAP = 5;

    private final Connection model;

    public ConnectionSelectionDialog(JFrame owner, Connection model) {
        super(owner);
        setTitle(_("STARTUP_MODE_SELECTION_DIALOG_TITLE"));
        this.model = model;
        setupUi();
    }

    private void setupUi() {
        BorderLayout layout = new BorderLayout();
        setLayout(layout);
        add(createModeSelectionUi(), BorderLayout.CENTER);

        FlowLayout bottomPanelLayout = new FlowLayout(FlowLayout.TRAILING);
        JPanel bottomPanel = new JPanel(bottomPanelLayout);
        add(bottomPanel, BorderLayout.PAGE_END);
        bottomPanel.add(Box.createGlue());

        JPanel buttonsPanel = new JPanel(new GridLayout(1, 5, ICON_LABEL_GAP, ICON_LABEL_GAP));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.add(buttonsPanel);

        JButton cancelButton = new JButton(_("BUTTON_CANCEL"));
        cancelButton.setMargin(new Insets(0, 15, 0, 15));
        cancelButton.addActionListener(new SetStartupModeListener(this, ConnectionType.NONE));
        buttonsPanel.add(cancelButton);
    }

    private JPanel createModeSelectionUi() {
        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createEmptyBorder(ICON_LABEL_GAP, ICON_LABEL_GAP, ICON_LABEL_GAP, ICON_LABEL_GAP));
        GridBagLayout layout = new GridBagLayout();
        container.setLayout(layout);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        Insets normalInsets = new Insets(ICON_LABEL_GAP, ICON_LABEL_GAP, ICON_LABEL_GAP, ICON_LABEL_GAP);
        c.insets = normalInsets;
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 0;

        JLabel info = new JLabel(_("STARTUP_MODE_SELECTION_INTRO"));
        container.add(info, c);

        c.gridy++;
        String localButtonHtml = buildHtml(_("STARTUP_MODE_SELECTION_TYPE_LOCAL"), IconResource.COMPUTER.getUrl());
        JButton localButton = new JButton(localButtonHtml);
        container.add(localButton, c);

        c.gridy++;
        String remoteButtonHtml = buildHtml(_("STARTUP_MODE_SELECTION_TYPE_REMOTE"), IconResource.NETWORK_SERVER.getUrl());
        JButton remoteButton = new JButton(remoteButtonHtml);
        container.add(remoteButton, c);

        c.gridy++;
        String clusterButtonHtml = buildHtml(_("STARTUP_MODE_SELECTION_TYPE_CLUSTER"), IconResource.NETWORK_GROUP.getUrl());
        JButton clusterButton = new JButton(clusterButtonHtml);
        container.add(clusterButton, c);

        localButton.addActionListener(new SetStartupModeListener(this, ConnectionType.LOCAL));
        remoteButton.addActionListener(new SetStartupModeListener(this, ConnectionType.REMOTE));
        clusterButton.addActionListener(new SetStartupModeListener(this, ConnectionType.CLUSTER));
        return container;
    }

    private String buildHtml(String text, String imageUrl) {
        /* build a table to vertically align image and text properly */
        // TODO does not deal correctly with right-to-left languages
        String html = "" +
                "<html>" +
                " <table>" +
                "  <tr> " +
                "   <td> " + "<img src='" + imageUrl + "'>" + "</td>" +
                "   <td>" + new HtmlTextBuilder().huge(text).toHtml() + "</td>" +
                "  </tr>" +
                " </table>" +
                "</html>";
        return html;
    }

    public Connection getModel() {
        return model;
    }

    private static class SetStartupModeListener implements ActionListener {
        private final ConnectionType mode;
        private final ConnectionSelectionDialog window;

        public SetStartupModeListener(ConnectionSelectionDialog frame, ConnectionType mode) {
            this.mode = mode;
            this.window = frame;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            window.getModel().setType(mode);
            window.setVisible(false);
            window.dispose();
        }
    }
}
