# Scout Alert

Scout Alert è un'app Android pensata per gestire i compleanni nelle unità scout a partire da un file Excel. L'app importa i dati da un file `.xlsx`, normalizza nomi e unità, salva localmente l'elenco dei compleanni e mostra notifiche giornaliere e un widget home screen per ricordare i compleanni del giorno.

## Funzionalità principali

- Importazione di compleanni da file Excel `.xlsx`
- Parsing automatico della prima scheda del file
- Riconoscimento “smart” delle colonne principali (`Cognome`, `Nome`, `Data di nascita`, `Unità`)
- Normalizzazione dei nomi e delle unità scout
- Salvataggio locale dei compleanni tramite `SharedPreferences`
- Ricerca e filtro per unità
- Inserimento manuale, modifica ed eliminazione di compleanni
- Notifiche giornaliere per i compleanni del giorno
- Selezione delle unità per cui abilitare le notifiche
- Widget Android per vedere rapidamente i compleanni di oggi

## Stack tecnico

- **Kotlin**
- **Android SDK**
- **Jetpack Compose** per l'interfaccia
- **WorkManager** per i controlli periodici e le notifiche
- **Apache POI** per leggere file Excel `.xlsx`
- **SharedPreferences** per la persistenza locale

## Requisiti

- **Android minSdk 26**
- **compileSdk / targetSdk 36**
- **Java 17**
- **Kotlin JVM toolchain 17**

## Come funziona

1. L'utente apre l'app e importa un file Excel.
2. Il parser legge la **prima sheet** del file e cerca le colonne rilevanti.
3. I dati vengono convertiti in un elenco di compleanni (`BirthdayEntry`).
4. L'elenco viene salvato localmente in JSON dentro `SharedPreferences`.
5. L'interfaccia consente di:
   - cercare per nome o unità
   - filtrare per unità
   - aggiungere manualmente un compleanno
   - modificare o eliminare voci esistenti
6. Un worker giornaliero controlla i compleanni del giorno e invia una notifica aggregata.
7. Il widget home screen mostra i compleanni di oggi per le unità selezionate.

## Formato del file Excel atteso

L'app legge il **primo foglio** del file `.xlsx` e prova a riconoscere automaticamente le intestazioni.

### Colonne attese

Le colonne logiche attese sono:

- `Cognome`
- `Nome`
- `Data di nascita`
- `Unità`

### Intestazioni riconosciute

Il parser usa un matching flessibile. In particolare:

- per il cognome cerca `cognome`
- per il nome cerca `nome`
- per la data cerca colonne che contengono `nascita` o `data`
- per l'unità accetta anche `unità`, `branca`, `reparto`

### Formati data supportati

Sono gestiti sia valori Excel formattati come data sia stringhe nei formati:

- `dd/MM/yyyy`
- `d/M/yyyy`
- `yyyy-MM-dd`
- `dd-MM-yyyy`
- `dd.MM.yyyy`

## Normalizzazione delle unità

Le unità vengono uniformate, quando possibile, in sigle standard:

- `Co.Ca.`
- `L/C`
- `E/G`
- `R/S`

Questo rende più consistente il filtro e la selezione delle notifiche anche quando l'Excel contiene varianti testuali diverse.

## Interfaccia utente

L'app include:

- una schermata principale per l'importazione del file
- un pannello notifiche per selezionare le unità abilitate
- una barra di ricerca e filtro
- una lista dei compleanni ordinata e filtrabile
- un dialog per creare, modificare o cancellare compleanni
- un riepilogo in alto con i compleanni del giorno

## Notifiche

Le notifiche vengono gestite tramite `WorkManager`.

- All'avvio dell'app viene creato un canale notifiche dedicato
- Viene schedulato un controllo periodico giornaliero
- Quando ci sono compleanni nel giorno corrente, l'app invia una **notifica aggregata**
- Le notifiche rispettano la selezione delle unità salvata nelle preferenze

## Widget

Il widget mostra:

- titolo: **Compleanni di oggi**
- numero di compleanni del giorno
- fino a tre nominativi con eventuale unità

Un tap sul widget apre direttamente l'app.

## Struttura del progetto

```text
app/
├── src/main/java/it/agesci/scoutalert/
│   ├── MainActivity.kt
│   ├── excel/
│   │   ├── ExcelModels.kt
│   │   └── ExcelParser.kt
│   ├── notifications/
│   │   ├── BirthdayScheduler.kt
│   │   └── BirthdayWorker.kt
│   ├── ui/
│   │   ├── BirthdayUiComponents.kt
│   │   ├── BirthdayUtils.kt
│   │   └── ExcelReaderScreen.kt
│   └── widget/
│       └── BirthdayWidgetProvider.kt
└── src/main/res/
    ├── layout/
    ├── values/
    └── xml/
```

## Avvio del progetto

1. Installa **JDK 17** e Android SDK 36.
2. Clona la repository e aprila con **Android Studio**.
3. Sincronizza Gradle e avvia l'app su Android 8.0 (API 26) o successivo.
4. Importa un file `.xlsx` fino a 10 MB contenente i compleanni.

Da PowerShell, nella root del progetto:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

Su macOS o Linux, usa `./gradlew` al posto di `.\gradlew.bat`.

## Possibili miglioramenti

- supporto a più fogli Excel
- esportazione/modifica batch dei dati
- backup su cloud
- impostazione esplicita dell'orario delle notifiche
- test UI e integrazione WorkManager
- localizzazione completa della UI

## Stato del repository

La repository contiene attualmente una singola app Android con un solo modulo `app`. Al momento non risultano presenti una descrizione del repository, release pubblicate o un file `LICENSE` dedicato.
