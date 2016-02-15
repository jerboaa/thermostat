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

package com.redhat.thermostat.storage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static com.redhat.thermostat.common.utils.IteratorUtils.head;

public class TimeStampedPojoCorrelator implements Iterable<TimeStampedPojoCorrelator.Correlation> {

    public static class Correlation {

        private long timestamp;
        private TimeStampedPojo[] correlation;

        private Correlation(long timestamp, TimeStampedPojo[] correlation) {
            this.timestamp = timestamp;
            this.correlation = correlation;
        }

        public TimeStampedPojo get(int i) {
            return correlation[i];
        }

        public long getTimeStamp() {
            return timestamp;
        }
        
    }

    private static class TimeStampedPojoComparator implements Comparator<TimeStampedPojo> {

        @Override
        public int compare(TimeStampedPojo o1, TimeStampedPojo o2) {
            return Long.compare(o1.getTimeStamp(), o2.getTimeStamp());
        }

        
    }

    private class Correlator implements Iterator<Correlation> {

        private List<Iterator<TimeStampedPojo>> seriesIterators;

        private Correlator() {
            seriesIterators = new ArrayList<>();
            int index = 0;
            for (List<TimeStampedPojo> series : seriesList) {
                Iterator<TimeStampedPojo> seriesIterator = series.iterator();
                seriesIterators.add(seriesIterator);
                if (seriesIterator.hasNext()) {
                    current[index] = seriesIterator.next();
                }
                index++;
            }
            
        }

        @Override
        public boolean hasNext() {
            boolean hasNext = false;
            for (TimeStampedPojo pojo : current) {
                hasNext |= pojo != null;
            }
            return hasNext;
        }

        @Override
        public Correlation next() {
            long minTimestamp = Long.MAX_VALUE;
            for (TimeStampedPojo pojo : current) {
                if (pojo != null) {
                    minTimestamp = Math.min(minTimestamp, pojo.getTimeStamp());
                }
            }
            TimeStampedPojo[] next = new TimeStampedPojo[numSeries];
            for (int i = 0; i < numSeries; i++) {
                if (current[i] != null && current[i].getTimeStamp() == minTimestamp) {
                    next[i] = current[i];
                    Iterator<TimeStampedPojo> iterator = seriesIterators.get(i);
                    current[i] = head(iterator);
                } else {
                    next[i] = last != null ? last.get(i) : null;
                }
            }
            last = new Correlation(minTimestamp, next);
            return last;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }

    private int numSeries;

    private List<List<TimeStampedPojo>> seriesList;

    private TimeStampedPojo[] current;
    private Correlation last;

    public TimeStampedPojoCorrelator(int numSeries) {
        this.numSeries = numSeries;
        seriesList = new ArrayList<>();
        for (int i = 0; i < numSeries; i++) {
            seriesList.add(new ArrayList<TimeStampedPojo>());
        }
        current = new TimeStampedPojo[numSeries];
    }

    public void add(int seriesIndex, TimeStampedPojo timeStampedPojo) {
        List<? extends TimeStampedPojo> series = seriesList.get(seriesIndex);
        int insertIdx = Collections.binarySearch(series, timeStampedPojo, new TimeStampedPojoComparator());
        if (insertIdx < 0) {
            insertIdx = -(insertIdx + 1);
        }
        seriesList.get(seriesIndex).add(insertIdx, timeStampedPojo);
    }

    public Iterator<Correlation> iterator() {
        return new Correlator();
    }

    public void clear() {
        for (List<TimeStampedPojo> series : seriesList) {
            series.clear();
        }
    }

}

