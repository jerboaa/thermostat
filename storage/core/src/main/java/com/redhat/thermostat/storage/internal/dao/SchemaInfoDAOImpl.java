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

package com.redhat.thermostat.storage.internal.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.SchemaInfo;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.SchemaInfoDAO;
import com.redhat.thermostat.storage.model.SchemaInformation;

public class SchemaInfoDAOImpl implements SchemaInfoDAO {
    private static final Logger logger = LoggingUtils.getLogger(SchemaInfoDAOImpl.class);
    static final String QUERY_ALL_COLLECTIONS = "QUERY "
            + SchemaInfo.CATEGORY.getName();      
    private final Storage storage;
    
    public SchemaInfoDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(SchemaInfo.CATEGORY);
    }
    
    @Override
    public List<SchemaInformation> getSchemaInfos() {
        Cursor<SchemaInformation> schemaCursor;
        StatementDescriptor<SchemaInformation> desc = new StatementDescriptor<>(SchemaInfo.CATEGORY, QUERY_ALL_COLLECTIONS);
        PreparedStatement<SchemaInformation> prepared = null;
        try {
            prepared = storage.prepareStatement(desc);
            schemaCursor = prepared.executeQuery();
        } catch (DescriptorParsingException | StatementExecutionException e) {
            logger.log(Level.SEVERE, "Query '" + desc + "' failed!", e);
            return Collections.emptyList();
        }
        
        List<SchemaInformation> results = new ArrayList<>();
        
        while (schemaCursor.hasNext()) {
            SchemaInformation elementInfo = schemaCursor.next();
            results.add(elementInfo);
        }
        
        return results;
    }

}