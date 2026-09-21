### Features:
#### Librarian features
- Log in through the shared authentication system.
- Register, edit, deactivate, and search members.
- Add, edit, remove, and search books.
- Record damaged or lost books.
- View, edit and remove fines for members.
- View overdue, loan and reservation.
- Send in-app alerts for overdue loans and reminder for pending reservations.
#### Member features
- Register and log in.
- Search and filter the book catalogue.
- View book availability and details.
- Borrow or request available books.
- Renew active loans when permitted.
- Reserve unavailable books.
- View current loans, due dates, reservations, fines, and borrowing history.
- Receive in-app overdue and reservation notifications.

### Architecture and implementation status
#### Current project structure
src/main/java/libconnect/
├── models/
│   ├── User
│   ├── Member
│   ├── Book
│   ├── BookCopy
│   ├── Loan
│   └── supporting enums
│
├── storage/
│   ├── repositories/
│   │   ├── UserRepository
│   │   ├── MemberRepository
│   │   ├── BookRepository
│   │   ├── BookCopyRepository
│   │   └── LoanRepository
│   │
│   ├── file/
│   │   ├── FileUserRepository
│   │   ├── FileMemberRepository
│   │   ├── FileBookRepository
│   │   ├── FileBookCopyRepository
│   │   ├── FileLoanRepository
│   │   ├── FileRepositorySupport
│   │   ├── JsonReader
│   │   └── JsonWriter
│   │
│   ├── exceptions/
│   │   └── DeleteFailureException
│   └── FileManager
│
└── util/
    └── ValidationUtils

#### Planned application structure
src/
├── models/
│   ├── User
│   ├── Member
│   ├── Librarian
│   ├── Book
│   ├── BookCopy
│   ├── Loan
│   ├── Reservation
│   ├── Fine
│   └── Notification
│
├── storage/
│   ├── repositories/
│   │   ├── UserRepository
│   │   ├── MemberRepository
│   │   ├── LibrarianRepository
│   │   ├── BookRepository
│   │   ├── BookCopyRepository
│   │   ├── LoanRepository
│   │   ├── ReservationRepository
│   │   ├── FineRepository
│   │   └── NotificationRepository
│   │
│   ├── file/
│   │   ├── FileUserRepository
│   │   ├── FileMemberRepository
│   │   ├── FileLibrarianRepository
│   │   ├── FileBookRepository
│   │   ├── FileBookCopyRepository
│   │   ├── FileLoanRepository
│   │   ├── FileReservationRepository
│   │   ├── FileFineRepository
│   │   └── FileNotificationRepository
│   │
│   └── FileManager
│
├── services/
│   ├── AuthenticationService
│   ├── CatalogueService
│   ├── LoanService
│   ├── ReservationService
│   ├── FineService
│   └── NotificationService
│
├── member/
│   ├── MemberController
│   └── MemberView
│
└── librarian/
    ├── LibrarianController
    └── LibrarianView

data/
├── users.json
├── books.json
├── book-copies.json
├── loans.json
├── reservations.json
├── fines.json
└── notifications.json

The repositories will use the above runtime-created files.

Reservation, fine, and notification files will be added when their
repositories are implemented.

#### Storage architecture

The application currently uses JSON file-backed persistence with one data file
for each implemented repository. User accounts are stored together in
`users.json` so that account identifiers and email addresses can be validated
globally, even when additional user roles are introduced. The storage layer is
separated from the domain models and future services using repository
interfaces.

- Repository interfaces define persistence operations for users, members,
  books, book copies, and loans. They expose entity-specific lookups such as
  `findByIsbn`, `findByMembershipId`, and `findByMemberId`.
- `File...Repository` classes translate model objects to and from JSON files.
  `FileUserRepository` owns `users.json`, while `FileMemberRepository` is a
  member-specific view over that shared user repository.
- `FileRepositorySupport` centralizes file-backed repository behavior,
  including reading, writing, upserting, matching, and deletion handling.
- `FileManager` provides UTF-8 file reads, missing-file initialization, parent
  directory creation, and atomic replacement after an update.
- `JsonReader` and `JsonWriter` provide shared low-level JSON parsing and
  serialization support for the file repositories.
- Services depend on repository interfaces, not on file repositories or file
  formats. Services remain responsible for business rules and for coordinating
  updates across multiple repositories.
- Models represent domain data and do not read from or write to files directly.

Each file-backed repository can be constructed with a custom file path. The
repository ensures that the configured file and its parent directories exist,
initializing a new file as an empty JSON array. The default paths are under
`data/`, but that directory is created at runtime and is not required to be
checked into the repository. This separation allows the file-backed
implementations to be replaced by other implementations, such as in-memory or
database repositories, without changing the services.

Entity relationships are persisted using stable IDs rather than duplicated
nested objects. For example, a `Loan` stores a member ID and book-copy ID. A
borrowing operation is coordinated by `LoanService`, which updates both the
loan repository and the relevant book-copy repository.

The initial implementation assumes a single active application instance. File
updates are written to a temporary file and the destination is replaced only
after the write succeeds. Repository operations report success only after
their corresponding file has been updated successfully; repositories do not
retain independent long-lived copies of records that could become stale.

`save` operations insert or replace records with the same stable identifier.
Delete operations for books, book copies, and loans throw
`DeleteFailureException` when no matching record is removed. Deleting a book
does not directly depend on `BookCopyRepository`; controller-level validation
is responsible for any required coordination. `BookCopyRepository` provides
`deleteByIsbn` so that this coordination can remove copies explicitly when
needed.

