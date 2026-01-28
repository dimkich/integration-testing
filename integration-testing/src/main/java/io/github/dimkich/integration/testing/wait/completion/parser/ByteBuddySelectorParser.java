package io.github.dimkich.integration.testing.wait.completion.parser;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Parser for selector expressions used to build ByteBuddy matchers
 * for wait-completion instrumentation.
 * <p>
 * Grammar:
 * <expression>     ::= <select> "(" <target> ")"
 * <select>         ::= "class" | "method"
 * <target>         ::= <simple_target> | <combined_target>
 * <simple_target>  ::= <annotation_ref>
 * | <class_ref> [ "+" ] [ <annotation_ref> ]
 * | <class_ref> [ "+" ] [ <method_ref> ] [ <annotation_ref> ]
 * <combined_target> ::= <target> "&&" <target>
 * <annotation_ref> ::= "@" <identifier>
 * <class_ref>      ::= <identifier>
 * <method_ref>     ::= "#" <identifier> "(" <arg_pattern> ")"
 * <arg_pattern>    ::= "()" | "(..)"
 * <identifier>     ::= [a-zA-Z_$][a-zA-Z0-9_$.]*
 */
public class ByteBuddySelectorParser {

    private static final TypePool TYPE_POOL = TypePool.Default.ofSystemLoader();

    /**
     * Parses a selector expression into a {@link ByteBuddySelectorResult}.
     * <p>
     * The supported grammar is documented in the class-level Javadoc.
     *
     * @param input selector expression, e.g. {@code "method(com.example.Service#doWork(..))"}
     * @return parsed selector containing type and/or method matchers
     * @throws IllegalArgumentException if the expression is syntactically invalid
     *                                  or referenced types cannot be resolved
     */
    public static ByteBuddySelectorResult parse(String input) {
        ByteBuddySelectorTokenizer t = new ByteBuddySelectorTokenizer(input);

        if (t.nextToken() != ByteBuddySelectorTokenizer.Token.SELECT) {
            throw error("Expected 'class' or 'method'", t);
        }
        String select = t.getLexeme();
        boolean isClassOnly = "class".equals(select);
        t.nextToken();

        expect(t, ByteBuddySelectorTokenizer.Token.LPAREN);

        ByteBuddySelectorResult result;
        if (isClassOnly) {
            result = parseClassTarget(t);
        } else {
            result = parseMethodTarget(t);
        }

        expect(t, ByteBuddySelectorTokenizer.Token.RPAREN);
        if (t.getCurrentToken() != ByteBuddySelectorTokenizer.Token.EOF) {
            throw error("Unexpected trailing input", t);
        }

        return result;
    }

    private static ByteBuddySelectorResult parseClassTarget(ByteBuddySelectorTokenizer t) {
        ByteBuddySelectorResult left = parseSimpleClassTarget(t);

        if (t.getCurrentToken() == ByteBuddySelectorTokenizer.Token.AND) {
            throw error("'&&' operator not supported in 'class(...)' selector", t);
        }

        return left;
    }

    private static ByteBuddySelectorResult parseSimpleClassTarget(ByteBuddySelectorTokenizer t) {
        if (t.getCurrentToken() == ByteBuddySelectorTokenizer.Token.AT) {
            t.nextToken();
            TypeDescription annType = parseFullyQualifiedName(t);
            return new ByteBuddySelectorResult(isAnnotatedWith(annType), null);
        }

        TypeDescription targetType = parseFullyQualifiedName(t);
        boolean includeSubtypes = false;

        if (t.getCurrentToken() == ByteBuddySelectorTokenizer.Token.PLUS) {
            includeSubtypes = true;
            t.nextToken();
        }

        ElementMatcher.Junction<TypeDescription> typeMatcher =
                includeSubtypes ? isSubTypeOf(targetType) : is(targetType);

        if (t.getCurrentToken() == ByteBuddySelectorTokenizer.Token.AT) {
            t.nextToken();
            TypeDescription annType = parseFullyQualifiedName(t);
            typeMatcher = typeMatcher.and(isAnnotatedWith(annType));
        }

        return new ByteBuddySelectorResult(typeMatcher, null);
    }

