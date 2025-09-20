-- Insert comprehensive test data for Sprint_Metrics_Fact table
-- All teams (1-78) across all sprints (1-52) with realistic performance variations
-- Mix of stable, variable, and trending teams with realistic fluctuations

-- Clear existing data
DELETE FROM "prism"."Sprint_Metrics_Fact";

-- Insert fact data for all sprints and teams
INSERT INTO "prism"."Sprint_Metrics_Fact" 
(sprint_pk, team_pk, committed_story_points_completed, committed_story_points, 
 story_points_completed, story_points_committed_at_start, avg_velocity_3_sprints, avg_velocity_4_sprints,
 story_points_added_during_sprint, story_points_removed_during_sprint, story_points_modified_scope,
 stories_with_modified_estimate, stories_completed, stories_estimated, total_stories)
VALUES
-- Sprint 1 (2024-12-23) - 2024 S26
-- Stable High Performers (Teams 1-3: Discovery, Endeavour, Atlantis)
(1, 1, 18, 20, 20, 20, 19.0, 18.5, 2, 0, 0, 0, 3, 3, 3),
(1, 2, 16, 18, 18, 18, 17.0, 16.5, 1, 0, 0, 0, 2, 2, 2),
(1, 3, 14, 16, 16, 16, 15.0, 14.5, 0, 0, 0, 0, 2, 2, 2),
-- Variable Moderate Performers (Teams 4-5: Everest, Kilimanjaro)
(1, 4, 12, 15, 16, 15, 14.0, 13.5, 3, 0, 1, 1, 2, 2, 2),
(1, 5, 10, 14, 15, 14, 13.0, 12.5, 2, 0, 0, 0, 2, 2, 2),
-- Declining Teams (Teams 6-7: Apache, Cobra)
(1, 6, 8, 12, 12, 12, 11.0, 10.5, 1, 0, 0, 0, 2, 2, 2),
(1, 7, 6, 10, 10, 10, 9.0, 8.5, 0, 0, 0, 0, 1, 1, 1),
-- Improving Teams (Teams 8-9: Grant, Patton)
(1, 8, 4, 8, 9, 8, 7.0, 6.5, 2, 0, 0, 0, 1, 1, 1),
(1, 9, 6, 9, 10, 9, 8.0, 7.5, 1, 0, 0, 0, 1, 1, 1),
-- Stable Low Performers (Teams 10-11: Flintstone, Jetson)
(1, 10, 5, 7, 7, 7, 6.0, 5.5, 0, 0, 0, 0, 1, 1, 1),
(1, 11, 4, 6, 6, 6, 5.0, 4.5, 0, 0, 0, 0, 1, 1, 1),
-- High Scope Churn Teams (Teams 12-13: Yellowstone, Yosemite)
(1, 12, 8, 12, 14, 12, 11.0, 10.5, 4, 1, 2, 1, 2, 2, 2),
(1, 13, 7, 11, 13, 11, 10.0, 9.5, 3, 0, 1, 0, 2, 2, 2),
-- Continue with remaining teams for Sprint 1 (Teams 14-78)
(1, 14, 15, 17, 18, 17, 16.0, 15.5, 2, 0, 0, 0, 2, 2, 2),
(1, 15, 13, 15, 16, 15, 14.0, 13.5, 1, 0, 0, 0, 2, 2, 2),
(1, 16, 11, 13, 14, 13, 12.0, 11.5, 2, 0, 0, 0, 2, 2, 2),
(1, 17, 9, 12, 13, 12, 11.0, 10.5, 1, 0, 0, 0, 2, 2, 2),
(1, 18, 7, 10, 11, 10, 9.0, 8.5, 2, 0, 0, 0, 1, 1, 1),
(1, 19, 5, 8, 9, 8, 7.0, 6.5, 1, 0, 0, 0, 1, 1, 1),
(1, 20, 12, 14, 15, 14, 13.0, 12.5, 2, 0, 0, 0, 2, 2, 2),
(1, 21, 10, 13, 14, 13, 12.0, 11.5, 1, 0, 0, 0, 2, 2, 2),
(1, 22, 8, 11, 12, 11, 10.0, 9.5, 2, 0, 0, 0, 1, 1, 1),
(1, 23, 6, 9, 10, 9, 8.0, 7.5, 1, 0, 0, 0, 1, 1, 1),
(1, 24, 9, 12, 13, 12, 11.0, 10.5, 2, 0, 1, 0, 2, 2, 2),
(1, 25, 7, 10, 11, 10, 9.0, 8.5, 1, 0, 0, 0, 2, 2, 2),
-- SVP 2 Teams (26-53)
(1, 26, 14, 16, 17, 16, 15.0, 14.5, 2, 0, 0, 0, 2, 2, 2),
(1, 27, 12, 15, 16, 15, 14.0, 13.5, 1, 0, 0, 0, 2, 2, 2),
(1, 28, 10, 13, 14, 13, 12.0, 11.5, 2, 0, 0, 0, 2, 2, 2),
(1, 29, 8, 11, 12, 11, 10.0, 9.5, 1, 0, 0, 0, 1, 1, 1),
(1, 30, 6, 9, 10, 9, 8.0, 7.5, 2, 0, 0, 0, 1, 1, 1),
(1, 31, 11, 14, 15, 14, 13.0, 12.5, 2, 0, 0, 0, 2, 2, 2),
(1, 32, 9, 12, 13, 12, 11.0, 10.5, 1, 0, 0, 0, 2, 2, 2),
(1, 33, 7, 10, 11, 10, 9.0, 8.5, 2, 0, 0, 0, 1, 1, 1),
(1, 34, 13, 15, 16, 15, 14.0, 13.5, 2, 0, 0, 0, 2, 2, 2),
(1, 35, 11, 14, 15, 14, 13.0, 12.5, 1, 0, 0, 0, 2, 2, 2),
(1, 36, 9, 12, 13, 12, 11.0, 10.5, 2, 0, 0, 0, 1, 1, 1),
(1, 37, 7, 10, 11, 10, 9.0, 8.5, 1, 0, 0, 0, 1, 1, 1),
(1, 38, 5, 8, 9, 8, 7.0, 6.5, 2, 0, 0, 0, 1, 1, 1),
(1, 39, 3, 6, 7, 6, 5.0, 4.5, 1, 0, 0, 0, 1, 1, 1),
(1, 40, 1, 4, 5, 4, 3.0, 2.5, 2, 0, 0, 0, 1, 1, 1),
(1, 41, 12, 14, 15, 14, 13.0, 12.5, 2, 0, 0, 0, 2, 2, 2),
(1, 42, 10, 13, 14, 13, 12.0, 11.5, 1, 0, 0, 0, 2, 2, 2),
(1, 43, 8, 11, 12, 11, 10.0, 9.5, 2, 0, 0, 0, 1, 1, 1),
(1, 44, 6, 9, 10, 9, 8.0, 7.5, 1, 0, 0, 0, 1, 1, 1),
(1, 45, 9, 12, 13, 12, 11.0, 10.5, 2, 0, 1, 0, 2, 2, 2),
(1, 46, 7, 10, 11, 10, 9.0, 8.5, 1, 0, 0, 0, 2, 2, 2),
(1, 47, 11, 13, 14, 13, 12.0, 11.5, 2, 0, 0, 0, 2, 2, 2),
(1, 48, 9, 12, 13, 12, 11.0, 10.5, 1, 0, 0, 0, 2, 2, 2),
(1, 49, 7, 10, 11, 10, 9.0, 8.5, 2, 0, 0, 0, 1, 1, 1),
(1, 50, 5, 8, 9, 8, 7.0, 6.5, 1, 0, 0, 0, 1, 1, 1),
(1, 51, 13, 15, 16, 15, 14.0, 13.5, 2, 0, 0, 0, 2, 2, 2),
(1, 52, 11, 14, 15, 14, 13.0, 12.5, 1, 0, 0, 0, 2, 2, 2),
(1, 53, 9, 12, 13, 12, 11.0, 10.5, 2, 0, 0, 0, 1, 1, 1),
-- SVP 3 Teams (54-78)
(1, 54, 14, 16, 17, 16, 15.0, 14.5, 2, 0, 0, 0, 2, 2, 2),
(1, 55, 12, 15, 16, 15, 14.0, 13.5, 1, 0, 0, 0, 2, 2, 2),
(1, 56, 10, 13, 14, 13, 12.0, 11.5, 2, 0, 0, 0, 2, 2, 2),
(1, 57, 8, 11, 12, 11, 10.0, 9.5, 1, 0, 0, 0, 1, 1, 1),
(1, 58, 6, 9, 10, 9, 8.0, 7.5, 2, 0, 0, 0, 1, 1, 1),
(1, 59, 4, 7, 8, 7, 6.0, 5.5, 1, 0, 0, 0, 1, 1, 1),
(1, 60, 11, 14, 15, 14, 13.0, 12.5, 2, 0, 0, 0, 2, 2, 2),
(1, 61, 9, 12, 13, 12, 11.0, 10.5, 1, 0, 0, 0, 2, 2, 2),
(1, 62, 7, 10, 11, 10, 9.0, 8.5, 2, 0, 0, 0, 1, 1, 1),
(1, 63, 5, 8, 9, 8, 7.0, 6.5, 1, 0, 0, 0, 1, 1, 1),
(1, 64, 3, 6, 7, 6, 5.0, 4.5, 2, 0, 0, 0, 1, 1, 1),
(1, 65, 1, 4, 5, 4, 3.0, 2.5, 1, 0, 0, 0, 1, 1, 1),
(1, 66, 12, 14, 15, 14, 13.0, 12.5, 2, 0, 0, 0, 2, 2, 2),
(1, 67, 10, 13, 14, 13, 12.0, 11.5, 1, 0, 0, 0, 2, 2, 2),
(1, 68, 8, 11, 12, 11, 10.0, 9.5, 2, 0, 0, 0, 1, 1, 1),
(1, 69, 6, 9, 10, 9, 8.0, 7.5, 1, 0, 0, 0, 1, 1, 1),
(1, 70, 9, 12, 13, 12, 11.0, 10.5, 2, 0, 1, 0, 2, 2, 2),
(1, 71, 7, 10, 11, 10, 9.0, 8.5, 1, 0, 0, 0, 2, 2, 2),
(1, 72, 11, 13, 14, 13, 12.0, 11.5, 2, 0, 0, 0, 2, 2, 2),
(1, 73, 9, 12, 13, 12, 11.0, 10.5, 1, 0, 0, 0, 2, 2, 2),
(1, 74, 7, 10, 11, 10, 9.0, 8.5, 2, 0, 0, 0, 1, 1, 1),
(1, 75, 5, 8, 9, 8, 7.0, 6.5, 1, 0, 0, 0, 1, 1, 1),
(1, 76, 3, 6, 7, 6, 5.0, 4.5, 2, 0, 0, 0, 1, 1, 1),
(1, 77, 13, 15, 16, 15, 14.0, 13.5, 2, 0, 0, 0, 2, 2, 2),
(1, 78, 11, 14, 15, 14, 13.0, 12.5, 1, 0, 0, 0, 2, 2, 2),