`FileUserRepository` validates supported user records and enforces unique user
IDs, membership IDs, and normalized email addresses across the shared user
file. This prevents a member and a future librarian account from sharing an
email address and causing ambiguous authentication.

Some operations, such as borrowing a book, require updates to multiple files.
These operations are coordinated by the relevant service. If one file update
fails, the service must either roll back the updates that have already
succeeded or use a transaction or journaling mechanism to prevent the files
from becoming inconsistent. Cross-file transaction support can be added inside
the storage layer without changing the service interfaces.

#### Class Diagram - just for reference, list out so later easier do integration
```mermaid
classDiagram
    class User {
        -String userId
        -String name
        -String email
        -String passwordHash
        -AccountStatus status
        +login(email, password)
        +logout()
        +updateProfile()
        +deactivateAccount()
    }

    class Member {
        -String membershipId
        -Date registrationDate
        +searchBooks(criteria)
        +borrowBook(copyId)
        +requestBook(bookId)
        +renewLoan(loanId)
        +reserveBook(bookId)
        +viewLoans()
        +viewReservations()
        +viewFines()
        +viewBorrowingHistory()
    }

    class Librarian {
        -String employeeId
        +registerMember(member)
        +editMember(memberId, details)
        +deactivateMember(memberId)
        +searchMembers(criteria)
        +addBook(book)
        +editBook(bookId, details)
        +removeBook(bookId)
        +searchBooks(criteria)
        +recordDamagedBook(copyId)
        +recordLostBook(copyId)
        +viewFines(memberId)
        +editFine(fineId, amount)
        +removeFine(fineId)
        +viewOverdueLoans()
        +viewLoans()
        +viewReservations()
        +sendAlert(notification)
    }

    class Book {
        -String isbn
        -String title
        -String author
        -String publisher
        -String category
        -int publicationYear
        +getDetails()
        +getAvailableCopies()
        +updateDetails()
    }

    class BookCopy {
        -String copyId
        -CopyStatus status
        -String shelfLocation
        +markAvailable()
        +markBorrowed()
        +markLost()
        +markDamaged()
    }

    class Loan {
        -String loanId
        -Date borrowDate
        -Date dueDate
        -Date returnDate
        -LoanStatus status
        +createLoan()
        +renew()
        +returnBook()
        +isOverdue()
        +calculateOverdueDays()
    }

    class Reservation {
        -String reservationId
        -Date reservationDate
        -Date expiryDate
        -ReservationStatus status
        +createReservation()
        +cancelReservation()
        +fulfilReservation()
        +isExpired()
    }

    class Fine {
        -String fineId
        -double amount
        -String reason
        -FineStatus status
        -Date issuedDate
        +calculateFine()
        +updateAmount()
        +waive()
        +remove()
    }

    class Notification {
        -String notificationId
        -String message
        -Date createdAt
        -boolean isRead
        +send()
        +markAsRead()
    }

    class AuthenticationService {
        +registerMember(details)
        +authenticate(email, password)
        +logout(userId)
        +changePassword(userId, password)
    }

    class CatalogueService {
        +addBook(book)
        +editBook(bookId, details)
        +removeBook(bookId)
        +searchBooks(criteria)
        +filterBooks(criteria)
        +checkAvailability(bookId)
    }

    class LoanService {
        +borrowBook(memberId, copyId)
        +returnBook(loanId)
        +renewLoan(loanId)
        +getActiveLoans(memberId)
        +getOverdueLoans()
        +validateBorrowingEligibility(memberId)
    }

    class ReservationService {
        +reserveBook(memberId, bookId)
        +cancelReservation(reservationId)
        +getReservations(memberId)
        +processPendingReservations(bookId)
    }

    class FineService {
        +createFine(loanId)
        +getMemberFines(memberId)
        +editFine(fineId, amount)
        +removeFine(fineId)
        +calculateOverdueFine(loanId)
    }

    class NotificationService {
        +sendOverdueAlert(memberId, loanId)
        +sendReservationReminder(memberId, reservationId)
        +getNotifications(userId)
        +markAsRead(notificationId)
    }

    User <|-- Member
    User <|-- Librarian

    Book "1" o-- "1..*" BookCopy
    Member "1" --> "0..*" Loan
    BookCopy "1" --> "0..*" Loan
    Member "1" --> "0..*" Reservation
    Book "1" --> "0..*" Reservation
    Loan "1" --> "0..1" Fine
    User "1" --> "0..*" Notification

    AuthenticationService ..> User
    CatalogueService ..> Book
    CatalogueService ..> BookCopy
    LoanService ..> Loan
    LoanService ..> Member
    LoanService ..> BookCopy
    ReservationService ..> Reservation
    FineService ..> Fine
    NotificationService ..> Notification
```
### Work split

| Shared class/service | Primary owner |
|---|---|
| `User`, `Member`, authentication | Member-role developer |
| `Book`, `BookCopy`, catalogue search | Member-role developer |
| `Loan`, borrowing rules | Member-role developer |
| `Reservation` | Librarian-role developer |
| `Librarian` | Librarian-role developer |
| Fine-related logic | Librarian-role developer |
| Notification logic | Librarian-role developer |
| Integration| Both |
