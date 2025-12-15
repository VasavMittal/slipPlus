package com.slipplus.screens.subSlipViewer;

import javafx.stage.Stage;
import java.util.Stack;

public class PopupStack {
    
    private Stack<BasePopup> popups;
    private Stage parentStage;
    
    public PopupStack(Stage parentStage) {
        this.parentStage = parentStage;
        this.popups = new Stack<>();
    }
    
    public void push(BasePopup popup) {
        // Close current popup if exists
        if (!popups.isEmpty()) {
            popups.peek().close();
        }
        popups.push(popup);
    }
    
    public void pop() {
        if (!popups.isEmpty()) {
            BasePopup current = popups.pop();
            current.close();
        }
    }
    
    public void closeAll() {
        while (!popups.isEmpty()) {
            pop();
        }
    }
    
    public boolean isEmpty() {
        return popups.isEmpty();
    }
}