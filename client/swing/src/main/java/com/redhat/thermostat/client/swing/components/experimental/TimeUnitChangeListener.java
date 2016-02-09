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

package com.redhat.thermostat.client.swing.components.experimental;

import java.util.concurrent.TimeUnit;

import javax.swing.JComboBox;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Duration;

public class TimeUnitChangeListener implements DocumentListener, java.awt.event.ActionListener {

    public enum TimeChangeEvent {
        /** Payload is the duration */
        TIME_CHANGE_EVENT;
    }


    private final ActionListener<TimeChangeEvent> listener;
    private int value;
    private TimeUnit unit;

    public TimeUnitChangeListener(ActionListener<TimeChangeEvent> listener, Duration defaultDuration) {
        this.listener = listener;
        this.value = defaultDuration.getValue();
        this.unit = defaultDuration.getUnit();
    }

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
        try {
            this.value = Integer.valueOf(doc.getText(0, doc.getLength()));
        } catch (NumberFormatException nfe) {
            // ignore
        } catch (BadLocationException ble) {
            // ignore
        }
        fireTimeChanged();
    }

    private void fireTimeChanged() {
        ActionEvent<TimeChangeEvent> e = new ActionEvent<>(this, TimeChangeEvent.TIME_CHANGE_EVENT);
        e.setPayload(new Duration(this.value, this.unit));
        listener.actionPerformed(e);
    }

    @Override
    public void actionPerformed(final java.awt.event.ActionEvent e) {
        @SuppressWarnings("unchecked") // We are a TimeUnitChangeListener, specifically.
                JComboBox<TimeUnit> comboBox = (JComboBox<TimeUnit>) e.getSource();
        TimeUnit time = (TimeUnit) comboBox.getSelectedItem();
        this.unit = time;
        fireTimeChanged();
    }

}