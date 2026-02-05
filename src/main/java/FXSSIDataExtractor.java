import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.scheduler.HourlyScheduler;
import com.fxssi.extractor.scraper.FXSSIScraper;
import com.fxssi.extractor.scraper.FearGreedScraper;
import com.fxssi.extractor.storage.DataFileManager;
import com.fxssi.extractor.storage.CurrencyPairDataManager;
import com.fxsssi.extractor.gui.FXSSIGuiApplication;

/**
 * Erweiterte Hauptklasse für das FXSSI Datenextraktions-Programm
 * Unterstützt sowohl Console-Modus als auch GUI-Modus mit konfigurierbarem Root-Pfad
 * Jetzt mit währungspaar-spezifischer Datenspeicherung
 * 
 * ERWEITERT: Integration des CNN Fear & Greed Index als BTC/USD Symbol
 * 
 * @author Generated for FXSSI Data Extraction
 * @version 2.3 (mit Fear & Greed Index Integration)
 */
public class FXSSIDataExtractor {
    
    private static final Logger LOGGER = Logger.getLogger(FXSSIDataExtractor.class.getName());
    private static final String DEFAULT_DATA_DIRECTORY = "data";
    
    // Command Line Argumente
    private static final String GUI_ARG = "--gui";
    private static final String CONSOLE_ARG = "--console";
    private static final String HELP_ARG = "--help";
    private static final String DATA_DIR_ARG = "--data-dir";
    
    private FXSSIScraper scraper;
    private FearGreedScraper fearGreedScraper;
    private DataFileManager fileManager;
    private CurrencyPairDataManager currencyPairManager;
    private HourlyScheduler scheduler;
    private boolean isGuiMode = false;
    private String dataDirectory;
    
    /**
     * Konstruktor für Console-Modus mit Standard-Verzeichnis
     */
    public FXSSIDataExtractor() {
        this(false, DEFAULT_DATA_DIRECTORY);
    }
    
    /**
     * Konstruktor mit Modus-Auswahl und Standard-Verzeichnis
     * @param guiMode true für GUI-Modus, false für Console-Modus
     */
    public FXSSIDataExtractor(boolean guiMode) {
        this(guiMode, DEFAULT_DATA_DIRECTORY);
    }
    
    /**
     * Vollständiger Konstruktor mit Modus-Auswahl und konfigurierbarem Datenverzeichnis
     * @param guiMode true für GUI-Modus, false für Console-Modus
     * @param dataDirectory Pfad zum Datenverzeichnis
     */
    public FXSSIDataExtractor(boolean guiMode, String dataDirectory) {
        this.isGuiMode = guiMode;
        this.dataDirectory = validateAndNormalizeDataDirectory(dataDirectory);
        
        if (!guiMode) {
            // Nur für Console-Modus initialisieren
            this.scraper = new FXSSIScraper(this.dataDirectory);
            this.fearGreedScraper = new FearGreedScraper(this.dataDirectory);
            this.fileManager = new DataFileManager(this.dataDirectory);
            this.currencyPairManager = new CurrencyPairDataManager(this.dataDirectory);
            this.scheduler = new HourlyScheduler(this::extractAndSaveData);
            
            // Erstelle Verzeichnisse falls sie nicht existieren
            fileManager.createDataDirectory();
            currencyPairManager.createCurrencyDataDirectory();
        }
        
        LOGGER.info("FXSSIDataExtractor initialisiert - Modus: " + (guiMode ? "GUI" : "Console") + 
                   ", Datenverzeichnis: " + this.dataDirectory);
        LOGGER.info("Erweiterte Speicherung: Tägliche Dateien UND währungspaar-spezifische Dateien");
        if (!guiMode) {
            LOGGER.info("Fear & Greed Index aktiviert: Symbol " + fearGreedScraper.getSymbol());
        }
    }
    
    /**
     * Startet das Programm basierend auf dem Modus
     */
    public void start() {
        if (isGuiMode) {
            LOGGER.severe("GUI-Modus kann nicht von dieser Instanz gestartet werden. Verwenden Sie FXSSIGuiApplication.main()");
            return;
        }
        
        LOGGER.info("FXSSI Data Extractor gestartet (Console-Modus) - Beginne mit stündlicher Datenextraktion");
        LOGGER.info("Datenverzeichnis: " + dataDirectory);
        LOGGER.info("Speichert in: tägliche CSV-Dateien UND währungspaar-spezifische Dateien");
        LOGGER.info("Fear & Greed Index (BTC/USD) aktiviert: " + fearGreedScraper.getThresholdInfo());
        
        // Führe eine initiale Extraktion durch
        extractAndSaveData();
        
        // Starte den stündlichen Scheduler
        scheduler.startScheduling();
        
        LOGGER.info("Stündlicher Scheduler aktiv - Daten werden alle 60 Minuten in beide Formate gespeichert");
    }
    
