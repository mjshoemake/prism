# Database Design Session Summary
## Agile Scrum Metrics STAR Schema Implementation

### Project Overview
**Objective**: Create a PostgreSQL STAR schema for Agile Scrum metrics with realistic test data for Power BI reporting.

**Duration**: Single session
**Final Deliverables**: 4 SQL scripts creating a complete data warehouse with 4,056 records

---

## Key Requirements & Metrics

### Agile Scrum Metrics Defined
1. **Say/Do Ratio**: Committed story points completed ÷ Committed story points (0-100%)
2. **Velocity**: Average story points completed per sprint
3. **Commitment Strength**: Story points committed ÷ Average velocity (70-105% optimal)
4. **Scope Churn**: Story points added/removed/modified during sprint
5. **Estimate Changed**: Stories with modified estimates ÷ Stories completed
6. **% Estimated**: Stories estimated ÷ Total stories

### Design Principles
- Store numerator/denominator pairs instead of calculated percentages
- Avoid averaging percentages to maintain data integrity
- Use explicit primary keys for easier fact table creation
- Create realistic team performance variations

---

## Final Schema Design

### Dimension Tables

#### Sprint_Dim (52 records)
- **Primary Key**: sprint_pk (1-52)
- **Attributes**: sprint_month, sprint_year, sprint_quarter, year_month, year_quarter, sprint_num, sprint_name, sprint_abbr, sprint_start_date
- **Coverage**: 2 years (2024-2025), 26 sprints per year, 2-week intervals

#### Team_Dim (78 records)
- **Primary Key**: team_pk (1-78)
- **Hierarchy**: 5 levels (CEO → SVP → Director → Senior Manager → Manager → Team)
- **Structure**: 1 CEO, 3 SVPs, 6 Directors, 12 Senior Managers, 36 Managers, 78 Teams
- **Themes**: Space shuttles, mountains, helicopters, war generals, cartoon characters, national parks, US states, Star Trek ships, WW2 ships, modern battleships

### Fact Table

#### Sprint_Metrics_Fact (4,056 records)
- **Coverage**: All 78 teams × All 52 sprints
- **Metrics Components**:
  - Say/Do: committed_story_points_completed, committed_story_points
  - Velocity: story_points_completed
  - Commitment Strength: story_points_committed_at_start, avg_velocity_3_sprints, avg_velocity_4_sprints
  - Scope Churn: story_points_added_during_sprint, story_points_removed_during_sprint, story_points_modified_scope
  - Estimate Changed: stories_with_modified_estimate, stories_completed
  - % Estimated: stories_estimated, total_stories

---

## Team Performance Profiles

### Realistic Team Variations
1. **Stable High Performers** (Teams 1-3): 90-100% Say/Do ratios, low scope churn
2. **Variable Moderate Performers** (Teams 4-5): 70-90% ratios, moderate scope churn
3. **Declining Teams** (Teams 6-7): Over-committing, declining performance over time
4. **Improving Teams** (Teams 8-9): Getting better at estimation, improving trends
5. **Stable Low Performers** (Teams 10-11): Consistent but low velocity
6. **High Scope Churn Teams** (Teams 12-13): Significant scope changes and estimate modifications
7. **Other Teams** (Teams 14-78): Mixed performance patterns with seasonal variations

### Data Generation Strategy
- **Manual Data**: Key sprints (1, 2, 3, 27) with specific patterns
- **SQL Generation**: Remaining 48 sprints using CASE statements and RANDOM() functions
- **Progressive Patterns**: Teams show improvement/decline over time
- **Seasonal Variations**: Performance changes based on sprint progression

---

## Technical Implementation

### Key Design Decisions
1. **Explicit Primary Keys**: Used fixed primary keys (1-52 for sprints, 1-78 for teams) instead of auto-generated SERIAL
2. **Schema Naming**: Used "prism" schema instead of "powerBI_test"
3. **Data Types**: INTEGER for IDs, DECIMAL for velocity averages, proper constraints
4. **Foreign Keys**: Proper referential integrity between fact and dimension tables

### SQL Scripts Created
1. **create_star_schema.sql**: Table structure, constraints, indexes
2. **insert_sprint_dim_data_complete.sql**: Sprint dimension with explicit primary keys
3. **insert_team_dim_data_complete.sql**: Team dimension with organizational hierarchy
4. **insert_fact_table_complete.sql**: Complete fact table with realistic patterns

### Data Quality Features
- **Realistic Ranges**: Say/Do ratios 50-100%, Commitment Strength 70-105%
- **Natural Variation**: Random fluctuations within realistic bounds
- **Trend Patterns**: Teams show improvement/decline over time
- **Scope Churn**: Occasional but realistic scope changes
- **Estimate Changes**: Infrequent but meaningful estimate modifications

---

## Lessons Learned & Best Practices

### Database Design
- Explicit primary keys make fact table creation much easier
- Store raw components instead of calculated percentages
- Use proper constraints and data validation
- Plan for realistic data variations from the start

### Data Generation
- Mix manual and generated data for key patterns
- Use SQL CASE statements for complex business logic
- Incorporate time-based trends and seasonal patterns
- Test with small datasets before generating full volumes

### Collaboration Process
- Iterative refinement based on feedback
- Clear communication of requirements and constraints
- Systematic approach to complex multi-table schemas
- Documentation of decisions and rationale

---

## Power BI Readiness

### Analysis Capabilities
- **Organizational Drill-Down**: CEO → SVP → Director → Senior Manager → Manager → Team
- **Time Analysis**: Sprint, month, quarter, year trends
- **Performance Metrics**: All 6 Agile metrics with proper calculations
- **Comparative Analysis**: Team performance across different time periods

### Data Relationships
- Proper foreign key relationships for accurate joins
- Hierarchical team structure for organizational analysis
- Time-based attributes for temporal analysis
- Rich fact table for comprehensive metric analysis

---

## Final Statistics
- **Total Tables**: 3 (2 dimensions, 1 fact)
- **Total Records**: 4,130 (52 + 78 + 4,056)
- **Time Period**: 2 years (2024-2025)
- **Teams**: 78 across 3 organizational levels
- **Sprints**: 52 (26 per year)
- **Metrics**: 6 Agile Scrum metrics with realistic variations

This STAR schema provides a solid foundation for comprehensive Agile metrics analysis and reporting in Power BI.
