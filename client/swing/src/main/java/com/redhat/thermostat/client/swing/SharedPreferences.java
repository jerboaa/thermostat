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

package com.redhat.thermostat.client.swing;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Provides a common location for persisted preferences to be stored.
 *
 * Preferences are namespaced by the class they belong to. If an instance of
 * Foo and an instance of Bar both store properties under the key Baz, then there
 * will be no conflict as Baz forms a different key when namespaced with Foo and Bar.
 * All instances of Foo, however, will have the same view of Baz.
 */
public class SharedPreferences {

    private static final Logger logger = LoggingUtils.getLogger(SharedPreferences.class);
    private static final ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock();

    private static final Object INIT_LOCK = new Object();
    private static Properties prefs = new Properties();
    private static File userConfig;
    private static volatile boolean initialized = false;
    private final String keyPrefix;

    public static SharedPreferences getInstance(CommonPaths commonPaths, Class<?> klazz) {
        synchronized (INIT_LOCK) {
            if (!initialized) {
                userConfig = commonPaths.getUserSharedPreferencesFile();
                loadPrefs(prefs);
                initialized = true;
            }
        }
        return new SharedPreferences(klazz);
    }

    //testing hook only
    static SharedPreferences getInstance(Properties properties, String keyPrefix) {
        return getInstance(properties, null, keyPrefix);
    }

    // testing hook only
    static SharedPreferences getInstance(Properties properties, File file, String keyPrefix) {
        prefs = properties;
        userConfig = file;
        return new SharedPreferences(keyPrefix);
    }

    private static void loadPrefs(Properties props) {
        try {
            if (userConfig.isFile()) {
                try {
                    try (InputStream fis = new FileInputStream(userConfig)) {
                        props.load(fis);
                    }
                } catch (IOException e) {
                    logger.log(Level.CONFIG, "unable to load shared preferences", e);
                }
            }
        } catch (InvalidConfigurationException e) {
            logger.log(Level.CONFIG, "unable to load shared preferences", e);
        }
    }

    private SharedPreferences(Class<?> klazz) {
        this(klazz.getCanonicalName());
    }

    private SharedPreferences(String keyPrefix) {
        this.keyPrefix = keyPrefix + ":";
    }

    public boolean containsKey(String key) {
        READ_WRITE_LOCK.readLock().lock();
        try {
            return validateKey(key).checkValidity()
                    && prefs.containsKey(createKey(key));
        } finally {
            READ_WRITE_LOCK.readLock().unlock();
        }
    }

    public String getString(String key) {
        READ_WRITE_LOCK.readLock().lock();
        try {
            ValidationResult validationResult = validateKey(key);
            if (!validationResult.checkValidity()) {
                throw new IllegalArgumentException(validationResult.getReason());
            }
            return prefs.getProperty(createKey(key));
        } finally {
            READ_WRITE_LOCK.readLock().unlock();
        }
    }

    public String getString(String key, String defaultValue) {
        READ_WRITE_LOCK.readLock().lock();
        try {
            if (!validateKey(key).checkValidity()) {
                return defaultValue;
            }
            return prefs.getProperty(createKey(key), defaultValue);
        } finally {
            READ_WRITE_LOCK.readLock().unlock();
        }
    }

    public int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    public int getInt(String key, int defaultValue) {
        return Integer.parseInt(getString(key, Integer.toString(defaultValue)));
    }

    public double getDouble(String key) {
        return Double.parseDouble(getString(key));
    }

    public double getDouble(String key, double defaultValue) {
        return Double.parseDouble(getString(key, Double.toString(defaultValue)));
    }

    public long getLong(String key) {
        return Long.parseLong(getString(key));
    }

    public long getLong(String key, long defaultValue) {
        return Long.parseLong(getString(key, Long.toString(defaultValue)));
    }

    public float getFloat(String key) {
        return Float.parseFloat(getString(key));
    }

