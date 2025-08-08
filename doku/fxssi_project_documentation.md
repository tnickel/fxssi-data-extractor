# FXSSI Data Extractor - Projektdokumentation

## Überblick

Der **FXSSI Data Extractor** ist eine Java-Anwendung zur automatisierten Extraktion von Forex-Sentiment-Daten von der Website FXSSI.com. Das System sammelt stündlich Current Ratio Daten für verschiedene Währungspaare und speichert diese strukturiert in CSV-Dateien für weitere Analysen.

### Hauptfunktionen
- **Automatisierte Datenextraktion**: Stündliche Sammlung von Buy/Sell-Verhältnissen von FXSSI.com
- **Web-Scraping**: Robustes HTML-Parsing mit JSoup und Fallback-Strategien
- **Datenmanagement**: Automatische CSV-Dateierstellung mit täglicher Segmentierung
- **Handelssignal-Generierung**: Contrarian-Approach basierend auf Sentiment-Daten
- **Scheduler-System**: Zeitgesteuerte Ausführung mit konfigurierbaren Intervallen
- **Dateiverwaltung**: Backup, Cleanup und Validierungsfunktionen

## Technologie-Stack

- **Java 11+** mit java.util.logging
- **JSoup** für Web-Scraping und HTML-Parsing
- **ScheduledExecutorService** für Zeitsteuerung
- **CSV-Format** für Datenexport
- **Log4j** für erweiterte Logging-Funktionen
- **JUnit 5** für Unit-Tests

## Projektarchitektur

### Package-Struktur
```
com.fxssi.extractor/
├── model/           # Datenmodelle
├── scraper/         # Web-Scraping Komponenten
├── storage/         # Dateiverwaltung
└── scheduler/       # Zeitsteuerung
```

### Architektur-Pattern
- **Modulare Architektur** mit klarer Package-Trennung
- **Scheduler-Pattern** für zeitgesteuerte Datensammlung
- **Facade-Pattern** für Scraper-Komponente
- **Safe Task Wrapper** für robuste Exception-Behandlung

## Klassenübersicht

### 1. FXSSIDataExtractor (Hauptklasse)
**Zweck**: Orchestrator und Einstiegspunkt der Anwendung

**Funktionen**:
- Koordiniert alle Komponenten (Scraper, FileManager, Scheduler)
- Führt initiale Datenextraktion durch
- Startet stündlichen Scheduler
- Behandelt Shutdown-Hooks für sauberes Beenden

**Wichtige Methoden**:
- `start()`: Startet das System
- `stop()`: Stoppt das System ordnungsgemäß
- `extractAndSaveData()`: Führt Datenextraktion durch

### 2. CurrencyPairData (model/)
**Zweck**: Datenmodell für Währungspaar-Sentiment-Daten

**Eigenschaften**:
- `currencyPair`: Währungspaar (z.B. "EUR/USD")
- `buyPercentage`: Kaufanteil in Prozent
- `sellPercentage`: Verkaufsanteil in Prozent
- `tradingSignal`: Handelssignal (BUY/SELL/NEUTRAL)
- `timestamp`: Zeitstempel der Datenerfassung

**Funktionen**:
- CSV-Import/Export
- Datenkonsistenz-Prüfung
- Automatische Handelssignal-Berechnung

### 3. FXSSIScraper (scraper/)
**Zweck**: Web-Scraper für FXSSI Current Ratio Daten

**Funktionen**:
- HTML-Parsing mit JSoup
- Extraktion von Währungspaaren und Prozentangaben
- Fallback-Strategien bei Parsing-Problemen
- Verbindungstest zur FXSSI-Website

**Wichtige Methoden**:
- `extractCurrentRatioData()`: Hauptmethode für Datenextraktion
- `testConnection()`: Verbindungstest
- `parseCurrentRatioData()`: HTML-Parsing

### 4. DataFileManager (storage/)
**Zweck**: CSV-Dateiverwaltung mit erweiterten Funktionen

**Funktionen**:
- Tägliche CSV-Dateisegmentierung
- Daten-Backup und -Cleanup
- Datenvalidierung
- Statistiken über gespeicherte Daten

**Wichtige Methoden**:
- `appendDataToFile()`: Daten an heutige Datei anhängen
- `readDataFromFile()`: Daten aus Datei lesen
- `cleanupOldFiles()`: Alte Dateien löschen
- `validateDataFile()`: Dateiintegrität prüfen

### 5. HourlyScheduler (scheduler/)
**Zweck**: Zeitsteuerung für stündliche Ausführung

**Funktionen**:
- Stündliche Ausführung zur vollen Stunde
- SafeTaskWrapper für Stabilität
- Daemon-Threads für sauberes Beenden
- Custom-Intervall-Unterstützung für Tests

**Wichtige Methoden**:
- `startScheduling()`: Scheduler starten
- `stopScheduling()`: Scheduler stoppen
- `executeTaskManually()`: Manuelle Ausführung

## Installation und Setup

### Voraussetzungen
- Java 11 oder höher
- Maven für Dependency-Management
- Internetverbindung für FXSSI.com

### Dependencies (pom.xml)
```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.15.3</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.20.0</version>
</dependency>
```

