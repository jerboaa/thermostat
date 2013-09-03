/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.storage.internal.statement;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.internal.statement.PatchedSetListPojoConverter.IllegalPojoException;
import com.redhat.thermostat.storage.model.Pojo;

public class PatchedSetListPojoConverterTest {

    @Test
    public void testBasicConversion() throws IllegalPojoException {
        PatchedSetListMember mem1 = new PatchedSetListMember(new Key<>("foo"), "foo-val");
        PatchedSetListMember mem2 = new PatchedSetListMember(new Key<>("barKey"), Long.MAX_VALUE);
        PatchedSetListMember[] members = new PatchedSetListMember[] {
                mem1,
                mem2
        };
        PatchedSetList setList = mock(PatchedSetList.class);
        when(setList.getSetListMembers()).thenReturn(members);
        PatchedSetListPojoConverter<TestMe> converter = new PatchedSetListPojoConverter<>(setList, TestMe.class);
        TestMe instance = converter.convertToPojo();
        assertEquals("foo-val", instance.getFoo());
        assertEquals(Long.MAX_VALUE, instance.getBarKey());
    }
    
    @Test
    public void testConversionFailBasic() {
        PatchedSetListMember mem1 = new PatchedSetListMember(new Key<>("wrong-Prop"), "foo-val");
        PatchedSetListMember[] members = new PatchedSetListMember[] {
                mem1
        };
        PatchedSetList setList = mock(PatchedSetList.class);
        when(setList.getSetListMembers()).thenReturn(members);
        PatchedSetListPojoConverter<TestMe> converter = new PatchedSetListPojoConverter<>(setList, TestMe.class);
        try {
            converter.convertToPojo();
            fail("Should not convert, property not present in Pojo");
        } catch (IllegalPojoException e) {
            // pass
            assertTrue(e.getMessage().contains("Property wrong-Prop not found in Pojo:"));
        }
    }
    
    public static class TestMe implements Pojo {
        
        private String foo;
        private long barKey;

        public String getFoo() {
            return foo;
        }
        public void setFoo(String foo) {
            this.foo = foo;
        }
        public long getBarKey() {
            return barKey;
        }
        public void setBarKey(long barKey) {
            this.barKey = barKey;
        }
    }
}
