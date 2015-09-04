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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.common.ApplicationInfo;
import com.redhat.thermostat.setup.command.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;

public class StartView extends JPanel implements SetupView {

    private JButton nextBtn;
    private JButton cancelBtn;
    private JButton moreInfoBtn;
    private JRadioButton quickSetupBtn;
    private JRadioButton customSetupBtn;

    private JPanel toolbar;
    private JEditorPane thermostatBlurb;
    private JPanel midPanel;

    private static final String THERMOSTAT_LOGO = "thermostat.png";
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final String SHOW_MORE = "Show More";
    private static final String SHOW_LESS = "Show Less";
    private static final String PROGRESS = "Step 1 of 3";
    private static final String FAST_TRACK_PROGRESS = "Step 1 of 2";

    public StartView(LayoutManager layout) {
        super(layout);

        createToolbarPanel();
        createMidPanel();
    }

    @Override
    public void setTitle(JLabel title) {
        title.setText(translator.localize(LocaleResources.WELCOME_SCREEN_TITLE).getContents());
    }

    @Override
    public void setProgress(JLabel progress) {
        if (quickSetupBtn.isSelected()) {
            progress.setText(FAST_TRACK_PROGRESS);
        } else {
            progress.setText(PROGRESS);
        }
    }

    private void createMidPanel() {
        thermostatBlurb = new JEditorPane();
        thermostatBlurb.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
        thermostatBlurb.setEditable(false);
        thermostatBlurb.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (Desktop.isDesktopSupported()) {
                        String userGuideURL = new ApplicationInfo().getUserGuide();
                        try {
                            Desktop.getDesktop().browse(new URI(userGuideURL));
                        } catch (IOException | URISyntaxException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        });
        JScrollPane scrollPane = new ThermostatScrollPane(thermostatBlurb);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        moreInfoBtn = new JButton();
        moreInfoBtn.setPreferredSize(new Dimension(150, 30));
        moreInfoBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        showMoreInfo(false);

        //Create radio buttons.
        int radioBtnOffset = SetupWindow.getFrameWidth() / 3;

        quickSetupBtn = new JRadioButton(translator.localize(LocaleResources.QUICK_SETUP).getContents());
        quickSetupBtn.setToolTipText(translator.localize(LocaleResources.QUICK_SETUP_INFO).getContents());
        quickSetupBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        quickSetupBtn.setBorder(BorderFactory.createEmptyBorder(0, radioBtnOffset, 0, 0));
        quickSetupBtn.setSelected(true);

        customSetupBtn = new JRadioButton(translator.localize(LocaleResources.CUSTOM_SETUP).getContents());
        customSetupBtn.setToolTipText(translator.localize(LocaleResources.CUSTOM_SETUP_INFO).getContents());
        customSetupBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        customSetupBtn.setBorder(BorderFactory.createEmptyBorder(0, radioBtnOffset, 0, 0));

        //Group the radio buttons.
        ButtonGroup setupOptionsGroup = new ButtonGroup();
        setupOptionsGroup.add(quickSetupBtn);
        setupOptionsGroup.add(customSetupBtn);

        midPanel = new JPanel();
        midPanel.setLayout(new BoxLayout(midPanel, BoxLayout.Y_AXIS));
        midPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        midPanel.add(scrollPane);
        midPanel.add(moreInfoBtn);
        midPanel.add(quickSetupBtn);
        midPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        midPanel.add(customSetupBtn);

        this.add(midPanel, BorderLayout.CENTER);
    }

    private void createToolbarPanel() {
        URL logoURL = SetupWindow.class.getClassLoader().getResource(THERMOSTAT_LOGO);
        JLabel thermostatLogo = new JLabel(new ImageIcon(logoURL));

        nextBtn = new JButton(translator.localize(LocaleResources.NEXT).getContents());
        nextBtn.setPreferredSize(new Dimension(70, 30));
        cancelBtn = new JButton(translator.localize(LocaleResources.CANCEL).getContents());
        cancelBtn.setPreferredSize(new Dimension(70, 30));

        toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.LINE_AXIS));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        toolbar.add(thermostatLogo);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(nextBtn);
        toolbar.add(cancelBtn);

        this.add(toolbar, BorderLayout.SOUTH);
    }

    public void showMoreInfo(boolean setDetailed) {
        String userGuideURL = new ApplicationInfo().getUserGuide();
        StringBuilder text = new StringBuilder();

        text.append("<html>");
        if (setDetailed) {
            text.append(translator.localize(LocaleResources.THERMOSTAT_BLURB).getContents());
            moreInfoBtn.setText(SHOW_LESS);
        } else {
            text.append(translator.localize(LocaleResources.THERMOSTAT_BRIEF).getContents());
            moreInfoBtn.setText(SHOW_MORE);
        }
        text.append("<center><a href=\"\">")
            .append(userGuideURL)
            .append("</a></center>")
            .append("</html>").toString();

        thermostatBlurb.setText(text.toString());
    }

    @Override
    public Component getUiComponent() {
        return this;
    }

    public JButton getShowMoreInfoBtn() {
        return moreInfoBtn;
    }

    public JButton getNextBtn() {
        return nextBtn;
    }

    public JButton getCancelBtn() {
        return cancelBtn;
    }

    @Override
    public void setDefaultButton() {
        getRootPane().setDefaultButton(nextBtn);
    }

    @Override
    public void focusInitialComponent() {
        nextBtn.requestFocusInWindow();
    }

    public boolean isQuickSetupSelected() {
        return quickSetupBtn.isSelected();
    }

    public JRadioButton getCustomSetupBtn() {
        return customSetupBtn;
    }

    public JRadioButton getQuickSetupBtn() {
        return quickSetupBtn;
    }

    public void enableButtons() {
        nextBtn.setEnabled(true);
        quickSetupBtn.setEnabled(true);
        customSetupBtn.setEnabled(true);
    }

    public void disableButtons() {
        nextBtn.setEnabled(false);
        quickSetupBtn.setEnabled(false);
        customSetupBtn.setEnabled(false);
    }

}
