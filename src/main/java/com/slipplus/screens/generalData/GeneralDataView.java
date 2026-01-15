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
            slips.values().forEach(slip -> page.getChildren().add(buildPartyBlock(slip)));
        }

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);

        Scene scene = new Scene(
                new BorderPane(scroll),
                Screen.getPrimary().getBounds().getWidth(),
                Screen.getPrimary().getBounds().getHeight()
        );

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

        VBox outer = new VBox(12);
        outer.setStyle("-fx-border-color:black; -fx-border-width:0 1 0 1;");

        GridPane grid = new GridPane();
        configureLedgerGrid(grid);

        // ---------- HEADER ----------
        grid.add(
                createRow(
                        slip.getPartyName(),
                        String.format("%.0f", slip.getTotalBeforeOperations()),
                        Pos.CENTER_RIGHT
                ),
                2, 0
        );

        // ---------- OPERATIONS ----------
        List<MainSlip.Operation> minus = new ArrayList<>();
        List<MainSlip.Operation> plus = new ArrayList<>();

        slip.getOperations().forEach(op -> {
            if ("-".equals(op.getOperationType())) minus.add(op);
            else plus.add(op);
        });

        int rows = Math.max(minus.size(), plus.size());

        for (int i = 0; i < rows; i++) {

            if (i < minus.size()) {
                grid.add(
                        createRow(
                                minus.get(i).getDescription(),
                                String.format("%.0f", minus.get(i).getAmount()),
                                Pos.CENTER_LEFT
                        ),
                        0, i + 1
                );
            }

            if (i < plus.size()) {
                grid.add(
                        createRow(
                                plus.get(i).getDescription(),
                                String.format("%.0f", plus.get(i).getAmount()),
                                Pos.CENTER_RIGHT
                        ),
                        2, i + 1
                );
            }
        }

        // ---------- FINAL TOTAL ----------
        grid.add(
                createRow(
                        "Ranjodh",
                        String.format("%.0f", Math.abs(slip.getTotalAfterOperations())),
                        slip.getTotalAfterOperations() < 0 ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT,
                        true
                ),
                slip.getTotalAfterOperations() < 0 ? 2 : 0,
                rows + 1
        );

        // ---------- CENTER LINE ----------
        Rectangle centerLine = new Rectangle(2, 1, Color.BLACK);
        centerLine.heightProperty().bind(grid.heightProperty());
        grid.add(centerLine, 1, 0);
        GridPane.setRowSpan(centerLine, rows + 2);

        outer.getChildren().addAll(grid, new Separator());
        return outer;
    }

    // =========================================================
    // ROW FACTORY (KEY FIX)
    // =========================================================
    private HBox createRow(String text, String amount, Pos alignment) {
        return createRow(text, amount, alignment, false);
    }

    private HBox createRow(String text, String amount, Pos alignment, boolean bold) {

        Label desc = new Label(text);
        Label amt  = new Label(amount);

        desc.setStyle("-fx-font-size:22px;");
        amt.setStyle("-fx-font-size:22px;");

        if (bold) {
            desc.setStyle(desc.getStyle() + "-fx-font-weight:bold;");
            amt.setStyle(amt.getStyle() + "-fx-font-weight:bold;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox box = new HBox(10, desc, spacer, amt);
        box.setAlignment(alignment);
        box.setMaxWidth(Double.MAX_VALUE);

        return box;
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
    // PRINT + PDF (unchanged logic, already correct)
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
            new Alert(Alert.AlertType.ERROR, "Printing failed").showAndWait();
        }
    }

    private void saveGeneralDataPDF() {
        try {
            FileChooser fc = new FileChooser();
            fc.setInitialFileName("GeneralData_" + date + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

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

    private PDDocument createGeneralDataPDF() throws Exception {

        PDDocument doc = new PDDocument();
        PDType1Font font = PDType1Font.HELVETICA;

        // ================= PAGE CONSTANTS =================
        final float PAGE_WIDTH  = PDRectangle.A4.getWidth();
        final float PAGE_HEIGHT = PDRectangle.A4.getHeight();

        final float TOP_MARGIN    = 60;
        final float BOTTOM_MARGIN = 60;
        final float LEFT_MARGIN   = 40;
        final float RIGHT_MARGIN  = 40;

        final float USABLE_WIDTH = PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN;

        // ================= COLUMN ANCHORS =================
        final float LEFT_BORDER  = LEFT_MARGIN;
        final float RIGHT_BORDER = PAGE_WIDTH - RIGHT_MARGIN;
        final float CENTER_X     = LEFT_MARGIN + USABLE_WIDTH / 2;

        final float LEFT_DESC_X  = LEFT_BORDER + 12;
        final float LEFT_AMT_X   = CENTER_X - 12;

        final float RIGHT_DESC_X = CENTER_X + 12;
        final float RIGHT_AMT_X  = RIGHT_BORDER - 12;

        // ================= VERTICAL SPACING =================
        final float HEADER_GAP = 34;
        final float ROW_GAP    = 26;
        final float FINAL_GAP  = 26;
        final float BLOCK_GAP  = 40;

        // ================= CREATE FIRST PAGE =================
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page);

        float y = PAGE_HEIGHT - TOP_MARGIN;

        Map<String, MainSlip> slips =
                StorageManager.loadMainSlips().get(date.toString());

        for (MainSlip slip : slips.values()) {

            // ================= PAGE BREAK =================
            if (y < BOTTOM_MARGIN + 140) {
                cs.close();
                page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                cs = new PDPageContentStream(doc, page);
                y = PAGE_HEIGHT - TOP_MARGIN;
            }

            // ================= HEADER =================
            String headerDesc = slip.getPartyName();
            String headerAmt  = String.format("%.0f", slip.getTotalBeforeOperations());

            cs.setFont(font, 14);

            cs.beginText();
            cs.newLineAtOffset(RIGHT_DESC_X, y);
            cs.showText(headerDesc);
            cs.endText();

            float hw = font.getStringWidth(headerAmt) / 1000 * 14;
            cs.beginText();
            cs.newLineAtOffset(RIGHT_AMT_X - hw, y);
            cs.showText(headerAmt);
            cs.endText();

            float blockTop = y + 10;
            y -= HEADER_GAP;

            // ================= OPERATIONS =================
            List<MainSlip.Operation> minus = new ArrayList<>();
            List<MainSlip.Operation> plus  = new ArrayList<>();

            for (MainSlip.Operation op : slip.getOperations()) {
                if ("-".equals(op.getOperationType())) minus.add(op);
                else plus.add(op);
            }

            int rows = Math.max(1, Math.max(minus.size(), plus.size()));
            cs.setFont(font, 13);

            for (int i = 0; i < rows; i++) {

                // ---------- LEFT SIDE ----------
                if (i < minus.size()) {
                    MainSlip.Operation op = minus.get(i);

                    cs.beginText();
                    cs.newLineAtOffset(LEFT_DESC_X, y);
                    cs.showText(op.getDescription());
                    cs.endText();

                    String amt = String.format("%.0f", op.getAmount());
                    float w = font.getStringWidth(amt) / 1000 * 13;

                    cs.beginText();
                    cs.newLineAtOffset(LEFT_AMT_X - w, y);
                    cs.showText(amt);
                    cs.endText();
                }

                // ---------- RIGHT SIDE ----------
                if (i < plus.size()) {
                    MainSlip.Operation op = plus.get(i);

                    cs.beginText();
                    cs.newLineAtOffset(RIGHT_DESC_X, y);
                    cs.showText(op.getDescription());
                    cs.endText();

                    String amt = String.format("%.0f", op.getAmount());
                    float w = font.getStringWidth(amt) / 1000 * 13;

                    cs.beginText();
                    cs.newLineAtOffset(RIGHT_AMT_X - w, y);
                    cs.showText(amt);
                    cs.endText();
                }

                y -= ROW_GAP;
            }

            // ================= FINAL TOTAL =================

            String finalDesc = "Ranjodh";
            String finalAmt  = String.format("%.0f",
                    Math.abs(slip.getTotalAfterOperations()));

            boolean isNegative = slip.getTotalAfterOperations() < 0;

            float descX = isNegative ? RIGHT_DESC_X : LEFT_DESC_X;
            float amtX  = isNegative ? RIGHT_AMT_X  : LEFT_AMT_X;

            cs.setFont(font, 14);

            cs.beginText();
            cs.newLineAtOffset(descX, y);
            cs.showText(finalDesc);
            cs.endText();

            float fw = font.getStringWidth(finalAmt) / 1000 * 14;
            cs.beginText();
            cs.newLineAtOffset(amtX - fw, y);
            cs.showText(finalAmt);
            cs.endText();

            float blockBottom = y - 10;

            // ================= LEDGER LINES =================
            cs.moveTo(LEFT_BORDER, blockTop);
            cs.lineTo(LEFT_BORDER, blockBottom);

            cs.moveTo(CENTER_X, blockTop);
            cs.lineTo(CENTER_X, blockBottom);

            cs.moveTo(RIGHT_BORDER, blockTop);
            cs.lineTo(RIGHT_BORDER, blockBottom);

            cs.stroke();

            y -= BLOCK_GAP;
        }

        cs.close();
        return doc;
    }


}
