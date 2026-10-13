package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.CreateProcedureStatement;
import io.github.fupacat.ast.Identifier;
import io.github.fupacat.ast.ProcedureReference;
import io.github.fupacat.ast.SchemaObjectName;

/**
 * Rule: Avoid the sp_ prefix for user-defined stored procedures.
 */
public class AvoidSpPrefixProcedureNameCheck extends TSqlCheck {

    public static final String RULE_KEY = "SR0016";

    // TRACEABILITY_MAPPING: REQ-024
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-024
    @Override
    public void visit(CreateProcedureStatement node) {
        if (node == null || node.ProcedureReference == null || node.ProcedureReference.Name == null) {
            super.visit(node);
            return;
        }
        SchemaObjectName name = node.ProcedureReference.Name;
        Identifier baseIdentifier = name.BaseIdentifier != null ? name.BaseIdentifier : (name.Identifiers != null && !name.Identifiers.isEmpty() ? name.Identifiers.get(name.Identifiers.size() - 1) : null);
        if (baseIdentifier != null && baseIdentifier.Value != null) {
            String value = baseIdentifier.Value.trim();
            if (value.regionMatches(true, 0, "sp_", 0, 3) || value.regionMatches(true, 0, "sp", 0, 2)) {
                reportIssue(node, "Avoid the 'sp_' prefix for user-defined stored procedures; use a distinct, application-specific prefix instead.");
            }
        }
        super.visit(node);
    }
}
