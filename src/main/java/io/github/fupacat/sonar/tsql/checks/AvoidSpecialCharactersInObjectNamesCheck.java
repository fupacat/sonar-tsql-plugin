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
        if (node == null) {
            return false;
        }

        for (Identifier identifier : extractIdentifiers(node)) {
            if (identifier == null || identifier.Value == null) {
                continue;
            }
            String value = normalizeIdentifier(identifier.Value);
            if (value.isEmpty()) {
                continue;
            }
            if (!value.matches("[A-Za-z0-9_#]+")) {
                return true;
            }
            if (Character.isDigit(value.charAt(0)) && !value.startsWith("#")) {
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
