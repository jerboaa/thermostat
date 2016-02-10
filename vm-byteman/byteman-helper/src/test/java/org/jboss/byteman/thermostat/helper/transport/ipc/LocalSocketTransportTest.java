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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import org.jboss.byteman.thermostat.helper.BytemanMetric;
import org.jboss.byteman.thermostat.helper.Transport;
import org.jboss.byteman.thermostat.helper.Utils;
import org.jboss.byteman.thermostat.helper.transport.ipc.LocalSocketChannel;
import org.jboss.byteman.thermostat.helper.transport.ipc.LocalSocketChannelFactory;
import org.jboss.byteman.thermostat.helper.transport.ipc.LocalSocketTransport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class LocalSocketTransportTest {
    
    private LocalSocketChannelFactory factory;
    private LocalSocketChannel socketChannel;
    
    @Before
    public void setup() {
        factory = mock(LocalSocketChannelFactory.class);
        socketChannel = mock(LocalSocketChannel.class);
        when(factory.open(any(File.class), any(String.class))).thenReturn(socketChannel);
    }
    
    @Test
    public void instantiationOpensChannel() {
        try (Transport t = new LocalSocketTransport(0, 0, mock(File.class), "foo-name", 10, 1, 100, factory)) {
            // empty - using try-with-resources
        }
        verify(factory).open(any(File.class), eq("foo-name"));
    }

    @Test
    public void sendWritesToChannelInBatches() throws InterruptedException, IOException {
        ArgumentCaptor<ByteBuffer> byteBuffer = ArgumentCaptor.forClass(ByteBuffer.class);
        CountDownLatch latch = new CountDownLatch(1); // expect transfer to be called a single time
        try (SynchronizableLocalSocketTransport transport = new SynchronizableLocalSocketTransport(3, Integer.MAX_VALUE, "foo-name", 10, 1, 100, factory, latch)) {
            transport.send(new BytemanMetric("marker1", Utils.toMap(new Object[] { "key1", "value1" })));
            transport.send(new BytemanMetric("marker2", Utils.toMap(new Object[] { "key2", "value2" })));
            transport.send(new BytemanMetric("marker3", Utils.toMap(new Object[] { "key3", "value3" })));
            latch.await(); // wait for async transfer
            verify(socketChannel).write(byteBuffer.capture());
            assertEquals("expected transferToPeer() to be called once", 1, transport.callCount);
        }
        String actualJson = getFromByteBuffer(byteBuffer.getValue());
        String expectedJson = "[" +
                                    "{" +
                                        "\"marker\":\"marker1\"," +
                                        "\"data\":{" +
                                            "\"key1\":\"value1\"" +
                                        "}" +
                                    "}," +
                                    "{" +
                                        "\"marker\":\"marker2\"," +
                                        "\"data\":{" +
                                            "\"key2\":\"value2\"" +
                                        "}" +
                                    "}," +
                                    "{" +
                                        "\"marker\":\"marker3\"," +
                                        "\"data\":{" +
                                            "\"key3\":\"value3\"" +
                                        "}" +
                                    "}" +
                               "]";
        assertEquals(expectedJson, actualJson);
    }
    
    private String getFromByteBuffer(ByteBuffer byteBuffer) {
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return new String(bytes, Charset.forName("UTF-8"));
    }

    @Test
    public void closeClosesChannel() throws IOException {
        Transport transport = new LocalSocketTransport(2, 1024, mock(File.class), "test-me", 2, 16, 1000, factory);
        transport.close();
        verify(socketChannel).close();
    }
    
    private static class SynchronizableLocalSocketTransport extends LocalSocketTransport {

        private int callCount = 0;
        private final CountDownLatch sentLatch;
        SynchronizableLocalSocketTransport(int sendThreshold,
                                          int loseThreshold,
                                          String socketName,
                                          int batchSize,
                                          int attempts,
                                          long breakIntervalMillis,
                                          LocalSocketChannelFactory channelFactory,
                                          CountDownLatch sentLatch) {
            super(sendThreshold, loseThreshold, mock(File.class), socketName, batchSize, attempts,
                    breakIntervalMillis, channelFactory);
            this.sentLatch = sentLatch;
        }
        
        @Override
        protected void transferToPeer(ArrayList<BytemanMetric> records) {
            try {
                super.transferToPeer(records);
            } finally {
                sentLatch.countDown();
                callCount++;
            }
        }
        
    }

}
