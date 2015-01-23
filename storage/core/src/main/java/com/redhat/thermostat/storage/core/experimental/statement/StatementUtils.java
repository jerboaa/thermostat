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

import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.model.Pojo;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class StatementUtils {

    static boolean isPersistent(PropertyDescriptor desc) {
        return hasAnnotation(desc, Persist.class);
    }

    static boolean isIndexed(PropertyDescriptor desc) {
        return hasAnnotation(desc, Indexed.class);
    }

    static <T extends Pojo> String getDocument(Class<T> pojoClass) {
        if (!pojoClass.isAnnotationPresent(Category.class)) {
            return null;
        }

        return pojoClass.getAnnotation(Category.class).value();
    }

    static boolean hasAnnotation(PropertyDescriptor desc,
                                 Class<? extends Annotation> annotation)
    {
        Method readMethod = desc.getReadMethod();
        boolean hasRead = (readMethod != null &&
                           readMethod.isAnnotationPresent(annotation));

        Method writeMethod = desc.getWriteMethod();
        boolean hasWrite = (writeMethod != null &&
                            writeMethod.isAnnotationPresent(annotation));

        return hasRead && hasWrite;
    }

    public static Map<String, FieldDescriptor> createDescriptorMap(List<FieldDescriptor> descriptors) {
        Map<String, FieldDescriptor> map = new HashMap<>();
        for (FieldDescriptor desc : descriptors) {
            map.put(desc.getName(), desc);
        }
        return map;
    }

    public static <T extends Pojo> List<FieldDescriptor> createDescriptors(Class<T> pojoClass) {
        try {
            BeanInfo info = Introspector.getBeanInfo(pojoClass);
            PropertyDescriptor[] props = info.getPropertyDescriptors();

            List<FieldDescriptor> descriptors = new ArrayList<>();
            for (PropertyDescriptor desc : props) {

                if (StatementUtils.isPersistent(desc)) {
                    FieldDescriptor descriptor = new FieldDescriptor();
                    descriptor.setType(desc.getPropertyType());
                    descriptor.setName(desc.getName());
                    descriptor.setIndexed(StatementUtils.isIndexed(desc));

                    descriptors.add(descriptor);
                }
            }

            // Afaik it's not specified that methods are returned or listed in
            // any particular order by Introspector.getBeanInfo, so if we don't
            // sort, we may end up with two differently ordered lists in two
            // different calls to this method.
            // This is a problem because the prepated statement API doesn't
            // check if two statements are the same if fields are sorted in
            // random order, it only considers two statements the same if
            // their string representation completely matches;
            // it also make the code depending on FieldDescriptor easier to
            // write if it can count on the ordering being always consistent
            Collections.sort(descriptors, new Comparator<FieldDescriptor>() {
                @Override
                public int compare(FieldDescriptor o1, FieldDescriptor o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            return Collections.unmodifiableList(descriptors);

        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    static <T extends Pojo> Value getValue(T bean,  FieldDescriptor descriptor)
    {
        try {
            PropertyDescriptor property =
                    new PropertyDescriptor(descriptor.getName(),
                                           bean.getClass());
            Method method = property.getReadMethod();
            method.setAccessible(true);

            return new Value(method.invoke(bean, new Object[0]));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static <T extends Pojo> void setData(PreparedStatement<T> prepared,
                                         Value value, int index)
    {
        Class type = value.get().getClass();
        if (type.isAssignableFrom(int.class) ||
            type.isAssignableFrom(Integer.class))
        {
            prepared.setInt(index, ((Integer) value.get()).intValue());

        } else if (type.isAssignableFrom(long.class) ||
                   type.isAssignableFrom(Long.class))
        {
            prepared.setLong(index, ((Long) value.get()).longValue());

        } else if (type.isAssignableFrom(boolean.class) ||
                   type.isAssignableFrom(Boolean.class))
        {
            prepared.setBoolean(index, ((Boolean) value.get()).booleanValue());

        } else if (type.isAssignableFrom(String.class)) {
            prepared.setString(index, (String) value.get());
        }
    }
}
