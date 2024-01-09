# Pull requests

<!--- TOC -->

* [Introduction](#introduction)
* [Who should read this document?](#who-should-read-this-document?)
* [Submitting PR](#submitting-pr)
  * [Who can submit pull requests?](#who-can-submit-pull-requests?)
    * [Humans](#humans)
      * [Draft PR?](#draft-pr?)
      * [Base branch](#base-branch)
      * [PR Review Assignment](#pr-review-assignment)
      * [PR review time](#pr-review-time)
      * [Re-request PR review](#re-request-pr-review)
      * [When create split PR?](#when-create-split-pr?)
      * [Avoid fixing other unrelated issue in a big PR](#avoid-fixing-other-unrelated-issue-in-a-big-pr)
    * [Bots](#bots)
      * [Dependabot](#dependabot)
      * [Gradle wrapper](#gradle-wrapper)
      * [Sync analytics plan](#sync-analytics-plan)
* [Reviewing PR](#reviewing-pr)
  * [Who can review pull requests?](#who-can-review-pull-requests?)
  * [What to have in mind when reviewing a PR](#what-to-have-in-mind-when-reviewing-a-pr)
  * [Rules](#rules)
    * [Check the form](#check-the-form)
      * [PR title](#pr-title)
      * [PR description](#pr-description)
      * [File change](#file-change)
      * [Check the commit](#check-the-commit)
      * [Check the substance](#check-the-substance)
      * [Make a dedicated meeting to review the PR](#make-a-dedicated-meeting-to-review-the-pr)
  * [What happen to the issue(s)?](#what-happen-to-the-issues?)
  * [Merge conflict](#merge-conflict)
  * [When and who can merge PR](#when-and-who-can-merge-pr)
    * [Merge type](#merge-type)
  * [Resolve conversation](#resolve-conversation)
* [Responsibility](#responsibility)

<!--- END -->

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

##### Base branch

The `develop` branch is generally the base branch for every PRs.

Exceptions can occur:

- if a feature implementation is split into multiple PRs. We can have a chain of PRs in this case. PR can be merged one by one on develop, and GitHub change the target branch to `develop` for the next PR automatically.
- we want to merge a PR from the community, but there is still work to do, and the PR is not updated by the submitter. First, we can kindly ask the submitter if they will update their PR, by commenting it. If there is no answer after a few days (including a week-end), we can create a new branch, push it, and change the target branch of the PR to this new branch. The PR can then be merged, and we can add more commits to fix the issues. After that a new PR can be created with `develop` as a target branch.

**Important notice 1:** Releases are created from the `develop` branch. So `develop` branch should always contain a "releasable" source code. So when a feature is being implemented with several PRs, it has to be disabled by default (using a feature flag for instance), until the feature is fully implemented. A last PR to enable the feature can then be created.

**Important notice 2:** Database migration: some developers and some people from the community are using the nightly build from `develop`. Multiple database migrations should be properly handled for them. This is OK to have multiple migrations between 2 releases, this is not OK to add steps to the pending database migration on `develop`. So for instance `develop` users will migrate from version 11 to version 12, then 13, then 14, and `main` users will do all those steps after they get the app upgrade.

##### PR Review Assignment

We use automatic assignment for PR reviews. **A PR is automatically routed by GitHub to one team member** using the round robin algorithm. Additional reviewers can be used for complex changes or when the first reviewer is not confident enough on the changes.
The process is the following:

- The PR creator selects the [element-android-reviewers](https://github.com/orgs/element-hq/teams/element-android-reviewers) team as a reviewer.
- GitHub automatically assign the reviewer. If the reviewer is not available (holiday, etc.), remove them and set again the team, GitHub will select another reviewer.
- Alternatively, the PR creator can directly assign specific people if they have another Android developer in their team or they think a specific reviewer should take a look at their PR.
- Reviewers get a notification to make the review: they review the code following the good practice (see the rest of this document).
- After making their own review, if they feel not confident enough, they can ask another person for a full review, or they can tag someone within a PR comment to check specific lines.

For PRs coming from the community, the issue wrangler can assign either the team [element-android-reviewers](https://github.com/orgs/element-hq/teams/element-android-reviewers) or any member directly.

##### PR review time

As a PR submitter, you deserve a quick review. As a reviewer, you should do your best to unblock others.

Some tips to achieve it:

- Set up your GH notifications correctly
- Check your pulls page: [https://github.com/pulls](https://github.com/pulls)
- Check your pending assigned PRs before starting or resuming your day to day tasks
- If you are busy with high priority tasks, inform the author. They will find another developer

It is hard to define a deadline for a review. It depends on the PR size and the complexity. Let's start with a goal of 24h (working day!) for a PR smaller than 500 lines. If bigger, the submitter and the reviewer should discuss.

After this time, the submitter can ping the reviewer to get a status of the review.

##### Re-request PR review

Once all the remarks have been handled, it's possible to re-request a review from the (same) reviewer to let them know that the PR has been updated the PR is ready to be reviewed again. Use the double arrow next to the reviewer name to do that.

##### When create split PR?

To implement big new feature, it may be efficient to split the work into several smaller and scoped PRs. They will be easier to review, and they can be merged on `develop` faster.

Big PR can take time, and there is a risk of future merge conflict.

Feature flag can be used to avoid half implemented feature to be available in the application.

That said, splitting into several PRs should not have the side effect to have more review to do, for instance if some code is added, then finally removed.

##### Avoid fixing other unrelated issue in a big PR

Each PR should focus on a single task. If other issues may be fixed when working in the area of it, it's preferable to open a dedicated PR.

It will have the advantage to be reviewed and merged faster, and not interfere with the main PR.

It's also applicable for code rework (such as renaming for instance), or code formatting. Sometimes, it is more efficient to extract that work to a dedicated PR, and rebase your branch once this "rework" PR has been merged.

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
More info can be found in the file [analytics.md](./analytics.md)

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
7. All PRs from external contributors **MUST** include a sign-off. It's in the checklist, and sometimes it's checked by the submitter, but there is actually no sign-off. In this case, ask nicely for a sign-off and request changes (do not approve the PR, even if everything else is fine).

### Rules

#### Check the form

##### PR title

PR title should describe in one line what's brought by the PR. Reviewer can edit the title if it's not clear enough, or to add suffix like `[BLOCKED]` or similar. Fixing typo is also a good practice, since GitHub search is quite not efficient, so the words must be spelled without any issue. Adding suffix will help when viewing the PR list.

It's free form, but prefix tags could also be used to help understand what's in the PR.

Examples of prefixes:
- `[Refacto]`
- `[Feature]`
- `[Bugfix]`
- etc.

Also, it's still possible to add labels to the PRs, such as `A-` or `T-` labels, even if this is not a strong requirement. We prefer to spend time to add labels on issues.

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

PR from bots will always be merged by the reviewer, right after approving the changes, or in case of critical changes, right after a release.

#### Merge type

Generally we use "Create a merge commit", which has the advantage to keep the branch visible.

If git history is noisy (code added, then removed, etc.), it's possible to use "Squash and merge". But the branch will not be visible anymore, a commit will be added on top of develop. Git commit message can (and probably must) be edited from the GitHub web app. It's better if the submitter do the work to cleanup the git history by using a git interactive rebase of their branch.

### Resolve conversation

Generally we do not close conversation added during PR review and update by clicking on "Resolve conversation"
If the submitter or the reviewer do so, it will more difficult for further readers to see again the content. They will have to open the conversation to see it again. it's a waste of time.

When remarks are handled, a small comment like "done" is enough, commit hash can also be added to the conversation.

Exception: for big PRs with lots of conversations, using "Resolve conversation" may help to see the remaining remarks.

Also "Resolve conversation" should probably be hit by the creator of the conversation.

## Responsibility

PR submitter is responsible of the incoming change. PR reviewers who approved the PR take a part of responsibility on the code which will land to develop, and then be used by our users, and the user of our forks.

That said, bug may still be merged on `develop`, this is still acceptable of course. In this case, please make sure an issue is created and correctly labelled. Ideally, such issues should be fixed before the next release candidate, i.e. with a higher priority. But as we release the application every 10 working days, it can be hard to fix every bugs. That's why PR should be fully tested and reviewed before being merge and we should never comment code review remark with "will be handled later", or similar comments.
