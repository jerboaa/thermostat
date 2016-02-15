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

package com.redhat.thermostat.vm.profiler.agent.jvm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.util.CheckClassAdapter;

public class AsmBasedInstrumentor extends ProfilerInstrumentor {

    private static final String RECORDER_CLASS_NAME =
            ProfileRecorder.class.getCanonicalName().replace('.', '/');

    @Override
    public byte[] transform(ClassLoader cl, String className, byte[] classBytes) {
        try {
            Thread.currentThread().setContextClassLoader(cl);
            // pipe data: reader -> instrumentor -> writer
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassLoaderFriendlyClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, cl);
            InstrumentingClassAdapter instrumentor = new InstrumentingClassAdapter(writer);
            reader.accept(instrumentor, ClassReader.SKIP_FRAMES);
            byte[] data = writer.toByteArray();

            // check that the bytecode is valid
            reader = new ClassReader(data);
            reader.accept(new CheckClassAdapter(new ClassWriter(0)), 0);

            return data;

        } catch (Exception e) {
            Debug.printlnError("Error transforming: " + className);
            Debug.printStackTrace(e);
            throw new AssertionError(e);
        }
    }

    static class InstrumentingClassAdapter extends ClassVisitor {

        private String className;

        public InstrumentingClassAdapter(ClassVisitor visitor) {
            super(Opcodes.ASM5, visitor);
        }

        @Override
        public void visit(int version, int access, String name,
                String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);

            className = name;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                String signature, String[] exceptions) {

            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

            // FIXME instrument constructors
            if (mv != null && !(name.equals("<init>"))) {
                MethodVisitor instrumentor = new InstrumentingMethodAdapter(mv, className, access, name, desc);
                mv = new JSRInlinerAdapter(instrumentor, access, name, desc, signature, exceptions);
            }

            return mv;
        }
    }

    /**
     * Inserts a try-finally around the an arbitrary method and calls
     * {@link ProfileRecorder} to record method execution times.
     * <p>
     * Functionally, it should transform:
     *
     * <pre>
     * public Object foo(int bar) {
     *     // do something
     *     return object;
     * }
     * </pre>
     *
     * to
     *
     * <pre>
     * public Object foo(int bar) {
     *     ProfilerData.enterMethod(&quot;description&quot;);
     *     try {
     *         // do something
     *         return object
     *     } finally {
     *         profilerData.exitMethod(&quot;description&quot;);
     *     }
     * }
     * </pre>
     *
     * Java bytecode has no concept of {@code finally} in a {@code try}-
     * {@code catch}-{@code finally} block. The {@code finally} code needs to be
     * duplicated in a {@code catch} block as well as in the normal-return
     * block. Because there may already be exception-handling code in the
     * method, it adds an exception-table entry in the last place that covers
     * the entire method.
     */
    static class InstrumentingMethodAdapter extends AdviceAdapter {

        private static final String EXIT_METHOD = "exitMethod";

        private String className;
        private String methodName;

        private Label startFinally = new Label();
        private Label endFinally = new Label();

        protected InstrumentingMethodAdapter(MethodVisitor mv, String className, int access, String methodName, String desc) {
            super(Opcodes.ASM5, mv, access, methodName, desc);

            this.className = className;
            this.methodName = methodName;
        }

        @Override
        public void visitCode() {
            super.visitCode();

            mv.visitLabel(startFinally);
        }

        @Override
        protected void onMethodEnter() {
            callProfilerRecorder("enterMethod");
        }

        @Override
        protected void onMethodExit(int opCode) {
            // An ATHROW will be caught in the catch-all-exceptions block and handled there
            if (opCode != ATHROW) {
                callProfilerRecorder(EXIT_METHOD);
            }
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {

            // add a catch-all-exceptions handler
            mv.visitLabel(endFinally);
            callProfilerRecorder(EXIT_METHOD);
            mv.visitInsn(ATHROW);

            // This is important. This catch-all-exceptions handler must be the
            // last entry in the exception table so it only gets invoked if
            // there is no other handler and the exception would have been
            // thrown to the caller.
            mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);

            super.visitMaxs(maxStack, maxLocals);
        }

        private void callProfilerRecorder(String method) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, RECORDER_CLASS_NAME, "getInstance", "()L" + RECORDER_CLASS_NAME + ";", false);
            mv.visitLdcInsn(className + "." + methodName + methodDesc);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, RECORDER_CLASS_NAME, method, "(Ljava/lang/String;)V", false);
        }

        // for debugging: insert opcodes to invoke System.exit()
        // private void insertSystemExit() {
        //     mv.visitInsn(ICONST_1);
        //     mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "exit", "(I)V", false);
        // }

    }
}
