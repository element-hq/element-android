## Danger

<!--- TOC -->

* [What does danger checks](#what-does-danger-checks)
  * [PR check](#pr-check)
  * [Quality check](#quality-check)
* [Setup](#setup)
* [Run danger locally](#run-danger-locally)
* [Danger user](#danger-user)
* [Useful links](#useful-links)

<!--- END -->

## What does danger checks

### PR check

See the [dangerfile](../tools/danger/dangerfile.js). If you add rules in the dangerfile, please update the list below!

Here are the checks that Danger does so far:

- PR description is not empty
- Big PR got a warning to recommend to split
- PR contains a file for towncrier and extension is checked
- PR does not modify frozen classes
- PR contains a Sign-Off, with exception for Element employee contributors
- PR with change on layout should include screenshot in the description
- PR which adds png file warn about the usage of vector drawables
- non draft PR should have a reviewer
- files containing translations are not modified by developers

### Quality check

After all the checks that generate checkstyle XML report, such as Ktlint, lint, or Detekt, Danger is run with this [dangerfile](../tools/danger/dangerfile-lint.js), in order to post comments to the PR with the detected error and warnings.

To run locally, you will have to install the plugin `danger-plugin-lint-report` using:

```shell
yarn add danger-plugin-lint-report --dev
```

## Setup

This operation should not be necessary, since Danger is already setup for the project.

To setup danger to the project, run:

```shell
bundle exec danger init
```

## Run danger locally

When modifying the [dangerfile](../tools/danger/dangerfile.js), you can check it by running Danger locally.

To run danger locally, install it and run:

```shell
bundle exec danger pr <PR_URL> --dangerfile=./tools/danger/dangerfile.js
```

For instance:

```shell
bundle exec danger pr https://github.com/element-hq/element-android/pull/6637 --dangerfile=./tools/danger/dangerfile.js
```

We may need to create a GitHub token to have less API rate limiting, and then set the env var:

```shell
export DANGER_GITHUB_API_TOKEN='YOUR_TOKEN'
```

Swift and Kotlin (just in case)

```shell
bundle exec danger-swift pr <PR_URL> --dangerfile=./tools/danger/dangerfile.js
bundle exec danger-kotlin pr <PR_URL> --dangerfile=./tools/danger/dangerfile.js
```

## Danger user

To let Danger check all the PRs, including PRs form forks, a GitHub account have been created:
- login: ElementBot
- password: Stored on Passbolt
- GitHub token: A token with limited access has been created and added to the repository https://github.com/element-hq/element-android as secret DANGER_GITHUB_API_TOKEN. This token is not saved anywhere else. In case of problem, just delete it and create a new one, then update the secret.

PRs from forks do not always have access to the secret `secrets.DANGER_GITHUB_API_TOKEN`, so `secrets.GITHUB_TOKEN` is also provided to the job environment. If `secrets.DANGER_GITHUB_API_TOKEN` is available, it will be used, so user `ElementBot` will comment the PR. Else `secrets.GITHUB_TOKEN` will be used, and bot `github-actions` will comment the PR.

## Useful links

- https://danger.systems/
- https://danger.systems/js/
- https://danger.systems/js/guides/getting_started.html
- https://danger.systems/js/reference.html
- https://github.com/danger/awesome-danger

Some danger files to get inspired from

- https://github.com/artsy/emission/blob/master/dangerfile.ts
- https://github.com/facebook/react-native/blob/master/bots/dangerfile.js
- https://github.com/apollographql/apollo-client/blob/master/config/dangerfile.ts
- https://github.com/styleguidist/react-styleguidist/blob/master/dangerfile.js
- https://github.com/storybooks/storybook/blob/master/dangerfile.js
- https://github.com/ReactiveX/rxjs/blob/master/dangerfile.js
