package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import libconnect.ui.UiTestFixtures;
import libconnect.ui.UiTestSupport;

/** Tests loan status, renewal, and action visibility. */
class LoanCardTest {
    @Test 
    void renewableLoan_isActiveIsReturnableAndRenewable() {
        UiTestSupport.runOnFxThread(() -> {
            LoanCard card = new LoanCard(UiTestFixtures.activeLoan("LOAN-1", "COPY-1"),
                    "Title", "Author", "978-1", true, () -> {}, () -> {});
            assertTrue(UiTestSupport.labelTexts(card).contains("Active"));
            assertFalse(UiTestSupport.findButton(card, "Renew").isDisable());
        });
    }

    @Test 
    void renewedLoan_isActiveIsReturnableAndNotRenewable() {
        UiTestSupport.runOnFxThread(() -> {
            LoanCard card = new LoanCard(UiTestFixtures.renewedLoan("LOAN-1", "COPY-1"),
                    "Title", "Author", "978-1", true, () -> {}, () -> {});
            assertTrue(UiTestSupport.labelTexts(card).contains("Active"));
            assertTrue(UiTestSupport.findButton(card, "Renew").isDisable());
            assertFalse(UiTestSupport.findButton(card, "Return").isDisable());
        });
    }

    @Test
    void returnedLoan_isReturnedAndHasNoActions() {
        UiTestSupport.runOnFxThread(() -> {
            LoanCard card = new LoanCard(UiTestFixtures.returnedLoan("LOAN-1", "COPY-1"),
                    "Title", "Author", "978-1", false, () -> {}, () -> {});
            assertTrue(UiTestSupport.labelTexts(card).contains("Returned"));
            assertTrue(UiTestSupport.findButtons(card).isEmpty());
        });
    }

    @Test 
    void overdueLoan_isOverdueIsReturnableAndNotRenewable() {
        UiTestSupport.runOnFxThread(() -> {
            LoanCard card = new LoanCard(UiTestFixtures.overdueLoan("LOAN-1", "COPY-1"),
                    "Title", "Author", "978-1", true, () -> {}, () -> {});
            assertTrue(UiTestSupport.labelTexts(card).contains("Overdue"));
            assertTrue(UiTestSupport.findButton(card, "Renew").isDisable());
            assertFalse(UiTestSupport.findButton(card, "Return").isDisable());
        });
    }
}
