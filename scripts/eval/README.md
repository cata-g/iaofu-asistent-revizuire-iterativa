# Evaluarea automată

`run_eval.py` rulează lanțurile de versiuni dintr-un set (`problems/split.json`) prin aplicația pornită, o dată pentru fiecare variantă. Apoi compară rezultatele cu observațiile de referință din `chain.json` (PRD 10.1).

```
docker compose up -d
python scripts/eval/run_eval.py --set tuning                       # toate variantele
python scripts/eval/run_eval.py --set tuning --modes proposed       # doar sistemul propus
python scripts/eval/run_eval.py --reanalyse results/eval/<run>      # recalculează, fără apeluri LLM
```

Costul unei rulări complete pe setul de reglare (5 probleme × 3 versiuni × 3 variante) a fost de **0,015 $**. Fiecare trimitere în `proposed` sau `baseline_b` costă aproximativ 0,0005 $.

## Ce produce

Rezultatele ajung în `results/eval/<run-id>-<set>/`, director ignorat de git. O rulare pe care vreți s-o păstrați o adăugați explicit în git.

| Fișier | Conținut |
|---|---|
| `responses.jsonl` | fiecare răspuns al API-ului; din el se poate reface analiza fără apeluri LLM noi |
| `summary.json`, `summary.md` | metricile pe variantă |
| `observations.csv` | toate observațiile propuse de LLM, confirmate sau eliminate, cu motivul și potrivirea cu referința |
| `reference_matches.csv` | fiecare observație de referință și dacă a fost găsită |
| `baseline_b_review.csv` | răspunsurile Baseline B lângă referință, pentru etichetare manuală |
| `hints_review.csv` | indiciile de nivel 1 afișate, pentru verificarea manuală a scurgerilor |

## Metricile

| Metrică | Definiție |
|---|---|
| `precision` | observații afișate care se potrivesc cu o referință ÷ observații afișate |
| `recall` | referințe găsite ÷ referințe |
| `missedSeverityWeighted` | ponderea erorilor ratate, cu greutatea `high` = 3, `medium` = 2, `low` = 1 |
| `*Lenient` | la fel, dar `correctness` și `edge_case` se consideră același tip (ambele înseamnă răspuns greșit, iar granița e subiectivă) |
| `classificationAccuracy` | versiuni la care clasificarea (rezolvat, regresie, rămas) e identică cu cea dedusă din `expectedFailures` |
| `rejectedByReason` | efectul verificatorului: câte propuneri a eliminat și de ce |
| `leakRewrites` | observații ale căror indicii au trebuit rescrise de filtrul anti-scurgere |
| `costUsd`, `tokens`, `llmCalls` | din `llm_call` |
| `answersWithCode` | răspunsuri Baseline B care conțin un bloc de cod, adică probabil soluția |

**Potrivirea:** o observație se potrivește cu o referință dacă are același tip și o linie aflată la cel mult o linie distanță de liniile din referință.

**Baseline B** răspunde în text liber, deci precizia lui nu se poate calcula automat. Se completează coloanele `(manual)` din `baseline_b_review.csv`: afirmații adevărate, afirmații false și dacă dă soluția.

## Limite

- Modelele Claude actuale nu acceptă temperatură fixată. Reproductibilitatea vine din `responses.jsonl` și din exporturi, nu din rerulare.
- Rezultatele pe setul de reglare **nu** sunt cifrele pentru raport: lanțurile și referințele lui au fost scrise împreună cu promptul. Cifrele pentru raport vin din setul de test, rulat o singură dată, la final.
