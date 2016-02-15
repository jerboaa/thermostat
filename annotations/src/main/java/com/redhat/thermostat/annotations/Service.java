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

package com.redhat.thermostat.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates that the annotated class is a service available through OSGi.
 * Clients can obtain an instance of this service and use it directly.
 * <p>
 * An instance of this service (if one is registered) can be obtained using
 * {@link BundleContext#getService(ServiceReference)} or
 * {@link OSGIUtils#getService(Class)}.
 * <p>
 * The annotation is meant for service providers that wish to export a new
 * service via OSGi and not consumers, hence consumers must not mark their
 * classes or interfaces as {@code @Service}, nor directly extend the classes or
 * implement the interfaces marked as @Service. Only the bundle(s) that
 * publishes the service interface/class should do that.
 * <p>
 * Consumers wanting to extend functionality should make use of
 * {@code @ExtensionPoint}s instead.
 * <p>
 * This does not infer any behaviour on a class; this is for documentation
 * purposes only.
 *
 * @see ExtensionPoint
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface Service {

}

