package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.Literal;
import io.github.fupacat.ast.SqlDataTypeOption;
import io.github.fupacat.ast.SqlDataTypeReference;

import java.util.List;

/**
 * Rule: Avoid variable-length types with precision 1 or 2.
 */
public class AvoidVariableLengthDataTypeCheck extends TSqlCheck {

    public static final String RULE_KEY = "SR0009";

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public void visit(SqlDataTypeReference node) {
        if (node != null && node.SqlDataTypeOption != null && hasProblematicLength(node)) {
            reportIssue(node, "Avoid variable-length types with a length of 1 or 2; prefer a wider type to reduce storage and improve readability.");
        }
        super.visit(node);
    }

    private boolean hasProblematicLength(SqlDataTypeReference node) {
        if (node.Parameters == null || node.Parameters.isEmpty()) {
            return false;
        }
        SqlDataTypeOption option = node.SqlDataTypeOption;
        if (option != SqlDataTypeOption.VarChar
                && option != SqlDataTypeOption.NVarChar
                && option != SqlDataTypeOption.VarBinary) {
            return false;
        }

        for (Literal parameter : node.Parameters) {
            if (parameter == null || parameter.Value == null) {
                continue;
            }
            try {
                int length = Integer.parseInt(parameter.Value);
                if (length == 1 || length == 2) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // Not a numeric precision literal.
            }
        }
        return false;
    }
}