-- Sprint 2 (2024-12-09) - 2024 S25
-- Stable High Performers (Teams 1-3: Discovery, Endeavour, Atlantis)
(2, 1, 19, 20, 22, 20, 19.0, 18.5, 3, 0, 0, 0, 3, 3, 3),
(2, 2, 17, 18, 19, 18, 17.0, 16.5, 2, 0, 0, 0, 2, 2, 2),
(2, 3, 15, 16, 17, 16, 15.0, 14.5, 1, 0, 0, 0, 2, 2, 2),
-- Variable Moderate Performers (Teams 4-5: Everest, Kilimanjaro)
(2, 4, 11, 15, 17, 15, 14.0, 13.5, 4, 0, 2, 1, 2, 2, 2),
(2, 5, 9, 14, 16, 14, 13.0, 12.5, 3, 0, 1, 0, 2, 2, 2),
-- Declining Teams (Teams 6-7: Apache, Cobra)
(2, 6, 7, 12, 12, 12, 11.0, 10.5, 1, 0, 0, 0, 2, 2, 2),
(2, 7, 5, 10, 10, 10, 9.0, 8.5, 0, 0, 0, 0, 1, 1, 1),
-- Improving Teams (Teams 8-9: Grant, Patton)
(2, 8, 6, 8, 9, 8, 7.0, 6.5, 2, 0, 0, 0, 1, 1, 1),
(2, 9, 7, 9, 10, 9, 8.0, 7.5, 1, 0, 0, 0, 1, 1, 1),
-- Stable Low Performers (Teams 10-11: Flintstone, Jetson)
(2, 10, 5, 7, 7, 7, 6.0, 5.5, 0, 0, 0, 0, 1, 1, 1),
(2, 11, 4, 6, 6, 6, 5.0, 4.5, 0, 0, 0, 0, 1, 1, 1),
-- High Scope Churn Teams (Teams 12-13: Yellowstone, Yosemite)
(2, 12, 9, 12, 15, 12, 11.0, 10.5, 5, 1, 3, 1, 2, 2, 2),
(2, 13, 8, 11, 14, 11, 10.0, 9.5, 4, 0, 2, 0, 2, 2, 2),
-- Continue with remaining teams for Sprint 2 (Teams 14-78)
(2, 14, 16, 17, 19, 17, 16.0, 15.5, 3, 0, 0, 0, 2, 2, 2),
(2, 15, 14, 15, 17, 15, 14.0, 13.5, 2, 0, 0, 0, 2, 2, 2),
(2, 16, 12, 13, 15, 13, 12.0, 11.5, 3, 0, 0, 0, 2, 2, 2),
(2, 17, 10, 12, 14, 12, 11.0, 10.5, 2, 0, 0, 0, 2, 2, 2),
(2, 18, 8, 10, 12, 10, 9.0, 8.5, 3, 0, 0, 0, 1, 1, 1),
(2, 19, 6, 8, 10, 8, 7.0, 6.5, 2, 0, 0, 0, 1, 1, 1),
(2, 20, 13, 14, 16, 14, 13.0, 12.5, 3, 0, 0, 0, 2, 2, 2),
(2, 21, 11, 13, 15, 13, 12.0, 11.5, 2, 0, 0, 0, 2, 2, 2),
(2, 22, 9, 11, 13, 11, 10.0, 9.5, 3, 0, 0, 0, 1, 1, 1),
(2, 23, 7, 9, 11, 9, 8.0, 7.5, 2, 0, 0, 0, 1, 1, 1),
(2, 24, 10, 12, 14, 12, 11.0, 10.5, 3, 0, 2, 0, 2, 2, 2),
(2, 25, 8, 10, 12, 10, 9.0, 8.5, 2, 0, 0, 0, 2, 2, 2),
-- SVP 2 Teams (26-53)
(2, 26, 15, 16, 18, 16, 15.0, 14.5, 3, 0, 0, 0, 2, 2, 2),
(2, 27, 13, 15, 17, 15, 14.0, 13.5, 2, 0, 0, 0, 2, 2, 2),
(2, 28, 11, 13, 15, 13, 12.0, 11.5, 3, 0, 0, 0, 2, 2, 2),
(2, 29, 9, 11, 13, 11, 10.0, 9.5, 2, 0, 0, 0, 1, 1, 1),
(2, 30, 7, 9, 11, 9, 8.0, 7.5, 3, 0, 0, 0, 1, 1, 1),
(2, 31, 12, 14, 16, 14, 13.0, 12.5, 3, 0, 0, 0, 2, 2, 2),
(2, 32, 10, 12, 14, 12, 11.0, 10.5, 2, 0, 0, 0, 2, 2, 2),
(2, 33, 8, 10, 12, 10, 9.0, 8.5, 3, 0, 0, 0, 1, 1, 1),
(2, 34, 14, 15, 17, 15, 14.0, 13.5, 3, 0, 0, 0, 2, 2, 2),
(2, 35, 12, 14, 16, 14, 13.0, 12.5, 2, 0, 0, 0, 2, 2, 2),
(2, 36, 10, 12, 14, 12, 11.0, 10.5, 3, 0, 0, 0, 1, 1, 1),
(2, 37, 8, 10, 12, 10, 9.0, 8.5, 2, 0, 0, 0, 1, 1, 1),
(2, 38, 6, 8, 10, 8, 7.0, 6.5, 3, 0, 0, 0, 1, 1, 1),
(2, 39, 4, 6, 8, 6, 5.0, 4.5, 2, 0, 0, 0, 1, 1, 1),
(2, 40, 2, 4, 6, 4, 3.0, 2.5, 3, 0, 0, 0, 1, 1, 1),
(2, 41, 13, 14, 16, 14, 13.0, 12.5, 3, 0, 0, 0, 2, 2, 2),
(2, 42, 11, 13, 15, 13, 12.0, 11.5, 2, 0, 0, 0, 2, 2, 2),
(2, 43, 9, 11, 13, 11, 10.0, 9.5, 3, 0, 0, 0, 1, 1, 1),
(2, 44, 7, 9, 11, 9, 8.0, 7.5, 2, 0, 0, 0, 1, 1, 1),
(2, 45, 10, 12, 14, 12, 11.0, 10.5, 3, 0, 2, 0, 2, 2, 2),
(2, 46, 8, 10, 12, 10, 9.0, 8.5, 2, 0, 0, 0, 2, 2, 2),
(2, 47, 12, 13, 15, 13, 12.0, 11.5, 3, 0, 0, 0, 2, 2, 2),
(2, 48, 10, 12, 14, 12, 11.0, 10.5, 2, 0, 0, 0, 2, 2, 2),
(2, 49, 8, 10, 12, 10, 9.0, 8.5, 3, 0, 0, 0, 1, 1, 1),
(2, 50, 6, 8, 10, 8, 7.0, 6.5, 2, 0, 0, 0, 1, 1, 1),
(2, 51, 14, 15, 17, 15, 14.0, 13.5, 3, 0, 0, 0, 2, 2, 2),
(2, 52, 12, 14, 16, 14, 13.0, 12.5, 2, 0, 0, 0, 2, 2, 2),
(2, 53, 10, 12, 14, 12, 11.0, 10.5, 3, 0, 0, 0, 1, 1, 1),
-- SVP 3 Teams (54-78)
(2, 54, 15, 16, 18, 16, 15.0, 14.5, 3, 0, 0, 0, 2, 2, 2),
(2, 55, 13, 15, 17, 15, 14.0, 13.5, 2, 0, 0, 0, 2, 2, 2),
(2, 56, 11, 13, 15, 13, 12.0, 11.5, 3, 0, 0, 0, 2, 2, 2),
(2, 57, 9, 11, 13, 11, 10.0, 9.5, 2, 0, 0, 0, 1, 1, 1),
(2, 58, 7, 9, 11, 9, 8.0, 7.5, 3, 0, 0, 0, 1, 1, 1),
(2, 59, 5, 7, 9, 7, 6.0, 5.5, 2, 0, 0, 0, 1, 1, 1),
(2, 60, 12, 14, 16, 14, 13.0, 12.5, 3, 0, 0, 0, 2, 2, 2),
(2, 61, 10, 12, 14, 12, 11.0, 10.5, 2, 0, 0, 0, 2, 2, 2),
(2, 62, 8, 10, 12, 10, 9.0, 8.5, 3, 0, 0, 0, 1, 1, 1),
(2, 63, 6, 8, 10, 8, 7.0, 6.5, 2, 0, 0, 0, 1, 1, 1),
(2, 64, 4, 6, 8, 6, 5.0, 4.5, 3, 0, 0, 0, 1, 1, 1),
(2, 65, 2, 4, 6, 4, 3.0, 2.5, 2, 0, 0, 0, 1, 1, 1),
(2, 66, 13, 14, 16, 14, 13.0, 12.5, 3, 0, 0, 0, 2, 2, 2),
(2, 67, 11, 13, 15, 13, 12.0, 11.5, 2, 0, 0, 0, 2, 2, 2),
(2, 68, 9, 11, 13, 11, 10.0, 9.5, 3, 0, 0, 0, 1, 1, 1),
(2, 69, 7, 9, 11, 9, 8.0, 7.5, 2, 0, 0, 0, 1, 1, 1),
(2, 70, 10, 12, 14, 12, 11.0, 10.5, 3, 0, 2, 0, 2, 2, 2),
(2, 71, 8, 10, 12, 10, 9.0, 8.5, 2, 0, 0, 0, 2, 2, 2),
(2, 72, 12, 13, 15, 13, 12.0, 11.5, 3, 0, 0, 0, 2, 2, 2),
(2, 73, 10, 12, 14, 12, 11.0, 10.5, 2, 0, 0, 0, 2, 2, 2),
(2, 74, 8, 10, 12, 10, 9.0, 8.5, 3, 0, 0, 0, 1, 1, 1),
(2, 75, 6, 8, 10, 8, 7.0, 6.5, 2, 0, 0, 0, 1, 1, 1),
(2, 76, 4, 6, 8, 6, 5.0, 4.5, 3, 0, 0, 0, 1, 1, 1),
(2, 77, 14, 15, 17, 15, 14.0, 13.5, 3, 0, 0, 0, 2, 2, 2),
(2, 78, 12, 14, 16, 14, 13.0, 12.5, 2, 0, 0, 0, 2, 2, 2);

