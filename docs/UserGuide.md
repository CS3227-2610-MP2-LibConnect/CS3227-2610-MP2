---
layout: page
title: User Guide
permalink: /user-guide
---

# LibConnect User Guide

LibConnect is a Java desktop application for library catalogue, book-copy, loan,
reservation, fine, and notification management. Members use it to find and
borrow books, manage loans and reservations, pay fines, and read notifications.
Librarians use it to maintain members, catalogue books, physical copies,
reservations, fines, loans, and alerts.

## Start here

### Requirements

- Java Development Kit (JDK) 25.
- Maven 3.9 or later when running from the project checkout.
- A desktop display supported by JavaFX. JavaFX UI tests and the application do
  not run correctly in a display-less shell.

The release JAR contains the application dependencies, but it still needs a JDK
25 runtime and a writable working directory containing the `data/` directory.

Check the tools before starting:

```text
java --version
mvn --version
```

Both commands must report Java 25. If Maven reports an older Java version, select
JDK 25 as `JAVA_HOME` before running Maven.

### Start from the project checkout

From the repository root, run:

```text
mvn exec:java
```

The LibConnect login window opens. All library actions are performed in the
desktop window; LibConnect has no command-line command language.

### Start the release JAR

Build the release artifact from the repository root:

```text
mvn -Prelease clean package
```

The executable JAR is created at `target/libconnect-1.0.0.jar` and copied to
`release/libconnect-1.0.0.jar`. Run it from a directory that contains the
application's writable `data/` directory:

```text
java -jar target/libconnect-1.0.0.jar
```

The JavaFX native libraries in a release are platform-specific. Build the JAR on
the operating system and architecture where it will be used.

### Set up a repeatable manual test

The checked-in data files may contain no books, copies, members, loans,
reservations, fines, or notifications. For a clean manual test, make a backup of
`data/` first, then:

1. Run LibConnect.
2. Select `Create account`.
3. Register one `Librarian` account with an employee ID.
4. Register one `Member` account with a different email address.
5. Log in as the librarian and add a book in `Books`.
6. Add an available copy for that book in `BookCopy`.
7. Log out with `Log out` and log in as the member.
8. Use the member workflows below to search for the book, borrow the copy, and
   inspect loans, fines, reservations, and notifications.

Use a copy of the data directory for peer testing if you need to repeat the
workflow without retaining previous test records.

## Authentication

### Register an account

1. From the login page, select `Create account`.
2. Select `Member` or `Librarian` in `Account type`.
3. Enter `Name`, `Email`, `Password`, and `Confirm password`.
4. If `Librarian` is selected, enter `Employee ID`.
5. Select `Register`.
6. Return to the login page and sign in.

The two password fields must match. Blank required fields show `Name, email and
password are required.`, and mismatched passwords show `Passwords do not match.`
Librarian registration without an employee ID shows `Employee ID is required for
librarian accounts.` A successful registration shows `Account created
successfully. Please log in.`

### Log in or log out

1. Select `Member` or `Librarian` in the login page's account-type selector.
2. Enter the account email in the `Email` field.
3. Enter the password in the `Password` field.
4. Select `Login`.
5. Select `Logout` in the member navigation bar or `Log out` in the librarian
   workspace when finished.

Successful member login opens `Dashboard`. Successful librarian login opens the
`LibConnect Librarian` workspace. Invalid credentials show `Invalid email or
password.` A deactivated account shows `This account is deactivated and cannot
log in.`

## Member guide

After member login, the navigation bar contains `Dashboard`, `Borrow books`,
`Reservation`, `Profile`, and `Logout`.

### Search the catalogue

1. Open `Dashboard`.
2. In `Search type`, keep `Simple search` selected to search titles, or select
   `Advanced search`.
3. For `Simple search`, enter a title in `Search by title`.
4. For `Advanced search`, fill any combination of `Exact ISBN`, `Title contains`,
   `Author contains`, `Publisher contains`, `Category contains`, `Start year`,
   and `End year`.
5. Select `Search`.
6. Select a result card to open its details.
7. Select `Reset` to clear the filters and show the full catalogue again.

Book details show the title, author, ISBN, publication year, publisher, category,
and visible copies. Available copies show their shelf location; borrowed copies
show `Borrowed`. Lost and damaged copies are not shown to members. Select `Back
to Dashboard` to return.

An empty result list shows `No books match your search.` A non-numeric or
non-positive year shows `Start year must be a number.`, `Start year must be
positive.`, `End year must be a number.`, or `End year must be positive.` as
appropriate. A reversed year range is rejected by the search criteria
validation.

The dashboard also shows a randomly selected `Book recommendation` when the
catalogue is not empty. If there are no books, it shows `no books in database`.

