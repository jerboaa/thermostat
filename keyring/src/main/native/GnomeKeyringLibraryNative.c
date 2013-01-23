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

#include "com_redhat_thermostat_utils_keyring_GnomeKeyringLibraryNative.h"

#include <jni.h>
#include <glib.h>
#include <gnome-keyring.h>

static void init(void) {
    if (g_get_application_name() == NULL) {
        g_set_application_name("");
    }
}

JNIEXPORT jboolean JNICALL
Java_com_redhat_thermostat_utils_keyring_GnomeKeyringLibraryNative_gnomeKeyringWrapperSetPasswordNative
  (JNIEnv *env, jclass GnomeKeyringLibraryNativeClass, jstring juserName, jstring jpassword, jstring jdescription)
{
    const char *userName = (*env)->GetStringUTFChars(env, juserName, NULL);
    if (userName == NULL) {
        return JNI_FALSE;
    }

    const char *password = (*env)->GetStringUTFChars(env, jpassword, NULL);
    if (password == NULL) {
        (*env)->ReleaseStringUTFChars(env, juserName, userName);
        return JNI_FALSE;
    }

    const char *description = (*env)->GetStringUTFChars(env, jdescription, NULL);
    if (description == NULL) {
        (*env)->ReleaseStringUTFChars(env, juserName, userName);
        (*env)->ReleaseStringUTFChars(env, jpassword, password);
        return JNI_FALSE;
    }

    init();
    GnomeKeyringResult res = gnome_keyring_store_password_sync(GNOME_KEYRING_NETWORK_PASSWORD,
                                                                GNOME_KEYRING_DEFAULT,
                                                                description,
                                                                password,
                                                                "user", userName,
                                                                "server", "gnome.org",
                                                                NULL);

    (*env)->ReleaseStringUTFChars(env, juserName, userName);
    (*env)->ReleaseStringUTFChars(env, jpassword, password);
    (*env)->ReleaseStringUTFChars(env, jdescription, description);

    return (res == GNOME_KEYRING_RESULT_OK) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_redhat_thermostat_utils_keyring_GnomeKeyringLibraryNative_gnomeKeyringWrapperGetPasswordNative
  (JNIEnv *env, jclass GnomeKeyringLibraryNative, jstring juserName)
{
    const char *userName = (*env)->GetStringUTFChars(env, juserName, NULL);
    if (userName == NULL) {
        return NULL;
    }

    gchar *password = NULL;
    GnomeKeyringResult res;

    init();
    res = gnome_keyring_find_password_sync(GNOME_KEYRING_NETWORK_PASSWORD,
                                           &password,
                                           "user", userName,
                                           "server", "gnome.org",
                                           NULL);

    (*env)->ReleaseStringUTFChars(env, juserName, userName);

    if (res == GNOME_KEYRING_RESULT_OK) {
        jstring jpassword = (*env)->NewStringUTF(env, password);
        gnome_keyring_free_password(password);
        return jpassword;

    } else {
        return NULL;
    }
}

