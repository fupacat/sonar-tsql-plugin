# Requirements

## Specification & Capabilities

[REQ-001] TSqlLanguage SHALL declare the language key and default file extensions for T-SQL analysis.

```json
{
  "requirement": "REQ-001",
  "kind": "categorical",
  "precondition": "TSqlLanguage instance initialized",
  "action": "invoke TSqlLanguage getFileSuffixes",
  "expected": "contains .sql and .tsql suffixes",
  "method": "junit",
  "expected_output": "[.sql, .tsql]"
}
```

[REQ-002] TSqlPlugin SHALL register core extensions with the SonarQube plugin context.

```json
{
  "requirement": "REQ-002",
  "kind": "categorical",
  "precondition": "Plugin context provided to TSqlPlugin",
  "action": "invoke TSqlPlugin define",
  "expected": "registers language, sensor, rules definition, and quality profile",
  "method": "junit",
  "expected_output": "Context"
}
```

[REQ-003] TSqlRulesDefinition SHALL define repository rules and repository metadata for T-SQL.

```json
{
  "requirement": "REQ-003",
  "kind": "categorical",
  "precondition": "RulesDefinition Context provided",
  "action": "invoke TSqlRulesDefinition define",
  "expected": "repository tsql defined with repository name and active rules",
  "method": "junit",
  "expected_output": "Context"
}
```

[REQ-004] TSqlQualityProfile SHALL declare the Sonar way built-in profile activating registered rules.

```json
{
  "requirement": "REQ-004",
  "kind": "categorical",
  "precondition": "BuiltInQualityProfilesDefinition Context provided",
  "action": "invoke TSqlQualityProfile define",
  "expected": "built-in profile defined for tsql language",
  "method": "junit",
  "expected_output": "Context"
}
```

[REQ-005] TSqlSensor SHALL describe sensor capabilities and execute AST analysis on T-SQL files.

```json
{
  "requirement": "REQ-005",
  "kind": "categorical",
  "precondition": "SensorContext provided with input files",
  "action": "invoke TSqlSensor execute",
  "expected": "processes files and reports rule violations",
  "method": "junit",
  "expected_output": "void"
}
```

[REQ-006] CheckList SHALL provide the registered list of AST check classes.

```json
{
  "requirement": "REQ-006",
  "kind": "categorical",
  "precondition": "CheckList class accessed",
  "action": "invoke CheckList instantiateChecks",
  "expected": "returns non-empty list of check instances",
  "method": "junit",
  "expected_output": "List"
}
```

[REQ-007] AvoidSelectAllCheck SHALL flag SELECT star expressions in query specifications.

```json
{
  "requirement": "REQ-007",
  "kind": "categorical",
  "precondition": "SELECT star query parsed",
  "action": "invoke AvoidSelectAllCheck scan",
  "expected": "issues reported for select star expressions",
  "method": "junit",
  "expected_output": "void"
}
```

[REQ-008] AvoidLeadingWildcardInLikeCheck SHALL flag LIKE predicates starting with percentage or underscore wildcards.

```json
{
  "requirement": "REQ-008",
  "kind": "categorical",
  "precondition": "LIKE expression with leading wildcard parsed",
  "action": "invoke AvoidLeadingWildcardInLikeCheck scan",
  "expected": "issue reported for leading wildcard pattern",
  "method": "junit",
  "expected_output": "void"
}
```

[REQ-009] AvoidNoLockHintCheck SHALL flag table hints specifying NOLOCK or READUNCOMMITTED.

```json
{
  "requirement": "REQ-009",
  "kind": "categorical",
  "precondition": "table query with NOLOCK hint parsed",
  "action": "invoke AvoidNoLockHintCheck scan",
  "expected": "issue reported for NOLOCK table hint",
  "method": "junit",
  "expected_output": "void"
}
```

[REQ-010] AvoidDeprecatedTypesCheck SHALL flag declarations using deprecated data types text, ntext, or image.

