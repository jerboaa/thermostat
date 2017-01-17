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

package com.redhat.thermostat.dev.ipc.test.client.internal;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.redhat.thermostat.agent.ipc.client.ClientIPCService;
import com.redhat.thermostat.agent.ipc.client.ClientIPCServiceFactory;
import com.redhat.thermostat.agent.ipc.client.IPCMessageChannel;

public class UnixSocketTestClient {
    
    // Filename prefix for socket file
    private static final String SOCKET_NAME = "test";
    // Number of iterations of this test program
    private static final int NUM_ITERATIONS = 20;
    // Number of messages for this client to send, not taking into account messages automatically
    // broken up into multipart messages
    private static final int NUM_MESSAGES = 10;
    
    private static List<Thread> clientThreads;
    private static volatile Throwable threadEx;
    
    private static int threadCount = 1;
    private static ClientIPCService ipcService;
    
    public static void main(final String[] args) throws IOException {
        if (args.length == 0) {
            throw new RuntimeException("Number of active servers must be specified");
        }
        
        int numServers = Integer.parseInt(args[0]);
        ipcService = ClientIPCServiceFactory.getIPCService(new File(args[1]));
        for (int i = 0; i < NUM_ITERATIONS; i++) {
            // Spawn a thread for each server name argument
            clientThreads = new ArrayList<>();
            // Randomly choose servers to connect to
            List<String> serverNames = chooseServers(numServers);
            System.out.println("Communicating with servers: " + serverNames);
            for (final String serverName : serverNames) {
                Thread clientThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            communicateWithServer(serverName);
                        } catch (IOException e) {
                            threadEx = e;
                        }
                    }
                }, "TestClient-" + threadCount);
                System.out.println("Creating new client thread #" + threadCount++);

                clientThreads.add(clientThread);
                clientThread.setDaemon(true);
                clientThread.start();
            }

            try {
                for (Thread thread : clientThreads) {
                    thread.join();
                }
            } catch (InterruptedException e) {
                System.err.println("Interrupted while joining client threads");
                Thread.currentThread().interrupt();
            }

            if (threadEx != null) {
                throw new RuntimeException("Exception in client thread", threadEx);
            }
        }
    }
    
    private static void communicateWithServer(String serverName) throws IOException {
        IPCMessageChannel channel = ipcService.connectToServer(serverName);
        System.out.println("Connected to server \"" + serverName + "\"");
        
        String[] questions = new DummyReceiver().getQuestions();
        Random random = new Random();
        for (int i = 0; i < NUM_MESSAGES; i++) {
            // Generate pseudo-random question, potentially unknown to the receiver
            int value = random.nextInt(questions.length + 2);
            String question = "What is the meaning of life?";
            if (value < questions.length) {
                question = questions[value];
            }
            
            // Write message to server
            byte[] questionAsBytes = question.getBytes("UTF-8");
            if (questionAsBytes == null) {
                throw new IOException("Received null response from server");
            }
            ByteBuffer buf = ByteBuffer.wrap(questionAsBytes);
            channel.writeMessage(buf);
            System.out.println("Wrote message: " + question);
            
            // Read reply from server
            ByteBuffer result = channel.readMessage();
            CharBuffer charBuf = Charset.forName("UTF-8").decode(result);
            String receivedMsg = charBuf.toString();
            System.out.println("Client received message: \"" + receivedMsg + "\"");
            
            // Verify against what we expected
            DummyReceiver dummy = new DummyReceiver();
            String expected = dummy.answer(question);
            if (!expected.equals(receivedMsg)) {
                throw new IOException("RECEIVED WRONG MESSAGE! Expected: " + expected + ", Received: " + receivedMsg);
            }
        }
        
        channel.close();
    }
    
    private static List<String> chooseServers(int numServers) {
        List<String> result = new ArrayList<>();
        // Populate result with all servers
        for (int i = 0; i < numServers; i++) {
            result.add(SOCKET_NAME + String.valueOf(i));
        }
        Random rand = new Random();
        // Choose random number of servers to remove from result
        int nServers = rand.nextInt(result.size());
        for (int i = 0; i < nServers; i++) {
            // Choose random server to remove 
            int removeMe = rand.nextInt(result.size());
            result.remove(removeMe);
        }
        return result;
    }
    
}
