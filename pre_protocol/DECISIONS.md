# Decisions

## ADR-001
- Status: [RESOLVED]
- Decision: Target Java 17 LTS as baseline language level with Maven build targeting SonarQube plugin API 10.11+.

## ADR-002
- Status: [RESOLVED]
- Decision: Leverage tsql-scriptdom-java (io.github.fupacat:tsql-scriptdom-core) for AST parsing, token streams, and AST visitor hierarchy.

## ADR-003
- Status: [RESOLVED]
- Decision: Implement custom rules as subclasses of TSqlCheck extending TSqlConcreteFragmentVisitor for recursive AST inspection.

## ADR-004
- Status: [RESOLVED]
- Decision: Register TSqlLanguage, TSqlSensor, TSqlRulesDefinition, and TSqlQualityProfile via TSqlPlugin entry point.
