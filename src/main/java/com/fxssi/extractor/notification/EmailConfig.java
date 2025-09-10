package com.fxssi.extractor.notification;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Konfigurationsklasse für E-Mail-Einstellungen
 * Speichert und lädt GMX-Server-Konfiguration und E-Mail-Präferenzen
 * 
 * @author Generated for FXSSI Email Notifications
 * @version 1.1 - Erweitert um Signal-Threshold
 */
public class EmailConfig {
    
    private static final Logger LOGGER = Logger.getLogger(EmailConfig.class.getName());
    private static final String CONFIG_FILENAME = "email_config.properties";
    private static final String CONFIG_SUBDIRECTORY = "config";
    
    // GMX Standard-Einstellungen
    private static final String DEFAULT_SMTP_HOST = "mail.gmx.net";
    private static final int DEFAULT_SMTP_PORT = 587;
    private static final boolean DEFAULT_USE_STARTTLS = true;
    private static final boolean DEFAULT_USE_SSL = false;
    private static final double DEFAULT_SIGNAL_THRESHOLD = 3.0; // Standard 3%
    
    // Konfigurationsfelder
    private String smtpHost;
    private int smtpPort;
    private boolean useStartTLS;
    private boolean useSSL;
    private String username;
    private String password;
    private String fromEmail;
    private String fromName;
    private String toEmail;
    private boolean emailEnabled;
    private boolean sendOnCriticalChanges;
    private boolean sendOnHighChanges;
    private boolean sendOnAllChanges;
    private int maxEmailsPerHour;
    private double signalChangeThreshold; // NEU: Threshold für Signalwechsel
    
    private final String dataDirectory;
    private final Path configPath;
    
    /**
     * Konstruktor mit Standard-Datenverzeichnis
     */
    public EmailConfig() {
        this("data");
    }
    
    /**
     * Konstruktor mit konfigurierbarem Datenverzeichnis
     * @param dataDirectory Pfad zum Hauptdatenverzeichnis
     */
    public EmailConfig(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.configPath = Paths.get(dataDirectory, CONFIG_SUBDIRECTORY);
        
        // Setze Standard-Werte für GMX
        initializeDefaults();
        
        LOGGER.info("EmailConfig initialisiert für Verzeichnis: " + dataDirectory);
    }
    
    /**
     * Initialisiert Standard-Werte für GMX-Konfiguration
     */
    private void initializeDefaults() {
        this.smtpHost = DEFAULT_SMTP_HOST;
        this.smtpPort = DEFAULT_SMTP_PORT;
        this.useStartTLS = DEFAULT_USE_STARTTLS;
        this.useSSL = DEFAULT_USE_SSL;
        this.username = "";
        this.password = "";
        this.fromEmail = "";
        this.fromName = "FXSSI Monitor";
        this.toEmail = "";
        this.emailEnabled = false;
        this.sendOnCriticalChanges = true;
        this.sendOnHighChanges = true;
        this.sendOnAllChanges = false;
        this.maxEmailsPerHour = 10;
        this.signalChangeThreshold = DEFAULT_SIGNAL_THRESHOLD; // NEU
    }
    
