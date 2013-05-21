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

#include "com_redhat_thermostat_utils_username_internal_UserNameUtilImpl.h"

#include <jni.h>
#include <pwd.h>
#include <string.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>

static jint throw_IOException(JNIEnv *env, const char *message) {
    const char *class_name = "java/io/IOException";
    jclass class = (*env)->FindClass(env, class_name);
    if (class == NULL) {
        return -1;
    }
    return (*env)->ThrowNew(env, class, message);
}

JNIEXPORT jstring JNICALL
Java_com_redhat_thermostat_utils_username_internal_UserNameUtilImpl_getUserName0
    (JNIEnv *env, jclass ProcessUserInfoBuilderClass, jlong uid) {
    size_t bufsize = sysconf(_SC_GETPW_R_SIZE_MAX);
    if (bufsize < 0) {
        throw_IOException(env, "Unable to retrieve recommended buffer size from sysconf");
        return NULL;
    }

    char *buf = malloc(bufsize * sizeof(char));
    if (!buf) {
        throw_IOException(env, "Unable to allocate buffer for username");
        return NULL;
    }

    struct passwd pwd;
    struct passwd *ret;
    int rc = getpwuid_r(uid, &pwd, buf, bufsize, &ret);
    if (rc) {
        // Error occurred
        const char *error_message = strerror(rc);
        throw_IOException(env, error_message);
        free(buf);
        return NULL;
    }
    if (!ret) {
        // No username found
        char err_buf[128]; // Large enough for even the largest UIDs
        snprintf(err_buf, sizeof(err_buf), "Username not found for uid: %ld", uid);
        throw_IOException(env, err_buf);
        free(buf);
        return NULL;
    }

    jstring name = (*env)->NewStringUTF(env, pwd.pw_name);
    free(buf);
    return name;
}
