# Fixtures

Date de test pentru felii, ca fiecare să poată lucra fără celelalte. Rezultatele de execuție sunt generate din rulări reale prin Piston, nu scrise de mână. Formatele sunt descrise în [`docs/contracte.md`](../docs/contracte.md).

| Fișier | Ce este | Folosit de |
|---|---|---|
| `two-sum/v1.py` | două bucle, corectă, O(n²) | toți |
| `two-sum/v2.py` | dicționar, dar adaugă elementul înainte să caute; pică pe `[3, 2, 4]` | toți |
| `two-sum/harness-raw-v2.json` | ieșirea brută a `harness.py` pentru v2 (secțiunea 2.1) | #30 |
| `two-sum/report-v1.json`, `report-v2.json` | `TestReport` pentru v1 și v2 (secțiunea 2.2) | #31, #32 |
| `two-sum/classification-v1-v2.json` | clasificarea v1 → v2 (secțiunea 3) | #32 |
| `two-sum/observations-v2.json` | o observație confirmată și una eliminată (`same_output`) | #32 |
| `llm/observations-valid.json` | răspuns LLM valid; prima observație se confirmă, a doua nu | #31 |
| `llm/observations-invalid.json` | răspuns LLM care nu respectă schema (tip, linie, input, număr de indicii) | #31 |
| `llm/baseline-b-response.md` | răspuns tipic Baseline B: explică și dă soluția | #31 |

Regenerare: `python scripts/check_problem.py problems/two-sum --run fixtures/two-sum/v2.py` afișează ieșirea brută a harness-ului.
