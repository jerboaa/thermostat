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

package com.redhat.thermostat.thread.model;

import com.google.gson.Gson;

public class StackFrame {

    private static final Gson gson = new Gson();

    private String fileName;
    private String packageName;
    private String className;
    private String methodName;
    private int lineNumber;
    private boolean isNativeMethod;

    public StackFrame() {}

    public StackFrame(String fileName, String packageName, String className, String methodName,
                      int lineNumber, boolean isNativeMethod) {
        this.fileName = fileName;
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
        this.isNativeMethod = isNativeMethod;
    }

    public StackFrame(StackTraceElement frame) {
        this(
                frame.getFileName(),
                parsePackage(frame.getClassName()),
                parseClass(frame.getClassName()),
                frame.getMethodName(),
                frame.getLineNumber(),
                frame.isNativeMethod()
        );
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isNativeMethod() {
        return isNativeMethod;
    }

    public void setNativeMethod(boolean nativeMethod) {
        isNativeMethod = nativeMethod;
    }

    static String parsePackage(String className) {
        if (className.contains(".")) {
            return className.substring(0, className.lastIndexOf('.'));
        } else {
            return null;
        }
    }

    static String parseClass(String className) {
        if (className.contains(".")) {
            return className.substring(className.lastIndexOf('.') + 1);
        } else {
            return className;
        }
    }

    public static StackFrame fromJson(String json) {
        return gson.fromJson(json, StackFrame.class);
    }

    @Override
    public String toString() {
        return gson.toJson(this);
    }

}
