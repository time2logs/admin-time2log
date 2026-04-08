package ch.time2log.backend.domain.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SemesterType {
    S1("1"),
    S2("2"),
    S3("3"),
    S4("4"),
    S5("5"),
    S6("6"),
    S7("7"),
    S8("8");

    private final String value;

    SemesterType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SemesterType fromValue(String value) {
        for (SemesterType s : values()) {
            if (s.value.equals(value)) return s;
        }
        throw new IllegalArgumentException("Unknown SemesterType: " + value);
    }
}
