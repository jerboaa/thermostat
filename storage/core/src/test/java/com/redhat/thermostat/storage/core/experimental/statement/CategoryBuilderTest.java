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

package com.redhat.thermostat.storage.core.experimental.statement;

import com.redhat.thermostat.storage.core.Key;
import java.util.Collection;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CategoryBuilderTest {

    private static final String CATEGORY_NAME = "testCategory";
    @Test
    public void testBuild() throws Exception {
        CategoryBuilder<SampleBean> builder =  new CategoryBuilder<>(SampleBean.class);
        com.redhat.thermostat.storage.core.Category<SampleBean> category =
                builder.build();

        assertNotNull(category);
        assertEquals(CATEGORY_NAME, category.getName());
        assertEquals(SampleBean.class, category.getDataClass());

        List<Key<?>> indexed = category.getIndexedKeys();
        assertEquals(2, indexed.size());

        Key key0 = new Key("vmId");
        Key key1 = new Key("timeStamp");

        assertTrue(indexed.contains(key0));
        assertTrue(indexed.contains(key1));

        Collection<Key<?>> keys = category.getKeys();
        assertEquals(3, keys.size());

        Key key2 = new Key("name");

        assertTrue(keys.contains(key0));
        assertTrue(keys.contains(key1));
        assertTrue(keys.contains(key2));
    }
}
