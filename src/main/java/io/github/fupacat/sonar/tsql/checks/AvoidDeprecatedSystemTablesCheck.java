package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlFragment;

import java.util.regex.Pattern;

/**
 * Rule: Deprecated SQL Server system tables should be replaced with sys catalog views.
 */
public class AvoidDeprecatedSystemTablesCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4155";
    private static final Pattern DEPRECATED_TABLES = Pattern.compile(
            "(?is)\\b(?:sysobjects|syscolumns|sysindexes|sysconstraints|sysdepends|systypes|sysusers|sysmembers|syscomments)\\b"
    );

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void scan(TSqlFragment root, TSqlCheckContext context) {
        super.scan(root, context);
        String sql = getSourceText();
        if (sql != null && DEPRECATED_TABLES.matcher(sql).find()) {
            reportIssue(root, "Replace deprecated system tables with the sys catalog views.");
        }
    }
}
