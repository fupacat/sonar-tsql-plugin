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
        int blockStart = findProcedureStart(lines);
        if (blockStart >= 0 && isProcedureTooLong(lines, blockStart)) {
            reportIssueAtLine(blockStart + 1, "Procedures should not be too long.");
        }
        super.visit(node);
    }

    private int findProcedureStart(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (isProcedureDeclaration(lines.get(i).trim())) {
                return i;
            }
        }
        return -1;
    }

    private boolean isProcedureDeclaration(String trimmed) {
        String lowered = trimmed.toLowerCase(Locale.ROOT);
        return lowered.startsWith("create procedure")
                || lowered.startsWith("alter procedure")
                || lowered.startsWith("create function")
                || lowered.startsWith("alter function");
    }

    private boolean isProcedureTooLong(List<String> lines, int blockStart) {
        int length = 0;
        for (int i = blockStart; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (isIgnoredProcedureLine(trimmed)) {
                continue;
            }
            length++;
            if (length > 20) {
                return true;
            }
        }
        return false;
    }

    private boolean isIgnoredProcedureLine(String trimmed) {
        if (trimmed.isEmpty()) {
            return true;
        }
        String lowered = trimmed.toLowerCase(Locale.ROOT);
        return trimmed.equalsIgnoreCase("end")
                || trimmed.equalsIgnoreCase("begin")
                || lowered.contains("as")
                || trimmed.startsWith("--")
                || trimmed.startsWith("/*")
                || lowered.startsWith("create procedure")
                || lowered.startsWith("alter procedure")
                || lowered.startsWith("create function")
                || lowered.startsWith("alter function");
    }
}
