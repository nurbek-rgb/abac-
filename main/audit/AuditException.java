package kz.abac.audit;

/** Audit failures abort the audited operation; callers must not grant access on exceptions. */
public final class AuditException extends RuntimeException {
    public AuditException(String message) { super(message); }
    public AuditException(String message, Throwable cause) { super(message, cause); }
}
