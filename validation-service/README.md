# validation-service (Python - FastAPI)

This folder will contain the Python validation service (FastAPI recommended).

Responsibilities:
- POST /validate -> validate transformed payloads
- GET  /validate/rules -> list active validation rules

Next steps:
1. Create a virtualenv and add `fastapi` + `uvicorn` + `pydantic` to requirements.
2. Implement endpoints and simple business rules (e.g., age > 18).
3. Add AI integration if generating validation rules automatically.
