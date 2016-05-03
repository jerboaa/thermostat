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

package com.redhat.thermostat.killvm.client.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.Translate;

public class SwingVMKilledListener implements RequestResponseListener {

    private final Translate<LocaleResources> t;
    private static final Logger logger = LoggingUtils
            .getLogger(SwingVMKilledListener.class);

    public SwingVMKilledListener() {
        this.t = LocaleResources.createLocalizer();
    }
    
    @Override
    public void fireComplete(Request request, Response response) {
        switch (response.getType()) {
        case ERROR:
            String pid = request.getParameter("vm-pid");
            logger.log(Level.SEVERE,
                    "Kill request error for VM ID "
                            + pid);
            showErrorMessage(t.localize(LocaleResources.KILL_ACTION_ERROR_RESPONSE_MSG, pid).getContents());
            break;
        case OK:
            logger.log(Level.INFO,
                    "VM with id " + request.getParameter("vm-pid")
                            + " killed on host "
                            + request.getTarget().toString());
            break;
        default:
            logger.log(Level.WARNING, "Unknown result from KILL VM command.");
            break;
        }
    }
    
    /* 
     * protected for testing
     * 
     */
    protected void showErrorMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, message, "",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}

