package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.DeclareCursorStatement;
import io.github.fupacat.ast.FetchCursorStatement;
import io.github.fupacat.ast.QuerySpecification;
import io.github.fupacat.ast.TSqlFragment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule: FETCH INTO variable count must match the cursor SELECT list.
 */
public class FetchIntoCursorCountCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4103";
    private static final Pattern CURSOR_SELECT_PATTERN = Pattern.compile("(?is)declare\\s+\\w+\\s+cursor\\s+for\\s+select\\s+(.+?)(?:from|where|order\\s+by|group\\s+by|having|for)\\b");
    private static final Pattern FETCH_PATTERN = Pattern.compile("(?is)fetch\\s+(?:next|prior|first|last|absolute|relative|\S+)\\s+from\\s+(\w+)\\s+into\\s+(.+?)(?:;|$)");

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(DeclareCursorStatement node) {
        super.visit(node);
    }

    @Override
    public void visit(FetchCursorStatement node) {
        String sql = getSourceText();
        if (sql != null) {
            Matcher fetchMatcher = FETCH_PATTERN.matcher(sql);
            while (fetchMatcher.find()) {
                String cursorName = fetchMatcher.group(1);
                int intoVars = countVariables(fetchMatcher.group(2));
                int declaredVars = countDeclaredCursorColumns(sql, cursorName);
                if (declaredVars > 0 && intoVars != declaredVars) {
                    reportIssue(node, "The number of variables in FETCH INTO must match the number of columns in the cursor declaration.");
                    break;
                }
            }
        }
        super.visit(node);
    }

    private int countDeclaredCursorColumns(String sql, String cursorName) {
        String pattern = "(?is)declare\\s+" + Pattern.quote(cursorName) + "\\s+cursor\\s+for\\s+select\\s+(.+?)(?:from|;|$)";
        Matcher matcher = Pattern.compile(pattern).matcher(sql);
        if (!matcher.find()) {
            return 0;
        }
        String selectList = matcher.group(1);
        int count = 0;
        for (String part : selectList.split(",")) {
            if (!part.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private int countVariables(String list) {
        int count = 0;
        for (String part : list.split(",")) {
            if (!part.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
