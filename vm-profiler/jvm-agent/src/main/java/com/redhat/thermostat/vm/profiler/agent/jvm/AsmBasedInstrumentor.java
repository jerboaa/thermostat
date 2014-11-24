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

package com.redhat.thermostat.vm.profiler.agent.jvm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;

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
            return writer.toByteArray();

        } catch (Exception e) {
            System.err.println("Error transforming: " + className);
            e.printStackTrace();
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

            if (mv != null) {
                MethodVisitor instrumentor = new InstrumentingMethodAdapter(mv, className, access, name, desc);
                mv = new JSRInlinerAdapter(instrumentor, access, name, desc, signature, exceptions);
            }
            return mv;
        }
    }

    static class InstrumentingMethodAdapter extends AdviceAdapter {
        private String className;
        private String methodName;

        protected InstrumentingMethodAdapter(MethodVisitor mv, String className, int access, String methodName, String desc) {
            super(Opcodes.ASM5, mv, access, methodName, desc);

            this.className = className;
            this.methodName = methodName;
        }

        @Override
        protected void onMethodEnter() {
            callProfilerRecorder("enterMethod");
        }

        @Override
        protected void onMethodExit(int opCode) {
            callProfilerRecorder("exitMethod");
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
