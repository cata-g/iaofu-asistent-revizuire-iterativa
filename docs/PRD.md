# PRD: Asistent de revizuire iterativă pentru exerciții de algoritmică

Proiect pentru cursul Inteligență artificială orientată către factorul uman, tema 18 (Revizuirea codului cu indicii pentru autor), Master Știința Datelor, 2026-2027.

| | |
|---|---|
| Stare | Draft pentru Etapa 1 |
| Echipă | 3 membri, fiecare lucrează pe toate straturile (date, backend, IA, frontend, evaluare) |
| Document sursă | Google Doc „Asistent de revizuire iterativă pentru exerciții de algoritmică” |

## 1. Rezumat

Un student rezolvă o problemă de algoritmică în mai multe versiuni. La fiecare versiune, asistentul arată ce s-a rezolvat, ce regresie nouă a apărut și ce a rămas, prin indicii gradate care nu dau soluția. LLM-ul este folosit ca generator de ipoteze: faptele vin din execuția codului, iar fiecare afirmație despre o eroare este verificată prin contraexemplu înainte să ajungă la student.

## 2. Problema

Platformele de exerciții de tip LeetCode oferă feedback brut („Wrong Answer”, „Time Limit Exceeded”) care nu explică cauza. Un chat AI întrebat direct dă de obicei soluția completă, iar studentul nu mai rezolvă singur. Lipsește un feedback care explică problema, arată dovada și lasă rezolvarea în seama studentului, urmărind progresul de la o versiune la alta.

## 3. Utilizator și sarcină

**Utilizatorul** este un student care se pregătește pentru interviuri tehnice sau pentru un curs de structuri de date și algoritmi.

**Sarcina** este să ajungă de la o soluție greșită sau lentă la una corectă și eficientă, înțelegând de ce prima variantă nu era suficientă.

**Ce rămâne la decizia utilizatorului:**

- câte niveluri de indicii cere;
- ce observații acceptă, ignoră sau marchează ca false;
- când consideră problema terminată, fără aprobare automată;
- codul: asistentul nu rescrie și nu modifică niciodată soluția.

## 4. Obiective și non-obiective

### Obiective

- O1. Studentul ajunge la o soluție corectă și eficientă cu mai puține versiuni decât cu feedback brut (Baseline A).
- O2. Mai puține observații false decât un apel simplu la LLM (Baseline B).
- O3. Indiciile de nivel 1 și 2 nu dezvăluie soluția.
- O4. Studentul percepe că a rezolvat singur problema (autonomie percepută).
- O5. Proiectul pornește reproductibil cu o singură comandă.

### Non-obiective (în afara proiectului)

- Alte limbaje decât Python.
- Probleme introduse liber de student; setul de probleme este predefinit (vezi 7.1).
- Autentificare, conturi, panou de administrare.
- Evaluare cu valoare statistică; studiul cu utilizatori este explorator.

## 5. Fluxul de lucru

Acesta este fluxul minim care trebuie să funcționeze la demonstrația din săptămânile 8-9.

1. Studentul alege o problemă din set și trimite v1.
2. Sistemul rulează v1 pe testele ascunse, care includ cazuri limită și inputuri mari pentru măsurarea timpului.
3. Asistentul afișează 1-3 observații, fiecare cu tipul problemei (corectitudine, caz limită sau eficiență), dovada, riscul și un indiciu de nivel 1.
4. Studentul marchează observațiile ca utile sau false și trimite v2.
5. Asistentul compară v1 cu v2 și raportează pe trei categorii: rezolvat, regresie nouă și rămas.
6. Ciclul se repetă până când studentul marchează problema ca terminată sau toate testele trec.

## 6. Cerințe funcționale

### 6.1 Probleme

- F1. Lista de probleme, filtrabilă după categorie și dificultate.
- F2. Ecranul unei probleme afișează enunțul, exemplele și semnătura funcției.
- F3. Soluția de referință și testele ascunse nu sunt trimise niciodată către frontend.

### 6.2 Execuție și teste

- F4. Fiecare trimitere rulează pe testele de bază, pe cazurile limită și pe inputurile mari, izolat, cu limită de timp și memorie.
- F5. Rezultatele raportează, pentru fiecare test picat, inputul, rezultatul așteptat și rezultatul obținut.
- F6. Timpul se măsoară pe n = 10², 10³, 10⁴, 10⁵, iar panta pe scară log-log estimează complexitatea.
- F7. O buclă infinită pe un input mare nu afectează rezultatele celorlalte teste.

### 6.3 Versiuni

- F8. Fiecare trimitere devine o versiune nouă a sesiunii.
- F9. Clasificatorul de versiuni compară testele picate între versiunea anterioară și cea curentă și stabilește determinist: rezolvat, regresie nouă, rămas.
- F10. Studentul poate compara oricare două versiuni într-un diff vizual.

