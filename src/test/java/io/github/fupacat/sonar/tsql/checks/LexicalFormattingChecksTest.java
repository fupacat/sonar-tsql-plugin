package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LexicalFormattingChecksTest {

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testLineLengthTabsMultistatementCommentedCodeAndTodoMarkers() {
        assertThat(CheckTestUtils.scan(new S103Check(), "SELECT '" + "x".repeat(121) + "';")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S105Check(), "\tSELECT 1;")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S122Check(), "DECLARE @x INT; SELECT 1; ")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S125Check(), "-- SELECT * FROM Orders;\nSELECT 1; ")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S1134Check(), "-- FIXME: revisit this logic\nSELECT 1; ")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S1135Check(), "-- TODO: revisit this logic\nSELECT 1; ")).hasSize(1);
        assertThat(CheckTestUtils.scan(new NoSonarCheck(), "SELECT 1; -- NOSONAR\n")).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testFileHeaderAndFormattingRules() {
        assertThat(CheckTestUtils.scan(new S1451Check(), "SELECT 1;\n")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S3972Check(), "IF 1 = 1 BEGIN SELECT 1; END;\n")).hasSize(1);
        assertThat(CheckTestUtils.scan(new S3973Check(), " BEGIN\n    SELECT 1;\nEND;\n")).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testProcedureAndWhenBlockLengthRules() {
        String procedure = String.join("\n",
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
        assertThat(CheckTestUtils.scan(new S138Check(), procedure)).hasSize(1);

        String whenClause = String.join("\n",
                "CASE",
                "    WHEN 1 = 1 THEN 42",
                "    WHEN 2 = 2 THEN 43",
                "    WHEN 3 = 3 THEN 44",
                "    WHEN 4 = 4 THEN 45",
                "    WHEN 5 = 5 THEN 46",
                "    WHEN 6 = 6 THEN 47",
                "    WHEN 7 = 7 THEN 48",
                "    WHEN 8 = 8 THEN 49",
                "    WHEN 9 = 9 THEN 50",
                "    WHEN 10 = 10 THEN 51",
                "    WHEN 11 = 11 THEN 52",
                "    ELSE 53",
                "END");
        assertThat(CheckTestUtils.scan(new S1151Check(), "SELECT " + whenClause + ";")).hasSizeGreaterThanOrEqualTo(1);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testCompliantSourceDoesNotTriggerLexicalRules() {
        String sql = String.join("\n",
                "-- Copyright 2024 Example",
                "-- Licensed under the Apache License, Version 2.0",
                "DECLARE @value INT = 1;",
                "IF @value = 1",
                "BEGIN",
                "    SELECT @value;",
                "END;");

        assertThat(CheckTestUtils.scan(new S103Check(), sql)).isEmpty();
        assertThat(CheckTestUtils.scan(new S105Check(), sql)).isEmpty();
        assertThat(CheckTestUtils.scan(new S122Check(), sql)).isEmpty();
        assertThat(CheckTestUtils.scan(new S125Check(), sql)).isEmpty();
        assertThat(CheckTestUtils.scan(new S1134Check(), sql)).isEmpty();
        assertThat(CheckTestUtils.scan(new S1135Check(), sql)).isEmpty();
        assertThat(CheckTestUtils.scan(new NoSonarCheck(), sql)).isEmpty();
        assertThat(CheckTestUtils.scan(new S1451Check(), sql)).isEmpty();
        assertThat(CheckTestUtils.scan(new S3972Check(), sql)).isEmpty();
        assertThat(CheckTestUtils.scan(new S3973Check(), sql)).isEmpty();
        assertThat(CheckTestUtils.scan(new S138Check(), sql)).isEmpty();
        assertThat(CheckTestUtils.scan(new S1151Check(), sql)).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testFileLengthRule() {
        String sql = String.join("\n", Collections.nCopies(1001, "SELECT 1;"));
        assertThat(CheckTestUtils.scan(new S104Check(), sql)).hasSize(1);
    }
}
