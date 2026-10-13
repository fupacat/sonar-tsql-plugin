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
        if (node.Source instanceof PasswordCreateLoginSource source && isNonEmptyString(source.Password)) {
            reportIssue(node, "Do not hard-code passwords in CREATE LOGIN statements.");
        }
        super.visit(node);
    }

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public void visit(AlterLoginOptionsStatement node) {
        if (node.Options != null) {
            for (PrincipalOption option : node.Options) {
                if (option instanceof PasswordAlterPrincipalOption passwordOption && isNonEmptyString(passwordOption.Password)) {
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
        if (isNonEmptyString(node.UserId) || isNonEmptyString(node.Password)) {
            return true;
        }

        if (node.ProviderString != null && node.ProviderString.Value != null) {
            String normalized = node.ProviderString.Value.toLowerCase(Locale.ROOT);
            return normalized.contains("user=")
                    || normalized.contains("uid=")
                    || normalized.contains("password=")
                    || normalized.contains("pwd=");
        }
        return false;
    }

    private boolean isNonEmptyString(Literal literal) {
        if (literal == null || literal.Value == null) {
            return false;
        }
        String val = literal.Value.trim();
        if (val.length() >= 2 && ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith("N'") && val.endsWith("'")))) {
            int start = val.startsWith("N'") ? 2 : 1;
            val = val.substring(start, val.length() - 1);
        }
        return !val.isBlank();
    }
}
