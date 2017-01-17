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

package com.redhat.thermostat.storage.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.redhat.thermostat.storage.model.AgentInformation;

public class PreparedParamentersTest {

    @Test
    public void cannotSetNullPojo() {
        PreparedParameters params = new PreparedParameters(1);
        try {
            params.setPojo(0, null);
            fail("should not be able to set null Pojo");
        } catch (NullPointerException e) {
            // passs
        }
    }
    
    @Test
    public void cannotSetNullPojoList() {
        PreparedParameters params = new PreparedParameters(1);
        try {
            params.setPojoList(0, null);
            fail("should not be able to set null Pojo list");
        } catch (NullPointerException e) {
            // passs
        }
    }
    
    @Test
    public void canSetPojoType() {
        PreparedParameters params = new PreparedParameters(1);
        AgentInformation info = new AgentInformation(); 
        params.setPojo(0, info);
        assertEquals(AgentInformation.class, params.getParams()[0].getType());
        assertEquals(info, params.getParams()[0].getValue());
    }
    
    @Test
    public void canSetPojoListType() {
        PreparedParameters params = new PreparedParameters(1);
        AgentInformation info = new AgentInformation();
        params.setPojoList(0, new AgentInformation[] { info });
        PreparedParameter p = params.getParams()[0];
        assertEquals("Type should be component type", AgentInformation.class, p.getType());
        assertEquals(info, ((AgentInformation[])p.getValue())[0]);
    }
    
    @Test
    public void testTypeInt() {
        PreparedParameters params = new PreparedParameters(1);
        params.setInt(0, -1);
        assertEquals(int.class, params.getParams()[0].getType());
    }
    
    @Test
    public void testTypeIntList() {
        PreparedParameters params = new PreparedParameters(1);
        params.setIntList(0, new int[] { -1 });
        assertEquals(int[].class, params.getParams()[0].getType());
    }
    
    @Test
    public void testTypeLong() {
        PreparedParameters params = new PreparedParameters(1);
        params.setLong(0, -1);
        assertEquals(long.class, params.getParams()[0].getType());
    }
    
    @Test
    public void testTypeLongList() {
        PreparedParameters params = new PreparedParameters(1);
        params.setLongList(0, new long[] { -1 });
        assertEquals(long[].class, params.getParams()[0].getType());
    }
    
    @Test
    public void testTypeBoolList() {
        PreparedParameters params = new PreparedParameters(1);
        params.setBooleanList(0, new boolean[] { true } );
        assertEquals(boolean[].class, params.getParams()[0].getType());
    }
    
    @Test
    public void testTypeDouble() {
        PreparedParameters params = new PreparedParameters(1);
        params.setDouble(0, Math.E);
        assertEquals(double.class, params.getParams()[0].getType());
    }
    
    @Test
    public void testTypeDoubleList() {
        PreparedParameters params = new PreparedParameters(1);
        params.setDoubleList(0, new double[] { Math.E });
        assertEquals(double[].class, params.getParams()[0].getType());
    }
}