-- Generate remaining sprints (3-52) using SQL to create realistic patterns
-- This approach is much more efficient than manually typing 4,000+ records

-- Insert data for sprints 3-52 using a more efficient approach
-- We'll use a pattern-based generation for the remaining 50 sprints

-- For demonstration, let's add a few more key sprints manually to show the patterns
-- Sprint 3 (2024-11-25) - 2024 S24
INSERT INTO "prism"."Sprint_Metrics_Fact" 
(sprint_pk, team_pk, committed_story_points_completed, committed_story_points, 
 story_points_completed, story_points_committed_at_start, avg_velocity_3_sprints, avg_velocity_4_sprints,
 story_points_added_during_sprint, story_points_removed_during_sprint, story_points_modified_scope,
 stories_with_modified_estimate, stories_completed, stories_estimated, total_stories)
SELECT 
    3 as sprint_pk,
    td.team_pk,
    -- Generate realistic variations based on team performance profile
    CASE 
        WHEN td.team_pk <= 3 THEN GREATEST(16, LEAST(20, 18 + (RANDOM() * 4 - 2)::INTEGER)) -- Stable high performers
        WHEN td.team_pk <= 5 THEN GREATEST(8, LEAST(16, 12 + (RANDOM() * 6 - 3)::INTEGER)) -- Variable moderate
        WHEN td.team_pk <= 7 THEN GREATEST(4, LEAST(10, 8 + (RANDOM() * 4 - 2)::INTEGER)) -- Declining teams
        WHEN td.team_pk <= 9 THEN GREATEST(6, LEAST(12, 8 + (RANDOM() * 4 - 2)::INTEGER)) -- Improving teams
        WHEN td.team_pk <= 11 THEN GREATEST(3, LEAST(7, 5 + (RANDOM() * 2 - 1)::INTEGER)) -- Stable low
        WHEN td.team_pk <= 13 THEN GREATEST(6, LEAST(14, 10 + (RANDOM() * 6 - 3)::INTEGER)) -- High scope churn
        ELSE GREATEST(5, LEAST(15, 10 + (RANDOM() * 8 - 4)::INTEGER)) -- Other teams
    END as committed_story_points_completed,
    CASE 
        WHEN td.team_pk <= 3 THEN 20 -- Stable high performers
        WHEN td.team_pk <= 5 THEN 15 -- Variable moderate
        WHEN td.team_pk <= 7 THEN 12 -- Declining teams
        WHEN td.team_pk <= 9 THEN 10 -- Improving teams
        WHEN td.team_pk <= 11 THEN 7 -- Stable low
        WHEN td.team_pk <= 13 THEN 12 -- High scope churn
        ELSE 12 -- Other teams
    END as committed_story_points,
    CASE 
        WHEN td.team_pk <= 3 THEN 20 + (RANDOM() * 3)::INTEGER -- Stable high performers
        WHEN td.team_pk <= 5 THEN 15 + (RANDOM() * 4)::INTEGER -- Variable moderate
        WHEN td.team_pk <= 7 THEN 12 -- Declining teams
        WHEN td.team_pk <= 9 THEN 10 + (RANDOM() * 2)::INTEGER -- Improving teams
        WHEN td.team_pk <= 11 THEN 7 -- Stable low
        WHEN td.team_pk <= 13 THEN 12 + (RANDOM() * 4)::INTEGER -- High scope churn
        ELSE 12 + (RANDOM() * 3)::INTEGER -- Other teams
    END as story_points_completed,
    CASE 
        WHEN td.team_pk <= 3 THEN 20 -- Stable high performers
        WHEN td.team_pk <= 5 THEN 15 -- Variable moderate
        WHEN td.team_pk <= 7 THEN 12 -- Declining teams
        WHEN td.team_pk <= 9 THEN 10 -- Improving teams
        WHEN td.team_pk <= 11 THEN 7 -- Stable low
        WHEN td.team_pk <= 13 THEN 12 -- High scope churn
        ELSE 12 -- Other teams
    END as story_points_committed_at_start,
    CASE 
        WHEN td.team_pk <= 3 THEN 19.0 -- Stable high performers
        WHEN td.team_pk <= 5 THEN 14.0 -- Variable moderate
        WHEN td.team_pk <= 7 THEN 11.0 -- Declining teams
        WHEN td.team_pk <= 9 THEN 8.0 -- Improving teams
        WHEN td.team_pk <= 11 THEN 6.0 -- Stable low
        WHEN td.team_pk <= 13 THEN 11.0 -- High scope churn
        ELSE 10.0 -- Other teams
    END as avg_velocity_3_sprints,
    CASE 
        WHEN td.team_pk <= 3 THEN 18.5 -- Stable high performers
        WHEN td.team_pk <= 5 THEN 13.5 -- Variable moderate
        WHEN td.team_pk <= 7 THEN 10.5 -- Declining teams
        WHEN td.team_pk <= 9 THEN 7.5 -- Improving teams
        WHEN td.team_pk <= 11 THEN 5.5 -- Stable low
        WHEN td.team_pk <= 13 THEN 10.5 -- High scope churn
        ELSE 9.5 -- Other teams
    END as avg_velocity_4_sprints,
    CASE 
        WHEN td.team_pk <= 3 THEN (RANDOM() * 3)::INTEGER -- Stable high performers
        WHEN td.team_pk <= 5 THEN (RANDOM() * 4)::INTEGER -- Variable moderate
        WHEN td.team_pk <= 7 THEN (RANDOM() * 2)::INTEGER -- Declining teams
        WHEN td.team_pk <= 9 THEN (RANDOM() * 3)::INTEGER -- Improving teams
        WHEN td.team_pk <= 11 THEN 0 -- Stable low
        WHEN td.team_pk <= 13 THEN (RANDOM() * 6)::INTEGER -- High scope churn
        ELSE (RANDOM() * 3)::INTEGER -- Other teams
    END as story_points_added_during_sprint,
    CASE 
        WHEN td.team_pk <= 13 AND RANDOM() < 0.3 THEN 1 -- Occasional removals for scope churn teams
        ELSE 0
    END as story_points_removed_during_sprint,
    CASE 
        WHEN td.team_pk <= 5 AND RANDOM() < 0.4 THEN (RANDOM() * 3)::INTEGER -- Variable moderate
        WHEN td.team_pk <= 7 AND RANDOM() < 0.2 THEN (RANDOM() * 2)::INTEGER -- Declining teams
        WHEN td.team_pk <= 13 AND RANDOM() < 0.6 THEN (RANDOM() * 4)::INTEGER -- High scope churn
        ELSE 0
    END as story_points_modified_scope,
    CASE 
        WHEN td.team_pk <= 5 AND RANDOM() < 0.3 THEN 1 -- Variable moderate
        WHEN td.team_pk <= 13 AND RANDOM() < 0.4 THEN 1 -- High scope churn
        ELSE 0
    END as stories_with_modified_estimate,
    CASE 
        WHEN td.team_pk <= 3 THEN 3 -- Stable high performers
        WHEN td.team_pk <= 13 THEN 2 -- Most other teams
        ELSE 1 -- Some teams
    END as stories_completed,
    CASE 
        WHEN td.team_pk <= 3 THEN 3 -- Stable high performers
        WHEN td.team_pk <= 13 THEN 2 -- Most other teams
        ELSE 1 -- Some teams
    END as stories_estimated,
    CASE 
        WHEN td.team_pk <= 3 THEN 3 -- Stable high performers
        WHEN td.team_pk <= 13 THEN 2 -- Most other teams
        ELSE 1 -- Some teams
    END as total_stories
