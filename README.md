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

The keys below are the actual rule keys registered by `TSqlRulesDefinition` and activated in the default `Sonar way` profile. Custom plugin rules use their implementation names (for example `AvoidSelectAll`), while standard Sonar keys use the corresponding Sonar identifier (for example `S1523`, `SR0014`, `S1614`).

#### Core correctness and query safety (default active)

`AvoidSelectAll`, `AvoidLeadingWildcardInLike`, `AvoidNoLockHint`, `AvoidDeprecatedTypes`, `S1590`, `S3641`, `S2527`, `S4054`, `S4094`, `S1523`, `S2070`, `S2068`, `S1614`, `S1745`, `S4078`, `S1735`, `S4801`, `S4102`, `S4149`, `SR0006`, `S4108`, `SR0009`, `SR0010`, `SR0011`, `S4413`, `S4196`, `SR0015`, `SR0016`, `S4819`, `S6498`, `S4154`, `S4155`, `S4103`, `S4104`, `S4109`, `S3643`, `S1742`, `SR0004`, `SR0007`, `SR0014`, `SyntaxError`

#### Logic and control-flow checks (default active)

`S1481`, `S1172`, `S1065`, `S1763`, `S1751`, `S1656`, `S2737`, `S3626`, `S1764`, `S1862`, `S1871`, `S2761`, `S3633`, `S121`, `S131`, `S907`, `S1066`

#### Maintainability, formatting, and conventions (default active)

`S103`, `S104`, `S105`, `S122`, `S125`, `S1134`, `S1135`, `NoSonar`, `S1451`, `S3972`, `S3973`, `S138`, `S1151`

> The `Sonar way` profile is the default profile; it is not a partial or experimental subset. The plugin intentionally ships the complete rule set that is implemented in code and metadata.

---

## Cross-file DDL catalog scope and limitations

The plugin builds a project-wide schema catalog from the T-SQL DDL it can parse in the analyzed codebase. This catalog is used by schema-aware checks such as:

- `SR0004` (`InPredicateIndexedColumn`)
- `SR0007` (`NullableColumnIsNull`)
- `SR0014` (`NarrowingCast`)

`S1614` (`PrimaryKeyRequired`) inspects `CREATE TABLE` statements directly and does not depend on the cross-file project catalog.

### What the catalog covers

- `CREATE TABLE` statements, including column definitions and basic nullability metadata
- `CREATE INDEX` / indexed column discovery for the project scope
- Cross-file resolution of table and column names within the analyzed project for those checks that explicitly read the project catalog
- Case-insensitive table / column matching for SQL Server-style identifier behavior

### What the catalog does not cover

- Live database metadata or remote schema introspection
- Dynamic DDL generation, runtime schema creation, or migration tools executed outside the analyzed files
- Full dependency analysis across external libraries, generated scripts, or stored procedures not included in the project scan
- Full SQL Server semantic coverage such as alias resolution beyond the simplest same-name lookups, advanced partitioning, constraint semantics, or runtime object metadata

This catalog is intentionally best-effort: it is designed to support project-aware checks without requiring a remote database connection or a full SQL Server engine. Current limitations include dropping schema/database qualifiers when indexing tables, treating aliases as table names, and falling back to a matching column elsewhere in the project. These can produce incorrect findings when names collide or metadata is missing; scoped resolution is tracked in issue #37.

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

- AST parsing and rule execution for T-SQL files
- Built-in Sonar rule registration and the default `Sonar way` quality profile
- Schema-aware checks that rely on the best-effort project DDL catalog
- Rule metadata, severities, tags, and repository registration

Not currently implemented:

- Syntax highlighting in the IDE or Sonar UI
- CPD token-based duplication detection
- Source code measures such as lines-of-code, complexity metrics, or code churn dashboards
- Live SQL execution metrics, runtime query plans, or dashboards backed by a running SQL Server instance

---

## Compatibility

- **SonarQube Server:** 9.9 LTS and 10.x+
- **Java Runtime:** Java 17 or higher
- **SQL Server Versions:** SQL Server 2008 through SQL Server 2022, Azure SQL Database

---

## Installation

This repository is currently on the `0.1.0-SNAPSHOT` development line and does not have a published production release on GitHub Releases yet. Until a release tag is created and published, install the plugin by building the current checkout from source.

Prerequisites for source builds:

- Java Development Kit (JDK) 17+
- Maven 3.8+

1. Build the current project from source:
   ```bash
   git clone https://github.com/fupacat/sonar-tsql-plugin.git
   cd sonar-tsql-plugin
   mvn clean package
   ```
2. Copy the generated JAR into your SonarQube server's plugin directory:
   ```bash
   cp target/sonar-tsql-plugin-*.jar $SONARQUBE_HOME/extensions/plugins/
   ```
3. Restart your SonarQube server:
   ```bash
   $SONARQUBE_HOME/bin/linux-x86-64/sonar.sh restart
   ```
4. Verify under **Administration > Marketplace > Installed Plugins** that "SonarQube T-SQL Plugin" is active.

For a tagged release such as `v0.1.0`, the plugin artifact will be named `target/sonar-tsql-plugin-0.1.0.jar`, matching the Git tag without the leading `v`.

---

## Release Procedure

Release publication is triggered by pushing a matching semantic-version tag (`vX.Y.Z`). The workflow enforces that the requested tag exists, the POM version matches the tag, and the packaged artifact exists with the expected plugin class before creating the GitHub release.

Before tagging:

1. Update `pom.xml` to the intended release version (for example `0.1.0`) and remove `-SNAPSHOT`.
2. Update `CHANGELOG.md` to the same version and record the release notes.
3. Run `mvn clean verify --batch-mode` locally to confirm the release artifact builds cleanly.

Publish:

1. Create and push the matching Git tag, such as `v0.1.0`.
2. Confirm the `Release` workflow succeeds for that tag.

---

## Maintaining rule inventory consistency

The consistency test in TSqlRulesDefinitionTest extracts every documented rule key from the category lists and compares the complete set against the registered RulesDefinition repository, rejecting missing, extra, or duplicate entries. It also checks the displayed rule count against the registry size and verifies the default profile and catalog documentation. This keeps the inventory aligned with the actual Sonar rule keys rather than implementation class names.

---

## License

This project is licensed under the [MIT License](LICENSE).
T-SQL parsing capabilities powered by [tsql-scriptdom-java](https://github.com/fupacat/tsql-scriptdom-java).
