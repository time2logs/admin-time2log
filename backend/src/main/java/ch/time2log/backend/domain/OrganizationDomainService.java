package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityAlreadyExistsException;
import ch.time2log.backend.domain.exception.EntityNotCreatedException;
import ch.time2log.backend.domain.exception.NoRowsAffectedException;
import ch.time2log.backend.domain.models.Organization;
import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.infrastructure.supabase.SupabaseApiException;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationMemberResponse;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrganizationDomainService {
    private final SupabaseService supabaseService;
    private final ProfileDomainService profileDomainService;

    public OrganizationDomainService(SupabaseService supabaseService, ProfileDomainService profileDomainService) {
        this.supabaseService = supabaseService;
        this.profileDomainService = profileDomainService;
    }

    public List<Organization> getOrganizations() {
        var responses = supabaseService.getList("admin.organizations", OrganizationResponse.class);
        return Organization.ofList(responses);
    }

    public Organization createOrganization(String name) {
        var created = supabaseService.post("admin.organizations", Map.of("name", name), OrganizationResponse[].class);
        if (created == null || created.length == 0) {
            throw new EntityNotCreatedException("Supabase returned no created organization");
        }
        return Organization.of(created[0]);
    }

    public void deleteOrganization(UUID id) {
        int deleted = supabaseService.deleteReturningCount("admin.organizations", "id=eq." + id);
        if (deleted == 0) {
            throw new NoRowsAffectedException(
                    HttpStatus.FORBIDDEN,
                    "ORGANIZATION_DELETE_BLOCKED",
                    "Forbidden operation",
                    "Organization could not be deleted."
            );
        }
    }

    public void inviteToOrganization(UUID organizationId, UUID userId, String userRole) {
        var body = Map.of(
                "user_id", userId,
                "organization_id", organizationId,
                "user_role", userRole
        );
        try {
            supabaseService.post("admin.organization_members", body, Void.class);
        } catch (SupabaseApiException e) {
            if (e.getStatusCode() == 409) {
                throw new EntityAlreadyExistsException("Member already exists in this organization");
            }
            throw e;
        }
    }

    public void removeOrganizationMember(UUID organizationId, UUID userId) {
        int deleted = supabaseService.deleteReturningCount(
                "admin.organization_members",
                "organization_id=eq." + organizationId + "&user_id=eq." + userId
        );
        if (deleted == 0) {
            throw new NoRowsAffectedException(
                    HttpStatus.FORBIDDEN,
                    "MEMBER_REMOVE_BLOCKED",
                    "Forbidden operation",
                    "Organization member could not be removed."
            );
        }
    }

    public List<Profile> getOrganizationMemberProfiles(UUID organizationId) {
        var members = supabaseService.getListWithQuery(
                "admin.organization_members",
                "organization_id=eq." + organizationId,
                OrganizationMemberResponse.class
        );
        var ids = members.stream().map(OrganizationMemberResponse::user_id).toList();
        return profileDomainService.getByIds(ids);
    }
}
