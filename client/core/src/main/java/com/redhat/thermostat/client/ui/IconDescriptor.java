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

package com.redhat.thermostat.client.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.StreamUtils;

import java.io.FileInputStream;

/**
 * Class that encapsulates an images raw data.
 * 
 * <br /><br />
 * 
 * An {@link IconDescriptor} is an image resource that needs to be processed
 * by higher level classes that can read, modify and display the image data.
 * 
 * <br /><br />
 * 
 * Since constructing an {@link IconDescriptor} can be very expensive, is highly
 * recommended to cache the result when possible.
 */
public class IconDescriptor {

    private static final Logger logger = LoggingUtils.getLogger(IconDescriptor.class);

    private ByteBuffer data;
    private int hash;
    
    public IconDescriptor(ByteBuffer data) {
        this.data = data;
        hash = data.hashCode();
    }

    public ByteBuffer getData() {
        return data;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + hash;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IconDescriptor other = (IconDescriptor) obj;
        if (hash != other.hash)
            return false;
        return true;
    }
    
    /**
     * Loads an icon from the given {@link InputStream}.
     */
    public static IconDescriptor loadIcon(InputStream stream) throws IOException {
        byte[] bytes = StreamUtils.readAll(stream);
        ByteBuffer data = ByteBuffer.wrap(bytes);
        return new IconDescriptor(data);
    }
    
    /**
     * Loads an icon from a file.
     */
    public static IconDescriptor loadIcon(File resource) throws IOException {
        return loadIcon(new FileInputStream(resource));
    }
    
    /**
     * Loads an icon by calling from the given resource file and {@link ClassLoader}.
     */
    public static IconDescriptor loadIcon(ClassLoader classloader, String resource) throws IOException {
        InputStream stream = classloader.getResourceAsStream(resource);
        if (stream == null) {
            throw new IOException("no resource found");
        }
        return loadIcon(stream);
    }
}

