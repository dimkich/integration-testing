package io.github.dimkich.integration.testing.expression;

import eu.ciechanowiec.sneakyfun.SneakyPredicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionTest {
    public static class A {
        public void method() {
        }
    }

    public static class MathSimple {
        public float multiplyByTwo(float value) {
            return 2 * value;
        }
    }

    @Getter
    @ToString
    @RequiredArgsConstructor
    private static class Pointcut {
        private final String expression;
        private final Class<?> cls;
        private final Method method;
        private final boolean expected;
        private final String error;

        static Pointcut of(String expr, Class<?> cls, boolean matches) {
            return new Pointcut(expr, cls, null, matches, null);
        }

        static Pointcut of(String expr, Method method, boolean matches) {
            return new Pointcut(expr, null, method, matches, null);
        }

        static Pointcut of(String expr, String error) {
            return new Pointcut(expr, null, null, false, error);
        }

        public void run() {
            try {
                PointcutMatch match = ExpressionFactory.createPointcutMatch(expression);
                boolean actual;
                if (method != null) {
                    actual = match.getMethodJunction().matches(new MethodDescription.ForLoadedMethod(method));
                } else {
                    actual = match.getTypeJunction().matches(TypeDescription.ForLoadedType.of(cls));
                }
                assertEquals(expected, actual, "Failed: " + expression + (method != null ? " on method " + method.getName() : " on class " + cls.getSimpleName()));
            } catch (Exception e) {
                if (error == null) {
                    throw e;
                }
                assertTrue(e.getMessage().contains(error), "Expected error containing: " + error
                        + " but got: " + e.getMessage());
            }
        }
    }

    static List<Pointcut> pointcutData() throws NoSuchMethodException {
        Method arrayListGet = ArrayList.class.getMethod("get", int.class);
        Method arrayListToString = ArrayList.class.getMethod("toString");
        Method objectToString = Object.class.getMethod("toString");
        Method arrayListWait = ArrayList.class.getMethod("wait");
        Method arrayListAdd = ArrayList.class.getMethod("add", Object.class);
        Method arrayListIsEmpty = ArrayList.class.getMethod("isEmpty");
        Method linkedListGet = LinkedList.class.getMethod("get", int.class);
        Method linkedListAdd = LinkedList.class.getMethod("add", Object.class);
        Method listOf = List.class.getMethod("of", Object[].class);
        Method listGet = List.class.getMethod("get", int.class);

        return Arrays.asList(
                // --- CLASS SELECTORS ---
                Pointcut.of("t.name('java.lang.String[]')", String[].class, true),
                Pointcut.of("t.name('int[]')", int[].class, true),
                Pointcut.of("t.name('int[][]')", int[][].class, true),
                Pointcut.of("t.inherits('java.io.Serializable')", Object[].class, true),
                Pointcut.of("t.inherits('java.lang.Object')", int[].class, true),
                Pointcut.of("t.inherits('java.util.List')", ArrayList.class, true),
                Pointcut.of("t.inherits('java.util.List')", LinkedList.class, true),
                Pointcut.of("t.inherits('java.util.List')", List.class, true),
                Pointcut.of("t.inherits('java.util.List')", HashSet.class, false),
                Pointcut.of("t.name('java.util.ArrayList')", ArrayList.class, true),
                Pointcut.of("t.name('java.util.ArrayList')", LinkedList.class, false),
                Pointcut.of("t.name('byte')", byte.class, true),
                Pointcut.of("t.name('short')", short.class, true),
                Pointcut.of("t.name('int')", int.class, true),
                Pointcut.of("t.name('long')", long.class, true),
                Pointcut.of("t.name('float')", float.class, true),
                Pointcut.of("t.name('double')", double.class, true),
                Pointcut.of("t.name('boolean')", boolean.class, true),
                Pointcut.of("t.name('char')", char.class, true),
                Pointcut.of("t.name('void')", void.class, true),
                Pointcut.of("t.name('int')", long.class, false),
                Pointcut.of("t.name('java.util.Map$Entry')", Map.Entry.class, true),
                Pointcut.of("t.name('java.util.Map$Entry')", Map.class, false),
                Pointcut.of("t.inherits('java.util.Map$Entry')", AbstractMap.SimpleEntry.class, true),
                Pointcut.of("t.inherits('java.util.Map$Entry')", Map.class, false),
                // --- ANNOTATIONS ---
                Pointcut.of("t.ann('java.lang.Deprecated')", ArrayList.class, false),
                Pointcut.of("t.ann('java.lang.FunctionalInterface')", Runnable.class, true),
                Pointcut.of("t.name('java.util.concurrent.Callable') && t.ann('java.lang.FunctionalInterface')", Callable.class, true),
                Pointcut.of("t.name('java.lang.Runnable') && t.ann('java.lang.FunctionalInterface')", Callable.class, false),
                Pointcut.of("t.ann('java.lang.FunctionalInterface')", ArrayList.class, false),
                Pointcut.of("m.ann('org.junit.jupiter.params.ParameterizedTest')",
                        ExpressionTest.class.getDeclaredMethod("testPointcut", Pointcut.class), true),
                // --- METHOD SELECTORS ---
                Pointcut.of("t.name('java.util.ArrayList') && m.name('get') && m.args(1)", arrayListGet, true),
                Pointcut.of("t.name('java.util.ArrayList') && m.name('get') && m.args('int')", arrayListGet, true),
                Pointcut.of("t.name('java.util.ArrayList') && m.name('get')", linkedListGet, false),
                Pointcut.of("t.name('java.util.ArrayList') && m.name('add')", arrayListAdd, true),
                Pointcut.of("t.name('java.util.ArrayList') && m.name('add') && m.args('java.lang.Object')", arrayListAdd, true),
                Pointcut.of("m.name('add') && m.args(1)", linkedListAdd, true),
                Pointcut.of("m.name('isEmpty') && m.args(0)", arrayListIsEmpty, true),
                Pointcut.of("m.name('isEmpty') && m.args(1)", arrayListIsEmpty, false),
                Pointcut.of("m.ann('java.lang.SafeVarargs')", listOf, true),
                Pointcut.of("t.inherits('java.util.List') && m.name('of') && m.ann('java.lang.SafeVarargs')", listOf, true),
                Pointcut.of("m.ann('java.lang.Deprecated')", ArrayList.class.getDeclaredMethods()[0], false),
                // --- HIERARCHY + METHODS ---
                Pointcut.of("t.inherits('java.lang.Object') && m.name('toString')", arrayListToString, true),
                Pointcut.of("t.name('java.lang.Object') && m.name('toString')", objectToString, true),
                Pointcut.of("t.name('java.lang.Object') && m.name('toString')", arrayListToString, false),
                Pointcut.of("t.inherits('java.lang.Object') && m.name('wait') && m.args(0)", arrayListWait, true),
                Pointcut.of("t.name('java.lang.Object') && m.name('wait')", arrayListWait, true),
                Pointcut.of("t.name('java.lang.Object') && m.name('wait')", Object.class.getMethod("wait"), true),
                Pointcut.of("m.name('add') && m.args('java.lang.Object')", arrayListAdd, true),
                Pointcut.of("m.returns('java.lang.Object') && m.name('get')", listGet, true),
                Pointcut.of("t.inherits('java.util.RandomAccess')", ArrayList.class, true),
                Pointcut.of("t.inherits('java.util.RandomAccess')", LinkedList.class, false),
                Pointcut.of("t.inherits('java.io.Serializable')", String.class, true),
                // --- LOGIC ---
                Pointcut.of("m.name('toString') && (t.name('java.util.ArrayList') || t.name('java.lang.Object'))", arrayListToString, false),
                Pointcut.of("m.name('toString') && (t.name('java.util.ArrayList') || t.name('java.lang.Object'))", objectToString, true),
                Pointcut.of("m.isPublic() && !m.isStatic() && m.name('add')", arrayListAdd, true),
                Pointcut.of("t.name('io.github.dimkich.integration.testing.expression.ExpressionTest$A') && m.name('method')", A.class.getMethod("method"), true),
                Pointcut.of("t.isFinal()", String.class, true),
                Pointcut.of("t.isAbstract()", List.class, true),
                Pointcut.of("t.isAnnotation()", Override.class, true),
                Pointcut.of("t.isEnum()", java.util.concurrent.TimeUnit.class, true),
                Pointcut.of("t.inherits('java.util.List') && !t.isInterface() && t.isPublic()", ArrayList.class, true),
                Pointcut.of("m.isPublic() && (m.name('add') || m.name('remove'))", arrayListAdd, true),
                Pointcut.of("t.inPackage('java.util') && t.simpleName('ArrayList')", ArrayList.class, true),
                Pointcut.of("t.packageStartsWith('java.util')", Map.Entry.class, true),
                // --- ERRORS ---
                Pointcut.of("t.namme('ArrayList')", "A method named \"namme\" is not declared"),
                Pointcut.of("t.name('A') &&& m.isPublic()", "Unexpected token \"&\""),
                Pointcut.of("t.name('com.Foo') && m.isSomething()", "A method named \"isSomething\" is not declared"),
                Pointcut.of("t.name('ArrayList')", "Cannot resolve type description for ArrayList"),
                Pointcut.of("t.name('A') && m.args('int', )", "Unexpected token \")\""),
                Pointcut.of("m.ann(NonExistent.class)", "No applicable constructor/method found for actual parameters"),
                Pointcut.of("t.name('ArrayList'))", "';' expected instead of ')'")
        );
    }

    @ParameterizedTest
    @MethodSource("pointcutData")
    void testPointcut(Pointcut pointcut) {
        pointcut.run();
    }

    @Getter
    @ToString
    @RequiredArgsConstructor
    private static class Invoke {
        private final String expression;
        private final Object target;
        private final Object[] args;
        private final boolean expected;
        private final String error;

        static Invoke of(String expr, Object target, boolean expected) {
            return new Invoke(expr, target, new Object[0], expected, null);
        }

        static Invoke of(String expr, Object target, Object[] args, boolean expected) {
            return new Invoke(expr, target, args, expected, null);
        }

        static Invoke of(String expr, String error) {
            return new Invoke(expr, null, null, false, error);
        }

        static Invoke of(String expr, Object target, String error) {
            return new Invoke(expr, target, null, false, error);
        }

        public void run() throws Exception {
            try {
                BiPredicate<Object, Object[]> predicate = ExpressionFactory.createInvokePredicate(expression);
                boolean actual = predicate.test(target, args);
                assertEquals(expected, actual, "Failed expression: " + expression);
            } catch (Exception e) {
                if (error == null) {
                    throw e;
                }
                assertTrue(e.getMessage().contains(error), "Expected error containing: " + error
                        + " but got: " + e.getMessage());
            }
        }
    }

    static List<Invoke> invokeData() {
        List<String> list = new ArrayList<>();
        list.add("test");

        return List.of(
                Invoke.of("o.get() instanceof java.util.List", list, true),
                Invoke.of("o.isInstance(java.util.Map.class)", list, false),
                Invoke.of("a.size() == 1 && a.arg(0).get().equals('test')", null, new Object[]{"test"}, true),
                Invoke.of("a.size() == 2", null, new Object[]{1, 2}, true),
                Invoke.of("o.asList().size() == 1", list, true),
                Invoke.of("o.asList().get(0).equals('test')", list, true),
                Invoke.of("!o.isNull() && a.size() > 0 && a.arg(0).get() instanceof String", "some", new Object[]{"str"}, true),
                Invoke.of("o.asString().equals('test')", "test", true),
                Invoke.of("o.asString().charAt(1) == ''e''", "test", true),
                Invoke.of("o.asString().charAt(3) == ''2''", "test", false),
                Invoke.of("o.asString().equals('te\\'st')", "te'st", true),
                Invoke.of("o.asString().equals('te\\'st')", "test", false),
                Invoke.of("o.call('ensureCapacity', (byte)10).isNull()", new ArrayList<>(), true),
                Invoke.of("o.call('ensureCapacity', (short)10).isNull()", new ArrayList<>(), true),
                // --- ERRORS ---
                Invoke.of("o.nonExistentMethod()", "A method named \"nonExistentMethod\" is not declared"),
                Invoke.of("a.arg(0) == 'test' &&& true", "Unexpected token"),
                Invoke.of("(String)o.get().substring(0)", "A method named \"substring\" is not declared"),
                Invoke.of("o.get() instanceof NonExistentClass", "Cannot determine simple type"),
                Invoke.of("o.asString().charAt(1) == 'e'", "Cannot compare types \"char\" and \"java.lang.String\""),
                Invoke.of("o.call('ensureCapacity', (long)10).isNull()", new ArrayList<>(), "ensureCapacity with params"),
                Invoke.of("o.call('ensureCapacity', true).isNull()", new ArrayList<>(), "ensureCapacity with params")
        );
    }

    @ParameterizedTest
    @MethodSource("invokeData")
    void testInvokeMatcher(Invoke testCase) throws Exception {
        testCase.run();
    }

    @Getter
    @ToString
    @RequiredArgsConstructor
    private static class ObjectCase {
        private final String expression;
        private final Object target;
        private final boolean expected;
        private final String error;

        static ObjectCase of(String expr, Object target, boolean expected) {
            return new ObjectCase(expr, target, expected, null);
        }

        static ObjectCase of(String expr, Object target, String error) {
            return new ObjectCase(expr, target, false, error);
        }

        public void run() throws Exception {
            try {
                Predicate<Object> predicate = ExpressionFactory.createObjectPredicate(expression);
                boolean actual = predicate.test(target);
                assertEquals(expected, actual, "Failed expression: " + expression + " on target: " + target);
            } catch (Exception e) {
                if (error == null) throw e;
                if (e instanceof InvocationTargetException ite) {
                    e = (Exception) ite.getTargetException();
                }
                assertTrue(e.getMessage().contains(error),
                        "Expected error containing: " + error + " but got: " + e.getMessage());
            }
        }
    }

    static List<ObjectCase> objectData() {
        List<String> list = new ArrayList<>(List.of("one", "two"));
        return List.of(
                // --- BASE ---
                ObjectCase.of("o.isNull()", null, true),
                ObjectCase.of("o.isNull()", "not null", false),
                ObjectCase.of("o.isInstance(String.class)", null, false),
                ObjectCase.of("o.isSameClass(String.class)", null, false),
                ObjectCase.of("o.isInstance(java.util.List.class)", list, true),
                ObjectCase.of("o.isSameClass(java.util.List.class)", list, false),
                ObjectCase.of("o.isInstance(java.util.ArrayList.class)", list, true),
                ObjectCase.of("o.isSameClass(java.util.ArrayList.class)", list, true),
                ObjectCase.of("o.isInstance(Object.class)", "string", true),
                ObjectCase.of("o.isSameClass(Object.class)", "string", false),
                ObjectCase.of("o.isInstance(String.class)", "hello", true),
                ObjectCase.of("o.isInstance(Integer.class)", "hello", false),
                ObjectCase.of("o.isInstance(byte.class)", (byte) 1, true),
                ObjectCase.of("o.isInstance(short.class)", (short) 1, true),
                ObjectCase.of("o.isInstance(int.class)", 1, true),
                ObjectCase.of("o.isSameClass(int.class)", 100, true),
                ObjectCase.of("o.isSameClass(byte.class)", 100, false),
                ObjectCase.of("o.isSameClass(int.class)", Integer.valueOf(100), true),
                ObjectCase.of("o.isInstance(long.class)", 1L, true),
                ObjectCase.of("o.isInstance(boolean.class)", true, true),
                ObjectCase.of("o.isInstance(double.class)", 1.0, true),
                ObjectCase.of("o.isInstance(int[].class)", new int[]{1, 2}, true),
                ObjectCase.of("o.isInstance(String[].class)", new String[]{"a"}, true),
                ObjectCase.of("o.isInstance(float[][].class)", new float[][]{{1}, {2}}, true),
                ObjectCase.of("o.isInstance(int.class)", 100L, false),
                ObjectCase.of("o.isInstance(double.class)", 10.5f, false),
                ObjectCase.of("o.isInstance(boolean.class)", "true", false),
                ObjectCase.of("o.asInt() == Integer.MAX_VALUE", Integer.MAX_VALUE, true),
                ObjectCase.of("o.isSameClass(int[].class)", new int[]{1, 2}, true),
                ObjectCase.of("o.isSameClass(long[].class)", new int[]{1, 2}, false),
                ObjectCase.of("o.isSameClass(String[].class)", new String[]{"a", "b"}, true),
                ObjectCase.of("o.isSameClass(Object[].class)", new String[]{"a"}, false),
                ObjectCase.of("o.isSameClass(int[][].class)", new int[][]{{1}, {2}}, true),
                // --- PRIMITIVE TYPES (Casting) ---
                ObjectCase.of("o.asInt() == 100", 100, true),
                ObjectCase.of("o.asInt() > 50 && o.asInt() < 150", 100, true),
                ObjectCase.of("o.asLong() == 1000L", 1000L, true),
                ObjectCase.of("o.asByte() == 1", (byte) 1, true),
                ObjectCase.of("o.asShort() == 1", (short) 1, true),
                ObjectCase.of("o.asBoolean()", true, true),
                ObjectCase.of("!o.asBoolean()", false, true),
                ObjectCase.of("o.asDouble() == 3.14", 3.14, true),
                ObjectCase.of("o.call('getAndSet', 2).asLong() == 1L", new AtomicLong(1), true),
                ObjectCase.of("o.call('getAndSet', (short)2).asLong() == 1L", new AtomicLong(1), true),
                ObjectCase.of("o.call('getAndSet', (byte)2).asLong() == 1L", new AtomicLong(1), true),
                ObjectCase.of("o.call('getAndSet', ''1'').asLong() == 1L", new AtomicLong(1), true),
                ObjectCase.of("o.call('orElse', 2L).asDouble() == 2.0", OptionalDouble.empty(), true),
                ObjectCase.of("o.call('orElse', 2).asDouble() == 2.0", OptionalDouble.empty(), true),
                ObjectCase.of("o.call('orElse', 2f).asDouble() == 2.0", OptionalDouble.empty(), true),
                ObjectCase.of("o.call('orElse', ''2'').asDouble() == 50.0", OptionalDouble.empty(), true),
                ObjectCase.of("o.call('getAndSet', true).asBoolean()", new AtomicBoolean(true), true),
                ObjectCase.of("o.call('multiplyByTwo', (byte)1).asFloat() == 2.0", new MathSimple(), true),
                ObjectCase.of("o.call('multiplyByTwo', (short)1).asFloat() == 2.0", new MathSimple(), true),
                ObjectCase.of("o.call('multiplyByTwo', 1).asFloat() == 2.0", new MathSimple(), true),
                ObjectCase.of("o.call('multiplyByTwo', ''1'').asFloat() == 98.0", new MathSimple(), true),
                // --- STRINGS AND COLLECTIONS ---
                ObjectCase.of("o.asString().length() == 5", "hello", true),
                ObjectCase.of("o.asString().startsWith('java')", "javascript", true),
                ObjectCase.of("o.asString().contains('test')", "this is a test", true),
                ObjectCase.of("o.asList().size() == 0", new ArrayList<>(), true),
                ObjectCase.of("o.asList().isEmpty()", List.of(1, 2), false),
                ObjectCase.of("o.asMap().containsKey('key')", Map.of("key", "value"), true),
                ObjectCase.of("o.call('charAt', 0).asChar() == ''t''", "test", true),
                ObjectCase.of("o.call('substring', 1, 3).asString().equals('es')", "test", true),
                ObjectCase.of("o.call('addAll', o.asList()).asBoolean()", new ArrayList<>(List.of(1)), true),
                ObjectCase.of("o.asString().concat('!').equals('test!')", "test", true),
                ObjectCase.of("o.asList().size() == 2", list, true),
                ObjectCase.of("o.field('size').asInt() == 2", list, true),
                ObjectCase.of("o.field('size').isNull()", null, true),
                ObjectCase.of("o.call('add', 'three').asBoolean()", new ArrayList<>(List.of("one", "two")), true),
                ObjectCase.of("o.call('clear').isNull()", new ArrayList<>(List.of(1)), true),
                ObjectCase.of("o.call('substring', (char)1).asString().equals('est')", "test", true),
                ObjectCase.of("o.call('substring').isNull()", null, true),
                ObjectCase.of("o.asIterable().iterator().next().equals('one')", list, true),
                ObjectCase.of("!o.asIterable().iterator().hasNext()", new java.util.ArrayDeque<>(), true),
                // --- NESTED CALLS ---
                ObjectCase.of("o.call('substring', 0, 4).asString().equals('test')", "test-string", true),
                ObjectCase.of("o.call('trim').asString().isEmpty()", "   ", true),
                ObjectCase.of("o.call('getClass').call('getSimpleName').asString().equals('ArrayList')", list, true),
                ObjectCase.of("o.call('equals', 100).asBoolean()", "100", false),
                ObjectCase.of("o.call('toString').asString().contains('key')",
                        new java.util.HashMap<>(java.util.Map.of("key", "val")), true),
                ObjectCase.of("o.call('size').asInt() == 1",
                        java.util.Collections.singletonList("item"), true),
                ObjectCase.of("java.lang.System.getProperty('java.version') != null", new Object(), true),
                // --- ERRORS ---
                ObjectCase.of("o.asInt() == '100'", 100, "Cannot compare types \"int\" and \"java.lang.String\""),
                ObjectCase.of("o.nonExistent()", "hello", "A method named \"nonExistent\" is not declared"),
                ObjectCase.of("o.asString().substring(1))", "err", "';' expected instead of ')'"),
                ObjectCase.of("o.call('get', 'invalid_index').asBoolean()", list, "get with params"),
                ObjectCase.of("o.field('nonExistentField').asBoolean()", list, "nonExistentField"),
                ObjectCase.of("o.call('getAndSet', true).asLong() == 1L", new AtomicLong(1), "getAndSet with params"),
                ObjectCase.of("o.call('orElse', true).asDouble() == 2.0", OptionalDouble.empty(), "orElse with params"),
                ObjectCase.of("o.call('getAndSet', 1).asBoolean()", new AtomicBoolean(true), "getAndSet with params"),
                ObjectCase.of("o.call('multiplyByTwo', true).asFloat() == 2.0", new MathSimple(), "multiplyByTwo with params"),
                ObjectCase.of("o.call('substring', 'invalid').asBoolean()", "test", "substring with params"),
                ObjectCase.of("o.asIterable().iterator().next().equals('one')", "str", "cannot be cast to class java.lang.Iterable"),
                ObjectCase.of("o.call('compareTo', new java.util.ArrayList()).asBoolean()", "test", "class java.util.ArrayList cannot be cast to class java.lang.String")

        );
    }

    @ParameterizedTest
    @MethodSource("objectData")
    void testObjectPredicate(ObjectCase testCase) throws Exception {
        testCase.run();
    }

    @Getter
    @ToString
    @RequiredArgsConstructor
    private static class FunctionCase {
        private final String expression;
        private final Object target;
        private final Class<?> returnType;
        private final Object expected;
        private final String error;

        static FunctionCase of(String expr, Object target, Class<?> returnType, Object expected) {
            return new FunctionCase(expr, target, returnType, expected, null);
        }

        static FunctionCase of(String expr, Class<?> returnType, String error) {
            return new FunctionCase(expr, null, returnType, null, error);
        }

        public void run() throws Exception {
            try {
                Function<Object, ?> function = ExpressionFactory.createObjectFunction(expression, returnType);
                Object actual = function.apply(target);
                assertEquals(expected, actual, "Failed function: " + expression);
            } catch (Exception e) {
                if (error == null) throw e;
                assertTrue(e.getMessage().contains(error),
                        "Expected error containing: " + error + " but got: " + e.getMessage());
            }
        }
    }

    static List<FunctionCase> functionData() {
        List<String> list = List.of("apple", "banana", "cherry");
        Map<String, Integer> map = Map.of("id", 101, "score", 500);

        return List.of(
                // --- ARITHMETIC AND PRIMITIVES ---
                FunctionCase.of("o.asInt() * 2", 25, Integer.class, 50),
                FunctionCase.of("Math.pow(o.asDouble(), 2)", 3.0, Double.class, 9.0),
                FunctionCase.of("o.asString().length()", "Janino", Integer.class, 6),
                FunctionCase.of("o.asLong() + 100L", 400L, Long.class, 500L),
                FunctionCase.of("o.asInt() << 1", 8, Integer.class, 16),
                FunctionCase.of("((String[])o.get()).length", new String[]{"a", "b"}, Integer.class, 2),
                // --- STRING TRANSFORMATIONS ---
                FunctionCase.of("o.asString().toUpperCase()", "hello", String.class, "HELLO"),
                FunctionCase.of("o.asString().substring(0, 3)", "expression", String.class, "exp"),
                FunctionCase.of("o.asString().trim().isEmpty()", "   ", Boolean.class, true),
                FunctionCase.of("o.asString().replace('e', '3')", "leet", String.class, "l33t"),
                FunctionCase.of("o.asString().split(',')[1]", "red,green,blue", String.class, "green"),
                // --- REFLECTION-BASED EXTRACTION (ObjectWrapper) ---
                FunctionCase.of("o.call('getClass').call('getName').asString()", 123, String.class, "java.lang.Integer"),
                FunctionCase.of("o.field('hash').asInt()", "test", Integer.class, "test".hashCode()),
                FunctionCase.of("o.call('isEmpty').asBoolean()", new ArrayList<>(), Boolean.class, true),
                // --- COLLECTIONS AND ARRAYS ---
                FunctionCase.of("o.asList().get(0)", list, Object.class, "apple"),
                FunctionCase.of("o.asList().size()", list, Integer.class, 3),
                FunctionCase.of("o.asMap().get('id')", map, Object.class, 101),
                FunctionCase.of("java.util.Arrays.asList(o.asString().split(','))", "a,b,c", List.class, List.of("a", "b", "c")),
                FunctionCase.of("o.asCollection().contains('banana')", list, Boolean.class, true),
                FunctionCase.of("java.util.Arrays.asList((Object[])o.get()).size()", new Object[]{1, 2, 3, 4}, Integer.class, 4),
                FunctionCase.of("new String[] { o.asString(), 'world' }.length", "hello", Integer.class, 2),
                FunctionCase.of("((int[][])o.get())[0][1]", new int[][]{{1, 2}, {3, 4}}, Integer.class, 2),
                // --- COMPLEX LOGIC AND CASTING ---
                FunctionCase.of("o.asInt() > 10 ? 'high' : 'low'", 15, String.class, "high"),
                FunctionCase.of("o.asString().indexOf('n')", "Janino", Integer.class, 2),
                FunctionCase.of("((String)o.get()).charAt(0)", "test", Character.class, 't'),
                FunctionCase.of("java.lang.Math.max(o.asInt(), 100)", 50, Integer.class, 100),
                FunctionCase.of("java.lang.String.valueOf(o.asBoolean())", true, String.class, "true"),
                // --- DATE AND TIME (Standard Java API) ---
                FunctionCase.of("java.time.LocalDate.parse(o.asString()).getYear()", "2024-01-01", Integer.class, 2024),
                FunctionCase.of("o.isInstance(java.lang.String.class) ? 'is-string' : 'not-string'", "hi", String.class, "is-string"),
                // --- TYPING AND COMPILATION ERRORS ---
                FunctionCase.of("o.asString()", Integer.class, "Assignment conversion not possible from type"),
                FunctionCase.of("o.unknownMethod()", String.class, "A method named \"unknownMethod\" is not declared"),
                FunctionCase.of("o.asInt() + 'string'", Integer.class, "Assignment conversion not possible from type")
        );
    }

    @ParameterizedTest
    @MethodSource("functionData")
    void testObjectFunction(FunctionCase testCase) throws Exception {
        testCase.run();
    }

    @Getter
    @ToString
    @RequiredArgsConstructor
    private static class ConsumerCase {
        private final String expression;
        private final Object target;
        private final SneakyPredicate<Object, Exception> check;
        private final String error;

        static ConsumerCase of(String expr, Object target, SneakyPredicate<Object, Exception> check) {
            return new ConsumerCase(expr, target, check, null);
        }

        static ConsumerCase of(String expr, String error) {
            return new ConsumerCase(expr, null, null, error);
        }

        static ConsumerCase of(String expr, Object target, String error) {
            return new ConsumerCase(expr, target, null, error);
        }

        public void run() throws Exception {
            try {
                Consumer<Object> consumer = ExpressionFactory.createObjectConsumer(expression);
                consumer.accept(target);
                if (check != null) {
                    assertTrue(check.test(target), "Post-condition failed for expression: " + expression);
                }
            } catch (Exception e) {
                if (error == null) throw e;
                assertTrue(e.getMessage().contains(error),
                        "Expected error containing: " + error + " but got: " + e.getMessage());
            }
        }
    }

    static List<ConsumerCase> consumerData() {
        List<String> list = new ArrayList<>(List.of("a"));
        Map<String, Object> map = new HashMap<>();
        StringBuilder sb = new StringBuilder("hello");

        class StatusHolder {
            public String status = "none";
        }
        StatusHolder holder = new StatusHolder();

        return List.of(
                // --- COLLECTION MODIFICATIONS ---
                ConsumerCase.of("o.asList().add('b')", list, t -> ((List<?>) t).contains("b")),
                ConsumerCase.of("o.asList().clear()", new ArrayList<>(List.of(1, 2)), t -> ((List<?>) t).isEmpty()),
                ConsumerCase.of("o.asMap().put('key', 'val')", map, t -> "val".equals(((Map<?, ?>) t).get("key"))),
                // --- REFLECTION-BASED CALLS (Side Effects) ---
                ConsumerCase.of("o.call('add', 'c')", list, t -> ((List<?>) t).contains("c")),
                ConsumerCase.of("o.call('append', ' world')", sb, t -> t.toString().equals("hello world")),
                ConsumerCase.of("o.call('setLength', 0)", sb, t -> t.toString().isEmpty()),
                ConsumerCase.of("o.call('addAndGet', 10)", new java.util.concurrent.atomic.AtomicInteger(0),
                        t -> ((java.util.concurrent.atomic.AtomicInteger) t).get() == 10),
                // --- MULTIPLE STATEMENTS ---
                ConsumerCase.of("{ o.asList().add('1'); o.asList().add('2'); }", new ArrayList<>(),
                        t -> ((List<?>) t).size() == 2),
                // --- CONDITIONAL AND LOOP CONTROL FLOW ---
                ConsumerCase.of("if (o.asList().isEmpty()) o.asList().add('init')", new ArrayList<>(),
                        t -> ((List<?>) t).size() == 1),
                ConsumerCase.of("for(int i=0; i<3; i++) o.asList().add(String.valueOf(i))", new ArrayList<>(),
                        t -> ((List<?>) t).size() == 3),
                // --- FIELD ACCESS (Side Effects) ---
                ConsumerCase.of("o.field('status').get().getClass(); // Just access check", holder, t -> true),
                // --- TYPING AND CASTING ---
                ConsumerCase.of("((java.util.ArrayList)o.get()).ensureCapacity(100)", new ArrayList<>(), t -> true),
                // --- ERRORS ---
                ConsumerCase.of("o.unknownMethod()", "A method named \"unknownMethod\" is not declared"),
                ConsumerCase.of("return o.asString()", "Method must not return a value"),
                ConsumerCase.of("{ o.asList().add('1') o.asList().add('2') }", "is not a type"),
                ConsumerCase.of("o.asList().add(123)", new ArrayList<String>(), "Argument 0 of method \"add\" is not applicable")
        );
    }


    @ParameterizedTest
    @MethodSource("consumerData")
    void testObjectConsumer(ConsumerCase testCase) throws Exception {
        testCase.run();
    }
}