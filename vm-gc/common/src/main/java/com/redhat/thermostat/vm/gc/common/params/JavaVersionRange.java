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

/**
 * Models a range of Java versions, using maven version format:
 * https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html
 *
 * A single, exact version number of "w.x.y.z" is considered as a short form for the range "[w.x.y.z,w.x.y.z]"
 */
public class JavaVersionRange implements Comparable<JavaVersionRange> {

    private static final String LBRACK = "([\\[\\(])";
    private static final String RBRACK = "([\\]\\)])";
    private static final String NUM = "([0-9]+)";
    private static final String DOT = "\\.";
    private static final String SCORE = "[\\._]";
    private static final String COMMA = ",";
    private static final String SINGLE_VERSION_PATTERN_STRING = NUM + DOT + NUM + DOT + NUM + SCORE + NUM;
    private static final String VERSION_PATTERN_STRING = LBRACK + "?" + "(" + SINGLE_VERSION_PATTERN_STRING + ")" + RBRACK + "?";
    private static final String RANGE_PATTERN_STRING = LBRACK + "(" + SINGLE_VERSION_PATTERN_STRING + ")?" + COMMA
            + "(" + SINGLE_VERSION_PATTERN_STRING + ")?" + RBRACK;
    private static final Pattern VERSION_PATTERN = Pattern.compile(VERSION_PATTERN_STRING);
    private static final Pattern RANGE_PATTERN = Pattern.compile(RANGE_PATTERN_STRING);

    private final boolean lowerInclusive, upperInclusive;
    private final VersionPoints lowerBound, upperBound;

    public JavaVersionRange(int major, int minor, int micro, int update) {
        this(new VersionPoints(major, minor, micro, update));
    }

    public JavaVersionRange(VersionPoints versionPoints) {
        this(versionPoints, true, versionPoints, true);
    }

    public JavaVersionRange(VersionPoints lowerBound, boolean lowerInclusive, VersionPoints upperBound, boolean upperInclusive) {
        this.lowerBound = requireNonNull(lowerBound);
        this.upperBound = requireNonNull(upperBound);
        this.lowerInclusive = lowerInclusive;
        this.upperInclusive = upperInclusive;
        if (lowerBound.compareTo(upperBound) > 0) {
            throw new InvalidJavaVersionFormatException("Range must ascend");
        }
        if (lowerBound.equals(upperBound) && !isLowerBoundInclusive() && !isUpperBoundInclusive()) {
            throw new InvalidJavaVersionFormatException("Range is empty: " + this);
        }
    }

    public static JavaVersionRange fromString(String javaVersionString) {
        validateFormat(javaVersionString);
        Matcher singleVersionMatcher = VERSION_PATTERN.matcher(javaVersionString);
        if (singleVersionMatcher.matches()) {
            VersionPoints points = VersionPoints.fromString(singleVersionMatcher.group(2));
            String leftBracket = singleVersionMatcher.group(1);
            String rightBracket = singleVersionMatcher.group(7);
            if (leftBracket == null && rightBracket == null) {
                return new JavaVersionRange(points);
            } else if (leftBracket != null && rightBracket != null) {
                return new JavaVersionRange(points, isInclusive(leftBracket.charAt(0)), points, isInclusive(rightBracket.charAt(0)));
            } else {
                throw new InvalidJavaVersionFormatException("Unmatched bracket in version string: " + javaVersionString);
            }
        } else {
            Matcher rangeVersionMatcher = RANGE_PATTERN.matcher(javaVersionString);
            if (rangeVersionMatcher.matches()) {
                String lower = rangeVersionMatcher.group(2);
                String upper = rangeVersionMatcher.group(7);
                VersionPoints lowerBound, upperBound;
                if (lower == null && upper == null) {
                    throw new InvalidJavaVersionFormatException("Cannot specify a range without any bounds");
                } else if (lower == null) {
                    lowerBound = VersionPoints.MINIMUM_VERSION;
                    upperBound = VersionPoints.fromString(upper);
                } else if (upper == null) {
                    lowerBound = VersionPoints.fromString(lower);
                    upperBound = VersionPoints.MAXIMUM_VERSION;
                } else {
                    lowerBound = VersionPoints.fromString(lower);
                    upperBound = VersionPoints.fromString(upper);
                }
                String leftBracket = rangeVersionMatcher.group(1);
                String rightBracket = rangeVersionMatcher.group(12);
                return new JavaVersionRange(lowerBound, isInclusive(leftBracket.charAt(0)), upperBound, isInclusive(rightBracket.charAt(0)));
            } else {
                throw new InvalidJavaVersionFormatException(javaVersionString);
                // shouldn't reach here - if it doesn't match either pattern then validateFormat should have caught it already
            }
        }
    }

