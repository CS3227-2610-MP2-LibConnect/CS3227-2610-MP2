# LibConnect User Guide

LibConnect is a desktop library-management application. Members use it to
discover books, borrow copies, reserve unavailable books, manage loans and fines,
and read notifications. Librarians use the same application to manage members,
books, copies, reservations, fines, and alerts.

## Requirements

- Java SE 25.
- Maven 3.9 or later when running from the project checkout.
- A desktop environment supported by JavaFX.

The release JAR contains the runtime dependencies, so Maven is not required to run
the released application.

## Start the application

From the project checkout, run:

```text
mvn exec:java
```

Expected output:

```text
[INFO] --- exec:3.5.0:java (default-cli) @ libconnect ---
```

The LibConnect login window opens. Maven may print additional dependency and JavaFX
messages before or after this line.

To start a release JAR from a writable working directory, run:

```text
java -jar libconnect-1.0.0.jar
```

Expected result:

```text
The LibConnect login window opens.
```

There is no command-line command language in the application. All library actions
are performed through the desktop window.

## Authentication

1. Select `Member` or `Librarian` from the account-type selector.
2. Enter the account email and password.
3. Select `Login`.

Expected output for a successful login:

```text
The corresponding member dashboard or librarian dashboard is displayed.
```

Expected output for invalid credentials:

```text
Invalid email or password.
```

Select `Create account` to register a new account. Complete every field and use
matching password and confirmation values. A successful registration returns to the
login page with:

```text
Account created successfully. Please log in.
```

Passwords are stored as salted hashes. Do not edit password hashes by hand in the
JSON data files.

## Member workflows

### Search the catalogue

1. Sign in as a member.
2. Enter a title, author, category, or ISBN in the search panel. Leave the field
   empty to show the complete catalogue.
3. Select `Search`.
4. Select a book to open its details and copy availability.

Expected output:

```text
Matching books are shown with title, author, category, and availability.
```

If there are no matches:

```text
No books match the selected search criteria.
```

### Borrow books

1. From the dashboard, select `Borrow`.
2. Enter an available book-copy ID and select `Add Book`.
3. Add more copies if needed; a single transaction accepts at most 10 books.
4. Select `Confirm Borrowing`.

Example input:

```text
Book copy ID: COPY-001
```

Expected output:

```text
Successfully borrowed 1 book.
```

Unavailable, missing, or repeated copies are rejected with a specific message and
remain out of the transaction.

### Reserve an unavailable book

1. Select `Reservations`.
2. Enter the book ISBN.
3. Select `Create reservation`.

Example input:

```text
ISBN: 978-0134685991
```

Expected output:

```text
Reservation created successfully.
```

Pending reservations appear in the list with their reservation and expiry dates.
Select `Cancel reservation` to cancel a pending reservation. Expected output:

```text
Reservation cancelled successfully.
```

### Manage loans

1. Select `Profile`, then `My Loans`.
2. Select `Current Loans` to see active loans and due dates.
3. Select `Return` to return a book.
4. Select `Renew` when the loan is eligible for renewal.
5. Select `Loan History` to see returned loans.

Expected output after a successful return or renewal:

```text
The book was returned successfully.
The loan was renewed successfully.
```

Only the action that is valid for the selected loan is available. Returning an
overdue loan automatically creates an outstanding fine.

### View and pay fines

1. Select `Profile`, then `My Fines`.
2. Select `Outstanding` to see unpaid fines.
3. Select `Pay` on an outstanding fine.
4. Select `History` to see paid or waived fines.

Expected output:

```text
Fine paid successfully.
```

Fine amounts are calculated from overdue days and the configured daily rate. The
member view does not allow a fine belonging to another member to be paid.

### Read notifications

1. Select `Notifications`, or select `View all` in the dashboard notification area.
2. Select a notification card to open it.

Expected output:

```text
The notification opens and is marked as read.
```

Overdue alerts and reservation reminders are delivered once per loan or reservation.

### Update a profile

1. Select `Profile`.
2. Select `Edit Profile`.
3. Update the name or email and select `Update information`.

Expected output:

```text
Profile updated successfully.
```

Use `Reset password` to enter and confirm a new password. Expected output:

```text
Password updated successfully.
```

## Librarian workflows

After signing in as an active librarian, the librarian dashboard provides the
following sections:

- `Dashboard`: refresh operational summaries and inspect overdue loans,
  reservations, and fines.
- `Members`: register, search, edit, deactivate, and reset member passwords.
- `Books`: add, edit, search, and remove catalogue books.
- `Book copies`: add copies and record damaged or lost copies.
- `Reservations`: view pending reservations, fulfil reservations, and send
  reservation reminders.
- `Fines`: create, edit, remove, and inspect member fines.
- `Notifications`: trigger overdue alerts and reservation reminders.

Each successful action shows a confirmation such as:

```text
Member registered
Book added
Book updated
Book removed
Fine updated
Fine removed
Reservation fulfilled
Overdue alerts sent
Reservation reminders sent
```

If a librarian session is inactive or missing, protected actions show:

```text
Only an active librarian can perform this action.
```

The librarian dashboard refreshes after a successful mutation so the visible lists
match the persisted records.

## Data and recovery

The application stores JSON arrays in the `data/` directory. Keep the complete
directory together with the release. Before making manual changes, copy the data
directory as a backup. Invalid records are skipped and written to
`data/malformed/malformed-records.json` with the source file, record number, raw
record, and reason.

The application writes replacements through temporary files before replacing the
original data file. Do not terminate the process while a storage operation is in
progress if it can be avoided.

## Build and test commands

These commands are intended for a project checkout, not for an unpacked release.
Every command below includes an example of the expected result.

Run the unit and integration test suite:

```text
mvn test
```

Expected output:

```text
Tests run: <number>, Failures: 0, Errors: 0, Skipped: <number>
BUILD SUCCESS
```

Run the full verification lifecycle and generate the coverage report:

```text
mvn clean verify
```

Expected result:

```text
BUILD SUCCESS
Coverage report: target/site/jacoco/index.html
```

Build the executable application release JAR:

```text
mvn -Prelease clean package
```

Expected result:

```text
BUILD SUCCESS
Release JAR: target/libconnect-1.0.0.jar
```

The same JAR is staged at `release/libconnect-1.0.0.jar`. From that directory,
run:

```text
cd release
java -jar libconnect-1.0.0.jar
```

Expected result:

```text
The LibConnect login window opens.
```
