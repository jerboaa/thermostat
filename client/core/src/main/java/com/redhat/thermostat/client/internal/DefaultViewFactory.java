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

package com.redhat.thermostat.client.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.View;
import com.redhat.thermostat.common.ViewFactory;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class DefaultViewFactory implements ViewFactory {

    private static final Logger logger = LoggingUtils.getLogger(SwingViewFactory.class);
    private final Map<Class<?>, Class<?>> lookupTable = Collections.synchronizedMap(new HashMap<Class<?>, Class<?>>());

    @Override
    public <T extends View> T getView(Class<T> viewClass) {
        Class<? extends T> klass = getViewClass(viewClass);
        if (klass == null) {
            logger.log(Level.WARNING, "no view class registered for " + viewClass.toString());
            return null;
        }

        try {
            return klass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            logger.log(Level.WARNING, "error instantiaitng" + klass);
            return null;
        }
    }

    @Override
    public <T extends View> Class<? extends T> getViewClass(Class<T> viewClass) {
        // the cast is safe because the only way to insert an entry into the table is through
        // a method that enforces this constraint
        @SuppressWarnings("unchecked")
        Class<? extends T> result = (Class<? extends T>) lookupTable.get(viewClass);
        return result;
    }

    @Override
    public <T extends View> void setViewClass(Class<T> viewClass, Class<? extends T> implClass) {
        lookupTable.put(viewClass, implClass);
    }

}