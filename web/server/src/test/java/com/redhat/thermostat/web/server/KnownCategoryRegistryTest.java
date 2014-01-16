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

package com.redhat.thermostat.web.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.redhat.thermostat.storage.core.auth.CategoryRegistration;

public class KnownCategoryRegistryTest {
    
    private static final String DUPLICATE_CAT_NAME = "some-category";

    @Test
    public void canAddCategories() {
        Set<String> categoryNames = new HashSet<>();
        categoryNames.add("some-category"); // duplicate
        categoryNames.add("some-other-category");
        Iterable<CategoryRegistration> regs = getRegs(categoryNames);
        KnownCategoryRegistry reg = null;
        try {
            reg = new KnownCategoryRegistry(regs);
        } catch (IllegalStateException e) {
            // should not fail
            fail(e.getMessage());
        }
        Set<String> actualRegs = reg.getRegisteredCategoryNames();
        assertFalse(actualRegs.contains(null));
        assertEquals(2, actualRegs.size());
        for (String item: categoryNames) {
            assertTrue(actualRegs.contains(item));
        }
    }
    
    private Iterable<CategoryRegistration> getRegs(Set<String> categoryNames) {
        Set<CategoryRegistration> regs = new HashSet<>();
        regs.add(new TestCategoryRegistration(categoryNames));
        Set<String> myDup = new HashSet<>();
        myDup.add(DUPLICATE_CAT_NAME);
        regs.add(new TestCategoryRegistration(myDup));
        return regs;
    }

    @Test
    public void refuseToAddNullCategoryName() {
        Set<String> categoryNames = new HashSet<>();
        categoryNames.add(null);
        categoryNames.add("some-other-category");
        Iterable<CategoryRegistration> regs = getRegs(categoryNames);
        try {
            new KnownCategoryRegistry(regs);
            fail("Should not reach here, null name added!");
        } catch (IllegalStateException e) {
            // pass
        }
    }
    
    @Test
    public void canLoadRegistrationsInClasspath() {
        KnownCategoryRegistry reg = new KnownCategoryRegistry();
        Set<String> actualRegs = reg.getRegisteredCategoryNames();
        assertNotNull(actualRegs);
        // storage-core registers them. no-other modules in classpath.
        assertEquals(5, actualRegs.size());
    }
    
    private static class TestCategoryRegistration implements CategoryRegistration {

        private final Set<String> catNames;
        
        private TestCategoryRegistration(Set<String> catNames) {
            this.catNames = catNames;
        }
        
        @Override
        public Set<String> getCategoryNames() {
            return catNames;
        }
        
    }
    
}

