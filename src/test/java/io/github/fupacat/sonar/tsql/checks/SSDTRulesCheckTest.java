package io.github.fupacat.sonar.tsql.checks;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
class SSDTRulesCheckTest {
    private final SargableWhereCheck sargableWhereCheck = new SargableWhereCheck();
    private final UseScopeIdentityCheck useScopeIdentityCheck = new UseScopeIdentityCheck();
    private final AvoidVariableLengthDataTypeCheck avoidVariableLengthDataTypeCheck = new AvoidVariableLengthDataTypeCheck();
    private final AvoidDeprecatedJoinSyntaxCheck avoidDeprecatedJoinSyntaxCheck = new AvoidDeprecatedJoinSyntaxCheck();
    private final AvoidSpecialCharactersInObjectNamesCheck avoidSpecialCharactersInObjectNamesCheck = new AvoidSpecialCharactersInObjectNamesCheck();
    private final AvoidReservedWordsInObjectNamesCheck avoidReservedWordsInObjectNamesCheck = new AvoidReservedWordsInObjectNamesCheck();
    private final StoredProcedureOutputParameterCheck storedProcedureOutputParameterCheck = new StoredProcedureOutputParameterCheck();
    private final DeterministicFunctionInWhereCheck deterministicFunctionInWhereCheck = new DeterministicFunctionInWhereCheck();
    private final AvoidSpPrefixProcedureNameCheck avoidSpPrefixProcedureNameCheck = new AvoidSpPrefixProcedureNameCheck();
    @Test
    void testSargableWhereCheck() {
        assertThat(CheckTestUtils.scan(sargableWhereCheck, "SELECT * FROM Orders WHERE ABS(OrderTotal) > 100;")).hasSize(1);
        assertThat(CheckTestUtils.scan(sargableWhereCheck, "SELECT * FROM Orders WHERE OrderTotal > 100;")).isEmpty();
    }
    @Test
    void testUseScopeIdentityCheck() {
        assertThat(CheckTestUtils.scan(useScopeIdentityCheck, "SELECT @@IDENTITY;")).hasSize(1);
        assertThat(CheckTestUtils.scan(useScopeIdentityCheck, "SELECT SCOPE_IDENTITY();")).isEmpty();
    }
    @Test
    void testAvoidVariableLengthDataTypeCheck() {
        List<String> issues = CheckTestUtils.scan(avoidVariableLengthDataTypeCheck, "CREATE TABLE T (Code VARCHAR(1), Name NVARCHAR(2), Photo VARBINARY(1), Value VARCHAR(10), Other INT);");
        assertThat(issues).hasSize(3);
    }
    @Test
    void testAvoidDeprecatedJoinSyntaxCheck() {
        List<String> issues = CheckTestUtils.scanWithContents(avoidDeprecatedJoinSyntaxCheck,
                "SELECT * FROM Customers c, Orders o WHERE c.Id *= o.CustomerId;");
        assertThat(issues).hasSize(1);
        List<String> issues2 = CheckTestUtils.scanWithContents(avoidDeprecatedJoinSyntaxCheck,
                "SELECT * FROM Customers c, Orders o WHERE c.Id =* o.CustomerId;");
        assertThat(issues2).hasSize(1);
        List<String> ok = CheckTestUtils.scanWithContents(avoidDeprecatedJoinSyntaxCheck,
                "SELECT * FROM Customers c LEFT JOIN Orders o ON c.Id = o.CustomerId;");
        assertThat(ok).isEmpty();
    }
    @Test
    void testAvoidSpecialAndReservedWordsInObjectNamesCheck() {
        assertThat(CheckTestUtils.scan(avoidSpecialCharactersInObjectNamesCheck,
                "CREATE TABLE dbo.[Order$] (Id INT);")).hasSize(1);
        assertThat(CheckTestUtils.scan(avoidSpecialCharactersInObjectNamesCheck,
                "CREATE TABLE dbo.OrderSummary (Id INT);")).isEmpty();
        assertThat(CheckTestUtils.scan(avoidReservedWordsInObjectNamesCheck,
                "CREATE TABLE dbo.[Order] (Id INT);")).hasSize(1);
        assertThat(CheckTestUtils.scan(avoidReservedWordsInObjectNamesCheck,
                "CREATE TABLE dbo.Orders (Id INT);")).isEmpty();
    }
    @Test
    void testStoredProcedureOutputParameterCheck() {
        assertThat(CheckTestUtils.scan(storedProcedureOutputParameterCheck,
                "CREATE PROCEDURE dbo.GetValue @Result INT OUTPUT AS SET @Result = 1;"))
                .isEmpty();
        assertThat(CheckTestUtils.scan(storedProcedureOutputParameterCheck,
                "CREATE PROCEDURE dbo.GetValue @Result INT OUTPUT AS SELECT @Result = 10;"))
                .isEmpty();
        assertThat(CheckTestUtils.scan(storedProcedureOutputParameterCheck,
                "CREATE PROCEDURE dbo.GetValue @Result INT OUTPUT AS UPDATE dbo.Orders SET @Result = 1;"))
                .isEmpty();
        assertThat(CheckTestUtils.scan(storedProcedureOutputParameterCheck,
                "CREATE PROCEDURE dbo.GetValue @Result INT OUTPUT AS SELECT 1;"))
                .hasSize(1);
        assertThat(CheckTestUtils.scan(storedProcedureOutputParameterCheck,
                "CREATE PROCEDURE dbo.GetValue @Input INT AS SELECT 1;"))
                .isEmpty();
    }

