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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.data.DefaultKeyedValues;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;

class GraphDataset {
    
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    
    enum CoordinateType {
        INTEGRAL,
        REAL,
        TIME,
        CATEGORY
    };
    
    /**
     * A special coordinate name used to identify the timestamp associated
     * with any given Byteman metric. If it is entered as the x or y coordinate
     * name in the graph dialogue then it will select the tiemstamp as the value
     * to be graphed against the other chosen coordinate. Timestamp values
     * are stored as longs but are displayed as time values.
     *
     * TODO: it really only makes sense to use timestamp as the X axis. maybe
     * we should reject any attempt to use it for the y axis?
     */
    static final String TIMESTAMP_KEY = "timestamp";

    /**
     * A special coordinate name used to identify the frequency count
     * of any given Byteman metric. If it is entered as the x or y coordinate
     * name in the graph dialogue then it will count 1 for each occurence of
     * other value. Frequency values are stored as longs.
     */
    static final String FREQUENCY_KEY = "frequency";

    /**
     * A special coordinate name used to identify the marker string
     * of any given Byteman metric. If it is entered as the x or y coordinate
     * name in the graph dialogue then it will select the marker as the value
     * to be graphed against the other chosen coordinate.
     */
    static final String MARKER_KEY = "marker";
    
    private final List<Pair<Object, Object>> data;
    String xkey;
    String ykey;
    CoordinateType xtype;
    CoordinateType ytype;
    private static CategoryDataset emptyCategoryDataset = new DefaultCategoryDataset();
    private static PieDataset emptyPieDataset = new DefaultPieDataset();
    private static XYDataset emptyXYDataset = new XYSeriesCollection();
    private static Number frequencyUnit = Long.valueOf(1);

    public GraphDataset(List<BytemanMetric> metrics, String xkey, String ykey, String filter, String value)
    {
        this.xkey = xkey;
        this.ykey = ykey;
        xtype = CoordinateType.INTEGRAL;
        ytype = CoordinateType.INTEGRAL;
        data = new ArrayList<Pair<Object,Object>>();
        if (TIMESTAMP_KEY.equals(xkey)) {
            xtype = CoordinateType.TIME;
        } else if (FREQUENCY_KEY.equals(xkey)) {
            xtype = CoordinateType.INTEGRAL;
        } else if (MARKER_KEY.equals(xkey)) {
            xtype = CoordinateType.CATEGORY;
        }
        if (TIMESTAMP_KEY.equals(ykey)) {
            ytype = CoordinateType.TIME;
        } else if (FREQUENCY_KEY.equals(ykey)) {
            ytype = CoordinateType.INTEGRAL;
        } else if (MARKER_KEY.equals(ykey)) {
            ytype = CoordinateType.CATEGORY;
        }
        // if we have a filter value then convert it to a number if it is numeric
        Object filterValue = value;
        if (filter != null && value != null) {
            // may need to convert String to Numeric
            filterValue = maybeNumeric(value);
        }
        if (metrics != null) {
            for (BytemanMetric m : metrics) {
                Map<String, Object> map = m.getDataAsMap();
                // ensure that lookups for the timestamp key always retrieve
                // the Long timestamp value associated with the metric and
                // that lookups for the frequency key always retrieve
                // the Long value 1.
                map.put(TIMESTAMP_KEY, m.getTimeStamp());
                map.put(FREQUENCY_KEY, frequencyUnit);
                map.put(MARKER_KEY, m.getMarker());
                // if we have a filter then check for presence of filter key
                if (filter != null && filter.length() > 0) {
                    Object v = map.get(filter);
                    if (v == null) {
                        // skip this metric
                        continue;
                    }
                    if (filterValue != null) {
                        // may need to process String value as Numeric
                        if (v instanceof String) {
                            v = maybeNumeric((String)v);
                        }
                        if (!filterValue.equals(v)) {
                            // skip this metric
                            continue;
                        }
                    }
                }
                Object xval = map.get(xkey);
                Object yval = map.get(ykey);
                // only include records which contain values for both coordinates
                if(xval != null && yval != null) {
                    // maybe re-present retrieved values as Numeric
                    // and/or downgrade coordinate type from INTEGRAL
                    // to REAL or even CATEGORY
                    xval = newCoordinate(xkey, xval, true);
                    yval = newCoordinate(ykey, yval, false);
                    data.add(new Pair<Object, Object>(xval, yval));
                }
            }
        }
    }

    public int size() {
        return data.size();
    }

    public XYDataset getXYDataset()
    {
        if (xtype == CoordinateType.CATEGORY ||
                ytype == CoordinateType.CATEGORY) {
            return emptyXYDataset;
        }

        XYSeries xyseries = new XYSeries(ykey + " against  " + xkey);

        for (Pair<Object,Object> p : data) {
            Number x = (Number)p.getFirst();
            Number y = (Number)p.getSecond();
            int idx = xyseries.indexOf(x);
            if (idx >= 0) {
                Number y1 = xyseries.getY(idx);
                switch (ytype) {
                case REAL:
                    y = y.doubleValue() + y1.doubleValue();
                    break;
                default:
                    y = y.longValue() + y1.longValue();
                }
                xyseries.updateByIndex(idx, y);
            } else {
                xyseries.add(x, y);
            }
        }
        XYSeriesCollection xycollection = new  XYSeriesCollection();
        xycollection.addSeries(xyseries);
        return xycollection;
    }

