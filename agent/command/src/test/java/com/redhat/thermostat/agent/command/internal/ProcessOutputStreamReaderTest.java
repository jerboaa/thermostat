/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.agent.command.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.command.internal.ProcessOutputStreamReader.ProcessOutputStreamRequestListener;
import com.redhat.thermostat.agent.command.internal.ProcessStreamReader.ExceptionListener;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;

public class ProcessOutputStreamReaderTest {
    
    private ProcessOutputStreamRequestListener listener;
    private ExceptionListener exListener;
    
    @Test
    public void testSuccessNoParams() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        byte[] testData = getRequestAsBytes(req);
        
        parseRequest(testData);
        Request request = getRequest();
        
        assertEquals(RequestType.RESPONSE_EXPECTED, request.getType());
        assertEquals(new InetSocketAddress("127.0.0.1", 12222), request.getTarget());
        assertEquals(Collections.emptySet(), request.getParameterNames());
    }

    private byte[] getRequestAsBytes(Request req) throws IOException {
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF(req.getTarget().getHostString());
        dos.writeInt(req.getTarget().getPort());
        TestRequestEncoder encoder = new TestRequestEncoder();
        ChannelBuffer buf = encoder.encode(req);
        byte[] reqBytes = buf.toByteBuffer().array();
        dos.writeInt(reqBytes.length);
        dos.write(reqBytes);
        dos.writeUTF(CommandChannelConstants.END_REQUEST_TOKEN);
        dos.close();
        return testData.toByteArray();
    }

    @Test
    public void testSuccessOneParam() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        req.setReceiver("com.example.MyReceiver");
        byte[] testData = getRequestAsBytes(req);
        
        parseRequest(testData);
        Request request = getRequest();
        
        assertEquals(RequestType.RESPONSE_EXPECTED, request.getType());
        assertEquals(new InetSocketAddress("127.0.0.1", 12222), request.getTarget());
        assertEquals("com.example.MyReceiver", request.getReceiver());
    }
    
    @Test
    public void testSuccessMultiParams() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        req.setReceiver("com.example.MyReceiver");
        req.setParameter("param", "value");
        byte[] testData = getRequestAsBytes(req);
        
        parseRequest(testData);
        Request request = getRequest();
        
        assertEquals(RequestType.RESPONSE_EXPECTED, request.getType());
        assertEquals(new InetSocketAddress("127.0.0.1", 12222), request.getTarget());
        assertEquals("com.example.MyReceiver", request.getReceiver());
        assertEquals("value", request.getParameter("param"));
    }

    @Test
    public void testEOFBeginToken() throws InterruptedException, IOException {
        // This should not be an error
        parseRequest(new byte[0]);
        verify(listener, never()).requestReceived(any(Request.class));
    }
    
    @Test
    public void testBadBeginToken() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        byte[] testData = getBadBeginTokenRequestAsBytes(req);
        parseRequest(testData);
        verify(listener, never()).requestReceived(any(Request.class));
        
        IOException ex = getFirstException();
        assertTrue(CommandChannelIOException.class.isInstance(ex));
    }
    
    private byte[] getBadBeginTokenRequestAsBytes(Request req) throws IOException {
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF("<BEGIN REQ>");
        dos.writeUTF(req.getTarget().getHostString());
        dos.writeInt(req.getTarget().getPort());
        TestRequestEncoder encoder = new TestRequestEncoder();
        ChannelBuffer buf = encoder.encode(req);
        byte[] reqBytes = buf.toByteBuffer().array();
        dos.writeInt(reqBytes.length);
        dos.write(reqBytes);
        dos.writeUTF(CommandChannelConstants.END_REQUEST_TOKEN);
        dos.close();
        return testData.toByteArray();
    }
    
    @Test
    public void testEOFHostname() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        byte[] testData = getEOFHostnameRequestAsBytes(req);
        parseRequest(testData);
        verify(listener, never()).requestReceived(any(Request.class));
        IOException ex = getFirstException();
        assertTrue(EOFException.class.isInstance(ex));
    }
    
    private byte[] getEOFHostnameRequestAsBytes(Request req) throws IOException {
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.close();
        return testData.toByteArray();
    }
    
    @Test
    public void testEOFPort() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        byte[] testData = getEOFPortRequestAsBytes(req);
        parseRequest(testData);
        verify(listener, never()).requestReceived(any(Request.class));
        IOException ex = getFirstException();
        assertTrue(EOFException.class.isInstance(ex));
    }
    
    private byte[] getEOFPortRequestAsBytes(Request req) throws IOException {
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF("127.0.0.1");
        dos.close();
        return testData.toByteArray();
    }
    
    @Test
    public void testNotIntPort() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        byte[] testData = getNotIntPortRequestAsBytes(req);
        parseRequest(testData);
        verify(listener, never()).requestReceived(any(Request.class));
        IOException ex = getFirstException();
        assertTrue(CommandChannelIOException.class.isInstance(ex));
    }
    
    private byte[] getNotIntPortRequestAsBytes(Request req) throws IOException {
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF(req.getTarget().getHostString());
        dos.writeUTF("hello");
        TestRequestEncoder encoder = new TestRequestEncoder();
        ChannelBuffer buf = encoder.encode(req);
        byte[] reqBytes = buf.toByteBuffer().array();
        dos.writeInt(reqBytes.length);
        dos.write(reqBytes);
        dos.writeUTF(CommandChannelConstants.END_REQUEST_TOKEN);
        dos.close();
        return testData.toByteArray();
    }
    
    @Test
    public void testPortOutOfRange() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        byte[] testData = getPortOutOfRangeRequestAsBytes(req);
        parseRequest(testData);
        verify(listener, never()).requestReceived(any(Request.class));
        IOException ex = getFirstException();
        assertTrue(CommandChannelIOException.class.isInstance(ex));
    }
    
    private byte[] getPortOutOfRangeRequestAsBytes(Request req) throws IOException {
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF(req.getTarget().getHostString());
        dos.writeInt(200000);
        TestRequestEncoder encoder = new TestRequestEncoder();
        ChannelBuffer buf = encoder.encode(req);
        byte[] reqBytes = buf.toByteBuffer().array();
        dos.writeInt(reqBytes.length);
        dos.write(reqBytes);
        dos.writeUTF(CommandChannelConstants.END_REQUEST_TOKEN);
        dos.close();
        return testData.toByteArray();
    }
    
    @Test
    public void testEOFEncodedLength() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        byte[] testData = getEOFEncodedLengthRequestAsBytes(req);
        parseRequest(testData);
        verify(listener, never()).requestReceived(any(Request.class));
        IOException ex = getFirstException();
        assertTrue(CommandChannelIOException.class.isInstance(ex));
    }
    
    private byte[] getEOFEncodedLengthRequestAsBytes(Request req) throws IOException {
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF(req.getTarget().getHostString());
        dos.writeInt(12222);
        TestRequestEncoder encoder = new TestRequestEncoder();
        ChannelBuffer buf = encoder.encode(req);
        byte[] reqBytes = buf.toByteBuffer().array();
        dos.write(reqBytes);
        dos.writeUTF(CommandChannelConstants.END_REQUEST_TOKEN);
        dos.close();
        return testData.toByteArray();
    }
    
    @Test
    public void testMissingRequestType() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        byte[] testData = getMissingTypeRequestAsBytes(req);
        parseRequest(testData);
        verify(listener, never()).requestReceived(any(Request.class));
        IOException ex = getFirstException();
        assertTrue(CommandChannelIOException.class.isInstance(ex));
    }

    private byte[] getMissingTypeRequestAsBytes(Request req) throws IOException {
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF(req.getTarget().getHostString());
        dos.writeInt(req.getTarget().getPort());
        TestRequestEncoder encoder = new TestRequestEncoder();
        ChannelBuffer buf = encoder.encode(req, null, null);
        byte[] reqBytes = buf.toByteBuffer().array();
        dos.writeInt(reqBytes.length);
        dos.write(reqBytes);
        dos.writeUTF(CommandChannelConstants.END_REQUEST_TOKEN);
        dos.close();
        return testData.toByteArray();
    }
    
    @Test
    public void testMissingParams() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        req.setParameter("bad", "param");
        byte[] testData = getMissingParamsRequestAsBytes(req);
        parseRequest(testData);
        verify(listener, never()).requestReceived(any(Request.class));
        IOException ex = getFirstException();
        assertTrue(CommandChannelIOException.class.isInstance(ex));
    }

    private byte[] getMissingParamsRequestAsBytes(Request req) throws IOException {
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF(req.getTarget().getHostString());
        dos.writeInt(req.getTarget().getPort());
        TestRequestEncoder encoder = new TestRequestEncoder();
        ChannelBuffer buf = encoder.encode(req, "RESPONSE_EXPECTED", "bad");
        byte[] reqBytes = buf.toByteBuffer().array();
        dos.writeInt(reqBytes.length);
        dos.write(reqBytes);
        dos.writeUTF(CommandChannelConstants.END_REQUEST_TOKEN);
        dos.close();
        return testData.toByteArray();
    }

    @Test
    public void testBadRequestType() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        byte[] testData = getBadTypeRequestAsBytes(req);
        parseRequest(testData);
        verify(listener, never()).requestReceived(any(Request.class));
        IOException ex = getFirstException();
        assertTrue(CommandChannelIOException.class.isInstance(ex));
    }

    private byte[] getBadTypeRequestAsBytes(Request req) throws IOException {
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF(req.getTarget().getHostString());
        dos.writeInt(req.getTarget().getPort());
        TestRequestEncoder encoder = new TestRequestEncoder();
        ChannelBuffer buf = encoder.encode(req, "BadType", null);
        byte[] reqBytes = buf.toByteBuffer().array();
        dos.writeInt(reqBytes.length);
        dos.write(reqBytes);
        dos.writeUTF(CommandChannelConstants.END_REQUEST_TOKEN);
        dos.close();
        return testData.toByteArray();
    }
    
    @Test
    public void testMissingEncodedRequest() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        byte[] testData = getMissingEncodedRequestAsBytes(req);
        parseRequest(testData);
        verify(listener, never()).requestReceived(any(Request.class));
        IOException ex = getFirstException();
        assertTrue(CommandChannelIOException.class.isInstance(ex));
    }

    private byte[] getMissingEncodedRequestAsBytes(Request req) throws IOException {
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF(req.getTarget().getHostString());
        dos.writeInt(req.getTarget().getPort());
        dos.writeInt(0);
        dos.writeUTF(CommandChannelConstants.END_REQUEST_TOKEN);
        dos.close();
        return testData.toByteArray();
    }

    @Test
    public void testEOFEndToken() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        byte[] testData = getEOFEndTokenRequestAsBytes(req);
        parseRequest(testData);
        verify(listener, never()).requestReceived(any(Request.class));
        IOException ex = getFirstException();
        assertTrue(EOFException.class.isInstance(ex));
    }
    
    private byte[] getEOFEndTokenRequestAsBytes(Request req) throws IOException {
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF(req.getTarget().getHostString());
        dos.writeInt(req.getTarget().getPort());
        TestRequestEncoder encoder = new TestRequestEncoder();
        ChannelBuffer buf = encoder.encode(req);
        byte[] reqBytes = buf.toByteBuffer().array();
        dos.writeInt(reqBytes.length);
        dos.write(reqBytes);
        dos.close();
        return testData.toByteArray();
    }
    
    @Test
    public void testBadEndToken() throws InterruptedException, IOException {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        byte[] testData = getBadEndTokenRequestAsBytes(req);
        parseRequest(testData);
        verify(listener, never()).requestReceived(any(Request.class));
        IOException ex = getFirstException();
        assertTrue(CommandChannelIOException.class.isInstance(ex));
    }
    
    private byte[] getBadEndTokenRequestAsBytes(Request req) throws IOException {
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF(req.getTarget().getHostString());
        dos.writeInt(req.getTarget().getPort());
        TestRequestEncoder encoder = new TestRequestEncoder();
        ChannelBuffer buf = encoder.encode(req);
        byte[] reqBytes = buf.toByteBuffer().array();
        dos.writeInt(reqBytes.length);
        dos.write(reqBytes);
        dos.writeUTF("<END REQ>");
        dos.close();
        return testData.toByteArray();
    }
    
    @Test
    public void testEarlyEnd() throws IOException, InterruptedException {
        // Create a request with a parameter designed to mimic the CommandChannelConstants.END_REQUEST_TOKEN token,
        // and give an incorrect encoded length pointing the byte before the parameter name.
        // The encoded length should always be correct when receiving the request from the server,
        // but this test ensures this class can handle malicious input.
        Request req1 = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeUTF(CommandChannelConstants.END_REQUEST_TOKEN);
        dos.close();
        String encodedEndRequest = new String(baos.toByteArray());
        req1.setParameter(encodedEndRequest, "");
        
        // Try another request to make sure the command channel is still usable
        Request req2 = new Request(RequestType.NO_RESPONSE_EXPECTED, new InetSocketAddress("localhost", 123));
        req2.setParameter("hello", "world");
        
        byte[] testData = getEarlyEndRequestsAsBytes(req1, encodedEndRequest.getBytes().length, req2);
        parseRequest(testData);
        List<Request> requests = getRequests();
        
        assertEquals(2, requests.size());
        Request result = requests.get(0);
        assertEquals(RequestType.RESPONSE_EXPECTED, result.getType());
        assertEquals(new InetSocketAddress("127.0.0.1", 12222), result.getTarget());
        assertEquals(Collections.emptySet(), result.getParameterNames());
        
        result = requests.get(1);
        assertEquals(RequestType.NO_RESPONSE_EXPECTED, result.getType());
        assertEquals(new InetSocketAddress("localhost", 123), result.getTarget());
        assertEquals(1, result.getParameterNames().size());
        assertEquals("world", result.getParameter("hello"));
    }
    
    private byte[] getEarlyEndRequestsAsBytes(Request req1, int subLength, Request req2) throws IOException {
        // First request ends early
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF(req1.getTarget().getHostString());
        dos.writeInt(req1.getTarget().getPort());
        TestRequestEncoder encoder = new TestRequestEncoder();
        // Replace the number of parameters with 0
        ChannelBuffer buf = encoder.encode(req1, "RESPONSE_EXPECTED", null, 0);
        byte[] reqBytes = buf.toByteBuffer().array();
        dos.writeInt(reqBytes.length - subLength);
        dos.write(reqBytes);
        
        // Try another request to ensure the first malformed request doesn't
        // affect future request
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF(req2.getTarget().getHostString());
        dos.writeInt(req2.getTarget().getPort());
        buf = encoder.encode(req2);
        reqBytes = buf.toByteBuffer().array();
        dos.writeInt(reqBytes.length);
        dos.write(reqBytes);
        dos.writeUTF(CommandChannelConstants.END_REQUEST_TOKEN);
        dos.close();
        return testData.toByteArray();
    }

    @Test
    public void testGarbageAtEnd() throws IOException, InterruptedException {
        // Create a request with extra data at the end, which is not included
        // in the request length.
        // Note: This garbage data can cause any following data in the stream to
        // be read and discarded. In practice this should never happen because
        // the command channel should not send extraneous data.
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 12222));
        byte[] testData = getRequestWithExtraData(req);
        try {
            parseRequest(testData);
        } catch (IOException e) {
            // Pass
        }
        
        // We should still have parsed a valid request
        Request result = getRequest();
        assertEquals(RequestType.RESPONSE_EXPECTED, result.getType());
        assertEquals(new InetSocketAddress("127.0.0.1", 12222), result.getTarget());
        assertEquals(Collections.emptySet(), result.getParameterNames());
    }
    
    private byte[] getRequestWithExtraData(Request req) throws IOException {
        // First request has extra garbage data at the end
        ByteArrayOutputStream testData = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(testData);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF(req.getTarget().getHostString());
        dos.writeInt(req.getTarget().getPort());
        TestRequestEncoder encoder = new TestRequestEncoder();
        ChannelBuffer buf = encoder.encode(req);
        byte[] reqBytes = buf.toByteBuffer().array();
        dos.writeInt(reqBytes.length);
        dos.write(reqBytes);
        dos.writeUTF(CommandChannelConstants.END_REQUEST_TOKEN);
        Random rand = new Random();
        byte[] garbage = new byte[32];
        rand.nextBytes(garbage);
        dos.write(garbage);
        dos.close();
        return testData.toByteArray();
    }   

    private Request getRequest() {
        List<Request> requests = getRequests();
        assertEquals(1, requests.size());
        return requests.get(0);
    }
    
    private List<Request> getRequests() {
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(listener, atLeastOnce()).requestReceived(requestCaptor.capture());
        List<Request> requests = requestCaptor.getAllValues();
        return requests;
    }
    
    private IOException getFirstException() {
        List<IOException> exceptions = getExceptions();
        //assertEquals(1, exceptions.size());
        return exceptions.get(0);
    }
    
    private List<IOException> getExceptions() {
        ArgumentCaptor<IOException> exceptionCaptor = ArgumentCaptor.forClass(IOException.class);
        verify(exListener, atLeastOnce()).notifyException(exceptionCaptor.capture());
        List<IOException> exceptions = exceptionCaptor.getAllValues();
        return exceptions;
    }

    private void parseRequest(byte[] testData) throws InterruptedException, IOException {
        InputStream is = new ByteArrayInputStream(testData);
        listener = mock(ProcessOutputStreamRequestListener.class);
        exListener = mock(ExceptionListener.class);
        
        ProcessOutputStreamReader reader = new ProcessOutputStreamReader(is, listener, exListener);
        reader.run();
    }

}
