package com.fxsssi.extractor.gui;


import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;
import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.model.SignalChangeEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Custom TableCell für Mini-Signalverlauf-Charts.
 * Zeigt historische Signalverläufe mit Signalwechsel-Markierungen.
 * Arbeitet mit CurrencyPairTableRow Datentyp vom MainWindowController.
 */
public class SignalHistoryChartTableCell extends TableCell<MainWindowController.CurrencyPairTableRow, MainWindowController.CurrencyPairTableRow> {
    
    private static final Logger LOGGER = Logger.getLogger(SignalHistoryChartTableCell.class.getName());
    
    private final Canvas canvas;
    private final int daysPeriod;
    private final GuiDataService dataService;
    
    // Chart-Konfiguration
    private static final double CHART_WIDTH = 100.0;
    private static final double CHART_HEIGHT = 30.0;
    private static final double MARGIN = 2.0;
    
    // Signal-Farben
    private static final Color BUY_COLOR = Color.GREEN;
    private static final Color SELL_COLOR = Color.RED;
    private static final Color NEUTRAL_COLOR = Color.GRAY;
    private static final Color BACKGROUND_COLOR = Color.WHITE;
    private static final Color GRID_COLOR = Color.LIGHTGRAY;
    
    public SignalHistoryChartTableCell(int daysPeriod, GuiDataService dataService) {
        this.daysPeriod = daysPeriod;
        this.dataService = dataService;
        this.canvas = new Canvas(CHART_WIDTH, CHART_HEIGHT);
        
        // Canvas als Graphic setzen
        setGraphic(canvas);
    }
    
    @Override
    protected void updateItem(MainWindowController.CurrencyPairTableRow item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty || item == null) {
            setGraphic(null);
            return;
        }
        
