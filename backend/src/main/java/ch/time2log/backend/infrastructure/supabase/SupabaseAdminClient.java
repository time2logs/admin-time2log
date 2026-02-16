package ch.time2log.backend.infrastructure.supabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class SupabaseAdminClient {
    private static final Logger log = LoggerFactory.getLogger(SupabaseAdminClient.class);
    private final WebClient webClient;

    public SupabaseAdminClient(@Value("${supabase.url}") String supabaseUrl,
                               @Value("${supabase.service-role-key}") String serviceRoleKey) {
        this.webClient = WebClient.builder()
                .baseUrl(supabaseUrl + "/auth/v1/admin")
                .defaultHeader("apikey", serviceRoleKey)
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .build();
    }

    public Mono<Void> deleteUser(UUID userId) {
        return webClient.delete()
                .uri("/users/{id}", userId)
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
}
