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

package com.redhat.thermostat.thread.model;

import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.storage.model.TimeStampedPojo;

@Entity
public class LockInfo extends BasePojo implements TimeStampedPojo {

    public static final int UNKNOWN = -1;

    private long timeStamp;
    private String vmId;
    private long contendedLockAttempts;
    private long deflations;
    private long emptyNotifications;
    private long failedSpins;
    private long futileWakeups;
    private long inflations;
    private long monExtant;
    private long monInCirculation;
    private long monScavenged;
    private long notifications;
    private long parks;
    private long privateA;
    private long privateB;
    private long slowEnter;
    private long slowExit;
    private long slowNotify;
    private long slowNotifyAll;
    private long successfulSpins;

    /** for de-serialization only */
    public LockInfo() {
        super(null);
    }

    public LockInfo(long timeStamp, String writerId, String vmId,
            long contendedLockAttempts, long deflations, long emptyNotifications,
            long failedSpins, long futileWakeups, long inflations,
            long monExtant, long monInCirculation, long monScavenged,
            long notifications, long parks, long privateA, long privateB,
            long slowEnter, long slowExit, long slowNotify, long slowNotifyAll,
            long successfulSpins) {
        super(writerId);
        this.vmId = vmId;
        this.timeStamp = timeStamp;
        this.contendedLockAttempts = contendedLockAttempts ;
        this.deflations = deflations;
        this.emptyNotifications = emptyNotifications;
        this.failedSpins = failedSpins;
        this.futileWakeups = futileWakeups;
        this.inflations = inflations;
        this.monExtant = monExtant;
        this.monInCirculation = monInCirculation;
        this.monScavenged = monScavenged;
        this.notifications = notifications;
        this.parks = parks;
        this.privateA = privateA;
        this.privateB = privateB;
        this.slowEnter = slowEnter;
        this.slowExit = slowExit;
        this.slowNotify = slowNotify;
        this.slowNotifyAll = slowNotifyAll;
        this.successfulSpins = successfulSpins;
    }

    @Override
    @Persist
    public long getTimeStamp() {
        return timeStamp;
    }

    @Persist
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
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
    public long getContendedLockAttempts() {
        return contendedLockAttempts;
    }

    @Persist
    public void setContendedLockAttempts(long newValue) {
        this.contendedLockAttempts = newValue;
    }

    @Persist
    public long getDeflations() {
        return deflations;
    }

    @Persist
    public void setDeflations(long newValue) {
        this.deflations = newValue;
    }

    @Persist
    public long getEmptyNotifications() {
        return emptyNotifications;
    }

    @Persist
    public void setEmptyNotifications(long newValue) {
        this.emptyNotifications = newValue;
    }

    @Persist
    public long getFailedSpins() {
        return failedSpins;
    }

    @Persist
    public void setFailedSpins(long newValue) {
        this.failedSpins = newValue;
    }

    @Persist
    public long getFutileWakeups() {
        return futileWakeups;
    }

    @Persist
    public void setFutileWakeups(long newValue) {
        this.futileWakeups = newValue;
    }

    @Persist
    public long getInflations() {
        return inflations;
    }

    @Persist
    public void setInflations(long newValue) {
        this.inflations = newValue;
    }

    @Persist
    public long getMonExtant() {
        return monExtant;
    }

    @Persist
    public void setMonExtant(long newValue) {
        this.monExtant = newValue;
    }

    @Persist
    public long getMonInCirculation() {
        return monInCirculation;
    }

    @Persist
    public void setMonInCirculation(long newValue) {
        this.monInCirculation = newValue;
    }

    @Persist
    public long getMonScavenged() {
        return monScavenged;
    }

    @Persist
    public void setMonScavenged(long newValue) {
        this.monScavenged = newValue;
    }

    @Persist
    public long getNotifications() {
        return notifications;
    }

    @Persist
    public void setNotifications(long newValue) {
        this.notifications = newValue;
    }

    @Persist
    public long getParks() {
        return parks;
    }

    @Persist
    public void setParks(long newValue) {
        parks = newValue;
    }

    @Persist
    public long getPrivateA() {
        return privateA;
    }

    @Persist
    public void setPrivateA(long newValue) {
        this.privateA = newValue;
    }

    @Persist
    public long getPrivateB() {
        return privateB;
    }

    @Persist
    public void setPrivateB(long newValue) {
        this.privateB = newValue;
    }

    @Persist
    public long getSlowEnter() {
        return slowEnter;
    }

    @Persist
    public void setSlowEnter(long newValue) {
        this.slowEnter = newValue;
    }

    @Persist
    public long getSlowExit() {
        return slowExit;
    }

    @Persist
    public void setSlowExit(long newValue) {
        this.slowExit = newValue;
    }

    @Persist
    public long getSlowNotify() {
        return slowNotify;
    }

    @Persist
    public void setSlowNotify(long newValue) {
        this.slowNotify = newValue;
    }

    @Persist
    public long getSlowNotifyAll() {
        return slowNotifyAll;
    }

    @Persist
    public void setSlowNotifyAll(long newValue) {
        this.slowNotifyAll = newValue;
    }

    @Persist
    public long getSuccessfulSpins() {
        return successfulSpins;
    }

    @Persist
    public void setSuccessfulSpins(long newValue) {
        this.successfulSpins = newValue;
    }

}
