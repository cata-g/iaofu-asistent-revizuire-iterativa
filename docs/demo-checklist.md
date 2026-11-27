# Lista de verificare pentru demo

Aplicația e declarată gata pentru demo când toate punctele de mai jos sunt bifate. Fiecare punct a fost verificat în aplicația pornită cu `docker compose up --build`, în browser (Playwright), nu doar prin teste unitare. Coloana „Cerință” trimite la [PRD](PRD.md).

Legendă: `[x]` verificat și merge; `[!]` bug găsit, vezi secțiunea „Buguri”; `[ ]` neverificat încă.

## 0. Pornire

- [ ] 0.1 `docker compose up --build` pornește toate serviciile dintr-un repo curat (N5)
- [x] 0.2 `/api/health` răspunde `UP`; Piston are Python 3.12.0 instalat automat
- [x] 0.3 Portul backend-ului se poate schimba cu `BACKEND_PORT`, fără să afecteze aplicația
- [ ] 0.4 Cele 20 de probleme trec `check_problem.py` (referința, testele, lanțurile)

## 1. Lista de probleme (F1)

- [ ] 1.1 Apar toate cele 20 de probleme, cu titlu, categorie și dificultate în română
- [x] 1.2 Filtrul după categorie arată doar problemele din categoria aleasă, pentru fiecare din cele 7 categorii
- [x] 1.3 Filtrul după dificultate merge, singur și combinat cu categoria
- [x] 1.4 O combinație fără rezultate afișează un mesaj, nu o listă goală fără explicație
- [x] 1.5 Click pe o problemă deschide spațiul de lucru
- [ ] 1.6 Dacă backend-ul nu răspunde, apare un mesaj de eroare clar

## 2. Ecranul problemei (F2, F3)

- [ ] 2.1 Enunțul (Markdown, cu diacritice), exemplele, constrângerile și limita de timp se afișează corect pentru fiecare problemă
- [x] 2.2 Editorul pornește cu semnătura funcției, cu evidențiere Python
- [x] 2.3 Un id de problemă inexistent afișează o eroare, nu un ecran alb
- [x] 2.4 Niciun răspuns din rețea nu conține soluția de referință, testele ascunse, `forbiddenTerms`, `inputs.py` sau lanțurile (F3, verificat în traficul real)

## 3. Execuția (F4-F7)

- [x] 3.1 Soluția corectă: toate testele trec, mesaj de succes, complexitate estimată
- [x] 3.2 Codul de pornire (`pass`): testele pică cu `None`, fără eroare de server
- [x] 3.3 Eroare de sintaxă: mesaj „Codul nu se poate rula”, cu eroarea Python
- [x] 3.4 Excepție la rulare pe un test: testul arată eroarea, celelalte rulează
- [!] 3.5 Buclă infinită: doar testele afectate pică cu timeout, restul rulează; răspunsul vine în < 30 s (F7, N1)
- [x] 3.6 Soluție lentă O(n²): inputurile mari dau timeout, panta și eficiența se afișează (F6)
- [x] 3.7 Pentru fiecare test picat se văd inputul, așteptatul și obținutul (F5)
- [x] 3.8 `print` în codul studentului nu strică rezultatul
- [x] 3.9 Codul nu poate accesa rețeaua sau fișierele gazdei (N2)
- [x] 3.10 Funcție cu alt nume decât semnătura: mesaj de eroare clar
- [x] 3.11 Cod gol: mesaj de eroare clar, fără 500
- [x] 3.12 Butonul „Trimite” e dezactivat cât rulează; dublu-click nu creează două versiuni

## 4. Versiuni (F8-F10)

- [x] 4.1 Fiecare trimitere devine o versiune nouă, numerotată v1, v2, …
- [ ] 4.2 Clasificarea rezolvat / regresie nouă / rămas e corectă pe lanțurile din probleme (F9)
- [x] 4.3 Clasificarea apare doar în varianta `proposed`
- [x] 4.4 Tab-ul „Versiuni”: lista versiunilor, diff vizual între oricare două versiuni (F10)
- [ ] 4.5 Diff-ul între aceeași versiune cu ea însăși nu strică ecranul
- [x] 4.6 Refresh în mijlocul sesiunii: codul și versiunile nu se pierd sau comportamentul e clar
- [x] 4.7 „← Probleme” și înapoi la aceeași problemă: codul și feedback-ul rămân

## 5. Observații și indicii (F11-F16), varianta `proposed`

- [x] 5.1 1-3 observații, fiecare cu tip, linie, dovadă (input, așteptat, obținut) și risc (F11, F13)
- [x] 5.2 Linia observației e evidențiată în editor
- [ ] 5.3 Fiecare observație afișată e confirmată de contraexemplu; cele respinse nu apar (F12)
- [x] 5.4 Implicit doar nivelul 1; „Mai mult ajutor” deblochează 2, apoi 3; după 3 butonul dispare (F15)
- [ ] 5.5 Indiciile de nivel 1-2 nu conțin termenii interziși (F16), pe un eșantion din toate categoriile
- [x] 5.6 Marcajul Utilă / Falsă se salvează și se poate schimba (F14)
- [x] 5.7 Toate testele trec: LLM-ul nu e apelat, fără observații, mesaj de succes
- [x] 5.8 Eroare de sintaxă: fără observații inventate
- [x] 5.9 LLM indisponibil (cheie greșită): se afișează doar rezultatele testelor, cu mesaj (N3)

