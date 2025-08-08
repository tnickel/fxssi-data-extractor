package com.fxsssi.extractor.gui;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager für die automatische Aktualisierung der GUI-Daten
 * Ermöglicht konfigurierbare Refresh-Intervalle in Minuten
 * 
 * @author Generated for FXSSI Data Extraction GUI
 * @version 1.0
 */
public class DataRefreshManager {
    
    private static final Logger LOGGER = Logger.getLogger(DataRefreshManager.class.getName());
    private static final int MIN_REFRESH_INTERVAL = 1; // Minimum 1 Minute
    private static final int MAX_REFRESH_INTERVAL = 60; // Maximum 60 Minuten
    
    private final ScheduledExecutorService scheduler;
    private final Runnable refreshTask;
    private ScheduledFuture<?> currentRefreshTask;
    private boolean isRunning = false;
    private int currentIntervalMinutes = 5; // Standard: 5 Minuten
    
    /**
     * Konstruktor
     * @param refreshTask Die Aufgabe die für das Refresh ausgeführt werden soll
     */
    public DataRefreshManager(Runnable refreshTask) {
        this.refreshTask = refreshTask;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GUI-DataRefresh-Thread");
            t.setDaemon(true); // Daemon Thread für ordnungsgemäßes Beenden
            return t;
        });
        
        LOGGER.info("DataRefreshManager initialisiert");
    }
    
    /**
     * Startet das automatische Refresh mit dem angegebenen Intervall
     * @param intervalMinutes Intervall in Minuten (1-60)
     */
    public void startAutoRefresh(int intervalMinutes) {
        // Validiere Intervall
        intervalMinutes = validateInterval(intervalMinutes);
        
        // Stoppe eventuell laufendes Refresh
        stopAutoRefresh();
        
        try {
            LOGGER.info("Starte Auto-Refresh mit " + intervalMinutes + " Minuten Intervall");
            
            // Starte neues Refresh
            currentRefreshTask = scheduler.scheduleAtFixedRate(
                new SafeRefreshWrapper(refreshTask),
                0,                    // Sofortige erste Ausführung
                intervalMinutes,      // Intervall
                TimeUnit.MINUTES
            );
            
            isRunning = true;
            currentIntervalMinutes = intervalMinutes;
            
            LOGGER.info("Auto-Refresh erfolgreich gestartet");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Starten des Auto-Refresh: " + e.getMessage(), e);
            isRunning = false;
        }
    }
    
    /**
     * Stoppt das automatische Refresh
     */
    public void stopAutoRefresh() {
        if (currentRefreshTask != null && !currentRefreshTask.isCancelled()) {
            LOGGER.info("Stoppe Auto-Refresh...");
            
            currentRefreshTask.cancel(false); // false = laufende Aufgabe nicht unterbrechen
            currentRefreshTask = null;
            
            LOGGER.info("Auto-Refresh gestoppt");
        }
        
        isRunning = false;
    }
    
    /**
     * Aktualisiert das Refresh-Intervall
     * @param newIntervalMinutes Neues Intervall in Minuten
     */
    public void updateRefreshInterval(int newIntervalMinutes) {
        newIntervalMinutes = validateInterval(newIntervalMinutes);
        
        if (newIntervalMinutes == currentIntervalMinutes) {
            LOGGER.fine("Refresh-Intervall unverändert: " + newIntervalMinutes + " Minuten");
            return;
        }
        
        LOGGER.info("Aktualisiere Refresh-Intervall von " + currentIntervalMinutes + 
                   " auf " + newIntervalMinutes + " Minuten");
        
        if (isRunning) {
            // Starte Refresh mit neuem Intervall neu
            startAutoRefresh(newIntervalMinutes);
        } else {
            // Speichere nur das neue Intervall
            currentIntervalMinutes = newIntervalMinutes;
        }
    }
    
    /**
     * Führt ein manuelles Refresh durch
     */
    public void executeManualRefresh() {
        LOGGER.info("Führe manuelles Refresh durch...");
        
        try {
            refreshTask.run();
            LOGGER.info("Manuelles Refresh abgeschlossen");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim manuellen Refresh: " + e.getMessage(), e);
        }
    }
    
    /**
     * Überprüft ob Auto-Refresh aktiviert ist
     */
    public boolean isAutoRefreshEnabled() {
        return isRunning && currentRefreshTask != null && !currentRefreshTask.isCancelled();
    }
    
    /**
     * Gibt das aktuelle Refresh-Intervall zurück
     */
    public int getCurrentIntervalMinutes() {
        return currentIntervalMinutes;
    }
    
    /**
     * Gibt Informationen über den Status zurück
     */
    public String getStatus() {
        if (!isAutoRefreshEnabled()) {
            return "Auto-Refresh: Deaktiviert";
        }
        
        return String.format("Auto-Refresh: Aktiv (%d Min. Intervall)", currentIntervalMinutes);
    }
    
    /**
     * Berechnet die Zeit bis zum nächsten Refresh (geschätzt)
     */
    public String getTimeToNextRefresh() {
        if (!isAutoRefreshEnabled()) {
            return "Nicht verfügbar";
        }
        
        // Vereinfachte Schätzung - in einer echten Implementierung könnte man
        // die tatsächliche verbleibende Zeit berechnen
        return "< " + currentIntervalMinutes + " Minuten";
    }
    
    /**
     * Fährt den Manager ordnungsgemäß herunter
     */
    public void shutdown() {
        LOGGER.info("Fahre DataRefreshManager herunter...");
        
        try {
            // Stoppe Auto-Refresh
            stopAutoRefresh();
            
            // Fahre Scheduler herunter
            scheduler.shutdown();
            
            // Warte maximal 5 Sekunden auf ordnungsgemäßes Beenden
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warning("Scheduler beendet sich nicht ordnungsgemäß - forciere shutdown");
                scheduler.shutdownNow();
                
                // Warte nochmals kurz
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    LOGGER.severe("Scheduler konnte nicht beendet werden");
                }
            }
            
            LOGGER.info("DataRefreshManager erfolgreich heruntergefahren");
            
        } catch (InterruptedException e) {
            LOGGER.warning("Unterbrochen beim Herunterfahren des DataRefreshManagers");
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Herunterfahren des DataRefreshManagers: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validiert und korrigiert das Refresh-Intervall
     */
    private int validateInterval(int interval) {
        if (interval < MIN_REFRESH_INTERVAL) {
            LOGGER.warning("Refresh-Intervall " + interval + " zu klein, setze auf Minimum: " + MIN_REFRESH_INTERVAL);
            return MIN_REFRESH_INTERVAL;
        }
        
        if (interval > MAX_REFRESH_INTERVAL) {
            LOGGER.warning("Refresh-Intervall " + interval + " zu groß, setze auf Maximum: " + MAX_REFRESH_INTERVAL);
            return MAX_REFRESH_INTERVAL;
        }
        
        return interval;
    }
    
    /**
     * Wrapper-Klasse für sichere Ausführung der Refresh-Aufgaben
     * Verhindert dass eine Exception das Auto-Refresh stoppt
     */
    private static class SafeRefreshWrapper implements Runnable {
        
        private final Runnable wrappedTask;
        private static final Logger WRAPPER_LOGGER = Logger.getLogger(SafeRefreshWrapper.class.getName());
        
        public SafeRefreshWrapper(Runnable wrappedTask) {
            this.wrappedTask = wrappedTask;
        }
        
        @Override
        public void run() {
            try {
                WRAPPER_LOGGER.fine("Starte Auto-Refresh Aufgabe...");
                wrappedTask.run();
                WRAPPER_LOGGER.fine("Auto-Refresh Aufgabe abgeschlossen");
                
            } catch (Exception e) {
                WRAPPER_LOGGER.log(Level.WARNING, "Fehler bei Auto-Refresh Aufgabe: " + e.getMessage(), e);
                // Exception wird nicht weitergegeben damit Auto-Refresh weiterläuft
            }
        }
    }
    
    /**
     * Erstellt einen DataRefreshManager mit angepassten Einstellungen für Tests
     */
    public static DataRefreshManager createTestManager(Runnable refreshTask, int defaultInterval) {
        DataRefreshManager manager = new DataRefreshManager(refreshTask);
        manager.currentIntervalMinutes = manager.validateInterval(defaultInterval);
        return manager;
    }
}