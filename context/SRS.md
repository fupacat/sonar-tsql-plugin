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
