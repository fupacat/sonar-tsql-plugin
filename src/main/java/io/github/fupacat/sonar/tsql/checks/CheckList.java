package io.github.fupacat.sonar.tsql.checks;

import java.util.List;

/**
 * Registry of all available T-SQL rule checks.
 */
public final class CheckList {

    public static final String SYNTAX_ERROR_RULE_KEY = "SyntaxError";

    private static final List<Class<? extends TSqlCheck>> CHECKS = List.of(
            AvoidSelectAllCheck.class,
            AvoidLeadingWildcardInLikeCheck.class,
            AvoidNoLockHintCheck.class,
            AvoidDeprecatedTypesCheck.class,
            DeleteAndUpdateWithoutWhereCheck.class,
            NotInSubqueryCheck.class,
            NullComparisonCheck.class,
            TopWithoutOrderByCheck.class,
            CaseInputInvariantCheck.class,
            DynamicSqlExecutionCheck.class,
            WeakHashCheck.class,
            HardCodedCredentialsCheck.class,
            PrimaryKeyRequiredCheck.class,
            InsertColumnsExplicitCheck.class,
            ExplicitLengthForVariableTypesCheck.class,
            OrderByColumnNamesCheck.class,
            ExistsSelectOneCheck.class,
            AlterTableCheckEnforcementCheck.class,
            ColumnReferenceDepthCheck.class,
            SargableWhereCheck.class,
            UseScopeIdentityCheck.class,
            AvoidVariableLengthDataTypeCheck.class,
            AvoidDeprecatedJoinSyntaxCheck.class,
            AvoidSpecialCharactersInObjectNamesCheck.class,
            AvoidReservedWordsInObjectNamesCheck.class,
            StoredProcedureOutputParameterCheck.class,
            DeterministicFunctionInWhereCheck.class,
            AvoidSpPrefixProcedureNameCheck.class,
            SetNoCountCheck.class,
            SetAnsiWarningsAndArithabortCheck.class,
            SetAnsiNullsCheck.class,
            AvoidDeprecatedSystemTablesCheck.class,
            FetchIntoCursorCountCheck.class,
            CaseSubqueryCheck.class,
            TriggerResultSetCheck.class,
            AvoidLikeWithoutWildcardCheck.class,
            AvoidNonStandardComparisonOperatorsCheck.class,
            UnusedLocalVariableCheck.class,
            UnusedParameterCheck.class,
            UnusedLabelCheck.class,
            UnreachableCodeCheck.class,
            SingleIterationLoopCheck.class,
            SelfAssignmentCheck.class,
            CatchClauseShouldDoMoreThanRethrowCheck.class,
            RedundantJumpCheck.class,
            S103Check.class,
            S104Check.class,
            S105Check.class,
            S122Check.class,
            S125Check.class,
            S1134Check.class,
            S1135Check.class,
            NoSonarCheck.class,
            S1451Check.class,
            S3972Check.class,
            S3973Check.class,
            S138Check.class,
            S1151Check.class
    );

    private CheckList() {
    }

    // TRACEABILITY_MAPPING: REQ-006
    public static List<Class<? extends TSqlCheck>> getChecks() {
        return CHECKS;
    }

    // TRACEABILITY_MAPPING: REQ-006
    public static List<TSqlCheck> instantiateChecks() {
        return List.of(
                new AvoidSelectAllCheck(),
                new AvoidLeadingWildcardInLikeCheck(),
                new AvoidNoLockHintCheck(),
                new AvoidDeprecatedTypesCheck(),
                new DeleteAndUpdateWithoutWhereCheck(),
                new NotInSubqueryCheck(),
                new NullComparisonCheck(),
                new TopWithoutOrderByCheck(),
                new CaseInputInvariantCheck(),
                new DynamicSqlExecutionCheck(),
                new WeakHashCheck(),
                new HardCodedCredentialsCheck(),
                new PrimaryKeyRequiredCheck(),
                new InsertColumnsExplicitCheck(),
                new ExplicitLengthForVariableTypesCheck(),
                new OrderByColumnNamesCheck(),
                new ExistsSelectOneCheck(),
                new AlterTableCheckEnforcementCheck(),
                new ColumnReferenceDepthCheck(),
                new SargableWhereCheck(),
                new UseScopeIdentityCheck(),
                new AvoidVariableLengthDataTypeCheck(),
                new AvoidDeprecatedJoinSyntaxCheck(),
                new AvoidSpecialCharactersInObjectNamesCheck(),
                new AvoidReservedWordsInObjectNamesCheck(),
                new StoredProcedureOutputParameterCheck(),
                new DeterministicFunctionInWhereCheck(),
                new AvoidSpPrefixProcedureNameCheck(),
                new SetNoCountCheck(),
                new SetAnsiWarningsAndArithabortCheck(),
                new SetAnsiNullsCheck(),
                new AvoidDeprecatedSystemTablesCheck(),
                new FetchIntoCursorCountCheck(),
                new CaseSubqueryCheck(),
                new TriggerResultSetCheck(),
                new AvoidLikeWithoutWildcardCheck(),
                new AvoidNonStandardComparisonOperatorsCheck(),
                new UnusedLocalVariableCheck(),
                new UnusedParameterCheck(),
                new UnusedLabelCheck(),
                new UnreachableCodeCheck(),
                new SingleIterationLoopCheck(),
                new SelfAssignmentCheck(),
                new CatchClauseShouldDoMoreThanRethrowCheck(),
                new RedundantJumpCheck(),
                new S103Check(),
                new S104Check(),
                new S105Check(),
                new S122Check(),
                new S125Check(),
                new S1134Check(),
                new S1135Check(),
                new NoSonarCheck(),
                new S1451Check(),
                new S3972Check(),
                new S3973Check(),
                new S138Check(),
                new S1151Check()
        );
    }
}
