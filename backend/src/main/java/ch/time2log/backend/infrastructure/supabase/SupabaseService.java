package ch.time2log.backend.infrastructure.supabase;

import ch.time2log.backend.security.AuthenticatedUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SupabaseService {

    private final SupabaseClient client;

    public SupabaseService(SupabaseClient client) {
        this.client = client;
    }

    public String getCurrentUserToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.token();
        }
        throw new IllegalStateException("No authenticated user found");
    }

    public <T> T get(String table, Class<T> responseType) {
        return client.get(table, getCurrentUserToken(), responseType).block();
    }

    public <T> List<T> getList(String table, Class<T> elementType) {
        return client.getList(table, getCurrentUserToken(), elementType).block();
    }

    public <T> T getWithQuery(String table, String query, Class<T> responseType) {
        return client.getWithQuery(table, query, getCurrentUserToken(), responseType).block();
    }

    public <T> List<T> getListWithQuery(String table, String query, Class<T> elementType) {
        return client.getListWithQuery(table, query, getCurrentUserToken(), elementType).block();
    }

    public <T> T post(String table, Object body, Class<T> responseType) {
        return client.post(table, body, getCurrentUserToken(), responseType).block();
    }

    public <T> T patch(String table, String query, Object body, Class<T> responseType) {
        return client.patch(table, query, body, getCurrentUserToken(), responseType).block();
    }

    public void delete(String table, String query) {
        client.delete(table, query, getCurrentUserToken()).block();
    }

    public <T> T rpc(String functionName, Map<String, Object> params, Class<T> responseType) {
        return client.rpc(functionName, params, getCurrentUserToken(), responseType).block();
    }
}
