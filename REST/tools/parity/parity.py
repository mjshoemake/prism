"""Parity check, part 1: builds the case list and the OLD-path SQL (see README.md)."""
import subprocess, random, itertools, collections, sys
import os, shlex
# Connection: override with PRISM_PSQL, e.g. PRISM_PSQL="psql -h localhost -p 5432 -U postgres -d playpen"
PSQL = shlex.split(os.environ.get("PRISM_PSQL", "psql -h localhost -p 5432 -U postgres -d playpen")) + ["-At", "-F", "\t", "-v", "ON_ERROR_STOP=1"]
def q(sql): return subprocess.run(PSQL + ["-c", sql], capture_output=True, text=True, check=True).stdout.splitlines()

FROM = '''FROM "prism"."Sprint_Metrics_Fact" smf
JOIN "prism"."Sprint_Dim" sd  ON smf.sprint_pk = sd.sprint_pk
JOIN "prism"."Team_Dim" td    ON smf.team_pk   = td.team_pk'''
levels = [l.split("\t") for l in q('select focus_level_pk, focus_area_pk, name, column_name, sort_order from prism."Focus_Levels" order by focus_area_pk, sort_order')]
open("levels.tsv", "w").write("".join("\t".join(l[:4]) + "\n" for l in levels))
byid = {int(l[0]): l for l in levels}
values = {}
for l in levels:
    values[int(l[0])] = q(f'select distinct {l[3]}::text {FROM} order by 1')

# "Old" expressions: Say/Do is the old UI's selectSql verbatim; the others are written by hand, independently of the builder.
OLD = {
 "saydo": 'ROUND((SUM(smf.committed_story_points_completed)::DECIMAL / SUM(smf.committed_story_points)::DECIMAL) * 100, 2) as "Say/Do Ratio"',
 "velocity": 'ROUND(AVG(smf.avg_velocity_3_sprints), 2) AS v',
 "churn": 'ROUND(SUM(smf.story_points_added_during_sprint + smf.story_points_removed_during_sprint)::DECIMAL / NULLIF(SUM(GREATEST(smf.committed_story_points, smf.story_points_completed)), 0) * 100, 2) AS v',
 "estchanged": 'ROUND(SUM(smf.stories_with_modified_estimate)::DECIMAL / NULLIF(SUM(smf.total_stories), 0) * 100, 2) AS v',
 "estimated": 'ROUND(SUM(smf.stories_estimated)::DECIMAL / NULLIF(SUM(smf.total_stories), 0) * 100, 2) AS v',
}
def old_sql(metric, group, filters):
    col = byid[group][3]
    # Old UI: buildWhereClause (quotes doubled), old Java: METRICS_SQL template.
    where = "" if not filters else "WHERE " + " and ".join(f"{byid[f][3]} = '{v.replace(chr(39), chr(39)*2)}'" for f, v in filters)
    return f"SELECT {col}, {OLD[metric]}\n{FROM}\n{where}\nGROUP BY {col}\nORDER BY {col}"

cases = []
def add(kind, metric, group, filters): cases.append((f"{len(cases)}:{kind}:{metric}", metric, group, filters))
rng = random.Random(20261021)
ids = sorted(byid)
next_level = {}
for area in {byid[i][1] for i in ids}:
    chain = [i for i in ids if byid[i][1] == area]
    chain.sort(key=lambda i: int(byid[i][4]))
    for a, b in zip(chain, chain[1:]): next_level[a] = b
def drill(metric, level, filters, depth):
    add("drill", metric, level, filters)
    if depth == 0 or level not in next_level: return
    for v in values[level]:
        drill(metric, next_level[level], filters + [(level, v)], depth - 1)
for metric in OLD:
    for g in ids: add("nofilter", metric, g, [])
    for g in ids:
        for f in ids:
            for v in values[f]: add("single", metric, g, [(f, v)])
    for start in ids: drill(metric, start, [], 2 if metric != "saydo" else 3)
    for _ in range(300):
        fl = rng.sample(ids, rng.choice([2, 3]))
        add("random", metric, rng.choice(ids), [(f, rng.choice(values[f])) for f in fl])
# Quote handling: a value containing an apostrophe (no rows match in the demo data, but both must agree).
for metric in OLD: add("quote", metric, 12, [(12, "O'Brien")])

with open("cases.tsv", "w") as fh:
    for cid, metric, group, filters in cases:
        fh.write(f"{cid}\t{metric}\t{group}\t" + "\x1f".join(f"{f}={v}" for f, v in filters) + "\n")
with open("old.sql", "w") as fh:
    for cid, metric, group, filters in cases:
        fh.write(f"\\echo ##CASE {cid}\n{old_sql(metric, group, filters)};\n")
print("cases:", len(cases), collections.Counter(c[0].split(":")[1] for c in cases))
