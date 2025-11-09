# mapping-service (Java - Spring Boot)

This folder will contain the Java Spring Boot mapping service.

Responsibilities:
- POST /mapping/suggest  -> provide AI-based mapping suggestions
- POST /mapping/transform -> apply transformation rules
- GET  /mapping/templates -> list saved mapping templates

Next steps:
1. Initialize a Spring Boot project (Maven/Gradle) with web dependency.
2. Add controllers for the above endpoints.
3. Integrate AI client (e.g., call out to an LLM API) to generate mapping suggestions.
