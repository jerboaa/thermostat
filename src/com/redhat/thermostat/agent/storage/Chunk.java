package com.redhat.thermostat.agent.storage;

import java.util.HashMap;
import java.util.Map;

/**
 * A Chunk is a unit containing a set of data that can be added as a whole to the dataset
 * that exists behind the storage layer.
 */
public class Chunk {
    private final Category category;
    private final boolean replace;

    private Map<Key, String> values = new HashMap<Key, String>();

    /**
     * 
     * @param timestamp The time that should be associated with the data in this nugget.
     * @param category The {@link Category} of this data.  This should be a Category that the {@link Backend}
     * who is producing this Chunk has registered via {@link Storage#registerCategory()}
     * @param add whether this chunk should replace the values based on the keys for this category,
     * or be added to a set of values in this category
     */
    public Chunk(Category category, boolean replace) {
        this.category = category;
        this.replace = replace;
    }

    public Category getCategory() {
        return category;
    }

    public boolean getReplace() {
        return replace;
    }

    public void put(Key entry, String value) {
        values.put(entry, value);
    }

    public String get(Key entry) {
        return values.get(entry);
    }
}
