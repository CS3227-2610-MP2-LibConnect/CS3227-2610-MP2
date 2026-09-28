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

#### Contents
- [Quick Start](#quick-start)
- [Account Creation and Login](#account-creation-and-login)
- [Member Guide](#member-guide)
- [Librarian Guide](#librarian-guide)
- [Troubleshooting Common Errors](#troubleshooting-common-errors)
- [Testing Commands](#testing-commands)


## Quick Start

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

## Account Creation and Login

### Register an account

1. From the login page, select `Create account`.
   ![Create account from login](images/login_createAccount.png)

1. Select `Member` or `Librarian` in `Account type`.
   ![Select user type](images/createAccountSelectUserType.png)
1. Enter `Name`, `Email`, `Password`, and `Confirm password`.
   ![Input account details](images/createAccountFields.png)
1. If `Librarian` is selected, provide the `Employee ID` as well.
1. Select `Register`.
1. Return to the login page and sign in.

The two password fields must match. Blank required fields show `Name, email and
password are required.`, and mismatched passwords show `Passwords do not match.`
Librarian registration without an employee ID shows `Employee ID is required for
librarian accounts.` A successful registration shows `Account created
successfully. Please log in.`

### Logging in

1. Select `Member` or `Librarian` in the login page's account-type selector.
   ![Select account type on login](images/login_selectAccountType.png)
2. Enter the account email in the `Email` field.
3. Enter the password in the `Password` field.
4. Select `Login`.
5. Select `Logout` in the member navigation bar or `Log out` in the librarian
   workspace when finished.

Successful member login opens `Dashboard`. 
![Member dashboard](images/memberDashboard.png)

Successful librarian login opens the `LibConnect Librarian` workspace. 
![LibConnect Librarian Workspace](images/libconnectLibrarianWorkspace.png)

Invalid credentials show `Invalid email or password.` A deactivated account shows `This account is deactivated and cannot log in.`

## Member Guide

After logging in, members can access four pages from the navigation bar: **Dashboard**, **Borrow books**, **Reservation**, and **Profile**. The navigation bar also includes a **Logout** button.

Members can view their loans and fines, and update their name, email address, or password on the **Profile** page.

#### Contents
- [View Notifications](#view-notifications)
- [Random Book Recommendation](#random-book-recommendation)
- [Searching the Book Catalogue](#searching-the-book-catalogue)
- [Borrowing Books](#borrowing-books)
- [Creating a Reservation](#creating-a-reservation)
- [Update User Profile and Password](#update-user-profile-and-password)
- [View, Renew and Return Loans](#view-renew-and-return-loans)
- [View and Pay Fines](#view-and-pay-fines)

### View Notifications

![notification bar](images/notificationBar.png)

Members can view unread notifications in the notification bar on the dashboard. Click a notification to read its full details. To view all notifications, including those already read, click **View All**.

### Random Book Recommendation

LibConnect will provide a random recommendation for a book for members to try to read. 

### Searching the Book Catalogue

Members can search for a book in the available catalog using the search function on the `Dashboard` page. Members can click on a book to view more information about the book, as well as the number of copies available, and the shelf locations.

LibConnect supports 2 types of searches, Simple and Advanced.

![book card](images/bookCard.png)

![book information](images/bookInfo.png)


#### Simple Search

![simple search](images/simpleSearch.png)

Simple search is case-insensitive and returns books whose titles contain the keyword or phrase you enter. It supports partial matches. For example, searching for `cook` returns titles such as `COOK`, `cook`, and `cookbook`.

To view all books in the catalog, leave the search field blank and click **Search**, or click **Reset**.

#### Advanced Search

![advanced search](images/advancedSearch.png)

Advanced search lets you filter books by additional details, such as author, ISBN, and publication year. Leave a field blank to exclude it from the search.

Like simple search, text searches are case-insensitive and match partial text. ISBN and publication year searches are exceptions: enter the exact ISBN to search by ISBN.

For publication year, you can enter a start year, an end year, or both:

- **Start year only:** Finds books published in that year or later. For example, `2008` finds books published in 2008 and later.
- **End year only:** Finds books published in that year or earlier. For example, `2020` finds books published in 2020 and earlier.
- **Both years:** Finds books published between the start and end years, inclusive.

Similarly, to view all books in the catalog, leave the search field blank and click **Search**, or click **Reset**.

### Borrowing Books

![borrow books page](images/borrowBookPage.png)

Members can borrow books from the **Borrow books** page.

Enter the **Copy ID** of a book and click **Add book** to add it to the transaction. You can add up to 10 books per transaction. To remove a book from the list, click **Remove** beside it.

After adding all the books you want to borrow, click **Confirm Borrowing**. LibConnect will create a loan for each selected book.

![borrowing books process](images/borrowBookOperation.png)

> **NOTE:**
> Books marked as `BORROWED`, `DAMAGED`, or `LOST` cannot be borrowed. LibConnect will reject attempts to borrow these books.

### Creating a Reservation

![reservation page](images/reservationPage.png)

If all copies of a book are unavailable, members can reserve it from the **Reservation** page. Enter the book’s ISBN and click **Create Reservation**.

When a copy becomes available, LibConnect will notify the member to collect it. Members can cancel their reservation at any time before it is fulfilled. Once they have received the book, it can no longer be cancelled.

> **NOTE:**
> Members will not be allowed to make a reservation for a book if there are copies available currently.

![reservation process](images/makeReservation.png)

The default reservation period is seven days. LibConnect displays an error message if the book is unknown or if the member has a duplicate pending reservation, or the member is inactive or unknown.

### Update User Profile and Password 

![Update user profile and password](images/profilePage_updateProfile.png)

Members can update their name, email address, or password on the **Profile** page. LibConnect displays a success message when an update is complete or an error message if it fails.

When changing a password, enter the same password in both fields. If the passwords do not match, LibConnect displays an error message. Members cannot change their email address to one that is already in use; LibConnect displays an error message if they try.

### View, Renew, and Return Loans

![My Loans page](images/myloans.png)

Members can view their loans on the **My Loans** subpage of the **Profile** page. Select **Current Loans** to view active loans. From there, members can renew or return a loan. To view past loans, select **Loan History**.

> **NOTE:**
> All loans can only be renewed a maximum of one time.

> **NOTE:**
> Overdue loans cannot be renewed. When an overdue loan is returned, LibConnect automatically calculates and creates an outstanding fine for the member.

### View and Pay Fines

![My Fines page](images/myFines.png)

Members can view their fines on the **My Fines** subpage of the **Profile** page. Select **Outstanding** to view unpaid fines. Click **Pay Fine** to pay a fine. To view paid fines, select **History**.

> **NOTE:**
> A member can only pay a fine issued to their account. Currently, LibConnect does not support payment of fines for another member's account.

## Librarian Guide

After logging in with account type `Librarian`, the `LibConnect Librarian`
workspace opens. Librarians can manage members, books, physical book copies,
loans, reservations, fines, and notifications from the workspace tabs.

#### Contents

- [Librarian Dashboard](#librarian-dashboard)
- [Managing Members](#managing-members)
- [Managing Catalogue Books](#managing-catalogue-books)
- [Managing Book Copies](#managing-book-copies)
- [Reviewing Loans and Sending Overdue Alerts](#reviewing-loans-and-sending-overdue-alerts)
- [Managing Reservations and Reminders](#managing-reservations-and-reminders)
- [Managing Fines](#managing-fines)
- [Reviewing Notifications](#reviewing-notifications)
- [Signing Out](#signing-out)

### Librarian Dashboard

![Librarian Dashboard](images/librarianDashboard.png)

The `Dashboard` tab shows the current counts for active loans, overdue loans,
reservations, and fines.

1. Open the `Dashboard` tab.
2. Select `Refresh summary`.

LibConnect displays the summary as `Active loans: N | Overdue: N |
Reservations: N | Fines: N`, followed by `Dashboard refreshed`.

### Managing Members

[Member Management](images/librarianDashboard_member.png)

The `Members` tab lets librarians search for members, register new members,
edit member details, reset passwords, and change account status.

1. Open `Members`.
2. Enter a member ID, name, or email in `Search by ID, name, or email`.
3. Select `Search`.
4. Select a member row to edit, reset the password, deactivate, or activate the
   account.
5. Select `Edit selected`, `Reset password`, `Deactivate selected`, or `Activate
   selected` as required.
6. To register a member, select `Register`.
7. Enter `Name`, `Email`, `Password`, and `Confirm password` in the dialog.
8. Confirm the dialog.

Successful actions show `Member registered`, `Member updated`, `Member password
reset`, `Member deactivated`, or `Member activated`.

> **NOTE:**
> Select a member row before using an action that ends with `selected`. If no
> row is selected, LibConnect shows `Select a member first`.

### Managing Catalogue Books

![Book Catalogue Management](images/librarianDashboard_books.png)

Use the `Books` tab to maintain the catalogue records that members search.

1. Open `Books`.
2. Enter an ID, title, author, or category in `Search by ID, title, author, or
   category`.
3. Select `Search`.
4. Select `Add` to enter the book's `ISBN`, `Title`, `Author`, `Publisher`,
   `Category`, and `Publication year`.
5. Select a book row and use `Edit selected` to update its details.
6. Select `Remove selected` and confirm the `Delete book` dialog to permanently
   remove a book.

Successful actions show `Book added`, `Book updated`, or `Book removed`.

> **NOTE:**
> Removing a book also permanently removes all physical copies with the same
> ISBN. Confirm the `Delete book` dialog only when this is intended.

### Managing Book Copies

![Book Copy Management](images/librarianDashboard_bookCopy.png)

The `BookCopy` tab manages individual physical copies and their shelf locations.

1. Open `BookCopy`.
2. Enter a copy ID, ISBN, or shelf location in `Search by copy ID, ISBN, or
   shelf`.
3. Select `Search`.
4. Select `Add` and enter `Copy ID`, an existing `ISBN`, and `Shelf location`.
5. Select a copy row and use `Edit selected` to change its shelf location.
6. Select `Delete selected` to remove a copy.
7. Select a copy row and use `Mark damaged`, `Mark lost`, or `Mark available` to
   change its status.

Success messages include `Book copy added`, `Book copy updated`, `Book copy
deleted`, `Book copy marked damaged`, `Book copy marked lost`, and `Book copy
marked available`.

> **NOTE:**
> A copy can be added only for an existing ISBN. An unknown ISBN shows an error
> instead of creating the copy.

### Reviewing Loans and Sending Overdue Alerts

![Reviewing Loans](images/librarianDashboard_loans.png)

The `Loans` tab displays loan ID, member, copy, due date, and status. Use the
filters to review all loans, active loans, or overdue loans.

1. Open `Loans`.
2. Select `All`, `Active`, or `Overdue`.
3. Select a loan row.
4. Select `Alert selected member` to send an overdue notification to the member.

The alert action shows `Overdue alert sent`. Repeating the same alert does not
create a duplicate notification for the same loan.

### Managing Reservations and Reminders

![Manage Reservations](images/librarianDashboard_reservations.png)

The `Reservations` tab lets librarians create, view, cancel, and fulfil
reservations, as well as send collection reminders.

1. Open `Reservations`.
2. Enter `Member ID` and `Book ID`.
3. Select `Create` to create a reservation.
4. Leave `Member ID` blank and select `List` to view all reservations, or enter
   a member ID to view that member's reservations.
5. Enter a book ID and select `Pending for book` to view the pending reservation
   queue for that book.
6. Select a reservation row.
7. Select `Cancel selected`, `Fulfil selected`, or `Send reminder` as required.

Success messages include `Reservation created`, `Reservation cancelled`,
`Reservation fulfilled`, and `Reservation reminder sent`. A reminder can be sent
only for a pending reservation that is still current.

### Managing Fines

![Managing Fines](images/librarianDashboard_fines.png)

Use the `Fines` tab to view, create, edit, and remove member fines.

1. Open `Fines`.
2. Enter a member ID and select `List member fines`, or select `List all fines`.
3. Enter a loan ID in `Loan ID for new fine`.
4. Select `Create from loan` to create a fine for an overdue loan.
5. Select a fine row.
6. Select `Edit selected` to change an outstanding fine's amount, or select
   `Remove selected` to remove it.

Fine amounts use the configured daily overdue rate of $1.50. Successful actions
show `Fine created`, `Fine updated`, or `Fine removed`.

### Reviewing Notifications

![Reviewing Notifications](images/librarianDashboard_notifications.png)

The `Notifications` tab lets librarians review notifications for a member and
mark them as read.

1. Open `Notifications`.
2. Enter a user or member ID in `User/member ID`.
3. Select `List`.
4. Select a notification row.
5. Select `Mark selected read`.

The table shows the notification ID, type, read state, and message. A successful
update shows `Notification marked read`. The `Send reminder` action in
`Reservations` and the `Alert selected member` action in `Loans` are the normal
librarian entry points for creating member notifications.

### Signing Out

Select `Log out` in the librarian header. LibConnect clears the session and
returns to the login page.

## Troubleshooting Common Errors

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
