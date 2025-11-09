"""Sample validation rules for the homework validation-service.

This module contains a tiny, easily-understood rule set that the
validators use. In later steps the rules can be expanded or replaced by
dynamic rule storage.
"""

from typing import Dict, Any


VALIDATION_RULES = {
    "age": {
        "min": 18,
        "max": 120,
        "message": "Age must be between 18 and 120"
    },
    "email": {
        "pattern": r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$',
        "message": "Invalid email format"
    },
    "phone": {
        "pattern": r'^\+?[0-9]{10,15}$',
        "message": "Invalid phone number"
    }
}


def get_rules() -> Dict[str, Any]:
    """Return the raw rules dictionary."""

    return VALIDATION_RULES


def list_rules() -> Dict[str, Any]:
    """Public helper used by the FastAPI endpoint to list rules."""

    return get_rules()
