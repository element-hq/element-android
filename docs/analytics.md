# Analytics in Element

<!--- TOC -->

* [Solution](#solution)
* [How to add a new Event](#how-to-add-a-new-event)
* [Forks of Element](#forks-of-element)

<!--- END -->

## Solution

Element is using PostHog to send analytics event.
We ask for the user to give consent before sending any analytics data.

## How to add a new Event

The analytics plan is shared between all Element clients. To add an Event, please open a PR to this project: https://github.com/matrix-org/matrix-analytics-events

Then, once the PR has been merged, and the library is release, you can update the version of the library in the `build.gradle` file.

## Forks of Element

Analytics on forks are disabled by default. Please refer to AnalyticsConfig and there implementation to setup analytics on your project.
