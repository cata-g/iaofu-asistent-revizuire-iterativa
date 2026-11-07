# CONTEXT: Asistent de revizuire iterativă pentru exerciții de algoritmică

Acest fișier mută în repo tot contextul din conversația de planificare, ca dezvoltarea să poată continua în Claude Code. Specificația completă e în [`docs/PRD.md`](docs/PRD.md); aici sunt deciziile, motivele lor, starea proiectului și ce urmează.

## 1. Cursul și tema

- Curs: **Inteligență artificială orientată către factorul uman**, Master Știința Datelor, anul I, semestrul I, 2026-2027, Politehnica București.
- Tema: **18 – Revizuirea codului cu indicii pentru autor**. Textul temei: asistentul găsește probleme concrete într-o modificare de cod, explică riscul și oferă un indiciu, fără să rescrie automat proiectul; autorul marchează sugestiile utile și pe cele false. Date: 20-30 de modificări mici cu observații de referință și teste. Evaluare: precizia observațiilor, severitatea erorilor ratate, rata de acceptare, capacitatea autorului de a rezolva singur.
- Echipă de 3. Proprietarul repo-ului: Cătălin (`cata-g`).
- Cerințe comune pentru orice temă: utilizator, sarcină și decizie rămasă la om; o componentă IA evaluabilă și o variantă de referință simplă (un apel la LLM fără evaluare nu ajunge); set de test separat de cel de reglare; test cu utilizatori dacă e fezabil (concluzii exploratorii); proveniența datelor, limite, cost estimat și cum se corectează un rezultat.
- Grila notei finale: 30% componenta IA și comparația cu referința, 25% adecvarea pentru utilizator și controlul oferit, 25% evaluarea și analiza erorilor, 10% reproductibilitate, 10% demonstrație și claritatea raportului.

## 2. Ideea, într-o frază

Un student rezolvă o problemă de algoritmică în mai multe versiuni; la fiecare versiune, asistentul arată ce s-a rezolvat, ce regresie a apărut și ce a rămas, prin indicii gradate care nu dau soluția. **LLM-ul e un generator de ipoteze: faptele vin din execuția codului, iar o observație ajunge la student doar dacă un contraexemplu rulat o confirmă.**

## 3. Decizii luate și de ce

| Decizie | De ce |
|---|---|
| „Modificarea de cod” = diferența dintre versiuni succesive ale aceluiași student (v1 → v2) | Se potrivește direct cu formularea temei; de confirmat cu profesorul |
| Probleme de tip LeetCode, nu proiecte reale | Avem teste și o soluție de referință, deci adevăr de bază măsurabil |
| Set **predefinit** de probleme, scrise de echipă | Verificatorul are nevoie de soluția de referință și de teste ascunse; fără probleme de drepturi |
| Verificare prin **contraexemplu** | Atacă riscul principal (observații false) și face diferența față de un simplu wrapper peste LLM |
| Clasificarea rezolvat / regresie / rămas e **deterministă** (din teste), nu din LLM | LLM-ul doar o formulează |
| Indicii pe **3 niveluri**, după cât din soluție dezvăluie (nu după gravitate) | Păstrează autonomia studentului; metrică: niveluri cerute până la rezolvare |
| Filtru anti-scurgere cu termeni interziși per problemă | Indiciile de nivel 1-2 nu au voie să numească tehnica |
| Baseline A = doar teste (ca LeetCode); Baseline B = același LLM cu „ce e greșit aici?” | Implementate în aceeași aplicație, cu același model, ca diferențele să vină doar din tipul de feedback; produsele reale nu permit control și jurnalizare |
| Studiu within-subjects: fiecare participant testează toate cele 3 variante, pe 3 probleme similare, în ordine rotită (6 ordini) | Cu 4-6 participanți, diferențele dintre oameni ar acoperi diferențele dintre variante |
| Stack: React + Vite + TS + `@monaco-editor/react`; Spring Boot; Piston în Docker; PostgreSQL | Alegerea echipei; Monaco are DiffEditor și decorații pe linii |
| Piston **găzduit local** | API-ul public Piston nu mai e liber din 15 februarie 2026; containerul cere `--privileged` și cgroup v2 |
| Doar Python, fără autentificare, probleme în fișiere | Limitează prima versiune la un flux complet |
| Termenul „versiuni”, nu „tranziții” | Decizie de vocabular: `VersionClassifier`, „clasificatorul de versiuni” |
| Muncă pe **felii verticale**, nu pe straturi | Fiecare membru face puțin din toate și poate lucra asincron |

## 4. Starea proiectului (7 noiembrie 2026)

Gata:

