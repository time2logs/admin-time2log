package ch.time2log.backend.api.rest.exception;

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

    @ExceptionHandler(ProfileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleProfileNotFound(ProfileNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problemDetail.setTitle("Profile not found");
        problemDetail.setProperty("path", request.getRequestURI());
        return problemDetail;
    }

    @ExceptionHandler(SupabaseApiException.class)
    public ProblemDetail handleSupabaseApiException(SupabaseApiException exception, HttpServletRequest request) {
        HttpStatusCode status = HttpStatus.resolve(exception.getStatusCode());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problemDetail.setTitle("Supabase request failed");
        problemDetail.setProperty("path", request.getRequestURI());
        problemDetail.setProperty("supabaseResponse", exception.getResponseBody());
        return problemDetail;
    }
}
