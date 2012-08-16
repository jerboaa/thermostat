package com.redhat.thermostat.agent.command;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;

public interface RequestReceiver {

    public Response receive(Request request);

}
