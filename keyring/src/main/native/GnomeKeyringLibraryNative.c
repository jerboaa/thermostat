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

#include "com_redhat_thermostat_utils_keyring_impl_KeyringImpl.h"

#include <jni.h>
#include <glib.h>
#include <gnome-keyring.h>
#include <stdlib.h>
#include <string.h>

GnomeKeyringPasswordSchema thermostat_schema = {
    GNOME_KEYRING_ITEM_GENERIC_SECRET,
    {
        { "username", GNOME_KEYRING_ATTRIBUTE_TYPE_STRING },
        { "url", GNOME_KEYRING_ATTRIBUTE_TYPE_STRING },
        { NULL, 0 }
    }
};

static void init(void) {
    if (g_get_application_name() == NULL) {
        g_set_application_name("Thermostat");
    }
}

JNIEXPORT jboolean JNICALL
Java_com_redhat_thermostat_utils_keyring_impl_KeyringImpl_gnomeKeyringWrapperSavePasswordNative
  (JNIEnv *env, jclass GnomeKeyringLibraryNativeClass, jstring jurl, jstring juserName, jbyteArray jpassword, jstring jdescription)
{
    int passIndex;
    const char *url = (*env)->GetStringUTFChars(env, jurl, NULL);
    if (url == NULL) {
        return JNI_FALSE;
    }
    const char *userName = (*env)->GetStringUTFChars(env, juserName, NULL);
    if (userName == NULL) {
        (*env)->ReleaseStringUTFChars(env, jurl, url);
        return JNI_FALSE;
    }

    jsize passwordLength = (*env)->GetArrayLength(env, jpassword);
    jbyte *password = (*env)->GetByteArrayElements(env, jpassword, NULL);
    if (password == NULL) {
        (*env)->ReleaseStringUTFChars(env, jurl, url);
        (*env)->ReleaseStringUTFChars(env, juserName, userName);
        return JNI_FALSE;
    }

    /* Make into null terminated (g)char * to make gnome api happy */
    gchar *gpassword = malloc(sizeof(gchar) * (passwordLength + 1));
    if (gpassword == NULL) {
        (*env)->ReleaseStringUTFChars(env, jurl, url);
        (*env)->ReleaseStringUTFChars(env, juserName, userName);
        for (passIndex = 0; passIndex < (int) passwordLength; passIndex++) {
            password[passIndex] = '\0';
        }
        (*env)->ReleaseByteArrayElements(env, jpassword, password, JNI_ABORT);
        return JNI_FALSE;
    }
    for (passIndex = 0; passIndex < passwordLength; passIndex++) {
        gpassword[passIndex] = (gchar) (password[passIndex]);
    }
    gpassword[passwordLength] = (gchar) '\0';

    /* Overwrite original array, release back to java-land. */
    for (passIndex = 0; passIndex < (int) passwordLength; passIndex++) {
        password[passIndex] = '\0';
    }
    (*env)->ReleaseByteArrayElements(env, jpassword, password, JNI_ABORT);

    const char *description = (*env)->GetStringUTFChars(env, jdescription, NULL);
    if (description == NULL) {
    	(*env)->ReleaseStringUTFChars(env, jurl, url);
        (*env)->ReleaseStringUTFChars(env, juserName, userName);
        for (passIndex = 0; passIndex < (int) passwordLength; passIndex++) {
            gpassword[passIndex] = (gchar) '\0';
        }
        free(gpassword);
        return JNI_FALSE;
    }

    init();
    GnomeKeyringResult res = gnome_keyring_store_password_sync(&thermostat_schema,
                                                                GNOME_KEYRING_DEFAULT,
                                                                description,
                                                                gpassword,
                                                                "username", userName,
                                                                "url", url,
                                                                NULL);
    (*env)->ReleaseStringUTFChars(env, jurl, url);
    (*env)->ReleaseStringUTFChars(env, juserName, userName);
    for (passIndex = 0; passIndex < (int) passwordLength; passIndex++) {
        gpassword[passIndex] = '\0';
    }
    free(gpassword);
    (*env)->ReleaseStringUTFChars(env, jdescription, description);

    return (res == GNOME_KEYRING_RESULT_OK) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL
Java_com_redhat_thermostat_utils_keyring_impl_KeyringImpl_gnomeKeyringWrapperGetPasswordNative
  (JNIEnv *env, jclass GnomeKeyringLibraryNative, jstring jurl, jstring juserName)
{
	const char *url = (*env)->GetStringUTFChars(env, jurl, NULL);
    if (url == NULL) {

        return NULL;
    }

    const char *userName = (*env)->GetStringUTFChars(env, juserName, NULL);
    if (userName == NULL) {
    	(*env)->ReleaseStringUTFChars(env, jurl, url);
        return NULL;
    }

    gchar *password = NULL;
    GnomeKeyringResult res;

    init();
    res = gnome_keyring_find_password_sync(&thermostat_schema,
                                           &password,
                                           "username", userName,
                                           "url", url,
                                           NULL);

    (*env)->ReleaseStringUTFChars(env, jurl, url);
    (*env)->ReleaseStringUTFChars(env, juserName, userName);

    if (res == GNOME_KEYRING_RESULT_OK) {
        const jbyte *jbytePassword = (const jbyte *) password;

        jsize passwordLength = strlen(password);
        jbyteArray jpassword = (*env)->NewByteArray(env, passwordLength);
        (*env)->SetByteArrayRegion(env, jpassword, 0, passwordLength, jbytePassword);
        gnome_keyring_free_password(password);
        return jpassword;
    } else {
        return NULL;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_redhat_thermostat_utils_keyring_impl_KeyringImpl_gnomeKeyringWrapperClearPasswordNative
  (JNIEnv *env, jclass GnomeKeyringLibraryNative, jstring jurl, jstring juserName)
{
    const char *url = (*env)->GetStringUTFChars(env, jurl, NULL);
    if (url == NULL) {
        return JNI_FALSE;
    }
    const char *userName = (*env)->GetStringUTFChars(env, juserName, NULL);
    if (userName == NULL) {
        (*env)->ReleaseStringUTFChars(env, jurl, url);
        return JNI_FALSE;
    }

    init();
    GnomeKeyringResult res = gnome_keyring_delete_password_sync(&thermostat_schema,
                                                                "username", userName,
                                                                "url", url,
                                                                NULL);

    (*env)->ReleaseStringUTFChars(env, jurl, url);
    (*env)->ReleaseStringUTFChars(env, juserName, userName);

    return (res == GNOME_KEYRING_RESULT_OK) ? JNI_TRUE : JNI_FALSE;
}

