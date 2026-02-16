package ch.time2log.backend.api.rest.exception;

import org.springframework.http.HttpStatus;

public class NoRowsAffectedException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String title;

    public NoRowsAffectedException(HttpStatus status, String code, String title, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }
}
