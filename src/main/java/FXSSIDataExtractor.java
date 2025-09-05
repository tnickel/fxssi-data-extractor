import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.scheduler.HourlyScheduler;
import com.fxssi.extractor.scraper.FXSSIScraper;
import com.fxssi.extractor.storage.DataFileManager;
import com.fxsssi.extractor.gui.FXSSIGuiApplication;

/**
 * Erweiterte Hauptklasse für das FXSSI Datenextraktions-Programm
 * Unterstützt sowohl Console-Modus als auch GUI-Modus mit konfigurierbarem Root-Pfad
 * 
 * @author Generated for FXSSI Data Extraction
 * @version 2.1 (mit konfigurierbarem Root-Pfad)
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
    private DataFileManager fileManager;
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
            this.scraper = new FXSSIScraper();
            this.fileManager = new DataFileManager(this.dataDirectory);
            this.scheduler = new HourlyScheduler(this::extractAndSaveData);
            
            // Erstelle das data Verzeichnis falls es nicht existiert
            fileManager.createDataDirectory();
        }
        
        LOGGER.info("FXSSIDataExtractor initialisiert - Modus: " + (guiMode ? "GUI" : "Console") + 
                   ", Datenverzeichnis: " + this.dataDirectory);
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
        
        // Führe eine initiale Extraktion durch
        extractAndSaveData();
        
        // Starte den stündlichen Scheduler
        scheduler.startScheduling();
        
        LOGGER.info("Stündlicher Scheduler aktiv - Daten werden alle 60 Minuten aktualisiert");
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
     * Führt die Datenextraktion durch und speichert die Ergebnisse
     * Diese Methode wird stündlich vom Scheduler aufgerufen
     */
    private void extractAndSaveData() {
        try {
            LOGGER.info("Beginne Datenextraktion von FXSSI...");
            
            List<CurrencyPairData> currentData = scraper.extractCurrentRatioData();
            
            if (currentData != null && !currentData.isEmpty()) {
                fileManager.appendDataToFile(currentData);
                LOGGER.info("Erfolgreich " + currentData.size() + " Währungspaare extrahiert und gespeichert");
                
                // Logge eine Zusammenfassung der extrahierten Daten
                logDataSummary(currentData);
            } else {
                LOGGER.warning("Keine Daten von FXSSI erhalten - möglicherweise Website-Problem");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler bei der Datenextraktion: " + e.getMessage(), e);
        }
    }
    
    /**
     * Loggt eine Zusammenfassung der extrahierten Daten
     */
    private void logDataSummary(List<CurrencyPairData> data) {
        for (CurrencyPairData pair : data) {
            LOGGER.info(String.format("Extrahiert: %s - Buy: %.1f%%, Sell: %.1f%%, Signal: %s", 
                pair.getCurrencyPair(), pair.getBuyPercentage(), pair.getSellPercentage(), pair.getTradingSignal()));
        }
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
        System.out.println("FXSSI Data Extractor v2.1");
        System.out.println("=========================");
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
        System.out.println();
        System.out.println("Im Console-Modus:");
        System.out.println("- Automatische stündliche Datenextraktion");
        System.out.println("- CSV-Speicherung im konfigurierten Verzeichnis");
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
                    FXSSIGuiApplication.launchGui(args, config.dataDirectory);
                    break;
                    
                case CONSOLE:
                    LOGGER.info("Starte FXSSI Data Extractor im Console-Modus...");
                    LOGGER.info("Datenverzeichnis: " + config.dataDirectory);
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
        AppMode mode = AppMode.CONSOLE;
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