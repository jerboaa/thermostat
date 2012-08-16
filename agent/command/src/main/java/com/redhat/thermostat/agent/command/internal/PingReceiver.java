package com.redhat.thermostat.agent.command.internal;

import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;

public class PingReceiver implements RequestReceiver {

    @Override
    public Response receive(Request request) {
        return new Response(ResponseType.PONG);
    }

}
