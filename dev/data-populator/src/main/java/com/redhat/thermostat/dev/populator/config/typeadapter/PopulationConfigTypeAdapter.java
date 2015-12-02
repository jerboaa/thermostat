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

package com.redhat.thermostat.dev.populator.config.typeadapter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.redhat.thermostat.dev.populator.config.ConfigItem;
import com.redhat.thermostat.dev.populator.config.PopulationConfig;
import com.redhat.thermostat.dev.populator.dependencies.Relationship;

public class PopulationConfigTypeAdapter extends TypeAdapter<PopulationConfig> {
    
    private final Gson gson;
    
    public PopulationConfigTypeAdapter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void write(JsonWriter out, PopulationConfig config) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public PopulationConfig read(JsonReader in) throws IOException {
        // handle null
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        in.beginObject();
        PopulationConfig config = parsePopulationConfig(in);
        in.endObject();
        return config;
    }

    private PopulationConfig parsePopulationConfig(JsonReader in) throws IOException {
        Map<String, Object> values = new HashMap<>();
        while (in.hasNext()) {
            String name = in.nextName();
            switch(name) {
            case PopulationConfig.RECORDS:
                values.put(PopulationConfig.RECORDS, parseRecords(in));
                break;
            case PopulationConfig.RELATIONSHIPS:
                values.put(PopulationConfig.RELATIONSHIPS, parseRelationships(in));
                break;
            default:
                throw new IllegalStateException("Unknown population config value: '" + name + "'");
            }
        }
        ConfigItem[] rawItems = (ConfigItem[])values.get(PopulationConfig.RECORDS);
        Relationship[] rawRels = (Relationship[])values.get(PopulationConfig.RELATIONSHIPS);
        List<Relationship> rels;
        if (rawRels == null) {
            rels = Collections.emptyList();
        } else {
            rels = Arrays.asList(rawRels);
        }
        return PopulationConfig.createFromLists(Arrays.asList(rawItems), rels);
    }

    private Relationship[] parseRelationships(JsonReader in) {
        return gson.fromJson(in, Relationship[].class);
    }

    private ConfigItem[] parseRecords(JsonReader in) throws IOException {
        return gson.fromJson(in, ConfigItem[].class);
    }

}
