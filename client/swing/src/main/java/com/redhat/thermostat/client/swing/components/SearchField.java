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

package com.redhat.thermostat.client.swing.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.IconResource;
import com.redhat.thermostat.client.swing.internal.search.BaseSearchProvider;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;

/**
 * A swing component meant for entering search terms.
 * <p>
 * Similar to other swing components, this component should only be
 * modified on the swing EDT.
 */
@SuppressWarnings("serial")
public class SearchField extends BaseSearchProvider {

    /** For use by tests only */
    public static final String VIEW_NAME = "searchField";

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final Color hintForegroundColor = Palette.DARK_GRAY.getColor();

    private final ThermostatTextField searchField;

    private final AtomicReference<String> searchText = new AtomicReference<>("");
    private final AtomicReference<LocalizedString> label = new AtomicReference<>(translator.localize(LocaleResources.SEARCH_HINT));
    private final AtomicBoolean labelDisplayed = new AtomicBoolean(true);
    private final Color originalForegroundColor;

    public SearchField() {
        this(0);
    }

    public SearchField(int columns) {
        searchField = new ThermostatTextField(columns);
        searchField.setName(VIEW_NAME);
        searchField.setBackground(Palette.WHITE.getColor());
        searchField.setBorder(new EmptyBorder(2, 5, 2, 2));
        searchField.getCaret().setBlinkRate(0);
        searchField.getDocument().addDocumentListener(new DocumentListener() {

            private String previousText = searchText.get();

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!labelDisplayed.get()) {
                    try {
                        Document doc = e.getDocument();
                        String filter = doc.getText(0, doc.getLength());
                        if (!filter.equals(previousText)) {
                            previousText = filter;
                            searchText.set(filter);
                            fireViewAction(SearchAction.PERFORM_SEARCH, filter);
                        }
                    } catch (BadLocationException ble) {
                        // ignore
                    }
                }
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

        originalForegroundColor = searchField.getForeground();
        searchField.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
                if (searchText.get().equals("")) {
                    setLabelEnabled(true);
                }
            }

            @Override
            public void focusGained(FocusEvent e) {
                if (labelDisplayed.get()) {
                    setLabelEnabled(false);
                }
            }
        });

        searchField.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireViewAction(SearchAction.PERFORM_SEARCH, searchField.getText());
            }
        });

        final JLabel searchLabel = new JLabel(IconResource.SEARCH.getIcon());
        JPanel iconPanel = new JPanel();
        iconPanel.setLayout(new BorderLayout());
        iconPanel.add(searchLabel, BorderLayout.CENTER);
        iconPanel.add(Box.createRigidArea(new Dimension(2, 5)), BorderLayout.WEST);
        iconPanel.setOpaque(false);

        setFocusable(true);
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        setLabelEnabled(true);
        add(iconPanel, BorderLayout.WEST);
        add(searchField, BorderLayout.CENTER);
    }

    private void setLabelEnabled(boolean isEnabled) {
        labelDisplayed.set(isEnabled);
        if (isEnabled) {
            searchField.setForeground(hintForegroundColor);
            searchField.setText(label.get().getContents());
        } else {
            searchField.setForeground(originalForegroundColor);
            searchField.setText("");
        }
    }

    public String getSearchText() {
        return searchText.get();
    }

    public void setSearchText(final String text) {
        searchText.set(text);
        searchField.setText(text);
    }

    public void setLabel(LocalizedString label) {
        this.label.set(label);
        if (labelDisplayed.get()) {
            setLabelEnabled(true);
        }
    }

    public void setTooltip(final LocalizedString tooltip) {
        searchField.setToolTipText(tooltip.getContents());
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

