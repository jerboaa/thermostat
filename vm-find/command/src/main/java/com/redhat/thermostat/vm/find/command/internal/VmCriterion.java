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

package com.redhat.thermostat.vm.find.command.internal;

import com.redhat.thermostat.storage.model.VmInfo;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

enum VmCriterion implements CriterionMatcher {
    JAVA_VERSION("javaversion", new JavaVersionMatcher()),
    MAINCLASS("mainclass", new MainclassMatcher()),
    VM_STATUS("vmstatus", new VmStatusMatcher()),
    VM_NAME("vmname", new VmNameMatcher()),
    VM_ARGS("vmargs", new VmArgsMatcher()),
    VM_VERSION("vmversion", new VmVersionMatcher()),
    USERNAME("username", new UsernameMatcher()),
    JAVA_HOME("javahome", new JavaHomeMatcher()),
    ;

    private final String cliSwitch;
    private final CriterionMatcher criterionMatcher;

    VmCriterion(String cliSwitch, CriterionMatcher criterionMatcher) {
        this.cliSwitch = cliSwitch;
        this.criterionMatcher = criterionMatcher;
    }

    @Override
    public boolean match(MatchContext matchContext, String value) throws UnrecognizedArgumentException {
        return this.criterionMatcher.match(matchContext, value);
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

    static class JavaVersionMatcher implements CriterionMatcher {
        @Override
        public boolean match(MatchContext matchContext, String s) {
            return matchContext.getVmInfo().getJavaVersion().equals(s);
        }
    }

    static class MainclassMatcher implements CriterionMatcher {
        @Override
        public boolean match(MatchContext matchContext, String s) {
            try {
                String mainClass = matchContext.getVmInfo().getMainClass();
                return mainClass.equals(s) || mainClass.contains(s) || mainClass.matches(s);
            } catch (PatternSyntaxException e) {
                return false;
            }
        }
    }

    static class VmStatusMatcher implements CriterionMatcher {
        @Override
        public boolean match(MatchContext matchContext, String string) throws UnrecognizedArgumentException {
            if (!isRecognizedValue(string)) {
                throw new UnrecognizedArgumentException(LocaleResources.createTranslator(),
                        FindVmCommand.REGISTER_NAME, "--" + VM_STATUS.getCliSwitch(), string, getAcceptedValues());
            }

            VmInfo.AliveStatus status = matchContext.getVmInfo().isAlive(matchContext.getAgentInfo());
            return status.toString().equalsIgnoreCase(string);
        }

        private static boolean isRecognizedValue(String s) {
            boolean matched = false;
            for (VmInfo.AliveStatus status : VmInfo.AliveStatus.values()) {
                matched = status.toString().equalsIgnoreCase(s);
                if (matched) {
                    break;
                }
            }
            return matched;
        }

        private static List<String> getAcceptedValues() {
            List<String> list = new ArrayList<>();
            for (VmInfo.AliveStatus status : VmInfo.AliveStatus.values()) {
                list.add(status.toString().toUpperCase());
            }
            return list;
        }
    }

    static class VmNameMatcher implements CriterionMatcher {
        @Override
        public boolean match(MatchContext matchContext, String s) {
            return matchContext.getVmInfo().getVmName().equals(s);
        }
    }

    static class VmArgsMatcher implements CriterionMatcher {
        @Override
        public boolean match(MatchContext matchContext, String s) {
            if (!s.contains(",")) {
                return matchContext.getVmInfo().getVmArguments().contains(s);
            }
            boolean allPresent = true;
            String[] args = s.split(",");
            for (String arg : args) {
                allPresent = allPresent && matchContext.getVmInfo().getVmArguments().contains(arg);
            }
            return allPresent;
        }
    }

    static class VmVersionMatcher implements CriterionMatcher {
        @Override
        public boolean match(MatchContext matchContext, String s) {
            return matchContext.getVmInfo().getVmVersion().equals(s);
        }
    }

    static class UsernameMatcher implements CriterionMatcher {
        @Override
        public boolean match(MatchContext matchContext, String s) {
            return matchContext.getVmInfo().getUsername().equals(s);
        }
    }

    static class JavaHomeMatcher implements CriterionMatcher {
        @Override
        public boolean match(MatchContext matchContext, String s) {
            if (matchContext.getVmInfo().getJavaHome().equals(s)) {
                return true;
            }
            try {
                Path vmPath = Paths.get(matchContext.getVmInfo().getJavaHome());
                Path givenPath = Paths.get(s);
                return vmPath.equals(givenPath);
            } catch (InvalidPathException e) {
                return false;
            }
        }
    }


}
