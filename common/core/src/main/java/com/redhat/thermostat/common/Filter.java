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

package com.redhat.thermostat.common;

import com.redhat.thermostat.annotations.ExtensionPoint;

/**
 * A {@link Filter} decides if some information matches what this
 * filter is designed to work with. The exact meaning of the
 * word "match" depends on the context this filter is applied.
 * 
 * <br /><br />
 * 
 * For example, a {@link String} filter that match for all the input containing
 * the word "test" would return {@code true} for both "a test" or "testing",
 * but {@code false} for the mispelled word "tesst". This example filter could
 * then be used to implement a search function, by testing various strings
 * and only showing the ones that match this filter.
 * 
 * <br /><br />
 * 
 * Filters may change their behavior due to external events in a certain
 * context. In such cases, the filter managers specific to those context should
 * register as {@link FilterEvent} listeners and the {@link Filter}
 * implementation should notify of {@link FilterEvent#FILTER_CHANGED} events.
 * 
 * <br /><br />
 * 
 * As an example let's take again our {@link String} filter. If such filter
 * was used in a search context, the filter could react to user input and
 * change the string to be used as matcher. At each matcher change, it should
 * notify its listeners that such change occurred in order to allow correct
 * re-processing of the filter.
 */
@ExtensionPoint
public abstract class Filter<T> {

    public enum FilterEvent {
        FILTER_CHANGED,
    }
    
    private final ActionNotifier<FilterEvent> notifier;
    public Filter() {
        notifier = new ActionNotifier<>(this);
    }
    
    /**
     * Return {@code true} if this filter match the given input, {@code false}
     * otherwise. 
     */
    public abstract boolean matches(T toMatch);

    public void addFilterEventListener(ActionListener<FilterEvent> listener) {
        notifier.addActionListener(listener);
    }
    
    public void removeFilterEventListener(ActionListener<FilterEvent> listener) {
        notifier.removeActionListener(listener);
    }
    
    /**
     * Notify all the listeners that a change occurred in this filter.
     */
    protected void notify(FilterEvent action) {
        notifier.fireAction(action);
    }    
}

