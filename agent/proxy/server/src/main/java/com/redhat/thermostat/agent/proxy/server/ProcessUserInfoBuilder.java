/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.agent.proxy.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

class ProcessUserInfoBuilder {
    
    private static final String PROC_STATUS_UID = "Uid:";
    private static final String PROC_STATUS_GID = "Gid:";
    
    private final ProcDataSource source;
    
    ProcessUserInfoBuilder(ProcDataSource source) {
        this.source = source;
    }
    
    UnixCredentials build(int pid) throws IOException {
        Reader reader = source.getStatusReader(pid);
        UnixCredentials creds = getUidGidFromProcfs(new BufferedReader(reader), pid);
        return creds;
    }

    /*
     * Look for the following lines:
     * Uid:  <RealUid>   <EffectiveUid>   <SavedUid>   <FSUid>
     * Gid:  <RealGid>   <EffectiveGid>   <SavedGid>   <FSGid>
     */
    private UnixCredentials getUidGidFromProcfs(BufferedReader br, int pid) throws IOException {
        long uid = -1;
        long gid = -1;
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith(PROC_STATUS_UID)) {
                uid = parseUidGid(line);
            }
            else if (line.startsWith(PROC_STATUS_GID)) {
                gid = parseUidGid(line);
            }
        }
        if (uid < 0) {
            throw new IOException("Unable to determine UID from /proc/${pid}/status");
        }
        if (gid < 0) {
            throw new IOException("Unable to determine GID from /proc/${pid}/status");
        }
        
        return new UnixCredentials(uid, gid, pid);
    }

    private long parseUidGid(String line) throws IOException {
        long result = -1;
        String[] parts = line.split("\\s+");
        if (parts.length == 5) {
            try {
                // Use Effective UID/GID
                result = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                throw new IOException("Unexpected output from ps command", e);
            }
        }
        else {
            throw new IOException("Expected 5 parts from split /proc/${pid}/status output, got " + parts.length);
        }
        return result;
    }
    
}

