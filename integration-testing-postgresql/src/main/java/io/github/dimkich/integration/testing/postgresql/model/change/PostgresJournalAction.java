package io.github.dimkich.integration.testing.postgresql.model.change;

import lombok.RequiredArgsConstructor;

/**
 * Action recorded in the audit journal.
 */
@RequiredArgsConstructor
public enum PostgresJournalAction {
    INSERT('I'),
    UPDATE('U'),
    DELETE('D');

    private final char code;

    public static PostgresJournalAction fromCode(String code) {
        if (code == null || code.length() != 1) {
            throw new IllegalArgumentException("Unknown audit action: " + code);
        }
        return switch (code.charAt(0)) {
            case 'I' -> INSERT;
            case 'U' -> UPDATE;
            case 'D' -> DELETE;
            default -> throw new IllegalArgumentException("Unknown audit action: " + code);
        };
    }
}
