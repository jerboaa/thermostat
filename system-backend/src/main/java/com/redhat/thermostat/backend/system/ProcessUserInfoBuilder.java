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

package com.redhat.thermostat.backend.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.utils.ProcDataSource;
import com.redhat.thermostat.agent.utils.username.UserNameLookupException;
import com.redhat.thermostat.agent.utils.username.UserNameUtil;
import com.redhat.thermostat.common.utils.LoggingUtils;

class ProcessUserInfoBuilder {
    
    private static final ProcessUserInfo NON_EXISTENT_USER = new ProcessUserInfo();
    private static final String PROC_STATUS_UID = "Uid:";
    private static final Logger logger = LoggingUtils.getLogger(ProcessUserInfoBuilder.class);
    private ProcDataSource source;
    private UserNameUtil userNameUtil;
    
    ProcessUserInfoBuilder(ProcDataSource source, UserNameUtil userNameUtil) {
        this.source = source;
        this.userNameUtil = userNameUtil;
    }
    
    static class ProcessUserInfo {
        
        private long uid;
        private String username;
        
        ProcessUserInfo(long uid, String username) {
            this.uid = uid;
            this.username = username;
        }
        
        ProcessUserInfo() {
            this.uid = -1;
            this.username = null;
        }
        
        public long getUid() {
            return uid;
        }
        
        public String getUsername() {
            return username;
        }
    }
    
    ProcessUserInfo build(int pid) {
        ProcessUserInfo info = NON_EXISTENT_USER;
        try {
            Reader reader = source.getStatusReader(pid);
            long uid = getUidFromProcfs(new BufferedReader(reader));
            String name = null;
            try {
                name = userNameUtil.getUserName(uid);
            } catch (UserNameLookupException e) {
                logger.log(Level.WARNING, "Unable to retrieve username for uid: " + uid, e);
            }
            info = new ProcessUserInfo(uid, name);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to read user info for " + pid, e);
        }
        
        return info;
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


}
