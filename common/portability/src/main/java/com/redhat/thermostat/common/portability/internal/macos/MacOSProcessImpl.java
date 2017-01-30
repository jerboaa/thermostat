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

package com.redhat.thermostat.common.portability.internal.macos;

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.portability.PortableProcess;
import com.redhat.thermostat.common.portability.PortableProcessStat;
import com.redhat.thermostat.common.portability.PortableVmIoStat;

import java.util.Map;

public class MacOSProcessImpl implements PortableProcess {

    public static final MacOSProcessImpl INSTANCE = new MacOSProcessImpl();
    private static final MacOSHelperImpl helper = MacOSHelperImpl.INSTANCE;

    @Override
    public boolean exists(int pid) {
        return helper.exists(pid);
    }

    @Override
    public String getUserName(int pid) {
        return helper.getUserName(pid);
    }

    @Override
    public int getUid(int pid) {
        return helper.getUid(pid);
    }

    @Override
    public Map<String, String> getEnvironment(int pid) {
        return helper.getEnvironment(pid);
    }

    @Override
    public PortableProcessStat getProcessStat(int pid) {
        final long[] info = helper.getProcessCPUInfo(pid);
        final long utime = info[1];
        final long stime = info[2];
        return new PortableProcessStat(pid, utime, stime);
    }

    @Override
    public PortableVmIoStat getVmIoStat(Clock clock, int pid) {
        return new MacOSVmIoStat(clock, pid);
    }

    @Override
    public boolean terminateProcess(int pid) {
        return helper.terminateProcess(pid);
    }

    @Override
    public boolean terminateProcess(int pid, boolean wait) {
        return helper.terminateProcess(pid, wait);
    }

    @Override
    public boolean terminateProcess(int pid, int exitcode, int waitMillis) {
        return helper.terminateProcess(pid, exitcode, waitMillis);
    }
}
