## MP2 Reflections

###### Cai Jiali

### What tasks were the AI agent customized to perform and how did I determine the appropriate skill set for each task?

Before I let the agent implement anything, I gave it three kinds of instructions: the architecture our team had agreed on, the librarian business rules I did not want it to invent, and one skill for each kind of task.

The architecture came first because we were building different roles. We agreed to split the system into Member and Librarian, separate a `Book` from a physical `BookCopy`, keep business rules in services, put persistence behind repositories, and keep the JavaFX views away from the files. I wrote that into `PLAN.md`, including the class diagram and the work-split section, so both of us were using the same names before either role was coded. I pointed the agent at `PLAN.md` while I built the librarian features. The rules that had to stay true during coding, such as stable IDs, shared interfaces, and who owned which classes, went into `AGENTS.md`.

A shared architecture still left the librarian rules open. I wrote those down before the classes existed. For example, a reservation is rejected when that member already has a pending reservation for the same book, and it is allowed only for an active member and a book that is unavailable. And a fine is calculated in `FineService`, only for days after the due date, and rounded with `RoundingMode.HALF_UP` to two decimal places.

I chose a skill by the artifact it had to produce. If my teammate had already added a skill for that artifact, I used his. If the artifact was different, I added one.

- `plan-with-me` was for a design that was still open. I used it for the librarian JavaFX layout. The choice was one librarian page with tabs, or a separate scene for each task. We kept one `LibrarianPage`: a header, tabs for Dashboard, Members, Books, BookCopy, Loans, Reservations, Fines, and Notifications, and one status line. The session, the navigation, and the message could then live in that page.
- `propose-unit-tests` was for choosing cases before the agent wrote the test code. I used it after each feature, for boundaries, state changes, bad input, a failing collaborator, and a save-and-load round trip.
- `seedu-java-coding-standard` and `seedu-git-standard` were the shared rules for both of us. They kept generated classes documented, which made my teammate's code easier for me to read during integration, and they kept branches and commits understandable while we merged the UI, the guides, CI, and the release.
- I added `authoring-user-guide` and `authoring-developer-guide` by adapting online guide skills to this repository. They have different readers and different sources of truth.
- I added `github-actions-javafx-ci` after a JavaFX run that passed on my Mac and did not finish on GitHub Actions.
- I added `acceptance-testing` because a passing fine-amount test does not show that login, a cross-role effect, a restart, and the packaged JAR work together.

`AGENTS.md` was the file I had to keep editing. It started as development rules. By the end it also held role ownership, test requirements, what had been integrated, and what was still unfinished, including the product website. That gave the agent current context once the repository was large. It also showed me a risk thata temporary status line sitting next to a permanent rule can be treated as part of the design.

### How did I define a specific skill and make sure that it was working?

I kept each skill small, and I trusted it only after I had used it on a real task and compared the result with a file I could open.

The two guide skills show how I drew the line. Both write Markdown, and they are different skills. `authoring-user-guide` is for a member or a librarian: the steps, the exact label, and the error they will see. I adapted it from an online authoring skill. Its sources are the JavaFX pages and the passing tests, including labels such as `Login`, `Reservations`, and `Confirm Borrowing`. `authoring-developer-guide` is for someone who has to build the repository: Java 25, Maven, the packages, persistence, the tests, CI, and the release. The CI skill only diagnoses `.github/workflows/ci.yml` and `pom.xml`. The acceptance skill only records evidence about the finished product, with a status of `Pass`, `Fail`, `Blocked`, or `Not run`.

To check a skill, I compared the output with the repository. For the guides, that was the pages, the tests, `pom.xml`, the workflows, and the release configuration. A paragraph can sound finished and still name a button, a path, or a command that is not in the project. The first drafts needed another pass for the exact commands and the failure messages. I also ran `git diff --check`. That only catches whitespace problems. I never built a check that the skill had actually been invoked, or that the prose was true. The useful check was opening the file the skill claimed to describe.

