package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.CreateProcedureStatement;
import io.github.fupacat.ast.Identifier;
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
        if (node.ProcedureReference != null && node.ProcedureReference.Name != null) {
            Identifier baseIdentifier = node.ProcedureReference.Name.BaseIdentifier;
            if (baseIdentifier != null && baseIdentifier.Value != null
                    && baseIdentifier.Value.trim().regionMatches(true, 0, "sp_", 0, 3)) {
                reportIssue(node, "Avoid the 'sp_' prefix for user-defined stored procedures; use a distinct, application-specific prefix instead.");
            }
        }
        super.visit(node);
    }
}
