# Le mie piante 🌿

App Android per ricordarsi quando innaffiare e concimare le piante di casa.

## Funzioni

- **Scheda pianta**: foto (fotocamera o galleria), nome, specie, posizione, note.
- **Timer a giorni** per acqua e concime, con frequenza **estiva** e **invernale**
  (inverno = novembre–febbraio; 0 = sospeso, es. niente concime d'inverno).
- **"Innaffiata ora" / "Concimata ora"**: azzera il timer. Per il concime si annotano prodotto e dose.
- **"Ancora umida"**: hai controllato il terreno ed è ancora bagnato? Il promemoria si sposta
  di qualche giorno senza azzerare il timer, per evitare di innaffiare troppo.
- **Notifica giornaliera alle 9:00** per le piante da curare, con i pulsanti "Fatto" e "Rimanda".
- **Lista ordinata per urgenza**, con colori di stato (🔴 da fare/in ritardo, 🟡 a breve, 🟢 a posto),
  filtro per stanza e pulsanti rapidi (con "Annulla").
- **Storico** di ogni pianta; una voce sbagliata si può eliminare e il timer viene ricalcolato.

Tutti i dati restano sul telefono: niente account, niente server, funziona offline.

## Tecnologie

Kotlin · Jetpack Compose (Material 3) · Room · WorkManager · Coil · minSdk 26 (Android 8.0)

## Come ottenere l'APK

**Opzione A, Android Studio (consigliata)**
1. Apri la cartella del progetto con Android Studio.
2. Collega il telefono via USB con il debug USB attivo, oppure usa un emulatore.
3. Premi ▶ Run.

**Opzione B, GitHub Actions**
Carica il progetto su un repository GitHub: il workflow `.github/workflows/build-apk.yml`
compila l'app a ogni push. L'APK si scarica dalla pagina della build (sezione *Artifacts*).

**Opzione C, riga di comando** (serve l'Android SDK)
```bash
./gradlew assembleDebug
# APK in app/build/outputs/apk/debug/app-debug.apk
```

## Struttura

```
app/src/main/java/com/lemiepiante/
├── domain/         logica dei timer (stagioni, scadenze, urgenza) + testi
├── data/           database Room: piante, storico, repository
├── notifications/  controllo giornaliero e azioni dalla notifica
├── ui/             schermate Compose: lista, dettaglio, modifica
└── util/           salvataggio delle foto
```
