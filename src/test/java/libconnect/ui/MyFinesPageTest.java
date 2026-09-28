package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import libconnect.integration.LoanQuery;
import libconnect.integration.LoanSummary;
import libconnect.integration.MemberDirectory;
import libconnect.models.Fine;
import libconnect.models.FineStatus;
import libconnect.models.Member;
import libconnect.services.FineService;
import libconnect.services.ServiceException;
import libconnect.storage.repositories.FineRepository;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.components.FineCard;
import libconnect.ui.pages.MyFinesPage;

/** Tests fine filtering, payment actions, sorting, and failure feedback. */
class MyFinesPageTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"),
            ZoneOffset.UTC);

    @Test
    void nonMemberCannotViewFines() {
        UiTestSupport.runOnFxThread(() -> {
            MyFinesPage page = new MyFinesPage(createFineService(new InMemoryFineRepository()),
                    new SessionManager());

            UiPageTestSupport.assertFeedback(page, "Only an authenticated member can view fines.");
            assertTrue(UiTestSupport.findNodes(page, FineCard.class).isEmpty());
        });
    }

    @Test
    void outstandingViewFiltersAndSortsFines() {
        UiTestSupport.runOnFxThread(() -> {
            InMemoryFineRepository repository = new InMemoryFineRepository();
            Fine olderOutstanding = fine("FINE-1", FineStatus.OUTSTANDING,
                    LocalDate.of(2026, 9, 1));
            Fine newerOutstanding = fine("FINE-2", FineStatus.OUTSTANDING,
                    LocalDate.of(2026, 9, 20));
            repository.save(olderOutstanding);
            repository.save(newerOutstanding);
            repository.save(fine("FINE-3", FineStatus.PAID, LocalDate.of(2026, 9, 19)));

            MyFinesPage page = new MyFinesPage(createFineService(repository), loggedInSession());
            List<FineCard> cards = UiTestSupport.findNodes(page, FineCard.class);

            assertEquals(2, cards.size());
            assertTrue(UiTestSupport.labelTexts(cards.get(0)).contains("Fine FINE-2"));
            assertTrue(UiTestSupport.labelTexts(cards.get(1)).contains("Fine FINE-1"));
            assertTrue(UiTestSupport.findButton(page, "Outstanding").isDisable());
            assertFalse(UiTestSupport.findButton(page, "History").isDisable());
        });
    }

    @Test
    void historyViewShowsPaidFinesOnly() {
        UiTestSupport.runOnFxThread(() -> {
            InMemoryFineRepository repository = new InMemoryFineRepository();
            repository.save(fine("FINE-1", FineStatus.OUTSTANDING, LocalDate.of(2026, 9, 20)));
            repository.save(fine("FINE-2", FineStatus.PAID, LocalDate.of(2026, 9, 1)));

            MyFinesPage page = new MyFinesPage(createFineService(repository), loggedInSession());
            UiTestSupport.findButton(page, "History").fire();

            List<FineCard> cards = UiTestSupport.findNodes(page, FineCard.class);
            assertEquals(1, cards.size());
            assertTrue(UiTestSupport.labelTexts(cards.get(0)).contains("Fine FINE-2"));
            assertTrue(UiTestSupport.findButtons(page).stream()
                    .noneMatch(button -> "Pay Fine".equals(button.getText())));
            assertTrue(UiTestSupport.findButton(page, "History").isDisable());
        });
    }

    @Test
    void payingOutstandingFineMarksItPaidAndRefreshesPage() {
        UiTestSupport.runOnFxThread(() -> {
            InMemoryFineRepository repository = new InMemoryFineRepository();
            repository.save(fine("FINE-1", FineStatus.OUTSTANDING, LocalDate.of(2026, 9, 20)));
            MyFinesPage page = new MyFinesPage(createFineService(repository), loggedInSession());

            UiTestSupport.findButton(page, "Pay Fine").fire();

            assertEquals(FineStatus.PAID, repository.findById("FINE-1").orElseThrow().getStatus());
            UiPageTestSupport.assertFeedback(page, "Fine paid successfully.");
            assertTrue(UiTestSupport.labelTexts(page).contains("You have no outstanding fines."));
        });
    }

    @Test
    void repositoryFailureShowsStorageError() {
        UiTestSupport.runOnFxThread(() -> {
            InMemoryFineRepository repository = new InMemoryFineRepository();
            repository.findByMemberFailure = new RepositoryException("storage failure",
                    new IllegalStateException());

            MyFinesPage page = new MyFinesPage(createFineService(repository), loggedInSession());

            UiPageTestSupport.assertFeedback(page, "Unable to access fine data. Please try again.");
        });
    }

    @Test
    void paymentServiceFailureShowsServiceMessage() {
        UiTestSupport.runOnFxThread(() -> {
            InMemoryFineRepository repository = new InMemoryFineRepository();
            repository.save(fine("FINE-1", FineStatus.OUTSTANDING, LocalDate.of(2026, 9, 20)));
            repository.findFailure = new ServiceException("Cannot pay this fine.");
            MyFinesPage page = new MyFinesPage(createFineService(repository), loggedInSession());

            UiTestSupport.findButton(page, "Pay Fine").fire();

            UiPageTestSupport.assertFeedback(page, "Cannot pay this fine.");
        });
    }

    private static FineService createFineService(InMemoryFineRepository repository) {
        LoanQuery loanQuery = new LoanQuery() {
            @Override
            public Optional<LoanSummary> findById(String loanId) {
                return Optional.empty();
            }

            @Override
            public List<LoanSummary> findActiveLoans() {
                return List.of();
            }

            @Override
            public List<LoanSummary> findOverdueLoans(LocalDate date) {
                return List.of();
            }
        };
        MemberDirectory members = new MemberDirectory() {
            @Override
            public boolean exists(String memberId) {
                return "MEM-1".equals(memberId);
            }

            @Override
            public boolean isActive(String memberId) {
                return "MEM-1".equals(memberId);
            }
        };
        return new FineService(repository, loanQuery, members, CLOCK, BigDecimal.valueOf(1.50));
    }

    private static SessionManager loggedInSession() {
        SessionManager sessionManager = new SessionManager();
        sessionManager.login(new Member("USER-1", "Alex Member", "alex@example.com", "hash", "MEM-1"));
        return sessionManager;
    }

    private static Fine fine(String fineId, FineStatus status, LocalDate issuedDate) {
        return new Fine(fineId, "LOAN-1", "MEM-1", BigDecimal.valueOf(3.00),
                "Overdue loan", status, issuedDate, "The Great Book");
    }

    private static final class InMemoryFineRepository implements FineRepository {
        private final Map<String, Fine> fines = new HashMap<>();
        private RuntimeException findByMemberFailure;
        private RuntimeException findFailure;

        @Override
        public Optional<Fine> findById(String id) {
            if (findFailure != null) {
                throw findFailure;
            }
            return Optional.ofNullable(fines.get(id));
        }

        @Override
        public boolean deleteById(String id) {
            return fines.remove(id) != null;
        }

        @Override
        public void save(Fine fine) {
            fines.put(fine.getId(), fine);
        }

        @Override
        public List<Fine> findAll() {
            return new ArrayList<>(fines.values());
        }

        @Override
        public List<Fine> findByMemberId(String memberId) {
            if (findByMemberFailure != null) {
                throw findByMemberFailure;
            }
            return fines.values().stream()
                    .filter(fine -> fine.getMemberId().equals(memberId))
                    .toList();
        }

        @Override
        public List<Fine> findByLoanId(String loanId) {
            return fines.values().stream()
                    .filter(fine -> fine.getLoanId().equals(loanId))
                    .toList();
        }
    }
}
