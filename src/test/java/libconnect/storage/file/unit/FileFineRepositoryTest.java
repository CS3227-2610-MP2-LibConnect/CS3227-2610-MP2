package libconnect.storage.file.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import libconnect.models.Fine;
import libconnect.models.FineStatus;
import libconnect.storage.StorageManager;
import libconnect.storage.file.FileFineRepository;

/** Tests fine-specific queries in the file-backed repository. */
class FileFineRepositoryTest extends AbstractFileRepositoryTest<Fine, FileFineRepository> {

    @Override
    protected FileFineRepository createRepository(Path dataFile) {
        return new FileFineRepository(new StorageManager(temporaryDirectory), dataFile);
    }

    @Override
    protected void save(FileFineRepository repository, Fine fine) {
        repository.save(fine);
    }

    @Override
    protected List<Fine> findAll(FileFineRepository repository) {
        return repository.findAll();
    }

    @Override
    protected String getIdentifier(Fine fine) {
        return fine.getId();
    }

    @Override
    protected Fine createEntity(String identifier, String variant) {
        return new Fine(identifier, "LOAN-" + variant, "MEM-" + variant, BigDecimal.TEN,
                "Overdue loan", FineStatus.OUTSTANDING, LocalDate.of(2026, 9, 1));
    }

    @Test
    void queries_filterByMemberAndLoan() {
        FileFineRepository repository = createRepository(temporaryDirectory.resolve("fines.json"));
        Fine first = createEntity("FINE-1", "1");
        Fine second = createEntity("FINE-2", "2");
        repository.save(first);
        repository.save(second);

        assertEquals(List.of("FINE-1"), repository.findByMemberId("MEM-1").stream()
                .map(Fine::getId).toList());
        assertEquals(List.of("FINE-2"), repository.findByLoanId("LOAN-2").stream()
                .map(Fine::getId).toList());
        assertTrue(repository.findByLoanId("LOAN-3").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByMemberId_blankId_throwsIllegalArgumentException(String memberId) {
        FileFineRepository repository = createRepository(temporaryDirectory.resolve("fines.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.findByMemberId(memberId));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByLoanId_blankId_throwsIllegalArgumentException(String loanId) {
        FileFineRepository repository = createRepository(temporaryDirectory.resolve("fines.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.findByLoanId(loanId));
    }
}
