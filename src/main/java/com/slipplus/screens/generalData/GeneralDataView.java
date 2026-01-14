package com.slipplus.screens.generalData;

import com.slipplus.core.AppNavigator;
import com.slipplus.core.StorageManager;
import com.slipplus.models.MainSlip;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.*;

import java.awt.print.PrinterJob;
import java.io.File;
import java.time.LocalDate;
import java.util.*;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.printing.PDFPrintable;

public class GeneralDataView {

    private final LocalDate date;

    public GeneralDataView(LocalDate date) {
        this.date = date;
    }

    // =========================================================
    // START
    // =========================================================
    public void start(Stage stage) {

        VBox page = new VBox(40);
        page.setPadding(new Insets(40, 120, 40, 120));
        page.setAlignment(Pos.TOP_CENTER);

        Button printBtn = new Button("Print (P)");
        Button pdfBtn = new Button("Save PDF (S)");

        printBtn.setStyle("-fx-font-size:18px; -fx-pref-width:200px; -fx-pref-height:50px;");
        pdfBtn.setStyle("-fx-font-size:18px; -fx-pref-width:200px; -fx-pref-height:50px;");

        printBtn.setOnAction(e -> printGeneralData());
        pdfBtn.setOnAction(e -> saveGeneralDataPDF());

        HBox actions = new HBox(30, printBtn, pdfBtn);
        actions.setAlignment(Pos.CENTER);
        page.getChildren().add(actions);

        Map<String, MainSlip> slips =
                StorageManager.loadMainSlips().get(date.toString());

        if (slips != null) {
            for (MainSlip slip : slips.values()) {
                page.getChildren().add(buildPartyBlock(slip));
            }
        }

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);

