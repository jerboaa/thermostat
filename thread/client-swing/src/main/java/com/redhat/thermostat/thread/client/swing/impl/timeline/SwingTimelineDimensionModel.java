/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.thread.client.swing.impl.timeline;

import com.redhat.thermostat.thread.client.common.model.timeline.TimelineDimensionModel;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 */
public class SwingTimelineDimensionModel implements TimelineDimensionModel {

    /**
     * Default increment is 20 pixels per units.
     * Subclasses may use different values.
     *
     * @see #DEFAULT_INCREMENT_IN_MILLIS
     */
    public static final int DEFAULT_INCREMENT_IN_PIXELS = 20;

    /**
     * Default increments is 1 second (1000 ms) per pixel unit.
     * Subclasses may use different values.
     *
     * @see #DEFAULT_INCREMENT_IN_PIXELS
     */
    public static final long DEFAULT_INCREMENT_IN_MILLIS = 1_000;

    private PropertyChangeSupport propertyChangeSupport;
    
    private int width;
    private int incrementInPixels = DEFAULT_INCREMENT_IN_PIXELS;
    private long incrementInMillis = DEFAULT_INCREMENT_IN_MILLIS;
    
    private volatile long length;
    
    public SwingTimelineDimensionModel() {
        propertyChangeSupport = new PropertyChangeSupport(this);
    }
    
    public void setWidth(int width) {
        this.width = width;

        computeLength();
    }

    public int getWidth() {
        return width;
    }

    public void setIncrement(int pixels, long millis) {
        this.incrementInMillis = millis;
        this.incrementInPixels = pixels;

        computeLength();
    }

    public int getIncrementInPixels() {
        return incrementInPixels;
    }

    public long getIncrementInMillis() {
        return incrementInMillis;
    }

    public int getLengthInPixels() {
        return width / incrementInPixels;
    }

    private void computeLength() {

        long oldLength = length;

        int lengthInPixels = getLengthInPixels();
        length = incrementInMillis * lengthInPixels;

        propertyChangeSupport.firePropertyChange(LENGTH_PROPERTY, oldLength, length);
    }
    
    @Override
    public long getLengthInMillis() {
        return length;
    }
    
    @Override
    public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(property, listener);
    }
}
