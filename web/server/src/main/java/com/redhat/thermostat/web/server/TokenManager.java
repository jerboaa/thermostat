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


package com.redhat.thermostat.web.server;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

class TokenManager {

    private static final int TOKEN_LENGTH = 256;

    private final SecureRandom random = new SecureRandom();

    private final Map<String,byte[]> tokens = Collections.synchronizedMap(new HashMap<String,byte[]>());

    private final TokenManagerTimer timer;

    private int timeout = 30 * 1000;

    TokenManager(TimerRegistry registry) {
        this(registry, new TokenManagerTimer());
    }
    
    // for testing
    TokenManager(TimerRegistry registry, TokenManagerTimer timer) {
        this.timer = timer;
        registry.registerTimer(timer);
    }
    
    void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    byte[] generateToken(byte[] clientToken, String actionName) {
        byte[] token = new byte[TOKEN_LENGTH];
        random.nextBytes(token);
        final String clientKey = getKey(clientToken,
                Objects.requireNonNull(actionName));
        tokens.put(clientKey, token);
        scheduleRemoval(clientKey);
        return token;
    }

    private void scheduleRemoval(final String clientToken) {
        TimerTask task = new TimerTask() {
            
            @Override
            public void run() {
                tokens.remove(clientToken);
            }
        };
        timer.schedule(task, timeout);
    }
    
    private String getKey(byte[] clientToken, String actionName) {
        try {
            return getSha256HexString(clientToken, actionName.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // If this happens, this is clearly a bug.
            throw new RuntimeException(e);
        }
    }

    boolean verifyToken(byte[] clientToken, byte[] candidateToken, String actionName) {
        final String clientKey = getKey(clientToken, Objects.requireNonNull(actionName));
        if (tokens.containsKey(clientKey)) {
            byte[] storedToken = tokens.get(clientKey);
            boolean verified = Arrays.equals(candidateToken, storedToken);
            if (verified) {
                tokens.remove(clientToken);
            }
            return verified;
        }
        return false;
    }
    
    private String getSha256HexString(byte[] clientToken, byte[] actionName) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        digest.update(clientToken);
        digest.update(actionName);
        byte[] result = digest.digest();
        return convertBytesToHexString(result);
    }
    
    // package private for testing
    String convertBytesToHexString(byte[] shaBytes) {
        StringBuilder hexString = new StringBuilder();

        for (byte shaByte : shaBytes) {
            String hex = Integer.toHexString(0xff & shaByte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }

    // Used for testing only
    byte[] getStoredToken(String sha256) {
        return tokens.get(sha256);
    }

    static class TokenManagerTimer implements StoppableTimer {
        
        private static final String NAME = NAME_PREFIX + "token-manager";
        private final Timer timer;
        
        // for testing
        TokenManagerTimer(Timer timer) {
            this.timer = timer;
        }

        TokenManagerTimer() {
            this(new Timer(NAME));
        }

        @Override
        public void stop() {
            timer.cancel();
        }
        
        void schedule(TimerTask task, int timeout) {
            timer.schedule(task, timeout);
        }
        
    }
}

