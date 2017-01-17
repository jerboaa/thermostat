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

package com.redhat.thermostat.storage.core.experimental.statement;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QueryEngineTest {
    private com.redhat.thermostat.storage.core.Category category;
    private FieldDescriptor descriptor0;
    private FieldDescriptor descriptor1;
    private FieldDescriptor descriptor2;

    @Before
    public void setup() {
        category = mock(com.redhat.thermostat.storage.core.Category.class);
        when(category.getName()).thenReturn("testCategory");

        descriptor0 = mock(FieldDescriptor.class);
        when(descriptor0.getName()).thenReturn("descriptor0");
        Class type = String.class;
        when(descriptor0.getType()).thenReturn(type);

        descriptor1 = mock(FieldDescriptor.class);
        when(descriptor1.getName()).thenReturn("descriptor1");
        type = long.class;
        when(descriptor1.getType()).thenReturn(type);

        descriptor2 = mock(FieldDescriptor.class);
        when(descriptor2.getName()).thenReturn("descriptor2");
        type = int.class;
        when(descriptor2.getType()).thenReturn(type);
    }

    @Test
    public void testBuildQuery() throws Exception {
        QueryEngine engine = new QueryEngine();

        engine.prologue(category);
        engine.add(descriptor0, TypeMapper.Criteria.Equal);
        engine.add(descriptor1, TypeMapper.Criteria.GreaterEqual);
        engine.add(descriptor2, TypeMapper.Criteria.LessEqual);

        engine.limit();
        engine.sort(descriptor1, TypeMapper.Sort.Ascending);

        Statement statement = engine.build();

        String expected = "QUERY testCategory WHERE 'descriptor0' = ?s AND 'descriptor1' >= ?l AND 'descriptor2' <= ?i SORT 'descriptor1' ASC LIMIT ?i";

        assertEquals(expected, statement.get());
    }
}
