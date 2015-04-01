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

package com.redhat.thermostat.storage.core;

import java.util.NoSuchElementException;

import com.redhat.thermostat.storage.model.Pojo;

/**
 * Allows traversing over objects obtained from {@link Storage}.
 * 
 * @see PreparedStatement#executeQuery()
 */
public interface Cursor<T extends Pojo> {

    public static final int DEFAULT_BATCH_SIZE = 100;

    /**
     * <p>
     * Sets the configured batch size when retrieving more elements from the
     * database. That is, no more elements will be loaded into memory than the
     * configured batch size. Note that the new batch size will only take effect
     * once the current batch is exhausted.
     * </p>
     * <p>
     * The default batch size is 100.
     * </p>
     * 
     * @param n
     *            The number of results to fetch from storage in a single batch.
     * @return A cursor with the configured batch size.
     * @throws IllegalArgumentException
     *             If {@code n} is < 1
     */
    void setBatchSize(int n) throws IllegalArgumentException;

    /**
     * 
     * @return The configured batch size set via {@link setBatchSize} or
     *         {@link BatchCursor#DEFAULT_BATCH_SIZE} if it was never set
     *         explicitly.
     */
    int getBatchSize();
    
    /**
     * @return {@code true} if there are more elements, {@code false} otherwise.
     * 
     * @throws StorageException
     *             If there was a problem with underlying {@link Storage}.
     */
    boolean hasNext();

    /**
     * Retrieves the next element from the result set. Users are advised to call
     * {@link #hasNext()} prior to calling this method.
     * 
     * @return <p>
     *         The next element of the result set. {@code null} if there is no
     *         next element.
     *         </p>
     *         <p>
     *         <strong>Please note: </strong> This will change with the next
     *         release. In the next major release a
     *         {@link NoSuchElementException} will be thrown if there is no next
     *         element. I.e. {@link #hasNext()} returns {@code false}, but
     *         {@code next()} is still being called.
     *         </p>
     * 
     * @throws StorageException
     *             If there was a problem with underlying {@link Storage}.
     */
    T next();

}

