"""Validation helpers used by the validation-service endpoints.

This module provides small, focused validators that return structured
validation results: dicts with keys `field`, `valid`, and `error`.
"""

from datetime import date, datetime
import re
from typing import Any, Dict, List

from .rules import get_rules


_EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")


def _type_name(value: Any) -> str:
    return type(value).__name__


def validate_age(birthdate: Any, field_name: str = "birthDate") -> Dict[str, Any]:
    """Validate that `birthdate` (ISO date string) corresponds to age >= 18.

    Accepts YYYY-MM-DD or a date/datetime instance.
    Returns a dict: {field, valid, error}.
    """

    if birthdate is None:
        return {"field": field_name, "valid": False, "error": "missing birthDate"}

    try:
        if isinstance(birthdate, (date, datetime)):
            dob = birthdate.date() if isinstance(birthdate, datetime) else birthdate
        elif isinstance(birthdate, str):
            dob = date.fromisoformat(birthdate)
        else:
            return {"field": field_name, "valid": False, "error": "unsupported birthDate type"}
    except Exception:
        return {"field": field_name, "valid": False, "error": "invalid date format, expected YYYY-MM-DD"}

    today = date.today()
    age = today.year - dob.year - ((today.month, today.day) < (dob.month, dob.day))
    if age >= 18:
        return {"field": field_name, "valid": True, "error": None}
    else:
        return {"field": field_name, "valid": False, "error": "must be at least 18 years old"}


def validate_email(email: Any, field_name: str = "email") -> Dict[str, Any]:
    """Validate `email` using a simple regex; returns structured result."""

    if email is None:
        return {"field": field_name, "valid": False, "error": "missing email"}
    if not isinstance(email, str):
        return {"field": field_name, "valid": False, "error": f"expected string for {field_name}, got {_type_name(email)}"}
    if _EMAIL_RE.match(email):
        return {"field": field_name, "valid": True, "error": None}
    return {"field": field_name, "valid": False, "error": "invalid email format"}


def validate_phone(phone: Any, field_name: str = "phone") -> Dict[str, Any]:
    """Validate phone numbers: allow optional +, digits and separators; ensure length reasonable."""

    if phone is None:
        return {"field": field_name, "valid": False, "error": "missing phone"}
    if not isinstance(phone, str):
        return {"field": field_name, "valid": False, "error": f"expected string for {field_name}, got {_type_name(phone)}"}

    # allow +, digits, spaces, dashes, parentheses
    if not re.match(r"^\+?[0-9\-\s\(\)]+$", phone):
        return {"field": field_name, "valid": False, "error": "invalid characters in phone number"}

    digits = re.sub(r"\D", "", phone)
    if len(digits) < 7 or len(digits) > 15:
        return {"field": field_name, "valid": False, "error": "phone number has invalid length"}

    return {"field": field_name, "valid": True, "error": None}


def check_required_fields(data: Dict[str, Any], required_fields: List[str]) -> List[Dict[str, Any]]:
    """Return a list of validation results for required fields that are missing."""

    results: List[Dict[str, Any]] = []
    for field in required_fields:
        if field not in data:
            results.append({"field": field, "valid": False, "error": "missing required field"})
        else:
            results.append({"field": field, "valid": True, "error": None})
    return results


def validate_payload(data: Dict[str, Any]) -> List[Dict[str, Any]]:
    """Validate `data` against the sample rules and return structured results.

    Results are a list of dicts with keys: field, valid, error.
    """

    rules = get_rules()
    results: List[Dict[str, Any]] = []

    # required fields
    required = rules.get("required_fields", [])
    results.extend(check_required_fields(data, required))

    # type expectations (keep these but report structured results)
    types = rules.get("types", {})
    for field, expected in types.items():
        if field in data:
            val = data[field]
            if expected == "int":
                if not isinstance(val, int):
                    results.append({"field": field, "valid": False, "error": f"expected int, got {_type_name(val)}"})
                else:
                    results.append({"field": field, "valid": True, "error": None})
            elif expected == "str":
                if not isinstance(val, str):
                    results.append({"field": field, "valid": False, "error": f"expected str, got {_type_name(val)}"})
                else:
                    results.append({"field": field, "valid": True, "error": None})

    # domain-specific checks
    if "birthDate" in data:
        results.append(validate_age(data.get("birthDate"), field_name="birthDate"))

    if "email" in data:
        results.append(validate_email(data.get("email"), field_name="email"))

    # accept phone or phoneNumber keys
    phone_key = "phone" if "phone" in data else ("phoneNumber" if "phoneNumber" in data else None)
    if phone_key:
        results.append(validate_phone(data.get(phone_key), field_name=phone_key))

    # deduplicate results by field, preferring invalid messages if multiple
    final: Dict[str, Dict[str, Any]] = {}
    for r in results:
        f = r.get("field")
        if f not in final:
            final[f] = r
        else:
            # if existing valid but new is invalid, overwrite
            if final[f].get("valid", True) and not r.get("valid", True):
                final[f] = r

    return list(final.values())

