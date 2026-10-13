# Developer & Agent Guide (`sonar-tsql-plugin`)

## 1. Architecture Overview

`sonar-tsql-plugin` is a SonarQube plugin for Microsoft Transact-SQL (T-SQL) powered by `io.github.fupacat:tsql-scriptdom-core:0.1.0` (`tsql-scriptdom-java`).

- **Language & Plugin Entry Point**:
  - `TSqlPlugin` (`src/main/java/io/github/fupacat/sonar/tsql/TSqlPlugin.java`)
  - `TSqlLanguage` (`src/main/java/io/github/fupacat/sonar/tsql/TSqlLanguage.java`)
- **Sensor**:
  - `TSqlSensor` (`src/main/java/io/github/fupacat/sonar/tsql/TSqlSensor.java`) — parses `.sql` / `.tsql` files with `TSqlParser.parse(content)` and invokes active `TSqlCheck` visitors.
- **Rules & Quality Profile**:
  - `TSqlRulesDefinition` (`src/main/java/io/github/fupacat/sonar/tsql/rules/TSqlRulesDefinition.java`)
  - `TSqlQualityProfile` (`src/main/java/io/github/fupacat/sonar/tsql/rules/TSqlQualityProfile.java`)
- **AST Checks**:
  - `TSqlCheck` base class (`src/main/java/io/github/fupacat/sonar/tsql/checks/TSqlCheck.java`) — extends `TSqlFragmentVisitor` from `io.github.fupacat.ast`.
  - `CheckList` (`src/main/java/io/github/fupacat/sonar/tsql/checks/CheckList.java`) — central registry of all check classes and instances.

## 2. How to Add a New T-SQL Check

1. **Inspect `tsql-scriptdom-core` AST classes** (if needed):
   ```bash
   javap -cp ~/.m2/repository/io/github/fupacat/tsql-scriptdom-core/0.1.0/tsql-scriptdom-core-0.1.0.jar io.github.fupacat.ast.<AstClassName>
   ```
2. **Create `<RuleName>Check.java`** in `src/main/java/io/github/fupacat/sonar/tsql/checks/`:
   - Extend `TSqlCheck`.
   - Override `getRuleKey()` and the relevant `visit(<AstNode> node)` methods (calling `super.visit(node)` so child nodes are traversed).
   - Call `reportIssue(node, "...")` when a violation is detected.
3. **Register the check**:
   - Add the class and instance to `CheckList.getChecks()` and `CheckList.instantiateChecks()`.
   - Register the rule metadata in `TSqlRulesDefinition.define(Context)`.
   - Activate the rule in `TSqlQualityProfile.define(Context)` if part of the built-in `"Sonar way"` profile.
4. **Write unit tests**:
   - Add `<RuleName>CheckTest.java` in `src/test/java/io/github/fupacat/sonar/tsql/checks/` using `CheckTestUtils.scan(check, sql)`.
   - Update rule count assertions in `TSqlRulesDefinitionTest` and `TSqlQualityProfileTest`.
   - Ensure test coverage on new code is $\ge 80\%$ (`mvn clean verify --batch-mode`).

## 3. MCSWE v3 Protocol

This repository uses the **MCSWE v3** traceability standard:
- Place `// TRACEABILITY_MAPPING: REQ-NNN` immediately above methods/tests in scope.
- When `mcswe-v3` is installed locally, run `mcswe-v3 verify` and `mcswe-v3 audit` after updating `context/SRS.md` and `context/ICD.md`.
