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

package com.redhat.thermostat.storage.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.redhat.thermostat.storage.model.Pojo;

/**
 * A description for data persisted in storage. It describes how model objects
 * are going to be categorized in persistent storage.
 * 
 * @param <T>
 *            The model class used for data mapped to this category.
 */
public class Category<T extends Pojo> {

    /*
     * The name of the category. This is an de-facto immutable field set only by
     * the constructor via setName(). An exception to this rule is
     * AdaptedCategory and JSON serialization.
     * 
     * This field gets serialized via JSON.
     */
    protected String name;
    /*
     * A de-facto unmodifiable map of key-name => key pairs. A key-name may
     * represent a property in storage. Set via the constructor. Exceptions are
     * AdaptedCategory and JSON serialization.
     * 
     * This key map gets serialized via JSON.
     */
    protected Map<String, Key<?>> keys;

    /*
     * A de-facto unmodifiable list of keys to be indexed. All of these keys
     * should be indexed by the storage (so sorting by this should be possible).
     * Set via the constructor. Exceptions are AdaptedCategory and JSON
     * serialization.
     *
     * This list gets serialized via JSON.
     */
    protected List<Key<?>> indexedKeys;

    /*
     * A de-facto immutable field, set via setDataClass() called by the
     * constructor. If null dataClassName must be set. This is to make Category
     * JSON serializable.
     * 
     * This field does not get serialized. Instead it's name gets serialized.
     */
    private transient Class<T> dataClass;
    /*
     * A de-facto immutable field, set via setDataClass() called by the
     * constructor. Essentially a buddy-field to dataClass.
     * 
     * This field gets serialized via JSON.
     */
    protected String dataClassName;
    
    /* No-arg Constructor.
     * 
     * Used for serialization and - implicitly - by AdaptedCategory
     */
    protected Category() {
        // empty
    }
    
    /**
     * Creates a new Category instance with the specified name.
     * 
     * @param name
     *            the name of the category
     * @param dataClass
     *            the Class object representing the data
     * @param keys
     *            an array of Key object which represent the data for this category
     * 
     * @throws IllegalArgumentException
     *             if a Category is created with a name that has been used
     *             before
     */
    public Category(String name, Class<T> dataClass, Key<?>... keys) {
    	this(name, dataClass, Arrays.asList(keys), Collections.<Key<?>>emptyList());
    }

    /**
     * Creates a new Category instance with the specified name.
     *
     * @param name
     *            the name of the category
     * @param dataClass
     *            the Class object representing the data
     * @param indexedKeys
     *            the keys that will be used for sorting and should be indexed
     *            (or otherwise optimized) by the storage
     * @param keys
     *            an array of Key object which represent the data for this category
     *
     * @throws IllegalArgumentException
     *             if a Category is created with a name that has been used
     *             before
     */
    public Category(String name, Class<T> dataClass, List<Key<?>> keys, List<Key<?>> indexedKeys) {
        Map<String, Key<?>> keysMap = new HashMap<String, Key<?>>();
        for (Key<?> key : keys) {
            keysMap.put(key.getName(), key);
        }
        this.keys = Collections.unmodifiableMap(keysMap);
        this.indexedKeys = Collections.unmodifiableList(indexedKeys);
        setName(name);
        setDataClass(dataClass);
    }

    /**
     * 
     * @return The category name which uniquely identifies this category.
     */
    public String getName() {
        return name;
    }

    private void setName(String name) {
        if (Categories.contains(name)) {
            throw new IllegalStateException("category " + name + " already created!");
        }

        this.name = name;

        if (name != null) {
            Categories.add(this);
        }
    }

    private void setDataClass(Class<T> dataClass) {
        this.dataClass = dataClass;
        if (dataClass != null) {
            dataClassName = dataClass.getName();
        }
    }

    public Class<T> getDataClass() {
        if (dataClass == null && dataClassName != null) {
            initializeDataClassFromName();
        }
        return dataClass;
    }

    @SuppressWarnings("unchecked")
    private void initializeDataClassFromName() {
        try {
            dataClass = (Class<T>) Class.forName(dataClassName);
        } catch (ClassNotFoundException e) {
            throw new StorageException(e);
        }
    }

    /**
     * 
     * @return A collection of {@link Key}s for this category or an empty
     *         collection if no keys.
     */
    public synchronized Collection<Key<?>> getKeys() {
        if (keys == null) {
            return Collections.emptySet();
        }
        return keys.values();
    }

    /**
     * 
     * @param name
     *            The name of the key to retrieve.
     * @return The key with the specified name or {@code null} if there was no
     *         such key.
     */
    public Key<?> getKey(String name) {
        if (keys == null) {
            return null;
        }
        return keys.get(name);
    }

    public List<Key<?>> getIndexedKeys() {
    	return indexedKeys;
    }

    @Override
    public String toString() {
        return getName() + "|" + getDataClass().getName() + "|" + keys;
    }
    
    @Override
    public int hashCode() {
        /*
         * The assumption is that name, keys and dataClass are immutable once
         * created. This occurs either via JSON deserialization, the only public
         * constructor or AdaptedCategory.
         */

        // ignore indexed keys intentionally
        return Objects.hash(name, keys, getDataClass());
    }

    @Override
    public boolean equals(Object o) {
        if (! (o instanceof Category)) {
            return false;
        }
        Category<?> other = (Category<?>) o;
        // ignore indexed keys intentionally
        return Objects.equals(name, other.name) &&
                Objects.equals(keys, other.keys) &&
                Objects.equals(getDataClass(), other.getDataClass());
    }
}

