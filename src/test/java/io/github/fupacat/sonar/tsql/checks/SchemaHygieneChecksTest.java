package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaHygieneChecksTest {

    private final PrimaryKeyRequiredCheck primaryKeyRequiredCheck = new PrimaryKeyRequiredCheck();
    private final InsertColumnsExplicitCheck insertColumnsExplicitCheck = new InsertColumnsExplicitCheck();
    private final ExplicitLengthForVariableTypesCheck explicitLengthCheck = new ExplicitLengthForVariableTypesCheck();
    private final OrderByColumnNamesCheck orderByColumnNamesCheck = new OrderByColumnNamesCheck();
    private final ExistsSelectOneCheck existsSelectOneCheck = new ExistsSelectOneCheck();
    private final AlterTableCheckEnforcementCheck alterTableCheckEnforcementCheck = new AlterTableCheckEnforcementCheck();
    private final ColumnReferenceDepthCheck columnReferenceDepthCheck = new ColumnReferenceDepthCheck();

    @Test
    void testPrimaryKeyRequired() {
        assertThat(primaryKeyRequiredCheck.getRuleKey()).isEqualTo("S1614");
        List<String> issues = CheckTestUtils.scan(primaryKeyRequiredCheck,
                "CREATE TABLE Orders (OrderId INT, CustomerId INT);");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testPrimaryKeyCompliant() {
        List<String> issues = CheckTestUtils.scan(primaryKeyRequiredCheck,
                "CREATE TABLE Orders (OrderId INT, CONSTRAINT PK_Orders PRIMARY KEY (OrderId));");
        assertThat(issues).isEmpty();
    }

    @Test
    void testInsertColumnsExplicit() {
        assertThat(insertColumnsExplicitCheck.getRuleKey()).isEqualTo("S1745");
        List<String> issues = CheckTestUtils.scan(insertColumnsExplicitCheck,
                "INSERT INTO Orders VALUES (1, 'Active');");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testInsertColumnsExplicitCompliant() {
        List<String> issues = CheckTestUtils.scan(insertColumnsExplicitCheck,
                "INSERT INTO Orders (OrderId, Status) VALUES (1, 'Active');");
        assertThat(issues).isEmpty();
    }

    @Test
    void testExplicitLengthForVariableTypes() {
        assertThat(explicitLengthCheck.getRuleKey()).isEqualTo("S4078");
        List<String> issues = CheckTestUtils.scan(explicitLengthCheck,
                "CREATE TABLE Users (Name VARCHAR, LastName NVARCHAR, Photo VARBINARY);");
        assertThat(issues).hasSize(3);
    }

    @Test
    void testExplicitLengthCompliant() {
        List<String> issues = CheckTestUtils.scan(explicitLengthCheck,
                "CREATE TABLE Users (Name VARCHAR(50), LastName NVARCHAR(50), Photo VARBINARY(256));");
        assertThat(issues).isEmpty();
    }

    @Test
    void testOrderByOrdinalIsViolated() {
        assertThat(orderByColumnNamesCheck.getRuleKey()).isEqualTo("S1735");
        List<String> issues = CheckTestUtils.scan(orderByColumnNamesCheck,
                "SELECT Id FROM Orders ORDER BY 1, 2;");
        assertThat(issues).hasSize(2);
    }

    @Test
    void testOrderByColumnNamesCompliant() {
        List<String> issues = CheckTestUtils.scan(orderByColumnNamesCheck,
                "SELECT Id FROM Orders ORDER BY OrderDate, CustomerId;");
        assertThat(issues).isEmpty();
    }

    @Test
    void testExistsSelectColumnListIsViolated() {
        assertThat(existsSelectOneCheck.getRuleKey()).isEqualTo("S4801");
        List<String> issues = CheckTestUtils.scan(existsSelectOneCheck,
                "SELECT * FROM Customers c WHERE EXISTS (SELECT CustomerId FROM Orders o WHERE o.CustomerId = c.Id);");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testExistsSelectOneCompliant() {
        List<String> issues = CheckTestUtils.scan(existsSelectOneCheck,
                "SELECT * FROM Customers c WHERE EXISTS (SELECT 1 FROM Orders o WHERE o.CustomerId = c.Id);");
        assertThat(issues).isEmpty();
    }

    @Test
    void testAlterTableCheckEnforcementExplicit() {
        assertThat(alterTableCheckEnforcementCheck.getRuleKey()).isEqualTo("S4102");
        List<String> issues = CheckTestUtils.scan(alterTableCheckEnforcementCheck,
                "ALTER TABLE Orders ADD CONSTRAINT CK_Order CHECK (Total > 0);");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testAlterTableCheckEnforcementCompliant() {
        List<String> issues = CheckTestUtils.scan(alterTableCheckEnforcementCheck,
                "ALTER TABLE Orders WITH CHECK ADD CONSTRAINT CK_Order CHECK (Total > 0);");
        assertThat(issues).isEmpty();
    }

    @Test
    void testColumnReferenceDepthViolation() {
        assertThat(columnReferenceDepthCheck.getRuleKey()).isEqualTo("S4149");
        List<String> issues = CheckTestUtils.scan(columnReferenceDepthCheck,
                "SELECT [db].[schema].[table].[column] FROM dbo.Table1;");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testPrimaryKeyWithNonPkConstraints() {
        List<String> issues = CheckTestUtils.scan(primaryKeyRequiredCheck,
                "CREATE TABLE Orders (OrderId INT NOT NULL, Total MONEY, CONSTRAINT UQ_Orders UNIQUE (OrderId));");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testExplicitLengthInCastAndConvert() {
        List<String> issues = CheckTestUtils.scan(explicitLengthCheck,
                "SELECT CAST(Name AS VARCHAR), CONVERT(VARBINARY, Photo) FROM Users;");
        assertThat(issues).hasSize(2);
    }

    @Test
    void testExplicitLengthInCastAndConvertCompliant() {
        List<String> issues = CheckTestUtils.scan(explicitLengthCheck,
                "SELECT CAST(Name AS VARCHAR(50)), CONVERT(VARBINARY(128), Photo), CAST(Id AS INT) FROM Users;");
        assertThat(issues).isEmpty();
    }

    @Test
    void testAlterTableConstraintModification() {
        List<String> issues = CheckTestUtils.scan(alterTableCheckEnforcementCheck,
                "ALTER TABLE Orders NOCHECK CONSTRAINT CK_Order;");
        assertThat(issues).hasSize(1);

        List<String> compliant = CheckTestUtils.scan(alterTableCheckEnforcementCheck,
                "ALTER TABLE Orders WITH CHECK CHECK CONSTRAINT CK_Order;");
        assertThat(compliant).isEmpty();
    }

    @Test
    void testExistsSelectStarCompliantAndMultiSelectViolated() {
        List<String> starIssues = CheckTestUtils.scan(existsSelectOneCheck,
                "SELECT * FROM Customers c WHERE EXISTS (SELECT * FROM Orders o WHERE o.CustomerId = c.Id);");
        assertThat(starIssues).isEmpty();

        List<String> multiIssues = CheckTestUtils.scan(existsSelectOneCheck,
                "SELECT * FROM Customers c WHERE EXISTS (SELECT 1, 2 FROM Orders o);");
        assertThat(multiIssues).hasSize(1);
    }

    @Test
    void testColumnReferenceDepthCompliant() {
        List<String> issues = CheckTestUtils.scan(columnReferenceDepthCheck,
                "SELECT OrderId, o.CustomerId FROM dbo.Orders o;");
        assertThat(issues).isEmpty();
    }
}
