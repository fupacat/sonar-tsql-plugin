# SonarQube T-SQL Plugin

[![CI](https://github.com/fupacat/sonar-tsql-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/fupacat/sonar-tsql-plugin/actions/workflows/ci.yml)
[![CodeQL](https://github.com/fupacat/sonar-tsql-plugin/actions/workflows/codeql.yml/badge.svg)](https://github.com/fupacat/sonar-tsql-plugin/actions/workflows/codeql.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fupacat_sonar-tsql-plugin&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fupacat_sonar-tsql-plugin)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![ScriptDOM](https://img.shields.io/badge/Powered%20By-tsql--scriptdom--java-orange.svg)](https://github.com/fupacat/tsql-scriptdom-java)

**SonarQube T-SQL Plugin** provides native static code analysis and AST-based linting for **Microsoft Transact-SQL (T-SQL)** in SonarQube and SonarCloud.

Powered by [**tsql-scriptdom-java**](https://github.com/fupacat/tsql-scriptdom-java), this plugin parses T-SQL files using Microsoft's official ScriptDOM grammar directly on the JVM—without external native CLI dependencies or .NET bridges.

---

## Features

- **Language Registration:** Recognizes `.sql` and `.tsql` files as first-class Transact-SQL source units.
- **Deep AST Analysis:** Evaluates parsed Abstract Syntax Tree fragments with token streams and exact file coordinates.
- **Syntax Error Detection:** Flags parsing and grammar violations with line and column precision.
- **Built-in Quality Profile:** Pre-configured "Sonar way" quality profile activating all built-in rules out of the box.

---

## Built-In Rules

| Rule Key | Rule Name | Severity | Type | Description |
| :--- | :--- | :--- | :--- | :--- |
| `AvoidSelectAll` | Avoid using `SELECT *` | Major | Code Smell | Discourages wildcard column selection. Enforces explicit column lists to minimize I/O and prevent schema coupling. |
| `AvoidLeadingWildcardInLike` | Avoid leading wildcards in `LIKE` predicates | Critical | Bug | Prevents `LIKE '%...'` and `LIKE '_...'` expressions that defeat index seeks and cause full table/index scans. |
| `AvoidNoLockHint` | Avoid `NOLOCK` and `READUNCOMMITTED` table hints | Major | Bug | Flags dirty reads, missing rows, and allocation scan inconsistencies caused by the `NOLOCK` hint. |
| `AvoidDeprecatedTypes` | Avoid deprecated data types (`TEXT`, `NTEXT`, `IMAGE`) | Major | Code Smell | Detects legacy LOB types and recommends modern replacements (`VARCHAR(MAX)`, `NVARCHAR(MAX)`, `VARBINARY(MAX)`). |
| `SyntaxError` | T-SQL syntax error | Blocker | Bug | Surfaces syntax errors directly reported by the Microsoft T-SQL ScriptDOM parser. |

---

## Compatibility

- **SonarQube Server:** 9.9 LTS and 10.x+
- **Java Runtime:** Java 17 or higher
- **SQL Server Versions:** SQL Server 2008 through SQL Server 2022, Azure SQL Database

---

## Installation

This repository is currently on the `0.1.0-SNAPSHOT` development line and does not have a published production release on GitHub Releases yet. Until a release tag is created and published, install the plugin by building the current checkout from source.

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

This repository does not publish a production release as part of issue resolution. A release is only created after the tag, POM version, changelog, and artifact name agree.

1. Update `pom.xml` to the intended release version (for example `0.1.0`) and remove `-SNAPSHOT`.
2. Update `CHANGELOG.md` to the same version and record the release notes.
3. Create and push the matching Git tag, such as `v0.1.0`.
4. Run the GitHub Actions `Release` workflow for that tag.
5. Verify the built artifact exists as `target/sonar-tsql-plugin-<version>.jar` and matches the requested tag before publishing the GitHub release.

---

## License

This project is licensed under the [MIT License](LICENSE).
T-SQL parsing capabilities powered by [tsql-scriptdom-java](https://github.com/fupacat/tsql-scriptdom-java).
