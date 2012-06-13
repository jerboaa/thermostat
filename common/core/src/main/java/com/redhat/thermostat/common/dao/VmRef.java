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

package com.redhat.thermostat.common.dao;

public class VmRef implements Ref {

    private final HostRef hostRef;
    private final Integer uid;
    private final String uidString;
    private final String name;

    public VmRef(HostRef hostRef, Integer id, String name) {
        this.hostRef = hostRef;
        this.uid = id;
        this.uidString = id.toString();
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public HostRef getAgent() {
        return hostRef;
    }

    public Integer getId() {
        return uid;
    }

    public String getIdString() {
        return uidString;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        VmRef other = (VmRef) obj;
        if (equals(this.hostRef, other.hostRef) && equals(this.uid, other.uid) && equals(this.name, other.name)) {
            return true;
        }
        return false;
    }

    private static boolean equals(Object obj1, Object obj2) {
        return (obj1 == null && obj2 == null) || (obj1 != null && obj1.equals(obj2));
    }

    @Override
    public int hashCode() {
        return uid.hashCode();
    }

    @Override
    public String getStringID() {
        return getIdString();
    }
}
