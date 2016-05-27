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

package com.redhat.thermostat.common.cli;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A simple structure used for tab-completion candidates.
 */
public class CompletionInfo {

    private String actualCompletion;
    private String userVisibleText;

    /**
     * Cosntruct a CompletionInfo with a tab completion candidate as well as a descriptive label
     * to distinguish the candidate.
     * @param actualCompletion the text which may be tab completed
     * @param userVisibleText a label displayed beside the completion text
     */
    public CompletionInfo(String actualCompletion, String userVisibleText) {
        this.actualCompletion = requireNonNull(actualCompletion);
        this.userVisibleText = userVisibleText;
    }

    /**
     * Construct a CompletionInfo with a tab completion candidate, and no additional label to
     * distinguish the candidate.
     * @param actualCompletion the text which may be tab completed
     */
    public CompletionInfo(String actualCompletion) {
        this(actualCompletion, null);
    }

    /**
     * Provides the String completion when there is only one tab-completion match.
     * @return the completed String
     */
    public String getActualCompletion() {
        return actualCompletion;
    }

    /**
     * Provides a human-friendly identifier text describing {@link #getActualCompletion()}.
     * @return the identifier text
     */
    public String getUserVisibleText() {
        return userVisibleText;
    }

    /**
     * Provides the String displayed to the user when tab-completion returns more than one possible result.
     * This will contain the completion candidate ({@link #getActualCompletion()}), as well as the descriptive
     * label ({@link #getUserVisibleText()}), if available.
     * @return the display String
     */
    public String getCompletionWithUserVisibleText() {
        if (userVisibleText == null) {
            return actualCompletion;
        } else {
            return actualCompletion + " [" + userVisibleText + "]";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CompletionInfo that = (CompletionInfo) o;
        return Objects.equals(this.actualCompletion, that.actualCompletion)
                && Objects.equals(this.userVisibleText, that.userVisibleText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actualCompletion, userVisibleText);
    }
}
