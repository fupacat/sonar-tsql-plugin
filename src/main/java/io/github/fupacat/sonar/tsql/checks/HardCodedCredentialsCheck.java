package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.AlterLoginOptionsStatement;
import io.github.fupacat.ast.CreateLoginStatement;
import io.github.fupacat.ast.CreateUserStatement;
import io.github.fupacat.ast.Literal;
import io.github.fupacat.ast.LiteralPrincipalOption;
import io.github.fupacat.ast.OpenRowsetTableReference;
import io.github.fupacat.ast.PasswordAlterPrincipalOption;
import io.github.fupacat.ast.PasswordCreateLoginSource;
import io.github.fupacat.ast.PrincipalOption;
import io.github.fupacat.ast.PrincipalOptionKind;

import java.util.Locale;

/**
 * Rule: Credentials should not be hard-coded in SQL scripts.
 */
public class HardCodedCredentialsCheck extends TSqlCheck {

    public static final String RULE_KEY = "S2068";

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public void visit(CreateLoginStatement node) {
        if (node.Source instanceof PasswordCreateLoginSource source && source.Password != null && isNonEmptyString(source.Password)) {
            reportIssue(node, "Do not hard-code passwords in CREATE LOGIN statements.");
        }
        super.visit(node);
    }

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public void visit(AlterLoginOptionsStatement node) {
        if (node.Options != null) {
            for (PrincipalOption option : node.Options) {
                if (option instanceof PasswordAlterPrincipalOption passwordOption && passwordOption.Password != null
                        && isNonEmptyString(passwordOption.Password)) {
                    reportIssue(node, "Do not hard-code passwords in ALTER LOGIN statements.");
                    break;
                }
            }
        }
        super.visit(node);
    }

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public void visit(CreateUserStatement node) {
        if (node.UserOptions != null) {
            for (PrincipalOption option : node.UserOptions) {
                if (option instanceof LiteralPrincipalOption literalPrincipalOption
                        && option.OptionKind == PrincipalOptionKind.Password
                        && literalPrincipalOption.Value != null
                        && isNonEmptyString(literalPrincipalOption.Value)) {
                    reportIssue(node, "Do not hard-code passwords in CREATE USER statements.");
                    break;
                }
            }
        }
        super.visit(node);
    }

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public void visit(OpenRowsetTableReference node) {
        if (hasEmbeddedCredentials(node)) {
            reportIssue(node, "Do not hard-code credentials in OPENROWSET connections.");
        }
        super.visit(node);
    }

    private boolean hasEmbeddedCredentials(OpenRowsetTableReference node) {
        if ((node.UserId != null && isNonEmptyString(node.UserId))
                || (node.Password != null && isNonEmptyString(node.Password))) {
            return true;
        }

        String providerString = safeLiteralValue(node.ProviderString);
        if (providerString == null) {
            return false;
        }

        String normalized = providerString.toLowerCase(Locale.ROOT);
        return normalized.contains("user=") || normalized.contains("uid=")
                || normalized.contains("******pwd=");
    }

    private boolean isNonEmptyString(Literal literal) {
        return literal != null && literal.Value != null && !literal.Value.isBlank();
    }

    private String safeLiteralValue(Literal literal) {
        return literal != null && literal.Value != null && !literal.Value.isBlank() ? literal.Value : null;
    }
}
