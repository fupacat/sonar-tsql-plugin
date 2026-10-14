# SonarQube T-SQL Plugin

[![CI](https://github.com/fupacat/sonar-tsql-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/fupacat/sonar-tsql-plugin/actions/workflows/ci.yml)
[![CodeQL](https://github.com/fupacat/sonar-tsql-plugin/actions/workflows/codeql.yml/badge.svg)](https://github.com/fupacat/sonar-tsql-plugin/actions/workflows/codeql.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fupacat_sonar-tsql-plugin&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fupacat_sonar-tsql-plugin)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![ScriptDOM](https://img.shields.io/badge/Powered%20By-tsql--scriptdom--java-orange.svg)](https://github.com/fupacat/tsql-scriptdom-java)

**SonarQube T-SQL Plugin** provides native static analysis and AST-based linting for **Microsoft Transact-SQL (T-SQL)** in SonarQube and SonarCloud.

Powered by [**tsql-scriptdom-java**](https://github.com/fupacat/tsql-scriptdom-java), this plugin parses T-SQL files using Microsoft's official ScriptDOM grammar directly on the JVM—without external native CLI dependencies or .NET bridges.

---

## Features

- **Language registration:** Recognizes `.sql` and `.tsql` files as first-class Transact-SQL sources.
- **Deep AST analysis:** Evaluates parsed Abstract Syntax Tree fragments with token streams and exact file coordinates.
- **Cross-file DDL catalog:** Builds a project schema catalog from `CREATE TABLE`, `CREATE INDEX`, and related DDL in analyzed files.
- **Syntax error detection:** Flags parsing and grammar violations with line and column precision.
- **Built-in quality profile:** The default `Sonar way` profile activates the full built-in rule set out of the box.

---

## Current rule inventory

This repository currently registers 71 built-in T-SQL rules in the `tsql` repository and activates them in the default `Sonar way` profile (`TSqlRulesDefinition` + `TSqlQualityProfile`). The inventory below reflects the implementation at the current revision, not the initial five-rule baseline.

### Default activation

All implemented rules below are active by default in the built-in `Sonar way` quality profile. The default profile is created by `TSqlQualityProfile.define(...)` and marked as `setDefault(true)`.

### Rule inventory by category

#### Core correctness and query safety (default active)

`AvoidSelectAll`, `AvoidLeadingWildcardInLike`, `AvoidNoLockHint`, `AvoidDeprecatedTypes`, `DeleteAndUpdateWithoutWhere`, `NotInSubquery`, `NullComparison`, `TopWithoutOrderBy`, `CaseInputInvariant`, `PrimaryKeyRequired`, `InsertColumnsExplicit`, `ExplicitLengthForVariableTypes`, `OrderByColumnNames`, `ExistsSelectOne`, `AlterTableCheckEnforcement`, `ColumnReferenceDepth`, `SargableWhere`, `UseScopeIdentity`, `AvoidVariableLengthDataType`, `AvoidDeprecatedJoinSyntax`, `AvoidSpecialCharactersInObjectNames`, `AvoidReservedWordsInObjectNames`, `StoredProcedureOutputParameter`, `DeterministicFunctionInWhere`, `AvoidSpPrefixProcedureName`, `SetNoCount`, `SetAnsiWarningsAndArithabort`, `SetAnsiNulls`, `AvoidDeprecatedSystemTables`, `FetchIntoCursorCount`, `CaseSubquery`, `TriggerResultSetCheck`, `AvoidLikeWithoutWildcard`, `AvoidNonStandardComparisonOperators`, `InPredicateIndexedColumn`, `NullableColumnIsNull`, `NarrowingCast`, `SyntaxError`

#### Security and vulnerability checks (default active)

`DynamicSqlExecution`, `WeakHash`, `HardCodedCredentials`

#### Logic and control-flow checks (default active)

`UnusedLocalVariable`, `UnusedParameter`, `UnusedLabel`, `UnreachableCode`, `SingleIterationLoop`, `SelfAssignment`, `CatchClauseShouldDoMoreThanRethrow`, `RedundantJump`, `DuplicateBinaryOperand`, `DuplicateIfCondition`, `IdenticalBranch`, `RepeatedUnaryOperator`, `WhereClauseRedundancy`, `ControlStructureBlock`, `CaseElseMissing`, `GotoStatement`, `NestedIfMerge`

#### Maintainability, formatting, and conventions (default active)

`S103`, `S104`, `S105`, `S122`, `S125`, `S1134`, `S1135`, `NoSonar`, `S1451`, `S3972`, `S3973`, `S138`, `S1151`

> The `Sonar way` profile is the default profile; it is not a partial or experimental subset. The plugin intentionally ships the complete rule set that is implemented in code and metadata.

---

## Cross-file DDL catalog scope and limitations

The plugin builds a project-wide schema catalog from the T-SQL DDL it can parse in the analyzed codebase. This catalog is used by schema-aware checks such as:

- `InPredicateIndexedColumn`
- `NullableColumnIsNull`
- `NarrowingCast`
- `PrimaryKeyRequired`

### What the catalog covers

- `CREATE TABLE` statements, including column definitions and basic nullability metadata
- `CREATE INDEX` / indexed column discovery for the project scope
- Cross-file resolution of column references and table/column lookups within the analyzed project
- Case-insensitive table / column matching for SQL Server-style identifier behavior

### What the catalog does not cover

- Live database metadata or remote schema introspection
- Dynamic DDL generation, runtime schema creation, or migration tools executed outside the analyzed files
- Full dependency analysis across external libraries, generated scripts, or stored procedures not included in the project scan
- A complete model of every SQL Server object type, partitioning, or advanced constraint semantics

This catalog is intentionally best-effort: it is designed to support project-aware checks without requiring a remote database connection or a full SQL Server engine.

---

## Known semantic limitations

The implemented rules are intentionally scoped to what the plugin can reason about reliably from the parsed T-SQL AST and the in-repo schema catalog.

- **AST-only logic:** Many checks are syntax-aware but not full semantic execution analysis.
- **Schema limitations:** Cross-file DDL metadata is derived from project files only and is not a live database schema.
- **No runtime metrics:** The plugin does not infer live query plans, execution timing, or distribution metrics from a running SQL Server instance.
- **No deep duplicate analysis:** The project does not currently implement repository-wide duplicate-code analysis or structural deduplication reporting.
- **No highlighting layer:** Inline editor highlights, code lens annotations, and richer UX-only rendering are not part of the current plugin behavior.

### Implemented vs. planned

Implemented today:

- AST parsing and rule execution
- Built-in Sonar rule registration and default quality profile
- Schema-aware checks based on project DDL catalog
- Rule metadata, severities, and repository registration

Not currently implemented:

- UI-level highlighting and code annotations
- Duplicate detection / clone analysis beyond local rule logic
- Project metrics dashboards or SQL-quality trend reporting

---

## Compatibility

- **SonarQube Server:** 9.9 LTS and 10.x+
- **Java Runtime:** Java 17 or higher
- **SQL Server Versions:** SQL Server 2008 through SQL Server 2022, Azure SQL Database

---

## Installation

1. Download the latest `sonar-tsql-plugin-<version>.jar` from [Releases](https://github.com/fupacat/sonar-tsql-plugin/releases).
2. Copy the `.jar` into your SonarQube server's plugin directory:
   ```bash
   cp sonar-tsql-plugin-*.jar $SONARQUBE_HOME/extensions/plugins/
   ```
3. Restart your SonarQube server:
   ```bash
   $SONARQUBE_HOME/bin/linux-x86-64/sonar.sh restart
   ```
4. Verify under **Administration > Marketplace > Installed Plugins** that "SonarQube T-SQL Plugin" is active.

---

## Building from Source

Prerequisites:
- JDK 17+
- Maven 3.8+

```bash
git clone https://github.com/fupacat/sonar-tsql-plugin.git
cd sonar-tsql-plugin
mvn clean package
```

The packaged plugin will be generated at `target/sonar-tsql-plugin-<version>.jar`.

---

## Maintaining rule inventory consistency

To reduce drift between the rule implementation and the documentation, the test suite includes a lightweight consistency check in `TSqlRulesDefinitionTest` that asserts the README still documents the current rule inventory, default profile, cross-file DDL catalog, and representative implementation keys (`AvoidSelectAll`, `DynamicSqlExecution`, `DuplicateBinaryOperand`, `NarrowingCast`, `SyntaxError`). This keeps the human-facing documentation aligned with the source of truth in `TSqlRulesDefinition` and `CheckList` without generating a full documentation artifact at build time.

---

## License

This project is licensed under the [MIT License](LICENSE).
T-SQL parsing capabilities powered by [tsql-scriptdom-java](https://github.com/fupacat/tsql-scriptdom-java).
