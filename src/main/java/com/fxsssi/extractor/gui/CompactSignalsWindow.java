package com.fxsssi.extractor.gui;

import com.fxssi.extractor.storage.CurrencyPairDataManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CompactSignalsWindow {

    private final CurrencyPairDataManager dataManager;
    private Stage stage;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Button startButton;
    private Button closeButton;

    public CompactSignalsWindow(Stage parentStage, CurrencyPairDataManager dataManager) {
        this.dataManager = dataManager;
        createWindow(parentStage);
    }

    private void createWindow(Stage parentStage) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(parentStage);
        stage.setTitle("Signale komprimieren (1H)");

        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.setPrefWidth(400);

        Label titleLabel = new Label("Daten komprimieren");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label infoLabel = new Label("Dieser Vorgang geht durch alle Währungspaar-Dateien und\nreduziert die Daten auf maximal 1 Signal pro Stunde.");
        infoLabel.setWrapText(true);
        infoLabel.setAlignment(Pos.CENTER);
        
        Label directoryLabel = new Label("Verzeichnis:\n" + dataManager.getCurrencyDataPath().toAbsolutePath().toString());
        directoryLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666666;");
        directoryLabel.setWrapText(true);
        directoryLabel.setAlignment(Pos.CENTER);

        statusLabel = new Label("Bereit zum Starten.");
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(350);

        startButton = new Button("Starten");
        startButton.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        startButton.setOnAction(e -> startCompaction());

        closeButton = new Button("Schließen");
        closeButton.setOnAction(e -> stage.close());

        layout.getChildren().addAll(titleLabel, infoLabel, directoryLabel, statusLabel, progressBar, startButton, closeButton);

        Scene scene = new Scene(layout);
        stage.setScene(scene);
        stage.setResizable(false);
    }

    public void show() {
        stage.showAndWait();
    }

    private void startCompaction() {
        startButton.setDisable(true);
        closeButton.setDisable(true);
        statusLabel.setText("Starte Komprimierung...");
        progressBar.setProgress(-1); // Indeterminate to start with

        Task<String> compactionTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return dataManager.compactAllDataToHourly(
                    (current, total) -> {
                        updateProgress(current, total);
                    },
                    (message) -> {
                        updateMessage(message);
                    }
                );
            }
        };

        compactionTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            statusLabel.setText(newMsg);
        });

        compactionTask.progressProperty().addListener((obs, oldProg, newProg) -> {
            progressBar.setProgress(newProg.doubleValue());
        });

        compactionTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                String report = compactionTask.getValue();
                statusLabel.setText("✅ Komprimierung erfolgreich abgeschlossen!");
                progressBar.setProgress(1.0);
                closeButton.setDisable(false);
                closeButton.setText("Fertig");
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Komprimierungs-Bericht");
                alert.setHeaderText("Zusammenfassung der komprimierten Daten");
                
                TextArea textArea = new TextArea(report);
                textArea.setEditable(false);
                textArea.setWrapText(false);
                textArea.setFont(Font.font("Monospaced", 12));
                
                alert.getDialogPane().setContent(textArea);
                alert.getDialogPane().setPrefWidth(450);
                alert.showAndWait();
            });
        });

        compactionTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                statusLabel.setText("❌ Fehler bei der Komprimierung aufgetreten.");
                progressBar.setStyle("-fx-accent: red;");
                closeButton.setDisable(false);
                Throwable exception = compactionTask.getException();
                if(exception != null) {
                    exception.printStackTrace();
                }
            });
        });

        Thread thread = new Thread(compactionTask);
        thread.setDaemon(true);
        thread.start();
    }
}
