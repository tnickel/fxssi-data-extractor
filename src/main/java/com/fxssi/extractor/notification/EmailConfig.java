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
 * ERWEITERT um MetaTrader-Synchronisation-Support mit Dual-Directory
 * 
 * @author Generated for FXSSI Email Notifications
 * @version 1.3 - Dual MetaTrader-Directory Support
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
    // NEU: MetaTrader Standard-Einstellungen
    private static final boolean DEFAULT_METATRADER_SYNC_ENABLED = false;
    private static final String DEFAULT_METATRADER_DIRECTORY = "";
    private static final String DEFAULT_METATRADER_DIRECTORY2 = "";
    
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
    private double signalChangeThreshold; // Threshold für Signalwechsel
    
    // NEU: MetaTrader-Synchronisation Felder (Dual-Directory)
    private boolean metatraderSyncEnabled;
    private String metatraderDirectory;
    private String metatraderDirectory2; // NEUES FELD für zweites Verzeichnis
    
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
        
        // Setze Standard-Werte für GMX und MetaTrader
        initializeDefaults();
        
        LOGGER.info("EmailConfig initialisiert für Verzeichnis: " + dataDirectory);
    }
    
    /**
     * Initialisiert Standard-Werte für GMX-Konfiguration und MetaTrader
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
        this.signalChangeThreshold = DEFAULT_SIGNAL_THRESHOLD;
        
        // NEU: MetaTrader Standard-Werte (Dual-Directory)
        this.metatraderSyncEnabled = DEFAULT_METATRADER_SYNC_ENABLED;
        this.metatraderDirectory = DEFAULT_METATRADER_DIRECTORY;
        this.metatraderDirectory2 = DEFAULT_METATRADER_DIRECTORY2;
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
     * ERWEITERT um MetaTrader-Synchronisation mit Dual-Directory
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
            signalChangeThreshold = Double.parseDouble(props.getProperty("signal.threshold.percent", String.valueOf(DEFAULT_SIGNAL_THRESHOLD)));
            
            // NEU: Lade MetaTrader-Konfiguration (Dual-Directory)
            metatraderSyncEnabled = Boolean.parseBoolean(props.getProperty("metatrader.sync.enabled", String.valueOf(DEFAULT_METATRADER_SYNC_ENABLED)));
            metatraderDirectory = props.getProperty("metatrader.directory", DEFAULT_METATRADER_DIRECTORY);
            metatraderDirectory2 = props.getProperty("metatrader.directory2", DEFAULT_METATRADER_DIRECTORY2); // NEUES FELD
            
            LOGGER.info("E-Mail-Konfiguration erfolgreich geladen (Threshold: " + signalChangeThreshold + "%, MetaTrader-Sync: " + metatraderSyncEnabled + 
                       ", Directories: " + (hasMetatraderDirectory() ? "1" : "0") + (hasMetatraderDirectory2() ? "+1" : "") + ")");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Laden der E-Mail-Konfiguration: " + e.getMessage(), e);
            initializeDefaults();
        }
    }
    
    /**
     * Speichert die E-Mail-Konfiguration in eine Datei
     * ERWEITERT um MetaTrader-Synchronisation mit Dual-Directory
     */
    public void saveConfig() {
        createConfigDirectory();
        Path configFile = configPath.resolve(CONFIG_FILENAME);
        
        Properties props = new Properties();
        
        // Speichere Konfigurationswerte
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
        props.setProperty("signal.threshold.percent", String.valueOf(signalChangeThreshold));
        
        // NEU: Speichere MetaTrader-Konfiguration (Dual-Directory)
        props.setProperty("metatrader.sync.enabled", String.valueOf(metatraderSyncEnabled));
        props.setProperty("metatrader.directory", metatraderDirectory != null ? metatraderDirectory : "");
        props.setProperty("metatrader.directory2", metatraderDirectory2 != null ? metatraderDirectory2 : ""); // NEUES FELD
        
        try (OutputStream os = Files.newOutputStream(configFile)) {
            props.store(os, "FXSSI E-Mail-Konfiguration (mit Dual MetaTrader-Directory Support)");
            LOGGER.info("E-Mail-Konfiguration gespeichert in: " + configFile.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Speichern der E-Mail-Konfiguration: " + e.getMessage(), e);
            throw new RuntimeException("Konnte E-Mail-Konfiguration nicht speichern", e);
        }
    }
    
    /**
     * Validiert die Konfiguration
     * ERWEITERT um MetaTrader-Directory-Validierung
     */
    public ValidationResult validateConfig() {
        if (!emailEnabled) {
            return new ValidationResult(true, ""); // Keine Validierung wenn deaktiviert
        }
        
        // Validiere GMX-Einstellungen
        if (username == null || username.trim().isEmpty()) {
            return new ValidationResult(false, "Benutzername ist erforderlich");
        }
        
        if (password == null || password.trim().isEmpty()) {
            return new ValidationResult(false, "Passwort ist erforderlich");
        }
        
        if (fromEmail == null || fromEmail.trim().isEmpty() || !fromEmail.contains("@")) {
            return new ValidationResult(false, "Gültige Absender-E-Mail ist erforderlich");
        }
        
        if (toEmail == null || toEmail.trim().isEmpty() || !toEmail.contains("@")) {
            return new ValidationResult(false, "Gültige Empfänger-E-Mail ist erforderlich");
        }
        
        if (maxEmailsPerHour < 1 || maxEmailsPerHour > 100) {
            return new ValidationResult(false, "E-Mail-Limit muss zwischen 1 und 100 liegen");
        }
        
        if (signalChangeThreshold < 0.1 || signalChangeThreshold > 50.0) {
            return new ValidationResult(false, "Signal-Threshold muss zwischen 0.1% und 50% liegen");
        }
        
        // NEU: Validiere MetaTrader-Verzeichnisse
        if (metatraderSyncEnabled) {
            if (!hasMetatraderDirectory() && !hasMetatraderDirectory2()) {
                return new ValidationResult(false, "MetaTrader-Synchronisation aktiviert, aber kein Verzeichnis konfiguriert");
            }
            
            // Prüfe ob Verzeichnisse existieren
            if (hasMetatraderDirectory()) {
                Path dir1 = Paths.get(metatraderDirectory);
                if (!Files.exists(dir1)) {
                    return new ValidationResult(false, "MetaTrader-Verzeichnis 1 existiert nicht: " + metatraderDirectory);
                }
                if (!Files.isDirectory(dir1)) {
                    return new ValidationResult(false, "MetaTrader-Pfad 1 ist kein Verzeichnis: " + metatraderDirectory);
                }
            }
            
            if (hasMetatraderDirectory2()) {
                Path dir2 = Paths.get(metatraderDirectory2);
                if (!Files.exists(dir2)) {
                    return new ValidationResult(false, "MetaTrader-Verzeichnis 2 existiert nicht: " + metatraderDirectory2);
                }
                if (!Files.isDirectory(dir2)) {
                    return new ValidationResult(false, "MetaTrader-Pfad 2 ist kein Verzeichnis: " + metatraderDirectory2);
                }
            }
        }
        
        return new ValidationResult(true, "Konfiguration ist gültig");
    }
    
    /**
     * Erstellt Properties-Objekt für JavaMail
     */
    public Properties createMailProperties() {
        Properties props = new Properties();
        
        // SMTP-Server-Konfiguration
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", "true");
        
        // TLS/SSL-Konfiguration
        if (useStartTLS) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        
        if (useSSL) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", String.valueOf(smtpPort));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }
        
        // Timeouts
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.connectiontimeout", "10000");
        
        return props;
    }
    
    /**
     * Kodiert das Passwort für die Speicherung (einfache Base64-Kodierung)
     */
    private String encodePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        try {
            return java.util.Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Kodieren des Passworts", e);
            return password;
        }
    }
    
    /**
     * Dekodiert das Passwort beim Laden
     */
    private String decodePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return "";
        }
        try {
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(encodedPassword);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Dekodieren des Passworts - verwende Originalwert", e);
            return encodedPassword;
        }
    }
    
    /**
     * NEU: Prüft ob das erste MetaTrader-Verzeichnis gesetzt ist
     */
    public boolean hasMetatraderDirectory() {
        return metatraderDirectory != null && !metatraderDirectory.trim().isEmpty();
    }
    
    /**
     * NEU: Prüft ob das zweite MetaTrader-Verzeichnis gesetzt ist
     */
    public boolean hasMetatraderDirectory2() {
        return metatraderDirectory2 != null && !metatraderDirectory2.trim().isEmpty();
    }
    
    /**
     * NEU: Gibt die Anzahl konfigurierter MetaTrader-Verzeichnisse zurück
     */
    public int getMetatraderDirectoryCount() {
        int count = 0;
        if (hasMetatraderDirectory()) count++;
        if (hasMetatraderDirectory2()) count++;
        return count;
    }
    
    /**
     * Gibt eine detaillierte Erklärung des Signal-Threshold-Systems zurück
     * ERWEITERT um Dual-Directory-Informationen
     */
    public String getSignalThresholdExplanation() {
        StringBuilder explanation = new StringBuilder();
        
        explanation.append("SIGNAL-THRESHOLD-SYSTEM (Anti-Spam-Mechanismus)\n");
        explanation.append("=================================================\n\n");
        
        explanation.append("WAS IST DER SIGNAL-THRESHOLD?\n");
        explanation.append("-----------------------------\n");
        explanation.append("Der Signal-Threshold ist ein Prozentwert, der bestimmt, wann eine neue E-Mail\n");
        explanation.append("für ein Währungspaar gesendet wird. Er verhindert E-Mail-Spam bei kleinen\n");
        explanation.append("Schwankungen der Buy/Sell-Verhältnisse.\n\n");
        
        explanation.append("FUNKTIONSWEISE:\n");
        explanation.append("---------------\n");
        explanation.append("1. ERSTE E-MAIL: Wird IMMER gesendet (egal welcher Threshold)\n");
        explanation.append("2. FOLGE-E-MAILS: Nur wenn die Differenz >= ").append(signalChangeThreshold).append("% ist\n\n");
        
        explanation.append("Die Differenz wird wie folgt berechnet:\n");
        explanation.append("• Differenz = |Neuer Buy_Prozent - Letzter gesendeter Buy_Prozent|\n");
        explanation.append("• Wenn Differenz >= ").append(signalChangeThreshold).append("% → E-Mail wird gesendet\n");
        explanation.append("• Wenn Differenz < ").append(signalChangeThreshold).append("% → E-Mail wird NICHT gesendet\n\n");
        
        explanation.append("WICHTIG:\n");
        explanation.append("--------\n");
        explanation.append("• Der Threshold gilt NUR für dasselbe Währungspaar\n");
        explanation.append("• Jedes Währungspaar hat seinen eigenen \"letzten gesendeten\" Status\n");
        explanation.append("• Ein Signalwechsel (BUY→SELL) triggert immer eine E-Mail wenn Threshold erreicht\n\n");
        
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
        
        explanation.append("METATRADER-SYNCHRONISATION (DUAL-DIRECTORY):\n");
        explanation.append("--------------------------------------------\n");
        if (metatraderSyncEnabled) {
            explanation.append("Status: ✅ Aktiviert\n");
            int dirCount = getMetatraderDirectoryCount();
            explanation.append("Konfigurierte Verzeichnisse: ").append(dirCount).append("\n\n");
            
            if (hasMetatraderDirectory()) {
                explanation.append("📂 Verzeichnis 1: ").append(metatraderDirectory).append("\n");
            }
            if (hasMetatraderDirectory2()) {
                explanation.append("📂 Verzeichnis 2: ").append(metatraderDirectory2).append("\n");
            }
            
            explanation.append("\nSynchronisierte Datei: last_known_signals.csv\n");
            explanation.append("Format: Währungspaar;Letztes_Signal;Prozent\n");
            explanation.append("Beispiel:\n");
            explanation.append("  NZD/USD;SELL;55\n");
            explanation.append("  AUD/JPY;BUY;60\n");
            explanation.append("  GOLD;BUY;65      (XAUUSD → GOLD)\n");
            explanation.append("  SILBER;SELL;45   (XAGUSD → SILBER)\n\n");
            
            explanation.append("• Datei wird bei jedem Signalwechsel automatisch aktualisiert\n");
            explanation.append("• Wird in ").append(dirCount).append(" Verzeichnis").append(dirCount > 1 ? "se" : "").append(" kopiert\n");
            explanation.append("• Ermöglicht MetaTrader Expert Advisors Zugriff auf aktuelle Signale\n");
            explanation.append("• Währungsersetzung: XAUUSD→GOLD, XAGUSD→SILBER\n\n");
        } else {
            explanation.append("Status: ❌ Deaktiviert\n");
            explanation.append("• Aktivieren Sie die Synchronisation für MetaTrader EA-Integration\n");
            explanation.append("• Datei wird dann automatisch in MT5/Files-Verzeichnisse kopiert\n");
            explanation.append("• Unterstützt 1 oder 2 Ziel-Verzeichnisse\n\n");
        }
        
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
        explanation.append("E-MAIL-STATUS: ").append(emailEnabled ? "E-Mails aktiviert" : "E-Mails deaktiviert").append("\n");
        explanation.append("METATRADER-STATUS: ").append(metatraderSyncEnabled ? "Synchronisation aktiviert (" + getMetatraderDirectoryCount() + " Verzeichnis" + (getMetatraderDirectoryCount() > 1 ? "se" : "") + ")" : "Synchronisation deaktiviert");
        
        return explanation.toString();
    }
    
    /**
     * Gibt eine kurze Tooltip-Erklärung für die GUI zurück
     * ERWEITERT um Dual-Directory-Informationen
     */
    public String getSignalThresholdTooltip() {
        StringBuilder tooltip = new StringBuilder();
        
        tooltip.append(String.format(
            "Anti-Spam-Threshold: %.1f%%\n" +
            "\n" +
            "Verhindert E-Mail-Spam bei Signal-Schwankungen:\n" +
            "• Erste E-Mail pro Währungspaar: immer gesendet\n" +
            "• Folge-E-Mails: nur wenn Differenz >= %.1f%%\n" +
            "• Beispiel: 55%% → 57%% = 2%% Differenz %s %.1f%%\n" +
            "\n" +
            "Letzte gesendete Signale werden in CSV gespeichert:\n" +
            "%s/signal_changes/lastsend.csv\n",
            signalChangeThreshold,
            signalChangeThreshold,
            (2.0 >= signalChangeThreshold ? "≥" : "<"),
            signalChangeThreshold,
            dataDirectory
        ));
        
        // NEU: MetaTrader-Information im Tooltip (Dual-Directory)
        if (metatraderSyncEnabled) {
            int dirCount = getMetatraderDirectoryCount();
            tooltip.append("\nMetaTrader-Synchronisation: ✅ AKTIVIERT\n");
            tooltip.append("Konfigurierte Verzeichnisse: ").append(dirCount).append("\n");
            
            if (hasMetatraderDirectory()) {
                tooltip.append("Dir 1: ").append(metatraderDirectory).append("\n");
            }
            if (hasMetatraderDirectory2()) {
                tooltip.append("Dir 2: ").append(metatraderDirectory2).append("\n");
            }
            
            tooltip.append("Datei: last_known_signals.csv (Währungspaar;Signal;Prozent)\n");
            tooltip.append("Ersetzung: XAUUSD→GOLD, XAGUSD→SILBER");
        } else {
            tooltip.append("\nMetaTrader-Synchronisation: ❌ Deaktiviert\n");
            tooltip.append("(Kann 1 oder 2 Ziel-Verzeichnisse unterstützen)");
        }
        
        tooltip.append("\n\nEmpfehlung: 3-5% für ausgewogenes Verhalten");
        
        return tooltip.toString();
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
    
    public double getSignalChangeThreshold() { return signalChangeThreshold; }
    public void setSignalChangeThreshold(double signalChangeThreshold) { this.signalChangeThreshold = signalChangeThreshold; }
    
    // NEU: MetaTrader Getter/Setter (Dual-Directory)
    public boolean isMetatraderSyncEnabled() { return metatraderSyncEnabled; }
    public void setMetatraderSyncEnabled(boolean metatraderSyncEnabled) { this.metatraderSyncEnabled = metatraderSyncEnabled; }
    
    public String getMetatraderDirectory() { return metatraderDirectory; }
    public void setMetatraderDirectory(String metatraderDirectory) { 
        this.metatraderDirectory = metatraderDirectory != null ? metatraderDirectory.trim() : "";
    }
    
    // NEU: Getter/Setter für zweites MetaTrader-Verzeichnis
    public String getMetatraderDirectory2() { return metatraderDirectory2; }
    public void setMetatraderDirectory2(String metatraderDirectory2) { 
        this.metatraderDirectory2 = metatraderDirectory2 != null ? metatraderDirectory2.trim() : "";
    }
    
    public String getDataDirectory() { return dataDirectory; }
    
    /**
     * Gibt eine Zusammenfassung der Konfiguration zurück
     * @return String mit Konfigurations-Übersicht
     */
    public String getConfigSummary() {
        StringBuilder summary = new StringBuilder();
        
        // E-Mail-Status
        summary.append("E-Mail-Benachrichtigungen: ").append(emailEnabled ? "✅ Aktiviert" : "❌ Deaktiviert").append("\n");
        
        if (emailEnabled) {
            // Server-Konfiguration
            summary.append("Server: ").append(smtpHost).append(":").append(smtpPort).append("\n");
            summary.append("Benutzername: ").append(username.isEmpty() ? "(nicht gesetzt)" : username).append("\n");
            summary.append("Passwort: ").append(password.isEmpty() ? "(nicht gesetzt)" : "********").append("\n");
            summary.append("StartTLS: ").append(useStartTLS ? "Aktiviert" : "Deaktiviert").append("\n");
            summary.append("SSL: ").append(useSSL ? "Aktiviert" : "Deaktiviert").append("\n");
            
            // E-Mail-Adressen
            summary.append("Von: ").append(fromEmail.isEmpty() ? "(nicht gesetzt)" : fromEmail).append("\n");
            summary.append("An: ").append(toEmail.isEmpty() ? "(nicht gesetzt)" : toEmail).append("\n");
            
            // Benachrichtigungseinstellungen
            summary.append("Bei kritischen Änderungen: ").append(sendOnCriticalChanges ? "Ja" : "Nein").append("\n");
            summary.append("Bei hohen Änderungen: ").append(sendOnHighChanges ? "Ja" : "Nein").append("\n");
            summary.append("Bei allen Änderungen: ").append(sendOnAllChanges ? "Ja" : "Nein").append("\n");
            summary.append("Max. E-Mails/Stunde: ").append(maxEmailsPerHour).append("\n");
        }
        
        // Signal-Threshold
        summary.append("Signal-Threshold: ").append(String.format("%.1f", signalChangeThreshold)).append("%\n");
        
        // MetaTrader-Synchronisation
        summary.append("MetaTrader-Sync: ").append(metatraderSyncEnabled ? "✅ Aktiviert" : "❌ Deaktiviert").append("\n");
        
        if (metatraderSyncEnabled) {
            int dirCount = getMetatraderDirectoryCount();
            summary.append("MetaTrader-Verzeichnisse: ").append(dirCount).append("\n");
            
            if (hasMetatraderDirectory()) {
                boolean available1 = isMetaTraderDirectoryAvailable(metatraderDirectory);
                summary.append("  └─ Dir 1: ").append(available1 ? "✅" : "⚠️").append(" ").append(metatraderDirectory).append("\n");
            }
            
            if (hasMetatraderDirectory2()) {
                boolean available2 = isMetaTraderDirectoryAvailable(metatraderDirectory2);
                summary.append("  └─ Dir 2: ").append(available2 ? "✅" : "⚠️").append(" ").append(metatraderDirectory2).append("\n");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Prüft ob ein MetaTrader-Verzeichnis verfügbar ist (existiert und beschreibbar)
     * @param directory Zu prüfendes Verzeichnis
     * @return true wenn verfügbar, sonst false
     */
    private boolean isMetaTraderDirectoryAvailable(String directory) {
        if (directory == null || directory.trim().isEmpty()) {
            return false;
        }
        
        try {
            java.io.File dir = new java.io.File(directory);
            return dir.exists() && dir.isDirectory() && dir.canWrite();
        } catch (Exception e) {
            return false;
        }
    }
    
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