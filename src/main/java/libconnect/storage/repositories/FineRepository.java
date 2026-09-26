package libconnect.storage.repositories;

import java.util.List;

import libconnect.models.Fine;

/** Defines fine persistence and query operations. */
public interface FineRepository extends Repository<Fine> {
    /** Returns fines belonging to a member. */
    List<Fine> findByMemberId(String memberId);

    /** Returns fines associated with a loan. */
    List<Fine> findByLoanId(String loanId);
}
