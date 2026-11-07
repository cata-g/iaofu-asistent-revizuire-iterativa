# Asistent de revizuire iterativă pentru exerciții de algoritmică

Proiect pentru cursul *Inteligență artificială orientată către factorul uman* (tema 18), Master Știința Datelor, Politehnica București, 2026-2027.

Un student rezolvă o problemă de algoritmică în mai multe versiuni. La fiecare versiune, asistentul arată ce s-a rezolvat, ce regresie a apărut și ce a rămas, prin indicii gradate care nu dau soluția. LLM-ul propune ipoteze, iar o observație ajunge la student doar dacă un contraexemplu rulat o confirmă.

Documente:

- [`docs/PRD.md`](docs/PRD.md): cerințele;
- [`docs/contracte.md`](docs/contracte.md): formatele comune și API-ul;
- [`CONTEXT.md`](CONTEXT.md): deciziile și starea proiectului.

## Cerințe

- **Docker Desktop**, cu Docker Compose v2.
- **Pe Windows: WSL 2.5 sau mai nou.** Piston are nevoie de cgroup v2. Cu WSL 2.3 (kernel 5.15), containerul se oprește cu `Cgroup v2 not found`. Rezolvarea e `wsl --update`, urmată de `wsl --shutdown` și de repornirea Docker Desktop.
- Pentru dezvoltare fără Docker: Java 21, Node 22 și Python 3.10+, ultimul doar pentru scripturi.

## Pornire

```
cp .env.example .env      # completează ANTHROPIC_API_KEY
docker compose up --build
```

| Serviciu | Adresă |
|---|---|
| Aplicația | http://localhost:5173 |
| Backend | http://localhost:8080/api/health |
| Piston | http://localhost:2000/api/v2/runtimes |

La prima pornire, containerul `piston-init` instalează Python 3.12 în Piston, ceea ce durează aproximativ un minut.

Varianta testată se alege din URL: `http://localhost:5173/?mode=baseline_a&participant=P3`. Valorile pentru `mode` sunt `proposed` (implicit), `baseline_a` și `baseline_b`. Aplicația păstrează parametrii pentru tab-ul curent și îi scoate din bara de adrese, deci participantul nu vede varianta.

## Studiul cu utilizatori

```
python scripts/study_links.py <P1> <P2> <P3> --base-url http://<ip>:5173
```

Scriptul afișează, pentru fiecare participant, linkurile celor trei probleme, în ordinea rotită din PRD 10.2. Fiecare link se deschide într-un **tab nou**. În același tab, aplicația reia sesiunea deschisă anterior pentru acea problemă.

După studiu, jurnalul se descarcă ca CSV:

```
curl -O http://localhost:8080/api/export/sessions.csv
# la fel: submissions, observations, hint_reveals, feedback, llm_calls
```

## Evaluarea automată

```
python scripts/eval/run_eval.py --set tuning
```

Runner-ul rulează lanțurile de versiuni prin toate variantele și calculează metricile din PRD 10.1. Detalii în [`scripts/eval/README.md`](scripts/eval/README.md).

## Dezvoltare

```
# backend cu H2 în memorie; Piston trebuie să ruleze
docker compose up -d piston piston-init
cd backend && ./mvnw spring-boot:run

# frontend cu reîncărcare automată, cu /api trimis la localhost:8080
cd frontend && npm install && npm run dev
```

### Teste

```
sh scripts/test-backend.sh          # testele backend în Docker, fără JDK local
cd frontend && npm run build        # verificarea tipurilor și build-ul frontend
```

CI-ul (`.github/workflows/ci.yml`) rulează ambele la fiecare PR.

### Probleme noi

Fiecare problemă are un director `problems/<id>/` cu `problem.json`, `reference.py` și `inputs.py`. Formatul e descris în [`docs/contracte.md`](docs/contracte.md#1-problema). Verificarea, cu Piston pornit:

```
python scripts/check_problem.py problems/<id>
python scripts/check_problem.py problems/<id> --run versiune.py   # rulează o soluție
```

## Variabile de mediu

| Variabilă | Implicit | Rol |
|---|---|---|
| `ANTHROPIC_API_KEY` | (lipsă) | cheia LLM, doar în `.env`; fără ea, aplicația merge doar cu rezultatele testelor |
| `LLM_MODEL` | `claude-haiku-5-5` | modelul pentru observații și Baseline B |
| `LLM_BUDGET_USD` | `1.00` | plafonul de cost, adunat din tabela `llm_call`; după atingerea lui se afișează doar rezultatele testelor |
| `PISTON_URL` | `http://localhost:2000` | |
| `PROBLEMS_DIR` | `../problems` | |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | H2 în memorie | în compose: PostgreSQL |

## Structura

```
backend/    Spring Boot 4, Java 21; promptul în src/main/resources/prompts/
frontend/   React, Vite, TypeScript, Monaco
problems/   setul de probleme (enunț, teste, referință, generator, lanțul de versiuni) și split.json
fixtures/   date de test pentru dezvoltare
scripts/    verificarea problemelor, testele în Docker, evaluarea, linkurile pentru studiu
docs/       PRD și contracte
```
