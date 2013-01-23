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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.swing.EdtHelper;
import com.redhat.thermostat.client.swing.IconResource;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.locale.Translate;

/**
 * A swing component meant for entering search terms.
 * <p>
 * Similar to other swing components, this component should only be
 * modified on the swing EDT.
 */
public class SearchField extends JPanel {

    /** For use by tests only */
    public static final String VIEW_NAME = "searchField";

    public enum SearchAction {
        TEXT_CHANGED,
        PERFORM_SEARCH,
    }

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final ActionNotifier<SearchAction> notifier = new ActionNotifier<>(this);
    private final JTextField searchField = new JTextField();

    private final AtomicReference<String> searchText = new AtomicReference<String>("");
    private final AtomicReference<String> label = new AtomicReference<>(translator.localize(LocaleResources.SEARCH_HINT));
    private final AtomicBoolean labelDisplayed = new AtomicBoolean(true);

    public SearchField() {
        super(new BorderLayout());

        // TODO move this icon inside the search field
        JLabel searchIcon = new JLabel(IconResource.SEARCH.getIcon());
        searchIcon.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        searchField.setText(label.get());
        searchField.setName(VIEW_NAME);
        /* the insets are so we can place the actual icon inside the searchField */
        searchField.setMargin(new Insets(0, 0, 0, 30));

        searchField.getDocument().addDocumentListener(new DocumentListener() {

            private String previousText = searchText.get();

            @Override
            public void removeUpdate(DocumentEvent event) {
                changed(event.getDocument());
            }

            @Override
            public void insertUpdate(DocumentEvent event) {
                changed(event.getDocument());
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                changed(event.getDocument());
            }

            private void changed(Document doc) {
                if (!labelDisplayed.get()) {
                    String filter = null;
                    try {
                        filter = doc.getText(0, doc.getLength());
                    } catch (BadLocationException ble) {
                        // ignore
                    }

                    searchText.set(filter);
                    if (!(filter.equals(previousText))) {
                        previousText = filter;
                        fireViewAction(SearchAction.TEXT_CHANGED);
                    }
                }
            }
        });

        final Color originalForegroundColor = searchField.getForeground();
        searchField.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
                if (searchText.get().equals("")) {
                    labelDisplayed.set(true);
                    searchField.setForeground(Color.GRAY);
                    searchField.setText(label.get());
                }
            }

            @Override
            public void focusGained(FocusEvent e) {
                if (labelDisplayed.get()) {
                    labelDisplayed.set(false);
                    searchField.setForeground(originalForegroundColor);
                    searchField.setText("");
                }

            }
        });

        final java.awt.event.ActionListener searchActionListener = new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireViewAction(SearchAction.PERFORM_SEARCH);
            }
        };

        searchField.addActionListener(searchActionListener);

        add(searchField);
        add(searchIcon, BorderLayout.LINE_END);

    }

    public String getSearchText() {
        return searchText.get();
    }

    public void setSearchText(final String text) {
        searchText.set(text);
        searchField.setText(text);
    }

    public void setLabel(String label) {
        this.label.set(label);
        if (labelDisplayed.get()) {
            searchField.setText(this.label.get());
        }
    }

    public void setTooltip(final String tooltip) {
        searchField.setToolTipText(tooltip);
    }

    public void addActionListener(ActionListener<SearchAction> listener) {
        notifier.addActionListener(listener);
    }

    public void removeActionListener(ActionListener<SearchAction> listener) {
        notifier.removeActionListener(listener);
    }

    private void fireViewAction(SearchAction action) {
        notifier.fireAction(action);
    }
}

