package com.slipplus.screens.subSlip;

import com.slipplus.models.Party;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SlipContext {

    // Layout
    BorderPane root;
    StackPane overlay;
    Scene scene;

    // Font / formatting
    DoubleBinding fontSize;
    DecimalFormat moneyFmt = new DecimalFormat("#,##0");

    // Top fields
    TextField partyField;
    TextField truckField;

    // Left-side fields
    TextField price1Field;
    TextField price2Field;
    TextField mainWeightField;

    // Right side
    VBox rightContainer;
    ScrollPane rightScrollPane;
    VBox swArea;
    VBox totalsArea;
    VBox leftContainer;
    VBox priceStack;

    // Dynamic SW/price/quality fields
    List<TextField> swLive = new ArrayList<>();
    List<TextField> swFields = new ArrayList<>();
    List<TextField> priceFields = new ArrayList<>();
    List<TextField> qualityFields = new ArrayList<>();

    ComboBox<String> dustDiscountBox;
    TextField gstField;

    // Bottom buttons
    HBox bottomButtons;
    Button saveButton;
    Button resetButton;

    // Loader
    StackPane loaderOverlay;

    // Party
    Party selectedParty;

    // State
    boolean subweightsFinished = false;
}