    public CategoryDataset getCategoryDataset()
    {
        if (xtype == CoordinateType.TIME) {
            return emptyCategoryDataset;
        }
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        // treat x values as category values by calling toString
        // where they are numeric we ought to support binning them into ranges
        switch (ytype) {
        case CATEGORY:
            // graph category against category by frequency
            for (Pair<Object, Object> p : data) {
                String first = p.getFirst().toString();
                String second = p.getSecond().toString();
                if(dataset.getRowKeys().contains(first) && dataset.getColumnKeys().contains(second)) {
                    dataset.incrementValue(1.0, first, second);
                } else {
                    dataset.addValue(1.0, first, second);
                }
            }
            break;
        case TIME:
            // bin time values into ranges and label range with start time
            // for now just drop through to treat time value as numeric
        default:
            // graph category against numeric by summing numeric values
            for (Pair<Object, Object> p : data) {
                String first = p.getFirst().toString();
                String second = "";
                double increment = ((Number) p.getSecond()).doubleValue();
                if (dataset.getRowKeys().contains(first)) {
                    dataset.incrementValue(increment, first, second);
                } else {
                    dataset.addValue(increment, first, second);
                }
            }
            break;
        }
        return dataset;
    }

    // alternative option for presenting category xkey with numeric ykey
    public PieDataset getPieDataset()
    {
        if (xtype != CoordinateType.CATEGORY || ytype == CoordinateType.CATEGORY) {
            return emptyPieDataset;
        }

        DefaultKeyedValues keyedValues = new DefaultKeyedValues();

        for (Pair<Object,Object> p : data) {
            String first = p.getFirst().toString();
            double second = ((Number)p.getSecond()).doubleValue();
            int index = keyedValues.getIndex(first);
            if (index >= 0) {
                Number existing = keyedValues.getValue(first);
                keyedValues.addValue(first, existing.doubleValue() + second);
            } else {
                keyedValues.addValue(first, second);
            }
        }
        PieDataset pieDataset = new DefaultPieDataset(keyedValues);
        return pieDataset;
    }

    public XYDataset getCategoryTimePlot(String[][] symbolsReturn)
    {
        if (xtype != CoordinateType.TIME || ytype != CoordinateType.CATEGORY) {
            return emptyXYDataset;
        }

        // we need to display changing category state over time
        //
        // we can create an XYDataSet substituting numeric Y values
        // to encode the category data. then we provide the data
        // set with a range axis which displays the numeric
        // values symbolically.

        XYSeries xyseries = new XYSeries(t.localize(LocaleResources.X_AGAINST_Y, xkey, ykey).getContents());
        int count = 0;
        HashMap<String, Number> tickmap = new HashMap<String, Number>();

        for (Pair<Object,Object> p : data) {
            Number x = (Number)p.getFirst();
            String ysym = (String)p.getSecond();
            Number y = tickmap.get(ysym);
            if (y == null) {
                y = Long.valueOf(count++);
                tickmap.put(ysym, y);
            }
            xyseries.add(x, y);
        }
        // populate key array
        String[] symbols = new String[count];
        for (String key: tickmap.keySet()) {
            int value = tickmap.get(key).intValue();
            symbols[value] = key;
        }

        symbolsReturn[0] = symbols;

        XYSeriesCollection xycollection = new  XYSeriesCollection();
        xycollection.addSeries(xyseries);

        return xycollection;
    }

    public String getXLabel() {
        return xkey;
    }

    public String getYLabel() {
        return ykey;
    }

    public CoordinateType getXType() {
        return xtype;
    }

    public CoordinateType getYType() {
        return ytype;
    }

    private Object maybeNumeric(String value) {
        if (value == null || value.length() == 0)  {
            return null;
        }
        try {
            if(value.contains(".")) {
                return Double.valueOf(value);
            } else {
                return Long.valueOf(value);
            }
        } catch (NumberFormatException nfe) {
            return value;
        }
    }

    /**
     * process a newly read x or y coordinate value, which is either a Long timestanp or an unparsed
     * numeric or category value String, returning a Long, parsed Numeric or String value. As a side
     * effect of attempting to parse an input String the coordinate type for the relevant coordinate
     * axis may be downgraded from INTEGRAL (assumed default) to DOUBLE or CATEGORY.
     * @param key the label for the coordinate axis which may be the special value timestamp
     * @param value the new found coordinate value which may be a Long timestamp or a String yet to be parsed
     * @param isX  true if this is an x coordinate value false if it is a y coordinate value
     * @return an Object repreenting
     */
    private Object newCoordinate(String key, Object value, boolean isX) {

        CoordinateType ctype = (isX ? xtype : ytype);
        if (ctype == CoordinateType.TIME) {
            // guaranteed already to be a Long
            return value;
        }

        boolean updateCType = false;

        if (value instanceof String && ctype != CoordinateType.CATEGORY) {
            String str = (String)value;
            // see if we can parse this as a number
            try {
                if (str.contains(".")) {
                    value = Double.valueOf(str);
                    if (ctype != CoordinateType.REAL) {
                        ctype = CoordinateType.REAL;
                        updateCType = true;
                    }
                } else {
                    value = Long.valueOf(str);
                }
            } catch (NumberFormatException nfe) {
                ctype = CoordinateType.CATEGORY;
                updateCType = true;
            }
        }
        if (value instanceof Double || value.getClass() == double.class) {
            ctype = CoordinateType.REAL;
            updateCType = true;
        }
        if (updateCType) {
            if (isX) {
                xtype = ctype;
            } else {
                ytype = ctype;
            }
        }
        return value;
    }
}