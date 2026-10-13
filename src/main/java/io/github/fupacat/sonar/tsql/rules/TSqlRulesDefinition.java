package io.github.fupacat.sonar.tsql.rules;

import io.github.fupacat.sonar.tsql.TSqlLanguage;
import io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedTypesCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidLeadingWildcardInLikeCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidNoLockHintCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck;
import io.github.fupacat.sonar.tsql.checks.CaseInputInvariantCheck;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.checks.DeleteAndUpdateWithoutWhereCheck;
import io.github.fupacat.sonar.tsql.checks.NotInSubqueryCheck;
import io.github.fupacat.sonar.tsql.checks.NullComparisonCheck;
import io.github.fupacat.sonar.tsql.checks.TopWithoutOrderByCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedSystemTablesCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidLikeWithoutWildcardCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidNonStandardComparisonOperatorsCheck;
import io.github.fupacat.sonar.tsql.checks.CaseSubqueryCheck;
import io.github.fupacat.sonar.tsql.checks.FetchIntoCursorCountCheck;
import io.github.fupacat.sonar.tsql.checks.SetAnsiNullsCheck;
import io.github.fupacat.sonar.tsql.checks.SetAnsiWarningsAndArithabortCheck;
import io.github.fupacat.sonar.tsql.checks.SetNoCountCheck;
import io.github.fupacat.sonar.tsql.checks.TriggerResultSetCheck;
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
    private static final String TAG_PITFALL = "pitfall";
    private static final String TAG_INDEXING = "indexing";
    private static final String TAG_CONCURRENCY = "concurrency";
    private static final String TAG_DATA_INTEGRITY = "data-integrity";
    private static final String TAG_MAINTENANCE = "maintenance";
    private static final String TAG_NULL_HANDLING = "null-handling";
    private static final String TAG_LOGIC = "logic";
    private static final String TAG_DETERMINISM = "determinism";
    private static final String TAG_ORDERING = "ordering";
    private static final String TAG_READABILITY = "readability";
    private static final String TAG_CURSOR = "cursor";
    private static final String TAG_TRIGGER = "trigger";
    private static final String TAG_PROCEDURE = "procedure";
    private static final String TAG_SESSION_SETTINGS = "session-settings";
    private static final String TAG_OBSOLETE = "obsolete";
    private static final String TAG_SYNTAX_ERROR = "syntax-error";
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
                .setTags(TAG_SQL_SERVER, TAG_PERFORMANCE, TAG_PITFALL);

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
                .setTags(TAG_SQL_SERVER, TAG_PERFORMANCE, TAG_INDEXING);

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
                .setTags(TAG_SQL_SERVER, TAG_CONCURRENCY, TAG_DATA_INTEGRITY);

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
                .setTags(TAG_SQL_SERVER, TAG_OBSOLETE, TAG_MAINTENANCE);

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
                .setTags(TAG_SQL_SERVER, TAG_NULL_HANDLING, TAG_DATA_SAFETY);

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
                .setTags(TAG_SQL_SERVER, TAG_NULL_HANDLING, TAG_LOGIC);

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
                .setTags(TAG_SQL_SERVER, TAG_DETERMINISM, TAG_ORDERING);

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
                .setTags(TAG_SQL_SERVER, TAG_DETERMINISM, TAG_DATA_SAFETY);

        // Rule 10: S4819
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
                .setTags(TAG_SQL_SERVER, TAG_PERFORMANCE, TAG_PROCEDURE);

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
                .setTags(TAG_SQL_SERVER, TAG_SESSION_SETTINGS);

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
                .setTags(TAG_SQL_SERVER, TAG_SESSION_SETTINGS);

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
                .setTags(TAG_SQL_SERVER, TAG_OBSOLETE);

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
                .setTags(TAG_SQL_SERVER, TAG_CURSOR);

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
                .setTags(TAG_SQL_SERVER, TAG_TRIGGER);

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
                .setTags(TAG_SQL_SERVER, TAG_READABILITY);

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
                .setTags(TAG_SQL_SERVER, TAG_READABILITY);

        // Rule 19: SyntaxError
        repository.createRule(CheckList.SYNTAX_ERROR_RULE_KEY)
                .setName("T-SQL syntax error")
                .setHtmlDescription(
                        "<p>The file failed to parse with the Microsoft T-SQL ScriptDOM parser due to syntax errors.</p>")
                .setSeverity(Severity.BLOCKER)
                .setType(RuleType.BUG)
                .setStatus(RuleStatus.READY)
                .setTags(TAG_SQL_SERVER, TAG_SYNTAX_ERROR);

        repository.done();
    }
}
