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

package com.redhat.thermostat.agent.ipc.winpipes.common.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

public class WinPipesChannelImpl implements ByteChannel {
    
    private final WinPipesChannelHelper channelHelper;
    
    private final WinPipe pipe;
    
    protected WinPipesChannelImpl(WinPipe pipe) {
        this(pipe, new WinPipesChannelHelper());
    }

    protected WinPipesChannelImpl(WinPipe pipe, WinPipesChannelHelper helper) {
        this.pipe = pipe;
        this.channelHelper = helper;
    }

    public static WinPipesChannelImpl open(String channelname) throws IOException {
        final WinPipesChannelHelper helper = new WinPipesChannelHelper();
        final WinPipe.Id addr = helper.createAddress(channelname);
        final WinPipe pipe = helper.open(addr);
        return new WinPipesChannelImpl(pipe,helper);
    }

    public static WinPipesChannelImpl open(String channelname, WinPipesChannelHelper helper) throws IOException {
        final WinPipe.Id addr = helper.createAddress(channelname);
        final WinPipe pipe = helper.open(addr);
        return new WinPipesChannelImpl(pipe,helper);
    }
    
    public String getName() {
        return pipe.getPipeName();
    }

    /**
     * synchronous pipe read
     * @param dst data to read
     * @return number of bytes read or error if < 0
     * @throws IOException if there'a n IO error
     */
    @Override
    public int read(ByteBuffer dst) throws IOException {
        return channelHelper.read(pipe, dst);
    }

    /**
     * synchronous pipe write
     * @param src data to write
     * @return number of byte written
     * @throws IOException if theree's an IO error
     */
    @Override
    public int write(ByteBuffer src) throws IOException {
        return channelHelper.write(pipe, src);
    }
    
    @Override
    public boolean isOpen() {
        return channelHelper.isOpen(pipe);
    }

    @Override
    public void close() throws IOException {
        channelHelper.close(pipe);
    }
    
    // ---- For testing purposes ----
    
    // Wraps methods that can't be mocked
    static class WinPipesChannelHelper {
        WinPipe open(WinPipe.Id addr) throws IOException {
            WinPipe pipe = new WinPipe(addr);
            pipe.open();
            return pipe;
        }
        
        int read(WinPipe pipe, ByteBuffer dst) throws IOException {
            return pipe.read(dst);
        }
        
        int write(WinPipe pipe, ByteBuffer src) throws IOException {
            return pipe.write(src);
        }
        
        boolean isOpen(WinPipe pipe) {
            return pipe.isOpen();
        }
        
        void close(WinPipe pipe) throws IOException {
            pipe.close();
        }

        WinPipe.Id createAddress(final String channelname) throws IOException {
            return new WinPipe.Id(channelname);
        }
    }
}