- PRD în [`docs/PRD.md`](docs/PRD.md).
- 9 issues deschise, **#29-#37**, toate asignate momentan lui `cata-g`; fiecare spune ce trebuie să meargă, de ce depinde, ce cerințe din PRD acoperă și criteriile de acceptare. Issues-urile vechi #1-#28 sunt închise ca „not planned” (prea granulare și prea legate între ele).
- Label-uri de etapă: `fezabilitate`, `etapa-1`, `control`, `etapa-2`, `test-utilizatori`, `final`. Label-urile pe straturi (`frontend`, `backend`, `ia`, `date`, `evaluare`, `infra`, `docs`) mai există doar pentru issues-urile vechi.
- Board GitHub Project creat de Cătălin, cu issues-urile importate.
- În afara repo-ului: documentul Google Docs al propunerii și un plan pentru Etapa 1 cu prezentare de 7 slide-uri.

Cod: **încă nimic**. Primul pas e #29.

| Issue | Ce | Cine |
|---|---|---|
| #29 | Fundația și contractele | toți, o sesiune |
| #30 | Execuție și Baseline A, cap-coadă | A |
| #31 | Observații verificate și Baseline B, cap-coadă | B |
| #32 | Versiuni și indicii gradate, cap-coadă | C |
| #33 | Setul de probleme și lanțurile de versiuni | toți, câte o treime |
| #34 | Integrare: fluxul complet v1 → v2 | primul care termină |
| #35 | Evaluarea automată și analiza erorilor | neasignat |
| #36 | Studiul cu utilizatori | toți |
| #37 | Livrabile: Etapa 1, Etapa 2, Final | toți |

## 5. Primul task în Claude Code: #29

Rezultatul: după #29, fiecare felie (#30, #31, #32) poate porni doar cu `fixtures/`.

1. Schelet: `backend/` (Spring Boot, Java 21, Maven), `frontend/` (Vite + React + TS + `@monaco-editor/react`), `problems/`, `fixtures/`, `docs/`.
2. `docker-compose.yml` cu `piston` (privileged, Python instalat prin CLI-ul Piston), `postgres`, `backend`, `frontend` (nginx). Verificare: un `POST /api/v2/execute` cu `print(1)` merge.
3. `docs/contracte.md` cu:
   - schema problemei (enunț, exemple, semnătură, constrângeri, limită de timp, soluție de referință, teste de bază / limită / mari, categorie, dificultate, termeni interziși);
   - schema rezultatelor de test (pe test: trecut/picat, input, așteptat, obținut, eroare; timpi pe n = 10², 10³, 10⁴, 10⁵);
   - schema observației (id, tip `correctness | edge_case | efficiency`, linie, input-dovadă, risc, `hints[3]`, stare `confirmed | rejected`);
   - API-ul din PRD 8.3, cu request/response;
   - modelul de date: Session, Submission, Observation, HintReveal, Feedback.
4. `problems/two-sum.json`, complet, ca exemplu de referință.
5. `fixtures/`: rezultate de test pentru two sum v1 și v2, un răspuns LLM valid, unul invalid, perechea de versiuni v1/v2.
6. README cu `docker compose up`.

Exemplul two sum folosit peste tot în documentație:

- v1: două bucle, corectă, O(n²).
- v2: dicționar, dar adaugă elementul înainte să caute complementul → pică pe `[3, 2, 4]`, target 6, returnează `[0, 0]`.
- Clasificarea v1 → v2: rezolvat (complexitatea), regresie nouă (elementul folosit de două ori).
- Indicii pentru regresie: N1 „Pe [3, 2, 4] primești [0, 0]. Ce index apare de două ori?”; N2 „Contează ordinea în care adaugi și cauți în dicționar?”; N3 „Verifică complementul înainte să adaugi elementul curent.”

## 6. Convenții

- Un issue = un branch = un PR; fără push direct pe `main`. PR-ul închide issue-ul cu `Closes #n`.
- Contractele din `docs/contracte.md` se schimbă doar prin PR aprobat de toți trei.
- Documentația în română; codul, identificatorii și mesajele de commit în engleză.
- Cheia API pentru LLM doar din variabile de mediu (`.env`, ignorat de git); niciodată în cod.
- Soluțiile de referință și testele nu ajung niciodată în răspunsurile către frontend.
- Fără concluzii statistice din studiu: rezultate exploratorii (medie și interval pe variantă).

## 7. Calendar

| Săpt. | Reper |
|---|---|
| 4 | Fezabilitate: 5 probleme complete, Piston rulează, primul apel LLM |
| 5-6 | Etapa 1: plan de 2-4 pagini, prezentare de 5 minute |
| 7 | Control: Baseline A funcțional, setul de test fixat |
| 8-9 | Etapa 2: prototip, rezultate preliminare, demo de 6 minute |
| 10-11 | Test cu utilizatori |
| 12 | O îmbunătățire justificată de date, înghețarea funcțiilor |
| 13-14 | Final: cod, set de test, raport de 4-6 pagini, demo de 7 minute |

## 8. Întrebări deschise

- Confirmarea interpretării „modificării de cod” cu profesorul.
- SDK-ul LLM: SDK-ul oficial Anthropic pentru Java sau Spring AI.
- Biblioteca de componente: shadcn/ui sau MUI.
- Username-urile GitHub ale celor doi colegi și cine e A, B, C.
