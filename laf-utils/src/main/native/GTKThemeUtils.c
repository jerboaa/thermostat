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

#include "com_redhat_thermostat_internal_utils_laf_gtk_GTKThemeUtils.h"

#include <jni.h>
#include <glib.h>

#include <stdio.h>
#include <stdlib.h>

#include <gtk/gtk.h>
#include <gdk/gdk.h>
#include <X11/Xlib.h>

JNIEXPORT jboolean JNICALL
Java_com_redhat_thermostat_internal_utils_laf_gtk_GTKThemeUtils_init
    (JNIEnv *env, jclass GTKThemeUtils)
{
    int (*handler)();
    int (*io_handler)();

    // without this code we will get BadWindow
    handler = XSetErrorHandler(NULL);
    io_handler = XSetIOErrorHandler(NULL);

    gboolean result = gtk_init_check(NULL, NULL);

    XSetErrorHandler(handler);
    XSetIOErrorHandler(io_handler);

    return (result == TRUE) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_redhat_thermostat_internal_utils_laf_gtk_GTKThemeUtils_hasColor
    (JNIEnv *env, jclass GTKThemeUtils, jstring jColourID)
{
    const char *colourID = (*env)->GetStringUTFChars(env, jColourID, NULL);
    gboolean result = FALSE;

    GtkWidget *dummy = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    if (dummy == NULL) {
        goto bailString;
    }

    gtk_widget_ensure_style(dummy);

    GtkStyle *style = gtk_rc_get_style(dummy);
    if (style == NULL) {
        goto bailWidget;
    }

    GdkColor color;
    result = gtk_style_lookup_color(style, colourID, &color);

bailWidget:
    gtk_widget_destroy(dummy);

bailString:
    (*env)->ReleaseStringUTFChars(env, jColourID, colourID);

    return (result == TRUE) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_redhat_thermostat_internal_utils_laf_gtk_GTKThemeUtils_getColor
    (JNIEnv *env, jclass GTKThemeUtils, jstring jColourID)
{
    const char *colourID = (*env)->GetStringUTFChars(env, jColourID, NULL);

    GtkWidget *dummy = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    if (dummy == NULL) {
        goto bailString;
    }

    gtk_widget_ensure_style(dummy);

    GtkStyle *style = gtk_rc_get_style(dummy);
    if (style == NULL) {
        goto bailWidget;
    }

    jint pixel = 0L;
    GdkColor color;
    if (gtk_style_lookup_color(style, colourID, &color)) {
        gdk_color_parse(colourID, &color);

        // GTK uses 48 bits to represent colours, while we want a format with
        // 8 bits per channel. We just save the most significant bits rather
        // than trying to normalise the values.
        jint r = color.red >> 8;
        jint g = color.green >> 8;
        jint b = color.blue >> 8;

        pixel = (r << 16) | (g << 8) | b;

        // fprintf(stderr, "bg colour: %x%x%x\n", r, g, b);
        // fprintf(stderr, "bg colour: %x%x%x\n", color.red, color.green, color.blue);
        // fprintf(stderr, "bg colour: %u%u %u\n", color.red, color.green, color.blue);
    }

bailWidget:
    gtk_widget_destroy(dummy);

bailString:
    (*env)->ReleaseStringUTFChars(env, jColourID, colourID);

    return pixel;
}

JNIEXPORT jstring JNICALL
Java_com_redhat_thermostat_internal_utils_laf_gtk_GTKThemeUtils_getDefaultFont
    (JNIEnv *env, jclass GTKThemeUtils)
{
    jstring result = NULL;
    char* fontName = NULL;
    GtkWidget *dummy = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    if (dummy == NULL) {
        goto bailString;
    }

    gtk_widget_ensure_style(dummy);

    GtkStyle *style = gtk_rc_get_style(dummy);
    if (style != NULL) {
        PangoFontDescription *desc = style->font_desc;
	fontName = pango_font_description_to_string(desc);

        // fprintf(stderr, "default font: %s\n", fontName);
    }

    gtk_widget_destroy(dummy);

bailString:
    if (fontName == NULL) {
        result = (*env)->NewStringUTF(env, "");
    } else {
        result = (*env)->NewStringUTF(env, fontName);
        g_free(fontName);
    }

    return result;
}
