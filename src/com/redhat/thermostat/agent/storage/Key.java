package com.redhat.thermostat.agent.storage;

/**
 * A Key is used to refer to data in a {@link Chunk}.  It may also be a partial key to the
 * set of data represented by a {@link Chunk} in a category.
 */
public class Key {

    // Key used by most Categories.
    public static Key TIMESTAMP = new Key("timestamp", false);

    private String name;
    private boolean isPartialCategoryKey;

    public Key(String name, boolean isPartialCategoryKey) {
        this.name = name;
        this.isPartialCategoryKey = isPartialCategoryKey;
    }

    public String getName() {
        return name;
    }

    public boolean isPartialCategoryKey() {
        return isPartialCategoryKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (o.getClass() != this.getClass())) {
            return false;
        }
        Key e = (Key) o;
        return (isPartialCategoryKey == e.isPartialCategoryKey()) &&
            name.equals(e.getName());
    }

    @Override
    public int hashCode() {
        int hash = 1867;
        hash = hash * 37 + (isPartialCategoryKey ? 0 : 1);
        hash = hash * 37 + (name == null ? 0 : name.hashCode());
        return hash;
    }
}
