package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityAlreadyExistsException;
import ch.time2log.backend.domain.exception.NoRowsAffectedException;
import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.infrastructure.supabase.SupabaseApiException;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.MentorLearnerResponse;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationMemberResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorDomainServiceTest {

    @Mock
    private SupabaseService supabaseService;

    @Mock
    private ProfileDomainService profileDomainService;

    @InjectMocks
    private MentorDomainService mentorDomainService;

    private final UUID orgId = UUID.randomUUID();
    private final UUID mentorId = UUID.randomUUID();
    private final UUID learnerId = UUID.randomUUID();

    @Test
    void getAssignedLearners_returnsProfilesOfAssignedLearners() {
        when(supabaseService.getListWithQuery(eq("admin.mentor_learners"),
                eq("organization_id=eq." + orgId + "&mentor_id=eq." + mentorId), eq(MentorLearnerResponse.class)))
                .thenReturn(List.of(new MentorLearnerResponse(orgId, mentorId, learnerId, OffsetDateTime.now())));
        var profile = new Profile(learnerId, "Lea", "Lernende", OffsetDateTime.now(), OffsetDateTime.now(), "user", "normal");
        when(profileDomainService.getByIds(List.of(learnerId))).thenReturn(List.of(profile));

        assertThat(mentorDomainService.getAssignedLearners(orgId, mentorId)).containsExactly(profile);
    }

    @Test
    void assignLearner_happyPath_postsAssignment() {
        stubRole(mentorId, "mentor");
        stubRole(learnerId, "user");

        mentorDomainService.assignLearner(orgId, mentorId, learnerId);

        verify(supabaseService).post(eq("admin.mentor_learners"), any(), eq(Void.class));
    }

    @Test
    void assignLearner_whenTargetIsNotMentor_throwsBadRequest() {
        stubRole(mentorId, "admin");

        assertThatThrownBy(() -> mentorDomainService.assignLearner(orgId, mentorId, learnerId))
                .isInstanceOf(NoRowsAffectedException.class)
                .hasMessageContaining("not a mentor");
        verify(supabaseService, never()).post(any(), any(), any());
    }

    @Test
    void assignLearner_whenLearnerIsNotUser_throwsBadRequest() {
        stubRole(mentorId, "mentor");
        stubRole(learnerId, "moderator");

        assertThatThrownBy(() -> mentorDomainService.assignLearner(orgId, mentorId, learnerId))
                .isInstanceOf(NoRowsAffectedException.class)
                .hasMessageContaining("not a learner");
    }

    @Test
    void assignLearner_whenAlreadyAssigned_throwsAlreadyExists() {
        stubRole(mentorId, "mentor");
        stubRole(learnerId, "user");
        when(supabaseService.post(eq("admin.mentor_learners"), any(), eq(Void.class)))
                .thenThrow(new SupabaseApiException(409, "duplicate"));

        assertThatThrownBy(() -> mentorDomainService.assignLearner(orgId, mentorId, learnerId))
                .isInstanceOf(EntityAlreadyExistsException.class);
    }

    @Test
    void unassignLearner_whenNothingDeleted_throwsForbidden() {
        when(supabaseService.deleteReturningCount(eq("admin.mentor_learners"), any())).thenReturn(0);

        assertThatThrownBy(() -> mentorDomainService.unassignLearner(orgId, mentorId, learnerId))
                .isInstanceOf(NoRowsAffectedException.class);
    }

    @Test
    void unassignLearner_happyPath_deletesAssignment() {
        when(supabaseService.deleteReturningCount(eq("admin.mentor_learners"),
                eq("organization_id=eq." + orgId + "&mentor_id=eq." + mentorId + "&learner_id=eq." + learnerId)))
                .thenReturn(1);

        mentorDomainService.unassignLearner(orgId, mentorId, learnerId);
    }

    private void stubRole(UUID userId, String role) {
        when(supabaseService.getListWithQuery(eq("admin.organization_members"),
                eq("organization_id=eq." + orgId + "&user_id=eq." + userId), eq(OrganizationMemberResponse.class)))
                .thenReturn(List.of(new OrganizationMemberResponse(userId, orgId, role, OffsetDateTime.now())));
    }
}
