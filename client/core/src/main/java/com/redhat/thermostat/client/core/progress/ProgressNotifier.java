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

package com.redhat.thermostat.client.core.progress;

import com.redhat.thermostat.annotations.Service;

/**
 * Provides graphical progress notification for long-running tasks.
 * <p>
 * Use this to notify the user that some long-running task is still running
 * without either blocking the UI or forcing the user to wait in the same part
 * of the application.
 * <p>
 * Obtain an instance of this via regular means (see {@link Service}) and then
 * create and {@link #register(ProgressHandle)} an appropriate
 * {@link ProgressHandle}.
 * <p>
 * An example of a simple progress notification that provides an indeterminate
 * progress while the task is executing:
 * <pre>
 * <code>
 * ProgressNotifier notifier = ... // get an instance
 * ProgressHandle handle = new ProgressHandle(new LocalizedString("test"));
 * handle.setIndeterminate(true);
 * handle.runTask(new Runnable() {
 *   public void run {
 *     // actual code to execute
 *   }
 * });
 * </code>
 * </pre>
 */
@Service
public interface ProgressNotifier {

    /**
     * Register a handle that is used to communicate progress of tasks
     */
    void register(ProgressHandle handle);

    // FIXME there's no reason to export this method
    // it's only ever used by implementations.
    boolean hasTasks();

}

