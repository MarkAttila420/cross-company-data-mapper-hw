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
            # Accept ISO YYYY-MM-DD or common dd/MM/YYYY formats produced by the transformer
            try:
                dob = date.fromisoformat(birthdate)
            except Exception:
                try:
                    dob = datetime.strptime(birthdate, "%d/%m/%Y").date()
                except Exception:
                    raise
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

    # Helper: recursively search nested dicts for a key (first occurrence)
    def _find_key(obj: Any, key_names: List[str]):
        """Return a tuple (found_key_name, value) for the first matching key name in the nested object.

        Searches dicts and lists recursively. Returns (None, None) if nothing found.
        """
        if obj is None:
            return (None, None)
        if isinstance(obj, dict):
            for k, v in obj.items():
                if k in key_names:
                    return (k, v)
            # not at this level, search deeper
            for v in obj.values():
                fk, fv = _find_key(v, key_names)
                if fk is not None:
                    return (fk, fv)
        elif isinstance(obj, list):
            for item in obj:
                fk, fv = _find_key(item, key_names)
                if fk is not None:
                    return (fk, fv)
        return (None, None)

    # Build a lightweight lookup that prefers top-level fields but will
    # also find commonly-named fields nested anywhere in the payload.
    lookup: Dict[str, Any] = {}
    # copy top-level keys
    for k, v in data.items():
        lookup[k] = v

    # fields we know how to validate by name (and common variants)
    # we search nested structures if the top-level key is not present
    search_map = {
        "birthDate": ["birthDate", "dateOfBirth", "dob"],
        "email": ["email", "emailAddress", "emailAddr"],
        "phone": ["phone", "phoneNumber", "mobile", "PrimaryPhone"]
    }

    for canonical, variants in search_map.items():
        if canonical not in lookup:
            fk, fv = _find_key(data, variants)
            if fk is not None:
                lookup[canonical] = fv

    # Content-based fallback: if email or phone still not found, scan values for
    # obvious candidates (an '@' containing string for email, or a digit-heavy
    # string for phone). This helps when transformed payloads place fields under
    # unexpected names.
    def _scan_for_values(obj: Any):
        """Yield all string values found anywhere in obj."""
        if obj is None:
            return
        if isinstance(obj, dict):
            for v in obj.values():
                yield from _scan_for_values(v)
        elif isinstance(obj, list):
            for item in obj:
                yield from _scan_for_values(item)
        elif isinstance(obj, str):
            yield obj

    # attempt to find an email-like string if not present
    if "email" not in lookup:
        for s in _scan_for_values(data):
            if isinstance(s, str) and _EMAIL_RE.match(s):
                lookup["email"] = s
                break

    # attempt to find a phone-like string if not present
    if "phone" not in lookup:
        for s in _scan_for_values(data):
            if isinstance(s, str):
                digits = re.sub(r"\D", "", s)
                if 7 <= len(digits) <= 15:
                    lookup["phone"] = s
                    break

    # required fields
    required = rules.get("required_fields", [])
    results.extend(check_required_fields(lookup, required))

    # type expectations (keep these but report structured results)
    types = rules.get("types", {})
    for field, expected in types.items():
        if field in lookup:
            val = lookup[field]
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

    # domain-specific checks using the lookup that includes nested discoveries
    if "birthDate" in lookup:
        results.append(validate_age(lookup.get("birthDate"), field_name="birthDate"))

    if "email" in lookup:
        results.append(validate_email(lookup.get("email"), field_name="email"))

    # accept phone or phoneNumber keys (lookup already normalizes common variants to 'phone')
    if "phone" in lookup:
        results.append(validate_phone(lookup.get("phone"), field_name="phone"))

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

