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

package com.redhat.thermostat.client.swing;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

import com.redhat.thermostat.client.ui.IconDescriptor;

public class IconDescriptorTest {

    @Test
    public void test() throws IOException {
        ClassLoader classLoader = IconResource.class.getClassLoader();
        String resource = IconResource.JAVA_APPLICATION.getPath();
        IconDescriptor descriptor = IconDescriptor.loadIcon(classLoader, resource);
        ByteBuffer buffer = descriptor.getData();
        
        assertEquals(626, buffer.capacity());
        
        byte[] data = buffer.array();
        
        // the first four bytes should be .PNG
        int current = ((int) data[0]) & 0xFF;
        assertEquals(0x89, current);
        
        current = ((int) data[1]) & 0xFF;
        assertEquals(0x50, current);
        
        current = ((int) data[2]) & 0xFF;
        assertEquals(0x4E, current);
        
        current = ((int) data[3]) & 0xFF;
        assertEquals(0x47, current);
        
        // check IHDR chunk
        current = ((int) data[12]) & 0xFF;
        assertEquals(0x49, current);
        
        current = ((int) data[12]) & 0xFF;
        assertEquals(0x49, current);
        
        current = ((int) data[13]) & 0xFF;
        assertEquals(0x48, current);
        
        current = ((int) data[14]) & 0xFF;
        assertEquals(0x44, current);
        
        current = ((int) data[15]) & 0xFF;
        assertEquals(0x52, current);
        
        // get width and height, 4 bytes each, the icon is 16x16
        current = ((int) data[16]) << 24 | ((int) data[17]) << 16 | ((int) data[18]) << 8 | (((int) data[19]) & 0xFF);
        assertEquals(16, current);
        
        current = ((int) data[20]) << 24 | ((int) data[21]) << 16 | ((int) data[22]) << 8 | (((int) data[23]) & 0xFF);
        assertEquals(16, current);
    }
    
}

