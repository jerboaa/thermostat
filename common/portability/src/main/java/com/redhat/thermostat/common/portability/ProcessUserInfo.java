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

package com.redhat.thermostat.common.portability;

import com.redhat.thermostat.common.portability.internal.UnimplementedError;
import com.redhat.thermostat.common.portability.internal.linux.LinuxProcessUserInfoBuilderImpl;
import com.redhat.thermostat.common.portability.internal.macos.MacOSUserInfoBuilderImpl;
import com.redhat.thermostat.common.portability.linux.ProcDataSource;
import com.redhat.thermostat.common.portability.internal.windows.WindowsUserInfoBuilderImpl;
import com.redhat.thermostat.shared.config.OS;

public class ProcessUserInfo {

    private long uid;
    private String username;

    public ProcessUserInfo(long uid, String username) {
        this.uid = uid;
        this.username = username;
    }

    public ProcessUserInfo() {
        this.uid = -1;
        this.username = null;
    }

    public long getUid() {
        return uid;
    }

    public String getUsername() {
        return username;
    }

    public static ProcessUserInfoBuilder createBuilder(ProcDataSource source, UserNameUtil userNameUtil) {
        final ProcessUserInfoBuilder builder;
        if (OS.IS_LINUX) {
            builder = new LinuxProcessUserInfoBuilderImpl(source, userNameUtil);
        }
        else if (OS.IS_WINDOWS) {
            builder = new WindowsUserInfoBuilderImpl();
        }
        else if (OS.IS_MACOS) {
            builder = new MacOSUserInfoBuilderImpl();
        }
        else {
            throw new UnimplementedError("ProcessUserInfo");
        }
        return builder;
    }

    public static ProcessUserInfoBuilder createBuilder() {
        final ProcessUserInfoBuilder builder;
        if (OS.IS_LINUX) {
            builder = new LinuxProcessUserInfoBuilderImpl();
        }
        else if (OS.IS_WINDOWS) {
            builder = new WindowsUserInfoBuilderImpl();
        }
        else {
            builder = new MacOSUserInfoBuilderImpl();
        }
        return builder;
    }
}
