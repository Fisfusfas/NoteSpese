# NoteSpese — CLAUDE.md

Documentazione di progetto per Claude Code. Aggiornato a: 2026-05-23.

---



## Panoramica

Expense tracker Android nativo multi-gruppo con logica di split avanzata.
Destinazione: distribuzione Play Store (uso personale/familiare).

**Package**: `com.app.notespese`
**minSdk**: 29 | **targetSdk / compileSdk**: 35
**Kotlin**: 2.0.21 | **AGP**: 8.3.2 | **Compose BOM**: 2024.12.01

---

## Stack tecnico

| Categoria | Librerie |
|---|---|
| UI | Jetpack Compose + Material3 |
| Navigation | Navigation Compose 2.8.5 |
| State | ViewModel + StateFlow + Coroutines 1.9.0 |
| DI | Hilt 2.53.1 |
| Auth | Firebase Auth + Credential Manager 1.3.0 + googleid 1.1.1 |
| Database cloud | Firebase Firestore |
| Push | Firebase Cloud Messaging |
| Storage | Firebase Storage |
| Cache locale | Room 2.6.1 |
| Immagini | Coil 2.7.0 |
| Widget | Jetpack Glance 1.1.1 |
| Background | WorkManager 2.10.0 |
| Preferenze | DataStore 1.1.2 |
| Export | Apache POI 5.2.3 (XLSX) |
| OCR | ML Kit Text Recognition 16.0.1 |

---

## Struttura package

```
com.app.notespese/
├── App.kt                          @HiltAndroidApp
├── MainActivity.kt                 routing auth (Login ↔ AppNavigation)
├── data/
│   ├── model/
│   │   ├── Enums.kt                Ruolo, ModalitaSplit, TipoSpesa, TipoCategoria,
│   │   │                           StatoDebitore, StatoCreditore, TipoDebito, StatoInvito
│   │   ├── Utente.kt
│   │   ├── Gruppo.kt               include membroIds[] per whereArrayContains
│   │   ├── Membro.kt               include userId duplicato per collectionGroup queries
│   │   ├── Categoria.kt
│   │   ├── Spesa.kt                mese/anno denormalizzati per filtri semplici
│   │   ├── Entrata.kt
│   │   ├── MeseConfig.kt
│   │   ├── Saldo.kt                helper coppiaId(), computed isSaldato
│   │   ├── Ricorrenza.kt
│   │   ├── Budget.kt
│   │   ├── Debito.kt
│   │   └── Invito.kt
│   └── repository/
│       ├── AuthRepository.kt (interface)
│       ├── FirebaseAuthRepository.kt
│       ├── MockAuthRepository.kt   (per test offline)
│       ├── GruppoRepository.kt (interface)
│       ├── FirebaseGruppoRepository.kt
│       ├── SpesaRepository.kt (interface)
│       ├── FirebaseSpesaRepository.kt
│       ├── EntrataRepository.kt (interface)
│       ├── FirebaseEntrataRepository.kt
│       ├── CategoriaRepository.kt (interface)
│       ├── FirebaseCategoriaRepository.kt
│       ├── SaldoRepository.kt (interface)
│       ├── FirebaseSaldoRepository.kt
│       ├── DebitoRepository.kt (interface)
│       ├── FirebaseDebitoRepository.kt
│       ├── InvitoRepository.kt (interface)
│       └── FirebaseInvitoRepository.kt
├── di/
│   ├── AuthModule.kt               @Binds FirebaseAuthRepository
│   ├── FirebaseModule.kt           @Provides FirebaseAuth, FirebaseFirestore
│   └── RepositoryModule.kt         @Binds tutti i repository
├── ui/
│   ├── auth/
│   │   ├── AuthViewModel.kt        signInWithGoogle(activity), signInAnonymously()
│   │   └── LoginScreen.kt
│   ├── gruppi/
│   │   ├── ListaGruppiViewModel.kt flatMapLatest su userId
│   │   ├── ListaGruppiScreen.kt    GruppoCard + helpers parseColore/iconaPerNome
│   │   ├── CreaGruppoViewModel.kt  form state con mutableStateOf
│   │   └── CreaGruppoScreen.kt     anteprima live + picker icona/colore (FlowRow)
│   ├── navigation/
│   │   ├── AppNavigation.kt        NavHost area autenticata
│   │   └── Screen.kt              sealed class con route e withId()
│   └── theme/
│       ├── Color.kt                seed #1565C0 (Blue 800)
│       ├── Theme.kt                dynamic color Android 12+
│       └── Type.kt
└── (placeholder) widget/, export/, notification/, domain/usecase/
```

---

## Struttura Firestore

