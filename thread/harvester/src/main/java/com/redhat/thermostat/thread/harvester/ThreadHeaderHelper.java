/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.thread.harvester;

import java.lang.management.ThreadInfo;

import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadHeader;
import com.redhat.thermostat.thread.model.ThreadState;

/**
 */
class ThreadHeaderHelper {

    private ThreadDao threadDao;
    private String vmId;
    private WriterID writerId;

    public ThreadHeaderHelper(ThreadDao threadDao, WriterID writerId, String vmId)
    {
        this.threadDao = threadDao;
        this.vmId = vmId;
        this.writerId = writerId;
    }

    public ThreadHeader createThreadHeader(ThreadInfo beanInfo, long timestamp)
    {
        String wId = writerId.getWriterID();

        ThreadHeader header = new ThreadHeader(wId);
        header.setThreadName(beanInfo.getThreadName());
        header.setThreadId(beanInfo.getThreadId());
        header.setTimeStamp(timestamp);
        header.setVmId(vmId);

        return header;
    }

    /**
     * Checks if the template is in the database. If the template is not in
     * the database, it is added and the same object is returned, otherwise
     * the database object is returned with the correct referenceID field.
     * The input template header itself is never updated.
     */
    public ThreadHeader checkAndSaveThreadHeader(ThreadHeader template) {
        ThreadHeader inStorage = threadDao.getThread(template);
        if (inStorage == null || !template.equals(inStorage)) {
            threadDao.saveThread(template);
            inStorage = template;
        }
        return inStorage;
    }
}
