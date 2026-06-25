package ch.time2log.backend.domain.models;

import ch.time2log.backend.infrastructure.supabase.responses.AbsenceResponse;

import java.util.List;
import java.util.UUID;

public record MemberAbsence(
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
    public static MemberAbsence of(AbsenceResponse r) {
        return new MemberAbsence(
                r.id(),
                r.absence_type_id(),
                r.start_date(),
                r.end_date(),
                r.rrule(),
                r.is_recurring(),
                r.day_fraction() != null ? r.day_fraction() : 1.0,
                r.notes(),
                r.current_semester()
        );
    }

    public static List<MemberAbsence> ofList(List<AbsenceResponse> list) {
        return list.stream().map(MemberAbsence::of).toList();
    }
}
