# Analytics in Element

## Solution

Element is using PostHog to send analytics event.
We ask for the user to give consent before sending any analytics data.

## How to add a new Event

The analytics plan is shared between all Element clients. To add an Event, please open a PR to this project: https://github.com/matrix-org/matrix-analytics-events

Then, once the PR has been merged, you can run the tool `import_analytic_plan.sh` to import the plan to Element, and then you can use the new Event. Note that this tool is run by Github action once a week.

## Forks of Element

Analytics on forks are disabled by default. Please refer to AnalyticsConfig and there implementation to setup analytics on your project.
