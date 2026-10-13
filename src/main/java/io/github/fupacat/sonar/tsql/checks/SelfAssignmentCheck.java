package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.SetVariableStatement;
import io.github.fupacat.ast.VariableReference;

/**
 * Rule: Variables should not be self-assigned.
 */
public class SelfAssignmentCheck extends TSqlCheck {

    public static final String RULE_KEY = "S1656";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(SetVariableStatement node) {
        if (node.Variable != null
                && node.Expression != null
                && node.Variable instanceof VariableReference
                && node.Expression instanceof VariableReference) {
            VariableReference variable = (VariableReference) node.Variable;
            VariableReference expression = (VariableReference) node.Expression;
            if (variable.Name != null && variable.Name.equals(expression.Name)) {
            reportIssue(node, "Remove this self-assignment; assigning a variable to itself has no effect.");
            }
        }
        super.visit(node);
    }
}