For `github-actions-javafx-ci`, the file I could open was the workflow after a failed run. On the librarian branch I had already added `.github/workflows/ui-tests.yml`. That file already wrapped Maven in `xvfb-run`, and I deleted it the same night and went back to local UI tests. The failure that remained was on the main workflow: `mvn --batch-mode --update-snapshots clean verify` on `ubuntu-latest`, with no virtual display. The developer guide at that point still said the JavaFX display requirement was an environment risk. The skill told me to read the whole workflow, map each job to its Maven command, and look at `needs`. The change that landed was to run both the verify job and the release `package` job under `xvfb-run`. The release job already waited on the build job with `needs: build`, so a tag does not start the release unless verify succeeds. I could confirm that by reading the YAML. I could not confirm the Ubuntu hang from my Mac, because this machine already has a display.

### What tasks were handled effectively by the agent and helped to improve productivity, code quality, or testing efficiency?

The agent helped most when I gave it one small piece of work, a clear contract, and a test that had to pass before I moved on.

The time saving was the repetitive structure. Once `PLAN.md` had fixed the shape of a repository, a service, and a controller, the agent could fill in the next class in that shape, and the coding standard required a Javadoc on the new public methods. I spent my time on the rule and the review.

The code-quality gain is real for part of the librarian screen, and I need to say which part. Fine amounts stay in `FineService.calculateOverdueFine`: days after the due date, times the daily rate, rounded with `HALF_UP`. Member registration and search, book add, edit, and remove, loan lists, fine edit and remove, and sending an overdue alert or a reservation reminder go through `LibrarianController`. That controller calls `librarianService.requireActive` before the service, so those button handlers do not contain a second copy of the fine arithmetic. `LibrarianPage` still calls `BookCopyService`, `ReservationService`, `FineService.createFine`, and `NotificationService` directly, and several of those handlers call `requireActiveLibrarian()` in the page. The controller boundary held for the member and catalogue actions. Copies, reservation changes, creating a fine, and marking a notification read still go around it.

The testing gain depended on me choosing the cases. The agent was good at the fixtures and assertions after that choice. The librarian tests grew from 21 to 75 in the coverage expansion. Those tests cover invalid and duplicate librarian data, an inactive librarian, reservation transitions and expiry, pending-reservation order, fine amount boundaries, duplicate notifications, malformed JSON, a missing file, safe replacement, and a save-and-load round trip. After the roles were integrated, I added tests for signed-in and signed-out routes, registration, borrowing failures, renewal, notification read state, returning a loan, reservation validation, fine rollback, and the librarian active and overdue filters. `mvn --batch-mode clean verify` now reports 527 tests, 0 failures, 0 errors, and 0 skips. The JaCoCo report from that run covers 92.6% of the lines in `libconnect.ui.pages` and 96.4% of the lines in `libconnect.services`. Those percentages showed me which lines had never run. They were a map of unexercised code, which is a different result from a correct product.

### Where did the agent require additional guidance or correction? Were there situations where using the agent created additional work?

Most of the corrections were about the size of the claim. The agent would report a result that was true for a smaller check than the one I thought we had finished, and twice that sent me into extra work.

A green service test did not mean the JavaFX route, the save to disk, or the other role had been exercised. Starting the JAR without an exception did not mean a user had finished a workflow. I got into the habit of opening the report, the source, the workflow file, and the release folder, and of treating the closing summary as a pointer to those files.

The CI failure is where the agent cost me time, and I had the sequence muddled while I was in it. I added a separate UI workflow, `ui-tests.yml`, which already used `xvfb-run`, then removed it the same night because I still could not tell whether the hang was our test or the runner. After that I treated a local UI run as evidence about GitHub. The main `ci.yml` was still running `mvn clean verify` with no display. On the hosted runner the suite could reach a JavaFX test and stall during toolkit startup, because `ubuntu-latest` has no screen. The fix I committed wraps the verify job and the release `package` job in `xvfb-run`. `needs: build` was already on the release job from when that job was added, so the display wrapper was the missing piece. I still cannot reproduce that hang on my Mac. The YAML shows the wrapper. A local green run does not show how the next Ubuntu job will behave.

The other extra work came from a shortcut I had allowed. The first librarian shell, both the CLI and the JavaFX main class, was wired through `LibrarianCompositionRoot` to `DemoMemberManagement`, `DemoBookManagement`, `DemoBookCopyManagement`, and `DemoLoanQuery`, so the screen could run before shared authentication and the member-role providers were connected. That was a reasonable step. It became extra work because the agent was content to leave the demos in place. I had to keep sending it back to the shared contracts. I also wanted `LibrarianController` to be the authorization boundary. That holds for members, books, loan views, and fine edit and remove. The page still checks `requireActive` itself for copies, reservation changes, fine creation, and notification reads.

