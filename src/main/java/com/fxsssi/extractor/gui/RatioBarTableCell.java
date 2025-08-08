package com.fxsssi.extractor.gui;

import com.fxsssi.extractor.gui.MainWindowController.CurrencyPairTableRow;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Custom TableCell für die Anzeige von horizontalen Ratio-Balken
 * Zeigt Buy-Percentage (blau) und Sell-Percentage (rot) als horizontale Balken an
 * 
 * @author Generated for FXSSI Data Extraction GUI
 * @version 1.0
 */
public class RatioBarTableCell extends TableCell<CurrencyPairTableRow, CurrencyPairTableRow> {
    
    private final HBox ratioContainer;
    private final Region buyBar;
    private final Region sellBar;
    private final Label buyLabel;
    private final Label sellLabel;
    private final StackPane buyBarContainer;
    private final StackPane sellBarContainer;
    
    private static final double BAR_HEIGHT = 25.0;
    private static final double CONTAINER_WIDTH = 350.0;
    private static final String BUY_BAR_STYLE = "-fx-background-color: #5B9BD5; -fx-background-radius: 3 0 0 3;";
    private static final String SELL_BAR_STYLE = "-fx-background-color: #E74C3C; -fx-background-radius: 0 3 3 0;";
    private static final String LABEL_STYLE = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;";
    
    public RatioBarTableCell() {
        // Erstelle Container für die Balken
        ratioContainer = new HBox();
        ratioContainer.setAlignment(Pos.CENTER_LEFT);
        ratioContainer.setPrefWidth(CONTAINER_WIDTH);
        ratioContainer.setMaxWidth(CONTAINER_WIDTH);
        ratioContainer.setMinWidth(CONTAINER_WIDTH);
        
        // Erstelle Buy-Bar Container
        buyBarContainer = new StackPane();
        buyBar = new Region();
        buyBar.setPrefHeight(BAR_HEIGHT);
        buyBar.setMaxHeight(BAR_HEIGHT);
        buyBar.setMinHeight(BAR_HEIGHT);
        buyBar.setStyle(BUY_BAR_STYLE);
        
        buyLabel = new Label();
        buyLabel.setStyle(LABEL_STYLE);
        buyLabel.setAlignment(Pos.CENTER);
        
        buyBarContainer.getChildren().addAll(buyBar, buyLabel);
        buyBarContainer.setAlignment(Pos.CENTER_LEFT);
        
        // Erstelle Sell-Bar Container
        sellBarContainer = new StackPane();
        sellBar = new Region();
        sellBar.setPrefHeight(BAR_HEIGHT);
        sellBar.setMaxHeight(BAR_HEIGHT);
        sellBar.setMinHeight(BAR_HEIGHT);
        sellBar.setStyle(SELL_BAR_STYLE);
        
        sellLabel = new Label();
        sellLabel.setStyle(LABEL_STYLE);
        sellLabel.setAlignment(Pos.CENTER);
        
        sellBarContainer.getChildren().addAll(sellBar, sellLabel);
        sellBarContainer.setAlignment(Pos.CENTER_RIGHT);
        
        // Füge Container zusammen
        ratioContainer.getChildren().addAll(buyBarContainer, sellBarContainer);
        ratioContainer.setSpacing(0);
        
        // Styling für die gesamte Zelle
        this.setPadding(new Insets(2, 5, 2, 5));
        this.setAlignment(Pos.CENTER);
    }
    
    @Override
    protected void updateItem(CurrencyPairTableRow item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty || item == null) {
            setGraphic(null);
            return;
        }
        
        // Hole Percentage-Werte
        double buyPercentage = item.getBuyPercentage();
        double sellPercentage = item.getSellPercentage();
        
        // Stelle sicher, dass die Werte valid sind
        if (buyPercentage < 0) buyPercentage = 0;
        if (sellPercentage < 0) sellPercentage = 0;
        
