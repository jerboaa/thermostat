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

package com.redhat.thermostat.storage.core;

import java.io.InputStream;

import com.redhat.thermostat.annotations.Service;
import com.redhat.thermostat.storage.model.Pojo;

/**
 * A storage can be used to store, query, update and remove data.
 * Implementations may use memory, a file, some database or even a network
 * server as the backing store.
 */
@Service
public interface Storage {

    /**
     * Register the category into the Storage. A Category must be registered
     * into storage (normally handled by the Category's constructor) before it
     * can be used.
     *
     * @throws StorageException
     *             If the category can not be registered for some reason
     */
    void registerCategory(Category<?> category);
    
    /**
     * Prepares the given statement for execution.
     * 
     * @param desc
     *            The statement descriptor to prepare.
     * @return A {@link PreparedStatement} if the given statement descriptor was
     *         known and did not fail to parse.
     * @throws DescriptorParsingException
     *             If the descriptor string failed to parse.
     * @throws IllegalDescriptorException
     *             If storage refused to prepare a statement descriptor for
     *             security reasons.
     */
    <T extends Pojo> PreparedStatement<T> prepareStatement(StatementDescriptor<T> desc)
            throws DescriptorParsingException;

    /**
     * Returns the Connection object that may be used to manage connections
     * to this Storage. Subsequent calls to this method should return
     * the same Connection.
     * @return the Connection for this Storage
     */
    Connection getConnection();

    /**
     * Drop all data related to the specified agent.
     *
     * @param agentId
     *            The id of the agent
     * @throws StorageException
     *            If the purge operation fails
     */
    void purge(String agentId);

    /**
     * Save the contents of the input stream as the given name.
     *
     * @param filename
     *            the name to save this data stream as. This must be unique
     *            across all machines and processes using the storage or it will
     *            overwrite data.
     * @param data
     *            the data to save
     *
     * @throws StorageException
     *            If the save operation fails
     */
    void saveFile(String filename, InputStream data);

    /**
     * Load the file with the given name and return the data as an InputStream.
     *
     * @return the data as an {@link InputStream} or {@code null} if not found.
     *
     * @throws StorageException
     *            If the load operation fails
     */
    InputStream loadFile(String filename);

    /**
     * Shutdown the storage
     *
     * @throws StorageException
     *            If the shutdown operation fails
     */
    void shutdown();

}

