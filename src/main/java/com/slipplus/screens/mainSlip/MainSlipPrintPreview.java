package com.slipplus.screens.mainSlip;

import com.slipplus.core.StorageManager;
import com.slipplus.models.MainSlip;
import com.slipplus.models.SubSlip;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.printing.PDFPrintable;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.print.PrinterJob;
import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

public class MainSlipPrintPreview {
    
    private Stage stage;
    private MainSlipScreen parentScreen;
    private LocalDate selectedDate;
    private String selectedParty;
    private List<SubSlip> subSlips;
    private MainSlip mainSlip;
    private DecimalFormat moneyFmt = new DecimalFormat("#,##0.00");
    private double fontSize;
    
    public MainSlipPrintPreview(MainSlipScreen parent, LocalDate date, String party, MainSlip slip) {
        this.parentScreen = parent;
        this.selectedDate = date;
        this.selectedParty = party;
        this.mainSlip = slip;
        this.subSlips = StorageManager.getSubSlipsForDateAndParty(date, party);
    }
    
    public void start(Stage stage) {
        this.stage = stage;
        
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        this.fontSize = (screenWidth / 1920.0) * 18;
        if (fontSize < 14) fontSize = 14;
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white;");
        
        // Create preview content
        VBox previewContent = createPreviewContent();
        
        ScrollPane scrollPane = new ScrollPane(previewContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white;");
        root.setCenter(scrollPane);
        
        // Bottom buttons
        HBox buttonArea = new HBox(20);
        buttonArea.setAlignment(Pos.CENTER);
        buttonArea.setPadding(new Insets(20));

        Button printButton = new Button("Print (Enter)");
        printButton.setStyle("-fx-font-size: 16px; -fx-pref-width: 150px; -fx-pref-height: 40px;");
        printButton.setOnAction(e -> printMainSlipAndSubSlips());

        Button saveButton = new Button("Save PDF (S)");
        saveButton.setStyle("-fx-font-size: 16px; -fx-pref-width: 150px; -fx-pref-height: 40px;");
        saveButton.setOnAction(e -> saveAsPDF());

        Button backButton = new Button("Back (ESC)");
        backButton.setStyle("-fx-font-size: 16px; -fx-pref-width: 150px; -fx-pref-height: 40px;");
        backButton.setOnAction(e -> goBack());

        buttonArea.getChildren().addAll(printButton, saveButton, backButton);
        root.setBottom(buttonArea);
        
        Scene scene = new Scene(root, 1600, 900);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                printMainSlipAndSubSlips();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                goBack();
            } else if (e.getCode() == KeyCode.S) {
                saveAsPDF();
            }
        });
        
        stage.setScene(scene);
        stage.setTitle("Print Preview - Main Slip");
        stage.setMaximized(true);
        stage.show();
        
        Platform.runLater(() -> printButton.requestFocus());
    }
    
    private VBox createPreviewContent() {
        VBox content = new VBox(15);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(30));
        
        // Title
        Label titleLabel = new Label("PRINT PREVIEW");
        titleLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold; -fx-text-fill: #333;", fontSize * 1.5));
        content.getChildren().add(titleLabel);
        
        // Party Name (centered, no "PARTY:" label)
        Label partyLabel = new Label(selectedParty);
        partyLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold; -fx-text-fill: black;", fontSize * 1.2));
        partyLabel.setAlignment(Pos.CENTER);
        content.getChildren().add(partyLabel);
        
        // Sub-slip data (centered, no "SUB-SLIP SUMMARY:" label)
        VBox subSlipSummary = new VBox(5);
        subSlipSummary.setAlignment(Pos.CENTER);
        
        double subSlipTotal = 0;
        for (SubSlip slip : subSlips) {
            String summaryText = String.format("%.0f → ₹%s", 
                slip.getMainWeight(), moneyFmt.format(slip.getFinalAmount()));
            Label summaryLabel = new Label(summaryText);
            summaryLabel.setStyle(String.format("-fx-font-size: %.0fpx;", fontSize));
            summaryLabel.setAlignment(Pos.CENTER);
            subSlipSummary.getChildren().add(summaryLabel);
            subSlipTotal += slip.getFinalAmount();
        }
        
        // Sub-slip total (centered, no label)
        Label subTotalLabel = new Label("₹" + moneyFmt.format(subSlipTotal));
        subTotalLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold;", fontSize));
        subTotalLabel.setAlignment(Pos.CENTER);
        subSlipSummary.getChildren().add(subTotalLabel);
        content.getChildren().add(subSlipSummary);
        
        // Operations (centered, no "OPERATIONS:" label)
        if (!mainSlip.getOperations().isEmpty()) {
            VBox operationsBox = new VBox(5);
            operationsBox.setAlignment(Pos.CENTER);
            
            double operationsTotal = 0;
            for (MainSlip.Operation op : mainSlip.getOperations()) {
                String opText = String.format("₹%s %s (%s)", 
                    moneyFmt.format(op.getAmount()), op.getDescription(), op.getOperationType());
                Label opLabel = new Label(opText);
                opLabel.setStyle(String.format("-fx-font-size: %.0fpx;", fontSize));
                opLabel.setAlignment(Pos.CENTER);
                operationsBox.getChildren().add(opLabel);
                
                if ("+".equals(op.getOperationType())) {
                    operationsTotal += op.getAmount();
                } else if ("-".equals(op.getOperationType())) {
                    operationsTotal -= op.getAmount();
                }
            }
            
            // Operations total (centered, no label)
            Label opTotalLabel = new Label("₹" + moneyFmt.format(operationsTotal));
            opTotalLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold;", fontSize));
            opTotalLabel.setAlignment(Pos.CENTER);
            operationsBox.getChildren().add(opTotalLabel);
            content.getChildren().add(operationsBox);
        }
        
        // Final Amount (centered, no label)
        Label finalLabel = new Label("₹" + moneyFmt.format(mainSlip.getTotalAfterOperations()));
        finalLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold; -fx-text-fill: green;", fontSize * 1.3));
        finalLabel.setAlignment(Pos.CENTER);
        content.getChildren().add(finalLabel);
        
        // Separator
        Label separator = new Label("─────────────────────────────────────");
        separator.setStyle(String.format("-fx-font-size: %.0fpx;", fontSize));
        separator.setAlignment(Pos.CENTER);
        content.getChildren().add(separator);
        
        // Sub-slip Details Preview - FULL FORMAT
        Label detailsLabel = new Label("SUB-SLIP DETAILS (will be printed after main slip):");
        detailsLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold;", fontSize));
        content.getChildren().add(detailsLabel);
        
        // Show each sub-slip in full print format
        for (int i = 0; i < subSlips.size(); i++) {
            SubSlip slip = subSlips.get(i);
            
            VBox slipPreview = createSubSlipPreview(slip, i + 1);
            content.getChildren().add(slipPreview);
            
            // Add space between sub-slips
            if (i < subSlips.size() - 1) {
                Label spacer = new Label(" ");
                content.getChildren().add(spacer);
            }
        }
        
        return content;
    }

    private VBox createSubSlipPreview(SubSlip slip, int slipNumber) {
        VBox slipBox = new VBox(3);
        slipBox.setAlignment(Pos.CENTER);
        slipBox.setStyle("-fx-border-color: #ccc; -fx-border-width: 1; -fx-padding: 15; -fx-background-color: #f9f9f9;");
        
        Label slipTitle = new Label("Sub-slip " + slipNumber + ":");
        slipTitle.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold;", fontSize * 0.9));
        slipBox.getChildren().add(slipTitle);
        
        // Add space after title
        Label spacer = new Label(" ");
        slipBox.getChildren().add(spacer);
        
        // Create the exact print format preview
        VBox slipContent = new VBox(2);
        slipContent.setAlignment(Pos.CENTER);
        slipContent.setMaxWidth(300);
        
        // Top line: Price1, Party name (centered), Truck number
        HBox topLine = new HBox();
        topLine.setAlignment(Pos.CENTER);
        topLine.setPrefWidth(280);
        
        Label price1Label = new Label(String.format("%.0f", slip.getPrice1()));
        price1Label.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));
        
        Label partyNameLabel = new Label(slip.getPartyName());
        partyNameLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));
        
        Label truckLabel = new Label(slip.getTruckNumber());
        truckLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));
        
        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(spacer2, javafx.scene.layout.Priority.ALWAYS);
        
        topLine.getChildren().addAll(price1Label, spacer1, partyNameLabel, spacer2, truckLabel);
        slipContent.getChildren().add(topLine);
        
        // Price2 (left aligned, below price1)
        HBox price2Line = new HBox();
        price2Line.setAlignment(Pos.CENTER_LEFT);
        price2Line.setPrefWidth(280);
        
        Label price2Label = new Label(String.format("%.0f", slip.getPrice2()));
        price2Label.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));
        price2Line.getChildren().add(price2Label);
        slipContent.getChildren().add(price2Line);
        
        // Horizontal line
        Label line1 = new Label("_________________________");
        line1.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.6));
        slipContent.getChildren().add(line1);
        
        // Main calculation line: mainWeight - subWeight × calculatedPrice
        HBox calcLine1 = new HBox();
        calcLine1.setAlignment(Pos.CENTER_LEFT);
        calcLine1.setPrefWidth(280);

        String calc1Text = String.format("%.0f - %.0f × %.0f", 
            slip.getMainWeight(), 
            slip.getSubWeights().get(0), 
            slip.getCalculatedPrices().get(0));
        Label calc1Label = new Label(calc1Text);
        calc1Label.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));

        String quality1Text = String.format("%+.0f", slip.getQualityValues().get(0));
        Label quality1Label = new Label(quality1Text);
        quality1Label.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));

        Region spacer3 = new Region();
        HBox.setHgrow(spacer3, javafx.scene.layout.Priority.ALWAYS);

        calcLine1.getChildren().addAll(calc1Label, spacer3, quality1Label);
        slipContent.getChildren().add(calcLine1);

        // Second calculation line: subWeight × calculatedPrice (aligned under first subWeight)
        if (slip.getSubWeights().size() > 1) {
            HBox calcLine2 = new HBox();
            calcLine2.setAlignment(Pos.CENTER_LEFT);
            calcLine2.setPrefWidth(280);
            
            // Calculate spacing to align under first subWeight (after "6500 - ")
            String mainWeightText = String.format("%.0f - ", slip.getMainWeight());
            Label spacingLabel = new Label(mainWeightText);
            spacingLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace; -fx-text-fill: transparent;", fontSize * 0.8));
            
            String calc2Text = String.format("%.0f × %.0f", 
                slip.getSubWeights().get(1), 
                slip.getCalculatedPrices().get(1));
            Label calc2Label = new Label(calc2Text);
            calc2Label.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));
            
            String quality2Text = String.format("%+.0f", slip.getQualityValues().get(1));
            Label quality2Label = new Label(quality2Text);
            quality2Label.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));
            
            Region spacer4 = new Region();
            HBox.setHgrow(spacer4, javafx.scene.layout.Priority.ALWAYS);
            
            calcLine2.getChildren().addAll(spacingLabel, calc2Label, spacer4, quality2Label);
            slipContent.getChildren().add(calcLine2);
        }

        // Third line: subWeight and dustDiscount (aligned under previous subWeight)
        if (slip.getSubWeights().size() > 2) {
            HBox calcLine3 = new HBox();
            calcLine3.setAlignment(Pos.CENTER_LEFT);
            calcLine3.setPrefWidth(280);
            
            // Calculate spacing to align under first subWeight (after "6500 - ")
            String mainWeightText = String.format("%.0f - ", slip.getMainWeight());
            Label spacingLabel2 = new Label(mainWeightText);
            spacingLabel2.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace; -fx-text-fill: transparent;", fontSize * 0.8));
            
            Label subWeight3Label = new Label(String.format("%.0f", slip.getSubWeights().get(2)));
            subWeight3Label.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));
            
            Label dustLabel = new Label(slip.getDustDiscount());
            dustLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));
            
            Region spacer5 = new Region();
            HBox.setHgrow(spacer5, javafx.scene.layout.Priority.ALWAYS);
            
            calcLine3.getChildren().addAll(spacingLabel2, subWeight3Label, spacer5, dustLabel);
            slipContent.getChildren().add(calcLine3);
        }
        
        // Horizontal line
        Label line2 = new Label("_________________________");
        line2.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.6));
        slipContent.getChildren().add(line2);
        
        // Total before GST (centered)
        Label totalBeforeLabel = new Label(String.format("%.0f", slip.getTotalBeforeGst()));
        totalBeforeLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace; -fx-font-weight: bold;", fontSize * 0.9));
        totalBeforeLabel.setAlignment(Pos.CENTER);
        slipContent.getChildren().add(totalBeforeLabel);
        
        // GST (centered)
        Label gstLabel = new Label(String.format("%.0f", slip.getGst()));
        gstLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));
        gstLabel.setAlignment(Pos.CENTER);
        slipContent.getChildren().add(gstLabel);
        
        // Final horizontal line
        Label line3 = new Label("_________________________");
        line3.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.6));
        slipContent.getChildren().add(line3);
        
        // Final amount (centered)
        Label finalLabel = new Label(String.format("%.0f", slip.getFinalAmount()));
        finalLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace; -fx-font-weight: bold;", fontSize));
        finalLabel.setAlignment(Pos.CENTER);
        slipContent.getChildren().add(finalLabel);
        
        slipBox.getChildren().add(slipContent);
        return slipBox;
    }
    
    private void printMainSlipAndSubSlips() {
        try {
            // Show printer selection dialog
            PrintService[] printers = PrintServiceLookup.lookupPrintServices(null, null);
            
            if (printers.length > 0) {
                PrinterJob job = PrinterJob.getPrinterJob();
                
                if (job.printDialog()) {
                    try {
                        // Create single PDF with all slips
                        PDDocument combinedDoc = createCombinedSlipsPDF();
                        job.setPrintable(new PDFPrintable(combinedDoc));
                        job.print();
                        combinedDoc.close();
                        
                        // Show success message
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Print Complete");
                        alert.setHeaderText(null);
                        alert.setContentText("Main slip and all sub-slips printed successfully!");
                        alert.showAndWait();
                        
                        // Go back to main slip screen
                        goBack();
                        
                    } catch (java.awt.print.PrinterException pe) {
                        // Handle printer access denied error
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Printer Access Error");
                        errorAlert.setHeaderText("Cannot access printer");
                        errorAlert.setContentText("Access denied to printer. This may be due to:\n" +
                            "• Printer permissions\n" +
                            "• Printer driver issues\n" +
                            "• Windows security settings\n\n" +
                            "Would you like to save as PDF instead?");
                        
                        ButtonType saveAsPdf = new ButtonType("Save as PDF");
                        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                        errorAlert.getButtonTypes().setAll(saveAsPdf, cancel);
                        
                        errorAlert.showAndWait().ifPresent(response -> {
                            if (response == saveAsPdf) {
                                saveAsPDF();
                            }
                        });
                    }
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("No Printers");
                alert.setHeaderText(null);
                alert.setContentText("No printers found! Would you like to save as PDF instead?");
                
                ButtonType saveAsPdf = new ButtonType("Save as PDF");
                ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(saveAsPdf, cancel);
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == saveAsPdf) {
                        saveAsPDF();
                    }
                });
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Print Error");
            alert.setHeaderText(null);
            alert.setContentText("Error during printing: " + e.getMessage() + 
                "\n\nWould you like to save as PDF instead?");
            
            ButtonType saveAsPdf = new ButtonType("Save as PDF");
            ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(saveAsPdf, cancel);
            
            alert.showAndWait().ifPresent(response -> {
                if (response == saveAsPdf) {
                    saveAsPDF();
                }
            });
        }
    }

    private void saveAsPDF() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save PDF");
            fileChooser.setInitialFileName(selectedParty + "_" + selectedDate.toString() + "_slip.pdf");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );
            
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                PDDocument doc = createCombinedSlipsPDF();
                doc.save(file);
                doc.close();
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("PDF Saved");
                alert.setHeaderText(null);
                alert.setContentText("PDF saved successfully to:\n" + file.getAbsolutePath());
                alert.showAndWait();
                
                goBack();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Error");
            alert.setHeaderText(null);
            alert.setContentText("Error saving PDF: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private PDDocument createCombinedSlipsPDF() throws Exception {
        PDDocument doc = new PDDocument();
        PDType1Font font = PDType1Font.HELVETICA;
        
        PDPage currentPage = new PDPage(PDRectangle.A4);
        doc.addPage(currentPage);
        
        PDRectangle rect = currentPage.getMediaBox();
        float pageW = rect.getWidth();
        float pageH = rect.getHeight();
        
        PDPageContentStream currentCS = new PDPageContentStream(doc, currentPage);
        float currentY = pageH - 56.69f;
        
        // Add main slip content first
        currentY = addMainSlipContent(currentCS, font, pageW, currentY);
        
        // Add each sub-slip to the same page if space allows
        for (int i = 0; i < subSlips.size(); i++) {
            SubSlip subSlip = subSlips.get(i);
            
            // Estimate space needed for sub-slip (approximately 250 points)
            float subSlipHeight = 250f;
            
            // Check if we need a new page
            if (currentY < subSlipHeight + 50f) {
                // Close current content stream
                currentCS.close();
                
                // Create new page
                currentPage = new PDPage(PDRectangle.A4);
                doc.addPage(currentPage);
                currentCS = new PDPageContentStream(doc, currentPage);
                currentY = pageH - 56.69f;
            }
            
            // Add sub-slip content
            currentY = addSubSlipContent(currentCS, font, subSlip, pageW, currentY);
            currentY -= 30f; // Reduced space between sub-slips
        }
        
        // Close final content stream
        currentCS.close();
        
        return doc;
    }

    private float addMainSlipContent(PDPageContentStream cs, PDType1Font font, float pageW, float startY) throws Exception {
        float y = startY;
        
        // Party Name (centered)
        cs.setFont(font, 16f);
        String partyText = selectedParty;
        float partyWidth = font.getStringWidth(partyText) / 1000f * 16f;
        cs.beginText();
        cs.newLineAtOffset((pageW - partyWidth) / 2f, y);
        cs.showText(partyText);
        cs.endText();
        y -= 25f; // Reduced spacing
        
        // Sub-slip data (centered)
        cs.setFont(font, 12f);
        double subSlipTotal = 0;
        for (SubSlip slip : subSlips) {
            String summaryText = String.format("%.0f -> Rs.%s", 
                slip.getMainWeight(), moneyFmt.format(slip.getFinalAmount()));
            float summaryWidth = font.getStringWidth(summaryText) / 1000f * 12f;
            cs.beginText();
            cs.newLineAtOffset((pageW - summaryWidth) / 2f, y);
            cs.showText(summaryText);
            cs.endText();
            y -= 12f; // Reduced spacing
            subSlipTotal += slip.getFinalAmount();
        }
        
        // Sub-slip total (centered)
        cs.setFont(font, 14f);
        String subTotalText = "Rs." + moneyFmt.format(subSlipTotal);
        float subTotalWidth = font.getStringWidth(subTotalText) / 1000f * 14f;
        cs.beginText();
        cs.newLineAtOffset((pageW - subTotalWidth) / 2f, y);
        cs.showText(subTotalText);
        cs.endText();
        y -= 20f; // Reduced spacing
        
        // Operations (centered)
        if (!mainSlip.getOperations().isEmpty()) {
            cs.setFont(font, 12f);
            double operationsTotal = 0;
            for (MainSlip.Operation op : mainSlip.getOperations()) {
                String opText = String.format("Rs.%s %s (%s)", 
                    moneyFmt.format(op.getAmount()), op.getDescription(), op.getOperationType());
                float opWidth = font.getStringWidth(opText) / 1000f * 12f;
                cs.beginText();
                cs.newLineAtOffset((pageW - opWidth) / 2f, y);
                cs.showText(opText);
                cs.endText();
                y -= 12f; // Reduced spacing
                
                if ("+".equals(op.getOperationType())) {
                    operationsTotal += op.getAmount();
                } else if ("-".equals(op.getOperationType())) {
                    operationsTotal -= op.getAmount();
                }
            }
            
            // Operations total (centered)
            cs.setFont(font, 14f);
            String opTotalText = "Rs." + moneyFmt.format(operationsTotal);
            float opTotalWidth = font.getStringWidth(opTotalText) / 1000f * 14f;
            cs.beginText();
            cs.newLineAtOffset((pageW - opTotalWidth) / 2f, y);
            cs.showText(opTotalText);
            cs.endText();
            y -= 20f; // Reduced spacing
        }
        
        // Final amount (centered)
        cs.setFont(font, 16f);
        String finalText = "Rs." + moneyFmt.format(mainSlip.getTotalAfterOperations());
        float finalWidth = font.getStringWidth(finalText) / 1000f * 16f;
        cs.beginText();
        cs.newLineAtOffset((pageW - finalWidth) / 2f, y);
        cs.showText(finalText);
        cs.endText();
        y -= 40f; // Space before sub-slips
        
        return y;
    }

    private float addSubSlipContent(PDPageContentStream cs, PDType1Font font, SubSlip subSlip, float pageW, float startY) throws Exception {
        float slipWidth = 300f;
        float leftMargin = (pageW - slipWidth) / 2f;
        float y = startY;
        
        // Top line: Price1 (left), Party name (center), Truck number (right)
        cs.setFont(font, 14f);
        
        // Price1 (left)
        cs.beginText();
        cs.newLineAtOffset(leftMargin, y);
        cs.showText(String.format("%.0f", subSlip.getPrice1()));
        cs.endText();
        
        // Party name (centered)
        String partyText = subSlip.getPartyName();
        float partyWidth = font.getStringWidth(partyText) / 1000f * 14f;
        cs.beginText();
        cs.newLineAtOffset(leftMargin + (slipWidth - partyWidth) / 2f, y);
        cs.showText(partyText);
        cs.endText();
        
        // Truck number (right)
        cs.beginText();
        cs.newLineAtOffset(leftMargin + slipWidth - 80f, y);
        cs.showText(subSlip.getTruckNumber());
        cs.endText();
        y -= 20f;
        
        // Price2 (left aligned, below price1)
        cs.beginText();
        cs.newLineAtOffset(leftMargin, y);
        cs.showText(String.format("%.0f", subSlip.getPrice2()));
        cs.endText();
        y -= 20f;
        
        // Horizontal line
        cs.setLineWidth(1f);
        cs.moveTo(leftMargin, y);
        cs.lineTo(leftMargin + slipWidth - 80f, y);
        cs.stroke();
        y -= 20f;
        
        // Main calculation lines
        cs.beginText();
        cs.newLineAtOffset(leftMargin, y);
        cs.showText(String.format("%.0f - %.0f × %.0f", 
            subSlip.getMainWeight(), 
            subSlip.getSubWeights().get(0), 
            subSlip.getCalculatedPrices().get(0)));
        cs.endText();
        
        String qualityText = String.format("%+.0f", subSlip.getQualityValues().get(0));
        cs.beginText();
        cs.newLineAtOffset(leftMargin + slipWidth - 80f, y);
        cs.showText(qualityText);
        cs.endText();
        y -= 20f;
        
        // Additional calculation lines if needed
        if (subSlip.getSubWeights().size() > 1) {
            String mainWeightSpacing = String.format("%.0f - ", subSlip.getMainWeight());
            float spacingWidth = font.getStringWidth(mainWeightSpacing) / 1000f * 14f;
            
            cs.beginText();
            cs.newLineAtOffset(leftMargin + spacingWidth, y);
            cs.showText(String.format("%.0f × %.0f", 
                subSlip.getSubWeights().get(1), 
                subSlip.getCalculatedPrices().get(1)));
            cs.endText();
            
            String quality2Text = String.format("%+.0f", subSlip.getQualityValues().get(1));
            cs.beginText();
            cs.newLineAtOffset(leftMargin + slipWidth - 80f, y);
            cs.showText(quality2Text);
            cs.endText();
            y -= 20f;
        }
        
        if (subSlip.getSubWeights().size() > 2) {
            String mainWeightSpacing = String.format("%.0f - ", subSlip.getMainWeight());
            float spacingWidth = font.getStringWidth(mainWeightSpacing) / 1000f * 14f;
            
            cs.beginText();
            cs.newLineAtOffset(leftMargin + spacingWidth, y);
            cs.showText(String.format("%.0f", subSlip.getSubWeights().get(2)));
            cs.endText();
            
            cs.beginText();
            cs.newLineAtOffset(leftMargin + slipWidth - 80f, y);
            cs.showText(subSlip.getDustDiscount());
            cs.endText();
            y -= 20f;
        }
        
        // Bottom section
        cs.setLineWidth(2f);
        cs.moveTo(leftMargin, y);
        cs.lineTo(leftMargin + slipWidth - 80f, y);
        cs.stroke();
        y -= 25f;
        
        // Total before GST (centered)
        cs.setFont(font, 16f);
        String totalText = String.format("%.0f", subSlip.getTotalBeforeGst());
        float totalWidth = font.getStringWidth(totalText) / 1000f * 16f;
        cs.beginText();
        cs.newLineAtOffset(leftMargin + (slipWidth - totalWidth) / 2f, y);
        cs.showText(totalText);
        cs.endText();
        y -= 25f;
        
        // GST (centered)
        cs.setFont(font, 14f);
        String gstText = String.format("%.0f", subSlip.getGst());
        float gstWidth = font.getStringWidth(gstText) / 1000f * 14f;
        cs.beginText();
        cs.newLineAtOffset(leftMargin + (slipWidth - gstWidth) / 2f, y);
        cs.showText(gstText);
        cs.endText();
        y -= 20f;
        
        // Final line and amount
        cs.setLineWidth(2f);
        cs.moveTo(leftMargin, y);
        cs.lineTo(leftMargin + slipWidth - 80f, y);
        cs.stroke();
        y -= 25f;
        
        cs.setFont(font, 16f);
        String finalText = String.format("%.0f", subSlip.getFinalAmount());
        float finalWidth = font.getStringWidth(finalText) / 1000f * 16f;
        cs.beginText();
        cs.newLineAtOffset(leftMargin + (slipWidth - finalWidth) / 2f, y);
        cs.showText(finalText);
        cs.endText();
        y -= 30f;
        
        return y;
    }
    
    private void goBack() {
        // Reset popup flag when going back
        parentScreen.resetPopupFlag();
        parentScreen.start(stage);
    }
}




