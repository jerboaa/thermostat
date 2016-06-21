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

package com.redhat.thermostat.setup.command.internal;

import com.redhat.thermostat.shared.locale.Translate;


public enum LocaleResources {

    WINDOW_TITLE,
    WELCOME_SCREEN_TITLE,
    MONGO_SETUP_TITLE,
    USERS_SETUP_TITLE,
    SETUP_COMPLETE_TITLE,
    MONGO_CRED_TITLE,
    CLIENT_CRED_TITLE,
    AGENT_CRED_TITLE,
    NEXT,
    BACK,
    CANCEL,
    FINISH,
    SHOW_MORE,
    SHOW_LESS,
    STEP_X_OF_Y,
    AGENT_HELP_INFO,
    CLIENT_HELP_INFO,
    STORAGE_HELP_INFO,
    PASSWORD_MISMATCH,
    DETAILS_MISSING,
    USERNAMES_IDENTICAL,
    SHOW_PASSWORDS,
    USE_DEFAULTS,
    THERMOSTAT_BRIEF,
    THERMOSTAT_BLURB,
    STORAGE_FAILED,
    STORAGE_RUNNING,
    SERVICE_UNAVAILABLE_MESSAGE,
    SETUP_FAILED,
    SETUP_INTERRUPTED,
    SETUP_CANCELLED,
    USERNAME,
    PASSWORD,
    VERIFY_PASSWORD,
    QUICK_SETUP,
    CUSTOM_SETUP,
    QUICK_SETUP_INFO,
    CUSTOM_SETUP_INFO,
    USER_PREFIX,
    MONGO_USER_PREFIX,
    CLIENT_USER_PREFIX,
    AGENT_USER_PREFIX,
    CLI_SETUP_INTRO,
    CLI_SETUP_PROCEED_QUESTION,
    CLI_SETUP_UNKNOWN_RESPONSE,
    CLI_SETUP_PROCEED_WORD,
    CLI_SETUP_CANCEL_WORD,
    CLI_SETUP_YES,
    CLI_SETUP_NO,
    CLI_SETUP_PASSWORD_INVALID,
    CLI_SETUP_PASSWORD_MISMATCH,
    CLI_SETUP_USERNAME_INVALID,
    CLI_SETUP_USERNAMES_IDENTICAL,
    CLI_SETUP_MONGODB_USER_CREDS_INTRO,
    CLI_SETUP_MONGODB_USERNAME_PROMPT,
    CLI_SETUP_PASSWORD_PROMPT,
    CLI_SETUP_USERNAME_REPEAT,
    CLI_SETUP_PASSWORD_REPEAT_PROMPT,
    CLI_SETUP_THERMOSTAT_USER_CREDS_INTRO,
    CLI_SETUP_THERMOSTAT_CLIENT_USERNAME_PROMPT, 
    CLI_SETUP_THERMOSTAT_AGENT_USERNAME_PROMPT,
    CLI_SETUP_FINISH_SUCCESS,
    SETUP_FAILED_DIALOG_TITLE,
    SETUP_FAILED_DIALOG_MESSAGE,
    SHOW_MORE_ERROR_INFO,
    SHOW_LESS_ERROR_INFO,
    STEPS_TO_RESOLVE_ERROR_LABEL_TEXT,
    SETUP_COMPLETE_NOTE_CREDENTIALS_TEXT,
    THERMOSTAT_ALREADY_CONFIGURED_TITLE,
    THERMOSTAT_ALREADY_CONFIGURED_MESSAGE,
    ;

    static final String RESOURCE_BUNDLE =
            "com.redhat.thermostat.setup.locale.strings";

    public static Translate<LocaleResources> createLocalizer() {
        return new Translate<>(RESOURCE_BUNDLE, LocaleResources.class);
    }

}

