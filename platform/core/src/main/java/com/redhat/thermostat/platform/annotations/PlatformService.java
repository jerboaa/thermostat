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

package com.redhat.thermostat.platform.annotations;

import com.redhat.thermostat.platform.mvc.MVCComponent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signal that the class annotated by this annotation must be exported as an
 * OSGi Service by the platform at Runtime. This is different than the
 * {@link org.osgi.service.component.annotations.Component} in that the class
 * is exported ar runtime at a predefined life cycle phase.
 *
 * <br /><br />
 *
 * The Annotation processor for this annotation will first check the services
 * element, if this is empty it will check the value and finally, if this
 * is also empty, it will use the name of the class itself.
 *
 * <br /><br />
 *
 * This annotation only has effect on instances returned via registered
 * {@link com.redhat.thermostat.platform.mvc.MVCProvider}s.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PlatformService {

    /**
     * Defines the list of
     * {@link com.redhat.thermostat.common.Constants.GENERIC_SERVICE_CLASSNAME}
     * to use when exporting this service.
     *
     */
    Class<? extends MVCComponent>[] service() default {};

    /**
     * Defines the name of this service
     * {@link com.redhat.thermostat.common.Constants.GENERIC_SERVICE_CLASSNAME}
     */
    String value() default "";
}
