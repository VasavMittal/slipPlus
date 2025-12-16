package com.slipplus.screens.mainSlip;

import com.slipplus.core.AppNavigator;
import com.slipplus.core.StorageManager;
import com.slipplus.screens.subSlipViewer.DateSelectionPopup;
import com.slipplus.screens.subSlipViewer.PartySelectionPopup;
import com.slipplus.screens.subSlipViewer.PopupStack;

import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.List;

public class MainSlipViewerScreen {
    
    private Stage stage;
    private PopupStack popupStack;
    private MainSlipScreen originalMainSlipScreen; // Add reference to original screen
    
    // Add constructor that accepts original screen
    public MainSlipViewerScreen(MainSlipScreen originalScreen) {
        this.originalMainSlipScreen = originalScreen;
    }
    
    public MainSlipViewerScreen() {
        this.originalMainSlipScreen = null;
    }
    
    public void start(Stage stage) {
        this.stage = stage;
        this.popupStack = new PopupStack(stage);
        
        // Check if data exists
        List<LocalDate> availableDates = StorageManager.getAvailableDates();
        
        if (availableDates.isEmpty()) {
            showNoDataMessage();
            return;
        }
        
        if (availableDates.size() == 1) {
            // Only one date, skip date selection
            LocalDate singleDate = availableDates.get(0);
            onDateSelected(singleDate);
        } else {
            // Multiple dates, show date selection
            showDateSelectionPopup();
        }
    }
    
    private void showDateSelectionPopup() {
        DateSelectionPopup datePopup = new DateSelectionPopup(stage, this::onDateSelected, this::onExit);
        popupStack.push(datePopup);
        datePopup.show();
    }
    
    private void onDateSelected(LocalDate selectedDate) {
        List<String> partyIds = StorageManager.getPartyIdsForDate(selectedDate);
        
        if (partyIds.isEmpty()) {
            showNoDataMessage();
            return;
        }
        
        if (partyIds.size() == 1) {
            // Only one party, skip party selection and open main slip directly
            String singlePartyId = partyIds.get(0);
            String partyName = StorageManager.getPartyNameById(singlePartyId);
            onPartySelected(selectedDate, partyName);
        } else {
            // Multiple parties, show party selection
            PartySelectionPopup partyPopup = new PartySelectionPopup(stage, selectedDate, 
                this::onPartySelected, 
                () -> {
                    // When ESC is pressed in party popup
                    popupStack.pop(); // Remove party popup
                    
                    // If we came from MainSlipScreen (F2), go back to it
                    if (originalMainSlipScreen != null) {
                        popupStack.closeAll();
                        originalMainSlipScreen.resetPopupFlag(); // Reset the popup flag
                        originalMainSlipScreen.start(stage); // Return to original screen
                        return;
                    }
                    
                    // Otherwise, check if we should show date selection or exit
                    List<LocalDate> availableDates = StorageManager.getAvailableDates();
                    
                    if (availableDates.size() > 1) {
                        // Multiple dates available, show date selection
                        showDateSelectionPopup();
                    } else {
                        // Only one date or no dates, go to main screen
                        onExit();
                    }
                });
            popupStack.push(partyPopup);
            partyPopup.show();
        }
    }
    
    private void onPartySelected(LocalDate date, String partyName) {
        // Close all popups and open main slip screen
        popupStack.closeAll();
        
        MainSlipScreen mainSlipScreen = new MainSlipScreen(date, partyName);
        mainSlipScreen.start(stage);
    }
    
    private void onBackToDate() {
        popupStack.pop(); // Remove party popup
        
        // Check if there are still dates available
        List<LocalDate> availableDates = StorageManager.getAvailableDates();
        
        if (availableDates.isEmpty()) {
            // No dates left, go to main screen
            onExit();
        } else if (availableDates.size() == 1) {
            // Only one date left, go to main screen to avoid auto-opening
            onExit();
        } else {
            // Multiple dates, show date selection
            showDateSelectionPopup();
        }
    }
    
    private void onExit() {
        popupStack.closeAll();
        AppNavigator.startApp(stage);
    }
    
    private void showNoDataMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("No Data");
        alert.setHeaderText(null);
        alert.setContentText("No sub-slip data available for Main Slip creation.");
        alert.initOwner(stage);
        alert.showAndWait();
        
        AppNavigator.startApp(stage);
    }
}


