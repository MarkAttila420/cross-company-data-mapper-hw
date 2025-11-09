# Cross-Company Mapper (POC)

Ez a repository egy proof-of-concept projektet tartalmaz, a cross-company data mapping otthoni feladat megoldását, ami a FluentaOne Full-Stack developer pozíció kiválasztási folyamatának egyik lépése.

## Létrehozott struktúra:

- `mapping-service/`    - Java (Spring Boot) service 
- `validation-service/` - Python (FastAPI) service 
- `frontend/`           - React + TypeScript app
- `example_data/`       - Példa JSON fájlok
- `docker-compose.yml`  - compose, ami a service-eket futtatjaa együtt
- `AI_USAGE.md`         - AI használat dokumentáció
- `ARCHITECTURE.md`     - Architekturális döntések rövid leírása
- `README.md`           - Futtatási útmutató, időrefárdítás
- `.gitignore`          - Gitignore fájl, mik ne legyenek feltöltve GitHubra
- `.env`                - Környezeti változók (nincs verziókezelve)
- `.env.example`        - A .env-be szükséges változók

## Időráfordítás:
- Leképező szolgáltatás: 1ó 15p
- Validációs szolgáltatás: 30p
- Frontend: 30p
- Integráció és infra: 30p
- A generált kód javítása: 2ó 15p

## Futtatás Docker Compose-al

- A .env.example tartalmát be kell másolnia .env-be, szükséges változókat kitölteni (GEMINI_API_KEY). 
- Ezután build és indítás:
    - `docker-compose up --build`
- Ezután a következő helyen lesznek elérhetők a rendszerek:
    - `frontend`: http://localhost:3000
    - `mapping-service`: http://localhost:8080
    - `validation-service`: http://localhost:8000