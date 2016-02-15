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

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.model.Pojo;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class CategoryBuilder<T extends Pojo> {

    private static final String LOCK = new String("CategoryBuilder_lock");
    private static final Map<String, Category> categories = new HashMap<>();

    private Class<T> bean;

    public CategoryBuilder(Class<T> bean) {
        this.bean = bean;
    }

    public Category<T> build() {

        synchronized (LOCK) {

            String document = StatementUtils.getDocument(bean);

            // This helps performance as well, but it's needed because Category
            // doesn't allow us to create the same twice, even with all the
            // same data; categories are in fact some kind of singleton
            if (categories.containsKey(document)) {
                return categories.get(document);
            }

            List<FieldDescriptor> descriptors =
                    StatementUtils.createDescriptors(bean);

            // we first build the keys and the indexed keys
            List<Key> indexed = new ArrayList<>();
            List<Key> keys = new ArrayList<>();

            // key are easy, the arguments are in the form:
            // new Key<Type>(String name), we don't need reflection

            for (FieldDescriptor descriptor : descriptors) {

                Key key = new Key(descriptor.getName());

                keys.add(key);
                if (descriptor.isIndexed()) {
                    indexed.add(key);
                }
            }

            List<Class<?>> argumentClasses = new ArrayList<>();
            argumentClasses.add(String.class);
            argumentClasses.add(Class.class);
            argumentClasses.add(List.class);

            List argumentObjects = new ArrayList();
            argumentObjects.add(document);
            argumentObjects.add(bean);
            argumentObjects.add(keys);

            // indexed keys are optional
            if (!indexed.isEmpty()) {
                argumentClasses.add(List.class);
                argumentObjects.add(indexed);
            }

            Class<?>[] classes = argumentClasses.toArray(new Class[argumentClasses.size()]);
            Object[] objects = argumentObjects.toArray();

            Category<T> category = create(classes, objects);
            categories.put(document, category);

            return category;
        }
    }

    private Category<T> create(Class[] classes, Object[] objects) {

        // we are using this constructor:
        // Category(String name, Class<T> dataClass,
        //          List<Key<?>> keys, List<Key<?>> indexedKeys)

        try {
            Constructor<Category> constructor =
                    Category.class.getConstructor(classes);
            constructor.setAccessible(true);

            return constructor.newInstance(objects);

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
