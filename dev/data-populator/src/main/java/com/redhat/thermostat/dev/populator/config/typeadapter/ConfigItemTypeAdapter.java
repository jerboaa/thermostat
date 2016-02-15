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

package com.redhat.thermostat.dev.populator.config.typeadapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.redhat.thermostat.dev.populator.config.ConfigItem;

public class ConfigItemTypeAdapter extends TypeAdapter<ConfigItem> {

    @Override
    public ConfigItem read(JsonReader reader) throws IOException {
        // handle null
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        
        reader.beginObject();
        ConfigItem item = parseConfigItem(reader);
        reader.endObject();
        
        return item;
    }

    private ConfigItem parseConfigItem(JsonReader reader) throws IOException {
        Map<String, Object> values = new HashMap<>();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch(name) {
            case ConfigItem.NAME:
                values.put(ConfigItem.NAME, reader.nextString());
                break;
            case ConfigItem.ALIVE:
                values.put(ConfigItem.ALIVE, reader.nextInt());
                break;
            case ConfigItem.NUMBER:
                values.put(ConfigItem.NUMBER, reader.nextInt());
                break;
            default:
                throw new IllegalStateException("Unknown config value: '" + name + "'");
            }
        }
        String collName = (String)values.get(ConfigItem.NAME);
        String collectionDetails = collName == null ? "" : "[" + collName + "] ";
        Integer number = (Integer)values.get(ConfigItem.NUMBER);
        Integer aliveItems = (Integer)values.get(ConfigItem.ALIVE);
        int aliveItemsForSanityTest = getAliveCount(aliveItems);
        if (number < 0 || aliveItemsForSanityTest < 0) {
            throw new IllegalArgumentException(collectionDetails + "number of items and alive items must be positive");
        }
        if (number - aliveItemsForSanityTest < 0) {
            throw new IllegalArgumentException(collectionDetails + "alive items > number of total items.");
        }
        return new ConfigItem(getValue(number), getValue(aliveItems), collName);
    }

    private int getAliveCount(Integer aliveItems) {
        if (aliveItems != null) {
            return aliveItems;
        }
        return 0; // return 0 for unset, just so that we make sanity checks happy
    }

    private int getValue(Integer value) {
        return value == null ? ConfigItem.UNSET : value;
    }

    @Override
    public void write(JsonWriter writer, ConfigItem item) throws IOException {
        throw new NotImplementedException();
    }
}