    @Test
    void testSargableWhereCheckIgnoresNonWhereComparisons() {
        assertThat(CheckTestUtils.scan(sargableWhereCheck,
                "SELECT CASE WHEN ABS(OrderTotal) > 0 THEN 1 END AS Flag FROM Orders;"))
                .isEmpty();
    }
    @Test
    void testDeterministicFunctionInWhereCheck() {
        assertThat(CheckTestUtils.scan(deterministicFunctionInWhereCheck,
                "SELECT * FROM Orders WHERE ABS(Total) > 500;"))
                .hasSize(1);
        assertThat(CheckTestUtils.scan(deterministicFunctionInWhereCheck,
                "SELECT * FROM Orders WHERE Total > 500;"))
                .isEmpty();
    }
    @Test
    void testAvoidSpPrefixProcedureNameCheck() {
        assertThat(CheckTestUtils.scan(avoidSpPrefixProcedureNameCheck,
                "CREATE PROCEDURE dbo.sp_GetOrders AS SELECT 1;"))
                .hasSize(1);
        assertThat(CheckTestUtils.scan(avoidSpPrefixProcedureNameCheck,
                "CREATE PROCEDURE sp_GetOrders AS SELECT 1;"))
                .hasSize(1);
        assertThat(CheckTestUtils.scan(avoidSpPrefixProcedureNameCheck,
                "CREATE PROCEDURE dbo.GetOrders AS SELECT 1;"))
                .isEmpty();
    }

    @Test
    void testSargableWhereCheckAdditionalBranches() {
        assertThat(CheckTestUtils.scan(sargableWhereCheck,
                "SELECT * FROM Orders WHERE OrderTotal + 10 > 100 AND (Qty * 2) < 50;"))
                .hasSize(1);
        assertThat(CheckTestUtils.scan(sargableWhereCheck,
                "SELECT * FROM Orders WHERE (OrderTotal > 100) OR (Qty < 50);"))
                .isEmpty();
    }

    @Test
    void testDeterministicFunctionInWhereAdditionalBranches() {
        assertThat(CheckTestUtils.scan(deterministicFunctionInWhereCheck,
                "SELECT * FROM Orders WHERE ABS(Total) > 500 AND ROUND(Tax, 2) < 10;"))
                .hasSize(1);
    }
}
