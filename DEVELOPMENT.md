# Development validation policy

Use only quick compilation, static inspection, and focused pure unit tests. Do not launch a Minecraft client, dedicated server, full-pack server, temporary server, GameTest runtime, or staged distribution.

For renderer changes, preserve nested render calls and compute item state once per top-level `ItemRenderer.render` invocation rather than once per quad or vertex.
