package com.fxssi.extractor.storage;

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.model.SignalChangeEvent;
import com.fxssi.extractor.notification.EmailService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manager für die Verwaltung und Speicherung von Signalwechsel-Ereignissen
 * Erkennt automatisch Signalwechsel und speichert sie persistent
 * ERWEITERT um EmailService-Integration mit Threshold-basierter E-Mail-Versendung
 * ERWEITERT um MetaTrader-Datei-Synchronisation
 * FIXED: MetaTrader-Sync wird jetzt bei JEDEM Refresh ausgeführt
 * 
 * @author Generated for FXSSI Signal Change Detection
 * @version 1.2.1 - FIX: MetaTrader-Sync bei jedem Refresh
 */
public class SignalChangeHistoryManager {
    
    private static final Logger LOGGER = Logger.getLogger(SignalChangeHistoryManager.class.getName());
    private static final String SIGNAL_CHANGES_SUBDIRECTORY = "signal_changes";
    private static final String SIGNAL_CHANGES_FILE = "signal_changes_history.csv";
    private static final String LAST_SIGNALS_FILE = "last_known_signals.csv";
    
    private final String dataDirectory;
    private final Path signalChangesPath;
    private final Path historyFilePath;
    private final Path lastSignalsFilePath;
    private final ReentrantLock managerLock = new ReentrantLock();
    
    // Cache für letzte bekannte Signale pro Währungspaar
    private final ConcurrentHashMap<String, CurrencyPairData.TradingSignal> lastKnownSignals;
    private final ConcurrentHashMap<String, List<SignalChangeEvent>> changeHistoryCache;
    
    // EmailService für Threshold-basierte Benachrichtigungen
    private EmailService emailService;
    private boolean emailNotificationsEnabled = false;
    
    // NEU: MetaTrader-Verzeichnis-Synchronisation
    private String metatraderFileDir = null;
    private boolean metatraderSyncEnabled = false;
    
    /**
     * Konstruktor mit Standard-Datenverzeichnis (ohne E-Mail)
     */
    public SignalChangeHistoryManager() {
        this("data");
    }
    
    /**
     * Konstruktor mit konfigurierbarem Datenverzeichnis (ohne E-Mail)
     * @param dataDirectory Pfad zum Hauptdatenverzeichnis
     */
    public SignalChangeHistoryManager(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.signalChangesPath = Paths.get(dataDirectory, SIGNAL_CHANGES_SUBDIRECTORY);
        this.historyFilePath = signalChangesPath.resolve(SIGNAL_CHANGES_FILE);
        this.lastSignalsFilePath = signalChangesPath.resolve(LAST_SIGNALS_FILE);
        
        this.lastKnownSignals = new ConcurrentHashMap<>();
        this.changeHistoryCache = new ConcurrentHashMap<>();
        this.emailService = null;
        this.emailNotificationsEnabled = false;
        
        LOGGER.info("SignalChangeHistoryManager initialisiert für Verzeichnis: " + dataDirectory + " (ohne E-Mail)");
        LOGGER.info("Signalwechsel-Dateien werden gespeichert in: " + signalChangesPath.toAbsolutePath());
    }
    
    /**
     * Konstruktor mit EmailService für Threshold-basierte Benachrichtigungen
     * @param dataDirectory Pfad zum Hauptdatenverzeichnis
     * @param emailService EmailService für Benachrichtigungen
     */
    public SignalChangeHistoryManager(String dataDirectory, EmailService emailService) {
        this.dataDirectory = dataDirectory;
        this.signalChangesPath = Paths.get(dataDirectory, SIGNAL_CHANGES_SUBDIRECTORY);
        this.historyFilePath = signalChangesPath.resolve(SIGNAL_CHANGES_FILE);
        this.lastSignalsFilePath = signalChangesPath.resolve(LAST_SIGNALS_FILE);
        
        this.lastKnownSignals = new ConcurrentHashMap<>();
        this.changeHistoryCache = new ConcurrentHashMap<>();
        this.emailService = emailService;
        this.emailNotificationsEnabled = (emailService != null);
        
        LOGGER.info("SignalChangeHistoryManager initialisiert für Verzeichnis: " + dataDirectory + 
                   " (mit E-Mail-Benachrichtigungen: " + emailNotificationsEnabled + ")");
        LOGGER.info("Signalwechsel-Dateien werden gespeichert in: " + signalChangesPath.toAbsolutePath());
    }
    
