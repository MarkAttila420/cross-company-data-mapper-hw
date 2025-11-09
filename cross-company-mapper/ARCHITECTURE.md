# Architecture Decisions (one page)

This document captures the high-level choices for the POC.

Services
- mapping-service: Java (Spring Boot). Chosen for strong JSON/XML tooling, and familiarity in enterprise environments.
- validation-service: Python (FastAPI). Chosen for quick prototyping, lightweight runtime and convenient data validation with Pydantic.
- frontend: React + TypeScript. Familiar for quick UI and type safety.

AI
- Use any modern LLM (OpenAI/GPT, Claude, Gemini). The AI will be used for:
  - Field mapping suggestions
  - Transformation rule suggestions (date formats, name splitting)
  - Validation rule generation

Infra
- Docker Compose to orchestrate the three services for the demo.
- In-memory storage for mapping templates (to keep the POC simple).

Tradeoffs
- No DB in POC to save time (templates in-memory). This means mappings are ephemeral across restarts.
- No auth (API key or local dev only), as per assignment constraints.

Next steps when moving beyond POC:
- Persist mapping templates in a small database (SQLite/Postgres).
- Add tests around mapping/validation logic.
- Add OpenAPI/Swagger for each service.
