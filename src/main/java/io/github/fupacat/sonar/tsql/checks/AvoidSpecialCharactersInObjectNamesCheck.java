package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.Identifier;
import io.github.fupacat.ast.SchemaObjectName;

import java.util.ArrayList;
import java.util.List;

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
            String value = normalizeIdentifier(identifier == null ? null : identifier.Value);
            if (value.isEmpty()) {
                continue;
            }
            if (isDisallowedName(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDisallowedName(String value) {
        return !value.matches("[A-Za-z0-9_#]+") || (Character.isDigit(value.charAt(0)) && !value.startsWith("#"));
    }

    private List<Identifier> extractIdentifiers(SchemaObjectName node) {
        List<Identifier> identifiers = new ArrayList<>();
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
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
