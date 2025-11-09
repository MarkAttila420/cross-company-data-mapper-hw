"""
Placeholder FastAPI app for validation-service.
Do not implement validation endpoints yet — this file provides a starter app.
"""
from fastapi import FastAPI

app = FastAPI(title="validation-service")

# TODO: add POST /validate and GET /validate/rules endpoints during implementation phase

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
