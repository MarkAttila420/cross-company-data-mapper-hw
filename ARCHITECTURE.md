# Architecture Decisions (one page)

Architektúra döntések.

## Services
- mapping-service: Java (Spring Boot). Azért ezt választottam, mivel ez az amelyiket a legjobban ismerem, amelyikben a legtöbb tapasztalatom van.
- validation-service: Python. Azért, mert ebben van a második legtöbb tapastzalatom, és ennek egyszerűbb a struktúrája.
- frontend: React + TypeScript. A frontendnek ez volt megadva, de más frontend is használható lett volna, mivel nem kellett összetett frontendet fejleszteni, csak pár gomb és szerkeszthető mező

## AI
- Gemini API lett használva, mivel ez ingyenes és elterjedt.
- AI mapping javaslatok generálására
- Validációs szabályók generálására (kifutottam az időből, így ez végül nem lett megvlósítva, pár promt és kevés idő és megvalósítható lett volna)

## Továbbfejlesztési javaslatok
- A mapping minták eltárolása adatbázisban (pl. PostgreSQL)
- Tesztek
- Dokumentáció
- Kód részletesebb átvizsgálása, és hibák kijavítása, amik a POC alatt nem jöttek elő
