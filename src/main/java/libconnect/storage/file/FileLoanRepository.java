package libconnect.storage.file;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import libconnect.models.Loan;
import libconnect.models.LoanStatus;
import libconnect.storage.FileManager;
import libconnect.storage.exceptions.DeleteFailureException;
import libconnect.storage.repositories.LoanRepository;
import libconnect.util.ValidationUtils;

/**
 * Provides JSON file-backed persistence for loans.
 */
public class FileLoanRepository extends FileRepositorySupport implements LoanRepository {
    private static final Path DEFAULT_DATA_FILE = Path.of("data", "loans.json");
    private static final String LOAN_ID_FIELD = "loanId";
    private static final String MEMBER_ID_FIELD = "memberId";
    private static final String COPY_ID_FIELD = "copyId";
    private static final String BORROW_DATE_FIELD = "borrowDate";
    private static final String DUE_DATE_FIELD = "dueDate";
    private static final String RETURN_DATE_FIELD = "returnDate";
    private static final String STATUS_FIELD = "status";
    private static final String RENEWED_FIELD = "isRenewed";

    /**
     * Creates a repository backed by {@code data/loans.json}.
     *
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileLoanRepository() {
        this(new FileManager(), DEFAULT_DATA_FILE);
    }

    /**
     * Creates a repository backed by the supplied file.
     *
     * @param dataFile the JSON file used for persistence.
     * @throws NullPointerException if {@code dataFile} is null.
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileLoanRepository(Path dataFile) {
        this(new FileManager(), dataFile);
    }

    /**
     * Creates a repository with explicit storage dependencies.
     *
     * @param fileManager the manager used for file operations.
     * @param dataFile the JSON file used for persistence.
     * @throws NullPointerException if either argument is null.
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileLoanRepository(FileManager fileManager, Path dataFile) {
        super(fileManager, dataFile, "loans");
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code loanId} is blank.
     * @throws IllegalStateException if the data file cannot be read or is invalid.
     */
    @Override
    public Optional<Loan> findById(String loanId) {
        String requiredLoanId = ValidationUtils.requireNonBlank(loanId, LOAN_ID_FIELD);

        return findFirst(this::parseLoans, loan -> loan.getLoanId().equals(requiredLoanId));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the data file cannot be read or is invalid.
     */
    @Override
    public List<Loan> findAll() {
        return readAllRecords(this::parseLoans);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code memberId} is blank.
     * @throws IllegalStateException if the data file cannot be read or is invalid.
     */
    @Override
    public List<Loan> findByMemberId(String memberId) {
        String requiredMemberId = ValidationUtils.requireNonBlank(memberId, MEMBER_ID_FIELD);

        return findMatching(this::parseLoans, loan -> loan.getMemberId().equals(requiredMemberId));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code copyId} is blank.
     * @throws IllegalStateException if the data file cannot be read or is invalid.
     */
    @Override
    public List<Loan> findByCopyId(String copyId) {
        String requiredCopyId = ValidationUtils.requireNonBlank(copyId, COPY_ID_FIELD);

        return findMatching(this::parseLoans, loan -> loan.getCopyId().equals(requiredCopyId));
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code status} is null.
     * @throws IllegalStateException if the data file cannot be read or is invalid.
     */
    @Override
    public List<Loan> findByStatus(LoanStatus status) {
        Objects.requireNonNull(status, "status cannot be null");

        return findMatching(this::parseLoans, loan -> loan.getStatus() == status);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code loan} is null.
     * @throws IllegalStateException if the data file cannot be read or written.
     */
    @Override
    public void save(Loan loan) {
        Objects.requireNonNull(loan, "loan cannot be null");

        List<Loan> loans = readRecords(this::parseLoans);
        upsert(loans, loan, existingLoan -> existingLoan.getLoanId().equals(loan.getLoanId()));

        writeRecords(loans, this::toJson);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code loanId} is blank.
     * @throws DeleteFailureException if no loan with the supplied identifier can be deleted.
     * @throws IllegalStateException if the data file cannot be read or written.
     */
    @Override
    public void deleteById(String loanId) throws DeleteFailureException {
        String requiredLoanId = ValidationUtils.requireNonBlank(loanId, LOAN_ID_FIELD);
        List<Loan> loans = readRecords(this::parseLoans);

        deleteMatching(loans, loan -> loan.getLoanId().equals(requiredLoanId),
                this::toJson, "No loan found with loanId: " + requiredLoanId);
    }

    /**
     * Parses a JSON array containing loan objects.
     *
     * @param json the JSON content to parse.
     * @return the parsed loans.
     * @throws IllegalArgumentException if the JSON structure or a record is invalid.
     */
    private List<Loan> parseLoans(String json) {
        JsonParser parser = new JsonParser(json);
        List<Loan> loans = parser.parseLoanArray();
        Set<String> loanIds = new LinkedHashSet<>();

        for (Loan loan : loans) {
            if (!loanIds.add(loan.getLoanId())) {
                throw new IllegalArgumentException("Duplicate loan ID in loan data: "
                        + loan.getLoanId());
            }
        }

        return loans;
    }

    /**
     * Serializes loans as a readable JSON array.
     *
     * @param loans the loans to serialize.
     * @return the JSON representation of the loans.
     */
    private String toJson(List<Loan> loans) {
        return JsonWriter.toJsonArray(loans, this::toJsonObject);
    }

    /**
     * Serializes one loan as a JSON object.
     *
     * @param loan the loan to serialize.
     * @return the JSON object representation of the loan.
     */
    private String toJsonObject(Loan loan) {
        String returnDate = loan.getReturnDate() == null
                ? "null"
                : JsonWriter.quote(loan.getReturnDate().toString());

        return "  {\n"
                + "    \"" + LOAN_ID_FIELD + "\": " + JsonWriter.quote(loan.getLoanId()) + ",\n"
                + "    \"" + MEMBER_ID_FIELD + "\": " + JsonWriter.quote(loan.getMemberId()) + ",\n"
                + "    \"" + COPY_ID_FIELD + "\": " + JsonWriter.quote(loan.getCopyId()) + ",\n"
                + "    \"" + BORROW_DATE_FIELD + "\": " + JsonWriter.quote(loan.getBorrowDate().toString()) + ",\n"
                + "    \"" + DUE_DATE_FIELD + "\": " + JsonWriter.quote(loan.getDueDate().toString()) + ",\n"
                + "    \"" + RETURN_DATE_FIELD + "\": " + returnDate + ",\n"
                + "    \"" + STATUS_FIELD + "\": " + JsonWriter.quote(loan.getStatus().name()) + ",\n"
                + "    \"" + RENEWED_FIELD + "\": " + loan.hasBeenRenewed() + "\n"
                + "  }";
    }

    /**
     * Parses the limited JSON structure used by this repository.
     */
    private static final class JsonParser extends JsonReader {

        /**
         * Creates a parser for JSON content.
         *
         * @param json the JSON content to parse.
         */
        private JsonParser(String json) {
            super(json, "loan");
        }

        /**
         * Parses the root JSON array into loans.
         *
         * @return the parsed loans.
         * @throws IllegalArgumentException if the JSON is invalid.
         */
        private List<Loan> parseLoanArray() {
            List<Loan> loans = new ArrayList<>();
            skipWhitespace();
            expect('[');
            skipWhitespace();

            if (consume(']')) {
                ensureEnd();
                return loans;
            }

            while (true) {
                loans.add(parseLoan());
                skipWhitespace();

                if (consume(']')) {
                    ensureEnd();
                    return loans;
                }

                expect(',');
                skipWhitespace();
            }
        }

        /**
         * Parses one loan JSON object.
         *
         * @return the parsed loan.
         * @throws IllegalArgumentException if the object is invalid.
         */
        private Loan parseLoan() {
            skipWhitespace();
            expect('{');
            skipWhitespace();

            String loanId = null;
            String memberId = null;
            String copyId = null;
            String borrowDate = null;
            String dueDate = null;
            String returnDate = null;
            LoanStatus status = null;
            Boolean isRenewed = null;

            if (!consume('}')) {
                while (true) {
                    String fieldName = parseString();
                    skipWhitespace();
                    expect(':');

                    switch (fieldName) {
                    case LOAN_ID_FIELD -> loanId = parseString();
                    case MEMBER_ID_FIELD -> memberId = parseString();
                    case COPY_ID_FIELD -> copyId = parseString();
                    case BORROW_DATE_FIELD -> borrowDate = parseString();
                    case DUE_DATE_FIELD -> dueDate = parseString();
                    case RETURN_DATE_FIELD -> returnDate = parseNullableString();
                    case STATUS_FIELD -> status = parseStatus();
                    case RENEWED_FIELD -> isRenewed = parseBoolean();
                    default -> skipValue();
                    }

                    skipWhitespace();
                    if (consume('}')) {
                        break;
                    }
                    expect(',');
                    skipWhitespace();
                }
            }

            if (loanId == null || memberId == null || copyId == null || borrowDate == null
                    || dueDate == null || status == null || isRenewed == null) {
                throw new IllegalArgumentException("Loan record is missing a required field");
            }

            try {
                return new Loan(loanId, memberId, copyId, LocalDate.parse(borrowDate),
                        LocalDate.parse(dueDate), returnDate == null ? null : LocalDate.parse(returnDate),
                        status, isRenewed);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid loan record", exception);
            }
        }

        /**
         * Parses a loan status string.
         *
         * @return the parsed loan status.
         * @throws IllegalArgumentException if the value is not a valid loan status.
         */
        private LoanStatus parseStatus() {
            try {
                return LoanStatus.valueOf(parseString());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid loan status", exception);
            }
        }

    }
}
