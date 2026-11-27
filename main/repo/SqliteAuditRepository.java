package kz.abac.repo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kz.abac.Effect;
import kz.abac.audit.AuditException;
import kz.abac.audit.AuditRecord;

/** One JDBC connection, serialized access, bound SQL parameters and durable records. */
public final class SqliteAuditRepository implements Repository<AuditRecord>, AutoCloseable {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Connection connection;

    public SqliteAuditRepository(String url) {
        if (url == null || !url.startsWith("jdbc:sqlite:")) throw new AuditException("SQLite JDBC URL required");
        Connection opened = null;
        try {
            opened = DriverManager.getConnection(url);
            try (Statement statement = opened.createStatement()) {
                statement.execute("PRAGMA busy_timeout=5000");
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS audit_records (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      timestamp TEXT NOT NULL,
                      attributes_json TEXT NOT NULL,
                      effect TEXT NOT NULL CHECK (effect IN ('PERMIT','DENY')),
                      policy_id TEXT,
                      reason TEXT NOT NULL
                    )
                    """);
            }
            connection = opened;
        } catch (SQLException e) {
            if (opened != null) try { opened.close(); } catch (SQLException closeError) { e.addSuppressed(closeError); }
            throw new AuditException("Cannot open SQLite audit database",e);
        }
    }

    @Override public synchronized AuditRecord save(AuditRecord record) {
        String sql = record.id() == null
                ? "INSERT INTO audit_records(timestamp,attributes_json,effect,policy_id,reason) VALUES(?,?,?,?,?)"
                : "UPDATE audit_records SET timestamp=?,attributes_json=?,effect=?,policy_id=?,reason=? WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1,record.timestamp().toString());
            statement.setString(2,JSON.writeValueAsString(record.attributes()));
            statement.setString(3,record.effect().name());
            statement.setString(4,record.policyId());
            statement.setString(5,record.reason());
            if (record.id() != null) statement.setLong(6,record.id());
            if (statement.executeUpdate() != 1) throw new AuditException("Unknown audit ID");
            if (record.id() != null) return record;
            try (Statement query = connection.createStatement(); ResultSet result = query.executeQuery("SELECT last_insert_rowid()")) {
                if (!result.next()) throw new AuditException("SQLite did not return an ID");
                return record.withId(result.getLong(1));
            }
        } catch (SQLException | java.io.IOException e) { throw new AuditException("Cannot save audit record",e); }
    }

    @Override public synchronized Optional<AuditRecord> findById(long id) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM audit_records WHERE id=?")) {
            statement.setLong(1,id);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? Optional.of(read(result)) : Optional.empty(); }
        } catch (SQLException e) { throw new AuditException("Cannot read audit record",e); }
    }
    @Override public synchronized List<AuditRecord> findAll() {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM audit_records ORDER BY id")) {
            List<AuditRecord> records = new ArrayList<>();
            while(result.next()) records.add(read(result));
            return List.copyOf(records);
        } catch (SQLException e) { throw new AuditException("Cannot read audit records",e); }
    }
    private AuditRecord read(ResultSet result) throws SQLException {
        try {
            Map<String,String> attributes = JSON.readValue(result.getString("attributes_json"),new TypeReference<Map<String,String>>() {});
            return new AuditRecord(result.getLong("id"),Instant.parse(result.getString("timestamp")),attributes,
                    Effect.valueOf(result.getString("effect")),result.getString("policy_id"),result.getString("reason"));
        } catch (java.io.IOException | RuntimeException e) { throw new AuditException("Invalid stored audit record",e); }
    }
    @Override public synchronized boolean deleteById(long id) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM audit_records WHERE id=?")) {
            statement.setLong(1,id); return statement.executeUpdate() == 1;
        } catch (SQLException e) { throw new AuditException("Cannot delete audit record",e); }
    }
    @Override public synchronized long count() {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM audit_records")) {
            result.next(); return result.getLong(1);
        } catch (SQLException e) { throw new AuditException("Cannot count audit records",e); }
    }
    @Override public synchronized void close() {
        try { connection.close(); } catch (SQLException e) { throw new AuditException("Cannot close audit database",e); }
    }
}
