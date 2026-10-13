package io.github.fupacat.sonar.tsql.rules;

import io.github.fupacat.sonar.tsql.TSqlLanguage;
import io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedTypesCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidLeadingWildcardInLikeCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidNoLockHintCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck;
import io.github.fupacat.sonar.tsql.checks.CaseInputInvariantCheck;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.checks.DeleteAndUpdateWithoutWhereCheck;
import io.github.fupacat.sonar.tsql.checks.DynamicSqlExecutionCheck;
import io.github.fupacat.sonar.tsql.checks.HardCodedCredentialsCheck;
import io.github.fupacat.sonar.tsql.checks.NotInSubqueryCheck;
import io.github.fupacat.sonar.tsql.checks.NullComparisonCheck;
import io.github.fupacat.sonar.tsql.checks.TopWithoutOrderByCheck;
import io.github.fupacat.sonar.tsql.checks.WeakHashCheck;
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
                .setTags(TAG_SQL_SERVER, "concurrency", "data-integrity");

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
                .setTags(TAG_SQL_SERVER, TAG_DATA_SAFETY, "data-integrity");

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
                .setTags(TAG_SQL_SERVER, "null-handling", "logic");

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

        // Rule 10: S1523
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
                .setTags(TAG_SQL_SERVER, "security", "injection");

        // Rule 11: S2070 / S4790
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
                .setTags(TAG_SQL_SERVER, "security", "cryptography");

        // Rule 12: S2068
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
                .setTags(TAG_SQL_SERVER, "security", "authentication");

        // Rule 13: SyntaxError
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
