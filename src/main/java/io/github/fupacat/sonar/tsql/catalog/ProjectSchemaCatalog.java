package io.github.fupacat.sonar.tsql.catalog;

import io.github.fupacat.ParseResult;
import io.github.fupacat.TSqlParser;
import io.github.fupacat.ast.ColumnDefinition;
import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.ast.ConstraintDefinition;
import io.github.fupacat.ast.CreateIndexStatement;
import io.github.fupacat.ast.CreateTableStatement;
import io.github.fupacat.ast.DataTypeReference;
import io.github.fupacat.ast.Identifier;
import io.github.fupacat.ast.IndexDefinition;
import io.github.fupacat.ast.Literal;
import io.github.fupacat.ast.MultiPartIdentifier;
import io.github.fupacat.ast.NullableConstraintDefinition;
import io.github.fupacat.ast.ParameterizedDataTypeReference;
import io.github.fupacat.ast.SchemaObjectName;
import io.github.fupacat.ast.SqlDataTypeReference;
import io.github.fupacat.ast.TSqlConcreteFragmentVisitor;
import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.TSqlScript;
import org.sonar.api.batch.fs.InputFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProjectSchemaCatalog {

    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("(?is)create\\s+table\\s+(?:\\[?(?:[^\\s\\.\\[\\]]+)\\]?\\.)?\\[?([A-Za-z_][A-Za-z0-9_#$]*)\\]?\\s*\\((.*)\\)");
    private static final Pattern CREATE_INDEX_PATTERN = Pattern.compile("(?is)create\\s+(?:unique\\s+)?index\\s+(?:\\[?[^\\s\\.\\[\\]]+\\]?\\.)?\\[?([A-Za-z_][A-Za-z0-9_#$]*)\\]?\\s+on\\s+(?:\\[?[^\\s\\.\\[\\]]+\\]?\\.)?\\[?([A-Za-z_][A-Za-z0-9_#$]*)\\]?\\s*\\((.*)\\)");
    private final Map<String, TableSchema> tables = new LinkedHashMap<>();
    private final Map<String, Set<String>> indexedColumnsByTable = new HashMap<>();

    public static ProjectSchemaCatalog empty() {
        return new ProjectSchemaCatalog();
    }

    public static ProjectSchemaCatalog fromInputFiles(Iterable<InputFile> inputFiles) {
        ProjectSchemaCatalog catalog = new ProjectSchemaCatalog();
        for (InputFile inputFile : inputFiles) {
            if (inputFile == null) {
                continue;
            }
            String content;
            try {
                content = inputFile.contents();
            } catch (IOException ignored) {
                continue;
            }
            catalog.indexSql(content);
            ParseResult<TSqlScript> result = new TSqlParser().parse(content);
            if (result != null && result.fragment() != null) {
                catalog.indexScript(result.fragment());
            }
        }
        return catalog;
    }

    public void indexSql(String sqlText) {
        if (sqlText == null || sqlText.isBlank()) {
            return;
        }
        indexSqlText(sqlText);
    }

    public void indexScript(TSqlFragment root) {
        if (root == null) {
            return;
        }
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
        return Optional.ofNullable(tables.get(normalized));
    }

    public Optional<ColumnInfo> resolveColumn(ColumnReferenceExpression expression) {
        if (expression == null || expression.MultiPartIdentifier == null) {
            return Optional.empty();
        }
        MultiPartIdentifier multi = expression.MultiPartIdentifier;
        List<String> identifiers = new ArrayList<>();
        if (multi.Identifiers != null) {
            for (Identifier identifier : multi.Identifiers) {
                if (identifier != null && identifier.Value != null && !identifier.Value.isBlank()) {
                    identifiers.add(identifier.Value);
                }
            }
        }
        if (identifiers.isEmpty()) {
            return Optional.empty();
        }
        String columnName = identifiers.get(identifiers.size() - 1);
        String tableName = identifiers.size() > 1 ? identifiers.get(identifiers.size() - 2) : null;

        if (tableName != null) {
            Optional<TableSchema> table = getTable(tableName);
            if (table.isPresent()) {
                return table.get().getColumn(columnName);
            }
        }

        for (TableSchema table : tables.values()) {
            Optional<ColumnInfo> column = table.getColumn(columnName);
            if (column.isPresent()) {
                return column;
            }
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
        String columnName = lastIdentifier(expression.MultiPartIdentifier);
        String tableName = tableNameFrom(expression.MultiPartIdentifier);
        if (tableName != null) {
            Set<String> columns = indexedColumnsByTable.get(normalizeTableName(tableName));
            return columns != null && columns.contains(normalizeColumnName(columnName));
        }
        for (Map.Entry<String, Set<String>> entry : indexedColumnsByTable.entrySet()) {
            if (entry.getValue().contains(normalizeColumnName(columnName))) {
                return true;
            }
        }
        return false;
    }

    public Optional<TypeInfo> typeOf(ColumnReferenceExpression expression) {
        return resolveColumn(expression).map(ColumnInfo::toTypeInfo);
    }

    private void registerTable(String tableName, TableSchema table) {
        if (tableName == null || tableName.isBlank()) {
            return;
        }
        String normalized = normalizeTableName(tableName);
        TableSchema existing = tables.get(normalized);
        if (existing == null) {
            tables.put(normalized, table);
            return;
        }
        for (Map.Entry<String, ColumnInfo> entry : table.columns.entrySet()) {
            existing.columns.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private void indexSqlText(String sqlText) {
        if (sqlText == null || sqlText.isBlank()) {
            return;
        }
        Matcher createTableMatcher = CREATE_TABLE_PATTERN.matcher(sqlText);
        while (createTableMatcher.find()) {
            String tableName = createTableMatcher.group(1);
            String columnsText = createTableMatcher.group(2);
            if (tableName == null || tableName.isBlank() || columnsText == null || columnsText.isBlank()) {
                continue;
            }
            TableSchema table = new TableSchema(normalizeTableName(tableName));
            List<String> columnEntries = splitTopLevel(columnsText, ',');
            for (String columnEntry : columnEntries) {
                if (columnEntry == null || columnEntry.isBlank()) {
                    continue;
                }
                Matcher columnMatcher = Pattern.compile("(?is)^\\s*(?:\\[?([A-Za-z_][A-Za-z0-9_#$]*)\\]?|([A-Za-z_][A-Za-z0-9_#$]*))\\s+(.+)$").matcher(columnEntry);
                if (!columnMatcher.matches()) {
                    continue;
                }
                String columnName = firstNonBlank(columnMatcher.group(1), columnMatcher.group(2));
                String remainder = columnMatcher.group(3);
                String sqlType = extractSqlTypeFromText(remainder);
                Integer length = extractLengthFromText(remainder);
                boolean nullable = isNullableText(remainder);
                table.addColumn(new ColumnInfo(columnName, nullable, sqlType, length));
            }
            registerTable(table.tableName, table);
        }

        Matcher createIndexMatcher = CREATE_INDEX_PATTERN.matcher(sqlText);
        while (createIndexMatcher.find()) {
            String tableName = createIndexMatcher.group(2);
            String columnsText = createIndexMatcher.group(3);
            if (tableName == null || tableName.isBlank() || columnsText == null || columnsText.isBlank()) {
                continue;
            }
            List<String> columnNames = new ArrayList<>();
            for (String columnEntry : splitTopLevel(columnsText, ',')) {
                String trimmed = columnEntry.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                Matcher columnMatcher = Pattern.compile("(?is)^\\s*(?:\\[?([A-Za-z_][A-Za-z0-9_#$]*)\\]?|([A-Za-z_][A-Za-z0-9_#$]*))(?:\\s+(?:asc|desc))?\\s*$").matcher(trimmed);
                if (columnMatcher.matches()) {
                    columnNames.add(firstNonBlank(columnMatcher.group(1), columnMatcher.group(2)));
                }
            }
            registerIndex(tableName, columnNames);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static List<String> splitTopLevel(String input, char delimiter) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenthesisDepth = 0;
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == '(') {
                parenthesisDepth++;
            } else if (ch == ')') {
                parenthesisDepth = Math.max(0, parenthesisDepth - 1);
            }
            if (ch == delimiter && parenthesisDepth == 0) {
                chunks.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private static String extractSqlTypeFromText(String text) {
        if (text == null || text.isBlank()) {
            return "unknown";
        }
        String remainder = text.trim();
        String[] tokens = remainder.split("\\s+");
        if (tokens.length < 2) {
            return "unknown";
        }
        return tokens[0].replace("[", "").replace("]", "").toUpperCase(Locale.ROOT);
    }

    private static Integer extractLengthFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?is)(?:varchar|char|nvarchar|nchar|binary|varbinary|decimal|numeric)\\s*\\((\\d+)\\)").matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        Matcher decimalMatcher = Pattern.compile("(?is)(?:decimal|numeric)\\s*\\((\\d+),\\s*(\\d+)\\)").matcher(text);
        if (decimalMatcher.find()) {
            return Integer.parseInt(decimalMatcher.group(1));
        }
        return null;
    }

    private static boolean isNullableText(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String normalized = text.toUpperCase(Locale.ROOT);
        if (normalized.contains(" NOT NULL")) {
            return false;
        }
        if (normalized.contains(" NULL")) {
            return true;
        }
        return true;
    }

    private void registerIndex(String tableName, List<String> columns) {
        if (tableName == null || tableName.isBlank() || columns == null || columns.isEmpty()) {
            return;
        }
        String normalized = normalizeTableName(tableName);
        indexedColumnsByTable.computeIfAbsent(normalized, key -> new HashSet<>())
                .addAll(columns.stream().map(this::normalizeColumnName).toList());
    }

    public String normalizeTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return "";
        }
        String normalized = tableName.trim();
        if (normalized.contains(".")) {
            String[] parts = normalized.split("\\.");
            normalized = parts[parts.length - 1];
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    public String normalizeColumnName(String columnName) {
        if (columnName == null || columnName.isBlank()) {
            return "";
        }
        return columnName.trim().replace("[", "").replace("]", "").toLowerCase(Locale.ROOT);
    }

    private String tableNameFrom(MultiPartIdentifier multiPartIdentifier) {
        if (multiPartIdentifier == null || multiPartIdentifier.Identifiers == null || multiPartIdentifier.Identifiers.isEmpty()) {
            return null;
        }
        if (multiPartIdentifier.Identifiers.size() > 1) {
            return multiPartIdentifier.Identifiers.get(multiPartIdentifier.Identifiers.size() - 2).Value;
        }
        return null;
    }

    private String lastIdentifier(MultiPartIdentifier multiPartIdentifier) {
        if (multiPartIdentifier == null || multiPartIdentifier.Identifiers == null || multiPartIdentifier.Identifiers.isEmpty()) {
            return "";
        }
        Identifier identifier = multiPartIdentifier.Identifiers.get(multiPartIdentifier.Identifiers.size() - 1);
        return identifier != null ? identifier.Value : "";
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

    public static final class ColumnInfo {
        private final String name;
        private final boolean nullable;
        private final String sqlType;
        private final Integer length;

        public ColumnInfo(String name, boolean nullable, String sqlType, Integer length) {
            this.name = name;
            this.sqlType = sqlType;
            this.length = length;
            this.nullable = nullable;
        }

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

    public static final class TypeInfo {
        private final String typeName;
        private final Integer length;

        public TypeInfo(String typeName, Integer length) {
            this.typeName = typeName;
            this.length = length;
        }

        public String getTypeName() {
            return typeName;
        }

        public Integer getLength() {
            return length;
        }

        public static TypeInfo fromSqlType(String sqlType, Integer length) {
            return new TypeInfo(sqlType == null ? "unknown" : sqlType.toLowerCase(Locale.ROOT), length);
        }

        public boolean isNarrowerThan(TypeInfo other) {
            if (other == null) {
                return false;
            }
            int thisSize = effectiveLength();
            int otherSize = other.effectiveLength();
            if (thisSize <= 0 || otherSize <= 0) {
                return false;
            }
            return thisSize < otherSize;
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

    private final class CatalogIndexer extends TSqlConcreteFragmentVisitor {
        @Override
        public void visit(CreateTableStatement node) {
            if (node == null || node.SchemaObjectName == null) {
                super.visit(node);
                return;
            }
            TableSchema table = new TableSchema(normalizeTableName(schemaName(node.SchemaObjectName)));
            if (node.Definition != null) {
                if (node.Definition.ColumnDefinitions != null) {
                    for (ColumnDefinition columnDefinition : node.Definition.ColumnDefinitions) {
                        if (columnDefinition == null) {
                            continue;
                        }
                        String columnName = columnDefinition.ColumnIdentifier != null && columnDefinition.ColumnIdentifier.Value != null
                                ? columnDefinition.ColumnIdentifier.Value
                                : "";
                        boolean nullable = true;
                        if (columnDefinition.Constraints != null) {
                            for (ConstraintDefinition constraint : columnDefinition.Constraints) {
                                if (constraint instanceof NullableConstraintDefinition nullableConstraint) {
                                    nullable = nullableConstraint.Nullable;
                                }
                            }
                        }
                        String sqlType = extractSqlType(columnDefinition.DataType);
                        Integer length = extractLength(columnDefinition.DataType);
                        table.addColumn(new ColumnInfo(columnName, nullable, sqlType, length));
                    }
                }
                if (node.Definition.Indexes != null) {
                    for (IndexDefinition indexDefinition : node.Definition.Indexes) {
                        if (indexDefinition == null || indexDefinition.Columns == null || indexDefinition.Columns.isEmpty()) {
                            continue;
                        }
                        List<String> columns = new ArrayList<>();
                        for (var columnWithSortOrder : indexDefinition.Columns) {
                            if (columnWithSortOrder != null && columnWithSortOrder.Column != null
                                    && columnWithSortOrder.Column.MultiPartIdentifier != null) {
                                columns.add(lastIdentifier(columnWithSortOrder.Column.MultiPartIdentifier));
                            }
                        }
                        registerIndex(table.tableName, columns);
                    }
                }
            }
            registerTable(table.tableName, table);
            super.visit(node);
        }

        @Override
        public void visit(CreateIndexStatement node) {
            if (node != null && node.OnName != null && node.Columns != null) {
                String tableName = schemaName(node.OnName);
                List<String> columns = new ArrayList<>();
                for (var columnWithSortOrder : node.Columns) {
                    if (columnWithSortOrder != null && columnWithSortOrder.Column != null
                            && columnWithSortOrder.Column.MultiPartIdentifier != null) {
                        columns.add(lastIdentifier(columnWithSortOrder.Column.MultiPartIdentifier));
                    }
                }
                registerIndex(tableName, columns);
            }
            super.visit(node);
        }

        private String schemaName(SchemaObjectName name) {
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

        private String extractSqlType(DataTypeReference dataType) {
            if (dataType == null) {
                return "unknown";
            }
            if (dataType instanceof SqlDataTypeReference sqlDataTypeReference) {
                return sqlDataTypeReference.SqlDataTypeOption != null
                        ? sqlDataTypeReference.SqlDataTypeOption.name()
                        : "unknown";
            }
            if (dataType instanceof ParameterizedDataTypeReference parameterizedDataTypeReference) {
                if (parameterizedDataTypeReference.Name != null && parameterizedDataTypeReference.Name.BaseIdentifier != null) {
                    return parameterizedDataTypeReference.Name.BaseIdentifier.Value;
                }
            }
            return dataType.getClass().getSimpleName();
        }

        private Integer extractLength(DataTypeReference dataType) {
            if (!(dataType instanceof ParameterizedDataTypeReference parameterizedDataTypeReference) || parameterizedDataTypeReference.Parameters == null) {
                return null;
            }
            for (Literal literal : parameterizedDataTypeReference.Parameters) {
                if (literal != null && literal.Value != null) {
                    try {
                        return Integer.parseInt(literal.Value.replace("'", "").replace("N", ""));
                    } catch (NumberFormatException ignored) {
                        // ignore non-numeric parameters such as precision/scale pairs
                    }
                }
            }
            return null;
        }
    }
}