Two smaller corrections were details that looked precise and were wrong. During the coverage pass, two tests failed because the fixtures were wrong. One used the wrong overdue-date boundary. The other gave the notification a recipient ID that did not belong to that scenario. I fixed the tests and reran them. I left the production code as it was. The first guide draft had a similar shape. It described LibConnect in general, and it left out the commands, the labels, and the failure messages someone would need in order to carry out a task.

### What would I change in the agent's instructions or skill set if I repeated the task? What additional skills or tools would make the agent more useful?

I would keep the four skills I added: `authoring-user-guide`, `authoring-developer-guide`, `github-actions-javafx-ci`, and `acceptance-testing`. What I would change is the work they never covered, and the way their results get written down.

The product website is the obvious missing skill. `AGENTS.md` still lists it as unfinished, and none of the current skills tells the agent how to build a site. A website skill should take the implemented features and `libconnect-1.0.0.jar` as its sources, check the links and the screenshots, and stop when a claim is not in those sources.

I would also want an integration-review skill, used as soon as the two role branches are merged. We could test a class, and we could run final acceptance, and we had no single checklist for the things that appear only when both roles exist: stable IDs, shared login, a member action, the librarian's view of that action, the automatic overdue fine, the notification, and whether the data is still there after a restart. The same checklist would ask whether a button calls `LibrarianController` or reaches a service from the page. I found that split by reading `LibrarianPage`. Running the review right after the merge would have shown it while the demos were still being removed.

The acceptance skill already asks for the command, the test count, where the report is, the environment, and which cases were not run. I would turn that into a fixed table. In a paragraph it is easy to mention the 527 passes and let the blocked manual check fade into the ending. I would also state that building the JAR, seeing the login window, and finishing a workflow are three separate results.

I would separate the design from the progress notes. `PLAN.md` worked as the design we agreed at the beginning. `AGENTS.md` became a mixture of permanent rules, current status, and temporary blockers. I would keep the status in its own section, so a temporary line is not sitting in the same list as a rule the agent must always follow.

I would put the virtual display on the main verify job before I treated a local green run as a CI result. The separate UI workflow was removed the night I added it, and `ci.yml` still had no display until the later fix. If `xvfb-run` had been on that job from the start, the hang would have been easier to separate from a product bug.

There are two skills I would try on a later project. I came across test-driven development and a code-review skill after most of LibConnect was already written, so I cannot claim they would have changed this repository. I can say where they would have fitted. `propose-unit-tests` was used after a feature existed, so the failing cases arrived late. Writing the service test first would have fixed the contract before the agent wrote the class. A review pass aimed at pages would have asked whether `LibrarianPage` was calling `BookCopyService` and `ReservationService` beside the controller. I would still read that review myself.

### What did I learn about designing an effective single AI agent for software engineering tasks?

One agent can take on a lot of the writing when the task is small, the instruction stays stable, and the evidence is a file I can open.

The weak instructions were the vague ones. "Write a good user guide" gave me a plausible overview. "Use the current JavaFX labels, tests that pass, and the real failure states" gave me text I could compare with `LoginPage`, `ReservationPage`, and `BorrowPage`. "Check CI" was similarly weak next to "read `ci.yml`, check `needs`, and give JavaFX a display before a release job runs." `PLAN.md`, the reservation and fine rules, and the four skills were all ways of making an instruction that specific.

The time I saved was real for services, repositories, tests, the UI wiring, the guides, and the workflow edit. What stayed with me was choosing the architecture, checking that the business rules were the ones we wanted, fixing fixtures, and deciding that a green summary was the wrong kind of evidence. The rule that made this manageable was to finish one boundary, test it, and only then connect it to the next piece. When I changed several providers, or the workflow, in the same step, I could no longer tell which change had broken the build. The write-up had to say, plainly, which checks I had not been able to run. For this project that includes clicking through the workflows on a real Java 25 window.

### Summary of interesting skills used

These are the four skills I created for LibConnect. For each one I have said why I needed it, how I used it, what convinced me that it helped, and where it still fell short.

#### 1. `authoring-user-guide`

