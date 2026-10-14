# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Declared `sonar.tsql.file.suffixes` `PropertyDefinition` in `TSqlPlugin` for automatic file association and SonarQube settings UI discovery.
- Community and open-source standards documentation (`CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, PR/Issue templates).
- GitHub Actions CodeQL SAST workflow for Java security analysis.
- MCSWE v3 mission-control gate protocol configuration and acceptance criteria verification.
- Documented the complete implemented rule inventory, default `Sonar way` activation, and schema catalog limitations in the public README.
- Added a lightweight rule-inventory consistency check to keep README metadata aligned with the Java rule registry.

### Changed
- Upgraded Maven plugins and test dependencies: `sonar-packaging-maven-plugin` 2.0.0, `maven-compiler-plugin` 3.16.0, `maven-surefire-plugin` 3.6.0, `junit-jupiter` 6.1.3, `assertj-core` 3.27.7, `mockito` 5.24.0.
- Refreshed the changelog and README to describe the current 71-rule implementation, including security, logic, and schema-aware checks, instead of the original five-rule baseline.
- Clarified the distinction between implemented behavior and planned future capabilities such as highlighting, duplication analysis, and metrics.

### Notes
- The plugin now documents the project-wide DDL schema catalog as a best-effort, cross-file metadata layer rather than a full live database schema mirror.
- The rule set includes 71 built-in checks, including schema-aware, security, control-flow, formatting, and syntax-error detection rules.

## [0.1.0] - 2026-10-11

### Added
- Initial release of SonarQube T-SQL Plugin powered by `tsql-scriptdom-java`.
- T-SQL language registration (`.sql`, `.tsql`).
- Default `"Sonar way"` Quality Profile.
- AST parsing sensor with syntax error reporting (`tsql:SyntaxError`).
- Built-in static code analysis rules:
  - `AvoidSelectAll` (`SELECT *` detection)
  - `AvoidNoLockHint` (`NOLOCK` / `READUNCOMMITTED` dirty read hints)
  - `AvoidLeadingWildcardInLike` (non-sargable `LIKE '%...'` predicates)
  - `AvoidDeprecatedTypes` (`TEXT`, `NTEXT`, `IMAGE` legacy data types)
