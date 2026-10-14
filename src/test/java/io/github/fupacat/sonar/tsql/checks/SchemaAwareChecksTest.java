package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ParseResult;
import io.github.fupacat.TSqlParser;
import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.ast.TSqlConcreteFragmentVisitor;
import io.github.fupacat.ast.TSqlScript;
import io.github.fupacat.sonar.tsql.catalog.ProjectSchemaCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaAwareChecksTest {

    private final InPredicateIndexedColumnCheck indexedColumnCheck = new InPredicateIndexedColumnCheck();
    private final NullableColumnIsNullCheck nullableColumnCheck = new NullableColumnIsNullCheck();
    private final NarrowingCastCheck narrowingCastCheck = new NarrowingCastCheck();

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testInPredicateOnUnindexedColumn() {
        List<String> issues = CheckTestUtils.scan(indexedColumnCheck,
                "CREATE TABLE Orders (OrderId INT, CustomerId INT, Total DECIMAL(18,2)); " +
                        "CREATE INDEX IX_Orders_OrderId ON Orders(OrderId); " +
                        "SELECT CustomerId FROM Orders WHERE CustomerId IN (1, 2, 3);");
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("not indexed");
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testInPredicateOnIndexedColumnIsCompliant() {
        List<String> issues = CheckTestUtils.scan(indexedColumnCheck,
                "CREATE TABLE Orders (OrderId INT, CustomerId INT, Total DECIMAL(18,2)); " +
                        "CREATE INDEX IX_Orders_CustomerId ON Orders(CustomerId); " +
                        "SELECT CustomerId FROM Orders WHERE CustomerId IN (1, 2, 3);");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-017
    @Test
    void testNullableColumnCoalesceTwoArgumentFallbackIsCompliant() {
        List<String> issues = CheckTestUtils.scan(nullableColumnCheck,
                "CREATE TABLE dbo.Items (Code VARCHAR(3) NULL); " +
                        "SELECT COALESCE(Code, 'fallback') FROM dbo.Items;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-017
    @Test
    void testNullableColumnCoalesceWithNullableFallbackIsCompliant() {
        List<String> issues = CheckTestUtils.scan(nullableColumnCheck,
                "CREATE TABLE dbo.Items (Code VARCHAR(3) NULL, AltCode VARCHAR(10) NULL); " +
                        "SELECT COALESCE(Code, AltCode) FROM dbo.Items;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-017
    @Test
    void testNullableColumnCoalesceMultipleArgumentsFlagged() {
        List<String> issues = CheckTestUtils.scan(nullableColumnCheck,
                "CREATE TABLE dbo.Items (Code VARCHAR(3) NULL); " +
                        "SELECT COALESCE(Code, 'fallback', 'another') FROM dbo.Items;");
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("more than two arguments");
    }

    // TRACEABILITY_MAPPING: REQ-017
    @Test
    void testNullableColumnAlreadyUsingIsNullIsCompliant() {
        List<String> issues = CheckTestUtils.scan(nullableColumnCheck,
                "CREATE TABLE Users (Id INT NOT NULL, LastLogin DATETIME NULL); " +
                        "SELECT ISNULL(LastLogin, '1970-01-01') FROM Users;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testNarrowingCastFlagged() {
        List<String> issues = CheckTestUtils.scan(narrowingCastCheck,
                "CREATE TABLE Orders (CustomerId INT, OrderId INT); " +
                        "SELECT CAST(CustomerId AS SMALLINT) FROM Orders;");
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("Data loss");
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testWideningCastIsCompliant() {
        List<String> issues = CheckTestUtils.scan(narrowingCastCheck,
                "CREATE TABLE Orders (CustomerId SMALLINT, OrderId INT); " +
                        "SELECT CAST(CustomerId AS INT) FROM Orders;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testCrossFamilyCastIsCompliant() {
        List<String> issues = CheckTestUtils.scan(narrowingCastCheck,
                "CREATE TABLE Orders (CustomerCode VARCHAR(100)); " +
                        "SELECT CAST(CustomerCode AS INT) FROM Orders;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testNarrowingConvertAndTryConvertAreFlagged() {
        List<String> convertIssues = CheckTestUtils.scan(narrowingCastCheck,
                "CREATE TABLE Orders (CustomerId BIGINT); " +
                        "SELECT CONVERT(INT, CustomerId) FROM Orders;");
        List<String> tryConvertIssues = CheckTestUtils.scan(narrowingCastCheck,
                "CREATE TABLE Orders (CustomerId BIGINT); " +
                        "SELECT TRY_CONVERT(INT, CustomerId) FROM Orders;");
        List<String> tryCastIssues = CheckTestUtils.scan(narrowingCastCheck,
                "CREATE TABLE Orders (CustomerId BIGINT); " +
                        "SELECT TRY_CAST(CustomerId AS INT) FROM Orders;");
        assertThat(convertIssues).hasSize(1);
        assertThat(tryConvertIssues).hasSize(1);
        assertThat(tryCastIssues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testNarrowingCharacterLengthIsFlagged() {
        List<String> issues = CheckTestUtils.scan(narrowingCastCheck,
                "CREATE TABLE Orders (CustomerCode NVARCHAR(100)); " +
                        "SELECT CAST(CustomerCode AS NVARCHAR(10)) FROM Orders;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testLiteralCastIsIgnored() {
        List<String> issues = CheckTestUtils.scan(narrowingCastCheck,
                "SELECT CAST(123 AS SMALLINT);");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testParameterizedTargetWithMaxLengthIsHandled() {
        List<String> issues = CheckTestUtils.scan(narrowingCastCheck,
                "CREATE TABLE Orders (CustomerCode NVARCHAR(100)); " +
                        "SELECT CAST(CustomerCode AS NVARCHAR(MAX)) FROM Orders;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testUserDefinedTypeAndNullCheckInNarrowingCast() {
        List<String> issues = CheckTestUtils.scan(narrowingCastCheck,
                "CREATE TABLE Items (Code VARCHAR(100)); " +
                        "SELECT CAST(Code AS VARCHAR(10)) FROM Items;");
        assertThat(issues).hasSize(1);

        // Null target / non-column / non-narrowing casts
        assertThat(CheckTestUtils.scan(narrowingCastCheck, "SELECT 1;")).isEmpty();
        assertThat(CheckTestUtils.scan(narrowingCastCheck, "SELECT CAST('hello' AS VARCHAR(10));")).isEmpty();
        assertThat(CheckTestUtils.scan(narrowingCastCheck, "SELECT CAST(1 + 1 AS INT);")).isEmpty();
        assertThat(CheckTestUtils.scan(narrowingCastCheck, "SELECT CAST(Code AS MyCustomType) FROM Items;")).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-016
    @Test
    void testProjectSchemaCatalogTracksTableMetadata() {
        ParseResult<TSqlScript> result = new TSqlParser().parse(
                "CREATE TABLE Users (Id INT NOT NULL, LastLogin DATETIME NULL); " +
                        "CREATE INDEX IX_Users_Id ON Users(Id); " +
                        "CREATE TABLE Orders (CustomerId INT NOT NULL); " +
                        "CREATE INDEX IX_Orders_CustomerId ON Orders(CustomerId);");
        ProjectSchemaCatalog catalog = new ProjectSchemaCatalog();
        catalog.indexScript(result.fragment());

        assertThat(catalog.hasTable("Users")).isTrue();
        assertThat(catalog.getTable("Users")).isPresent();
        assertThat(catalog.getTable("Users").orElseThrow().getColumn("Id")).isPresent();
        assertThat(catalog.getTable("Users").orElseThrow().getColumn("LastLogin")).isPresent();
        assertThat(catalog.getTable("Users").orElseThrow().getColumn("Id").orElseThrow().toTypeInfo().getTypeName()).isEqualTo("int");
        assertThat(catalog.getTable("Users").orElseThrow().getColumn("LastLogin").orElseThrow().toTypeInfo().getTypeName()).isEqualTo("datetime");

        ColumnReferenceExpression customerId = findColumnReferenceExpression(
                "SELECT CustomerId FROM Orders WHERE CustomerId IN (1, 2, 3);",
                "CustomerId");
        assertThat(customerId).isNotNull();
        assertThat(catalog.isIndexed(customerId)).isTrue();
        assertThat(catalog.typeOf(customerId).orElseThrow().getTypeName()).isEqualTo("int");

        ProjectSchemaCatalog.TypeInfo intType = catalog.getTable("Users").orElseThrow().getColumn("Id").orElseThrow().toTypeInfo();
        assertThat(intType.isNarrowerThan(new ProjectSchemaCatalog.TypeInfo("bigint", null))).isTrue();
    }

    private static ColumnReferenceExpression findColumnReferenceExpression(String sql, String targetColumn) {
        ParseResult<TSqlScript> result = new TSqlParser().parse(sql);
        if (result == null || result.fragment() == null) {
            return null;
        }
        final ColumnReferenceExpression[] found = new ColumnReferenceExpression[1];
        result.fragment().accept(new TSqlConcreteFragmentVisitor() {
            @Override
            public void visit(ColumnReferenceExpression node) {
                if (node != null && node.MultiPartIdentifier != null && node.MultiPartIdentifier.Identifiers != null) {
                    for (var identifier : node.MultiPartIdentifier.Identifiers) {
                        if (identifier != null && targetColumn.equalsIgnoreCase(identifier.Value)) {
                            found[0] = node;
                            break;
                        }
                    }
                }
                super.visit(node);
            }
        });
        return found[0];
    }
}
