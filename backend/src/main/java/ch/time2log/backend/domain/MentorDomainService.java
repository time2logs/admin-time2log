package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityAlreadyExistsException;
import ch.time2log.backend.domain.exception.NoRowsAffectedException;
import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.infrastructure.supabase.SupabaseApiException;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.MentorLearnerResponse;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationMemberResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Zuweisung von Lernenden zu Mentoren. Die Sichtbarkeit selbst wird per RLS
 * (admin.can_view_learner) in der DB durchgesetzt; hier wird nur verwaltet.
 */
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
        requireRole(organizationId, mentorId, "mentor", "MENTOR_ROLE_REQUIRED", "User is not a mentor of this organization.");
        requireRole(organizationId, learnerId, "user", "LEARNER_ROLE_REQUIRED", "User is not a learner of this organization.");

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

    private void requireRole(UUID organizationId, UUID userId, String role, String code, String message) {
        var memberships = supabaseService.getListWithQuery(
                "admin.organization_members",
                "organization_id=eq." + organizationId + "&user_id=eq." + userId,
                OrganizationMemberResponse.class
        );
        boolean hasRole = memberships.stream().anyMatch(m -> role.equals(m.user_role()));
        if (!hasRole) {
            throw new NoRowsAffectedException(HttpStatus.BAD_REQUEST, code, "Invalid assignment", message);
        }
    }
}
