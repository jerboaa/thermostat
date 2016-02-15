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

package com.redhat.thermostat.common.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Contains dependencies for a command.
 *
 * Provides ways to add ({@link #addService(Class, Object)}), remove (
 * {@link #removeService(Class)}) and fetch ({@link #getService(Class)})
 * services. Fetching has timeout support.
 */
public class DependencyServices {

    private Map<Class<?>, BlockingQueue<?>> serviceHolder = new HashMap<>();

    private <T> BlockingQueue<T> getHolder(Class<T> serviceClass) {
        if (!serviceHolder.containsKey(serviceClass)) {
            serviceHolder.put(serviceClass, new LinkedBlockingQueue<T>(1));
        }
        return (BlockingQueue<T>) serviceHolder.get(serviceClass);
    }

    /** @throws IllegalArgumentException if service is already added */
    public <T> void addService(Class<T> serviceClass, T item) {
        Objects.requireNonNull(item);
        BlockingQueue<T> holder = getHolder(serviceClass);
        boolean added = holder.offer(item);
        if (!added) {
            throw new IllegalArgumentException("service is already being tracked");
        }
    }

    public <T> void removeService(Class<T> serviceClass) {
        BlockingQueue<T> holder = getHolder(serviceClass);
        if (holder.peek() != null) {
            holder.remove();
        }
    }

    /** @return the service, or {@code null} */
    public <T> T getService(Class<T> serviceClass) {
        BlockingQueue<T> holder = getHolder(serviceClass);
        try {
            // a crappy version of peek()-with-timeout
            T retValue = holder.poll(500, TimeUnit.MILLISECONDS);
            if (retValue != null) {
                holder.add(retValue);
            }
            return retValue;
        } catch (InterruptedException e) {
            return null;
        }
    }

}