### 6.4 Observații

- F11. Generatorul de observații (LLM) primește enunțul, diff-ul, testele picate, panta și clasificarea și returnează observații în JSON structurat: tip, linie, input-dovadă, risc și trei niveluri de indiciu.
- F12. Verificatorul prin contraexemplu rulează codul studentului și soluția de referință pe inputul-dovadă. Observația ajunge la student doar dacă rezultatele diferă; altfel este eliminată și jurnalizată.
- F13. Fiecare observație afișată arată dovada („pică pe inputul []”).
- F14. Studentul poate marca fiecare observație ca utilă sau falsă.

### 6.5 Indicii gradate

Nivelul arată cât din soluție dezvăluie indiciul, nu cât de gravă este problema.

| Nivel | Ce face | Ce nu are voie |
|---|---|---|
| 1 – Orientare | Arată unde sau la ce să te uiți, de obicei ca întrebare | Să numească tehnica sau soluția |
| 2 – Direcție | Numește categoria de idee sau conceptul relevant | Să spună structura sau algoritmul exact |
| 3 – Concret | Numește tehnica sau structura specifică și rolul ei | Să scrie codul |

- F15. Implicit este vizibil doar nivelul 1; studentul deblochează nivelul următor cu „Mai mult ajutor”.
- F16. Filtrul anti-scurgere respinge indiciile de nivel 1 și 2 care conțin termenii interziși ai problemei și le regenerează, cu cel mult două reîncercări.

### 6.6 Variante de comparație

- F17. Aplicația servește trei variante prin aceeași interfață: sistemul propus, Baseline A (doar rezultatul testelor, ca LeetCode) și Baseline B (același LLM întrebat „ce e greșit aici?”, fără teste, diff, verificare sau niveluri).
- F18. Varianta se alege la crearea sesiunii (`mode`) și nu este afișată participantului.

### 6.7 Jurnal

- F19. Se salvează fiecare versiune, rezultatele testelor, observațiile confirmate și eliminate, nivelurile de indicii deblocate (cu momentul) și marcajele studentului.

## 7. Date

### 7.1 Setul de probleme

20-30 de probleme ușoare și medii, scrise de echipă, deci fără restricții de drepturi. Setul este predefinit pentru că verificatorul are nevoie de o soluție de referință și de teste ascunse.

| Categorie | Exemple |
|---|---|
| Hashing | two sum, anagrame grupate, primul caracter unic |
| Doi pointeri / fereastră glisantă | subșir fără repetiții, container cu cea mai multă apă |
| Sortare și intervale | unirea intervalelor, întâlniri suprapuse |
| Stivă | paranteze echilibrate, următorul element mai mare |
| Căutare binară | căutare în vector rotit, prima poziție a unui element |
| Grafuri | drum minim pe grilă (BFS), număr de insule |
| Programare dinamică simplă | subsecvența de sumă maximă, moduri de a urca scările |

Fiecare problemă conține: enunț cu 1-2 exemple, semnătura funcției, constrângeri, limita de timp, soluția de referință, teste ascunse (de bază, cazuri limită, inputuri mari generate automat), etichete (categorie, dificultate) și termenii interziși pentru nivelul 1.

### 7.2 Lanțurile de versiuni

Pentru evaluarea automată, fiecare problemă are 2-4 versiuni intenționat imperfecte (de exemplu caz limită ratat, off-by-one introdus la corectură, soluție corectă dar lentă), fiecare cu observațiile de referință notate dinainte. Rezultă aproximativ 40-80 de versiuni adnotate. Lanțurile nu sunt vizibile studentului.

### 7.3 Separarea seturilor

Aproximativ o treime din probleme servesc la reglarea promptului; restul formează setul de test, neatins până la evaluarea finală.

## 8. Arhitectura

### 8.1 Componente logice

1. **Executorul de teste:** rulează codul în Piston și măsoară timpii.
2. **Modulul de diff:** compară versiunea curentă cu cea anterioară.
3. **Clasificatorul de versiuni:** rezolvat, regresie nouă, rămas.
4. **Generatorul de observații:** LLM, răspuns JSON structurat.
5. **Verificatorul prin contraexemplu:** confirmă sau elimină observațiile.
6. **Filtrul anti-scurgere:** termeni interziși, opțional un al doilea apel LLM.
7. **Jurnalul:** PostgreSQL.
8. **Harness-ul de evaluare:** rulează variantele pe lanțurile din setul de test.

### 8.2 Stack tehnic

