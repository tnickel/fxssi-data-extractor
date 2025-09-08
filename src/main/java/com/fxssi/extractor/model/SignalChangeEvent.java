package com.fxssi.extractor.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Datenmodell für Signalwechsel-Ereignisse
 * Speichert Informationen über Handelssignal-Änderungen für ein Währungspaar
 * 
 * @author Generated for FXSSI Signal Change Detection
 * @version 1.0
 */
public class SignalChangeEvent {
    
    private final String currencyPair;
    private final CurrencyPairData.TradingSignal fromSignal;
    private final CurrencyPairData.TradingSignal toSignal;
    private final LocalDateTime changeTime;
    private final double fromBuyPercentage;
    private final double toBuyPercentage;
    private final SignalChangeImportance importance;
    
    // Formatter für CSV-Verarbeitung
    private static final DecimalFormat CSV_DECIMAL_FORMAT;
    private static final NumberFormat CSV_PARSE_FORMAT;
    
    static {
        // Für CSV-Output: Komma als Dezimaltrennzeichen (Deutsches Format)
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
        CSV_DECIMAL_FORMAT = new DecimalFormat("#0.00", symbols);
        
        // Für CSV-Input: Flexibles Parsing (sowohl Komma als auch Punkt)
        CSV_PARSE_FORMAT = NumberFormat.getNumberInstance(Locale.GERMAN);
    }
    
    /**
     * Enum für die Wichtigkeit von Signalwechseln
     */
    public enum SignalChangeImportance {
        CRITICAL("🔴", "Kritisch"),    // BUY ↔ SELL (direkte Umkehrung)
        HIGH("🟠", "Hoch"),           // BUY/SELL → NEUTRAL oder umgekehrt  
        MEDIUM("🟡", "Mittel"),       // Kleine Änderungen
        LOW("🟢", "Niedrig");         // Sonstige
        
        private final String icon;
        private final String description;
        
        SignalChangeImportance(String icon, String description) {
            this.icon = icon;
            this.description = description;
        }
        
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
    }
    
    /**
     * Enum für die Aktualität von Signalwechseln
     */
    public enum SignalChangeActuality {
        VERY_RECENT("🔴", "Sehr aktuell", 2),      // Letzten 2 Stunden
        RECENT("🟡", "Aktuell", 24),               // Heute
        THIS_WEEK("🟢", "Diese Woche", 168),       // Letzten 7 Tage
        OLD("⚪", "Älter", Integer.MAX_VALUE);     // Älter als 7 Tage
        
        private final String icon;
        private final String description;
        private final int maxHoursOld;
        
        SignalChangeActuality(String icon, String description, int maxHoursOld) {
            this.icon = icon;
            this.description = description;
            this.maxHoursOld = maxHoursOld;
        }
        
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
        public int getMaxHoursOld() { return maxHoursOld; }
    }
    
    /**
     * Vollständiger Konstruktor
     */
    public SignalChangeEvent(String currencyPair, 
                           CurrencyPairData.TradingSignal fromSignal,
                           CurrencyPairData.TradingSignal toSignal,
                           LocalDateTime changeTime,
                           double fromBuyPercentage,
                           double toBuyPercentage) {
        this.currencyPair = currencyPair;
        this.fromSignal = fromSignal;
        this.toSignal = toSignal;
        this.changeTime = changeTime;
        this.fromBuyPercentage = fromBuyPercentage;
        this.toBuyPercentage = toBuyPercentage;
        this.importance = calculateImportance(fromSignal, toSignal);
    }
    
