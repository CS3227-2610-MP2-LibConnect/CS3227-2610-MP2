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

### Possible Architecture
#### Project structure
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
│   └── StorageManager
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
├── members.json
├── librarians.json
├── books.json
├── book-copies.json
├── loans.json
├── reservations.json
├── fines.json
└── notifications.json

#### Storage architecture

The application uses file-backed persistence with one data file for each unique
model type. The storage layer is separated from the domain models and services
using repository interfaces.

- `Repository` interfaces define persistence operations such as `findById`,
  `findAll`, `save`, and `delete` for a specific model type.
- `File...Repository` classes implement those interfaces and translate model
  objects to and from their corresponding files in `data/`.
- `StorageManager` provides shared file-system responsibilities, including the
  data directory, file paths, reading, writing, missing-file initialization, and
  safe replacement of files after an update.
- Services depend on repository interfaces, not on file repositories or file
  formats. Services remain responsible for business rules and for coordinating
  updates across multiple repositories.
- Models represent domain data and do not read from or write to files directly.

The `storage/repositories/` and `storage/file/` directories contain Java source
code, while the `data/` directory contains the persisted application records.
Repository interfaces in `storage/repositories/` define persistence operations,
and the corresponding `File...Repository` classes in `storage/file/` implement
those operations using the files in `data/` through `StorageManager`. This
separation allows the file-backed implementations to be replaced by other
implementations, such as in-memory or database repositories, without changing
the services.

Entity relationships are persisted using stable IDs rather than duplicated
nested objects. For example, a `Loan` stores a member ID and book-copy ID. A
borrowing operation is coordinated by `LoanService`, which updates both the
loan repository and the relevant book-copy repository.

The initial implementation assumes a single active application instance. File
updates should still be written safely, preferably by writing to a temporary
file and replacing the original only after the write succeeds. A repository
operation should only report success after its corresponding file has been
updated successfully; repositories should not retain independent long-lived
copies of the records that could become stale.

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