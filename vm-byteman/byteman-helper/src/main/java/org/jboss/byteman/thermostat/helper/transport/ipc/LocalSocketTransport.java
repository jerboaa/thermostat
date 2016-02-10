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

package org.jboss.byteman.thermostat.helper.transport.ipc;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.jboss.byteman.thermostat.helper.BytemanMetric;
import org.jboss.byteman.thermostat.helper.Transport;
import org.jboss.byteman.thermostat.helper.TransportException;
import org.jboss.byteman.thermostat.helper.Utils;

/**
 * Transport implementation that sends records to the Thermostat agent over
 * local IPC
 *
 * @author akashche
 */
class LocalSocketTransport extends Transport {
    private final int batchSize;
    private final int attempts;
    private final long breakIntervalMillis;
    private final LocalSocketChannel channel;
    private final String socketName;

    /**
     * Constructor
     *
     * @param sendThreshold min number of records to cache before sending
     * @param loseThreshold max number of packages to cache
     * @param socketName the named server socket to connect to
     * @param batchSize number of records to send at once
     * @param attempts number of send attempts to repeat in case of error
     * @param breakIntervalMillis number of milliseconds to wait between the attempts
     * @param ipcConfig reference to the Thermostat IPC config file.
     * @param channelFactory A channel factory suitable for creating a new IPC channel
     */
    LocalSocketTransport(int sendThreshold, int loseThreshold, File ipcConfig, String socketName, int batchSize, int attempts, long breakIntervalMillis, LocalSocketChannelFactory channelFactory) {
        super(sendThreshold, loseThreshold);
        this.batchSize = batchSize;
        this.attempts = attempts;
        this.breakIntervalMillis = breakIntervalMillis;
        this.socketName = socketName;
        try {
            this.channel = channelFactory.open(ipcConfig, socketName);
        } catch (Exception e) {
            throw new TransportException("Error opening Thermostat socket: [" + socketName + "]", e);
        }
    }

    /**
     * Sends specified records o the Thermostat agent over
     * Thermostat local IPC
     *
     * @param records records to transfer
     */
    @Override
    protected void transferToPeer(ArrayList<BytemanMetric> records) {
        List<Exception> exList = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            try {
                tryToWrite(records);
                return;
            } catch (Exception e) {
                Utils.sleep(breakIntervalMillis);
                exList.add(e);
            }
        }
        System.err.println("ERROR: Error sending data to Thermostat socket: [" + socketName + "]," +
                " attempts count: [" + attempts + "], breakIntervalMillis: [" + breakIntervalMillis + "]");
        for (Exception ex : exList) {
            ex.printStackTrace();
        }
    }

    /**
     * Sends cached records and closes the channel
     */
    @Override
    public void close() {
        super.close();
        try {
            channel.close();
        } catch (IOException e) {
            System.err.println("WARNING: Thermostat error closing socket channel: [" + socketName + "]");
            e.printStackTrace();
        }
    }

    private void tryToWrite(ArrayList<BytemanMetric> records) throws IOException {
        ArrayList<BytemanMetric> batch = new ArrayList<>(batchSize);
        for (BytemanMetric re : records) {
            batch.add(re);
            if (batchSize == batch.size()) {
                writeBatch(batch);
                batch.clear();
            }
        }
        if (batch.size() > 0) {
            writeBatch(batch);
        }
    }

    private void writeBatch(ArrayList<BytemanMetric> records) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (BytemanMetric rec : records) {
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(rec.toJson());
        }
        sb.append("]");
        ByteBuffer envelope = ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("UTF-8")));
        channel.write(envelope);
    }

    // package-private for testing
    String getSocketName() {
        return socketName;
    }
    
    // package-private for testing
    LocalSocketChannel getChannel() {
        return channel;
    }

}
