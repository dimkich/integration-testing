package io.github.dimkich.integration.testing.openapi;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Configures an integration test that verifies a REST API using an OpenAPI specification.
 * <p>
 * This annotation bootstraps the {@link io.github.dimkich.integration.testing.IntegrationTesting}
 * environment and provides metadata about the tested API, such as the controller class,
 * error response type and bean names to use for HTTP communication.
 * </p>
 *
 * <p>
 * Can be declared multiple times on a single test class via {@link TestOpenAPI.List}.
 * </p>
 */
@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@IntegrationTesting
@Repeatable(TestOpenAPI.List.class)
public @interface TestOpenAPI {
    /**
     * Class that defines the API under test, typically a Spring MVC or WebFlux controller
     * or a class generated from the OpenAPI definition.
     */
    Class<?> apiClass();

    /**
     * Type representing the error response body returned by the API.
     * <p>
     * By default, {@link SpringErrorDto} is used to map Spring error responses.
     * </p>
     */
    Class<?> errorResponseClass() default SpringErrorDto.class;

    /**
     * Name of the Spring bean that implements the API interface.
     * <p>
     * Leave empty to let the framework resolve the bean by type.
     * </p>
     */
    String apiBeanName() default "";

    /**
     * Name of the Spring {@code RestTemplate} bean to use for sending HTTP requests.
     * <p>
     * Leave empty to use the default {@code RestTemplate} provided by the test context.
     * </p>
     */
    String restTemplateBeanName() default "";

    /**
     * Base path for all API calls during the test, e.g. {@code "/api"}.
     * <p>
     * If empty, the framework's default base path is used.
     * </p>
     */
    String basePath() default "";

    /**
     * Container annotation that allows repeating {@link TestOpenAPI} on the same test class.
     */
    @Inherited
    @Target(TYPE)
    @Retention(RUNTIME)
    @interface List {
        /**
         * The {@link TestOpenAPI} declarations associated with the annotated test class.
         */
        TestOpenAPI[] value();
    }
}
