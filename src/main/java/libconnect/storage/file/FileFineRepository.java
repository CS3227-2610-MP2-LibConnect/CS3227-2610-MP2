package libconnect.storage.file;

import java.nio.file.Path;
import java.util.List;

import libconnect.models.Fine;
import libconnect.storage.StorageManager;
import libconnect.storage.repositories.FineRepository;

/** Stores fines in a JSON array and provides member and loan queries. */
public final class FileFineRepository extends AbstractFileRepository<Fine> implements FineRepository {
    /** Creates a file-backed fine repository. */
    public FileFineRepository(StorageManager storageManager, Path file) {
        super(storageManager, file, Fine.class);
    }

    /** Returns fines belonging to a member. */
    @Override
    public List<Fine> findByMemberId(String memberId) {
        requireText(memberId, "memberId");
        return readAll().stream().filter(fine -> fine.getMemberId().equals(memberId)).toList();
    }

    /** Returns fines associated with a loan. */
    @Override
    public List<Fine> findByLoanId(String loanId) {
        requireText(loanId, "loanId");
        return readAll().stream().filter(fine -> fine.getLoanId().equals(loanId)).toList();
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
