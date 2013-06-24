/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.shared.locale.LocalizedString;

/**
 * A component that host a panel with a nicely rendered header.
 */
@SuppressWarnings("serial")
public class HeaderPanel extends JPanel {
        
    public static final String SHOW_TEXT = "SHOW_TEXT";
    
    private boolean showText;
    
    private LocalizedString header;
    
    private JPanel contentPanel;
    private JLabel headerLabel;
    private JPanel headerPanel;
    private JPanel controlPanel;
    
    private boolean hasButtons;
    
    private Preferences prefs;
    
    public HeaderPanel() {
        this(LocalizedString.EMPTY_STRING);
    }
    
    public HeaderPanel(LocalizedString header) {
        this(Preferences.userRoot().node(HeaderPanel.class.getName()), header);
    }
    
    public HeaderPanel(Preferences prefs, LocalizedString header) {
                
        this.prefs = prefs;
        
        this.header = header;
        
        setLayout(new BorderLayout(0, 0));

        setName(HeaderPanel.class.getName());
        
        headerLabel = new ShadowLabel(header, new EmptyIcon(5, 5));
        headerPanel = new GradientPanel(Color.WHITE, getBackground());
        headerPanel.setName("clickableArea");
        headerPanel.setPreferredSize(new Dimension(HeaderPanel.this.getWidth(), 40));
        
        headerPanel.setLayout(new BorderLayout(0, 0));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);
        
        controlPanel = new JPanel();
        controlPanel.setOpaque(false);
        controlPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 10));
        
        headerPanel.add(controlPanel, BorderLayout.EAST);
        
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
        
        add(contentPanel, BorderLayout.CENTER);
        showText = prefs.getBoolean(HeaderPanel.class.getName(), false);
        registerPreferences();
        
        headerPanel.addMouseListener(new PreferencesPopupListener());
    }
   
    public boolean isShowToolbarText() {
        return showText;
    }
    
    private void registerPreferences() {
        prefs.addPreferenceChangeListener(new PreferenceChangeListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent evt) {
                
                String key = evt.getKey();
                boolean _value = false;
                if (key.equalsIgnoreCase(HeaderPanel.class.getName())) {
                    _value = prefs.getBoolean(HeaderPanel.class.getName(), false);
                }
                final boolean value = _value;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        boolean oldShowText = showText;
                        showText = value;
                        firePropertyChange(SHOW_TEXT, oldShowText, showText);
                    }
                });
            }
        });
    }
    
    public LocalizedString getHeader() {
        return header;
    }
    
    public void setHeader(LocalizedString header) {
        this.header = header;
        headerLabel.setText(header.getContents());
    }
    
    public void setContent(JComponent content) {
        contentPanel.removeAll();
        contentPanel.add(content);
        contentPanel.revalidate();
        repaint();
    }
    
    public void addToolBarButton(final ToolbarButton button) {
        AbstractButton theButton = button.getToolbarButton();
        button.toggleText(isShowToolbarText());
        addPropertyChangeListener(SHOW_TEXT, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                button.toggleText(isShowToolbarText());
            }
        });
        controlPanel.add(theButton);
        hasButtons = true;
    }
    
    class PreferencesPopup extends ThermostatPopupMenu {
        JMenuItem preferencesMenu;
        public PreferencesPopup() {
            // TODO: localize
            String text = "Show button text";
            if (showText) {
                text = "Hide button text";
            }
            preferencesMenu = new JMenuItem(text);
            preferencesMenu.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    prefs.putBoolean(HeaderPanel.class.getName(), !showText);
                }
            });
            add(preferencesMenu);
        }
    }

    class PreferencesPopupListener extends MouseAdapter {
        public void mousePressed(MouseEvent e){
            if (e.isPopupTrigger()) {
                popupPreferences(e);
            }
        }

        public void mouseReleased(MouseEvent e){
            if (e.isPopupTrigger()) {
                popupPreferences(e);
            }
        }

        private void popupPreferences(MouseEvent e){
            if (hasButtons) {
                PreferencesPopup menu = new PreferencesPopup();
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}

