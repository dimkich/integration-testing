package io.github.dimkich.integration.testing.openapi;

import lombok.Data;
import org.springframework.validation.FieldError;

import java.util.List;

@Data
public class SpringErrorDto {
    private Integer status;
    private String error;
    private String message;
    private List<FieldError> errors;
    private String path;
}
