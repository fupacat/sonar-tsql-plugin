package io.github.fupacat.sonar.tsql.catalog;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SourceNullabilityTest {
    // TRACEABILITY_MAPPING: REQ-017
    @Test
    void declarationsAreBoundedAndWhitespaceIndependent() {
        var source = new SourceNullability("CREATE TABLE dbo.Items(Code VARCHAR(3) NULL, Id INT NOT\t /* x */ NULL, Amount DECIMAL(9,2));"
                + "CREATE TABLE archive.Items(Code VARCHAR(3) NOT    NULL);");
        assertThat(source.nullable("dbo.Items", "Code")).contains(true);
        assertThat(source.nullable("dbo.Items", "Id")).contains(false);
        assertThat(source.nullable("archive.Items", "Code")).contains(false);
        assertThat(source.nullable("dbo.Items", "Amount")).isEmpty();
        assertThat(source.nullable("Items", "Code")).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-017
    @Test
    void commentsStringsAndNestedExpressionsCannotDeclareNullability() {
        var source = new SourceNullability("-- CREATE TABLE Fake(Code INT NULL);\n"
                + "CREATE TABLE [dbo].[Items]([Code] VARCHAR(3) /* NOT /* nested */ NULL */ NULL, "
                + "Other VARCHAR(3) DEFAULT 'NOT NULL, CREATE TABLE Fake(Code INT NULL)', "
                + "Checked INT CHECK (Checked IS NOT NULL), CONSTRAINT CK CHECK (Code IS NOT NULL));");
        assertThat(source.nullable("dbo.Items", "Code")).contains(true);
        assertThat(source.nullable("dbo.Items", "Other")).isEmpty();
        assertThat(source.nullable("dbo.Items", "Checked")).isEmpty();
        assertThat(source.nullable("Fake", "Code")).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-017
    @Test
    void conflictingAndIncompleteDeclarationsRemainUnknown() {
        var source = new SourceNullability("CREATE TABLE Items(Code INT NULL); CREATE TABLE Items(Code INT NOT NULL);"
                + "CREATE TABLE Broken(Code VARCHAR(3) NOT NULL");
        assertThat(source.nullable("Items", "Code")).isEmpty();
        assertThat(source.nullable("Broken", "Code")).isEmpty();
        assertThat(new SourceNullability(null).nullable("Items", "Code")).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-017
    @Test
    void quotedIdentifiersAndLongDeclarationsRemainBounded() {
        var source = new SourceNullability("CREATE TABLE \"dbo\".\"Odd\"([a]]b] VARCHAR(3) "
                + "/* " + "padding ".repeat(30) + " */ NOT\r\nNULL, [NULL] INT NULL); -- tail\r"
                + "SELECT 'CREATE TABLE Pretend(Code INT NULL)'; CREATE TABLE Incomplete(Code INT DEFAULT 'unclosed");
        assertThat(source.nullable("dbo.Odd", "a]b")).contains(false);
        assertThat(source.nullable("dbo.Odd", "NULL")).contains(true);
        assertThat(source.nullable("Pretend", "Code")).isEmpty();
        assertThat(source.nullable("Incomplete", "Code")).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-017
    @Test
    void conflictingFilesDoNotProveNullability() {
        var nullable = io.github.fupacat.TSqlParser.parse("CREATE TABLE Items(Code VARCHAR(3) NULL);");
        nullable.fragment().Value = "CREATE TABLE Items(Code VARCHAR(3) NULL);";
        var required = io.github.fupacat.TSqlParser.parse("CREATE TABLE Items(Code VARCHAR(3) NOT NULL);");
        required.fragment().Value = "CREATE TABLE Items(Code VARCHAR(3) NOT NULL);";
        for (var files : java.util.List.of(java.util.List.of(nullable, required), java.util.List.of(required, nullable))) {
            var catalog = ProjectSchemaCatalog.fromParsedResults(files);
            assertThat(catalog.getTable("Items").orElseThrow().getColumn("Code").orElseThrow().nullabilityKnown()).isFalse();
        }
    }
}
