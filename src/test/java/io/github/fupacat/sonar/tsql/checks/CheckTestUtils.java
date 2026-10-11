package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ParseResult;
import io.github.fupacat.TSqlParser;
import io.github.fupacat.ast.TSqlFragment;
import org.sonar.api.batch.fs.InputFile;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public final class CheckTestUtils {

    private CheckTestUtils() {
    }

    public static List<String> scan(TSqlCheck check, String sql) {
        TSqlParser parser = new TSqlParser();
        ParseResult result = parser.parse(sql);
        if (result.getFragment() == null) {
            throw new IllegalArgumentException("Failed to parse SQL: " + sql + ", errors: " + result.getErrors());
        }

        CollectingContext context = new CollectingContext();
        check.scan(result.getFragment(), context);
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
        public void reportIssue(TSqlCheck check, int line, String message) {
            issues.add(message);
        }
    }
}
