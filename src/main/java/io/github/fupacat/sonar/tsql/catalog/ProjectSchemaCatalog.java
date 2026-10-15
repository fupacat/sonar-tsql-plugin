package io.github.fupacat.sonar.tsql.catalog;

import io.github.fupacat.ParseResult;
import io.github.fupacat.TSqlParser;
import io.github.fupacat.ast.ColumnDefinition;
import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.ast.ConstraintDefinition;
import io.github.fupacat.ast.CreateIndexStatement;
import io.github.fupacat.ast.CreateTableStatement;
import io.github.fupacat.ast.DataTypeReference;
import io.github.fupacat.ast.FromClause;
import io.github.fupacat.ast.Identifier;
import io.github.fupacat.ast.IndexDefinition;
import io.github.fupacat.ast.JoinTableReference;
import io.github.fupacat.ast.Literal;
import io.github.fupacat.ast.MultiPartIdentifier;
import io.github.fupacat.ast.NamedTableReference;
import io.github.fupacat.ast.NullableConstraintDefinition;
import io.github.fupacat.ast.ParameterizedDataTypeReference;
import io.github.fupacat.ast.QuerySpecification;
import io.github.fupacat.ast.SchemaObjectName;
import io.github.fupacat.ast.SqlDataTypeReference;
import io.github.fupacat.ast.TableReference;
import io.github.fupacat.ast.TSqlConcreteFragmentVisitor;
import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.TSqlScript;
import io.github.fupacat.ast.UniqueConstraintDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ProjectSchemaCatalog {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectSchemaCatalog.class);
    private static final String UNKNOWN_TYPE = "unknown";
    private static final char OPEN_BRACKET = '[';
    private static final char CLOSE_BRACKET = ']';

    private final Map<String, TableSchema> tables = new LinkedHashMap<>();
    private final Map<String, List<String>> shortTableNames = new HashMap<>();
    private final Map<String, Set<String>> indexedColumnsByTable = new HashMap<>();
    private final Map<ColumnReferenceExpression, QueryScope> expressionScopeByColumn = new IdentityHashMap<>();
    private final ArrayDeque<QueryScope> queryScopeStack = new ArrayDeque<>();
    private final List<QueryScope> queryScopes = new ArrayList<>();

    public static ProjectSchemaCatalog empty() {
        return new ProjectSchemaCatalog();
    }

    public static ProjectSchemaCatalog fromInputFiles(Iterable<InputFile> inputFiles) {
        if (inputFiles == null) {
            return empty();
        }
        List<ParseResult<TSqlScript>> parsedResults = new ArrayList<>();
        for (InputFile inputFile : inputFiles) {
            if (inputFile == null) {
                continue;
            }
            try {
                String content = inputFile.contents();
                parsedResults.add(TSqlParser.parse(content));
            } catch (IOException ignored) {
                // ignore unreadable file
            } catch (RuntimeException e) {
                LOG.warn("Failed to parse schema catalog input: {}", inputFile.uri(), e);
            }
        }
        return fromParsedResults(parsedResults);
    }

    public static ProjectSchemaCatalog fromParsedResults(Iterable<ParseResult<TSqlScript>> parsedResults) {
        ProjectSchemaCatalog catalog = new ProjectSchemaCatalog();
        if (parsedResults == null) {
            return catalog;
        }
        for (ParseResult<TSqlScript> result : parsedResults) {
            if (result == null || result.fragment() == null) {
                continue;
            }
            try {
                catalog.indexScript(result.fragment());
            } catch (Exception e) {
                LOG.warn("Failed to index script while building project schema catalog", e);
            }
        }
        return catalog;
    }

    public void indexScript(TSqlFragment root) {
        if (root == null) {
            return;
        }
        queryScopeStack.clear();
        queryScopes.clear();
        expressionScopeByColumn.clear();
        root.accept(new CatalogIndexer());
    }

    public boolean hasTable(String tableName) {
        return getTable(tableName).isPresent();
    }

    public Optional<TableSchema> getTable(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalizeTableName(tableName);
        TableSchema exact = tables.get(normalized);
        if (exact != null) {
            return Optional.of(exact);
        }
        if (normalized.contains(".")) {
            return Optional.empty();
        }
        String shortName = shortTableName(normalized);
        List<String> matches = shortTableNames.get(shortName);
        if (matches == null || matches.isEmpty()) {
            return Optional.empty();
        }
        if (matches.size() == 1) {
            return Optional.ofNullable(tables.get(matches.get(0)));
        }
        return Optional.empty();
    }

    public Optional<ColumnInfo> resolveColumn(ColumnReferenceExpression expression) {
        if (expression == null || expression.MultiPartIdentifier == null) {
            return Optional.empty();
        }

        List<String> identifiers = extractIdentifiers(expression.MultiPartIdentifier);
        if (identifiers.isEmpty()) {
            return Optional.empty();
        }

        String columnName = identifiers.get(identifiers.size() - 1);
        QueryScope scope = findEnclosingScope(expression);
        if (identifiers.size() > 1) {
            String qualifier = qualifierFor(identifiers);
            TableSchema resolved = resolveTableReference(qualifier, scope);
            if (resolved != null) {
                return resolved.getColumn(columnName);
            }
            return Optional.empty();
        }

        List<TableSchema> matches = (scope == null) ? findUnqualifiedColumnMatches(columnName) : findColumnMatchesInScope(scope, columnName);
        if (matches.size() == 1) {
            return matches.get(0).getColumn(columnName);
        }
        return Optional.empty();
    }

    public boolean isNullable(ColumnReferenceExpression expression) {
        return resolveColumn(expression).map(ColumnInfo::isNullable).orElse(false);
    }

    public boolean isIndexed(ColumnReferenceExpression expression) {
        if (expression == null || expression.MultiPartIdentifier == null) {
            return false;
        }

        List<String> identifiers = extractIdentifiers(expression.MultiPartIdentifier);
        if (identifiers.isEmpty()) {
            return false;
        }

        String columnName = identifiers.get(identifiers.size() - 1);
        QueryScope scope = findEnclosingScope(expression);
        if (identifiers.size() > 1) {
            String qualifier = qualifierFor(identifiers);
            TableSchema table = resolveTableReference(qualifier, scope);
            if (table == null) {
                return false;
            }
            return hasIndexedColumn(table.getTableName(), columnName);
        }

        List<TableSchema> matches = (scope == null) ? findUnqualifiedColumnMatches(columnName) : findColumnMatchesInScope(scope, columnName);
        if (matches.size() == 1) {
            return hasIndexedColumn(matches.get(0).getTableName(), columnName);
        }
        return false;
    }

    public Optional<TypeInfo> typeOf(ColumnReferenceExpression expression) {
        return resolveColumn(expression).map(ColumnInfo::toTypeInfo);
    }

    static String normalizeTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return "";
        }
        return stripDelimiters(tableName.trim()).toLowerCase(Locale.ROOT);
    }

    private static String normalizeColumnName(String columnName) {
        if (columnName == null || columnName.isBlank()) {
            return "";
        }
        return stripDelimiters(columnName.trim()).toLowerCase(Locale.ROOT);
    }

    private static String stripDelimiters(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch != OPEN_BRACKET && ch != CLOSE_BRACKET) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static String shortTableName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    private QueryScope findEnclosingScope(ColumnReferenceExpression expression) {
        if (expression == null) {
            return null;
        }
        QueryScope bound = expressionScopeByColumn.get(expression);
        if (bound != null) {
            return bound;
        }
        if (expression.firstTokenIndex < 0) {
            return null;
        }
        QueryScope best = null;
        for (QueryScope scope : queryScopes) {
            if (scope.startTokenIndex <= expression.firstTokenIndex && expression.firstTokenIndex <= scope.endTokenIndex) {
                if (best == null || (scope.startTokenIndex >= best.startTokenIndex && scope.endTokenIndex <= best.endTokenIndex)) {
                    best = scope;
                }
            }
        }
        return best;
    }

    private TableSchema resolveTableReference(String qualifier, QueryScope scope) {
        if (qualifier == null || qualifier.isBlank()) {
            return null;
        }
        String normalized = normalizeTableName(qualifier);
        if (scope != null && scope.aliasLookup.containsKey(normalized)) {
            String aliasTarget = scope.aliasLookup.get(normalized);
            return aliasTarget == null ? null : getTable(aliasTarget).orElse(null);
        }
        if (tables.containsKey(normalized)) {
            return tables.get(normalized);
        }
        if (normalized.contains(".")) {
            return null;
        }
        return getTable(qualifier).orElse(null);
    }

    private static String qualifierFor(List<String> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return "";
        }
        if (identifiers.size() == 1) {
            return identifiers.get(0);
        }
        StringBuilder qualifier = new StringBuilder();
        for (int i = 0; i < identifiers.size() - 1; i++) {
            if (i > 0) {
                qualifier.append('.');
            }
            qualifier.append(identifiers.get(i));
        }
        return qualifier.toString();
    }

    private List<TableSchema> findUnqualifiedColumnMatches(String columnName) {
        List<TableSchema> matches = new ArrayList<>();
        for (TableSchema table : tables.values()) {
            if (table.getColumn(columnName).isPresent()) {
                matches.add(table);
            }
        }
        return matches;
    }

    private List<TableSchema> findColumnMatchesInScope(QueryScope scope, String columnName) {
        if (scope == null || columnName == null || columnName.isBlank()) {
            return List.of();
        }
        List<TableSchema> matches = new ArrayList<>();
        for (TableSchema table : scope.tablesInScope) {
            if (table.getColumn(columnName).isPresent()) {
                matches.add(table);
            }
        }
        return matches;
    }

    private boolean hasIndexedColumn(String tableName, String columnName) {
        if (tableName == null || tableName.isBlank() || columnName == null || columnName.isBlank()) {
            return false;
        }
        Set<String> columns = indexedColumnsByTable.get(normalizeTableName(tableName));
        return columns != null && columns.contains(normalizeColumnName(columnName));
    }

    private List<String> extractIdentifiers(MultiPartIdentifier multi) {
        List<String> identifiers = new ArrayList<>();
        if (multi != null && multi.Identifiers != null) {
            for (Identifier identifier : multi.Identifiers) {
                if (identifier != null && identifier.Value != null && !identifier.Value.isBlank()) {
                    identifiers.add(identifier.Value);
                }
            }
        }
        return identifiers;
    }

    private static String schemaName(SchemaObjectName name) {
        if (name == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        if (name.DatabaseIdentifier != null && name.DatabaseIdentifier.Value != null) {
            builder.append(name.DatabaseIdentifier.Value).append('.');
        }
        if (name.SchemaIdentifier != null && name.SchemaIdentifier.Value != null) {
            builder.append(name.SchemaIdentifier.Value).append('.');
        }
        if (name.BaseIdentifier != null && name.BaseIdentifier.Value != null) {
            builder.append(name.BaseIdentifier.Value);
        }
        return builder.toString();
    }

    private void registerTable(String tableName, TableSchema table) {
        if (tableName == null || tableName.isBlank()) {
            return;
        }
        String normalized = normalizeTableName(tableName);
        if (normalized.isBlank()) {
            return;
        }
        TableSchema existing = tables.get(normalized);
        if (existing == null) {
            tables.put(normalized, table);
        } else {
            for (Map.Entry<String, ColumnInfo> entry : table.columns.entrySet()) {
                existing.columns.putIfAbsent(entry.getKey(), entry.getValue());
            }
            return;
        }
        shortTableNames.computeIfAbsent(shortTableName(normalized), key -> new ArrayList<>()).add(normalized);
    }

    private void registerIndex(String tableName, List<String> columns) {
        if (tableName == null || tableName.isBlank() || columns == null || columns.isEmpty()) {
            return;
        }
        String normalized = normalizeTableName(tableName);
        indexedColumnsByTable.computeIfAbsent(normalized, key -> new HashSet<>())
                .addAll(columns.stream().map(ProjectSchemaCatalog::normalizeColumnName).toList());
    }

    private void registerStandaloneIndex(CreateIndexStatement node) {
        if (node == null || node.OnName == null || node.Columns == null) {
            return;
        }
        registerIndex(schemaName(node.OnName), extractIndexColumns(node.Columns));
    }

    private List<String> extractIndexColumns(List<?> indexedColumns) {
        if (indexedColumns == null || indexedColumns.isEmpty()) {
            return List.of();
        }
        return indexedColumns.stream()
                .filter(entry -> entry instanceof io.github.fupacat.ast.ColumnWithSortOrder columnWithSortOrder
                        && columnWithSortOrder.Column != null
                        && columnWithSortOrder.Column.MultiPartIdentifier != null)
                .map(entry -> lastIdentifier(((io.github.fupacat.ast.ColumnWithSortOrder) entry).Column.MultiPartIdentifier))
                .toList();
    }

    private String lastIdentifier(MultiPartIdentifier multiPartIdentifier) {
        if (multiPartIdentifier == null || multiPartIdentifier.Identifiers == null || multiPartIdentifier.Identifiers.isEmpty()) {
            return "";
        }
        Identifier identifier = multiPartIdentifier.Identifiers.get(multiPartIdentifier.Identifiers.size() - 1);
        return identifier != null ? identifier.Value : "";
    }

    private QueryScope registerQueryScope(QuerySpecification specification) {
        if (specification == null || specification.FromClause == null || specification.FromClause.TableReferences == null
                || specification.FromClause.TableReferences.isEmpty()) {
            return null;
        }

        Map<String, String> aliasLookup = new HashMap<>();
        List<TableSchema> tablesInScope = new ArrayList<>();
        collectTableReferences(specification.FromClause, aliasLookup, tablesInScope);

        if (tablesInScope.isEmpty()) {
            return null;
        }

        QueryScope scope = new QueryScope(specification.firstTokenIndex, specification.lastTokenIndex, aliasLookup, tablesInScope);
        queryScopes.add(scope);
        return scope;
    }

    private void collectTableReferences(FromClause fromClause, Map<String, String> aliasLookup, List<TableSchema> tablesInScope) {
        if (fromClause == null || fromClause.TableReferences == null) {
            return;
        }
        for (TableReference tableReference : fromClause.TableReferences) {
            collectTableReference(tableReference, aliasLookup, tablesInScope);
        }
    }

    private void collectTableReference(TableReference tableReference, Map<String, String> aliasLookup, List<TableSchema> tablesInScope) {
        if (tableReference == null) {
            return;
        }
        if (tableReference instanceof NamedTableReference namedTableReference) {
            String tableName = schemaName(namedTableReference.SchemaObject);
            TableSchema table = getTable(tableName).orElse(null);
            if (table == null) {
                return;
            }
            tablesInScope.add(table);
            if (namedTableReference.Alias != null && namedTableReference.Alias.Value != null && !namedTableReference.Alias.Value.isBlank()) {
                aliasLookup.put(normalizeTableName(namedTableReference.Alias.Value), table.getTableName());
            }
            return;
        }
        if (tableReference instanceof JoinTableReference joinTableReference) {
            collectTableReference(joinTableReference.FirstTableReference, aliasLookup, tablesInScope);
            collectTableReference(joinTableReference.SecondTableReference, aliasLookup, tablesInScope);
        }
    }

    private final class CatalogIndexer extends TSqlConcreteFragmentVisitor {
        @Override
        public void visit(CreateTableStatement node) {
            if (node == null || node.SchemaObjectName == null) {
                super.visit(node);
                return;
            }
            String tableName = schemaName(node.SchemaObjectName);
            TableSchema table = new TableSchema(tableName);
            indexTableDefinition(table, node);
            registerTable(tableName, table);
            super.visit(node);
        }

        @Override
        public void visit(QuerySpecification node) {
            QueryScope scope = registerQueryScope(node);
            if (scope != null) {
                queryScopeStack.push(scope);
            }
            super.visit(node);
            if (scope != null) {
                queryScopeStack.pop();
            }
        }

        @Override
        public void visit(ColumnReferenceExpression node) {
            if (node != null && node.MultiPartIdentifier != null && node.MultiPartIdentifier.Identifiers != null && !node.MultiPartIdentifier.Identifiers.isEmpty()) {
                QueryScope active = queryScopeStack.peekFirst();
                if (active != null) {
                    expressionScopeByColumn.put(node, active);
                }
            }
            super.visit(node);
        }

        @Override
        public void visit(CreateIndexStatement node) {
            registerStandaloneIndex(node);
            super.visit(node);
        }

        private void indexTableDefinition(TableSchema table, CreateTableStatement node) {
            if (table == null || node == null || node.Definition == null) {
                return;
            }
            indexColumns(table, node.Definition.ColumnDefinitions);
            registerConstraintIndexes(table.tableName, node.Definition.TableConstraints);
            registerPrimaryKeyNullability(table, node.Definition.TableConstraints);
            registerInlineColumnIndexes(table.tableName, node.Definition.ColumnDefinitions);
            if (node.Definition.Indexes != null) {
                for (IndexDefinition indexDefinition : node.Definition.Indexes) {
                    registerIndex(table.tableName, extractIndexColumns(indexDefinition == null ? null : indexDefinition.Columns));
                }
            }
        }

        private void indexColumns(TableSchema table, List<ColumnDefinition> columnDefinitions) {
            if (table == null || columnDefinitions == null) {
                return;
            }
            for (ColumnDefinition columnDefinition : columnDefinitions) {
                if (columnDefinition == null) {
                    continue;
                }
                String columnName = columnDefinition.ColumnIdentifier != null && columnDefinition.ColumnIdentifier.Value != null
                        ? columnDefinition.ColumnIdentifier.Value
                        : "";
                boolean nullable = isNullable(columnDefinition.Constraints);
                String sqlType = extractSqlType(columnDefinition.DataType);
                Integer length = extractLength(columnDefinition.DataType);
                table.addColumn(new ColumnInfo(columnName, nullable, sqlType, length));
                if (columnDefinition.Index != null) {
                    registerIndex(table.tableName, extractIndexColumns(columnDefinition.Index.Columns));
                }
            }
        }

        private void registerConstraintIndexes(String tableName, List<ConstraintDefinition> constraints) {
            if (tableName == null || tableName.isBlank() || constraints == null) {
                return;
            }
            for (ConstraintDefinition constraint : constraints) {
                if (constraint instanceof UniqueConstraintDefinition uniqueConstraint) {
                    registerIndex(tableName, extractIndexColumns(uniqueConstraint.Columns));
                }
            }
        }

        private void registerPrimaryKeyNullability(TableSchema table, List<ConstraintDefinition> constraints) {
            if (table == null || constraints == null) {
                return;
            }
            for (ConstraintDefinition constraint : constraints) {
                if (!(constraint instanceof UniqueConstraintDefinition uniqueConstraint) || !uniqueConstraint.IsPrimaryKey || uniqueConstraint.Columns == null) {
                    continue;
                }
                for (String columnName : extractIndexColumns(uniqueConstraint.Columns)) {
                    table.getColumn(columnName).ifPresent(column -> table.addColumn(new ColumnInfo(column.name(), false, column.sqlType(), column.length())));
                }
            }
        }

        private void registerInlineColumnIndexes(String tableName, List<ColumnDefinition> columnDefinitions) {
            if (tableName == null || tableName.isBlank() || columnDefinitions == null) {
                return;
            }
            for (ColumnDefinition columnDefinition : columnDefinitions) {
                if (columnDefinition != null && columnDefinition.Constraints != null) {
                    registerColumnConstraintIndexes(tableName, columnDefinition);
                }
            }
        }

        private void registerColumnConstraintIndexes(String tableName, ColumnDefinition columnDefinition) {
            for (ConstraintDefinition constraint : columnDefinition.Constraints) {
                if (constraint instanceof UniqueConstraintDefinition uniqueConstraint) {
                    List<String> columnNames = extractConstraintColumnNames(columnDefinition, uniqueConstraint);
                    if (!columnNames.isEmpty()) {
                        registerIndex(tableName, columnNames);
                    }
                }
            }
        }

        private List<String> extractConstraintColumnNames(ColumnDefinition columnDefinition, UniqueConstraintDefinition uniqueConstraint) {
            if (uniqueConstraint.Columns != null && !uniqueConstraint.Columns.isEmpty()) {
                return extractIndexColumns(uniqueConstraint.Columns);
            }
            String columnName = columnDefinition.ColumnIdentifier != null ? columnDefinition.ColumnIdentifier.Value : null;
            return (columnName != null && !columnName.isBlank()) ? List.of(columnName) : List.of();
        }

        private boolean isNullable(List<ConstraintDefinition> constraints) {
            if (constraints == null) {
                return true;
            }
            for (ConstraintDefinition constraint : constraints) {
                if (constraint instanceof NullableConstraintDefinition nullableConstraint) {
                    return nullableConstraint.Nullable;
                }
                if (constraint instanceof UniqueConstraintDefinition uniqueConstraint && uniqueConstraint.IsPrimaryKey) {
                    return false;
                }
            }
            return true;
        }

        private String extractSqlType(DataTypeReference dataType) {
            if (dataType == null) {
                return UNKNOWN_TYPE;
            }
            if (dataType instanceof SqlDataTypeReference sqlDataTypeReference) {
                return sqlDataTypeReference.SqlDataTypeOption != null
                        ? sqlDataTypeReference.SqlDataTypeOption.name().toLowerCase(Locale.ROOT)
                        : UNKNOWN_TYPE;
            }
            if (dataType instanceof ParameterizedDataTypeReference parameterizedDataTypeReference
                    && parameterizedDataTypeReference.Name != null
                    && parameterizedDataTypeReference.Name.BaseIdentifier != null) {
                return parameterizedDataTypeReference.Name.BaseIdentifier.Value.toLowerCase(Locale.ROOT);
            }
            return dataType.getClass().getSimpleName();
        }

        private Integer extractLength(DataTypeReference dataType) {
            if (!(dataType instanceof ParameterizedDataTypeReference parameterizedDataTypeReference) || parameterizedDataTypeReference.Parameters == null) {
                return null;
            }
            for (Literal literal : parameterizedDataTypeReference.Parameters) {
                Integer parsed = parseLengthLiteral(literal);
                if (parsed != null) {
                    return parsed;
                }
            }
            return null;
        }

        private Integer parseLengthLiteral(Literal literal) {
            if (literal == null || literal.Value == null || literal.Value.isBlank()) {
                return null;
            }
            try {
                return Integer.parseInt(literal.Value.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }

    private record QueryScope(int startTokenIndex, int endTokenIndex, Map<String, String> aliasLookup, List<TableSchema> tablesInScope) {
    }

    public static final class TableSchema {
        private final String tableName;
        private final Map<String, ColumnInfo> columns = new LinkedHashMap<>();

        public TableSchema(String tableName) {
            this.tableName = tableName;
        }

        public String getTableName() {
            return tableName;
        }

        public Map<String, ColumnInfo> getColumns() {
            return Collections.unmodifiableMap(columns);
        }

        public Optional<ColumnInfo> getColumn(String columnName) {
            if (columnName == null || columnName.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(columns.get(columnName.toLowerCase(Locale.ROOT)));
        }

        public void addColumn(ColumnInfo columnInfo) {
            if (columnInfo == null || columnInfo.getName() == null || columnInfo.getName().isBlank()) {
                return;
            }
            columns.put(columnInfo.getName().toLowerCase(Locale.ROOT), columnInfo);
        }
    }

    public record ColumnInfo(String name, boolean nullable, String sqlType, Integer length) {
        public String getName() {
            return name;
        }

        public boolean isNullable() {
            return nullable;
        }

        public String getSqlType() {
            return sqlType;
        }

        public Integer getLength() {
            return length;
        }

        public TypeInfo toTypeInfo() {
            return TypeInfo.fromSqlType(sqlType, length);
        }
    }

    public record TypeInfo(String typeName, Integer length) {
        private enum TypeFamily {
            NUMERIC,
            CHARACTER,
            TEMPORAL,
            BINARY,
            OTHER
        }

        public String getTypeName() {
            return typeName;
        }

        public Integer getLength() {
            return length;
        }

        public static TypeInfo fromSqlType(String sqlType, Integer length) {
            return new TypeInfo(sqlType == null ? UNKNOWN_TYPE : sqlType.toLowerCase(Locale.ROOT), length);
        }

        public boolean isNarrowerThan(TypeInfo other) {
            if (other == null) {
                return false;
            }
            TypeFamily thisFamily = family();
            TypeFamily otherFamily = other.family();
            if (thisFamily == TypeFamily.OTHER || thisFamily != otherFamily) {
                return false;
            }
            int thisSize = effectiveLength();
            int otherSize = other.effectiveLength();
            return thisSize > 0 && otherSize > 0 && thisSize < otherSize;
        }

        private TypeFamily family() {
            String normalized = typeName == null ? "" : typeName.toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "tinyint", "smallint", "int", "integer", "bigint", "bit",
                        "smallmoney", "money", "real", "float", "decimal", "numeric" -> TypeFamily.NUMERIC;
                case "char", "nchar", "varchar", "nvarchar", "text", "ntext", "nvarcharmax", "varcharmax" -> TypeFamily.CHARACTER;
                case "date", "time", "datetime", "smalldatetime", "datetime2", "datetimeoffset" -> TypeFamily.TEMPORAL;
                case "binary", "varbinary", "image" -> TypeFamily.BINARY;
                default -> TypeFamily.OTHER;
            };
        }

        private int effectiveLength() {
            if (length != null && length > 0) {
                return length;
            }
            String normalized = typeName == null ? "" : typeName.toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "tinyint" -> 1;
                case "smallint" -> 2;
                case "int", "integer" -> 4;
                case "bigint" -> 8;
                case "bit" -> 1;
                case "smallmoney", "money" -> 4;
                case "real" -> 4;
                case "float" -> 8;
                case "date", "time" -> 3;
                case "datetime", "smalldatetime" -> 8;
                case "datetime2" -> 8;
                case "uniqueidentifier" -> 16;
                case "char", "nchar" -> 1;
                case "varchar", "nvarchar", "varbinary" -> 8000;
                case "binary", "nvarcharmax", "varcharmax", "ntext", "text" -> 8000;
                default -> length != null ? length : 0;
            };
        }
    }
}
