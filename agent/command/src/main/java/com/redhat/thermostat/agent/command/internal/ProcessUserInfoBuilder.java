/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.agent.command.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/*
 * FIXME: This class was copied from system-backend.
 * Replace when this information is available from an API.
 */
class ProcessUserInfoBuilder {
    
    private static final String PROC_STATUS_SELF_PATH = "/proc/self/status";
    private static final String PROC_STATUS_UID = "Uid:";
    private final FileReaderCreator readerCreator;
    
    ProcessUserInfoBuilder() {
        this(new FileReaderCreator());
    }
    
    ProcessUserInfoBuilder(FileReaderCreator readerCreator) {
        this.readerCreator = readerCreator;
    }
    
    private long getUid() throws IOException {
        FileReader reader = readerCreator.create(PROC_STATUS_SELF_PATH);
        long uid = getUidFromProcfs(new BufferedReader(reader));
        return uid;
    }
    
    boolean isPrivilegedUser() throws IOException {
        return (getUid() == 0);
    }

    /*
     * Look for the following line:
     * Uid:  <RealUid>   <EffectiveUid>   <SavedUid>   <FSUid>
     */
    private long getUidFromProcfs(BufferedReader br) throws IOException {
        long uid = -1;
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith(PROC_STATUS_UID)) {
                String[] parts = line.split("\\s+");
                if (parts.length == 5) {
                    try {
                        // Use Real UID
                        uid = Long.parseLong(parts[1]);
                    } catch (NumberFormatException e) {
                        throw new IOException("Unexpected output from ps command", e);
                    }
                }
                else {
                    throw new IOException("Expected 5 parts from split /proc/${pid}/status output, got " + parts.length);
                }
            }
        }
        if (uid < 0) {
            throw new IOException("Unable to determine UID from /proc/${pid}/status");
        }
        return uid;
    }

    // For testing purposes
    static class FileReaderCreator {
        FileReader create(String path) throws IOException {
            return new FileReader(new File(path));
        }
    }

}

