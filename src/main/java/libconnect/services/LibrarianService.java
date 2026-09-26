package libconnect.services;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import libconnect.models.AccountStatus;
import libconnect.models.Librarian;
import libconnect.storage.file.FileLibrarianRepository;
import libconnect.storage.file.FileMemberRepository;
import libconnect.storage.repositories.LibrarianRepository;
import libconnect.storage.repositories.MemberRepository;

/** Enforces librarian account lifecycle and authorization rules. */
public final class LibrarianService {
    private final LibrarianRepository repository;
    private final UserService userService;

    /** Creates a librarian service using the supplied repository. */
    public LibrarianService(LibrarianRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.userService = new UserService();
    }

    public LibrarianService(LibrarianRepository repository, MemberRepository memberRepository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(memberRepository, "memberRepository");
        this.userService = new UserService(memberRepository, repository);

    }

    public LibrarianService() {
        this(new FileLibrarianRepository(), new FileMemberRepository());
    }

    /** Registers a librarian while preventing duplicate identifiers and emails. */
    public void register(Librarian librarian) {
        Objects.requireNonNull(librarian, "librarian");
        if (repository.findById(librarian.getId()).isPresent()) {
            throw new IllegalStateException("A librarian with this employee ID already exists");
        }
        if (userService.isEmailInUse(librarian.getEmail())) {
            throw new IllegalStateException("A librarian or member with this email already exists");
        }
        repository.save(librarian);
    }

    /** Updates a librarian's name and email while preserving account status. */
    public Librarian edit(String employeeId, String name, String email) {
        Librarian current = getRequired(employeeId);
        repository.findByEmail(email).filter(other -> !other.getId().equals(employeeId))
                .ifPresent(other -> {
                    throw new IllegalStateException("A librarian with this email already exists");
                });
        Librarian updated = new Librarian(current.getUserId(), employeeId, name, email, current.getPasswordHash(), current.getStatus());
        repository.save(updated);
        return updated;
    }

    /** Deactivates a librarian account. */
    public Librarian deactivate(String employeeId) {
        Librarian deactivated = getRequired(employeeId).withStatus(AccountStatus.INACTIVE);
        repository.save(deactivated);
        return deactivated;
    }

    /** Returns the active librarian or rejects the protected operation. */
    public Librarian requireActive(String employeeId) {
        Librarian librarian = getRequired(employeeId);
        if (!librarian.isActive()) {
            throw new IllegalStateException("Librarian account is inactive");
        }
        return librarian;
    }

    /** Searches librarians by employee ID, name, or email. */
    public List<Librarian> search(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .filter(librarian -> normalizedQuery.isEmpty()
                        || librarian.getId().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || librarian.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || librarian.getEmail().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(Comparator.comparing(Librarian::getId))
                .toList();
    }

    private Librarian getRequired(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("employeeId must not be blank");
        }
        return repository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Librarian does not exist"));
    }
}
