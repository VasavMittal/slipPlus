package com.slipplus.screens.mainSlip;

import com.slipplus.core.StorageManager;
import com.slipplus.models.MainSlip;
import com.slipplus.models.SubSlip;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
        double screenHeight = Screen.getPrimary().getBounds().getHeight();
        this.fontSize = (screenWidth / 1920.0) * 18;
        if (fontSize < 14) fontSize = 14;
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white;");
        
        // Top buttons (moved from bottom)
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
        root.setTop(buttonArea);
        
        // Create preview content
        VBox previewContent = createPreviewContent();
        
        ScrollPane scrollPane = new ScrollPane(previewContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white;");
        root.setCenter(scrollPane);
        
        // Use same sizing as MainSlipScreen
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
        VBox content = new VBox(10);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white;");
        
        // Title
        Label titleLabel = new Label("PRINT PREVIEW");
        titleLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold;", fontSize * 1.2));
        content.getChildren().add(titleLabel);
        
        // Party Name
        Label partyLabel = new Label(mainSlip.getPartyName());
        partyLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold;", fontSize));
        content.getChildren().add(partyLabel);
        
        // Sub-slip list with aligned columns using GridPane for perfect alignment
        GridPane slipGrid = new GridPane();
        slipGrid.setAlignment(Pos.CENTER);
        slipGrid.setHgap(10);
        slipGrid.setVgap(5);
        
        int row = 0;
        for (SubSlip slip : subSlips) {
            // Weight column - right aligned
            Label weightLabel = new Label(String.format("%.0f", slip.getMainWeight()));
            weightLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, fontSize * 0.9));
            weightLabel.setMinWidth(80);
            weightLabel.setAlignment(Pos.CENTER_RIGHT);
            GridPane.setHalignment(weightLabel, HPos.RIGHT);
            
            // Arrow column - center aligned
            Label arrowLabel = new Label("->");
            arrowLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, fontSize * 0.9));
            arrowLabel.setMinWidth(30);
            arrowLabel.setAlignment(Pos.CENTER);
            GridPane.setHalignment(arrowLabel, HPos.CENTER);
            
            // Price column - left aligned
            Label priceLabel = new Label(String.format("%.0f", slip.getFinalAmount()));
            priceLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, fontSize * 0.9));
            priceLabel.setMinWidth(100);
            priceLabel.setAlignment(Pos.CENTER_LEFT);
            GridPane.setHalignment(priceLabel, HPos.LEFT);
            
            slipGrid.add(weightLabel, 0, row);
            slipGrid.add(arrowLabel, 1, row);
            slipGrid.add(priceLabel, 2, row);
            row++;
        }
        content.getChildren().add(slipGrid);
        
        // Separator line
        Label line1 = new Label("─".repeat(35));
        line1.setFont(Font.font("Consolas", FontWeight.NORMAL, fontSize * 0.8));
        content.getChildren().add(line1);
        
        // Total
        double total = subSlips.stream().mapToDouble(SubSlip::getFinalAmount).sum();
        Label totalLabel = new Label(String.format("%,.0f", total));
        totalLabel.setFont(Font.font("Consolas", FontWeight.BOLD, fontSize));
        content.getChildren().add(totalLabel);
        
        // Operations section
        if (mainSlip.getOperations() != null && !mainSlip.getOperations().isEmpty()) {
            Label line2 = new Label("─".repeat(35));
            line2.setFont(Font.font("Consolas", FontWeight.NORMAL, fontSize * 0.8));
            content.getChildren().add(line2);
            
            // Operations with GridPane for alignment
            GridPane opGrid = new GridPane();
            opGrid.setAlignment(Pos.CENTER);
            opGrid.setHgap(10);
            opGrid.setVgap(3);
            
            int opRow = 0;
            for (MainSlip.Operation op : mainSlip.getOperations()) {
                String color = op.getOperationType().equals("-") ? "#cc0000" : "#006600";
                
                // Amount column - right aligned
                Label amountLabel = new Label(String.format("%.0f", op.getAmount()));
                amountLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, fontSize * 0.9));
                amountLabel.setStyle("-fx-text-fill: " + color + ";");
                amountLabel.setMinWidth(80);
                amountLabel.setAlignment(Pos.CENTER_RIGHT);
                GridPane.setHalignment(amountLabel, HPos.RIGHT);
                
                // Description column - left aligned
                Label descLabel = new Label(op.getDescription());
                descLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, fontSize * 0.9));
                descLabel.setStyle("-fx-text-fill: " + color + ";");
                descLabel.setMinWidth(60);
                descLabel.setAlignment(Pos.CENTER_LEFT);
                GridPane.setHalignment(descLabel, HPos.LEFT);
                
                // Type column
                Label typeLabel = new Label("(" + op.getOperationType() + ")");
                typeLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, fontSize * 0.9));
                typeLabel.setStyle("-fx-text-fill: " + color + ";");
                typeLabel.setMinWidth(30);
                typeLabel.setAlignment(Pos.CENTER_LEFT);
                GridPane.setHalignment(typeLabel, HPos.LEFT);
                
                opGrid.add(amountLabel, 0, opRow);
                opGrid.add(descLabel, 1, opRow);
                opGrid.add(typeLabel, 2, opRow);
                opRow++;
            }
            content.getChildren().add(opGrid);
        }
        
        // Final separator
        Label line3 = new Label("═".repeat(35));
        line3.setFont(Font.font("Consolas", FontWeight.NORMAL, fontSize * 0.8));
        content.getChildren().add(line3);
        
        // Final amount
        double finalAmount = calculateFinalAmount(total);
        String finalColor = finalAmount < 0 ? "#cc0000" : "#006600";
        Label finalLabel = new Label(String.format("%,.0f", finalAmount));
        finalLabel.setFont(Font.font("Consolas", FontWeight.BOLD, fontSize * 1.1));
        finalLabel.setStyle("-fx-text-fill: " + finalColor + ";");
        content.getChildren().add(finalLabel);
        
        Label line4 = new Label("═".repeat(35));
        line4.setFont(Font.font("Consolas", FontWeight.NORMAL, fontSize * 0.8));
        content.getChildren().add(line4);
        
        // Sub-slip details section
        Label detailsTitle = new Label("SUB-SLIP DETAILS (will be printed after main slip):");
        detailsTitle.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold;", fontSize * 0.8));
        content.getChildren().add(detailsTitle);
        
        // Individual sub-slip previews
        HBox subSlipPreviews = new HBox(20);
        subSlipPreviews.setAlignment(Pos.CENTER);
        
        int slipNumber = 1;
        for (SubSlip slip : subSlips) {
            VBox slipPreview = createSubSlipPreview(slip, slipNumber++);
            subSlipPreviews.getChildren().add(slipPreview);
        }
        
        content.getChildren().add(subSlipPreviews);
        
        return content;
    }

    private VBox createSubSlipPreview(SubSlip slip, int slipNumber) {

        VBox slipBox = new VBox(3);
        slipBox.setAlignment(Pos.CENTER);
        slipBox.setStyle("-fx-border-color: #ccc; -fx-border-width: 1; -fx-padding: 20; -fx-background-color: #f9f9f9;");
        slipBox.setPrefHeight(400);
        slipBox.setMinHeight(400);

        Label slipTitle = new Label("Sub-slip " + slipNumber + ":");
        slipTitle.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold;", fontSize * 0.9));
        slipBox.getChildren().add(slipTitle);

        slipBox.getChildren().add(new Label(" "));

        VBox slipContent = new VBox(3);
        slipContent.setAlignment(Pos.CENTER);
        slipContent.setMaxWidth(250);

        /* ================= TOP LINE ================= */

        HBox topLine = new HBox();
        topLine.setAlignment(Pos.CENTER);
        topLine.setPrefWidth(230);

        Label price1Label = new Label(String.format("%.0f", slip.getPrice1()));
        price1Label.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));

        Label partyNameLabel = new Label(slip.getPartyName());
        partyNameLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));

        Label truckLabel = new Label(slip.getTruckNumber());
        truckLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));

        Region s1 = new Region();
        Region s2 = new Region();
        HBox.setHgrow(s1, Priority.ALWAYS);
        HBox.setHgrow(s2, Priority.ALWAYS);

        topLine.getChildren().addAll(price1Label, s1, partyNameLabel, s2, truckLabel);
        slipContent.getChildren().add(topLine);

        /* ================= PRICE 2 ================= */

        HBox price2Line = new HBox();
        price2Line.setAlignment(Pos.CENTER_LEFT);
        price2Line.setPrefWidth(230);

        Label price2Label = new Label(String.format("%.0f", slip.getPrice2()));
        price2Label.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));

        price2Line.getChildren().add(price2Label);
        slipContent.getChildren().add(price2Line);

        /* ================= LINE ================= */

        slipContent.getChildren().add(
            new Label("____________________")
        );

        /* ================= FIRST CALC LINE ================= */

        HBox calcLine1 = new HBox();
        calcLine1.setAlignment(Pos.CENTER_LEFT);
        calcLine1.setPrefWidth(230);

        Label calc1Label = new Label(String.format("%.0f - %.0f × %.0f",
                slip.getMainWeight(),
                slip.getSubWeights().get(0),
                slip.getCalculatedPrices().get(0)));

        calc1Label.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));

        Label quality1Label = new Label(String.format("%+.0f", slip.getQualityValues().get(0)));
        quality1Label.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));

        Region s3 = new Region();
        HBox.setHgrow(s3, Priority.ALWAYS);

        calcLine1.getChildren().addAll(calc1Label, s3, quality1Label);
        slipContent.getChildren().add(calcLine1);

        /* ================= REMAINING SUB-WEIGHTS (LAST = DUST) ================= */

        int count = slip.getSubWeights().size();

        for (int i = 1; i < count; i++) {

            boolean isDust = (i == count - 1);

            HBox calcLine = new HBox();
            calcLine.setAlignment(Pos.CENTER_LEFT);
            calcLine.setPrefWidth(230);

            // spacing to align under "xxxx - "
            Label spacingLabel = new Label(String.format("%.0f - ", slip.getMainWeight()));
            spacingLabel.setStyle(String.format(
                    "-fx-font-size: %.0fpx; -fx-font-family: monospace; -fx-text-fill: transparent;",
                    fontSize * 0.8
            ));

            Label leftLabel;
            if (isDust) {
                leftLabel = new Label(String.format("%.0f", slip.getSubWeights().get(i)));
            } else {
                leftLabel = new Label(String.format("%.0f × %.0f",
                        slip.getSubWeights().get(i),
                        slip.getCalculatedPrices().get(i)));
            }

            leftLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));

            Label rightLabel;
            if (isDust) {
                rightLabel = new Label(slip.getDustDiscount());   // N or value
            } else {
                rightLabel = new Label(String.format("%+.0f", slip.getQualityValues().get(i)));
            }

            rightLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));

            Region s4 = new Region();
            HBox.setHgrow(s4, Priority.ALWAYS);

            calcLine.getChildren().addAll(spacingLabel, leftLabel, s4, rightLabel);
            slipContent.getChildren().add(calcLine);
        }

        /* ================= TOTAL / GST / FINAL ================= */

        slipContent.getChildren().add(new Label("____________________"));

        Label totalBefore = new Label(String.format("%.0f", slip.getTotalBeforeGst()));
        totalBefore.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace; -fx-font-weight: bold;", fontSize * 0.9));
        slipContent.getChildren().add(totalBefore);

        Label gstLabel = new Label(String.format("%.0f", slip.getGst()));
        gstLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace;", fontSize * 0.8));
        slipContent.getChildren().add(gstLabel);

        slipContent.getChildren().add(new Label("____________________"));

        Label finalLabel = new Label(String.format("%.0f", slip.getFinalAmount()));
        finalLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-family: monospace; -fx-font-weight: bold;", fontSize));
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
                        PDDocument combinedDoc = createSlipsPDF(PrintMode.INDIVIDUAL);
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
                PDDocument doc = createSlipsPDF( PrintMode.INDIVIDUAL);
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
    
    private PDDocument createSlipsPDF(PrintMode mode) throws Exception {

        PDDocument doc = new PDDocument();
        PDType1Font font = PDType1Font.HELVETICA;

        if (mode == PrintMode.COMBINED) {
            // ==============================
            // EXISTING COMBINED BEHAVIOR
            // ==============================
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDRectangle rect = page.getMediaBox();
            float pageW = rect.getWidth();
            float pageH = rect.getHeight();

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            float y = pageH - 56.69f;

            // Main slip first
            y = addMainSlipContent(cs, font, pageW, y);

            // Then sub-slips with auto page break
            for (SubSlip slip : subSlips) {

                float needed = calculateSubSlipHeight(slip);
                if (y < needed + 30f) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = pageH - 56.69f;
                }

                y = addSubSlipContent(cs, font, slip, pageW, y);
                y -= 15f;
            }

            cs.close();
        }

        else if (mode == PrintMode.INDIVIDUAL) {
            // ==============================
            // NEW INDIVIDUAL PAGE MODE
            // ==============================

            // ---- PAGE 1: MAIN SLIP ----
            PDPage mainPage = new PDPage(PDRectangle.A4);
            doc.addPage(mainPage);

            PDRectangle rect = mainPage.getMediaBox();
            float pageW = rect.getWidth();
            float pageH = rect.getHeight();

            PDPageContentStream cs = new PDPageContentStream(doc, mainPage);
            float y = pageH - 56.69f;

            addMainSlipContent(cs, font, pageW, y);
            cs.close();

            // ---- ONE PAGE PER SUB-SLIP ----
            for (SubSlip slip : subSlips) {

                PDPage subPage = new PDPage(PDRectangle.A4);
                doc.addPage(subPage);

                PDPageContentStream subCS =
                        new PDPageContentStream(doc, subPage);

                float subY = pageH - 56.69f;
                addSubSlipContent(subCS, font, slip, pageW, subY);

                subCS.close();
            }
        }

        return doc;
    }


    private float calculateSubSlipHeight(SubSlip subSlip) {
        float height = 0f; // Remove base spacing
        
        // Top line (price1, party, truck)
        height += 18f;
        
        // Price2 line
        height += 18f;
        
        // Horizontal line
        height += 8f; // Reduced
        
        // Main calculation line
        height += 18f;
        
        // Additional calculation lines based on subWeights size
        if (subSlip.getSubWeights().size() > 1) {
            height += 18f;
        }
        if (subSlip.getSubWeights().size() > 2) {
            height += 18f;
        }
        
        // Bottom horizontal line
        height += 8f; // Reduced
        
        // Total before GST
        height += 20f;
        
        // GST line
        height += 18f;
        
        // Final horizontal line
        height += 8f; // Reduced
        
        // Final amount
        height += 25f;
        
        return height; // Total: ~130-165f instead of ~200f
    }

    private float addMainSlipContent(
            PDPageContentStream cs,
            PDType1Font font,
            float pageW,
            float startY
    ) throws Exception {

        float y = startY;

        // =========================
        // FONT SIZES
        // =========================
        float bodyFont = 12f;
        float titleFont = 16f;

        // =========================
        // COLUMN WIDTHS (REAL)
        // =========================
        float weightColWidth = 60f;   // enough for 5 digits
        float arrowColWidth  = 30f;
        float amountColWidth = 80f;

        float contentWidth = weightColWidth + arrowColWidth + amountColWidth;

        // TRUE CENTERING
        float baseX = (pageW - contentWidth) / 2f;

        float colWeightRightX = baseX + weightColWidth;
        float colArrowX       = colWeightRightX + 10f;
        float colAmountX      = colArrowX + arrowColWidth;

        // Operations reuse same columns
        float colOpAmtRightX = colWeightRightX;
        float colOpDescX     = colArrowX;
        float colOpSignX     = colAmountX + 40f;

        // =========================
        // PARTY NAME (CENTERED)
        // =========================
        cs.setFont(font, titleFont);
        String party = selectedParty;
        float partyW = font.getStringWidth(party) / 1000f * titleFont;

        cs.beginText();
        cs.newLineAtOffset((pageW - partyW) / 2f, y);
        cs.showText(party);
        cs.endText();

        y -= 30f;

        // =========================
        // SUB SLIP ROWS
        // =========================
        cs.setFont(font, bodyFont);
        double subTotal = 0;

        for (SubSlip slip : subSlips) {

            // Weight (RIGHT aligned)
            String w = String.format("%.0f", slip.getMainWeight());
            float wW = font.getStringWidth(w) / 1000f * bodyFont;

            cs.beginText();
            cs.newLineAtOffset(colWeightRightX - wW, y);
            cs.showText(w);
            cs.endText();

            // Arrow (CENTER)
            cs.beginText();
            cs.newLineAtOffset(colArrowX, y);
            cs.showText("->");
            cs.endText();

            // Amount (LEFT aligned)
            cs.beginText();
            cs.newLineAtOffset(colAmountX, y);
            cs.showText(String.format("%.0f", slip.getFinalAmount()));
            cs.endText();

            y -= 16f;
            subTotal += slip.getFinalAmount();
        }

        // =========================
        // SUB TOTAL
        // =========================
        y -= 4f;
        cs.moveTo(baseX, y);
        cs.lineTo(baseX + contentWidth, y);
        cs.stroke();
        y -= 18f;

        cs.setFont(font, 14f);
        String st = String.format("%.0f", subTotal);
        float stW = font.getStringWidth(st) / 1000f * 14f;

        cs.beginText();
        cs.newLineAtOffset((pageW - stW) / 2f, y);
        cs.showText(st);
        cs.endText();

        y -= 22f;
        cs.moveTo(baseX, y);
        cs.lineTo(baseX + contentWidth, y);
        cs.stroke();
        y -= 24f;

        // =========================
        // OPERATIONS
        // =========================
        cs.setFont(font, bodyFont);
        if (mainSlip.getOperations() != null) {
            for (MainSlip.Operation op : mainSlip.getOperations()) {

                String amt = String.format("%.0f", op.getAmount());
                float aW = font.getStringWidth(amt) / 1000f * bodyFont;

                cs.beginText();
                cs.newLineAtOffset(colOpAmtRightX - aW, y);
                cs.showText(amt);
                cs.endText();

                cs.beginText();
                cs.newLineAtOffset(colOpDescX, y);
                cs.showText(op.getDescription());
                cs.endText();

                cs.beginText();
                cs.newLineAtOffset(colOpSignX, y);
                cs.showText("(-)");
                cs.endText();

                y -= 16f;
            }
        }

        // =========================
        // FINAL TOTAL
        // =========================
        y -= 8f;
        cs.setLineWidth(2f);
        cs.moveTo(baseX, y);
        cs.lineTo(baseX + contentWidth, y);
        cs.stroke();
        y -= 18f;

        cs.setFont(font, titleFont);
        String finalTxt = String.format("%.0f", mainSlip.getTotalAfterOperations());
        float fW = font.getStringWidth(finalTxt) / 1000f * titleFont;

        cs.beginText();
        cs.newLineAtOffset((pageW - fW) / 2f, y);
        cs.showText(finalTxt);
        cs.endText();

        y -= 18f;
        cs.moveTo(baseX, y);
        cs.lineTo(baseX + contentWidth, y);
        cs.stroke();

        y -= 40f;
        return y;
    }

    private float addSubSlipContent(
            PDPageContentStream cs,
            PDType1Font font,
            SubSlip subSlip,
            float pageW,
            float startY
    ) throws Exception {

        float slipWidth = 300f;
        float leftMargin = (pageW - slipWidth) / 2f;
        float y = startY;

        /* ================= TOP LINE ================= */

        cs.setFont(font, 12f);

        // Price1 (left)
        cs.beginText();
        cs.newLineAtOffset(leftMargin, y);
        cs.showText(String.format("%.0f", subSlip.getPrice1()));
        cs.endText();

        // Party name (center)
        String partyText = subSlip.getPartyName();
        float partyWidth = font.getStringWidth(partyText) / 1000f * 12f;
        cs.beginText();
        cs.newLineAtOffset(leftMargin + (slipWidth - partyWidth) / 2f, y);
        cs.showText(partyText);
        cs.endText();

        // Truck number (right)
        String truckText = subSlip.getTruckNumber();
        float truckWidth = font.getStringWidth(truckText) / 1000f * 12f;
        cs.beginText();
        cs.newLineAtOffset(leftMargin + slipWidth - truckWidth, y);
        cs.showText(truckText);
        cs.endText();

        y -= 18f;

        /* ================= PRICE 2 ================= */

        cs.beginText();
        cs.newLineAtOffset(leftMargin, y);
        cs.showText(String.format("%.0f", subSlip.getPrice2()));
        cs.endText();

        y -= 18f;

        /* ================= LINE ================= */

        cs.setLineWidth(1f);
        cs.moveTo(leftMargin, y);
        cs.lineTo(leftMargin + slipWidth, y);
        cs.stroke();

        y -= 18f;

        /* ================= FIRST CALC LINE ================= */

        String calc1Text = String.format("%.0f - %.0f × %.0f",
                subSlip.getMainWeight(),
                subSlip.getSubWeights().get(0),
                subSlip.getCalculatedPrices().get(0));

        cs.beginText();
        cs.newLineAtOffset(leftMargin, y);
        cs.showText(calc1Text);
        cs.endText();

        String quality1Text = String.format("%+.0f", subSlip.getQualityValues().get(0));
        float q1Width = font.getStringWidth(quality1Text) / 1000f * 12f;

        cs.beginText();
        cs.newLineAtOffset(leftMargin + slipWidth - q1Width, y);
        cs.showText(quality1Text);
        cs.endText();

        y -= 18f;

        /* ================= REMAINING SUB-WEIGHTS (LAST = DUST) ================= */

        int count = subSlip.getSubWeights().size();
        String mainWeightSpacing = String.format("%.0f - ", subSlip.getMainWeight());
        float spacingWidth = font.getStringWidth(mainWeightSpacing) / 1000f * 12f;

        for (int i = 1; i < count; i++) {

            boolean isDust = (i == count - 1);

            // LEFT SIDE
            String leftText;
            if (isDust) {
                // dust → only weight
                leftText = String.format("%.0f", subSlip.getSubWeights().get(i));
            } else {
                leftText = String.format("%.0f × %.0f",
                        subSlip.getSubWeights().get(i),
                        subSlip.getCalculatedPrices().get(i));
            }

            cs.beginText();
            cs.newLineAtOffset(leftMargin + spacingWidth, y);
            cs.showText(leftText);
            cs.endText();

            // RIGHT SIDE
            String rightText;
            if (isDust) {
                rightText = subSlip.getDustDiscount(); // N or value
            } else {
                rightText = String.format("%+.0f", subSlip.getQualityValues().get(i));
            }

            float rightWidth = font.getStringWidth(rightText) / 1000f * 12f;
            cs.beginText();
            cs.newLineAtOffset(leftMargin + slipWidth - rightWidth, y);
            cs.showText(rightText);
            cs.endText();

            y -= 18f;
        }

        /* ================= BOTTOM LINE ================= */

        cs.setLineWidth(2f);
        cs.moveTo(leftMargin, y);
        cs.lineTo(leftMargin + slipWidth, y);
        cs.stroke();

        y -= 20f;

        /* ================= TOTAL BEFORE GST ================= */

        cs.setFont(font, 14f);
        String totalText = String.format("%.0f", subSlip.getTotalBeforeGst());
        float totalWidth = font.getStringWidth(totalText) / 1000f * 14f;

        cs.beginText();
        cs.newLineAtOffset(leftMargin + (slipWidth - totalWidth) / 2f, y);
        cs.showText(totalText);
        cs.endText();

        y -= 20f;

        /* ================= GST ================= */

        cs.setFont(font, 12f);
        String gstText = String.format("%.0f", subSlip.getGst());
        float gstWidth = font.getStringWidth(gstText) / 1000f * 12f;

        cs.beginText();
        cs.newLineAtOffset(leftMargin + (slipWidth - gstWidth) / 2f, y);
        cs.showText(gstText);
        cs.endText();

        y -= 18f;

        /* ================= FINAL LINE ================= */

        cs.setLineWidth(2f);
        cs.moveTo(leftMargin, y);
        cs.lineTo(leftMargin + slipWidth, y);
        cs.stroke();

        y -= 20f;

        /* ================= FINAL AMOUNT ================= */

        cs.setFont(font, 16f);
        String finalText = String.format("%.0f", subSlip.getFinalAmount());
        float finalWidth = font.getStringWidth(finalText) / 1000f * 16f;

        cs.beginText();
        cs.newLineAtOffset(leftMargin + (slipWidth - finalWidth) / 2f, y);
        cs.showText(finalText);
        cs.endText();

        y -= 40f;

        return y;
    }

    
    private void goBack() {
        // Reset popup flag when going back
        parentScreen.resetPopupFlag();
        parentScreen.start(stage);
    }

    private void addMainSlipToPDF(PDDocument doc) throws Exception {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        
        PDPageContentStream cs = new PDPageContentStream(doc, page);
        PDType1Font font = PDType1Font.COURIER;
        PDType1Font boldFont = PDType1Font.COURIER_BOLD;
        
        float y = PDRectangle.A4.getHeight() - 50;
        float centerX = PDRectangle.A4.getWidth() / 2;
        
        // Column positions for alignment
        float col1X = centerX - 100;  // Weight column (right edge)
        float col2X = centerX - 30;   // Arrow column (center)
        float col3X = centerX + 10;   // Price column (left edge)
        
        // Party Name
        cs.setFont(boldFont, 16);
        String partyName = mainSlip.getPartyName();
        float partyWidth = boldFont.getStringWidth(partyName) / 1000 * 16;
        cs.beginText();
        cs.newLineAtOffset(centerX - partyWidth / 2, y);
        cs.showText(partyName);
        cs.endText();
        y -= 30;
        
        // Sub-slip list with aligned columns
        cs.setFont(font, 12);
        for (SubSlip slip : subSlips) {
            String weight = String.format("%7.0f", slip.getMainWeight());
            String arrow = " -> ";
            String price = String.format("%-10.0f", slip.getFinalAmount());
            
            // Draw weight (right-aligned)
            float weightWidth = font.getStringWidth(weight) / 1000 * 12;
            cs.beginText();
            cs.newLineAtOffset(col1X - weightWidth + 60, y);
            cs.showText(weight);
            cs.endText();
            
            // Draw arrow (centered)
            cs.beginText();
            cs.newLineAtOffset(col2X, y);
            cs.showText(arrow);
            cs.endText();
            
            // Draw price (left-aligned)
            cs.beginText();
            cs.newLineAtOffset(col3X + 20, y);
            cs.showText(price);
            cs.endText();
            
            y -= 18;
        }
        
        // Separator line
        y -= 5;
        String separator = "------------------------------";
        float sepWidth = font.getStringWidth(separator) / 1000 * 12;
        cs.beginText();
        cs.newLineAtOffset(centerX - sepWidth / 2, y);
        cs.showText(separator);
        cs.endText();
        y -= 20;
        
        // Total
        double total = subSlips.stream().mapToDouble(SubSlip::getFinalAmount).sum();
        cs.setFont(boldFont, 14);
        String totalStr = String.format("%,.0f", total);
        float totalWidth = boldFont.getStringWidth(totalStr) / 1000 * 14;
        cs.beginText();
        cs.newLineAtOffset(centerX - totalWidth / 2, y);
        cs.showText(totalStr);
        cs.endText();
        y -= 25;
        
        // Operations
        if (mainSlip.getOperations() != null && !mainSlip.getOperations().isEmpty()) {
            cs.setFont(font, 12);
            cs.beginText();
            cs.newLineAtOffset(centerX - sepWidth / 2, y);
            cs.showText(separator);
            cs.endText();
            y -= 20;
            
            // Find max description length
            int maxDescLen = mainSlip.getOperations().stream()
                    .mapToInt(op -> op.getDescription().length())
                    .max().orElse(10);
            
            for (MainSlip.Operation op : mainSlip.getOperations()) {
                String amount = String.format("%7.0f", op.getAmount());
                String desc = String.format("%-" + maxDescLen + "s", op.getDescription());
                String type = "(" + op.getOperationType() + ")";
                
                // Draw amount (right-aligned)
                float amountWidth = font.getStringWidth(amount) / 1000 * 12;
                cs.beginText();
                cs.newLineAtOffset(col1X - amountWidth + 60, y);
                cs.showText(amount);
                cs.endText();
                
                // Draw description
                cs.beginText();
                cs.newLineAtOffset(col2X, y);
                cs.showText(desc);
                cs.endText();
                
                // Draw type
                cs.beginText();
                cs.newLineAtOffset(col3X + 40, y);
                cs.showText(type);
                cs.endText();
                
                y -= 18;
            }
        }
        
        // Final separator
        y -= 5;
        String doubleSep = "==============================";
        float doubleSepWidth = font.getStringWidth(doubleSep) / 1000 * 12;
        cs.beginText();
        cs.newLineAtOffset(centerX - doubleSepWidth / 2, y);
        cs.showText(doubleSep);
        cs.endText();
        y -= 20;
        
        // Final amount
        double finalAmount = calculateFinalAmount(total);
        cs.setFont(boldFont, 16);
        String finalStr = String.format("%,.0f", finalAmount);
        float finalWidth = boldFont.getStringWidth(finalStr) / 1000 * 16;
        cs.beginText();
        cs.newLineAtOffset(centerX - finalWidth / 2, y);
        cs.showText(finalStr);
        cs.endText();
        y -= 20;
        
        // Bottom separator
        cs.setFont(font, 12);
        cs.beginText();
        cs.newLineAtOffset(centerX - doubleSepWidth / 2, y);
        cs.showText(doubleSep);
        cs.endText();
        
        cs.close();
    }
    private double calculateFinalAmount(double total) {
        double finalAmount = total;
        
        if (mainSlip.getOperations() != null) {
            for (MainSlip.Operation op : mainSlip.getOperations()) {
                if (op.getOperationType().equals("-")) {
                    finalAmount -= op.getAmount();
                } else if (op.getOperationType().equals("+")) {
                    finalAmount += op.getAmount();
                }
            }
        }
        
        return finalAmount;
    }
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    public enum PrintMode {
        COMBINED,      // existing behavior
        INDIVIDUAL     // new behavior
    }

}
