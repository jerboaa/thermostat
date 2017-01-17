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

package com.redhat.thermostat.storage.mongodb;

import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.shared.perflog.PerformanceLogFormatter;
import com.redhat.thermostat.shared.perflog.PerformanceLogFormatterBuilder;
import com.redhat.thermostat.storage.core.QueuedStorage;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.StorageProvider;
import com.redhat.thermostat.storage.mongodb.internal.MongoStorage;

public class MongoStorageProvider implements StorageProvider {

    private String url;
    private StorageCredentials creds;
    private SSLConfiguration sslConf;

    public void setConfig(String url, StorageCredentials creds, SSLConfiguration sslConf) {
        this.url = url;
        this.creds = creds;
        this.sslConf = sslConf;
    }

    @Override
    public Storage createStorage() {
        MongoStorage storage = new MongoStorage(url, creds, sslConf);
        if (LoggingUtils.getEffectiveLogLevel(LoggingUtils.getLogger(MongoStorageProvider.class)).intValue() <= LoggingUtils.LogLevel.PERFLOG.getLevel().intValue()) {
            PerformanceLogFormatterBuilder builder = PerformanceLogFormatterBuilder.create();
            PerformanceLogFormatter lf = builder.setLoggedTimeUnit(TimeUnit.NANOSECONDS)
                                              .build();
            return new QueuedStorage(storage, lf); 
        }
        return new QueuedStorage(storage);
    }

    @Override
    public boolean canHandleProtocol() {
        return url.startsWith("mongodb://");
    }

}

