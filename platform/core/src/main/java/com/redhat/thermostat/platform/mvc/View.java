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

package com.redhat.thermostat.platform.mvc;

import com.redhat.thermostat.beans.property.BooleanProperty;
import com.redhat.thermostat.platform.Platform;

/**
 * Toplevel View class for Thermostat components.
 *
 * <br /><br />
 *
 * <strong>Note</strong>: you should <strong>not</strong> export implementations
 * of this class as OSGi services directly, either via the {@literal @}Component
 * annotation or at runtime via service registration. You should instead use the
 * {@link com.redhat.thermostat.platform.annotations.PlatformService}
 * annotation to ensure the correct lifecycle management of this component.
 */
public class View implements MVCComponent {

    private BooleanProperty showing;

    protected Platform platform;

    public void create() {}

    public void init(Platform platform) {
        this.platform = platform;
        init();
    }

    protected void init() {}

    public void start() {}

    public void stop() {}

    public void destroy() {}

    public BooleanProperty showingProperty() {
        if (showing == null) {
            showing = new BooleanProperty();
        }
        return showing;
    }
}
