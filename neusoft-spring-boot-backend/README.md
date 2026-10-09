# Java application API

Java 21 / Spring Boot 3.4.12 backend for users, appointments, reports, analysis records, system logs, patient-context chat, and doctor PDF RAG. MyBatis Plus accesses MySQL; Ollama supplies local inference and embeddings.

See [setup](../docs/SETUP.md), [architecture](../docs/ARCHITECTURE.md), and [API reference](../docs/API.md). Database credentials come from the process environment or an ignored local Spring profile. `.env.example` is documentation, not an automatically loaded file.

Authentication remains a prototype: BCrypt verifies login passwords, but API authorization is permissive and returned tokens are placeholders. See [limitations](../docs/LIMITATIONS.md).
