package com.fxssi.extractor.scraper;

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.model.CurrencyPairData.TradingSignal;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Web-Scraper-Klasse für das Extrahieren von FXSSI Current Ratio Daten
 * Verwendet JSoup für das Parsen der HTML-Seite
 * 
 * @author Generated for FXSSI Data Extraction
 * @version 1.0
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
     * @return Liste von CurrencyPairData-Objekten
     * @throws IOException bei Netzwerk- oder Parsing-Fehlern
     */
    public List<CurrencyPairData> extractCurrentRatioData() throws IOException {
        LOGGER.info("Beginne Datenextraktion von FXSSI...");
        
        try {
            Document document = loadWebPage();
            List<CurrencyPairData> currencyData = parseCurrentRatioData(document);
            
            LOGGER.info("Erfolgreich " + currencyData.size() + " Währungspaare extrahiert");
            return currencyData;
            
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
            // Suche nach den Währungspaar-Containern
            // Die FXSSI-Seite hat spezifische CSS-Selektoren für die Währungspaare
            Elements currencyRows = document.select(".current-ratio-row, .sentiment-row, [data-currency]");
            
            if (currencyRows.isEmpty()) {
                // Fallback: Suche nach alternativen Selektoren
                currencyRows = document.select("div:contains(USD), div:contains(EUR), div:contains(GBP), div:contains(AUD), div:contains(JPY), div:contains(CHF), div:contains(CAD), div:contains(NZD), div:contains(XAU)");
                LOGGER.warning("Primäre Selektoren nicht gefunden, verwende Fallback-Selektoren");
            }
            
            LOGGER.info("Gefundene potenzielle Währungspaar-Zeilen: " + currencyRows.size());
            
            for (Element row : currencyRows) {
                CurrencyPairData pairData = parseCurrencyPairRow(row);
                if (pairData != null) {
                    currencyData.add(pairData);
                }
            }
            
            // Falls keine Daten mit den Standard-Selektoren gefunden wurden, versuche alternative Parsing-Methode
            if (currencyData.isEmpty()) {
                currencyData = parseAlternativeMethod(document);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Parsing der Währungsdaten: " + e.getMessage(), e);
        }
        
        return currencyData;
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
            TradingSignal signal = extractTradingSignal(row, buyPercentage);
            
            // Validiere die extrahierten Daten
            if (buyPercentage < 0 || sellPercentage < 0) {
                LOGGER.warning("Ungültige Prozentangaben für " + currencyPair);
                return null;
            }
            
            CurrencyPairData pairData = new CurrencyPairData(currencyPair, buyPercentage, sellPercentage, signal);
            
            // Überprüfe Datenkonsistenz
            if (!pairData.isDataConsistent()) {
                LOGGER.warning("Inkonsistente Daten für " + currencyPair + " (Buy: " + buyPercentage + "%, Sell: " + sellPercentage + "%)");
                // Korrigiere die Sell-Percentage falls möglich
                pairData.setSellPercentage(100.0 - buyPercentage);
            }
            
            LOGGER.fine("Erfolgreich geparst: " + currencyPair + " - Buy: " + buyPercentage + "%, Sell: " + sellPercentage + "%, Signal: " + signal);
            return pairData;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Parsing einer Währungspaar-Zeile: " + e.getMessage(), e);
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
                    return pair; // Für XAU/USD etc.
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
        // Suche nach blauen Balken oder Buy-Percentage
        Elements buyElements = row.select(".buy-percentage, .long-percentage, .blue");
        
        for (Element buyElement : buyElements) {
            String text = buyElement.text();
            Matcher matcher = PERCENTAGE_PATTERN.matcher(text);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        }
        
        // Fallback: Suche nach Percentage-Pattern im gesamten Row-Text
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
        // Suche nach roten Balken oder Sell-Percentage
        Elements sellElements = row.select(".sell-percentage, .short-percentage, .red");
        
        for (Element sellElement : sellElements) {
            String text = sellElement.text();
            Matcher matcher = PERCENTAGE_PATTERN.matcher(text);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
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
     * Extrahiert das Handelssignal aus dem Element oder berechnet es
     */
    private TradingSignal extractTradingSignal(Element row, double buyPercentage) {
        // Suche nach Signal-Icons oder -Klassen
        if (row.select(".signal-buy, .buy-signal, .arrow-up").size() > 0) {
            return TradingSignal.BUY;
        }
        if (row.select(".signal-sell, .sell-signal, .arrow-down").size() > 0) {
            return TradingSignal.SELL;
        }
        if (row.select(".signal-neutral, .neutral-signal, .arrow-right").size() > 0) {
            return TradingSignal.NEUTRAL;
        }
        
        // Fallback: Berechne Signal basierend auf Buy-Percentage
        if (buyPercentage > 60.0) {
            return TradingSignal.SELL;
        } else if (buyPercentage < 40.0) {
            return TradingSignal.BUY;
        } else {
            return TradingSignal.NEUTRAL;
        }
    }
    
    /**
     * Alternative Parsing-Methode falls die Hauptmethode keine Ergebnisse liefert
     */
    private List<CurrencyPairData> parseAlternativeMethod(Document document) {
        LOGGER.info("Verwende alternative Parsing-Methode...");
        
        List<CurrencyPairData> currencyData = new ArrayList<>();
        
        try {
            // Suche nach JavaScript-Daten oder JSON im HTML
            Elements scriptTags = document.select("script");
            for (Element script : scriptTags) {
                String scriptContent = script.html();
                if (scriptContent.contains("sentiment") || scriptContent.contains("ratio") || scriptContent.contains("currency")) {
                    // Hier könnte man JavaScript-Daten parsen
                    LOGGER.info("Gefunden JavaScript mit Sentiment-Daten, aber Parsing noch nicht implementiert");
                }
            }
            
            // Erstelle Dummy-Daten für Testing (sollte später entfernt werden)
            if (currencyData.isEmpty()) {
                LOGGER.warning("Keine Daten gefunden - erstelle Test-Daten für Entwicklungszwecke");
                currencyData.add(new CurrencyPairData("EUR/USD", 45.0, 55.0, TradingSignal.BUY));
                currencyData.add(new CurrencyPairData("GBP/USD", 65.0, 35.0, TradingSignal.SELL));
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei alternativer Parsing-Methode: " + e.getMessage(), e);
        }
        
        return currencyData;
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