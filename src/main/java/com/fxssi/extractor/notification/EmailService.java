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

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.fxssi.extractor.model.SignalChangeEvent;

/**
 * Service-Klasse für E-Mail-Versendung bei Signalwechseln
 * Unterstützt GMX-Server und E-Mail-Limits zur Spam-Vermeidung
 * 
 * @author Generated for FXSSI Email Notifications
 * @version 1.0
 */
public class EmailService {
    
    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());
    private static final DateTimeFormatter EMAIL_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    
    private EmailConfig config;
    private Session mailSession;
    
    // E-Mail-Limit-Tracking
    private final ConcurrentLinkedQueue<LocalDateTime> sentEmailTimes;
    private final AtomicInteger emailsSentThisHour;
    
    /**
     * Konstruktor
     * @param config E-Mail-Konfiguration
     */
    public EmailService(EmailConfig config) {
        this.config = config;
        this.sentEmailTimes = new ConcurrentLinkedQueue<>();
        this.emailsSentThisHour = new AtomicInteger(0);
        
        initializeMailSession();
        LOGGER.info("EmailService initialisiert für Server: " + config.getSmtpHost());
    }
    
    /**
     * Aktualisiert die E-Mail-Konfiguration
     * @param newConfig Neue Konfiguration
     */
    public void updateConfig(EmailConfig newConfig) {
        this.config = newConfig;
        initializeMailSession();
        LOGGER.info("E-Mail-Konfiguration aktualisiert");
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
     * Sendet eine Signalwechsel-Benachrichtigung
     * @param signalChanges Liste der Signalwechsel
     * @return Erfolgsmeldung oder Fehlermeldung
     */
    public EmailSendResult sendSignalChangeNotification(List<SignalChangeEvent> signalChanges) {
        if (!config.isEmailEnabled()) {
            LOGGER.fine("E-Mail-Benachrichtigungen sind deaktiviert");
            return new EmailSendResult(false, "E-Mail-Benachrichtigungen sind deaktiviert");
        }
        
        if (signalChanges == null || signalChanges.isEmpty()) {
            return new EmailSendResult(false, "Keine Signalwechsel zum Versenden");
        }
        
        // Filtere Signalwechsel basierend auf Konfiguration
        List<SignalChangeEvent> relevantChanges = filterRelevantChanges(signalChanges);
        
        if (relevantChanges.isEmpty()) {
            LOGGER.fine("Keine relevanten Signalwechsel für E-Mail-Benachrichtigung");
            return new EmailSendResult(false, "Keine relevanten Signalwechsel");
        }
        
        // Prüfe E-Mail-Limit
        if (!checkEmailLimit()) {
            String limitMessage = "E-Mail-Limit erreicht (" + config.getMaxEmailsPerHour() + "/Stunde)";
            LOGGER.warning(limitMessage);
            return new EmailSendResult(false, limitMessage);
        }
        
        try {
            String subject = createSignalChangeSubject(relevantChanges);
            String body = createSignalChangeEmailBody(relevantChanges);
            
            EmailSendResult result = sendEmail(subject, body, "Signalwechsel-Benachrichtigung");
            
            if (result.isSuccess()) {
                // Tracking für erfolgreich gesendete E-Mails
                recordEmailSent();
                LOGGER.info("Signalwechsel-E-Mail erfolgreich gesendet für " + relevantChanges.size() + " Wechsel");
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Senden der Signalwechsel-E-Mail: " + e.getMessage(), e);
            return new EmailSendResult(false, "Fehler beim Senden: " + e.getMessage());
        }
    }
    
    /**
     * Zentrale E-Mail-Versendungs-Methode
     */
    private EmailSendResult sendEmail(String subject, String body, String emailType) {
        try {
            LOGGER.info("Sende " + emailType + " an: " + config.getToEmail());
            
            // Erstelle Nachricht
            MimeMessage message = new MimeMessage(mailSession);
            
            // Setze Absender
            InternetAddress fromAddress = new InternetAddress(config.getFromEmail(), config.getFromName());
            message.setFrom(fromAddress);
            
            // Setze Empfänger
            InternetAddress toAddress = new InternetAddress(config.getToEmail());
            message.setRecipient(Message.RecipientType.TO, toAddress);
            
            // Setze Betreff und Inhalt
            message.setSubject(subject, "UTF-8");
            message.setText(body, "UTF-8", "html");
            
            // Setze zusätzliche Header
            message.setHeader("X-Mailer", "FXSSI Monitor v1.0");
            message.setSentDate(new java.util.Date());
            
            // Sende E-Mail
            Transport.send(message);
            
            String successMessage = emailType + " erfolgreich gesendet an " + config.getToEmail();
            LOGGER.info(successMessage);
            
            return new EmailSendResult(true, successMessage);
            
        } catch (MessagingException e) {
            String errorMessage = "Messaging-Fehler: " + e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " (Ursache: " + e.getCause().getMessage() + ")";
            }
            LOGGER.log(Level.SEVERE, "Fehler beim Senden der E-Mail: " + errorMessage, e);
            return new EmailSendResult(false, errorMessage);
            
        } catch (UnsupportedEncodingException e) {
            String errorMessage = "Encoding-Fehler: " + e.getMessage();
            LOGGER.log(Level.SEVERE, "Fehler beim E-Mail-Encoding: " + errorMessage, e);
            return new EmailSendResult(false, errorMessage);
            
        } catch (Exception e) {
            String errorMessage = "Unbekannter Fehler: " + e.getMessage();
            LOGGER.log(Level.SEVERE, "Unbekannter Fehler beim E-Mail-Versand: " + errorMessage, e);
            return new EmailSendResult(false, errorMessage);
        }
    }
    
    /**
     * Filtert relevante Signalwechsel basierend auf Konfiguration
     */
    private List<SignalChangeEvent> filterRelevantChanges(List<SignalChangeEvent> allChanges) {
        return allChanges.stream()
            .filter(change -> {
                SignalChangeEvent.SignalChangeImportance importance = change.getImportance();
                
                switch (importance) {
                    case CRITICAL:
                        return config.isSendOnCriticalChanges();
                    case HIGH:
                        return config.isSendOnHighChanges();
                    case MEDIUM:
                    case LOW:
                        return config.isSendOnAllChanges();
                    default:
                        return false;
                }
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Prüft das E-Mail-Limit pro Stunde
     */
    private boolean checkEmailLimit() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        // Entferne alte Einträge
        sentEmailTimes.removeIf(time -> time.isBefore(oneHourAgo));
        
        // Prüfe aktuelles Limit
        int currentCount = sentEmailTimes.size();
        emailsSentThisHour.set(currentCount);
        
        return currentCount < config.getMaxEmailsPerHour();
    }
    
    /**
     * Zeichnet eine gesendete E-Mail auf
     */
    private void recordEmailSent() {
        LocalDateTime now = LocalDateTime.now();
        sentEmailTimes.offer(now);
        emailsSentThisHour.incrementAndGet();
        
        LOGGER.fine("E-Mail aufgezeichnet. Aktuell: " + emailsSentThisHour.get() + "/" + config.getMaxEmailsPerHour() + " pro Stunde");
    }
    
    /**
     * Erstellt den Betreff für Signalwechsel-E-Mails
     */
    private String createSignalChangeSubject(List<SignalChangeEvent> changes) {
        if (changes.size() == 1) {
            SignalChangeEvent change = changes.get(0);
            return String.format("🔄 FXSSI Signal: %s %s", 
                change.getCurrencyPair(), change.getChangeDescription());
        } else {
            long criticalCount = changes.stream()
                .filter(c -> c.getImportance() == SignalChangeEvent.SignalChangeImportance.CRITICAL)
                .count();
            
            if (criticalCount > 0) {
                return String.format("🚨 FXSSI: %d Signalwechsel (%d kritisch)", changes.size(), criticalCount);
            } else {
                return String.format("🔄 FXSSI: %d Signalwechsel erkannt", changes.size());
            }
        }
    }
    
    /**
     * Erstellt den E-Mail-Body für Test-E-Mails
     */
    private String createTestEmailBody() {
        StringBuilder body = new StringBuilder();
        
        body.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>FXSSI Test-E-Mail</title></head><body>");
        body.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        
        body.append("<h2 style='color: #2E86AB; border-bottom: 2px solid #2E86AB; padding-bottom: 10px;'>");
        body.append("✅ FXSSI Monitor - Test-E-Mail</h2>");
        
        body.append("<p>Diese Test-E-Mail bestätigt, dass Ihre E-Mail-Konfiguration korrekt funktioniert.</p>");
        
        body.append("<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
        body.append("<h3 style='margin-top: 0; color: #495057;'>📧 Konfiguration:</h3>");
        body.append("<ul style='margin-bottom: 0;'>");
        body.append("<li><strong>Server:</strong> ").append(config.getSmtpHost()).append(":").append(config.getSmtpPort()).append("</li>");
        body.append("<li><strong>Verschlüsselung:</strong> ").append(config.isUseStartTLS() ? "STARTTLS" : (config.isUseSSL() ? "SSL" : "Keine")).append("</li>");
        body.append("<li><strong>Von:</strong> ").append(config.getFromName()).append(" &lt;").append(config.getFromEmail()).append("&gt;</li>");
        body.append("<li><strong>An:</strong> ").append(config.getToEmail()).append("</li>");
        body.append("</ul>");
        body.append("</div>");
        
        body.append("<div style='background-color: #e7f3ff; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
        body.append("<h3 style='margin-top: 0; color: #0066cc;'>🔔 Benachrichtigungs-Einstellungen:</h3>");
        body.append("<ul style='margin-bottom: 0;'>");
        body.append("<li>Kritische Wechsel: ").append(config.isSendOnCriticalChanges() ? "✅ Aktiviert" : "❌ Deaktiviert").append("</li>");
        body.append("<li>Hohe Wichtigkeit: ").append(config.isSendOnHighChanges() ? "✅ Aktiviert" : "❌ Deaktiviert").append("</li>");
        body.append("<li>Alle Wechsel: ").append(config.isSendOnAllChanges() ? "✅ Aktiviert" : "❌ Deaktiviert").append("</li>");
        body.append("<li>Max. E-Mails/Stunde: ").append(config.getMaxEmailsPerHour()).append("</li>");
        body.append("</ul>");
        body.append("</div>");
        
        body.append("<p style='margin-top: 30px; color: #6c757d; font-size: 12px;'>");
        body.append("Diese E-Mail wurde am ").append(LocalDateTime.now().format(EMAIL_TIME_FORMATTER));
        body.append(" von FXSSI Monitor gesendet.<br>");
        body.append("Sie erhalten diese E-Mail, weil Sie eine Test-E-Mail angefordert haben.");
        body.append("</p>");
        
        body.append("</div></body></html>");
        
        return body.toString();
    }
    
    /**
     * Erstellt den E-Mail-Body für Signalwechsel-Benachrichtigungen
     */
    private String createSignalChangeEmailBody(List<SignalChangeEvent> changes) {
        StringBuilder body = new StringBuilder();
        
        body.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>FXSSI Signalwechsel</title></head><body>");
        body.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        
        // Header
        body.append("<h2 style='color: #E74C3C; border-bottom: 2px solid #E74C3C; padding-bottom: 10px;'>");
        body.append("🔄 FXSSI Signalwechsel-Alarm</h2>");
        
        // Zusammenfassung
        long criticalCount = changes.stream().filter(c -> c.getImportance() == SignalChangeEvent.SignalChangeImportance.CRITICAL).count();
        long highCount = changes.stream().filter(c -> c.getImportance() == SignalChangeEvent.SignalChangeImportance.HIGH).count();
        
        body.append("<div style='background-color: #ffebee; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
        body.append("<h3 style='margin-top: 0; color: #c62828;'>📊 Zusammenfassung:</h3>");
        body.append("<p style='margin-bottom: 0; font-size: 16px;'>");
        body.append("<strong>").append(changes.size()).append(" Signalwechsel erkannt</strong>");
        if (criticalCount > 0) {
            body.append(" (🚨 ").append(criticalCount).append(" kritisch");
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
     * Gibt die aktuelle E-Mail-Statistik zurück
     */
    public String getEmailStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("E-Mail-Statistiken:\n");
        stats.append("==================\n");
        stats.append("Status: ").append(config.isEmailEnabled() ? "Aktiviert" : "Deaktiviert").append("\n");
        stats.append("Server: ").append(config.getSmtpHost()).append(":").append(config.getSmtpPort()).append("\n");
        stats.append("E-Mails diese Stunde: ").append(emailsSentThisHour.get()).append("/").append(config.getMaxEmailsPerHour()).append("\n");
        stats.append("Warteschlange: ").append(sentEmailTimes.size()).append(" Einträge\n");
        
        return stats.toString();
    }
    
    /**
     * Beendet den E-Mail-Service ordnungsgemäß
     */
    public void shutdown() {
        LOGGER.info("Fahre EmailService herunter...");
        sentEmailTimes.clear();
        emailsSentThisHour.set(0);
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