package com.fxssi.extractor.scheduler;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Scheduler-Klasse für die stündliche Ausführung der Datenextraktion
 * Verwendet ScheduledExecutorService für präzise Zeitsteuerung
 * 
 * @author Generated for FXSSI Data Extraction
 * @version 1.0
 */
public class HourlyScheduler {
    
    private static final Logger LOGGER = Logger.getLogger(HourlyScheduler.class.getName());
    private static final long HOUR_IN_MINUTES = 60;
    
    protected final ScheduledExecutorService scheduler;
    protected final Runnable taskToExecute;
    protected ScheduledFuture<?> scheduledTask;
    protected boolean isRunning = false;
    
    /**
     * Konstruktor
     * @param taskToExecute Die Aufgabe die stündlich ausgeführt werden soll
     */
    public HourlyScheduler(Runnable taskToExecute) {
        this.taskToExecute = taskToExecute;
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "FXSSI-Scheduler");
            t.setDaemon(true); // Daemon Thread damit das Programm beendet werden kann
            return t;
        });
        
        LOGGER.info("HourlyScheduler initialisiert");
    }
    
    /**
     * Startet die stündliche Ausführung der Aufgabe
     */
    public void startScheduling() {
        if (isRunning) {
            LOGGER.warning("Scheduler läuft bereits");
            return;
        }
        
        try {
            // Berechne die Zeit bis zur nächsten vollen Stunde
            long minutesToNextHour = calculateMinutesToNextHour();
            
            LOGGER.info("Starte Scheduler - Erste Ausführung in " + minutesToNextHour + " Minuten");
            
            // Führe die Aufgabe zur nächsten vollen Stunde aus und dann jede Stunde
            this.scheduledTask = scheduler.scheduleAtFixedRate(
                new SafeTaskWrapper(taskToExecute),
                minutesToNextHour,          // Initial delay
                HOUR_IN_MINUTES,           // Period (60 Minuten)
                TimeUnit.MINUTES
            );
            
            isRunning = true;
            LOGGER.info("Scheduler erfolgreich gestartet");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Starten des Schedulers: " + e.getMessage(), e);
            throw new RuntimeException("Scheduler konnte nicht gestartet werden", e);
        }
    }
    
    /**
     * Startet die stündliche Ausführung mit sofortiger erster Ausführung
     */
    public void startSchedulingImmediately() {
        if (isRunning) {
            LOGGER.warning("Scheduler läuft bereits");
            return;
        }
        
        try {
            LOGGER.info("Starte Scheduler mit sofortiger erster Ausführung");
            
            // Führe die Aufgabe sofort aus und dann jede Stunde
            this.scheduledTask = scheduler.scheduleAtFixedRate(
                new SafeTaskWrapper(taskToExecute),
                0,                         // Keine initial delay
                HOUR_IN_MINUTES,          // Period (60 Minuten)
                TimeUnit.MINUTES
            );
            
            isRunning = true;
            LOGGER.info("Scheduler erfolgreich gestartet (sofortige Ausführung)");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Starten des Schedulers: " + e.getMessage(), e);
            throw new RuntimeException("Scheduler konnte nicht gestartet werden", e);
        }
    }
    
    /**
     * Stoppt die Ausführung des Schedulers
     */
    public void stopScheduling() {
        if (!isRunning) {
            LOGGER.info("Scheduler läuft nicht");
            return;
        }
        
        try {
            if (scheduledTask != null) {
                scheduledTask.cancel(false); // false = laufende Aufgabe nicht unterbrechen
                LOGGER.info("Scheduled Task abgebrochen");
            }
            
            scheduler.shutdown();
            LOGGER.info("Scheduler shutdown eingeleitet");
            
            // Warte maximal 10 Sekunden auf das ordnungsgemäße Beenden
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                LOGGER.warning("Scheduler beendet sich nicht ordnungsgemäß - forciere shutdown");
                scheduler.shutdownNow();
                
                // Warte nochmals kurz
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.severe("Scheduler konnte nicht beendet werden");
                }
            }
            
            isRunning = false;
            LOGGER.info("Scheduler erfolgreich gestoppt");
            
        } catch (InterruptedException e) {
            LOGGER.warning("Unterbrochen beim Stoppen des Schedulers");
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Stoppen des Schedulers: " + e.getMessage(), e);
        }
    }
    
    /**
     * Führt die Aufgabe einmal manuell aus
     */
    public void executeTaskManually() {
        LOGGER.info("Führe Aufgabe manuell aus...");
        
        try {
            taskToExecute.run();
            LOGGER.info("Manuelle Ausführung abgeschlossen");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler bei manueller Ausführung: " + e.getMessage(), e);
        }
    }
    
    /**
     * Überprüft ob der Scheduler läuft
     */
    public boolean isRunning() {
        return isRunning && !scheduler.isShutdown();
    }
    
    /**
     * Gibt Informationen über den aktuellen Status zurück
     */
    public String getStatus() {
        if (!isRunning) {
            return "Scheduler: Gestoppt";
        }
        
        if (scheduler.isShutdown()) {
            return "Scheduler: Shutdown";
        }
        
        LocalDateTime nextExecution = calculateNextExecution();
        return String.format("Scheduler: Aktiv - Nächste Ausführung: %s", nextExecution);
    }
    
    /**
     * Berechnet die Anzahl Minuten bis zur nächsten vollen Stunde
     */
    private long calculateMinutesToNextHour() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        return ChronoUnit.MINUTES.between(now, nextHour);
    }
    
    /**
     * Berechnet die Zeit der nächsten Ausführung
     */
    private LocalDateTime calculateNextExecution() {
        LocalDateTime now = LocalDateTime.now();
        
        if (!isRunning) {
            return null;
        }
        
        // Wenn Scheduler läuft, ist die nächste Ausführung zur nächsten vollen Stunde
        return now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
    }
    
    /**
     * Wrapper-Klasse für sichere Ausführung der Aufgaben
     * Verhindert dass eine Exception den Scheduler stoppt
     */
    private static class SafeTaskWrapper implements Runnable {
        
        private final Runnable wrappedTask;
        private static final Logger WRAPPER_LOGGER = Logger.getLogger(SafeTaskWrapper.class.getName());
        
        public SafeTaskWrapper(Runnable wrappedTask) {
            this.wrappedTask = wrappedTask;
        }
        
        @Override
        public void run() {
            try {
                LocalDateTime startTime = LocalDateTime.now();
                WRAPPER_LOGGER.info("Starte geplante Aufgabe um " + startTime);
                
                wrappedTask.run();
                
                LocalDateTime endTime = LocalDateTime.now();
                long duration = ChronoUnit.SECONDS.between(startTime, endTime);
                WRAPPER_LOGGER.info("Geplante Aufgabe abgeschlossen in " + duration + " Sekunden");
                
            } catch (Exception e) {
                WRAPPER_LOGGER.log(Level.SEVERE, "Fehler bei der Ausführung der geplanten Aufgabe: " + e.getMessage(), e);
                // Exception wird nicht weitergegeben damit der Scheduler weiterläuft
            }
        }
    }
    
    /**
     * Erstellt einen Scheduler mit angepassten Ausführungszeiten für Testing
     * @param taskToExecute Die auszuführende Aufgabe
     * @param intervalMinutes Intervall in Minuten
     * @return Neuer Scheduler mit angepasstem Intervall
     */
    public static HourlyScheduler createCustomIntervalScheduler(Runnable taskToExecute, long intervalMinutes) {
        return new CustomIntervalScheduler(taskToExecute, intervalMinutes);
    }
    
    /**
     * Spezieller Scheduler für Testing mit anpassbaren Intervallen
     */
    private static class CustomIntervalScheduler extends HourlyScheduler {
        
        private final long customInterval;
        
        public CustomIntervalScheduler(Runnable taskToExecute, long intervalMinutes) {
            super(taskToExecute);
            this.customInterval = intervalMinutes;
            LOGGER.info("CustomIntervalScheduler erstellt mit " + intervalMinutes + " Minuten Intervall");
        }
        
        @Override
        public void startScheduling() {
            if (isRunning()) {
                LOGGER.warning("Custom Scheduler läuft bereits");
                return;
            }
            
            try {
                LOGGER.info("Starte Custom Scheduler mit " + customInterval + " Minuten Intervall");
                
                this.scheduledTask = scheduler.scheduleAtFixedRate(
                    new SafeTaskWrapper(taskToExecute),
                    0,                    // Sofortige erste Ausführung
                    customInterval,       // Custom interval
                    TimeUnit.MINUTES
                );
                
                isRunning = true;
                LOGGER.info("Custom Scheduler erfolgreich gestartet");
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Fehler beim Starten des Custom Schedulers: " + e.getMessage(), e);
                throw new RuntimeException("Custom Scheduler konnte nicht gestartet werden", e);
            }
        }
    }
}