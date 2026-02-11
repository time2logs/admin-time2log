package ch.time2log.backend.infrastructure.supabase;

public record SchemaTable(String schema, String table) {
    static SchemaTable parse(String schemaTable) {
        if (schemaTable.contains(".")) {
            String[] parts = schemaTable.split("\\.", 2);
            return new SchemaTable(parts[0], parts[1]);
        }
        return new SchemaTable("public", schemaTable);
    }
}
