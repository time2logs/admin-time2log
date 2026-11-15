package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityAlreadyExistsException;
import ch.time2log.backend.domain.exception.NoRowsAffectedException;
import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.infrastructure.supabase.SupabaseApiException;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.ModeratorLearnerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ModeratorDomainService {
    private static final String TABLE = "admin.moderator_learners";

    private final SupabaseService supabaseService;
    private final ProfileDomainService profileDomainService;

    public ModeratorDomainService(SupabaseService supabaseService, ProfileDomainService profileDomainService) {
        this.supabaseService = supabaseService;
        this.profileDomainService = profileDomainService;
    }

    public List<Profile> getAssignedLearners(UUID organizationId, UUID moderatorId) {
        var assignments = supabaseService.getListWithQuery(
                TABLE,
                "organization_id=eq." + organizationId + "&moderator_id=eq." + moderatorId,
                ModeratorLearnerResponse.class
        );
        var ids = assignments.stream().map(ModeratorLearnerResponse::learner_id).toList();
        return profileDomainService.getByIds(ids);
    }

    public void assignLearner(UUID organizationId, UUID moderatorId, UUID learnerId) {
        var body = Map.of(
                "organization_id", organizationId,
                "moderator_id", moderatorId,
                "learner_id", learnerId
        );
        try {
            supabaseService.post(TABLE, body, Void.class);
        } catch (SupabaseApiException e) {
            if (e.getStatusCode() == 409) {
                throw new EntityAlreadyExistsException("Learner is already assigned to this moderator");
            }
            throw e;
        }
    }

    public void unassignLearner(UUID organizationId, UUID moderatorId, UUID learnerId) {
        int deleted = supabaseService.deleteReturningCount(
                TABLE,
                "organization_id=eq." + organizationId + "&moderator_id=eq." + moderatorId + "&learner_id=eq." + learnerId
        );
        if (deleted == 0) {
            throw new NoRowsAffectedException(
                    HttpStatus.FORBIDDEN,
                    "MODERATOR_LEARNER_REMOVE_BLOCKED",
                    "Forbidden operation",
                    "Learner could not be unassigned."
            );
        }
    }
}
