package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityAlreadyExistsException;
import ch.time2log.backend.domain.exception.NoRowsAffectedException;
import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.infrastructure.supabase.SupabaseApiException;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.MentorLearnerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MentorDomainService {
    private static final String TABLE = "admin.mentor_learners";

    private final SupabaseService supabaseService;
    private final ProfileDomainService profileDomainService;

    public MentorDomainService(SupabaseService supabaseService, ProfileDomainService profileDomainService) {
        this.supabaseService = supabaseService;
        this.profileDomainService = profileDomainService;
    }

    public List<Profile> getAssignedLearners(UUID organizationId, UUID mentorId) {
        var assignments = supabaseService.getListWithQuery(
                TABLE,
                "organization_id=eq." + organizationId + "&mentor_id=eq." + mentorId,
                MentorLearnerResponse.class
        );
        var ids = assignments.stream().map(MentorLearnerResponse::learner_id).toList();
        return profileDomainService.getByIds(ids);
    }

    public void assignLearner(UUID organizationId, UUID mentorId, UUID learnerId) {
        var body = Map.of(
                "organization_id", organizationId,
                "mentor_id", mentorId,
                "learner_id", learnerId
        );
        try {
            supabaseService.post(TABLE, body, Void.class);
        } catch (SupabaseApiException e) {
            if (e.getStatusCode() == 409) {
                throw new EntityAlreadyExistsException("Learner is already assigned to this mentor");
            }
            throw e;
        }
    }

    public void unassignLearner(UUID organizationId, UUID mentorId, UUID learnerId) {
        int deleted = supabaseService.deleteReturningCount(
                TABLE,
                "organization_id=eq." + organizationId + "&mentor_id=eq." + mentorId + "&learner_id=eq." + learnerId
        );
        if (deleted == 0) {
            throw new NoRowsAffectedException(
                    HttpStatus.FORBIDDEN,
                    "MENTOR_LEARNER_REMOVE_BLOCKED",
                    "Forbidden operation",
                    "Learner could not be unassigned."
            );
        }
    }
}
