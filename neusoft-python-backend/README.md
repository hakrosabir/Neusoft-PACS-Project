# Python imaging engine

The main `app.py` combines lung, brain MRI, spleen CT, volume conversion, and preview/viewer endpoints. Java manages application records; this engine manages imaging artifacts.

Use [setup](../docs/SETUP.md), [API reference](../docs/API.md), and [known limitations](../docs/LIMITATIONS.md). Bundles, weights, uploads, and generated cases are excluded from Git. The original separate spleen backend is a local development copy; the public entry point is this combined engine.

Process environment variables control storage paths, bundle paths, host, port, and debug mode. `.env.example` documents them but is not loaded automatically. Default binding is `127.0.0.1:5000`, with debug disabled.