FROM "prism"."Team_Dim" td;

-- Add a few more key sprints to show progression patterns
-- Sprint 27 (2025-01-06) - 2025 S1 - New Year, fresh start
INSERT INTO "prism"."Sprint_Metrics_Fact" 
(sprint_pk, team_pk, committed_story_points_completed, committed_story_points, 
 story_points_completed, story_points_committed_at_start, avg_velocity_3_sprints, avg_velocity_4_sprints,
 story_points_added_during_sprint, story_points_removed_during_sprint, story_points_modified_scope,
 stories_with_modified_estimate, stories_completed, stories_estimated, total_stories)
SELECT 
    27 as sprint_pk,
    td.team_pk,
    -- New Year boost for most teams, but some struggling teams continue to struggle
    CASE 
        WHEN td.team_pk <= 3 THEN GREATEST(18, LEAST(22, 20 + (RANDOM() * 4 - 2)::INTEGER)) -- Stable high performers get boost
        WHEN td.team_pk <= 5 THEN GREATEST(10, LEAST(18, 14 + (RANDOM() * 6 - 3)::INTEGER)) -- Variable moderate
        WHEN td.team_pk <= 7 THEN GREATEST(3, LEAST(8, 6 + (RANDOM() * 4 - 2)::INTEGER)) -- Declining teams still struggling
        WHEN td.team_pk <= 9 THEN GREATEST(8, LEAST(14, 10 + (RANDOM() * 6 - 3)::INTEGER)) -- Improving teams get boost
        WHEN td.team_pk <= 11 THEN GREATEST(4, LEAST(8, 6 + (RANDOM() * 2 - 1)::INTEGER)) -- Stable low get slight boost
        WHEN td.team_pk <= 13 THEN GREATEST(8, LEAST(16, 12 + (RANDOM() * 6 - 3)::INTEGER)) -- High scope churn
        ELSE GREATEST(6, LEAST(16, 11 + (RANDOM() * 8 - 4)::INTEGER)) -- Other teams get boost
    END as committed_story_points_completed,
    CASE 
        WHEN td.team_pk <= 3 THEN 22 -- Stable high performers increase commitment
        WHEN td.team_pk <= 5 THEN 16 -- Variable moderate
        WHEN td.team_pk <= 7 THEN 10 -- Declining teams reduce commitment
        WHEN td.team_pk <= 9 THEN 12 -- Improving teams increase commitment
        WHEN td.team_pk <= 11 THEN 8 -- Stable low increase slightly
        WHEN td.team_pk <= 13 THEN 14 -- High scope churn
        ELSE 13 -- Other teams
    END as committed_story_points,
    CASE 
        WHEN td.team_pk <= 3 THEN 22 + (RANDOM() * 3)::INTEGER -- Stable high performers
        WHEN td.team_pk <= 5 THEN 16 + (RANDOM() * 4)::INTEGER -- Variable moderate
        WHEN td.team_pk <= 7 THEN 10 -- Declining teams
        WHEN td.team_pk <= 9 THEN 12 + (RANDOM() * 3)::INTEGER -- Improving teams
        WHEN td.team_pk <= 11 THEN 8 -- Stable low
        WHEN td.team_pk <= 13 THEN 14 + (RANDOM() * 4)::INTEGER -- High scope churn
        ELSE 13 + (RANDOM() * 3)::INTEGER -- Other teams
    END as story_points_completed,
    CASE 
        WHEN td.team_pk <= 3 THEN 22 -- Stable high performers
        WHEN td.team_pk <= 5 THEN 16 -- Variable moderate
        WHEN td.team_pk <= 7 THEN 10 -- Declining teams
        WHEN td.team_pk <= 9 THEN 12 -- Improving teams
        WHEN td.team_pk <= 11 THEN 8 -- Stable low
        WHEN td.team_pk <= 13 THEN 14 -- High scope churn
        ELSE 13 -- Other teams
    END as story_points_committed_at_start,
    CASE 
        WHEN td.team_pk <= 3 THEN 20.0 -- Stable high performers
        WHEN td.team_pk <= 5 THEN 15.0 -- Variable moderate
        WHEN td.team_pk <= 7 THEN 9.0 -- Declining teams
        WHEN td.team_pk <= 9 THEN 9.0 -- Improving teams
        WHEN td.team_pk <= 11 THEN 7.0 -- Stable low
        WHEN td.team_pk <= 13 THEN 12.0 -- High scope churn
        ELSE 11.0 -- Other teams
    END as avg_velocity_3_sprints,
    CASE 
        WHEN td.team_pk <= 3 THEN 19.0 -- Stable high performers
        WHEN td.team_pk <= 5 THEN 14.0 -- Variable moderate
        WHEN td.team_pk <= 7 THEN 8.0 -- Declining teams
        WHEN td.team_pk <= 9 THEN 8.5 -- Improving teams
        WHEN td.team_pk <= 11 THEN 6.5 -- Stable low
        WHEN td.team_pk <= 13 THEN 11.0 -- High scope churn
        ELSE 10.0 -- Other teams
    END as avg_velocity_4_sprints,
    CASE 
        WHEN td.team_pk <= 3 THEN (RANDOM() * 3)::INTEGER -- Stable high performers
        WHEN td.team_pk <= 5 THEN (RANDOM() * 4)::INTEGER -- Variable moderate
        WHEN td.team_pk <= 7 THEN (RANDOM() * 2)::INTEGER -- Declining teams
        WHEN td.team_pk <= 9 THEN (RANDOM() * 3)::INTEGER -- Improving teams
        WHEN td.team_pk <= 11 THEN 0 -- Stable low
        WHEN td.team_pk <= 13 THEN (RANDOM() * 6)::INTEGER -- High scope churn
        ELSE (RANDOM() * 3)::INTEGER -- Other teams
    END as story_points_added_during_sprint,
    CASE 
        WHEN td.team_pk <= 13 AND RANDOM() < 0.3 THEN 1 -- Occasional removals for scope churn teams
        ELSE 0
    END as story_points_removed_during_sprint,
    CASE 
        WHEN td.team_pk <= 5 AND RANDOM() < 0.4 THEN (RANDOM() * 3)::INTEGER -- Variable moderate
        WHEN td.team_pk <= 7 AND RANDOM() < 0.2 THEN (RANDOM() * 2)::INTEGER -- Declining teams
        WHEN td.team_pk <= 13 AND RANDOM() < 0.6 THEN (RANDOM() * 4)::INTEGER -- High scope churn
        ELSE 0
    END as story_points_modified_scope,
    CASE 
        WHEN td.team_pk <= 5 AND RANDOM() < 0.3 THEN 1 -- Variable moderate
        WHEN td.team_pk <= 13 AND RANDOM() < 0.4 THEN 1 -- High scope churn
        ELSE 0
    END as stories_with_modified_estimate,
    CASE 
        WHEN td.team_pk <= 3 THEN 3 -- Stable high performers
        WHEN td.team_pk <= 13 THEN 2 -- Most other teams
        ELSE 1 -- Some teams
    END as stories_completed,
    CASE 
        WHEN td.team_pk <= 3 THEN 3 -- Stable high performers
        WHEN td.team_pk <= 13 THEN 2 -- Most other teams
        ELSE 1 -- Some teams
    END as stories_estimated,
    CASE 
        WHEN td.team_pk <= 3 THEN 3 -- Stable high performers
        WHEN td.team_pk <= 13 THEN 2 -- Most other teams
        ELSE 1 -- Some teams
    END as total_stories
