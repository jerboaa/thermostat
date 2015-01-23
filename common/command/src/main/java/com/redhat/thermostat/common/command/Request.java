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

package com.redhat.thermostat.common.command;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>
 * A Request object represents a request passed from a client
 * to an agent. Each request is separate, complete and unordered. The
 * agent may or may not take an action when it receives a request.
 * </p>
 * <p>
 * Requests are meant for controlling the agent, not for asking an
 * agent nor sending an agent any sort of data.
 * </p>
 */
public class Request implements Message {
    
    private static final String RECEIVER = "receiver";
    
    public static final String CLIENT_TOKEN = "client-token";
    public static final String AUTH_TOKEN = "auth-token";
    public static final String ACTION = "action-name";
    public static final String UNKNOWN_HOSTNAME = "";

    private static final String FILTERED_PARAM_VALUE = "<filtered>";
    private static final Set<String> FILTERED_PARAMS;
    
    static {
        FILTERED_PARAMS = new HashSet<>();
        FILTERED_PARAMS.add(AUTH_TOKEN);
        FILTERED_PARAMS.add(CLIENT_TOKEN);
    }
    

    public enum RequestType implements MessageType {
        NO_RESPONSE_EXPECTED,
        RESPONSE_EXPECTED,
        MULTIPART_RESPONSE_EXPECTED;
    }

    private final RequestType type;
    private final Map<String, String> parameters;
    private final InetSocketAddress target;
    private final Collection<RequestResponseListener> listeners;


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
        Map<String, String> filteredParams = getFilteredParams(parameters);
        return "{ Request: {target = " + target.toString() + "}, {type = " +
                type.name() + "}, {parameters = " + filteredParams +
                "} }";
    }
    
    // package-private for testing
    Map<String, String> getFilteredParams(Map<String, String> unfiltered) {
        Map<String, String> filtered = new TreeMap<>();
        for (String key: unfiltered.keySet()) {
            if (FILTERED_PARAMS.contains(key)) {
                // actual value may be security sensitive
                filtered.put(key, FILTERED_PARAM_VALUE);
            } else {
                filtered.put(key, unfiltered.get(key));
            }
        }
        return filtered;
    }
    
}

