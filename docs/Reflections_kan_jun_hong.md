## MP2 Reflections 
###### Kan Jun Hong, A0272072L

### What tasks were the AI agent customized to perform and how to determine the appropriate skill set for each task?
We divided the development roles according to the functions associated with the `Member` and `Librarian` user types. Before implementing our respective parts, we agreed on a predefined application architecture, which we documented in `PLAN.md`. The agent was instructed to refer to this document so that we could develop in a consistent direction and make it easier to integrate both parts later in the development process.

I instructed the agent to follow the good coding practices defined in the `seedu-git-standard` and `seedu-java-coding-standard` skills. This helped ensure that each class was well documented and that the codebase remained maintainable. These practices proved especially useful during the integration phase, as they allowed me to understand the responsibilities of the functions coded by my teammate more easily because we both used the skill during development. This reduced the time I needed to spend examining the codebase to understand its control flow.

I also added an instruction to supplement these coding practices. Although the aforementioned skills helped ensure code quality, they did not verify whether the code was implemented correctly through tests. Therefore, I included test generation in the agent's code-generation workflow. This ensured that, as classes were modified, their tests were kept up to date: new tests were added when new functionality was introduced, broken tests were fixed after refactoring, and redundant tests were removed when functionality was removed.

Finally, I instructed the agent to reuse existing library implementations where possible instead of implementing solutions from scratch. I added this instruction because of an incident I encountered during development when I needed a JSON parser with functionality beyond what the standard library provided. Specifically, I wanted the application to be able to read a `.json` data file even when it contained malformed or corrupted entries. The existing implementation using the Jackson library discarded all the data if one or more entries were malformed. This behaviour was extremely undesirable because I did not want a single corrupted entry to result in the loss of the entire local database. When I discussed the issue with the agent, it initially suggested implementing a parser from scratch, which I accepted at first. However, as development progressed, I realised that reimplementing a parser for each data file that required parsing and writing—such as `members.json`, `librarians.json`, and `books.json`—would be wasteful and inefficient. I also realised that integrating this approach with my teammate's code would be more difficult because they were using Jackson's JSON parser. After further discussion with the agent, I adopted the current solution: using the library's parser while adding checks to ignore malformed data without discarding all valid entries.

Because this was a greenfield project, the `plan-with-me` skill was developed to help with planning the application's architecture and the implementation of its classes and features. This skill was inspired by the [`grill-me` skill](https://github.com/mattpocock/skills/tree/main/skills/productivity/grill-me), which I found interesting and useful for planning and wanted to implement a similar idea myself. Although my implementation was not as thorough as `grill-me`, I found it effective overall. Since this project was simpler and smaller in scale than a production application, I could define the requirements clearly in the prompt, after which the agent provided useful responses that I could analyse before deciding which approach to adopt. However, I recognise that the lack of thorough questioning in the “grilling” process meant that any oversight on my part, such as forgetting to state a key requirement, could cause the agent to overlook it as well. The resulting code might then be less suitable for the intended use case. One example occurred when I implemented the `File...Repository` classes. I originally planned to implement them as separate classes, each with its own methods and extending its respective `...Repository` parent class. However, because our original architectural plan did not specify how these classes should be implemented, my teammate and I adopted different approaches: my teammate implemented the `...Repository` classes as interfaces instead. I believe that if the skill had questioned the user about specific implementation details, this inconsistency might have been partially avoided.

The `propose-unit-tests` skill was originally created to help me identify gaps in my proposed tests. It was designed as a sister skill to `plan-with-me`, but for test generation. During development, I realised that the agent could also use it as part of its workflow by proposing tests for classes it had just created. However, I decided not to include this in the workflow for a personal reason: I did not want the agent's output to become too long and distract me. I would then have had two tasks to focus on—validating whether the changes were made correctly and assessing whether the proposed test suite was appropriate. Therefore, I kept the instruction for the agent to highlight any required changes to the test suite instead of immediately providing a detailed proposal for those modifications.


### How did you define a specific skill and make sure that it is working?

The development of the `plan-with-me`, `propose-unit-tests`, and `generate-summary` skills followed a similar timeline. I will use `plan-with-me` to illustrate the process.

