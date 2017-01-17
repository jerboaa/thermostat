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

package com.redhat.thermostat.common.command.noapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Parameter decoding context.
 * 
 * @see DecodingHelper
 * @see ParameterDecodingState
 */
public class ParameterDecodingContext {
    
    ParameterDecodingContext() {
        // package-private constructor. Only this package creates instances.
    }

    private final Map<String, String> values = new HashMap<>();
    private ParameterDecodingState state;
    private int bytesRead;
    
    /**
     * 
     * @return A map of parameter {@code key=value} pairs.
     * 
     * @throws IllegalStateException if not all parameters have yet been decoded.
     */
    public Map<String, String> getValues() {
        if (state != ParameterDecodingState.ALL_PARAMETERS_READ) {
            throw new IllegalStateException("Not all parameters have yet been decoded");
        }
        return Collections.unmodifiableMap(values);
    }
    
    /**
     * 
     * @return The current decoding state.
     */
    public ParameterDecodingState getState() {
        return state;
    }
    
    /**
     * 
     * @return The number of bytes consumed so far.
     */
    public int getBytesRead() {
        return bytesRead;
    }
    
    void setState(ParameterDecodingState newState) {
        state = newState;
    }
    
    void addParameter(String key, String value) {
        values.put(key, value);
    }
    
    void addToBytesRead(int numBytes) {
        bytesRead += numBytes;
    }
}
