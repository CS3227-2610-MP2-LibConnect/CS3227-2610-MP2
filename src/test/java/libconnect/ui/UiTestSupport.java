package libconnect.ui;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;

/** Provides JavaFX-thread and scene-tree helpers shared by UI tests. */
public final class UiTestSupport {
    private static final long FX_TIMEOUT_SECONDS = 10;
    private static final Object TOOLKIT_LOCK = new Object();
    private static boolean toolkitStarted;

    private UiTestSupport() {
    }

    public static <T> T runOnFxThread(Callable<T> action) {
        startToolkit();
        if (Platform.isFxApplicationThread()) {
            return call(action);
        }

        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.call());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                completed.countDown();
            }
        });
        await(completed);
        if (failure.get() != null) {
            throw new RuntimeException("JavaFX test action failed", failure.get());
        }
        return result.get();
    }

    public static void runOnFxThread(Runnable action) {
        runOnFxThread(() -> {
            action.run();
            return null;
        });
    }

    public static Button findButton(Parent root, String text) {
        return findNodes(root, Button.class).stream()
                .filter(button -> text.equals(button.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Button not found: " + text));
    }

    public static List<Button> findButtons(Parent root) {
        return findNodes(root, Button.class);
    }

    public static List<TextField> findTextFields(Parent root) {
        return findNodes(root, TextField.class);
    }

    public static List<String> labelTexts(Parent root) {
        return findNodes(root, Label.class).stream().map(Label::getText).toList();
    }

    public static <T extends Node> List<T> findNodes(Parent root, Class<T> nodeType) {
        List<T> nodes = new ArrayList<>();
        collectNodes(root, nodeType, nodes);
        return nodes;
    }

    private static <T extends Node> void collectNodes(Node node, Class<T> nodeType,
                                                       List<T> nodes) {
        if (nodeType.isInstance(node)) {
            nodes.add(nodeType.cast(node));
        }
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> collectNodes(child, nodeType, nodes));
        }
        if (node instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            collectNodes(scrollPane.getContent(), nodeType, nodes);
        }
    }

    private static void startToolkit() {
        synchronized (TOOLKIT_LOCK) {
            if (toolkitStarted) {
                return;
            }
            CountDownLatch started = new CountDownLatch(1);
            try {
                Platform.startup(started::countDown);
                await(started);
                Platform.setImplicitExit(false);
            } catch (IllegalStateException exception) {
                toolkitStarted = true;
            }
            toolkitStarted = true;
        }
    }

    private static <T> T call(Callable<T> action) {
        try {
            return action.call();
        } catch (Exception exception) {
            throw new RuntimeException("JavaFX test action failed", exception);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                fail("Timed out waiting for JavaFX test action");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for JavaFX test action", exception);
        }
    }
}
