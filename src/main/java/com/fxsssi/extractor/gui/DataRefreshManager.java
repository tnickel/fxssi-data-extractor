package com.fxsssi.extractor.gui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager für die automatische Aktualisierung der GUI-Daten
 * Ermöglicht konfigurierbare Refresh-Intervalle in Minuten mit verbesserter Thread-Sicherheit
 * 
 * ERWEITERT: Unterstützt jetzt zwei Modi:
 * 1. Intervall-Modus: Refresh alle X Minuten (wie bisher)
 * 2. Tageszeit-Modus: Refresh einmal täglich zu einer bestimmten Uhrzeit
 * 
 * @author Generated for FXSSI Data Extraction GUI
 * @version 1.2 (mit täglichem Zeitplan-Support)
 */
public class DataRefreshManager {
    
    private static final Logger LOGGER = Logger.getLogger(DataRefreshManager.class.getName());
    private static final int MIN_REFRESH_INTERVAL = 1; // Minimum 1 Minute
    private static final int MAX_REFRESH_INTERVAL = 60; // Maximum 60 Minuten
    
    private final ScheduledExecutorService scheduler;
    private final Runnable refreshTask;
    private ScheduledFuture<?> currentRefreshTask;
    private boolean isRunning = false;
    private int currentIntervalMinutes = 15; // Standard: 15 Minuten
    
    // NEU: Täglicher Zeitplan-Support
    private ScheduledFuture<?> dailyRefreshTask;
    private boolean isDailyRunning = false;
    private int dailyHour = 12;    // Standard: 12 Uhr
    private int dailyMinute = 0;   // Standard: 00 Minuten
    private LocalDate lastDailyExecutionDate = null; // Verhindert doppelte Ausführung am selben Tag
    
