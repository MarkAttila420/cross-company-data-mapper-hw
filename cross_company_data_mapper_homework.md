# Cross-Company Data Mapper - Otthoni Feladat

## 📋 Áttekintés
**Pozíció:** Full-Stack Developer - FluentaOne  
**Időkeret:** 3-4 óra  
**Szint:** POC/Prototípus (nem production-ready kód várható!)

## 🎯 Feladat célja
Demonstráld a képességeidet az AI-vezérelt fejlesztésben és mikroszerviz architektúra építésében egy valós üzleti probléma megoldásán keresztül: különböző vállalati rendszerek közötti intelligens adatmapping.

## 📖 Szcenárió
Két vállalat szeretne ügyféladatokat megosztani egymással:
- **BankCorp**: Hagyományos banki formátum (XML-szerű struktúra)
- **InsureTech**: Modern biztosító (REST/JSON)

A feladatod egy POC szintű rendszer építése, amely AI segítségével automatikusan map-peli a különböző formátumokat.

## 🔧 Kötelező komponensek

### 1. **Mapping Service** (Backend - választott nyelv)
- Két különböző adatformátum fogadása
- AI-alapú field mapping javaslatok generálása
- Transformation rules végrehajtása
- Mapping template-ek tárolása (in-memory elég)

**Minimális funkciók:**
- POST `/mapping/suggest` - AI javaslatok generálása
- POST `/mapping/transform` - Adattranszformáció végrehajtása
- GET `/mapping/templates` - Mentett mapping-ek listázása

### 2. **Validation Service** (Backend - MÁSIK nyelv kötelező!)
- Transzformált adatok validálása
- Egyszerű business rules (pl. életkor > 18)
- Format ellenőrzés

**Minimális funkciók:**
- POST `/validate` - Adat validálás
- GET `/validate/rules` - Aktív szabályok listázása

### 3. **Simple Dashboard** (React + TypeScript)
- Két különböző JSON feltöltése
- AI mapping javaslatok megjelenítése
- Manuális felülbírálás lehetősége
- Transzformáció eredményének preview

### 4. **Infrastructure**
- Docker Compose az összes szolgáltatáshoz
- Egy paranccsal indítható: `docker-compose up`

## 📊 Példa adatformátumok

### BankCorp Format (XML-style JSON):
```json
{
  "Customer": {
    "CIF": "12345",
    "PersonalData": {
      "FullName": "Nagy János",
      "BirthDate": "1985-03-15",
      "TaxID": "8765432109"
    },
    "ContactInfo": {
      "PrimaryPhone": "+36301234567",
      "EmailAddr": "nagy.janos@email.hu"
    },
    "Address": {
      "PostalCode": "1011",
      "City": "Budapest",
      "StreetAddress": "Fő utca 1."
    }
  }
}
```

### InsureTech Format (Modern JSON):
```json
{
  "client": {
    "id": "INS-98765",
    "personal": {
      "firstName": "János",
      "lastName": "Nagy",
      "dateOfBirth": "15/03/1985",
      "nationalId": "8765432109"
    },
    "contact": {
      "mobile": "06301234567",
      "email": "nagy.janos@email.hu"
    },
    "location": {
      "zip": "1011",
      "city": "Budapest",
      "street": "Fő utca",
      "number": "1"
    }
  }
}
```

## 🤖 AI használati követelmények

### Kötelezően használandó AI funkciók:

1. **Intelligent Field Mapping**
```javascript
// Példa prompt struktura:
const mappingPrompt = `
Given these two data structures:

SOURCE FORMAT:
${JSON.stringify(sourceFormat, null, 2)}

TARGET FORMAT:
${JSON.stringify(targetFormat, null, 2)}

Generate field mappings with:
1. Source path -> Target path
2. Transformation needed (if any)
3. Confidence score (0-1)

Return as JSON array.
`;
```

