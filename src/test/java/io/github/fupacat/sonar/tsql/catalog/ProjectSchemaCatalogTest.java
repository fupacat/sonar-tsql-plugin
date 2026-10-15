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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectSchemaCatalogTest {

    // TRACEABILITY_MAPPING: REQ-016
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

    // TRACEABILITY_MAPPING: REQ-016
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

    // TRACEABILITY_MAPPING: REQ-016
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

    // TRACEABILITY_MAPPING: REQ-016
    @Test
    void testFromParsedResultsIsolatesIndexerFailuresPerScript() {
        ParseResult<TSqlScript> goodResult = TSqlParser.parse("CREATE TABLE dbo.Users (Id INT NOT NULL);");
        TSqlScript badFragment = mock(TSqlScript.class);
        doThrow(new RuntimeException("boom")).when(badFragment).accept(any());
        ParseResult<TSqlScript> badResult = new ParseResult<>(badFragment, List.of());

        ProjectSchemaCatalog catalog = ProjectSchemaCatalog.fromParsedResults(List.of(badResult, goodResult));

        assertThat(catalog.hasTable("dbo.Users")).isTrue();
    }

    // TRACEABILITY_MAPPING: REQ-017
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

    // TRACEABILITY_MAPPING: REQ-016
    @Test
    void testUnknownColumnLookupsReturnEmpty() {
        ProjectSchemaCatalog catalog = catalogFromSql("CREATE TABLE Orders (OrderId INT NOT NULL);");

        ColumnReferenceExpression unknownRef = columnReferenceFrom("SELECT Missing FROM Orders", "Missing");

        assertThat(catalog.resolveColumn(unknownRef)).isEmpty();
        assertThat(catalog.typeOf(unknownRef)).isEmpty();
        assertThat(catalog.isIndexed(unknownRef)).isFalse();
        assertThat(catalog.isNullable(unknownRef)).isFalse();
    }

    // TRACEABILITY_MAPPING: REQ-016
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

    // TRACEABILITY_MAPPING: REQ-016
    @Test
    void testUnqualifiedColumnLookupFallsBackAcrossTables() {
        String sql = "CREATE TABLE SalesOrders (OrderId INT NOT NULL, CustomerId INT NOT NULL);"
                + "CREATE INDEX IX_SalesOrders_CustomerId ON SalesOrders(CustomerId);"
                + "SELECT CustomerId FROM SalesOrders;";
        ProjectSchemaCatalog catalog = catalogFromSql(sql);
        ColumnReferenceExpression customerIdRef = columnReferenceFrom(sql, "CustomerId");

        assertThat(catalog.resolveColumn(customerIdRef)).isPresent();
        assertThat(catalog.isIndexed(customerIdRef)).isTrue();
    }

    // TRACEABILITY_MAPPING: REQ-016
    @Test
    void testQualifiedNamesRemainDistinctAcrossSchemasAndDuplicateTableNames() {
        String sql = "CREATE TABLE dbo.A (Code BIGINT);"
                + "CREATE TABLE dbo.B (Code SMALLINT);"
                + "CREATE TABLE sales.Items (Code BIGINT);"
                + "CREATE TABLE archive.Items (Code SMALLINT);"
                + "CREATE TABLE dbo.Orders (Id INT, CONSTRAINT PK_Orders PRIMARY KEY (Id));"
                + "SELECT CAST(Code AS INT) FROM dbo.B;"
                + "SELECT CAST(Code AS INT) FROM archive.Items;"
                + "SELECT o.Id FROM dbo.Orders AS o WHERE o.Id IN (1, 2);";
        ParseResult<TSqlScript> result = new TSqlParser().parse(sql);
        ProjectSchemaCatalog catalog = new ProjectSchemaCatalog();
        catalog.indexScript(result.fragment());

        ColumnReferenceExpression firstRef = columnReferenceFrom(result.fragment(), "Code", 0);
        ColumnReferenceExpression sameNameRef = columnReferenceFrom(result.fragment(), "Code", 1);
        ColumnReferenceExpression aliasRef = columnReferenceFrom(result.fragment(), "Id", 0);

        assertThat(catalog.resolveColumn(firstRef)).isPresent();
        assertThat(catalog.resolveColumn(firstRef).orElseThrow().getSqlType()).isEqualToIgnoringCase("smallint");
        assertThat(catalog.resolveColumn(sameNameRef).orElseThrow().getSqlType()).isEqualToIgnoringCase("smallint");
        assertThat(catalog.resolveColumn(aliasRef)).isPresent();
        assertThat(catalog.resolveColumn(aliasRef).orElseThrow().getSqlType()).isEqualToIgnoringCase("int");
        assertThat(catalog.isIndexed(aliasRef)).isTrue();
    }

    // TRACEABILITY_MAPPING: REQ-016
    @Test
    void testFileOrderDoesNotChangeQualifiedResolution() {
        ProjectSchemaCatalog firstCatalog = catalogFromSql(
                "CREATE TABLE dbo.A (Code BIGINT);"
                        + "CREATE TABLE dbo.B (Code SMALLINT);"
                        + "SELECT CAST(Code AS INT) FROM dbo.B;"
        );
        ProjectSchemaCatalog secondCatalog = catalogFromSql(
                "CREATE TABLE dbo.B (Code SMALLINT);"
                        + "CREATE TABLE dbo.A (Code BIGINT);"
                        + "SELECT CAST(Code AS INT) FROM dbo.B;"
        );

        ColumnReferenceExpression firstRef = columnReferenceFrom("SELECT CAST(Code AS INT) FROM dbo.B", "Code");
        assertThat(firstCatalog.resolveColumn(firstRef).orElseThrow().getSqlType()).isEqualTo("smallint");
        assertThat(secondCatalog.resolveColumn(firstRef).orElseThrow().getSqlType()).isEqualTo("smallint");
    }

    // TRACEABILITY_MAPPING: REQ-016
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

    // TRACEABILITY_MAPPING: REQ-018
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

        // Edge case: comparing with null or unknown family
        assertThat(intType.isNarrowerThan(null)).isFalse();
        assertThat(new ProjectSchemaCatalog.TypeInfo(null, null).isNarrowerThan(intType)).isFalse();

        // Effective length switch cases coverage
        ProjectSchemaCatalog.TypeInfo tinyintType = new ProjectSchemaCatalog.TypeInfo("tinyint", null);
        ProjectSchemaCatalog.TypeInfo bitType = new ProjectSchemaCatalog.TypeInfo("bit", null);
        ProjectSchemaCatalog.TypeInfo moneyType = new ProjectSchemaCatalog.TypeInfo("money", null);
        ProjectSchemaCatalog.TypeInfo realType = new ProjectSchemaCatalog.TypeInfo("real", null);
        ProjectSchemaCatalog.TypeInfo floatType = new ProjectSchemaCatalog.TypeInfo("float", null);
        ProjectSchemaCatalog.TypeInfo bigintType = new ProjectSchemaCatalog.TypeInfo("bigint", null);
        ProjectSchemaCatalog.TypeInfo smalldatetime = new ProjectSchemaCatalog.TypeInfo("smalldatetime", null);
        ProjectSchemaCatalog.TypeInfo datetime2 = new ProjectSchemaCatalog.TypeInfo("datetime2", null);
        ProjectSchemaCatalog.TypeInfo timeType = new ProjectSchemaCatalog.TypeInfo("time", null);
        ProjectSchemaCatalog.TypeInfo uniqueidentifier = new ProjectSchemaCatalog.TypeInfo("uniqueidentifier", null);
        ProjectSchemaCatalog.TypeInfo charType = new ProjectSchemaCatalog.TypeInfo("char", null);
        ProjectSchemaCatalog.TypeInfo ncharType = new ProjectSchemaCatalog.TypeInfo("nchar", null);
        ProjectSchemaCatalog.TypeInfo textType = new ProjectSchemaCatalog.TypeInfo("text", null);
        ProjectSchemaCatalog.TypeInfo nvarcharmax = new ProjectSchemaCatalog.TypeInfo("nvarcharmax", null);
        ProjectSchemaCatalog.TypeInfo binaryType = new ProjectSchemaCatalog.TypeInfo("binary", null);

        assertThat(bitType.isNarrowerThan(bigintType)).isTrue();
        assertThat(tinyintType.isNarrowerThan(bigintType)).isTrue();
        assertThat(realType.isNarrowerThan(floatType)).isTrue();
        assertThat(moneyType.isNarrowerThan(bigintType)).isTrue();
        assertThat(timeType.isNarrowerThan(datetime2)).isTrue();
        assertThat(smalldatetime.isNarrowerThan(datetime2)).isFalse();
        assertThat(charType.isNarrowerThan(textType)).isTrue();
        assertThat(ncharType.isNarrowerThan(nvarcharmax)).isTrue();
        assertThat(varbinary16.isNarrowerThan(binaryType)).isTrue();
        assertThat(uniqueidentifier.getLength()).isNull();
    }

    // TRACEABILITY_MAPPING: REQ-018
    @Test
    void testTypeExtractionSupportsParameterizedTypes() {
        ProjectSchemaCatalog catalog = catalogFromSql(
                "CREATE TABLE Audit (Message NVARCHAR(200) NULL, Payload VARBINARY(16) NULL, Description VARCHAR(50) NOT NULL); " +
                "CREATE INDEX IX_Audit_Message ON Audit(Message);");

        ProjectSchemaCatalog.ColumnInfo message = catalog.getTable("Audit").orElseThrow().getColumn("Message").orElseThrow();
        ProjectSchemaCatalog.ColumnInfo payload = catalog.getTable("Audit").orElseThrow().getColumn("Payload").orElseThrow();
        ProjectSchemaCatalog.ColumnInfo description = catalog.getTable("Audit").orElseThrow().getColumn("Description").orElseThrow();

        assertThat(message.getSqlType()).isEqualToIgnoringCase("NVARCHAR");
        assertThat(message.getLength()).isEqualTo(200);
        assertThat(payload.getSqlType()).isEqualToIgnoringCase("VARBINARY");
        assertThat(payload.getLength()).isEqualTo(16);
        assertThat(description.getLength()).isEqualTo(50);
        assertThat(description.isNullable()).isTrue();
    }

    // TRACEABILITY_MAPPING: REQ-016
    @Test
    void testTableSchemaAndCatalogEdgeCases() {
        ProjectSchemaCatalog.TableSchema tableSchema = new ProjectSchemaCatalog.TableSchema("dbo.TestTable");
        assertThat(tableSchema.getTableName()).isEqualTo("dbo.TestTable");
        assertThat(tableSchema.getColumn(null)).isEmpty();
        assertThat(tableSchema.getColumn("")).isEmpty();
        tableSchema.addColumn(null);
        tableSchema.addColumn(new ProjectSchemaCatalog.ColumnInfo(null, false, "int", 4));
        tableSchema.addColumn(new ProjectSchemaCatalog.ColumnInfo("", false, "int", 4));
        assertThat(tableSchema.getColumns()).isEmpty();

        // 3-part schema name
        ProjectSchemaCatalog catalog = catalogFromSql(
                "CREATE TABLE mydb.dbo.Inventory (ItemId INT NOT NULL); " +
                "CREATE INDEX IX_Inventory_ItemId ON mydb.dbo.Inventory(ItemId);");
        assertThat(catalog.hasTable("mydb.dbo.Inventory")).isTrue();
        assertThat(catalog.hasTable("Inventory")).isTrue();

        // Column with inline index and table-level index
        ProjectSchemaCatalog inlineIndexCatalog = catalogFromSql(
                "CREATE TABLE Products (ProductId INT NOT NULL INDEX IX_Products_ProductId (ProductId), " +
                "Code MyCustomType(25), INDEX IX_Products_Code (Code));");
        assertThat(inlineIndexCatalog.hasTable("Products")).isTrue();
        assertThat(inlineIndexCatalog.getTable("Products").orElseThrow().getColumn("Code")).isPresent();
        assertThat(inlineIndexCatalog.getTable("Products").orElseThrow().getColumn("Code").orElseThrow().getSqlType()).isEqualTo("MyCustomType");
        assertThat(inlineIndexCatalog.getTable("Products").orElseThrow().getColumn("Code").orElseThrow().getLength()).isEqualTo(25);

        // Column with inline UniqueConstraintDefinition
        ProjectSchemaCatalog uqCatalog = new ProjectSchemaCatalog();
        var createStmt = new io.github.fupacat.ast.CreateTableStatement();
        createStmt.SchemaObjectName = new io.github.fupacat.ast.SchemaObjectName();
        createStmt.SchemaObjectName.BaseIdentifier = new io.github.fupacat.ast.Identifier();
        createStmt.SchemaObjectName.BaseIdentifier.Value = "TestTable";
        createStmt.Definition = new io.github.fupacat.ast.TableDefinition();
        var colDef = new io.github.fupacat.ast.ColumnDefinition();
        colDef.ColumnIdentifier = new io.github.fupacat.ast.Identifier();
        colDef.ColumnIdentifier.Value = "ColA";
        var uq = new io.github.fupacat.ast.UniqueConstraintDefinition();
        colDef.Constraints = List.of(uq);
        createStmt.Definition.ColumnDefinitions = List.of(colDef);
        uqCatalog.indexScript(createStmt);
        assertThat(uqCatalog.hasTable("TestTable")).isTrue();

        // Empty multi-part identifier resolution
        var emptyRef = new ColumnReferenceExpression();
        emptyRef.MultiPartIdentifier = new io.github.fupacat.ast.MultiPartIdentifier();
        emptyRef.MultiPartIdentifier.Identifiers = List.of();
        assertThat(uqCatalog.resolveColumn(emptyRef)).isEmpty();

        // Standalone index edge cases (null OnName / null columns)
        var createIdx = new io.github.fupacat.ast.CreateIndexStatement();
        uqCatalog.indexScript(createIdx);
        createIdx.OnName = new io.github.fupacat.ast.SchemaObjectName();
        uqCatalog.indexScript(createIdx);
        createIdx.Columns = List.of();
        uqCatalog.indexScript(createIdx);
    }

    private static ProjectSchemaCatalog catalogFromSql(String sql) {
        ParseResult<TSqlScript> result = new TSqlParser().parse(sql);
        ProjectSchemaCatalog catalog = new ProjectSchemaCatalog();
        catalog.indexScript(result.fragment());
        return catalog;
    }

    private static ColumnReferenceExpression columnReferenceFrom(String sql, String targetColumnName) {
        return columnReferenceFrom(sql, targetColumnName, 0);
    }

    private static ColumnReferenceExpression columnReferenceFrom(String sql, String targetColumnName, int occurrence) {
        ParseResult<TSqlScript> result = new TSqlParser().parse(sql);
        return columnReferenceFrom(result.fragment(), targetColumnName, occurrence);
    }

    private static ColumnReferenceExpression columnReferenceFrom(TSqlScript script, String targetColumnName, int occurrence) {
        final ColumnReferenceExpression[] found = new ColumnReferenceExpression[1];
        if (script == null) {
            return null;
        }
        final int[] index = {0};
        script.accept(new TSqlConcreteFragmentVisitor() {
            @Override
            public void visit(ColumnReferenceExpression node) {
                if (found[0] != null) {
                    return;
                }
                if (node != null && node.MultiPartIdentifier != null && node.MultiPartIdentifier.Identifiers != null) {
                    for (var identifier : node.MultiPartIdentifier.Identifiers) {
                        if (identifier != null && targetColumnName.equalsIgnoreCase(identifier.Value)) {
                            if (index[0] == occurrence) {
                                found[0] = node;
                                return;
                            }
                            index[0]++;
                        }
                    }
                }
                super.visit(node);
            }
        });
        return found[0];
    }
}
