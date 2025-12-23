package com.slipplus.screens.purchaseBook;

import com.slipplus.core.AppNavigator;
import com.slipplus.core.StorageManager;
import com.slipplus.models.MainSlip;
import com.slipplus.models.Shortcut;
import com.slipplus.models.SubSlip;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.awt.print.PrinterJob;
import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.printing.PDFPrintable;

public class PurchaseBookTableView {
    
    private Stage stage;
    private LocalDate selectedDate;
    private TableView<PurchaseBookRow> table;
    private ObservableList<PurchaseBookRow> tableData;
    private DecimalFormat moneyFormat = new DecimalFormat("#,##0");
    private BorderPane root;
    private VBox tableContainer;
    private double fontSize;
    
    public PurchaseBookTableView(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
        this.root = new BorderPane(); // Initialize root
    }
    
    public void start(Stage stage) {
        this.stage = stage;
        
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();
        this.fontSize = (screenWidth / 1920.0) * 18;
        if (fontSize < 14) fontSize = 14;
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white;");
        
        // Header with title and buttons
        VBox header = createHeader();
        root.setTop(header);
        
        // Create table content
        createTable();
        
        ScrollPane scrollPane = new ScrollPane(tableContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white;");
        root.setCenter(scrollPane);
        
        // Use same sizing as MainSlipScreen
        Scene scene = new Scene(root, 1600, 900);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.P) {
                printPurchaseBook();
            } else if (e.getCode() == KeyCode.S) {
                savePurchaseBookAsPDF();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                goBack();
            }
        });
        
        stage.setScene(scene);
        stage.setTitle("Purchase Book - " + selectedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        stage.setMaximized(true);
        stage.show();
    }
    
    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: white;");
        
        Label title = new Label("Purchase Book - " + selectedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        // Add Print button
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button printButton = new Button("Print Purchase Book (P)");
        printButton.setStyle("-fx-font-size: 16px; -fx-pref-width: 200px; -fx-pref-height: 40px;");
        printButton.setOnAction(e -> printPurchaseBook());
        
        Button saveButton = new Button("Save PDF (S)");
        saveButton.setStyle("-fx-font-size: 16px; -fx-pref-width: 150px; -fx-pref-height: 40px;");
        saveButton.setOnAction(e -> savePurchaseBookAsPDF());
        
        buttonBox.getChildren().addAll(printButton, saveButton);
        
        header.getChildren().addAll(title, buttonBox);
        return header;
    }
    
    private void createTable() {
        createSimpleLayout();
    }

    private void createSimpleLayout() {
        VBox content = new VBox(5);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white;");
        
        // Create header using GridPane for proper column alignment
        GridPane headerGrid = createHeaderGrid();
        content.getChildren().add(headerGrid);
        
        // Load data rows (removed separator line)
        loadSimpleDataRows(content);
        
        // Store content as tableContainer
        this.tableContainer = content;
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white;");
        
        root.setCenter(scrollPane);
    }

    private GridPane createHeaderGrid() {
        GridPane grid = new GridPane();
        grid.setStyle("-fx-border-color: transparent;");
        
        // Get shortcuts that should be shown in purchase book
        List<Shortcut> purchaseBookShortcuts = StorageManager.loadShortcuts().stream()
                .filter(Shortcut::isShowInPurchaseBook)
                .collect(Collectors.toList());
        
        // Calculate total columns: 6 fixed + dynamic shortcuts
        int totalColumns = 6 + purchaseBookShortcuts.size();
        
        double screenWidth = javafx.stage.Screen.getPrimary().getBounds().getWidth();
        double tableWidth = screenWidth * 0.9;
        double columnWidth = tableWidth / totalColumns;
        
        // Fixed headers - reordered to put shortcuts before Amount
        String[] fixedHeaders = {"Party Name", "Main Wt", "sub Wt"};
        
        int columnIndex = 0;
        
        // Add first 3 fixed headers
        for (int i = 0; i < fixedHeaders.length; i++) {
            Label headerLabel = new Label(fixedHeaders[i]);
            headerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 0 1 1 0; -fx-padding: 5;");
            headerLabel.setPrefWidth(columnWidth);
            headerLabel.setAlignment(Pos.CENTER);
            grid.add(headerLabel, columnIndex++, 0);
        }
        
        // Add shortcut headers (before Amount)
        for (Shortcut shortcut : purchaseBookShortcuts) {
            Label shortcutHeader = new Label(shortcut.getDescription());
            shortcutHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 0 1 1 0; -fx-padding: 5;");
            shortcutHeader.setPrefWidth(columnWidth);
            shortcutHeader.setAlignment(Pos.CENTER);
            grid.add(shortcutHeader, columnIndex++, 0);
        }
        
        // Add remaining fixed headers: Amount, GST, Final Amount
        String[] remainingHeaders = {"Amount", "GST", "Truck No."};
        for (String header : remainingHeaders) {
            Label headerLabel = new Label(header);
            headerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 0 1 1 0; -fx-padding: 5;");
            headerLabel.setPrefWidth(columnWidth);
            headerLabel.setAlignment(Pos.CENTER);
            grid.add(headerLabel, columnIndex++, 0);
        }
        
        return grid;
    }

    private void loadSimpleDataRows(VBox container) {
        Map<String, List<SubSlip>> partiesData = StorageManager.getSubSlipsGroupedByParty(selectedDate);
        
        // Get shortcuts for column calculation
        List<Shortcut> purchaseBookShortcuts = StorageManager.loadShortcuts().stream()
                .filter(Shortcut::isShowInPurchaseBook)
                .collect(Collectors.toList());
        
        double screenWidth = javafx.stage.Screen.getPrimary().getBounds().getWidth();
        double tableWidth = screenWidth * 0.9;
        double columnWidth = tableWidth / (6 + purchaseBookShortcuts.size()); // Dynamic column count
        
        for (Map.Entry<String, List<SubSlip>> entry : partiesData.entrySet()) {
            String partyId = entry.getKey();
            List<SubSlip> subSlips = entry.getValue();
            String partyName = StorageManager.getPartyNameById(partyId);
            
            MainSlip mainSlip = StorageManager.getMainSlip(selectedDate, partyName);
            Map<String, Double> dividedAmounts = calculateDividedAmounts(mainSlip, subSlips.size());
            
            boolean isFirstRowOfParty = true;
            
            for (SubSlip subSlip : subSlips) {
                List<GridPane> subWeightRows = new ArrayList<>();
                
                for (int i = 0; i < subSlip.getSubWeights().size(); i++) {
                    boolean isFirstRowOfSlip = (i == 0);
                    
                    GridPane dataRow = createDataRowGrid(
                        isFirstRowOfParty ? partyName : "",
                        isFirstRowOfSlip ? subSlip.getMainWeight() : 0,
                        subSlip.getSubWeights().get(i),
                        subSlip.getCalculatedPrices().get(i),
                        0, 0, 
                        subSlip.getTruckNumber(), // Don't show totals initially
                        columnWidth,
                        new HashMap<>() // Don't show shortcuts on any row initially
                    );
                    subWeightRows.add(dataRow);
                    isFirstRowOfParty = false;
                }
                
                // Add all sub-weight rows
                container.getChildren().addAll(subWeightRows);
                
                // Add totals AND shortcuts to center row only
                int centerIndex = subWeightRows.size() / 2;
                if (!subWeightRows.isEmpty()) {
                    updateGridRowWithTotals(subWeightRows.get(centerIndex), 
                        subSlip.getTotalBeforeGst(), 
                        subSlip.getGst(),
                        subSlip.getTruckNumber(), 
                        dividedAmounts); // Pass shortcuts only to center row
                }
            }
            
            // Add space between parties
            Label spacer = new Label(" ");
            container.getChildren().add(spacer);
        }
    }

    private GridPane createDataRowGrid(String partyName, double mainWeight, double subWeight, 
                                      double rate, double totalBeforeGst, double gst, String truckNumber, 
                                      double columnWidth, Map<String, Double> dividedAmounts) {
        GridPane grid = new GridPane();
        
        // Get shortcuts for dynamic columns
        List<Shortcut> purchaseBookShortcuts = StorageManager.loadShortcuts().stream()
                .filter(Shortcut::isShowInPurchaseBook)
                .collect(Collectors.toList());
        
        int columnIndex = 0;
        
        // Party Name
        Label partyLabel = new Label(partyName);
        partyLabel.setPrefWidth(columnWidth);
        partyLabel.setAlignment(Pos.CENTER);
        partyLabel.setStyle("-fx-border-color: black; -fx-border-width: 0 1 0 0; -fx-padding: 5; -fx-font-size: 12px;");
        grid.add(partyLabel, columnIndex++, 0);
        
        // Main Weight
        String mainWeightText = mainWeight > 0 ? String.format("%,d", (int)mainWeight) : "";
        Label mainWtLabel = new Label(mainWeightText);
        mainWtLabel.setPrefWidth(columnWidth);
        mainWtLabel.setAlignment(Pos.CENTER);
        mainWtLabel.setStyle("-fx-border-color: black; -fx-border-width: 0 1 0 0; -fx-padding: 5; -fx-font-size: 12px;");
        grid.add(mainWtLabel, columnIndex++, 0);
        
        // Sub Weight (Rate column)
        String rateText;
        if (rate == 0) {
            rateText = String.format("%,d", (int)subWeight);
        } else {
            rateText = String.format("%,d × %,d", (int)subWeight, (int)rate);
        }
        Label rateLabel = new Label(rateText);
        rateLabel.setPrefWidth(columnWidth);
        rateLabel.setAlignment(Pos.CENTER_LEFT);
        rateLabel.setStyle("-fx-border-color: black; -fx-border-width: 0 1 0 0; -fx-padding: 5; -fx-font-size: 12px;");
        grid.add(rateLabel, columnIndex++, 0);
        
        // Dynamic shortcut columns with actual values (before Amount)
        for (Shortcut shortcut : purchaseBookShortcuts) {
            Double amount = dividedAmounts.get(shortcut.getAlphabet());
            String shortcutText = (amount != null && amount > 0) ? String.format("%,d", (int)amount.doubleValue()) : "";
            
            Label shortcutLabel = new Label(shortcutText);
            shortcutLabel.setPrefWidth(columnWidth);
            shortcutLabel.setAlignment(Pos.CENTER);
            shortcutLabel.setStyle("-fx-border-color: black; -fx-border-width: 0 1 0 0; -fx-padding: 5; -fx-font-size: 12px;");
            grid.add(shortcutLabel, columnIndex++, 0);
        }
        
        // Amount (Total Before GST)
        String totalText = totalBeforeGst > 0 ? String.format("%,d", (int)totalBeforeGst) : "";
        Label totalLabel = new Label(totalText);
        totalLabel.setPrefWidth(columnWidth);
        totalLabel.setAlignment(Pos.CENTER);
        totalLabel.setStyle("-fx-border-color: black; -fx-border-width: 0 1 0 0; -fx-padding: 5; -fx-font-size: 12px;");
        grid.add(totalLabel, columnIndex++, 0);
        
        // GST - Show value even if 0
        String gstText = (gst >= 0 && (totalBeforeGst > 0 || gst > 0)) ? String.format("%,d", (int)gst) : "";
        Label gstLabel = new Label(gstText);
        gstLabel.setPrefWidth(columnWidth);
        gstLabel.setAlignment(Pos.CENTER);
        gstLabel.setStyle("-fx-border-color: black; -fx-border-width: 0 1 0 0; -fx-padding: 5; -fx-font-size: 12px;");
        grid.add(gstLabel, columnIndex++, 0);
        
        // Final Amount
       Label truckLabel = new Label("");
        truckLabel.setPrefWidth(columnWidth);
        truckLabel.setAlignment(Pos.CENTER);
        truckLabel.setStyle("-fx-border-color: black; -fx-border-width: 0 1 0 0; -fx-padding: 5; -fx-font-size: 12px;");
        grid.add(truckLabel, columnIndex, 0);

        
        return grid;
    }

    private void updateGridRowWithTotals(GridPane grid, double totalBeforeGst, double gst, String truckNumber, Map<String, Double> dividedAmounts) {
        List<Shortcut> purchaseBookShortcuts = StorageManager.loadShortcuts().stream()
                .filter(Shortcut::isShowInPurchaseBook)
                .collect(Collectors.toList());
        
        // Calculate column positions
        int shortcutStartIndex = 3; // After Party Name, Main Wt, sub Wt
        int amountIndex = shortcutStartIndex + purchaseBookShortcuts.size();
        int gstIndex = amountIndex + 1;
        int truckIndex = gstIndex + 1;
        
        // Update shortcut columns with actual values
        for (int i = 0; i < purchaseBookShortcuts.size(); i++) {
            Shortcut shortcut = purchaseBookShortcuts.get(i);
            Double amount = dividedAmounts.get(shortcut.getAlphabet());
            String shortcutText = (amount != null && amount > 0) ? String.format("%,d", (int)amount.doubleValue()) : "";
            
            Label shortcutLabel = (Label) grid.getChildren().get(shortcutStartIndex + i);
            shortcutLabel.setText(shortcutText);
        }
        
        // Update Amount (Total Before GST)
        Label totalLabel = (Label) grid.getChildren().get(amountIndex);
        totalLabel.setText(totalBeforeGst > 0 ? String.format("%,d", (int)totalBeforeGst) : "");
        
        // Update GST - Show value even if 0
        Label gstLabel = (Label) grid.getChildren().get(gstIndex);
        gstLabel.setText((gst >= 0 && totalBeforeGst > 0) ? String.format("%,d", (int)gst) : "");

        // Update Truck Number
        Label truckLabel = (Label) grid.getChildren().get(truckIndex);
        truckLabel.setText(truckNumber != null ? truckNumber : "");
        
    }

    private double getColumnWidth(int columnIndex) {
        switch (columnIndex) {
            case 0: return 150; // Party Name
            case 1: return 120; // Main Weight
            case 2: return 100; // Sub Weight
            case 3: return 180; // Rate (wider for "× format")
            default: return 120; // Shortcuts and final columns
        }
    }
    
    private void loadData() {
        tableData = FXCollections.observableArrayList();
        
        // Get all parties with sub-slips for this date
        Map<String, List<SubSlip>> partiesData = StorageManager.getSubSlipsGroupedByParty(selectedDate);
        
        for (Map.Entry<String, List<SubSlip>> entry : partiesData.entrySet()) {
            String partyId = entry.getKey();
            List<SubSlip> subSlips = entry.getValue();
            String partyName = StorageManager.getPartyNameById(partyId);
            
            // Get main slip for this party to get operation amounts
            MainSlip mainSlip = StorageManager.getMainSlip(selectedDate, partyName);
            Map<String, Double> dividedAmounts = calculateDividedAmounts(mainSlip, subSlips.size());
            
            // Create rows for each sub-slip and its sub-weights
            for (int slipIndex = 0; slipIndex < subSlips.size(); slipIndex++) {
                SubSlip subSlip = subSlips.get(slipIndex);
                
                // Create rows for each sub-weight
                for (int weightIndex = 0; weightIndex < subSlip.getSubWeights().size(); weightIndex++) {
                    boolean isFirstRowOfParty = (slipIndex == 0 && weightIndex == 0);
                    boolean isFirstRowOfSlip = (weightIndex == 0);
                    
                    PurchaseBookRow row = new PurchaseBookRow(
                        isFirstRowOfParty ? partyName : "", // Only show party name on first row
                        isFirstRowOfSlip ? subSlip.getMainWeight() : 0, // Only show main weight on first row of slip
                        subSlip.getSubWeights().get(weightIndex),
                        subSlip.getCalculatedPrices().get(weightIndex),
                        isFirstRowOfSlip ? dividedAmounts : new HashMap<>(), // Only show shortcut amounts on first row of slip
                        isFirstRowOfSlip ? subSlip.getTotalBeforeGst() : 0,
                        isFirstRowOfSlip ? subSlip.getGst() : 0,
                        isFirstRowOfSlip ? calculateFinalAmount(subSlip, dividedAmounts) : 0,
                        isFirstRowOfParty,
                        isFirstRowOfSlip,
                        subSlip.getSubWeights().size()
                    );
                    tableData.add(row);
                }
            }
        }
        
        table.setItems(tableData);
    }
    
    private Map<String, Double> calculateDividedAmounts(MainSlip mainSlip, int subSlipCount) {
        Map<String, Double> dividedAmounts = new HashMap<>();
        
        if (mainSlip == null || mainSlip.getOperations() == null || subSlipCount == 0) {
            return dividedAmounts;
        }
        
        // Divide each operation amount by number of sub slips
        for (MainSlip.Operation operation : mainSlip.getOperations()) {
            String alphabet = operation.getShortcutId();
            double totalAmount = operation.getAmount();
            double dividedAmount = totalAmount / subSlipCount;
            
            dividedAmounts.put(alphabet, dividedAmount);
        }
        
        return dividedAmounts;
    }
    
    private double calculateFinalAmount(SubSlip subSlip, Map<String, Double> dividedAmounts) {
        double finalAmount = subSlip.getTotalBeforeGst() + subSlip.getGst();
        
        // Get shortcuts that should be shown in purchase book
        List<Shortcut> shortcuts = StorageManager.loadShortcuts().stream()
                .filter(Shortcut::isShowInPurchaseBook)
                .collect(Collectors.toList());
        
        for (Shortcut shortcut : shortcuts) {
            Double amount = dividedAmounts.get(shortcut.getAlphabet());
            if (amount != null) {
                if ("+".equals(shortcut.getOperation())) {
                    finalAmount += amount;
                } else if ("-".equals(shortcut.getOperation())) {
                    finalAmount -= amount;
                }
            }
        }
        
        return finalAmount;
    }

    private void printPurchaseBook() {
        try {
            PrintService[] printers = PrintServiceLookup.lookupPrintServices(null, null);
            
            if (printers.length > 0) {
                PrinterJob job = PrinterJob.getPrinterJob();
                
                if (job.printDialog()) {
                    try {
                        PDDocument doc = createPurchaseBookPDF();
                        job.setPrintable(new PDFPrintable(doc));
                        job.print();
                        doc.close();
                        
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Print Complete");
                        alert.setHeaderText(null);
                        alert.setContentText("Purchase Book printed successfully!");
                        alert.showAndWait();
                        
                    } catch (java.awt.print.PrinterException pe) {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Printer Error");
                        errorAlert.setHeaderText("Cannot access printer");
                        errorAlert.setContentText("Would you like to save as PDF instead?");
                        
                        ButtonType saveAsPdf = new ButtonType("Save as PDF");
                        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                        errorAlert.getButtonTypes().setAll(saveAsPdf, cancel);
                        
                        errorAlert.showAndWait().ifPresent(response -> {
                            if (response == saveAsPdf) {
                                savePurchaseBookAsPDF();
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
                        savePurchaseBookAsPDF();
                    }
                });
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Print Error");
            alert.setHeaderText(null);
            alert.setContentText("Error during printing: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void savePurchaseBookAsPDF() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Purchase Book PDF");
            fileChooser.setInitialFileName("PurchaseBook_" + selectedDate.toString() + ".pdf");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );
            
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                PDDocument doc = createPurchaseBookPDF();
                doc.save(file);
                doc.close();
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("PDF Saved");
                alert.setHeaderText(null);
                alert.setContentText("Purchase Book PDF saved successfully to:\n" + file.getAbsolutePath());
                alert.showAndWait();
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

    private PDDocument createPurchaseBookPDF() throws Exception {
        PDDocument doc = new PDDocument();
        PDType1Font font = PDType1Font.HELVETICA;
        
        // Get data
        Map<String, List<SubSlip>> partiesData = StorageManager.getSubSlipsGroupedByParty(selectedDate);
        List<Shortcut> purchaseBookShortcuts = StorageManager.loadShortcuts().stream()
                .filter(Shortcut::isShowInPurchaseBook)
                .collect(Collectors.toList());
        
        // Calculate dynamic column widths
        float pageWidth = PDRectangle.A4.getWidth();
        float margin = 20f; // Reduced from 40f
        float tableWidth = pageWidth - (2 * margin);
        
        // Define custom column widths (in proportions)
        float[] columnProportions = getColumnProportions(purchaseBookShortcuts.size());
        float[] columnWidths = new float[columnProportions.length];
        
        for (int i = 0; i < columnProportions.length; i++) {
            columnWidths[i] = tableWidth * columnProportions[i];
        }
        
        PDPage currentPage = new PDPage(PDRectangle.A4);
        doc.addPage(currentPage);
        
        PDPageContentStream cs = new PDPageContentStream(doc, currentPage);
        float currentY = PDRectangle.A4.getHeight() - 30f; // Reduced top margin
        
        // Add title
        cs.setFont(font, 14f); // Reduced from 16f
        String title = "Purchase Book - " + selectedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        float titleWidth = font.getStringWidth(title) / 1000f * 14f;
        cs.beginText();
        cs.newLineAtOffset((pageWidth - titleWidth) / 2f, currentY);
        cs.showText(title);
        cs.endText();
        currentY -= 30f; // Reduced from 40f
        
        // Add headers
        currentY = addPurchaseBookHeaders(cs, font, margin, currentY, columnWidths, purchaseBookShortcuts);
        currentY -= 5f; // Reduced spacing
        
        // Add data rows
        for (Map.Entry<String, List<SubSlip>> entry : partiesData.entrySet()) {
            String partyId = entry.getKey();
            List<SubSlip> subSlips = entry.getValue();
            String partyName = StorageManager.getPartyNameById(partyId);
            
            MainSlip mainSlip = StorageManager.getMainSlip(selectedDate, partyName);
            Map<String, Double> dividedAmounts = calculateDividedAmounts(mainSlip, subSlips.size());
            
            boolean isFirstRowOfParty = true;
            
            for (SubSlip subSlip : subSlips) {
                // Check if we need a new page for this sub-slip
                float neededHeight = subSlip.getSubWeights().size() * 16f + 20f; // Reduced row height
                if (currentY < neededHeight + 40f) {
                    cs.close();
                    currentPage = new PDPage(PDRectangle.A4);
                    doc.addPage(currentPage);
                    cs = new PDPageContentStream(doc, currentPage);
                    currentY = PDRectangle.A4.getHeight() - 30f;
                    
                    // Add headers on new page
                    currentY = addPurchaseBookHeaders(cs, font, margin, currentY, columnWidths, purchaseBookShortcuts);
                    currentY -= 5f;
                }
                
                // Add sub-slip rows
                for (int i = 0; i < subSlip.getSubWeights().size(); i++) {
                    boolean isFirstRowOfSlip = (i == 0);
                    boolean isCenterRow = (i == subSlip.getSubWeights().size() / 2);
                    
                    currentY = addPurchaseBookDataRow(cs, font, margin, currentY, columnWidths,
                        isFirstRowOfParty ? partyName : "",
                        isFirstRowOfSlip ? subSlip.getMainWeight() : 0,
                        subSlip.getSubWeights().get(i),
                        subSlip.getCalculatedPrices().get(i),
                        isCenterRow ? subSlip.getTotalBeforeGst() : 0,
                        isCenterRow ? subSlip.getGst() : 0,
                        isCenterRow ? subSlip.getTruckNumber() : "",
                        isCenterRow ? dividedAmounts : new HashMap<>(),
                        purchaseBookShortcuts);
                    
                    isFirstRowOfParty = false;
                }
                
                currentY -= 3f; // Reduced space between sub-slips
            }
            
            currentY -= 5f; // Reduced space between parties
        }
        
        cs.close();
        return doc;
    }

    private float[] getColumnProportions(int shortcutCount) {
        // Total columns: Party Name, Main Wt, Sub Wt, [shortcuts], Amount, GST, Total
        int totalColumns = 6 + shortcutCount;
        
        float[] proportions = new float[totalColumns];
        int index = 0;
        
        // Party Name - 25% of width
        proportions[index++] = 0.25f;
        
        // Main Wt - 10% of width
        proportions[index++] = 0.10f;
        
        // Sub Wt - 25% of width (most space for complex data)
        proportions[index++] = 0.20f;
        
        // Shortcut columns - share 20% equally
        float shortcutProportion = shortcutCount > 0 ? 0.20f / shortcutCount : 0f;
        for (int i = 0; i < shortcutCount; i++) {
            proportions[index++] = shortcutProportion;
        }
        
        // Amount - 8% of width
        proportions[index++] = 0.08f;
        
        // GST - 6% of width
        proportions[index++] = 0.06f;
        
        // Truck No - 11%   of width
        proportions[index++] = 0.11f;
        
        return proportions;
    }

    private float addPurchaseBookHeaders(PDPageContentStream cs, PDType1Font font, float margin, 
                                       float y, float[] columnWidths, List<Shortcut> shortcuts) throws Exception {
        cs.setFont(font, 8f); // Reduced from 10f
        
        float x = margin;
        float headerHeight = 20f; // Reduced from 25f
        float tableWidth = 0f;
        for (float width : columnWidths) {
            tableWidth += width;
        }
        
        // Draw vertical lines for headers
        float currentX = margin;
        for (int i = 0; i <= columnWidths.length; i++) {
            cs.setLineWidth(0.5f);
            cs.moveTo(currentX, y + 3f);
            cs.lineTo(currentX, y - headerHeight);
            cs.stroke();
            if (i < columnWidths.length) {
                currentX += columnWidths[i];
            }
        }
        
        // Fixed headers
        String[] fixedHeaders = {"Party Name", "Main Wt", "Sub Wt"};
        int headerIndex = 0;
        
        for (String header : fixedHeaders) {
            float textWidth = font.getStringWidth(header) / 1000f * 8f;
            cs.beginText();
            cs.newLineAtOffset(x + (columnWidths[headerIndex] - textWidth) / 2f, y - 6f);
            cs.showText(header);
            cs.endText();
            x += columnWidths[headerIndex];
            headerIndex++;
        }
        
        // Shortcut headers
        for (Shortcut shortcut : shortcuts) {
            String headerText = shortcut.getDescription();
            // Truncate if too long
            if (headerText.length() > 8) {
                headerText = headerText.substring(0, 8);
            }
            float textWidth = font.getStringWidth(headerText) / 1000f * 8f;
            cs.beginText();
            cs.newLineAtOffset(x + (columnWidths[headerIndex] - textWidth) / 2f, y - 6f);
            cs.showText(headerText);
            cs.endText();
            x += columnWidths[headerIndex];
            headerIndex++;
        }
        
        // Remaining headers
        String[] remainingHeaders = {"Amount", "GST", "Truck No"};
        for (String header : remainingHeaders) {
            float textWidth = font.getStringWidth(header) / 1000f * 8f;
            cs.beginText();
            cs.newLineAtOffset(x + (columnWidths[headerIndex] - textWidth) / 2f, y - 6f);
            cs.showText(header);
            cs.endText();
            x += columnWidths[headerIndex];
            headerIndex++;
        }
        
        // Draw header bottom line
        y -= headerHeight;
        cs.setLineWidth(0.5f);
        cs.moveTo(margin, y);
        cs.lineTo(margin + tableWidth, y);
        cs.stroke();
        
        return y - 3f;
    }

    private float addPurchaseBookDataRow(PDPageContentStream cs, PDType1Font font, float margin, 
                                       float y, float[] columnWidths, String partyName, double mainWeight,
                                       double subWeight, double rate, double totalBeforeGst, double gst,
                                       String truckNumber, Map<String, Double> dividedAmounts,
                                       List<Shortcut> shortcuts) throws Exception {
        cs.setFont(font, 8f);
        
        float x = margin;
        float rowHeight = 16f;
        
        // Draw vertical lines for each column
        float currentX = margin;
        for (int i = 0; i <= columnWidths.length; i++) {
            cs.setLineWidth(0.3f);
            cs.moveTo(currentX, y + 3f);
            cs.lineTo(currentX, y - 13f);
            cs.stroke();
            if (i < columnWidths.length) {
                currentX += columnWidths[i];
            }
        }
        
        int columnIndex = 0;
        
        // Party Name - center aligned
        String displayPartyName = partyName;
        if (displayPartyName.length() > 15) {
            displayPartyName = displayPartyName.substring(0, 15);
        }
        float partyTextWidth = font.getStringWidth(displayPartyName) / 1000f * 8f;
        cs.beginText();
        cs.newLineAtOffset(x + (columnWidths[columnIndex] - partyTextWidth) / 2f, y);
        cs.showText(displayPartyName);
        cs.endText();
        x += columnWidths[columnIndex++];
        
        // Main Weight - center aligned
        String mainWeightText = mainWeight > 0 ? String.format("%.0f", mainWeight) : "";
        float mainWeightTextWidth = font.getStringWidth(mainWeightText) / 1000f * 8f;
        cs.beginText();
        cs.newLineAtOffset(x + (columnWidths[columnIndex] - mainWeightTextWidth) / 2f, y);
        cs.showText(mainWeightText);
        cs.endText();
        x += columnWidths[columnIndex++];
        
        // Sub Weight (Rate) - center aligned
        String rateText;
        if (rate == 0) {
            rateText = String.format("%.0f", subWeight);
        } else {
            rateText = String.format("%.0f x %.0f", subWeight, rate);
            if (rateText.length() > 18) {
                rateText = String.format("%.0f x %.0f", subWeight, rate);
            }
        }
        float rateTextWidth = font.getStringWidth(rateText) / 1000f * 8f;
        cs.beginText();
        cs.newLineAtOffset(x + 4f, y);
        cs.showText(rateText);
        cs.endText();
        x += columnWidths[columnIndex++];
        
        // Shortcut columns - center aligned
        for (Shortcut shortcut : shortcuts) {
            Double amount = dividedAmounts.get(shortcut.getAlphabet());
            String shortcutText = (amount != null && amount > 0) ? String.format("%.0f", amount) : "";
            float shortcutTextWidth = font.getStringWidth(shortcutText) / 1000f * 8f;
            cs.beginText();
            cs.newLineAtOffset(x + (columnWidths[columnIndex] - shortcutTextWidth) / 2f, y);
            cs.showText(shortcutText);
            cs.endText();
            x += columnWidths[columnIndex++];
        }
        
        // Amount - center aligned
        String totalText = totalBeforeGst > 0 ? String.format("%.0f", totalBeforeGst) : "";
        float totalTextWidth = font.getStringWidth(totalText) / 1000f * 8f;
        cs.beginText();
        cs.newLineAtOffset(x + (columnWidths[columnIndex] - totalTextWidth) / 2f, y);
        cs.showText(totalText);
        cs.endText();
        x += columnWidths[columnIndex++];
        
        // GST - center aligned
        String gstText = (gst >= 0 && (totalBeforeGst > 0 || gst > 0)) ? String.format("%.0f", gst) : "";
        float gstTextWidth = font.getStringWidth(gstText) / 1000f * 8f;
        cs.beginText();
        cs.newLineAtOffset(x + (columnWidths[columnIndex] - gstTextWidth) / 2f, y);
        cs.showText(gstText);
        cs.endText();
        x += columnWidths[columnIndex++];
        
        // Final Amount - center aligned
        String finalText = truckNumber != null ? truckNumber : "";
        float finalTextWidth = font.getStringWidth(finalText) / 1000f * 8f;
        cs.beginText();
        cs.newLineAtOffset(x + (columnWidths[columnIndex] - finalTextWidth) / 2f, y);
        cs.showText(finalText);
        cs.endText();
        
        return y - rowHeight;
    }

    private void goBack() {
        AppNavigator.startApp(stage);
    }
}

































