package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

import java.util.List;
import java.util.Locale;

public class S138Check extends TSqlCheck {

    public static final String RULE_KEY = "S138";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        List<String> lines = TextRuleSupport.lines(getSourceText());
        int blockStart = -1;
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("create procedure")
                    || trimmed.toLowerCase(Locale.ROOT).startsWith("alter procedure")
                    || trimmed.toLowerCase(Locale.ROOT).startsWith("create function")
                    || trimmed.toLowerCase(Locale.ROOT).startsWith("alter function")) {
                blockStart = i;
                break;
            }
        }

        if (blockStart >= 0) {
            int length = 0;
            for (int i = blockStart; i < lines.size(); i++) {
                String trimmed = lines.get(i).trim();
                if (trimmed.isEmpty()) {
                    length++;
                    continue;
                }
                if (trimmed.equalsIgnoreCase("end") || trimmed.equalsIgnoreCase("begin") || trimmed.toLowerCase(Locale.ROOT).contains("as")) {
                    length++;
                    continue;
                }
                if (trimmed.startsWith("--") || trimmed.startsWith("/*")) {
                    length++;
                    continue;
                }
                if (trimmed.endsWith(";")) {
                    length++;
                    continue;
                }
                if (trimmed.toLowerCase(Locale.ROOT).startsWith("create procedure")
                        || trimmed.toLowerCase(Locale.ROOT).startsWith("alter procedure")) {
                    length++;
                    continue;
                }
                length++;
                if (length > 20) {
                    reportIssueAtLine(blockStart + 1, "Procedures should not be too long.");
                    break;
                }
            }
        }
        super.visit(node);
    }
}
