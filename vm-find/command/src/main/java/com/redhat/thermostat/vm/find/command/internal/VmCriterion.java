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

import com.redhat.thermostat.storage.model.VmInfo;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.PatternSyntaxException;

enum VmCriterion implements CriterionMatcher<VmInfo, String> {
    JAVA_VERSION("javaversion", new JavaVersionMatcher()),
    MAINCLASS("mainclass", new MainclassMatcher()),
    VM_NAME("vmname", new VmNameMatcher()),
    VM_ARGS("vmargs", new VmArgsMatcher()),
    VM_VERSION("vmversion", new VmVersionMatcher()),
    USERNAME("username", new UsernameMatcher()),
    JAVA_HOME("javahome", new JavaHomeMatcher()),
    ;

    private final String cliSwitch;
    private final CriterionMatcher<VmInfo, String> criterionMatcher;

    VmCriterion(String cliSwitch, CriterionMatcher<VmInfo, String> criterionMatcher) {
        this.cliSwitch = cliSwitch;
        this.criterionMatcher = criterionMatcher;
    }

    @Override
    public boolean match(VmInfo vmInfo, String value) {
        return this.criterionMatcher.match(vmInfo, value);
    }

    String getCliSwitch() {
        return cliSwitch;
    }

    static VmCriterion fromString(String string) {
        for (VmCriterion criterion : VmCriterion.values()) {
            if (criterion.cliSwitch.equals(string)) {
                return criterion;
            }
        }
        throw new IllegalArgumentException(string + " is not a legal VmCriterion");
    }

    static class JavaVersionMatcher implements CriterionMatcher<VmInfo, String> {
        @Override
        public boolean match(VmInfo vmInfo, String s) {
            return vmInfo.getJavaVersion().equals(s);
        }
    }

    static class MainclassMatcher implements CriterionMatcher<VmInfo, String> {
        @Override
        public boolean match(VmInfo vmInfo, String s) {
            try {
                return vmInfo.getMainClass().equals(s) || vmInfo.getMainClass().contains(s) || vmInfo.getMainClass().matches(s);
            } catch (PatternSyntaxException e) {
                return false;
            }
        }
    }

    static class VmNameMatcher implements CriterionMatcher<VmInfo, String> {
        @Override
        public boolean match(VmInfo vmInfo, String s) {
            return vmInfo.getVmName().equals(s);
        }
    }

    static class VmArgsMatcher implements CriterionMatcher<VmInfo, String> {
        @Override
        public boolean match(VmInfo vmInfo, String s) {
            if (!s.contains(",")) {
                return vmInfo.getVmArguments().contains(s);
            }
            boolean allPresent = true;
            String[] args = s.split(",");
            for (String arg : args) {
                allPresent = allPresent && vmInfo.getVmArguments().contains(arg);
            }
            return allPresent;
        }
    }

    static class VmVersionMatcher implements CriterionMatcher<VmInfo, String> {
        @Override
        public boolean match(VmInfo vmInfo, String s) {
            return vmInfo.getVmVersion().equals(s);
        }
    }

    static class UsernameMatcher implements CriterionMatcher<VmInfo, String> {
        @Override
        public boolean match(VmInfo vmInfo, String s) {
            return vmInfo.getUsername().equals(s);
        }
    }

    static class JavaHomeMatcher implements CriterionMatcher<VmInfo, String> {
        @Override
        public boolean match(VmInfo vmInfo, String s) {
            if (vmInfo.getJavaHome().equals(s)) {
                return true;
            }
            try {
                Path vmPath = Paths.get(vmInfo.getJavaHome());
                Path givenPath = Paths.get(s);
                return vmPath.equals(givenPath);
            } catch (InvalidPathException e) {
                return false;
            }
        }
    }


}
