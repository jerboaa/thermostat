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

package com.redhat.thermostat.common.portability.internal.linux;

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.portability.PortableProcess;
import com.redhat.thermostat.common.portability.PortableProcessStat;
import com.redhat.thermostat.common.portability.PortableVmIoStat;
import com.redhat.thermostat.common.portability.ProcessChecker;
import com.redhat.thermostat.common.portability.internal.UnimplementedError;
import com.redhat.thermostat.common.portability.internal.linux.vmio.LinuxVmIoStatBuilderImpl;
import com.redhat.thermostat.common.portability.internal.linux.vmio.ProcIoDataReader;
import com.redhat.thermostat.common.portability.linux.ProcDataSource;

import java.util.Map;

public class LinuxPortableProcessImpl implements PortableProcess {

    private LinuxPortableProcessStatBuilderImpl procStatHelper;
    private LinuxProcessEnvironmentBuilderImpl procEnvHelper;
    private LinuxVmIoStatBuilderImpl vmioHelper;

    public static LinuxPortableProcessImpl INSTANCE = new LinuxPortableProcessImpl(new SystemClock(), new ProcDataSource());

    public static PortableProcess createInstance() {
        return new LinuxPortableProcessImpl(new SystemClock(), new ProcDataSource());
    }

    private LinuxPortableProcessImpl(Clock clock, ProcDataSource dataSource) {
        procStatHelper = new LinuxPortableProcessStatBuilderImpl(dataSource);
        procEnvHelper = new LinuxProcessEnvironmentBuilderImpl(dataSource);
        vmioHelper = new LinuxVmIoStatBuilderImpl(clock, new ProcIoDataReader(dataSource));
    }

    @Override
    public boolean exists(int pid) {
        return new ProcessChecker().exists(pid);
    }

    @Override
    public String getUserName(int pid) {
        throw new UnimplementedError("getUserName()");
    }

    @Override
    public int getUid(int pid) {
        throw new UnimplementedError("getUid()");
    }

    @Override
    public Map<String, String> getEnvironment(int pid) {
        return procEnvHelper.build(pid);
    }

    @Override
    public PortableProcessStat getProcessStat(int pid) {
        return procStatHelper.build(pid);
    }

    @Override
    public PortableVmIoStat getVmIoStat(Clock clock, int pid) {
        return vmioHelper.build(pid);
    }

    @Override
    public boolean terminateProcess(int pid) {
        throw new UnimplementedError("terminateProcess()");
    }

    @Override
    public boolean terminateProcess(int pid, boolean wait) {
        throw new UnimplementedError("terminateProcess()");
    }

    @Override
    public boolean terminateProcess(int pid, int exitcode, int waitMillis) {
        throw new UnimplementedError("terminateProcess()");
    }
}
