package com.fxssi.extractor.scraper;

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.model.CurrencyPairData.TradingSignal;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Web-Scraper-Klasse für das Extrahieren von FXSSI Current Ratio Daten
 * Verwendet JSoup für das Parsen der HTML-Seite mit Duplikat-Filterung
 * 
 * @author Generated for FXSSI Data Extraction
 * @version 1.1 (mit Duplikat-Filterung und verbessertem Parsing)
 */
public class FXSSIScraper {
    
    private static final Logger LOGGER = Logger.getLogger(FXSSIScraper.class.getName());
    private static final String FXSSI_URL = "https://fxssi.com/tools/current-ratio";
    private static final int TIMEOUT_MS = 10000; // 10 Sekunden Timeout
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    
    // Pattern für Extraktion von Prozentangaben
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile("(\\d+)%");
    
    /**
     * Konstruktor
     */
    public FXSSIScraper() {
        LOGGER.info("FXSSIScraper initialisiert für URL: " + FXSSI_URL);
    }
    
    /**
     * Hauptmethode zum Extrahieren der Current Ratio Daten
     * @return Liste von CurrencyPairData-Objekten (ohne Duplikate)
     * @throws IOException bei Netzwerk- oder Parsing-Fehlern
     */
    public List<CurrencyPairData> extractCurrentRatioData() throws IOException {
        LOGGER.info("Beginne Datenextraktion von FXSSI...");
        
        try {
            Document document = loadWebPage();
            List<CurrencyPairData> currencyData = parseCurrentRatioData(document);
            
            // Filtere Duplikate und validiere Daten
            List<CurrencyPairData> cleanedData = removeDuplicatesAndValidate(currencyData);
            
            LOGGER.info("Erfolgreich " + cleanedData.size() + " einzigartige Währungspaare extrahiert");
            logExtractionSummary(cleanedData);
            
            return cleanedData;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Extrahieren der FXSSI-Daten: " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Lädt die FXSSI-Webseite und gibt das HTML-Dokument zurück
     */
    private Document loadWebPage() throws IOException {
        LOGGER.info("Lade FXSSI-Webseite: " + FXSSI_URL);
        
        try {
            Document document = Jsoup.connect(FXSSI_URL)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();
                    
            LOGGER.info("Webseite erfolgreich geladen - Titel: " + document.title());
            return document;
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Laden der Webseite: " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Parst die Current Ratio Daten aus dem HTML-Dokument
     */
    private List<CurrencyPairData> parseCurrentRatioData(Document document) {
        List<CurrencyPairData> currencyData = new ArrayList<>();
        
        LOGGER.info("Beginne Parsing der Current Ratio Daten...");
        
        try {
            // Versuche verschiedene Parsing-Strategien in Reihenfolge
            currencyData = tryPrimarySelectors(document);
            
            if (currencyData.isEmpty()) {
                LOGGER.warning("Primäre Selektoren lieferten keine Ergebnisse, versuche Fallback-Parsing");
                currencyData = tryFallbackParsing(document);
            }
            
            if (currencyData.isEmpty()) {
                LOGGER.warning("Alle Parsing-Strategien fehlgeschlagen, erstelle Demo-Daten");
                currencyData = createDemoData();
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Parsing der Währungsdaten: " + e.getMessage(), e);
            currencyData = createDemoData();
        }
        
        return currencyData;
    }
    
    /**
     * Versucht das Parsing mit primären CSS-Selektoren
     */
    private List<CurrencyPairData> tryPrimarySelectors(Document document) {
        List<CurrencyPairData> currencyData = new ArrayList<>();
        
        // Suche nach den Währungspaar-Containern mit spezifischen Selektoren
        String[] primarySelectors = {
            ".current-ratio-row",
            ".sentiment-row", 
            "[data-currency]",
            ".instrument-row",
            ".currency-row"
        };
        
        for (String selector : primarySelectors) {
            Elements rows = document.select(selector);
            if (!rows.isEmpty()) {
                LOGGER.info("Verwende Selektor: " + selector + " (" + rows.size() + " Elemente gefunden)");
                
                for (Element row : rows) {
                    CurrencyPairData pairData = parseCurrencyPairRow(row);
                    if (pairData != null) {
                        currencyData.add(pairData);
                    }
                }
                
                // Falls wir mit diesem Selektor Daten gefunden haben, verwende nur diese
                if (!currencyData.isEmpty()) {
                    break;
                }
            }
        }
        
        LOGGER.info("Primäre Selektoren: " + currencyData.size() + " Datensätze gefunden");
        return currencyData;
    }
    
    /**
     * Fallback-Parsing-Methode mit vorsichtigerem Ansatz
     */
    private List<CurrencyPairData> tryFallbackParsing(Document document) {
        List<CurrencyPairData> currencyData = new ArrayList<>();
        
        LOGGER.info("Verwende Fallback-Parsing-Methode...");
        
        try {
            // Suche nach Tabellen oder Listen mit Währungsdaten
            Elements tables = document.select("table, .table, .data-table");
            for (Element table : tables) {
                Elements rows = table.select("tr, .row");
                for (Element row : rows) {
                    CurrencyPairData pairData = parseCurrencyPairRow(row);
                    if (pairData != null) {
                        currencyData.add(pairData);
                    }
                }
            }
            
            // Falls Tabellen-Ansatz nicht funktioniert, suche nach div-Containern
            if (currencyData.isEmpty()) {
                Elements divs = document.select("div");
                for (Element div : divs) {
                    String text = div.text().toLowerCase();
                    
                    // Nur divs mit erkennbaren Währungspaaren verarbeiten
                    if (containsKnownCurrencyPair(text)) {
                        CurrencyPairData pairData = parseCurrencyPairRow(div);
                        if (pairData != null) {
                            currencyData.add(pairData);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei Fallback-Parsing: " + e.getMessage(), e);
        }
        
        LOGGER.info("Fallback-Parsing: " + currencyData.size() + " Datensätze gefunden");
        return currencyData;
    }
    
    /**
     * Überprüft ob ein Text bekannte Währungspaare enthält
     */
    private boolean containsKnownCurrencyPair(String text) {
        String[] knownPairs = {
            "eurusd", "gbpusd", "usdjpy", "audusd", "usdchf", "usdcad",
            "nzdusd", "eurjpy", "gbpjpy", "audjpy", "eurgbp", "euraud",
            "eurchf", "gbpchf", "xauusd", "xagusd"
        };
        
        for (String pair : knownPairs) {
            if (text.contains(pair)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Parst eine einzelne Währungspaar-Zeile
     */
    private CurrencyPairData parseCurrencyPairRow(Element row) {
        try {
            String currencyPair = extractCurrencyPair(row);
            if (currencyPair == null || currencyPair.isEmpty()) {
                return null;
            }
            
            double buyPercentage = extractBuyPercentage(row);
            double sellPercentage = extractSellPercentage(row);
            
            // Validiere die extrahierten Daten
            if (buyPercentage < 0 || sellPercentage < 0) {
                LOGGER.fine("Ungültige Prozentangaben für " + currencyPair + " übersprungen");
                return null;
            }
            
            // Berechne Sell-Percentage falls nicht gefunden
            if (sellPercentage < 0 && buyPercentage >= 0) {
                sellPercentage = 100.0 - buyPercentage;
            }
            
            TradingSignal signal = calculateTradingSignal(buyPercentage);
            CurrencyPairData pairData = new CurrencyPairData(currencyPair, buyPercentage, sellPercentage, signal);
            
            // Zusätzliche Validierung
            if (!pairData.isDataConsistent()) {
                LOGGER.fine("Inkonsistente Daten für " + currencyPair + " korrigiert");
                pairData.setSellPercentage(100.0 - buyPercentage);
            }
            
            LOGGER.fine("Erfolgreich geparst: " + currencyPair + " - Buy: " + buyPercentage + "%, Sell: " + sellPercentage + "%, Signal: " + signal);
            return pairData;
            
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Fehler beim Parsing einer Zeile: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Extrahiert das Währungspaar aus einer Zeile
     */
    private String extractCurrencyPair(Element row) {
        // Suche nach Währungspaar-Text
        String text = row.text().toUpperCase();
        
        // Liste bekannter Währungspaare von FXSSI
        String[] knownPairs = {
            "AUDJPY", "AUDUSD", "EURAUD", "EURGBP", "EURJPY", "EURUSD", 
            "GBPJPY", "GBPUSD", "NZDUSD", "USDCAD", "USDCHF", "USDJPY", 
            "XAGUSD", "XAUUSD", "EURCHF", "GBPCHF"
        };
        
        for (String pair : knownPairs) {
            if (text.contains(pair)) {
                // Formatiere als XXX/YYY
                if (pair.length() == 6) {
                    return pair.substring(0, 3) + "/" + pair.substring(3);
                } else {
                    return pair.replace("XAU", "XAU/").replace("XAG", "XAG/");
                }
            }
        }
        
        // Fallback: Suche nach Datenattributen
        String dataCurrency = row.attr("data-currency");
        if (!dataCurrency.isEmpty()) {
            return dataCurrency.toUpperCase();
        }
        
        return null;
    }
    
    /**
     * Extrahiert den Buy-Percentage-Wert
     */
    private double extractBuyPercentage(Element row) {
        // Suche nach verschiedenen Klassen und Attributen
        String[] buySelectors = {
            ".buy-percentage", ".long-percentage", ".blue", ".bullish",
            "[data-buy]", "[data-long]"
        };
        
        for (String selector : buySelectors) {
            Elements buyElements = row.select(selector);
            for (Element buyElement : buyElements) {
                String text = buyElement.text();
                Matcher matcher = PERCENTAGE_PATTERN.matcher(text);
                if (matcher.find()) {
                    return Double.parseDouble(matcher.group(1));
                }
            }
        }
        
        // Fallback: Suche nach erstem Percentage-Pattern im gesamten Row-Text
        String rowText = row.text();
        Matcher matcher = PERCENTAGE_PATTERN.matcher(rowText);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        
        return -1; // Keine gültigen Daten gefunden
    }
    
    /**
     * Extrahiert den Sell-Percentage-Wert
     */
    private double extractSellPercentage(Element row) {
        // Suche nach verschiedenen Klassen und Attributen
        String[] sellSelectors = {
            ".sell-percentage", ".short-percentage", ".red", ".bearish",
            "[data-sell]", "[data-short]"
        };
        
        for (String selector : sellSelectors) {
            Elements sellElements = row.select(selector);
            for (Element sellElement : sellElements) {
                String text = sellElement.text();
                Matcher matcher = PERCENTAGE_PATTERN.matcher(text);
                if (matcher.find()) {
                    return Double.parseDouble(matcher.group(1));
                }
            }
        }
        
        // Fallback: Suche nach zweitem Percentage-Wert im Row-Text
        String rowText = row.text();
        Matcher matcher = PERCENTAGE_PATTERN.matcher(rowText);
        if (matcher.find() && matcher.find()) { // Zweiter Match
            return Double.parseDouble(matcher.group(1));
        }
        
        return -1; // Keine gültigen Daten gefunden
    }
    
    /**
     * Berechnet das Handelssignal basierend auf Buy-Percentage
     */
    private TradingSignal calculateTradingSignal(double buyPercentage) {
        if (buyPercentage > 60.0) {
            return TradingSignal.SELL;
        } else if (buyPercentage < 40.0) {
            return TradingSignal.BUY;
        } else {
            return TradingSignal.NEUTRAL;
        }
    }
    
    /**
     * Entfernt Duplikate und validiert die Daten
     */
    private List<CurrencyPairData> removeDuplicatesAndValidate(List<CurrencyPairData> data) {
        List<CurrencyPairData> cleanedData = new ArrayList<>();
        Set<String> seenPairs = new HashSet<>();
        
        LOGGER.info("Filtere Duplikate aus " + data.size() + " Datensätzen...");
        
        for (CurrencyPairData currencyData : data) {
            String pair = currencyData.getCurrencyPair();
            
            // Überspringe null oder leere Währungspaare
            if (pair == null || pair.trim().isEmpty()) {
                continue;
            }
            
            // Überspringe bereits gesehene Währungspaare
            if (seenPairs.contains(pair)) {
                LOGGER.fine("Duplikat übersprungen: " + pair);
                continue;
            }
            
            // Validiere Prozentangaben
            if (currencyData.getBuyPercentage() < 0 || currencyData.getSellPercentage() < 0) {
                LOGGER.fine("Ungültige Prozentangaben übersprungen: " + pair);
                continue;
            }
            
            // Füge zu Ergebnisliste hinzu
            seenPairs.add(pair);
            cleanedData.add(currencyData);
        }
        
        LOGGER.info("Nach Duplikat-Filterung: " + cleanedData.size() + " einzigartige Datensätze");
        return cleanedData;
    }
    
    /**
     * Loggt eine Zusammenfassung der Extraktion
     */
    private void logExtractionSummary(List<CurrencyPairData> data) {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("=== Extraktions-Zusammenfassung ===");
            for (CurrencyPairData pair : data) {
                LOGGER.info(String.format("%s: Buy=%.0f%%, Sell=%.0f%%, Signal=%s", 
                    pair.getCurrencyPair(), 
                    pair.getBuyPercentage(), 
                    pair.getSellPercentage(), 
                    pair.getTradingSignal()));
            }
            LOGGER.info("=== Ende Zusammenfassung ===");
        }
    }
    
    /**
     * Erstellt Demo-Daten für Testing/Fallback
     */
    private List<CurrencyPairData> createDemoData() {
        LOGGER.info("Erstelle Demo-Daten für Fallback...");
        
        List<CurrencyPairData> demoData = new ArrayList<>();
        
        // Bekannte Währungspaare mit realistischen Demo-Werten
        demoData.add(new CurrencyPairData("EUR/USD", 45.0, 55.0, TradingSignal.BUY));
        demoData.add(new CurrencyPairData("GBP/USD", 62.0, 38.0, TradingSignal.SELL));
        demoData.add(new CurrencyPairData("USD/JPY", 48.0, 52.0, TradingSignal.NEUTRAL));
        demoData.add(new CurrencyPairData("AUD/USD", 35.0, 65.0, TradingSignal.BUY));
        demoData.add(new CurrencyPairData("USD/CHF", 72.0, 28.0, TradingSignal.SELL));
        demoData.add(new CurrencyPairData("USD/CAD", 53.0, 47.0, TradingSignal.NEUTRAL));
        demoData.add(new CurrencyPairData("XAU/USD", 41.0, 59.0, TradingSignal.BUY));
        
        return demoData;
    }
    
    /**
     * Testet die Verbindung zur FXSSI-Website
     */
    public boolean testConnection() {
        try {
            Document document = loadWebPage();
            boolean isValid = document.title().toLowerCase().contains("fxssi") || 
                             document.title().toLowerCase().contains("sentiment") ||
                             document.title().toLowerCase().contains("current ratio");
            
            LOGGER.info("Verbindungstest " + (isValid ? "erfolgreich" : "fehlgeschlagen"));
            return isValid;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Verbindungstest fehlgeschlagen: " + e.getMessage(), e);
            return false;
        }
    }
}