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

package com.redhat.thermostat.vm.gc.common.params;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class JavaVersion implements Comparable<JavaVersion> {

    public static final String VERSION_POINT_PATTERN = "(\\d+)";
    public static final String RANGE_DELIMITER = ":";
    public static final String POINT_RELEASE_DELIMITER = "\\.";
    public static final String UPDATE_NUMBER_DELIMITER = "_";
    public static final String VERSION_FORMAT_REGEX = VERSION_POINT_PATTERN + POINT_RELEASE_DELIMITER
            + VERSION_POINT_PATTERN + POINT_RELEASE_DELIMITER + VERSION_POINT_PATTERN + UPDATE_NUMBER_DELIMITER + VERSION_POINT_PATTERN;
    public static final Pattern SINGLE_VERSION_PATTERN = Pattern.compile(VERSION_FORMAT_REGEX);
    public static final Pattern VERSION_RANGE_PATTERN = Pattern.compile(VERSION_FORMAT_REGEX + RANGE_DELIMITER + VERSION_FORMAT_REGEX);

    private final VersionPoints lowerBound, upperBound;

    public JavaVersion(int major, int minor, int micro, int update) {
        this(new VersionPoints(major, minor, micro, update));
    }

    public JavaVersion(VersionPoints versionPoints) {
        this(versionPoints, versionPoints);
    }

    public JavaVersion(VersionPoints lowerBound, VersionPoints upperBound) {
        this.lowerBound = requireNonNull(lowerBound);
        this.upperBound = requireNonNull(upperBound);
        if (lowerBound.compareTo(upperBound) > 0) {
            throw new InvalidJavaVersionFormatException("Range must ascend");
        }
    }

    public static JavaVersion fromString(String javaVersionString) {
        validateFormat(javaVersionString);
        Matcher singleVersionMatcher = SINGLE_VERSION_PATTERN.matcher(javaVersionString);
        if (singleVersionMatcher.matches()) {
            return new JavaVersion(VersionPoints.fromString(javaVersionString));
        } else {
            Matcher rangeVersionMatcher = VERSION_RANGE_PATTERN.matcher(javaVersionString);
            if (rangeVersionMatcher.matches()) {
                String[] parts = javaVersionString.split(RANGE_DELIMITER);
                VersionPoints lowerBound = VersionPoints.fromString(parts[0]);
                VersionPoints upperBound = VersionPoints.fromString(parts[1]);
                return new JavaVersion(lowerBound, upperBound);
            } else {
                throw new InvalidJavaVersionFormatException(javaVersionString);
                // shouldn't reach here - if it doesn't match either pattern then validateFormat should have caught it already
            }
        }
    }

    public boolean isRange() {
        return !lowerBound.equals(upperBound);
    }

    public JavaVersion getLowerBound() {
        return new JavaVersion(lowerBound);
    }

    public JavaVersion getUpperBound() {
        return new JavaVersion(upperBound);
    }

    public boolean contains(JavaVersion other) {
        if (!isRange()) {
            return this.equals(other);
        }
        return (lowerBound.compareTo(other.lowerBound) <= 0
                && upperBound.compareTo(other.upperBound) >= 0);
    }

    @Override
    public int compareTo(JavaVersion javaVersion) {
        if (this.equals(javaVersion)) {
            return 0;
        }
        if (javaVersion == null) {
            return 1;
        }
        if (isRange() && javaVersion.isRange()) {
            return -1;
        }

        return lowerBound.compareTo(javaVersion.lowerBound);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JavaVersion javaVersion = (JavaVersion) o;

        return lowerBound.equals(javaVersion.lowerBound) && upperBound.equals(javaVersion.upperBound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowerBound, upperBound);
    }

    @Override
    public String toString() {
        return lowerBound.toString() + RANGE_DELIMITER + lowerBound.toString();
    }

    static void validateFormat(String rawVersion) {
        if (rawVersion == null) {
            throw new InvalidJavaVersionFormatException(null);
        }
        Matcher singleVersionMatcher = SINGLE_VERSION_PATTERN.matcher(rawVersion);
        Matcher rangeMatcher = VERSION_RANGE_PATTERN.matcher(rawVersion);
        if (!(singleVersionMatcher.matches() || rangeMatcher.matches())) {
            throw new InvalidJavaVersionFormatException(rawVersion);
        }
    }

    public static class VersionPoints implements Comparable<VersionPoints> {

        private final int major;
        private final int minor;
        private final int micro;
        private final int update;

        public VersionPoints(int major, int minor, int micro, int update) {
            this.major = requirePositive(major);
            this.minor = requirePositive(minor);
            this.micro = requirePositive(micro);
            this.update = requirePositive(update);
        }

        static int requirePositive(int i) {
            if (i < 0) {
                throw new IllegalArgumentException("Negative arguments are not permitted");
            }
            return i;
        }

        static VersionPoints fromString(String string) {
            Matcher matcher = SINGLE_VERSION_PATTERN.matcher(string);
            if (!matcher.matches()) {
                throw new InvalidJavaVersionFormatException(string);
            }
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int micro = Integer.parseInt(matcher.group(3));
            int update = Integer.parseInt(matcher.group(4));
            return new VersionPoints(major, minor, micro, update);
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        public int getMicro() {
            return micro;
        }

        public int getUpdate() {
            return update;
        }

        @Override
        public int compareTo(VersionPoints versionPoints) {
            int majorComparison = Integer.compare(getMajor(), versionPoints.getMajor());
            if (majorComparison != 0) {
                return majorComparison;
            }
            int minorComparison = Integer.compare(getMinor(), versionPoints.getMinor());
            if (minorComparison != 0) {
                return minorComparison;
            }
            int microComparison = Integer.compare(getMicro(), versionPoints.getMicro());
            if (microComparison != 0) {
                return microComparison;
            }
            return Integer.compare(getUpdate(), versionPoints.getUpdate());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            VersionPoints that = (VersionPoints) o;

            return Objects.equals(major, that.major)
                    && Objects.equals(minor, that.minor)
                    && Objects.equals(micro, that.micro)
                    && Objects.equals(update, that.update);
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, minor, micro, update);
        }

        @Override
        public String toString() {
            return getMajor() + POINT_RELEASE_DELIMITER
                    + getMinor() + POINT_RELEASE_DELIMITER
                    + getMicro() + UPDATE_NUMBER_DELIMITER
                    + getUpdate();
        }
    }

    public static class InvalidJavaVersionFormatException extends RuntimeException {
        public InvalidJavaVersionFormatException(String message) {
            super("Invalid version string: " + message);
        }
    }
}
