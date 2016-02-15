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

package com.redhat.thermostat.vm.gc.common.params;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class CollectorInfo {

    private final JavaVersionRange javaVersionRange;
    private final String commonName;
    private final Set<String> collectorDistinctNames;
    private final String referenceUrl;

    public CollectorInfo(JavaVersionRange javaVersionRange, String commonName, Set<String> collectorDistinctNames, String referenceUrl) {
        this.javaVersionRange = requireNonNull(javaVersionRange);
        this.commonName = requireNonNull(commonName);
        this.collectorDistinctNames = new HashSet<>(requireNonNull(collectorDistinctNames));
        this.referenceUrl = requireNonNull(referenceUrl);
    }

    public JavaVersionRange getJavaVersionRange() {
        return javaVersionRange;
    }

    public String getCommonName() {
        return commonName;
    }

    public Set<String> getCollectorDistinctNames() {
        return new HashSet<>(collectorDistinctNames);
    }

    public String getReferenceUrl() {
        return referenceUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CollectorInfo that = (CollectorInfo) o;

        return Objects.equals(javaVersionRange, that.javaVersionRange)
                && Objects.equals(commonName, that.commonName)
                && Objects.equals(collectorDistinctNames, that.collectorDistinctNames)
                && Objects.equals(referenceUrl, that.referenceUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(javaVersionRange, commonName, collectorDistinctNames, referenceUrl);
    }
}
