/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.utils.keyring;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeyringProvider {

    // Does not depend on common-core. Use java.util.logging.Logger
    private static final Logger logger = Logger.getLogger(KeyringProvider.class.getName());
    
    public static final String DEFAULT_KEYRING = "com.redhat.thermostat.utils.keyring.GnomeKeyringLibraryNative";
    public static final String MEMORY_KEYRING = "com.redhat.thermostat.utils.keyring.MemoryKeyring";
    
    public static final String KEYRING_FACTORY_PROPERTY = "com.redhat.thermostat.utils.keyring.provider";
   
    private KeyringProvider() { /* nothing to do*/ }
    
    private static final Keyring keyring = getDefaultKeyring();
    
    public static Keyring getKeyring() {
        return keyring;
    }

    static Keyring getDefaultKeyring() {
        
        Keyring keyring = null;
        String keyringName =
                AccessController.doPrivileged(new GetUserDefaultKeyringClassName());
        if (keyringName != null) {
            // try to load it first
            keyring = AccessController.doPrivileged(new CreateKeying(keyringName));
        }
        
        if (keyring == null) {
            throw new InternalError("can't instantiate keyring from class: " + keyringName);
        }
        
        return keyring;
    }
    
    private static class CreateKeying implements PrivilegedAction<Keyring> {
        private String className;
        CreateKeying(String className) {
            this.className = className;
        }
        
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public Keyring run() {
           try {
              Class keyringClass = Class.forName(className, false, getClass().getClassLoader());
              Constructor constructor = keyringClass.getDeclaredConstructor();
              constructor.setAccessible(true);
              Keyring keyring = (Keyring) constructor.newInstance();
              return keyring;
              
           } catch (InstantiationException | IllegalAccessException | ClassNotFoundException |
                    IllegalArgumentException | InvocationTargetException |
                    NoSuchMethodException | SecurityException e) {
               logger.log(Level.SEVERE, "can't create keyring for class: " + className, e);
           }
           return null;
        }
    }
    
    private static class GetUserDefaultKeyringClassName implements PrivilegedAction<String> {
        @Override
        public String run() {
            return System.getProperty(KEYRING_FACTORY_PROPERTY, DEFAULT_KEYRING);
        }
    }
}

