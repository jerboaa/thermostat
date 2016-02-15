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

package com.redhat.thermostat.storage.core.experimental.statement;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.redhat.thermostat.storage.core.Id;

public class QueryValuesTest {

    private Query query;
    private List<Criterion> criterias;

    private Criterion criterion0;
    private Criterion criterion1;
    private Criterion criterion2;

    @Before
    public void setup() {
        query = mock(Query.class);
        criterias = new ArrayList<>();

        criterion0 = mock(Criterion.class);
        when(criterion0.getType()).thenReturn((Class)  int.class);
        when(criterion0.getId()).thenReturn(new Id("0"));

        criterion1 = mock(Criterion.class);
        when(criterion1.getType()).thenReturn((Class) String.class);
        when(criterion1.getId()).thenReturn(new Id("1"));

        criterion2 = mock(Criterion.class);
        when(criterion2.getType()).thenReturn((Class) long.class);
        when(criterion2.getId()).thenReturn(new Id("2"));

        criterias.add(criterion0);
        criterias.add(criterion1);
        criterias.add(criterion2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetWithWrongArguments() throws Exception {

        QueryValues values = new QueryValues(query);
        values.addCriteria(criterias);

        values.set(criterion0, "wrong type");
    }

    @Test
    public void testSet() throws Exception {

        QueryValues values = new QueryValues(query);
        values.addCriteria(criterias);

        values.set(criterion0, 10);
        values.set(criterion1, "test");
        values.set(criterion2, 42l);
    }
}
