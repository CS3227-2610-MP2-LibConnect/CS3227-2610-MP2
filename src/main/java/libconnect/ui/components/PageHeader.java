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
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("page-subtitle");

        setAlignment(Pos.CENTER);
        setSpacing(SPACING);
        getChildren().addAll(titleLabel, subtitleLabel);
    }
}
