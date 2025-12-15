package com.slipplus.screens.subSlipViewer;

import com.slipplus.core.AppNavigator;
import com.slipplus.core.StorageManager;

import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.List;

public class SubSlipViewerScreen {
    
    private Stage stage;
    private PopupStack popupStack;
    
    public void start(Stage stage) {
        this.stage = stage;
        this.popupStack = new PopupStack(stage);
        
        // Start the popup sequence
        showDateSelectionPopup();
    }
    
    private void showDateSelectionPopup() {
        DateSelectionPopup datePopup = new DateSelectionPopup(stage, this::onDateSelected, this::onExit);
        popupStack.push(datePopup);
        datePopup.show();
    }
    
    private void onDateSelected(LocalDate selectedDate) {
        PartySelectionPopup partyPopup = new PartySelectionPopup(stage, selectedDate, 
            this::onPartySelected, this::onBackToDate);
        popupStack.push(partyPopup);
        partyPopup.show();
    }
    
    private void onPartySelected(LocalDate date, String partyName) {
        DataViewPopup dataPopup = new DataViewPopup(stage, date, partyName, 
            this::onDataAction, this::onBackToParty);
        popupStack.push(dataPopup);
        dataPopup.show();
    }
    
    private void onDataAction(LocalDate date, String partyName) {
        // After delete action, go back to party selection
        popupStack.pop(); // Remove data popup
        onDateSelected(date); // Refresh party popup
    }
    
    private void onBackToParty(LocalDate date) {
        popupStack.pop(); // Remove data popup
        
        // Check if there are still parties for this date
        List<String> availableParties = StorageManager.getPartiesForDate(date);
        
        if (availableParties.isEmpty()) {
            // No parties left, go back to date selection
            onBackToDate();
        } else if (availableParties.size() == 1) {
            // Only one party left, go back to date selection to avoid auto-opening
            onBackToDate();
        } else {
            // Multiple parties, show party selection
            PartySelectionPopup partyPopup = new PartySelectionPopup(stage, date, 
                this::onPartySelected, this::onBackToDate);
            popupStack.push(partyPopup);
            partyPopup.show();
        }
    }
    
    private void onBackToDate() {
        popupStack.pop(); // Remove current popup
        
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
}

