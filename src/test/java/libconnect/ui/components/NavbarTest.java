package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import libconnect.ui.UiTestSupport;

/** Tests dashboard navigation button state and callbacks. */
class NavbarTest {
    @Test
    void activePageDisablesOnlyMatchingNavigationButton() {
        UiTestSupport.runOnFxThread(() -> {
            AtomicInteger calls = new AtomicInteger();
            Navbar navbar = new Navbar(calls::incrementAndGet,
                    calls::incrementAndGet, calls::incrementAndGet, calls::incrementAndGet,
                    calls::incrementAndGet, Navbar.ActivePage.BORROW);
            UiTestSupport.findButtons(navbar).forEach(button -> {
                if (button.getText().equals("Borrow books")) {
                    assertTrue(button.isDisable());
                } else {
                    assertFalse(button.isDisable());
                }
            });
        });
    }

    @Test
    void navigationInvokesCallbackAndDisablesNewActivePageButton() {
        UiTestSupport.runOnFxThread(() -> {
            AtomicInteger calls = new AtomicInteger();
            Navbar navbar = new Navbar(calls::incrementAndGet,
                    calls::incrementAndGet, calls::incrementAndGet, calls::incrementAndGet,
                    calls::incrementAndGet, Navbar.ActivePage.BORROW);
            UiTestSupport.findButton(navbar, "Dashboard").fire();
            UiTestSupport.findButtons(navbar).forEach(button -> {
                if (!button.getText().equals("Dashboard")) {
                    assertTrue(button.isDisable());
                } else {
                    assertFalse(button.isDisable());
                }
            });
            assertEquals(1, calls.get());
        });
    }
}
