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

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.model.SignalChangeEvent;
import com.fxssi.extractor.storage.LastSentSignalManager;

/**
 * Service-Klasse für E-Mail-Versendung bei Signalwechseln
 * Unterstützt GMX-Server und E-Mail-Limits zur Spam-Vermeidung
 * ERWEITERT um Threshold-basierte Anti-Spam-Funktionalität
 * 
 * @author Generated for FXSSI Email Notifications
 * @version 1.1 - Anti-Spam mit Signal-Threshold
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
     * @param config E-Mail-Konfiguration
     */
    public EmailService(EmailConfig config) {
        this.config = config;
        this.sentEmailTimes = new ConcurrentLinkedQueue<>();
        this.emailsSentThisHour = new AtomicInteger(0);
        this.lastSentSignalManager = new LastSentSignalManager(config.getDataDirectory()); // NEU
        
        // Initialisiere LastSentSignalManager
        lastSentSignalManager.loadLastSentSignals(); // NEU
        
        initializeMailSession();
        LOGGER.info("EmailService initialisiert für Server: " + config.getSmtpHost() + " (mit Anti-Spam-Threshold: " + config.getSignalChangeThreshold() + "%)");
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
        
        initializeMailSession();
        LOGGER.info("EmailService initialisiert mit externem LastSentSignalManager");
    }
    
    /**
     * Aktualisiert die E-Mail-Konfiguration
     * @param newConfig Neue Konfiguration
     */
    public void updateConfig(EmailConfig newConfig) {
        this.config = newConfig;
        initializeMailSession();
        LOGGER.info("E-Mail-Konfiguration aktualisiert (Threshold: " + newConfig.getSignalChangeThreshold() + "%)");
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
                for (CurrencyPairData data : thresholdExceededData) {
                    lastSentSignalManager.recordSentSignal(
                        data.getCurrencyPair(), 
                        data.getTradingSignal(), 
                        data.getBuyPercentage()
                    );
                }
                
                // Tracking für erfolgreich gesendete E-Mails
                recordEmailSent();
                LOGGER.info("Threshold-Signal-E-Mail erfolgreich gesendet für " + thresholdExceededData.size() + " Signale");
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Senden der Threshold-Signal-E-Mail: " + e.getMessage(), e);
            return new EmailSendResult(false, "Fehler beim Senden: " + e.getMessage());
        }
    }
    
    /**
     * ORIGINAL: Sendet eine Signalwechsel-Benachrichtigung (ohne Threshold-Prüfung)
     * Wird für Kompatibilität beibehalten
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
     * NEU: Filtert Signale die den konfigurierten Threshold überschreiten
     */
    private List<CurrencyPairData> filterSignalsAboveThreshold(List<CurrencyPairData> allData) {
        double thresholdPercent = config.getSignalChangeThreshold();
        
        return allData.stream()
            .filter(data -> {
                boolean shouldSend = lastSentSignalManager.shouldSendEmail(
                    data.getCurrencyPair(),
                    data.getTradingSignal(),
                    data.getBuyPercentage(),
                    thresholdPercent
                );
                
                if (shouldSend) {
                    LOGGER.fine(String.format("Threshold überschritten für %s: %s bei %.1f%%",
                        data.getCurrencyPair(), data.getTradingSignal().getDescription(), data.getBuyPercentage()));
                }
                
                return shouldSend;
            })
            .collect(Collectors.toList());
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
            message.setHeader("X-Mailer", "FXSSI Monitor v1.1");
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
            .collect(Collectors.toList());
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
     * NEU: Erstellt den Betreff für Threshold-Signal-E-Mails
     */
    private String createThresholdSignalSubject(List<CurrencyPairData> data) {
        if (data.size() == 1) {
            CurrencyPairData single = data.get(0);
            return String.format("🔔 FXSSI Signal: %s %s (%.1f%%)", 
                single.getCurrencyPair(), single.getTradingSignal().getDescription(), single.getBuyPercentage());
        } else {
            long buySignals = data.stream().filter(d -> d.getTradingSignal() == CurrencyPairData.TradingSignal.BUY).count();
            long sellSignals = data.stream().filter(d -> d.getTradingSignal() == CurrencyPairData.TradingSignal.SELL).count();
            
            return String.format("🔔 FXSSI: %d Signale (🟢%d Buy, 🔴%d Sell)", data.size(), buySignals, sellSignals);
        }
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
     * NEU: Erstellt den E-Mail-Body für Threshold-Signale
     */
    private String createThresholdSignalEmailBody(List<CurrencyPairData> data) {
        StringBuilder body = new StringBuilder();
        
        body.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>FXSSI Threshold Signale</title></head><body>");
        body.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        
        // Header
        body.append("<h2 style='color: #2E86AB; border-bottom: 2px solid #2E86AB; padding-bottom: 10px;'>");
        body.append("🔔 FXSSI Signal-Alarm (Threshold: " + config.getSignalChangeThreshold() + "%)</h2>");
        
        // Threshold-Info
        body.append("<div style='background-color: #e3f2fd; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
        body.append("<h3 style='margin-top: 0; color: #1976d2;'>📊 Anti-Spam-Filterung aktiv:</h3>");
        body.append("<p style='margin-bottom: 0;'>");
        body.append("Diese E-Mail wird nur gesendet wenn Signale sich um mindestens <strong>");
        body.append(config.getSignalChangeThreshold()).append("%</strong> geändert haben.");
        body.append("</p>");
        body.append("</div>");
        
        // Signale
        body.append("<h3 style='color: #495057;'>📋 Aktuelle Signale über Threshold:</h3>");
        body.append("<div style='background-color: #f8f9fa; padding: 10px; border-radius: 5px;'>");
        
        for (CurrencyPairData currencyData : data) {
            String signalColor = getSignalColor(currencyData.getTradingSignal());
            String signalIcon = getSignalIcon(currencyData.getTradingSignal());
            
            LastSentSignalManager.LastSentSignal lastSent = lastSentSignalManager.getLastSentSignal(currencyData.getCurrencyPair());
            
            body.append("<div style='border-left: 4px solid ").append(signalColor).append("; padding: 10px; margin: 10px 0; background-color: white;'>");
            body.append("<h4 style='margin: 0 0 5px 0; color: ").append(signalColor).append(";'>");
            body.append(signalIcon).append(" ").append(currencyData.getCurrencyPair()).append("</h4>");
            
            body.append("<p style='margin: 5px 0; font-size: 14px;'>");
            body.append("<strong>Aktuelles Signal:</strong> ").append(currencyData.getTradingSignal().getDescription());
            body.append(" (").append(String.format("%.1f", currencyData.getBuyPercentage())).append("% Buy)<br>");
            
            if (lastSent != null) {
                double diff = Math.abs(currencyData.getBuyPercentage() - lastSent.getBuyPercentage());
                body.append("<strong>Letzte E-Mail:</strong> ").append(lastSent.getSignal().getDescription());
                body.append(" (").append(String.format("%.1f", lastSent.getBuyPercentage())).append("% Buy)<br>");
                body.append("<strong>Änderung:</strong> ").append(String.format("%.1f", diff)).append("% ");
                body.append("(Threshold: ").append(config.getSignalChangeThreshold()).append("%)");
            } else {
                body.append("<strong>Status:</strong> Erste E-Mail für dieses Währungspaar");
            }
            body.append("</p>");
            body.append("</div>");
        }
        
        body.append("</div>");
        
        // LastSent Statistiken
        body.append("<div style='background-color: #f1f3f4; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
        body.append("<h3 style='margin-top: 0; color: #5f6368;'>📈 Anti-Spam-Statistiken:</h3>");
        body.append("<ul style='margin-bottom: 0;'>");
        body.append("<li>Überwachte Währungspaare: ").append(lastSentSignalManager.getStatistics().split("Überwachte Währungspaare: ")[1].split("\n")[0]).append("</li>");
        body.append("<li>Konfigurierter Threshold: ").append(config.getSignalChangeThreshold()).append("%</li>");
        body.append("<li>Diese E-Mail wurde gesendet weil alle Signale den Threshold überschritten haben</li>");
        body.append("</ul>");
        body.append("</div>");
        
        // Footer
        body.append("<p style='margin-top: 30px; color: #6c757d; font-size: 12px; text-align: center; border-top: 1px solid #dee2e6; padding-top: 15px;'>");
        body.append("Diese Benachrichtigung wurde am ").append(LocalDateTime.now().format(EMAIL_TIME_FORMATTER));
        body.append(" automatisch von FXSSI Monitor gesendet.<br>");
        body.append("E-Mail-Zähler: ").append(emailsSentThisHour.get()).append("/").append(config.getMaxEmailsPerHour()).append(" pro Stunde<br>");
        body.append("Anti-Spam-Threshold: ").append(config.getSignalChangeThreshold()).append("% aktiviert");
        body.append("</p>");
        
        body.append("</div></body></html>");
        
        return body.toString();
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
        body.append("<h3 style='margin-top: 0; color: #495057;'>🔧 Konfiguration:</h3>");
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
        body.append("<li><strong>Signal-Threshold: ").append(config.getSignalChangeThreshold()).append("% (NEU!)</strong></li>"); // NEU
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
     * NEU: Gibt die aktuelle E-Mail-Statistik inklusive Threshold-Info zurück
     */
    public String getEmailStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("E-Mail-Statistiken:\n");
        stats.append("==================\n");
        stats.append("Status: ").append(config.isEmailEnabled() ? "Aktiviert" : "Deaktiviert").append("\n");
        stats.append("Server: ").append(config.getSmtpHost()).append(":").append(config.getSmtpPort()).append("\n");
        stats.append("E-Mails diese Stunde: ").append(emailsSentThisHour.get()).append("/").append(config.getMaxEmailsPerHour()).append("\n");
        stats.append("Warteschlange: ").append(sentEmailTimes.size()).append(" Einträge\n");
        stats.append("Signal-Threshold: ").append(config.getSignalChangeThreshold()).append("%\n"); // NEU
        stats.append("\n").append(lastSentSignalManager.getStatistics()); // NEU
        
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
        lastSentSignalManager.shutdown(); // NEU
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