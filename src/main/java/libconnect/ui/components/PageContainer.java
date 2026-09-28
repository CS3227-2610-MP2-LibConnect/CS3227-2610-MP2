package libconnect.ui.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** Provides a consistent centered vertical container for application pages. */
public final class PageContainer extends VBox {
    private static final double CONTAINER_WIDTH = 420;
    private static final double SPACING = 16;

    /**
     * Creates a page container containing the supplied child nodes.
     *
     * @param children the nodes displayed in the container.
     */
    public PageContainer(Node... children) {
        super(SPACING, children);
        setAlignment(Pos.CENTER);
        setMaxWidth(CONTAINER_WIDTH);
    }
}
