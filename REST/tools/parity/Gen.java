import com.prism.api.metrics.*;
import java.nio.file.*;
import java.util.*;

/** Reads cases (metricKey \t groupPk \t filterPk=value \x1f ...) and writes the NEW-path SQL as PREPARE/EXECUTE. */
public class Gen {
    public static void main(String[] a) throws Exception {
        var levels = new HashMap<Integer, FocusLevelDefinition>();
        for (var line : Files.readAllLines(Path.of("levels.tsv"))) {
            var f = line.split("\t");
            levels.put(Integer.parseInt(f[0]), new FocusLevelDefinition(Integer.parseInt(f[0]), Integer.parseInt(f[1]), f[2], f[3]));
        }
        // Metric rows exactly as seeded by SQL/012.
        var metrics = Map.of(
            "saydo", new MetricDefinition(1, "Say/Do Ratio", MetricType.PERCENTAGE, "smf.committed_story_points_completed", "smf.committed_story_points", null, "Say/Do Ratio", "%", 2, null, null, null, true),
            "velocity", new MetricDefinition(2, "Velocity", MetricType.AVERAGE, "smf.avg_velocity_3_sprints", null, null, "Velocity", "points", 2, null, null, null, true),
            "churn", new MetricDefinition(3, "Scope Churn", MetricType.PERCENTAGE, "smf.story_points_added_during_sprint + smf.story_points_removed_during_sprint", "GREATEST(smf.committed_story_points, smf.story_points_completed)", null, "Scope Churn", "%", 2, "fit", null, null, true),
            "estchanged", new MetricDefinition(4, "Estimate Changed", MetricType.PERCENTAGE, "smf.stories_with_modified_estimate", "smf.total_stories", null, "Estimate Changed", "%", 2, "fit", null, null, true),
            "estimated", new MetricDefinition(5, "% Estimated", MetricType.PERCENTAGE, "smf.stories_estimated", "smf.total_stories", null, "% Estimated", "%", 2, null, null, null, true));
        var out = new StringBuilder();
        int n = 0;
        for (var line : Files.readAllLines(Path.of("cases.tsv"))) {
            var f = line.split("\t", -1);
            var filters = new ArrayList<MetricQueryBuilder.ResolvedFilter>();
            if (!f[3].isEmpty()) for (var part : f[3].split("\u001f")) {
                var eq = part.indexOf('=');
                filters.add(new MetricQueryBuilder.ResolvedFilter(levels.get(Integer.parseInt(part.substring(0, eq))), part.substring(eq + 1)));
            }
            var q = MetricQueryBuilder.build(metrics.get(f[1]), levels.get(Integer.parseInt(f[2])), filters);
            var sql = q.sql();
            for (int i = 1; sql.contains("?"); i++) sql = sql.replaceFirst("\\?", "\\$" + i);
            out.append("\\echo ##CASE ").append(f[0]).append('\n');
            out.append("PREPARE q").append(n).append(" AS ").append(sql).append(";\n");
            out.append("EXECUTE q").append(n);
            if (!q.parameters().isEmpty()) {
                out.append('(');
                for (int i = 0; i < q.parameters().size(); i++) out.append(i == 0 ? "" : ", ").append('\'').append(q.parameters().get(i).replace("'", "''")).append('\'');
                out.append(')');
            }
            out.append(";\nDEALLOCATE q").append(n++).append(";\n");
        }
        Files.writeString(Path.of("new.sql"), out);
        System.out.println("generated " + n);
    }
}