FROM "prism"."Team_Dim" td;

-- Generate data for all remaining sprints (4-26 and 28-52)
-- This creates realistic patterns for all 48 remaining sprints
INSERT INTO "prism"."Sprint_Metrics_Fact" 
(sprint_pk, team_pk, committed_story_points_completed, committed_story_points, 
 story_points_completed, story_points_committed_at_start, avg_velocity_3_sprints, avg_velocity_4_sprints,
 story_points_added_during_sprint, story_points_removed_during_sprint, story_points_modified_scope,
 stories_with_modified_estimate, stories_completed, stories_estimated, total_stories)
SELECT 
    sd.sprint_pk,
    td.team_pk,
    -- Generate realistic variations based on team performance profile and sprint progression
    CASE 
        WHEN td.team_pk <= 3 THEN GREATEST(16, LEAST(22, 19 + (RANDOM() * 6 - 3)::INTEGER + (sd.sprint_pk % 3 - 1))) -- Stable high performers with slight variation
        WHEN td.team_pk <= 5 THEN GREATEST(8, LEAST(18, 13 + (RANDOM() * 8 - 4)::INTEGER + (sd.sprint_pk % 4 - 2))) -- Variable moderate with more variation
        WHEN td.team_pk <= 7 THEN GREATEST(3, LEAST(12, 7 + (RANDOM() * 6 - 3)::INTEGER - (sd.sprint_pk % 5))) -- Declining teams get worse over time
        WHEN td.team_pk <= 9 THEN GREATEST(6, LEAST(15, 9 + (RANDOM() * 6 - 3)::INTEGER + (sd.sprint_pk % 4))) -- Improving teams get better over time
        WHEN td.team_pk <= 11 THEN GREATEST(3, LEAST(8, 6 + (RANDOM() * 3 - 1)::INTEGER)) -- Stable low performers
        WHEN td.team_pk <= 13 THEN GREATEST(6, LEAST(16, 11 + (RANDOM() * 8 - 4)::INTEGER)) -- High scope churn teams
        ELSE GREATEST(5, LEAST(17, 11 + (RANDOM() * 10 - 5)::INTEGER + (sd.sprint_pk % 3 - 1))) -- Other teams with seasonal variation
    END as committed_story_points_completed,
    CASE 
        WHEN td.team_pk <= 3 THEN 20 + (sd.sprint_pk % 3) -- Stable high performers
        WHEN td.team_pk <= 5 THEN 15 + (sd.sprint_pk % 4) -- Variable moderate
        WHEN td.team_pk <= 7 THEN GREATEST(8, 12 - (sd.sprint_pk % 3)) -- Declining teams reduce commitment over time
        WHEN td.team_pk <= 9 THEN 10 + (sd.sprint_pk % 3) -- Improving teams increase commitment
        WHEN td.team_pk <= 11 THEN 7 + (sd.sprint_pk % 2) -- Stable low
        WHEN td.team_pk <= 13 THEN 12 + (sd.sprint_pk % 4) -- High scope churn
        ELSE 12 + (sd.sprint_pk % 3) -- Other teams
    END as committed_story_points,
    CASE 
        WHEN td.team_pk <= 3 THEN 20 + (RANDOM() * 4)::INTEGER + (sd.sprint_pk % 3) -- Stable high performers
        WHEN td.team_pk <= 5 THEN 15 + (RANDOM() * 6)::INTEGER + (sd.sprint_pk % 4) -- Variable moderate
        WHEN td.team_pk <= 7 THEN GREATEST(8, 12 - (sd.sprint_pk % 3)) -- Declining teams
        WHEN td.team_pk <= 9 THEN 10 + (RANDOM() * 4)::INTEGER + (sd.sprint_pk % 3) -- Improving teams
        WHEN td.team_pk <= 11 THEN 7 + (sd.sprint_pk % 2) -- Stable low
        WHEN td.team_pk <= 13 THEN 12 + (RANDOM() * 6)::INTEGER + (sd.sprint_pk % 4) -- High scope churn
        ELSE 12 + (RANDOM() * 5)::INTEGER + (sd.sprint_pk % 3) -- Other teams
    END as story_points_completed,
    CASE 
        WHEN td.team_pk <= 3 THEN 20 + (sd.sprint_pk % 3) -- Stable high performers
        WHEN td.team_pk <= 5 THEN 15 + (sd.sprint_pk % 4) -- Variable moderate
        WHEN td.team_pk <= 7 THEN GREATEST(8, 12 - (sd.sprint_pk % 3)) -- Declining teams
        WHEN td.team_pk <= 9 THEN 10 + (sd.sprint_pk % 3) -- Improving teams
        WHEN td.team_pk <= 11 THEN 7 + (sd.sprint_pk % 2) -- Stable low
        WHEN td.team_pk <= 13 THEN 12 + (sd.sprint_pk % 4) -- High scope churn
        ELSE 12 + (sd.sprint_pk % 3) -- Other teams
    END as story_points_committed_at_start,
    CASE 
        WHEN td.team_pk <= 3 THEN 19.0 + (sd.sprint_pk % 3) -- Stable high performers
        WHEN td.team_pk <= 5 THEN 14.0 + (sd.sprint_pk % 3) -- Variable moderate
        WHEN td.team_pk <= 7 THEN GREATEST(8.0, 11.0 - (sd.sprint_pk % 3)) -- Declining teams
        WHEN td.team_pk <= 9 THEN 8.0 + (sd.sprint_pk % 2) -- Improving teams
        WHEN td.team_pk <= 11 THEN 6.0 + (sd.sprint_pk % 2) -- Stable low
        WHEN td.team_pk <= 13 THEN 11.0 + (sd.sprint_pk % 3) -- High scope churn
        ELSE 10.0 + (sd.sprint_pk % 3) -- Other teams
    END as avg_velocity_3_sprints,
    CASE 
        WHEN td.team_pk <= 3 THEN 18.5 + (sd.sprint_pk % 3) -- Stable high performers
        WHEN td.team_pk <= 5 THEN 13.5 + (sd.sprint_pk % 3) -- Variable moderate
        WHEN td.team_pk <= 7 THEN GREATEST(7.0, 10.5 - (sd.sprint_pk % 3)) -- Declining teams
        WHEN td.team_pk <= 9 THEN 7.5 + (sd.sprint_pk % 2) -- Improving teams
        WHEN td.team_pk <= 11 THEN 5.5 + (sd.sprint_pk % 2) -- Stable low
        WHEN td.team_pk <= 13 THEN 10.5 + (sd.sprint_pk % 3) -- High scope churn
        ELSE 9.5 + (sd.sprint_pk % 3) -- Other teams
    END as avg_velocity_4_sprints,
    CASE 
        WHEN td.team_pk <= 3 THEN (RANDOM() * 4)::INTEGER -- Stable high performers
        WHEN td.team_pk <= 5 THEN (RANDOM() * 5)::INTEGER -- Variable moderate
        WHEN td.team_pk <= 7 THEN (RANDOM() * 3)::INTEGER -- Declining teams
        WHEN td.team_pk <= 9 THEN (RANDOM() * 4)::INTEGER -- Improving teams
        WHEN td.team_pk <= 11 THEN (RANDOM() * 2)::INTEGER -- Stable low
        WHEN td.team_pk <= 13 THEN (RANDOM() * 7)::INTEGER -- High scope churn
        ELSE (RANDOM() * 4)::INTEGER -- Other teams
    END as story_points_added_during_sprint,
    CASE 
        WHEN td.team_pk <= 13 AND RANDOM() < 0.3 THEN 1 -- Occasional removals for scope churn teams
        ELSE 0
    END as story_points_removed_during_sprint,
    CASE 
        WHEN td.team_pk <= 5 AND RANDOM() < 0.4 THEN (RANDOM() * 4)::INTEGER -- Variable moderate
        WHEN td.team_pk <= 7 AND RANDOM() < 0.2 THEN (RANDOM() * 3)::INTEGER -- Declining teams
        WHEN td.team_pk <= 13 AND RANDOM() < 0.6 THEN (RANDOM() * 5)::INTEGER -- High scope churn
        ELSE 0
    END as story_points_modified_scope,
    CASE 
        WHEN td.team_pk <= 5 AND RANDOM() < 0.3 THEN 1 -- Variable moderate
        WHEN td.team_pk <= 13 AND RANDOM() < 0.4 THEN 1 -- High scope churn
        ELSE 0
    END as stories_with_modified_estimate,
    CASE 
        WHEN td.team_pk <= 3 THEN 3 -- Stable high performers
        WHEN td.team_pk <= 13 THEN 2 -- Most other teams
        ELSE 1 -- Some teams
    END as stories_completed,
    CASE 
        WHEN td.team_pk <= 3 THEN 3 -- Stable high performers
        WHEN td.team_pk <= 13 THEN 2 -- Most other teams
        ELSE 1 -- Some teams
    END as stories_estimated,
    CASE 
        WHEN td.team_pk <= 3 THEN 3 -- Stable high performers
        WHEN td.team_pk <= 13 THEN 2 -- Most other teams
        ELSE 1 -- Some teams
    END as total_stories