        // Normalisiere die Werte falls sie nicht 100% ergeben
        double total = buyPercentage + sellPercentage;
        if (total > 0) {
            buyPercentage = (buyPercentage / total) * 100.0;
            sellPercentage = (sellPercentage / total) * 100.0;
        }
        
        // Berechne Balkenbreiten basierend auf Container-Breite
        double buyWidth = (buyPercentage / 100.0) * CONTAINER_WIDTH;
        double sellWidth = (sellPercentage / 100.0) * CONTAINER_WIDTH;
        
        // Setze Balkenbreiten
        buyBar.setPrefWidth(buyWidth);
        buyBar.setMaxWidth(buyWidth);
        buyBar.setMinWidth(buyWidth);
        
        sellBar.setPrefWidth(sellWidth);
        sellBar.setMaxWidth(sellWidth);
        sellBar.setMinWidth(sellWidth);
        
        // Setze Container-Breiten
        buyBarContainer.setPrefWidth(buyWidth);
        buyBarContainer.setMaxWidth(buyWidth);
        buyBarContainer.setMinWidth(buyWidth);
        
        sellBarContainer.setPrefWidth(sellWidth);
        sellBarContainer.setMaxWidth(sellWidth);
        sellBarContainer.setMinWidth(sellWidth);
        
        // Setze Label-Texte
        buyLabel.setText(String.format("%.0f%%", buyPercentage));
        sellLabel.setText(String.format("%.0f%%", sellPercentage));
        
        // Verstecke Labels bei sehr schmalen Balken
        buyLabel.setVisible(buyWidth > 30);
        sellLabel.setVisible(sellWidth > 30);
        
        // Spezielle Styling-Anpassungen basierend auf Werten
        updateBarStyling(buyPercentage, sellPercentage);
        
        setGraphic(ratioContainer);
    }
    
    /**
     * Aktualisiert das Styling der Balken basierend auf den Werten
     */
    private void updateBarStyling(double buyPercentage, double sellPercentage) {
        // Basis-Styles
        String buyStyle = BUY_BAR_STYLE;
        String sellStyle = SELL_BAR_STYLE;
        
        // Spezielle Radius-Behandlung wenn einer der Balken sehr klein ist
        if (buyPercentage < 5) {
            // Buy-Balken sehr klein, Sell-Balken bekommt linken Radius
            sellStyle = "-fx-background-color: #E74C3C; -fx-background-radius: 3;";
        } else if (sellPercentage < 5) {
            // Sell-Balken sehr klein, Buy-Balken bekommt rechten Radius
            buyStyle = "-fx-background-color: #5B9BD5; -fx-background-radius: 3;";
        }
        
        // Farbintensität basierend auf extremen Werten
        if (buyPercentage > 70) {
            buyStyle = "-fx-background-color: #2E86AB; -fx-background-radius: 3 0 0 3;"; // Dunkleres Blau
        } else if (sellPercentage > 70) {
            sellStyle = "-fx-background-color: #C0392B; -fx-background-radius: 0 3 3 0;"; // Dunkleres Rot
        }
        
        buyBar.setStyle(buyStyle);
        sellBar.setStyle(sellStyle);
    }
    
    /**
     * Berechnet die optimale Textfarbe basierend auf der Hintergrundfarbe
     */
    private String getOptimalTextColor(double percentage) {
        // Bei sehr extremen Werten (dunklere Farben) verwende weiße Schrift
        if (percentage > 70) {
            return "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;";
        } else {
            return "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;";
        }
    }
    
    /**
     * Fügt Hover-Effekte hinzu
     */
    private void addHoverEffects() {
        ratioContainer.setOnMouseEntered(event -> {
            buyBar.setStyle(buyBar.getStyle() + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);");
            sellBar.setStyle(sellBar.getStyle() + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);");
        });
        
        ratioContainer.setOnMouseExited(event -> {
            // Entferne Schatten-Effekt
            String buyStyle = buyBar.getStyle().replace(" -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);", "");
            String sellStyle = sellBar.getStyle().replace(" -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);", "");
            buyBar.setStyle(buyStyle);
            sellBar.setStyle(sellStyle);
        });
    }
}