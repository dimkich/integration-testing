package io.github.dimkich.integration.testing.wait.completion.parser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

class ByteBuddySelectorParserTest {
    public static class A {
        public void method() {
        }
    }

    @Getter
    @ToString
    @RequiredArgsConstructor
    private static class Value {
        private final Class<?> cls;
        private final Method method;
        private final Class<?> typeFilter;
        private final boolean matches;

        static Value of(Method method, boolean matches) {
            return new Value(null, method, null, matches);
        }

        static Value of(Class<?> cls, boolean matches) {
            return new Value(cls, null, null, matches);
        }

        static Value of(Class<?> cls, Method method, boolean matches) {
            return new Value(cls, method, null, matches);
        }

        static Value of(Method method, Class<?> typeFilter, boolean matches) {
            return new Value(null, method, typeFilter, matches);
        }

        public TypeDescription type() {
            return cls == null ? TypeDescription.ForLoadedType.of(method.getDeclaringClass())
                    : TypeDescription.ForLoadedType.of(cls);
        }

        public TypeDescription typeFilter() {
            return typeFilter == null ? type() : TypeDescription.ForLoadedType.of(typeFilter);
        }

        public MethodDescription method() {
            return new MethodDescription.ForLoadedMethod(method);
        }

        public void assertResult(String expression, ByteBuddySelectorResult byteBuddySelectorResult) {
            boolean res = true;
            if (byteBuddySelectorResult.getTypeMatcher() != null) {
                res = byteBuddySelectorResult.getTypeMatcher().matches(type());
            }
            if (byteBuddySelectorResult.getMethodMatcher() != null) {
                res = res && byteBuddySelectorResult.getMethodMatcher().matches(method());
            }
            if (byteBuddySelectorResult.getTypeFilter() != null) {
                res = res && byteBuddySelectorResult.getTypeFilter().matches(typeFilter());
            }
            assertEquals(isMatches(), res,
                    expression + " must " + (isMatches() ? "" : "not ") + "match " + this);
        }
    }

