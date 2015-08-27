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

package com.redhat.thermostat.common;

import java.util.Objects;

import com.redhat.thermostat.common.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;

/**
 * Represents a size (of a file, memory, or disk) with a number and a unit.
 * <p>
 * Once created, an instance of this class is immutable. All operations that
 * modify this will return new objects.
 */
public class Size {

    /* This is the Quantity pattern. */

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String DOUBLE_FORMAT_STRING = "%.1f";

    private static final long BYTES_IN_KiB = 1024;
    private static final long BYTES_IN_MiB = 1024 * BYTES_IN_KiB;
    private static final long BYTES_IN_GiB = 1024 * BYTES_IN_MiB;
    private static final long BYTES_IN_TiB = 1024 * BYTES_IN_GiB;

    public enum Unit {
        B(1),
        KiB(BYTES_IN_KiB),
        MiB(BYTES_IN_MiB),
        GiB(BYTES_IN_GiB),
        TiB(BYTES_IN_TiB);

        private long numBytes;

        private Unit(long numBytes) {
            this.numBytes = numBytes;
        }

        private long getNumBytes() {
            return numBytes;
        }

        public static Unit getBestUnit(long bytes) {
            if (bytes < BYTES_IN_KiB) {
                return Unit.B;
            } else if (bytes < BYTES_IN_MiB) {
                return Unit.KiB;
            } else if (bytes < BYTES_IN_GiB) {
                return Unit.MiB;
            } else if (bytes < BYTES_IN_TiB) {
                return Unit.GiB;
            } else {
                return Unit.TiB;
            }
        }
    }

    private final double amount;
    private final Unit unit;

    public Size(double amount, Unit unit) {
        this.amount = amount;
        this.unit = Objects.requireNonNull(unit);
    }

    public double getValue() {
        return amount;
    }

    public Unit getUnit() {
        return unit;
    }

    public Size convertTo(Unit target) {
        return new Size(1.0 * amount * unit.getNumBytes() / target.getNumBytes(), target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, unit);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Size)) {
            return false;
        }
        Size other = (Size) obj;
        return this.getValue() == other.convertTo(this.getUnit()).getValue();
    }

    // TODO consider implementing these operations:
    //
    // public int compareTo(Object other)
    //
    // public boolean greaterThan(Size other) { }
    //
    // public boolean lessThan(Size other) { }
    //
    // public Size add(Size toAdd) { }
    //
    // public Size subtract(Size toSubtract) { }
    //
    // public Size multiply(double by) { }
    //
    // public Size divide(double by) { }

    /**
     * Returns a simplified and human-readable version of this Size
     */
    @Override
    public String toString() {
        String[] parts = toStringParts(DOUBLE_FORMAT_STRING);
        return translator.localize(LocaleResources.VALUE_AND_UNIT, parts[0], parts[1]).getContents();
    }

    /**
     * Returns a simplified and human-readable version of this Size
     *
     * @param valueFormatString indicates how to format the value (such as {@code "%.2f"}).
     */
    public String toString(String valueFormatString) {
        String[] parts = toStringParts(valueFormatString);
        return translator.localize(LocaleResources.VALUE_AND_UNIT, parts[0], parts[1]).getContents();
    }

    /**
     * Returns a human-readable version of this Size, appropriate for localization.
     *
     * @return a two-element string array. The first element is the value. The second element is the unit.
     */
    public String[] toStringParts() {
        return toStringParts(DOUBLE_FORMAT_STRING);
    }

    /**
     * Returns a human-readable version of this Size, appropriate for localization.
     *
     * @param valueFormatString indicates how to format the value (such as {@code "%.2f"}).
     *
     * @return a two-element string array. The first element is the value. The second element is the unit.
     */
    public String[] toStringParts(String valueFormatString) {
        long amountInBytes = (long) (amount * unit.getNumBytes());
        if (amountInBytes < BYTES_IN_KiB) {
            // No decimal units in plain bytes
            return new String[] { String.valueOf(amountInBytes), Unit.B.name() };
        } else if (amountInBytes < BYTES_IN_MiB) {
            return new String[] { String.format(valueFormatString, (double) amountInBytes / BYTES_IN_KiB), Unit.KiB.name() };
        } else if (amountInBytes < BYTES_IN_GiB) {
            return new String[] { String.format(valueFormatString, (double) amountInBytes / BYTES_IN_MiB), Unit.MiB.name() };
        } else if (amountInBytes < BYTES_IN_TiB) {
            return new String[] { String.format(valueFormatString, (double) amountInBytes / BYTES_IN_GiB), Unit.GiB.name() };
        } else {
            return new String[] { String.format(valueFormatString, (double) amountInBytes / BYTES_IN_TiB), Unit.TiB.name() };
        }
    }

    public static Size bytes(long bytes) {
        return new Size(bytes, Unit.B);
    }

    /**
     * Parses a string (such as "1.0 KiB") into a size.
     */
    public static Size parse(String toParse) {
        String[] parts = toParse.split(" +");
        String value = toParse;
        String units = null;
        if (parts.length > 1) {
            value = parts[0];
            units = parts[1];
        }

        double result = Double.NaN;
        try {
            result = Double.parseDouble(value);
            if (units != null) {
                Unit parsedUnit = Unit.valueOf(units.trim());
                return new Size(result, parsedUnit);
            }
            return new Size(result, Unit.B);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("unable to parse '" + toParse + "'", nfe);
        }
    }

}

