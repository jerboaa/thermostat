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

package com.redhat.thermostat.utils.username.internal;

import java.io.IOException;

import com.redhat.thermostat.agent.utils.username.UserNameLookupException;
import com.redhat.thermostat.agent.utils.username.UserNameUtil;
import com.redhat.thermostat.shared.config.NativeLibraryResolver;

public class UserNameUtilImpl implements UserNameUtil {
    
    static {
        String lib = NativeLibraryResolver.getAbsoluteLibraryPath("UserNameUtilWrapper");
        System.load(lib);
    }
    
    public String getUserName(long uid) throws UserNameLookupException {
        String username = null;
        if (uid >= 0) {
            try {
                // getUserName0 may throw IOException. We catch it here and
                // throw a more specific exception in order to avoid API 
                // leakage. This exception is then dealt with in the caller of
                // this method rather than here. This free's us from using the
                // logger in a JNI class.
                username = getUserName0(uid);
            } catch (IOException e) {
                throw new UserNameLookupException(e);
            }
        }
        return username;
    }
    
    private native String getUserName0(long uid) throws IOException;

}

