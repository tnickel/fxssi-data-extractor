

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.scraper.FXSSIScraper;
import com.fxssi.extractor.storage.DataFileManager;
import com.fxssi.extractor.scheduler.HourlyScheduler;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Hauptklasse für das FXSSI Datenextraktions-Programm
 * Startet den stündlichen Scraping-Prozess für Forex Sentiment Daten
 * 
 * @author Generated for FXSSI Data Extraction
 * @version 1.0
 */
public class FXSSIDataExtractor {
    
    private static final Logger LOGGER = Logger.getLogger(FXSSIDataExtractor.class.getName());
    private static final String DATA_DIRECTORY = "data";
    
    private FXSSIScraper scraper;
    private DataFileManager fileManager;
    private HourlyScheduler scheduler;
    
    /**
     * Konstruktor initialisiert alle notwendigen Komponenten
     */
    public FXSSIDataExtractor() {
        this.scraper = new FXSSIScraper();
        this.fileManager = new DataFileManager(DATA_DIRECTORY);
        this.scheduler = new HourlyScheduler(this::extractAndSaveData);
        
        // Erstelle das data Verzeichnis falls es nicht existiert
        fileManager.createDataDirectory();
    }
    
    /**
     * Startet das Programm und beginnt mit der stündlichen Datenextraktion
     */
    public void start() {
        LOGGER.info("FXSSI Data Extractor gestartet - Beginne mit stündlicher Datenextraktion");
        
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
     * Main-Methode - Einstiegspunkt des Programms
     */
    public static void main(String[] args) {
        try {
            FXSSIDataExtractor extractor = new FXSSIDataExtractor();
            
            // Füge einen Shutdown Hook hinzu für sauberes Beenden
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                extractor.stop();
            }));
            
            extractor.start();
            
            // Halte das Programm am Leben
            while (true) {
                Thread.sleep(60000); // Sleep für 1 Minute
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Kritischer Fehler beim Starten des Programms: " + e.getMessage(), e);
            System.exit(1);
        }
    }
}