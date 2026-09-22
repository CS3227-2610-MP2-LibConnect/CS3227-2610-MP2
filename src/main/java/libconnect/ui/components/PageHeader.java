package libconnect.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Displays a consistent title and optional subtitle for an application page. */
public final class PageHeader extends VBox {
    private static final double SPACING = 6;

    /**
     * Creates a page header with a title and subtitle.
     *
     * @param title the main page title.
     * @param subtitle the supporting page description.
     */
    public PageHeader(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-text-fill: #666666;");

        setAlignment(Pos.CENTER);
        setSpacing(SPACING);
        getChildren().addAll(titleLabel, subtitleLabel);
    }
}
