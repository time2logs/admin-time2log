package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.CreateActivityRequest;
import ch.time2log.backend.api.rest.dto.inbound.UpdateActivityRequest;
import ch.time2log.backend.api.rest.dto.outbound.PreDefinedActivityDto;
import ch.time2log.backend.api.rest.exception.EntityNotCreatedException;
import ch.time2log.backend.api.rest.exception.EntityNotFoundException;
import ch.time2log.backend.api.rest.exception.NoRowsAffectedException;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.PreDefinedActivityResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequestMapping("api/organizations/{organizationId}/activities")
@RestController
public class ActivityController {

    private final SupabaseService supabase;

    public ActivityController(SupabaseService supabase) {
        this.supabase = supabase;
    }

    @GetMapping
    public List<PreDefinedActivityDto> getActivities(@PathVariable UUID organizationId) {
        var activities = supabase.getListWithQuery(
                "app.pre_defined_activities",
                "organization_id=eq." + organizationId,
                PreDefinedActivityResponse.class
        );
        return PreDefinedActivityDto.ofList(activities);
    }

    @PostMapping
    public PreDefinedActivityDto createActivity(@PathVariable UUID organizationId,
                                                @RequestBody CreateActivityRequest request) {
        var body = new HashMap<String, Object>();
        body.put("organization_id", organizationId.toString());
        body.put("key", request.key());
        body.put("label", request.label());
        body.put("description", request.description());
        body.put("category", request.category());

        var created = supabase.post("app.pre_defined_activities", body, PreDefinedActivityResponse[].class);
        if (created == null || created.length == 0) {
            throw new EntityNotCreatedException("Supabase returned no created activity");
        }
        return PreDefinedActivityDto.of(created[0]);
    }

    @PatchMapping("/{activityId}")
    public PreDefinedActivityDto updateActivity(@PathVariable UUID organizationId,
                                                @PathVariable UUID activityId,
                                                @RequestBody UpdateActivityRequest request) {
        var body = new HashMap<String, Object>();
        body.put("key", request.key());
        body.put("label", request.label());
        body.put("description", request.description());
        body.put("category", request.category());
        body.put("is_active", request.isActive());

        var updated = supabase.patch(
                "app.pre_defined_activities",
                "id=eq." + activityId + "&organization_id=eq." + organizationId,
                body,
                PreDefinedActivityResponse[].class
        );
        if (updated == null || updated.length == 0) {
            throw new EntityNotFoundException("Activity not found");
        }
        return PreDefinedActivityDto.of(updated[0]);
    }

    @DeleteMapping("/{activityId}")
    public void deleteActivity(@PathVariable UUID organizationId,
                               @PathVariable UUID activityId) {
        int deleted = supabase.deleteReturningCount(
                "app.pre_defined_activities",
                "id=eq." + activityId + "&organization_id=eq." + organizationId
        );
        if (deleted == 0) {
            throw new NoRowsAffectedException(
                    HttpStatus.NOT_FOUND,
                    "ACTIVITY_NOT_FOUND",
                    "Activity not found",
                    "Activity could not be deleted."
            );
        }
    }
}