FROM "prism"."Sprint_Dim" sd
CROSS JOIN "prism"."Team_Dim" td
WHERE sd.sprint_pk NOT IN (1, 2, 3, 27); -- Exclude the sprints we already have data for

-- Verify the data
SELECT 
    sd.sprint_abbr,
    td.team_name,
    smf.committed_story_points_completed,
    smf.committed_story_points,
    ROUND((smf.committed_story_points_completed::DECIMAL / smf.committed_story_points) * 100, 1) as say_do_ratio_pct,
    smf.story_points_completed,
    smf.story_points_committed_at_start,
    ROUND((smf.story_points_committed_at_start::DECIMAL / smf.avg_velocity_3_sprints) * 100, 1) as commitment_strength_3sprint_pct,
    smf.story_points_added_during_sprint,
    smf.story_points_removed_during_sprint,
    smf.story_points_modified_scope
FROM "prism"."Sprint_Metrics_Fact" smf
JOIN "prism"."Sprint_Dim" sd ON smf.sprint_pk = sd.sprint_pk
JOIN "prism"."Team_Dim" td ON smf.team_pk = td.team_pk
WHERE sd.sprint_pk IN (1, 2, 3, 27)
ORDER BY sd.sprint_pk, td.team_pk;

SELECT 'Sample fact table data with realistic patterns inserted successfully!' as status;
SELECT 'Total records: ' || COUNT(*) as record_count FROM "prism"."Sprint_Metrics_Fact";
