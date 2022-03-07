# Pull requests

## Introduction

This document gives some clue about how to efficiently manage Pull Requests (PR). This document is a first draft and may be improved later.

## Who should read this document?

Every pull request reviewers, but also probably every ones who submit PRs.

## Submitting PR

### Who can submit pull requests?

Basically every one who wants to contribute to the project! But there are some rules to follow.

#### Humans

People with write access to the project can directly clone the project, push their branches and create PR.

External contributors must first fork the project and create PR to the mainline from there.

##### Draft PR?

Draft PR can be created when the submitter does not expect the PR to be reviewed and merged yet. It can be useful to publicly show the work, or to do a self-review first.

Draft PR can also be created when it depends on other un-merged PR.

In any case, it is better to explicitly declare in the description why the PR is a draft PR.

Also, draft PR should not stay indefinitely in this state. It may be removed if it is the case and the submitter does not update it after a few days.

##### Assigned to reviewer?

For the moment we do not directly assign PR to ask for a review. In the future we could have some automation to do that. Anyway the core team should have a look to every PR, even when not digging into details. More reviewers, more eyes on the code change, more valuable feedback!

##### When create split PR?

To implement big new feature, it may be efficient to split the work into several smaller and scoped PRs. They will be easier to review, and they can be merged on `develop` faster.

Big PR can take time, and there is a risk of future merge conflict.

Feature flag can be used to avoid half implemented feature to be available in the application.

That said, splitting into several PRs should not have the side effect to have more review to do, for instance if some code is added, then finally removed.

##### Avoid fixing other unrelated issue in a big PR

Each PR should focus on a single task. If other issues may be fixed when working in the area of it, it's preferable to open a dedicated PR.

It will have the advantage to be reviewed and merged faster, and not interfere with the main PR.

It's also applicable for code rework, or code formatting. Sometimes, it is more efficient to extract that work to a dedicated PR, and rebase your branch once this "rework" PR has been merged.

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

`Update Gradle Wrapper` is a tool which can create PR to upgrade our gradle.properties file.
Review such PR is the same recipe than for PR from Dependabot

##### Sync analytics plan

This tools imports any update in the analytics plan. See instruction in the PR itself to handle it.
More info can be found in the file [analytics.md]

## Reviewing PR

### Who can review pull requests?

As an open source project, every one can review each others PR. Of course an approval from internal developer is mandatory for a PR to be merged.
But comment in PR from the community are always appreciated!

### What to have in mind when reviewing a PR

1. User experience: is the UX and UI correct? You will probably be the second person to test the new thing, the first one is the developer.
2. Developer experience: does the code look nice and decoupled? No big functions, new classes added to the right module, etc.
3. Code maintenance. A bit similar to point 2. Tricky code must be documented for instance
4. Fork consideration. Will configuration of forks be easy? Some documentation may help in some cases.
5. We are building long term products. "Quick and dirty" code must be avoided.
6. The PR includes new tests for the added code, updated test for the existing code

### Rules

#### Check the form

##### PR description

PR description should follow the PR template, and at least provide some context about the code change.

##### File change

1. Code should follow the guidelines
2. Code should be formatted correctly
3. XML attribute must be sorted
4. New code is added at the correct location
5. New classes are added to the correct location
6. Naming is correct. Naming is really important, it's considered part of the documentation
7. Architecture is followed. For instance, the logic is in the ViewModel and not in the Fragment
8. There is at least one file for the changelog. Exception if the PR fixes something which has not been released yet. Changelog content should target their audience: `.sdk` extension are mainly targeted for developers, other extensions are targeted for users and forks maintainers. It should generally describe visual change rather than give technical details. More details can be found [here](../CONTRIBUTING.md#changelog).
9. PR includes tests. allScreensTest when applicable, and unit tests
10. Avoid over complicating things. Keep it simple (KISS)!
11. PR contains only the expected change. Sometimes, the diff is showing changes that are already on `develop`. This is not good, submitter has to fix that up.

##### Check the commit

Commit message must be short, one line and valuable. "WIP" is not a good commit message. Commit message can contain issue number, starting with `#`. GitHub will add some link between the issue and such commit, which can be useful. It's possible to change a commit message at any time (may require a force push).

Commit messages can contain extra lines with more details, links, etc. But keep in mind that those lines are quite less visible than the first line.

Also commit history should be nice. Having commits like "Adding temporary code" then later "Removing temporary code" is not good. The branch has to be rebased and those commit have to be dropped.

PR merger could decide to squash and merge if commit history is not good.

Commit like "Code review fixes" is good when reviewing the PR, since new changes can be reviewed easily, but is less valuable when looking at git history. To avoid this, PR submitter should always push new commits after a review (no commit amend with force push), and when the PR is approved decide to interactive rebase the PR to improve the git history and reduce noise.

##### Check the substance

1. Test the changes!
2. Test the nominal case and the edge cases
3. Run the sanity test for critical PR

##### Make a dedicated meeting to review the PR

Sometimes a big PR can be hard to review. Setting up a call with the PR submitter can speed up the communication, rather than putting comments and questions in GitHub comments. It has the inconvenience of making the discussion non-public, consider including a summary of the main points of the "offline" conversation in the PR.

### What happen to the issue(s)?

The issue(s) should be referenced in the PR description using keywords like `Closes` of `Fixes` followed by the issue number.

Example:
> Closes #1

Note that you have to repeat the keyword in case of a list of issue

> Closes #1, Closes #2, etc.

When PR will be merged, such referenced issue will be automatically closed.
It is up to the person who has merged the PR to go to the (closed) issue(s) and to add a comment to inform in which version the issue fix will be available. Use the current version of `develop` branch.

> Closed in Element Android v1.x.y

### Merge conflict

It's up to the submitter to handle merge conflict. Sometimes, they can be fixed directly from GitHub, sometimes this is not possible. The branch can be rebased on `develop`, or the `develop` branch can be merged on the branch, it's up to the submitter to decide what is best.
Keep in mind that Github Actions are not run in case of conflict.

### When and who can merge PR

PR can be merged by the submitter, if and only if at least one approval from another developer is done. Approval from all people added as reviewer is also a good thing to have. Approval from design team may be mandatory, but is not sufficient to merge a PR.

PR can also be merged by the reviewer, to reduce the time the PR is open. But only if the PR is not in draft and the change are quite small, or behind a feature flag.

Dangerous PR should not be merged just before a release. Dangerous PR are PR that could break the app. Update of Realm library, rework in the chunk of Events management in the SDK, etc.

We prefer to merge such PR after a release so that it can be tested during several days by the team before behind included in a release candidate.

### Resolve conversation

Generally we do not close conversation added during PR review and update by clicking on "Resolve conversation"
If the submitter or the reviewer do so, it will more difficult for further readers to see again the content. They will have to open the conversation to see it again. it's a waste of time.

When remarks are handled, a small comment like "done" is enough, commit hash can also be added to the conversation.

## Responsibility

PR submitter is responsible of the incoming change. PR reviewers who approved the PR take a part of responsibility on the code which will land to develop, and then be used by our users, and the user of our forks.

That said, bug may still be merged on `develop`, this is still acceptable of course. In this case, please make sure an issue is created and correctly labelled. Ideally, such issues should be fixed before the next release candidate, i.e. with a higher priority. But as we release the application every 10 working days, it can be hard to fix every bugs. That's why PR should be fully tested and reviewed before being merge and we should never comment code review remark with "will be handled later", or similar comments.