    /**
     * Erstellt das Konfigurationsverzeichnis falls es nicht existiert
     */
    public void createConfigDirectory() {
        try {
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                LOGGER.info("Konfigurationsverzeichnis erstellt: " + configPath.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Erstellen des Konfigurationsverzeichnisses: " + e.getMessage(), e);
            throw new RuntimeException("Konnte Konfigurationsverzeichnis nicht erstellen", e);
        }
    }
    
    /**
     * Lädt die E-Mail-Konfiguration aus der Datei
     */
    public void loadConfig() {
        createConfigDirectory();
        Path configFile = configPath.resolve(CONFIG_FILENAME);
        
        if (!Files.exists(configFile)) {
            LOGGER.info("Keine E-Mail-Konfigurationsdatei gefunden - verwende Standard-Werte");
            return;
        }
        
        Properties props = new Properties();
        
        try (InputStream is = Files.newInputStream(configFile)) {
            props.load(is);
            
            // Lade Konfigurationswerte
            smtpHost = props.getProperty("smtp.host", DEFAULT_SMTP_HOST);
            smtpPort = Integer.parseInt(props.getProperty("smtp.port", String.valueOf(DEFAULT_SMTP_PORT)));
            useStartTLS = Boolean.parseBoolean(props.getProperty("smtp.starttls", String.valueOf(DEFAULT_USE_STARTTLS)));
            useSSL = Boolean.parseBoolean(props.getProperty("smtp.ssl", String.valueOf(DEFAULT_USE_SSL)));
            username = props.getProperty("auth.username", "");
            password = decodePassword(props.getProperty("auth.password", ""));
            fromEmail = props.getProperty("mail.from.email", "");
            fromName = props.getProperty("mail.from.name", "FXSSI Monitor");
            toEmail = props.getProperty("mail.to.email", "");
            emailEnabled = Boolean.parseBoolean(props.getProperty("email.enabled", "false"));
            sendOnCriticalChanges = Boolean.parseBoolean(props.getProperty("notification.critical", "true"));
            sendOnHighChanges = Boolean.parseBoolean(props.getProperty("notification.high", "true"));
            sendOnAllChanges = Boolean.parseBoolean(props.getProperty("notification.all", "false"));
            maxEmailsPerHour = Integer.parseInt(props.getProperty("limit.max.per.hour", "10"));
            // NEU: Lade Signal-Threshold
            signalChangeThreshold = Double.parseDouble(props.getProperty("signal.threshold.percent", String.valueOf(DEFAULT_SIGNAL_THRESHOLD)));
            
            LOGGER.info("E-Mail-Konfiguration erfolgreich geladen (Threshold: " + signalChangeThreshold + "%)");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Laden der E-Mail-Konfiguration: " + e.getMessage(), e);
            initializeDefaults();
        }
    }
    
    /**
     * Speichert die E-Mail-Konfiguration in die Datei
     */
    public void saveConfig() {
        createConfigDirectory();
        Path configFile = configPath.resolve(CONFIG_FILENAME);
        
        Properties props = new Properties();
        
        // Setze Konfigurationswerte
        props.setProperty("smtp.host", smtpHost);
        props.setProperty("smtp.port", String.valueOf(smtpPort));
        props.setProperty("smtp.starttls", String.valueOf(useStartTLS));
        props.setProperty("smtp.ssl", String.valueOf(useSSL));
        props.setProperty("auth.username", username);
        props.setProperty("auth.password", encodePassword(password));
        props.setProperty("mail.from.email", fromEmail);
        props.setProperty("mail.from.name", fromName);
        props.setProperty("mail.to.email", toEmail);
        props.setProperty("email.enabled", String.valueOf(emailEnabled));
        props.setProperty("notification.critical", String.valueOf(sendOnCriticalChanges));
        props.setProperty("notification.high", String.valueOf(sendOnHighChanges));
        props.setProperty("notification.all", String.valueOf(sendOnAllChanges));
        props.setProperty("limit.max.per.hour", String.valueOf(maxEmailsPerHour));
        // NEU: Speichere Signal-Threshold
        props.setProperty("signal.threshold.percent", String.valueOf(signalChangeThreshold));
        
        try (OutputStream os = Files.newOutputStream(configFile)) {
            props.store(os, "FXSSI E-Mail Konfiguration - GMX Server Setup");
            LOGGER.info("E-Mail-Konfiguration erfolgreich gespeichert");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Speichern der E-Mail-Konfiguration: " + e.getMessage(), e);
            throw new RuntimeException("Konnte E-Mail-Konfiguration nicht speichern", e);
        }
    }
    
    /**
     * Erstellt Java Mail Properties basierend auf der Konfiguration
     */
    public Properties createMailProperties() {
        Properties mailProps = new Properties();
        
        mailProps.setProperty("mail.smtp.host", smtpHost);
        mailProps.setProperty("mail.smtp.port", String.valueOf(smtpPort));
        mailProps.setProperty("mail.smtp.auth", "true");
        
        if (useStartTLS) {
            mailProps.setProperty("mail.smtp.starttls.enable", "true");
            mailProps.setProperty("mail.smtp.starttls.required", "true");
        }
        
        if (useSSL) {
            mailProps.setProperty("mail.smtp.ssl.enable", "true");
            mailProps.setProperty("mail.smtp.socketFactory.port", String.valueOf(smtpPort));
            mailProps.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            mailProps.setProperty("mail.smtp.socketFactory.fallback", "false");
        }
        
        // Debug-Modus für Troubleshooting
        mailProps.setProperty("mail.debug", "false");
        mailProps.setProperty("mail.smtp.timeout", "10000");
        mailProps.setProperty("mail.smtp.connectiontimeout", "10000");
        
        return mailProps;
    }
    
    /**
     * Validiert die aktuelle Konfiguration
     */
    public ValidationResult validateConfig() {
        StringBuilder errors = new StringBuilder();
        boolean isValid = true;
        
        // SMTP-Host prüfen
        if (smtpHost == null || smtpHost.trim().isEmpty()) {
            errors.append("- SMTP-Host ist erforderlich\n");
            isValid = false;
        }
        
        // Port prüfen
        if (smtpPort <= 0 || smtpPort > 65535) {
            errors.append("- SMTP-Port muss zwischen 1 und 65535 liegen\n");
            isValid = false;
        }
        
        // Benutzername prüfen
        if (username == null || username.trim().isEmpty()) {
            errors.append("- Benutzername ist erforderlich\n");
            isValid = false;
        }
        
        // Passwort prüfen
        if (password == null || password.trim().isEmpty()) {
            errors.append("- Passwort ist erforderlich\n");
            isValid = false;
        }
        
        // From-E-Mail prüfen
        if (fromEmail == null || fromEmail.trim().isEmpty() || !isValidEmail(fromEmail)) {
            errors.append("- Gültige Absender-E-Mail ist erforderlich\n");
            isValid = false;
        }
        
        // To-E-Mail prüfen
        if (toEmail == null || toEmail.trim().isEmpty() || !isValidEmail(toEmail)) {
            errors.append("- Gültige Empfänger-E-Mail ist erforderlich\n");
            isValid = false;
        }
        
        // Limit prüfen
        if (maxEmailsPerHour <= 0 || maxEmailsPerHour > 100) {
            errors.append("- E-Mail-Limit pro Stunde muss zwischen 1 und 100 liegen\n");
            isValid = false;
        }
        
        // NEU: Threshold prüfen
        if (signalChangeThreshold < 0.1 || signalChangeThreshold > 50.0) {
            errors.append("- Signal-Threshold muss zwischen 0,1% und 50% liegen\n");
            isValid = false;
        }
        
        return new ValidationResult(isValid, errors.toString());
    }
    
    /**
     * Prüft ob die E-Mail-Adresse gültig ist (vereinfacht)
     */
    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".") && email.length() > 5;
    }
    
    /**
     * Einfache Passwort-Kodierung (Base64)
     */
    private String encodePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        return java.util.Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Einfache Passwort-Dekodierung (Base64)
     */
    private String decodePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return "";
        }
        try {
            return new String(java.util.Base64.getDecoder().decode(encodedPassword), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Dekodieren des Passworts: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Erstellt Standard-GMX-Konfiguration
     */
    public void setGmxDefaults() {
        this.smtpHost = "mail.gmx.net";
        this.smtpPort = 587;
        this.useStartTLS = true;
        this.useSSL = false;
        LOGGER.info("GMX Standard-Konfiguration gesetzt");
    }
    
    /**
     * Gibt eine Zusammenfassung der Konfiguration zurück
     */
    public String getConfigSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("E-Mail-Konfiguration:\n");
        summary.append("====================\n");
        summary.append("Server: ").append(smtpHost).append(":").append(smtpPort).append("\n");
        summary.append("Verschlüsselung: ").append(useStartTLS ? "STARTTLS" : (useSSL ? "SSL" : "Keine")).append("\n");
        summary.append("Benutzername: ").append(username).append("\n");
        summary.append("Von: ").append(fromName).append(" <").append(fromEmail).append(">\n");
        summary.append("An: ").append(toEmail).append("\n");
        summary.append("Status: ").append(emailEnabled ? "Aktiviert" : "Deaktiviert").append("\n");
        summary.append("Benachrichtigungen: ");
        if (sendOnCriticalChanges) summary.append("Kritisch ");
        if (sendOnHighChanges) summary.append("Hoch ");
        if (sendOnAllChanges) summary.append("Alle ");
        summary.append("\n");
        summary.append("Max. E-Mails/Stunde: ").append(maxEmailsPerHour).append("\n");
        summary.append("Signal-Threshold: ").append(signalChangeThreshold).append("%\n"); // NEU
        
        return summary.toString();
    }
    
    /**
     * NEU: Gibt eine ausführliche Erklärung des Signal-Threshold-Mechanismus zurück
     * Diese Erklärung soll in der GUI angezeigt werden wo der Threshold konfiguriert wird
     */
    public String getSignalThresholdExplanation() {
        StringBuilder explanation = new StringBuilder();
        
        explanation.append("SIGNAL-THRESHOLD ANTI-SPAM-SYSTEM\n");
        explanation.append("=================================\n\n");
        
        explanation.append("ZWECK:\n");
        explanation.append("------\n");
        explanation.append("Verhindert E-Mail-Spam durch Signal-Schwankungen um die 55%-Marke.\n");
        explanation.append("Ohne Threshold: Signal wechselt zwischen 54% und 56% → ständige E-Mails\n");
        explanation.append("Mit Threshold: E-Mail nur bei signifikanten Änderungen\n\n");
        
        explanation.append("FUNKTIONSWEISE:\n");
        explanation.append("---------------\n");
        explanation.append("1. ERSTE E-MAIL für ein Währungspaar:\n");
        explanation.append("   → Wird IMMER gesendet (keine Historie vorhanden)\n");
        explanation.append("   → Beispiel: EURUSD BUY bei 58% → E-Mail gesendet\n\n");
        
        explanation.append("2. GLEICHES SIGNAL:\n");
        explanation.append("   → KEINE E-Mail (Signal unverändert)\n");
        explanation.append("   → Beispiel: EURUSD BUY 58% → BUY 59% → keine E-Mail\n\n");
        
        explanation.append("3. NEUES SIGNAL:\n");
        explanation.append("   → E-Mail NUR wenn Threshold überschritten\n");
        explanation.append("   → Berechnung: |aktuelle_% - letzte_versendete_%| >= Threshold\n\n");
        
        explanation.append("BERECHNUNGSBEISPIELE (Threshold: ").append(signalChangeThreshold).append("%):\n");
        explanation.append("-------------------------\n");
        explanation.append("Letzte E-Mail: EURUSD BUY bei 55%\n");
        explanation.append("• Neues Signal: SELL 53% → |53-55| = 2% < ").append(signalChangeThreshold).append("% → KEINE E-Mail\n");
        explanation.append("• Neues Signal: SELL 51% → |51-55| = 4% >= ").append(signalChangeThreshold).append("% → E-Mail senden!\n");
        explanation.append("• Neues Signal: NEUTRAL 59% → |59-55| = 4% >= ").append(signalChangeThreshold).append("% → E-Mail senden!\n\n");
        
        explanation.append("CSV-DATENSPEICHERUNG:\n");
        explanation.append("---------------------\n");
        explanation.append("Datei: ").append(dataDirectory).append("/signal_changes/lastsend.csv\n");
        explanation.append("Inhalt pro Zeile: Währungspaar;Signal;Buy_Prozent;Zeitstempel\n\n");
        
        explanation.append("Beispiel-Inhalt der lastsend.csv:\n");
        explanation.append("Währungspaar;Signal;Buy_Prozent;Zeitstempel\n");
        explanation.append("EURUSD;BUY;55,30;2025-09-10 15:23:45\n");
        explanation.append("GBPUSD;SELL;42,10;2025-09-10 14:15:22\n");
        explanation.append("USDJPY;NEUTRAL;51,80;2025-09-10 13:44:11\n\n");
        
        explanation.append("DATEI-DETAILS:\n");
        explanation.append("--------------\n");
        explanation.append("• Pro Währungspaar gibt es nur EINEN Eintrag (neuester überschreibt alten)\n");
        explanation.append("• Datei wird automatisch bei jeder gesendeten E-Mail aktualisiert\n");
        explanation.append("• Buy_Prozent: Der genaue %-Wert zum Zeitpunkt der letzten E-Mail\n");
        explanation.append("• Zeitstempel: Wann die letzte E-Mail für dieses Paar gesendet wurde\n\n");
        
        explanation.append("EMPFOHLENE THRESHOLD-WERTE:\n");
        explanation.append("---------------------------\n");
        explanation.append("• 1-2%: Sehr sensibel (viele E-Mails, aber wenig verpasst)\n");
        explanation.append("• 3-5%: Ausgewogen (Standard, verhindert Spam effektiv)\n");
        explanation.append("• 5-10%: Konservativ (nur bei starken Bewegungen)\n\n");
        
        explanation.append("AKTUELL KONFIGURIERT: ").append(signalChangeThreshold).append("%\n");
        explanation.append("STATUS: ").append(emailEnabled ? "E-Mails aktiviert" : "E-Mails deaktiviert");
        
        return explanation.toString();
    }
    
    /**
     * NEU: Gibt eine kurze Tooltip-Erklärung für die GUI zurück
     */
    public String getSignalThresholdTooltip() {
        return String.format(
            "Anti-Spam-Threshold: %.1f%%\n" +
            "\n" +
            "Verhindert E-Mail-Spam bei Signal-Schwankungen:\n" +
            "• Erste E-Mail pro Währungspaar: immer gesendet\n" +
            "• Folge-E-Mails: nur wenn Differenz >= %.1f%%\n" +
            "• Beispiel: 55%% → 57%% = 2%% Differenz %s %.1f%%\n" +
            "\n" +
            "Letzte gesendete Signale werden in CSV gespeichert:\n" +
            "%s/signal_changes/lastsend.csv\n" +
            "\n" +
            "Empfohlung: 3-5%% für ausgewogenes Verhalten",
            signalChangeThreshold,
            signalChangeThreshold,
            (2.0 >= signalChangeThreshold ? "≥" : "<"),
            signalChangeThreshold,
            dataDirectory
        );
    }
    
    // ===== GETTER UND SETTER =====
    
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }
    
    public int getSmtpPort() { return smtpPort; }
    public void setSmtpPort(int smtpPort) { this.smtpPort = smtpPort; }
    
    public boolean isUseStartTLS() { return useStartTLS; }
    public void setUseStartTLS(boolean useStartTLS) { this.useStartTLS = useStartTLS; }
    
    public boolean isUseSSL() { return useSSL; }
    public void setUseSSL(boolean useSSL) { this.useSSL = useSSL; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFromEmail() { return fromEmail; }
    public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }
    
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    
    public String getToEmail() { return toEmail; }
    public void setToEmail(String toEmail) { this.toEmail = toEmail; }
    
    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    
    public boolean isSendOnCriticalChanges() { return sendOnCriticalChanges; }
    public void setSendOnCriticalChanges(boolean sendOnCriticalChanges) { this.sendOnCriticalChanges = sendOnCriticalChanges; }
    
    public boolean isSendOnHighChanges() { return sendOnHighChanges; }
    public void setSendOnHighChanges(boolean sendOnHighChanges) { this.sendOnHighChanges = sendOnHighChanges; }
    
    public boolean isSendOnAllChanges() { return sendOnAllChanges; }
    public void setSendOnAllChanges(boolean sendOnAllChanges) { this.sendOnAllChanges = sendOnAllChanges; }
    
    public int getMaxEmailsPerHour() { return maxEmailsPerHour; }
    public void setMaxEmailsPerHour(int maxEmailsPerHour) { this.maxEmailsPerHour = maxEmailsPerHour; }
    
    // NEU: Getter/Setter für Signal-Threshold
    public double getSignalChangeThreshold() { return signalChangeThreshold; }
    public void setSignalChangeThreshold(double signalChangeThreshold) { this.signalChangeThreshold = signalChangeThreshold; }
    
    public String getDataDirectory() { return dataDirectory; }
    
    // ===== INNERE KLASSEN =====
    
    /**
     * Validierungsergebnis-Klasse
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}