    /**
     * Stoppt das Programm und den Scheduler
     */
    public void stop() {
        if (isGuiMode) {
            LOGGER.info("GUI-Modus wird von FXSSIGuiApplication verwaltet");
            return;
        }
        
        LOGGER.info("Stoppe FXSSI Data Extractor...");
        scheduler.stopScheduling();
        LOGGER.info("FXSSI Data Extractor gestoppt");
    }
    
    /**
     * Gibt das konfigurierte Datenverzeichnis zurück
     */
    public String getDataDirectory() {
        return dataDirectory;
    }
    
    /**
     * Gibt den CurrencyPairDataManager zurück (für externe Zugriffe)
     */
    public CurrencyPairDataManager getCurrencyPairManager() {
        return currencyPairManager;
    }
    
    /**
     * Gibt den FearGreedScraper zurück (für externe Zugriffe)
     */
    public FearGreedScraper getFearGreedScraper() {
        return fearGreedScraper;
    }
    
    /**
     * Führt die Datenextraktion durch und speichert die Ergebnisse in BEIDEN Formaten
     * ERWEITERT: Kombiniert FXSSI-Daten mit Fear & Greed Index (BTC/USD)
     * Diese Methode wird stündlich vom Scheduler aufgerufen UND bei jedem GUI-Refresh
     */
    private void extractAndSaveData() {
        try {
            LOGGER.info("Beginne Datenextraktion von FXSSI + Fear & Greed Index...");
            
            // 1. FXSSI-Daten laden
            List<CurrencyPairData> currentData = scraper.extractCurrentRatioData();
            
            if (currentData == null) {
                currentData = new ArrayList<>();
            }
            
            // 2. Fear & Greed Index laden und hinzufügen
            try {
                CurrencyPairData fearGreedData = fearGreedScraper.extractFearGreedData();
                
                if (fearGreedData != null) {
                    // Prüfe ob BTC/USD bereits existiert
                    boolean exists = currentData.stream()
                            .anyMatch(d -> d.getCurrencyPair().equals(fearGreedData.getCurrencyPair()));
                    
                    if (!exists) {
                        currentData.add(fearGreedData);
                        LOGGER.info("Fear & Greed Index hinzugefügt: " + fearGreedData.getCurrencyPair() +
                                   " | Index-Mapping: Buy=" + String.format("%.1f%%", fearGreedData.getBuyPercentage()) +
                                   ", Sell=" + String.format("%.1f%%", fearGreedData.getSellPercentage()) +
                                   " | Signal: " + fearGreedData.getTradingSignal());
                    }
                } else {
                    LOGGER.warning("Fear & Greed Index konnte nicht geladen werden");
                }
            } catch (Exception fgError) {
                LOGGER.log(Level.WARNING, "Fehler beim Laden des Fear & Greed Index: " + fgError.getMessage(), fgError);
            }
            
            // 3. Daten speichern wenn vorhanden
            if (!currentData.isEmpty()) {
                // BESTEHENDE SPEICHERUNG: Tägliche CSV-Dateien
                fileManager.appendDataToFile(currentData);
                
                // NEUE SPEICHERUNG: Währungspaar-spezifische Dateien
                currencyPairManager.appendDataForAllPairs(currentData);
                
                LOGGER.info("Erfolgreich " + currentData.size() + " Währungspaare extrahiert und gespeichert (inkl. BTC/USD Fear & Greed)");
                LOGGER.info("Daten gespeichert in: tägliche Datei UND " + currentData.size() + " währungspaar-spezifische Dateien");
                
                // Logge eine Zusammenfassung der extrahierten Daten
                logDataSummary(currentData);
                
                // Logge Speicher-Statistiken
                logStorageStatistics();
                
            } else {
                LOGGER.warning("Keine Daten erhalten - möglicherweise Website-Problem");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler bei der Datenextraktion: " + e.getMessage(), e);
        }
    }
    
    /**
     * Führt eine manuelle Datenextraktion durch (für GUI-Refresh)
     * Öffentliche Methode die von der GUI aufgerufen werden kann
     */
    public void executeManualDataExtraction() {
        LOGGER.info("Manuelle Datenextraktion ausgelöst...");
        extractAndSaveData();
    }
    
    /**
     * Loggt eine Zusammenfassung der extrahierten Daten
     */
    private void logDataSummary(List<CurrencyPairData> data) {
        if (LOGGER.isLoggable(Level.INFO)) {
            for (CurrencyPairData pair : data) {
                LOGGER.info(String.format("Extrahiert: %s - Buy: %.1f%%, Sell: %.1f%%, Signal: %s", 
                    pair.getCurrencyPair(), pair.getBuyPercentage(), pair.getSellPercentage(), pair.getTradingSignal()));
            }
        }
    }
    
    /**
     * Loggt Speicher-Statistiken für beide Speichersysteme
     */
    private void logStorageStatistics() {
        try {
            // Statistiken für tägliche Dateien
            String dailyStats = fileManager.getDataStatistics();
            LOGGER.info("Tägliche Dateien: " + dailyStats);
            
            // Statistiken für währungspaar-spezifische Dateien
            String currencyPairStats = currencyPairManager.getOverallStatistics();
            LOGGER.fine("Währungspaar-spezifische Dateien:\n" + currencyPairStats);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Abrufen der Speicher-Statistiken: " + e.getMessage(), e);
        }
    }
    
    /**
     * Bereinigt alte Daten in beiden Speichersystemen
     * @param daysToKeep Anzahl Tage die behalten werden sollen
     */
    public void cleanupOldDataInBothSystems(int daysToKeep) {
        LOGGER.info("Beginne Bereinigung alter Daten in beiden Speichersystemen (" + daysToKeep + " Tage behalten)...");
        
        try {
            // Bereinige tägliche Dateien
            fileManager.cleanupOldFiles(daysToKeep);
            
            // Bereinige währungspaar-spezifische Dateien
            currencyPairManager.cleanupOldData(daysToKeep);
            
            LOGGER.info("Bereinigung in beiden Speichersystemen abgeschlossen");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei der Bereinigung: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validiert Daten in beiden Speichersystemen
     * @return Validierungsbericht
     */
    public String validateBothStorageSystems() {
        StringBuilder report = new StringBuilder();
        
        report.append("=== VOLLSTÄNDIGE DATENVALIDIERUNG ===\n\n");
        
        try {
            // Validiere tägliche Dateien
            report.append("TÄGLICHE DATEIEN:\n");
            report.append("================\n");
            List<String> dailyFiles = fileManager.listDataFiles();
            int validDailyFiles = 0;
            
            for (String filename : dailyFiles) {
                boolean isValid = fileManager.validateDataFile(filename);
                report.append(String.format("%-30s: %s\n", filename, isValid ? "✓ Gültig" : "✗ Ungültig"));
                if (isValid) validDailyFiles++;
            }
            
            report.append(String.format("\nTägliche Dateien: %d/%d gültig\n\n", validDailyFiles, dailyFiles.size()));
            
            // Validiere währungspaar-spezifische Dateien
            report.append("WÄHRUNGSPAAR-SPEZIFISCHE DATEIEN:\n");
            report.append("=================================\n");
            String currencyValidation = currencyPairManager.validateAllData();
            report.append(currencyValidation);
            
            // Fear & Greed Status
            report.append("\nFEAR & GREED INDEX:\n");
            report.append("==================\n");
            if (fearGreedScraper != null) {
                report.append("Symbol: ").append(fearGreedScraper.getSymbol()).append("\n");
                report.append("Thresholds: ").append(fearGreedScraper.getThresholdInfo()).append("\n");
                boolean connected = fearGreedScraper.testConnection();
                report.append("API-Verbindung: ").append(connected ? "✓ OK" : "✗ Fehler").append("\n");
            }
            
        } catch (Exception e) {
            report.append("Fehler bei der Validierung: ").append(e.getMessage()).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * Gibt detaillierte Statistiken für beide Speichersysteme zurück
     */
    public String getDetailedStorageStatistics() {
        StringBuilder stats = new StringBuilder();
        
        stats.append("=== DETAILLIERTE SPEICHER-STATISTIKEN ===\n\n");
        
        try {
            // Tägliche Dateien
            stats.append("TÄGLICHE SPEICHERUNG:\n");
            stats.append("====================\n");
            stats.append(fileManager.getDataStatistics()).append("\n\n");
            
            // Währungspaar-spezifische Dateien
            stats.append("WÄHRUNGSPAAR-SPEZIFISCHE SPEICHERUNG:\n");
            stats.append("====================================\n");
            stats.append(currencyPairManager.getOverallStatistics()).append("\n\n");
            
            // Fear & Greed Info
            stats.append("FEAR & GREED INDEX:\n");
            stats.append("==================\n");
            if (fearGreedScraper != null) {
                stats.append("Symbol: ").append(fearGreedScraper.getSymbol()).append("\n");
                stats.append("Signal-Logik: ").append(fearGreedScraper.getThresholdInfo()).append("\n\n");
            }
            
            // Datenverzeichnis-Info
            stats.append("KONFIGURATION:\n");
            stats.append("==============\n");
            stats.append("Hauptverzeichnis: ").append(dataDirectory).append("\n");
            stats.append("Tägliche Dateien: ").append(dataDirectory).append("/\n");
            stats.append("Währungspaar-Dateien: ").append(dataDirectory).append("/currency_pairs/\n");
            
        } catch (Exception e) {
            stats.append("Fehler beim Abrufen der Statistiken: ").append(e.getMessage()).append("\n");
        }
        
        return stats.toString();
    }
    
    /**
     * Validiert und normalisiert das Datenverzeichnis
     */
    private String validateAndNormalizeDataDirectory(String directory) {
        if (directory == null || directory.trim().isEmpty()) {
            LOGGER.warning("Leeres Datenverzeichnis angegeben, verwende Standard: " + DEFAULT_DATA_DIRECTORY);
            return DEFAULT_DATA_DIRECTORY;
        }
        
        String normalized = directory.trim();
        
        // Entferne abschließende Pfad-Separatoren
        while (normalized.endsWith("/") || normalized.endsWith("\\")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        
        // Falls leer nach Normalisierung, verwende Standard
        if (normalized.isEmpty()) {
            LOGGER.warning("Ungültiges Datenverzeichnis nach Normalisierung, verwende Standard: " + DEFAULT_DATA_DIRECTORY);
            return DEFAULT_DATA_DIRECTORY;
        }
        
        LOGGER.info("Datenverzeichnis validiert: " + normalized);
        return normalized;
    }
    
    /**
     * Zeigt die Hilfe-Informationen an
     */
    private static void showHelp() {
        System.out.println("FXSSI Data Extractor v2.3 (mit Fear & Greed Index)");
        System.out.println("==================================================");
        System.out.println();
        System.out.println("Verwendung:");
        System.out.println("  java FXSSIDataExtractor [OPTIONEN]");
        System.out.println();
        System.out.println("Optionen:");
        System.out.println("  " + GUI_ARG + "        Startet die grafische Benutzeroberfläche");
        System.out.println("  " + CONSOLE_ARG + "    Startet im Console-Modus (Standard)");
        System.out.println("  " + DATA_DIR_ARG + " <PFAD>  Setzt das Datenverzeichnis (Standard: ./data)");
        System.out.println("  " + HELP_ARG + "       Zeigt diese Hilfe an");
        System.out.println();
        System.out.println("Beispiele:");
        System.out.println("  java FXSSIDataExtractor " + GUI_ARG);
        System.out.println("  java FXSSIDataExtractor " + CONSOLE_ARG + " " + DATA_DIR_ARG + " /home/user/fxssi-data");
        System.out.println("  java FXSSIDataExtractor " + DATA_DIR_ARG + " C:\\FXSSIData " + GUI_ARG);
        System.out.println("  java FXSSIDataExtractor " + DATA_DIR_ARG + " ./custom-data");
        System.out.println();
        System.out.println("Datenstruktur:");
        System.out.println("- Tägliche Dateien: <datenverzeichnis>/fxssi_data_YYYY-MM-DD.csv");
        System.out.println("- Währungspaar-Dateien: <datenverzeichnis>/currency_pairs/EUR_USD.csv");
        System.out.println("- Beide Formate werden parallel gepflegt");
        System.out.println();
        System.out.println("Datenverzeichnis-Hinweise:");
        System.out.println("- Relative Pfade sind erlaubt (z.B. ./data, ../shared-data)");
        System.out.println("- Absolute Pfade sind erlaubt (z.B. /var/data, C:\\Data)");
        System.out.println("- Das Verzeichnis wird automatisch erstellt falls es nicht existiert");
        System.out.println("- Standard-Verzeichnis ist './data' im Arbeitsverzeichnis");
        System.out.println();
        System.out.println("Im GUI-Modus:");
        System.out.println("- Grafische Anzeige der Live-Sentiment-Daten");
        System.out.println("- Konfigurierbare Auto-Refresh-Intervalle");
        System.out.println("- Interaktive Tabelle mit Ratio-Balken und Signal-Icons");
        System.out.println("- Daten werden bei jedem Refresh in beide Formate gespeichert");
        System.out.println("- NEUE: Signalwechsel-Erkennung mit visuellen Indikatoren");
        System.out.println("- NEUE: Klickbare Signalwechsel-Historie pro Währungspaar");
        System.out.println("- NEUE: CNN Fear & Greed Index als BTC/USD integriert");
        System.out.println("  → Fear (0-44) = BUY Signal, Neutral (45-55), Greed (56-100) = SELL Signal");
        System.out.println();
        System.out.println("Im Console-Modus:");
        System.out.println("- Automatische stündliche Datenextraktion");
        System.out.println("- Duale CSV-Speicherung im konfigurierten Verzeichnis");
        System.out.println("- Automatische Signalwechsel-Erkennung und -Logging");
        System.out.println("- Inklusive Fear & Greed Index für BTC/USD");
        System.out.println("- Läuft als Hintergrund-Service");
    }
    
    /**
     * Analysiert die Command Line Argumente
     */
    private static CommandLineConfig parseArguments(String[] args) {
        CommandLineConfig config = new CommandLineConfig();
        
        if (args.length == 0) {
            return config; // Standard-Konfiguration
        }
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            switch (arg.toLowerCase()) {
                case GUI_ARG:
                    config.mode = AppMode.GUI;
                    break;
                case CONSOLE_ARG:
                    config.mode = AppMode.CONSOLE;
                    break;
                case DATA_DIR_ARG:
                    // Nächstes Argument sollte der Pfad sein
                    if (i + 1 < args.length) {
                        config.dataDirectory = args[i + 1];
                        i++; // Überspringe das nächste Argument
                    } else {
                        System.err.println("Fehler: " + DATA_DIR_ARG + " benötigt einen Pfad als Parameter");
                        config.mode = AppMode.HELP;
                        return config;
                    }
                    break;
                case HELP_ARG:
                case "-h":
                case "/h":
                case "/?":
                    config.mode = AppMode.HELP;
                    return config;
                default:
                    System.err.println("Unbekanntes Argument: " + arg);
                    config.mode = AppMode.HELP;
                    return config;
            }
        }
        
        return config;
    }
    
    /**
     * Main-Methode - Einstiegspunkt des Programms
     */
    public static void main(String[] args) {
        try {
            // Parse Command Line Argumente
            CommandLineConfig config = parseArguments(args);
            
            switch (config.mode) {
                case GUI:
                    LOGGER.info("Starte FXSSI Data Extractor im GUI-Modus...");
                    LOGGER.info("Datenverzeichnis: " + config.dataDirectory);
                    LOGGER.info("Duale Speicherung: Tägliche UND währungspaar-spezifische Dateien");
                    LOGGER.info("Fear & Greed Index (BTC/USD) aktiviert");
                    FXSSIGuiApplication.launchGui(args, config.dataDirectory);
                    break;
                    
                case CONSOLE:
                    LOGGER.info("Starte FXSSI Data Extractor im Console-Modus...");
                    LOGGER.info("Datenverzeichnis: " + config.dataDirectory);
                    LOGGER.info("Duale Speicherung: Tägliche UND währungspaar-spezifische Dateien");
                    LOGGER.info("Fear & Greed Index (BTC/USD) aktiviert");
                    startConsoleMode(config.dataDirectory);
                    break;
                    
                case HELP:
                    showHelp();
                    System.exit(0);
                    break;
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Kritischer Fehler beim Starten des Programms: " + e.getMessage(), e);
            System.err.println("Kritischer Fehler: " + e.getMessage());
            System.err.println("Verwenden Sie --help für weitere Informationen.");
            System.exit(1);
        }
    }
    
    /**
     * Startet den Console-Modus mit konfiguriertem Datenverzeichnis
     */
    private static void startConsoleMode(String dataDirectory) {
        FXSSIDataExtractor extractor = new FXSSIDataExtractor(false, dataDirectory);
        
        // Füge einen Shutdown Hook hinzu für sauberes Beenden
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            extractor.stop();
        }));
        
        extractor.start();
        
        // Halte das Programm am Leben
        try {
            while (true) {
                Thread.sleep(60000); // Sleep für 1 Minute
            }
        } catch (InterruptedException e) {
            LOGGER.info("Programm wurde unterbrochen - beende ordnungsgemäß");
            extractor.stop();
        }
    }
    
    /**
     * Konfigurationsklasse für Command Line Argumente
     */
    private static class CommandLineConfig {
        AppMode mode = AppMode.GUI;
        String dataDirectory = DEFAULT_DATA_DIRECTORY;
    }
    
    /**
     * Enum für die verschiedenen Anwendungsmodi
     */
    private enum AppMode {
        GUI,
        CONSOLE,
        HELP
    }
}