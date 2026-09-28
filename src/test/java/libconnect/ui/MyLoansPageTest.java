package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import libconnect.models.CopyStatus;
import libconnect.models.Loan;
import libconnect.services.ServiceException;
import libconnect.ui.pages.MyLoansPage;

/** Tests current-loan/history views and loan-service failures. */
class MyLoansPageTest {
    @Test
    void filtersAndSortsCurrentAndHistoryLoans() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                context.bookService().books.add(UiTestFixtures.book("978-1", "Title"));
                context.bookCopyService().copies.put("COPY-1", UiTestFixtures.copy("COPY-1",
                        "978-1", CopyStatus.BORROWED));
                context.bookCopyService().copies.put("COPY-2", UiTestFixtures.copy("COPY-2",
                        "978-1", CopyStatus.BORROWED));
                context.loanService().loans.addAll(List.of(
                        UiTestFixtures.returnedLoan("RETURNED", "COPY-2"),
                        UiTestFixtures.activeLoan("CURRENT", "COPY-1")));
                MyLoansPage page = new MyLoansPage(context.loanService(), context.bookService(),
                        context.bookCopyService(), context.borrowService(), context.sessionManager());
                assertTrue(UiTestSupport.labelTexts(page).contains("Active"));
                UiTestSupport.findButton(page, "Loan History").fire();
                assertTrue(UiTestSupport.labelTexts(page).contains("Returned"));
                assertTrue(UiTestSupport.findButton(page, "Loan History").isDisable());
                assertTrue(UiTestSupport.findButtons(page).stream()
                        .noneMatch(button -> "Return".equals(button.getText())));
            } finally {
                context.close();
            }
        });
    }

    @Test
    void repositoryFailureAndRenewalFailure_showErrors() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                context.loanService().loadFailure = UiPageTestSupport.repositoryFailure();
                MyLoansPage page = new MyLoansPage(context.loanService(), context.bookService(),
                        context.bookCopyService(), context.borrowService(), context.sessionManager());
                UiPageTestSupport.assertFeedback(page, "Unable to access loan data. Please try again.");

                context.loanService().loadFailure = null;
                context.loanService().loans.add(UiTestFixtures.activeLoan("CURRENT", "COPY-1"));
                context.bookService().books.add(UiTestFixtures.book("978-1", "Title"));
                context.bookCopyService().copies.put("COPY-1", UiTestFixtures.copy("COPY-1",
                        "978-1", CopyStatus.BORROWED));
                context.loanService().renewFailure = new ServiceException("Cannot renew.");
                page = new MyLoansPage(context.loanService(), context.bookService(),
                        context.bookCopyService(), context.borrowService(), context.sessionManager());
                UiTestSupport.findButton(page, "Renew").fire();
                UiPageTestSupport.assertFeedback(page, "Cannot renew.");
            } finally {
                context.close();
            }
        });
    }

    @Test
    void successfulRenewal_recordsLoanIdAndShowsSuccess() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                context.loanService().loans.add(UiTestFixtures.activeLoan("CURRENT", "COPY-1"));
                context.bookService().books.add(UiTestFixtures.book("978-1", "Title"));
                context.bookCopyService().copies.put("COPY-1", UiTestFixtures.copy("COPY-1",
                        "978-1", CopyStatus.BORROWED));
                MyLoansPage page = new MyLoansPage(context.loanService(), context.bookService(),
                        context.bookCopyService(), context.borrowService(), context.sessionManager());

                UiTestSupport.findButton(page, "Renew").fire();

                assertEquals("CURRENT", context.loanService().lastRenewedLoanId);
                UiPageTestSupport.assertFeedback(page, "The loan was renewed successfully.");
            } finally {
                context.close();
            }
        });
    }

    @Test
    void successfulReturn_updatesLoanAndCopyAndShowsSuccess() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                Loan loan = UiTestFixtures.activeLoan("CURRENT", "COPY-1");
                context.loanService().loans.add(loan);
                context.loanRepository().save(loan);
                context.bookService().books.add(UiTestFixtures.book("978-1", "Title"));
                context.bookCopyService().copies.put("COPY-1", UiTestFixtures.copy("COPY-1",
                        "978-1", CopyStatus.BORROWED));
                context.copyRepository().save(UiTestFixtures.copy("COPY-1", "978-1",
                        CopyStatus.BORROWED));
                MyLoansPage page = new MyLoansPage(context.loanService(), context.bookService(),
                        context.bookCopyService(), context.borrowService(), context.sessionManager());

                UiTestSupport.findButton(page, "Return").fire();

                assertEquals(libconnect.models.LoanStatus.RETURNED,
                        context.loanRepository().findById("CURRENT").orElseThrow().getStatus());
                assertEquals(CopyStatus.AVAILABLE,
                        context.copyRepository().findById("COPY-1").orElseThrow().getStatus());
                UiPageTestSupport.assertFeedback(page, "The book was returned successfully.");
            } finally {
                context.close();
            }
        });
    }

    @Test
    void unauthenticatedUser_cannotViewLoans() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                context.sessionManager().logout();
                MyLoansPage page = new MyLoansPage(context.loanService(), context.bookService(),
                        context.bookCopyService(), context.borrowService(), context.sessionManager());

                UiPageTestSupport.assertFeedback(page,
                        "Only an authenticated member can view loans.");
            } finally {
                context.close();
            }
        });
    }
}
