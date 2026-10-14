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

### Changed
- Upgraded Maven plugins and test dependencies: `sonar-packaging-maven-plugin` 2.0.0, `maven-compiler-plugin` 3.16.0, `maven-surefire-plugin` 3.6.0, `junit-jupiter` 6.1.3, `assertj-core` 3.27.7, `mockito` 5.24.0.


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

### Notes
- This project has not published a GitHub release for `0.1.0` yet. The `0.1.0` changelog entry documents the intended first release, but the repository remains on the `0.1.0-SNAPSHOT` development line until the matching tag and release artifact are produced.
