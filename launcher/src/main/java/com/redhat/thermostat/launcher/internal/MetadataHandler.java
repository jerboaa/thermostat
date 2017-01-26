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

package com.redhat.thermostat.launcher.internal;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.launcher.BundleInformation;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

public class MetadataHandler {

    private static final String LBRACK = "([\\(|\\[])";
    private static final String VERSION = "(\\d+(\\.\\d+){0,2})";
    private static final String RBRACK = "([\\)|\\]])";
    private static final String COMMA = ",";
    private static final String INCLUSIVE_LOWER = "([\\[])";
    private static final String INCLUSIVE_UPPER = "([\\]])";
    private static final String VERSION_RANGE = LBRACK + VERSION + COMMA + VERSION + RBRACK;
    private static final String INCLUSIVE_UPPER_RANGE = LBRACK + VERSION + COMMA + VERSION + INCLUSIVE_UPPER;
    private static final String INCLUSIVE_LOWER_RANGE = INCLUSIVE_LOWER + VERSION + COMMA + VERSION + RBRACK;
    private static final String NO_VERSION = "0";

    private static final Pattern VERSION_RANGE_PATTERN = Pattern.compile(VERSION_RANGE);
    private static final Pattern INCLUSIVE_UPPER_PATTERN = Pattern.compile(INCLUSIVE_UPPER_RANGE);
    private static final Pattern INCLUSIVE_LOWER_PATTERN = Pattern.compile(INCLUSIVE_LOWER_RANGE);
    private static final Logger logger = LoggingUtils.getLogger(MetadataHandler.class);

    public List<BundleInformation> parseHeader(String header) {
        header = header.concat("\0");
        List<BundleInformation> packages = new ArrayList<>();
        int index = 0;
        int start = 0;
        boolean invalid = false;
        boolean newSubstring = true;
        boolean parsingDirective = false;
        boolean parsingVersion = false;
        boolean isVersionRange = false;
        boolean inQuotes = false;
        String version;
        BundleInformation lastPackage = new BundleInformation("","");
        String directive;
        while (index < header.length()) {
            char charAtIndex = header.charAt(index);
            if (parsingVersion) {
                if (charAtIndex == '[' || charAtIndex == '(') {
                    isVersionRange = true;
                } else if (charAtIndex == ']' || charAtIndex == ')') {
                    isVersionRange = false;
                }
            }
            if (charAtIndex == '\"') {
                inQuotes = !inQuotes;
            }
            if (charAtIndex == '=') {
                if (parsingDirective) {
                    directive = header.substring(start, index);
                    if (directive.equals("version")) {
                        parsingVersion = true;
                    }
                    parsingDirective = false;
                }
                invalid = true;
                newSubstring = false;
                start = index + 1;
            }
            if (charAtIndex == ';' || charAtIndex == ',' || charAtIndex == '\0') {
                if (parsingVersion) {
                    if (!isVersionRange) {
                        version = header.substring(start, index);
                        packages.remove(lastPackage);
                        packages.add(new BundleInformation(
                                lastPackage.getName(), version.replace("\"", "")));
                        parsingVersion = false;
                    } else {
                        index++;
                        continue;
                    }
                } else {
                    if (!inQuotes) {
                        if (!invalid && !newSubstring) {
                            // Packages are given a default version of 0. This is changed later if a version directive
                            // is specified in the manifest.
                            lastPackage = new BundleInformation(header.substring(start, index),
                                    MetadataHandler.NO_VERSION);
                            packages.add(lastPackage);
                        }
                        if (charAtIndex == ';') {
                            parsingDirective = true;
                        }
                    }
                }
                start = index + 1;
                invalid = false;
                newSubstring = true;
            } else if (newSubstring) {
                if (!Character.isJavaIdentifierStart(charAtIndex) && !Character.isWhitespace(charAtIndex)) {
                    invalid = true;
                }
                newSubstring = false;
            } else if ((!Character.isJavaIdentifierPart(charAtIndex)) && charAtIndex != '.'
                    && !Character.isWhitespace(charAtIndex)) {
                invalid = true;
            }
            index++;
        }
        return packages;
    }

    public BundleInformation parseAndCheckBounds(String versionString, String TargetVersion, BundleInformation target) {
        boolean strictUpper = !INCLUSIVE_UPPER_PATTERN.matcher(versionString).matches();
        boolean strictLower = !INCLUSIVE_LOWER_PATTERN.matcher(versionString).matches();
        String [] bounds = parseVersionRange(versionString);
        int[] parsedLowerBound = extractVersions(bounds[0]);
        int[] parsedUpperBound = extractVersions(bounds[1]);
        int[] parsedTarget = extractVersions(TargetVersion);
        // Need to satisfy lowerBound <= versionString <= upperBound
        if ((satisfiesBound(parsedTarget, parsedLowerBound, strictLower))
                && (satisfiesBound(parsedUpperBound, parsedTarget, strictUpper))) {
            return target;
        }
        return null;
    }

    public boolean isVersionRange(String versionString) {
        return VERSION_RANGE_PATTERN.matcher(versionString).matches();
    }


    // Package Private for testing
    boolean satisfiesBound(int[] target, int[] bound, boolean exclusive) {
        int major = Integer.compare(target[0], bound[0]);
        int minor = Integer.compare(target[1], bound[1]);
        int micro = Integer.compare(target[2], bound[2]);
        if (major > 0) {
            return true;
        } else if (major == 0) {
            if (target[1] != -1 && bound[1] != -1) {
                if (minor > 0) {
                    return true;
                } else if (minor == 0) {
                    if (target[2] != -1 && bound[2] != -1) {
                        if (micro > 0) {
                            return true;
                        } else if (micro == 0 && !exclusive) {
                            return true;
                        }
                    } else if (!exclusive && bound[2] == -1) {
                        return true;
                    }
                }
            } else if (!exclusive && bound[1] == -1) {
                return true;
            }
        }
        return false;
    }

    public String[] parseVersionRange(String versionString) {
        Matcher m = VERSION_RANGE_PATTERN.matcher(versionString);
        if (m.matches()) {
            return new String[]{m.group(2), m.group(4)};
        }
        return null;
    }

    public int[] extractVersions(String versionString) {
        String[] split = versionString.split(Pattern.quote("."));
        int[] versions = {-1, -1, -1};
        try {
            for (int i = 0; i < Math.min(split.length, versions.length); i++) {
                versions[i] = Integer.parseInt(split[i]);
            }
        } catch (NumberFormatException nfe) {
            // Should we get a malformed version string, make sure we log it
            logger.log(Level.WARNING, "Malformed version string: " + versionString);
        }
        return versions;
    }

}
