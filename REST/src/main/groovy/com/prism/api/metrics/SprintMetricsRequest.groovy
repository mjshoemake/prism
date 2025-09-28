package com.prism.api.metrics

class SprintMetricsRequest {
    String focusLevelColumn;
    String valueColumnsForSelectClause;
    String whereClause;

    // Constructors
    SprintMetricsRequest() {}

    SprintMetricsRequest(String focusLevelColumn, String valueColumnsForSelectClause, String whereClause) {
        this.focusLevelColumn = focusLevelColumn;
        this.valueColumnsForSelectClause = valueColumnsForSelectClause;
        this.whereClause = whereClause;
    }
}