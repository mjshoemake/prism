package com.prism.api.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.prism.common.logging.LogCategory;

/**
 * Picks the {@link MetricDataProvider} for a data source by its connection method. Every provider bean registers
 * itself (Spring injects them all); a data source whose method has no provider on this server is refused with a clear
 * message instead of being queried some other way.
 */
@Component
public class MetricDataProviders {

    private static final Logger log = LogCategory.SERVICE.logger(MetricDataProviders.class);

    private final Map<String, MetricDataProvider> byMethod = new HashMap<>();

    public MetricDataProviders(List<MetricDataProvider> providers) {
        log.debug("Started");
        for (var provider : providers) {
            if (byMethod.put(provider.connectionMethod(), provider) != null) {
                throw new IllegalStateException("Two metric data providers for " + provider.connectionMethod());
            }
        }
    }

    /**
     * @throws IllegalArgumentException no provider for the data source's connection method (answered with 400)
     */
    public MetricDataProvider forDataSource(DataSourceDefinition dataSource) {
        log.debug("Started");
        var provider = byMethod.get(dataSource.connectionMethod());
        if (provider == null) {
            throw new IllegalArgumentException("Data source '" + dataSource.name() + "' uses "
                    + dataSource.connectionMethod() + ", which this Prism server can't connect to yet.");
        }
        return provider;
    }
}
