package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javafx.scene.control.Label;

import org.junit.jupiter.api.Test;

import libconnect.ui.UiTestSupport;

/** Tests page-container child composition and sizing. */
class PageContainerTest {
    @Test
    void preservesChildrenAndMaximumWidth() {
        UiTestSupport.runOnFxThread(() -> {
            Label first = new Label("first");
            Label second = new Label("second");
            PageContainer container = new PageContainer(first, second);
            assertEquals(List.of(first, second), container.getChildren());
            assertEquals(420, container.getMaxWidth());
        });
    }
}
