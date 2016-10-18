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

package com.redhat.thermostat.vm.byteman.client.swing.internal;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.junit.Test;

import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.vm.byteman.client.swing.internal.GraphDataset.CategoryTimePlotData;
import com.redhat.thermostat.vm.byteman.client.swing.internal.GraphDataset.CoordinateType;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;

public class GraphDatasetTest {

    private static final double DELTA = 0.001;
    private static final String TEST_WRITER_ID = "test-writerId";
    private static final String COUNT_FIELD = "count";
    private static final String TEST_VM_ID = "test-vmId";
    
    private static List<BytemanMetric> buildMetrics(MetricConfig config) {
        List<BytemanMetric> mList = new ArrayList<>();
        for (int i = 0; i < config.getNumMetrics(); i++) {
            BytemanMetric m = new BytemanMetric(TEST_WRITER_ID);
            m.setVmId(TEST_VM_ID);
            m.setData(config.produceDataJson(i));
            m.setMarker(config.produceMarker(i));
            m.setTimeStamp(config.produceTimeStamp(i));
            mList.add(m);
        }
        return mList;
    }
    
    @Test
    public void testEmptyXYDataSet() {
        final List<BytemanMetric> empty = Collections.emptyList();
        // x => marker
        GraphDataset dataset = new GraphDataset(empty, GraphDataset.MARKER_KEY, GraphDataset.TIMESTAMP_KEY, null);
        XYDataset actualDataset = dataset.getXYDataset();
        assertEquals(0, actualDataset.getSeriesCount());
        
        // y => marker
        dataset = new GraphDataset(empty, GraphDataset.TIMESTAMP_KEY, GraphDataset.MARKER_KEY, null);
        actualDataset = dataset.getXYDataset();
        assertEquals(0, actualDataset.getSeriesCount());
    }

    @Test
    public void testXYDataSetBasic() {
        List<BytemanMetric> mList = buildMetrics(new DefaultMetricConfig() {
           @Override
           public String produceDataJson(int i) {
               return new DataJsonBuilder()
                       .addKeyValue(COUNT_FIELD, new Long(i))
                       .addKeyValue("foobar", "baz" + i) // extra fields shouldn't matter
                       .build();
           }
        });
        // plots (x, y) where (x_i, y_i) == (i, i)
        GraphDataset dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, COUNT_FIELD, null);
        XYDataset xyDataSet = dataSet.getXYDataset();
        assertEquals(1, xyDataSet.getSeriesCount());
        assertEquals(DefaultMetricConfig.DEFAULT_NUM, xyDataSet.getItemCount(0));
        