| Strat | Tehnologie |
|---|---|
| Frontend | React, Vite, TypeScript, @monaco-editor/react (Editor, DiffEditor), TanStack Query, shadcn/ui sau MUI, react-markdown |
| Backend | Spring Boot (ProblemService, HarnessBuilder, PistonClient, ComplexityEstimator, DiffService cu java-diff-utils, VersionClassifier, LlmClient, CounterexampleVerifier, HintLeakFilter, FeedbackStrategy) |
| Execuție | Piston găzduit local în Docker (privileged, cgroup v2); API-ul public nu mai este liber |
| LLM | SDK-ul oficial Anthropic pentru Java sau Spring AI |
| Date | PostgreSQL (H2 la dezvoltare) |
| Rulare | docker compose up: piston, backend, frontend (nginx), postgres |

### 8.3 API

```
GET  /api/problems                        lista, fără soluții și fără teste
GET  /api/problems/{id}                   enunț, semnătură, exemple
POST /api/sessions                        { problemId, mode } → sessionId
POST /api/sessions/{id}/submissions       { code } → rezultate și observații
POST /api/observations/{id}/next-hint     următorul nivel de indiciu
POST /api/observations/{id}/feedback      { verdict: useful | false }
GET  /api/sessions/{id}/history           versiuni și diff-uri
```

### 8.4 Modelul de date

Session, Submission (cod, rezultate, timpi), Observation (tip, dovadă, confirmată sau eliminată), HintReveal (nivel, moment), Feedback.

## 9. Cerințe nefuncționale

- N1. O trimitere returnează rezultatul în cel mult 30 de secunde.
- N2. Codul studentului rulează izolat; nu poate accesa rețeaua sau sistemul de fișiere al gazdei.
- N3. Răspunsurile LLM invalide (JSON greșit) sunt reîncercate o dată, apoi se afișează doar rezultatele testelor.
- N4. Costul LLM per trimitere este măsurat și raportat.
- N5. README-ul descrie instalarea și rularea; proiectul pornește cu docker compose up.

## 10. Evaluare

### 10.1 Evaluare automată

Pe lanțurile din setul de test, pentru sistemul propus și Baseline B:

- precizia observațiilor;
- erorile ratate, ponderate după severitate;
- acuratețea clasificării pe versiuni;
- efectul verificatorului: observații propuse față de observații eliminate;
- rata de scurgere a indiciilor, verificată manual pe un eșantion;
- costul în tokeni și apeluri LLM.

### 10.2 Studiu cu utilizatori

4-6 colegi, fiecare rezolvă trei probleme similare (P1, P2, P3), câte una pentru fiecare variantă, în ordine rotită. 15 minute pe problemă, varianta nu este afișată.

| Participant | P1 | P2 | P3 |
|---|---|---|---|
| 1 | Propus | A | B |
| 2 | Propus | B | A |
| 3 | A | Propus | B |
| 4 | A | B | Propus |
| 5 | B | Propus | A |
| 6 | B | A | Propus |

Măsurăm rezolvarea (câți termină și după câte versiuni), autonomia (niveluri cerute, scala 1-5) și încrederea (sugestii acceptate și marcate ca false). Rezultatele sunt exploratorii: medie și interval pe variantă, plus observații calitative.

## 11. Riscuri

| Risc | Atenuare | Cum îl observăm |
|---|---|---|
| Observații false care strică o soluție corectă | Verificatorul prin contraexemplu, dovada afișată | Sugestii marcate ca false, regresii după un indiciu |
| Indicii care dau soluția | Filtrul anti-scurgere, definiții stricte ale nivelurilor | Rata de scurgere |
| Măsurarea timpului instabilă | Înlocuire cu „trece sau nu în limita de timp pe inputul mare” | Varianța timpilor în săptămâna 4 |
| Piston nu rulează pe mașinile echipei | Verificare în săptămâna 4 (privileged, cgroup v2, WSL2) | Eșantionul de fezabilitate |

## 12. Calendar

| Săptămâni | Reper | Livrabil |
|---|---|---|
| 4 | Fezabilitate | 5 probleme complete, Piston rulează, primul apel LLM |
| 5-6 | Etapa 1 | Plan de 2-4 pagini, prezentare de 5 minute |
| 7 | Control | Baseline A funcțional, setul de test fixat |
| 8-9 | Etapa 2 | Prototip utilizabil, rezultate preliminare, demo de 6 minute |
| 10-11 | Test cu utilizatori | Studiul rulat, observații anonimizate, analiza erorilor |
| 12 | Îmbunătățire | O schimbare justificată de date, înghețarea funcțiilor |
| 13-14 | Etapa finală | Cod, setul de test, raport de 4-6 pagini, demo de 7 minute |

## 13. Întrebări deschise

- Confirmare cu profesorul: interpretăm „modificarea de cod” ca diferența dintre versiuni succesive ale aceluiași student.
- SDK-ul pentru LLM: SDK-ul oficial Anthropic pentru Java sau Spring AI.
- Biblioteca de componente: shadcn/ui sau MUI.
