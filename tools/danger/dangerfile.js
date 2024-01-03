const {danger, warn} = require('danger')

/**
 * Note: if you update the checks in this file, please also update the file ./docs/danger.md
 */

// Useful to see what we got in danger object
// warn(JSON.stringify(danger))

const pr = danger.github.pr
const github = danger.github
// User who has created the PR.
const user = pr.user.login
const modified = danger.git.modified_files
const created = danger.git.created_files
const editedFiles = [...modified, ...created]

// Check that the PR has a description
if (pr.body.length == 0) {
    warn("Please provide a description for this PR.")
}

// Warn when there is a big PR
if (editedFiles.length > 50) {
    message("This pull request seems relatively large. Please consider splitting it into multiple smaller ones.")
}

// Request a changelog for each PR
const changelogAllowList = [
    "dependabot[bot]",
]

const requiresChangelog = !changelogAllowList.includes(user)

if (requiresChangelog) {
    const changelogFiles = editedFiles.filter(file => file.startsWith("changelog.d/"))

    if (changelogFiles.length == 0) {
        warn("Please add a changelog. See instructions [here](https://github.com/element-hq/element-android/blob/develop/CONTRIBUTING.md#changelog)")
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
            fail("Invalid extension for changelog. See instructions [here](https://github.com/element-hq/element-android/blob/develop/CONTRIBUTING.md#changelog)")
        }
    }
}

// check that frozen classes have not been modified
const frozenClasses = [
    "OlmInboundGroupSessionWrapper.kt",
    "OlmInboundGroupSessionWrapper2.kt",
]

frozenClasses.forEach(frozen => {
    if (editedFiles.some(file => file.endsWith(frozen))) {
        fail("Frozen class `" + frozen + "` has been modified. Please do not modify frozen class.")
    }
  }
)

// Check for a sign-off
const signOff = "Signed-off-by:"

// Please add new names following the alphabetical order.
const allowList = [
    "amitkma",
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
    "jonnyandrew",
    "kittykat",
    "langleyd",
    "MadLittleMods",
    "manuroe",
    "mnaturel",
    "onurays",
    "ouchadam",
    "stefanceriu",
    "yostyle",
]

const requiresSignOff = !allowList.includes(user)

if (requiresSignOff) {
    const hasPRBodySignOff = pr.body.includes(signOff)
    const hasCommitSignOff = danger.git.commits.every(commit => commit.message.includes(signOff))
    if (!hasPRBodySignOff && !hasCommitSignOff) {
        fail("Please add a sign-off to either the PR description or to the commits themselves. See instructions [here](https://matrix-org.github.io/synapse/latest/development/contributing_guide.html#sign-off).")
    }
}

// Check for screenshots on view changes
const hasChangedViews = editedFiles.filter(file => file.includes("/layout")).length > 0
if (hasChangedViews) {
    if (!pr.body.includes("user-images")) {
        warn("You seem to have made changes to views. Please consider adding screenshots.")
    }
}

// Check for pngs on resources
const hasPngs = editedFiles.filter(file => file.toLowerCase().endsWith(".png")).length > 0
if (hasPngs) {
    warn("You seem to have made changes to some images. Please consider using an vector drawable.")
}

// Check for reviewers
if (github.requested_reviewers.users.length == 0 && !pr.draft) {
    warn("Please add a reviewer to your PR.")
}

// Check that translations have not been modified by developers
if (user != "RiotTranslateBot") {
   if (editedFiles.some(file => file.endsWith("strings.xml") && !file.endsWith("values/strings.xml"))) {
       fail("Some translation files have been edited. Only user `RiotTranslateBot` (i.e. translations coming from Weblate) is allowed to do that.\nPlease read more about translations management [in the doc](https://github.com/element-hq/element-android/blob/develop/CONTRIBUTING.md#internationalisation).")
   }
}
