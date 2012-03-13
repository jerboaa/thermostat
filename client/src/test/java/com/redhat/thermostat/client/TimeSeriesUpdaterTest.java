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

package com.redhat.thermostat.client;

import static org.mockito.Mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingUtilities;

import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.junit.Test;

import com.redhat.thermostat.client.TimeSeriesUpdater.LastUpdateTimeCallback;

public class TimeSeriesUpdaterTest {

    @Test
    public void test() throws InterruptedException {

        long TIMESTAMP = 1;
        int DATA_VALUE = 1;
        @SuppressWarnings("unchecked")
        Iterable<Integer> dataSource = mock(Iterable.class);
        when(dataSource.iterator()).thenReturn(Arrays.asList(new Integer[] { DATA_VALUE }).iterator());

        TimeSeries toUpdate = mock(TimeSeries.class);

        @SuppressWarnings("unchecked")
        TimeSeriesUpdater.Converter<Integer, Integer> converter = mock(TimeSeriesUpdater.Converter.class);
        when(converter.convert(1)).thenReturn(new DiscreteTimeData<Integer>(TIMESTAMP, DATA_VALUE));

        LastUpdateTimeCallback lastUpdateTime = mock(TimeSeriesUpdater.LastUpdateTimeCallback.class);

        final CountDownLatch latch = new CountDownLatch(1);

        TimeSeriesUpdater<Integer, Integer> tester;
        tester = new TimeSeriesUpdater<Integer, Integer>(dataSource, toUpdate, converter, lastUpdateTime) {
            @Override
            protected void done() {
                super.done();
                latch.countDown();
            }
        };
        tester.execute();
        
        latch.await();
        
        verify(dataSource).iterator();
        verify(toUpdate).add(new FixedMillisecond(TIMESTAMP), (Integer)DATA_VALUE, false);
        verify(toUpdate).fireSeriesChanged();
        verify(lastUpdateTime).update(TIMESTAMP);
    }
}
