package com.prism.api.metrics;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * One row of "prism"."Facts" with its linked dimensions ("prism"."Facts_Dimensions_Assoc", SQL/013), as cached by
 * {@link MetricsMetadataCache}. Metric expressions use the fact's alias; the query builder adds a JOIN for each linked
 * dimension a query needs.
 *
 * @param dataSourcePk the data source the fact lives in (picks the {@link MetricDataProvider})
 * @param joins        linked dimensions in a fixed order (by dimension_pk), so generated SQL is stable
 */
public record FactDefinition(int factPk, int dataSourcePk, TableDefinition table, List<Join> joins) {

    /** {@code fact.factColumn = dimension.dimensionColumn}; both columns are plain identifiers. */
    public record Join(int dimensionPk, TableDefinition dimension, String factColumn, String dimensionColumn) {

        public Join {
            if (!MetricQueryBuilder.isIdentifier(factColumn) || !MetricQueryBuilder.isIdentifier(dimensionColumn)) {
                throw new IllegalArgumentException("Invalid join column: " + factColumn + " = " + dimensionColumn);
            }
        }
    }

    public FactDefinition {
        joins = List.copyOf(joins);
        var aliases = new HashSet<String>();
        aliases.add(table.alias());
        for (var join : joins) {
            if (!aliases.add(join.dimension().alias())) {
                throw new IllegalArgumentException("Alias '" + join.dimension().alias() + "' is used twice in fact "
                        + factPk + " and its dimensions");
            }
        }
    }

    /** The link to a dimension, if this fact has one. */
    public Optional<Join> join(int dimensionPk) {
        return joins.stream().filter(j -> j.dimensionPk() == dimensionPk).findFirst();
    }
}
