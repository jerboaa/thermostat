/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.common.storage;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;

/**
 * Adapts a bean into a Chunk. The bean must be annotated with {@link Entity}.
 * All methods to persist must be annotated with {@link Persist}
 */
public class ChunkAdapter extends Chunk {

    private final Object adaptee;

    public ChunkAdapter(Object obj) {
        this(obj, null, false);
        Set<Key<?>> keys = identifyKeys(obj);
        category = createCategory(obj, keys);
    }

    public ChunkAdapter(Object obj, Category category, boolean replace) {
        super(category, replace);
        checkForAnnotation(obj);
        adaptee = obj;
    }

    public Object getAdaptee() {
        return adaptee;
    }

    private void checkForAnnotation(Object toCheck) {
        if (!toCheck.getClass().isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("object to adapt must be annotated with Entity");
        }
    }

    private Set<Key<?>> identifyKeys(Object obj) {
        Set<Key<?>> keys = new HashSet<>();

        PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(obj);
        for (PropertyDescriptor descriptor : descriptors) {
            if (hasValidGetAndSetMethods(descriptor)) {
                // FIXME this is sometimes a partial key
                Key<?> key = new Key<>(findKeyName(descriptor), false);
                keys.add(key);
            }
        }
        return keys;
    }

    private boolean hasValidGetAndSetMethods(PropertyDescriptor descriptor) {
        Method readMethod = descriptor.getReadMethod();
        Method writeMethod = descriptor.getWriteMethod();

        if (readMethod != null && writeMethod != null) {
            if (readMethod.isAnnotationPresent(Persist.class) &&
                    writeMethod.isAnnotationPresent(Persist.class)) {
                return true;
            } else if (readMethod.isAnnotationPresent(Persist.class) ^
                    writeMethod.isAnnotationPresent(Persist.class)) {
                throw new IllegalArgumentException("annotation only present on one of get/set method");
            }
        }
        return false;
    }

    private String findKeyName(PropertyDescriptor descriptor) {
        final String NAME_UNSPECIFIED = "";

        String computedName = descriptor.getName();
        String nameOnGetMethod = descriptor.getReadMethod().getAnnotation(Persist.class).name();
        String nameOnSetMethod = descriptor.getWriteMethod().getAnnotation(Persist.class).name();

        String attributeName;
        if (nameOnGetMethod.equals(NAME_UNSPECIFIED) && nameOnSetMethod.equals(NAME_UNSPECIFIED)) {
            attributeName = computedName;
        } else {
            if (!nameOnGetMethod.equals(nameOnSetMethod) && nameOnSetMethod.equals(NAME_UNSPECIFIED)) {
                attributeName = nameOnGetMethod;
            } else if (!nameOnSetMethod.equals(nameOnGetMethod) && nameOnGetMethod.equals(NAME_UNSPECIFIED)) {
                attributeName = nameOnSetMethod;
            } else {
                throw new IllegalArgumentException("set/get methods have mismatching names in annotation");
            }
        }

        return attributeName;
    }

    private Category createCategory(Object obj, Set<Key<?>> keys) {
        String newCategoryName = findCategoryName(obj);
        Category category;
        if (Categories.contains(newCategoryName)) {
            Set<Key<?>> existingKeys= new HashSet<>(Categories.getByName(newCategoryName).getKeys());
            if (!keys.equals(existingKeys)) {
                throw new IllegalArgumentException("this class, with a differet organization was seen previously");
            }
            category = Categories.getByName(newCategoryName);
        } else {
            category = new Category(newCategoryName, keys.toArray(new Key<?>[0]));
        }
        return category;
    }

    private String findCategoryName(Object obj) {
        Entity categoryAnnotation = obj.getClass().getAnnotation(Entity.class);
        String desiredName = categoryAnnotation.name();
        if (desiredName.equals("")) {
            desiredName = obj.getClass().getSimpleName();
        }
        return desiredName;
    }

    @Override
    public <T> T get(Key<T> entry) {
        try {
            return (T) PropertyUtils.getProperty(adaptee, entry.getName());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            try {
                System.err.println(BeanUtils.describe(adaptee));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public <T> void put(Key<T> entry, T value) {
        String keyName = entry.getName();
        try {
            BeanUtils.setProperty(adaptee, keyName, value);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            try {
                System.err.println(BeanUtils.describe(adaptee));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public Set<Key<?>> getKeys() {
        Category category = getCategory();
        return new HashSet<>(category.getKeys());
    }

}
