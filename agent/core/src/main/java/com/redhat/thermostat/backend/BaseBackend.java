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

package com.redhat.thermostat.backend;

import java.util.Objects;

public abstract class BaseBackend implements Backend {

    private boolean observeNewJvm;
    
    private String name, description, vendor, version;

    public BaseBackend(String name, String description, String vendor, String version) {
        this(name, description, vendor, version, false);
    }

    public BaseBackend(String name, String description, String vendor, String version, boolean observeNewJvm) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.vendor = Objects.requireNonNull(vendor);
        this.version = Objects.requireNonNull(version);
        this.observeNewJvm = observeNewJvm;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getVendor() {
        return vendor;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean getObserveNewJvm() {
        return observeNewJvm;
    }

    @Override
    public void setObserveNewJvm(boolean newValue) {
        observeNewJvm = newValue;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, vendor, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BaseBackend other = (BaseBackend) obj;
        return Objects.equals(name, other.name) &&
                Objects.equals(version, other.version) &&
                Objects.equals(vendor, other.vendor);
    }

    @Override
    public String toString() {
        return "Backend [name=" + getName() + ", version=" + getVersion() + ", vendor=" + getVendor()
                + ", description=" + getDescription() + "]";
    }
}

