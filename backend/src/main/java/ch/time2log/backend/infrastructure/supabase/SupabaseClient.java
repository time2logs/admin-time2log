package ch.time2log.backend.infrastructure.supabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class SupabaseClient {
    private static final Logger log = LoggerFactory.getLogger(SupabaseClient.class);
    private final WebClient webClient;

    public SupabaseClient(@Value("${supabase.url}") String supabaseUrl,
                          @Value("${supabase.anon-key}") String anonKey) {
        this.webClient = WebClient.builder()
                .baseUrl(supabaseUrl + "/rest/v1")
                .defaultHeader("apikey", anonKey)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    private static String toBearerAuth(String userToken) {
        if (userToken == null) return null;
        String t = userToken.trim();
        if (t.isEmpty()) return null;
        return t.regionMatches(true, 0, "Bearer ", 0, 7) ? t : "Bearer " + t;
    }

    private static String requireQuery(String query, String operation) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(operation + " requires a non-empty query filter");
        }
        return query;
    }

    private Mono<? extends Throwable> mapError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> {
                    int statusCode = response.statusCode().value();
                    log.error("Supabase error: {} - {}", statusCode, body);
                    return Mono.error(new SupabaseApiException(statusCode, body));
                });
    }

    public <T> Mono<T> get(String schemaTable, String userToken, Class<T> responseType) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.get()
                .uri("/{table}", st.table())
                .header("Authorization", toBearerAuth(userToken))
                .header("Accept-Profile", st.schema())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapError)
                .bodyToMono(responseType);
    }

    public <T> Mono<List<T>> getList(String schemaTable, String userToken, Class<T> elementType) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.get()
                .uri("/{table}", st.table())
                .header("Authorization", toBearerAuth(userToken))
                .header("Accept-Profile", st.schema())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapError)
                .bodyToFlux(elementType)
                .collectList();
    }

    public <T> Mono<T> getWithQuery(String schemaTable, String query, String userToken, Class<T> responseType) {
        var st = SchemaTable.parse(schemaTable);
        String safeQuery = requireQuery(query, "GET");
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{table}")
                        .query(safeQuery)
                        .build(st.table()))
                .header("Authorization", toBearerAuth(userToken))
                .header("Accept-Profile", st.schema())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapError)
                .bodyToMono(responseType);
    }

    public <T> Mono<List<T>> getListWithQuery(String schemaTable, String query, String userToken, Class<T> elementType) {
        var st = SchemaTable.parse(schemaTable);
        String safeQuery = requireQuery(query, "GET");
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{table}")
                        .query(safeQuery)
                        .build(st.table()))
                .header("Authorization", toBearerAuth(userToken))
                .header("Accept-Profile", st.schema())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapError)
                .bodyToFlux(elementType)
                .collectList();
    }

    public <T> Mono<T> post(String schemaTable, Object body, String userToken, Class<T> responseType) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.post()
                .uri("/{table}", st.table())
                .header("Authorization", toBearerAuth(userToken))
                .header("Content-Profile", st.schema())
                .header("Accept-Profile", st.schema())
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapError)
                .bodyToMono(responseType);
    }

    public <T> Mono<T> patch(String schemaTable, String query, Object body, String userToken, Class<T> responseType) {
        var st = SchemaTable.parse(schemaTable);
        String safeQuery = requireQuery(query, "PATCH");
        return webClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/{table}")
                        .query(safeQuery)
                        .build(st.table()))
                .header("Authorization", toBearerAuth(userToken))
                .header("Content-Profile", st.schema())
                .header("Accept-Profile", st.schema())
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapError)
                .bodyToMono(responseType);
    }

    public Mono<Void> delete(String schemaTable, String query, String userToken) {
        var st = SchemaTable.parse(schemaTable);
        String safeQuery = requireQuery(query, "DELETE");
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/{table}")
                        .query(safeQuery)
                        .build(st.table()))
                .header("Authorization", toBearerAuth(userToken))
                .header("Content-Profile", st.schema())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapError)
                .bodyToMono(Void.class);
    }

    public Mono<Integer> deleteReturningCount(String schemaTable, String query, String userToken) {
        var st = SchemaTable.parse(schemaTable);
        String safeQuery = requireQuery(query, "DELETE");
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/{table}")
                        .query(safeQuery)
                        .build(st.table()))
                .header("Authorization", toBearerAuth(userToken))
                .header("Content-Profile", st.schema())
                .header("Accept-Profile", st.schema())
                .header("Prefer", "return=representation")
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapError)
                .bodyToFlux(Object.class)
                .collectList()
                .map(List::size);
    }

    public <T> Mono<T> rpc(String schemaFunction, Map<String, Object> params, String userToken, Class<T> responseType) {
        var st = SchemaTable.parse(schemaFunction);
        return webClient.post()
                .uri("/rpc/{function}", st.table())
                .header("Authorization", toBearerAuth(userToken))
                .header("Content-Profile", st.schema())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(params)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapError)
                .bodyToMono(responseType);
    }
}
