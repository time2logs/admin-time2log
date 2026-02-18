package ch.time2log.backend.domain;

import ch.time2log.backend.domain.models.Competency;
import ch.time2log.backend.domain.models.CurriculumNode;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.CompetencyResponse;
import ch.time2log.backend.infrastructure.supabase.responses.CurriculumNodeCompetencyResponse;
import ch.time2log.backend.infrastructure.supabase.responses.CurriculumNodeResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CurriculumDomainService {

    private final SupabaseService supabaseService;

    public CurriculumDomainService(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
    }

    public List<CurriculumNode> getNodes(UUID organizationId, UUID professionId) {
        var nodeResponses = supabaseService.getListWithQuery(
                "app.curriculum_nodes",
                "organization_id=eq." + organizationId
                        + "&profession_id=eq." + professionId
                        + "&is_active=eq.true"
                        + "&order=sort_order.asc",
                CurriculumNodeResponse.class
        );

        if (nodeResponses.isEmpty()) return List.of();

        var nodeIds = nodeResponses.stream()
                .map(n -> n.id().toString())
                .collect(Collectors.joining(","));

        var mappings = supabaseService.getListWithQuery(
                "app.curriculum_node_competencies",
                "curriculum_node_id=in.(" + nodeIds + ")",
                CurriculumNodeCompetencyResponse.class
        );

        Map<UUID, List<UUID>> competenciesByNode = mappings.stream()
                .collect(Collectors.groupingBy(
                        CurriculumNodeCompetencyResponse::curriculum_node_id,
                        Collectors.mapping(CurriculumNodeCompetencyResponse::competency_id, Collectors.toList())
                ));

        return nodeResponses.stream()
                .map(n -> CurriculumNode.of(n, competenciesByNode.getOrDefault(n.id(), List.of())))
                .toList();
    }

    public List<Competency> getCompetencies(UUID organizationId, UUID professionId) {
        var responses = supabaseService.getListWithQuery(
                "app.competencies",
                "organization_id=eq." + organizationId + "&profession_id=eq." + professionId,
                CompetencyResponse.class
        );
        return Competency.ofList(responses);
    }
}
