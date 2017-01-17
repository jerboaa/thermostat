/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package org.jboss.byteman.thermostat.helper;

import static org.jboss.byteman.thermostat.helper.Utils.escapeQuotes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic record for data collected for Thermostat
 */
public class BytemanMetric {
    
    private final String marker;
    private final LinkedHashMap<String, Object> data;
    private final long timestamp;

    /**
     * Constructor
     *
     * @param marker marker value
     * @param data arbitrary data
     */
    public BytemanMetric(String marker, LinkedHashMap<String, Object> data) {
        this.marker = marker;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Marker accessor
     *
     * @return  market field
     */
    public String getMarker() {
        return marker;
    }

    /**
     * timestamp accessor
     *
     * @return  timestamp field
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Data accessor
     *
     * @return data field
     */
    public LinkedHashMap<String, Object> getData() {
        return data;
    }

    /**
     * Converts this record to JSON string
     *
     * @return JSON string
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"marker\":").append(toJsonValue(marker)).append(",");
        sb.append("\"timestamp\":").append(toJsonValue(Long.toString(timestamp))).append(",");
        if (data == null) {
            sb.append("\"data\":null");
        } else {
            sb.append("\"data\":{");
            boolean first = true;
            for (Map.Entry<String, Object> en : data.entrySet()) {
                if (!first) {
                    sb.append(",");
                } else {
                    first = false;
                }
                sb.append(toJsonValue(en.getKey())).append(":").append(toJsonValue(en.getValue()));
            }
            sb.append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    private String toJsonValue(Object valObj) {
        if (null == valObj) {
            return "null";
        } else if (valObj instanceof String) {
            String valStr = (String) valObj;
            return "\"" + escapeQuotes(valStr) + "\"";
        } else if (Number.class.isAssignableFrom(valObj.getClass()) ||
                Boolean.class.isAssignableFrom(valObj.getClass())) {
            return valObj.toString();
        } else {
            throw new UnsupportedOperationException("Cannot serialize object of type " + valObj.getClass().getName());
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("  marker='").append(marker).append('\'');
        sb.append(", timestamp='").append(timestamp).append('\'');
        sb.append(", data=").append(data);
        sb.append('}');
        return sb.toString();
    }
}
