package io.github.fupacat.sonar.tsql.rules;

import io.github.fupacat.sonar.tsql.TSqlLanguage;
import io.github.fupacat.sonar.tsql.checks.*;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

/**
 * Declares the T-SQL static analysis rules repository in SonarQube.
 */
public class TSqlRulesDefinition implements RulesDefinition {

    public static final String REPOSITORY_KEY = "tsql";
    public static final String REPOSITORY_NAME = "SonarQube T-SQL Analyzer";
    private static final String TAG_SQL_SERVER = "sql-server";
    private static final String TAG_DATA_SAFETY = "data-safety";
    private static final String TAG_DATA_INTEGRITY = "data-integrity";
    private static final String TAG_SCHEMA = "schema";
    private static final String TAG_MAINTAINABILITY = "maintainability";
    private static final String TAG_FORMATTING = "formatting";
    private static final String TAG_SECURITY = "security";
    private static final String TAG_LOGIC = "logic";
    private static final String TAG_CONTROL_FLOW = "control-flow";
    private static final String NONCOMPLIANT_HEADER = "<h3>Noncompliant Code Example</h3>";
    private static final String COMPLIANT_HEADER = "<h3>Compliant Solution</h3>";

    // TRACEABILITY_MAPPING: REQ-003
    @Override
    public void define(Context context) {
        NewRepository repository = context.createRepository(REPOSITORY_KEY, TSqlLanguage.KEY)
                .setName(REPOSITORY_NAME);

        // Rule 1: AvoidSelectAll
        repository.createRule(AvoidSelectAllCheck.RULE_KEY)
                .setName("Avoid using 'SELECT *'")
                .setHtmlDescription(
                        "<p>Explicitly declaring column names in <code>SELECT</code> queries ensures predictable "
                        + "schema contracts, avoids unnecessary I/O, and enables database index coverage.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT * FROM Users;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT Id, Username, Email FROM Users;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "performance", "pitfall");

        // Rule 2: AvoidLeadingWildcardInLike
        repository.createRule(AvoidLeadingWildcardInLikeCheck.RULE_KEY)
                .setName("Avoid leading wildcards in LIKE predicates")
                .setHtmlDescription(
                        "<p>A <code>LIKE</code> predicate beginning with a wildcard character (<code>%</code> or <code>_</code>) "
                        + "cannot utilize an index seek on the filtered column, forcing the optimizer to execute full table or index scans.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT Id FROM Customers WHERE LastName LIKE '%son';</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT Id FROM Customers WHERE LastName LIKE 'John%';</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "performance", "indexing");

        // Rule 3: AvoidNoLockHint
        repository.createRule(AvoidNoLockHintCheck.RULE_KEY)
                .setName("Avoid using 'NOLOCK' or 'READUNCOMMITTED' table hints")
                .setHtmlDescription(
                        "<p>The <code>NOLOCK</code> and <code>READUNCOMMITTED</code> hints allow reading dirty, uncommitted rows "
                        + "and can skip or duplicate rows during allocation-order scans. Prefer Snapshot Isolation or Read Committed "
                        + "Snapshot Isolation (RCSI) instead.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT OrderId, Total FROM Orders WITH (NOLOCK);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT OrderId, Total FROM Orders;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "concurrency", TAG_DATA_INTEGRITY);

        // Rule 4: AvoidDeprecatedTypes
        repository.createRule(AvoidDeprecatedTypesCheck.RULE_KEY)
                .setName("Avoid deprecated data types (TEXT, NTEXT, IMAGE)")
                .setHtmlDescription(
                        "<p>The <code>TEXT</code>, <code>NTEXT</code>, and <code>IMAGE</code> data types are deprecated in SQL Server. "
                        + "Replace them with <code>VARCHAR(MAX)</code>, <code>NVARCHAR(MAX)</code>, or <code>VARBINARY(MAX)</code>.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>CREATE TABLE Articles (Title VARCHAR(100), Body TEXT);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CREATE TABLE Articles (Title VARCHAR(100), Body VARCHAR(MAX));</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "obsolete", "maintenance");

        // Rule 5: S1590
        repository.createRule(DeleteAndUpdateWithoutWhereCheck.RULE_KEY)
                .setName("DELETE and UPDATE statements should contain WHERE clauses")
                .setHtmlDescription(
                        "<p>DELETE and UPDATE operations without a WHERE clause affect every row in the target table.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>DELETE FROM Orders; UPDATE Orders SET Status = 'Closed';</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>DELETE FROM Orders WHERE OrderId = 42; UPDATE Orders SET Status = 'Closed' WHERE OrderId = 42;</pre>")
                .setSeverity(Severity.BLOCKER)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_DATA_SAFETY, TAG_DATA_INTEGRITY);

        // Rule 6: S3641
        repository.createRule(NotInSubqueryCheck.RULE_KEY)
                .setName("Subqueries should not be used in NOT IN conditions")
                .setHtmlDescription(
                        "<p>Using <code>NOT IN</code> with a subquery can yield unexpected results when NULL values are present.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT * FROM Customers WHERE CustomerId NOT IN (SELECT CustomerId FROM Orders);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT * FROM Customers WHERE CustomerId NOT IN (SELECT CustomerId FROM Orders WHERE CustomerId IS NOT NULL);</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "null-handling", TAG_DATA_SAFETY);

        // Rule 7: S2527
        repository.createRule(NullComparisonCheck.RULE_KEY)
                .setName("NULL should not be compared directly with = or <>")
                .setHtmlDescription(
                        "<p>NULL is not a value, so comparisons like <code>NULL = 1</code> or <code>NULL <> 1</code> are not valid SQL logic.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>WHERE Value = NULL OR Value <> NULL;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>WHERE Value IS NULL OR Value IS NOT NULL;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "null-handling", TAG_LOGIC);

        // Rule 8: S4054
        repository.createRule(TopWithoutOrderByCheck.RULE_KEY)
                .setName("TOP without ORDER BY returns non-deterministic rows")
                .setHtmlDescription(
                        "<p>A query using <code>TOP</code> without an <code>ORDER BY</code> can return arbitrary rows on each execution.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT TOP 10 * FROM Orders;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT TOP 10 * FROM Orders ORDER BY CreatedAt DESC;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "determinism", "ordering");

        // Rule 9: S4094
        repository.createRule(CaseInputInvariantCheck.RULE_KEY)
                .setName("CASE input expressions should be invariant")
                .setHtmlDescription(
                        "<p>The input expression of a simple CASE should not be non-deterministic because it can change branch selection.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>CASE RAND() WHEN 1 THEN 'A' ELSE 'B' END;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CASE Status WHEN 'A' THEN 'X' ELSE 'Y' END;</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "determinism", TAG_DATA_SAFETY);

        repository.createRule(S103Check.RULE_KEY)
                .setName("Lines should not be too long")
                .setHtmlDescription("<p>Long lines reduce readability and make reviews harder.</p>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "readability");

        repository.createRule(S104Check.RULE_KEY)
                .setName("Files should not have too many lines")
                .setHtmlDescription("<p>Large files are harder to manage and review.</p>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_MAINTAINABILITY);

        repository.createRule(S105Check.RULE_KEY)
                .setName("Tabulation characters should not be used")
                .setHtmlDescription("<p>Use spaces instead of tabs for consistent formatting.</p>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_FORMATTING);

        repository.createRule(S122Check.RULE_KEY)
                .setName("Multiple statements should not appear on a single line")
                .setHtmlDescription("<p>Code is easier to read and review when each statement is on its own line.</p>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "readability");

        repository.createRule(S125Check.RULE_KEY)
                .setName("Sections of T-SQL code should not be commented out")
                .setHtmlDescription("<p>Commented-out code is misleading and should be removed.</p>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_MAINTAINABILITY);

        repository.createRule(S1134Check.RULE_KEY)
                .setName("FIXME tags should be reviewed")
                .setHtmlDescription("<p>FIXME markers indicate unresolved work that needs attention.</p>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "todo");

        repository.createRule(S1135Check.RULE_KEY)
                .setName("TODO tags should be reviewed")
                .setHtmlDescription("<p>TODO markers indicate pending work and should be tracked.</p>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "todo");

        repository.createRule(NoSonarCheck.RULE_KEY)
                .setName("NOSONAR comments should be reviewed")
                .setHtmlDescription("<p>Suppressions should be intentional and maintained.</p>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "suppressions");

        repository.createRule(S1451Check.RULE_KEY)
                .setName("Track file copyright and license headers")
                .setHtmlDescription("<p>Source files should include ownership and license information.</p>")
                .setSeverity(Severity.INFO)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "legal");

        repository.createRule(S3972Check.RULE_KEY)
                .setName("Conditional statements should be formatted consistently")
                .setHtmlDescription("<p>Formatting conditions consistently reduces complexity and review friction.</p>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_FORMATTING);

        repository.createRule(S3973Check.RULE_KEY)
                .setName("Blocks should be indented consistently")
                .setHtmlDescription("<p>Consistent indentation helps readers understand nested blocks quickly.</p>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_FORMATTING);

        repository.createRule(S138Check.RULE_KEY)
                .setName("Procedures should not be too long")
                .setHtmlDescription("<p>Large procedures are harder to understand and maintain.</p>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_MAINTAINABILITY);

        repository.createRule(S1151Check.RULE_KEY)
                .setName("WHEN blocks should not be too long")
                .setHtmlDescription("<p>Large WHEN blocks are difficult to understand and validate.</p>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_MAINTAINABILITY);

        // Rule 10: S1614
        repository.createRule(PrimaryKeyRequiredCheck.RULE_KEY)
                .setName("Primary keys should be specified in CREATE TABLE")
                .setHtmlDescription(
                        "<p>Every table should declare a primary key to ensure row identity and stable keys for indexing and referential integrity.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>CREATE TABLE Orders (OrderId INT, CustomerId INT);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CREATE TABLE Orders (OrderId INT PRIMARY KEY, CustomerId INT);</pre>")
                .setSeverity(Severity.BLOCKER)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_SCHEMA, TAG_DATA_INTEGRITY);

        // Rule 11: S1745
        repository.createRule(InsertColumnsExplicitCheck.RULE_KEY)
                .setName("INSERT statements should specify columns explicitly")
                .setHtmlDescription(
                        "<p>Always list the target columns in <code>INSERT</code> statements to guard against accidental schema drift.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>INSERT INTO Orders VALUES (1, 'A');</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>INSERT INTO Orders (OrderId, Status) VALUES (1, 'A');</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_DATA_SAFETY, TAG_SCHEMA);

        // Rule 12: S4078
        repository.createRule(ExplicitLengthForVariableTypesCheck.RULE_KEY)
                .setName("VARCHAR, NVARCHAR, and VARBINARY should declare length explicitly")
                .setHtmlDescription(
                        "<p>Variable-length types without a length are ambiguous and can create inconsistent schema definitions.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>CREATE TABLE Users (Name VARCHAR, LastName NVARCHAR, Photo VARBINARY);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CREATE TABLE Users (Name VARCHAR(100), LastName NVARCHAR(50), Photo VARBINARY(1024));</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "data-types", TAG_SCHEMA);

        // Rule 13: S1735
        repository.createRule(OrderByColumnNamesCheck.RULE_KEY)
                .setName("ORDER BY clauses should use column names instead of ordinals")
                .setHtmlDescription(
                        "<p>Positional ordering like <code>ORDER BY 1, 2</code> is fragile when column order changes.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT * FROM Orders ORDER BY 1, 2;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT * FROM Orders ORDER BY OrderDate, CustomerId;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "ordering", TAG_MAINTAINABILITY);

        // Rule 14: S4801
        repository.createRule(ExistsSelectOneCheck.RULE_KEY)
                .setName("EXISTS subqueries should use SELECT 1 or SELECT *")
                .setHtmlDescription(
                        "<p>Subqueries inside <code>EXISTS</code> should not enumerate columns, because the result only matters for existence.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>WHERE EXISTS (SELECT CustomerId FROM Orders WHERE Orders.CustomerId = Customers.Id);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>WHERE EXISTS (SELECT 1 FROM Orders WHERE Orders.CustomerId = Customers.Id);</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "readability", TAG_DATA_SAFETY);

        // Rule 15: S4102
        repository.createRule(AlterTableCheckEnforcementCheck.RULE_KEY)
                .setName("CHECK or NOCHECK should be explicit in ALTER TABLE")
                .setHtmlDescription(
                        "<p>When enabled state is changed for constraints during an ALTER TABLE, the behavior should be explicit.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>ALTER TABLE Orders ADD CONSTRAINT CK_Orders CHECK (Total > 0);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>ALTER TABLE Orders WITH CHECK ADD CONSTRAINT CK_Orders CHECK (Total > 0);</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_SCHEMA, "constraint");

        // Rule 16: S4149
        repository.createRule(ColumnReferenceDepthCheck.RULE_KEY)
                .setName("Column references should not exceed two parts")
                .setHtmlDescription(
                        "<p>Using more than two identifier parts in a column reference obscures the source and makes maintenance harder.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT db1.schema1.table1.column1 FROM dbo.Table1;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT table1.column1 FROM dbo.Table1;</pre>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_MAINTAINABILITY);
        repository.createRule(DynamicSqlExecutionCheck.RULE_KEY)
                .setName("Dynamic SQL should not concatenate unvalidated input")
                .setHtmlDescription(
                        "<p>Executing dynamically built SQL with concatenated variables or parameters allows injection attacks because untrusted input can alter the SQL text.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>DECLARE @sql NVARCHAR(MAX) = 'SELECT * FROM Users WHERE Name = ''' + @name + ''''; EXEC (@sql);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>DECLARE @sql NVARCHAR(MAX) = N'SELECT * FROM Users WHERE Name = @name'; EXEC sys.sp_executesql @sql, N'@name NVARCHAR(50)', @name = @input;</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.VULNERABILITY)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_SECURITY, "injection");

        repository.createRule(WeakHashCheck.RULE_KEY)
                .setName("Weak hashing algorithms should not be used")
                .setHtmlDescription(
                        "<p>Weak hash algorithms such as MD2, MD4, MD5, SHA, and SHA1 are vulnerable to collision and preimage attacks. Use SHA2_256 or SHA2_512 instead.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT HASHBYTES('SHA1', @secret);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT HASHBYTES('SHA2_512', @secret);</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.VULNERABILITY)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_SECURITY, "cryptography");

        repository.createRule(HardCodedCredentialsCheck.RULE_KEY)
                .setName("Credentials should not be hard-coded in SQL code")
                .setHtmlDescription(
                        "<p>Hard-coded credentials inside SQL scripts can be exposed in source control, logs, or application traces. Use secure secret stores, environment variables, or generated credentials instead.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>CREATE LOGIN app WITH PASSWORD = 'P@ssw0rd!'; ALTER LOGIN app WITH PASSWORD = 'Password123';</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CREATE LOGIN app WITH PASSWORD = CONVERT(varchar(32), NEWID());</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.VULNERABILITY)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_SECURITY, "authentication");

        // Rule 10: SR0006
        repository.createRule(SargableWhereCheck.RULE_KEY)
                .setName("WHERE predicates should be SARGable")
                .setHtmlDescription(
                        "<p>Columns should be placed on one side of the comparison and not wrapped in functions or arithmetic in <code>WHERE</code> clauses.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT * FROM Orders WHERE ABS(OrderTotal) > 100;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT * FROM Orders WHERE OrderTotal > 100;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "performance", "indexing");

        // Rule 11: S4108
        repository.createRule(UseScopeIdentityCheck.RULE_KEY)
                .setName("Use SCOPE_IDENTITY() instead of @@IDENTITY")
                .setHtmlDescription(
                        "<p><code>@@IDENTITY</code> returns the last identity value across all scopes, which can produce misleading results in nested triggers or procedures.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT @@IDENTITY;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT SCOPE_IDENTITY();</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "identity", "scope");

        // Rule 12: SR0009
        repository.createRule(AvoidVariableLengthDataTypeCheck.RULE_KEY)
                .setName("Avoid variable-length data types of length 1 or 2")
                .setHtmlDescription(
                        "<p>Using <code>VARCHAR(1)</code>, <code>NVARCHAR(2)</code>, or similar types is often a design smell and can reduce readability.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>CREATE TABLE T (Code VARCHAR(1), Name NVARCHAR(2));</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CREATE TABLE T (Code VARCHAR(10), Name NVARCHAR(50));</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_MAINTAINABILITY);

        // Rule 13: SR0010
        repository.createRule(AvoidDeprecatedJoinSyntaxCheck.RULE_KEY)
                .setName("Avoid deprecated join syntax")
                .setHtmlDescription(
                        "<p>The old outer join operators <code>*=</code> and <code>=*</code> are deprecated and should be replaced with explicit JOIN syntax.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT * FROM Customers c, Orders o WHERE c.Id *= o.CustomerId;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT * FROM Customers c LEFT JOIN Orders o ON c.Id = o.CustomerId;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "compatibility");

        // Rule 14: SR0011
        repository.createRule(AvoidSpecialCharactersInObjectNamesCheck.RULE_KEY)
                .setName("Avoid special characters in DDL object names")
                .setHtmlDescription(
                        "<p>Special characters in database object names make scripting and maintenance harder and can trigger quoting issues.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>CREATE TABLE dbo.[Order$] (Id int);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CREATE TABLE dbo.OrderSummary (Id int);</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "naming");

        // Rule 15: S4413
        repository.createRule(AvoidReservedWordsInObjectNamesCheck.RULE_KEY)
                .setName("Avoid reserved words in object and type names")
                .setHtmlDescription(
                        "<p>Database object and type names should not reuse SQL reserved words because it can degrade readability and produce ambiguous references.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>CREATE TABLE Order (Id int);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CREATE TABLE Orders (Id int);</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "naming");

        // Rule 16: S4196
        repository.createRule(StoredProcedureOutputParameterCheck.RULE_KEY)
                .setName("OUTPUT parameters should be assigned in stored procedures")
                .setHtmlDescription(
                        "<p>Stored procedure OUTPUT parameters should be assigned in the procedure body before return so callers receive valid values.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>CREATE PROCEDURE dbo.GetValue @Result INT OUTPUT AS SELECT 1;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CREATE PROCEDURE dbo.GetValue @Result INT OUTPUT AS SET @Result = 1;</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "procedure", TAG_DATA_SAFETY);

        // Rule 17: SR0015
        repository.createRule(DeterministicFunctionInWhereCheck.RULE_KEY)
                .setName("Extract deterministic functions from WHERE predicates")
                .setHtmlDescription(
                        "<p>Deterministic function calls in predicate expressions can hide indexable conditions and force evaluation on the wrong side of comparisons.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT * FROM Orders WHERE ABS(Total) > 500;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT * FROM Orders WHERE Total > 500;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "performance", "indexing");

        // Rule 18: SR0016
        repository.createRule(AvoidSpPrefixProcedureNameCheck.RULE_KEY)
                .setName("Avoid the sp_ prefix for user-defined stored procedures")
                .setHtmlDescription(
                        "<p>Stored procedures that begin with <code>sp_</code> will be resolved in the master database first, which can cause confusing behavior.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>CREATE PROCEDURE dbo.sp_GetOrders AS SELECT 1;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CREATE PROCEDURE dbo.GetOrders AS SELECT 1;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "naming");

        repository.createRule(SetNoCountCheck.RULE_KEY)
                .setName("SET NOCOUNT ON should be enabled in procedures and triggers")
                .setHtmlDescription(
                        "<p>Stored procedures and triggers should set <code>NOCOUNT</code> to <code>ON</code> to avoid sending row count messages to clients.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>CREATE PROCEDURE p AS BEGIN SET NOCOUNT OFF; SELECT 1; END</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>CREATE PROCEDURE p AS BEGIN SET NOCOUNT ON; SELECT 1; END</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "performance", "procedure");

        // Rule 11: S6498
        repository.createRule(SetAnsiWarningsAndArithabortCheck.RULE_KEY)
                .setName("ANSI_WARNINGS and ARITHABORT should not be set to OFF")
                .setHtmlDescription(
                        "<p>Turning off <code>ANSI_WARNINGS</code> or <code>ARITHABORT</code> can hide errors and change query semantics.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>SET ANSI_WARNINGS OFF; SET ARITHABORT OFF;</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>SET ANSI_WARNINGS ON; SET ARITHABORT ON;</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "session-settings");

        // Rule 12: S4154
        repository.createRule(SetAnsiNullsCheck.RULE_KEY)
                .setName("ANSI_NULLS, ANSI_PADDING, and CONCAT_NULL_YIELDS_NULL should not be toggled off")
                .setHtmlDescription(
                        "<p>Disabling these session settings changes comparison and concatenation semantics in ways that can produce unexpected results.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>SET ANSI_NULLS OFF; SET ANSI_PADDING OFF; SET CONCAT_NULL_YIELDS_NULL OFF;</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>SET ANSI_NULLS ON; SET ANSI_PADDING ON; SET CONCAT_NULL_YIELDS_NULL ON;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "session-settings");

        // Rule 13: S4155
        repository.createRule(AvoidDeprecatedSystemTablesCheck.RULE_KEY)
                .setName("Deprecated SQL Server system tables should be replaced")
                .setHtmlDescription(
                        "<p>Legacy compatibility tables such as <code>sysobjects</code> or <code>syscolumns</code> are deprecated; prefer the modern <code>sys</code> catalog views.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>SELECT * FROM sysobjects;</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>SELECT * FROM sys.objects;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "obsolete");

        // Rule 14: S4103
        repository.createRule(FetchIntoCursorCountCheck.RULE_KEY)
                .setName("FETCH INTO variable count must match cursor column count")
                .setHtmlDescription(
                        "<p>Every variable in a <code>FETCH INTO</code> must correspond to one selected column in the cursor declaration.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>DECLARE c CURSOR FOR SELECT a, b FROM t; FETCH NEXT FROM c INTO @a;</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>DECLARE c CURSOR FOR SELECT a, b FROM t; FETCH NEXT FROM c INTO @a, @b;</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "cursor");

        // Rule 15: S4104
        repository.createRule(CaseSubqueryCheck.RULE_KEY)
                .setName("CASE, COALESCE, and IIF inputs should not contain subqueries")
                .setHtmlDescription(
                        "<p>Subqueries used as expression inputs to <code>COALESCE</code>, <code>IIF</code>, or <code>CASE</code> make logic less predictable and harder to optimize.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>SELECT COALESCE((SELECT MAX(Id) FROM Orders), 0);</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>SELECT COALESCE(LastOrderId, 0);</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_LOGIC);

        // Rule 16: S4109
        repository.createRule(TriggerResultSetCheck.RULE_KEY)
                .setName("Triggers should not return result sets")
                .setHtmlDescription(
                        "<p>Triggers should not issue <code>SELECT</code>, <code>PRINT</code>, or <code>FETCH</code> statements that return rows or messages to callers.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>CREATE TRIGGER t ON dbo.Test AFTER UPDATE AS SELECT 1;</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>CREATE TRIGGER t ON dbo.Test AFTER UPDATE AS SET NOCOUNT ON;</pre>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "trigger");

        // Rule 17: S3643
        repository.createRule(AvoidLikeWithoutWildcardCheck.RULE_KEY)
                .setName("LIKE predicates without wildcards should use =")
                .setHtmlDescription(
                        "<p>When a predicate does not contain wildcard characters, use <code>=</code> instead of <code>LIKE</code>.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>WHERE Name LIKE 'Acme';</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>WHERE Name = 'Acme';</pre>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "readability");

        // Rule 18: S1742
        repository.createRule(AvoidNonStandardComparisonOperatorsCheck.RULE_KEY)
                .setName("Replace non-standard comparison operators with ISO equivalents")
                .setHtmlDescription(
                        "<p>Non-standard operators such as <code>!=</code>, <code>!<</code>, and <code>!></code> are less readable than the ISO forms <code><></code>, <code>>=</code>, and <code><=</code>.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>WHERE a != b OR c !< d OR e !> f;</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>WHERE a <> b OR c <= d OR e >= f;</pre>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "readability");

        // Rule: S1117
        repository.createRule(UnusedLocalVariableCheck.RULE_KEY)
                .setName("Unused local variables should be removed")
                .setHtmlDescription(
                        "<p>Local variables that are never used should be removed to keep the code easier to read and maintain.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>DECLARE @count INT; SET @count = 1;</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>DECLARE @count INT; SET @count = 1; SELECT @count;</pre>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_MAINTAINABILITY, "unused-code");

        // Rule: S1172
        repository.createRule(UnusedParameterCheck.RULE_KEY)
                .setName("Unused parameters should be removed")
                .setHtmlDescription(
                        "<p>Unused procedure or function parameters should be removed because they add noise without affecting behavior.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>CREATE PROCEDURE dbo.GetOrders (@customerId INT, @unused INT) AS BEGIN SELECT * FROM Orders WHERE CustomerId = @customerId; END;</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>CREATE PROCEDURE dbo.GetOrders (@customerId INT) AS BEGIN SELECT * FROM Orders WHERE CustomerId = @customerId; END;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_MAINTAINABILITY, "unused-code");

        // Rule: S1065
        repository.createRule(UnusedLabelCheck.RULE_KEY)
                .setName("Unused labels should be removed")
                .setHtmlDescription(
                        "<p>Labels that are never referenced by a GOTO statement should be removed.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>label: SELECT 1;</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>IF 1 = 1 GOTO label; label: SELECT 1;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_MAINTAINABILITY, TAG_CONTROL_FLOW);

        // Rule: S1763
        repository.createRule(UnreachableCodeCheck.RULE_KEY)
                .setName("Statements should not be unreachable")
                .setHtmlDescription(
                        "<p>Statements following unconditional terminators such as <code>RETURN</code>, <code>BREAK</code>, <code>CONTINUE</code>, or <code>THROW</code> are unreachable and should be removed.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>IF 1 = 1 RETURN; SELECT 1;</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>IF 1 = 1 RETURN; ELSE SELECT 1;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_LOGIC, TAG_CONTROL_FLOW);

        // Rule: S1751
        repository.createRule(SingleIterationLoopCheck.RULE_KEY)
                .setName("Loops with at most one iteration should be refactored")
                .setHtmlDescription(
                        "<p>While loops that are effectively single-iteration should be refactored to the simpler statement that expresses the intent.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>WHILE 1 = 1 BEGIN SELECT 1; BREAK; END;</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>SELECT 1;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_LOGIC, "loop");

        // Rule: S1656
        repository.createRule(SelfAssignmentCheck.RULE_KEY)
                .setName("Variables should not be self-assigned")
                .setHtmlDescription(
                        "<p>A variable assigned to itself is a no-op and usually indicates a logic error or leftover code.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>SET @x = @x;</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>SET @x = 1;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_LOGIC, "assignment");

        // Rule: S2737
        repository.createRule(CatchClauseShouldDoMoreThanRethrowCheck.RULE_KEY)
                .setName("CATCH blocks should do more than simply rethrow")
                .setHtmlDescription(
                        "<p>A catch block that only rethrows the same exception is redundant and hides important error-handling logic.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>BEGIN TRY SELECT 1/0; END TRY BEGIN CATCH THROW; END CATCH;</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>BEGIN TRY SELECT 1/0; END TRY BEGIN CATCH INSERT INTO Errors VALUES (ERROR_MESSAGE()); THROW; END CATCH;</pre>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "error-handling", "logging");

        // Rule: S3626
        repository.createRule(RedundantJumpCheck.RULE_KEY)
                .setName("Redundant jump statements should be removed")
                .setHtmlDescription(
                        "<p>Returning immediately at the end of a procedure is redundant because the procedure already ends there.</p>"
                                + NONCOMPLIANT_HEADER
                                + "<pre>CREATE PROCEDURE p AS BEGIN SELECT 1; RETURN; END;</pre>"
                                + COMPLIANT_HEADER
                                + "<pre>CREATE PROCEDURE p AS BEGIN SELECT 1; END;</pre>")
                .setSeverity(Severity.MINOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_CONTROL_FLOW, TAG_MAINTAINABILITY);

        // Rule 10: S1764
        repository.createRule(DuplicateBinaryOperandCheck.RULE_KEY)
                .setName("Identical expressions should not be used on both sides of a binary operator")
                .setHtmlDescription(
                        "<p>Using the same expression on both sides of a binary operator is usually a logic bug and almost always indicates a mistake.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>WHERE a = a OR x OR x;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>WHERE a = b OR x OR y;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_LOGIC, "bug");

        // Rule 11: S1862
        repository.createRule(DuplicateIfConditionCheck.RULE_KEY)
                .setName("Related IF/ELSE IF and CASE conditions should not be duplicated")
                .setHtmlDescription(
                        "<p>Duplicate conditions prevent the code from reaching later branches and usually hide logic errors.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>IF @flag = 1 ... ELSE IF @flag = 1 ...; CASE WHEN a = 1 THEN ... WHEN a = 1 THEN ... END;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>IF @flag = 1 ... ELSE IF @flag = 2 ...; CASE WHEN a = 1 THEN ... WHEN a = 2 THEN ... END;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_LOGIC, "conditionals");

        // Rule 12: S1871
        repository.createRule(IdenticalBranchCheck.RULE_KEY_S1871)
                .setName("Conditional branches should not have identical implementations")
                .setHtmlDescription(
                        "<p>Branches with identical logic are dead code and should be merged or split to make the intent clear.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>IF A BEGIN SELECT 1; END ELSE BEGIN SELECT 1; END;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>IF A BEGIN SELECT 1; END ELSE BEGIN SELECT 2; END;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "duplication", TAG_MAINTAINABILITY);

        // Rule 13: S2761
        repository.createRule(RepeatedUnaryOperatorCheck.RULE_KEY)
                .setName("Repeated unary operators should not be used")
                .setHtmlDescription(
                        "<p>Applying the same prefix operator multiple times is confusing and often a mistake.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT NOT NOT Flag, ~~~Value;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT NOT Flag, ~Value;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_LOGIC, "operators");

        // Rule 14: S3633
        repository.createRule(WhereClauseRedundancyCheck.RULE_KEY_S3633)
                .setName("WHERE clauses should not contain redundant or contradictory conditions")
                .setHtmlDescription(
                        "<p>Redundant conditions in a WHERE clause both increase complexity and hide semantic errors.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>WHERE A = A AND A = A;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>WHERE A = 1 AND B = 2;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_LOGIC, "query");

        // Rule 15: S121
        repository.createRule(ControlStructureBlockCheck.RULE_KEY)
                .setName("Control structures should use BEGIN...END blocks")
                .setHtmlDescription(
                        "<p>Single statements in control structures are harder to read and more error-prone if later logic is added.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>IF @flag = 1 SELECT 1; WHILE @flag = 1 SELECT 1;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>IF @flag = 1 BEGIN SELECT 1; END; WHILE @flag = 1 BEGIN SELECT 1; END;</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_MAINTAINABILITY, TAG_CONTROL_FLOW);

        // Rule 16: S131
        repository.createRule(CaseElseMissingCheck.RULE_KEY_S131)
                .setName("CASE expressions and IF/ELSE IF chains should end with ELSE")
                .setHtmlDescription(
                        "<p>Missing ELSE implies a fall-through path that can hide bugs and lead to unexpected behavior.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>CASE WHEN x = 1 THEN 'A' END; IF x = 1 SELECT 'A'; ELSE IF x = 2 SELECT 'B'; END;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CASE WHEN x = 1 THEN 'A' ELSE 'default' END; IF x = 1 SELECT 'A'; ELSE IF x = 2 SELECT 'B'; ELSE SELECT 'default';</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_LOGIC, TAG_CONTROL_FLOW);

        // Rule 17: S907
        repository.createRule(GotoStatementCheck.RULE_KEY)
                .setName("GOTO statements should not be used")
                .setHtmlDescription(
                        "<p>GOTO creates complex, difficult-to-follow control flow and makes code maintenance error-prone.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>GOTO NextStep;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>IF @ok = 1 BEGIN RETURN; END;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_CONTROL_FLOW, "complexity");

        // Rule 18: S1066
        repository.createRule(NestedIfMergeCheck.RULE_KEY)
                .setName("Mergeable nested IF statements should be combined")
                .setHtmlDescription(
                        "<p>Nested IF statements that can be merged create unnecessary complexity.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>IF a = 1 BEGIN IF b = 2 BEGIN SELECT 1; END; END;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>IF a = 1 AND b = 2 BEGIN SELECT 1; END;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "complexity", TAG_MAINTAINABILITY);

        // Rule 10: SR0004
        repository.createRule(InPredicateIndexedColumnCheck.RULE_KEY)
                .setName("Avoid using unindexed columns in IN predicates")
                .setHtmlDescription(
                        "<p>Using a column in an <code>IN</code> predicate without an index can force a scan of the entire table and may invalidate assumptions about predicate selectivity.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT Id FROM Orders WHERE CustomerId IN (1, 2, 3);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CREATE INDEX IX_Orders_CustomerId ON Orders(CustomerId); SELECT Id FROM Orders WHERE CustomerId IN (1, 2, 3);</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "performance", "indexing");

        // Rule 11: SR0007
        repository.createRule(NullableColumnIsNullCheck.RULE_KEY)
                .setName("Use ISNULL on nullable columns in expressions")
                .setHtmlDescription(
                        "<p>Using <code>COALESCE</code> on a nullable column is less clear than <code>ISNULL</code> and can hide nullable-column intent in expressions.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT COALESCE(LastLogin, '1970-01-01') FROM Users;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT ISNULL(LastLogin, '1970-01-01') FROM Users;</pre>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "null-handling", TAG_LOGIC);

        // Rule 12: SR0014
        repository.createRule(NarrowingCastCheck.RULE_KEY)
                .setName("Data loss might occur when narrowing casts are used")
                .setHtmlDescription(
                        "<p>Converting or assigning a wider column value into a narrower type or shorter length can silently truncate or round data.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT CAST(BigNumber AS SMALLINT) FROM Orders;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT CAST(BigNumber AS BIGINT) FROM Orders;</pre>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_DATA_SAFETY, "data-loss");

        // Rule: SyntaxError
        repository.createRule(CheckList.SYNTAX_ERROR_RULE_KEY)
                .setName("T-SQL syntax error")
                .setHtmlDescription(
                        "<p>The file failed to parse with the Microsoft T-SQL ScriptDOM parser due to syntax errors.</p>")
                .setSeverity(Severity.BLOCKER)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, "syntax-error");

        repository.done();
    }
}
