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

package com.redhat.thermostat.common.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class InterpolatorTest {
    @Test
    public void lerp_integers() throws Exception {
        int result = Interpolator.lerp(0, 10, 1);
        assertEquals(10, result);

        result = Interpolator.lerp(0, 10, 0);
        assertEquals(0, result);

        result = Interpolator.lerp(0, 10, 0.5f);
        assertEquals(5, result);

        result = Interpolator.lerp(0, 10, 1.f/5.f);
        assertEquals(2, result);

        result = Interpolator.lerp(5, 10, .5f);
        assertEquals(7, result);
    }

    @Test
    public void lerp_float() throws Exception {
        float result = Interpolator.lerp(0.f, 10.f, 1.f);
        assertEquals(10.f, result, 0);

        result = Interpolator.lerp(0.f, 10.f, 0.f);
        assertEquals(0.f, result, 0);

        result = Interpolator.lerp(0.f, 10.f, .57f);
        assertEquals(5.699f, result, 0.001);

        result = Interpolator.lerp(0.f, 10.f, .5f);
        assertEquals(5.f, result, 0);
    }
}
