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

package com.redhat.thermostat.vm.find.command.internal;

import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

class ResultsRenderer {

    enum Field {
        VM_ID("show-vm-ids", new VmIdFieldAdapter()),
        MAINCLASS("show-mainclasses", new MainClassFieldAdapter()),
        VMNAME("show-vmnames", new VmNameFieldAdapter()),
        JAVAVERSION("show-javaversions", new JavaVersionFieldAdapter()),
        VMVERSION("show-vmversions", new VmVersionFieldAdapter()),
        PID("show-pids", new PidFieldAdapter()),
        USERNAME("show-usernames", new UsernameFieldAdapter()),
        HOSTNAME("show-hostnames", new HostnameFieldAdapter()),
        OSNAME("show-osnames", new OsNameFieldAdapter()),
        OSKERNEL("show-oskernels", new OsKernelFieldAdapter()),
        ;

        private final String cliSwitch;
        private final FieldAdapter fieldAdapter;

        Field(String cliSwitch, FieldAdapter fieldAdapter) {
            this.cliSwitch = cliSwitch;
            this.fieldAdapter = fieldAdapter;
        }

        String getCliSwitch() {
            return cliSwitch;
        }

        String getAdaptedField(Pair<HostInfo, VmInfo> pair) {
            return fieldAdapter.map(pair);
        }
    }

    static final String ENABLE_ALL_FIELDS_FLAG = "show-all";
    private static final Set<Field> DEFAULT_ENABLED_FIELDS = Collections.unmodifiableSet(EnumSet.of(Field.VM_ID));

    private final Set<Field> enabledFields = EnumSet.noneOf(Field.class);

    ResultsRenderer(Arguments arguments) {
        if (arguments.hasArgument(ENABLE_ALL_FIELDS_FLAG)) {
            enabledFields.addAll(Arrays.asList(Field.values()));
        }
        for (Field field : Field.values()) {
            if (arguments.hasArgument(field.getCliSwitch())) {
                enabledFields.add(field);
            }
        }
        if (enabledFields.isEmpty()) {
            enabledFields.addAll(DEFAULT_ENABLED_FIELDS);
        }
    }

    void print(PrintStream printStream, Iterable<Pair<HostInfo, VmInfo>> pairs) {
        TableRenderer renderer = new TableRenderer(enabledFields.size());
        if (!isShortOutput()) {
            renderer.printHeader(getHeaderFields().toArray(new String[enabledFields.size()]));
        }
        for (Pair<HostInfo, VmInfo> pair : pairs) {
            List<String> info = getInfo(pair);
            renderer.printLine(info.toArray(new String[enabledFields.size()]));
        }
        renderer.render(printStream);
    }

    boolean isShortOutput() {
        return enabledFields.equals(DEFAULT_ENABLED_FIELDS) || enabledFields.size() == 1;
    }

    List<String> getHeaderFields() {
        List<String> list = new ArrayList<>();
        for (Field field : enabledFields) {
            list.add(field.toString());
        }
        return list;
    }

    List<String> getInfo(Pair<HostInfo, VmInfo> pair) {
        List<String> list = new ArrayList<>();
        for (Field field : enabledFields) {
            list.add(field.getAdaptedField(pair));
        }
        return list;
    }

    interface FieldAdapter {
        String map(Pair<HostInfo, VmInfo> pair);
    }

    static class VmIdFieldAdapter implements FieldAdapter {
        @Override
        public String map(Pair<HostInfo, VmInfo> pair) {
            VmInfo vmInfo = pair.getSecond();
            return vmInfo.getVmId();
        }
    }

    static class MainClassFieldAdapter implements FieldAdapter {
        @Override
        public String map(Pair<HostInfo, VmInfo> pair) {
            VmInfo vmInfo = pair.getSecond();
            return vmInfo.getMainClass();
        }
    }

    static class VmNameFieldAdapter implements FieldAdapter {
        @Override
        public String map(Pair<HostInfo, VmInfo> pair) {
            VmInfo vmInfo = pair.getSecond();
            return vmInfo.getVmName();
        }
    }

    static class JavaVersionFieldAdapter implements FieldAdapter {
        @Override
        public String map(Pair<HostInfo, VmInfo> pair) {
            VmInfo vmInfo = pair.getSecond();
            return vmInfo.getJavaVersion();
        }
    }

    static class VmVersionFieldAdapter implements FieldAdapter {
        @Override
        public String map(Pair<HostInfo, VmInfo> pair) {
            VmInfo vmInfo = pair.getSecond();
            return vmInfo.getVmVersion();
        }
    }

    static class PidFieldAdapter implements FieldAdapter {
        @Override
        public String map(Pair<HostInfo, VmInfo> pair) {
            VmInfo vmInfo = pair.getSecond();
            return Integer.toString(vmInfo.getVmPid());
        }
    }

    static class UsernameFieldAdapter implements FieldAdapter {
        @Override
        public String map(Pair<HostInfo, VmInfo> pair) {
            VmInfo vmInfo = pair.getSecond();
            return vmInfo.getUsername();
        }
    }

    static class HostnameFieldAdapter implements FieldAdapter {
        @Override
        public String map(Pair<HostInfo, VmInfo> pair) {
            HostInfo hostInfo = pair.getFirst();
            return hostInfo.getHostname();
        }
    }

    static class OsNameFieldAdapter implements FieldAdapter {
        @Override
        public String map(Pair<HostInfo, VmInfo> pair) {
            HostInfo hostInfo = pair.getFirst();
            return hostInfo.getOsName();
        }
    }

    static class OsKernelFieldAdapter implements FieldAdapter {
        @Override
        public String map(Pair<HostInfo, VmInfo> pair) {
            HostInfo hostInfo = pair.getFirst();
            return hostInfo.getOsKernel();
        }
    }

}
