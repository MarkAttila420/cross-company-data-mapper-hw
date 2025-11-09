"""validation-service FastAPI app (minimal starter).

This module exposes two lightweight endpoints used during development:
- GET /validate/rules  -> returns available validation rules
- POST /validate      -> validate a JSON payload against the sample rules

The actual validation logic lives in `validators.py` and rules are defined
in `rules.py`. Models are defined in `models.py`.
"""
from typing import Any, Dict

from fastapi import FastAPI

from .validators import validate_payload
from .rules import list_rules

app = FastAPI(title="validation-service")


@app.get("/validate/rules")
async def get_rules() -> Any:
    """Return available sample validation rules."""
    return list_rules()


@app.post("/validate")
async def validate_data(data: Dict[str, Any]):
    """Validate the incoming JSON payload using the sample rules.

    This endpoint accepts a raw JSON object (as a dict) and returns a
    JSON object with 'valid' and 'errors' keys. Errors is a list of
    per-field validation result objects: {field, valid, error}.
    """
    results = validate_payload(data)
    overall = all(item.get("valid", False) for item in results) if results else True
    return {"valid": overall, "errors": results}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
