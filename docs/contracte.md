# Contracte

Contractele comune ale proiectului: formatul problemelor, rezultatele execuției, observațiile, API-ul și modelul de date. Fiecare felie (#30, #31, #32) lucrează pe ele și pe datele din [`fixtures/`](../fixtures).

> **Regula:** un contract se schimbă doar prin PR aprobat de toți trei. Orice PR care modifică acest fișier spune explicit în descriere că e o schimbare de contract.

Cuprins:

1. [Problema](#1-problema)
2. [Execuția și rezultatele testelor](#2-execuția-și-rezultatele-testelor)
3. [Clasificarea versiunilor](#3-clasificarea-versiunilor)
4. [Observația](#4-observația)
5. [Răspunsul LLM](#5-răspunsul-llm)
6. [API](#6-api)
7. [Modelul de date](#7-modelul-de-date)
8. [Ce nu ajunge niciodată la frontend](#8-ce-nu-ajunge-niciodată-la-frontend)

---

## 1. Problema

Fiecare problemă are un director propriu în `problems/<id>/`, cu trei fișiere:

| Fișier | Conținut | Ajunge la frontend? |
|---|---|---|
| `problem.json` | enunț, exemple, semnătură, constrângeri, teste, configurare | doar câmpurile publice (vezi mai jos) |
| `reference.py` | soluția de referință, aceeași semnătură ca a studentului | **niciodată** |
| `inputs.py` | `generate(n, rng)` pentru inputurile mari și `is_valid(args)` pentru inputurile propuse de LLM | **niciodată** |

Codul Python stă în fișiere separate ca să poată fi rulat și verificat direct. O problemă nouă se verifică cu:

```
python scripts/check_problem.py problems/<id>
```

Scriptul confirmă că referința trece toate testele, că termină fiecare n mare în limita de timp și că fiecare test și exemplu e valid pentru `is_valid`.

### 1.1 `problem.json`

| Câmp | Tip | Public | Descriere |
|---|---|---|---|
| `id` | string | da | identic cu numele directorului, kebab-case |
| `title` | string | da | titlul afișat |
| `category` | string | da | `hashing`, `two-pointers`, `sorting`, `stack`, `binary-search`, `graphs`, `dp` |
| `difficulty` | string | da | `easy` sau `medium` |
| `statement` | string | da | enunțul, în Markdown |
| `examples` | listă | da | `{ input, output, explanation? }`, unde `input` e un obiect cu numele parametrilor |
| `signature` | obiect | da | `{ function, params: [{ name, type }], returns }` |
| `constraints` | listă de string | da | constrângerile, afișate ca listă |
| `timeLimitMs` | int | da | limita de timp pentru un apel pe inputul mare |
| `comparator` | string | nu | cum se compară răspunsurile: `exact`, `unordered` (lista de pe primul nivel nu contează ca ordine), `unordered_nested` (nici listele interioare) |
| `forbiddenTerms` | listă | **nu** | `{ term, allowedIfCode? }`, termenii interziși în indiciile de nivel 1 și 2 |
| `tests` | listă | **nu** | `{ id, group, input, expected }`, cu `group` = `basic` sau `edge` |
| `large` | obiect | **nu** | `{ sizes: [100, 1000, 10000, 100000], seed }` |

Codul de pornire din editor (`starterCode`) nu se scrie în fișier. Backend-ul îl generează din semnătură:

```python
def two_sum(nums: list[int], target: int) -> list[int]:
    pass
```

Floats se compară rotunjite la 6 zecimale, iar mulțimile (`set`) ca liste sortate.

**`forbiddenTerms`:** potrivirea se face pe cuvinte întregi, fără să țină cont de majuscule. Când `allowedIfCode` e o expresie regulată care se potrivește cu codul studentului, termenul devine permis. Motivul: dacă studentul folosește deja un dicționar, un indiciu care spune „dicționar” nu dezvăluie nimic nou.

Exemplul complet: [`problems/two-sum/`](../problems/two-sum).

## 2. Execuția și rezultatele testelor

### 2.1 Cum rulează codul

Backend-ul trimite la Piston (`POST /api/v2/execute`, Python 3.12.0) trei fișiere:

- `harness.py`, punctul de intrare, aflat în [`backend/src/main/resources/harness/harness.py`](../backend/src/main/resources/harness/harness.py);
- `solution.py`, codul care se testează;
- `inputs.py`, din directorul problemei.

Jobul ajunge pe stdin, ca JSON. Rezultatul vine ca o singură linie, prefixată cu `__HARNESS_RESULT__`, așa că un `print` din codul studentului nu-l poate strica.

Fiecare apel al funcției are propria limită de timp, impusă cu `signal.setitimer`. De aceea o buclă infinită pe un test afectează doar acel test (F7). Testele de bază și cazurile limită au 2 s pe test. Inputurile mari au `timeLimitMs` pe apel.

| Mod | Ce face | Folosit de |
|---|---|---|
| `evaluate` | rulează testele de bază și cazurile limită, apoi n = 10², 10³, 10⁴, 10⁵. Se oprește la primul timeout, iar n-urile mai mari sunt marcate `skipped` | fiecare trimitere și referința (o dată per problemă, păstrată în cache) |
| `single` | rulează un singur input | verificatorul prin contraexemplu |
| `validate` | rulează `inputs.is_valid(input)` | verificatorul, înainte să ruleze un input propus de LLM |

Inputul mare se generează în sandbox, cu `generate(n, random.Random(seed + n))`. Pe fiecare n, funcția se rulează de până la 3 ori și se păstrează timpul minim. Corectitudinea pe inputul mare se verifică prin digest: hash-ul răspunsului normalizat al studentului se compară cu hash-ul răspunsului dat de referință.

### 2.2 `TestReport`: ce returnează execuția

```json
{
  "status": "ok",
  "error": null,
  "summary": { "passed": 12, "total": 14 },
  "tests": [
    { "id": "basic-1", "group": "basic", "passed": true, "timeMs": 0.004 },
    {
      "id": "edge-half-target-once", "group": "edge", "passed": false,
      "input": { "nums": [3, 2, 4], "target": 6 },
      "expected": [1, 2], "actual": [0, 0], "error": null, "timeMs": 0.003
    },
    { "id": "large-10000", "group": "large", "passed": false, "input": "n = 10000 (generat)", "error": "timeout" }
  ],
  "timings": [
    { "n": 100, "status": "ok", "timeMs": 0.194 },
    { "n": 1000, "status": "ok", "timeMs": 21.55 },
    { "n": 10000, "status": "timeout", "timeMs": null },
    { "n": 100000, "status": "skipped", "timeMs": null }
  ],
  "slope": 2.05,
  "efficiency": "too_slow"
}
```

| Câmp | Valori |
|---|---|
| `status` | `ok`, sau `compile_error` (eroare de sintaxă, funcție lipsă), caz în care `error` conține mesajul și `tests` e gol |
| `tests[].group` | `basic`, `edge`, `large` (`large-<n>` per dimensiune) |
| `tests[]` trecut | doar `id`, `group`, `passed`, `timeMs` |
| `tests[]` picat | în plus `input`, `expected`, `actual`, `error` (F5). La `large`, `input` e o descriere și lipsește `expected` |
| `error` | `"timeout"` sau `"line 4: IndexError: list index out of range"` (linia din `solution.py`) |
| `timings[].status` | `ok`, `timeout`, `error`, `wrong` (răspuns greșit pe inputul mare), `skipped` |
| `slope` | panta regresiei pe scară log-log (log₁₀ timp față de log₁₀ n), calculată pe punctele `ok` sau `wrong`; `null` dacă sunt mai puțin de două |
| `efficiency` | `ok` dacă toate n-urile au terminat; `too_slow` dacă a apărut vreun timeout; `unknown` dacă nu s-a ajuns la inputurile mari (`compile_error`) |

Un test `large-<n>` trece dacă `timings[n].status == "ok"` și digest-ul coincide cu cel al referinței. Testele `large` intră în `summary`, iar diferențele de eficiență apar astfel și în clasificare.

## 3. Clasificarea versiunilor

`VersionClassifier` compară `TestReport`-ul versiunii anterioare cu cel al versiunii curente, test cu test, după `id`. Rezultatul e determinist și nu folosește LLM-ul (F9).

| Categorie | Regula |
|---|---|
| `resolved` | picat în versiunea anterioară, trecut acum |
| `regression` | trecut în versiunea anterioară, picat acum |
| `remaining` | picat în ambele |

Pentru prima versiune, `classification` e `null`. Dacă versiunea anterioară avea `compile_error`, toate testele picate acum sunt `remaining`.

```json
{
  "previousVersion": 1,
  "resolved":   [{ "testId": "large-10000", "group": "large" }, { "testId": "large-100000", "group": "large" }],
  "regression": [{ "testId": "edge-same-value-twice", "group": "edge" }],
  "remaining":  [],
  "efficiency": { "before": "too_slow", "after": "ok" }
}
```

Exemplul complet: [`fixtures/two-sum/classification-v1-v2.json`](../fixtures/two-sum/classification-v1-v2.json).

## 4. Observația

O observație e o ipoteză a LLM-ului despre o problemă din cod. Ajunge la student doar dacă e confirmată.

| Câmp | Tip | Descriere |
|---|---|---|
| `id` | uuid | |
| `type` | string | `correctness`, `edge_case`, `efficiency` |
| `line` | int sau null | linia din codul studentului |
| `evidenceInput` | obiect sau null | inputul-dovadă, cu numele parametrilor; `null` la `efficiency` |
| `risk` | string | ce se poate întâmpla, într-o frază |
| `hints` | listă de 3 string | nivelurile 1, 2, 3 (vezi PRD 6.5) |
| `status` | string | `confirmed` sau `rejected` |
| `rejectReason` | string sau null | `invalid_input`, `same_output`, `reference_error`, `not_slow`, `leak`, `duplicate` |
| `studentOutput` | JSON | ce a returnat codul studentului pe `evidenceInput` (sau eroarea) |
| `expectedOutput` | JSON | ce a returnat referința; se arată studentului doar ca „rezultatul corect”, nu codul referinței |

**Verificarea prin contraexemplu (F12):**

- `correctness` și `edge_case`: inputul trebuie să treacă `is_valid` (altfel `invalid_input`). Apoi se rulează codul studentului și referința pe input. Observația e confirmată dacă rezultatele normalizate diferă sau dacă studentul dă eroare ori timeout. Altfel e `same_output`.
- `efficiency`: confirmată doar dacă `TestReport.efficiency == "too_slow"`; altfel `not_slow`. Dovada afișată e timpul măsurat („pe n = 10 000 depășește 1 s”).
- După verificare, filtrul anti-scurgere verifică nivelurile 1 și 2. O observație care încă scurge după 2 regenerări e `rejected` cu `leak`.
- Două observații confirmate pe aceeași linie și de același tip: a doua e `duplicate`.

Studentul vede cel mult 3 observații confirmate. Observațiile eliminate se salvează în jurnal, dar nu se trimit la frontend.

**Ce primește frontend-ul (`ObservationView`):**

```json
{
  "id": "6f1c…",
  "type": "edge_case",
  "line": 4,
  "evidence": { "input": { "nums": [3, 2, 4], "target": 6 }, "actual": [0, 0], "expected": [1, 2] },
  "risk": "Același element poate fi folosit de două ori, deci răspunsul indică un singur index.",
  "hints": [{ "level": 1, "text": "Pe [3, 2, 4] primești [0, 0]. Ce index apare de două ori?" }],
  "levelsAvailable": 3,
  "verdict": null
}
```

`hints` conține doar nivelurile deblocate (F15). Nivelurile 2 și 3 vin doar prin `next-hint`.

## 5. Răspunsul LLM

Generatorul de observații cere modelului JSON cu schema de mai jos, prin ieșire structurată. Dacă răspunsul nu se poate parsa sau nu respectă schema, cererea se repetă o dată. Dacă nici a doua oară nu merge, studentul vede doar rezultatele testelor (N3).

```json
{
  "observations": [
    {
      "type": "edge_case",
      "line": 4,
      "evidenceInput": { "nums": [3, 2, 4], "target": 6 },
      "risk": "…",
      "hints": ["nivel 1", "nivel 2", "nivel 3"]
    }
  ]
}
```

- Cel mult 3 observații. `hints` are exact 3 elemente, iar `type` e unul dintre cele trei tipuri.
- La `efficiency`, `evidenceInput` e `null`.
- Exemple: [`fixtures/llm/observations-valid.json`](../fixtures/llm/observations-valid.json) și [`fixtures/llm/observations-invalid.json`](../fixtures/llm/observations-invalid.json).

**Baseline B** trimite același model cu enunțul și codul, sub întrebarea „ce e greșit aici?”. Fără teste, diff, verificare sau niveluri. Răspunsul e text liber (Markdown), afișat ca atare.

Fiecare apel se jurnalizează în `llm_call`, cu tokenii și costul (N4). Un plafon de cost (`LLM_BUDGET_USD`) oprește apelurile, iar după atingerea lui studentul vede doar rezultatele testelor.

## 6. API

Toate rutele sunt sub `/api`, cu JSON. Erorile au forma `{ "error": "mesaj" }`, cu statusurile 400 (input invalid), 404 (resursă inexistentă) și 409 (sesiune terminată).

### `GET /api/problems?category=&difficulty=`

Lista de probleme (F1), fără teste și fără soluții.

```json
[{ "id": "two-sum", "title": "Suma a două numere", "category": "hashing", "difficulty": "easy" }]
```

### `GET /api/problems/{id}`

Doar câmpurile publice (F2, F3), plus `starterCode`.

```json
{
  "id": "two-sum", "title": "…", "category": "hashing", "difficulty": "easy",
  "statement": "…", "examples": [ … ], "constraints": [ … ], "timeLimitMs": 1000,
  "signature": { "function": "two_sum", "params": [ … ], "returns": "list[int]" },
  "starterCode": "def two_sum(nums: list[int], target: int) -> list[int]:\n    pass\n"
}
```

### `POST /api/sessions`

```json
{ "problemId": "two-sum", "mode": "proposed", "participantId": "P3" }
```

- `mode` poate fi `proposed`, `baseline_a` sau `baseline_b`. Implicit e `proposed`.
- `participantId` e opțional și se folosește în studiu.
- Răspunsul nu conține `mode` (F18):

```json
{ "sessionId": "b2a9…", "problemId": "two-sum" }
```

### `POST /api/sessions/{id}/submissions`

```json
{ "code": "def two_sum(nums, target): …" }
```

Răspunsul are aceeași formă în toate variantele. Câmpurile pe care o variantă nu le folosește vin `null` sau goale:

```json
{
  "submissionId": "…",
  "version": 2,
  "report": { "...": "TestReport, secțiunea 2.2" },
  "classification": { "...": "secțiunea 3; null la v1 și în baseline_a / baseline_b" },
  "observations": [ { "...": "ObservationView, secțiunea 4; gol în baseline_a și baseline_b" } ],
  "assistantMessage": "text Markdown, doar în baseline_b; altfel null",
  "llmStatus": "ok"
}
```

`llmStatus` poate fi:

- `ok`;
- `not_used` (baseline_a, sau toate testele trec);
- `failed` (JSON invalid de două ori, sau eroare API);
- `budget_exceeded`.

### `POST /api/observations/{id}/next-hint`

Fără body. Deblochează nivelul următor și îl salvează în `hint_reveal`, cu momentul cererii.

```json
{ "level": 2, "text": "Contează ordinea în care adaugi și cauți în dicționar?", "levelsAvailable": 3 }
```

Dacă nivelul 3 e deja deblocat, răspunsul e 409.

### `POST /api/observations/{id}/feedback`

```json
{ "verdict": "useful" }
```

`verdict` poate fi `useful` sau `false`. Se poate schimba: ultimul marcaj e cel valabil, dar toate se păstrează în jurnal. Răspunsul e 204.

### `POST /api/sessions/{id}/finish`

Studentul marchează problema ca terminată. Ruta nu era în PRD 8.3; e adăugată pentru pasul 6 din fluxul de lucru. Răspunsul e 204, iar după asta trimiterile primesc 409.

### `GET /api/sessions/{id}/history`

Versiunile sesiunii, cu codul lor (F10). Diff-ul vizual îl face Monaco `DiffEditor` între două versiuni alese.

```json
{
  "sessionId": "…", "problemId": "two-sum", "finished": false,
  "versions": [
    { "version": 1, "submissionId": "…", "code": "…", "createdAt": "2026-11-07T18:00:00Z",
      "summary": { "passed": 12, "total": 14 }, "efficiency": "too_slow",
      "classification": null, "observations": [ … ] }
  ]
}
```

### `GET /api/export/{table}.csv`

Exportul jurnalului pentru analiză. `table` poate fi `sessions`, `submissions`, `observations`, `hint_reveals`, `feedback` sau `llm_calls`. Exportul include și observațiile eliminate, deci e un endpoint de cercetare, nu unul pentru student.

## 7. Modelul de date

PostgreSQL în compose, H2 la dezvoltare. Schema e creată de Flyway, în `backend/src/main/resources/db/migration`. Coloanele JSON sunt `text`, ca să meargă identic pe ambele baze.

```
session        id uuid PK, problem_id, mode, participant_id, created_at, finished_at
submission     id uuid PK, session_id FK, version, code, report_json, classification_json,
               passed, total, efficiency, slope, assistant_message, llm_status,
               duration_ms, created_at                       UNIQUE (session_id, version)
observation    id uuid PK, submission_id FK, type, line, evidence_input_json, risk, hints_json,
               status, reject_reason, student_output_json, expected_output_json,
               leak_retries, shown, created_at
hint_reveal    id uuid PK, observation_id FK, level, revealed_at
feedback       id uuid PK, observation_id FK, verdict, created_at
llm_call       id uuid PK, submission_id FK nullable, purpose, model, input_tokens,
               output_tokens, cost_usd, latency_ms, success, error, created_at
```

- `observation.shown` e `true` pentru observațiile confirmate trimise la student (cel mult 3).
- `hint_reveal` are un rând pentru nivelul 1, scris când observația e afișată, și câte un rând pentru fiecare `next-hint`.
- `llm_call.purpose` poate fi `observations`, `observations_retry`, `leak_regeneration` sau `baseline_b`.

## 8. Ce nu ajunge niciodată la frontend

- `reference.py`, `inputs.py`, `tests[].expected` pentru testele trecute, `forbiddenTerms`, `comparator`, `large.seed`.
- Nivelurile de indiciu nedeblocate.
- Observațiile `rejected`.
- `mode`-ul sesiunii.

Excepție (F5): un test picat își arată inputul, rezultatul așteptat și rezultatul obținut, ca pe platformele de exerciții. Un input-dovadă confirmat își arată rezultatul corect.
