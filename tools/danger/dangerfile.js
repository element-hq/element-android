const {danger, warn} = require('danger')

/**
 * Note: if you update the checks in this file, please also update the file ./docs/danger.md
 */

// Useful to see what we got in danger object
// warn(JSON.stringify(danger))

const pr = danger.github.pr
// User who has created the PR.
const user = pr.user.login
const modified = danger.git.modified_files
const created = danger.git.created_files
let editedFiles = [...modified, ...created]

// Check that the PR has a description
if (pr.body.length == 0) {
    warn("Please provide a description for this PR.")
}

// Warn when there is a big PR
if (editedFiles.length > 50) {
    warn("This pull request seems relatively large. Please consider splitting it into multiple smaller ones.")
}

// Request a changelog for each PR
const changelogAllowList = [
    "dependabot[bot]",
]

let requiresChangelog = !changelogAllowList.includes(user)

if (requiresChangelog) {
    let changelogFiles = editedFiles.filter(file => file.startsWith("changelog.d/"))

    if (changelogFiles.length == 0) {
        warn("Please add a changelog. See instructions [here](https://github.com/vector-im/element-android/blob/develop/CONTRIBUTING.md#changelog)")
    } else {
        const validTowncrierExtensions = [
            "bugfix",
            "doc",
            "feature",
            "misc",
            "sdk",
            "wip",
        ]
        if (!changelogFiles.every(file => validTowncrierExtensions.includes(file.split(".").pop()))) {
            fail("Invalid extension for changelog. See instructions [here](https://github.com/vector-im/element-android/blob/develop/CONTRIBUTING.md#changelog)")
        }
    }
}

// Check for a sign-off
const signOff = "Signed-off-by:"

// Please add new names following the alphabetical order.
const allowList = [
    "aringenbach",
    "BillCarsonFr",
    "bmarty",
    "Claire1817",
    "dependabot[bot]",
    "ericdecanini",
    "fedrunov",
    "Florian14",
    "ganfra",
    "jmartinesp",
    "langleyd",
    "MadLittleMods",
    "manuroe",
    "mnaturel",
    "onurays",
    "ouchadam",
    "stefanceriu",
    "yostyle",
]

let requiresSignOff = !allowList.includes(user)

if (requiresSignOff) {
    let hasPRBodySignOff = pr.body.includes(signOff)
    let hasCommitSignOff = danger.git.commits.every(commit => commit.message.includes(signOff))
    if (!hasPRBodySignOff && !hasCommitSignOff) {
        fail("Please add a sign-off to either the PR description or to the commits themselves.")
    }
}

// Check for screenshots on view changes
let hasChangedViews = editedFiles.filter(file => file.includes("/layout")).length > 0
if (hasChangedViews) {
    if (!pr.body.includes("user-images")) {
        warn("You seem to have made changes to views. Please consider adding screenshots.")
    }
}

// Check for pngs on resources
let hasPngs = editedFiles.filter(file => file.toLowerCase().endsWith(".png")).length > 0
if (hasPngs) {
    warn("You seem to have made changes to some images. Please consider using an vector drawable.")
}

// Check for reviewers
if (pr.requested_reviewers.length == 0 && !pr.draft) {
    fail("Please add a reviewer to your PR.")
}
