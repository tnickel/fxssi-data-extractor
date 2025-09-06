# FXSSI Data Extractor - Vollständige Projektdokumentation

## 📋 Inhaltsverzeichnis

1. [Überblick](#überblick)
2. [Features & Funktionalität](#features--funktionalität)
3. [Systemarchitektur](#systemarchitektur)
4. [Installation & Setup](#installation--setup)
5. [Verwendung](#verwendung)
6. [Technische Details](#technische-details)
7. [Konfiguration](#konfiguration)
8. [Datenstrukturen](#datenstrukturen)
9. [API & Code-Struktur](#api--code-struktur)
10. [Troubleshooting](#troubleshooting)
11. [Entwicklung & Erweiterung](#entwicklung--erweiterung)
12. [Lizenz & Credits](#lizenz--credits)

---

## 🎯 Überblick

### Projektbeschreibung
Der **FXSSI Data Extractor** ist eine professionelle Java-Anwendung zur automatisierten Extraktion und Analyse von Forex-Sentiment-Daten von FXSSI.com. Das Tool sammelt kontinuierlich Buy/Sell-Verhältnisse für Währungspaare und generiert Trading-Signale basierend auf einer Contrarian-Strategie.

### Zielgruppe
- **Forex-Trader** für Sentiment-basierte Handelsstrategien
- **Quantitative Analysten** für Datensammlung und -analyse
- **Algorithmic Trading Entwickler** für Signal-Integration
- **Marktforscher** für Sentiment-Studien

### Kernvorteile
- ✅ **Automatisierte Datensammlung** ohne manuelle Intervention
- ✅ **Dual-Mode Architektur** für verschiedene Anwendungsfälle
- ✅ **Robuste Datenvalidierung** mit Backup-Strategien
- ✅ **Echtzeit-Signalwechsel-Erkennung** für Trading-Alerts
- ✅ **Umfassende historische Datenanalyse** mit Statistiken

---

## 🚀 Features & Funktionalität

### Core Features

#### 1. **Dual-Mode Architektur**
- **Console-Modus**: Hintergrund-Service für kontinuierliche Datensammlung
- **GUI-Modus**: Interaktive JavaFX-Anwendung für Live-Monitoring
- **Command-Line-Switching**: Flexibler Wechsel zwischen Modi

#### 2. **Web-Scraping Engine**
- **JSoup-basierte Extraktion** von FXSSI Current Ratio Daten
- **Robuste HTML-Parsing-Strategien** mit Fallback-Mechanismen
- **15+ Währungspaare** automatisch erkannt und verarbeitet
- **Fehlertolerante Implementierung** bei Website-Änderungen

#### 3. **Dreifache Datenspeicherung**
```
data/
├── fxssi_data_2025-01-15.csv          # Tägliche Sammel-Dateien
├── currency_pairs/
│   ├── EUR_USD.csv                     # Währungspaar-spezifische Historie
│   ├── GBP_USD.csv
│   └── USD_JPY.csv
└── signal_changes/
    ├── signal_changes_history.csv      # Signalwechsel-Ereignisse
    └── last_known_signals.csv          # Aktuelle Signal-States
```

#### 4. **Trading-Signal-Generierung**
- **Contrarian-Approach**: Gegen die Masse handeln
- **BUY Signal**: Wenn Buy-Sentiment < 40% (Mehrheit verkauft → Kaufgelegenheit)
- **SELL Signal**: Wenn Buy-Sentiment > 60% (Mehrheit kauft → Verkaufsgelegenheit)  
- **NEUTRAL Signal**: Bei ausgeglichenem Sentiment (40-60%)

#### 5. **Live-GUI Features**
- **Interaktive Sentiment-Tabelle** mit visuellen Ratio-Balken
- **Echtzeit Signal-Icons** (📈 BUY, 📉 SELL, ↔️ NEUTRAL)
- **Auto-Refresh-System** (konfigurierbar 1-60 Minuten)
- **Signalwechsel-Anzeige** mit Wichtigkeits-Klassifizierung
- **Historische Daten-Popup** mit umfassenden Statistiken

#### 6. **Signalwechsel-Erkennung**
- **Automatische Erkennung** von Signal-Änderungen
- **Wichtigkeits-Klassifizierung**: 
  - 🔴 **CRITICAL**: Direkte Umkehrung (BUY ↔ SELL)
  - 🟠 **HIGH**: Wechsel zu/von NEUTRAL
  - 🟡 **MEDIUM**: Kleinere Änderungen
- **Aktualitäts-Bewertung**: Sehr aktuell, Heute, Diese Woche, Älter
- **Timeline-Integration** mit visuellen Indikatoren

#### 7. **Historische Datenanalyse**
- **Vollständige CSV-Historie** pro Währungspaar
- **Statistische Auswertungen**: Durchschnitt, Min/Max, Extremwerte
- **Signal-Verteilungsanalyse** mit prozentualer Aufschlüsselung
- **Export-Funktionalität** für externe Analyse
- **Filterbare Zeiträume**: Letzte 100/500/1000 oder alle Einträge

---

## 🏗️ Systemarchitektur

### Architektur-Überblick
```
┌─────────────────────────────────────────────────────────────┐
│                    FXSSI Data Extractor                    │
├─────────────────────────────────────────────────────────────┤
│  Command Line Interface (Argument Parsing)                 │
├─────────────────┬───────────────────────────────────────────┤
│   Console Mode │                GUI Mode                   │
│                 │                                           │
│ ┌─────────────┐ │ ┌─────────────────────────────────────┐   │
│ │ Hourly      │ │ │        JavaFX Application          │   │
│ │ Scheduler   │ │ │                                     │   │
│ └─────────────┘ │ │ ┌─────────────────────────────────┐ │   │
│                 │ │ │     MainWindowController       │ │   │
│                 │ │ │  ┌─────────────────────────────┐│ │   │
│                 │ │ │  │   Data Refresh Manager     ││ │   │
│                 │ │ │  └─────────────────────────────┘│ │   │
│                 │ │ └─────────────────────────────────┘ │   │
│                 │ └─────────────────────────────────────┘   │
├─────────────────┴───────────────────────────────────────────┤
│                  Shared Core Components                     │
│                                                             │
│ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ │
│ │  GuiDataService │ │  FXSSIScraper   │ │ Storage Manager │ │
│ │                 │ │                 │ │                 │ │
│ │ • Data Caching  │ │ • JSoup Parsing │ │ • CSV Files     │ │
│ │ • Service Layer │ │ • Fallback      │ │ • Validation    │ │
│ │ • Thread Safety │ │ • Demo Data     │ │ • Cleanup       │ │
│ └─────────────────┘ └─────────────────┘ └─────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │              Storage Layer (Triple Storage)            │ │
│ │                                                         │ │
│ │ ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │ │
│ │ │   Daily     │ │ Currency    │ │  Signal Change      │ │ │
│ │ │   Files     │ │ Pair Files  │ │     History         │ │ │
│ │ │             │ │             │ │                     │ │ │
│ │ │ • Combined  │ │ • Individual│ │ • Change Events     │ │ │
│ │ │ • Backup    │ │ • Statistics│ │ • Importance        │ │ │
│ │ │ • Cleanup   │ │ • Export    │ │ • Timeline          │ │ │
│ │ └─────────────┘ └─────────────┘ └─────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Design Patterns
- **Service Layer Pattern**: GuiDataService für Datenbereitstellung
- **Facade Pattern**: FXSSIScraper als Web-Scraping-Schnittstelle
- **Scheduler Pattern**: HourlyScheduler für zeitgesteuerte Ausführung
- **Factory Pattern**: Custom TableCell-Factories für GUI-Komponenten
- **Observer Pattern**: DataRefreshManager für GUI-Updates

### Thread-Sicherheit
- **ScheduledExecutorService** für zeitgesteuerte Operationen
- **CompletableFuture** für asynchrone GUI-Datenladung
- **Platform.runLater()** für thread-sichere JavaFX-Updates
- **ReentrantLock** für währungspaar-spezifische Dateizugriffe
- **ConcurrentHashMap** für thread-sichere Caches

---

## 📦 Installation & Setup

### Systemanforderungen
- **Java**: Version 11 oder höher
- **JavaFX**: Inklusive oder separat installiert
- **RAM**: Minimum 512 MB, empfohlen 1 GB
- **Festplatte**: 100 MB für Anwendung + Speicherplatz für Daten
- **Netzwerk**: Internetverbindung für FXSSI.com-Zugriff

### Installation

#### 1. **Repository klonen**
```bash
git clone https://github.com/your-repo/fxssi-data-extractor.git
cd fxssi-data-extractor
```

#### 2. **Dependencies installieren**
```bash
# Maven
mvn clean install

# Oder Gradle
gradle build
```

#### 3. **JAR-Datei erstellen**
```bash
mvn package
# Erstellt: target/fxssi-data-extractor-{version}.jar
```

#### 4. **Erste Ausführung testen**
```bash
# Hilfe anzeigen
java -jar fxssi-data-extractor.jar --help

# GUI-Modus testen
java -jar fxssi-data-extractor.jar --gui

# Console-Modus testen
java -jar fxssi-data-extractor.jar --console
```

### Verzeichnisstruktur nach Installation
```
fxssi-data-extractor/
├── fxssi-data-extractor.jar
├── data/                           # Datenverzeichnis (wird automatisch erstellt)
│   ├── currency_pairs/
│   ├── signal_changes/
│   └── debug_html/
├── logs/                          # Log-Dateien (wird automatisch erstellt)
└── config/                        # Optional: Konfigurationsdateien
```

---

## 💻 Verwendung

### Console-Modus (Hintergrund-Service)

#### Standard-Ausführung
```bash
# Standard-Datenverzeichnis (./data)
java -jar fxssi-data-extractor.jar --console

# Benutzerdefiniertes Datenverzeichnis
java -jar fxssi-data-extractor.jar --console --data-dir /path/to/data

# Windows-Beispiel
java -jar fxssi-data-extractor.jar --console --data-dir C:\FXSSIData
```

#### Als Service konfigurieren (Linux)
```bash
# systemd Service-Datei erstellen
sudo nano /etc/systemd/system/fxssi-extractor.service
```

```ini
[Unit]
Description=FXSSI Data Extractor Service
After=network.target

[Service]
Type=simple
User=your-username
WorkingDirectory=/path/to/fxssi-data-extractor
ExecStart=/usr/bin/java -jar fxssi-data-extractor.jar --console --data-dir /var/data/fxssi
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# Service aktivieren und starten
sudo systemctl enable fxssi-extractor
sudo systemctl start fxssi-extractor
sudo systemctl status fxssi-extractor
```

### GUI-Modus (Interaktive Anwendung)

#### GUI starten
```bash
# Standard GUI mit Standard-Datenverzeichnis
java -jar fxssi-data-extractor.jar --gui

# GUI mit benutzerdefiniertem Datenverzeichnis
java -jar fxssi-data-extractor.jar --gui --data-dir /path/to/data
```

#### GUI-Features nutzen

**1. Live-Daten-Monitoring**
- **Auto-Refresh**: Checkbox aktivieren für automatische Updates
- **Intervall einstellen**: 1-60 Minuten je nach Bedarf
- **Manueller Refresh**: "🔄 Refresh" Button für sofortige Aktualisierung

**2. Historische Daten anzeigen**
- **Währungspaar auswählen**: Klick auf Tabellenzeile
- **Button-Methode**: "📊 Historische Daten" Button klicken
- **Doppelklick-Methode**: Direkt auf Tabellenzeile doppelklicken

**3. Signalwechsel-Monitoring**
- **Icons beachten**: 🔴🟠🟡⚪ für verschiedene Wichtigkeiten
- **Details anzeigen**: Klick auf Signalwechsel-Icon
- **Historie durchsuchen**: Zeitfilter in Signalwechsel-Popup

**4. Debug und Troubleshooting**
- **Debug-Button**: "🔧 Debug CSV" für Diagnose
- **Log-Ausgabe**: Console für detaillierte Informationen
- **Verbindungstest**: Wird automatisch beim Start durchgeführt

---

## 🔧 Technische Details

### Datenextraktion

#### Web-Scraping-Mechanismus
```java
// Vereinfachtes Beispiel der Scraping-Logik
public List<CurrencyPairData> extractCurrentRatioData() {
    Document document = Jsoup.connect(FXSSI_URL)
        .userAgent(USER_AGENT)
        .timeout(TIMEOUT_MS)
        .get();
    
    // Multiple Parsing-Strategien
    List<CurrencyPairData> data = parseCorrectFXSSIStructure(document);
    if (data.isEmpty()) data = tryFXSSISelectors(document);
    if (data.isEmpty()) data = tryTableParsing(document);
    if (data.isEmpty()) data = createDemoData();
    
    return removeDuplicatesAndValidate(data);
}
```

#### Fehlerbehandlung und Fallbacks
1. **Primary Strategy**: Korrekte FXSSI HTML-Struktur-Erkennung
2. **Fallback 1**: Alternative CSS-Selektoren
3. **Fallback 2**: Tabellen-basiertes Parsing
4. **Fallback 3**: Intelligentes Text-Parsing
5. **Emergency Fallback**: Demo-Daten für Testing

### Datenverarbeitung

#### Signal-Generierung-Algorithmus
```java
public void calculateTradingSignal() {
    if (buyPercentage > 60.0) {
        this.tradingSignal = TradingSignal.SELL;  // Contrarian: Gegen die Masse
    } else if (buyPercentage < 40.0) {
        this.tradingSignal = TradingSignal.BUY;   // Contrarian: Gegen die Masse
    } else {
        this.tradingSignal = TradingSignal.NEUTRAL; // Unklare Marktlage
    }
}
```

#### Signalwechsel-Erkennung
```java
public List<SignalChangeEvent> processNewData(List<CurrencyPairData> newData) {
    List<SignalChangeEvent> detectedChanges = new ArrayList<>();
    
    for (CurrencyPairData data : newData) {
        TradingSignal currentSignal = data.getTradingSignal();
        TradingSignal lastSignal = lastKnownSignals.get(currencyPair);
        
        if (lastSignal != null && lastSignal != currentSignal) {
            // SIGNALWECHSEL ERKANNT!
            SignalChangeEvent changeEvent = new SignalChangeEvent(
                currencyPair, lastSignal, currentSignal, 
                data.getTimestamp(), lastBuyPercentage, data.getBuyPercentage()
            );
            detectedChanges.add(changeEvent);
        }
        
        lastKnownSignals.put(currencyPair, currentSignal);
    }
    
    return detectedChanges;
}
```

### Performance-Optimierungen

#### Caching-Strategien
- **GUI-Data-Service**: 2-Minuten-Cache für Live-Daten
- **Signal-Change-History**: In-Memory-Cache für frequente Zugriffe
- **Currency-Pair-Statistics**: Lazy-Loading bei Bedarf

#### Thread-Pool-Management
```java
// Scheduler für Console-Modus
private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
    Thread t = new Thread(r, "FXSSI-Scheduler");
    t.setDaemon(true);
    return t;
});

// GUI-Refresh-Manager
private final ScheduledExecutorService refreshScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "GUI-DataRefresh-Thread");
    t.setDaemon(true);
    return t;
});
```

---

## ⚙️ Konfiguration

### Command-Line-Parameter
```bash
# Vollständige Parameter-Liste
java -jar fxssi-data-extractor.jar [OPTIONEN]

Optionen:
  --gui                     Startet die grafische Benutzeroberfläche
  --console                 Startet im Console-Modus (Standard)
  --data-dir <PFAD>        Setzt das Datenverzeichnis (Standard: ./data)
  --help                   Zeigt diese Hilfe an

Beispiele:
  java -jar fxssi-data-extractor.jar --gui
  java -jar fxssi-data-extractor.jar --console --data-dir /home/user/fxssi-data
  java -jar fxssi-data-extractor.jar --data-dir C:\FXSSIData --gui
```

### Erweiterte Konfiguration

#### Logging-Konfiguration (log4j.xml)
```xml
<!-- Beispiel-Konfiguration für verschiedene Log-Level -->
<Configuration status="WARN">
    <Properties>
        <Property name="LOG_LEVEL">${sys:log.level:-INFO}</Property>
    </Properties>
    
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <ThresholdFilter level="INFO"/>
        </Console>
        
        <RollingFile name="FileAppender" fileName="logs/fxssi-extractor.log">
            <SizeBasedTriggeringPolicy size="10MB"/>
            <DefaultRolloverStrategy max="10"/>
        </RollingFile>
    </Appenders>
</Configuration>
```

#### Umgebungsvariablen
```bash
# Optional: Log-Level setzen
export LOG_LEVEL=DEBUG

# Java-Memory-Einstellungen
export JAVA_OPTS="-Xms256m -Xmx1024m"

# Anwendung mit Umgebungsvariablen starten
java $JAVA_OPTS -Dlog.level=$LOG_LEVEL -jar fxssi-data-extractor.jar
```

---

## 📊 Datenstrukturen

### CSV-Dateiformat

#### Tägliche Sammeldateien
```csv
# fxssi_data_2025-01-15.csv
Zeitstempel;Währungspaar;Buy_Prozent;Sell_Prozent;Handelssignal
2025-01-15 09:00:00;EUR/USD;45,20;54,80;BUY
2025-01-15 09:00:00;GBP/USD;62,10;37,90;SELL
2025-01-15 09:00:00;USD/JPY;48,50;51,50;NEUTRAL
```

#### Währungspaar-spezifische Dateien
```csv
# EUR_USD.csv
Zeitstempel;Buy_Prozent;Sell_Prozent;Handelssignal
2025-01-15 08:00:00;44,80;55,20;BUY
2025-01-15 09:00:00;45,20;54,80;BUY
2025-01-15 10:00:00;46,10;53,90;BUY
```

#### Signalwechsel-Historie
```csv
# signal_changes_history.csv
Zeitstempel;Währungspaar;Von_Signal;Zu_Signal;Von_Buy_Prozent;Zu_Buy_Prozent
2025-01-15 10:30:00;EUR/USD;BUY;NEUTRAL;38,20;42,10
2025-01-15 11:15:00;GBP/USD;NEUTRAL;SELL;48,90;61,20
```

### Datenmodell-Strukturen

#### CurrencyPairData
```java
public class CurrencyPairData {
    private String currencyPair;           // z.B. "EUR/USD"
    private double buyPercentage;          // 0.0 - 100.0
    private double sellPercentage;         // 0.0 - 100.0
    private TradingSignal tradingSignal;   // BUY, SELL, NEUTRAL, UNKNOWN
    private LocalDateTime timestamp;       // Zeitstempel der Messung
    
    public enum TradingSignal {
        BUY("Kaufen"),
        SELL("Verkaufen"), 
        NEUTRAL("Seitwärts"),
        UNKNOWN("Unbekannt");
    }
}
```

#### SignalChangeEvent
```java
public class SignalChangeEvent {
    private String currencyPair;
    private TradingSignal fromSignal;
    private TradingSignal toSignal;
    private LocalDateTime changeTime;
    private double fromBuyPercentage;
    private double toBuyPercentage;
    private SignalChangeImportance importance;  // CRITICAL, HIGH, MEDIUM, LOW
    
    public enum SignalChangeImportance {
        CRITICAL("🔴", "Kritisch"),    // BUY ↔ SELL
        HIGH("🟠", "Hoch"),           // BUY/SELL → NEUTRAL
        MEDIUM("🟡", "Mittel"),       // Kleinere Änderungen
        LOW("🟢", "Niedrig");         // Sonstige
    }
}
```

---

## 🔌 API & Code-Struktur

### Hauptkomponenten

#### 1. FXSSIDataExtractor (Main Class)
```java
// Einstiegspunkt mit Command-Line-Parsing
public class FXSSIDataExtractor {
    public static void main(String[] args)
    public void start()                    // Console-Modus
    public void stop()                     // Graceful Shutdown
    public void executeManualDataExtraction()
}
```

#### 2. GuiDataService (Service Layer)
```java
public class GuiDataService {
    // Primäre Datenladung mit Triple-Storage
    public List<CurrencyPairData> getCurrentData()
    public CompletableFuture<List<CurrencyPairData>> getCurrentDataAsync()
    public List<CurrencyPairData> forceDataRefresh()
    
    // Historische Datenanalyse
    public List<CurrencyPairData> getHistoricalDataForCurrencyPair(String currencyPair)
    public List<CurrencyPairData> getRecentDataForCurrencyPair(String currencyPair, int count)
    
    // Signalwechsel-Integration
    public SignalChangeHistoryManager getSignalChangeHistoryManager()
    public List<SignalChangeEvent> getSignalChangeHistory(String currencyPair)
    
    // Statistiken und Diagnose
    public ExtendedDataStatistics getExtendedDataStatistics()
    public Set<String> getAvailableCurrencyPairs()
    public boolean testConnection()
}
```

#### 3. Storage Manager (Triple Storage)
```java
// Tägliche Dateien
public class DataFileManager {
    public void appendDataToFile(List<CurrencyPairData> data)
    public List<CurrencyPairData> readTodayData()
    public List<CurrencyPairData> readDataForDate(LocalDate date)
    public void cleanupOldFiles(int daysToKeep)
}

// Währungspaar-spezifische Dateien  
public class CurrencyPairDataManager {
    public void appendDataForAllPairs(List<CurrencyPairData> data)
    public List<CurrencyPairData> readDataForCurrencyPair(String currencyPair)
    public Set<String> listAvailableCurrencyPairs()
    public CurrencyPairStatistics getStatisticsForCurrencyPair(String currencyPair)
}

// Signalwechsel-Historie
public class SignalChangeHistoryManager {
    public List<SignalChangeEvent> processNewData(List<CurrencyPairData> newData)
    public List<SignalChangeEvent> getSignalChangeHistory(String currencyPair)
    public List<SignalChangeEvent> getSignalChangesWithinHours(String currencyPair, int hours)
}
```

#### 4. Web-Scraping Engine
```java
public class FXSSIScraper {
    public List<CurrencyPairData> extractCurrentRatioData()
    public boolean testConnection()
    
    // Private Parsing-Strategien
    private List<CurrencyPairData> parseCorrectFXSSIStructure(Document document)
    private List<CurrencyPairData> tryFXSSISelectors(Document document)
    private List<CurrencyPairData> tryTableParsing(Document document)
    private List<CurrencyPairData> createDemoData()
}
```

### GUI-Komponenten

#### MainWindowController
```java
public class MainWindowController {
    // UI-Erstellung
    public Scene createMainWindow(Stage primaryStage)
    private VBox createTopArea()
    private VBox createCenterArea()
    
    // Datenmanagement
    private void refreshData()
    private void updateTableData(List<CurrencyPairData> data)
    
    // Historische Daten Integration
    private void showHistoricalDataForSelectedPair()
    private void showHistoricalDataForPair(String currencyPair)
    
    // Debug und Diagnose
    private void debugHistoricalDataLoading()
}
```

#### Custom TableCells
```java
// Ratio-Balken für visuelle Darstellung
public class RatioBarTableCell extends TableCell<CurrencyPairTableRow, CurrencyPairTableRow>

// Signal-Icons für Trading-Signale
public class SignalIconTableCell extends TableCell<CurrencyPairTableRow, CurrencyPairTableRow>

// Signalwechsel-Anzeige mit Popup-Integration
public class SignalChangeTableCell extends TableCell<CurrencyPairTableRow, CurrencyPairTableRow>
```

---

## 🐛 Troubleshooting

### Häufige Probleme und Lösungen

#### 1. **"Keine historischen Daten angezeigt"**
**Problem**: HistoricalDataWindow zeigt leere Tabelle  
**Ursachen**:
- Deutsches Dezimaltrennzeichen in CSV (Komma statt Punkt)
- CSV-Datei existiert nicht oder ist leer
- Ungültiges CSV-Format

**Lösung**:
```java
// Fix für deutsche Dezimaltrennzeichen in CurrencyPairDataManager
private CurrencyPairData parseCurrencyDataFromCsv(String csvLine, String currencyPair) {
    // Konvertiere Kommas zu Punkten
    String buyPercentageStr = parts[1].replace(",", ".");
    String sellPercentageStr = parts[2].replace(",", ".");
    
    double buyPercentage = Double.parseDouble(buyPercentageStr);
    double sellPercentage = Double.parseDouble(sellPercentageStr);
    // ...
}
```

**Debug-Schritte**:
1. "🔧 Debug CSV" Button in GUI klicken
2. Log-Ausgabe prüfen für Parsing-Fehler
3. CSV-Datei manuell öffnen und Format prüfen
4. Datenverzeichnis-Pfad validieren

#### 2. **"FXSSI-Website nicht erreichbar"**
**Problem**: Web-Scraping schlägt fehl  
**Symptome**: "Keine Daten von FXSSI erhalten"

**Lösungsansätze**:
```bash
# 1. Internetverbindung testen
ping fxssi.com

# 2. Firewall/Proxy prüfen
# 3. User-Agent anpassen falls blockiert
# 4. Demo-Daten-Modus verwenden für Testing
```

**Fallback-Aktivierung**:
```java
// Automatische Fallback-Strategien sind implementiert
// Bei Scraping-Fehlern werden Demo-Daten verwendet
private List<CurrencyPairData> createDemoData() {
    // Realistische Demo-Währungspaare für Testing
}
```

#### 3. **"GUI startet nicht / JavaFX-Fehler"**
**Problem**: GUI-Modus startet nicht  
**Fehlermeldungen**: "JavaFX runtime components are missing"

**Lösungen**:
```bash
# Java 11+: JavaFX separat installieren
# Option 1: JavaFX als Module
java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -jar fxssi-data-extractor.jar --gui

# Option 2: JavaFX auf Classpath
java -cp "fxssi-data-extractor.jar:/path/to/javafx/lib/*" FXSSIDataExtractor --gui

# Option 3: OpenJDK mit JavaFX verwenden
sudo apt install openjfx  # Ubuntu/Debian
```

#### 4. **"Speicher-/Performance-Probleme"**
**Problem**: Hoher RAM-Verbrauch oder langsame GUI  
**Optimierungen**:

```bash
# Java Memory-Einstellungen anpassen
java -Xms256m -Xmx1024m -jar fxssi-data-extractor.jar

# Garbage Collection optimieren
java -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -jar fxssi-data-extractor.jar

# GUI Auto-Refresh-Intervall erhöhen
# In GUI: Intervall von 1 auf 5+ Minuten setzen
```

#### 5. **"CSV-Dateien werden nicht erstellt"**
**Problem**: Keine Datenverzeichnisse oder CSV-Dateien  
**Ursachen**: Fehlende Schreibrechte, falscher Pfad

**Diagnose**:
```bash
# Verzeichnis-Berechtigungen prüfen
ls -la ./data/
ls -la ./data/currency_pairs/

# Manuell erstellen falls nötig
mkdir -p ./data/currency_pairs
mkdir -p ./data/signal_changes
chmod 755 ./data/

# Datenverzeichnis explizit setzen
java -jar fxssi-data-extractor.jar --console --data-dir /tmp/fxssi-test
```

### Debug-Logging aktivieren

#### Temporäres Debug-Logging
```bash
# Detaillierte Logs für Debugging
java -Dlog.level=DEBUG -jar fxssi-data-extractor.jar --gui
```

#### Log-Dateien analysieren
```bash
# Log-Dateien finden
find . -name "*.log" -type f

# Aktuelle Logs anzeigen
tail -f logs/fxssi-extractor.log

# Fehler-spezifische Logs
grep "ERROR\|SEVERE" logs/fxssi-extractor.log
```

---

## 🚧 Entwicklung & Erweiterung

### Entwicklungsumgebung einrichten

#### Prerequisites
```bash
# Java 11+ Development Kit
sudo apt install openjdk-11-jdk  # Ubuntu/Debian
# oder
brew install openjdk@11         # macOS

# JavaFX für GUI-Entwicklung
sudo apt install openjfx

# Maven für Build-Management
sudo apt install maven
```

#### IDE-Setup (IntelliJ IDEA)
1. **Projekt importieren**: File → Open → pom.xml
2. **JavaFX konfigurieren**: Run Configuration → VM Options
   ```
   --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml
   ```
3. **Code Style**: Java Google Style Guide
4. **Plugins installieren**: 
   - Lombok (falls verwendet)
   - CheckStyle-IDEA
   - SonarLint

### Code-Konventionen

#### Naming Conventions
```java
// Klassen: PascalCase
public class CurrencyPairDataManager

// Methoden: camelCase
public void appendDataForSinglePair()

// Konstanten: UPPER_SNAKE_CASE  
private static final String DEFAULT_DATA_DIRECTORY = "data";

// Packages: lowercase mit Unterpunkten
package com.fxssi.extractor.storage;
```

#### Logging Best Practices
```java
private static final Logger LOGGER = Logger.getLogger(ClassName.class.getName());

// Info für wichtige Business-Events
LOGGER.info("Erfolgreich " + data.size() + " Währungspaare extrahiert");

// Warning für erwartbare Probleme
LOGGER.warning("Keine Daten von FXSSI erhalten - möglicherweise Website-Problem");

// Severe für kritische Fehler
LOGGER.log(Level.SEVERE, "Fehler beim Starten des Programms: " + e.getMessage(), e);

// Fine für Debug-Details
LOGGER.fine("Cache aktualisiert mit " + newData.size() + " Datensätzen");
```

### Neue Features hinzufügen

#### 1. Neues Währungspaar integrieren
```java
// In FXSSIScraper.java erweitern
private String[] knownPairs = {
    "AUDJPY", "AUDUSD", "EURAUD", "EURGBP", "EURJPY", "EURUSD", 
    "GBPJPY", "GBPUSD", "NZDUSD", "USDCAD", "USDCHF", "USDJPY", 
    "XAGUSD", "XAUUSD", "EURCHF", "GBPCHF",
    "USDTRY", "EURTRY", "GBPTRY"  // NEUE Türkische Lira Paare
};
```

#### 2. Neue Trading-Signal-Logik
```java
// In CurrencyPairData.java erweitern
public void calculateAdvancedTradingSignal() {
    if (buyPercentage > 80.0) {
        this.tradingSignal = TradingSignal.STRONG_SELL;
    } else if (buyPercentage > 60.0) {
        this.tradingSignal = TradingSignal.SELL;
    } else if (buyPercentage < 20.0) {
        this.tradingSignal = TradingSignal.STRONG_BUY;
    } else if (buyPercentage < 40.0) {
        this.tradingSignal = TradingSignal.BUY;
    } else {
        this.tradingSignal = TradingSignal.NEUTRAL;
    }
}
```

#### 3. Neue GUI-Komponenten
```java
// Custom TableCell für Volatilitäts-Anzeige
public class VolatilityTableCell extends TableCell<CurrencyPairTableRow, CurrencyPairTableRow> {
    @Override
    protected void updateItem(CurrencyPairTableRow item, boolean empty) {
        // Volatilitäts-Berechnung basierend auf historischen Daten
        double volatility = calculateVolatility(item.getCurrencyPair());
        
        // Visuelle Darstellung
        if (volatility > 5.0) {
            setStyle("-fx-background-color: #ffcccb;"); // Hohe Volatilität = Rot
        } else if (volatility > 2.0) {
            setStyle("-fx-background-color: #fff4cc;"); // Mittlere Volatilität = Gelb
        } else {
            setStyle("-fx-background-color: #ccffcc;"); // Niedrige Volatilität = Grün
        }
        
        setText(String.format("%.2f%%", volatility));
    }
}
```

### Testing-Framework

#### Unit Tests erweitern
```java
@Test
public void testSignalCalculation() {
    CurrencyPairData data = new CurrencyPairData("EUR/USD", 35.0, 65.0, null);
    data.calculateTradingSignal();
    
    assertEquals(TradingSignal.BUY, data.getTradingSignal());
    assertTrue(data.isDataConsistent());
}

@Test
public void testCsvParsing() {
    String csvLine = "2025-01-15 10:00:00;EUR/USD;45,20;54,80;BUY";
    CurrencyPairData data = CurrencyPairData.fromCsvLine(csvLine);
    
    assertEquals("EUR/USD", data.getCurrencyPair());
    assertEquals(45.20, data.getBuyPercentage(), 0.01);
    assertEquals(TradingSignal.BUY, data.getTradingSignal());
}
```

#### Integration Tests
```java
@Test
public void testWebScrapingFallback() {
    FXSSIScraper scraper = new FXSSIScraper();
    
    // Simuliere Website-Ausfall
    // Erwarte Demo-Daten als Fallback
    List<CurrencyPairData> data = scraper.extractCurrentRatioData();
    
    assertFalse(data.isEmpty());
    assertTrue(data.size() >= 5); // Mindestens Demo-Daten
}
```

### Performance-Monitoring

#### Metriken sammeln
```java
public class PerformanceMonitor {
    private static final Logger PERF_LOGGER = Logger.getLogger("PERFORMANCE");
    
    public static void logDataExtractionTime(long startTime, int recordCount) {
        long duration = System.currentTimeMillis() - startTime;
        PERF_LOGGER.info(String.format("Extraktion: %d Datensätze in %d ms (%.2f/s)", 
            recordCount, duration, recordCount * 1000.0 / duration));
    }
    
    public static void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        
        PERF_LOGGER.fine(String.format("Memory: %d MB used / %d MB max (%.1f%%)", 
            used / 1024 / 1024, max / 1024 / 1024, used * 100.0 / max));
    }
}
```

---

## 📄 Lizenz & Credits

### Lizenz
Dieses Projekt steht unter der **MIT License**.

```
MIT License

Copyright (c) 2025 FXSSI Data Extractor Project

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

### Credits & Dependencies

#### Core-Libraries
- **Java SE 11+**: Oracle Corporation
- **JavaFX**: OpenJDK / Oracle Corporation  
- **JSoup 1.15+**: Jonathan Hedley - HTML Parsing Library
- **JUnit 5**: JUnit Team - Testing Framework

#### Design-Inspiration
- **FXSSI.com**: Datenquelle für Forex-Sentiment-Daten
- **Material Design**: Google - UI/UX-Inspiration für GUI-Komponenten
- **Trading View**: Chart-Styling-Inspiration für Ratio-Balken

### Disclaimer

**⚠️ Wichtiger Hinweis**: Dieses Tool dient ausschließlich zu Bildungs- und Forschungszwecken. 

- **Keine Anlageberatung**: Die generierten Trading-Signale stellen keine Finanzberatung dar
- **Eigenverantwortung**: Trading-Entscheidungen erfolgen auf eigenes Risiko
- **Datenqualität**: Korrektheit der FXSSI-Daten kann nicht garantiert werden
- **Website-Abhängigkeit**: Funktionalität abhängig von FXSSI.com-Verfügbarkeit

**Nutzen Sie das Tool verantwortungsbewusst und konsultieren Sie professionelle Finanzberater für Investitionsentscheidungen.**

---

## 📞 Support & Community

### Bug Reports & Feature Requests
- **GitHub Issues**: [Repository Issues](https://github.com/your-repo/fxssi-data-extractor/issues)
- **E-Mail**: support@your-domain.com
- **Template für Bug Reports**:
  ```
  **Bug Description**: Kurze Beschreibung des Problems
  **Steps to Reproduce**: 1. ... 2. ... 3. ...
  **Expected Behavior**: Was sollte passieren
  **Actual Behavior**: Was passiert tatsächlich
  **Environment**: OS, Java Version, GUI/Console Mode
  **Log Output**: Relevante Log-Ausgaben
  ```

### Contributing Guidelines
1. **Fork** das Repository
2. **Branch** erstellen: `git checkout -b feature/amazing-feature`
3. **Code-Konventionen** befolgen (siehe Entwicklung-Sektion)
4. **Tests** hinzufügen für neue Features
5. **Commit**: `git commit -m 'Add amazing feature'`
6. **Push**: `git push origin feature/amazing-feature`
7. **Pull Request** erstellen

### Roadmap & Future Features
- [ ] **REST API**: Web-API für externe Integration
- [ ] **Alert-System**: E-Mail/SMS-Benachrichtigungen bei Signalwechseln  
- [ ] **Machine Learning**: KI-basierte Signal-Vorhersagen
- [ ] **Multi-Source-Integration**: Weitere Sentiment-Datenquellen
- [ ] **Mobile App**: Android/iOS-Companion-App
- [ ] **Cloud-Deployment**: Docker-Container und Cloud-Integration
- [ ] **Advanced Charts**: Interaktive Charting-Komponenten
- [ ] **Portfolio-Integration**: Verbindung zu Broker-APIs

---

*Letzte Aktualisierung: Januar 2025 | Version 1.5 | FXSSI Data Extractor Project*
