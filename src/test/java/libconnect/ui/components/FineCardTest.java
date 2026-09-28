package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import libconnect.models.Fine;
import libconnect.models.FineStatus;
import libconnect.ui.UiTestSupport;

/** Tests fine details, payment action visibility, and callback behavior. */
class FineCardTest {
    private static final LocalDate ISSUED_DATE = LocalDate.of(2026, 9, 21);

    @Test
    void displaysFineDetailsAndFormatsIssuedDate() {
        UiTestSupport.runOnFxThread(() -> {
            Fine fine = createFine("FINE-1", FineStatus.OUTSTANDING);
            FineCard card = new FineCard(fine, "The Great Book", false, () -> {
            });

            assertTrue(UiTestSupport.labelTexts(card).contains("Fine FINE-1"));
            assertTrue(UiTestSupport.labelTexts(card).contains("The Great Book"));
            assertTrue(UiTestSupport.labelTexts(card).contains("$12.50"));
            assertTrue(UiTestSupport.labelTexts(card).contains("Overdue loan"));
            assertTrue(UiTestSupport.labelTexts(card).contains(
                    ISSUED_DATE.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));
            assertTrue(UiTestSupport.labelTexts(card).contains("OUTSTANDING"));
        });
    }

    @Test
    void payActionIsShownAndInvokedWhenEnabled() {
        UiTestSupport.runOnFxThread(() -> {
            AtomicInteger invocations = new AtomicInteger();
            FineCard card = new FineCard(createFine("FINE-1", FineStatus.OUTSTANDING),
                    "The Great Book", true, invocations::incrementAndGet);

            assertFalse(UiTestSupport.findButton(card, "Pay Fine").isDisable());
            UiTestSupport.findButton(card, "Pay Fine").fire();
            assertEquals(1, invocations.get());
            assertTrue(card.getStyleClass().contains("fine-card"));
        });
    }

    @Test
    void payActionIsHiddenWhenDisabled() {
        UiTestSupport.runOnFxThread(() -> {
            FineCard card = new FineCard(createFine("FINE-1", FineStatus.PAID),
                    "The Great Book", false, null);

            assertTrue(UiTestSupport.findButtons(card).isEmpty());
        });
    }

    @Test
    void requiredArgumentsAreValidated() {
        UiTestSupport.runOnFxThread(() -> {
            Fine fine = createFine("FINE-1", FineStatus.OUTSTANDING);
            assertThrows(NullPointerException.class,
                    () -> new FineCard(null, "The Great Book", false, null));
            assertThrows(NullPointerException.class,
                    () -> new FineCard(fine, null, false, null));
            assertThrows(NullPointerException.class,
                    () -> new FineCard(fine, "The Great Book", true, null));
        });
    }

    private static Fine createFine(String fineId, FineStatus status) {
        return new Fine(fineId, "LOAN-1", "MEM-1", new BigDecimal("12.50"),
                "Overdue loan", status, ISSUED_DATE, "The Great Book");
    }
}
