package io.github.xpakx.tictactoe.error;

import io.github.xpakx.tictactoe.error.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<Object> handleException(RuntimeException ex, WebRequest request) {
        HttpStatus status = getStatus(ex);
        return handleExceptionInternal(
                ex,
                constructErrorBody(ex, status),
                new HttpHeaders(),
                status,
                request
        );
    }

    private ErrorResponse constructErrorBody(RuntimeException ex, HttpStatus status) {
        ErrorResponse errorBody = new ErrorResponse();
        errorBody.setMessage(ex.getMessage());
        errorBody.setStatus(status.getReasonPhrase());
        errorBody.setError(status.value());
        return errorBody;
    }

    private HttpStatus getStatus(RuntimeException ex) {
        ResponseStatus status = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
        return status != null ? status.code() : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        return handleExceptionInternal(
                ex,
                constructErrorBody(ex),
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    private ErrorResponse constructErrorBody(ConstraintViolationException ex) {
        ErrorResponse errorBody = new ErrorResponse();
        errorBody.setMessage("Validation failed!");
        errorBody.setStatus(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorBody.setError(HttpStatus.BAD_REQUEST.value());
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .filter(Objects::nonNull)
                .toList();
        errorBody.setErrors(errors.size() > 0 ? errors : null);
        return errorBody;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(
                ex,
                constructErrorBody(ex),
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    private ErrorResponse constructErrorBody(MethodArgumentNotValidException ex) {
        ErrorResponse errorBody = new ErrorResponse();
        errorBody.setMessage("Validation failed!");
        errorBody.setStatus(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorBody.setError(HttpStatus.BAD_REQUEST.value());
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .toList();
        errorBody.setErrors(errors.size() > 0 ? errors : null);
        return errorBody;
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        return handleExceptionInternal(
                ex,
                constructErrorBody(ex, HttpStatus.FORBIDDEN),
                new HttpHeaders(),
                HttpStatus.FORBIDDEN,
                request
        );
    }
}