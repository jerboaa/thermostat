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
 * A {@link CompletionFinder} with facilities for service dependency tracking.
 *
 * @see AbstractCompleterService
 */
public abstract class AbstractCompletionFinder implements CompletionFinder {

    protected final DependencyServices dependencyServices;

    /**
     * Construct an instance.
     *
     * The dependency services are expected to be passed in from the "parent" CompleterService,
     * and dependency addition/removal handled by the CompleterService as well, most likely
     * via the Activator that registered the CompleterService.
     * @param dependencyServices the dependency services
     */
    protected AbstractCompletionFinder(DependencyServices dependencyServices) {
        this.dependencyServices = dependencyServices;
    }

    /**
     * Get an instance of a service dependency, if any.
     * @param klazz the Class parameter
     * @param <T> the type of the class parameter
     * @return the service instance, or null if none available.
     * @see #allDependenciesAvailable()
     */
    protected <T> T getService(Class<T> klazz) {
        return dependencyServices.getService(klazz);
    }

    /**
     * Get the list of dependency classes required for this CompletionFinder to work correctly.
     *
     * If there are no required service dependencies, then you can implement {@link CompletionFinder}.
     * @return an array representing the list of dependencies.
     */
    protected abstract Class<?>[] getRequiredDependencies();

    /**
     * Helper method which returns true iff all dependencies listed by {@link #getRequiredDependencies()}
     * are currently available.
     *
     * Implementations of {@link #findCompletions()} should first call this method and, most likely,
     * return an empty list if it returns false.
     * @return
     */
    protected boolean allDependenciesAvailable() {
        boolean available = true;
        for (Class<?> cl : getRequiredDependencies()) {
            if (!dependencyServices.hasService(cl)) {
                available = false;
                break;
            }
        }
        return available;
    }

}