As a baseline, I used the `Skill-Creator` skill to create the skill's skeleton by providing its intended behaviour, use case, and output. I then manually tested several prompt variations to check whether the skill was invoked successfully. For example, the prompts I used to test the `plan-with-me` skill were as follows. The actual implementation details were also included in these tests, but they have been redacted here for conciseness.
- Direct invocation
    - “Use the plan with me skill to propose implementations for the Book class. It should (implementation requirements).”
    - “Use the skill $plan-with-me to propose how you would implement the Book class. It should (implementation requirements).”
- Implicit Invocation
    - “Propose a plan to implement the Book class. It should (implementation requirements).”
    - “Help me plan the implementation of the Book class. It should (implementation requirements).”
- Contextual Invocation
    - “The Book class is required to (implementation requirements). How would you implement this class?”
    - “What would the implementation of the Book class look like? It should (implementation requirements).”
- Negative Test
    - “Lay out the missing unit tests for the Book class.”

After verifying that all the prompts successfully invoked the skill as required, I began using the skill during development. I also looked out for instances in which the agent did not follow the output format specified by the skill, but no such instances occurred during development. About 25% of the way through development, I attempted to implement automated evaluations for the skill.

First, I collated the prompt inputs used in the earlier tests into a JSON file. A Python script read this file and passed the prompts to the agent through the Command Line Interface. I also created a separate validation script to check the agent's output for keywords such as “advantages,” “disadvantages,” “proposal 1,” and “proposal 2.” The aim was to verify that the agent listed at least two options and described the strengths and limitations of each approach. However, these tests failed occasionally because the agent could use alternative phrasing, such as “implementation 1” or “option 1.” I concluded that the tests were too brittle for evaluating non-deterministic agent outputs. I also did not think that specifying exact output wording in the skill would be beneficial: although it would solve the regular-expression matching issue, it would not assess the quality or relevance of the output to the prompt.

Next, I attempted a model-assisted testing approach. One instance of the agent generated the output, while another instance graded it according to fixed criteria for relevance, conciseness, structure, and proposal quality. Each metric had a weight of 25%, and the grader assigned each output a score out of 100, with a minimum passing threshold of 75. Although this approach worked initially, after running the tests several times I noticed that the grader would occasionally fail outputs that, upon closer inspection of the logs, should have passed according to the grading criteria. Further debugging did not help to identify the cause of the issue, and because I had already spent almost two days debugging the evaluation for a single skill, I decided that it was not worth continuing in the interest of time. I regret having to make that decision, but it was necessary. Afterward, I continued monitoring the agent's skill outputs to ensure that they contained all relevant information. I did not encounter issues despite the lack of structured evaluations, although I recognise that this may have been due to luck, the model's quality, or the fact that I often used prompts similar to the "implicit invocation” examples. The model was generally able to infer that I intended it to use the skill.


### What tasks were handled effectively by the agent and help to improve productivity, code quality, or testing efficiency?

The most obvious improvement in efficiency was the speed of code generation. When given a clear implementation plan, the agent generated code quickly and according to the specifications. A related benefit was its ability to generate Javadocs. This was especially useful during the integration phase, when I often had to add or modify methods so that my teammate's code and mine could integrate seamlessly. The agent also helped check for and update missing or outdated Javadocs and remove unused imports. This saved me from having to search through the codebase manually for these issues.

The agent was also able to highlight gaps in my proposed test suite through the `propose-unit-tests` skill. This helped improve test coverage and ensured that the application was tested sufficiently.

In general, the generated code was of good quality and easy to read, which made it maintainable.

### Where did the agent require additional guidance or correction? Were there situations where using the agent created additional work rather than reducing it?

Although the unit tests generated by the agent generally had good coverage, they did not always cover boundary values thoroughly. For example, consider a lookup for books by publication year, where the test case searches for books published between 2000 and 2020. The initial tests checked the values just outside the boundary and on the boundary—1999, 2000, 2020, and 2021—but did not check the values just inside the boundary, namely 2001 and 2019.