    static Object[][] parseData() throws NoSuchMethodException {
        Method arrayListGet = ArrayList.class.getMethod("get", int.class);
        Method arrayListToString = ArrayList.class.getMethod("toString");
        Method objectToString = Object.class.getMethod("toString");
        Method arrayListWait = ArrayList.class.getMethod("wait");
        Method arrayListAdd = ArrayList.class.getMethod("add", Object.class);
        Method arrayListIsEmpty = ArrayList.class.getMethod("isEmpty");
        Method linkedListGet = LinkedList.class.getMethod("get", int.class);
        Method linkedListAdd = LinkedList.class.getMethod("add", Object.class);
        Method listVarargs = List.class.getMethod("of", Object[].class);
        return new Object[][]{
                {"class ( java.util.List + )  ", Value.of(ArrayList.class, true), null},
                {"class(java.util.List+)", Value.of(LinkedList.class, true), null},
                {"class(java.util.List+)", Value.of(List.class, true), null},
                {"class(java.util.List+)", Value.of(HashSet.class, false), null},
                {"method(java.util.ArrayList#get(..))", Value.of(arrayListGet, true), null},
                {"method(java.util.ArrayList#get(..))", Value.of(linkedListGet, false), null},
                {"method(java.util.ArrayList#add(..))", Value.of(arrayListAdd, true), null},
                {"method(java.util.ArrayList#add(..))", Value.of(linkedListAdd, false), null},
                {"method(java.util.ArrayList#isEmpty())", Value.of(arrayListIsEmpty, true), null},
                {"method(java.util.ArrayList#isEmpty())", Value.of(arrayListGet, false), null},
                {"class(@java.lang.Deprecated)", Value.of(ArrayList.class, false), null},
                {"class(@java.lang.FunctionalInterface)", Value.of(Runnable.class, true), null},
                {"class(java.util.concurrent.Callable@java.lang.FunctionalInterface)", Value.of(Callable.class, true), null},
                {"class(java.util.concurrent.Callable@java.lang.FunctionalInterface)", Value.of(Runnable.class, false), null},
                {"class(@java.lang.FunctionalInterface)", Value.of(ArrayList.class, false), null},
                {"method(@java.lang.Deprecated)", Value.of(ArrayList.class.getDeclaredMethods()[0], false), null},
                {"class(java.util.ArrayList)", Value.of(ArrayList.class, true), null},
                {"class(java.util.ArrayList)", Value.of(LinkedList.class, false), null},
                {"class(int)", Value.of(LinkedList.class, false), null},
                {"class(java.util.ArrayList@java.lang.Deprecated)", Value.of(ArrayList.class, false), null},
                {"class(java.util.Map$Entry+)", Value.of(java.util.AbstractMap.SimpleEntry.class, true), null},
                {"class(java.util.Map$Entry+)", Value.of(ArrayList.class, false), null},
                {"method(java.util.List#of(..)@java.lang.SafeVarargs)", Value.of(listVarargs, true), null},
                {"method(@java.lang.SafeVarargs)", Value.of(listVarargs, true), null},
                {"method(java.util.ArrayList#add(..)@java.lang.SafeVarargs)", Value.of(arrayListAdd, false), null},
                {"method(java.util.ArrayList#run(..))", Value.of(arrayListGet, false), null},
                {"class(java.util.List)", Value.of(ArrayList.class, false), null},
                {"class(java.lang.Object+)", Value.of(ArrayList.class, true), null},
                {"method(java.lang.Object+#toString(..))", Value.of(arrayListGet, false), null},
                {"method(java.lang.Object+#toString(..))", Value.of(arrayListToString, true), null},
                {"method(java.lang.Object#toString(..))", Value.of(objectToString, true), null},
                {"method(java.lang.Object#toString(..) && class(java.util.ArrayList))",
                        Value.of(objectToString, Object.class, false), null},
                {"method(java.lang.Object#toString(..) && class(java.util.ArrayList))",
                        Value.of(objectToString, List.class, false), null},
                {"method(java.lang.Object#toString(..) && class(java.util.ArrayList))",
                        Value.of(objectToString, ArrayList.class, true), null},
                {"method(java.lang.Object#toString(..))", Value.of(arrayListToString, false), null},
                {"method(java.lang.Object+#wait())", Value.of(arrayListGet, false), null},
                {"method(java.lang.Object+#wait())", Value.of(arrayListWait, true), null},
                {"method(java.lang.Object#wait())", Value.of(LinkedList.class, linkedListGet, false), null},
                {"method(java.lang.Object#wait())", Value.of(Object.class, arrayListWait, true), null},
                {"method(java.lang.Object#wait())", Value.of(ArrayList.class, arrayListWait, false), null},
                {"method(io.github.dimkich.integration.testing.wait.completion.parser.ByteBuddySelectorParserTest$A#method())",
                        Value.of(A.class.getMethod("method"), true), null},
                // parsing error
                {"method(java.util.ArrayList#get(int))", null, "Expected ')' or '..'"},
                {"class(com.Foo+#run())", null, "Cannot resolve type description for com.Foo"},
                {"method(com.Foo@Ann)", null, "Cannot resolve type description for com.Foo"},
                {"method(java.util.List+#run(int))", null, "Expected ')' or '..'"},
                {"class(@)", null, "Expected identifier at position 8"},
                {"method(#run())", null, "Expected identifier at position 8"},
                {"method(@java.lang.Override(..))", null, "Expected RPAREN at position 27"},
                {"cls(java.util.ArrayList)", null, "Expected 'class' or 'method' at position 3"},
                {"class(java.util.ArrayList+@)", null, "Expected identifier at position 28"},
                {"method(java.util.ArrayList+#get(,))", null, "Expected ')' or '..'"},
                {"method(java.util.ArrayList+#get(*)", null, "Unexpected character"},
                {"class", null, "Expected LPAREN at position 5"},
                {"class()", null, "Expected identifier at position 7"},
                {"method()", null, "Expected identifier at position 8"},
                {"method(java.util.List#of(..)@NonExistent)", null, "Cannot resolve type description for NonExistent"},
                {"class(java.util.ArrayList))", null, "Unexpected trailing input at position 27"},
                {"method(@java.lang.Deprecated))", null, "Unexpected trailing input at position 30"},
                {"class(java.util.ArrayList#add(..))", null, "Expected RPAREN at position 26"},
                {"method(java.util.ArrayList#(..))", null, "Expected method name at position 28"},
                {"method(java.util.List#of(..)@java.lang.SafeVarargs@java.lang.SafeVarargs)", null, "Annotation on class not allowed when method is specified at position 51"},
                {"method(java.util.List#of(..) && method())", null, "Expected 'class' selector after '&&' in method selector at position 38"},
                {"class(java.util.List && method())", null, "'&&' operator not supported in 'class(...)' selector at position 23"},
        };
    }

    @ParameterizedTest
    @MethodSource("parseData")
    void testParse(String input, Value value, String errorMessage) {
        if (errorMessage == null) {
            ByteBuddySelectorResult byteBuddySelectorResult = ByteBuddySelectorParser.parse(input);
            value.assertResult(input, byteBuddySelectorResult);
        } else {
            Exception exception = null;
            try {
                ByteBuddySelectorParser.parse(input);
            } catch (Exception e) {
                exception = e;
            }
            assertNotNull(exception);
            assertTrue(exception.getMessage().contains(errorMessage),
                    "'" + exception.getMessage() + "' must contain '" + errorMessage + "'");
        }
    }
}