    /**
     * Konstruktor aus CSV-Zeile - REPARIERT für deutsches Dezimalformat
     */
    public static SignalChangeEvent fromCsvLine(String csvLine) {
        String[] parts = csvLine.split(";");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Ungültiges Signal-Change CSV-Format: " + csvLine);
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime changeTime = LocalDateTime.parse(parts[0], formatter);
            String currencyPair = parts[1];
            CurrencyPairData.TradingSignal fromSignal = CurrencyPairData.TradingSignal.valueOf(parts[2]);
            CurrencyPairData.TradingSignal toSignal = CurrencyPairData.TradingSignal.valueOf(parts[3]);
            
            // REPARIERT: Flexibles Parsing für Dezimalzahlen (Komma und Punkt)
            double fromBuyPercentage = parseDecimalValue(parts[4]);
            double toBuyPercentage = parseDecimalValue(parts[5]);
            
            return new SignalChangeEvent(currencyPair, fromSignal, toSignal, changeTime, fromBuyPercentage, toBuyPercentage);
        } catch (Exception e) {
            throw new IllegalArgumentException("Fehler beim Parsen der Signal-Change CSV-Zeile: " + csvLine, e);
        }
    }
    
    /**
     * Hilfsmethode für flexibles Parsen von Dezimalwerten (Komma und Punkt)
     */
    private static double parseDecimalValue(String value) throws Exception {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        
        value = value.trim();
        
        try {
            // Versuche zuerst deutsches Format (Komma)
            if (value.contains(",")) {
                return CSV_PARSE_FORMAT.parse(value).doubleValue();
            } else {
                // Fallback: Standard-Format (Punkt)
                return Double.parseDouble(value);
            }
        } catch (Exception e) {
            // Letzter Versuch: Komma durch Punkt ersetzen
            String normalizedValue = value.replace(",", ".");
            return Double.parseDouble(normalizedValue);
        }
    }
    
    // Getter
    public String getCurrencyPair() { return currencyPair; }
    public CurrencyPairData.TradingSignal getFromSignal() { return fromSignal; }
    public CurrencyPairData.TradingSignal getToSignal() { return toSignal; }
    public LocalDateTime getChangeTime() { return changeTime; }
    public double getFromBuyPercentage() { return fromBuyPercentage; }
    public double getToBuyPercentage() { return toBuyPercentage; }
    public SignalChangeImportance getImportance() { return importance; }
    
    /**
     * Berechnet die Aktualität des Signalwechsels
     */
    public SignalChangeActuality getActuality() {
        LocalDateTime now = LocalDateTime.now();
        long hoursOld = ChronoUnit.HOURS.between(changeTime, now);
        
        if (hoursOld <= SignalChangeActuality.VERY_RECENT.getMaxHoursOld()) {
            return SignalChangeActuality.VERY_RECENT;
        } else if (hoursOld <= SignalChangeActuality.RECENT.getMaxHoursOld()) {
            return SignalChangeActuality.RECENT;
        } else if (hoursOld <= SignalChangeActuality.THIS_WEEK.getMaxHoursOld()) {
            return SignalChangeActuality.THIS_WEEK;
        } else {
            return SignalChangeActuality.OLD;
        }
    }
    
    /**
     * Gibt das kombinierte Icon für Wichtigkeit + Aktualität zurück
     */
    public String getCombinedIcon() {
        SignalChangeActuality actuality = getActuality();
        
        // Verwende Aktualitäts-Icon als Basis
        String baseIcon = actuality.getIcon();
        
        // Füge Wichtigkeits-Indikator hinzu für kritische Wechsel
        if (importance == SignalChangeImportance.CRITICAL) {
            return "🚨" + baseIcon; // Alarm-Symbol für kritische Wechsel
        } else if (importance == SignalChangeImportance.HIGH) {
            return "⚠️" + baseIcon; // Warnung für hohe Wichtigkeit
        }
        
        return baseIcon;
    }
    
    /**
     * Gibt eine benutzerfreundliche Beschreibung des Wechsels zurück
     */
    public String getChangeDescription() {
        return String.format("%s → %s", fromSignal.getDescription(), toSignal.getDescription());
    }
    
    /**
     * Gibt eine detaillierte Beschreibung mit Prozentangaben zurück
     */
    public String getDetailedDescription() {
        return String.format("%s → %s (%.1f%% → %.1f%% Buy)", 
            fromSignal.getDescription(), 
            toSignal.getDescription(),
            fromBuyPercentage,
            toBuyPercentage);
    }
    
    /**
     * Gibt eine kurze Timeline-Beschreibung zurück
     */
    public String getTimelineDescription() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM");
        
        SignalChangeActuality actuality = getActuality();
        String timeStr;
        
        if (actuality == SignalChangeActuality.VERY_RECENT || actuality == SignalChangeActuality.RECENT) {
            timeStr = "Heute " + changeTime.format(timeFormatter);
        } else if (actuality == SignalChangeActuality.THIS_WEEK) {
            timeStr = getGermanDayOfWeek() + " " + changeTime.format(timeFormatter);
        } else {
            timeStr = changeTime.format(dateFormatter) + " " + changeTime.format(timeFormatter);
        }
        
        return String.format("%s %s: %s", 
            getActuality().getIcon(),
            timeStr,
            getChangeDescription());
    }
    
    /**
     * Prüft ob dieser Wechsel ein direkter Umkehr-Wechsel ist (BUY ↔ SELL)
     */
    public boolean isDirectReversal() {
        return (fromSignal == CurrencyPairData.TradingSignal.BUY && toSignal == CurrencyPairData.TradingSignal.SELL) ||
               (fromSignal == CurrencyPairData.TradingSignal.SELL && toSignal == CurrencyPairData.TradingSignal.BUY);
    }
    
    /**
     * Prüft ob dieser Wechsel in den letzten X Stunden aufgetreten ist
     */
    public boolean isWithinHours(int hours) {
        LocalDateTime now = LocalDateTime.now();
        long hoursOld = ChronoUnit.HOURS.between(changeTime, now);
        return hoursOld <= hours;
    }
    
    /**
     * Formatiert als CSV-Zeile für Speicherung - KONSISTENT mit deutschem Format
     */
    public String toCsvLine() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("%s;%s;%s;%s;%s;%s",
            changeTime.format(formatter),
            currencyPair,
            fromSignal.name(),
            toSignal.name(),
            CSV_DECIMAL_FORMAT.format(fromBuyPercentage),
            CSV_DECIMAL_FORMAT.format(toBuyPercentage));
    }
    
    /**
     * CSV-Header für Dateiexport
     */
    public static String getCsvHeader() {
        return "Zeitstempel;Währungspaar;Von_Signal;Zu_Signal;Von_Buy_Prozent;Zu_Buy_Prozent";
    }
    
    /**
     * Berechnet die Wichtigkeit des Signalwechsels
     */
    private SignalChangeImportance calculateImportance(CurrencyPairData.TradingSignal from, CurrencyPairData.TradingSignal to) {
        // Direkte Umkehrung (BUY ↔ SELL) = CRITICAL
        if ((from == CurrencyPairData.TradingSignal.BUY && to == CurrencyPairData.TradingSignal.SELL) ||
            (from == CurrencyPairData.TradingSignal.SELL && to == CurrencyPairData.TradingSignal.BUY)) {
            return SignalChangeImportance.CRITICAL;
        }
        
        // Von/Zu NEUTRAL = HIGH
        if (from == CurrencyPairData.TradingSignal.NEUTRAL || to == CurrencyPairData.TradingSignal.NEUTRAL) {
            return SignalChangeImportance.HIGH;
        }
        
        // Von/Zu UNKNOWN = MEDIUM
        if (from == CurrencyPairData.TradingSignal.UNKNOWN || to == CurrencyPairData.TradingSignal.UNKNOWN) {
            return SignalChangeImportance.MEDIUM;
        }
        
        // Alles andere = LOW
        return SignalChangeImportance.LOW;
    }
    
    /**
     * Hilfsmethode für deutsche Wochentage
     */
    private String getGermanDayOfWeek() {
        switch (changeTime.getDayOfWeek()) {
            case MONDAY: return "Mo";
            case TUESDAY: return "Di";
            case WEDNESDAY: return "Mi";
            case THURSDAY: return "Do";
            case FRIDAY: return "Fr";
            case SATURDAY: return "Sa";
            case SUNDAY: return "So";
            default: return "??";
        }
    }
    
    @Override
    public String toString() {
        return String.format("SignalChangeEvent{pair='%s', %s, time=%s, importance=%s}",
            currencyPair, getChangeDescription(), changeTime, importance);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SignalChangeEvent that = (SignalChangeEvent) obj;
        return currencyPair.equals(that.currencyPair) &&
               fromSignal == that.fromSignal &&
               toSignal == that.toSignal &&
               changeTime.equals(that.changeTime);
    }
    
    @Override
    public int hashCode() {
        int result = currencyPair.hashCode();
        result = 31 * result + fromSignal.hashCode();
        result = 31 * result + toSignal.hashCode();
        result = 31 * result + changeTime.hashCode();
        return result;
    }
}