from typing import Any, Dict, List, Optional

from pydantic import BaseModel


class ValidationRequest(BaseModel):
    """Request model for validation endpoint.

    Contains a single JSON object to be validated.
    """

    data: Dict[str, Any]


class ValidationResult(BaseModel):
    """Single field validation result."""

    field: str
    valid: bool
    error: Optional[str] = None


class ValidationResponse(BaseModel):
    """Response model for validation results."""

    valid: bool
    errors: List[ValidationResult] = []