### Borrow one or more books

1. Select `Borrow books`.
2. Enter an available physical copy identifier in `Enter book copy ID`.
3. Select `Add Book`.
4. Repeat for other available copies if needed.
5. Select `Confirm Borrowing`.

A transaction accepts at most 10 copies. The page displays the selection count,
for example `2 / 10 books selected`. Remove a selected item from its book card
before confirming if necessary. A successful transaction returns to the
dashboard with `Successfully borrowed N book(s).`

Common validation messages include `Enter a book copy ID.`, `This book copy has
already been added.`, `A transaction cannot contain more than 10 books.`, `Book
copy not found: <copy ID>`, and `Add at least one available book before
confirming.` Copies that are borrowed, lost, or damaged cannot be added.

### View, return, and renew loans

1. Select `Profile`.
2. Select `My Loans`.
3. Select `Current Loans` to see active loans, due dates, and status.
4. Select `Return` on a current loan to return it.
5. Select `Renew` on an eligible current loan.
6. Select `Loan History` to see returned loans.

The `Renew` button is disabled after a loan has already been renewed or when the
loan is overdue. Successful actions show `The book was returned successfully.`
or `The loan was renewed successfully.` Returning an overdue loan automatically
creates an outstanding fine.

### Create and cancel a reservation

1. Select `Reservation`.
2. Enter the book ISBN in `Enter book ISBN`.
3. Select `Create reservation`.
4. Review the reservation ID, ISBN, reserved date, expiry date, and status in
   `My reservations`.
5. For a pending reservation, select `Cancel reservation` if you no longer want
   it.

Reservations are for unavailable books. A successful request shows `Reservation
created successfully.` A successful cancellation shows `Reservation cancelled
successfully.` The default reservation period is seven days. The application
rejects unknown books, available books, duplicate pending reservations, and
inactive or unknown members with the corresponding validation message.

### View and pay fines

1. Select `Profile`.
2. Select `My Fines`.
3. Select `Outstanding` to see unpaid fines.
4. Select `Pay Fine` on an outstanding fine.
5. Select `History` to see paid fine history.

Fine cards show the book, amount, reason, issue date, and status. A successful
payment shows `Fine paid successfully.` Only the member who owns a fine can pay
it. If there are no records, the page shows `You have no outstanding fines.` or
`You have no fine history.`

### Read notifications

1. On `Dashboard`, review the unread items in `Notifications`.
2. Select `View all` to open the full `Notifications` page, or select an unread
   notification card directly.
3. Read the full message in the notification dialog.
4. Select the dialog's confirmation button to close it.

Opening a notification marks it as read. If there are no items, the dashboard
shows `You have no unread notifications.` and the full page shows `You have no
notifications.` Notifications may be overdue alerts or reservation reminders.

### Update member details and password

1. Select `Profile`.
2. Edit `Name` or `Email` under `Update information`.
3. Select `Update information`.
4. To change the password, enter values in `New password` and `Re-type new
   password`.
5. Select `Reset password`.

Successful updates show `Profile updated successfully.` and `Password updated
successfully.` Blank profile fields show `Name and email are required.` Blank
passwords show `Password is required.`, and mismatched passwords show `Passwords
do not match.`

## Librarian guide

Log in with account type `Librarian` to open `LibConnect Librarian`. The workspace
contains these tabs in the current release:

| Tab | Available actions |
| --- | --- |
| `Dashboard` | Refresh counts for active loans, overdue loans, reservations, and fines. |
| `Members` | Search, register, edit, reset a member password, deactivate, and activate members. |
| `Books` | Search, add, edit, and remove catalogue books. |
| `BookCopy` | Search, add, edit, delete, and change copy status. |
| `Loans` | List all, active, or overdue loans and alert the selected member. |
| `Reservations` | List, create, inspect pending reservations, cancel, fulfil, and send reminders. |
| `Fines` | List member/all fines, create from a loan, edit, and remove fines. |
| `Notifications` | List notifications for a user/member and mark a selected notification read. |

### Refresh the librarian summary

1. Open the `Dashboard` tab.
2. Select `Refresh summary`.

The summary is displayed as `Active loans: N | Overdue: N | Reservations: N |
Fines: N`, followed by `Dashboard refreshed`.

### Manage members

1. Open `Members`.
2. Search with `Search by ID, name, or email` and select `Search`.
3. Select a member row.
4. Use `Edit selected`, `Reset password`, `Deactivate selected`, or `Activate
   selected` as required.
5. Select `Register` to open the member registration dialog. Enter `Name`,
   `Email`, `Password`, and `Confirm password`, then confirm the dialog.

Successful actions show `Member registered`, `Member updated`, `Member password
reset`, `Member deactivated`, or `Member activated`. An action requiring a row
shows `Select a member first` when no member is selected.

