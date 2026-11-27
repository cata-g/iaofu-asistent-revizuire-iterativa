# Scenariul de demo

Demo de 6-7 minute, pe problema two sum. Ideea de transmis: **asistentul explică problema și arată dovada, dar lasă rezolvarea studentului**, iar un chat AI obișnuit (Baseline B) dă direct soluția.

## Înainte de prezentare (10 minute înainte)

1. `docker compose up --build`, apoi verificați http://localhost:5173. Lista trebuie să aibă 20 de probleme.
2. **Ctrl+F5** pe pagină, ca browserul să încarce ultima versiune a aplicației.
3. Faceți o trimitere de probă pe altă problemă decât two sum, ca Piston și LLM-ul să fie „încălzite”. Prima rulare a fiecărei probleme e puțin mai lentă.
4. Deschideți **două taburi noi**: fiecare tab are sesiunea lui, deci pornește curat.
   - Tab 1: http://localhost:5173/problems/two-sum?mode=proposed
   - Tab 2: http://localhost:5173/problems/two-sum?mode=baseline_b
5. Țineți acest fișier deschis alături: codul de lipit e la final.
6. Verificați conexiunea la internet (LLM-ul are nevoie de ea).

## Desfășurarea

| Timp | Ce arătați | Ce spuneți |
|---|---|---|
| 0:00-0:40 | Lista de probleme | Problema: LeetCode spune doar „Wrong Answer”, iar un chat AI îți dă soluția. Noi vrem feedback care explică, arată dovada și lasă rezolvarea studentului. 20 de probleme, 7 categorii; filtrați după „Grafuri”. |
| 0:40-1:00 | Tab 1, two sum | Enunțul, exemplele, editorul cu semnătura. Varianta (`mode`) a dispărut din bara de adrese: participantul la studiu nu știe ce variantă testează. |
| 1:00-2:00 | **v1** (două bucle) → Trimite | Testele mici trec, inputurile mari depășesc timpul. Tabelul de timpi și panta (~n²) vin din execuție, nu din LLM. Observația de eficiență e confirmată de măsurătoare: „La n = 10 000 depășește limita”. |
| 2:00-3:45 | **v2** (dicționar, cu bug) → Trimite | **Clasificarea** față de v1: rezolvat (inputurile mari), regresie nouă (`[3, 3]`), rămas. E deterministă, din teste. **Observația**: linia evidențiată în editor, dovada „pică pe `nums = [3,3]`: obținut `[0,0]`, corect `[0,1]`”. Dovada a fost rulată: o observație ajunge la student doar dacă un contraexemplu o confirmă. Apăsați **„Mai mult ajutor”** de două ori: nivelul 1 e o întrebare, nivelul 2 numește ideea, nivelul 3 tehnica. Studentul decide câte cere. Marcați **„Utilă”**. |
| 3:45-4:15 | **Soluția corectă** → Trimite | 14 din 14. LLM-ul nu mai e apelat când totul trece. Butonul **„Am terminat”**: decizia de a încheia e a studentului. |
| 4:15-4:50 | Tab-ul **Versiuni** | Lista versiunilor și diff-ul între oricare două (v2 → v3). Fiecare versiune, observație, indiciu deblocat și marcaj e în jurnal, exportabil CSV pentru analiză. |
| 4:50-6:00 | Tab 2 (**Baseline B**), lipiți **v2** → Trimite | Același model, întrebat simplu „ce e greșit aici?”. Răspunsul conține de obicei codul corectat: studentul nu mai rezolvă nimic. Pe setul de reglare, 15 din 15 răspunsuri au conținut cod. Fără dovadă rulată, fără niveluri. |
| 6:00-6:40 | Încheiere | Trei variante în aceeași aplicație, cu același model: sistemul propus, Baseline A (doar teste, ca LeetCode), Baseline B. Urmează evaluarea automată pe setul de test (precizia observațiilor, scurgeri, cost ~0,0005 $ pe trimitere) și studiul cu utilizatori. |

## Dacă ceva nu merge

| Situație | Ce faceți |
|---|---|
| La v2 nu apare nicio observație | LLM-ul nu e determinist; uneori propunerile nu trec verificarea. Spuneți exact asta: „observațiile neconfirmate sunt eliminate, nu ajung la student”. Arătați testul picat cu inputul `[3, 3]` din rezultate și continuați. Dacă vreți observația, retrimiteți v2. |
| Apare „Asistentul nu a putut genera observații” | LLM-ul nu răspunde (internet sau cheie). Rezultatele testelor și clasificarea merg în continuare: arătați-le și spuneți că sistemul se degradează la Baseline A, nu se blochează. |
| Trimiterea durează mult | Normal 5-13 s cu LLM. Folosiți pauza ca să explicați ce se întâmplă: rulare izolată în Piston, apoi LLM, apoi verificarea prin contraexemplu. |
| Pagina arată ciudat sau vechi | Ctrl+F5. |
| Nu merge deloc | `docker compose ps`; dacă un serviciu e oprit, `docker compose up -d`. |

## Codul de lipit

**v1**, corectă dar lentă:

```python
def two_sum(nums: list[int], target: int) -> list[int]:
    for i in range(len(nums)):
        for j in range(i + 1, len(nums)):
            if nums[i] + nums[j] == target:
                return [i, j]
    return []
```

**v2**, rapidă, dar adaugă elementul înainte să caute complementul:

```python
def two_sum(nums: list[int], target: int) -> list[int]:
    seen = {}
    for i, x in enumerate(nums):
        seen[x] = i
        if target - x in seen:
            return [seen[target - x], i]
    return []
```

**Soluția corectă:**

```python
def two_sum(nums: list[int], target: int) -> list[int]:
    seen = {}
    for i, x in enumerate(nums):
        if target - x in seen:
            return [seen[target - x], i]
        seen[x] = i
    return []
```

Pentru o variantă mai lungă sau o a doua problemă, fiecare problemă are un lanț de versiuni greșite în `problems/<id>/chain/` (de exemplu `number-of-islands`, cu un DFS recursiv care cade pe grila mare).
