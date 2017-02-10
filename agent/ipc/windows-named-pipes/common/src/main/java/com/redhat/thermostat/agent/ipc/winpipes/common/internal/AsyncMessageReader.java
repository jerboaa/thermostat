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

/**
 * The windows pipes Async reader behaves slightly differently.
 * There is no read() for it to call, instead it gets "fed" data from the ClientHandler until a complete message is built up
 */
public class AsyncMessageReader extends MessageReader {

    private final MessageListener listener;

    public AsyncMessageReader(MessageListener listener) {
        this(listener, new MessageLimits());
    }
    
    public AsyncMessageReader(MessageListener listener, MessageLimits limits) {
        super(limits);
        this.listener = listener;
    }

    /**
     * process the next incoming block of data
     *
     * @param data chunk of a message
     * @return true if the data completed the current message and called readFullMessage()
     * @throws IOException if there was a protocol error or corrupt message
     */
    public boolean process(ByteBuffer data) throws IOException {
        processData(data);
        return getState() == ReadState.NEW_MESSAGE;
    }

    @Override
    protected void readFullMessage(ByteBuffer fullMessage) {
        listener.messageRead(fullMessage);
    }

}