    private static ByteBuddySelectorResult parseMethodTarget(ByteBuddySelectorTokenizer t) {
        ByteBuddySelectorResult leftResult = parseSimpleMethodTarget(t);

        if (t.getCurrentToken() == ByteBuddySelectorTokenizer.Token.AND) {

            if (t.nextToken() != ByteBuddySelectorTokenizer.Token.SELECT || !"class".equals(t.getLexeme())) {
                throw error("Expected 'class' selector after '&&' in method selector", t);
            }
            t.nextToken();
            expect(t, ByteBuddySelectorTokenizer.Token.LPAREN);

            ByteBuddySelectorResult rightResult = parseClassTarget(t);
            expect(t, ByteBuddySelectorTokenizer.Token.RPAREN);

            return new ByteBuddySelectorResult(
                    leftResult.getTypeMatcher(),
                    leftResult.getMethodMatcher(),
                    rightResult.getTypeMatcher()
            );
        }

        return leftResult;
    }

    private static ByteBuddySelectorResult parseSimpleMethodTarget(ByteBuddySelectorTokenizer t) {
        if (t.getCurrentToken() == ByteBuddySelectorTokenizer.Token.AT) {
            t.nextToken();
            TypeDescription annType = parseFullyQualifiedName(t);
            return new ByteBuddySelectorResult(any(), isAnnotatedWith(annType));
        }

        TypeDescription targetType = parseFullyQualifiedName(t);
        boolean includeSubtypes = false;

        if (t.getCurrentToken() == ByteBuddySelectorTokenizer.Token.PLUS) {
            includeSubtypes = true;
            t.nextToken();
        }

        ElementMatcher.Junction<TypeDescription> typeMatcher =
                includeSubtypes ? isSubTypeOf(targetType) : is(targetType);

        ElementMatcher.Junction<MethodDescription> methodMatcher = null;

        if (t.getCurrentToken() == ByteBuddySelectorTokenizer.Token.HASH) {
            t.nextToken();

            if (t.getCurrentToken() != ByteBuddySelectorTokenizer.Token.IDENTIFIER) {
                throw error("Expected method name", t);
            }
            String methodName = t.getLexeme();
            t.nextToken();

            boolean takesZeroArgs = false;

            expect(t, ByteBuddySelectorTokenizer.Token.LPAREN);
            if (t.getCurrentToken() == ByteBuddySelectorTokenizer.Token.RPAREN) {
                takesZeroArgs = true;
                t.nextToken();
            } else if (t.getCurrentToken() == ByteBuddySelectorTokenizer.Token.DOTDOT) {
                t.nextToken();
                expect(t, ByteBuddySelectorTokenizer.Token.RPAREN);
            } else {
                throw error("Expected ')' or '..'", t);
            }

            ElementMatcher.Junction<MethodDescription> mm = named(methodName);
            if (takesZeroArgs) {
                mm = mm.and(takesArguments(0));
            }

            if (t.getCurrentToken() == ByteBuddySelectorTokenizer.Token.AT) {
                t.nextToken();
                TypeDescription annType = parseFullyQualifiedName(t);
                mm = mm.and(isAnnotatedWith(annType));
            }

            methodMatcher = mm;
        }

        if (t.getCurrentToken() == ByteBuddySelectorTokenizer.Token.AT) {
            throw error("Annotation on class not allowed when method is specified", t);
        }

        return new ByteBuddySelectorResult(typeMatcher, methodMatcher);
    }

    private static TypeDescription parseFullyQualifiedName(ByteBuddySelectorTokenizer t) {
        if (t.getCurrentToken() != ByteBuddySelectorTokenizer.Token.IDENTIFIER) {
            throw error("Expected identifier", t);
        }
        String lexeme = t.getLexeme();
        t.nextToken();
        try {
            return TYPE_POOL.describe(lexeme).resolve();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot resolve type description for " + lexeme, e);
        }
    }

    private static void expect(ByteBuddySelectorTokenizer t, ByteBuddySelectorTokenizer.Token expected) {
        if (t.getCurrentToken() != expected) {
            throw error("Expected " + expected, t);
        }
        t.nextToken();
    }

    private static IllegalArgumentException error(String msg, ByteBuddySelectorTokenizer t) {
        return new IllegalArgumentException(msg + " at position " + t.getPos());
    }
}