        setGraphic(canvas);
        drawSignalChart(item);
    }
    
    /**
     * Zeichnet den Signalverlauf für das gegebene Währungspaar
     */
    private void drawSignalChart(MainWindowController.CurrencyPairTableRow tableRow) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Canvas löschen
        gc.clearRect(0, 0, CHART_WIDTH, CHART_HEIGHT);
        gc.setFill(BACKGROUND_COLOR);
        gc.fillRect(0, 0, CHART_WIDTH, CHART_HEIGHT);
        
        try {
            String currencyPair = tableRow.getCurrencyPair();
            
            // Historische Daten laden über GuiDataService
            List<CurrencyPairData> historicalData = loadHistoricalData(currencyPair);
            List<SignalChangeEvent> signalChanges = loadSignalChanges(currencyPair);
            
            if (historicalData.isEmpty()) {
                drawNoDataMessage(gc);
                return;
            }
            
            // Grid zeichnen
            drawGrid(gc);
            
            // Signalverlauf zeichnen
            drawSignalLine(gc, historicalData);
            
            // Signalwechsel-Punkte zeichnen
            drawSignalChangePoints(gc, signalChanges, historicalData);
            
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Zeichnen des Signalcharts für " + tableRow.getCurrencyPair() + ": " + e.getMessage());
            drawErrorMessage(gc);
        }
    }
    
    /**
     * Lädt historische Daten für das Währungspaar über den GuiDataService
     */
    private List<CurrencyPairData> loadHistoricalData(String symbol) {
        try {
            // Verwende GuiDataService um auf CurrencyPairDataManager zuzugreifen
            List<CurrencyPairData> allData = dataService.getHistoricalDataForCurrencyPair(symbol);
            if (allData == null || allData.isEmpty()) {
                return new ArrayList<>();
            }
            
            // Filtere auf die letzten X Tage
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysPeriod);
            return allData.stream()
                .filter(d -> d.getTimestamp().isAfter(cutoffTime))
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(java.util.stream.Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Laden historischer Daten für " + symbol + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Lädt Signalwechsel-Ereignisse für das Währungspaar über den GuiDataService
     */
    private List<SignalChangeEvent> loadSignalChanges(String symbol) {
        try {
            // Verwende GuiDataService um auf SignalChangeHistoryManager zuzugreifen
            return dataService.getSignalChangeHistoryManager()
                .getSignalChangesWithinHours(symbol, daysPeriod * 24);
            
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Laden der Signalwechsel für " + symbol + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Zeichnet das Hintergrundgrid
     */
    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(GRID_COLOR);
        gc.setLineWidth(0.5);
        
        // Horizontale Linien (BUY, NEUTRAL, SELL Ebenen)
        double buyLevel = MARGIN + 5;
        double neutralLevel = CHART_HEIGHT / 2;
        double sellLevel = CHART_HEIGHT - MARGIN - 5;
        
        gc.strokeLine(MARGIN, buyLevel, CHART_WIDTH - MARGIN, buyLevel);
        gc.strokeLine(MARGIN, neutralLevel, CHART_WIDTH - MARGIN, neutralLevel);
        gc.strokeLine(MARGIN, sellLevel, CHART_WIDTH - MARGIN, sellLevel);
    }
    
    /**
     * Zeichnet die Signallinie
     */
    private void drawSignalLine(GraphicsContext gc, List<CurrencyPairData> historicalData) {
        if (historicalData.size() < 2) return;
        
        gc.setLineWidth(1.5);
        
        double xStep = (CHART_WIDTH - 2 * MARGIN) / (double) (historicalData.size() - 1);
        
        for (int i = 0; i < historicalData.size() - 1; i++) {
            CurrencyPairData current = historicalData.get(i);
            CurrencyPairData next = historicalData.get(i + 1);
            
            double x1 = MARGIN + i * xStep;
            double y1 = getSignalY(current.getTradingSignal());
            double x2 = MARGIN + (i + 1) * xStep;
            double y2 = getSignalY(next.getTradingSignal());
            
            // Farbe basierend auf Signal setzen
            gc.setStroke(getSignalColor(current.getTradingSignal()));
            gc.strokeLine(x1, y1, x2, y2);
        }
    }
    
    /**
     * Zeichnet Signalwechsel-Punkte
     */
    private void drawSignalChangePoints(GraphicsContext gc, List<SignalChangeEvent> signalChanges, 
                                       List<CurrencyPairData> historicalData) {
        if (signalChanges.isEmpty() || historicalData.isEmpty()) return;
        
        LocalDateTime startTime = historicalData.get(0).getTimestamp();
        LocalDateTime endTime = historicalData.get(historicalData.size() - 1).getTimestamp();
        
        for (SignalChangeEvent change : signalChanges) {
            LocalDateTime changeTime = change.getChangeTime(); // Korrekte Methode aus SignalChangeEvent
            
            if (changeTime.isBefore(startTime) || changeTime.isAfter(endTime)) {
                continue;
            }
            
            // X-Position basierend auf Zeitstempel berechnen
            double timeProgress = getTimeProgress(changeTime, startTime, endTime);
            double x = MARGIN + timeProgress * (CHART_WIDTH - 2 * MARGIN);
            double y = getSignalY(change.getToSignal()); // Zeige das neue Signal
            
            // Punkt zeichnen - verschiedene Farben je nach Wichtigkeit
            Color pointColor = getImportanceColor(change.getImportance());
            gc.setFill(pointColor);
            gc.fillOval(x - 2, y - 2, 4, 4);
            
            // Weißer Rand für bessere Sichtbarkeit
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1);
            gc.strokeOval(x - 2, y - 2, 4, 4);
        }
    }
    
    /**
     * Berechnet Y-Position für ein Signal
     */
    private double getSignalY(CurrencyPairData.TradingSignal signal) {
        switch (signal) {
            case BUY:
                return MARGIN + 5;
            case SELL:
                return CHART_HEIGHT - MARGIN - 5;
            case NEUTRAL:
            default:
                return CHART_HEIGHT / 2;
        }
    }
    
    /**
     * Gibt Farbe für ein Signal zurück
     */
    private Color getSignalColor(CurrencyPairData.TradingSignal signal) {
        switch (signal) {
            case BUY:
                return BUY_COLOR;
            case SELL:
                return SELL_COLOR;
            case NEUTRAL:
            default:
                return NEUTRAL_COLOR;
        }
    }
    
    /**
     * Gibt Farbe für Signalwechsel-Wichtigkeit zurück
     */
    private Color getImportanceColor(SignalChangeEvent.SignalChangeImportance importance) {
        switch (importance) {
            case CRITICAL:
                return Color.RED;
            case HIGH:
                return Color.ORANGE;
            case MEDIUM:
                return Color.YELLOW;
            case LOW:
            default:
                return Color.BLACK;
        }
    }
    
    /**
     * Berechnet Zeitfortschritt zwischen Start und Ende
     */
    private double getTimeProgress(LocalDateTime timestamp, LocalDateTime start, LocalDateTime end) {
        if (start.equals(end)) return 0.0;
        
        long totalSeconds = java.time.Duration.between(start, end).getSeconds();
        long currentSeconds = java.time.Duration.between(start, timestamp).getSeconds();
        
        return Math.max(0.0, Math.min(1.0, (double) currentSeconds / totalSeconds));
    }
    
    /**
     * Zeichnet Nachricht wenn keine Daten verfügbar
     */
    private void drawNoDataMessage(GraphicsContext gc) {
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(javafx.scene.text.Font.font(8));
        gc.fillText("Keine Daten", CHART_WIDTH / 2 - 15, CHART_HEIGHT / 2 + 2);
    }
    
    /**
     * Zeichnet Fehlernachricht
     */
    private void drawErrorMessage(GraphicsContext gc) {
        gc.setFill(Color.RED);
        gc.setFont(javafx.scene.text.Font.font(8));
        gc.fillText("Fehler", CHART_WIDTH / 2 - 10, CHART_HEIGHT / 2 + 2);
    }
}