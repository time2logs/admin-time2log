package ch.time2log.backend.infrastructure.supabase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class SupabaseClient {

    private final WebClient webClient;

    public SupabaseClient(@Value("${supabase.url}") String supabaseUrl,
                          @Value("${supabase.anon-key}") String anonKey) {
        this.webClient = WebClient.builder()
                .baseUrl(supabaseUrl + "/rest/v1")
                .defaultHeader("apikey", anonKey)
                .build();
    }

    /**
     * Parse "schema.table" into schema and table parts
     */
    private record SchemaTable(String schema, String table) {
        static SchemaTable parse(String schemaTable) {
            if (schemaTable.contains(".")) {
                String[] parts = schemaTable.split("\\.", 2);
                return new SchemaTable(parts[0], parts[1]);
            }
            return new SchemaTable("public", schemaTable);
        }
    }

    public <T> Mono<T> get(String schemaTable, String userToken, Class<T> responseType) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.get()
                .uri("/{table}", st.table())
                .header("Authorization", "Bearer " + userToken)
                .header("Accept-Profile", st.schema())
                .retrieve()
                .bodyToMono(responseType);
    }

    public <T> Mono<List<T>> getList(String schemaTable, String userToken, Class<T> elementType) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.get()
                .uri("/{table}", st.table())
                .header("Authorization", "Bearer " + userToken)
                .header("Accept-Profile", st.schema())
                .retrieve()
                .bodyToFlux(elementType)
                .collectList();
    }

    public <T> Mono<T> getWithQuery(String schemaTable, String query, String userToken, Class<T> responseType) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.get()
                .uri("/{table}?{query}", st.table(), query)
                .header("Authorization", "Bearer " + userToken)
                .header("Accept-Profile", st.schema())
                .retrieve()
                .bodyToMono(responseType);
    }

    public <T> Mono<List<T>> getListWithQuery(String schemaTable, String query, String userToken, Class<T> elementType) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.get()
                .uri("/{table}?{query}", st.table(), query)
                .header("Authorization", "Bearer " + userToken)
                .header("Accept-Profile", st.schema())
                .retrieve()
                .bodyToFlux(elementType)
                .collectList();
    }

    public <T> Mono<T> post(String schemaTable, Object body, String userToken, Class<T> responseType) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.post()
                .uri("/{table}", st.table())
                .header("Authorization", "Bearer " + userToken)
                .header("Content-Profile", st.schema())
                .header("Prefer", "return=representation")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType);
    }

    public <T> Mono<T> patch(String schemaTable, String query, Object body, String userToken, Class<T> responseType) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.patch()
                .uri("/{table}?{query}", st.table(), query)
                .header("Authorization", "Bearer " + userToken)
                .header("Content-Profile", st.schema())
                .header("Prefer", "return=representation")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType);
    }

    public Mono<Void> delete(String schemaTable, String query, String userToken) {
        var st = SchemaTable.parse(schemaTable);
        return webClient.delete()
                .uri("/{table}?{query}", st.table(), query)
                .header("Authorization", "Bearer " + userToken)
                .header("Content-Profile", st.schema())
                .retrieve()
                .bodyToMono(Void.class);
    }

    /**
     * Call RPC function
     */
    public <T> Mono<T> rpc(String functionName, Map<String, Object> params, String userToken, Class<T> responseType) {
        return webClient.post()
                .uri("/rpc/{function}", functionName)
                .header("Authorization", "Bearer " + userToken)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(responseType);
    }
}
