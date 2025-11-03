package com.fxssi.extractor.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.notification.EmailConfig;

/**
 * Manager für die Verwaltung der zuletzt gesendeten E-Mail-Signale
 * Verhindert E-Mail-Spam durch Tracking der letzten gesendeten Signale pro Währungspaar
 * Verwendet einen konfigurierbaren Threshold um zu entscheiden ob eine neue E-Mail gesendet werden soll
 * ERWEITERT um MetaTrader-Synchronisation mit Dual-Directory-Support
 * 
 * @author Generated for FXSSI Email Anti-Spam System
 * @version 1.1 - MetaTrader Dual-Directory Sync
 */
public class LastSentSignalManager {
    
    private static final Logger LOGGER = Logger.getLogger(LastSentSignalManager.class.getName());
    private static final String SIGNAL_CHANGES_SUBDIRECTORY = "signal_changes";
    private static final String LAST_SENT_FILE = "lastsend.csv";
    private static final String MT_SYNC_FILE = "last_known_signals.csv"; // NEU: MetaTrader-Sync-Datei
    
    private final String dataDirectory;
    private final Path signalChangesPath;
    private final Path lastSentFilePath;
    private final Path mtSyncFilePath; // NEU: Pfad zur MetaTrader-Sync-Datei
    private final ReentrantLock managerLock = new ReentrantLock();
    
    // Cache für zuletzt gesendete Signale pro Währungspaar
    private final ConcurrentHashMap<String, LastSentSignal> lastSentSignals;
    
    // NEU: Referenz zur EmailConfig für MetaTrader-Sync
    private EmailConfig emailConfig;
    
    /**
     * Konstruktor mit Standard-Datenverzeichnis
     */
    public LastSentSignalManager() {
        this("data");
    }
    
    /**
     * Konstruktor mit konfigurierbarem Datenverzeichnis
     * @param dataDirectory Pfad zum Hauptdatenverzeichnis
     */
    public LastSentSignalManager(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.signalChangesPath = Paths.get(dataDirectory, SIGNAL_CHANGES_SUBDIRECTORY);
        this.lastSentFilePath = signalChangesPath.resolve(LAST_SENT_FILE);
        this.mtSyncFilePath = signalChangesPath.resolve(MT_SYNC_FILE); // NEU
        
        this.lastSentSignals = new ConcurrentHashMap<>();
        
        LOGGER.info("LastSentSignalManager initialisiert für Verzeichnis: " + dataDirectory);
        LOGGER.info("Letzte gesendete Signale werden gespeichert in: " + lastSentFilePath.toAbsolutePath());
        LOGGER.info("MetaTrader-Sync-Datei: " + mtSyncFilePath.toAbsolutePath());
    }
    
    /**
     * NEU: Setzt die EmailConfig für MetaTrader-Synchronisation
     * @param emailConfig Die E-Mail-Konfiguration mit MetaTrader-Verzeichnissen
     */
    public void setEmailConfig(EmailConfig emailConfig) {
        this.emailConfig = emailConfig;
        LOGGER.info("EmailConfig für MetaTrader-Sync gesetzt (Sync: " + 
                   (emailConfig != null && emailConfig.isMetatraderSyncEnabled()) + ")");
    }
    
