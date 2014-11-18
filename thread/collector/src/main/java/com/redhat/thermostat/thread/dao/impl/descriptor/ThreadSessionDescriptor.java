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

package com.redhat.thermostat.thread.dao.impl.descriptor;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.thread.model.ThreadSession;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ThreadSessionDescriptor extends Descriptor<ThreadSession> {

    private static final Logger logger = LoggingUtils.getLogger(ThreadSessionDescriptor.class);

    protected String querySessions;
    protected String statementAdd;

    @Override
    public Set<String> describe() {
        Set<String> description = new HashSet<>();
        description.add(querySessions);
        return description;
    }

    public Cursor<ThreadSession> queryGet(VmRef ref, Range<Long> range,
                                          int limit, Storage storage)
            throws DescriptorParsingException, StatementExecutionException
    {
        // "QUERY vm-thread-session WHERE 'vmId' = ?s , 'timeStamp' >= ?l
        //  AND 'timeStamp' <= ?l SORT 'timeStamp' DSC LIMIT ?i";

        StatementDescriptor<ThreadSession> desc = null;
        try {
            desc = new StatementDescriptor<>(getCategory(), querySessions);
            PreparedStatement<ThreadSession> prepared =
                    storage.prepareStatement(desc);

            int i = 0;
            prepared.setString(i++, ref.getVmId());

            prepared.setLong(i++, range.getMin());
            prepared.setLong(i++, range.getMax());

            prepared.setInt(i++, limit);

            return prepared.executeQuery();

        } catch (Exception e) {
            logger.log(Level.WARNING,
                       "exception executing statement: " + desc, e);
            throw e;
        }
    }

    public void statementAdd(ThreadSession session, Storage storage)
            throws DescriptorParsingException, StatementExecutionException
    {
        // "ADD vm-thread-session SET 'agentId' = ?s , 'vmId' = ?s ,
        // 'session' = ?s , 'timeStamp' = ?l"

        StatementDescriptor<ThreadSession> desc =
                new StatementDescriptor<>(getCategory(), statementAdd);

        PreparedStatement<ThreadSession> prepared;
        try {

            prepared = storage.prepareStatement(desc);

            int i = 0;
            prepared.setString(i++, session.getAgentId());
            prepared.setString(i++, session.getVmId());
            prepared.setString(i++, session.getSession());

            prepared.setLong(i++, session.getTimeStamp());

            prepared.execute();

        } catch (Exception e) {
            logger.log(Level.WARNING, "exception executing statement: " + desc, e);
            throw e;
        }
    }
}
