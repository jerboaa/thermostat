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


package com.redhat.thermostat.web.client.internal;

import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.BasicBatchCursor;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.web.common.PreparedStatementResponseCode;
import com.redhat.thermostat.web.common.WebPreparedStatement;
import com.redhat.thermostat.web.common.WebQueryResponse;

import java.util.NoSuchElementException;

class WebCursor<T extends Pojo> extends BasicBatchCursor<T> {
    
    private static final Logger logger = LoggingUtils.getLogger(WebCursor.class);

    private final Type parametrizedTypeToken;
    private final WebStorage storage;
    private final int cursorId;
    private final WebPreparedStatement<T> stmt;
    private int batchIndex;
    private T[] dataBatch;
    private boolean hasMoreBatches;

    // Main constructor called from doQueryExecute()
    WebCursor(WebStorage storage, T[] dataBatch, boolean hasMoreBatches, int cursorId, Type parametrizedTypeToken, WebPreparedStatement<T> stmt) {
        this.storage = storage;
        this.cursorId = cursorId;
        this.parametrizedTypeToken = parametrizedTypeToken;
        this.stmt = stmt;
        this.hasMoreBatches = hasMoreBatches;
        this.dataBatch = dataBatch;
        this.batchIndex = 0;
    }

    @Override
    public boolean hasNext() {
        return batchIndex < dataBatch.length || hasMoreBatches;
    }

    @Override
    public T next() {
        if (batchIndex >= dataBatch.length && !hasMoreBatches) {
            throw new NoSuchElementException();
        }
        T result = null;
        // Check if we have still results left in batch,
        // if not fetch a new batch.
        if (batchIndex >= dataBatch.length) {
            assert(hasMoreBatches);
            // This updates batchIndex, dataBatch and
            // hasMoreBatches
            fetchBatchFromStorage();
            assert(batchIndex == 0);
            assert(dataBatch.length > 0);
        }
        result = dataBatch[batchIndex];
        batchIndex++;
        return result;
    }

    private void fetchBatchFromStorage() throws StorageException {
        logger.log(Level.FINEST, "Getting more results for cursorId: " + cursorId);
        WebQueryResponse<T> nextBatchResponse = storage.getMore(cursorId, parametrizedTypeToken, getBatchSize(), stmt);
        switch(nextBatchResponse.getResponseCode()) {
        case PreparedStatementResponseCode.QUERY_SUCCESS: 
            this.batchIndex = 0;
            this.hasMoreBatches = nextBatchResponse.hasMoreBatches();
            this.dataBatch = nextBatchResponse.getResultList();
            break;
        case PreparedStatementResponseCode.GET_MORE_NULL_CURSOR:
            // Advise user about potentially timed-out cursor
            String msg = "[get-more] Failed to get more results for cursorId: " + cursorId +
                         " This may be caused because the cursor timed out." +
                         " Resubmitting the original query might be an approach to fix it." +
                         " See server logs for more details.";
            throw new StorageException(msg);
        default:
            msg = "[get-more] Failed to get more results for cursorId: " + cursorId +
            ". See server logs for details.";
            throw new StorageException(msg);
        }
    }

}

