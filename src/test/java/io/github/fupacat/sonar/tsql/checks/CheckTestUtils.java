package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ParseResult;
import io.github.fupacat.TSqlParser;
import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.TSqlScript;
import org.sonar.api.batch.fs.InputFile;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public final class CheckTestUtils {

    private CheckTestUtils() {
    }

    // TRACEABILITY_MAPPING: REQ-007, REQ-008, REQ-009, REQ-010
    public static List<String> scan(TSqlCheck check, String sql) {
        TSqlParser parser = new TSqlParser();
        ParseResult<TSqlScript> result = parser.parse(sql);
        if (result.fragment() == null) {
            throw new IllegalArgumentException("Failed to parse SQL: " + sql + ", errors: " + result.errors());
        }
        result.fragment().Value = sql;

        CollectingContext context = new CollectingContext();
        check.scan(result.fragment(), context);
        return context.issues;
    }

    public static class CollectingContext implements TSqlCheckContext {
        public final List<String> issues = new ArrayList<>();

        @Override
        public InputFile getFile() {
            return mock(InputFile.class);
        }

        @Override
        public void reportIssue(TSqlCheck check, TSqlFragment fragment, String message) {
            issues.add(message);
        }

        @Override
        public void reportIssueAtLine(TSqlCheck check, int line, String message) {
            issues.add(message);
        }
    }
}
