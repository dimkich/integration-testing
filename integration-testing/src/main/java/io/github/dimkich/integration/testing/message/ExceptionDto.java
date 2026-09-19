package io.github.dimkich.integration.testing.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Serializable snapshot of a throwable, used to persist exceptions in test
 * descriptions and storage diffs.
 *
 * <p>Only the exception type name and its message are kept: stack traces are
 * intentionally dropped so that expected results stay stable across runs.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"exceptionType", "message"})
public class ExceptionDto {
    private String exceptionType;
    private String message;

    /**
     * Creates a snapshot of the given throwable.
     *
     * @param throwable the throwable to capture; its simple class name is used
     *                  as the type and {@link Throwable#getMessage()} as the message
     */
    public ExceptionDto(Throwable throwable) {
        this.exceptionType = throwable.getClass().getSimpleName();
        this.message = throwable.getMessage();
    }
}
