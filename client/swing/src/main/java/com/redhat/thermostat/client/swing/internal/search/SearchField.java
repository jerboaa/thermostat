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

package com.redhat.thermostat.client.swing.internal.search;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.ThermostatTextField;
import com.redhat.thermostat.client.ui.Palette;

@SuppressWarnings("serial")
public class SearchField extends BaseSearchProvider {

    private ThermostatTextField searchField;
    
    public SearchField() {
        setFocusable(true);
        
        setLayout(new BorderLayout());
        
        final Icon searchIcon =
                new FontAwesomeIcon('\uf002', 12, Palette.DARK_GRAY.getColor());
                
        searchField = new ThermostatTextField(20);
        
        final JLabel searchLabel = new JLabel(searchIcon);

        JPanel iconPanel = new JPanel();
        iconPanel.setLayout(new BorderLayout());
        iconPanel.add(searchLabel, BorderLayout.CENTER);
        iconPanel.add(Box.createRigidArea(new Dimension(2, 5)), BorderLayout.WEST);

        iconPanel.setOpaque(false);
        
        add(iconPanel, BorderLayout.WEST);
        add(searchField, BorderLayout.CENTER);
        
        searchField.setBackground(Palette.WHITE.getColor());
        searchField.setBorder(new EmptyBorder(0, 5, 0, 0));
        searchField.setForeground(Palette.DARK_GRAY.getColor());
        
        searchField.getCaret().setBlinkRate(0);
        searchField.setCaretColor(Palette.DARK_GRAY.getColor());

        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                Document document = e.getDocument();
                try {
                    String text = document.getText(0, document.getLength());
                    fireViewAction(SearchAction.PERFORM_SEARCH, text);
                    
                } catch (BadLocationException ignore) {}
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        GraphicsUtils utils = GraphicsUtils.getInstance();
        Graphics2D graphics = utils.createAAGraphics(g);

        graphics.setPaint(Palette.WHITE.getColor());
        
        Shape shape = utils.getRoundShape(getWidth(), getHeight());
        graphics.fill(shape);
        
        graphics.dispose();
    }
}

