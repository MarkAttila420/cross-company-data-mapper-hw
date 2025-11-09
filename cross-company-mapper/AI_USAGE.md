# AI használat dokumentáció

## Használt eszközök
- Gemini - a feladat lefordítása angol nyelvre
- Gemini API - a kódban az AI funkciók megvalósítására
- Github Copilot (GPT-5 mini) - Kód generálás
- Claude - Rendszerterv

## Konkrét promptok

create the folder structure:
cross-company-mapper/
├── mapping-service/          # Java (Spring Boot)
├── validation-service/       # Python (Flask/FastAPI)
├── frontend/                 # React + TypeScript
├── docker-compose.yml
├── README.md
├── AI_USAGE.md
└── ARCHITECTURE.md

===================
create a .gitignore file for the react+typescript frontend, java, python backend

===================

do not write the solution yet, just make the dockerfiles, and the filestructure:
I need to build a microservice system with:
1. Mapping service in Java (Spring Boot) with these endpoints:
   - POST /mapping/suggest - Generate AI mapping suggestions
   - POST /mapping/transform - Transform data
   - GET /mapping/templates - List saved mappings
   
2. Validation service in Python (FastAPI) with:
   - POST /validate - Validate transformed data
   - GET /validate/rules - List validation rules
   
3. React TypeScript frontend for uploading JSONs and displaying mappings

Generate the basic project structure with Dockerfiles for each service.

===================

in the mapping-service:
Create Java POJOs for:
1. MappingRequest with fields: sourceFormat (Map), targetFormat (Map)
2. MappingResponse with field: mappings (List of FieldMapping)
3. FieldMapping with: sourcePath, targetPath, transformationType, confidence
4. TransformRequest with: sourceData, mappings
5. TransformResponse with: transformedData
```

### Step 3: Implement AI Integration

**File:** `OpenAIService.java`

**Prompt:**
```
Create a Java service class that calls OpenAI API to generate field mappings.
The prompt should be:
"Given SOURCE: {sourceFormat} and TARGET: {targetFormat}, 
generate JSON array of mappings with sourcePath, targetPath, 
transformationType (e.g., 'date_format', 'split_name'), and confidence (0-1)."

Use RestTemplate or HttpClient. Return parsed JSON.

===================

replace the openaiservice to use google gemi api

===================

add the required environment variables to the correct place, with a TODO value

===================

Create a Java service that:
1. Takes source data and mappings
2. Applies transformations like:
   - Date format conversion (1985-03-15 to 15/03/1985)
   - Name splitting (Nagy János to firstName: János, lastName: Nagy)
   - Phone format (+36301234567 to 06301234567)
3. Returns transformed data as Map

Use Jackson ObjectMapper for JSON manipulation.

===================

now implement the mapping controller to use the mappingservice aand gaminiaiservice

===================

Create a Dockerfile for Spring Boot application:
- Use openjdk:17-slim
- Copy jar file
- Expose port 8080
- Run the application

===================

### Step 1: Initialize FastAPI Project

**File structure:**
```
validation-service/
├── app/
│   ├── main.py
│   ├── models.py
│   ├── validators.py
│   └── rules.py
├── requirements.txt
└── Dockerfile
```

===================

Create Python functions to validate:
1. Age calculation from birthDate (must be >= 18)
2. Email format validation using regex
3. Phone number format validation
4. Required field checks
5. Return validation results with field name, valid (boolean), and error message

===================

## AI hibák és javítások
Hol kellett korrigálni az AI outputot és miért

- Nem korrigálás, de a .env-be az API kulcsot be kellett írni.
- Frontenden volt egy kis hiba amit gyorsan ki lehetett javítani.
- Frontenden rosz útvonalat használt, ez ki lett cserélve.

## Időmegtakarítás
Becslés: mennyi időt spórolt az AI használattal

Ha 100%-ban AI nélkül fejlesztettem volna, akkor több hétbe is beletelt volna a funkciók lefejlesztése. A frontend fejlesztésében is pár órát vagy talán 1-2 napot is segített, mivel ezelőtt nagyon keveset fejlesztettem React frontendet. Ha viszont AI-t csak úgy használtam volna, mint egy Senior fejlesztő segítségét, hogy csak akkor ha elakadok, akkor is legalább egy hét munkályát megspórolta.