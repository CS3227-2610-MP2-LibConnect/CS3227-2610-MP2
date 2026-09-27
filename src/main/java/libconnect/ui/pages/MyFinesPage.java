package libconnect.ui.pages;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import libconnect.models.Fine;
import libconnect.models.FineStatus;
import libconnect.models.Member;
import libconnect.models.User;
import libconnect.services.FineService;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.SessionManager;
import libconnect.ui.components.FeedbackMessage;
import libconnect.ui.components.FineCard;
import libconnect.ui.components.PageHeader;

/** Provides the authenticated member's outstanding fines and payment history. */
public final class MyFinesPage extends VBox {
    private static final String STORAGE_ERROR_MESSAGE =
            "Unable to access fine data. Please try again.";
    private static final String ACTION_ERROR_MESSAGE =
            "Unable to pay the fine. Please try again.";
    private final FineService fineService;
    private final SessionManager sessionManager;
    private final VBox fineList;
    private final FeedbackMessage feedbackMessage;
    private final Button outstandingButton;
    private final Button historyButton;
    private FineView selectedView;

    private enum FineView {
        OUTSTANDING,
        HISTORY
    }

    /**
     * Creates a my fines page backed by the supplied service and session.
     *
     * @param fineService the service used to load and pay fines.
     * @param sessionManager the session containing the authenticated member.
     * @throws NullPointerException if an argument is null.
     */
    public MyFinesPage(FineService fineService, SessionManager sessionManager) {
        this.fineService = Objects.requireNonNull(fineService, "fineService");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
        fineList = new VBox(12);
        feedbackMessage = new FeedbackMessage();
        outstandingButton = new Button("Outstanding");
        historyButton = new Button("History");
        selectedView = FineView.OUTSTANDING;

        configureControls();
        VBox content = createContent();
        VBox.setVgrow(content, Priority.ALWAYS);
        getChildren().add(content);
        refreshFines();
    }

    private VBox createContent() {
        HBox tabs = new HBox(8, outstandingButton, historyButton);
        tabs.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(16,
                new PageHeader("My Fines", "View and pay your fines"),
                tabs,
                feedbackMessage,
                fineList);
        content.setPadding(new Insets(20));
        VBox.setVgrow(fineList, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox wrapper = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return wrapper;
    }

    private void configureControls() {
        outstandingButton.setOnAction(event -> showView(FineView.OUTSTANDING));
        historyButton.setOnAction(event -> showView(FineView.HISTORY));
        updateTabStyles();
    }

    private void showView(FineView view) {
        selectedView = view;
        updateTabStyles();
        refreshFines();
    }

    private void updateTabStyles() {
        outstandingButton.setDisable(selectedView == FineView.OUTSTANDING);
        historyButton.setDisable(selectedView == FineView.HISTORY);
        updateTabStyle(outstandingButton, selectedView == FineView.OUTSTANDING);
        updateTabStyle(historyButton, selectedView == FineView.HISTORY);
    }

    private void updateTabStyle(Button button, boolean isSelected) {
        if (isSelected) {
            if (!button.getStyleClass().contains("selected-tab")) {
                button.getStyleClass().add("selected-tab");
            }
        } else {
            button.getStyleClass().remove("selected-tab");
        }
    }

    private void refreshFines() {
        fineList.getChildren().clear();
        User currentUser = sessionManager.getCurrentUser();
        if (!(currentUser instanceof Member member)) {
            showError("Only an authenticated member can view fines.");
            return;
        }

        try {
            List<Fine> fines = fineService.getMemberFines(member.getMembershipId());
            List<Fine> visibleFines = selectedView == FineView.OUTSTANDING
                    ? getOutstandingFines(fines)
                    : getPaidFines(fines);
            if (visibleFines.isEmpty()) {
                fineList.getChildren().add(new Label(selectedView == FineView.OUTSTANDING
                        ? "You have no outstanding fines."
                        : "You have no fine history."));
                return;
            }

            visibleFines.stream()
                    .map(fine -> createFineCard(fine, member.getMembershipId()))
                    .forEach(fineList.getChildren()::add);
        } catch (RepositoryException exception) {
            showError(STORAGE_ERROR_MESSAGE);
        }
    }

    private List<Fine> getOutstandingFines(List<Fine> fines) {
        return getFinesByStatus(fines, FineStatus.OUTSTANDING);
    }

    private List<Fine> getPaidFines(List<Fine> fines) {
        return getFinesByStatus(fines, FineStatus.PAID);
    }

    private List<Fine> getFinesByStatus(List<Fine> fines, FineStatus status) {
        return fines.stream()
                .filter(fine -> fine.getStatus() == status)
                .sorted(Comparator.comparing(Fine::getIssuedDate).reversed())
                .toList();
    }

    private FineCard createFineCard(Fine fine, String memberId) {
        return new FineCard(fine, fineService.getBookTitle(fine),
                selectedView == FineView.OUTSTANDING,
                () -> payFine(fine, memberId));
    }

    private void payFine(Fine fine, String memberId) {
        try {
            fineService.payFine(fine.getId(), memberId);
            feedbackMessage.showSuccess("Fine paid successfully.");
            refreshFines();
        } catch (RepositoryException exception) {
            showError(STORAGE_ERROR_MESSAGE);
        } catch (RuntimeException exception) {
            showError(getActionErrorMessage(exception));
        }
    }

    private String getActionErrorMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? ACTION_ERROR_MESSAGE : exception.getMessage();
    }

    private void showError(String message) {
        feedbackMessage.showError(message);
    }
}
