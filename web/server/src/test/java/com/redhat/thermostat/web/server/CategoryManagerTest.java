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

package com.redhat.thermostat.web.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.web.common.SharedStateId;
import com.redhat.thermostat.web.server.CategoryManager.CategoryIdentifier;

public class CategoryManagerTest {

    // CategoryIdentifier tests
    
    @Test
    public void testCategoryIdentifierEquals() {
        String catName = "foo-collection";
        String dataClassName = "com.redhat.thermostat.storage.core.model.Foo";
        CategoryIdentifier id = new CategoryIdentifier(catName, dataClassName);
        assertTrue(id.equals(id));
        assertFalse(id.equals(null));
        String dataClass2 = "com.redhat.thermostat.storage.core.model.Bar";
        CategoryIdentifier id2 = new CategoryIdentifier(catName, dataClass2);
        assertFalse("different data classes", id.equals(id2));
        String otherCatName = "bar-collection";
        CategoryIdentifier id3 = new CategoryIdentifier(otherCatName, dataClassName);
        assertFalse("different category names", id.equals(id3));
        CategoryIdentifier id4 = new CategoryIdentifier(catName, dataClassName);
        assertNotSame(id, id4);
        assertTrue("category name and data class name match up", id.equals(id4));
    }
    
    @Test
    public void testCategoryIdentifierHashCode() {
        String catName = "foo-collection";
        String dataClassName = "com.redhat.thermostat.storage.core.model.Foo";
        CategoryIdentifier id = new CategoryIdentifier(catName, dataClassName);
        assertTrue(id.hashCode() == id.hashCode());
        String dataClass2 = "com.redhat.thermostat.storage.core.model.Bar";
        CategoryIdentifier id2 = new CategoryIdentifier(catName, dataClass2);
        assertTrue("different data classes", id.hashCode() != id2.hashCode());
        String otherCatName = "bar-collection";
        CategoryIdentifier id3 = new CategoryIdentifier(otherCatName, dataClassName);
        assertTrue("different category names", id.hashCode() != id3.hashCode());
        CategoryIdentifier id4 = new CategoryIdentifier(catName, dataClassName);
        assertNotSame(id, id4);
        assertTrue("category name and data class name match up", id.hashCode() == id4.hashCode());
    }
    
    // CategoryManager tests
    
    /**
     * Verifies that only categories can be added to a manager correctly. It
     * should add a category which has been added already (same cat-identifier)
     * only once.
     */
    @Test
    public void testPutCategory() {
        UUID serverToken = UUID.randomUUID();
        String catName = "foo-collection";
        String dataClassName = "com.redhat.thermostat.storage.core.model.Foo";
        CategoryIdentifier identifier = new CategoryIdentifier(catName, dataClassName);
        Category<?> mockCategory = mock(Category.class);
        CategoryManager manager = new CategoryManager();
        SharedStateId id = manager.putCategory(serverToken, mockCategory, identifier);
        assertNotNull(id);
        assertEquals(serverToken, id.getServerToken());
        assertEquals(0, id.getId());
        CategoryIdentifier identifier2 = new CategoryIdentifier("bar", dataClassName);
        Category<?> otherCat = mock(Category.class);
        SharedStateId otherId = manager.putCategory(serverToken, otherCat, identifier2);
        assertNotNull(otherId);
        assertNotSame(id, otherId);
        assertFalse(otherId.equals(id));
        
        // This should give back the same id since it's the very same identifier
        SharedStateId thirdId = manager.putCategory(serverToken, mockCategory, identifier);
        assertSame(id, thirdId);
    }
    
    /**
     * Verifies that NPEs get thrown when essential params are null.
     */
    @Test
    public void testPutCategoryNullParams() {
        UUID serverToken = UUID.randomUUID();
        String catName = "foo-collection";
        String dataClassName = "com.redhat.thermostat.storage.core.model.Foo";
        CategoryIdentifier identifier = new CategoryIdentifier(catName, dataClassName);
        Category<?> mockCategory = mock(Category.class);
        CategoryManager manager = new CategoryManager();
        try {
            manager.putCategory(null, mockCategory, identifier);
            fail("Expected NPE due to null server token");
        } catch (NullPointerException e) {
            // pass
        }
        try {
            manager.putCategory(serverToken, null, identifier);
            fail("Expected NPE due to null category");
        } catch (NullPointerException e) {
            // pass
        }
        try {
            manager.putCategory(serverToken, mockCategory, null);
            fail("Expected NPE due to null category identifier");
        } catch (NullPointerException e) {
            // pass
        }
    }
    
    @Test
    public void testGetCategoryIdNullParams() {
        CategoryManager manager = new CategoryManager();
        try {
            manager.getCategoryId(null);
            fail("Expected NPE due to null identifier");
        } catch (NullPointerException e) {
            // pass
        }
    }
    
    @Test
    public void canGetCategoryId() {
        UUID serverNonce = UUID.randomUUID();
        CategoryManager manager = new CategoryManager();
        CategoryIdentifier catIdentifier = mock(CategoryIdentifier.class);
        Category<?> mockCat = mock(Category.class);
        SharedStateId id = manager.putCategory(serverNonce, mockCat, catIdentifier);
        SharedStateId getId = manager.getCategoryId(catIdentifier);
        assertEquals(id, getId);
        assertSame(id, getId);
        
        // unknown cat identifier should return null
        CategoryIdentifier otherIdentifier = mock(CategoryIdentifier.class);
        assertNull(manager.getCategoryId(otherIdentifier));
    }
    
    @Test
    public void testGetCategoryNullParams() {
        CategoryManager manager = new CategoryManager();
        try {
            manager.getCategory(null);
            fail("Expected NPE due to null shared state id");
        } catch (NullPointerException e) {
            // pass
        }
    }
    
    /**
     * Check CategoryManager.getCategory() and CategoryManager.getCategoryId()
     * in tandem.
     */
    @Test
    public void canGetCategoryCategoryId() {
        UUID serverNonce = UUID.randomUUID();
        CategoryManager manager = new CategoryManager();
        assertNull(manager.getCategory(mock(SharedStateId.class)));
        assertNull(manager.getCategoryId(mock(CategoryIdentifier.class)));
        
        // add an element
        Category<?> category = mock(Category.class);
        CategoryIdentifier catId = new CategoryIdentifier("foo", "bar");
        SharedStateId id = manager.putCategory(serverNonce, category, catId);
        
        // getting it should never be null now
        Category<?> cat = manager.getCategory(id);
        assertNotNull(cat);
        SharedStateId otherId = manager.getCategoryId(catId);
        assertNotNull(otherId);
        
        assertSame(id, otherId);
    }
}
