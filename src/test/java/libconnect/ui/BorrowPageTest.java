package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.Parent;
import javafx.scene.control.TextField;

import org.junit.jupiter.api.Test;

import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.ui.pages.BorrowPage;
import libconnect.ui.pages.DashboardPage;

/** Tests borrowing validation, selection limits, and confirmation navigation. */
class BorrowPageTest {
    @Test
    void addBook_AddsBookToList() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                BookCopy copy = UiTestFixtures.copy("COPY-1", "978-1", CopyStatus.AVAILABLE);
                context.bookCopyService().copies.put(copy.getCopyId(), copy);
                context.bookService().books.add(UiTestFixtures.book("978-1", "Title"));
                BorrowPage page = new BorrowPage(context.bookService(), context.bookCopyService(),
                        context.borrowService(), context.sessionManager(), context.navigator());
                TextField field = UiTestSupport.findTextFields(page).get(0);
                field.setText("COPY-1");
                UiTestSupport.findButton(page, "Add Book").fire();
                assertTrue(UiTestSupport.labelTexts(page).stream()
                        .anyMatch(text -> text.contains("COPY-1")));
            } finally {
                context.close();
            }
        });
    }

    @Test 
    void addBook_allowsAdditionOfUpToTenBooks() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                for (int index = 1; index <= 10; index++) {
                    String copyId = "COPY-" + index;
                    BookCopy copy = UiTestFixtures.copy(copyId, "978-1", CopyStatus.AVAILABLE);
                    context.bookCopyService().copies.put(copyId, copy);
                }
                context.bookService().books.add(UiTestFixtures.book("978-1", "Title"));
                BorrowPage page = new BorrowPage(context.bookService(), context.bookCopyService(),
                        context.borrowService(), context.sessionManager(), context.navigator());
                TextField copyField = UiTestSupport.findTextFields(page).get(0);
                for (int index = 1; index <= 10; index++) {
                    assertFalse(UiTestSupport.findButton(page, "Add Book").isDisable());
                    copyField.setText("COPY-" + index);
                    UiTestSupport.findButton(page, "Add Book").fire();
                }
            } finally {
                context.close();
            }
        });
    }

    @Test 
    void addBook_preventsAdditionOfMoreThanTenBooks() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                for (int index = 1; index <= 10; index++) {
                    String copyId = "COPY-" + index;
                    BookCopy copy = UiTestFixtures.copy(copyId, "978-1", CopyStatus.AVAILABLE);
                    context.bookCopyService().copies.put(copyId, copy);
                }
                context.bookService().books.add(UiTestFixtures.book("978-1", "Title"));
                BorrowPage page = new BorrowPage(context.bookService(), context.bookCopyService(),
                        context.borrowService(), context.sessionManager(), context.navigator());
                TextField copyField = UiTestSupport.findTextFields(page).get(0);
                for (int index = 1; index <= 10; index++) {
                    copyField.setText("COPY-" + index);
                    UiTestSupport.findButton(page, "Add Book").fire();
                }
                assertTrue(UiTestSupport.findButton(page, "Add Book").isDisable());
            } finally {
                context.close();
            }
        });
    }

    @Test 
    void removeBook_RemovesBookFromList() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                BookCopy copy = UiTestFixtures.copy("COPY-1", "978-1", CopyStatus.AVAILABLE);
                context.bookCopyService().copies.put(copy.getCopyId(), copy);
                context.bookService().books.add(UiTestFixtures.book("978-1", "Title"));
                BorrowPage page = new BorrowPage(context.bookService(), context.bookCopyService(),
                        context.borrowService(), context.sessionManager(), context.navigator());
                TextField field = UiTestSupport.findTextFields(page).get(0);
                field.setText("COPY-1");
                UiTestSupport.findButton(page, "Add Book").fire();
                assertTrue(UiTestSupport.labelTexts(page).stream()
                        .anyMatch(text -> text.contains("COPY-1")));
                UiTestSupport.findButton(page, "Remove").fire();
                assertFalse(UiTestSupport.labelTexts(page).stream()
                        .anyMatch(text -> text.contains("COPY-1")));
            } finally {
                context.close();
            }
        });
    }

    

    @Test
    void addBook_invalidCopyAndUnavailableCopy_showErrors() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                context.bookService().books.add(UiTestFixtures.book("978-1", "Title"));
                BookCopy copy = UiTestFixtures.copy("COPY-1", "978-1", CopyStatus.BORROWED);
                context.bookCopyService().copies.put(copy.getCopyId(), copy);
                BorrowPage page = new BorrowPage(context.bookService(), context.bookCopyService(),
                        context.borrowService(), context.sessionManager(), context.navigator());
                TextField field = UiTestSupport.findTextFields(page).get(0);
                field.setText("UNKNOWN");
                UiTestSupport.findButton(page, "Add Book").fire();
                assertTrue(UiTestSupport.labelTexts(page).stream()
                        .anyMatch(text -> text.contains("Book copy not found")));
                field.setText("COPY-1");
                UiTestSupport.findButton(page, "Add Book").fire();
                assertTrue(UiTestSupport.labelTexts(page).stream()
                        .anyMatch(text -> text.contains("cannot be borrowed")));
            } finally {
                context.close();
            }
        });
    }

    @Test 
    void confirmBorrowing_noBooksSelected_butttonIsDisabled() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                BorrowPage page = new BorrowPage(context.bookService(), context.bookCopyService(),
                        context.borrowService(), context.sessionManager(), context.navigator());
                assertTrue(UiTestSupport.findButton(page, "Confirm Borrowing").isDisable());
            } finally {
                context.close();
            }
        });
    }

    @Test
    void confirmBorrowing_successfulConfirmation_navigatesToDashboard() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                context.bookService().books.add(UiTestFixtures.book("978-1", "Title"));
                BookCopy copy = UiTestFixtures.copy("COPY-1", "978-1", CopyStatus.AVAILABLE);
                context.bookCopyService().copies.put(copy.getCopyId(), copy);
                context.copyRepository().copies.put(copy.getCopyId(), copy);
                BorrowPage page = new BorrowPage(context.bookService(), context.bookCopyService(),
                        context.borrowService(), context.sessionManager(), context.navigator());
                TextField field = UiTestSupport.findTextFields(page).get(0);
                field.setText("COPY-1");
                UiTestSupport.findButton(page, "Add Book").fire();
                UiTestSupport.findButton(page, "Confirm Borrowing").fire();
                assertInstanceOf(DashboardPage.class, context.stage().getScene().getRoot());
                assertTrue(UiTestSupport.labelTexts((Parent) context.stage().getScene().getRoot()).stream()
                        .anyMatch(text -> text.contains("Successfully borrowed 1 book")));
            } finally {
                context.close();
            }
        });
    }
}
