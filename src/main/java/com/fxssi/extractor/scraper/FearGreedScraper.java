package com.fxssi.extractor.scraper;

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.model.CurrencyPairData.TradingSignal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Web-Scraper-Klasse für das Extrahieren des CNN Fear & Greed Index
 * Verwendet die CNN API für JSON-basierte Datenabfrage
 * 
 * Der Fear & Greed Index misst die Marktstimmung von 0 (Extreme Fear) bis 100 (Extreme Greed)
 * 
 * Signal-Logik (Contrarian-Ansatz):
 * - Index 0-44 (Fear/Extreme Fear) → BUY Signal (Long)
 * - Index 45-55 (Neutral) → NEUTRAL Signal (Seitwärts)
 * - Index 56-100 (Greed/Extreme Greed) → SELL Signal (Short)
 * 
 * @author Generated for FXSSI Data Extraction
 * @version 1.0
 */
public class FearGreedScraper {
    
    private static final Logger LOGGER = Logger.getLogger(FearGreedScraper.class.getName());
    
    // CNN Fear & Greed API Endpoint
    private static final String CNN_API_URL = "https://production.dataviz.cnn.io/index/fearandgreed/graphdata";
    
    // Timeout für HTTP-Verbindung in Millisekunden
    private static final int TIMEOUT_MS = 15000;
    
    // User-Agent für HTTP-Requests
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    
    // Symbol für die Daten (wird als BTC/USD angezeigt)
    private static final String SYMBOL = "BTC/USD";
    
    // Regex-Pattern für JSON-Extraktion
    private static final Pattern SCORE_PATTERN = Pattern.compile("\"score\"\\s*:\\s*([0-9.]+)");
    private static final Pattern RATING_PATTERN = Pattern.compile("\"rating\"\\s*:\\s*\"([^\"]+)\"");
    
    // Schwellenwerte für Signal-Berechnung
    private static final double FEAR_THRESHOLD = 45.0;    // Unter 45 = Fear → BUY
    private static final double GREED_THRESHOLD = 55.0;   // Über 55 = Greed → SELL
    
    // Datenverzeichnis für Debug-Ausgaben
    private final String dataDirectory;
    
    /**
     * Konstruktor mit Standard-Datenverzeichnis
     */
    public FearGreedScraper() {
        this("data");
    }
    
    /**
     * Konstruktor mit konfigurierbarem Datenverzeichnis
     * @param dataDirectory Pfad zum Datenverzeichnis
     */
    public FearGreedScraper(String dataDirectory) {
        this.dataDirectory = dataDirectory != null ? dataDirectory : "data";
        LOGGER.info("FearGreedScraper initialisiert für CNN Fear & Greed Index API");
        LOGGER.info("Symbol: " + SYMBOL + " | API: " + CNN_API_URL);
    }
    