        // sanity check some values
        assertEquals(0L, xyDataSet.getX(0, 0));
        assertEquals(0.0, xyDataSet.getY(0, 0));
        assertEquals(1L, xyDataSet.getX(0, 1));
        assertEquals(1.0, xyDataSet.getY(0, 1));
        assertEquals(99L, xyDataSet.getX(0, 99));
        assertEquals(99.0, xyDataSet.getY(0, 99));
    }
    
    @Test
    public void testXYDataSetFiltered() {
        final String filterKey = "foo";
        final String filterValue = "bar";
        // Produce metrics so we filter every other metric
        List<BytemanMetric> mList = buildMetrics(new DefaultMetricConfig() {
           @Override
           public String produceDataJson(int i) {
               if (i % 2 == 0) {
                   return new DataJsonBuilder()
                       .addKeyValue(COUNT_FIELD, new Long(i))
                       .build();
               } else {
                   return new DataJsonBuilder()
                           .addKeyValue(COUNT_FIELD, new Long(i))
                           .addKeyValue(filterKey, filterValue)
                           .build();
               }
           }
        });
        GraphDataset.Filter filter = new GraphDataset.Filter(filterKey, filterValue);
        GraphDataset dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, COUNT_FIELD, filter);
        XYDataset xyDataSet = dataSet.getXYDataset();
        assertEquals(1, xyDataSet.getSeriesCount());
        assertEquals(DefaultMetricConfig.DEFAULT_NUM/2, xyDataSet.getItemCount(0));
        
        // sanity check some values
        assertEquals(1L, xyDataSet.getX(0, 0));
        assertEquals(1.0, xyDataSet.getY(0, 0));
        assertEquals(3L, xyDataSet.getX(0, 1));
        assertEquals(3.0, xyDataSet.getY(0, 1));
        assertEquals(99L, xyDataSet.getX(0, 49));
        assertEquals(99.0, xyDataSet.getY(0, 49));
    }
    
    /**
     * When there is a repeated x value in our data the corresponding y values
     * contain the running sum. That is for a series x_i = a, y_i = 0.i for all
     * n values, then the x_n == sum(y_0,y_n). So for n == 4
     * we have: x_4 = (0.0 + 0.1 + 0.2 + 0.3).  
     */
    @Test
    public void testXYDataSetXRepeatDoubleSingleKey() {
        final long timeStamp = 322L; // constant numeric x coordinate
        List<BytemanMetric> mList = buildMetrics(new DefaultMetricConfig() {
           @Override
           public String produceDataJson(int i) {
               String dStringVal = String.format("0.%d", i);
               Double dVal = Double.parseDouble(dStringVal);
                   return new DataJsonBuilder()
                       .addKeyValue(COUNT_FIELD, dVal)
                       .build();
           }
           
           @Override
           public int getNumMetrics() {
               return 4;
           }
           
           @Override
           public long produceTimeStamp(int i) {
               return timeStamp; // same timestamp for all metrics
           }
        });
        GraphDataset dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, COUNT_FIELD, null);
        XYDataset xyDataSet = dataSet.getXYDataset();
        assertEquals(1, xyDataSet.getSeriesCount());
        assertEquals(1, xyDataSet.getItemCount(0));
        assertEquals(0.6, xyDataSet.getYValue(0, 0), DELTA);
        assertEquals(timeStamp, (long)xyDataSet.getXValue(0, 0));
    }
    
    @Test
    public void testXYDataSetXRepeatLongMultipleRepeatedKeys() {
        final int repeatAmount = 3;
        final long timestamps[] = new long[] {
            322L, 300L, -1L
        };
        List<BytemanMetric> mList = buildMetrics(new DefaultMetricConfig() {
           @Override
           public String produceDataJson(int i) {
               Long lVal = Long.valueOf(i);
                   return new DataJsonBuilder()
                       .addKeyValue(COUNT_FIELD, lVal)
                       .build();
           }
           
           @Override
           public int getNumMetrics() {
               // x key repeats repeatAmount times
               return timestamps.length * repeatAmount;
           }
           
           @Override
           public long produceTimeStamp(int i) {
               int idx = (i % repeatAmount);
               return timestamps[idx]; // same timestamp every repeatAmount times
           }
        });
        GraphDataset dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, COUNT_FIELD, null);
        XYDataset xyDataSet = dataSet.getXYDataset();
        assertEquals(1, xyDataSet.getSeriesCount());
        assertEquals(repeatAmount, xyDataSet.getItemCount(0));
        // added coordinates are sorted by x (ascending)
        assertEquals(timestamps[2], (long)xyDataSet.getXValue(0, 0));
        assertEquals(timestamps[1], (long)xyDataSet.getXValue(0, 1));
        assertEquals(timestamps[0], (long)xyDataSet.getXValue(0, 2));
        assertEquals("0 + 3 + 6 = 9", 9.0, xyDataSet.getYValue(0, 2), DELTA);
        assertEquals("1 + 4 + 7 = 12", 12.0, xyDataSet.getYValue(0, 1), DELTA);
        assertEquals("2 + 5 + 8 = 15", 15.0, xyDataSet.getYValue(0, 0), DELTA);
    }
    
    /**
     * Special case of testXYDataSetXRepeatLongMultipleRepeatedKeys where
     * y_i = 1 for all i.
     */
    @Test
    public void testXYDataSetXRepeatLongMultipleRepeatedKeysFrequency() {
        final int repeatAmount = 3;
        final long timestamps[] = new long[] {
            322L, 300L, -1L
        };
        List<BytemanMetric> mList = buildMetrics(new DefaultMetricConfig() {
           @Override
           public String produceDataJson(int i) {
               Long lVal = Long.valueOf(1);
                   return new DataJsonBuilder()
                       .addKeyValue(COUNT_FIELD, lVal)
                       .build();
           }
           
           @Override
           public int getNumMetrics() {
               // x key repeats repeatAmount times
               return timestamps.length * repeatAmount;
           }
           
           @Override
           public long produceTimeStamp(int i) {
               int idx = (i % repeatAmount);
               return timestamps[idx]; // same timestamp every repeatAmount times
           }
        });
        GraphDataset dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, COUNT_FIELD, null);
        XYDataset xyDataSet = dataSet.getXYDataset();
        assertEquals(1, xyDataSet.getSeriesCount());
        assertEquals(repeatAmount, xyDataSet.getItemCount(0));
        // added coordinates are sorted by x (ascending)
        assertEquals(timestamps[2], (long)xyDataSet.getXValue(0, 0));
        assertEquals(timestamps[1], (long)xyDataSet.getXValue(0, 1));
        assertEquals(timestamps[0], (long)xyDataSet.getXValue(0, 2));
        assertEquals(timestamps[2] + " occurred 3 times in the data set", 3.0, xyDataSet.getYValue(0, 2), DELTA);
        assertEquals(timestamps[1] + " occurred 3 times in the data set", 3.0, xyDataSet.getYValue(0, 1), DELTA);
        assertEquals(timestamps[2] + " occurred 3 times in the data set", 3.0, xyDataSet.getYValue(0, 0), DELTA);
    }
    
    @Test
    public void testCategoryDatasetEmpty() {
        final List<BytemanMetric> empty = Collections.emptyList();
        GraphDataset dataSet = new GraphDataset(empty /* no matter */, GraphDataset.TIMESTAMP_KEY, COUNT_FIELD, null);
        CategoryDataset actualSet = dataSet.getCategoryDataset();
        assertEquals(0, actualSet.getColumnCount());
        assertEquals(0, actualSet.getRowCount());
    }
    
    /**
     * Category data plot for x = category, y = numeric. In that case
     * expect the y value (per category) to be the sum of all y values with
     * category x.
     */
    @Test
    public void testCategoryDatasetYNumeric() {
        final String evenMarker = "even_marker";
        final String oddMarker = "odd_marker";
        List<BytemanMetric> mList = buildMetrics(new DefaultMetricConfig() {
            
            @Override
            public String produceMarker(int i) {
                if (i % 2 == 0) {
                    // even
                    return evenMarker;
                } else {
                    // odd
                    return oddMarker;
                }
            }
            
            @Override
            public String produceDataJson(int i) {
                Long iLong = Long.valueOf(i);
                return new DataJsonBuilder()
                        .addKeyValue(COUNT_FIELD, iLong)
                        .build();
            }
            
            @Override
            public int getNumMetrics() {
                return 12;
            }
        });
        GraphDataset dataSet = new GraphDataset(mList, GraphDataset.MARKER_KEY, COUNT_FIELD, null);
        CategoryDataset actualSet = dataSet.getCategoryDataset();
        assertEquals("two different markers", 2, actualSet.getRowCount());
        assertEquals("one value per marker", 1, actualSet.getColumnCount());
        String actualEvenMarker = (String)actualSet.getRowKey(0);
        String actualOddMarker = (String)actualSet.getRowKey(1);
        assertEquals(evenMarker, actualEvenMarker);
        assertEquals(oddMarker, actualOddMarker);
        double oddSum = 1.0 + 3.0 + 5 + 7 + 9 + 11; // odd numbers in range [0,12)
        double evenSum = 2.0 + 4 + 6 + 8 + 10; // even numbers in range [0, 12)
        Comparable<?> columnKey = actualSet.getColumnKey(0);
        assertEquals(oddSum, (double)actualSet.getValue(oddMarker, columnKey), DELTA);
        assertEquals(evenSum, (double)actualSet.getValue(evenMarker, columnKey), DELTA);
    }
    
    /**
     * Category data plot for x = category, y = category. In that case
     * expect the y value to be the frequency of category x.
     */
    @Test
    public void testCategoryDatasetYCategory() {
        final String evenMarker = "even_marker";
        final String oddMarker = "odd_marker";
        List<BytemanMetric> mList = buildMetrics(new DefaultMetricConfig() {
            
            @Override
            public String produceMarker(int i) {
                if (i % 2 == 0) {
                    // even
                    return evenMarker;
                } else {
                    // odd
                    return oddMarker;
                }
            }
            
            @Override
            public int getNumMetrics() {
                // pick an odd number so as to have uneven frequency spread between odd/even numbers
                return 100 - 1;
            }
        });
        GraphDataset dataSet = new GraphDataset(mList, GraphDataset.MARKER_KEY, GraphDataset.MARKER_KEY, null);
        CategoryDataset actualSet = dataSet.getCategoryDataset();
        assertEquals("two different markers", 2, actualSet.getRowCount());
        assertEquals(2, actualSet.getColumnCount());
        String actualEvenMarker = (String)actualSet.getRowKey(0);
        String actualOddMarker = (String)actualSet.getRowKey(1);
        assertEquals(evenMarker, actualEvenMarker);
        assertEquals(oddMarker, actualOddMarker);
        String evenKey = (String)actualSet.getColumnKey(0);
        String oddKey = (String)actualSet.getColumnKey(1);
        assertEquals("100/2 = 50. i.e. frequency of even/odd numbers.",
                49.0, (double)actualSet.getValue(oddMarker, oddKey), DELTA);
        assertEquals("100/2 = 49. i.e. frequency of even/odd numbers.",
                50.0, (double)actualSet.getValue(evenMarker, evenKey), DELTA);
    }
    
    @Test
    public void testEmptyCategoryTimePlot() {
        final List<BytemanMetric> empty = Collections.emptyList();
        // x => marker
        GraphDataset dataset = new GraphDataset(empty, GraphDataset.MARKER_KEY, GraphDataset.TIMESTAMP_KEY, null);
        CategoryTimePlotData actualDataset = dataset.getCategoryTimePlot();
        assertEquals(0, actualDataset.getXYDataSet().getSeriesCount());
        
        // y => timestamp
        dataset = new GraphDataset(empty, GraphDataset.TIMESTAMP_KEY, GraphDataset.TIMESTAMP_KEY, null);
        actualDataset = dataset.getCategoryTimePlot();
        assertEquals(0, actualDataset.getXYDataSet().getSeriesCount());
    }
    
    @Test
    public void testCategoryTimePlotBasic() {
        final String evenMarker = "even_marker";
        final String oddMarker = "odd_marker";
        final int numMetrics = 5;
        List<BytemanMetric> mList = buildMetrics(new DefaultMetricConfig() {
            
            @Override
            public String produceMarker(int i) {
                if (i % 2 == 0) {
                    // even
                    return evenMarker;
                } else {
                    // odd
                    return oddMarker;
                }
            }
            
            @Override
            public int getNumMetrics() {
                return numMetrics;
            }
            
            @Override
            public long produceTimeStamp(int i) {
                return i * 2;
            }
            
        });
        GraphDataset dataset = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, GraphDataset.MARKER_KEY, null);
        CategoryTimePlotData actualData = dataset.getCategoryTimePlot();
        XYDataset actualDataset = actualData.getXYDataSet();
        assertEquals("Expected one series, exactly", 1, actualDataset.getSeriesCount());
        assertEquals(5, actualDataset.getItemCount(0));
        for (int i = 0; i < numMetrics; i++) {
            double retval = actualDataset.getXValue(0, i);
            double expectedVal = Double.valueOf(i * 2);
            assertEquals(expectedVal, retval, DELTA);
        }
        // Y-values are unique numbers per marker. I.e. 0 and 1 in this case
        for (int i = 0; i < numMetrics; i++) {
            double yVal = actualDataset.getYValue(0, i);
            double expectedVal;
            if (i %2 == 0) {
                expectedVal = 0.0;
            } else {
                expectedVal = 1.0;
            }
            assertEquals(expectedVal, yVal, DELTA);
        }
        
        // verify correct return values have been set
        List<String> retvalList = Arrays.asList(actualData.getSymbols());
        Collections.sort(retvalList);
        assertEquals(evenMarker, retvalList.get(0));
        assertEquals(oddMarker, retvalList.get(1));
    }
    
    @Test
    public void testXType() {
        List<BytemanMetric> mList = buildMetrics(new DefaultMetricConfig());
        GraphDataset dataSet = new GraphDataset(mList, GraphDataset.MARKER_KEY, GraphDataset.TIMESTAMP_KEY, null);
        assertEquals(CoordinateType.CATEGORY, dataSet.getXType());
        dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, GraphDataset.MARKER_KEY, null);
        assertEquals(CoordinateType.TIME, dataSet.getXType());
        dataSet = new GraphDataset(mList, GraphDataset.FREQUENCY_KEY, GraphDataset.MARKER_KEY, null);
        assertEquals(CoordinateType.INTEGRAL, dataSet.getXType());
        mList = buildMetrics(new DefaultMetricConfig() {
            @Override
            public String produceDataJson(int i) {
                Long iLong = Long.valueOf(i);
                return new DataJsonBuilder()
                        .addKeyValue(COUNT_FIELD, iLong)
                        .build();
            }
        });
        dataSet = new GraphDataset(mList, COUNT_FIELD, GraphDataset.MARKER_KEY, null);
        assertEquals(CoordinateType.REAL, dataSet.getXType());
    }
    
    @Test
    public void testXLabel() {
        List<BytemanMetric> mList = buildMetrics(new DefaultMetricConfig());
        GraphDataset dataSet = new GraphDataset(mList, GraphDataset.MARKER_KEY, GraphDataset.TIMESTAMP_KEY, null);
        assertEquals(GraphDataset.MARKER_KEY, dataSet.getXLabel());
        dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, GraphDataset.MARKER_KEY, null);
        assertEquals(GraphDataset.TIMESTAMP_KEY, dataSet.getXLabel());
        dataSet = new GraphDataset(mList, GraphDataset.FREQUENCY_KEY, GraphDataset.MARKER_KEY, null);
        assertEquals(GraphDataset.FREQUENCY_KEY, dataSet.getXLabel());
        
        mList = buildMetrics(new DefaultMetricConfig() {
            @Override
            public String produceDataJson(int i) {
                Long iLong = Long.valueOf(i);
                return new DataJsonBuilder()
                        .addKeyValue(COUNT_FIELD, iLong)
                        .build();
            }
        });
        dataSet = new GraphDataset(mList, COUNT_FIELD, GraphDataset.MARKER_KEY, null);
        assertEquals(COUNT_FIELD, dataSet.getXLabel());
    }
    
    @Test
    public void testYType() {
        List<BytemanMetric> mList = buildMetrics(new DefaultMetricConfig());
        GraphDataset dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, GraphDataset.MARKER_KEY, null);
        assertEquals(CoordinateType.CATEGORY, dataSet.getYType());
        dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, GraphDataset.TIMESTAMP_KEY, null);
        assertEquals(CoordinateType.TIME, dataSet.getYType());
        dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, GraphDataset.FREQUENCY_KEY, null);
        assertEquals(CoordinateType.INTEGRAL, dataSet.getYType());
        mList = buildMetrics(new DefaultMetricConfig() {
            @Override
            public String produceDataJson(int i) {
                Long iLong = Long.valueOf(i);
                return new DataJsonBuilder()
                        .addKeyValue(COUNT_FIELD, iLong)
                        .build();
            }
        });
        dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, COUNT_FIELD, null);
        assertEquals(CoordinateType.REAL, dataSet.getYType());
    }
    
    @Test
    public void testYLabel() {
        List<BytemanMetric> mList = buildMetrics(new DefaultMetricConfig());
        GraphDataset dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, GraphDataset.MARKER_KEY, null);
        assertEquals(GraphDataset.MARKER_KEY, dataSet.getYLabel());
        dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, GraphDataset.TIMESTAMP_KEY, null);
        assertEquals(GraphDataset.TIMESTAMP_KEY, dataSet.getYLabel());
        dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, GraphDataset.FREQUENCY_KEY, null);
        assertEquals(GraphDataset.FREQUENCY_KEY, dataSet.getYLabel());
        mList = buildMetrics(new DefaultMetricConfig() {
            @Override
            public String produceDataJson(int i) {
                Long iLong = Long.valueOf(i);
                return new DataJsonBuilder()
                        .addKeyValue(COUNT_FIELD, iLong)
                        .build();
            }
        });
        dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, COUNT_FIELD, null);
        assertEquals(COUNT_FIELD, dataSet.getYLabel());
    }
    
    @Test
    public void testSize() {
        List<BytemanMetric> mList = buildMetrics(new DefaultMetricConfig());
        GraphDataset dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, GraphDataset.MARKER_KEY, null);
        assertEquals(DefaultMetricConfig.DEFAULT_NUM, dataSet.size());
        final int numMetrics = 10;
        mList = buildMetrics(new DefaultMetricConfig() {
            @Override
            public int getNumMetrics() {
                return numMetrics;
            }
            
            @Override
            public String produceDataJson(int i) {
                Long iLong = Long.valueOf(i);
                return new DataJsonBuilder()
                    .addKeyValue(COUNT_FIELD, iLong)
                    .build();
            }
        });
        dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, COUNT_FIELD, null);
        assertEquals(numMetrics, dataSet.size());
        
        // some metrics are filtered (implicitly) so should not add to the data
        // set
        mList = buildMetrics(new DefaultMetricConfig() {
            @Override
            public String produceDataJson(int i) {
                if (i % 2 == 0) {
                    Long iLong = Long.valueOf(i);
                    return new DataJsonBuilder()
                        .addKeyValue(COUNT_FIELD, iLong)
                        .build();
                } else {
                    return new DataJsonBuilder().build();
                }
            }
        });
        dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, COUNT_FIELD, null);
        assertEquals(DefaultMetricConfig.DEFAULT_NUM/2, dataSet.size());
        
        // explicit filtering
        final double fooValue = 3000.0;
        final String fooKey = "foo-key";
        mList = buildMetrics(new DefaultMetricConfig() {
            @Override
            public String produceDataJson(int i) {
                if (i % 30 == 0) {
                    Long iLong = Long.valueOf(i);
                    return new DataJsonBuilder()
                        .addKeyValue(COUNT_FIELD, iLong)
                        .addKeyValue(fooKey, Double.valueOf(fooValue))
                        .build();
                } else {
                    Long iLong = Long.valueOf(i);
                    return new DataJsonBuilder()
                        .addKeyValue(COUNT_FIELD, iLong)
                        .build();
                }
            }
        });
        GraphDataset.Filter filter = new GraphDataset.Filter(fooKey, String.valueOf(fooValue));
        dataSet = new GraphDataset(mList, GraphDataset.TIMESTAMP_KEY, COUNT_FIELD, filter);
        assertEquals("0, 30, 60, 90 => 4", 4, dataSet.size());
    }

    static interface MetricConfig {
        
        String produceDataJson(int i);
        
        String produceMarker(int i);
        
        long produceTimeStamp(int i);
        
        int getNumMetrics();
    }
    
    static class DefaultMetricConfig implements MetricConfig {
        
        private static final int DEFAULT_NUM = 100;

        @Override
        public String produceDataJson(int i) {
            return "{\"key\": \"value\"}";
        }

        @Override
        public String produceMarker(int i) {
            return "marker-value";
        }

        @Override
        public long produceTimeStamp(int i) {
            return i;
        }

        @Override
        public int getNumMetrics() {
            return DEFAULT_NUM;
        }
        
    }
    
    static class DataJsonBuilder {
        
        private final List<Pair<String, Object>> keyValues = new ArrayList<>();
        
        DataJsonBuilder addKeyValue(String key, Object value) {
            keyValues.add(new Pair<>(key, value));
            return this;
        }
        
        String build() {
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            int i = 0;
            for (Pair<String, Object> pair: keyValues) {
                builder.append("\"" + pair.getFirst() + "\": ");
                Object val = pair.getSecond();
                if (val instanceof Long) {
                    Long lVal = (Long)val;
                    builder.append(lVal);
                } else if (val instanceof Double) {
                    Double dVal = (Double)val;
                    builder.append(dVal);
                } else if (val instanceof Boolean) {
                    Boolean bVal = (Boolean)val;
                    builder.append(bVal);
                } else {
                    if (!(val instanceof String)) {
                        throw new AssertionError("Unexpected value type: " + val.getClass());
                    }
                    String sVal = (String)val;
                    builder.append("\"" + sVal + "\"");
                }
                if (i != keyValues.size() - 1) {
                    builder.append(",\n");
                }
                i++;
            }
            builder.append("}");
            return builder.toString();
        }
    }
}
