package io.github.fupacat.sonar.tsql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TSqlSourceMetricsTest {

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void emptyFilePublishesZeroMetrics() {
        TSqlSourceMetrics metrics = TSqlSourceMetrics.fromText("");

        assertThat(metrics.lines()).isZero();
        assertThat(metrics.commentLines()).isZero();
        assertThat(metrics.nonCommentLines()).isZero();
        assertThat(metrics.statements()).isZero();
        assertThat(metrics.complexity()).isZero();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void commentOnlyFileCountsCommentLines() {
        TSqlSourceMetrics metrics = TSqlSourceMetrics.fromText("-- comment\n/* block\nstill comment */\n-- another");

        assertThat(metrics.lines()).isEqualTo(4);
        assertThat(metrics.commentLines()).isEqualTo(4);
        assertThat(metrics.nonCommentLines()).isZero();
        assertThat(metrics.complexity()).isZero();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void multilineSqlDoesNotBreakCommentAndStatementCounts() {
        TSqlSourceMetrics metrics = TSqlSourceMetrics.fromText("SELECT 1 /* multiline\ncomment */ + 2;\nSELECT 3;");

        assertThat(metrics.lines()).isEqualTo(3);
        assertThat(metrics.commentLines()).isEqualTo(0);
        assertThat(metrics.nonCommentLines()).isEqualTo(3);
        assertThat(metrics.statements()).isEqualTo(2);
        assertThat(metrics.complexity()).isZero();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void representativeRoutineCountsConditionalAndTryCatchComplexity() {
        String sql = "IF @x > 0\nBEGIN\n  WHILE @y < 10\n  BEGIN\n    SET @y = @y + 1;\n  END;\nEND;\n\nSELECT CASE WHEN @x > 0 THEN 1 ELSE 2 END;\n\nBEGIN TRY\n  SELECT 1;\nEND TRY\nBEGIN CATCH\n  SELECT 2;\nEND CATCH;";

        TSqlSourceMetrics metrics = TSqlSourceMetrics.fromText(sql);

        assertThat(metrics.statements()).isEqualTo(7);
        assertThat(metrics.complexity()).isEqualTo(5);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void ignoresStringLiteralContentWhenCountingStatementsAndComplexity() {
        TSqlSourceMetrics metrics = TSqlSourceMetrics.fromText(
                "SELECT 'IF ; GO CASE';\nSELECT \"WHILE\";\n");

        assertThat(metrics.lines()).isEqualTo(2);
        assertThat(metrics.commentLines()).isZero();
        assertThat(metrics.nonCommentLines()).isEqualTo(2);
        assertThat(metrics.statements()).isEqualTo(2);
        assertThat(metrics.complexity()).isZero();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void handlesEscapedQuotesWithoutInflatingMetrics() {
        String sql = "SELECT 'it''s -- text ; GO';\nSELECT \"\"\"IF\"\"\";\n";
        TSqlSourceMetrics metrics = TSqlSourceMetrics.fromText(sql);

        assertThat(metrics.lines()).isEqualTo(2);
        assertThat(metrics.commentLines()).isZero();
        assertThat(metrics.nonCommentLines()).isEqualTo(2);
        assertThat(metrics.statements()).isEqualTo(2);
        assertThat(metrics.complexity()).isZero();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void countsCommentOnlyTrailingLineWithoutTerminatingNewline() {
        TSqlSourceMetrics metrics = TSqlSourceMetrics.fromText("SELECT 1;\n-- trailing comment");

        assertThat(metrics.lines()).isEqualTo(2);
        assertThat(metrics.commentLines()).isEqualTo(1);
        assertThat(metrics.nonCommentLines()).isEqualTo(1);
        assertThat(metrics.statements()).isEqualTo(1);
        assertThat(metrics.complexity()).isZero();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void countsMultilineStringInteriorLinesAsCode() {
        TSqlSourceMetrics metrics = TSqlSourceMetrics.fromText(
                "SET @sql = 'line 1\nline 2\nline 3';\nSELECT 1;");

        assertThat(metrics.lines()).isEqualTo(4);
        assertThat(metrics.commentLines()).isZero();
        assertThat(metrics.nonCommentLines()).isEqualTo(4);
        assertThat(metrics.statements()).isEqualTo(2);
        assertThat(metrics.complexity()).isZero();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void ignoresBracketQuotedIdentifiersWhenCountingKeywordsAndStatements() {
        TSqlSourceMetrics metrics = TSqlSourceMetrics.fromText(
                "SELECT [IF], [CASE], [WHILE], [GO] FROM [table];\nSELECT [IF] FROM [GO];");

        assertThat(metrics.lines()).isEqualTo(2);
        assertThat(metrics.commentLines()).isZero();
        assertThat(metrics.nonCommentLines()).isEqualTo(2);
        assertThat(metrics.statements()).isEqualTo(2);
        assertThat(metrics.complexity()).isZero();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void handlesNestedBlockCommentsWithoutLeakingControlFlow() {
        TSqlSourceMetrics metrics = TSqlSourceMetrics.fromText(
                "/* outer /* inner IF */ still comment */\nSELECT 1;\n/* outer IF /* inner WHILE */ still comment */\nSELECT CASE WHEN 1=1 THEN 2 ELSE 3 END;");

        assertThat(metrics.lines()).isEqualTo(4);
        assertThat(metrics.commentLines()).isEqualTo(2);
        assertThat(metrics.nonCommentLines()).isEqualTo(2);
        assertThat(metrics.statements()).isEqualTo(2);
        assertThat(metrics.complexity()).isEqualTo(1);
    }
}
