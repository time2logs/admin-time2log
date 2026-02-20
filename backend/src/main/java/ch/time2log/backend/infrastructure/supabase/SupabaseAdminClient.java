package ch.time2log.backend.infrastructure.supabase;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Component
public class SupabaseAdminClient {
    private static final Logger log = LoggerFactory.getLogger(SupabaseAdminClient.class);
    private final WebClient webClient;

    public SupabaseAdminClient(@Value("${supabase.url}") String supabaseUrl,
                               @Value("${supabase.service-role-key}") String serviceRoleKey) {
        this.webClient = WebClient.builder()
                .baseUrl(supabaseUrl)
                .defaultHeader("apikey", serviceRoleKey)
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .build();
    }

    /**
     * Generates a Supabase invite link for the given email.
     * The returned action_link is the URL the user must visit to accept the invite.
     * In production, send this link via email. Locally, it is logged for testing.
     */
    public Mono<String> generateInviteLink(String email, String redirectTo, Map<String, Object> data) {
        var body = Map.of(
                "type", "invite",
                "email", email,
                "data", data,
                "redirect_to", redirectTo
        );
        return webClient.post()
                .uri("/auth/v1/admin/generate_link")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(b -> {
                                    log.error("Supabase generate_link error: {} - {}", response.statusCode().value(), b);
                                    return Mono.error(new SupabaseApiException(response.statusCode().value(), b));
                                }))
                .bodyToMono(GenerateLinkResponse.class)
                .map(GenerateLinkResponse::actionLink);
    }

    public Mono<Void> deleteUser(UUID userId) {
        return webClient.delete()
                .uri("/auth/v1/admin/users/{id}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("Supabase admin error: {} - {}", response.statusCode().value(), body);
                                    return Mono.error(new SupabaseApiException(response.statusCode().value(), body));
                                }))
                .bodyToMono(Void.class);
    }

    private record GenerateLinkResponse(@JsonProperty("action_link") String actionLink) {}
}
