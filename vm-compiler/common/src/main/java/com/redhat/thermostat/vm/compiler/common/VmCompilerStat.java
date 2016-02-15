/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.vm.compiler.common;

import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.storage.model.TimeStampedPojo;

@Entity
public class VmCompilerStat extends BasePojo implements TimeStampedPojo {

    // See the jdk sources for more information:
    // - jdk/src/share/classes/sun/tools/jstat/resources/jstat_options
    // - jdk/src/share/classes/sun/tools/jstat/resources/jstat_unsupported_options

    public static final int UNKNOWN = -1;

    private String vmId;
    private long timestamp;

    private long totalCompiles;
    private long totalBailouts;
    private long totalInvalidates;

    /**
     * There's unit no explicitly mentioned for this jstat value anywhere
     * that I could find. The closest thing I got was the hotspot
     * implementation referring to ComplilationMXBean which mentions the
     * return value is in milliseconds.
     */
    private long compilationTime;

    private long lastSize;

    /**
     * From hotspot code, this is an enum with the values:
     * { no_compile, normal_compile, osr_compile, native_compile }
     */
    private long lastType;

    /**
     *  is of the form "name/of/package/Class$InnerClass methodName"
     */
    private String lastMethod;

    /**
     * From hotspot code, this is an enum with the values:
     * { no_compile, normal_compile, osr_compile, native_compile }
     */
    private long lastFailedType;

    /**
     *  is of the form "name/of/package/Class$InnerClass methodName"
     */
    private String lastFailedMethod;

    public VmCompilerStat() {
        this(null, null, UNKNOWN,
                UNKNOWN, UNKNOWN, UNKNOWN,
                UNKNOWN,
                UNKNOWN, UNKNOWN, null,
                UNKNOWN, null);
    }

    public VmCompilerStat(String writerId, String vmId, long timestamp,
            long totalCompiles, long totalBailouts, long totalInvalidates,
            long compilationTime,
            long lastSize, long lastType, String lastMethod,
            long lastFailedType, String lastFailedMethod) {
        super(writerId);
        this.vmId = vmId;
        this.timestamp = timestamp;
        this.totalCompiles = totalCompiles;
        this.totalBailouts = totalBailouts;
        this.totalInvalidates = totalInvalidates;
        this.compilationTime = compilationTime;
        this.lastSize = lastSize;
        this.lastType = lastType;
        this.lastMethod = lastMethod;
        this.lastFailedType = lastFailedType;
        this.lastFailedMethod = lastFailedMethod;
    }

    @Persist
    public String getVmId() {
        return vmId;
    }

    @Persist
    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    @Persist
    @Override
    public long getTimeStamp() {
        return timestamp;
    }

    @Persist
    public void setTimeStamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /** Number of compilation tasks performed */
    @Persist
    public long getTotalCompiles() {
        return totalCompiles;
    }

    @Persist
    public void setTotalCompiles(long totalCompiles) {
        this.totalCompiles = totalCompiles;
    }

    /** Number of failed compilation tasks */
    @Persist
    public long getTotalBailouts() {
        return totalBailouts;
    }

    @Persist
    public void setTotalBailouts(long totalBailouts) {
        this.totalBailouts = totalBailouts;
    }

    /** Number of invalidated compilation tasks */
    @Persist
    public long getTotalInvalidates() {
        return totalInvalidates;
    }

    @Persist
    public void setTotalInvalidates(long totalInvalidates) {
        this.totalInvalidates = totalInvalidates;
    }

    /** Time spent in compilation. Cumulative, measured in ms */
    @Persist
    public long getCompilationTime() {
        return compilationTime;
    }

    /** Time spent in compilation. Cumulative, measured in ms */
    @Persist
    public void setCompilationTime(long compilationTime) {
        this.compilationTime = compilationTime;
    }

    /** Code Size in bytes of last compilation */
    @Persist
    public long getLastSize() {
        return lastSize;
    }

    @Persist
    public void setLastSize(long lastSize) {
        this.lastSize = lastSize;
    }

    /** Type of last compilation */
    @Persist
    public long getLastType() {
        return lastType;
    }

    @Persist
    public void setLastType(long lastType) {
        this.lastType = lastType;
    }

    /** Name of class and method for last compile */
    @Persist
    public String getLastMethod() {
        return lastMethod;
    }

    @Persist
    public void setLastMethod(String lastMethod) {
        this.lastMethod = lastMethod;
    }

    /** Type of last failed compilation */
    @Persist
    public long getLastFailedType() {
        return lastFailedType;
    }

    @Persist
    public void setLastFailedType(long lastFailedType) {
        this.lastFailedType = lastFailedType;
    }

    /** Name of class and method for last failed compile */
    @Persist
    public String getLastFailedMethod() {
        return lastFailedMethod;
    }

    @Persist
    public void setLastFailedMethod(String lastFailedMethod) {
        this.lastFailedMethod = lastFailedMethod;
    }

}
