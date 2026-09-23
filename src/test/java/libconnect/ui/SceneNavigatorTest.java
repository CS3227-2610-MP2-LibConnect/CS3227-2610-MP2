package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests application-level UI configuration shared by navigated pages. */
class SceneNavigatorTest {
    @Test
    void sharedStylesheet_isLoadedIntoApplicationScene() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                assertTrue(context.stage().getScene().getStylesheets().stream()
                        .anyMatch(stylesheet -> stylesheet.endsWith("/libconnect.css")));
            } finally {
                context.close();
            }
        });
    }
}
