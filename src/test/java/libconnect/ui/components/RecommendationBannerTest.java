package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import libconnect.ui.UiTestFixtures;
import libconnect.ui.UiTestSupport;

/** Tests empty and deterministic recommendation banner content. */
class RecommendationBannerTest {
    @Test
    void displaysEmptyAndDeterministicRecommendation() {
        UiTestSupport.runOnFxThread(() -> {
            RecommendationBanner banner = new RecommendationBanner();
            banner.showRecommendation(List.of());
            assertTrue(UiTestSupport.labelTexts(banner).contains("no books in database"));
            banner.showRecommendation(List.of(UiTestFixtures.book("978-1", "Title")));
            assertTrue(UiTestSupport.labelTexts(banner).stream()
                    .anyMatch(text -> text.contains("Try \"Title\" by Author (2020).")));
        });
    }
}
