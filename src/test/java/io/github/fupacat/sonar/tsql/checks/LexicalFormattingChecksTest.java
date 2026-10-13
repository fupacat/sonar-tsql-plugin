package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.TSqlParserToken;
import io.github.fupacat.TSqlTokenType;
import io.github.fupacat.ast.TSqlScript;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LexicalFormattingChecksTest {

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void s103AndS104BoundaryCases() {
        String longLine = "SELECT '" + "x".repeat(121) + "';";
        assertThat(CheckTestUtils.scan(new S103Check(), "SELECT 1;")).isEmpty();
        assertThat(CheckTestUtils.scan(new S103Check(), longLine)).hasSize(1);

        String file1000 = String.join("\n", java.util.Collections.nCopies(1000, "SELECT 1;"));
        String file1001 = String.join("\n", java.util.Collections.nCopies(1001, "SELECT 1;"));
        assertThat(CheckTestUtils.scan(new S104Check(), file1000)).isEmpty();
        assertThat(CheckTestUtils.scan(new S104Check(), file1001)).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void s105DetectsTabAndReportsLine() {
        assertThat(CheckTestUtils.scan(new S105Check(), "SELECT 1;\n\tSELECT 2;")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S105Check(), "SELECT 1;\n  SELECT 2;")).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void s122AndS125DetectNonCompliantPatterns() {
        assertThat(CheckTestUtils.scan(new S122Check(), "DECLARE @x INT; SELECT 1;")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S122Check(), "SELECT 1;;")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S122Check(), "SELECT 1;\nSELECT 2;")).isEmpty();

        assertThat(CheckTestUtils.scan(new S125Check(), "-- SELECT * FROM Orders;\nSELECT 1;")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S125Check(), "/* UPDATE T SET C = 1 */\nSELECT 1;")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S125Check(), "-- just a note\nSELECT 1;")).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void markerChecksDetectTodoFixmeAndNosonar() {
        assertThat(CheckTestUtils.scan(new S1134Check(), "-- fixme: revisit\nSELECT 1;")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S1135Check(), "-- TODO: revisit\nSELECT 1;")).hasSize(1);
        assertThat(CheckTestUtils.scan(new NoSonarCheck(), "SELECT 1; -- Nosonar")).hasSize(1);

        String cleanSql = "SELECT 1;\n-- review complete";
        assertThat(CheckTestUtils.scan(new S1134Check(), cleanSql)).isEmpty();
        assertThat(CheckTestUtils.scan(new S1135Check(), cleanSql)).isEmpty();
        assertThat(CheckTestUtils.scan(new NoSonarCheck(), cleanSql)).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void headerAndFormattingChecksCoverCompliantAndNonCompliantCases() {
        String headerSql = String.join("\n",
                "-- Copyright 2024 Example",
                "-- Licensed under Apache-2.0",
                "IF 1 = 1",
                "BEGIN",
                "  SELECT 1;",
                "END;");
        assertThat(CheckTestUtils.scan(new S1451Check(), headerSql)).isEmpty();
        assertThat(CheckTestUtils.scan(new S3972Check(), headerSql)).isEmpty();
        assertThat(CheckTestUtils.scan(new S3973Check(), headerSql)).isEmpty();

        assertThat(CheckTestUtils.scan(new S1451Check(), "SELECT 1;")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S3972Check(), "IF 1 = 1 BEGIN SELECT 1; END;")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S3973Check(), "   BEGIN\nSELECT 1;\n END")).hasSize(2);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void s138AndS1151DetectLongBlocks() {
        String longProcedure = String.join("\n",
                "CREATE PROCEDURE SampleProc AS",
                "BEGIN",
                "SELECT 1;",
                "SELECT 2;",
                "SELECT 3;",
                "SELECT 4;",
                "SELECT 5;",
                "SELECT 6;",
                "SELECT 7;",
                "SELECT 8;",
                "SELECT 9;",
                "SELECT 10;",
                "SELECT 11;",
                "SELECT 12;",
                "SELECT 13;",
                "SELECT 14;",
                "SELECT 15;",
                "SELECT 16;",
                "SELECT 17;",
                "SELECT 18;",
                "SELECT 19;",
                "SELECT 20;",
                "SELECT 21;",
                "END;");
        assertThat(CheckTestUtils.scan(new S138Check(), longProcedure)).hasSize(1);
        assertThat(CheckTestUtils.scan(new S138Check(), "CREATE FUNCTION F() RETURNS INT AS BEGIN RETURN 1; END")).isEmpty();

        String longWhen = String.join("\n",
                "SELECT CASE",
                "WHEN 1=1 THEN 1",
                "WHEN 2=2 THEN 2",
                "WHEN 3=3 THEN 3",
                "WHEN 4=4 THEN 4",
                "WHEN 5=5 THEN 5",
                "WHEN 6=6 THEN 6",
                "WHEN 7=7 THEN 7",
                "WHEN 8=8 THEN 8",
                "WHEN 9=9 THEN 9",
                "WHEN 10=10 THEN 10",
                "WHEN 11=11 THEN 11",
                "ELSE 12 END;");
        String shortWhen = "SELECT CASE WHEN 1=1 THEN 1 WHEN 2=2 THEN 2 ELSE 3 END;";
        assertThat(CheckTestUtils.scan(new S1151Check(), longWhen)).hasSize(1);
        assertThat(CheckTestUtils.scan(new S1151Check(), shortWhen)).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void textRuleSupportHelpersCovered() {
        assertThat(TextRuleSupport.sourceOf(null)).isEmpty();

        TSqlScript withValue = new TSqlScript();
        withValue.Value = "SELECT 1;";
        assertThat(TextRuleSupport.sourceOf(withValue)).isEqualTo("SELECT 1;");

        TSqlScript withTokens = new TSqlScript();
        withTokens.Value = "";
        withTokens.ScriptTokenStream = List.of(
                new TSqlParserToken(TSqlTokenType.Select, "SELECT"),
                new TSqlParserToken(TSqlTokenType.WhiteSpace, " "),
                new TSqlParserToken(TSqlTokenType.IntegerLiteral, "1"));
        assertThat(TextRuleSupport.sourceOf(withTokens)).isEqualTo("SELECT 1");

        assertThat(TextRuleSupport.lines(null)).isEmpty();
        assertThat(TextRuleSupport.lines("a\nb")).containsExactly("a", "b");
        assertThat(TextRuleSupport.matchingLines("A\nB\nA", line -> line.equals("A"))).containsExactly(0, 2);

        assertThat(TextRuleSupport.containsAnyIgnoreCase("NoSoNaR marker", "nosonar")).isTrue();
        assertThat(TextRuleSupport.isCommentLine("/* comment */")).isTrue();
        assertThat(TextRuleSupport.containsCommentFlag("-- FIXME now")).isTrue();
        assertThat(TextRuleSupport.hasKeyword("ALTER TABLE T", "alter")).isTrue();
        assertThat(TextRuleSupport.isMultistatementLine("SELECT 1; SELECT 2")).isTrue();
        assertThat(TextRuleSupport.isMultistatementLine("SELECT 1;;")).isTrue();
        assertThat(TextRuleSupport.isMultistatementLine("SELECT 1")).isFalse();
        assertThat(TextRuleSupport.looksLikeCommentedCode("* DELETE FROM T")).isTrue();
        assertThat(TextRuleSupport.looksLikeCommentedCode("-- note only")).isFalse();
        assertThat(TextRuleSupport.leadingSpaces("    BEGIN")).isEqualTo(4);
        assertThat(TextRuleSupport.hasHeader("-- license text\nSELECT 1;")).isTrue();
    }
}
