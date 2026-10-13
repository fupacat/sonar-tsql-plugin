package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.sonar.tsql.catalog.ProjectSchemaCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaAwareChecksTest {

    private final InPredicateIndexedColumnCheck indexedColumnCheck = new InPredicateIndexedColumnCheck();
    private final NullableColumnIsNullCheck nullableColumnCheck = new NullableColumnIsNullCheck();
    private final NarrowingCastCheck narrowingCastCheck = new NarrowingCastCheck();

    @Test
    void testInPredicateOnUnindexedColumn() {
        List<String> issues = CheckTestUtils.scan(indexedColumnCheck,
                "CREATE TABLE Orders (OrderId INT, CustomerId INT, Total DECIMAL(18,2)); " +
                        "CREATE INDEX IX_Orders_OrderId ON Orders(OrderId); " +
                        "SELECT CustomerId FROM Orders WHERE CustomerId IN (1, 2, 3);");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testNullableColumnUsesIsNull() {
        List<String> issues = CheckTestUtils.scan(nullableColumnCheck,
                "CREATE TABLE Users (Id INT NOT NULL, LastLogin DATETIME NULL); " +
                        "SELECT COALESCE(LastLogin, '1970-01-01') FROM Users;");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testNarrowingCastFlagged() {
        List<String> issues = CheckTestUtils.scan(narrowingCastCheck,
                "CREATE TABLE Orders (CustomerId INT, OrderId INT); " +
                        "SELECT CAST(CustomerId AS SMALLINT) FROM Orders;");
        assertThat(issues).hasSize(1);
    }
}
