-- Matches SqliteAuditRepository initialization.
CREATE TABLE IF NOT EXISTS audit_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp TEXT NOT NULL,
    attributes_json TEXT NOT NULL,
    effect TEXT NOT NULL CHECK (effect IN ('PERMIT','DENY')),
    policy_id TEXT,
    reason TEXT NOT NULL
);
