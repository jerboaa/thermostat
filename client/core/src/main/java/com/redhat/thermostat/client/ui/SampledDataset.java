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

package com.redhat.thermostat.client.ui;

import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * Represents a value sampled between two points in time.
 */
public class SampledDataset extends AbstractXYDataset implements IntervalXYDataset {

    private static final String SERIES_KEY = "0";
    private static final int DEFAULT_SIZE = 32;

    private long[] xStart = new long[DEFAULT_SIZE];
    private long[] xEnd = new long[DEFAULT_SIZE];
    private double[] value = new double[DEFAULT_SIZE];

    private int count = 0;

    public void add(long startX, long endX, double value) {
        if (xStart.length == count) {
            long[] newXStart = new long[xStart.length * 2];
            System.arraycopy(xStart, 0, newXStart, 0, xStart.length);
            xStart = newXStart;
            long[] newXEnd = new long[xEnd.length * 2];
            System.arraycopy(xEnd, 0, newXEnd, 0, xEnd.length);
            xEnd = newXEnd;
            double[] newValue = new double[this.value.length * 2];
            System.arraycopy(this.value, 0, newValue, 0, this.value.length);
            this.value = newValue;
        }
        xStart[count] = startX;
        xEnd[count] = endX;
        this.value[count] = value;
        count++;
    }

    public void clear() {
        count = 0;
        fireDatasetChanged();
    }

    @Override
    public int getItemCount(int series) {
        checkSeries(series);
        return count;
    }

    @Override
    public Number getX(int series, int item) {
        checkSeries(series);
        checkItemIndex(item);
        return xStart[item];
    }

    @Override
    public Number getY(int series, int item) {
        checkSeries(series);
        checkItemIndex(item);
        return value[item];
    }

    @Override
    public Number getStartX(int series, int item) {
        checkSeries(series);
        checkItemIndex(item);
        return xStart[item];
    }

    @Override
    public double getStartXValue(int series, int item) {
        checkSeries(series);
        checkItemIndex(item);
        return xStart[item];
    }

    @Override
    public Number getEndX(int series, int item) {
        checkSeries(series);
        checkItemIndex(item);
        return xEnd[item];
    }

    @Override
    public double getEndXValue(int series, int item) {
        checkSeries(series);
        checkItemIndex(item);
        return xEnd[item];
    }

    @Override
    public Number getStartY(int series, int item) {
        checkSeries(series);
        checkItemIndex(item);
        return value[item];
    }


    @Override
    public double getStartYValue(int series, int item) {
        checkSeries(series);
        checkItemIndex(item);
        return value[item];
    }

    @Override
    public Number getEndY(int series, int item) {
        checkSeries(series);
        checkItemIndex(item);
        return value[item];
    }

    @Override
    public double getEndYValue(int series, int item) {
        checkSeries(series);
        checkItemIndex(item);
        return value[item];
    }

    @Override
    public int getSeriesCount() {
        return 1;
    }

    @Override
    public Comparable<String> getSeriesKey(int series) {
        checkSeries(series);
        return SERIES_KEY;
    }

    @Override
    public int indexOf(Comparable seriesKey) {
        if (seriesKey == null) {
            return -1;
        }
        if (seriesKey.compareTo(SERIES_KEY) == 0) {
            return 0;
        }
        return -1;
    }

    private void checkSeries(int series) {
        if (series != 0) {
            throw new IllegalArgumentException(this.getClass().getName() + " supports only 1 series");
        }
    }

    private void checkItemIndex(int index) {
        if (index >= (count)) {
            throw new IllegalArgumentException(this.getClass().getName() +  " supports only 1 series");
        }
    }

    public void fireSeriesChanged() {
        fireDatasetChanged();
    }

}

