
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
 * Unterstützt sowohl Console-Modus als auch GUI-Modus
 * 
 * @author Generated for FXSSI Data Extraction
 * @version 2.0 (mit GUI-Support)
 */
public class FXSSIDataExtractor {
    
    private static final Logger LOGGER = Logger.getLogger(FXSSIDataExtractor.class.getName());
    private static final String DATA_DIRECTORY = "data";
    
    // Command Line Argumente
    private static final String GUI_ARG = "--gui";
    private static final String CONSOLE_ARG = "--console";
    private static final String HELP_ARG = "--help";
    
    private FXSSIScraper scraper;
    private DataFileManager fileManager;
    private HourlyScheduler scheduler;
    private boolean isGuiMode = false;
    
    /**
     * Konstruktor für Console-Modus
     */
    public FXSSIDataExtractor() {
        this(false);
    }
    
    /**
     * Konstruktor mit Modus-Auswahl
     * @param guiMode true für GUI-Modus, false für Console-Modus
     */
    public FXSSIDataExtractor(boolean guiMode) {
        this.isGuiMode = guiMode;
        
        if (!guiMode) {
            // Nur für Console-Modus initialisieren
            this.scraper = new FXSSIScraper();
            this.fileManager = new DataFileManager(DATA_DIRECTORY);
            this.scheduler = new HourlyScheduler(this::extractAndSaveData);
            
            // Erstelle das data Verzeichnis falls es nicht existiert
            fileManager.createDataDirectory();
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
     * Zeigt die Hilfe-Informationen an
     */
    private static void showHelp() {
        System.out.println("FXSSI Data Extractor v2.0");
        System.out.println("=========================");
        System.out.println();
        System.out.println("Verwendung:");
        System.out.println("  java FXSSIDataExtractor [OPTIONEN]");
        System.out.println();
        System.out.println("Optionen:");
        System.out.println("  " + GUI_ARG + "     Startet die grafische Benutzeroberfläche");
        System.out.println("  " + CONSOLE_ARG + " Startet im Console-Modus (Standard)");
        System.out.println("  " + HELP_ARG + "    Zeigt diese Hilfe an");
        System.out.println();
        System.out.println("Beispiele:");
        System.out.println("  java FXSSIDataExtractor " + GUI_ARG);
        System.out.println("  java FXSSIDataExtractor " + CONSOLE_ARG);
        System.out.println("  java FXSSIDataExtractor");
        System.out.println();
        System.out.println("Im GUI-Modus:");
        System.out.println("- Grafische Anzeige der Live-Sentiment-Daten");
        System.out.println("- Konfigurierbare Auto-Refresh-Intervalle");
        System.out.println("- Interaktive Tabelle mit Ratio-Balken und Signal-Icons");
        System.out.println();
        System.out.println("Im Console-Modus:");
        System.out.println("- Automatische stündliche Datenextraktion");
        System.out.println("- CSV-Speicherung in ./data/ Verzeichnis");
        System.out.println("- Läuft als Hintergrund-Service");
    }
    
    /**
     * Analysiert die Command Line Argumente
     */
    private static AppMode parseArguments(String[] args) {
        if (args.length == 0) {
            return AppMode.CONSOLE; // Standard-Modus
        }
        
        for (String arg : args) {
            switch (arg.toLowerCase()) {
                case GUI_ARG:
                    return AppMode.GUI;
                case CONSOLE_ARG:
                    return AppMode.CONSOLE;
                case HELP_ARG:
                case "-h":
                case "/h":
                case "/?":
                    return AppMode.HELP;
                default:
                    System.err.println("Unbekanntes Argument: " + arg);
                    return AppMode.HELP;
            }
        }
        
        return AppMode.CONSOLE;
    }
    
    /**
     * Main-Methode - Einstiegspunkt des Programms
     */
    public static void main(String[] args) {
        try {
            // Parse Command Line Argumente
            AppMode mode = parseArguments(args);
            
            switch (mode) {
                case GUI:
                    LOGGER.info("Starte FXSSI Data Extractor im GUI-Modus...");
                    FXSSIGuiApplication.launchGui(args);
                    break;
                    
                case CONSOLE:
                    LOGGER.info("Starte FXSSI Data Extractor im Console-Modus...");
                    startConsoleMode();
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
     * Startet den Console-Modus
     */
    private static void startConsoleMode() {
        FXSSIDataExtractor extractor = new FXSSIDataExtractor(false);
        
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
     * Enum für die verschiedenen Anwendungsmodi
     */
    private enum AppMode {
        GUI,
        CONSOLE,
        HELP
    }
}