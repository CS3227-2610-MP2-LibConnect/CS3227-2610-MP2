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
| `Reservation` | Member-role developer |
| `Librarian` | Librarian-role developer |
| Fine-related logic | Librarian-role developer |
| Notification logic | Librarian-role developer |
| Integration| Both |
