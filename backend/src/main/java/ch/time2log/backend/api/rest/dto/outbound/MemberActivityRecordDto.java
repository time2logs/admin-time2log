package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.domain.models.MemberActivityRecord;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public record MemberActivityRecordDto(
        UUID id,
        String entryDate,
        UUID curriculumActivityId,
        String activityLabel,
        int hours,
        String notes,
        Integer rating,
        UUID teamId
) {
    public static MemberActivityRecordDto of(MemberActivityRecord r) {
        return new MemberActivityRecordDto(r.id(), r.entryDate(), r.curriculumActivityId(), r.activityLabel(), r.hours(), r.notes(), r.rating(), r.teamId());
    }

    public static List<MemberActivityRecordDto> ofList(List<MemberActivityRecord> list) {
        return list.stream().map(MemberActivityRecordDto::of).toList();
    }
}