```json
{
  "requirement": "REQ-010",
  "kind": "categorical",
  "precondition": "table definition with deprecated data types parsed",
  "action": "invoke AvoidDeprecatedTypesCheck scan",
  "expected": "issue reported for deprecated data types",
  "method": "junit",
  "expected_output": "void"
}
```

[REQ-011] DeleteAndUpdateWithoutWhereCheck SHALL flag DELETE and UPDATE statements that omit a WHERE clause.

```json
{
  "requirement": "REQ-011",
  "kind": "categorical",
  "precondition": "DELETE or UPDATE statement without WHERE clause parsed",
  "action": "invoke DeleteAndUpdateWithoutWhereCheck scan",
  "expected": "issue reported for missing WHERE clause",
  "method": "junit",
  "expected_output": "void"
}
```

[REQ-012] NotInSubqueryCheck SHALL flag NOT IN predicates that use a subquery.

```json
{
  "requirement": "REQ-012",
  "kind": "categorical",
  "precondition": "NOT IN predicate with subquery parsed",
  "action": "invoke NotInSubqueryCheck scan",
  "expected": "issue reported for NOT IN subquery",
  "method": "junit",
  "expected_output": "void"
}
```

[REQ-013] NullComparisonCheck SHALL flag equality and inequality comparisons directly against NULL literals.

```json
{
  "requirement": "REQ-013",
  "kind": "categorical",
  "precondition": "equality or inequality comparison with NULL literal parsed",
  "action": "invoke NullComparisonCheck scan",
  "expected": "issue reported for direct NULL comparison",
  "method": "junit",
  "expected_output": "void"
}
```

[REQ-014] TopWithoutOrderByCheck SHALL flag SELECT TOP query specifications that omit an ORDER BY clause.

```json
{
  "requirement": "REQ-014",
  "kind": "categorical",
  "precondition": "SELECT TOP query without ORDER BY parsed",
  "action": "invoke TopWithoutOrderByCheck scan",
  "expected": "issue reported for TOP without ORDER BY",
  "method": "junit",
  "expected_output": "void"
}
```

[REQ-015] CaseInputInvariantCheck SHALL flag simple CASE expressions whose input expression invokes non-deterministic functions RAND or NEWID.

```json
{
  "requirement": "REQ-015",
  "kind": "categorical",
  "precondition": "simple CASE expression with non-deterministic input function parsed",
  "action": "invoke CaseInputInvariantCheck scan",
  "expected": "issue reported for non-invariant CASE input expression",
  "method": "junit",
  "expected_output": "void"
}
```

[REQ-016] InPredicateIndexedColumnCheck SHALL flag IN predicates whose left-hand column is not known to be indexed.

```json
{
  "requirement": "REQ-016",
  "kind": "categorical",
  "precondition": "IN predicate against a table column parsed",
  "action": "invoke InPredicateIndexedColumnCheck scan",
  "expected": "issue reported when the column is absent from the project schema catalog or not indexed",
  "method": "junit",
  "expected_output": "void"
}
```

[REQ-017] NullableColumnIsNullCheck SHALL flag COALESCE expressions that cannot be replaced with ISNULL without changing semantics, such as multi-argument COALESCE or cases where the fallback changes type precedence or length.

```json
{
  "requirement": "REQ-017",
  "kind": "categorical",
  "precondition": "COALESCE expression with a nullable column and a fallback that may change result semantics parsed",
  "action": "invoke NullableColumnIsNullCheck scan",
  "expected": "issue reported only when the COALESCE/ISNULL rewrite would alter evaluation order, type precedence, or result length",
  "method": "junit",
  "expected_output": "void"
}
```

[REQ-018] NarrowingCastCheck SHALL flag CAST/CONVERT/TRY_CONVERT operations that narrow a source column into a shorter or smaller type.

```json
{
  "requirement": "REQ-018",
  "kind": "categorical",
  "precondition": "CAST/CONVERT target type and source column metadata resolved from project schema",
  "action": "invoke NarrowingCastCheck scan",
  "expected": "issue reported when target type or length is narrower than the source column",
  "method": "junit",
  "expected_output": "void"
}
```

