package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.Identifier;
import io.github.fupacat.ast.SchemaObjectName;

import java.util.Locale;

/**
 * Rule: Avoid special characters in DDL object names.
 */
public class AvoidSpecialCharactersInObjectNamesCheck extends TSqlCheck {

    public static final String RULE_KEY = "SR0011";

    // TRACEABILITY_MAPPING: REQ-020
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-020
    @Override
    public void visit(SchemaObjectName node) {
        if (node != null && hasSpecialCharacters(node)) {
            reportIssue(node, "Avoid special characters in DDL object names; use simple identifiers such as letters, digits, and underscores.");
        }
        super.visit(node);
    }

    private boolean hasSpecialCharacters(SchemaObjectName node) {
        for (Identifier identifier : node.Identifiers) {
            if (identifier == null || identifier.Value == null) {
                continue;
            }
            String value = identifier.Value.trim();
            if (!value.isEmpty() && !value.matches("[A-Za-z0-9_#$]+")) {
                return true;
            }
            if (value.length() > 0 && Character.isDigit(value.charAt(0)) && !value.startsWith("#")) {
                return true;
            }
        }
        return false;
    }
}
