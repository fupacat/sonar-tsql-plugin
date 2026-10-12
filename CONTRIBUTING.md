# Contributing to sonar-tsql-plugin

Thank you for your interest in contributing to `sonar-tsql-plugin`!

`sonar-tsql-plugin` is a native SonarQube static code analyzer and linter for Microsoft Transact-SQL (T-SQL), powered by [`tsql-scriptdom-java`](https://github.com/fupacat/tsql-scriptdom-java).

---

## 🛠️ Prerequisites

To build and contribute to this repository, you will need:
* **JDK 17+** (JDK 17 or JDK 21 recommended)
* **Apache Maven 3.8+**

---

## 🏗️ Building from Source

To compile the plugin and run the full unit test suite:

```bash
mvn clean test
```

To package the standalone SonarQube plugin distribution JAR:

```bash
mvn clean package
```

The resulting plugin artifact will be located at:
`target/sonar-tsql-plugin-<version>.jar`

---

## ➕ Adding a New T-SQL Rule

1. **Implement `TSqlCheck`**:
   Create a new class in `src/main/java/io/github/fupacat/sonar/tsql/checks/` extending `TSqlCheck` (which extends `TSqlConcreteFragmentVisitor`).
   * Override the appropriate `explicitVisit(...)` method(s) for the target AST fragment(s).
   * Call `reportIssue(fragment, message)` when a violation is detected.
   * Annotate methods with `// TRACEABILITY_MAPPING: REQ-NNN` if governed by MCSWE v3.
2. **Register in `CheckList`**:
   Add the rule class to `CheckList.getChecks()` and `CheckList.instantiateChecks()`.
3. **Define Metadata in `TSqlRulesDefinition`**:
   Register the rule's name, HTML/Markdown description, severity, rule type, debt remediation cost, and tags.
4. **Write Unit Tests**:
   Create a corresponding test class in `src/test/java/io/github/fupacat/sonar/tsql/checks/` using `CheckTestUtils.scan(check, sql)` to verify both positive (violation) and negative (compliant) T-SQL scripts.

---

## 🧪 Testing & Quality Guidelines

* **Unit Tests**: Every rule and sensor behavior must have dedicated JUnit 5 and AssertJ unit tests.
* **Visitor Traversal**: Always call `super.explicitVisit(node)` if nested AST constructs inside the node also need to be inspected.
* **No External Native Bridges**: Keep the plugin self-contained on the JVM using `tsql-scriptdom-core`.

---

## 📄 Licensing

By contributing to `sonar-tsql-plugin`, you agree that your contributions will be licensed under the project's [MIT License](LICENSE).
