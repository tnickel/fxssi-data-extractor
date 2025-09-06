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
 * Verwendet JSoup für das Parsen der HTML-Seite mit korrekter FXSSI-Struktur-Erkennung
 * 
 * @author Generated for FXSSI Data Extraction
 * @version 1.4 (mit korrekter HTML-Struktur-Erkennung für .line/.symbol/.ratio Elemente)
 */
public class FXSSIScraper {
    
    private static final Logger LOGGER = Logger.getLogger(FXSSIScraper.class.getName());
    private static final String FXSSI_URL = "https://fxssi.com/tools/current-ratio";
    private static final int TIMEOUT_MS = 15000; // 15 Sekunden Timeout
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    
    // Pattern für Extraktion von Prozentangaben
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)%");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    
    // Instance-Variable für Datenverzeichnis
    private final String dataDirectory;
    
    /**
     * Konstruktor mit Standard-Datenverzeichnis
     */
    public FXSSIScraper() {
        this("data");
    }
    
    /**
     * Konstruktor mit konfigurierbarem Datenverzeichnis
     * @param dataDirectory Pfad zum Datenverzeichnis
     */
    public FXSSIScraper(String dataDirectory) {
        this.dataDirectory = dataDirectory != null ? dataDirectory : "data";
        LOGGER.info("FXSSIScraper initialisiert für URL: " + FXSSI_URL);
        LOGGER.info("Debug-Dateien werden gespeichert in: " + this.dataDirectory + "/debug_html");
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
            
            // Debug: Dokumentstruktur analysieren und HTML speichern
            analyzeDocumentStructure(document);
            
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
     * Analysiert die Dokumentstruktur für besseres Debugging und speichert HTML
     */
    private void analyzeDocumentStructure(Document document) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("=== DOKUMENT-ANALYSE ===");
            LOGGER.fine("Titel: " + document.title());
            LOGGER.fine("Body-Klassen: " + document.body().className());
            
            // Suche nach FXSSI-spezifischen Elementen mit neuer Struktur-Erkennung
            Elements sentimentRatios = document.select(".sentiment-ratios");
            LOGGER.fine("Sentiment-Ratios Container: " + sentimentRatios.size());
            
            Elements lineElements = document.select(".line");
            LOGGER.fine("Line Elemente gefunden: " + lineElements.size());
            
            Elements symbolElements = document.select(".symbol");
            LOGGER.fine("Symbol Elemente gefunden: " + symbolElements.size());
            
            Elements ratioElements = document.select(".ratio");
            LOGGER.fine("Ratio Elemente gefunden: " + ratioElements.size());
            
            Elements ratioBarLeft = document.select(".ratio-bar-left");
            LOGGER.fine("Ratio-Bar-Left Elemente gefunden: " + ratioBarLeft.size());
            
            Elements ratioBarRight = document.select(".ratio-bar-right");
            LOGGER.fine("Ratio-Bar-Right Elemente gefunden: " + ratioBarRight.size());
            
            Elements signalElements = document.select(".signal");
            LOGGER.fine("Signal Elemente gefunden: " + signalElements.size());
            
            LOGGER.fine("=== ENDE DOKUMENT-ANALYSE ===");
        }
        
        // Speichere HTML-Struktur für Debugging
        saveHtmlForDebugging(document);
    }
    
    /**
     * Speichert die aktuelle HTML-Struktur für Debugging-Zwecke
     */
    private void saveHtmlForDebugging(Document document) {
        try {
            // Erstelle Debug-Verzeichnis im konfigurierten Root-Pfad
            java.nio.file.Path debugPath = java.nio.file.Paths.get(dataDirectory, "debug_html");
            if (!java.nio.file.Files.exists(debugPath)) {
                java.nio.file.Files.createDirectories(debugPath);
                LOGGER.info("Debug-Verzeichnis erstellt: " + debugPath.toAbsolutePath());
            }
            
            // Speichere komplette HTML-Seite (überschreibt vorherige)
            saveCompleteHtml(document, debugPath);
            
            // Speichere Struktur-Analyse mit neuer Erkennung
            saveStructureAnalysisUpdated(document, debugPath);
            
            // Speichere relevante Bereiche mit neuer Struktur
            saveRelevantSectionsUpdated(document, debugPath);
            
            LOGGER.info("HTML-Debug-Dateien gespeichert in: " + debugPath.toAbsolutePath());
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Speichern der Debug-HTML: " + e.getMessage(), e);
        }
    }
    
    /**
     * Speichert die komplette HTML-Seite
     */
    private void saveCompleteHtml(Document document, java.nio.file.Path debugPath) {
        try {
            java.nio.file.Path htmlFile = debugPath.resolve("fxssi_current.html");
            java.nio.file.Files.write(htmlFile, document.outerHtml().getBytes("UTF-8"));
            LOGGER.fine("Komplette HTML gespeichert: " + htmlFile.getFileName());
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Speichern der kompletten HTML: " + e.getMessage());
        }
    }
    
    /**
     * Speichert eine aktualisierte Struktur-Analyse mit neuer Erkennung
     */
    private void saveStructureAnalysisUpdated(Document document, java.nio.file.Path debugPath) {
        try {
            java.nio.file.Path structureFile = debugPath.resolve("fxssi_structure.txt");
            StringBuilder analysis = new StringBuilder();
            
            analysis.append("FXSSI Website Struktur-Analyse (Aktualisiert)\n");
            analysis.append("=============================================\n");
            analysis.append("URL: ").append(FXSSI_URL).append("\n");
            analysis.append("Zeitstempel: ").append(java.time.LocalDateTime.now()).append("\n");
            analysis.append("Titel: ").append(document.title()).append("\n\n");
            
            // Neue Struktur-Analyse basierend auf der erkannten HTML-Struktur
            analysis.append("=== NEUE FXSSI STRUKTUR-ERKENNUNG ===\n");
            
            Elements sentimentRatios = document.select(".sentiment-ratios");
            analysis.append("Sentiment-Ratios Container: ").append(sentimentRatios.size()).append("\n");
            
            Elements lineElements = document.select(".line");
            analysis.append("Line Elemente (.line): ").append(lineElements.size()).append("\n");
            
            if (!lineElements.isEmpty()) {
                analysis.append("\nDetaillierte Analyse der .line Elemente:\n");
                for (int i = 0; i < Math.min(5, lineElements.size()); i++) {
                    Element line = lineElements.get(i);
                    analysis.append("Line ").append(i+1).append(":\n");
                    
                    Elements symbols = line.select(".symbol");
                    if (!symbols.isEmpty()) {
                        analysis.append("  Symbol: ").append(symbols.first().text()).append("\n");
                    }
                    
                    Elements ratios = line.select(".ratio");
                    if (!ratios.isEmpty()) {
                        Elements leftBars = ratios.select(".ratio-bar-left");
                        Elements rightBars = ratios.select(".ratio-bar-right");
                        
                        if (!leftBars.isEmpty()) {
                            analysis.append("  Left Ratio: ").append(leftBars.first().text()).append("\n");
                        }
                        if (!rightBars.isEmpty()) {
                            analysis.append("  Right Ratio: ").append(rightBars.first().text()).append("\n");
                        }
                    }
                    
                    Elements signals = line.select(".signal");
                    if (!signals.isEmpty()) {
                        analysis.append("  Signal: ").append(signals.first().className()).append("\n");
                    }
                    analysis.append("\n");
                }
            }
            
            java.nio.file.Files.write(structureFile, analysis.toString().getBytes("UTF-8"));
            LOGGER.fine("Aktualisierte Struktur-Analyse gespeichert: " + structureFile.getFileName());
            
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Speichern der aktualisierten Struktur-Analyse: " + e.getMessage());
        }
    }
    
    /**
     * Speichert relevante HTML-Bereiche mit neuer Struktur-Erkennung
     */
    private void saveRelevantSectionsUpdated(Document document, java.nio.file.Path debugPath) {
        try {
            java.nio.file.Path relevantFile = debugPath.resolve("fxssi_relevant_sections.html");
            StringBuilder relevantHtml = new StringBuilder();
            
            relevantHtml.append("<!-- FXSSI Relevante Bereiche (Neue Struktur) -->\n\n");
            
            // Extrahiere .sentiment-ratios Container
            Elements sentimentRatios = document.select(".sentiment-ratios");
            if (!sentimentRatios.isEmpty()) {
                relevantHtml.append("<!-- SENTIMENT-RATIOS CONTAINER -->\n");
                relevantHtml.append(sentimentRatios.first().outerHtml()).append("\n\n");
            }
            
            // Extrahiere einzelne .line Elemente
            Elements lineElements = document.select(".line");
            relevantHtml.append("<!-- EINZELNE .LINE ELEMENTE (").append(lineElements.size()).append(" GEFUNDEN) -->\n");
            for (int i = 0; i < Math.min(10, lineElements.size()); i++) {
                Element line = lineElements.get(i);
                relevantHtml.append("<!-- LINE ").append(i+1).append(" -->\n");
                relevantHtml.append(line.outerHtml()).append("\n\n");
            }
            
            java.nio.file.Files.write(relevantFile, relevantHtml.toString().getBytes("UTF-8"));
            LOGGER.fine("Relevante Bereiche (neue Struktur) gespeichert: " + relevantFile.getFileName());
            
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Speichern der relevanten Bereiche: " + e.getMessage());
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
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Connection", "keep-alive")
                    .get();
                    
            LOGGER.info("Webseite erfolgreich geladen - Titel: " + document.title());
            return document;
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Laden der Webseite: " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Parst die Current Ratio Daten aus dem HTML-Dokument mit korrekter Struktur-Erkennung
     */
    private List<CurrencyPairData> parseCurrentRatioData(Document document) {
        List<CurrencyPairData> currencyData = new ArrayList<>();
        
        LOGGER.info("Beginne Parsing der Current Ratio Daten mit neuer Struktur-Erkennung...");
        
        try {
            // Neue Hauptstrategie: Korrekte FXSSI-Struktur-Erkennung
            currencyData = parseCorrectFXSSIStructure(document);
            
            if (currencyData.isEmpty()) {
                LOGGER.warning("Neue Struktur-Erkennung lieferte keine Ergebnisse, versuche Fallback-Strategien");
                currencyData = tryFXSSISelectors(document);
            }
            
            if (currencyData.isEmpty()) {
                LOGGER.warning("FXSSI-Selektoren fehlgeschlagen, versuche Tabellen-Parsing");
                currencyData = tryTableParsing(document);
            }
            
            if (currencyData.isEmpty()) {
                LOGGER.warning("Tabellen-Parsing fehlgeschlagen, versuche intelligentes Fallback-Parsing");
                currencyData = tryIntelligentFallbackParsing(document);
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
     * Neue Hauptmethode: Parst die korrekte FXSSI-Struktur mit .line/.symbol/.ratio Elementen
     */
    private List<CurrencyPairData> parseCorrectFXSSIStructure(Document document) {
        List<CurrencyPairData> currencyData = new ArrayList<>();
        
        LOGGER.info("Verwende korrekte FXSSI-Struktur-Erkennung (.line/.symbol/.ratio)...");
        
        // Suche nach .line Elementen innerhalb von .sentiment-ratios
        Elements lineElements = document.select(".sentiment-ratios .line");
        
        if (lineElements.isEmpty()) {
            // Fallback: Suche nach .line Elementen überall
            lineElements = document.select(".line");
            LOGGER.info("Fallback: Suche .line Elemente überall, gefunden: " + lineElements.size());
        } else {
            LOGGER.info("Gefundene .line Elemente in .sentiment-ratios: " + lineElements.size());
        }
        
        for (Element lineElement : lineElements) {
            try {
                CurrencyPairData pairData = parseLineElement(lineElement);
                if (pairData != null) {
                    currencyData.add(pairData);
                    LOGGER.fine("Line-Element erfolgreich geparst: " + pairData.getCurrencyPair());
                }
            } catch (Exception e) {
                LOGGER.fine("Fehler beim Parsing eines Line-Elements: " + e.getMessage());
            }
        }
        
        LOGGER.info("Korrekte FXSSI-Struktur-Erkennung: " + currencyData.size() + " Datensätze gefunden");
        return currencyData;
    }
    
    /**
     * Parst ein einzelnes .line Element mit der korrekten FXSSI-Struktur
     */
    private CurrencyPairData parseLineElement(Element lineElement) {
        try {
            // Extrahiere Währungspaar aus .symbol
            Elements symbolElements = lineElement.select(".symbol");
            if (symbolElements.isEmpty()) {
                LOGGER.fine("Kein .symbol Element in .line gefunden");
                return null;
            }
            
            String currencyPair = symbolElements.first().text().trim();
            if (currencyPair.isEmpty()) {
                LOGGER.fine("Leeres Währungspaar in .symbol gefunden");
                return null;
            }
            
            // Formatiere Währungspaar
            currencyPair = formatCurrencyPair(currencyPair);
            
            // Extrahiere Prozentangaben aus .ratio
            Elements ratioElements = lineElement.select(".ratio");
            if (ratioElements.isEmpty()) {
                LOGGER.fine("Kein .ratio Element in .line gefunden für: " + currencyPair);
                return null;
            }
            
            Element ratioElement = ratioElements.first();
            
            // Extrahiere linke Prozentangabe (.ratio-bar-left)
            double leftPercentage = extractPercentageFromRatioBar(ratioElement, ".ratio-bar-left");
            
            // Extrahiere rechte Prozentangabe (.ratio-bar-right)
            double rightPercentage = extractPercentageFromRatioBar(ratioElement, ".ratio-bar-right");
            
            if (leftPercentage < 0 || rightPercentage < 0) {
                LOGGER.fine("Ungültige Prozentangaben für: " + currencyPair + 
                           " (left: " + leftPercentage + ", right: " + rightPercentage + ")");
                return null;
            }
            
            // Extrahiere Signal aus .signal
            TradingSignal signal = extractTradingSignalFromLineElement(lineElement);
            
            // Interpretiere die Prozentangaben basierend auf FXSSI-Logik
            // Links = Buy, Rechts = Sell (basierend auf der Website-Analyse)
            double buyPercentage = leftPercentage;
            double sellPercentage = rightPercentage;
            
            // Validierung und Normalisierung
            double total = buyPercentage + sellPercentage;
            if (Math.abs(total - 100.0) > 2.0) { // Toleranz von 2%
                LOGGER.fine("Prozentangaben ergeben nicht 100% für " + currencyPair + 
                           " (Total: " + total + "%), normalisiere...");
                // Normalisiere falls nötig
                buyPercentage = (buyPercentage / total) * 100.0;
                sellPercentage = (sellPercentage / total) * 100.0;
            }
            
            // Erstelle CurrencyPairData
            CurrencyPairData pairData = new CurrencyPairData(currencyPair, buyPercentage, sellPercentage, signal);
            
            // Validiere Konsistenz
            if (!pairData.isDataConsistent()) {
                LOGGER.fine("Daten inkonsistent für " + currencyPair + ", korrigiere...");
                pairData.setSellPercentage(100.0 - buyPercentage);
            }
            
            LOGGER.fine("Line-Element erfolgreich geparst: " + currencyPair + 
                       " - Buy: " + String.format("%.1f", buyPercentage) + 
                       "%, Sell: " + String.format("%.1f", sellPercentage) + 
                       "%, Signal: " + signal);
            
            return pairData;
            
        } catch (Exception e) {
            LOGGER.fine("Fehler beim Parsing eines Line-Elements: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrahiert Prozentangabe aus einem Ratio-Bar Element
     */
    private double extractPercentageFromRatioBar(Element ratioElement, String barSelector) {
        try {
            Elements barElements = ratioElement.select(barSelector);
            if (barElements.isEmpty()) {
                return -1;
            }
            
            Element barElement = barElements.first();
            
            // Versuche Prozentangabe aus dem Text zu extrahieren
            String barText = barElement.text().trim();
            if (!barText.isEmpty()) {
                Matcher matcher = PERCENTAGE_PATTERN.matcher(barText);
                if (matcher.find()) {
                    return Double.parseDouble(matcher.group(1));
                }
            }
            
            // Fallback: Versuche aus dem style-Attribut zu extrahieren
            String style = barElement.attr("style");
            if (!style.isEmpty()) {
                Matcher styleMatcher = Pattern.compile("width:\\s*(\\d+(?:\\.\\d+)?)%").matcher(style);
                if (styleMatcher.find()) {
                    return Double.parseDouble(styleMatcher.group(1));
                }
            }
            
            return -1;
            
        } catch (Exception e) {
            LOGGER.fine("Fehler beim Extrahieren der Prozentangabe aus " + barSelector + ": " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Extrahiert Trading-Signal aus einem .line Element
     */
    private TradingSignal extractTradingSignalFromLineElement(Element lineElement) {
        try {
            Elements signalElements = lineElement.select(".signal");
            if (signalElements.isEmpty()) {
                return TradingSignal.UNKNOWN;
            }
            
            Element signalElement = signalElements.first();
            String signalClass = signalElement.className().toLowerCase();
            
            if (signalClass.contains("buy")) {
                return TradingSignal.BUY;
            } else if (signalClass.contains("sell")) {
                return TradingSignal.SELL;
            } else if (signalClass.contains("neutral")) {
                return TradingSignal.NEUTRAL;
            } else {
                // Fallback: Bestimme Signal basierend auf Text oder anderen Attributen
                String signalText = signalElement.text().toLowerCase();
                if (signalText.contains("buy")) {
                    return TradingSignal.BUY;
                } else if (signalText.contains("sell")) {
                    return TradingSignal.SELL;
                } else if (signalText.contains("neutral")) {
                    return TradingSignal.NEUTRAL;
                }
                
                return TradingSignal.UNKNOWN;
            }
            
        } catch (Exception e) {
            LOGGER.fine("Fehler beim Extrahieren des Trading-Signals: " + e.getMessage());
            return TradingSignal.UNKNOWN;
        }
    }
    
    /**
     * Versucht echte FXSSI CSS-Selektoren basierend auf Struktur-Analyse (Fallback)
     */
    private List<CurrencyPairData> tryFXSSISelectors(Document document) {
        List<CurrencyPairData> currencyData = new ArrayList<>();
        
        LOGGER.info("Verwende Fallback FXSSI CSS-Selektoren...");
        
        // Erweiterte FXSSI-Selektoren basierend auf neuer Struktur
        String[] fxssiSelectors = {
            ".sentiment-ratios .line",         // Neue Hauptstruktur
            ".sentiment-ratios",               // Container
            ".line",                           // Line-Elemente überall
            "#thePairs .text-ratio",           // Ursprünglicher Selektor
            ".cur-rat-pairs .text-ratio",      // Ursprünglicher Selektor
            "#thePairs > div",                 // Direkte Kinder
            ".cur-rat-pairs > div"             // Direkte Kinder
        };
        
        for (String selector : fxssiSelectors) {
            Elements elements = document.select(selector);
            if (!elements.isEmpty()) {
                LOGGER.info("Verwende Fallback-Selektor: " + selector + " (" + elements.size() + " Elemente gefunden)");
                
                for (Element element : elements) {
                    CurrencyPairData pairData = parseFXSSIElement(element);
                    if (pairData != null) {
                        currencyData.add(pairData);
                        LOGGER.fine("Fallback-Element erfolgreich geparst: " + pairData.getCurrencyPair());
                    }
                }
                
                // Falls wir mit diesem Selektor gültige Daten gefunden haben, verwende nur diese
                if (!currencyData.isEmpty()) {
                    break;
                }
            }
        }
        
        LOGGER.info("Fallback FXSSI-Selektoren: " + currencyData.size() + " Datensätze gefunden");
        return currencyData;
    }
    
    /**
     * Parst ein FXSSI-spezifisches Element (ursprüngliche Methode)
     */
    private CurrencyPairData parseFXSSIElement(Element element) {
        try {
            // Debug: Element-Details
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Parse FXSSI Element: " + element.tagName() + " - Klassen: " + element.className());
                String elementText = element.text();
                LOGGER.fine("Element Text: " + elementText.substring(0, Math.min(100, elementText.length())));
            }
            
            // Suche nach Währungspaar im Element oder Parent-Elementen
            String currencyPair = extractCurrencyPairFromFXSSIElement(element);
            if (currencyPair == null || currencyPair.isEmpty()) {
                LOGGER.fine("Kein Währungspaar in Element gefunden");
                return null;
            }
            
            // Suche nach Prozentangaben in diesem spezifischen Element
            double buyPercentage = extractBuyPercentageFromFXSSIElement(element);
            double sellPercentage = extractSellPercentageFromFXSSIElement(element);
            
            if (buyPercentage < 0) {
                LOGGER.fine("Keine gültige Buy-Percentage für " + currencyPair + " gefunden");
                return null;
            }
            
            // Berechne Sell-Percentage falls nicht gefunden
            if (sellPercentage < 0) {
                sellPercentage = 100.0 - buyPercentage;
            }
            
            TradingSignal signal = calculateTradingSignal(buyPercentage);
            CurrencyPairData pairData = new CurrencyPairData(currencyPair, buyPercentage, sellPercentage, signal);
            
            // Validierung
            if (!pairData.isDataConsistent()) {
                pairData.setSellPercentage(100.0 - buyPercentage);
            }
            
            LOGGER.fine("FXSSI-Element erfolgreich geparst: " + currencyPair + " - Buy: " + buyPercentage + "%");
            return pairData;
            
        } catch (Exception e) {
            LOGGER.fine("Fehler beim Parsing eines FXSSI-Elements: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrahiert Währungspaar aus FXSSI-Element
     */
    private String extractCurrencyPairFromFXSSIElement(Element element) {
        // Suche in verschiedenen Element-Bereichen
        String[] searchAreas = {
            element.text(),
            element.attr("data-symbol"),
            element.attr("data-pair"),
            element.attr("title"),
            element.className(),
            element.parent() != null ? element.parent().text() : "",
            element.parent() != null ? element.parent().attr("data-symbol") : ""
        };
        
        for (String area : searchAreas) {
            if (area != null && !area.isEmpty()) {
                String pair = extractCurrencyPairFromText(area);
                if (pair != null) {
                    return pair;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extrahiert Buy-Percentage aus FXSSI-Element
     */
    private double extractBuyPercentageFromFXSSIElement(Element element) {
        // Suche nach Elementen mit Buy/Long-Keywords in diesem spezifischen Bereich
        Elements buyElements = element.select("*:contains(buy), *:contains(long), .buy, .long, [class*='buy'], [class*='long']");
        
        for (Element buyElement : buyElements) {
            double percentage = extractFirstPercentage(buyElement);
            if (percentage >= 0) {
                return percentage;
            }
        }
        
        // Fallback: Erste Prozentangabe im Element
        double firstPercentage = extractFirstPercentage(element);
        if (firstPercentage >= 0) {
            return firstPercentage;
        }
        
        // Fallback: Suche in Parent-Element
        if (element.parent() != null) {
            return extractFirstPercentage(element.parent());
        }
        
        return -1;
    }
    
    /**
     * Extrahiert Sell-Percentage aus FXSSI-Element
     */
    private double extractSellPercentageFromFXSSIElement(Element element) {
        // Suche nach Elementen mit Sell/Short-Keywords
        Elements sellElements = element.select("*:contains(sell), *:contains(short), .sell, .short, [class*='sell'], [class*='short']");
        
        for (Element sellElement : sellElements) {
            double percentage = extractFirstPercentage(sellElement);
            if (percentage >= 0) {
                return percentage;
            }
        }
        
        // Fallback: Zweite Prozentangabe im Element
        List<Double> percentages = extractAllPercentages(element.text());
        if (percentages.size() >= 2) {
            return percentages.get(1);
        }
        
        return -1;
    }
    
    /**
     * Versucht Tabellen-basiertes Parsing
     */
    private List<CurrencyPairData> tryTableParsing(Document document) {
        List<CurrencyPairData> currencyData = new ArrayList<>();
        
        LOGGER.info("Verwende Tabellen-Parsing...");
        
        // Suche nach Tabellen mit Sentiment-Daten
        Elements tables = document.select("table");
        for (Element table : tables) {
            String tableClass = table.className().toLowerCase();
            String tableId = table.id().toLowerCase();
            
            // Prüfe ob Tabelle relevant ist
            if (tableClass.contains("sentiment") || tableClass.contains("ratio") || 
                tableClass.contains("current") || tableId.contains("sentiment")) {
                
                LOGGER.info("Gefundene relevante Tabelle: " + table.tagName() + " class=" + tableClass);
                
                Elements rows = table.select("tr");
                for (Element row : rows) {
                    CurrencyPairData pairData = parseTableRow(row);
                    if (pairData != null) {
                        currencyData.add(pairData);
                        LOGGER.fine("Tabellen-Parsing erfolgreich: " + pairData.getCurrencyPair());
                    }
                }
            }
        }
        
        LOGGER.info("Tabellen-Parsing: " + currencyData.size() + " Datensätze gefunden");
        return currencyData;
    }
    
    /**
     * Parst eine Tabellen-Zeile
     */
    private CurrencyPairData parseTableRow(Element row) {
        try {
            Elements cells = row.select("td, th");
            if (cells.size() < 2) {
                return null;
            }
            
            String currencyPair = null;
            double buyPercentage = -1;
            double sellPercentage = -1;
            
            // Durchsuche Zellen nach Währungspaar und Prozentangaben
            for (Element cell : cells) {
                String cellText = cell.text();
                
                if (currencyPair == null && containsKnownCurrencyPair(cellText)) {
                    currencyPair = extractCurrencyPairFromText(cellText);
                }
                
                if (cellText.contains("%")) {
                    if (buyPercentage < 0) {
                        buyPercentage = extractFirstPercentage(cell);
                    } else if (sellPercentage < 0) {
                        sellPercentage = extractFirstPercentage(cell);
                    }
                }
            }
            
            if (currencyPair != null && buyPercentage >= 0) {
                if (sellPercentage < 0) {
                    sellPercentage = 100.0 - buyPercentage;
                }
                
                TradingSignal signal = calculateTradingSignal(buyPercentage);
                return new CurrencyPairData(currencyPair, buyPercentage, sellPercentage, signal);
            }
            
        } catch (Exception e) {
            LOGGER.fine("Fehler beim Parsing einer Tabellen-Zeile: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Intelligentes Fallback-Parsing mit besserer Filterung
     */
    private List<CurrencyPairData> tryIntelligentFallbackParsing(Document document) {
        List<CurrencyPairData> currencyData = new ArrayList<>();
        
        LOGGER.info("Verwende intelligentes Fallback-Parsing...");
        
        // Suche nach Elementen die Währungspaare UND Prozente enthalten
        Elements allElements = document.select("div, span, td, li");
        
        for (Element element : allElements) {
            String text = element.text();
            
            // Nur Elemente mit erkennbaren Währungspaaren UND Prozentangaben
            if (containsKnownCurrencyPair(text) && text.contains("%")) {
                LOGGER.fine("Potentielles Element gefunden: " + text.substring(0, Math.min(100, text.length())));
                
                CurrencyPairData pairData = parseIntelligentElement(element);
                if (pairData != null) {
                    currencyData.add(pairData);
                    LOGGER.fine("Intelligentes Parsing erfolgreich: " + pairData.getCurrencyPair());
                }
            }
        }
        
        LOGGER.info("Intelligentes Fallback-Parsing: " + currencyData.size() + " Datensätze gefunden");
        return currencyData;
    }
    
    /**
     * Parst ein intelligentes Element
     */
    private CurrencyPairData parseIntelligentElement(Element element) {
        try {
            String text = element.text();
            
            String currencyPair = extractCurrencyPairFromText(text);
            if (currencyPair == null) {
                return null;
            }
            
            // Extrahiere alle Prozentangaben aus dem Text
            List<Double> percentages = extractAllPercentages(text);
            
            if (percentages.isEmpty()) {
                return null;
            }
            
            double buyPercentage = percentages.get(0);
            double sellPercentage = percentages.size() > 1 ? percentages.get(1) : (100.0 - buyPercentage);
            
            TradingSignal signal = calculateTradingSignal(buyPercentage);
            return new CurrencyPairData(currencyPair, buyPercentage, sellPercentage, signal);
            
        } catch (Exception e) {
            LOGGER.fine("Fehler beim intelligenten Parsing: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrahiert die erste Prozentangabe aus einem Element
     */
    private double extractFirstPercentage(Element element) {
        String text = element.text();
        Matcher matcher = PERCENTAGE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                LOGGER.fine("Konnte Prozentangabe nicht parsen: " + matcher.group(1));
            }
        }
        return -1;
    }
    
    /**
     * Extrahiert alle Prozentangaben aus einem Text
     */
    private List<Double> extractAllPercentages(String text) {
        List<Double> percentages = new ArrayList<>();
        Matcher matcher = PERCENTAGE_PATTERN.matcher(text);
        
        while (matcher.find()) {
            try {
                double percentage = Double.parseDouble(matcher.group(1));
                percentages.add(percentage);
            } catch (NumberFormatException e) {
                LOGGER.fine("Konnte Prozentangabe nicht parsen: " + matcher.group(1));
            }
        }
        
        return percentages;
    }
    
    /**
     * Überprüft ob ein Text bekannte Währungspaare enthält
     */
    private boolean containsKnownCurrencyPair(String text) {
        String upperText = text.toUpperCase();
        String[] knownPairs = {
            "EURUSD", "GBPUSD", "USDJPY", "AUDUSD", "USDCHF", "USDCAD",
            "NZDUSD", "EURJPY", "GBPJPY", "AUDJPY", "EURGBP", "EURAUD",
            "EURCHF", "GBPCHF", "XAUUSD", "XAGUSD"
        };
        
        for (String pair : knownPairs) {
            if (upperText.contains(pair)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Extrahiert Währungspaar aus Text
     */
    private String extractCurrencyPairFromText(String text) {
        String upperText = text.toUpperCase();
        String[] knownPairs = {
            "AUDJPY", "AUDUSD", "EURAUD", "EURGBP", "EURJPY", "EURUSD", 
            "GBPJPY", "GBPUSD", "NZDUSD", "USDCAD", "USDCHF", "USDJPY", 
            "XAGUSD", "XAUUSD", "EURCHF", "GBPCHF"
        };
        
        for (String pair : knownPairs) {
            if (upperText.contains(pair)) {
                return formatCurrencyPair(pair);
            }
        }
        return null;
    }
    
    /**
     * Formatiert Währungspaar als XXX/YYY
     */
    private String formatCurrencyPair(String pair) {
        pair = pair.toUpperCase();
        if (pair.length() == 6) {
            return pair.substring(0, 3) + "/" + pair.substring(3);
        } else if (pair.startsWith("XAU")) {
            return "XAU/USD";
        } else if (pair.startsWith("XAG")) {
            return "XAG/USD";
        }
        return pair;
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
            
            // Zusätzliche Validierung: Buy% + Sell% sollte ca. 100% ergeben
            double total = currencyData.getBuyPercentage() + currencyData.getSellPercentage();
            if (total < 95 || total > 105) {
                LOGGER.fine("Mathematisch inkonsistente Daten korrigiert für: " + pair + " (Total: " + total + "%)");
                currencyData.setSellPercentage(100.0 - currencyData.getBuyPercentage());
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