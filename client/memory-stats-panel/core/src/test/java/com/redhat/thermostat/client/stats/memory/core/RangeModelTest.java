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

package com.redhat.thermostat.client.stats.memory.core;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.junit.Test;

public class RangeModelTest {

    @Test
    public void testSameRange() {
        RangeModel model = new RangeModel();
        
        model.setMaximum(10);
        model.setMinimum(0);
        model.setValue(5);
        
        model.setMaxNormalized(10);
        model.setMinNormalized(0);
        
        
        Assert.assertEquals((int) model.getValue(), model.getValueNormalized());
    }

    @Test
    public void testDoubleRange() {
        RangeModel model = new RangeModel();
        
        model.setMaximum(10);
        model.setMinimum(0);
        model.setValue(5);
        
        model.setMaxNormalized(20);
        model.setMinNormalized(0);
        
        
        Assert.assertEquals(10, model.getValueNormalized());
    }
    
    @Test
    public void testRanges() {
        RangeModel model = new RangeModel();
        
        model.setMaximum(10);
        model.setMinimum(0);
        model.setValue(5);
        
        model.setMaxNormalized(40);
        model.setMinNormalized(0);
                
        Assert.assertEquals(20, model.getValueNormalized());
        
        model.setMaxNormalized(60);
        model.setMinNormalized(0);
                
        Assert.assertEquals(30, model.getValueNormalized());
                
        model.setMaxNormalized(200);
        model.setMinNormalized(100);
                
        Assert.assertEquals(150, model.getValueNormalized());
        
        model.setMaximum(100);
        model.setMinimum(0);
        model.setValue(50);
        
        model.setMaxNormalized(1);
        model.setMinNormalized(0);
                
        Assert.assertEquals(1, model.getValueNormalized());
        
        model.setValue(49);
        Assert.assertEquals(0, model.getValueNormalized());
        
        model.setMaximum(1.0);
        model.setMinimum(0.0);
        model.setValue(0.5);
        
        model.setMaxNormalized(100);
        model.setMinNormalized(0);
        
        Assert.assertEquals(50, model.getValueNormalized());
        
        model.setValue(0.72);
        Assert.assertEquals(72, model.getValueNormalized());
    }
}
