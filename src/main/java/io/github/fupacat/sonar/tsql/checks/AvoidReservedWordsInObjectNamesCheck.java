package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.Identifier;
import io.github.fupacat.ast.SchemaObjectName;

import java.util.Locale;
import java.util.Set;

/**
 * Rule: Avoid using SQL reserved words for object and type names.
 */
public class AvoidReservedWordsInObjectNamesCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4413";
    private static final Set<String> RESERVED_WORDS = Set.of(
            "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "BACKUP", "BEGIN", "BETWEEN",
            "BREAK", "BY", "CASE", "CHECK", "CLOSE", "CLUSTERED", "COALESCE", "COLLATE", "COLUMN",
            "COMMIT", "CONSTRAINT", "CONTINUE", "CONVERT", "CREATE", "CROSS", "CURRENT", "CURRENT_DATE",
            "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "DATABASE", "DATEADD", "DATEDIFF",
            "DATEPART", "DBCC", "DEALLOCATE", "DECLARE", "DEFAULT", "DELETE", "DENY", "DESC", "DISK",
            "DISTINCT", "DROP", "DUMP", "ELSE", "END", "ERRLVL", "ESCAPE", "EXCEPT", "EXEC",
            "EXECUTE", "EXISTS", "EXIT", "EXTERNAL", "FETCH", "FILE", "FILLFACTOR", "FOR", "FOREIGN",
            "FORMATMESSAGE", "FROM", "FULL", "FUNCTION", "GOTO", "GRANT", "GROUP", "HAVING", "HOLDLOCK",
            "IDENTITY", "IF", "IN", "INDEX", "INNER", "INSERT", "INTERSECT", "INTO", "IS", "JOIN",
            "KEY", "KILL", "LEFT", "LIKE", "LINENO", "LOAD", "MERGE", "NATIONAL", "NEWID", "NO",
            "NOCHECK", "NONCLUSTERED", "NOT", "NULL", "NULLIF", "OF", "OFF", "OFFSETS", "ON", "OPEN",
            "OPENDATASOURCE", "OPENQUERY", "OPENROWSET", "OPENXML", "OPTION", "OR", "ORDER", "OUTER",
            "OVER", "PERCENT", "PLAN", "PRECISION", "PRIMARY", "PRINT", "PROC", "PROCEDURE", "RAISERROR",
            "READ", "READTEXT", "RECONFIGURE", "REFERENCES", "REPLICATION", "RESTORE", "RESTRICT", "RETURN",
            "REVERT", "REVOKE", "RIGHT", "ROLLBACK", "ROWCOUNT", "ROWGUIDCOL", "RULE", "SAVE", "SCHEMA",
            "SELECT", "SESSION_USER", "SET", "SETUSER", "SHUTDOWN", "SOME", "STATISTICS", "SYSTEM_USER",
            "TABLE", "TEXTSIZE", "THEN", "TO", "TOP", "TRAN", "TRANSACTION", "TRIGGER", "TRUNCATE",
            "TRY_CONVERT", "UNION", "UNIQUE", "UNIQUEIDENTIFIER", "UPDATE", "UPDATETEXT", "USE", "USER",
            "VALUES", "VIEW", "WAITFOR", "WHEN", "WHERE", "WHILE", "WITH", "WRITETEXT"
    );

    // TRACEABILITY_MAPPING: REQ-021
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-021
    @Override
    public void visit(SchemaObjectName node) {
        if (node != null && containsReservedWord(node)) {
            reportIssue(node, "Avoid reserved words in object names; use a non-keyword identifier instead.");
        }
        super.visit(node);
    }

    private boolean containsReservedWord(SchemaObjectName node) {
        if (node == null) {
            return false;
        }
        for (Identifier identifier : extractIdentifiers(node)) {
            if (identifier == null || identifier.Value == null) {
                continue;
            }
            String value = normalizeIdentifier(identifier.Value);
            if (!value.isEmpty() && RESERVED_WORDS.contains(value.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private java.util.List<Identifier> extractIdentifiers(SchemaObjectName node) {
        java.util.List<Identifier> identifiers = new java.util.ArrayList<>();
        if (node.Identifiers != null) {
            identifiers.addAll(node.Identifiers);
        }
        if (node.BaseIdentifier != null) {
            identifiers.add(node.BaseIdentifier);
        }
        if (node.SchemaIdentifier != null) {
            identifiers.add(node.SchemaIdentifier);
        }
        if (node.DatabaseIdentifier != null) {
            identifiers.add(node.DatabaseIdentifier);
        }
        if (node.ServerIdentifier != null) {
            identifiers.add(node.ServerIdentifier);
        }
        return identifiers;
    }

    private String normalizeIdentifier(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
