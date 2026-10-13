package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ParseResult;
import io.github.fupacat.TSqlParser;
import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.TSqlScript;
import io.github.fupacat.sonar.tsql.catalog.ProjectSchemaCatalog;
import org.sonar.api.batch.fs.InputFile;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        CollectingContext context = new CollectingContext(sql);
        context.projectSchema = new ProjectSchemaCatalog();
        context.projectSchema.indexScript(result.fragment());
        check.scan(result.fragment(), context);
        return context.issues;
    }

    public static List<String> scanWithContents(TSqlCheck check, String sql) {
        return scan(check, sql);
    }

    public static class CollectingContext implements TSqlCheckContext {
        public final List<String> issues = new ArrayList<>();
        private ProjectSchemaCatalog projectSchema = ProjectSchemaCatalog.empty();
        private final String sourceText;
        private final InputFile file;

        public CollectingContext(String sourceText) {
            this.sourceText = sourceText;
            this.file = mock(InputFile.class);
            try {
                when(file.contents()).thenReturn(sourceText);
            } catch (Exception ignored) {
                // Mockito mock setup exception ignored for unit tests
            }
        }

        @Override
        public InputFile getFile() {
            return file;
        }

        @Override
        public String getSourceText() {
            return sourceText;
        }

        @Override
        public ProjectSchemaCatalog getProjectSchema() {
            return projectSchema;
        }

        public void setProjectSchema(ProjectSchemaCatalog projectSchema) {
            this.projectSchema = projectSchema;
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