## 6. Variantele (F17, F18)

- [x] 6.1 `baseline_a`: doar rezultatele testelor, fără observații, clasificare sau mesaj LLM
- [x] 6.2 `baseline_b`: rezultatele testelor plus răspunsul LLM în Markdown, fără clasificare și fără observații
- [x] 6.3 Varianta din link (`?mode=...&participant=...`) dispare din bara de adrese și nu apare nicăieri în pagină sau în răspunsurile API (F18)
- [ ] 6.4 Varianta rămâne aceeași la navigarea între probleme în același tab
- [x] 6.5 Un `mode` invalid în link e ignorat (rămâne varianta deja aleasă în tab sau `proposed`)
- [ ] 6.6 Linkurile din `scripts/study_links.py` deschid problema și varianta corectă

## 7. Terminarea problemei

- [x] 7.1 „Am terminat” cere confirmare; „Nu încă” anulează
- [x] 7.2 După confirmare nu se mai pot trimite versiuni (buton dezactivat, API refuză)

## 8. Jurnal și export (F19, N4)

- [x] 8.1 Exportul CSV pentru fiecare tabel conține sesiunile, versiunile, observațiile (confirmate și eliminate), indiciile cu momentul, marcajele și apelurile LLM cu cost
- [x] 8.2 Costul per trimitere e înregistrat (N4)

## 9. Interfață

- [ ] 9.1 Fără erori în consola browserului pe tot fluxul
- [ ] 9.2 Textele sunt în română, cu diacritice, fără chei sau valori brute din backend
- [x] 9.3 Arată bine pe un ecran de laptop (1366×768) și pe unul mare (1920×1080)
- [x] 9.4 Tema întunecată a sistemului nu strică lizibilitatea (editor, diff, carduri)
- [x] 9.5 Stările de încărcare se văd (listă, problemă, trimitere)

## 10. Fluxul de demo, cap-coadă

- [ ] 10.1 O problemă din fiecare categorie: v1 → v2 → v3 din lanț → soluția corectă, în `proposed`
- [ ] 10.2 Aceeași problemă în `baseline_a` și `baseline_b`, pentru comparație
- [ ] 10.3 Timpul maxim pe trimitere în toate rulările de mai sus < 30 s (N1)

## Buguri

| # | Punct | Descriere | Stare |
|---|---|---|---|
| 1 | 3.2, 9.2 | Mesajul de eficiență spunea „termină în limita de timp” și când răspunsurile pe inputurile mari erau greșite sau dădeau eroare | reparat: eficiența e `unknown` dacă un input mare nu are răspunsul corect |
| 2 | 3.7 | La comparatorul `unordered`, „obținut” arăta forma canonică (sortată ca text), nu ce a returnat studentul | reparat în harness; comparația rămâne canonică |
| 3 | 3.10 | Funcție cu alt nume: eroare tehnică (`AttributeError: module 'solution'…`) | reparat: „funcția X nu este definită; păstrează numele din semnătură” |
| 4 | 3.11, 9.2 | Cod gol: mesajul „Invalid request body” în engleză | reparat: butonul e dezactivat pe cod gol; toate erorile API au mesaje în română |
| 5 | 3.12 | Dublu-click pe „Trimite”: două cereri simultane, a doua cu 500 (aceeași versiune) | reparat: blocare în frontend și 409 în backend |
| 6 | 4.6, 4.7 | Refresh sau „← Probleme”: sesiunea, codul și feedback-ul se pierdeau | reparat: sesiunea se reia din istoric; ciorna se păstrează în tab |
| 7 | 4.4 | Eroare Monaco în consolă la ieșirea din tab-ul Versiuni („TextModel got disposed…”) | reparat |
| 8 | 4.2 | Versiune care nu rulează (eroare de sintaxă): clasificarea lista toate testele ca regresie | reparat: clasificarea nu se afișează pentru o versiune care nu a rulat |
| 9 | 1.1 | Lista de probleme nu avea o ordine | reparat: după categorie (PRD 7.1), dificultate, titlu |
| 10 | 9.3 | Sub ~1200 px lățime, editorul ajungea la ~150 px și apărea scroll orizontal | reparat: coloanele se așază una sub alta sub 1200 px |
| 11 | 2.3 | O rută necunoscută afișa o pagină goală | reparat: redirect la lista de probleme |
| 12 | 3.5 | Buclă infinită pe toate testele: Piston oprea tot procesul după 3 s CPU („Time limit exceeded”) | în lucru: limita CPU = limita de timp; 1 s pe test mic |
| 13 | 0.1 | Portul 8080 ocupat pe mașina de demo oprea pornirea backend-ului | reparat: `BACKEND_PORT` în `.env` |
