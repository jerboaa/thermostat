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

package com.redhat.thermostat.vm.byteman.common;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.storage.model.TimeStampedPojo;

@Entity
public class BytemanMetric extends BasePojo implements TimeStampedPojo {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    public static final String MARKER_NAME = "marker";
    public static final String TIMESTAMP_NAME = "timestamp";
    public static final String DATA_NAME = "data";
    private String vmId;
    private String marker;
    private String jsonPayload;
    private long timestamp;
    
    public BytemanMetric(String writerId) {
        super(writerId);
    }
    
    public BytemanMetric() {
        this(null);
    }
    
    @Persist
    public String getVmId() {
        return vmId;
    }

    @Persist
    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    /**
     * Get the marker for this Byteman metric. A marker can be thought of a
     * category for related metrics.
     * 
     * @return The marker for this metric.
     */
    @Persist
    public String getMarker() {
        return marker;
    }

    @Persist
    public void setMarker(String marker) {
        this.marker = marker;
    }

    @Persist
    public void setTimeStamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Persist
    @Override
    public long getTimeStamp() {
        return timestamp;
    }
    
    @Persist
    public void setData(String json) {
        this.jsonPayload = json;
    }
    
    /**
     * Get the metrics payload as raw JSON string. The payload is a JSON object
     * in key, value form. Supported value types are {@code Boolean},
     * {@code String} and {@code Double}.
     * 
     * @return The raw JSON string.
     */
    @Persist
    public String getDataAsJson() {
        return jsonPayload;
    }
    
    /**
     * Get the metrics payload as parsed {@link Map}. Supported value types are
     * {@code Boolean}, {@code String} and {@code Double}.
     * 
     * @return The parsed JSON payload as {@link Map}.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDataAsMap() {
        try {
            return (Map<String, Object>)GSON.fromJson(jsonPayload, HashMap.class);
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("Payload not in expected format. Payload was: " + jsonPayload);
        }
    }

}