**Purpose.** I added this skill because my first user-guide draft read like a description of the product. A member or a librarian needs the steps for a task.

**How I used it.** I made the implemented JavaFX pages the source. The skill asks for the real labels, such as `Login`, `Reservations`, and `Confirm Borrowing`, the steps in order, the validation message the user sees, what has to be true before they start, and a short troubleshooting note in the form of symptom, cause, and fix. The guide keeps the two roles apart: borrowing, renewing, returning, reserving, viewing fines, reading notifications, managing members, and processing reservations.

**Evidence and impact.** I checked the guide against the UI source and the integration tests. That caught labels and failure messages that the first draft had smoothed over, and it kept the guide on workflows the pages actually implement.

**Limitation.** The agent can still give me a guide with the right sections and one wrong detail. The skill tells me what to verify. I still have to open the screen or the test. I also never checked whether a prompt had actually invoked the skill.

#### 2. `authoring-developer-guide`

**Purpose.** I wrote this for a developer who needs to build, extend, test, or release LibConnect. A generic Maven guide would not explain our dependency direction, the stable-ID contracts, the file-backed repositories, `StorageManager`, what happens to a malformed record, the JavaFX test limits, or the release profile.

**How I used it.** Before writing, the agent has to read `AGENTS.md`, `PLAN.md`, `pom.xml`, the workflows, the source packages, the composition roots, the repositories, the tests, and the release configuration. The guide has to include commands I can run, with the output I should expect. I used it to document the Java 25 and Maven setup, what each package is responsible for, how persistence behaves, how to run the UI tests, why GitHub Actions needs a display, and how `libconnect-1.0.0.jar` is produced.

**Evidence and impact.** The agent pulled together information that was scattered through the repository and put it in one guide. When `ci.yml` gained the `xvfb-run` wrapper, the guide still described the old `mvn --batch-mode --update-snapshots clean verify` command. Comparing the guide with the workflow caught that stale command.

**Limitation.** The guide goes out of date as soon as a workflow, a file name, or the release configuration changes. A well organised guide is a different thing from an accurate one, so I still checked the commands and paths against the current tree.

#### 3. `github-actions-javafx-ci`

**Purpose.** I created this skill after a concrete failure. The JavaFX tests ran on my machine, and the GitHub Actions workflow did not finish.

**How I used it.** The skill tells the agent to read the whole workflow and `pom.xml`, map each job to its Maven command and to the tests it runs, check the `needs` dependencies, and separate a product bug from a runner problem such as a missing display. Following that led me to `ci.yml`, which was running verify with no display, after I had already deleted the short-lived `ui-tests.yml`.

**Evidence and impact.** The hosted runner needed a virtual display. Maven on the build job and on the release job now runs through `xvfb-run --auto-servernum --server-args="-screen 0 1920x1080x24"`. The release job depends on the full build with `needs: build`, which was already true before the display wrapper was added. I checked that structure in the workflow file. A failed verify job is what stops the release job from starting.

**Limitation.** My Mac could not reproduce the Ubuntu runner. I treated the local reading of the YAML as support for the fix, and I left the remote run as something this machine cannot prove.

#### 4. `acceptance-testing`

**Purpose.** By the end of the project, a passing unit suite was necessary and still left the product-level questions open. A test can show that a fine amount was calculated correctly. It leaves open whether a member's overdue return created that fine, whether a librarian could see it, whether the data survived a restart, and whether the packaged JAR opened the right application.

**How I used it.** The skill lays out an acceptance matrix across login, member workflows, librarian workflows, effects that cross the two roles, persistence, and the release JAR. It asks for an isolated data directory, the Java and Maven versions, the exact command, the expected and actual results, and a status of `Pass`, `Fail`, `Blocked`, or `Not run`.

**Evidence and impact.** I ran `mvn --batch-mode clean verify`: 527 tests, 0 failures, 0 errors, and 0 skips. I rebuilt the JAR, checked its main class and Java 25 metadata, and launched it from temporary storage. The automated tests and JavaFX startup passed. Clicking through login, the member workflows, the librarian workflows, the cross-role effects, and a restart stayed blocked, because the session had no native window I could control.

**Limitation.** Recording the gap does not remove it. Full manual acceptance still needs a real Java 25 desktop session with a window I can use. Starting the application is a different result from a user finishing the workflow.
