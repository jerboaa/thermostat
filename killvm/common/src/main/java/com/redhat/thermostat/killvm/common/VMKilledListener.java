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

package com.redhat.thermostat.killvm.common;


import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.killvm.common.internal.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;

public class VMKilledListener implements RequestResponseListener {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final Logger logger = LoggingUtils
            .getLogger(VMKilledListener.class);

    @Override
    public void fireComplete(Request request, Response response) {
        String pid = request.getParameter("vm-pid");
        String message;
        switch (response.getType()) {
            case ERROR:
                message = translator.localize(LocaleResources.ERROR_RESPONSE, pid).getContents();
                logger.log(Level.SEVERE, message);
                onError(message);
                break;
            case OK:
                message = translator.localize(LocaleResources.OK_RESPONSE, pid).getContents();
                logger.log(Level.INFO, message);
                onOk(message);
                break;
            case NOK:
                message = translator.localize(LocaleResources.NOK_RESPONSE).getContents();
                logger.log(Level.WARNING, message);
                onNotOk(message);
                break;
            case NOOP:
                message = translator.localize(LocaleResources.NOOP_RESPONSE).getContents();
                logger.log(Level.WARNING, message);
                onNoOp(message);
                break;
            case AUTH_FAILED:
                message = translator.localize(LocaleResources.AUTH_FAILED_RESPONSE).getContents();
                logger.log(Level.WARNING, message);
                onAuthFail(message);
                break;
            default:
                message = translator.localize(LocaleResources.DEFAULT_RESPONSE).getContents();
                logger.log(Level.WARNING, message);
                onDefault(message);
                break;
        }

        onComplete();
    }

    protected void onError(String message) {
    }

    protected void onOk(String message) {
    }
    
    protected void onNotOk(String message) {
    }

    protected void onNoOp(String message) {
    }

    protected void onAuthFail(String message) {
    }

    protected void onDefault(String message) {
    }

    protected void onComplete() {
    }
}
