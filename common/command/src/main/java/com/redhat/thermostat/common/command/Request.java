/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.common.command;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A Request object represents a request passed from a client
 * to an agent. Each request is separate, complete and unordered. The
 * agent may or may not take an action when it receives a request.
 * <p>
 * Requests are meant for controlling the agent, not for asking an
 * agent nor sending an agent any sort of data.
 * <p>
 * The following implementation details of this class are subject to
 * change at any time.
 * <p>
 * Request objects are serialized over the command channel in the following
 * format:
 * 
 * -------------------------
 * | A | TYPE | B | PARAMS |
 * -------------------------
 * 
 * A is an 32 bit integer representing the length - in bytes - of TYPE. TYPE
 * is a byte array representing the string of the request type (e.g.
 * "RESPONSE_EXPECTED") B is a 32 bit integer representing the number of
 * request parameters which follow.
 * 
 * PARAMS (if B > 0) is a variable length stream of the following format:
 * 
 * It is a simple encoding of name => value pairs.
 * 
 * -----------------------------------------------------------------------------------------------
 * | I_1 | K_1 | P_1 | V_1 | ... | I_(n-1) | K_(n-1) | P_(n-1) | V_(n-1) | I_n | K_n | P_n | V_n |
 * -----------------------------------------------------------------------------------------------
 * 
 * I_n  A 32 bit integer representing the length - in bytes - of the n'th
 *      parameter name.
 * K_n  A 32 bit integer representing the length - in bytes - of the n'th
 *      parameter value.
 * P_n  A byte array representing the string of the n'th parameter name.
 * V_n  A byte array representing the string of the n'th parameter value.
 * 
 */
public class Request implements Message {
    
    public static final String UNKNOWN_HOSTNAME = "";

    public enum RequestType implements MessageType {
        NO_RESPONSE_EXPECTED,
        RESPONSE_EXPECTED,
        MULTIPART_RESPONSE_EXPECTED;
    }

    private final RequestType type;
    private final Map<String, String> parameters;
    private final InetSocketAddress target;
    private final Collection<RequestResponseListener> listeners;

    private static final String RECEIVER = "receiver";

    public static final String CLIENT_TOKEN = "client-token";
    public static final String AUTH_TOKEN = "auth-token";
    public static final String ACTION = "action-name";

    public Request(RequestType type, InetSocketAddress target) {
        this.type = type;
        parameters = new TreeMap<>();
        this.target = target;
        listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public MessageType getType() {
        return type;
    }

    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public Collection<String> getParameterNames() {
        return parameters.keySet();
    }

    public void setReceiver(String clazz) {
        setParameter(RECEIVER, clazz);
    }

    public String getReceiver() {
        return getParameter(RECEIVER);
    }

    public InetSocketAddress getTarget() {
        return target;
    }

    public void addListener(RequestResponseListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RequestResponseListener listener) {
        listeners.remove(listener);
    }

    public Collection<RequestResponseListener> getListeners() {
        return Collections.unmodifiableCollection(listeners);
    }
    
    @Override
    public String toString() {
        return "{ Request: {target = " + target.toString() + "}, {type = " + type.name() + "} }";
    }
}

