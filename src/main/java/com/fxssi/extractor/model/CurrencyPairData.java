package com.fxssi.extractor.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Datenmodell-Klasse für Währungspaar-Sentiment-Daten von FXSSI
 * Enthält alle relevanten Informationen eines Währungspaares
 * 
 * @author Generated for FXSSI Data Extraction
 * @version 1.0
 */
public class CurrencyPairData {
    
    private String currencyPair;
    private double buyPercentage;
    private double sellPercentage;
    private TradingSignal tradingSignal;
    private LocalDateTime timestamp;
    
    /**
     * Enum für Handelssignale
     */
    public enum TradingSignal {
        BUY("Kaufen"),
        SELL("Verkaufen"),
        NEUTRAL("Seitwärts"),
        UNKNOWN("Unbekannt");
        
        private final String description;
        
        TradingSignal(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Standardkonstruktor
     */
    public CurrencyPairData() {
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Vollständiger Konstruktor
     */
    public CurrencyPairData(String currencyPair, double buyPercentage, double sellPercentage, TradingSignal tradingSignal) {
        this.currencyPair = currencyPair;
        this.buyPercentage = buyPercentage;
        this.sellPercentage = sellPercentage;
        this.tradingSignal = tradingSignal;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Konstruktor mit Zeitstempel
     */
    public CurrencyPairData(String currencyPair, double buyPercentage, double sellPercentage, 
                           TradingSignal tradingSignal, LocalDateTime timestamp) {
        this.currencyPair = currencyPair;
        this.buyPercentage = buyPercentage;
        this.sellPercentage = sellPercentage;
        this.tradingSignal = tradingSignal;
        this.timestamp = timestamp;
    }
    
    // Getter und Setter
    
    public String getCurrencyPair() {
        return currencyPair;
    }
    
    public void setCurrencyPair(String currencyPair) {
        this.currencyPair = currencyPair;
    }
    
    public double getBuyPercentage() {
        return buyPercentage;
    }
    
    public void setBuyPercentage(double buyPercentage) {
        this.buyPercentage = buyPercentage;
    }
    
    public double getSellPercentage() {
        return sellPercentage;
    }
    
    public void setSellPercentage(double sellPercentage) {
        this.sellPercentage = sellPercentage;
    }
    
    public TradingSignal getTradingSignal() {
        return tradingSignal;
    }
    
    public void setTradingSignal(TradingSignal tradingSignal) {
        this.tradingSignal = tradingSignal;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Bestimmt das Handelssignal basierend auf Buy-Percentage
     * Buy > 60% = SELL Signal (gegen die Masse)
     * Buy < 40% = BUY Signal (gegen die Masse)
     * 40% <= Buy <= 60% = NEUTRAL
     */
    public void calculateTradingSignal() {
        if (buyPercentage > 60.0) {
            this.tradingSignal = TradingSignal.SELL;
        } else if (buyPercentage < 40.0) {
            this.tradingSignal = TradingSignal.BUY;
        } else {
            this.tradingSignal = TradingSignal.NEUTRAL;
        }
    }
    
    /**
     * Überprüft die Konsistenz der Daten
     * Buy% + Sell% sollte ungefähr 100% ergeben
     */
    public boolean isDataConsistent() {
        double total = buyPercentage + sellPercentage;
        return total >= 99.0 && total <= 101.0; // Toleranz von 1%
    }
    
    /**
     * Formatiert die Daten als CSV-Zeile
     */
    public String toCsvLine() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("%s;%s;%.2f;%.2f;%s", 
            timestamp.format(formatter),
            currencyPair,
            buyPercentage,
            sellPercentage,
            tradingSignal.name()
        );
    }
    
    /**
     * Erstellt ein CurrencyPairData-Objekt aus einer CSV-Zeile
     */
    public static CurrencyPairData fromCsvLine(String csvLine) {
        String[] parts = csvLine.split(";");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Ungültiges CSV-Format: " + csvLine);
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime timestamp = LocalDateTime.parse(parts[0], formatter);
            String currencyPair = parts[1];
            double buyPercentage = Double.parseDouble(parts[2]);
            double sellPercentage = Double.parseDouble(parts[3]);
            TradingSignal signal = TradingSignal.valueOf(parts[4]);
            
            return new CurrencyPairData(currencyPair, buyPercentage, sellPercentage, signal, timestamp);
        } catch (Exception e) {
            throw new IllegalArgumentException("Fehler beim Parsen der CSV-Zeile: " + csvLine, e);
        }
    }
    
    /**
     * CSV-Header für Dateiexport
     */
    public static String getCsvHeader() {
        return "Zeitstempel;Währungspaar;Buy_Prozent;Sell_Prozent;Handelssignal";
    }
    
    @Override
    public String toString() {
        return String.format("CurrencyPairData{currencyPair='%s', buyPercentage=%.2f, sellPercentage=%.2f, tradingSignal=%s, timestamp=%s}",
                currencyPair, buyPercentage, sellPercentage, tradingSignal, timestamp);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CurrencyPairData that = (CurrencyPairData) obj;
        return Double.compare(that.buyPercentage, buyPercentage) == 0 &&
               Double.compare(that.sellPercentage, sellPercentage) == 0 &&
               currencyPair.equals(that.currencyPair) &&
               tradingSignal == that.tradingSignal &&
               timestamp.equals(that.timestamp);
    }
    
    @Override
    public int hashCode() {
        int result = currencyPair.hashCode();
        result = 31 * result + Double.hashCode(buyPercentage);
        result = 31 * result + Double.hashCode(sellPercentage);
        result = 31 * result + tradingSignal.hashCode();
        result = 31 * result + timestamp.hashCode();
        return result;
    }
}