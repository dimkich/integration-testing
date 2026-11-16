package io.github.dimkich.integration.testing.serde;

import io.github.dimkich.integration.testing.util.TypeSpecificity;
import org.springframework.lang.Nullable;

import java.util.Objects;

/**
 * Specificity of serde roles, used to order components within one request.
 *
 * <p>An explicit role is more specific than a role class, which in turn is more specific than a
 * universal component. Roles of one kind are ordered by name, role classes by class specificity
 * and then by name. Type specificity itself is provided by
 * {@link io.github.dimkich.integration.testing.util.TypeSpecificity}.
 */
final class SerdeSpecificity {

    private SerdeSpecificity() {
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
            int result = TypeSpecificity.compare(firstRoleClass, otherRoleClass);
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
}