    static boolean isInclusive(char brack) {
        if (brack == '[' || brack == ']') {
            return true;
        } else if (brack == '(' || brack == ')') {
            return false;
        } else {
            throw new IllegalArgumentException(Character.toString(brack));
        }
    }

    public boolean isRange() {
        return !lowerBound.equals(upperBound);
    }

    public JavaVersionRange getLowerBound() {
        return new JavaVersionRange(lowerBound);
    }

    public JavaVersionRange getUpperBound() {
        return new JavaVersionRange(upperBound);
    }

    public boolean isLowerBoundInclusive() {
        return lowerInclusive;
    }

    public boolean isUpperBoundInclusive() {
        return upperInclusive;
    }

    public boolean contains(JavaVersionRange other) {
        if (!isRange()) {
            return this.equals(other);
        }

        final int lower = lowerBound.compareTo(other.lowerBound);
        final int upper = upperBound.compareTo(other.upperBound);

        if (lower > 0 || upper < 0) {
            return false;
        }

        if (lower == 0) {
            return isLowerBoundInclusive();
        }

        if (upper == 0) {
            return isUpperBoundInclusive();
        }

        return true;
    }

    @Override
    public int compareTo(JavaVersionRange javaVersionRange) {
        if (this.equals(javaVersionRange)) {
            return 0;
        }
        if (javaVersionRange == null) {
            return 1;
        }
        if (isRange() && javaVersionRange.isRange()) {
            return -1;
        }

        return lowerBound.compareTo(javaVersionRange.lowerBound);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JavaVersionRange javaVersionRange = (JavaVersionRange) o;

        return lowerBound.equals(javaVersionRange.lowerBound) && upperBound.equals(javaVersionRange.upperBound)
                && isLowerBoundInclusive() == javaVersionRange.isLowerBoundInclusive()
                && isUpperBoundInclusive() == javaVersionRange.isUpperBoundInclusive();
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowerBound, upperBound, lowerInclusive, upperInclusive);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (isLowerBoundInclusive()) {
            sb.append('[');
        } else {
            sb.append('(');
        }

        sb.append(lowerBound.toString());
        if (isRange()) {
            sb.append(',');
            sb.append(upperBound.toString());
        }

        if (isUpperBoundInclusive()) {
            sb.append(']');
        } else {
            sb.append(')');
        }

        return sb.toString();
    }

    static void validateFormat(String rawVersion) {
        if (rawVersion == null) {
            throw new InvalidJavaVersionFormatException(null);
        }
        Matcher singleVersionMatcher = VERSION_PATTERN.matcher(rawVersion);
        Matcher rangeMatcher = RANGE_PATTERN.matcher(rawVersion);
        if (!(singleVersionMatcher.matches() || rangeMatcher.matches())) {
            throw new InvalidJavaVersionFormatException(rawVersion);
        }
    }

    public static class VersionPoints implements Comparable<VersionPoints> {

        static final VersionPoints MINIMUM_VERSION = new VersionPoints(0, 0, 0, 0);
        static final VersionPoints MAXIMUM_VERSION = new VersionPoints(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

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
            Matcher matcher = Pattern.compile(SINGLE_VERSION_PATTERN_STRING).matcher(string);
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
            return getMajor() + "."
                    + getMinor() + "."
                    + getMicro() + "."
                    + getUpdate();
        }
    }

    public static class InvalidJavaVersionFormatException extends RuntimeException {
        public InvalidJavaVersionFormatException(String message) {
            super("Invalid version string: " + message);
        }
    }
}