2. **Data Transformation Suggestions**
- Az AI javasoljon transformation rule-okat (pl. dátum formátum konverzió)
- Kezelje a név szétbontást/összevonást

3. **Validation Rules Generation**
- AI generáljon validation szabályokat a mező típusok alapján

### AI_USAGE.md kötelező tartalma:
```markdown
# AI Használat Dokumentáció

## Használt eszközök
- [Eszköz neve]: [Mire használta]
- Példa: Cursor: Backend service boilerplate generálás
- Példa: Claude: Architektúra tervezés és code review

## Konkrét promptok
[Legalább 3 példa a használt promptokból]

## AI hibák és javítások
[Hol kellett korrigálni az AI outputot és miért]

## Időmegtakarítás
[Becslés: mennyi időt spórolt az AI használattal]
```

## ⚙️ Technikai elvárások

### Minimális követelmények:
- ✅ Legalább 2 különböző backend nyelv
- ✅ React + TypeScript frontend
- ✅ AI API integráció (OpenAI/Claude/Gemini/Groq)
- ✅ Docker Compose
- ✅ RESTful API design

### Opcionális extrák:
- WebSocket real-time mapping status
- Batch processing képesség
- Export/import mapping templates
- Swagger/OpenAPI dokumentáció

## 📦 Beadandók

1. **GitHub Repository** a következő struktúrával:
```
project/
├── mapping-service/      # Első backend nyelv
├── validation-service/   # Második backend nyelv
├── frontend/            # React app
├── docker-compose.yml
├── README.md           # Futtatási útmutató
├── AI_USAGE.md        # AI használat dokumentáció
└── ARCHITECTURE.md    # Döntések indoklása (max 1 oldal)
```

2. **Demo videó** (max 3 perc)
- Használj Loom-ot vagy hasonlót
- Mutasd be a működő rendszert
- Említsd meg az AI használatot

3. **Időkövetés**
- README-ben tüntesd fel a ráfordított időt komponensenként

## 💡 Tippek a sikerhez

### DO:
- 🚀 Kezdj egyszerűen - először csak 2-3 field mapping
- 📝 Dokumentáld az AI használatot real-time
- ⚡ Használj mock data-t, ne építs database-t
- 🎨 A UI lehet nagyon basic (nem design verseny)
- 🔄 Iterálj gyorsan AI segítségével

### DON'T:
- ❌ Ne próbálj minden edge case-t kezelni
- ❌ Ne írj unit teszteket (hacsak AI nem generálja automatikusan)
- ❌ Ne optimalizálj performance-ra
- ❌ Ne építs authentication-t (API key elég)

## 🚦 Gyors start útmutató

1. **Kezdd az AI-val való tervezéssel:**
```
"I need to build a microservice system for cross-company data mapping.
Services: mapping-service (Python), validation-service (Node.js), frontend (React).
Generate a basic project structure with Docker Compose."
```

2. **Építsd meg a core mapping logic-ot**
3. **Add hozzá az AI integrációt**
4. **Minimális UI csak a demo-hoz**
5. **Dokumentáld az AI használatot**

## ❓ Gyakori kérdések

**K: Melyik AI API-t használjam?**  
V: Bármelyik (OpenAI, Claude, Gemini, Groq). Használhatsz ingyenes tier-t vagy trial-t.

**K: Milyen nyelveket válasszak a backend-hez?**  
V: Azt amit ismersz + 1 amit AI-val tudsz kezelni. Példák: Python+Go, JavaScript+Elixir, Java+Rust.

**K: Kell automated testing?**  
V: Nem kötelező. Ha az AI generál teszteket "ingyen", mehet, de ne erre fordítsd az időt.

**K: Mi ha nem fér bele 4 órába?**  
V: Dokumentáld mi készült el és mi maradt ki. A pragmatizmus fontosabb mint a teljeskörűség.

---

**Beküldés:** GitHub repository link elküldése

**Sok sikert! Várjuk a kreatív megoldásokat! 🎯**