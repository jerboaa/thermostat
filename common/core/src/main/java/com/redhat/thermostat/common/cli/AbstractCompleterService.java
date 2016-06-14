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

/**
 * An abstract {@link CompleterService} implementation which simply adds facilities for easy
 * dependency tracking.
 *
 * May be useful for CompleterServices which require access to a resource like a DAO to perform
 * their duties. In this situation, consider also passing the DependencyServices instance
 * through to a {@link CompletionFinder}, which can decide to provide no results if the
 * dependencies are unavailable, or use the dependency resources to gather results.
 */
public abstract class AbstractCompleterService implements CompleterService {

    protected DependencyServices dependencyServices = new DependencyServices();

    protected <T> void setService(Class<T> klazz, T t) {
        if (t != null) {
            dependencyServices.addService(klazz, t);
        } else {
            dependencyServices.removeService(klazz);
        }
    }

    protected void unsetService(Class<?> klazz) {
        setService(klazz, null);
    }

}
