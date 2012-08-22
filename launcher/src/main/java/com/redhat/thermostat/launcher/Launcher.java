/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.launcher;

import java.util.Collection;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.tools.ApplicationState;

/**
 * Launcher is the main entry point for all Thermostat commands.
 */
public interface Launcher {

    /**
     * Invoked in order to start a command, either when Thermostat starts, or within
     * the thermostat shell.  Equivalent to calling run(null).
     */
    void run();

    /**
     * Invoked in order to start a command, either when Thermostat starts, or within
     * the thermostat shell.  If the command being run happens to be a BasicCommand,
     * and the argument is non-null, the listeners will be added to the command for
     * life cycle notifications.  Otherwise, the argument is ignored.
     */
    void run(Collection<ActionListener<ApplicationState>> listeners);

    /**
     * Should be set before calling run()
     * @param command line arguments to the program
     */
    void setArgs(String[] args);

}