    // Thread-Sicherheit für Refresh-Aufgaben
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);
    private volatile long lastRefreshTime = 0;
    private static final long MIN_REFRESH_INTERVAL_MS = 10000; // Minimum 10 Sekunden zwischen Refreshs
    
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
        
        LOGGER.info("DataRefreshManager initialisiert (mit Intervall + Tageszeit-Support)");
    }
    
    /**
     * Startet das automatische Refresh mit dem angegebenen Intervall
     * @param intervalMinutes Intervall in Minuten (1-60)
     */
    public void startAutoRefresh(int intervalMinutes) {
        // Validiere Intervall
        intervalMinutes = validateInterval(intervalMinutes);
        
        // Stoppe eventuell laufendes Intervall-Refresh
        stopAutoRefresh();
        
        try {
            LOGGER.info("Starte Intervall-Auto-Refresh mit " + intervalMinutes + " Minuten Intervall");
            
            // Starte neues Refresh mit thread-sicherem Wrapper
            currentRefreshTask = scheduler.scheduleAtFixedRate(
                new SafeRefreshWrapper(this::executeRefreshSafely),
                0,                    // Sofortige erste Ausführung
                intervalMinutes,      // Intervall
                TimeUnit.MINUTES
            );
            
            isRunning = true;
            currentIntervalMinutes = intervalMinutes;
            
            LOGGER.info("Intervall-Auto-Refresh erfolgreich gestartet (" + intervalMinutes + " Min.)");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Starten des Intervall-Auto-Refresh: " + e.getMessage(), e);
            isRunning = false;
        }
    }
    
    /**
     * Stoppt das Intervall-basierte automatische Refresh
     */
    public void stopAutoRefresh() {
        if (currentRefreshTask != null && !currentRefreshTask.isCancelled()) {
            LOGGER.info("Stoppe Intervall-Auto-Refresh...");
            
            currentRefreshTask.cancel(false); // false = laufende Aufgabe nicht unterbrechen
            currentRefreshTask = null;
            
            LOGGER.info("Intervall-Auto-Refresh gestoppt");
        }
        
        isRunning = false;
    }
    
    // ===================================================================
    // NEU: TÄGLICHER ZEITPLAN-SUPPORT
    // ===================================================================
    
    /**
     * NEU: Startet den täglichen Refresh zu einer bestimmten Uhrzeit
     * Berechnet die Verzögerung bis zur nächsten Ausführung und plant den Task
     * 
     * @param hour Stunde (0-23)
     * @param minute Minute (0-59)
     */
    public void startDailyRefresh(int hour, int minute) {
        // Stoppe eventuell laufenden täglichen Refresh
        stopDailyRefresh();
        
        // Validiere Eingaben
        hour = Math.max(0, Math.min(23, hour));
        minute = Math.max(0, Math.min(59, minute));
        
        this.dailyHour = hour;
        this.dailyMinute = minute;
        
        try {
            // Berechne Verzögerung bis zur nächsten Ausführung
            long delayMinutes = calculateDelayToNextExecution(hour, minute);
            
            LOGGER.info(String.format("Starte täglichen Refresh um %02d:%02d Uhr (nächste Ausführung in %d Minuten)", 
                hour, minute, delayMinutes));
            
            // Plane täglichen Task: Erste Ausführung nach berechneter Verzögerung, 
            // dann alle 60 Minuten prüfen ob es Zeit ist (um Drift zu vermeiden)
            dailyRefreshTask = scheduler.scheduleAtFixedRate(
                new SafeRefreshWrapper(this::executeDailyRefreshCheck),
                delayMinutes,         // Verzögerung bis zur ersten Ausführung
                60,                   // Alle 60 Minuten prüfen
                TimeUnit.MINUTES
            );
            
            isDailyRunning = true;
            
            LOGGER.info(String.format("Täglicher Refresh geplant: %02d:%02d Uhr (nächste Ausführung in %d Min.)", 
                hour, minute, delayMinutes));
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Starten des täglichen Refresh: " + e.getMessage(), e);
            isDailyRunning = false;
        }
    }
    
    /**
     * NEU: Stoppt den täglichen Refresh
     */
    public void stopDailyRefresh() {
        if (dailyRefreshTask != null && !dailyRefreshTask.isCancelled()) {
            LOGGER.info("Stoppe täglichen Refresh...");
            
            dailyRefreshTask.cancel(false);
            dailyRefreshTask = null;
            
            LOGGER.info("Täglicher Refresh gestoppt");
        }
        
        isDailyRunning = false;
        lastDailyExecutionDate = null; // Reset damit nächster Start sofort prüft
    }
    
    /**
     * NEU: Aktualisiert die Uhrzeit für den täglichen Refresh
     * @param hour Neue Stunde (0-23)
     * @param minute Neue Minute (0-59)
     */
    public void updateDailyRefreshTime(int hour, int minute) {
        hour = Math.max(0, Math.min(23, hour));
        minute = Math.max(0, Math.min(59, minute));
        
        if (hour == dailyHour && minute == dailyMinute) {
            LOGGER.fine("Tägliche Refresh-Zeit unverändert: " + String.format("%02d:%02d", hour, minute));
            return;
        }
        
        LOGGER.info(String.format("Aktualisiere tägliche Refresh-Zeit von %02d:%02d auf %02d:%02d", 
            dailyHour, dailyMinute, hour, minute));
        
        this.dailyHour = hour;
        this.dailyMinute = minute;
        
        if (isDailyRunning) {
            // Neustart mit neuer Zeit
            startDailyRefresh(hour, minute);
        }
    }
    
    /**
     * NEU: Prüft ob der tägliche Refresh gerade aktiv ist
     */
    public boolean isDailyRefreshEnabled() {
        return isDailyRunning && dailyRefreshTask != null && !dailyRefreshTask.isCancelled();
    }
    
    /**
     * NEU: Gibt die konfigurierte Stunde für den täglichen Refresh zurück
     */
    public int getDailyHour() {
        return dailyHour;
    }
    
    /**
     * NEU: Gibt die konfigurierte Minute für den täglichen Refresh zurück
     */
    public int getDailyMinute() {
        return dailyMinute;
    }
    
    /**
     * NEU: Interne Prüfmethode die alle 60 Minuten aufgerufen wird
     * Führt den Refresh nur aus, wenn die konfigurierte Uhrzeit erreicht ist
     * und heute noch nicht ausgeführt wurde
     */
    private void executeDailyRefreshCheck() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        // Prüfe ob heute schon ausgeführt wurde
        if (today.equals(lastDailyExecutionDate)) {
            LOGGER.fine("Täglicher Refresh heute bereits ausgeführt - überspringe");
            return;
        }
        
        // Prüfe ob die konfigurierte Zeit erreicht oder überschritten wurde
        LocalTime configuredTime = LocalTime.of(dailyHour, dailyMinute);
        LocalTime currentTime = now.toLocalTime();
        
        if (currentTime.isAfter(configuredTime) || currentTime.equals(configuredTime)) {
            // Zusätzliche Prüfung: Nicht mehr als 90 Minuten nach der konfigurierten Zeit
            // (um Mitternachts-Probleme zu vermeiden)
            long minutesSinceConfigured = ChronoUnit.MINUTES.between(configuredTime, currentTime);
            
            if (minutesSinceConfigured <= 90) {
                LOGGER.info(String.format("Täglicher Refresh-Zeitpunkt erreicht (%02d:%02d) - führe Refresh aus...", 
                    dailyHour, dailyMinute));
                
                // Markiere als heute ausgeführt BEVOR der Refresh startet
                lastDailyExecutionDate = today;
                
                // Führe den eigentlichen Refresh aus
                executeRefreshSafely();
                
                LOGGER.info("Täglicher Refresh abgeschlossen für " + today);
            } else {
                LOGGER.fine(String.format("Täglicher Refresh übersprungen - %d Minuten seit konfigurierter Zeit %02d:%02d", 
                    minutesSinceConfigured, dailyHour, dailyMinute));
            }
        } else {
            LOGGER.fine(String.format("Täglicher Refresh noch nicht fällig - aktuelle Zeit %s, konfiguriert %02d:%02d", 
                currentTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), dailyHour, dailyMinute));
        }
    }
    
    /**
     * NEU: Berechnet die Verzögerung in Minuten bis zur nächsten Ausführung
     */
    private long calculateDelayToNextExecution(int hour, int minute) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextExecution = LocalDate.now().atTime(hour, minute);
        
        // Wenn die Zeit heute schon vorbei ist, plane für morgen
        if (now.isAfter(nextExecution)) {
            nextExecution = nextExecution.plusDays(1);
        }
        
        long delayMinutes = ChronoUnit.MINUTES.between(now, nextExecution);
        
        // Mindestens 1 Minute Verzögerung
        return Math.max(1, delayMinutes);
    }
    
    // ===================================================================
    // BESTEHENDE METHODEN (teilweise erweitert)
    // ===================================================================
    
    /**
     * Thread-sichere Ausführung der Refresh-Aufgabe
     */
    private void executeRefreshSafely() {
        // Prüfe ob bereits ein Refresh läuft
        if (!refreshInProgress.compareAndSet(false, true)) {
            LOGGER.fine("Refresh übersprungen - bereits ein Refresh im Gange");
            return;
        }
        
        try {
            // Prüfe Minimum-Intervall zwischen Refreshs
            long currentTime = System.currentTimeMillis();
            if (lastRefreshTime > 0 && (currentTime - lastRefreshTime) < MIN_REFRESH_INTERVAL_MS) {
                long waitTime = MIN_REFRESH_INTERVAL_MS - (currentTime - lastRefreshTime);
                LOGGER.fine("Minimum-Intervall noch nicht erreicht, warte " + (waitTime / 1000) + " Sekunden");
                return;
            }
            
            LOGGER.fine("Führe Auto-Refresh aus...");
            refreshTask.run();
            lastRefreshTime = currentTime;
            LOGGER.fine("Auto-Refresh abgeschlossen");
            
        } finally {
            refreshInProgress.set(false);
        }
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
     * Führt ein manuelles Refresh durch (thread-sicher)
     */
    public void executeManualRefresh() {
        LOGGER.info("Führe manuelles Refresh durch...");
        
        // Verwende separaten Thread für manuelles Refresh
        scheduler.execute(() -> {
            try {
                executeRefreshSafely();
                LOGGER.info("Manuelles Refresh abgeschlossen");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Fehler beim manuellen Refresh: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Überprüft ob Intervall-Auto-Refresh aktiviert ist
     */
    public boolean isAutoRefreshEnabled() {
        return isRunning && currentRefreshTask != null && !currentRefreshTask.isCancelled();
    }
    
    /**
     * Überprüft ob gerade ein Refresh läuft
     */
    public boolean isRefreshInProgress() {
        return refreshInProgress.get();
    }
    
    /**
     * Gibt das aktuelle Refresh-Intervall zurück
     */
    public int getCurrentIntervalMinutes() {
        return currentIntervalMinutes;
    }
    
    /**
     * Gibt Informationen über den Status zurück (ERWEITERT um Tageszeit-Info)
     */
    public String getStatus() {
        StringBuilder status = new StringBuilder();
        
        if (refreshInProgress.get()) {
            status.append("Refresh: Läuft gerade...");
            return status.toString();
        }
        
        // Intervall-Status
        if (isAutoRefreshEnabled()) {
            status.append(String.format("Intervall: Aktiv (%d Min.)", currentIntervalMinutes));
        } else {
            status.append("Intervall: Deaktiviert");
        }
        
        // Tageszeit-Status
        if (isDailyRefreshEnabled()) {
            status.append(String.format(" | Täglicher Check: %02d:%02d Uhr", dailyHour, dailyMinute));
            if (lastDailyExecutionDate != null) {
                status.append(" (heute ausgeführt)");
            }
        } else {
            status.append(" | Täglicher Check: Deaktiviert");
        }
        
        return status.toString();
    }
    
    /**
     * Gibt detaillierte Statusinformationen zurück (ERWEITERT um Tageszeit-Info)
     */
    public String getDetailedStatus() {
        StringBuilder status = new StringBuilder();
        
        status.append("Refresh-Status:\n");
        status.append("- Intervall-Refresh: ").append(isAutoRefreshEnabled() ? "Aktiviert" : "Deaktiviert").append("\n");
        status.append("- Intervall: ").append(currentIntervalMinutes).append(" Minuten\n");
        status.append("- Täglicher Refresh: ").append(isDailyRefreshEnabled() ? "Aktiviert" : "Deaktiviert").append("\n");
        status.append("- Tägliche Zeit: ").append(String.format("%02d:%02d Uhr", dailyHour, dailyMinute)).append("\n");
        status.append("- Läuft gerade: ").append(refreshInProgress.get()).append("\n");
        
        if (lastDailyExecutionDate != null) {
            status.append("- Letzter täglicher Refresh: ").append(lastDailyExecutionDate).append("\n");
        } else {
            status.append("- Letzter täglicher Refresh: Noch keiner\n");
        }
        
        if (lastRefreshTime > 0) {
            long timeSinceLastRefresh = System.currentTimeMillis() - lastRefreshTime;
            status.append("- Letzter Refresh: vor ").append(timeSinceLastRefresh / 1000).append(" Sekunden\n");
        } else {
            status.append("- Letzter Refresh: Noch keiner\n");
        }
        
        return status.toString();
    }
    
    /**
     * Berechnet die Zeit bis zum nächsten Refresh (geschätzt) - ERWEITERT
     */
    public String getTimeToNextRefresh() {
        if (refreshInProgress.get()) {
            return "Läuft gerade...";
        }
        
        StringBuilder next = new StringBuilder();
        
        if (isAutoRefreshEnabled()) {
            next.append("Intervall: < ").append(currentIntervalMinutes).append(" Min.");
        }
        
        if (isDailyRefreshEnabled()) {
            if (next.length() > 0) next.append(" | ");
            
            LocalDate today = LocalDate.now();
            if (today.equals(lastDailyExecutionDate)) {
                next.append(String.format("Täglicher Check: Morgen %02d:%02d", dailyHour, dailyMinute));
            } else {
                next.append(String.format("Täglicher Check: Heute %02d:%02d", dailyHour, dailyMinute));
            }
        }
        
        if (next.length() == 0) {
            return "Kein Refresh geplant";
        }
        
        return next.toString();
    }
    
    /**
     * Fährt den Manager ordnungsgemäß herunter (ERWEITERT um täglichen Task)
     */
    public void shutdown() {
        LOGGER.info("Fahre DataRefreshManager herunter...");
        
        try {
            // Stoppe Intervall-Refresh
            stopAutoRefresh();
            
            // NEU: Stoppe täglichen Refresh
            stopDailyRefresh();
            
            // Warte auf laufende Refreshs
            if (refreshInProgress.get()) {
                LOGGER.info("Warte auf Abschluss des laufenden Refresh...");
                int waitCount = 0;
                while (refreshInProgress.get() && waitCount < 30) { // Max 30 Sekunden warten
                    Thread.sleep(1000);
                    waitCount++;
                }
            }
            
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
                WRAPPER_LOGGER.fine("Starte geplante Refresh-Aufgabe...");
                wrappedTask.run();
                WRAPPER_LOGGER.fine("Geplante Refresh-Aufgabe abgeschlossen");
                
            } catch (Exception e) {
                WRAPPER_LOGGER.log(Level.WARNING, "Fehler bei geplanter Refresh-Aufgabe: " + e.getMessage(), e);
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