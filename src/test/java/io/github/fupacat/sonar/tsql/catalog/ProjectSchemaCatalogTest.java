package io.github.fupacat.sonar.tsql.catalog;

import io.github.fupacat.ParseResult;
import io.github.fupacat.TSqlParser;
import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.ast.TSqlConcreteFragmentVisitor;
import io.github.fupacat.ast.TSqlScript;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectSchemaCatalogTest {

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testFromInputFilesBuildsProjectWideCatalog() throws IOException {
        InputFile ddlTable = mock(InputFile.class);
        when(ddlTable.contents()).thenReturn(
                "CREATE TABLE dbo.Users (Id INT NOT NULL, LoginName NVARCHAR(50) NULL, CustomerId INT NOT NULL);"
        );

        InputFile ddlIndex = mock(InputFile.class);
        when(ddlIndex.contents()).thenReturn(
                "CREATE INDEX IX_Users_CustomerId ON dbo.Users(CustomerId);"
        );

        ProjectSchemaCatalog catalog = ProjectSchemaCatalog.fromInputFiles(List.of(ddlTable, ddlIndex));

        assertThat(catalog.hasTable("dbo.Users")).isTrue();
        assertThat(catalog.getTable("Users")).isPresent();
        assertThat(catalog.getTable("Users").orElseThrow().getColumn("LoginName")).isPresent();
        assertThat(catalog.getTable("Users").orElseThrow().getColumn("LoginName").orElseThrow().isNullable()).isTrue();
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testFromInputFilesMergesColumnsAcrossDefinitions() throws IOException {
        InputFile file1 = mock(InputFile.class);
        when(file1.contents()).thenReturn("CREATE TABLE Users (Id INT NOT NULL);");

        InputFile file2 = mock(InputFile.class);
        when(file2.contents()).thenReturn("CREATE TABLE Users (DisplayName NVARCHAR(50) NULL);");

        ProjectSchemaCatalog catalog = ProjectSchemaCatalog.fromInputFiles(List.of(file1, file2));

        assertThat(catalog.getTable("Users")).isPresent();
        assertThat(catalog.getTable("Users").orElseThrow().getColumns()).containsKeys("id", "displayname");
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testFromInputFilesSkipsUnreadableOrNullFiles() throws IOException {
        InputFile unreadable = mock(InputFile.class);
        when(unreadable.contents()).thenThrow(new IOException("cannot read"));

        InputFile readable = mock(InputFile.class);
        when(readable.contents()).thenReturn("CREATE TABLE Orders (OrderId INT NOT NULL);");

        ProjectSchemaCatalog catalog = ProjectSchemaCatalog.fromInputFiles(Arrays.asList(null, unreadable, readable));

        assertThat(catalog.hasTable("Orders")).isTrue();
        assertThat(catalog.hasTable("Missing")).isFalse();
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testResolveColumnAndIndexedLookups() {
        ProjectSchemaCatalog catalog = catalogFromSql(
                "CREATE TABLE dbo.Orders (OrderId INT NOT NULL, CustomerId INT NULL);"
                        + "CREATE INDEX IX_Orders_CustomerId ON dbo.Orders(CustomerId);"
        );

        ColumnReferenceExpression customerIdRef = columnReferenceFrom(
                "SELECT Orders.CustomerId FROM dbo.Orders WHERE Orders.CustomerId IN (1, 2)",
                "CustomerId"
        );

        assertThat(catalog.resolveColumn(customerIdRef)).isPresent();
        assertThat(catalog.isNullable(customerIdRef)).isTrue();
        assertThat(catalog.isIndexed(customerIdRef)).isTrue();
        assertThat(catalog.typeOf(customerIdRef)).isPresent();
        assertThat(catalog.typeOf(customerIdRef).orElseThrow().getTypeName()).isEqualTo("int");
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testUnknownColumnLookupsReturnEmpty() {
        ProjectSchemaCatalog catalog = catalogFromSql("CREATE TABLE Orders (OrderId INT NOT NULL);");

        ColumnReferenceExpression unknownRef = columnReferenceFrom("SELECT Missing FROM Orders", "Missing");

        assertThat(catalog.resolveColumn(unknownRef)).isEmpty();
        assertThat(catalog.typeOf(unknownRef)).isEmpty();
        assertThat(catalog.isIndexed(unknownRef)).isFalse();
        assertThat(catalog.isNullable(unknownRef)).isFalse();
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testEmptyCatalogHandlesNullOrBlankInputs() {
        ProjectSchemaCatalog catalog = ProjectSchemaCatalog.empty();

        catalog.indexScript(null);

        assertThat(catalog.hasTable(null)).isFalse();
        assertThat(catalog.hasTable("")).isFalse();
        assertThat(catalog.getTable(" ")).isEmpty();
        assertThat(catalog.resolveColumn(null)).isEmpty();
        assertThat(catalog.typeOf(null)).isEmpty();
        assertThat(catalog.isIndexed(null)).isFalse();
        assertThat(catalog.isNullable(null)).isFalse();
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testUnqualifiedColumnLookupFallsBackAcrossTables() {
        ProjectSchemaCatalog catalog = catalogFromSql(
                "CREATE TABLE SalesOrders (OrderId INT NOT NULL, CustomerId INT NOT NULL);"
                        + "CREATE INDEX IX_SalesOrders_CustomerId ON SalesOrders(CustomerId);"
        );
        ColumnReferenceExpression customerIdRef = columnReferenceFrom("SELECT CustomerId FROM SalesOrders", "CustomerId");

        assertThat(catalog.resolveColumn(customerIdRef)).isPresent();
        assertThat(catalog.isIndexed(customerIdRef)).isTrue();
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testPrimaryKeyAndUniqueConstraintsCreateIndexedColumns() {
        ProjectSchemaCatalog catalog = catalogFromSql(
                "CREATE TABLE Orders (OrderId INT NOT NULL, CustomerId INT NOT NULL, CONSTRAINT PK_Orders PRIMARY KEY (OrderId), CONSTRAINT UQ_Orders_CustomerId UNIQUE(CustomerId));"
        );

        ColumnReferenceExpression orderIdRef = columnReferenceFrom("SELECT OrderId FROM Orders WHERE OrderId IN (1, 2)", "OrderId");
        ColumnReferenceExpression customerIdRef = columnReferenceFrom("SELECT CustomerId FROM Orders WHERE CustomerId IN (1, 2)", "CustomerId");

        assertThat(orderIdRef).isNotNull();
        assertThat(customerIdRef).isNotNull();
        assertThat(catalog.isIndexed(orderIdRef)).isTrue();
        assertThat(catalog.isIndexed(customerIdRef)).isTrue();
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testTypeInfoComparisonWithinAndAcrossFamilies() {
        ProjectSchemaCatalog.TypeInfo intType = new ProjectSchemaCatalog.TypeInfo("INT", null);
        ProjectSchemaCatalog.TypeInfo smallIntType = new ProjectSchemaCatalog.TypeInfo("SMALLINT", null);
        ProjectSchemaCatalog.TypeInfo varchar100 = new ProjectSchemaCatalog.TypeInfo("VARCHAR", 100);
        ProjectSchemaCatalog.TypeInfo nvarchar10 = new ProjectSchemaCatalog.TypeInfo("NVARCHAR", 10);
        ProjectSchemaCatalog.TypeInfo nvarchar100 = new ProjectSchemaCatalog.TypeInfo("NVARCHAR", 100);
        ProjectSchemaCatalog.TypeInfo unknownType = new ProjectSchemaCatalog.TypeInfo("sql_variant", null);
        ProjectSchemaCatalog.TypeInfo datetime = new ProjectSchemaCatalog.TypeInfo("datetime", null);
        ProjectSchemaCatalog.TypeInfo date = new ProjectSchemaCatalog.TypeInfo("date", null);
        ProjectSchemaCatalog.TypeInfo varbinary16 = new ProjectSchemaCatalog.TypeInfo("varbinary", 16);
        ProjectSchemaCatalog.TypeInfo varbinary32 = new ProjectSchemaCatalog.TypeInfo("varbinary", 32);

        assertThat(smallIntType.isNarrowerThan(intType)).isTrue();
        assertThat(intType.isNarrowerThan(smallIntType)).isFalse();
        assertThat(intType.isNarrowerThan(varchar100)).isFalse();
        assertThat(nvarchar10.isNarrowerThan(nvarchar100)).isTrue();
        assertThat(date.isNarrowerThan(datetime)).isTrue();
        assertThat(varbinary16.isNarrowerThan(varbinary32)).isTrue();
        assertThat(unknownType.isNarrowerThan(intType)).isFalse();
        assertThat(ProjectSchemaCatalog.TypeInfo.fromSqlType(null, null).getTypeName()).isEqualTo("unknown");
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testTypeExtractionSupportsParameterizedTypes() {
        ProjectSchemaCatalog catalog = catalogFromSql("CREATE TABLE Audit (Message NVARCHAR(200) NULL, Payload VARBINARY(16) NULL);");

        ProjectSchemaCatalog.ColumnInfo message = catalog.getTable("Audit").orElseThrow().getColumn("Message").orElseThrow();
        ProjectSchemaCatalog.ColumnInfo payload = catalog.getTable("Audit").orElseThrow().getColumn("Payload").orElseThrow();

        assertThat(message.getSqlType()).isEqualToIgnoringCase("NVARCHAR");
        assertThat(message.getLength()).isEqualTo(200);
        assertThat(payload.getSqlType()).isEqualToIgnoringCase("VARBINARY");
        assertThat(payload.getLength()).isEqualTo(16);
    }

    private static ProjectSchemaCatalog catalogFromSql(String sql) {
        ParseResult<TSqlScript> result = new TSqlParser().parse(sql);
        ProjectSchemaCatalog catalog = new ProjectSchemaCatalog();
        catalog.indexScript(result.fragment());
        return catalog;
    }

    private static ColumnReferenceExpression columnReferenceFrom(String sql, String targetColumnName) {
        ParseResult<TSqlScript> result = new TSqlParser().parse(sql);
        final ColumnReferenceExpression[] found = new ColumnReferenceExpression[1];
        if (result == null || result.fragment() == null) {
            return null;
        }
        result.fragment().accept(new TSqlConcreteFragmentVisitor() {
            @Override
            public void visit(ColumnReferenceExpression node) {
                if (node != null && node.MultiPartIdentifier != null && node.MultiPartIdentifier.Identifiers != null) {
                    for (var identifier : node.MultiPartIdentifier.Identifiers) {
                        if (identifier != null && targetColumnName.equalsIgnoreCase(identifier.Value)) {
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
