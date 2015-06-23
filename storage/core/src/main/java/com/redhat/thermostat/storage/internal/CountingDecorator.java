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

package com.redhat.thermostat.storage.internal;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.perflog.PerformanceLogFormatter;
import com.redhat.thermostat.shared.perflog.PerformanceLogFormatter.LogTag;
import com.redhat.thermostat.storage.core.QueuedStorage;

/**
 * Decorator for ExecutorService's based queued storage. Allows for inspecting
 * and logging the queue size.
 *
 * @see QueuedStorage
 */
public class CountingDecorator implements ExecutorService {

    private static final String QUEUE_SIZE_PREFIX = "Q_SIZE";
    private static final String QUEUE_SIZE_FORMAT = QUEUE_SIZE_PREFIX + " %s";
    private static final Logger logger = LoggingUtils.getLogger(CountingDecorator.class);
    private final LogTag logTag;
    private final PerformanceLogFormatter perfLogFormatter;
    private final ExecutorService decoratee;
    private final AtomicLong queueLength;
    
    /**
     * 
     * @param decoratee
     *            The executor service to decorate.
     * @param perfLogFormatter
     *            The log formatter to use for logged messages.
     * @param logTag The log tag to use when logging messages.
     */
    public CountingDecorator(ExecutorService decoratee, PerformanceLogFormatter perfLogFormatter, LogTag logTag) {
        this.decoratee = Objects.requireNonNull(decoratee);
        this.perfLogFormatter = Objects.requireNonNull(perfLogFormatter);
        this.logTag = logTag;
        this.queueLength = new AtomicLong();
    }
    
    @Override
    public void execute(final Runnable command) {
        queueLength.incrementAndGet();
        decoratee.execute(new Runnable() {

            @Override
            public void run() {
                command.run();
                queueLength.decrementAndGet();
            }
            
        });
        String msg = String.format(QUEUE_SIZE_FORMAT, queueLength.get());
        logger.log(LoggingUtils.LogLevel.PERFLOG.getLevel(), perfLogFormatter.format(logTag, msg));
    }

    @Override
    public void shutdown() {
        decoratee.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return decoratee.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return decoratee.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return decoratee.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        return decoratee.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return decoratee.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return decoratee.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return decoratee.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return decoratee.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return decoratee.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return decoratee.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
            long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return decoratee.invokeAny(tasks, timeout, unit);
    }
    
    public long getQueueLength() {
        return queueLength.get();
    }

}
