package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javafx.scene.Parent;
import javafx.stage.Stage;

import libconnect.services.BorrowService;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.components.FeedbackMessage;

/** Supplies shared context and assertions for page-level UI tests. */
final class UiPageTestSupport {
    private UiPageTestSupport() {
    }

    static void fillLoginFields(libconnect.ui.pages.LoginPage page) {
        List<javafx.scene.control.TextField> fields = UiTestSupport.findTextFields(page);
        fields.get(0).setText("alex@example.com");
        fields.get(1).setText("secret");
    }

    static void assertFeedback(Parent root, String expectedText) {
        assertTrue(UiTestSupport.findNodes(root, FeedbackMessage.class).stream()
                .anyMatch(feedback -> expectedText.equals(feedback.getText())
                        && feedback.isVisible()));
    }

    static RepositoryException repositoryFailure() {
        return new RepositoryException("storage failure", new IllegalStateException("failure"));
    }

    static TestContext context() {
        Stage stage = new Stage();
        SessionManager sessionManager = new SessionManager();
        sessionManager.login(UiTestFixtures.member());
        UiTestFixtures.TestAuthenticationService authenticationService =
                new UiTestFixtures.TestAuthenticationService();
        UiTestFixtures.TestBookService bookService = new UiTestFixtures.TestBookService();
        UiTestFixtures.TestBookCopyService bookCopyService = new UiTestFixtures.TestBookCopyService();
        UiTestFixtures.InMemoryBookCopyRepository copyRepository =
                new UiTestFixtures.InMemoryBookCopyRepository();
        UiTestFixtures.InMemoryLoanRepository loanRepository =
                new UiTestFixtures.InMemoryLoanRepository();
        BorrowService borrowService = new BorrowService(copyRepository, loanRepository);
        UiTestFixtures.TestLoanService loanService = new UiTestFixtures.TestLoanService();
        UiTestFixtures.TestMemberService memberService = new UiTestFixtures.TestMemberService();
        SceneNavigator navigator = UiTestFixtures.navigator(stage, sessionManager,
                authenticationService, bookService, bookCopyService, borrowService, loanService,
                memberService);
        return new TestContext(stage, sessionManager, authenticationService, bookService,
                bookCopyService, copyRepository, borrowService, loanService, memberService,
                navigator);
    }

    record TestContext(Stage stage, SessionManager sessionManager,
                       UiTestFixtures.TestAuthenticationService authenticationService,
                       UiTestFixtures.TestBookService bookService,
                       UiTestFixtures.TestBookCopyService bookCopyService,
                       UiTestFixtures.InMemoryBookCopyRepository copyRepository,
                       BorrowService borrowService,
                       UiTestFixtures.TestLoanService loanService,
                       UiTestFixtures.TestMemberService memberService,
                       SceneNavigator navigator) {
        void close() {
            stage.close();
        }
    }
}
