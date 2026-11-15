package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityAlreadyExistsException;
import ch.time2log.backend.domain.exception.NoRowsAffectedException;
import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.infrastructure.supabase.SupabaseApiException;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.ModeratorLearnerResponse;
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
class ModeratorDomainServiceTest {

    @Mock
    private SupabaseService supabaseService;

    @Mock
    private ProfileDomainService profileDomainService;

    @InjectMocks
    private ModeratorDomainService moderatorDomainService;

    private final UUID orgId = UUID.randomUUID();
    private final UUID moderatorId = UUID.randomUUID();
    private final UUID learnerId = UUID.randomUUID();

    @Test
    void getAssignedLearners_returnsProfilesOfAssignedLearners() {
        when(supabaseService.getListWithQuery(eq("admin.moderator_learners"),
                eq("organization_id=eq." + orgId + "&moderator_id=eq." + moderatorId), eq(ModeratorLearnerResponse.class)))
                .thenReturn(List.of(new ModeratorLearnerResponse(orgId, moderatorId, learnerId, OffsetDateTime.now())));
        var profile = new Profile(learnerId, "Lea", "Lernende", OffsetDateTime.now(), OffsetDateTime.now(), "user", "normal");
        when(profileDomainService.getByIds(List.of(learnerId))).thenReturn(List.of(profile));

        assertThat(moderatorDomainService.getAssignedLearners(orgId, moderatorId)).containsExactly(profile);
    }

    @Test
    void assignLearner_happyPath_postsAssignment() {

        moderatorDomainService.assignLearner(orgId, moderatorId, learnerId);

        verify(supabaseService).post(eq("admin.moderator_learners"), any(), eq(Void.class));
    }

    @Test
    void assignLearner_whenAlreadyAssigned_throwsAlreadyExists() {
        when(supabaseService.post(eq("admin.moderator_learners"), any(), eq(Void.class)))
                .thenThrow(new SupabaseApiException(409, "duplicate"));

        assertThatThrownBy(() -> moderatorDomainService.assignLearner(orgId, moderatorId, learnerId))
                .isInstanceOf(EntityAlreadyExistsException.class);
    }

    @Test
    void unassignLearner_whenNothingDeleted_throwsForbidden() {
        when(supabaseService.deleteReturningCount(eq("admin.moderator_learners"), any())).thenReturn(0);

        assertThatThrownBy(() -> moderatorDomainService.unassignLearner(orgId, moderatorId, learnerId))
                .isInstanceOf(NoRowsAffectedException.class);
    }

    @Test
    void unassignLearner_happyPath_deletesAssignment() {
        when(supabaseService.deleteReturningCount(eq("admin.moderator_learners"),
                eq("organization_id=eq." + orgId + "&moderator_id=eq." + moderatorId + "&learner_id=eq." + learnerId)))
                .thenReturn(1);

        moderatorDomainService.unassignLearner(orgId, moderatorId, learnerId);
    }
}
