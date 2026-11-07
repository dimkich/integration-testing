package io.github.dimkich.integration.testing.serde;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Specificity measure for serde components, used to order the fallback chain.
 *
 * <p>Rank is monotone with respect to assignability: if {@code B} is a subtype of {@code A}, then
 * every supertype of {@code A} is also a supertype of {@code B}, plus {@code A} itself, so
 * {@code rank(B) > rank(A)}. A more specific type therefore always wins over its ancestors.
 * Unrelated types have no defined specificity relation; rank still orders them deterministically
 * (by the number of their supertypes), so the sort is total and independent of bean order.
 * A {@code null} type is a universal one and is the least specific.
 */
final class TypeSpecificity {

    private static final ClassValue<Integer> RANKS = new ClassValue<>() {
        @Override
        protected Integer computeValue(@NonNull Class<?> type) {
            Set<Class<?>> supertypes = new HashSet<>();
            Deque<Class<?>> queue = new ArrayDeque<>();
            queue.add(type);
            while (!queue.isEmpty()) {
                Class<?> current = queue.poll();
                Class<?> superClass = current.getSuperclass();
                if (superClass != null && supertypes.add(superClass)) {
                    queue.add(superClass);
                }
                for (Class<?> iface : current.getInterfaces()) {
                    if (supertypes.add(iface)) {
                        queue.add(iface);
                    }
                }
            }
            return supertypes.size();
        }
    };

    private TypeSpecificity() {
    }

    /**
     * Orders more specific types first: a subtype precedes its ancestors, a concrete type precedes
     * {@code null}.
     */
    static int compare(Class<?> first, Class<?> second) {
        return Integer.compare(rank(second), rank(first));
    }

    /**
     * Orders components by role specificity: an explicit role precedes a role class, which in turn
     * precedes a universal component ({@code null} for both). Two explicit roles are ordered by
     * name; two role classes are ordered by class specificity and then by name.
     */
    static int compareRoles(@Nullable ComponentRole firstRole,
                            @Nullable Class<? extends ComponentRole> firstRoleClass,
                            @Nullable ComponentRole secondRole,
                            @Nullable Class<? extends ComponentRole> secondRoleClass) {
        int firstRank = roleRank(firstRole, firstRoleClass);
        int secondRank = roleRank(secondRole, secondRoleClass);
        if (firstRank != secondRank) {
            return Integer.compare(firstRank, secondRank);
        }
        if (firstRole != null) {
            ComponentRole otherRole = Objects.requireNonNull(secondRole, "secondRole");
            return firstRole.name().compareTo(otherRole.name());
        }
        if (firstRoleClass != null) {
            Class<? extends ComponentRole> otherRoleClass =
                    Objects.requireNonNull(secondRoleClass, "secondRoleClass");
            int result = compare(firstRoleClass, otherRoleClass);
            return result != 0 ? result : firstRoleClass.getName().compareTo(otherRoleClass.getName());
        }
        return 0;
    }

    private static int roleRank(@Nullable ComponentRole role,
                                @Nullable Class<? extends ComponentRole> roleClass) {
        if (role != null) {
            return 0;
        }
        return roleClass != null ? 1 : 2;
    }

    /**
     * Orders a subtype source before its ancestors; unrelated sources are considered equal.
     */
    static int compareSources(Class<?> first, Class<?> second) {
        return Boolean.compare(first.isAssignableFrom(second), second.isAssignableFrom(first));
    }

    private static int rank(Class<?> type) {
        return type == null ? -1 : RANKS.get(type);
    }
}
