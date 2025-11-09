# AI Usage Documentation

This file should document how AI was used in the project. Include the following sections:

## Used tools
- Gemini - translating the task from hungarian to english
- Github Copilot (GPT-5 mini) - Code generation
- Claude - System design

## Example prompts
- Provide at least 3 concrete prompts used to generate mapping suggestions, transformation rules, or validation rules.

create the folder structure:
cross-company-mapper/
├── mapping-service/          # Java (Spring Boot)
├── validation-service/       # Python (Flask/FastAPI)
├── frontend/                 # React + TypeScript
├── docker-compose.yml
├── README.md
├── AI_USAGE.md
└── ARCHITECTURE.md

==================
create a .gitignore file for the react+typescript frontend, java, python backend

=================
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

=================

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



===================



===================



===================



===================



===================



===================



===================



===================



===================



===================



===================



===================



===================



===================



===================



===================



===================



===================



===================

## Corrections made to AI outputs
- Describe where AI output needed manual correction and why.

When the .env was created with the environment variables for the api key, i had to write the api key obviously, because the copilot cannot know my api key.
On the frontend the wrong routes were used, so i had to fix it to use the correct ones.

## Time saved
- Estimate how much time AI saved for each task (e.g., scaffolding, mapping, tests).
