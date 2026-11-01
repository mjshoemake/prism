-- Hand-checked parity dataset (see ../README.md).
-- Loaded by fixture.sh into its own database, after every migration except the demo data (002–004). Since SQL/023
-- the STAR tables live in "demo_star", not in Prism's schema.
-- Every row exists to exercise one edge case; expected answers are in expected.tsv and worked out by hand
-- in README.md ("Hand-checked dataset").

-- Sprints: two in the same quarter, one in another quarter, and month 1 in two different years
-- (so "Month" (all years) and "Year & Month" disagree).
INSERT INTO "demo_star"."Sprint_Dim"
  (sprint_pk, sprint_month, sprint_year, sprint_quarter, year_month, year_quarter, sprint_num, sprint_name, sprint_abbr, sprint_start_date)
VALUES
  (1, 1, 2025, 1, '2025-01', '2025-Q1', 1, 'Sprint 25.01', 'S25.01', '2025-01-06'),
  (2, 2, 2025, 1, '2025-02', '2025-Q1', 2, 'Sprint 25.02', 'S25.02', '2025-02-03'),
  (3, 4, 2025, 2, '2025-04', '2025-Q2', 3, 'Sprint 25.04', 'S25.04', '2025-04-07'),
  (4, 1, 2026, 1, '2026-01', '2026-Q1', 4, 'Sprint 26.01', 'S26.01', '2026-01-05')
ON CONFLICT DO NOTHING;

-- Teams:
--   A and B share every level above Level 1  -> the 38% vs 57.5% example (OVERVIEW §4.1).
--   O'Brien Squad                            -> apostrophes in team and level names.
--   Team D                                   -> NULL Level 3 / 2 / 1 labels.
--   Team E                                   -> no fact rows at all (filtering on it = empty result).
INSERT INTO "demo_star"."Team_Dim" (team_pk, team_name, level_1_name, level_2_name, level_3_name, level_4_name, level_5_name)
VALUES
  (1, 'Team A',        'Lead Kim',    'Mgr Lee',     'Dept X', 'Division 1', 'Company'),
  (2, 'Team B',        'Lead Ray',    'Mgr Lee',     'Dept X', 'Division 1', 'Company'),
  (3, 'O''Brien Squad', 'Lead O''Neil', 'Mgr O''Brien', 'Dept X', 'Division 1', 'Company'),
  (4, 'Team D',        NULL,          NULL,          NULL,     'Division 2', 'Company'),
  (5, 'Team E',        'Lead Eve',    'Mgr Eve',     'Dept Y', 'Division 2', 'Company')
ON CONFLICT DO NOTHING;

-- Facts. Columns not used by any metric are filled with plausible values.
--   f1 + f2 : Team A 9/10 = 90 %, Team B 10/40 = 25 % in the same sprint -> Mgr Lee = 19/50 = 38 %.
--   f3      : Team A, a sprint with nothing committed -> every denominator is 0, velocity NULL.
--   f4 / f5 : O'Brien Squad and Team D both have velocity 15.50 (a tie).
--   f6 / f7 : month 1 of 2026 -> same team (A) in two different January sprints; f7 velocity NULL.
INSERT INTO "demo_star"."Sprint_Metrics_Fact"
  (fact_pk, sprint_pk, team_pk,
   committed_story_points, committed_story_points_completed, story_points_completed, story_points_committed_at_start,
   avg_velocity_3_sprints, avg_velocity_4_sprints,
   story_points_added_during_sprint, story_points_removed_during_sprint, story_points_modified_scope,
   stories_with_modified_estimate, stories_completed, stories_estimated, total_stories)
VALUES
  (1, 1, 1, 10,  9, 11, 10, 20.00, 20.00, 2, 1, 0, 1, 4,  4,  5),
  (2, 1, 2, 40, 10, 12, 40, 30.00, 30.00, 0, 0, 0, 0, 3, 10, 10),
  (3, 2, 1,  0,  0,  0,  0,  NULL,  NULL, 0, 0, 0, 0, 0,  0,  0),
  (4, 2, 3, 20, 15, 18, 20, 15.50, 15.50, 5, 0, 0, 2, 6,  8,  8),
  (5, 3, 4, 20, 10, 25, 20, 15.50, 15.50, 0, 5, 0, 3, 5,  6,  6),
  (6, 4, 1, 10, 10, 10, 10, 22.25, 22.25, 0, 0, 0, 0, 5,  5,  5),
  (7, 4, 3, 10,  5,  5, 10,  NULL,  NULL, 1, 1, 0, 0, 2,  3,  4)
ON CONFLICT DO NOTHING;
