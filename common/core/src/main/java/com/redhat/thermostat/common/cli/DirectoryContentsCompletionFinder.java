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

package com.redhat.thermostat.common.cli;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A CompletionFinder which provides completions for paths within a specific directory.
 *
 * This may be useful for commands which require a path to a config file, which is only
 * expected to reside within an application-specific directory.
 *
 * This is NOT recursive - paths will only be completed if they are direct children of
 * the specified directory.
 */
public class DirectoryContentsCompletionFinder implements CompletionFinder {

    public static final CompletionMode DEFAULT_COMPLETION_MODE = CompletionMode.NAME_ONLY;
    public static final DefaultFileFilter DEFAULT_FILE_FILTER = new DefaultFileFilter();

    private final File directory;
    private CompletionMode mode = DEFAULT_COMPLETION_MODE;
    private FileFilter filter = DEFAULT_FILE_FILTER;

    /**
     * @param directory the directory to search. Must not be null.
     */
    public DirectoryContentsCompletionFinder(File directory) {
        this.directory = Objects.requireNonNull(directory);
    }

    /**
     * Set the configurable completion mode.
     * The provided completions may be only the file name, or the relative path,
     * or the absolute path, or the canonical path. This affects what text is
     * offered as tab completions, but does not affect the search behaviour of
     * this CompletionFinder.
     * The default mode is NAME_ONLY.
     * @param mode the completion mode. Must not be null.
     */
    public void setCompletionMode(CompletionMode mode) {
        this.mode = Objects.requireNonNull(mode);
    }

    /**
     * Set the file filter.
     * This can be used to only suggest completion of files within the specified
     * directory which also match some criteria. For example, a filter
     * might be provided which accepts only PNG files, in which case no tab
     * completions will be offered for JPEGs within the directory. Or for another
     * example, a filter can even be provided which accepts only subdirectories,
     * rather than files.
     * The default filter accepts all files and ignores subdirectories.
     * @param filter the file filter. Must not be null.
     */
    public void setFileFilter(FileFilter filter) {
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public List<CompletionInfo> findCompletions() {
        if (!directory.exists()) {
            return Collections.emptyList();
        }
        if (!directory.canRead()) {
            return Collections.emptyList();
        }
        if (directory.isFile()) {
            try {
                return Collections.singletonList(getInfo(directory));
            } catch (IOException e) {
                // just ignore this file path
            }
        }
        // path exists, is readable, and is not a file (-> is a directory)
        List<CompletionInfo> results = new ArrayList<>();
        for (File file : directory.listFiles(filter)) {
            try {
                results.add(getInfo(file));
            } catch (IOException e) {
                // just ignore this file path
            }
        }
        return results;
    }

    private CompletionInfo getInfo(File file) throws IOException {
        return mode.map(file);
    }

    public enum CompletionMode {
        NAME_ONLY(new FileInfoMapper() {
            @Override
            public CompletionInfo map(File file) {
                return new CompletionInfo(file.getName());
            }
        }),
        RELATIVE_PATH(new FileInfoMapper() {
            @Override
            public CompletionInfo map(File file) {
                return new CompletionInfo(file.getPath());
            }
        }),
        ABSOLUTE_PATH(new FileInfoMapper() {
            @Override
            public CompletionInfo map(File file) {
                return new CompletionInfo(file.getAbsolutePath());
            }
        }),
        CANONICAL_PATH(new FileInfoMapper() {
            @Override
            public CompletionInfo map(File file) throws IOException {
                return new CompletionInfo(file.getCanonicalPath());
            }
        });

        CompletionMode(FileInfoMapper mapper) {
            this.mapper = mapper;
        }

        private FileInfoMapper mapper;

        CompletionInfo map(File file) throws IOException {
            return mapper.map(file);
        }

    }

    private interface FileInfoMapper {
        CompletionInfo map(File file) throws IOException;
    }

    private static class DefaultFileFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isFile();
        }
    }

}
