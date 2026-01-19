package com.fxssi.extractor.notification;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.model.SignalChangeEvent;
import com.fxssi.extractor.storage.LastSentSignalManager;

/**
 * Service-Klasse für E-Mail-Versendung bei Signalwechseln
 * Unterstützt GMX-Server und E-Mail-Limits zur Spam-Vermeidung
 * ERWEITERT um Threshold-basierte Anti-Spam-Funktionalität und MetaTrader-Sync
 * 
 * @author Generated for FXSSI Email Notifications
 * @version 1.2 - MetaTrader Dual-Directory Sync Support
 */
public class EmailService {
    
    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());
    private static final DateTimeFormatter EMAIL_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    
    private EmailConfig config;
    private Session mailSession;
    private final LastSentSignalManager lastSentSignalManager; // NEU: Anti-Spam Manager
    
    // E-Mail-Limit-Tracking
    private final ConcurrentLinkedQueue<LocalDateTime> sentEmailTimes;
    private final AtomicInteger emailsSentThisHour;
    
    /**
     * Konstruktor
     * ERWEITERT um EmailConfig-Übergabe an LastSentSignalManager für MetaTrader-Sync
     * 
     * @param config E-Mail-Konfiguration
     */
    public EmailService(EmailConfig config) {
        this.config = config;
        this.sentEmailTimes = new ConcurrentLinkedQueue<>();
        this.emailsSentThisHour = new AtomicInteger(0);
        this.lastSentSignalManager = new LastSentSignalManager(config.getDataDirectory());
        
        // Initialisiere LastSentSignalManager
        lastSentSignalManager.loadLastSentSignals();
        
        // NEU: Setze EmailConfig für MetaTrader-Synchronisation
        lastSentSignalManager.setEmailConfig(config);
        
        initializeMailSession();
        LOGGER.info("EmailService initialisiert für Server: " + config.getSmtpHost() + 
                   " (mit Anti-Spam-Threshold: " + config.getSignalChangeThreshold() + "%)");
        
        // NEU: Log MetaTrader-Sync-Status
        if (config.isMetatraderSyncEnabled()) {
            LOGGER.info("MetaTrader-Synchronisation aktiviert (" + config.getMetatraderDirectoryCount() + 
                       " Verzeichnis" + (config.getMetatraderDirectoryCount() > 1 ? "se" : "") + ")");
        }
    }
    
    /**
     * NEU: Konstruktor mit externem LastSentSignalManager (für Tests)
     * @param config E-Mail-Konfiguration
     * @param lastSentSignalManager Externer LastSentSignalManager
     */
    public EmailService(EmailConfig config, LastSentSignalManager lastSentSignalManager) {
        this.config = config;
        this.sentEmailTimes = new ConcurrentLinkedQueue<>();
        this.emailsSentThisHour = new AtomicInteger(0);
        this.lastSentSignalManager = lastSentSignalManager;
        
        // NEU: Setze EmailConfig für MetaTrader-Synchronisation
        lastSentSignalManager.setEmailConfig(config);
        
        initializeMailSession();
        LOGGER.info("EmailService initialisiert mit externem LastSentSignalManager");
    }
    
    /**
     * Aktualisiert die E-Mail-Konfiguration
     * ERWEITERT um EmailConfig-Update im LastSentSignalManager
     * 
     * @param newConfig Neue Konfiguration
     */
    public void updateConfig(EmailConfig newConfig) {
        this.config = newConfig;
        
        // NEU: Update EmailConfig im LastSentSignalManager für MetaTrader-Sync
        lastSentSignalManager.setEmailConfig(newConfig);
        
        initializeMailSession();
        
        LOGGER.info("E-Mail-Konfiguration aktualisiert (Threshold: " + newConfig.getSignalChangeThreshold() + "%)");
        
        // NEU: Log MetaTrader-Sync-Status
        if (newConfig.isMetatraderSyncEnabled()) {
            LOGGER.info("MetaTrader-Synchronisation: " + newConfig.getMetatraderDirectoryCount() + 
                       " Verzeichnis" + (newConfig.getMetatraderDirectoryCount() > 1 ? "se" : ""));
        }
    }
    
    /**
     * Initialisiert die Mail-Session basierend auf der Konfiguration
     */
    private void initializeMailSession() {
        try {
            Properties mailProps = config.createMailProperties();
            
            // Erstelle Authenticator
            Authenticator authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.getUsername(), config.getPassword());
                }
            };
            
            // Erstelle Mail-Session
            mailSession = Session.getInstance(mailProps, authenticator);
            
            LOGGER.info("Mail-Session erfolgreich initialisiert für: " + config.getSmtpHost());
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Initialisieren der Mail-Session: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sendet eine Test-E-Mail zur Konfigurationsprüfung
     * @return Erfolgsmeldung oder Fehlermeldung
     */
    public EmailSendResult sendTestEmail() {
        if (!config.isEmailEnabled()) {
            return new EmailSendResult(false, "E-Mail-Benachrichtigungen sind deaktiviert");
        }
        
        EmailConfig.ValidationResult validation = config.validateConfig();
        if (!validation.isValid()) {
            return new EmailSendResult(false, "Konfigurationsfehler:\n" + validation.getErrorMessage());
        }
        
        try {
            String subject = "FXSSI Monitor - Test-E-Mail";
            String body = createTestEmailBody();
            
            return sendEmail(subject, body, "Test-E-Mail");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Senden der Test-E-Mail: " + e.getMessage(), e);
            return new EmailSendResult(false, "Fehler beim Senden: " + e.getMessage());
        }
    }
    
    /**
     * NEU: Hauptmethode für Signalwechsel-Benachrichtigungen mit Threshold-Prüfung
     * Sendet nur E-Mails wenn der konfigurierte Threshold überschritten wird
     * @param currencyPairData Liste der aktuellen Währungspaar-Daten
     * @return Erfolgsmeldung oder Fehlermeldung
     */
    public EmailSendResult sendSignalChangeNotificationWithThreshold(List<CurrencyPairData> currencyPairData) {
        if (!config.isEmailEnabled()) {
            LOGGER.fine("E-Mail-Benachrichtigungen sind deaktiviert");
            return new EmailSendResult(false, "E-Mail-Benachrichtigungen sind deaktiviert");
        }
        
        if (currencyPairData == null || currencyPairData.isEmpty()) {
            return new EmailSendResult(false, "Keine Währungsdaten zum Prüfen");
        }
        
        // Filtere Signale die den Threshold überschreiten
        List<CurrencyPairData> thresholdExceededData = filterSignalsAboveThreshold(currencyPairData);
        
        if (thresholdExceededData.isEmpty()) {
            LOGGER.fine("Keine Signale überschreiten den konfigurierten Threshold von " + config.getSignalChangeThreshold() + "%");
            return new EmailSendResult(false, "Kein Signal überschreitet den Threshold");
        }
        
        // Prüfe E-Mail-Limit
        if (!checkEmailLimit()) {
            String limitMessage = "E-Mail-Limit erreicht (" + config.getMaxEmailsPerHour() + "/Stunde)";
            LOGGER.warning(limitMessage);
            return new EmailSendResult(false, limitMessage);
        }
        
        try {
            String subject = createThresholdSignalSubject(thresholdExceededData);
            String body = createThresholdSignalEmailBody(thresholdExceededData);
            
            EmailSendResult result = sendEmail(subject, body, "Threshold-Signalwechsel-Benachrichtigung");
            
            if (result.isSuccess()) {
                // Registriere alle gesendeten Signale beim LastSentSignalManager
                // NEU: MetaTrader-Sync wird automatisch in recordSentSignal() durchgeführt
                for (CurrencyPairData data : thresholdExceededData) {
                    lastSentSignalManager.recordSentSignal(
                        data.getCurrencyPair(), 
                        data.getTradingSignal(), 
                        data.getBuyPercentage()
                    );
                }
                
                LOGGER.info("Threshold-basierte E-Mail erfolgreich gesendet für " + 
                           thresholdExceededData.size() + " Währungspaar(e)");
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Senden der Threshold-Signalwechsel-E-Mail: " + e.getMessage(), e);
            return new EmailSendResult(false, "Fehler beim Senden: " + e.getMessage());
        }
    }
    
    /**
     * NEU: Filtert Signale die den konfigurierten Threshold überschreiten
     */
    private List<CurrencyPairData> filterSignalsAboveThreshold(List<CurrencyPairData> currencyPairData) {
        double threshold = config.getSignalChangeThreshold();
        
        return currencyPairData.stream()
            .filter(data -> lastSentSignalManager.shouldSendEmail(
                data.getCurrencyPair(),
                data.getTradingSignal(),
                data.getBuyPercentage(),
                threshold
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * NEU: Erstellt den Betreff für Threshold-basierte Signalwechsel-E-Mails
     */
    private String createThresholdSignalSubject(List<CurrencyPairData> data) {
        int count = data.size();
        
        if (count == 1) {
            CurrencyPairData single = data.get(0);
            return String.format("🔔 FXSSI Signal: %s %s (%.0f%%)", 
                single.getCurrencyPair(),
                single.getTradingSignal().name(),
                single.getBuyPercentage()
            );
        } else {
            return String.format("🔔 FXSSI Signale: %d Währungspaare (Threshold: %.1f%%)",
                count,
                config.getSignalChangeThreshold()
            );
        }
    }
    
    /**
     * NEU: Erstellt den E-Mail-Body für Threshold-basierte Signalwechsel
     */
    private String createThresholdSignalEmailBody(List<CurrencyPairData> data) {
        StringBuilder body = new StringBuilder();
        
        body.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
        body.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        
        // Header
        body.append("<h2 style='color: #2196F3; border-bottom: 2px solid #2196F3; padding-bottom: 10px;'>");
        body.append("🔔 FXSSI Trading Signal-Benachrichtigung");
        body.append("</h2>");
        
        // Intro
        body.append("<div style='background-color: #e3f2fd; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
        body.append("<p style='margin: 0; font-weight: bold; color: #1976d2;'>");
        body.append("Threshold-basierte Signal-Änderungen erkannt!");
        body.append("</p>");
        body.append("<p style='margin: 10px 0 0 0; font-size: 14px;'>");
        body.append("Die folgenden Währungspaare haben den konfigurierten Threshold von <strong>");
        body.append(String.format("%.1f%%", config.getSignalChangeThreshold()));
        body.append("</strong> überschritten:");
        body.append("</p>");
        body.append("</div>");
        
        // Signale-Details
        body.append("<h3 style='color: #495057;'>📊 Signal-Details:</h3>");
        body.append("<div style='background-color: #f8f9fa; padding: 10px; border-radius: 5px;'>");
        
        for (CurrencyPairData currData : data) {
            String signalColor = getSignalColor(currData.getTradingSignal());
            String signalIcon = getSignalIcon(currData.getTradingSignal());
            
            body.append("<div style='border-left: 4px solid ").append(signalColor).append("; padding: 10px; margin: 10px 0; background-color: white;'>");
            body.append("<h4 style='margin: 0 0 5px 0; color: ").append(signalColor).append(";'>");
            body.append(signalIcon).append(" ").append(currData.getCurrencyPair()).append("</h4>");
            
            body.append("<p style='margin: 5px 0; font-size: 14px;'>");
            body.append("<strong>Signal:</strong> ").append(currData.getTradingSignal().name()).append("<br>");
            body.append("<strong>Buy:</strong> ").append(String.format("%.1f%%", currData.getBuyPercentage())).append(" | ");
            body.append("<strong>Sell:</strong> ").append(String.format("%.1f%%", currData.getSellPercentage())).append("<br>");
            
            // Zeige Differenz zum letzten gesendeten Signal wenn vorhanden
            if (lastSentSignalManager.hasLastSentSignal(currData.getCurrencyPair())) {
                LastSentSignalManager.LastSentSignal lastSent = lastSentSignalManager.getLastSentSignal(currData.getCurrencyPair());
                double diff = Math.abs(currData.getBuyPercentage() - lastSent.getBuyPercentage());
                body.append("<strong>Änderung:</strong> ").append(String.format("%.1f%%", lastSent.getBuyPercentage()));
                body.append(" → ").append(String.format("%.1f%%", currData.getBuyPercentage()));
                body.append(" (Differenz: ").append(String.format("%.1f%%", diff)).append(")");
            } else {
                body.append("<strong>Status:</strong> Erstes Signal für dieses Währungspaar");
            }
            
            body.append("</p>");
            body.append("</div>");
        }
        
        body.append("</div>");
        
        // MetaTrader-Info
        if (config.isMetatraderSyncEnabled()) {
            body.append("<div style='background-color: #e8f5e9; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #4caf50;'>");
            body.append("<h3 style='margin-top: 0; color: #2e7d32;'>📂 MetaTrader-Synchronisation</h3>");
            body.append("<p style='margin: 5px 0; font-size: 14px;'>");
            body.append("Die Signale wurden automatisch synchronisiert zu:<br>");
            int dirCount = config.getMetatraderDirectoryCount();
            body.append("<strong>").append(dirCount).append(" Verzeichnis").append(dirCount > 1 ? "sen" : "").append("</strong><br>");
            body.append("Datei: <code>last_known_signals.csv</code><br>");
            body.append("Format: Währungspaar;Letztes_Signal;Prozent<br>");
            body.append("<em>XAUUSD → GOLD, XAGUSD → SILBER</em>");
            body.append("</p>");
            body.append("</div>");
        }
        
        // Footer
        body.append("<p style='margin-top: 30px; color: #6c757d; font-size: 12px; text-align: center; border-top: 1px solid #dee2e6; padding-top: 15px;'>");
        body.append("Diese Benachrichtigung wurde am ").append(LocalDateTime.now().format(EMAIL_TIME_FORMATTER));
        body.append(" automatisch von FXSSI Monitor gesendet.<br>");
        body.append("Aktueller E-Mail-Zähler: ").append(emailsSentThisHour.get()).append("/").append(config.getMaxEmailsPerHour()).append(" pro Stunde<br>");
        body.append("Signal-Threshold: ").append(String.format("%.1f%%", config.getSignalChangeThreshold()));
        if (config.isMetatraderSyncEnabled()) {
            body.append(" | MetaTrader-Sync: ✅");
        }
        body.append("</p>");
        
        body.append("</div></body></html>");
        
        return body.toString();
    }
    
    /**
     * Sendet E-Mail-Benachrichtigung bei Signalwechseln (alte Methode, für Kompatibilität)
     *
     * <p>Diese Methode ist veraltet und sollte nicht mehr für neue Implementierungen verwendet werden.
     * Sie bleibt vorerst bestehen, da sie noch in MainWindowController.java verwendet wird.</p>
     *
     * <p><b>Migration-Hinweis:</b> Die neue Methode sendSignalChangeNotificationWithThreshold()
     * verwendet ein anderes Datenmodell (CurrencyPairData statt SignalChangeEvent) und
     * bietet erweiterte Threshold-basierte Filterung.</p>
     *
     * @param changes Liste der Signalwechsel-Events
     * @return EmailSendResult mit Erfolgs-Status und Nachricht
     * @deprecated Verwende stattdessen sendSignalChangeNotificationWithThreshold()
     *             Diese Methode wird in einer zukünftigen Version entfernt.
     * @see #sendSignalChangeNotificationWithThreshold(List)
     */
    @Deprecated
    public EmailSendResult sendSignalChangeNotification(List<SignalChangeEvent> changes) {
        if (!config.isEmailEnabled()) {
            LOGGER.fine("E-Mail-Benachrichtigungen sind deaktiviert");
            return new EmailSendResult(false, "E-Mail-Benachrichtigungen sind deaktiviert");
        }
        
        if (changes == null || changes.isEmpty()) {
            return new EmailSendResult(false, "Keine Signalwechsel zum Benachrichtigen");
        }
        
        // Filtere nach Wichtigkeit basierend auf Einstellungen
        List<SignalChangeEvent> filteredChanges = filterSignalChangesByImportance(changes);
        
        if (filteredChanges.isEmpty()) {
            LOGGER.fine("Keine Signalwechsel entsprechen den konfigurierten Benachrichtigungseinstellungen");
            return new EmailSendResult(false, "Keine relevanten Signalwechsel");
        }
        
        // Prüfe E-Mail-Limit
        if (!checkEmailLimit()) {
            String limitMessage = "E-Mail-Limit erreicht (" + config.getMaxEmailsPerHour() + "/Stunde)";
            LOGGER.warning(limitMessage);
            return new EmailSendResult(false, limitMessage);
        }
        
        try {
            String subject = createSignalChangeSubject(filteredChanges);
            String body = createSignalChangeEmailBody(filteredChanges);
            
            EmailSendResult result = sendEmail(subject, body, "Signalwechsel-Benachrichtigung");
            
            if (result.isSuccess()) {
                LOGGER.info("E-Mail erfolgreich gesendet für " + filteredChanges.size() + " Signalwechsel");
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Senden der Signalwechsel-E-Mail: " + e.getMessage(), e);
            return new EmailSendResult(false, "Fehler beim Senden: " + e.getMessage());
        }
    }
    
    /**
     * Filtert Signalwechsel nach konfigurierten Wichtigkeitseinstellungen
     */
    private List<SignalChangeEvent> filterSignalChangesByImportance(List<SignalChangeEvent> changes) {
        return changes.stream()
            .filter(change -> {
                SignalChangeEvent.SignalChangeImportance importance = change.getImportance();
                
                if (config.isSendOnAllChanges()) {
                    return true; // Sende alle
                }
                
                if (importance == SignalChangeEvent.SignalChangeImportance.CRITICAL && config.isSendOnCriticalChanges()) {
                    return true;
                }
                
                if (importance == SignalChangeEvent.SignalChangeImportance.HIGH && config.isSendOnHighChanges()) {
                    return true;
                }
                
                return false;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Prüft ob das E-Mail-Limit noch nicht erreicht ist
     */
    private boolean checkEmailLimit() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        // Entferne alte Einträge
        while (!sentEmailTimes.isEmpty() && sentEmailTimes.peek().isBefore(oneHourAgo)) {
            sentEmailTimes.poll();
        }
        
        // Aktualisiere Zähler
        emailsSentThisHour.set(sentEmailTimes.size());
        
        // Prüfe Limit
        return emailsSentThisHour.get() < config.getMaxEmailsPerHour();
    }
    
    /**
     * Sendet eine E-Mail
     */
    private EmailSendResult sendEmail(String subject, String body, String emailType) {
        try {
            if (mailSession == null) {
                initializeMailSession();
            }
            
            MimeMessage message = new MimeMessage(mailSession);
            
            // Setze Absender
            try {
                message.setFrom(new InternetAddress(config.getFromEmail(), config.getFromName(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                message.setFrom(new InternetAddress(config.getFromEmail()));
            }
            
            // Setze Empfänger
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(config.getToEmail()));
            
            // Setze Betreff und Body
            message.setSubject(subject, "UTF-8");
            message.setContent(body, "text/html; charset=UTF-8");
            
            // Sende E-Mail
            Transport.send(message);
            
            // Registriere erfolgreiche Sendung
            sentEmailTimes.add(LocalDateTime.now());
            emailsSentThisHour.incrementAndGet();
            
            String successMessage = emailType + " erfolgreich gesendet an " + config.getToEmail();
            LOGGER.info(successMessage);
            return new EmailSendResult(true, successMessage);
            
        } catch (MessagingException e) {
            String errorMessage = "Fehler beim Senden der E-Mail: " + e.getMessage();
            LOGGER.log(Level.SEVERE, errorMessage, e);
            return new EmailSendResult(false, errorMessage);
        }
    }
    
    /**
     * Erstellt Test-E-Mail-Body
     */
    private String createTestEmailBody() {
        StringBuilder body = new StringBuilder();
        
        body.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
        body.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        
        body.append("<h2 style='color: #4caf50;'>✅ FXSSI Monitor - Test-E-Mail</h2>");
        body.append("<p>Diese Test-E-Mail bestätigt, dass Ihre E-Mail-Konfiguration korrekt funktioniert.</p>");
        
        body.append("<div style='background-color: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
        body.append("<h3>Konfigurationsdetails:</h3>");
        body.append("<ul>");
        body.append("<li><strong>SMTP-Server:</strong> ").append(config.getSmtpHost()).append(":").append(config.getSmtpPort()).append("</li>");
        body.append("<li><strong>Von:</strong> ").append(config.getFromEmail()).append(" (").append(config.getFromName()).append(")</li>");
        body.append("<li><strong>An:</strong> ").append(config.getToEmail()).append("</li>");
        body.append("<li><strong>E-Mail-Limit:</strong> ").append(config.getMaxEmailsPerHour()).append(" pro Stunde</li>");
        body.append("<li><strong>Signal-Threshold:</strong> ").append(String.format("%.1f%%", config.getSignalChangeThreshold())).append("</li>");
        body.append("</ul>");
        body.append("</div>");
        
        // NEU: MetaTrader-Sync-Info
        if (config.isMetatraderSyncEnabled()) {
            body.append("<div style='background-color: #e8f5e9; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
            body.append("<h3 style='color: #2e7d32;'>📂 MetaTrader-Synchronisation</h3>");
            body.append("<p><strong>Status:</strong> Aktiviert</p>");
            body.append("<p><strong>Konfigurierte Verzeichnisse:</strong> ").append(config.getMetatraderDirectoryCount()).append("</p>");
            if (config.hasMetatraderDirectory()) {
                body.append("<p><strong>Verzeichnis 1:</strong> <code>").append(config.getMetatraderDirectory()).append("</code></p>");
            }
            if (config.hasMetatraderDirectory2()) {
                body.append("<p><strong>Verzeichnis 2:</strong> <code>").append(config.getMetatraderDirectory2()).append("</code></p>");
            }
            body.append("<p><strong>Sync-Datei:</strong> <code>last_known_signals.csv</code></p>");
            body.append("<p><strong>Format:</strong> Währungspaar;Letztes_Signal;Prozent</p>");
            body.append("<p><em>Automatische Währungsersetzung: XAUUSD → GOLD, XAGUSD → SILBER</em></p>");
            body.append("</div>");
        }
        
        body.append("<div style='background-color: #fff3e0; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
        body.append("<h3>Benachrichtigungseinstellungen:</h3>");
        body.append("<ul>");
        body.append("<li><strong>Kritische Änderungen:</strong> ").append(config.isSendOnCriticalChanges() ? "✅ Aktiviert" : "❌ Deaktiviert").append("</li>");
        body.append("<li><strong>Hohe Änderungen:</strong> ").append(config.isSendOnHighChanges() ? "✅ Aktiviert" : "❌ Deaktiviert").append("</li>");
        body.append("<li><strong>Alle Änderungen:</strong> ").append(config.isSendOnAllChanges() ? "✅ Aktiviert" : "❌ Deaktiviert").append("</li>");
        body.append("</ul>");
        body.append("</div>");
        
        body.append("<p style='margin-top: 30px; color: #666; font-size: 12px; text-align: center;'>");
        body.append("Diese Test-E-Mail wurde am ").append(LocalDateTime.now().format(EMAIL_TIME_FORMATTER));
        body.append(" von FXSSI Monitor gesendet.");
        body.append("</p>");
        
        body.append("</div></body></html>");
        
        return body.toString();
    }
    
    /**
     * Erstellt Betreff für Signalwechsel-E-Mail
     */
    private String createSignalChangeSubject(List<SignalChangeEvent> changes) {
        int criticalCount = (int) changes.stream()
            .filter(c -> c.getImportance() == SignalChangeEvent.SignalChangeImportance.CRITICAL)
            .count();
        
        int highCount = (int) changes.stream()
            .filter(c -> c.getImportance() == SignalChangeEvent.SignalChangeImportance.HIGH)
            .count();
        
        if (changes.size() == 1) {
            SignalChangeEvent change = changes.get(0);
            String icon = change.getImportance() == SignalChangeEvent.SignalChangeImportance.CRITICAL ? "🚨" : "⚠️";
            return String.format("%s FXSSI: %s Signal-Wechsel", icon, change.getCurrencyPair());
        } else {
            String icon = criticalCount > 0 ? "🚨" : "⚠️";
            return String.format("%s FXSSI: %d Signal-Wechsel (%d kritisch)", icon, changes.size(), criticalCount);
        }
    }
    
    /**
     * Erstellt E-Mail-Body für Signalwechsel
     */
    private String createSignalChangeEmailBody(List<SignalChangeEvent> changes) {
        StringBuilder body = new StringBuilder();
        
        int criticalCount = (int) changes.stream()
            .filter(c -> c.getImportance() == SignalChangeEvent.SignalChangeImportance.CRITICAL)
            .count();
        
        int highCount = (int) changes.stream()
            .filter(c -> c.getImportance() == SignalChangeEvent.SignalChangeImportance.HIGH)
            .count();
        
        body.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
        body.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        
        // Header
        body.append("<h2 style='color: #f44336; border-bottom: 2px solid #f44336; padding-bottom: 10px;'>");
        body.append("🚨 FXSSI Trading Signal-Wechsel");
        body.append("</h2>");
        
        // Summary
        body.append("<div style='background-color: #ffebee; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
        body.append("<p style='margin: 0; font-size: 18px; font-weight: bold;'>");
        body.append(changes.size()).append(" Signal-Wechsel erkannt");
        
        if (criticalCount > 0 || highCount > 0) {
            body.append(" (");
            if (criticalCount > 0) {
                body.append("🚨 ").append(criticalCount).append(" kritisch");
            }
            if (highCount > 0) {
                body.append(", ⚠️ ").append(highCount).append(" hoch");
            }
            body.append(")");
        } else if (highCount > 0) {
            body.append(" (⚠️ ").append(highCount).append(" hoch)");
        }
        body.append("</p>");
        body.append("</div>");
        
        // Details der Signalwechsel
        body.append("<h3 style='color: #495057;'>📋 Details der Signalwechsel:</h3>");
        body.append("<div style='background-color: #f8f9fa; padding: 10px; border-radius: 5px;'>");
        
        for (SignalChangeEvent change : changes) {
            String importanceColor = getImportanceColor(change.getImportance());
            String actualityIcon = change.getActuality().getIcon();
            
            body.append("<div style='border-left: 4px solid ").append(importanceColor).append("; padding: 10px; margin: 10px 0; background-color: white;'>");
            body.append("<h4 style='margin: 0 0 5px 0; color: ").append(importanceColor).append(";'>");
            body.append(actualityIcon).append(" ").append(change.getCurrencyPair()).append("</h4>");
            
            body.append("<p style='margin: 5px 0; font-size: 14px;'>");
            body.append("<strong>Wechsel:</strong> ").append(change.getDetailedDescription()).append("<br>");
            body.append("<strong>Zeit:</strong> ").append(change.getChangeTime().format(EMAIL_TIME_FORMATTER)).append("<br>");
            body.append("<strong>Wichtigkeit:</strong> ").append(change.getImportance().getDescription());
            if (change.isDirectReversal()) {
                body.append(" <span style='color: #d32f2f; font-weight: bold;'>(Direkte Umkehrung!)</span>");
            }
            body.append("</p>");
            body.append("</div>");
        }
        
        body.append("</div>");
        
        // Trading-Hinweise
        if (criticalCount > 0) {
            body.append("<div style='background-color: #fff3e0; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #ff9800;'>");
            body.append("<h3 style='margin-top: 0; color: #e65100;'>⚠️ Trading-Hinweise:</h3>");
            body.append("<ul style='margin-bottom: 0;'>");
            body.append("<li>Kritische Signalwechsel erfordern besondere Aufmerksamkeit</li>");
            body.append("<li>Prüfen Sie die aktuellen Marktbedingungen</li>");
            body.append("<li>Direkte Umkehrungen können starke Marktbewegungen anzeigen</li>");
            body.append("</ul>");
            body.append("</div>");
        }
        
        // Footer
        body.append("<p style='margin-top: 30px; color: #6c757d; font-size: 12px; text-align: center; border-top: 1px solid #dee2e6; padding-top: 15px;'>");
        body.append("Diese Benachrichtigung wurde am ").append(LocalDateTime.now().format(EMAIL_TIME_FORMATTER));
        body.append(" automatisch von FXSSI Monitor gesendet.<br>");
        body.append("Aktueller E-Mail-Zähler: ").append(emailsSentThisHour.get()).append("/").append(config.getMaxEmailsPerHour()).append(" pro Stunde");
        body.append("</p>");
        
        body.append("</div></body></html>");
        
        return body.toString();
    }
    
    /**
     * NEU: Gibt die Farbe für ein Trading-Signal zurück
     */
    private String getSignalColor(CurrencyPairData.TradingSignal signal) {
        switch (signal) {
            case BUY: return "#4caf50";      // Grün
            case SELL: return "#f44336";     // Rot
            case NEUTRAL: return "#ff9800";  // Orange
            default: return "#9e9e9e";       // Grau
        }
    }
    
    /**
     * NEU: Gibt das Icon für ein Trading-Signal zurück
     */
    private String getSignalIcon(CurrencyPairData.TradingSignal signal) {
        switch (signal) {
            case BUY: return "🟢";
            case SELL: return "🔴";
            case NEUTRAL: return "🟡";
            default: return "⚪";
        }
    }
    
    /**
     * Gibt die Farbe für eine Wichtigkeitsstufe zurück
     */
    private String getImportanceColor(SignalChangeEvent.SignalChangeImportance importance) {
        switch (importance) {
            case CRITICAL: return "#d32f2f";
            case HIGH: return "#f57c00";
            case MEDIUM: return "#fbc02d";
            default: return "#388e3c";
        }
    }
    
    /**
     * Testet die Verbindung zum E-Mail-Server
     */
    public EmailSendResult testConnection() {
        try {
            LOGGER.info("Teste Verbindung zu E-Mail-Server: " + config.getSmtpHost());
            
            Properties mailProps = config.createMailProperties();
            Session testSession = Session.getInstance(mailProps);
            
            // Versuche Verbindung zum SMTP-Server
            Transport transport = testSession.getTransport("smtp");
            transport.connect(config.getSmtpHost(), config.getSmtpPort(), 
                             config.getUsername(), config.getPassword());
            transport.close();
            
            String successMessage = "Verbindung zu " + config.getSmtpHost() + " erfolgreich";
            LOGGER.info(successMessage);
            return new EmailSendResult(true, successMessage);
            
        } catch (MessagingException e) {
            String errorMessage = "Verbindungsfehler: " + e.getMessage();
            LOGGER.log(Level.WARNING, "E-Mail-Server-Verbindungstest fehlgeschlagen: " + errorMessage, e);
            return new EmailSendResult(false, errorMessage);
        } catch (Exception e) {
            String errorMessage = "Unbekannter Fehler: " + e.getMessage();
            LOGGER.log(Level.WARNING, "E-Mail-Server-Test fehlgeschlagen: " + errorMessage, e);
            return new EmailSendResult(false, errorMessage);
        }
    }
    
    /**
     * NEU: Gibt die aktuelle E-Mail-Statistik inklusive Threshold-Info und MetaTrader-Sync zurück
     */
    public String getEmailStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("E-Mail-Statistiken:\n");
        stats.append("==================\n");
        stats.append("Status: ").append(config.isEmailEnabled() ? "Aktiviert" : "Deaktiviert").append("\n");
        stats.append("Server: ").append(config.getSmtpHost()).append(":").append(config.getSmtpPort()).append("\n");
        stats.append("E-Mails diese Stunde: ").append(emailsSentThisHour.get()).append("/").append(config.getMaxEmailsPerHour()).append("\n");
        stats.append("Warteschlange: ").append(sentEmailTimes.size()).append(" Einträge\n");
        stats.append("Signal-Threshold: ").append(config.getSignalChangeThreshold()).append("%\n");
        
        // NEU: MetaTrader-Sync-Status
        if (config.isMetatraderSyncEnabled()) {
            stats.append("MetaTrader-Sync: Aktiviert (").append(config.getMetatraderDirectoryCount()).append(" Dir)\n");
        } else {
            stats.append("MetaTrader-Sync: Deaktiviert\n");
        }
        
        stats.append("\n").append(lastSentSignalManager.getStatistics());
        
        return stats.toString();
    }
    
    /**
     * NEU: Getter für LastSentSignalManager (für Tests/Debug)
     */
    public LastSentSignalManager getLastSentSignalManager() {
        return lastSentSignalManager;
    }
    
    /**
     * Beendet den E-Mail-Service ordnungsgemäß
     */
    public void shutdown() {
        LOGGER.info("Fahre EmailService herunter...");
        sentEmailTimes.clear();
        emailsSentThisHour.set(0);
        lastSentSignalManager.shutdown();
        LOGGER.info("EmailService heruntergefahren");
    }
    
    // ===== INNERE KLASSEN =====
    
    /**
     * Ergebnis-Klasse für E-Mail-Versendung
     */
    public static class EmailSendResult {
        private final boolean success;
        private final String message;
        
        public EmailSendResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return (success ? "✅ " : "❌ ") + message;
        }
    }
}