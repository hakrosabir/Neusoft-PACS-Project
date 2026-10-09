# Contributing

Start with the [architecture](docs/ARCHITECTURE.md), [setup](docs/SETUP.md), and [known limitations](docs/LIMITATIONS.md). Keep contributions focused on a documented behavior or gap and explain what changed and how it was validated.

Use synthetic data. Do not commit scans, database exports, credentials, model weights, or notebook outputs. Run `python scripts/verify_portfolio.py` for static publication checks. Runtime or inference validation should report the environment, model revision, input provenance, and actual result separately.

The original application has no repository-wide open-source license grant. Third-party dependencies and models retain their own terms; see [attributions](THIRD_PARTY_NOTICES.md).
