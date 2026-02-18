package ch.time2log.backend.api.rest.dto.outbound;

public enum DailyMemberReportStatus {
    REPORTED("reported"),
    BAD_RATING("bad_rating"),
    MISSING("missing");

    private final String value;

    DailyMemberReportStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
