package libconnect.services.unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import libconnect.models.AccountStatus;
import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.models.Librarian;
import libconnect.models.Loan;
import libconnect.models.LoanStatus;
import libconnect.models.Member;
import libconnect.storage.repositories.BookCopyRepository;
import libconnect.storage.repositories.LibrarianRepository;
import libconnect.storage.repositories.LoanRepository;
import libconnect.storage.repositories.MemberRepository;

/** Provides in-memory repository doubles for service unit tests. */
final class ServiceUnitTestDoubles {
    private ServiceUnitTestDoubles() {
    }

    /** Stores members in memory. */
    static final class Members implements MemberRepository {
        private final List<Member> entities = new ArrayList<>();

        @Override
        public Optional<Member> findById(String id) {
            return entities.stream().filter(member -> member.getId().equals(id)).findFirst();
        }

        @Override
        public boolean deleteById(String id) {
            return entities.removeIf(member -> member.getId().equals(id));
        }

        @Override
        public void save(Member member) {
            deleteById(member.getId());
            entities.add(member);
        }

        @Override
        public List<Member> findAll() {
            return List.copyOf(entities);
        }

        @Override
        public Optional<Member> findByMembershipId(String membershipId) {
            return entities.stream().filter(member -> member.getMembershipId().equals(membershipId)).findFirst();
        }

        @Override
        public Optional<Member> findByUserId(String userId) {
            return entities.stream().filter(member -> member.getUserId().equals(userId)).findFirst();
        }

        @Override
        public Optional<Member> findByEmail(String email) {
            return entities.stream().filter(member -> member.getEmail().equalsIgnoreCase(email.trim())).findFirst();
        }

        @Override
        public List<Member> findByName(String name) {
            return entities.stream().filter(member -> member.getName().toLowerCase()
                    .contains(name.trim().toLowerCase())).toList();
        }

        @Override
        public List<Member> findByStatus(AccountStatus status) {
            return entities.stream().filter(member -> member.getStatus() == status).toList();
        }
    }

    /** Stores librarians in memory. */
    static final class Librarians implements LibrarianRepository {
        private final List<Librarian> entities = new ArrayList<>();

        @Override
        public Optional<Librarian> findById(String id) {
            return entities.stream().filter(librarian -> librarian.getId().equals(id)).findFirst();
        }

        @Override
        public boolean deleteById(String id) {
            return entities.removeIf(librarian -> librarian.getId().equals(id));
        }

        @Override
        public void save(Librarian librarian) {
            deleteById(librarian.getId());
            entities.add(librarian);
        }

        @Override
        public List<Librarian> findAll() {
            return List.copyOf(entities);
        }

        @Override
        public Optional<Librarian> findByEmail(String email) {
            return entities.stream().filter(librarian -> librarian.getEmail().equalsIgnoreCase(email.trim()))
                    .findFirst();
        }

        @Override
        public Optional<Librarian> findByUserId(String userId) {
            return entities.stream().filter(librarian -> librarian.getUserId().equals(userId)).findFirst();
        }
    }

    /** Stores book copies in memory and supports injected save failures. */
    static final class Copies implements BookCopyRepository {
        private final List<BookCopy> entities = new ArrayList<>();
        int saveFailuresRemaining;

        @Override
        public Optional<BookCopy> findById(String id) {
            return entities.stream().filter(copy -> copy.getId().equals(id)).findFirst();
        }

        @Override
        public boolean deleteById(String id) {
            return entities.removeIf(copy -> copy.getId().equals(id));
        }

        @Override
        public void save(BookCopy copy) {
            if (saveFailuresRemaining > 0) {
                saveFailuresRemaining--;
                throw new IllegalStateException("copy save failed");
            }
            deleteById(copy.getId());
            entities.add(copy);
        }

        @Override
        public List<BookCopy> findAll() {
            return List.copyOf(entities);
        }

        @Override
        public List<BookCopy> findByIsbn(String isbn) {
            return entities.stream().filter(copy -> copy.getIsbn().equals(isbn)).toList();
        }

        @Override
        public void deleteByIsbn(String isbn) {
            entities.removeIf(copy -> copy.getIsbn().equals(isbn));
        }

        @Override
        public List<BookCopy> findByStatus(CopyStatus status) {
            return entities.stream().filter(copy -> copy.getStatus() == status).toList();
        }
    }

    /** Stores loans in memory and supports injected save failures. */
    static final class Loans implements LoanRepository {
        private final List<Loan> entities = new ArrayList<>();
        int saveFailuresRemaining;

        @Override
        public Optional<Loan> findById(String id) {
            return entities.stream().filter(loan -> loan.getId().equals(id)).findFirst();
        }

        @Override
        public boolean deleteById(String id) {
            return entities.removeIf(loan -> loan.getId().equals(id));
        }

        @Override
        public void save(Loan loan) {
            if (saveFailuresRemaining > 0) {
                saveFailuresRemaining--;
                throw new IllegalStateException("loan save failed");
            }
            deleteById(loan.getId());
            entities.add(loan);
        }

        @Override
        public List<Loan> findAll() {
            return List.copyOf(entities);
        }

        @Override
        public List<Loan> findByMemberId(String memberId) {
            return entities.stream().filter(loan -> loan.getMemberId().equals(memberId)).toList();
        }

        @Override
        public List<Loan> findByCopyId(String copyId) {
            return entities.stream().filter(loan -> loan.getCopyId().equals(copyId)).toList();
        }

        @Override
        public List<Loan> findByStatus(LoanStatus status) {
            return entities.stream().filter(loan -> loan.getStatus() == status).toList();
        }
    }
}
