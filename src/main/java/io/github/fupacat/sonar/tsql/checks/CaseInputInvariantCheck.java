package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.FunctionCall;
import io.github.fupacat.ast.SimpleCaseExpression;

/**
 * Rule: CASE input expressions should be invariant.
 */
public class CaseInputInvariantCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4094";

    // TRACEABILITY_MAPPING: REQ-015
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-015
    @Override
    public void visit(SimpleCaseExpression node) {
        if (node.InputExpression instanceof FunctionCall functionCall && functionCall.FunctionName != null) {
            String functionName = functionCall.FunctionName.Value;
            if ("RAND".equalsIgnoreCase(functionName) || "NEWID".equalsIgnoreCase(functionName)) {
                reportIssue(node, "CASE input expressions should be invariant; avoid RAND() and NEWID() in simple CASE expressions.");
            }
        }
        super.visit(node);
    }
}
