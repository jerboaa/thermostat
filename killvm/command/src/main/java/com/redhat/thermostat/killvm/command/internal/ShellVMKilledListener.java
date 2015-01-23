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

package com.redhat.thermostat.killvm.command.internal;

import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class ShellVMKilledListener implements RequestResponseListener {

    private static final Logger logger = LoggingUtils
            .getLogger(ShellVMKilledListener.class);
    private boolean complete = false;

    private PrintStream out;
    private PrintStream err;

    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void fireComplete(Request request, Response response) {
        String message;
        switch (response.getType()) {
            case ERROR:
                String pid = request.getParameter("vm-pid");
                message = "Kill request error for VM ID " + pid;
                logger.log(Level.SEVERE, message);
                if (err != null) {
                    err.println(message);
                }
                break;
            case OK:
                message = "VM with id " + request.getParameter("vm-pid") + " killed.";
                logger.log(Level.INFO, message);
                if (out != null) {
                    out.println(message);
                }
                break;
            default:
                message = "Unknown result from KILL VM command.";
                logger.log(Level.WARNING, message);
                if (out != null) {
                    out.println(message);
                }
                break;
        }
        latch.countDown();
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public void setErr(PrintStream err) {
        this.err = err;
    }

    public synchronized void await(long timeout) throws InterruptedException {
        this.latch.await(timeout, TimeUnit.MILLISECONDS);
    }
}