        BorderPane root = new BorderPane(scroll);

        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();

        
        Scene scene = new Scene(root, screenWidth, screenHeight);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) AppNavigator.startApp(stage);
            if (e.getCode() == KeyCode.P) printGeneralData();
            if (e.getCode() == KeyCode.S) saveGeneralDataPDF();
        });

        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    // =========================================================
    // PARTY BLOCK (PERFECT LEDGER)
    // =========================================================
    private VBox buildPartyBlock(MainSlip slip) {

        VBox outer = new VBox(15);
        outer.setStyle("-fx-border-color:black; -fx-border-width:0 1 0 1;");

        GridPane grid = new GridPane();
        configureLedgerGrid(grid);

        // -------- HEADER (RIGHT ALIGNED) --------
        Label headerText = new Label(
                slip.getPartyName() + "  " +
                (long) Math.floor(slip.getTotalBeforeOperations()) + " "
        );
        headerText.setStyle("-fx-font-size:22px; -fx-font-weight:bold;");

        HBox headerBox = new HBox(headerText);
        headerBox.setAlignment(Pos.CENTER_RIGHT);
        headerBox.setMaxWidth(Double.MAX_VALUE);

        grid.add(headerBox, 2, 0);

        // -------- OPERATIONS --------
        List<MainSlip.Operation> minus = new ArrayList<>();
        List<MainSlip.Operation> plus = new ArrayList<>();

        for (MainSlip.Operation op : slip.getOperations()) {
            if ("-".equals(op.getOperationType())) minus.add(op);
            else plus.add(op);
        }

        int rows = Math.max(minus.size(), plus.size());

        for (int i = 0; i < rows; i++) {

            if (i < minus.size()) {
                Label l = new Label(" " + (long) Math.floor(minus.get(i).getAmount()));
                l.setStyle("-fx-font-size:22px;");

                HBox box = new HBox(l);
                box.setAlignment(Pos.CENTER_LEFT);
                box.setMaxWidth(Double.MAX_VALUE);

                grid.add(box, 0, i + 1);
            }

            if (i < plus.size()) {
                Label l = new Label("" + (long) Math.floor(plus.get(i).getAmount()) + " ");
                l.setStyle("-fx-font-size:22px;");

                HBox box = new HBox(l);
                box.setAlignment(Pos.CENTER_RIGHT);
                box.setMaxWidth(Double.MAX_VALUE);

                grid.add(box, 2, i + 1);
            }
        }

        // -------- FINAL TOTAL --------
        double finalTotal = slip.getTotalAfterOperations();
        Label finalLabel = new Label(" ranjod " + (long) Math.floor(Math.abs(finalTotal)) +" ");
        finalLabel.setStyle("-fx-font-size:22px; -fx-font-weight:bold;");

        HBox finalBox = new HBox(finalLabel);
        finalBox.setAlignment(finalTotal < 0 ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        finalBox.setMaxWidth(Double.MAX_VALUE);

        grid.add(finalBox, finalTotal < 0 ? 2 : 0, rows + 1);

        // -------- CENTER LINE (ALWAYS FULL HEIGHT) --------
        Rectangle centerLine = new Rectangle(2, 1);
        centerLine.setFill(Color.BLACK);
        centerLine.heightProperty().bind(grid.heightProperty());

        grid.add(centerLine, 1, 0);
        GridPane.setRowSpan(centerLine, rows + 2);

        Separator sep = new Separator();
        outer.getChildren().addAll(grid, sep);

        return outer;
    }

    // =========================================================
    // GRID CONFIG
    // =========================================================
    private void configureLedgerGrid(GridPane grid) {

        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(45);

        ColumnConstraints mid = new ColumnConstraints();
        mid.setPercentWidth(10);

        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(45);

        grid.getColumnConstraints().setAll(left, mid, right);
    }

    // =========================================================
    // PRINT
    // =========================================================
    private void printGeneralData() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            if (job.printDialog()) {
                PDDocument doc = createGeneralDataPDF();
                job.setPrintable(new PDFPrintable(doc));
                job.print();
                doc.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Printing failed").showAndWait();
        }
    }

    // =========================================================
    // SAVE PDF
    // =========================================================
    private void saveGeneralDataPDF() {
        try {
            FileChooser fc = new FileChooser();
            fc.setInitialFileName("GeneralData_" + date + ".pdf");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );

            File file = fc.showSaveDialog(null);
            if (file == null) return;

            PDDocument doc = createGeneralDataPDF();
            doc.save(file);
            doc.close();

            new Alert(Alert.AlertType.INFORMATION, "PDF saved successfully").showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // PDF (RIGHT-ALIGNED PROPERLY)
    // =========================================================
    private PDDocument createGeneralDataPDF() throws Exception {

        PDDocument doc = new PDDocument();
        PDType1Font font = PDType1Font.HELVETICA;

        float margin = 40;
        float pageWidth = PDRectangle.A4.getWidth();
        float usable = pageWidth - margin * 2;

        float leftX = margin;
        float centerX = margin + usable / 2;
        float rightX = pageWidth - margin;

        float y = PDRectangle.A4.getHeight() - 60;

        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page);

        Map<String, MainSlip> slips =
                StorageManager.loadMainSlips().get(date.toString());

        final float HEADER_GAP = 34;
        final float ROW_GAP    = 26;
        final float FINAL_GAP  = 30;

        for (MainSlip slip : slips.values()) {

            // ---------- HEADER ----------
            String header = slip.getPartyName() + "  " +
                    (long) Math.floor(slip.getTotalBeforeOperations()) + " ";

            float headerWidth = font.getStringWidth(header) / 1000 * 14;

            cs.setFont(font, 14);
            cs.beginText();
            cs.newLineAtOffset(rightX - headerWidth, y);
            cs.showText(header);
            cs.endText();

            float blockTop = y + 10;
            y -= HEADER_GAP;

            // ---------- OPERATIONS ----------
            List<MainSlip.Operation> minus = new ArrayList<>();
            List<MainSlip.Operation> plus = new ArrayList<>();

            for (MainSlip.Operation op : slip.getOperations()) {
                if ("-".equals(op.getOperationType())) minus.add(op);
                else plus.add(op);
            }

            int rows = Math.max(1, Math.max(minus.size(), plus.size()));
            cs.setFont(font, 13);

            for (int i = 0; i < rows; i++) {

                if (i < minus.size()) {
                    cs.beginText();
                    cs.newLineAtOffset(leftX, y);
                    cs.showText(" " + (long) Math.floor(minus.get(i).getAmount()));
                    cs.endText();
                }

                if (i < plus.size()) {
                    String t = "" + (long) Math.floor(plus.get(i).getAmount()) + " ";
                    float w = font.getStringWidth(t) / 1000 * 13;

                    cs.beginText();
                    cs.newLineAtOffset(rightX - w, y);
                    cs.showText(t);
                    cs.endText();
                }

                y -= ROW_GAP;
            }

            // ---------- FINAL TOTAL ----------
            y -= FINAL_GAP;

            String ft = " ranjod " +
                    (long) Math.floor(Math.abs(slip.getTotalAfterOperations())) + " ";

            float fw = font.getStringWidth(ft) / 1000 * 14;

            cs.setFont(font, 14);
            cs.beginText();
            cs.newLineAtOffset(
                    slip.getTotalAfterOperations() < 0 ? rightX - fw : leftX,
                    y
            );
            cs.showText(ft);
            cs.endText();

            float blockBottom = y - 10;

            // ---------- LEDGER LINES ----------
            cs.moveTo(centerX, blockTop);
            cs.lineTo(centerX, blockBottom);

            cs.moveTo(leftX, blockTop);
            cs.lineTo(leftX, blockBottom);

            cs.moveTo(rightX, blockTop);
            cs.lineTo(rightX, blockBottom);
            cs.stroke();

            y -= 40;
        }

        cs.close();
        return doc;
    }
}
