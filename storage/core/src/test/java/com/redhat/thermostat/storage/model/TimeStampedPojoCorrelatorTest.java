/*
 * Copyright 2012 Red Hat, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;

import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.storage.model.TimeStampedPojo;
import com.redhat.thermostat.storage.model.TimeStampedPojoCorrelator;
import com.redhat.thermostat.storage.model.TimeStampedPojoCorrelator.Correlation;

public class TimeStampedPojoCorrelatorTest {

    private static class TestTimeStampedPojo extends BasePojo implements TimeStampedPojo {

        private long timestamp;

        private TestTimeStampedPojo(long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public long getTimeStamp() {
            return timestamp;
        }
        
    }

    @Test
    public void testOneSeries() {
        TimeStampedPojoCorrelator correlator = new TimeStampedPojoCorrelator(1);
        correlator.add(0, new TestTimeStampedPojo(3));
        correlator.add(0, new TestTimeStampedPojo(1));
        correlator.add(0, new TestTimeStampedPojo(2));

        Iterator<Correlation> i = correlator.iterator();

        assertTrue(i.hasNext());
        Correlation correlation1 = i.next();
        assertEquals(1, correlation1.get(0).getTimeStamp());
        assertEquals(1, correlation1.getTimeStamp());
        assertTrue(i.hasNext());
        Correlation correlation2 = i.next();
        assertEquals(2, correlation2.get(0).getTimeStamp());
        assertEquals(2, correlation2.getTimeStamp());
        assertTrue(i.hasNext());
        Correlation correlation3 = i.next();
        assertEquals(3, correlation3.get(0).getTimeStamp());
        assertEquals(3, correlation3.getTimeStamp());
        assertFalse(i.hasNext());
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testOneSeriesRemove() {
        TimeStampedPojoCorrelator correlator = new TimeStampedPojoCorrelator(1);
        correlator.add(0, new TestTimeStampedPojo(3));
        correlator.add(0, new TestTimeStampedPojo(1));
        correlator.add(0, new TestTimeStampedPojo(2));

        Iterator<Correlation> i = correlator.iterator();
        i.next();
        i.remove();
    }

    @Test
    public void test3SeriesInterleaving() {
        TimeStampedPojoCorrelator correlator = new TimeStampedPojoCorrelator(3);
        correlator.add(0, new TestTimeStampedPojo(9));
        correlator.add(0, new TestTimeStampedPojo(1));
        correlator.add(0, new TestTimeStampedPojo(4));
        correlator.add(1, new TestTimeStampedPojo(8));
        correlator.add(1, new TestTimeStampedPojo(2));
        correlator.add(1, new TestTimeStampedPojo(5));
        correlator.add(2, new TestTimeStampedPojo(7));
        correlator.add(2, new TestTimeStampedPojo(3));
        correlator.add(2, new TestTimeStampedPojo(6));

        Iterator<Correlation> i = correlator.iterator();
        assertNextCorrelation(i, 1, 1l, null, null);
        assertNextCorrelation(i, 2, 1l, 2l, null);
        assertNextCorrelation(i, 3, 1l, 2l, 3l);
        assertNextCorrelation(i, 4, 4l, 2l, 3l);
        assertNextCorrelation(i, 5, 4l, 5l, 3l);
        assertNextCorrelation(i, 6, 4l, 5l, 6l);
        assertNextCorrelation(i, 7, 4l, 5l, 7l);
        assertNextCorrelation(i, 8, 4l, 8l, 7l);
        assertNextCorrelation(i, 9, 9l, 8l, 7l);
        assertFalse(i.hasNext());
    }

    @Test
    public void test3SeriesColliding() {
        TimeStampedPojoCorrelator correlator = new TimeStampedPojoCorrelator(3);
        correlator.add(0, new TestTimeStampedPojo(3));
        correlator.add(0, new TestTimeStampedPojo(2));
        correlator.add(0, new TestTimeStampedPojo(1));
        correlator.add(1, new TestTimeStampedPojo(2));
        correlator.add(1, new TestTimeStampedPojo(1));
        correlator.add(1, new TestTimeStampedPojo(3));
        correlator.add(2, new TestTimeStampedPojo(1));
        correlator.add(2, new TestTimeStampedPojo(3));
        correlator.add(2, new TestTimeStampedPojo(2));

        Iterator<Correlation> i = correlator.iterator();
        assertNextCorrelation(i, 1, 1l, 1l, 1l);
        assertNextCorrelation(i, 2, 2l, 2l, 2l);
        assertNextCorrelation(i, 3, 3l, 3l, 3l);
        assertFalse(i.hasNext());
    }

    @Test
    public void test3SeriesMissing1() {
        TimeStampedPojoCorrelator correlator = new TimeStampedPojoCorrelator(3);
        correlator.add(0, new TestTimeStampedPojo(3));
        correlator.add(0, new TestTimeStampedPojo(2));
        correlator.add(0, new TestTimeStampedPojo(1));
        correlator.add(1, new TestTimeStampedPojo(2));
        correlator.add(1, new TestTimeStampedPojo(1));
        correlator.add(1, new TestTimeStampedPojo(3));

        Iterator<Correlation> i = correlator.iterator();
        assertNextCorrelation(i, 1, 1l, 1l, null);
        assertNextCorrelation(i, 2, 2l, 2l, null);
        assertNextCorrelation(i, 3, 3l, 3l, null);
        assertFalse(i.hasNext());
    }

    @Test
    public void test3SeriesEquals() {
        TimeStampedPojoCorrelator correlator = new TimeStampedPojoCorrelator(3);
        correlator.add(0, new TestTimeStampedPojo(1));
        correlator.add(0, new TestTimeStampedPojo(1));
        correlator.add(0, new TestTimeStampedPojo(1));
        correlator.add(1, new TestTimeStampedPojo(2));
        correlator.add(1, new TestTimeStampedPojo(2));
        correlator.add(1, new TestTimeStampedPojo(2));
        correlator.add(2, new TestTimeStampedPojo(3));
        correlator.add(2, new TestTimeStampedPojo(3));
        correlator.add(2, new TestTimeStampedPojo(3));

        Iterator<Correlation> i = correlator.iterator();
        assertNextCorrelation(i, 1, 1l, null, null);
        assertNextCorrelation(i, 1, 1l, null, null);
        assertNextCorrelation(i, 1, 1l, null, null);
        assertNextCorrelation(i, 2, 1l, 2l, null);
        assertNextCorrelation(i, 2, 1l, 2l, null);
        assertNextCorrelation(i, 2, 1l, 2l, null);
        assertNextCorrelation(i, 3, 1l, 2l, 3l);
        assertNextCorrelation(i, 3, 1l, 2l, 3l);
        assertNextCorrelation(i, 3, 1l, 2l, 3l);
        assertFalse(i.hasNext());
    }

    @Test
    public void test3SeriesContinuous() {
        TimeStampedPojoCorrelator correlator = new TimeStampedPojoCorrelator(3);
        correlator.add(0, new TestTimeStampedPojo(9));
        correlator.add(0, new TestTimeStampedPojo(1));
        correlator.add(0, new TestTimeStampedPojo(4));
        correlator.add(1, new TestTimeStampedPojo(8));
        correlator.add(1, new TestTimeStampedPojo(2));
        correlator.add(1, new TestTimeStampedPojo(5));
        correlator.add(2, new TestTimeStampedPojo(7));
        correlator.add(2, new TestTimeStampedPojo(3));
        correlator.add(2, new TestTimeStampedPojo(6));

        Iterator<Correlation> i = correlator.iterator();
        assertNextCorrelation(i, 1, 1l, null, null);
        assertNextCorrelation(i, 2, 1l, 2l, null);
        assertNextCorrelation(i, 3, 1l, 2l, 3l);
        assertNextCorrelation(i, 4, 4l, 2l, 3l);
        assertNextCorrelation(i, 5, 4l, 5l, 3l);
        assertNextCorrelation(i, 6, 4l, 5l, 6l);
        assertNextCorrelation(i, 7, 4l, 5l, 7l);
        assertNextCorrelation(i, 8, 4l, 8l, 7l);
        assertNextCorrelation(i, 9, 9l, 8l, 7l);
        assertFalse(i.hasNext());

        correlator.clear();
        correlator.add(0, new TestTimeStampedPojo(10));
        correlator.add(1, new TestTimeStampedPojo(11));
        correlator.add(2, new TestTimeStampedPojo(12));

        Iterator<Correlation> i2 = correlator.iterator();
        assertNextCorrelation(i2, 10, 10l, 8l, 7l);
        assertNextCorrelation(i2, 11, 10l, 11l, 7l);
        assertNextCorrelation(i2, 12, 10l, 11l, 12l);
        assertFalse(i2.hasNext());
    }

    @Test
    public void test3SeriesContinuousNoData() {
        TimeStampedPojoCorrelator correlator = new TimeStampedPojoCorrelator(3);
        correlator.add(0, new TestTimeStampedPojo(9));
        correlator.add(0, new TestTimeStampedPojo(1));
        correlator.add(0, new TestTimeStampedPojo(4));
        correlator.add(1, new TestTimeStampedPojo(8));
        correlator.add(1, new TestTimeStampedPojo(2));
        correlator.add(1, new TestTimeStampedPojo(5));
        correlator.add(2, new TestTimeStampedPojo(7));
        correlator.add(2, new TestTimeStampedPojo(3));
        correlator.add(2, new TestTimeStampedPojo(6));

        Iterator<Correlation> i = correlator.iterator();
        assertNextCorrelation(i, 1, 1l, null, null);
        assertNextCorrelation(i, 2, 1l, 2l, null);
        assertNextCorrelation(i, 3, 1l, 2l, 3l);
        assertNextCorrelation(i, 4, 4l, 2l, 3l);
        assertNextCorrelation(i, 5, 4l, 5l, 3l);
        assertNextCorrelation(i, 6, 4l, 5l, 6l);
        assertNextCorrelation(i, 7, 4l, 5l, 7l);
        assertNextCorrelation(i, 8, 4l, 8l, 7l);
        assertNextCorrelation(i, 9, 9l, 8l, 7l);
        assertFalse(i.hasNext());

        correlator.clear();

        Iterator<Correlation> i2 = correlator.iterator();
        assertFalse(i2.hasNext());
    }

    @Test
    public void test3SeriesContinuousOnlySingleSeries() {
        TimeStampedPojoCorrelator correlator = new TimeStampedPojoCorrelator(3);
        correlator.add(0, new TestTimeStampedPojo(9));
        correlator.add(0, new TestTimeStampedPojo(1));
        correlator.add(0, new TestTimeStampedPojo(4));
        correlator.add(1, new TestTimeStampedPojo(8));
        correlator.add(1, new TestTimeStampedPojo(2));
        correlator.add(1, new TestTimeStampedPojo(5));
        correlator.add(2, new TestTimeStampedPojo(7));
        correlator.add(2, new TestTimeStampedPojo(3));
        correlator.add(2, new TestTimeStampedPojo(6));

        Iterator<Correlation> i = correlator.iterator();
        assertNextCorrelation(i, 1, 1l, null, null);
        assertNextCorrelation(i, 2, 1l, 2l, null);
        assertNextCorrelation(i, 3, 1l, 2l, 3l);
        assertNextCorrelation(i, 4, 4l, 2l, 3l);
        assertNextCorrelation(i, 5, 4l, 5l, 3l);
        assertNextCorrelation(i, 6, 4l, 5l, 6l);
        assertNextCorrelation(i, 7, 4l, 5l, 7l);
        assertNextCorrelation(i, 8, 4l, 8l, 7l);
        assertNextCorrelation(i, 9, 9l, 8l, 7l);
        assertFalse(i.hasNext());

        correlator.clear();
        correlator.add(0, new TestTimeStampedPojo(10));

        Iterator<Correlation> i2 = correlator.iterator();
        assertNextCorrelation(i2, 10, 10l, 8l, 7l);
        assertFalse(i2.hasNext());
    }

    private void assertNextCorrelation(Iterator<Correlation> iter, long timestamp, Long... timestamps) {
        assertTrue(iter.hasNext());
        Correlation correlation = iter.next();
        assertEquals(timestamp, correlation.getTimeStamp());
        for (int i = 0 ; i < timestamps.length; i++) {
            if (timestamps[i] == null) {
                assertEquals(null, correlation.get(i));
            } else {
                assertEquals(timestamps[i].longValue(), correlation.get(i).getTimeStamp());
            }
        }
    }
}
