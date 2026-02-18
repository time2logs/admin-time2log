package ch.time2log.backend.api.rest.exception;

import ch.time2log.backend.domain.exception.EntityAlreadyExistsException;
import ch.time2log.backend.domain.exception.EntityNotCreatedException;
import ch.time2log.backend.domain.exception.EntityNotFoundException;
import ch.time2log.backend.domain.exception.NoRowsAffectedException;
import ch.time2log.backend.infrastructure.supabase.SupabaseApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleEntityNotFound(EntityNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problemDetail.setTitle("Entity not found");
        problemDetail.setProperty("path", request.getRequestURI());
        return problemDetail;
    }

    @ExceptionHandler(EntityNotCreatedException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ProblemDetail handleEntityNotCreated(EntityNotCreatedException exception, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
        problemDetail.setTitle("Entity not created");
        problemDetail.setProperty("path", request.getRequestURI());
        return problemDetail;
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleEntityAlreadyExists(EntityAlreadyExistsException exception, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Entity already exists");
        problemDetail.setProperty("path", request.getRequestURI());
        return problemDetail;
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleIllegalState(IllegalStateException exception, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problemDetail.setTitle("Unauthorized");
        problemDetail.setProperty("path", request.getRequestURI());
        return problemDetail;
    }

    @ExceptionHandler(SupabaseApiException.class)
    public ProblemDetail handleSupabaseApiException(SupabaseApiException exception, HttpServletRequest request) {
        HttpStatusCode status = resolveStatus(exception);
        boolean forbidden = HttpStatus.FORBIDDEN.equals(status);
        String detail = forbidden
                ? "You are not allowed to perform this operation."
                : exception.getMessage();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(forbidden ? "Forbidden operation" : "Supabase request failed");
        problemDetail.setProperty("path", request.getRequestURI());
        problemDetail.setProperty("supabaseResponse", exception.getResponseBody());
        if (forbidden) {
            problemDetail.setProperty("code", "FORBIDDEN_OPERATION");
        }
        return problemDetail;
    }

    @ExceptionHandler(NoRowsAffectedException.class)
    public ProblemDetail handleNoRowsAffected(NoRowsAffectedException exception, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problemDetail.setTitle(exception.getTitle());
        problemDetail.setProperty("path", request.getRequestURI());
        problemDetail.setProperty("code", exception.getCode());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGenericException(Exception exception, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal server error");
        problemDetail.setProperty("path", request.getRequestURI());
        return problemDetail;
    }

    private HttpStatusCode resolveStatus(SupabaseApiException exception) {
        HttpStatus resolved = HttpStatus.resolve(exception.getStatusCode());
        if (resolved == HttpStatus.UNAUTHORIZED || resolved == HttpStatus.FORBIDDEN) {
            return resolved;
        }
        if (isNotFoundError(exception.getResponseBody())) {
            return HttpStatus.NOT_FOUND;
        }
        if (isRlsPermissionError(exception.getResponseBody())) {
            return HttpStatus.FORBIDDEN;
        }
        return resolved != null ? resolved : HttpStatus.BAD_GATEWAY;
    }

    private boolean isRlsPermissionError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }

        String normalized = responseBody.toLowerCase();
        return normalized.contains("row-level security")
                || normalized.contains("permission denied")
                || normalized.contains("\"code\":\"42501\"")
                || normalized.contains("new row violates row-level security policy");
    }

    private boolean isNotFoundError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }

        String normalized = responseBody.toLowerCase();
        return normalized.contains("\"code\":\"p0002\"")
                || normalized.contains("not found");
    }
}
