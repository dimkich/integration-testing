package io.github.dimkich.integration.testing.collections;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.OpenedClassReader;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Rewrites {@code new HashMap(...)} and {@code new HashSet(...)} allocations to
 * {@code new LinkedHashMap(...)} and {@code new LinkedHashSet(...)}.
 * <p>
 * {@link net.bytebuddy.asm.MemberSubstitution} cannot do this because it substitutes method
 * and constructor invocations, while the allocated type is defined by the preceding
 * {@code NEW} instruction. This visitor tracks the most recent uninitialized {@code NEW}
 * targets and rewrites the matching constructor invocation ({@code <init>}) as well.
 * </p>
 * <p>
 * Calls to {@code super()} inside classes that extend {@link java.util.HashMap} or
 * {@link java.util.HashSet} are not rewritten: there is no matching {@code NEW} instruction
 * on the tracking stack, so the original superclass constructor is kept.
 * </p>
 */
class HashCollectionRewriter implements AsmVisitorWrapper {

    private static final String HASH_MAP = "java/util/HashMap";
    private static final String LINKED_HASH_MAP = "java/util/LinkedHashMap";
    private static final String HASH_SET = "java/util/HashSet";
    private static final String LINKED_HASH_SET = "java/util/LinkedHashSet";

    @Override
    public int mergeWriter(int flags) {
        return flags;
    }

    @Override
    public int mergeReader(int flags) {
        return flags;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public ClassVisitor wrap(TypeDescription instrumentedType,
                             ClassVisitor classVisitor,
                             Implementation.Context implementationContext,
                             TypePool typePool,
                             FieldList<FieldDescription.InDefinedShape> fields,
                             MethodList<?> methods,
                             int writerFlags,
                             int readerFlags) {
        return new ClassVisitor(OpenedClassReader.ASM_API, classVisitor) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (methodVisitor == null) {
                    return null;
                }
                return new RewritingMethodVisitor(methodVisitor);
            }
        };
    }

    private static class RewritingMethodVisitor extends MethodVisitor {

        private final Deque<String> pendingNewTypes = new ArrayDeque<>();

        protected RewritingMethodVisitor(MethodVisitor methodVisitor) {
            super(OpenedClassReader.ASM_API, methodVisitor);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (opcode == Opcodes.NEW && isHashCollection(type)) {
                pendingNewTypes.push(type);
                super.visitTypeInsn(opcode, linkedType(type));
                return;
            }
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                                    boolean isInterface) {
            if (opcode == Opcodes.INVOKESPECIAL
                    && "<init>".equals(name)
                    && !pendingNewTypes.isEmpty()
                    && pendingNewTypes.peek().equals(owner)) {
                pendingNewTypes.pop();
                super.visitMethodInsn(opcode, linkedType(owner), name, descriptor, isInterface);
                return;
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        private static boolean isHashCollection(String type) {
            return HASH_MAP.equals(type) || HASH_SET.equals(type);
        }

        private static String linkedType(String type) {
            if (HASH_MAP.equals(type)) {
                return LINKED_HASH_MAP;
            }
            if (HASH_SET.equals(type)) {
                return LINKED_HASH_SET;
            }
            return type;
        }
    }
}
