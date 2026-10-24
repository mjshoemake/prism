# AI workflow record

Prism is built with AI assistants (Claude, earlier also Cursor) working inside explicit rules: see
[`REST/CLAUDE.md`](../../REST/CLAUDE.md) for the conventions and guardrails, and the
[root README](../../README.md#how-this-was-built) for how the work is organized.

| File | What it is |
|---|---|
| [`original-vision.txt`](original-vision.txt) | The first description of what Prism should do: pick a metric, choose a focus area, drill in by clicking bars. Written before any code. |
| [`session-summary-db-design.md`](session-summary-db-design.md) | **Start here.** A readable summary of the session that designed the STAR schema and generated the demo data. |
| [`transcript-db-design.md`](transcript-db-design.md) | The full transcript of that session (long). |
| [`transcript-cursor-auth-and-ux.md`](transcript-cursor-auth-and-ux.md) | Full transcript of a Cursor session on authentication and UX (Nov 2025, long). |

The transcripts are kept as a record of how the AI was directed and how its work was checked; most readers will
want the summary and the conventions file instead.