### Manage books and physical copies

For catalogue books:

1. Open `Books`.
2. Search with `Search by ID, title, author, or category`.
3. Select `Add` to enter ISBN, title, author, publisher, category, and publication
   year, or select a row and use `Edit selected`.
4. Select `Remove selected` and confirm the `Delete book` dialog if the book and
   all copies for its ISBN should be permanently removed.

For copies:

1. Open `BookCopy`.
2. Search with `Search by copy ID, ISBN, or shelf`.
3. Select `Add` to enter a copy ID, an existing ISBN, and a shelf location.
4. Select a row and use `Edit selected` to change its shelf location or `Delete
   selected` to remove it.
5. Use `Mark damaged`, `Mark lost`, or `Mark available` to change its status.

Success messages include `Book added`, `Book updated`, `Book removed`, `Book copy
added`, `Book copy updated`, `Book copy deleted`, `Book copy marked damaged`,
`Book copy marked lost`, and `Book copy marked available`. A copy cannot be added
for an unknown ISBN.

### Review loans and send overdue alerts

1. Open `Loans`.
2. Select `All`, `Active`, or `Overdue`.
3. Select a loan row.
4. Select `Alert selected member` to create the member's overdue notification.

The table shows loan ID, member, copy, due date, and status. Alerts are
idempotent for the same member, notification type, and loan reference; retrying
does not create another copy of the same alert.

### Manage reservations and reminders

1. Open `Reservations`.
2. Enter `Member ID` and `Book ID` when creating a reservation, then select
   `Create`.
3. Leave `Member ID` blank and select `List` to list all reservations, or enter a
   member ID to list that member's reservations.
4. Enter a book ID and select `Pending for book` to inspect the pending queue.
5. Select a row and use `Cancel selected`, `Fulfil selected`, or `Send reminder`.

Success messages include `Reservation created`, `Reservation cancelled`,
`Reservation fulfilled`, and `Reservation reminder sent`. A reminder can be sent
only for a pending reservation that is still current.

### Manage fines

1. Open `Fines`.
2. Enter a `Member ID` and select `List member fines`, or select `List all fines`.
3. Enter a `Loan ID for new fine` and select `Create from loan` to create a fine
   for an overdue loan.
4. Select a fine row and use `Edit selected` or `Remove selected`.

Fine amounts use the configured daily overdue rate of $1.50. Only outstanding
fines can be edited. Success messages include `Fine created`, `Fine updated`,
and `Fine removed`.

### Review notifications

1. Open `Notifications`.
2. Enter a `User/member ID`.
3. Select `List`.
4. Select a notification and choose `Mark selected read`.

The table shows ID, type, read state, and message. The `Send reminder` action in
`Reservations` and the `Alert selected member` action in `Loans` are the normal
librarian entry points for creating member notifications.

### Sign out

Select `Log out` in the librarian header. The session is cleared and the login
page is displayed.

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| The window does not open from Maven. | JavaFX is running without a desktop display, or Maven is using the wrong Java. | Use JDK 25, verify `mvn --version`, and run with a desktop display. |
| Login reports `Select an account type.` | No account type was selected. | Select `Member` or `Librarian` before `Login`. |
| Login reports `Invalid member account.` or `Invalid librarian account.` | The selected account type does not match the account. | Select the account's actual type. |
| The dashboard is empty. | The catalogue has no matching records, or `data/` is empty. | Reset search filters, or ask a librarian to add books and copies. |
| Borrowing rejects a copy. | The ID is unknown or the copy is not `AVAILABLE`. | Open `BookCopy` as a librarian and check the copy status. |
| A reservation is rejected. | The book is available, unknown, or already has a pending reservation for this member. | Reserve only an unavailable book and check existing reservations. |
| A librarian action says `Select a ... first`. | No table row is selected. | Select the target row, then repeat the action. |
| Data cannot be loaded or saved. | A data file is missing, malformed at the root, inaccessible, or not writable. | Stop the application, restore the `data/` backup or permissions, and retry. |

Do not edit password hashes or delete individual JSON records while the
application is running. Keep a backup of the complete `data/` directory before
manual recovery.

## Testing commands

These commands are for a project checkout with JDK 25 and a desktop-capable
environment:

```text
mvn test
mvn clean verify
```

`mvn test` runs the unit, integration, persistence, controller, and JavaFX UI
tests. `mvn clean verify` runs the same lifecycle and creates the JaCoCo report at
`target/site/jacoco/index.html`. A successful run ends with `BUILD SUCCESS` and
zero failures. In a headless shell, the JavaFX tests require a configured
display; failure before assertions is an environment problem rather than a
member or librarian workflow result.