    /**
     * Setzt den EmailService für Benachrichtigungen
     * @param emailService EmailService für Threshold-basierte Benachrichtigungen
     */
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
        this.emailNotificationsEnabled = (emailService != null);
        LOGGER.info("EmailService gesetzt - E-Mail-Benachrichtigungen: " + emailNotificationsEnabled);
    }
    
    /**
     * Aktiviert/Deaktiviert E-Mail-Benachrichtigungen
     * @param enabled true = aktiviert, false = deaktiviert
     */
    public void setEmailNotificationsEnabled(boolean enabled) {
        this.emailNotificationsEnabled = enabled && (emailService != null);
        LOGGER.info("E-Mail-Benachrichtigungen " + (emailNotificationsEnabled ? "aktiviert" : "deaktiviert"));
    }
    
    /**
     * NEU: Setzt das MetaTrader-Verzeichnis für Datei-Synchronisation
     * @param metatraderFileDir Pfad zum MetaTrader-Verzeichnis
     * @throws IllegalArgumentException wenn das Verzeichnis nicht existiert
     */
    public void setMetatraderFileDir(String metatraderFileDir) {
        if (metatraderFileDir == null || metatraderFileDir.trim().isEmpty()) {
            this.metatraderFileDir = null;
            this.metatraderSyncEnabled = false;
            LOGGER.info("MetaTrader-Synchronisation deaktiviert");
            return;
        }
        
        Path mtDir = Paths.get(metatraderFileDir.trim());
        if (!Files.exists(mtDir)) {
            LOGGER.severe("MetaTrader-Verzeichnis existiert nicht: " + mtDir.toAbsolutePath());
            throw new IllegalArgumentException("MetaTrader-Verzeichnis existiert nicht: " + mtDir.toAbsolutePath());
        }
        
        if (!Files.isDirectory(mtDir)) {
            LOGGER.severe("MetaTrader-Pfad ist kein Verzeichnis: " + mtDir.toAbsolutePath());
            throw new IllegalArgumentException("MetaTrader-Pfad ist kein Verzeichnis: " + mtDir.toAbsolutePath());
        }
        
        if (!Files.isWritable(mtDir)) {
            LOGGER.severe("MetaTrader-Verzeichnis ist nicht beschreibbar: " + mtDir.toAbsolutePath());
            throw new IllegalArgumentException("MetaTrader-Verzeichnis ist nicht beschreibbar: " + mtDir.toAbsolutePath());
        }
        
        this.metatraderFileDir = metatraderFileDir.trim();
        this.metatraderSyncEnabled = true;
        
        LOGGER.info("MetaTrader-Synchronisation aktiviert für Verzeichnis: " + mtDir.toAbsolutePath());
        
        // Führe sofortige Synchronisation durch, wenn last_known_signals.csv bereits existiert
        syncLastKnownSignalsToMetaTrader();
    }
    
    /**
     * NEU: Gibt das aktuelle MetaTrader-Verzeichnis zurück
     * @return MetaTrader-Verzeichnispfad oder null wenn nicht gesetzt
     */
    public String getMetatraderFileDir() {
        return metatraderFileDir;
    }
    
    /**
     * NEU: Prüft ob MetaTrader-Synchronisation aktiviert ist
     * @return true wenn aktiviert und Verzeichnis gültig
     */
    public boolean isMetatraderSyncEnabled() {
        return metatraderSyncEnabled && metatraderFileDir != null;
    }
    
    /**
     * Erstellt das Verzeichnis für Signalwechsel-Dateien falls es nicht existiert
     */
    public void createSignalChangesDirectory() {
        try {
            if (!Files.exists(signalChangesPath)) {
                Files.createDirectories(signalChangesPath);
                LOGGER.info("Signalwechsel-Verzeichnis erstellt: " + signalChangesPath.toAbsolutePath());
            } else {
                LOGGER.info("Signalwechsel-Verzeichnis existiert bereits: " + signalChangesPath.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Erstellen des Signalwechsel-Verzeichnisses: " + e.getMessage(), e);
            throw new RuntimeException("Konnte Signalwechsel-Verzeichnis nicht erstellen", e);
        }
    }
    
    /**
     * Lädt die letzten bekannten Signale beim Start
     */
    public void loadLastKnownSignals() {
        managerLock.lock();
        try {
            createSignalChangesDirectory();
            
            if (!Files.exists(lastSignalsFilePath)) {
                LOGGER.info("Keine gespeicherten letzten Signale gefunden - beginne mit leerer Liste");
                return;
            }
            
            try (BufferedReader reader = Files.newBufferedReader(lastSignalsFilePath, StandardCharsets.UTF_8)) {
                String line;
                boolean isFirstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue; // Überspringe Header
                    }
                    
                    String[] parts = line.split(";");
                    if (parts.length == 2) {
                        String currencyPair = parts[0];
                        CurrencyPairData.TradingSignal signal = CurrencyPairData.TradingSignal.valueOf(parts[1]);
                        lastKnownSignals.put(currencyPair, signal);
                    }
                }
                
                LOGGER.info("Letzte bekannte Signale geladen: " + lastKnownSignals.size() + " Währungspaare");
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Fehler beim Laden der letzten Signale: " + e.getMessage(), e);
            }
            
        } finally {
            managerLock.unlock();
        }
    }
    
    /**
     * HAUPTMETHODE: Verarbeitet neue Währungsdaten und erkennt Signalwechsel
     * ERWEITERT um Threshold-basierte E-Mail-Versendung
     * ✅ FIXED: MetaTrader-Sync wird jetzt IMMER ausgeführt (auch ohne Signalwechsel)
     * @param newData Liste der neuen Währungsdaten
     * @return Liste der erkannten Signalwechsel
     */
    public List<SignalChangeEvent> processNewData(List<CurrencyPairData> newData) {
        if (newData == null || newData.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<SignalChangeEvent> detectedChanges = new ArrayList<>();
        
        managerLock.lock();
        try {
            LOGGER.fine("Verarbeite " + newData.size() + " Datensätze für Signalwechsel-Erkennung...");
            
            for (CurrencyPairData data : newData) {
                String currencyPair = data.getCurrencyPair();
                CurrencyPairData.TradingSignal currentSignal = data.getTradingSignal();
                CurrencyPairData.TradingSignal lastSignal = lastKnownSignals.get(currencyPair);
                
                // Prüfe auf Signalwechsel
                if (lastSignal != null && lastSignal != currentSignal) {
                    // SIGNALWECHSEL ERKANNT!
                    SignalChangeEvent changeEvent = new SignalChangeEvent(
                        currencyPair,
                        lastSignal,
                        currentSignal,
                        data.getTimestamp(),
                        getLastBuyPercentageForPair(currencyPair, data.getBuyPercentage()),
                        data.getBuyPercentage()
                    );
                    
                    detectedChanges.add(changeEvent);
                    
                    LOGGER.info(String.format("SIGNALWECHSEL ERKANNT: %s - %s (Wichtigkeit: %s)", 
                        currencyPair, changeEvent.getChangeDescription(), changeEvent.getImportance().getDescription()));
                }
                
                // Aktualisiere letztes bekanntes Signal
                lastKnownSignals.put(currencyPair, currentSignal);
            }
            
            // Speichere erkannte Wechsel
            if (!detectedChanges.isEmpty()) {
                saveSignalChanges(detectedChanges);
                
                // Aktualisiere Cache
                updateChangeHistoryCache(detectedChanges);
                
                LOGGER.info("Signalwechsel-Verarbeitung abgeschlossen: " + detectedChanges.size() + " Wechsel erkannt");
            } else {
                LOGGER.fine("Keine Signalwechsel erkannt");
            }
            
            // ✅ FIX: IMMER speichern und synchronisieren, auch ohne Signalwechsel
            // Dies stellt sicher dass bei jedem Refresh (auch ohne Signalwechsel) 
            // die MetaTrader-Datei aktualisiert wird
            saveLastKnownSignals();
            
            // Threshold-basierte E-Mail-Versendung
            if (emailNotificationsEnabled && emailService != null) {
                try {
                    LOGGER.fine("Prüfe Threshold-basierte E-Mail-Versendung für " + newData.size() + " Datensätze...");
                    
                    EmailService.EmailSendResult result = emailService.sendSignalChangeNotificationWithThreshold(newData);
                    
                    if (result.isSuccess()) {
                        LOGGER.info("Threshold-E-Mail erfolgreich versendet: " + result.getMessage());
                    } else {
                        LOGGER.fine("Keine Threshold-E-Mail versendet: " + result.getMessage());
                    }
                    
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Fehler bei Threshold-E-Mail-Versendung: " + e.getMessage(), e);
                }
            } else {
                LOGGER.fine("E-Mail-Benachrichtigungen deaktiviert oder EmailService nicht verfügbar");
            }
            
        } finally {
            managerLock.unlock();
        }
        
        return detectedChanges;
    }
    
    /**
     * Holt die komplette Signalwechsel-Historie für ein Währungspaar
     * @param currencyPair Das Währungspaar
     * @return Liste aller Signalwechsel für dieses Paar
     */
    public List<SignalChangeEvent> getSignalChangeHistory(String currencyPair) {
        // Prüfe Cache zuerst
        List<SignalChangeEvent> cachedHistory = changeHistoryCache.get(currencyPair);
        if (cachedHistory != null) {
            return new ArrayList<>(cachedHistory);
        }
        
        // Lade aus Datei
        List<SignalChangeEvent> allChanges = loadAllSignalChanges();
        List<SignalChangeEvent> pairChanges = allChanges.stream()
            .filter(change -> change.getCurrencyPair().equals(currencyPair))
            .sorted((a, b) -> b.getChangeTime().compareTo(a.getChangeTime())) // Neueste zuerst
            .collect(Collectors.toList());
        
        // Cache aktualisieren
        changeHistoryCache.put(currencyPair, pairChanges);
        
        LOGGER.fine("Signalwechsel-Historie für " + currencyPair + " geladen: " + pairChanges.size() + " Einträge");
        return new ArrayList<>(pairChanges);
    }
    
    /**
     * Holt die letzten N Signalwechsel für ein Währungspaar
     * @param currencyPair Das Währungspaar
     * @param count Anzahl der gewünschten Wechsel
     * @return Liste der letzten Signalwechsel
     */
    public List<SignalChangeEvent> getRecentSignalChanges(String currencyPair, int count) {
        List<SignalChangeEvent> allChanges = getSignalChangeHistory(currencyPair);
        
        if (count <= 0 || allChanges.isEmpty()) {
            return new ArrayList<>();
        }
        
        int endIndex = Math.min(count, allChanges.size());
        return new ArrayList<>(allChanges.subList(0, endIndex));
    }
    
    /**
     * Holt Signalwechsel für ein Währungspaar innerhalb der letzten X Stunden
     * @param currencyPair Das Währungspaar
     * @param hours Anzahl Stunden zurück
     * @return Liste der Signalwechsel in diesem Zeitraum
     */
    public List<SignalChangeEvent> getSignalChangesWithinHours(String currencyPair, int hours) {
        List<SignalChangeEvent> allChanges = getSignalChangeHistory(currencyPair);
        
        return allChanges.stream()
            .filter(change -> change.isWithinHours(hours))
            .collect(Collectors.toList());
    }
    
    /**
     * Prüft ob es für ein Währungspaar aktuelle Signalwechsel gibt
     * @param currencyPair Das Währungspaar
     * @return SignalChangeEvent wenn aktueller Wechsel vorhanden, sonst null
     */
    public SignalChangeEvent getMostRecentChangeForPair(String currencyPair) {
        List<SignalChangeEvent> recentChanges = getRecentSignalChanges(currencyPair, 1);
        return recentChanges.isEmpty() ? null : recentChanges.get(0);
    }
    
    /**
     * Gibt Statistiken über alle Signalwechsel UND E-Mail-Status zurück
     * @return Statistik-String
     */
    public String getSignalChangeStatistics() {
        try {
            List<SignalChangeEvent> allChanges = loadAllSignalChanges();
            
            Map<String, Integer> changeCountPerPair = new HashMap<>();
            Map<SignalChangeEvent.SignalChangeImportance, Integer> changeCountPerImportance = new HashMap<>();
            int recentChanges = 0;
            
            for (SignalChangeEvent change : allChanges) {
                // Zähle pro Währungspaar
                changeCountPerPair.merge(change.getCurrencyPair(), 1, Integer::sum);
                
                // Zähle pro Wichtigkeit
                changeCountPerImportance.merge(change.getImportance(), 1, Integer::sum);
                
                // Zähle aktuelle Wechsel (letzten 24 Stunden)
                if (change.isWithinHours(24)) {
                    recentChanges++;
                }
            }
            
            StringBuilder stats = new StringBuilder();
            stats.append("Signalwechsel-Statistiken:\n");
            stats.append("=========================\n");
            stats.append("Gesamt Signalwechsel: ").append(allChanges.size()).append("\n");
            stats.append("Aktuelle Wechsel (24h): ").append(recentChanges).append("\n");
            stats.append("Überwachte Währungspaare: ").append(lastKnownSignals.size()).append("\n\n");
            
            stats.append("Nach Wichtigkeit:\n");
            for (SignalChangeEvent.SignalChangeImportance importance : SignalChangeEvent.SignalChangeImportance.values()) {
                int count = changeCountPerImportance.getOrDefault(importance, 0);
                stats.append("  ").append(importance.getIcon()).append(" ").append(importance.getDescription()).append(": ").append(count).append("\n");
            }
            
            // E-Mail-Status
            stats.append("\nE-Mail-Benachrichtigungen:\n");
            stats.append("==========================\n");
            stats.append("Status: ").append(emailNotificationsEnabled ? "Aktiviert" : "Deaktiviert").append("\n");
            stats.append("EmailService: ").append(emailService != null ? "Verfügbar" : "Nicht verfügbar").append("\n");
            
            if (emailService != null) {
                try {
                    String emailStats = emailService.getEmailStatistics();
                    stats.append("\n").append(emailStats);
                } catch (Exception e) {
                    stats.append("Fehler beim Abrufen der E-Mail-Statistiken: ").append(e.getMessage()).append("\n");
                }
            }
            
            // NEU: MetaTrader-Synchronisation Status
            stats.append("\nMetaTrader-Synchronisation:\n");
            stats.append("============================\n");
            stats.append("Status: ").append(metatraderSyncEnabled ? "Aktiviert" : "Deaktiviert").append("\n");
            if (metatraderFileDir != null) {
                stats.append("Verzeichnis: ").append(metatraderFileDir).append("\n");
                Path mtPath = Paths.get(metatraderFileDir);
                stats.append("Verzeichnis existiert: ").append(Files.exists(mtPath) ? "Ja" : "Nein").append("\n");
                if (Files.exists(mtPath)) {
                    Path syncFile = mtPath.resolve(LAST_SIGNALS_FILE);
                    stats.append("Sync-Datei existiert: ").append(Files.exists(syncFile) ? "Ja" : "Nein").append("\n");
                }
            } else {
                stats.append("Verzeichnis: Nicht konfiguriert\n");
            }
            
            return stats.toString();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Abrufen der Signalwechsel-Statistiken: " + e.getMessage(), e);
            return "Fehler beim Abrufen der Statistiken";
        }
    }
    
    /**
     * Bereinigt alte Signalwechsel-Daten
     * @param daysToKeep Anzahl Tage die behalten werden sollen
     */
    public void cleanupOldSignalChanges(int daysToKeep) {
        managerLock.lock();
        try {
            LOGGER.info("Bereinige Signalwechsel älter als " + daysToKeep + " Tage...");
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
            List<SignalChangeEvent> allChanges = loadAllSignalChanges();
            
            List<SignalChangeEvent> filteredChanges = allChanges.stream()
                .filter(change -> change.getChangeTime().isAfter(cutoffDate))
                .collect(Collectors.toList());
            
            int removedCount = allChanges.size() - filteredChanges.size();
            
            if (removedCount > 0) {
                // Schreibe gefilterte Daten zurück
                rewriteSignalChangesFile(filteredChanges);
                
                // Cache leeren
                changeHistoryCache.clear();
                
                LOGGER.info("Signalwechsel-Bereinigung abgeschlossen: " + removedCount + " alte Einträge entfernt");
            } else {
                LOGGER.info("Keine alten Signalwechsel zum Bereinigen gefunden");
            }
            
            // Bereinige auch LastSentSignalManager falls verfügbar
            if (emailService != null) {
                try {
                    emailService.getLastSentSignalManager().cleanupOldEntries(daysToKeep);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Fehler beim Bereinigen der LastSent-Daten: " + e.getMessage(), e);
                }
            }
            
        } finally {
            managerLock.unlock();
        }
    }
    
    /**
     * Fährt den Manager ordnungsgemäß herunter
     */
    public void shutdown() {
        LOGGER.info("Fahre SignalChangeHistoryManager herunter...");
        
        try {
            // Speichere letzte bekannte Signale (inkl. MetaTrader-Sync)
            saveLastKnownSignals();
            
            // Cache leeren
            lastKnownSignals.clear();
            changeHistoryCache.clear();
            
            // EmailService shutdown (falls vorhanden)
            if (emailService != null) {
                try {
                    emailService.shutdown();
                    LOGGER.info("EmailService heruntergefahren");
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Fehler beim Herunterfahren des EmailService: " + e.getMessage(), e);
                }
            }
            
            LOGGER.info("SignalChangeHistoryManager heruntergefahren");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Herunterfahren des SignalChangeHistoryManager: " + e.getMessage(), e);
        }
    }
    
    // ===== GETTER FÜR INTEGRATION =====
    
    /**
     * Gibt den aktuellen EmailService zurück
     * @return EmailService oder null
     */
    public EmailService getEmailService() {
        return emailService;
    }
    
    /**
     * Prüft ob E-Mail-Benachrichtigungen aktiviert sind
     * @return true wenn aktiviert und EmailService verfügbar
     */
    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled && emailService != null;
    }
    
    // ===== PRIVATE HILFSMETHODEN =====
    
    /**
     * Speichert Signalwechsel-Ereignisse in die Datei
     */
    private void saveSignalChanges(List<SignalChangeEvent> changes) {
        try {
            createSignalChangesDirectory();
            
            boolean needsHeader = !Files.exists(historyFilePath) || Files.size(historyFilePath) == 0;
            
            try (BufferedWriter writer = Files.newBufferedWriter(historyFilePath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                
                if (needsHeader) {
                    writer.write(SignalChangeEvent.getCsvHeader());
                    writer.newLine();
                }
                
                for (SignalChangeEvent change : changes) {
                    writer.write(change.toCsvLine());
                    writer.newLine();
                }
                
                writer.flush();
            }
            
            LOGGER.fine("Signalwechsel gespeichert: " + changes.size() + " Einträge");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Speichern der Signalwechsel: " + e.getMessage(), e);
        }
    }
    
    /**
     * Speichert die letzten bekannten Signale
     * ERWEITERT um MetaTrader-Synchronisation mit verbessertem Logging
     * ✅ Diese Methode ruft IMMER syncLastKnownSignalsToMetaTrader() auf
     */
    private void saveLastKnownSignals() {
        try {
            createSignalChangesDirectory();
            
            LOGGER.info("Speichere " + lastKnownSignals.size() + " letzte bekannte Signale...");
            
            try (BufferedWriter writer = Files.newBufferedWriter(lastSignalsFilePath, StandardCharsets.UTF_8)) {
                writer.write("Währungspaar;Letztes_Signal");
                writer.newLine();
                
                for (Map.Entry<String, CurrencyPairData.TradingSignal> entry : lastKnownSignals.entrySet()) {
                    writer.write(entry.getKey() + ";" + entry.getValue().name());
                    writer.newLine();
                }
                
                writer.flush();
            }
            
            LOGGER.fine("Letzte bekannte Signale gespeichert: " + lastKnownSignals.size() + " Währungspaare");
            LOGGER.info("✅ Datei last_known_signals.csv erfolgreich geschrieben");
            
            // ✅ WICHTIG: Synchronisiere IMMER mit MetaTrader nach dem Speichern
            syncLastKnownSignalsToMetaTrader();
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Fehler beim Speichern der letzten Signale: " + e.getMessage(), e);
        }
    }
    
    /**
     * NEU: Synchronisiert die last_known_signals.csv ins MetaTrader-Verzeichnis
     * ERWEITERT um Währungspaar-Konvertierung für MetaTrader-Kompatibilität
     * VERBESSERT: Detailliertes Logging für jeden Synchronisationsvorgang
     */
    private void syncLastKnownSignalsToMetaTrader() {
        if (!metatraderSyncEnabled || metatraderFileDir == null) {
            LOGGER.fine("MetaTrader-Synchronisation übersprungen (deaktiviert oder nicht konfiguriert)");
            return;
        }
        
        LOGGER.info("🔄 Starte MetaTrader-Synchronisation...");
        LOGGER.info("   Zielverzeichnis: " + metatraderFileDir);
        LOGGER.info("   Anzahl Signale: " + lastKnownSignals.size());
        
        try {
            Path mtDir = Paths.get(metatraderFileDir);
            
            // Prüfe Verzeichnis erneut vor Synchronisation
            if (!Files.exists(mtDir)) {
                LOGGER.warning("❌ MetaTrader-Verzeichnis existiert nicht mehr: " + mtDir.toAbsolutePath() + 
                              " - Deaktiviere Synchronisation");
                metatraderSyncEnabled = false;
                return;
            }
            
            if (!Files.isDirectory(mtDir)) {
                LOGGER.warning("❌ MetaTrader-Pfad ist kein Verzeichnis: " + mtDir.toAbsolutePath() + 
                              " - Deaktiviere Synchronisation");
                metatraderSyncEnabled = false;
                return;
            }
            
            if (!Files.isWritable(mtDir)) {
                LOGGER.warning("❌ MetaTrader-Verzeichnis ist nicht beschreibbar: " + mtDir.toAbsolutePath() + 
                              " - Deaktiviere Synchronisation");
                metatraderSyncEnabled = false;
                return;
            }
            
            // Prüfe ob Quelldatei existiert
            if (!Files.exists(lastSignalsFilePath)) {
                LOGGER.fine("ℹ️ Quelldatei last_known_signals.csv existiert noch nicht - keine Synchronisation");
                return;
            }
            
            // Pfade für temporäre und finale Datei
            Path tmpFile = mtDir.resolve("last_known_signals_tmp.csv");
            Path targetFile = mtDir.resolve(LAST_SIGNALS_FILE);
            
            LOGGER.info("   Temporäre Datei: " + tmpFile.getFileName());
            LOGGER.info("   Ziel-Datei: " + targetFile.getFileName());
            
            // Schritt 1: Erstelle temporäre Datei mit konvertierten Währungspaaren
            createConvertedMetaTraderFile(tmpFile);
            
            // Schritt 2: Benenne temporäre Datei um zur finalen Datei
            Files.move(tmpFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            
            LOGGER.info("✅ MetaTrader-Datei erfolgreich synchronisiert!");
            LOGGER.info("   Datei: " + targetFile.toAbsolutePath());
            LOGGER.info("   Währungspaare konvertiert (EUR/USD→EURUSD, XAUUSD→GOLD, XAGUSD→SILVER)");
            LOGGER.info("   Synchronisation abgeschlossen");
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "❌ Fehler bei MetaTrader-Synchronisation: " + e.getMessage(), e);
            LOGGER.warning("MetaTrader-Synchronisation aufgrund von Fehlern temporär deaktiviert");
            // Nicht dauerhaft deaktivieren, da es temporäre I/O-Probleme sein könnten
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "❌ Unerwarteter Fehler bei MetaTrader-Synchronisation: " + e.getMessage(), e);
        }
    }
    
    /**
     * NEU: Erstellt die MetaTrader-kompatible Datei mit konvertierten Währungspaaren
     * ERWEITERT: Detailliertes Logging für Konvertierungen
     * @param tmpFilePath Pfad zur temporären Datei im MetaTrader-Verzeichnis
     */
    private void createConvertedMetaTraderFile(Path tmpFilePath) throws IOException {
        LOGGER.info("📝 Erstelle MetaTrader-kompatible Datei: " + tmpFilePath.getFileName());
        
        try (BufferedWriter writer = Files.newBufferedWriter(tmpFilePath, StandardCharsets.UTF_8)) {
            // Header schreiben (konvertiert)
            writer.write("Waehrungspaar;Letztes_Signal");
            writer.newLine();
            
            int convertedCount = 0;
            int totalCount = 0;
            
            // Konvertiere und schreibe Daten
            for (Map.Entry<String, CurrencyPairData.TradingSignal> entry : lastKnownSignals.entrySet()) {
                String originalPair = entry.getKey();
                String convertedPair = convertCurrencyPairForMetaTrader(originalPair);
                CurrencyPairData.TradingSignal signal = entry.getValue();
                
                writer.write(convertedPair + ";" + signal.name());
                writer.newLine();
                
                totalCount++;
                if (!originalPair.equals(convertedPair)) {
                    convertedCount++;
                    LOGGER.fine("   Konvertiert: " + originalPair + " → " + convertedPair);
                }
            }
            
            writer.flush();
            
            LOGGER.info("✅ MetaTrader-Datei erstellt:");
            LOGGER.info("   Gesamt: " + totalCount + " Währungspaare");
            LOGGER.info("   Konvertiert: " + convertedCount + " Währungspaare");
            LOGGER.info("   Unverändert: " + (totalCount - convertedCount) + " Währungspaare");
        }
    }
    
    /**
     * NEU: Konvertiert Währungspaare für MetaTrader-Kompatibilität
     * ERWEITERT: Logging für spezielle Konvertierungen
     * @param originalPair Ursprüngliches Währungspaar (z.B. "EUR/USD")
     * @return MetaTrader-kompatibles Währungspaar (z.B. "EURUSD")
     */
    private String convertCurrencyPairForMetaTrader(String originalPair) {
        if (originalPair == null || originalPair.trim().isEmpty()) {
            return originalPair;
        }
        
        String converted = originalPair.trim();
        
        // Schritt 1: Entferne Schrägstriche (/) - EUR/USD → EURUSD
        converted = converted.replace("/", "");
        
        // Schritt 2: Spezielle Konvertierungen für Edelmetalle
        switch (converted.toUpperCase()) {
            case "XAUUSD":
                LOGGER.fine("   Spezialkonvertierung: XAUUSD → GOLD");
                converted = "GOLD";
                break;
            case "XAGUSD":
                LOGGER.fine("   Spezialkonvertierung: XAGUSD → SILVER");
                converted = "SILVER";
                break;
            // Weitere spezielle Konvertierungen können hier hinzugefügt werden
            default:
                // Keine weitere Konvertierung nötig
                break;
        }
        
        return converted;
    }
    
    /**
     * Lädt alle Signalwechsel aus der Datei
     */
    private List<SignalChangeEvent> loadAllSignalChanges() {
        List<SignalChangeEvent> changes = new ArrayList<>();
        
        if (!Files.exists(historyFilePath)) {
            return changes;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(historyFilePath, StandardCharsets.UTF_8)) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Überspringe Header
                }
                
                try {
                    SignalChangeEvent change = SignalChangeEvent.fromCsvLine(line);
                    changes.add(change);
                } catch (Exception e) {
                    LOGGER.fine("Ungültige Signalwechsel-Zeile übersprungen: " + line);
                }
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Fehler beim Laden der Signalwechsel: " + e.getMessage(), e);
        }
        
        return changes;
    }
    
    /**
     * Schreibt die komplette Signalwechsel-Datei neu
     */
    private void rewriteSignalChangesFile(List<SignalChangeEvent> changes) {
        try {
            if (Files.exists(historyFilePath)) {
                Files.delete(historyFilePath);
            }
            
            if (!changes.isEmpty()) {
                try (BufferedWriter writer = Files.newBufferedWriter(historyFilePath, StandardCharsets.UTF_8)) {
                    writer.write(SignalChangeEvent.getCsvHeader());
                    writer.newLine();
                    
                    for (SignalChangeEvent change : changes) {
                        writer.write(change.toCsvLine());
                        writer.newLine();
                    }
                    
                    writer.flush();
                }
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Neuschreiben der Signalwechsel-Datei: " + e.getMessage(), e);
        }
    }
    
    /**
     * Aktualisiert den Cache für Signalwechsel-Historie
     */
    private void updateChangeHistoryCache(List<SignalChangeEvent> newChanges) {
        for (SignalChangeEvent change : newChanges) {
            String currencyPair = change.getCurrencyPair();
            
            changeHistoryCache.computeIfAbsent(currencyPair, k -> new ArrayList<>()).add(0, change);
            
            // Begrenze Cache-Größe pro Währungspaar
            List<SignalChangeEvent> pairHistory = changeHistoryCache.get(currencyPair);
            if (pairHistory.size() > 100) { // Maximal 100 Einträge im Cache
                pairHistory.subList(100, pairHistory.size()).clear();
            }
        }
    }
    
    /**
     * Hilfsmethode um letzte Buy-Percentage für ein Währungspaar zu ermitteln
     */
    private double getLastBuyPercentageForPair(String currencyPair, double fallback) {
        // Vereinfachte Implementierung - in einer echten Anwendung würde man
        // dies aus der CurrencyPairDataManager oder einem anderen Cache holen
        return fallback; // Placeholder
    }
}