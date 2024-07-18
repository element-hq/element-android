# Automate user interface tests

Element Android ensures that some fundamental flows are properly working by running automated user interface tests.
Ui tests are using the android [Espresso](https://developer.android.com/training/testing/espresso) library.

Tests can be run on a real device, or on a virtual device (such as the emulator in Android Studio).

Currently the test are covering a small set of application flows:
	- Registration
	- Self verification via emoji
	- Self verification via passphrase

<!--- TOC -->

* [Prerequisites:](#prerequisites:)
* [Run the tests](#run-the-tests)
  * [From the source code](#from-the-source-code)
  * [From command line](#from-command-line)
* [Recipes](#recipes)
  * [Wait for initial sync](#wait-for-initial-sync)
  * [Accessing current activity](#accessing-current-activity)
  * [Interact with other session](#interact-with-other-session)
  * [Contributing to the UiAllScreensSanityTest](#contributing-to-the-uiallscreenssanitytest)

<!--- END -->

## Prerequisites:

Out of the box, the tests use one of the homeservers (located at http://localhost:8080) of the "Demo Federation of Homeservers" (https://github.com/matrix-org/synapse#running-a-demo-federation-of-synapses).

You first need to follow instructions to set up Synapse in development mode at https://github.com/matrix-org/synapse#synapse-development. If you have already installed all dependencies, the steps are:

```shell script
$ git clone https://github.com/matrix-org/synapse.git
$ cd synapse
$ virtualenv -p python3 env
$ source env/bin/activate
(env) $ python -m pip install --no-use-pep517 -e .
```

Every time you want to launch these test homeservers, type:

```shell script
$ source env/bin/activate
(env) $ demo/start.sh --no-rate-limit
```

**Emulator/Device set up**

When running the test via android studio on a device, you have to disable system animations in order for the test to work properly.

First, ensure developer mode is enabled:

- To enable developer options, tap the **Build Number** option 7 times. You can find this option in one of the following locations, depending on your Android version:

	-   Android 9 (API level 28) and higher: **Settings > About Phone > Build Number**
	-   Android 8.0.0 (API level 26) and Android 8.1.0 (API level 26): **Settings > System > About Phone > Build Number**
	-   Android 7.1 (API level 25) and lower: **Settings > About Phone > Build Number**

On your device, under **Settings > Developer options**, disable the following 3 settings:

-   Window animation scale
-   Transition animation scale
-   Animator duration scale

## Run the tests

Once Synapse is running, and an emulator is running, you can run the UI tests.

### From the source code

Click on the green arrow in front of each test. Clicking on the arrow in front of the test class, or from the package directory does not always work (Tests not found issue).

### From command line

````shell script
./gradlew vector:connectedGplayDebugAndroidTest
````

To run all the tests from the `vector` module.

In case of trouble, you can try to uninstall the previous installed test APK first with this command:

```shell script
adb uninstall im.vector.app.debug.test
```
## Recipes

We added some specific Espresso IdlingResources, and other utilities for matrix related tests

### Wait for initial sync

```kotlin
// Wait for initial sync and check room list is there
withIdlingResource(initialSyncIdlingResource(uiSession)) {
  onView(withId(R.id.roomListContainer))
            .check(matches(isDisplayed()))
}
```

### Accessing current activity

```kotlin
    val activity = EspressoHelper.getCurrentActivity()!!
    val uiSession = (activity as HomeActivity).activeSessionHolder.getActiveSession()
```

### Interact with other session

It's possible to create a session via the SDK, and then use this session to interact with the one that the emulator is using (to check verifications for example)

```kotlin
@Before
fun initAccount() {
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  val matrix = Matrix.getInstance(context)
  val userName = "foobar_${System.currentTimeMillis()}"
  existingSession = createAccountAndSync(matrix, userName, password, true)
}
```

### Contributing to the UiAllScreensSanityTest

The `UiAllScreensSanityTest` makes use of the Robot pattern in order to model pages, components and interactions.
Each Robot aims to return the UI back to its original state after the interaction, allowing for a reusable and consistent DSL.

```kotlin
// launches and closes settings after executing the block
elementRobot.settings {
    // whilst in the settings, launches and closes the advanced settings sub screen
    advancedSettings {
        // crawls all the pages within the advanced settings
        crawl()
    }
}

// enables developer mode by navigating to the settings, enabling the toggle and then returning to the starting point to execute the block
// on block completion the Robot disables developer mode by navigating back to the settings and finally returning to the original starting point
elementRobot.withDeveloperMode {
    // the same starting point as the example above
    settings {
        advancedSettings { crawlDeveloperOptions() }
    }
}
```

The Robots used in the example above...

```kotlin
class ElementRobot {
    fun settings(block: SettingsRobot.() -> Unit) {
        // double check we're where we think we are
        waitUntilViewVisible(withId(R.id.bottomNavigationView))

        // navigate to the settings
        openDrawer()
        clickOn(R.id.homeDrawerHeaderSettingsView)

        // execute the robot with the context of the settings screen
        block(SettingsRobot())

        // close the settings and ensure we're back at the starting point
        pressBack()
        waitUntilViewVisible(withId(R.id.bottomNavigationView))
    }

    fun withDeveloperMode(block: ElementRobot.() -> Unit) {
        settings { toggleDeveloperMode() }
        block()
        settings { toggleDeveloperMode() }
    }
}

class SettingsRobot {
    fun toggleDeveloperMode() {
        advancedSettings {
            toggleDeveloperMode()
        }
    }

    fun advancedSettings(block: SettingsAdvancedRobot.() -> Unit) {
        clickOn(CommonStrings.settings_advanced_settings)
        block(SettingsAdvancedRobot())
        pressBack()
    }
}

class SettingsAdvancedRobot {
    fun toggleDeveloperMode() {
        clickOn(CommonStrings.settings_developer_mode_summary)
    }
}
```
