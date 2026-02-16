package ch.time2log.backend.infrastructure.supabase;

public class SupabaseApiException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    public SupabaseApiException(int statusCode, String responseBody) {
        super("Supabase request failed with status " + statusCode);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
