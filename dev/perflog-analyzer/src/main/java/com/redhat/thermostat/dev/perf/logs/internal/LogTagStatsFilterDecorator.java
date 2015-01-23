/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.dev.perf.logs.internal;

import java.util.Objects;

/**
 * A decorator-based implementation of a log-tag filter.
 *
 * @param <S> The {@link LineStat} implementation type
 * @param <T> The {@link LineStats} implementation type
 * 
 * @see LineStatsFilter
 * @see QueueStatsFilter
 * @see StatementStatsFilter
 */
class LogTagStatsFilterDecorator<S extends LineStat, T extends LineStats<S>> implements LineStatsFilter<S, T> {

    private final LogTag logTag;
    private final LineStatsFilter<S, T> decoratee;
    
    LogTagStatsFilterDecorator(LineStatsFilter<S, T> decoratee, LogTag logTag) {
        this.logTag = logTag;
        this.decoratee = Objects.requireNonNull(decoratee);
    }
    
    @Override
    public boolean matches(LineStat stat) {
        if (!decoratee.matches(stat)) {
            return false;
        }
        return logTag == stat.getLogTag();
    }

    @Override
    public String getBucketName() {
        return decoratee.getBucketName() + "-" + logTag.toString();
    }

    @Override
    public Class<T> getStatsClass() {
        return decoratee.getStatsClass();
    }

}
