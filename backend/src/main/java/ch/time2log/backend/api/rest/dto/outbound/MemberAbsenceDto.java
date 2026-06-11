package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.domain.models.MemberAbsence;

import java.util.List;
import java.util.UUID;

public record MemberAbsenceDto(
        UUID id,
        String absenceTypeId,
        String startDate,
        String endDate,
        String rrule,
        boolean isRecurring,
        double dayFraction,
        String notes,
        String currentSemester
) {
    public static MemberAbsenceDto of(MemberAbsence a) {
        return new MemberAbsenceDto(
                a.id(),
                a.absenceTypeId(),
                a.startDate(),
                a.endDate(),
                a.rrule(),
                a.isRecurring(),
                a.dayFraction(),
                a.notes(),
                a.currentSemester()
        );
    }

    public static List<MemberAbsenceDto> ofList(List<MemberAbsence> list) {
        return list.stream().map(MemberAbsenceDto::of).toList();
    }
}
