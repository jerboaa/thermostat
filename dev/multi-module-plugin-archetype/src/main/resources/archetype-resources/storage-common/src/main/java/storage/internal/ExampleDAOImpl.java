/*
 * Copyright 2012 - 2014, Red Hat, Inc.
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

package ${package}.storage.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.WriterID;
import ${package}.storage.ExampleMessage;
import ${package}.storage.ExampleDAO;

public class ExampleDAOImpl implements ExampleDAO {
    
    private static final Logger logger = LoggingUtils.getLogger(ExampleDAOImpl.class);

    private static final String REPLACE_DESCRIPTOR = "REPLACE " + exampleCategory.getName() +
                                                              " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                                                                   "'" + MESSAGE_KEY.getName() + "' = ?s " +
                                                                   "WHERE '" + Key.AGENT_ID.getName() + "' = ?s";
    private static final String QUERY_DESCRIPTOR = "QUERY " + exampleCategory.getName() + " WHERE " +
                                                                   "'" + Key.AGENT_ID.getName() + "' = ?s";
    
    private final Storage storage;
    private final WriterID writerId;
    
    ExampleDAOImpl(Storage storage, WriterID writerId) {
        this.storage = storage;
        this.writerId = writerId;
        storage.registerCategory(exampleCategory);
    }
    
    @Override
    public void putMessage(String message) {
        try {
            StatementDescriptor<ExampleMessage> stmtDesc = new StatementDescriptor<>(exampleCategory, REPLACE_DESCRIPTOR);
            PreparedStatement<ExampleMessage> replace = storage.prepareStatement(stmtDesc);
            replace.setString(0, writerId.getWriterID());
            replace.setString(1, message);
            replace.setString(2, writerId.getWriterID());
            replace.execute();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing '" + REPLACE_DESCRIPTOR + "' failed!", e);
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing '" + REPLACE_DESCRIPTOR + "' failed!", e);
        }
    }
    
    @Override
    public String getMessage(HostRef ref) {
        Cursor<ExampleMessage> cursor = null;
        try {
            StatementDescriptor<ExampleMessage> stmtDesc = new StatementDescriptor<>(exampleCategory, QUERY_DESCRIPTOR);
            PreparedStatement<ExampleMessage> query = storage.prepareStatement(stmtDesc);
            query.setString(0, ref.getAgentId());
            cursor = query.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing '" + QUERY_DESCRIPTOR + "' failed!", e);
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing '" + QUERY_DESCRIPTOR + "' failed!", e);
        }
        if (cursor == null) {
            return null;
        }
        if (cursor.hasNext()) {
            ExampleMessage pojo = cursor.next();
            return pojo.getMessage();
        } else {
            return null;
        }
    }

}

