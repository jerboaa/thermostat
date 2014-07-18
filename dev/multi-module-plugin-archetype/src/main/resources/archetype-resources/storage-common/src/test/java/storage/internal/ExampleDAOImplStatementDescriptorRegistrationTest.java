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

package ${package}.storage.internal;

import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorMetadataFactory;
import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class ExampleDAOImplStatementDescriptorRegistrationTest {

    static final String NAME = "example-messages";
    static final String REPLACE_DESCRIPTOR = "REPLACE " + NAME +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
            "'message' = ?s " +
            "WHERE '" + Key.AGENT_ID.getName() + "' = ?s";
    static final String QUERY_DESCRIPTOR = "QUERY " + NAME + " WHERE " +
            "'" + Key.AGENT_ID.getName() + "' = ?s";

    @Test
    public void registersAllQueries() {
        ExampleDAOImplStatementDescriptorRegistration reg = new ExampleDAOImplStatementDescriptorRegistration();
        Set<String> descriptors = reg.getStatementDescriptors();
        assertEquals("More descriptors than expected", 2, descriptors.size());
        assertFalse("One of the descriptors is null", descriptors.contains(null));
    }

    @Test
    public void descriptorRegistrationContainsCorrectDescriptors() {
        ExampleDAOImplStatementDescriptorRegistration reg = new ExampleDAOImplStatementDescriptorRegistration();
        Set<String> descriptors = reg.getStatementDescriptors();
        assertTrue("Expected Query Descriptor", descriptors.contains(QUERY_DESCRIPTOR));
        assertTrue("Expected Replace Descriptor", descriptors.contains(REPLACE_DESCRIPTOR));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownDescriptorThrowsException() {
        StatementDescriptorMetadataFactory factory = new ExampleDAOImplStatementDescriptorRegistration();
        factory.getDescriptorMetadata("QUERY foo-bar WHERE 'a' = 'b'", null);
        fail("should have thrown exception");
    }
}