```
users/{userId}
  nome, email, fotoUrl, fcmToken

gruppi/{gruppoId}
  nome, descrizione, creatoDa, icona, colore
  modalitaSplitDefault: COEFFICIENTE|CINQUANTA|PERSONALIZZATO
  membroIds: List<String>           ← per whereArrayContains (non eliminare!)

gruppi/{gruppoId}/membri/{userId}
  userId, ruolo: ADMIN|MEMBRO, nominativoLocale, aggiuntoIl, widgetDefault

gruppi/{gruppoId}/categorie/{categoriaId}
  nome, icona, colore, tipo: SPESA|ENTRATA|ENTRAMBI

gruppi/{gruppoId}/spese/{spesaId}
  importo, descrizione, categoriaId, pagante, condivisa, tipo
  data (Timestamp), mese (Int), anno (Int), note

gruppi/{gruppoId}/entrate/{entrataId}
  importo, persona, categoriaId, mese, anno, note

gruppi/{gruppoId}/mesi/{meseId}         es. "2026-05"
  modalitaSplit, splitPersonalizzato: Map<String,Double>
  chiusoManualmente, chiusoDa, chiusoIl

gruppi/{gruppoId}/mesi/{meseId}/saldi/{coppia}
  coppia = "userId1_userId2" (ordinati alfabeticamente)
  da, a, importoCalcolato
  statoDebitore: IN_ATTESA|PAGATO
  statoCreditore: IN_ATTESA|CONFERMATO
  dataPagamento, dataConferma, note

gruppi/{gruppoId}/ricorrenze/{ricorrenzaId}
  importo, descrizione, categoriaId, tipo, condivisa
  pagante, giornoDelMese, attiva

gruppi/{gruppoId}/budget/{categoriaId}
  importoMensile, notifica80, notifica100

gruppi/{gruppoId}/debiti/{debitoId}
  importo, tipo: PRESTITO_FATTO|PRESTITO_RICEVUTO
  controparte, persona, data, scadenza?, saldato, note

inviti/{invitoId}
  gruppoId, gruppoNome, creatoDa, codice (8 char)
  scadeIl, stato: PENDING|ACCETTATO|RIFIUTATO|SCADUTO
  usatoDa?
```

---

## Convenzioni di codice

- **Kotlin idiomatic** — niente Java
- **Compose** per tutta la UI — niente XML layouts
- **Coroutines + Flow** ovunque — niente callback
- **Repository pattern rigoroso** — la UI non tocca mai Firestore direttamente
- **ViewModel espone `StateFlow<UiState>`** — mai LiveData
- **`sealed interface UiState`** con Caricamento / Successo / Errore
- **`runCatching`** per wrappare le suspend fun dei repository Firebase
- **`callbackFlow` + `addSnapshotListener`** per i Flow real-time Firestore
- **`@DocumentId`** sui campi id nei modelli Firestore — necessario per la deserializzazione
- **Tutti i campi dei data class Firestore hanno default value** — genera il no-arg constructor
- **Hilt** per tutto il DI — `@HiltViewModel`, `@Singleton`, `@Binds`, `@Provides`
- Commenta solo i punti non ovvi

---

## Navigazione

```
Login
└── ListaGruppi (home)
    ├── CreaGruppo → dopo successo: Dashboard (popUpTo ListaGruppi)
    └── Dashboard/{gruppoId}           ← step 5
        ├── Spese/{gruppoId}           ← step 6
        ├── Entrate/{gruppoId}         ← step 9
        ├── Saldi/{gruppoId}           ← step 7
        ├── Debiti/{gruppoId}
        └── ImpostazioniGruppo/{gruppoId}
```

---

## Roadmap di sviluppo

| # | Step | Stato |
|---|---|---|
| 1 | Setup Gradle + dipendenze | ✅ |
| 2 | Firebase Auth Google Sign-In (Credential Manager) | ✅ |
| 3 | Modelli dati Kotlin + Repository Firestore | ✅ |
| 4 | Lista gruppi + Creazione gruppo | ✅ |
| 5 | Dashboard gruppo | ✅ |
| 6 | Form aggiunta spesa + CategoriaSelector | ⬜ |
| 7 | Calcolo saldo (debt simplification) + schermata saldi | ⬜ |
| 8 | Sistema inviti con codice 8 caratteri | ⬜ |
| 9 | Entrate + calcolo coefficiente split | ⬜ |
| 10 | Ricorrenze automatiche | ⬜ |
| 11 | Budget per categoria | ⬜ |
| 12 | Notifiche push FCM | ⬜ |
| 13 | Widget Glance | ⬜ |
| 14 | OCR scontrini ML Kit | ⬜ |
| 15 | Export Excel Apache POI | ⬜ |
| 16 | Analisi e grafici | ⬜ |
| 17 | Room cache offline | ⬜ |
| 18 | Onboarding + demo mode | ⬜ |

---

## Note importanti per Claude

- **`membroIds`** nel documento `Gruppo` NON è ridondante: serve per `whereArrayContains` e non va mai rimosso
- **`mese`/`anno`** in `Spesa` sono denormalizzati da `data` per evitare composite index su Timestamp
- **`userId`** in `Membro` duplica l'id documento: serve per collectionGroup queries (`FieldPath.documentId()` non è filtrabile in collectionGroup)
- **Eliminazione gruppo**: lato client elimina solo il documento radice. Le subcollection vanno eliminate con una Cloud Function
- **Invito**: `accettaInvito` usa una **transazione Firestore** — non semplificare a batch
- **`Saldo.coppiaId()`**: gli userId sono sempre ordinati alfabeticamente per garantire unicità della coppia
- Il binding `AuthModule` punta a `FirebaseAuthRepository`. `MockAuthRepository` esiste ma NON è in uso — non eliminarlo, serve per i test
- **Web Client ID** per Google Sign-In è in `res/values/strings.xml` come `google_web_client_id`
- Lo SHA-1 del debug keystore è `3B:2D:96:83:BE:E0:F5:A1:8D:22:95:95:6A:9B:16:5A:F5:08:3F:BA`