    /**
     * Extrahiert den aktuellen Fear & Greed Index von CNN
     * 
     * @return CurrencyPairData-Objekt mit dem Fear & Greed Index als BTC/USD
     *         oder null bei Fehlern
     */
    public CurrencyPairData extractFearGreedData() {
        LOGGER.info("Beginne Extraktion des CNN Fear & Greed Index...");
        
        try {
            // Lade JSON von CNN API
            String jsonResponse = loadApiData();
            
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                LOGGER.warning("Keine Daten von CNN API erhalten");
                return createFallbackData();
            }
            
            // Parse den aktuellen Score aus dem JSON
            double fearGreedScore = extractScore(jsonResponse);
            String rating = extractRating(jsonResponse);
            
            if (fearGreedScore < 0) {
                LOGGER.warning("Konnte Fear & Greed Score nicht extrahieren");
                return createFallbackData();
            }
            
            LOGGER.info(String.format("Fear & Greed Index extrahiert: %.1f (%s)", fearGreedScore, rating));
            
            // Konvertiere zu CurrencyPairData
            CurrencyPairData data = convertToCurrencyPairData(fearGreedScore, rating);
            
            // Logge Zusammenfassung
            logExtractionSummary(data, fearGreedScore, rating);
            
            return data;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Extrahieren des Fear & Greed Index: " + e.getMessage(), e);
            return createFallbackData();
        }
    }
    
    /**
     * Lädt die JSON-Daten von der CNN API
     */
    private String loadApiData() {
        HttpURLConnection connection = null;
        
        try {
            // Erstelle URL mit aktuellem Datum für frische Daten
            String dateParam = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            URL url = new URL(CNN_API_URL + "/" + dateParam);
            
            LOGGER.fine("Lade Daten von: " + url.toString());
            
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOGGER.warning("HTTP Fehler: " + responseCode);
                return null;
            }
            
            // Lese Response
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            String jsonData = response.toString();
            LOGGER.fine("API Response erhalten: " + jsonData.length() + " Bytes");
            
            // Speichere Debug-Daten falls aktiviert
            saveDebugData(jsonData);
            
            return jsonData;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Laden der CNN API: " + e.getMessage(), e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Extrahiert den Score aus dem JSON-Response
     * Sucht nach dem aktuellen "fear_and_greed" Objekt
     */
    private double extractScore(String json) {
        try {
            // Suche nach dem ersten "score" im "fear_and_greed" Block
            // JSON-Struktur: {"fear_and_greed":{"score":75,"rating":"greed",...},...}
            
            // Finde den fear_and_greed Block
            int fgStart = json.indexOf("\"fear_and_greed\"");
            if (fgStart == -1) {
                LOGGER.warning("fear_and_greed Block nicht gefunden");
                return -1;
            }
            
            // Extrahiere den Bereich nach fear_and_greed
            String fgBlock = json.substring(fgStart, Math.min(fgStart + 500, json.length()));
            
            // Suche nach score
            Matcher scoreMatcher = SCORE_PATTERN.matcher(fgBlock);
            if (scoreMatcher.find()) {
                double score = Double.parseDouble(scoreMatcher.group(1));
                LOGGER.fine("Score gefunden: " + score);
                return score;
            }
            
            LOGGER.warning("Score Pattern nicht gefunden in: " + fgBlock.substring(0, Math.min(200, fgBlock.length())));
            return -1;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Parsen des Scores: " + e.getMessage(), e);
            return -1;
        }
    }
    
    /**
     * Extrahiert das Rating aus dem JSON-Response
     */
    private String extractRating(String json) {
        try {
            // Finde den fear_and_greed Block
            int fgStart = json.indexOf("\"fear_and_greed\"");
            if (fgStart == -1) {
                return "unknown";
            }
            
            String fgBlock = json.substring(fgStart, Math.min(fgStart + 500, json.length()));
            
            Matcher ratingMatcher = RATING_PATTERN.matcher(fgBlock);
            if (ratingMatcher.find()) {
                return ratingMatcher.group(1);
            }
            
            return "unknown";
            
        } catch (Exception e) {
            LOGGER.fine("Konnte Rating nicht extrahieren: " + e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * Konvertiert den Fear & Greed Index zu einem CurrencyPairData-Objekt
     * 
     * Mapping:
     * - buyPercentage = 100 - fearGreedIndex (bei Fear ist "Buy" hoch = Contrarian Long)
     * - sellPercentage = fearGreedIndex (bei Greed ist "Sell" hoch = Contrarian Short)
     * 
     * Signal-Logik:
     * - Fear (0-44) → BUY Signal
     * - Neutral (45-55) → NEUTRAL Signal
     * - Greed (56-100) → SELL Signal
     */
    private CurrencyPairData convertToCurrencyPairData(double fearGreedIndex, String rating) {
        // Mapping: Bei Fear soll der "Buy"-Balken groß sein (Contrarian = Long gehen)
        // Bei Greed soll der "Sell"-Balken groß sein (Contrarian = Short gehen)
        double buyPercentage = 100.0 - fearGreedIndex;
        double sellPercentage = fearGreedIndex;
        
        // Berechne Trading-Signal basierend auf Fear & Greed Logik
        TradingSignal signal = calculateFearGreedSignal(fearGreedIndex);
        
        // Erstelle CurrencyPairData
        CurrencyPairData data = new CurrencyPairData(SYMBOL, buyPercentage, sellPercentage, signal);
        
        LOGGER.info(String.format("Konvertiert: %s | Index=%.1f (%s) → Buy=%.1f%%, Sell=%.1f%%, Signal=%s",
                SYMBOL, fearGreedIndex, rating, buyPercentage, sellPercentage, signal));
        
        return data;
    }
    
    /**
     * Berechnet das Trading-Signal basierend auf dem Fear & Greed Index
     * 
     * Contrarian-Logik:
     * - Extreme Fear / Fear (0-44): Andere haben Angst → Kaufen (BUY)
     * - Neutral (45-55): Abwarten (NEUTRAL)
     * - Greed / Extreme Greed (56-100): Andere sind gierig → Verkaufen (SELL)
     */
    private TradingSignal calculateFearGreedSignal(double fearGreedIndex) {
        if (fearGreedIndex < FEAR_THRESHOLD) {
            // Fear Zone (0-44) → Contrarian BUY
            return TradingSignal.BUY;
        } else if (fearGreedIndex > GREED_THRESHOLD) {
            // Greed Zone (56-100) → Contrarian SELL
            return TradingSignal.SELL;
        } else {
            // Neutral Zone (45-55) → Warten
            return TradingSignal.NEUTRAL;
        }
    }
    
    /**
     * Erstellt Fallback-Daten wenn die API nicht erreichbar ist
     */
    private CurrencyPairData createFallbackData() {
        LOGGER.info("Erstelle Fallback-Daten für " + SYMBOL);
        
        // Neutrale Fallback-Werte
        double neutralIndex = 50.0;
        return new CurrencyPairData(SYMBOL, 50.0, 50.0, TradingSignal.NEUTRAL);
    }
    
    /**
     * Speichert Debug-Daten für Analyse
     */
    private void saveDebugData(String jsonData) {
        if (!LOGGER.isLoggable(Level.FINE)) {
            return;
        }
        
        try {
            java.nio.file.Path debugPath = java.nio.file.Paths.get(dataDirectory, "debug_html");
            if (!java.nio.file.Files.exists(debugPath)) {
                java.nio.file.Files.createDirectories(debugPath);
            }
            
            java.nio.file.Path jsonFile = debugPath.resolve("fear_greed_api_response.json");
            java.nio.file.Files.write(jsonFile, jsonData.getBytes(StandardCharsets.UTF_8));
            LOGGER.fine("Debug JSON gespeichert: " + jsonFile.toAbsolutePath());
            
        } catch (Exception e) {
            LOGGER.fine("Konnte Debug-Daten nicht speichern: " + e.getMessage());
        }
    }
    
    /**
     * Loggt eine Zusammenfassung der Extraktion
     */
    private void logExtractionSummary(CurrencyPairData data, double fearGreedIndex, String rating) {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("=== Fear & Greed Extraktions-Zusammenfassung ===");
            LOGGER.info(String.format("CNN Fear & Greed Index: %.1f (%s)", fearGreedIndex, rating));
            LOGGER.info(String.format("Symbol: %s", data.getCurrencyPair()));
            LOGGER.info(String.format("Buy (Contrarian Long): %.1f%%", data.getBuyPercentage()));
            LOGGER.info(String.format("Sell (Contrarian Short): %.1f%%", data.getSellPercentage()));
            LOGGER.info(String.format("Trading-Signal: %s (%s)", 
                    data.getTradingSignal(), data.getTradingSignal().getDescription()));
            LOGGER.info("=== Ende Zusammenfassung ===");
        }
    }
    
    /**
     * Testet die Verbindung zur CNN API
     * 
     * @return true wenn die API erreichbar ist
     */
    public boolean testConnection() {
        try {
            String jsonData = loadApiData();
            
            if (jsonData != null && !jsonData.isEmpty()) {
                double score = extractScore(jsonData);
                boolean isValid = score >= 0 && score <= 100;
                
                LOGGER.info("CNN Fear & Greed API Verbindungstest: " + (isValid ? "ERFOLGREICH" : "FEHLGESCHLAGEN"));
                if (isValid) {
                    LOGGER.info("Aktueller Index: " + score);
                }
                return isValid;
            }
            
            LOGGER.warning("CNN Fear & Greed API Verbindungstest: FEHLGESCHLAGEN (keine Daten)");
            return false;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "CNN Fear & Greed API Verbindungstest fehlgeschlagen: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Gibt das Symbol zurück das dieser Scraper verwendet
     */
    public String getSymbol() {
        return SYMBOL;
    }
    
    /**
     * Gibt die Schwellenwerte für die Signal-Berechnung zurück
     */
    public String getThresholdInfo() {
        return String.format("Fear-Threshold: <%.0f (BUY), Greed-Threshold: >%.0f (SELL)", 
                FEAR_THRESHOLD, GREED_THRESHOLD);
    }
}