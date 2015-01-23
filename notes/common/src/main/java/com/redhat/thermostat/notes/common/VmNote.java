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

package com.redhat.thermostat.notes.common;

import java.util.Objects;

import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.storage.model.TimeStampedPojo;

@Entity
public class VmNote extends BasePojo implements TimeStampedPojo, Note {

    /** a GUID */
    private String id;
    private long timeStamp;
    private String vmId;
    private String content;

    public VmNote() {
        super(null);
        this.content = "";
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
    public String getId() {
        return id;
    }

    @Persist
    public void setId(String id) {
        this.id = id;
    }

    @Persist
    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    @Persist
    @Override
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Persist
    @Override
    public void setContent(String content) {
        this.content = content;
    }

    @Persist
    @Override
    public String getContent() {
        return this.content;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAgentId(), vmId, id, timeStamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof VmNote)) {
            return false;
        }
        VmNote other = (VmNote) obj;
        return super.equals(obj)
                && Objects.equals(this.content, other.content)
                && Objects.equals(this.id, other.id)
                && Objects.equals(this.timeStamp, other.timeStamp)
                && Objects.equals(this.vmId, other.vmId);
    }
}