    /**
     * Erstellt das Verzeichnis für Signal-Dateien falls es nicht existiert
     */
    public void createSignalChangesDirectory() {
        try {
            if (!Files.exists(signalChangesPath)) {
                Files.createDirectories(signalChangesPath);
                LOGGER.info("Signal-Verzeichnis erstellt: " + signalChangesPath.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Erstellen des Signal-Verzeichnisses: " + e.getMessage(), e);
            throw new RuntimeException("Konnte Signal-Verzeichnis nicht erstellen", e);
        }
    }
    
    /**
     * Lädt die zuletzt gesendeten Signale beim Start
     */
    public void loadLastSentSignals() {
        LOGGER.info("=== DEBUG: loadLastSentSignals gestartet ===");
        
        managerLock.lock();
        try {
            createSignalChangesDirectory();
            
            LOGGER.info("DEBUG: Datei-Pfad: " + lastSentFilePath.toAbsolutePath());
            LOGGER.info("DEBUG: Datei existiert: " + Files.exists(lastSentFilePath));
            
            if (!Files.exists(lastSentFilePath)) {
                LOGGER.info("DEBUG: Keine gespeicherten letzten gesendeten Signale gefunden - beginne mit leerer Liste");
                return;
            }
            
            long fileSize = Files.size(lastSentFilePath);
            LOGGER.info("DEBUG: Datei-Größe: " + fileSize + " Bytes");
            
            if (fileSize == 0) {
                LOGGER.warning("DEBUG: WARNUNG - Datei ist leer!");
                return;
            }
            
            LOGGER.info("DEBUG: Öffne BufferedReader...");
            try (BufferedReader reader = Files.newBufferedReader(lastSentFilePath, StandardCharsets.UTF_8)) {
                String line;
                boolean isFirstLine = true;
                int lineNumber = 0;
                int loadedCount = 0;
                int skippedCount = 0;
                
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    LOGGER.fine("DEBUG: Lese Zeile " + lineNumber + ": " + line);
                    
                    if (isFirstLine) {
                        isFirstLine = false;
                        LOGGER.info("DEBUG: Header übersprungen: " + line);
                        continue; // Überspringe Header
                    }
                    
                    try {
                        LastSentSignal lastSent = LastSentSignal.fromCsvLine(line);
                        lastSentSignals.put(lastSent.getCurrencyPair(), lastSent);
                        loadedCount++;
                        LOGGER.fine("DEBUG: Signal geladen: " + lastSent.toString());
                    } catch (Exception e) {
                        skippedCount++;
                        LOGGER.warning("DEBUG: Ungültige LastSent-Zeile übersprungen (Zeile " + lineNumber + "): " + line + " - Fehler: " + e.getMessage());
                    }
                }
                
                LOGGER.info("DEBUG: Datei-Analyse abgeschlossen:");
                LOGGER.info("DEBUG:   - Gesamte Zeilen: " + lineNumber);
                LOGGER.info("DEBUG:   - Erfolgreich geladen: " + loadedCount);
                LOGGER.info("DEBUG:   - Übersprungen/Fehler: " + skippedCount);
                LOGGER.info("DEBUG:   - Cache-Größe: " + lastSentSignals.size());
                
                LOGGER.info("DEBUG: Geladene Signale:");
                for (Map.Entry<String, LastSentSignal> entry : lastSentSignals.entrySet()) {
                    LOGGER.info("DEBUG:   " + entry.getKey() + " -> " + entry.getValue().toString());
                }
                
                LOGGER.info("Letzte gesendete Signale geladen: " + lastSentSignals.size() + " Währungspaare");
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "DEBUG: FEHLER beim Laden der letzten gesendeten Signale: " + e.getMessage(), e);
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "DEBUG: IO-FEHLER bei loadLastSentSignals: " + e.getMessage(), e);
        } finally {
            managerLock.unlock();
            LOGGER.info("=== DEBUG: loadLastSentSignals beendet ===");
        }
    }
    
