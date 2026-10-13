# Interfaces

```json
{
  "kind": "interface",
  "id": "IF-001",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/rules/TSqlQualityProfile.java",
  "symbol": "TSqlQualityProfile.define",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "Context",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "context"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.sonar.tsql.TSqlLanguage",
    "io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedTypesCheck",
    "io.github.fupacat.sonar.tsql.checks.AvoidLeadingWildcardInLikeCheck",
    "io.github.fupacat.sonar.tsql.checks.AvoidNoLockHintCheck",
    "io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck",
    "io.github.fupacat.sonar.tsql.checks.CaseInputInvariantCheck",
    "io.github.fupacat.sonar.tsql.checks.CheckList",
    "io.github.fupacat.sonar.tsql.checks.DeleteAndUpdateWithoutWhereCheck",
    "io.github.fupacat.sonar.tsql.checks.NotInSubqueryCheck",
    "io.github.fupacat.sonar.tsql.checks.NullComparisonCheck",
    "io.github.fupacat.sonar.tsql.checks.TopWithoutOrderByCheck",
    "org.sonar.api.server.profile.BuiltInQualityProfilesDefinition"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-002",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/rules/TSqlRulesDefinition.java",
  "symbol": "TSqlRulesDefinition.define",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "Context",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "context"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.sonar.tsql.TSqlLanguage",
    "io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedTypesCheck",
    "io.github.fupacat.sonar.tsql.checks.AvoidLeadingWildcardInLikeCheck",
    "io.github.fupacat.sonar.tsql.checks.AvoidNoLockHintCheck",
    "io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck",
    "io.github.fupacat.sonar.tsql.checks.CaseInputInvariantCheck",
    "io.github.fupacat.sonar.tsql.checks.CheckList",
    "io.github.fupacat.sonar.tsql.checks.DeleteAndUpdateWithoutWhereCheck",
    "io.github.fupacat.sonar.tsql.checks.NotInSubqueryCheck",
    "io.github.fupacat.sonar.tsql.checks.NullComparisonCheck",
    "io.github.fupacat.sonar.tsql.checks.TopWithoutOrderByCheck",
    "org.sonar.api.rule.RuleStatus",
    "org.sonar.api.rule.Severity",
    "org.sonar.api.rules.RuleType",
    "org.sonar.api.server.rule.RulesDefinition"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-003",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/TSqlLanguage.java",
  "symbol": "TSqlLanguage.getFileSuffixes",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "String[]"
  },
  "dependencies": [
    "java.util.Arrays",
    "java.util.Optional",
    "org.sonar.api.config.Configuration",
    "org.sonar.api.resources.AbstractLanguage"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-004",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/AvoidNoLockHintCheck.java",
  "symbol": "AvoidNoLockHintCheck.getRuleKey",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "String"
  },
  "dependencies": [
    "io.github.fupacat.ast.ForceSeekTableHint",
    "io.github.fupacat.ast.IndexTableHint",
    "io.github.fupacat.ast.LiteralTableHint",
    "io.github.fupacat.ast.TableHint",
    "io.github.fupacat.ast.TableHintKind"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-005",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/AvoidNoLockHintCheck.java",
  "symbol": "AvoidNoLockHintCheck.visit",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "TableHint",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "node"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ast.ForceSeekTableHint",
    "io.github.fupacat.ast.IndexTableHint",
    "io.github.fupacat.ast.LiteralTableHint",
    "io.github.fupacat.ast.TableHint",
    "io.github.fupacat.ast.TableHintKind"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-006",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/AvoidLeadingWildcardInLikeCheck.java",
  "symbol": "AvoidLeadingWildcardInLikeCheck.getRuleKey",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "String"
  },
  "dependencies": [
    "io.github.fupacat.ast.LikePredicate",
    "io.github.fupacat.ast.StringLiteral"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-007",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/AvoidLeadingWildcardInLikeCheck.java",
  "symbol": "AvoidLeadingWildcardInLikeCheck.visit",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "LikePredicate",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "node"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ast.LikePredicate",
    "io.github.fupacat.ast.StringLiteral"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-008",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/CheckList.java",
  "symbol": "CheckList.getChecks",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "List<Class<? extends TSqlCheck>>"
  },
  "dependencies": [
    "java.util.List"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-009",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/CheckList.java",
  "symbol": "CheckList.instantiateChecks",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "List<TSqlCheck>"
  },
  "dependencies": [
    "java.util.List"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-010",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/AvoidSelectAllCheck.java",
  "symbol": "AvoidSelectAllCheck.getRuleKey",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "String"
  },
  "dependencies": [
    "io.github.fupacat.ast.SelectStarExpression"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-011",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/AvoidSelectAllCheck.java",
  "symbol": "AvoidSelectAllCheck.visit",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "SelectStarExpression",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "node"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ast.SelectStarExpression"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-012",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/TSqlCheck.java",
  "symbol": "TSqlCheck.getRuleKey",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "String"
  },
  "dependencies": [
    "io.github.fupacat.ast.TSqlConcreteFragmentVisitor",
    "io.github.fupacat.ast.TSqlFragment"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-013",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/TSqlCheck.java",
  "symbol": "TSqlCheck.scan",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "TSqlFragment",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "root"
      },
      {
        "annotation": "TSqlCheckContext",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "context"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ast.TSqlConcreteFragmentVisitor",
    "io.github.fupacat.ast.TSqlFragment"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-014",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/TSqlCheck.java",
  "symbol": "TSqlCheck.getRoot",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "TSqlFragment"
  },
  "dependencies": [
    "io.github.fupacat.ast.TSqlConcreteFragmentVisitor",
    "io.github.fupacat.ast.TSqlFragment"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-015",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/TSqlCheck.java",
  "symbol": "TSqlCheck.reportIssue",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "TSqlFragment",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "fragment"
      },
      {
        "annotation": "String",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "message"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ast.TSqlConcreteFragmentVisitor",
    "io.github.fupacat.ast.TSqlFragment"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-016",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/TSqlCheck.java",
  "symbol": "TSqlCheck.reportIssueAtLine",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "int",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "line"
      },
      {
        "annotation": "String",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "message"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ast.TSqlConcreteFragmentVisitor",
    "io.github.fupacat.ast.TSqlFragment"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-017",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/AvoidDeprecatedTypesCheck.java",
  "symbol": "AvoidDeprecatedTypesCheck.getRuleKey",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "String"
  },
  "dependencies": [
    "io.github.fupacat.ast.SqlDataTypeOption",
    "io.github.fupacat.ast.SqlDataTypeReference"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-018",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/AvoidDeprecatedTypesCheck.java",
  "symbol": "AvoidDeprecatedTypesCheck.visit",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "SqlDataTypeReference",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "node"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ast.SqlDataTypeOption",
    "io.github.fupacat.ast.SqlDataTypeReference"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-019",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/TSqlCheckContext.java",
  "symbol": "TSqlCheckContext.getFile",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "InputFile"
  },
  "dependencies": [
    "io.github.fupacat.ast.TSqlFragment",
    "org.sonar.api.batch.fs.InputFile"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-020",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/TSqlCheckContext.java",
  "symbol": "TSqlCheckContext.reportIssue",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "TSqlCheck",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "check"
      },
      {
        "annotation": "TSqlFragment",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "fragment"
      },
      {
        "annotation": "String",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "message"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ast.TSqlFragment",
    "org.sonar.api.batch.fs.InputFile"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-021",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/TSqlCheckContext.java",
  "symbol": "TSqlCheckContext.reportIssueAtLine",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "TSqlCheck",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "check"
      },
      {
        "annotation": "int",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "line"
      },
      {
        "annotation": "String",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "message"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ast.TSqlFragment",
    "org.sonar.api.batch.fs.InputFile"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-022",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/TSqlPlugin.java",
  "symbol": "TSqlPlugin.define",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "Context",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "context"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.sonar.tsql.rules.TSqlQualityProfile",
    "io.github.fupacat.sonar.tsql.rules.TSqlRulesDefinition",
    "org.sonar.api.Plugin",
    "org.sonar.api.config.PropertyDefinition"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-023",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/TSqlSensor.java",
  "symbol": "TSqlSensor.describe",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "SensorDescriptor",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "descriptor"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ParseError",
    "io.github.fupacat.ParseResult",
    "io.github.fupacat.TSqlParser",
    "io.github.fupacat.TSqlParserToken",
    "io.github.fupacat.ast.TSqlFragment",
    "io.github.fupacat.ast.TSqlScript",
    "io.github.fupacat.sonar.tsql.checks.CheckList",
    "io.github.fupacat.sonar.tsql.checks.TSqlCheck",
    "io.github.fupacat.sonar.tsql.checks.TSqlCheckContext",
    "io.github.fupacat.sonar.tsql.rules.TSqlRulesDefinition",
    "java.io.IOException",
    "java.util.ArrayList",
    "java.util.List",
    "org.slf4j.Logger",
    "org.slf4j.LoggerFactory",
    "org.sonar.api.batch.fs.FilePredicate",
    "org.sonar.api.batch.fs.FileSystem",
    "org.sonar.api.batch.fs.InputFile",
    "org.sonar.api.batch.fs.TextRange",
    "org.sonar.api.batch.sensor.Sensor",
    "org.sonar.api.batch.sensor.SensorContext",
    "org.sonar.api.batch.sensor.SensorDescriptor",
    "org.sonar.api.batch.sensor.issue.NewIssue",
    "org.sonar.api.batch.sensor.issue.NewIssueLocation",
    "org.sonar.api.rule.RuleKey"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-024",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/TSqlSensor.java",
  "symbol": "TSqlSensor.execute",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "SensorContext",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "context"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ParseError",
    "io.github.fupacat.ParseResult",
    "io.github.fupacat.TSqlParser",
    "io.github.fupacat.TSqlParserToken",
    "io.github.fupacat.ast.TSqlFragment",
    "io.github.fupacat.ast.TSqlScript",
    "io.github.fupacat.sonar.tsql.checks.CheckList",
    "io.github.fupacat.sonar.tsql.checks.TSqlCheck",
    "io.github.fupacat.sonar.tsql.checks.TSqlCheckContext",
    "io.github.fupacat.sonar.tsql.rules.TSqlRulesDefinition",
    "java.io.IOException",
    "java.util.ArrayList",
    "java.util.List",
    "org.slf4j.Logger",
    "org.slf4j.LoggerFactory",
    "org.sonar.api.batch.fs.FilePredicate",
    "org.sonar.api.batch.fs.FileSystem",
    "org.sonar.api.batch.fs.InputFile",
    "org.sonar.api.batch.fs.TextRange",
    "org.sonar.api.batch.sensor.Sensor",
    "org.sonar.api.batch.sensor.SensorContext",
    "org.sonar.api.batch.sensor.SensorDescriptor",
    "org.sonar.api.batch.sensor.issue.NewIssue",
    "org.sonar.api.batch.sensor.issue.NewIssueLocation",
    "org.sonar.api.rule.RuleKey"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-025",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/DeleteAndUpdateWithoutWhereCheck.java",
  "symbol": "DeleteAndUpdateWithoutWhereCheck.getRuleKey",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "String"
  },
  "dependencies": [
    "io.github.fupacat.ast.DeleteSpecification",
    "io.github.fupacat.ast.TSqlConcreteFragmentVisitor",
    "io.github.fupacat.ast.TSqlScript",
    "io.github.fupacat.ast.UpdateSpecification"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-026",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/DeleteAndUpdateWithoutWhereCheck.java",
  "symbol": "DeleteAndUpdateWithoutWhereCheck.visit",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "TSqlScript",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "node"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ast.DeleteSpecification",
    "io.github.fupacat.ast.TSqlConcreteFragmentVisitor",
    "io.github.fupacat.ast.TSqlScript",
    "io.github.fupacat.ast.UpdateSpecification"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-027",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/NotInSubqueryCheck.java",
  "symbol": "NotInSubqueryCheck.getRuleKey",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "String"
  },
  "dependencies": [
    "io.github.fupacat.ast.InPredicate"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-028",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/NotInSubqueryCheck.java",
  "symbol": "NotInSubqueryCheck.visit",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "InPredicate",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "node"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ast.InPredicate"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-029",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/NullComparisonCheck.java",
  "symbol": "NullComparisonCheck.getRuleKey",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "String"
  },
  "dependencies": [
    "io.github.fupacat.ast.BooleanComparisonExpression",
    "io.github.fupacat.ast.BooleanComparisonType",
    "io.github.fupacat.ast.NullLiteral"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-030",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/NullComparisonCheck.java",
  "symbol": "NullComparisonCheck.visit",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "BooleanComparisonExpression",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "node"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ast.BooleanComparisonExpression",
    "io.github.fupacat.ast.BooleanComparisonType",
    "io.github.fupacat.ast.NullLiteral"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-031",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/TopWithoutOrderByCheck.java",
  "symbol": "TopWithoutOrderByCheck.getRuleKey",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "String"
  },
  "dependencies": [
    "io.github.fupacat.ast.QuerySpecification"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-032",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/TopWithoutOrderByCheck.java",
  "symbol": "TopWithoutOrderByCheck.visit",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "QuerySpecification",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "node"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ast.QuerySpecification"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-033",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/CaseInputInvariantCheck.java",
  "symbol": "CaseInputInvariantCheck.getRuleKey",
  "signature": {
    "async": false,
    "parameters": [],
    "return": "String"
  },
  "dependencies": [
    "io.github.fupacat.ast.FunctionCall",
    "io.github.fupacat.ast.SimpleCaseExpression"
  ],
  "exchanges": []
}
```

```json
{
  "kind": "interface",
  "id": "IF-034",
  "path": "src/main/java/io/github/fupacat/sonar/tsql/checks/CaseInputInvariantCheck.java",
  "symbol": "CaseInputInvariantCheck.visit",
  "signature": {
    "async": false,
    "parameters": [
      {
        "annotation": "SimpleCaseExpression",
        "default": null,
        "kind": "positional_or_keyword",
        "name": "node"
      }
    ],
    "return": "void"
  },
  "dependencies": [
    "io.github.fupacat.ast.FunctionCall",
    "io.github.fupacat.ast.SimpleCaseExpression"
  ],
  "exchanges": []
}
```

