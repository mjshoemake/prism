package com.prism.api.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Plain JUnit: a data source gets the provider of its connection method, or a clear refusal. */
class MetricDataProvidersTests {

    private record FakeProvider(String connectionMethod) implements MetricDataProvider {
        @Override
        public List<Map<String, Object>> chartRows(DataSourceDefinition dataSource, MetricDefinition metric,
                                                   FocusLevelDefinition groupBy,
                                                   List<MetricQueryBuilder.ResolvedFilter> filters) {
            return List.of();
        }
    }

    @Test
    void picksTheProviderOfTheConnectionMethod() {
        var demo = new FakeProvider("prism_database");
        var api = new FakeProvider(DataSourceDefinition.API);
        var providers = new MetricDataProviders(List.of(demo, api));
        assertSame(demo, providers.forDataSource(new DataSourceDefinition(1, "Demo data", "prism_database")));
        assertSame(api, providers.forDataSource(new DataSourceDefinition(2, "Client API", DataSourceDefinition.API)));
    }

    @Test
    void unreachableDataSourceIsRefusedClearly() {
        var providers = new MetricDataProviders(List.of(new FakeProvider("prism_database")));
        var error = assertThrows(IllegalArgumentException.class,
                () -> providers.forDataSource(new DataSourceDefinition(3, "Acme warehouse", "private_link")));
        assertTrue(error.getMessage().contains("'Acme warehouse'") && error.getMessage().contains("private_link"),
                error.getMessage());
    }

    @Test
    void twoProvidersForOneMethodAreAConfigurationError() {
        assertThrows(IllegalStateException.class, () -> new MetricDataProviders(
                List.of(new FakeProvider("prism_database"), new FakeProvider("prism_database"))));
        assertEquals("prism_database", PrismDatabaseDataProvider.CONNECTION_METHOD);
    }
}