    /**
     * HAUPTMETHODE: Prüft ob eine E-Mail für ein neues Signal gesendet werden soll
     * @param currencyPair Das Währungspaar
     * @param newSignal Das neue Signal
     * @param newBuyPercentage Die neue Buy-Percentage
     * @param thresholdPercent Der konfigurierte Threshold in Prozent
     * @return true wenn E-Mail gesendet werden soll, false wenn Threshold nicht erreicht
     */
    public boolean shouldSendEmail(String currencyPair, 
                                 CurrencyPairData.TradingSignal newSignal, 
                                 double newBuyPercentage, 
                                 double thresholdPercent) {
        managerLock.lock();
        try {
            LastSentSignal lastSent = lastSentSignals.get(currencyPair);
            
            // Wenn noch nie eine E-Mail gesendet wurde, sende erste E-Mail
            if (lastSent == null) {
                LOGGER.info(String.format("Erste E-Mail für %s: %s bei %.1f%%", 
                    currencyPair, newSignal.getDescription(), newBuyPercentage));
                return true;
            }
            
            // Berechne die Differenz zwischen neuem und letztem Wert
            double buyPercentDiff = Math.abs(newBuyPercentage - lastSent.getBuyPercentage());
            
            // Prüfe ob Threshold erreicht
            boolean thresholdReached = buyPercentDiff >= thresholdPercent;
            
            if (thresholdReached) {
                LOGGER.info(String.format("Threshold erreicht für %s: %.1f%% → %.1f%% (Differenz: %.1f%% >= %.1f%%)", 
                    currencyPair, lastSent.getBuyPercentage(), newBuyPercentage, buyPercentDiff, thresholdPercent));
            } else {
                LOGGER.fine(String.format("Threshold NICHT erreicht für %s: %.1f%% → %.1f%% (Differenz: %.1f%% < %.1f%%)", 
                    currencyPair, lastSent.getBuyPercentage(), newBuyPercentage, buyPercentDiff, thresholdPercent));
            }
            
            return thresholdReached;
            
        } finally {
            managerLock.unlock();
        }
    }
    
    /**
     * Registriert ein gesendetes Signal
     * ERWEITERT um automatische MetaTrader-Synchronisation
     * 
     * @param currencyPair Das Währungspaar
     * @param signal Das gesendete Signal
     * @param buyPercentage Die Buy-Percentage zum Zeitpunkt der E-Mail
     */
    public void recordSentSignal(String currencyPair, 
                                CurrencyPairData.TradingSignal signal, 
                                double buyPercentage) {
        managerLock.lock();
        try {
            LastSentSignal lastSent = new LastSentSignal(
                currencyPair, 
                signal, 
                buyPercentage, 
                LocalDateTime.now()
            );
            
            lastSentSignals.put(currencyPair, lastSent);
            
            LOGGER.info(String.format("Signal registriert: %s - %s (%.1f%%)", 
                currencyPair, signal.getDescription(), buyPercentage));
            
            // Speichere in lastsend.csv
            saveLastSentSignals();
            
            // NEU: Synchronisiere zu MetaTrader-Verzeichnissen
            syncToMetaTraderDirectories();
            
        } finally {
            managerLock.unlock();
        }
    }
    
