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

#include "com_redhat_thermostat_backend_system_internal_windows_WindowsHelperImpl.h"
#include <jni.h>
#include <unistd.h>
#include <string.h>

#if !defined(_WIN32)
# include <netdb.h>
#else // windows
# include <winsock2.h>
#endif

#ifndef NI_MAXHOST
#define NI_MAXHOST 1025
#endif /* NI_MAXHOST */

/*
 * Class:     com_redhat_thermostat_backend_system_internal_windows_WindowsHelperImpl
 * Method:    getHostName0
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_redhat_thermostat_backend_system_internal_windows_WindowsHelperImpl_getHostName0
  (JNIEnv *env, jclass winHelperClass)
{
      char hostname[NI_MAXHOST];
      memset(hostname, 0, sizeof(hostname));

      if (gethostname(hostname,  sizeof(hostname)) == 0) {
          return (*env)->NewStringUTF(env, hostname);
      }
      return NULL;
}

/*
 * Class:     com_redhat_thermostat_backend_system_internal_windows_WindowsHelperImpl
 * Method:    getUid0
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_com_redhat_thermostat_backend_system_internal_windows_WindowsHelperImpl_getUid0
  (JNIEnv *env, jclass winHelperClass, jint pid)
{
    // TODO - implement this stub
    return 987654321;
}

/*
 * Class:     com_redhat_thermostat_backend_system_internal_windows_WindowsHelperImpl
 * Method:    getUserName0
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_redhat_thermostat_backend_system_internal_windows_WindowsHelperImpl_getUserName0
  (JNIEnv *env, jclass winHelperClass, jint pid)
{
    // TODO - implement this stub
    return (*env)->NewStringUTF(env, "(stub username)");
}

/*
 * Class:     com_redhat_thermostat_backend_system_internal_windows_WindowsHelperImpl
 * Method:    getEnvironment0
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL
Java_com_redhat_thermostat_backend_system_internal_windows_WindowsHelperImpl_getEnvironment0
  (JNIEnv *env, jclass winHelperClass, jint pid)
{
    // TODO - implement this stub
    return NULL;
}
