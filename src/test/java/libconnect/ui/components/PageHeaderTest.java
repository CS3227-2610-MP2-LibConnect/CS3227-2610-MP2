package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import libconnect.ui.UiTestSupport;

/** Tests page-header title and subtitle rendering. */
class PageHeaderTest {
    @Test
    void rendersTitleAndSubtitle() {
        UiTestSupport.runOnFxThread(() -> {
            PageHeader header = new PageHeader("Title", "Subtitle");
            assertEquals(List.of("Title", "Subtitle"), UiTestSupport.labelTexts(header));
        });
    }
}
