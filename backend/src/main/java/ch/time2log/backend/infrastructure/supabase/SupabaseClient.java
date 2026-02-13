package ch.time2log.backend.infrastructure.supabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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

    private record SchemaTable(String schema, String table) {
        static SchemaTable parse(String schemaTable) {
            if (schemaTable.contains(".")) {
                String[] parts = schemaTable.split("\\.", 2);
                return new SchemaTable(parts[0], parts[1]);
            }
            return new SchemaTable("public", schemaTable);
        }
    }

    private static String toBearerAuth(String userToken) {
        if (userToken == null) return null;
        String t = userToken.trim();
        if (t.isEmpty()) return null;
        return t.regionMatches(true, 0, "Bearer ", 0, 7) ? t : "Bearer " + t;
    }

    public <T> Mono<T> get(String schemaTable, String userToken, Class<T> responseType) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.get()
                .uri("/{table}", st.table())
                .header("Authorization", toBearerAuth(userToken))
                .header("Accept-Profile", st.schema())
                .retrieve()
                .bodyToMono(responseType);
    }

    public <T> Mono<List<T>> getList(String schemaTable, String userToken, Class<T> elementType) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.get()
                .uri("/{table}", st.table())
                .header("Authorization", toBearerAuth(userToken))
                .header("Accept-Profile", st.schema())
                .retrieve()
                .bodyToFlux(elementType)
                .collectList();
    }

    public <T> Mono<T> getWithQuery(String schemaTable, String query, String userToken, Class<T> responseType) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.get()
                .uri("/{table}?{query}", st.table(), query)
                .header("Authorization", toBearerAuth(userToken))
                .header("Accept-Profile", st.schema())
                .retrieve()
                .bodyToMono(responseType);
    }

    public <T> Mono<List<T>> getListWithQuery(String schemaTable, String query, String userToken, Class<T> elementType) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.get()
                .uri("/{table}?{query}", st.table(), query)
                .header("Authorization", toBearerAuth(userToken))
                .header("Accept-Profile", st.schema())
                .retrieve()
                .bodyToFlux(elementType)
                .collectList();
    }

    public <T> Mono<T> post(String schemaTable, Object body, String userToken, Class<T> responseType) {
        var st = SchemaTable.parse(schemaTable);
        String authHeader = "Bearer " + userToken;
        log.debug("POST to {}.{}", st.schema(), st.table());
        log.debug("Authorization: {}...", authHeader.substring(0, Math.min(60, authHeader.length())));
        log.debug("Body: {}", body);

        return webClient.post()
                .uri("/{table}", st.table())
                .header("Authorization", authHeader)
                .header("Content-Profile", st.schema())
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.isError(), resp ->
                    resp.bodyToMono(String.class).flatMap(bodyStr -> {
                        log.error("Supabase error: {} - {}", resp.statusCode(), bodyStr);
                        return Mono.error(new RuntimeException(resp.statusCode() + " " + bodyStr));
                    })
                )
                .bodyToMono(responseType);
    }

    public <T> Mono<T> patch(String schemaTable, String query, Object body, String userToken, Class<T> responseType) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.patch()
                .uri("/{table}?{query}", st.table(), query)
                .header("Authorization", toBearerAuth(userToken))
                .header("Content-Profile", st.schema())
                .header("Accept-Profile", st.schema())
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType);
    }

    public Mono<Void> delete(String schemaTable, String query, String userToken) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.delete()
                .uri("/{table}?{query}", st.table(), query)
                .header("Authorization", toBearerAuth(userToken))
                .header("Content-Profile", st.schema())
                .retrieve()
                .bodyToMono(Void.class);
    }

    public <T> Mono<T> rpc(String functionName, Map<String, Object> params, String userToken, Class<T> responseType) {
        return webClient.post()
                .uri("/rpc/{function}", functionName)
                .header("Authorization", toBearerAuth(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(responseType);
    }
}
