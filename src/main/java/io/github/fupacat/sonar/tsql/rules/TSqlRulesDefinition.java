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
    private static final String TAG_PERFORMANCE = "performance";
    private static final String TAG_INDEXING = "indexing";
    private static final String TAG_CONCURRENCY = "concurrency";
    private static final String TAG_DATA_INTEGRITY = "data-integrity";
    private static final String TAG_OBSOLETE = "obsolete";
    private static final String TAG_MAINTENANCE = "maintenance";
    private static final String TAG_NULL_HANDLING = "null-handling";
    private static final String TAG_LOGIC = "logic";
    private static final String TAG_DETERMINISM = "determinism";
    private static final String TAG_ORDERING = "ordering";
    private static final String TAG_SECURITY = "security";
    private static final String TAG_INJECTION = "injection";
    private static final String TAG_CRYPTOGRAPHY = "cryptography";
    private static final String TAG_AUTHENTICATION = "authentication";
    private static final String TAG_SYNTAX_ERROR = "syntax-error";
    private static final String NONCOMPLIANT_HEADER = "<h3>Noncompliant Code Example</h3>";
    private static final String COMPLIANT_HEADER = "<h3>Compliant Solution</h3>";

    // TRACEABILITY_MAPPING: REQ-003
    @Override
    public void define(Context context) {
        NewRepository repository = context.createRepository(REPOSITORY_KEY, TSqlLanguage.KEY)
                .setName(REPOSITORY_NAME);

        registerRule(repository, AvoidSelectAllCheck.RULE_KEY,
                "Avoid using 'SELECT *'",
                "<p>Explicitly declaring column names in <code>SELECT</code> queries ensures predictable "
                        + "schema contracts, avoids unnecessary I/O, and enables database index coverage.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT * FROM Users;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT Id, Username, Email FROM Users;</pre>",
                Severity.MAJOR,
                RuleType.CODE_SMELL,
                TAG_SQL_SERVER, TAG_PERFORMANCE, "pitfall");

        registerRule(repository, AvoidLeadingWildcardInLikeCheck.RULE_KEY,
                "Avoid leading wildcards in LIKE predicates",
                "<p>A <code>LIKE</code> predicate beginning with a wildcard character (<code>%</code> or <code>_</code>) "
                        + "cannot utilize an index seek on the filtered column, forcing the optimizer to execute full table or index scans.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT Id FROM Customers WHERE LastName LIKE '%son';</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT Id FROM Customers WHERE LastName LIKE 'John%';</pre>",
                Severity.CRITICAL,
                RuleType.BUG,
                TAG_SQL_SERVER, TAG_PERFORMANCE, TAG_INDEXING);

        registerRule(repository, AvoidNoLockHintCheck.RULE_KEY,
                "Avoid using 'NOLOCK' or 'READUNCOMMITTED' table hints",
                "<p>The <code>NOLOCK</code> and <code>READUNCOMMITTED</code> hints allow reading dirty, uncommitted rows "
                        + "and can skip or duplicate rows during allocation-order scans. Prefer Snapshot Isolation or Read Committed "
                        + "Snapshot Isolation (RCSI) instead.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT OrderId, Total FROM Orders WITH (NOLOCK);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT OrderId, Total FROM Orders;</pre>",
                Severity.MAJOR,
                RuleType.BUG,
                TAG_SQL_SERVER, TAG_CONCURRENCY, TAG_DATA_INTEGRITY);

        registerRule(repository, AvoidDeprecatedTypesCheck.RULE_KEY,
                "Avoid deprecated data types (TEXT, NTEXT, IMAGE)",
                "<p>The <code>TEXT</code>, <code>NTEXT</code>, and <code>IMAGE</code> data types are deprecated in SQL Server. "
                        + "Replace them with <code>VARCHAR(MAX)</code>, <code>NVARCHAR(MAX)</code>, or <code>VARBINARY(MAX)</code>.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>CREATE TABLE Articles (Title VARCHAR(100), Body TEXT);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CREATE TABLE Articles (Title VARCHAR(100), Body VARCHAR(MAX));</pre>",
                Severity.MAJOR,
                RuleType.CODE_SMELL,
                TAG_SQL_SERVER, TAG_OBSOLETE, TAG_MAINTENANCE);

        registerRule(repository, DeleteAndUpdateWithoutWhereCheck.RULE_KEY,
                "DELETE and UPDATE statements should contain WHERE clauses",
                "<p>DELETE and UPDATE operations without a WHERE clause affect every row in the target table.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>DELETE FROM Orders; UPDATE Orders SET Status = 'Closed';</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>DELETE FROM Orders WHERE OrderId = 42; UPDATE Orders SET Status = 'Closed' WHERE OrderId = 42;</pre>",
                Severity.BLOCKER,
                RuleType.BUG,
                TAG_SQL_SERVER, TAG_DATA_SAFETY, TAG_DATA_INTEGRITY);

        registerRule(repository, NotInSubqueryCheck.RULE_KEY,
                "Subqueries should not be used in NOT IN conditions",
                "<p>Using <code>NOT IN</code> with a subquery can yield unexpected results when NULL values are present.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT * FROM Customers WHERE CustomerId NOT IN (SELECT CustomerId FROM Orders);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT * FROM Customers WHERE CustomerId NOT IN (SELECT CustomerId FROM Orders WHERE CustomerId IS NOT NULL);</pre>",
                Severity.CRITICAL,
                RuleType.BUG,
                TAG_SQL_SERVER, TAG_NULL_HANDLING, TAG_DATA_SAFETY);

        registerRule(repository, NullComparisonCheck.RULE_KEY,
                "NULL should not be compared directly with = or <>",
                "<p>NULL is not a value, so comparisons like <code>NULL = 1</code> or <code>NULL <> 1</code> are not valid SQL logic.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>WHERE Value = NULL OR Value <> NULL;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>WHERE Value IS NULL OR Value IS NOT NULL;</pre>",
                Severity.MAJOR,
                RuleType.BUG,
                TAG_SQL_SERVER, TAG_NULL_HANDLING, TAG_LOGIC);

        registerRule(repository, TopWithoutOrderByCheck.RULE_KEY,
                "TOP without ORDER BY returns non-deterministic rows",
                "<p>A query using <code>TOP</code> without an <code>ORDER BY</code> can return arbitrary rows on each execution.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT TOP 10 * FROM Orders;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT TOP 10 * FROM Orders ORDER BY CreatedAt DESC;</pre>",
                Severity.MAJOR,
                RuleType.BUG,
                TAG_SQL_SERVER, TAG_DETERMINISM, TAG_ORDERING);

        registerRule(repository, CaseInputInvariantCheck.RULE_KEY,
                "CASE input expressions should be invariant",
                "<p>The input expression of a simple CASE should not be non-deterministic because it can change branch selection.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>CASE RAND() WHEN 1 THEN 'A' ELSE 'B' END;</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CASE Status WHEN 'A' THEN 'X' ELSE 'Y' END;</pre>",
                Severity.CRITICAL,
                RuleType.BUG,
                TAG_SQL_SERVER, TAG_DETERMINISM, TAG_DATA_SAFETY);

        registerRule(repository, DynamicSqlExecutionCheck.RULE_KEY,
                "Dynamic SQL should not concatenate unvalidated input",
                "<p>Executing dynamically built SQL with concatenated variables or parameters allows injection attacks because untrusted input can alter the SQL text.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>DECLARE @sql NVARCHAR(MAX) = 'SELECT * FROM Users WHERE Name = ''' + @name + ''''; EXEC (@sql);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>DECLARE @sql NVARCHAR(MAX) = N'SELECT * FROM Users WHERE Name = @name'; EXEC sys.sp_executesql @sql, N'@name NVARCHAR(50)', @name = @input;</pre>",
                Severity.CRITICAL,
                RuleType.VULNERABILITY,
                TAG_SQL_SERVER, TAG_SECURITY, TAG_INJECTION);

        registerRule(repository, WeakHashCheck.RULE_KEY,
                "Weak hashing algorithms should not be used",
                "<p>Weak hash algorithms such as MD2, MD4, MD5, SHA, and SHA1 are vulnerable to collision and preimage attacks. Use SHA2_256 or SHA2_512 instead.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>SELECT HASHBYTES('SHA1', @secret);</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>SELECT HASHBYTES('SHA2_512', @secret);</pre>",
                Severity.CRITICAL,
                RuleType.VULNERABILITY,
                TAG_SQL_SERVER, TAG_SECURITY, TAG_CRYPTOGRAPHY);

        registerRule(repository, HardCodedCredentialsCheck.RULE_KEY,
                "Credentials should not be hard-coded in SQL code",
                "<p>Hard-coded credentials inside SQL scripts can be exposed in source control, logs, or application traces. Use secure secret stores, environment variables, or generated credentials instead.</p>"
                        + NONCOMPLIANT_HEADER
                        + "<pre>CREATE LOGIN app WITH PASSWORD = 'P@ssw0rd!'; ALTER LOGIN app WITH PASSWORD = 'Password123';</pre>"
                        + COMPLIANT_HEADER
                        + "<pre>CREATE LOGIN app WITH PASSWORD = CONVERT(varchar(32), NEWID());</pre>",
                Severity.MAJOR,
                RuleType.VULNERABILITY,
                TAG_SQL_SERVER, TAG_SECURITY, TAG_AUTHENTICATION);

        registerRule(repository, CheckList.SYNTAX_ERROR_RULE_KEY,
                "T-SQL syntax error",
                "<p>The file failed to parse with the Microsoft T-SQL ScriptDOM parser due to syntax errors.</p>",
                Severity.BLOCKER,
                RuleType.BUG,
                TAG_SQL_SERVER, TAG_SYNTAX_ERROR);

        repository.done();
    }

    private void registerRule(NewRepository repository, String ruleKey, String name, String htmlDescription,
            Severity severity, RuleType type, String... tags) {
        repository.createRule(ruleKey)
                .setName(name)
                .setHtmlDescription(htmlDescription)
                .setSeverity(severity)
                .setType(type)
                .setStatus(RuleStatus.READY)
                .setTags(tags);
    }
}
