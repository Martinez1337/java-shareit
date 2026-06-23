package ru.practicum.shareit.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlerTest {
    private final ErrorHandler errorHandler = new ErrorHandler();

    @Test
    void shouldMapApplicationExceptionsToErrorResponses() {
        assertThat(errorHandler.handleBadRequest(new BadRequestException("bad")).error()).isEqualTo("bad");
        assertThat(errorHandler.handleNotFound(new NotFoundException("missing")).error()).isEqualTo("missing");
        assertThat(errorHandler.handleConflict(new ConflictException("duplicate")).error()).isEqualTo("duplicate");
        assertThat(errorHandler.handleException(new RuntimeException("boom")).error()).isEqualTo("boom");
    }

    @Test
    void shouldMapValidationExceptionsToErrorResponses() {
        assertThat(errorHandler.handleValidation(new ConstraintViolationException("invalid", null)).error())
                .contains("invalid");
        assertThat(errorHandler.handleValidation(new MethodArgumentTypeMismatchException(
                "x",
                String.class,
                "id",
                null,
                new IllegalArgumentException("wrong")
        )).error()).contains("wrong");
    }
}
