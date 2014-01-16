/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.storage.internal.statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.core.IllegalPatchException;
import com.redhat.thermostat.storage.core.PreparedParameter;

public class LimitExpressionTest {
    
    private LimitExpression expn;
    
    @Before
    public void setup() {
        expn = new LimitExpression();
        UnfinishedLimitValue unfinished = new UnfinishedLimitValue();
        unfinished.setParameterIndex(0);
        expn.setValue(unfinished);
    }

    @Test
    public void canPatchWithInt() {
        PreparedParameter p = new PreparedParameter();
        p.setType(int.class);
        p.setValue(3);
        
        PatchedLimitExpression pLimit = null;
        try {
            pLimit = expn.patch(new PreparedParameter[] { p });
            // pass
        } catch (IllegalPatchException e) {
            fail(e.getMessage());
        }
        assertNotNull(pLimit);
        assertEquals(3, pLimit.getLimitValue());
    }
    
    @Test
    public void rejectPatchWithIntList() {
        PreparedParameter p = new PreparedParameter();
        p.setType(int[].class);
        p.setValue(new int[] { 3 });
        
        try {
            expn.patch(new PreparedParameter[] { p });
            fail("Should not be able to patch with int list");
        } catch (IllegalPatchException e) {
            // pass
            assertTrue(e.getMessage().contains("Invalid parameter type for limit expression."));
        }
    }
    
    @Test
    public void rejectPatchWithString() {
        PreparedParameter p = new PreparedParameter();
        p.setType(String.class);
        p.setValue("foo");
        
        try {
            expn.patch(new PreparedParameter[] { p });
            fail("Should not be able to patch with wrong type");
        } catch (IllegalPatchException e) {
            // pass
            assertTrue(e.getMessage().contains("Invalid parameter type for limit expression."));
        }
    }
}

