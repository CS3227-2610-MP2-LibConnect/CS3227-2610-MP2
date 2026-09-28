package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import libconnect.ui.UiTestSupport;

/** Tests feedback message visibility, text, and styles. */
class FeedbackMessageTest {
    @Test
    void defaultState_isNotVisibleAndManaged() {
        UiTestSupport.runOnFxThread(() -> {
            FeedbackMessage feedback = new FeedbackMessage();
            assertFalse(feedback.isVisible());
            assertFalse(feedback.isManaged());
        });
    }

    @Test
    void onError_isVisibleAndManagedWithErrorStyle() {
        UiTestSupport.runOnFxThread(() -> {
            FeedbackMessage feedback = new FeedbackMessage();
            feedback.showError("Error");
            assertTrue(feedback.isVisible());
            assertTrue(feedback.isManaged());
            assertTrue(feedback.getStyleClass().contains("feedback-error"));
        });
    }

    @Test
    void onSuccess_isVisibleAndManagedWithSuccessStyle() {
        UiTestSupport.runOnFxThread(() -> {
            FeedbackMessage feedback = new FeedbackMessage();
            feedback.showSuccess("Success");
            assertTrue(feedback.isVisible());
            assertTrue(feedback.isManaged());
            assertTrue(feedback.getStyleClass().contains("feedback-success"));
        });
    }

    @Test 
    void onClear_isNotVisibleAndManaged() {
        UiTestSupport.runOnFxThread(() -> {
            FeedbackMessage feedback = new FeedbackMessage();
            feedback.showError("Error");
            feedback.clearMessage();
            assertEquals("", feedback.getText());
            assertFalse(feedback.isVisible());
            assertFalse(feedback.isManaged());
        });
    }

}
