/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.itest.standalone;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Custom JUnitCore runner which produces human readable reports.
 *
 * @see StandaloneReportsListener
 * @see AllStandaloneTests
 */
public class ItestRunner {
    
    private static class Runner {
        
        private static final String PASS_TOKEN = "PASSED.";
        private static final String FAIL_TOKEN = "FAILED.";
        private static final File REPORTS_FOLDER = new File("thermostat-itest-reports");
        static final File MAIN_REPORT_FILE = new File(REPORTS_FOLDER, "summary.txt");
        private PrintStream printStream;
        
        private Runner() {
            printStream = prepareReportsFolder();
            String msg = "Running Thermostat standalone integration tests ...";
            printStream.println(msg + "\n");
        }
        
        private PrintStream prepareReportsFolder() {
            if (REPORTS_FOLDER.exists()) {
                deleteReportsFolder();
            }
            REPORTS_FOLDER.mkdir();
            PrintStream ps;
            try {
                ps = new PrintStream(MAIN_REPORT_FILE);
            } catch (FileNotFoundException e) {
                // can't continue
                throw new RuntimeException(e);
            }
            return ps;
        }

        private void deleteReportsFolder() {
            try {
                Files.walkFileTree(REPORTS_FOLDER.toPath(), new FileVisitor<Path>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir,
                            BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file,
                            BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.TERMINATE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file,
                            IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir,
                            IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                    
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        private void startTest(Class<?> clazz) {
            printStream.print("  Running " + clazz.getName() + " ");
        }
        
        private void testFinished(Result result) {
            String testToken = PASS_TOKEN;
            if (!result.wasSuccessful()) {
                testToken = FAIL_TOKEN;
            }
            printStream.println(testToken);
        }
        
        private void finished() {
            printStream.println("\n");
            printStream.println("Done.");
            printStream.close();
        }
        
    }
    
    public static void main(String args[]) {
        if (args.length != 0) {
            usage();
        }
        Runner myRunner = new Runner();
        System.out.print("Running Thermostat standalone integration tests ");
        SuiteClasses cls = AllStandaloneTests.class.getAnnotation(Suite.SuiteClasses.class);
        boolean allPass = true;
        for (Class<?> c: cls.value()) {
            myRunner.startTest(c);
            JUnitCore core = new JUnitCore();
            core.addListener(new StandaloneReportsListener(new File(Runner.REPORTS_FOLDER, c.getName() + ".txt")));
            Result result = core.run(c);
            allPass = allPass && result.wasSuccessful();
            String testToken = result.wasSuccessful() ? "." : "F";
            System.out.print(testToken);
            myRunner.testFinished(result);
        }
        System.out.println(" Done.\n\nSee " + Runner.MAIN_REPORT_FILE.getAbsolutePath() + " for details.");
        myRunner.finished();
    }

    private static void usage() {
        System.err.println("Usage: " + ItestRunner.class.getName());
    }
}
