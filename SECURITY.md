# Security and data handling

This repository is an internship prototype for source review. Its API access rules and placeholder login tokens are not suitable for a public clinical deployment. See [known limitations](docs/LIMITATIONS.md).

Do not upload patient records, imaging files, account exports, credentials, or model datasets to issues or pull requests. Use synthetic examples and provide only the affected code path and a minimal reproduction.

For a security issue, contact [Sabir Ali](https://github.com/hakrosabir) through the contact links on the profile before disclosing sensitive details publicly. This repository does not promise a security response SLA.

Local database exports, imaging artifacts, model weights, and secrets are excluded through `.gitignore` and an explicit portfolio file selection. Before publication, run `python scripts/verify_portfolio.py` and manually review the selected files. Static checks do not guarantee that all sensitive content has been detected.
