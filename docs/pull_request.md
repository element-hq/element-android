# Pull requests

## Introduction

This document gives some clue about how to efficiently manage Pull Requests (PR). This document is a first draft and may be improved later.

## Who should read this document?

Every pull request reviewers, but also probably every ones who submit PRs.

## Submitting PR

### Who can submit pull requests?

Basically every one who wants to contribute to the project! But there are some rules to follow.

#### Humans

Developers who are employed (or paid) by Element can directly clone the project and push their branches and create PR.

External contributors must first fork the project and create PR to the mainline from there.

##### Draft PR?

Draft PR can be created when the submitter does not expect the PR to be reviewed and merged yet. It can be useful to publicly show the work, or to do a self-review first.

Draft PR can also be created when it depends on other un-merged PR.

In any case, it is better to explicitly declare in the description why the PR is a draft PR.

##### Assigned to reviewer?

For the moment we do not directly assign PR to ask for a review. In the future we could have some automation to do that. Anyway the core team should have a look to every PR, even when not digging into details. More reviewers, more eys on the code change, more valuable feedback!

##### When create split PR?

To implement big new feature, it may be efficient to split the work into several smaller and scoped PRs. They will be easier to review, and they can be merged on develop faster.

Big PR can take time, and there is a risk of future merge conflict.

Feature flag can be used to avoid half implemented feature to be available in the application.

That said, splitting into several PRs should not have the side effect to have more review to do, for instance if some code is added, then finally removed.

##### Avoid unrelated fixing other issue in a big PR

Each PR should focus on a single task. If other issues may be fixed when working in the area of it, it's preferable to open a dedicated PR.

It will have the advantage to be reviewed and merged faster.

#### Bots

Some bots can create PR, but they still have to be reviewed by the team

##### Dependabot

Dependabot is a tool which maintain all our external dependencies up to date. A dedicated PR is created for each new available release for one of our external dependency.Dependabot

To review such PR, you have to
 - **IMPORTANT** check the diff files (as always).
 - Check the release note. Some existing bugs in Element project may be fixed by the upgrade
 - Make sure that the CI is happy
 - If the code does not compile (API break for instance), you have to checkout the branch and push new commits
 - Do some smoke test, depending of the library which has been upgraded

For some reason dependabot sometimes does not upgrade some dependencies. In this case, and when detected, the upgrade has to be done manually.

##### Gradle wrapper

`Update Gradle Wrapper` is a tool which can create PR tu upgrade our gradle.properties file.
Review such PR is the same recipe than for PR from Dependabot

##### Sync analytics plan

This tools import any update in the analytics plan. See instruction in the PR itself to handle it.
More info can be found in the file [analytics.md]

## Reviewing PR

### Who can review pull requests?

As an open source project, every one can review each others PR. Of course an approval from internal developer is mandatory for a PR to be merged.
But comment in PR from the community are always appreciated!

### What to have in mind when reviewing a PR

1- User experience: is the UX and UI correct? You will probably be the second person to test the new thing, the first one is the developer.
2- Developer experience: does the code looks nice and decoupled? No big functions, etc.
3- Code maintenance. A bit similar to point 2. Tricky code must be documented for instance
4- Fork consideration. Is configuration of forks will be easy? Some documentation may help in some cases.

### Rules

#### Check the form

##### PR description

PR description should follow the PR template, and at least provide some context about the code change.

##### File change

1- Code should follow the guidelines
2- Code should be formatted correctly
3- XML attribute must be sorted
4- New code is added at the correct location
5- New classes are added to the correct location
6- Naming is correct. Naming is really important, it's considered part of the documentation
7- Architecture is followed. For instance, the logic is in the ViewModel and not in the Fragment
8- There is at least one file for the changelog. Exception if the PR fixes something which has not been released yet.
9- PR includes test. allScreensTest when applicable, and unit tests
10- Avoid over complicating things. Keep it simple (KISS)!
11- PR contains only the expected change. Sometimes, the diff is showing changes that are already on develop. This is not good, submitter has to fix that up.

##### Check the commit

Commit message must be short, one line and valuable. "WIP" is not a good commit message.

Also commit history should be nice. Having commits like "Adding temporary code" then later "Removing temporary code" is not good. The branch has to be rebased and those commit have to be dropped.

PR merger could decide to squash and merge if commit history is not code.

Commit like "Code review fixes" is good when reviewing the PR, since new changes can be reviewed easily, but is less valuable when looking at git history. I have no good solution to avoid this.

##### Check the substance

1- Test the changes!
2- Test the nominal case and the edge cases
3- Run the sanity test for critical PR

##### Make a dedicated meeting to review the PR

Some times, big PR can be hard to review. Setting up a call with the PR submitter can faster the communication, rather than putting comment and questions as GitHub comment. It has the inconvenient to not make it public.

### What happen to the issue(s)?

The issue(s) should be referenced in the PR description using keywords like `Closes` of `Fixes` followed by the issue number.

Example:
> Closes #1

Note that you have to repeat the keyword in case of a list of issue

> Closes #1, Closes #2, etc.

When PR will be merged, such referenced issue will be automatically closed.
It up to the person who has merged the PR to go to the (closed) issue(s) and to add a comment to inform in which version the issue fix will be available. Use the current version of develop branch.

> Closed in Element Android v1.x.y

### Merge conflict

It's up to the submitter to handle merge conflict. Sometimes, they can be fixed directly from GitHub, sometimes this is not possible. The branch can be rebased on develop, or develop can be merged on the branch, it's up to the submitter to decide what is best.

### When and who can merge PR

PR can be merged by the submitter, if and only if at least one approval from another developer is done. Approval from all people added as reviewer is also a good thing to have. Approval from design team may be mandatory, but is not sufficient to merge a PR.

Dangerous PR should not be merged just before a release. Dangerous PR are PR that could break the app. Update of Realm library, rework in the chunk of Events management in the SDK, etc.
