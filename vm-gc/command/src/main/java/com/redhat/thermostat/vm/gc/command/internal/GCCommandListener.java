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

package com.redhat.thermostat.vm.gc.command.internal;

import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.Translate;

public class GCCommandListener implements RequestResponseListener {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final CountDownLatch latch = new CountDownLatch(1);

    private final Logger logger;
    private final PrintStream out;
    private final PrintStream err;

    // injectable logger intended for testing only
    GCCommandListener(Logger logger, PrintStream out, PrintStream err) {
        this.logger = logger;
        this.out = out;
        this.err = err;
    }

    GCCommandListener(PrintStream out, PrintStream err) {
        this(LoggingUtils.getLogger(GCCommandListener.class), out, err);
    }

    @Override
    public void fireComplete(Request request, Response response) {
        String message;
        String pid;

        switch (response.getType()) {
            case ERROR:
                pid = request.getParameter("VM_PID");
                message = "GC Request error for VM PID " + pid;
                logger.log(Level.SEVERE, message);
                if (err != null) {
                    err.println(translator.localize(LocaleResources.GC_ERROR_RESULT, pid).getContents());
                }
                break;
            case OK:
                pid = request.getParameter("VM_PID");
                message = "Garbage Collection performed on VM with PID " + pid;
                logger.log(Level.INFO, message);
                if (out != null) {
                    out.println(translator.localize(LocaleResources.GC_SUCCESS_RESULT, pid).getContents());
                }
                break;
            default:
                message = "Unknown result from GC command";
                logger.log(Level.WARNING, message);
                if (out != null) {
                    out.println(translator.localize(LocaleResources.GC_UNKNOWN_RESULT).getContents());
                }
                break;
        }
        latch.countDown();
    }

    public synchronized void await(long milliseconds) throws InterruptedException {
        this.latch.await(milliseconds, TimeUnit.MILLISECONDS);
    }

}
