package com.redhat.thermostat.agent.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Category {
    private final String name;
    private final List<Key> keys;
    private boolean locked = false;

    public Category(String name) {
        this.name = name;
        keys = new ArrayList<Key>();
    }

    public String getName() {
        return name;
    }

    public synchronized void lock() {
        locked = true;
    }

    public synchronized void addKey(Key key) {
        if (!locked) {
            keys.add(key);
        } else {
            throw new IllegalStateException("Once locked, a category's keys may not be changed.");
        }
    }

    public synchronized Iterator<Key> getEntryIterator() {
        return keys.iterator();
    }
}
