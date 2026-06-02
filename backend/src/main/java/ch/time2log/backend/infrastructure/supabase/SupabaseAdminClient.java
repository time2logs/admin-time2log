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

    /**
     * Looks up an existing Supabase Auth user id by email.
     * Returns null if no user with that email exists.
     * Used during invite-driven onboarding, where the auth user is already
     * created by generate_link at invite time.
     */
    public UUID getUserIdByEmail(String email) {
        var response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/auth/v1/admin/users")
                        .queryParam("email", email)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, r ->
                        r.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(b -> {
                                    log.error("Supabase list users error: {} - {}", r.statusCode().value(), b);
                                    return Mono.error(new SupabaseApiException(r.statusCode().value(), b));
                                }))
                .bodyToMono(AdminUsersListResponse.class)
                .block();

        if (response == null || response.users() == null) {
            return null;
        }
        return response.users().stream()
                .filter(u -> u.email() != null && u.email().equalsIgnoreCase(email))
                .map(AdminUser::id)
                .findFirst()
                .orElse(null);
    }

    private record GenerateLinkResponse(@JsonProperty("action_link") String actionLink) {}
    private record UserResponse(String email) {}
    private record CreateUserResponse(UUID id) {}
    private record AdminUsersListResponse(List<AdminUser> users) {}
    private record AdminUser(UUID id, String email) {}


    /**
     * Creates a new Supabase Auth user with the given email.
     * email_confirm=true skips the confirmation email (invite-driven onboarding).
     * Returns the new user's id.
     */
    public UUID createUser(String email, boolean emailConfirm) {
        var body = Map.of(
                "email", email,
                "email_confirm", emailConfirm
        );
        return webClient.post()
                .uri("/auth/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(b -> {
                                    log.error("Supabase create user error: {} - {}", response.statusCode().value(), b);
                                    return Mono.error(new SupabaseApiException(response.statusCode().value(), b));
                                }))
                .bodyToMono(CreateUserResponse.class)
                .map(CreateUserResponse::id)
                .block();
    }

    /**
     * Sets the password and first/last name metadata of an existing Auth user.
     */
    public void updateUser(UUID userId, String password, String firstName, String lastName) {
        var body = Map.of(
                "password", password,
                "user_metadata", Map.of(
                        "first_name", firstName,
                        "last_name", lastName
                )
        );
        webClient.put()
                .uri("/auth/v1/admin/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(b -> {
                                    log.error("Supabase update user error: {} - {}", response.statusCode().value(), b);
                                    return Mono.error(new SupabaseApiException(response.statusCode().value(), b));
                                }))
                .bodyToMono(Void.class)
                .block();
    }

    /**
     * Calls a Postgres function via PostgREST RPC using the service-role key (bypasses RLS).
     * The schema (e.g. "admin") is selected via the Content-Profile header.
     * Pass Void.class as responseType for functions that return void.
     */
    public <T> T callRpc(String schema, String functionName, Map<String, Object> params, Class<T> responseType) {
        return restClient.post()
                .uri("/rpc/{fn}", functionName)
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("Content-Profile", schema)
                .header("Accept-Profile", schema)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(params)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(b -> {
                                    log.error("Supabase rpc {} error: {} - {}", functionName, response.statusCode().value(), b);
                                    return Mono.error(new SupabaseApiException(response.statusCode().value(), b));
                                }))
                .bodyToMono(responseType)
                .block();
    }
}