In addition, AI-generated tests sometimes attempted to verify too much in a single test case. The following example should have been split into multiple tests, and similar issues occurred in multiple tests. This made it harder to identify exactly what had failed.
``` java
@Test
void showAndClear_updatesVisibilityStyleAndText() {
    UiTestSupport.runOnFxThread(() -> {
        FeedbackMessage feedback = new FeedbackMessage();
        assertFalse(feedback.isVisible());
        assertFalse(feedback.isManaged());
        feedback.showError("Error");
        assertTrue(feedback.isVisible());
        assertTrue(feedback.isManaged());
        assertEquals("Error", feedback.getText());
        assertTrue(feedback.getStyleClass().contains("feedback-error"));
        feedback.showSuccess("Success");
        assertEquals("Success", feedback.getText());
        assertTrue(feedback.getStyleClass().contains("feedback-success"));
        assertFalse(feedback.getStyleClass().contains("feedback-error"));
        feedback.clearMessage();
        assertEquals("", feedback.getText());
        assertFalse(feedback.isVisible());
        assertFalse(feedback.isManaged());
    });
}
```


As with all AI-generated output, the agent cannot be fully trusted and still requires human oversight. This is the main source of workload when working with agents. Although the agent increases the speed at which code is produced, validating the generated code is, in my opinion, equally as draining as compared with writing the code manually.

To address this, I found that the most effective approach was to work on one component at a time rather than plan the implementation of several components and then ask the agent to implement all the approved plans at once. This approach introduces some inefficiency, but in my case it was unavoidable because I believe accuracy and correctness should take priority over speed.

One instance in which the agent created additional work occurred during the implementation phase. I initially thought it would be useful to let the agent update `PLAN.md` to reflect the implementations that had already been completed, so that both the agent and I could see the remaining work more clearly. However, for some reason, the additional information in `PLAN.md` made the agent less focused on the original architecture. It began proposing additional classes outside the planned architecture, introducing unnecessary complexity. This happened multiple times while I updated `PLAN.md` after each incremental implementation. I believe the modifications to `PLAN.md` caused this behaviour: after I reverted the file to its original state, removing the information about the repository's current state, the agent returned to the original planned architecture and stopped proposing changes that deviated from it.


### What would you change in the agent's instructions or skill set if you repeated the task?  What additional skills or tools would make the agent more useful?

For future projects, I would provide more implementation-specific information in `PLAN.md`. In this project, `PLAN.md` contained only a high-level architectural design. I noticed that my teammate had included more design-specific information for each class and service, which might have provided the agent with useful additional context.

Next, I would like to try the [`test-driven-development` skill](https://github.com/obra/superpowers/tree/main/skills/test-driven-development). It was an interesting skill that I unfortunately encountered only about halfway through the development phase. By then, I had already implemented all the required classes, so I was unable to test it. I relied heavily on the `plan-with-me` skill because it was working well and had helped me build the application, so I did not look for alternative skills that could be used. I also recognise that TDD is a good software development practice: it ensures that the generated code first meets the specifications, after which it can be refined and refactored to improve efficiency and code quality while retaining the safety net of existing tests.

Another skill I would like to try is [`code-review-and-quality`](https://github.com/addyosmani/agent-skills/blob/main/skills/code-review-and-quality/SKILL.md). As mentioned above, reviewing the agent-generated code was one of the main time sinks. After reviewing multiple code outputs, I noticed that I sometimes skimmed sections that I considered less important, which could allow errors to go unnoticed. Using this skill could provide an additional safety net to help ensure code quality and identify bugs before the code is committed.


### What did you learn about designing an effective single AI agent for software engineering tasks?

I learned that designing an effective single AI agent requires substantial pre-planning. The developer must produce the various artefacts that the agent will use to understand the project from multiple viewpoints, including both the high-level architecture and the details of individual components. This gives the agent a clearer idea of what the finished product should look like, allowing it to propose implementations and generate code that more effectively achieves the desired result.

Although I provided more information to the agent this time than I did in MP1, as noted earlier in this reflection, it was still insufficient. Moving forward, I intend to spend more time writing `PLAN.md` and other related documents to ensure that all relevant information is present before discussing the implementation plan with the agent.

