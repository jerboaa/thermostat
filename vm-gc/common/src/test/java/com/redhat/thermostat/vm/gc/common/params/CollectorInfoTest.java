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

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class CollectorInfoTest {

    private static final JavaVersionRange JAVA_VERSION = new JavaVersionRange(new JavaVersionRange.VersionPoints(1, 8, 0, 45));
    private static final String COMMON_NAME = "COMMON_NAME";
    private static final Set<String> COLLECTOR_DISTINCT_NAMES = new HashSet<String>() {{
        add("COLLECTOR1");
        add("COLLECTOR2");
    }};
    private static final String REFERENCE_URL = "http://example.com";

    @Test
    public void testGetters() {
        CollectorInfo collectorInfo = new CollectorInfo(JAVA_VERSION, COMMON_NAME, COLLECTOR_DISTINCT_NAMES, REFERENCE_URL);
        assertTrue(JAVA_VERSION.toString(), JAVA_VERSION.equals(collectorInfo.getJavaVersionRange()));
        assertTrue(COMMON_NAME, COMMON_NAME.equals(collectorInfo.getCommonName()));
        assertTrue(COLLECTOR_DISTINCT_NAMES.toString(), COLLECTOR_DISTINCT_NAMES.equals(collectorInfo.getCollectorDistinctNames()));
        assertTrue(REFERENCE_URL, REFERENCE_URL.equals(collectorInfo.getReferenceUrl()));
    }

    @Test(expected = NullPointerException.class)
    public void testNullVersionDisallowed() {
        new CollectorInfo(null, COMMON_NAME, COLLECTOR_DISTINCT_NAMES, REFERENCE_URL);
    }

    @Test(expected = NullPointerException.class)
    public void testNullCommonNameDisallowed() {
        new CollectorInfo(JAVA_VERSION, null, COLLECTOR_DISTINCT_NAMES, REFERENCE_URL);
    }

    @Test(expected = NullPointerException.class)
    public void testNullCollectorDistinctNamesDisallowed() {
        new CollectorInfo(JAVA_VERSION, COMMON_NAME, null, REFERENCE_URL);
    }

    @Test(expected = NullPointerException.class)
    public void testNullReferenceUrlDisallowed() {
        new CollectorInfo(JAVA_VERSION, COMMON_NAME, COLLECTOR_DISTINCT_NAMES, null);
    }


}