    public float getFloat(String key, float defaultValue) {
        return Float.parseFloat(getString(key, Float.toString(defaultValue)));
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getString(key, Boolean.toString(defaultValue)));
    }

    public Editor edit() {
        return new EditorImpl();
    }

    void set(String key, String value) {
        READ_WRITE_LOCK.writeLock().lock();
        try {
            if (!validateKey(key).checkValidity()) {
                return;
            }
            prefs.setProperty(createKey(key), value);
        } finally {
            READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    void set(String key, int value) {
        READ_WRITE_LOCK.writeLock().lock();
        try {
            if (!validateKey(key).checkValidity()) {
                return;
            }
            prefs.setProperty(createKey(key), Integer.toString(value));
        } finally {
            READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    void set(String key, double value) {
        READ_WRITE_LOCK.writeLock().lock();
        try {
            if (!validateKey(key).checkValidity()) {
                return;
            }
            prefs.setProperty(createKey(key), Double.toString(value));
        } finally {
            READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    void set(String key, long value) {
        READ_WRITE_LOCK.writeLock().lock();
        try {
            if (!validateKey(key).checkValidity()) {
                return;
            }
            prefs.setProperty(createKey(key), Long.toString(value));
        } finally {
            READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    void set(String key, float value) {
        READ_WRITE_LOCK.writeLock().lock();
        try {
            if (!validateKey(key).checkValidity()) {
                return;
            }
            prefs.setProperty(createKey(key), Float.toString(value));
        } finally {
            READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    void set(String key, boolean value) {
        READ_WRITE_LOCK.writeLock().lock();
        try {
            if (!validateKey(key).checkValidity()) {
                return;
            }
            prefs.setProperty(createKey(key), Boolean.toString(value));
        } finally {
            READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    void remove(String key) {
        READ_WRITE_LOCK.writeLock().lock();
        try {
            if (!validateKey(key).checkValidity()) {
                return;
            }
            prefs.remove(createKey(key));
        } finally {
            READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    void clear() {
        READ_WRITE_LOCK.writeLock().lock();
        try {
            for (String prop : prefs.stringPropertyNames()) {
                if (prop.startsWith(keyPrefix)) {
                    prefs.remove(prop);
                }
            }
        } finally {
            READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    String createKey(String key) {
        return keyPrefix + key;
    }

    static ValidationResult validateKey(String key) {
        if (key == null) {
            return new ValidationResult(false, "Shared preference keys may not be null");
        }
        final Pattern pattern = Pattern.compile("^([0-9a-zA-Z\\-_]+)$");
        boolean valid = pattern.matcher(key).matches();
        ValidationResult result;
        if (valid) {
            result = new ValidationResult(true, null);
        } else {
            result = new ValidationResult(false, "Menu state key \"" + key + "\" is invalid and will be ignored." +
                    " Keys may contain Latin alphabet characters in upper or lower case, numerals, hyphens, and" +
                    " underscores only.");
        }
        return result;
    }

    private static void flush() throws IOException {
        READ_WRITE_LOCK.readLock().lock();
        try {
            prefs.store(new BufferedWriter(new FileWriter(userConfig)), null);
        } finally {
            READ_WRITE_LOCK.readLock().unlock();
        }
    }

    /**
     * Implementations of this interface provide a way for clients to update the contents of the "parent"
     * SharedPreferences instance. Clients should be sure to call {@link #commit()} when they wish for their
     * batched <code>set()</code> calls to be made effective.
     */
    public interface Editor {
        void set(String key, String value);
        void set(String key, int value);
        void set(String key, double value);
        void set(String key, long value);
        void set(String key, float value);
        void set(String key, boolean value);
        void remove(String key);
        void clear();
        void commit() throws IOException;
    }

    private class EditorImpl implements Editor {

        private final List<Edit> editBuffer = new ArrayList<>();

        /**
         * Set a <code>String</code> preference.
         */
        public void set(final String key, final String value) {
            editBuffer.add(new Edit() {
                @Override
                public void apply() {
                    SharedPreferences.this.set(key, value);
                }
            });
        }

        /**
         * Set an <code>int</code> preference.
         */
        public void set(final String key, final int value) {
            editBuffer.add(new Edit() {
                @Override
                public void apply() {
                    SharedPreferences.this.set(key, value);
                }
            });
        }

        /**
         * Set a <code>double</code> preference.
         */
        public void set(final String key, final double value) {
            editBuffer.add(new Edit() {
                @Override
                public void apply() {
                    SharedPreferences.this.set(key, value);
                }
            });
        }

        /**
         * Set a <code>long</code> preference.
         */
        public void set(final String key, final long value) {
            editBuffer.add(new Edit() {
                @Override
                public void apply() {
                    SharedPreferences.this.set(key, value);
                }
            });
        }

        /**
         * Set a <code>float</code> preference.
         */
        public void set(final String key, final float value) {
            editBuffer.add(new Edit() {
                @Override
                public void apply() {
                    SharedPreferences.this.set(key, value);
                }
            });
        }

        /**
         * Set a <code>boolean</code> preference.
         */
        public void set(final String key, final boolean value) {
            editBuffer.add(new Edit() {
                @Override
                public void apply() {
                    SharedPreferences.this.set(key, value);
                }
            });
        }

        /**
         * Remove the specified key from the parent SharedPreference class' namespace.
         */
        public void remove(final String key) {
            editBuffer.add(new Edit() {
                @Override
                public void apply() {
                    SharedPreferences.this.remove(key);
                }
            });
        }

        /**
         * Remove all preferences belonging to the parent SharedPreference class' namespace.
         */
        public void clear() {
            editBuffer.add(new Edit() {
                @Override
                public void apply() {
                    SharedPreferences.this.clear();
                }
            });
        }

        /**
         * Commit the changes so far buffered by this Editor.
         *
         * This atomically commits the changes both in-memory and on-disk.
         * @throws IOException
         */
        public void commit() throws IOException {
            READ_WRITE_LOCK.writeLock().lock();
            try {
                apply();
                flush();
            } finally {
                READ_WRITE_LOCK.writeLock().unlock();
            }
        }

        private void apply() {
            Iterator<Edit> it = editBuffer.iterator();
            while (it.hasNext()) {
                it.next().apply();
                it.remove();
            }
        }

    }

    private interface Edit {
        void apply();
    }

    static class ValidationResult {
        private final boolean valid;
        private final String reason;

        public ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }

        public boolean checkValidity() {
            if (!isValid()) {
                logger.log(Level.WARNING, getReason());
            }
            return isValid();
        }
    }

}
