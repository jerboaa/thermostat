/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.client.ui;

import com.redhat.thermostat.common.Ordered;

/**
 * The {@link Decorator} interface offers a subtle, yet powerful way to
 * decorate visual clues in the UI.
 * 
 * <br /><br />
 *
 * Control over when the decoration takes place is up to the client framework,
 * generally the {@link Decorator} is queried any time there's an update on the
 * state of the context the decorator refers to.
 * 
 * <br /><br />
 * 
 * Also control on how the decoration is performed is up to the client. The
 * implementation, though, can define the order of the decoration, and
 * it is always guaranteed that the stacking order as defined by the
 * {@link Ordered} interface is respected. This means that decorators with a
 * lower index returned by {@link #getOrderValue()} will be executed first than
 * decorators with an higher index. This is important to consider when some
 * decorators want to define the base for other decorators or ensure that their
 * information is property ordered.
 * 
 * <br /><br />
 * 
 * While this represents the most powerful feature of decorators, it also
 * introduce an inherent instability, so decorators should be appropriately
 * tested in order to ensure there are no conflicts.
 * 
 * <br /><br />
 * 
 * Clients will track for Decorator subclasses exported as OSGi services, where
 * each subclass define the context of the decoration. The actual client
 * implementation may then have more than one entry point for decoration,
 * those entry point mark the actual target for the decorator itself. For this
 * reason the {@link Decorator#ID} property needs to point to the appropriate
 * value for the decoration. The value is client and context dependent.
 */
public interface Decorator extends Ordered {

    /**
     * Property for OSGi services indicating the target for this decorator.
     */
    public static final String ID = "Decorator_ID";
}
