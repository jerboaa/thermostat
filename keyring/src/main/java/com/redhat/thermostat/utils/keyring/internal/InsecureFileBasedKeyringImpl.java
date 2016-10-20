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

package com.redhat.thermostat.utils.keyring.internal;

import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.utils.keyring.Keyring;
import com.redhat.thermostat.utils.keyring.KeyringException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

// A simple class to create a stub keyring; expect it to be replaced to use native keyrings in the future
// The code tries not to keep passwords in memory longer than it has to

// I/O buffers are the weak spot here - they'll contain file contents.
// Currently the keyring file is unencrypted.

class InsecureFileBasedKeyringImpl implements Keyring {

    private static final String DEFAULT_KEYRING_FILENAME = "keyring.bin";
    private static final Logger logger = Logger.getLogger(InsecureFileBasedKeyringImpl.class.getName());

    private final File krFile;

    File getKeyringFile() throws InvalidConfigurationException {
            return krFile;
    }

    InsecureFileBasedKeyringImpl(final CommonPaths cp) {
        final String keyRingFn = cp.getUserConfigurationDirectory() + File.separator + DEFAULT_KEYRING_FILENAME;
        krFile = new File(keyRingFn);
        init();
    }

    InsecureFileBasedKeyringImpl( final String fn ) {
        this.krFile = new File(fn);
        init();
    }

    private void init() {
        logger.warning("InsecureFileBasedKeyringImpl will store passwords in plaintext");
        final File krf = getKeyringFile();
        logger.info("Using '"+krf+"' as keyring file");
        if (krf.isDirectory()) {
            logger.severe("'"+krf+"' is an existing directory");
            throw new InvalidConfigurationException("'"+krf+"' is an existing directory");
        }
        else if (!krf.isFile()) {
            createKeyring(krf);
        }
    }

    private void createKeyring( final File krf ) {
        final Map<String,Map<String,char[]>> emptyMap = new HashMap<>();
        persist(krf,emptyMap);
    }

    @Override
    public synchronized void savePassword(String url, String username, char[] password) {
        final Map<String,Map<String,char[]>> pwMap = depersist();
        if (!pwMap.containsKey(url)) {
            Map<String, char[]> upMap = new HashMap<>();
            pwMap.put(url, upMap);
            upMap.put(username, password.clone());
        }
        else {
            Map<String, char[]> upMap = pwMap.get(url);
            upMap.put(username, password.clone());
        }
        persist(pwMap);
    }

    @Override
    public synchronized char[] getPassword(String url, String username) {
        final Map<String,Map<String,char[]>> pwMap = depersist();
        Map<String, char[]> upMap = pwMap.get(url);
        final char[] oldpwd = upMap == null ? null : upMap.get(username);
        final char[] pwd = oldpwd == null ? null : oldpwd.clone();
        persist(pwMap);
        return pwd;
    }

    @Override
    public synchronized void clearPassword(String url, String username) {
        final Map<String,Map<String,char[]>> pwMap = depersist();
        Map<String, char[]> upMap = pwMap.get(url);
        if (upMap != null) {
            char[] pwd = upMap.get(username);
            if (pwd != null) {
                Arrays.fill(pwd, '\0');
                upMap.remove(username);
                persist(pwMap);
            }
            else {
                empty(pwMap);
            }
        }
        else {
            empty(pwMap);
        }
    }

    protected void empty(final Map<String,Map<String,char[]>> pwMap) {
        for (final Map.Entry<String, Map<String, char[]>> entry : pwMap.entrySet()) {
            for (final Map.Entry<String, char[]> up : entry.getValue().entrySet()) {
                Arrays.fill(up.getValue(), '\0');
            }
            entry.getValue().clear();
        }
        pwMap.clear();
    }

    private void persist( final File krf, final Map<String,Map<String,char[]>> map ) {
        try ( FileOutputStream fileOut = new FileOutputStream(krf);
              ObjectOutputStream out = new ObjectOutputStream(fileOut);){
            krf.getParentFile().mkdirs();
            out.writeObject(map);
            out.close();
            fileOut.close();
            logger.finest("persisted to " + krf + ":\n" + dump(map));
        } catch( IOException e ) {
            e.printStackTrace();
        }
    }

    private void depersist( final File krf, final Map<String,Map<String,char[]>> map ) {
        try ( FileInputStream fileIn = new FileInputStream(krf);
            ObjectInputStream in = new ObjectInputStream(fileIn);) {
            final Map<String,Map<String,char[]>> newmap = (Map<String,Map<String,char[]>>) in.readObject();
            map.clear();
            map.putAll(newmap);
            in.close();
            fileIn.close();
            logger.finest("loaded from " + krf + ":\n" + dump(map));
        } catch(IOException i) {
            i.printStackTrace();
        } catch(ClassNotFoundException c) {
            throw new KeyringException("Map class not found.  Not sure how that could happen...");
        }
    }

    protected void persist(final Map<String,Map<String,char[]>> pwMap) {
        persist(getKeyringFile(), pwMap);
        empty(pwMap);
    }

    private Map<String,Map<String,char[]>> depersist() {
        final Map<String,Map<String,char[]>> pwMap = new HashMap<>();
        depersist(getKeyringFile(), pwMap);
        return pwMap;
    }

    private static String dump(final Map<String,Map<String,char[]>> pwMap) {
        final StringBuilder bb = new StringBuilder();
        for ( final Map.Entry<String,Map<String, char[]> > entry : pwMap.entrySet()) {
            bb.append("url: ").append(entry.getKey()).append('\n');
            for ( final Map.Entry<String, char[]> up : entry.getValue().entrySet()) {
                bb.append("     u=").append(up.getKey()).append(" p=").append(up.getValue()).append('\n');
            }
        }
        return bb.toString();
    }
}
