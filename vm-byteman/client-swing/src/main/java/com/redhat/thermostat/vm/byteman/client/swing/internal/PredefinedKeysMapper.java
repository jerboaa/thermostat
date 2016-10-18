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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.redhat.thermostat.shared.locale.Translate;

/**
 * Maps and reverse maps predefined keys from the view strings to
 * the data model strings.
 */
class PredefinedKeysMapper {
    
    static enum MapDirection {
        VIEW_TO_MODEL,
        MODEL_TO_VIEW
    }

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static final Map<String, String> PREDEFINED_KEY_MAP;
    private static final Map<String, String> REVERSE_PREDEFINED_KEY_MAP;
    private static final String EMPTY_STR = "";
    
    static final String PREDEFINED_PREFIX = "** ";
    static final String PREDEFINED_SUFFIX = " **";
    
    // Sorted list of predefined keys
    static final List<String> PREDEFINED_KEYS = Collections.unmodifiableList(Arrays.asList(
            GraphDataset.FREQUENCY_KEY, GraphDataset.MARKER_KEY, GraphDataset.TIMESTAMP_KEY
    ));
    static final String NO_FILTER_ITEM = t.localize(LocaleResources.NO_FILTER_NAME).getContents();
    
    static {
        PREDEFINED_KEY_MAP = new HashMap<>(PREDEFINED_KEYS.size());
        REVERSE_PREDEFINED_KEY_MAP = new HashMap<>(PREDEFINED_KEYS.size());
        for (String key: PREDEFINED_KEYS) {
            String mappedValue = PREDEFINED_PREFIX + key + PREDEFINED_SUFFIX;
            PREDEFINED_KEY_MAP.put(key, mappedValue);
            REVERSE_PREDEFINED_KEY_MAP.put(mappedValue, key);
        }
    }
    
    String mapPredefinedKey(String value, MapDirection direction) {
        switch (direction) {
        case MODEL_TO_VIEW:
            return mapValue(PREDEFINED_KEY_MAP, value);
        case VIEW_TO_MODEL:
            return mapValue(REVERSE_PREDEFINED_KEY_MAP, value);
        default:
            throw new AssertionError("Unknown direction: " + direction);
        }
    }
    
    String mapNoFilter(String value, MapDirection direction) {
        switch (direction) {
        case MODEL_TO_VIEW:
            if (EMPTY_STR.equals(value)) {
                return NO_FILTER_ITEM;
            } else {
                return value;
            }
        case VIEW_TO_MODEL:
            if (NO_FILTER_ITEM.equals(value)) {
                return EMPTY_STR;
            } else {
                return value;
            }
        default:
            throw new AssertionError("Unknown direction: " + direction);
        }
    }

    private String mapValue(Map<String, String> mapping, String value) {
        if (value == null) {
            return null;
        }
        String mappedValue = mapping.get(value);
        if (mappedValue != null) {
            return mappedValue;
        } else {
            // return unchanged
            return value;
        }
    }
}
