package io.github.fupacat.sonar.tsql.rules;

import io.github.fupacat.sonar.tsql.TSqlLanguage;
import io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedTypesCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidLeadingWildcardInLikeCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidNoLockHintCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck;
import io.github.fupacat.sonar.tsql.checks.CaseElseMissingCheck;
import io.github.fupacat.sonar.tsql.checks.CaseInputInvariantCheck;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.checks.ControlStructureBlockCheck;
import io.github.fupacat.sonar.tsql.checks.DeleteAndUpdateWithoutWhereCheck;
import io.github.fupacat.sonar.tsql.checks.DuplicateBinaryOperandCheck;
import io.github.fupacat.sonar.tsql.checks.DuplicateIfConditionCheck;
import io.github.fupacat.sonar.tsql.checks.GotoStatementCheck;
import io.github.fupacat.sonar.tsql.checks.IdenticalBranchCheck;
import io.github.fupacat.sonar.tsql.checks.NestedIfMergeCheck;
import io.github.fupacat.sonar.tsql.checks.NotInSubqueryCheck;
import io.github.fupacat.sonar.tsql.checks.NullComparisonCheck;
import io.github.fupacat.sonar.tsql.checks.RepeatedUnaryOperatorCheck;
import io.github.fupacat.sonar.tsql.checks.TopWithoutOrderByCheck;
import io.github.fupacat.sonar.tsql.checks.WhereClauseRedundancyCheck;
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
    private static final String TAG_LOGIC = "logic";
    private static final String TAG_CONTROL_FLOW = "control-flow";
    private static final String TAG_MAINTAINABILITY = "maintainability";

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

        // Rule 19: SyntaxError
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