### Projektstruktur
```
fxssi-extractor/
├── src/
│   ├── main/java/
│   │   ├── FXSSIDataExtractor.java
│   │   └── com/fxssi/extractor/
│   │       ├── model/
│   │       ├── scraper/
│   │       ├── storage/
│   │       └── scheduler/
│   └── main/resources/
│       └── log4j.xml
├── data/                    # Wird automatisch erstellt
├── logs/                    # Wird automatisch erstellt
└── pom.xml
```

## Verwendung

### 1. Programm starten
```bash
java -jar fxssi-extractor.jar
```

### 2. Manuelle Ausführung
Das Programm kann auch programmatisch verwendet werden:
```java
FXSSIDataExtractor extractor = new FXSSIDataExtractor();
extractor.start();
// ... läuft automatisch
extractor.stop();
```

### 3. Custom Scheduler für Tests
```java
HourlyScheduler customScheduler = HourlyScheduler.createCustomIntervalScheduler(
    () -> System.out.println("Test execution"), 
    5 // 5 Minuten Intervall
);
```

## Konfiguration

### Logging-Konfiguration (log4j.xml)
- **Console Appender**: INFO-Level für Konsole
- **File Appender**: DEBUG-Level für allgemeine Logs
- **Error File Appender**: ERROR-Level für Fehler-Logs
- **Data Extraction Appender**: Spezielle Logs für Datenextraktion

### Verzeichnisstruktur
- `data/`: CSV-Dateien mit Sentiment-Daten
- `logs/`: Log-Dateien der Anwendung

### CSV-Dateiformat
```
Zeitstempel;Währungspaar;Buy_Prozent;Sell_Prozent;Handelssignal
2025-08-08 14:30:00;EUR/USD;45.50;54.50;BUY
2025-08-08 14:30:00;GBP/USD;62.30;37.70;SELL
```

## Handelssignal-Logic

Das System verwendet einen **Contrarian-Approach**:
- **BUY Signal**: Wenn Buy% < 40% (gegen die Masse handeln)
- **SELL Signal**: Wenn Buy% > 60% (gegen die Masse handeln)
- **NEUTRAL**: Wenn 40% ≤ Buy% ≤ 60%

## Monitoring und Diagnostik

### Log-Levels
- **INFO**: Normale Betriebsmeldungen
- **WARNING**: Potenzielle Probleme
- **ERROR**: Kritische Fehler
- **DEBUG**: Detaillierte Debugging-Informationen

### Status-Überwachung
```java
// Scheduler-Status prüfen
scheduler.getStatus();

// Datenstatistiken abrufen
fileManager.getDataStatistics();

// Verbindungstest
scraper.testConnection();
```

## Wartung und Administration

### Automatische Cleanup
```java
// Dateien älter als 30 Tage löschen
fileManager.cleanupOldFiles(30);
```

### Backup erstellen
```java
// Backup der heutigen Datei
fileManager.backupTodayFile();
```

### Datenvalidierung
```java
// Datei auf Integrität prüfen
boolean isValid = fileManager.validateDataFile("fxssi_data_2025-08-08.csv");
```

## Fehlerbehandlung

### Robuste Ausführung
- **SafeTaskWrapper**: Verhindert Scheduler-Stopp bei Exceptions
- **Fallback-Parsing**: Alternative Parsing-Methoden bei Website-Änderungen
- **Timeout-Handling**: 10 Sekunden Timeout für Web-Requests
- **Retry-Logic**: Automatische Wiederholung bei temporären Fehlern

### Bekannte Limitationen
- Abhängig von FXSSI-Website-Struktur
- Benötigt stabile Internetverbindung
- User-Agent-abhängige Website-Zugriffe

## Erweiterungsmöglichkeiten

### Zusätzliche Datenquellen
- Weitere Forex-Sentiment-Websites
- API-Integration für Echtzeitdaten
- Historische Datenanalyse

### Erweiterte Features
- Web-Dashboard für Datenvisualisierung
- E-Mail-Benachrichtigungen bei Signalen
- Database-Integration (MySQL, PostgreSQL)
- RESTful API für Datenabfrage

## Support und Wartung

### Logging-Analyse
```bash
# Alle Logs anzeigen
tail -f logs/fxssi-extractor.log

# Nur Fehler anzeigen
tail -f logs/fxssi-extractor-error.log

# Datenextraktion verfolgen
tail -f logs/data-extraction.log
```

### Performance-Monitoring
- Durchschnittliche Extraktionszeit: ~10-30 Sekunden
- Speicherverbrauch: ~50-100 MB
- CPU-Last: Minimal außerhalb der Extraktionszeiten

## Entwicklung und Testing

### Unit-Tests ausführen
```bash
mvn test
```

### Debug-Modus
```java
// Custom Scheduler für schnellere Tests
HourlyScheduler debugScheduler = HourlyScheduler.createCustomIntervalScheduler(task, 1); // 1 Minute
```

### Code-Qualität
- Umfassende Exception-Behandlung
- Thread-sichere Implementierung
- Modulare, erweiterbare Architektur
- Ausführliche Logging und Diagnostik

---

**Version**: 1.0.0  
**Letzte Aktualisierung**: August 2025  
**Lizenz**: Proprietär