    /**
     * NEU: Synchronisiert die Signale zu MetaTrader-Verzeichnissen
     * Erstellt last_known_signals.csv im Format: Währungspaar;Letztes_Signal;Prozent
     * Kopiert die Datei in 1 oder 2 Verzeichnisse abhängig von der Konfiguration
     * Führt Währungsersetzung durch: XAUUSD → GOLD, XAGUSD → SILBER
     */
    private void syncToMetaTraderDirectories() {
        // Prüfe ob MetaTrader-Sync aktiviert ist
        if (emailConfig == null || !emailConfig.isMetatraderSyncEnabled()) {
            LOGGER.fine("MetaTrader-Sync nicht aktiviert - überspringe Synchronisation");
            return;
        }
        
        try {
            // Erstelle die MetaTrader-Sync-Datei im lokalen Verzeichnis
            createMetaTraderSyncFile();
            
            // Kopiere zu konfigurierten Verzeichnissen
            int copiedCount = 0;
            
            // Verzeichnis 1
            if (emailConfig.hasMetatraderDirectory()) {
                String dir1 = emailConfig.getMetatraderDirectory();
                if (copyToMetaTraderDirectory(dir1, "Verzeichnis 1")) {
                    copiedCount++;
                }
            }
            
            // Verzeichnis 2
            if (emailConfig.hasMetatraderDirectory2()) {
                String dir2 = emailConfig.getMetatraderDirectory2();
                if (copyToMetaTraderDirectory(dir2, "Verzeichnis 2")) {
                    copiedCount++;
                }
            }
            
            if (copiedCount > 0) {
                LOGGER.info("MetaTrader-Sync erfolgreich: " + copiedCount + " Verzeichnis" + 
                           (copiedCount > 1 ? "se" : "") + " aktualisiert");
            } else {
                LOGGER.warning("MetaTrader-Sync aktiviert, aber kein Verzeichnis konfiguriert oder Kopieren fehlgeschlagen");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei MetaTrader-Synchronisation: " + e.getMessage(), e);
        }
    }
    
    /**
     * NEU: Erstellt die MetaTrader-Sync-Datei im lokalen Verzeichnis
     */
    private void createMetaTraderSyncFile() throws IOException {
        createSignalChangesDirectory();
        
        try (BufferedWriter writer = Files.newBufferedWriter(mtSyncFilePath, StandardCharsets.UTF_8)) {
            // Schreibe Header (OHNE Zeitstempel!)
            writer.write("Währungspaar;Letztes_Signal;Prozent");
            writer.newLine();
            
            // Schreibe alle Signale
            for (LastSentSignal signal : lastSentSignals.values()) {
                String currencyPair = signal.getCurrencyPair();
                
                // NEU: Währungsersetzung für MetaTrader
                String mtCurrencyPair = replaceCurrencyForMetaTrader(currencyPair);
                
                // Format: Währungspaar;Signal;Prozent (gerundet auf ganze Zahl)
                String line = String.format("%s;%s;%d",
                    mtCurrencyPair,
                    signal.getSignal().name(),
                    Math.round(signal.getBuyPercentage())
                );
                
                writer.write(line);
                writer.newLine();
            }
            
            writer.flush();
        }
        
        LOGGER.fine("MetaTrader-Sync-Datei erstellt: " + mtSyncFilePath.toAbsolutePath() + 
                   " (" + lastSentSignals.size() + " Einträge)");
    }
    
    /**
     * NEU: Ersetzt spezielle Währungspaare für MetaTrader
     * XAUUSD → GOLD
     * XAGUSD → SILBER
     * 
     * @param currencyPair Original-Währungspaar
     * @return Ersetztes Währungspaar für MetaTrader
     */
    private String replaceCurrencyForMetaTrader(String currencyPair) {
        if (currencyPair == null) {
            return currencyPair;
        }
        
        // Ersetze XAUUSD mit GOLD
        if (currencyPair.equalsIgnoreCase("XAUUSD") || currencyPair.equalsIgnoreCase("XAU/USD")) {
            return "GOLD";
        }
        
        // Ersetze XAGUSD mit SILBER
        if (currencyPair.equalsIgnoreCase("XAGUSD") || currencyPair.equalsIgnoreCase("XAG/USD")) {
            return "SILBER";
        }
        
        return currencyPair;
    }
    
    /**
     * NEU: Kopiert die MetaTrader-Sync-Datei in ein Zielverzeichnis
     * 
     * @param targetDirectory Das Zielverzeichnis
     * @param dirLabel Label für Logging (z.B. "Verzeichnis 1")
     * @return true wenn erfolgreich kopiert, false bei Fehler
     */
    private boolean copyToMetaTraderDirectory(String targetDirectory, String dirLabel) {
        try {
            Path targetDir = Paths.get(targetDirectory);
            
            // Prüfe ob Verzeichnis existiert
            if (!Files.exists(targetDir)) {
                LOGGER.warning("MetaTrader " + dirLabel + " existiert nicht: " + targetDirectory);
                return false;
            }
            
            if (!Files.isDirectory(targetDir)) {
                LOGGER.warning("MetaTrader " + dirLabel + " ist kein Verzeichnis: " + targetDirectory);
                return false;
            }
            
            // Kopiere Datei
            Path targetFile = targetDir.resolve(MT_SYNC_FILE);
            Files.copy(mtSyncFilePath, targetFile, StandardCopyOption.REPLACE_EXISTING);
            
            LOGGER.info("MetaTrader-Sync-Datei kopiert nach " + dirLabel + ": " + targetFile.toAbsolutePath());
            return true;
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Fehler beim Kopieren zu MetaTrader " + dirLabel + ": " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Gibt Statistiken über gesendete Signale zurück
     */
    public String getStatistics() {
        managerLock.lock();
        try {
            StringBuilder stats = new StringBuilder();
            stats.append("LastSent-Signal-Statistiken:\n");
            stats.append("============================\n");
            stats.append("Gespeicherte Währungspaare: ").append(lastSentSignals.size()).append("\n");
            
            if (!lastSentSignals.isEmpty()) {
                stats.append("\nLetzte gesendete Signale:\n");
                for (Map.Entry<String, LastSentSignal> entry : lastSentSignals.entrySet()) {
                    LastSentSignal signal = entry.getValue();
                    stats.append(String.format("  %s: %s (%.1f%%) - %s\n",
                        signal.getCurrencyPair(),
                        signal.getSignal().getDescription(),
                        signal.getBuyPercentage(),
                        signal.getSentTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                    ));
                }
            }
            
            // NEU: MetaTrader-Sync-Status
            if (emailConfig != null && emailConfig.isMetatraderSyncEnabled()) {
                stats.append("\nMetaTrader-Synchronisation: AKTIVIERT\n");
                stats.append("Konfigurierte Verzeichnisse: ").append(emailConfig.getMetatraderDirectoryCount()).append("\n");
                if (emailConfig.hasMetatraderDirectory()) {
                    stats.append("  Dir 1: ").append(emailConfig.getMetatraderDirectory()).append("\n");
                }
                if (emailConfig.hasMetatraderDirectory2()) {
                    stats.append("  Dir 2: ").append(emailConfig.getMetatraderDirectory2()).append("\n");
                }
                stats.append("Sync-Datei: ").append(MT_SYNC_FILE).append("\n");
                stats.append("Format: Währungspaar;Letztes_Signal;Prozent\n");
                stats.append("Ersetzungen: XAUUSD→GOLD, XAGUSD→SILBER\n");
            } else {
                stats.append("\nMetaTrader-Synchronisation: DEAKTIVIERT\n");
            }
            
            return stats.toString();
            
        } finally {
            managerLock.unlock();
        }
    }
    
    /**
     * Gibt das zuletzt gesendete Signal für ein Währungspaar zurück
     */
    public LastSentSignal getLastSentSignal(String currencyPair) {
        return lastSentSignals.get(currencyPair);
    }
    
    /**
     * Prüft ob für ein Währungspaar bereits ein Signal gesendet wurde
     */
    public boolean hasLastSentSignal(String currencyPair) {
        return lastSentSignals.containsKey(currencyPair);
    }
    
    /**
     * Gibt die Anzahl gespeicherter Signale zurück
     */
    public int getSignalCount() {
        return lastSentSignals.size();
    }
    
    /**
     * Löscht das zuletzt gesendete Signal für ein Währungspaar
     */
    public void clearLastSentSignal(String currencyPair) {
        managerLock.lock();
        try {
            if (lastSentSignals.remove(currencyPair) != null) {
                LOGGER.info("Letztes gesendetes Signal gelöscht für: " + currencyPair);
                saveLastSentSignals();
                
                // NEU: Synchronisiere nach Löschung
                syncToMetaTraderDirectories();
            }
            
        } finally {
            managerLock.unlock();
        }
    }
    
    /**
     * NEU: Bereinigt alte Einträge aus lastSentSignals die älter als X Tage sind
     * @param daysToKeep Anzahl der Tage, die Einträge behalten werden sollen
     */
    public void cleanupOldEntries(int daysToKeep) {
        if (daysToKeep <= 0) {
            LOGGER.warning("cleanupOldEntries: Ungültiger daysToKeep-Wert: " + daysToKeep + " - überspringe Bereinigung");
            return;
        }
        
        managerLock.lock();
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
            int removedCount = 0;
            int totalCount = lastSentSignals.size();
            
            LOGGER.info("Starte Bereinigung alter LastSent-Einträge (älter als " + daysToKeep + " Tage)");
            LOGGER.fine("Cutoff-Datum: " + cutoffDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            
            // Entferne alle Einträge die älter als cutoffDate sind
            List<String> keysToRemove = lastSentSignals.entrySet().stream()
                .filter(entry -> entry.getValue().getSentTime().isBefore(cutoffDate))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            for (String key : keysToRemove) {
                LastSentSignal removed = lastSentSignals.remove(key);
                if (removed != null) {
                    removedCount++;
                    LOGGER.fine("Entfernt: " + removed.toString());
                }
            }
            
            if (removedCount > 0) {
                LOGGER.info("Bereinigung abgeschlossen: " + removedCount + " von " + totalCount + 
                           " Einträgen entfernt (verbleibend: " + lastSentSignals.size() + ")");
                
                // Speichere bereinigte Daten
                saveLastSentSignals();
                
                // Synchronisiere nach Bereinigung
                syncToMetaTraderDirectories();
            } else {
                LOGGER.info("Bereinigung abgeschlossen: Keine alten Einträge gefunden (alle " + 
                           totalCount + " Einträge sind aktuell)");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Bereinigen alter LastSent-Einträge: " + e.getMessage(), e);
        } finally {
            managerLock.unlock();
        }
    }
    
    /**
     * Fährt den Manager ordnungsgemäß herunter
     */
    public void shutdown() {
        LOGGER.info("Fahre LastSentSignalManager herunter...");
        
        try {
            // Speichere letzte Zustände
            saveLastSentSignals();
            
            // NEU: Finale MetaTrader-Sync
            syncToMetaTraderDirectories();
            
            // Cache leeren
            lastSentSignals.clear();
            
            LOGGER.info("LastSentSignalManager heruntergefahren");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Herunterfahren des LastSentSignalManager: " + e.getMessage(), e);
        }
    }
    
    // ===== PRIVATE HILFSMETHODEN =====
    
    /**
     * Speichert die zuletzt gesendeten Signale in die Datei
     */
    private void saveLastSentSignals() {
        LOGGER.info("=== DEBUG: saveLastSentSignals gestartet ===");
        
        try {
            LOGGER.info("DEBUG: Erstelle Signal-Verzeichnis...");
            createSignalChangesDirectory();
            LOGGER.info("DEBUG: Signal-Verzeichnis OK");
            
            LOGGER.info("DEBUG: Datei-Pfad: " + lastSentFilePath.toAbsolutePath());
            LOGGER.info("DEBUG: Verzeichnis existiert: " + Files.exists(signalChangesPath));
            LOGGER.info("DEBUG: Datei existiert vor Schreibung: " + Files.exists(lastSentFilePath));
            
            LOGGER.info("DEBUG: Cache enthält " + lastSentSignals.size() + " Einträge:");
            for (Map.Entry<String, LastSentSignal> entry : lastSentSignals.entrySet()) {
                LOGGER.info("DEBUG:   " + entry.getKey() + " -> " + entry.getValue().toCsvLine());
            }
            
            LOGGER.info("DEBUG: Öffne BufferedWriter...");
            try (BufferedWriter writer = Files.newBufferedWriter(lastSentFilePath, StandardCharsets.UTF_8)) {
                LOGGER.info("DEBUG: Schreibe Header...");
                String header = LastSentSignal.getCsvHeader();
                LOGGER.info("DEBUG: Header: " + header);
                writer.write(header);
                writer.newLine();
                
                LOGGER.info("DEBUG: Schreibe " + lastSentSignals.size() + " Datenzeilen...");
                int lineCount = 0;
                for (LastSentSignal signal : lastSentSignals.values()) {
                    String csvLine = signal.toCsvLine();
                    LOGGER.info("DEBUG: Zeile " + (++lineCount) + ": " + csvLine);
                    writer.write(csvLine);
                    writer.newLine();
                }
                
                LOGGER.info("DEBUG: Alle Daten geschrieben, rufe flush() auf...");
                writer.flush();
                LOGGER.info("DEBUG: flush() abgeschlossen");
            }
            
            // Verifikation nach dem Schreiben
            LOGGER.info("DEBUG: Datei existiert nach Schreibung: " + Files.exists(lastSentFilePath));
            if (Files.exists(lastSentFilePath)) {
                long fileSize = Files.size(lastSentFilePath);
                LOGGER.info("DEBUG: Datei-Größe nach Schreibung: " + fileSize + " Bytes");
                
                if (fileSize > 0) {
                    LOGGER.info("DEBUG: Lese Datei zur Verifikation...");
                    List<String> lines = Files.readAllLines(lastSentFilePath, StandardCharsets.UTF_8);
                    LOGGER.info("DEBUG: Datei enthält " + lines.size() + " Zeilen:");
                    for (int i = 0; i < lines.size() && i < 10; i++) { // Maximal 10 Zeilen loggen
                        LOGGER.info("DEBUG:   Zeile " + (i+1) + ": " + lines.get(i));
                    }
                } else {
                    LOGGER.warning("DEBUG: WARNUNG - Datei ist leer nach dem Schreiben!");
                }
            } else {
                LOGGER.severe("DEBUG: FEHLER - Datei existiert nicht nach dem Schreiben!");
            }
            
            LOGGER.info("DEBUG: Letzte gesendete Signale gespeichert: " + lastSentSignals.size() + " Einträge");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "DEBUG: IO-FEHLER beim Speichern der letzten gesendeten Signale: " + e.getMessage(), e);
            LOGGER.severe("DEBUG: Datei-Pfad: " + lastSentFilePath.toAbsolutePath());
            LOGGER.severe("DEBUG: Verzeichnis-Rechte: " + (Files.isWritable(signalChangesPath) ? "Schreibbar" : "Nicht schreibbar"));
            throw new RuntimeException("Fehler beim Speichern der LastSent-Signale", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "DEBUG: UNBEKANNTER FEHLER beim Speichern: " + e.getMessage(), e);
            throw new RuntimeException("Unbekannter Fehler beim Speichern der LastSent-Signale", e);
        } finally {
            LOGGER.info("=== DEBUG: saveLastSentSignals beendet ===");
        }
    }
    
    // ===== INNERE KLASSEN =====
    
    /**
     * Datenklasse für zuletzt gesendete Signale
     */
    public static class LastSentSignal {
        private final String currencyPair;
        private final CurrencyPairData.TradingSignal signal;
        private final double buyPercentage;
        private final LocalDateTime sentTime;
        
        public LastSentSignal(String currencyPair, CurrencyPairData.TradingSignal signal, 
                            double buyPercentage, LocalDateTime sentTime) {
            this.currencyPair = currencyPair;
            this.signal = signal;
            this.buyPercentage = buyPercentage;
            this.sentTime = sentTime;
        }
        
        /**
         * Erstellt LastSentSignal aus CSV-Zeile
         */
        public static LastSentSignal fromCsvLine(String csvLine) {
            String[] parts = csvLine.split(";");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Ungültiges LastSent CSV-Format: " + csvLine);
            }
            
            try {
                String currencyPair = parts[0];
                CurrencyPairData.TradingSignal signal = CurrencyPairData.TradingSignal.valueOf(parts[1]);
                double buyPercentage = Double.parseDouble(parts[2].replace(",", "."));
                LocalDateTime sentTime = LocalDateTime.parse(parts[3], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                return new LastSentSignal(currencyPair, signal, buyPercentage, sentTime);
            } catch (Exception e) {
                throw new IllegalArgumentException("Fehler beim Parsen der LastSent CSV-Zeile: " + csvLine, e);
            }
        }
        
        /**
         * Formatiert als CSV-Zeile für Speicherung
         */
        public String toCsvLine() {
            return String.format("%s;%s;%.2f;%s",
                currencyPair,
                signal.name(),
                buyPercentage,
                sentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        /**
         * CSV-Header für Dateiexport
         */
        public static String getCsvHeader() {
            return "Währungspaar;Signal;Buy_Prozent;Zeitstempel";
        }
        
        // Getter
        public String getCurrencyPair() { return currencyPair; }
        public CurrencyPairData.TradingSignal getSignal() { return signal; }
        public double getBuyPercentage() { return buyPercentage; }
        public LocalDateTime getSentTime() { return sentTime; }
        
        @Override
        public String toString() {
            return String.format("LastSentSignal{%s: %s (%.1f%%) - %s}", 
                currencyPair, signal.getDescription(), buyPercentage, 
                sentTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        }
    }
}