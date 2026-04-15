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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SupabaseAdminClient {
    private static final Logger log = LoggerFactory.getLogger(SupabaseAdminClient.class);
    private final WebClient webClient;
    private final WebClient restClient;
    private final String serviceRoleKey;

    public SupabaseAdminClient(@Value("${supabase.url}") String supabaseUrl,
                               @Value("${supabase.service-role-key}") String serviceRoleKey) {
        this.serviceRoleKey = serviceRoleKey;
        this.webClient = WebClient.builder()
                .baseUrl(supabaseUrl)
                .defaultHeader("apikey", serviceRoleKey)
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .build();
        this.restClient = WebClient.builder()
                .baseUrl(supabaseUrl + "/rest/v1")
                .defaultHeader("Accept", "application/json")
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

    /**
     * Fetches the email address of a Supabase Auth user by their ID.
     */
    public String getUserEmail(UUID userId) {
        return webClient.get()
                .uri("/auth/v1/admin/users/{id}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("Supabase admin get user error: {} - {}", response.statusCode().value(), body);
                                    return Mono.error(new SupabaseApiException(response.statusCode().value(), body));
                                }))
                .bodyToMono(UserResponse.class)
                .map(UserResponse::email)
                .block();
    }

    /**
     * Queries a Supabase table using the service-role key (bypasses RLS).
     * Used for scheduled tasks that run without a user context.
     */
    public <T> List<T> getListWithQuery(String schemaTable, String query, Class<T> elementType) {
        var st = SchemaTable.parse(schemaTable);
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{table}")
                        .query(query)
                        .build(st.table()))
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("Accept-Profile", st.schema())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("Supabase admin query error: {} - {}", response.statusCode().value(), body);
                                    return Mono.error(new SupabaseApiException(response.statusCode().value(), body));
                                }))
                .bodyToFlux(elementType)
                .collectList()
                .block();
    }

    private record GenerateLinkResponse(@JsonProperty("action_link") String actionLink) {}
    private record UserResponse(String email) {}
}
