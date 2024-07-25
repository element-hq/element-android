# Screenshot testing

<!--- TOC -->

* [Overview](#overview)
* [Setup](#setup)
* [Recording](#recording)
* [Verifying](#verifying)
* [Contributing](#contributing)
* [Example](#example)

<!--- END -->

## Overview

- Screenshot tests are tests which record the content of a rendered screen and verify subsequent runs to check if the screen renders differently.
- Element uses [Paparazzi](https://github.com/cashapp/paparazzi) to render, record and verify android layouts. 
- The screenshot verification occurs on every pull request as part of the `tests.yml` workflow.

## Setup

- Install Git LFS through your package manager of choice (`brew install git-lfs` | `yay -S git-lfs`).
- Install the Git LFS hooks into the project.

```bash
# with element-android as the current working directory
git lfs install --local
```

- If installed correctly, `git push` and `git pull` will now include LFS content.

## Recording

- `./gradlew recordScreenshots`
- Paparazzi will generate images in `${module}/src/test/snapshots`, which will need to be committed to the repository using Git LFS.

## Verifying

- `./gradlew verifyScreenshots`
- In the case of failure, Paparazzi will generate images in `${module}/out/failure`. The images will show the expected and actual screenshots along with a delta of the two images.

## Contributing

- When creating a test, the file (and class) name names must include `ScreenshotTest`, eg `ItemScreenshotTest`.
- After creating the new test, record and commit the newly rendered screens.
- `./tools/validate_lfs` can be ran to ensure everything is working correctly with Git LFS, the CI also runs this check.

## Example

```kotlin
class PaparazziExampleScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
            deviceConfig = PIXEL_3,
            theme = "Theme.Vector.Light",
    )

    @Test
    fun `example paparazzi test`() {
        // Inflate the layout
        val view = paparazzi.inflate<ConstraintLayout>(R.layout.item_radio)

        // Bind data to the view
        view.findViewById<TextView>(R.id.actionTitle).text = paparazzi.resources.getString(CommonStrings.room_settings_all_messages)
        view.findViewById<ImageView>(R.id.radioIcon).setImageResource(R.drawable.ic_radio_on)

        // Record the bound view
        paparazzi.snapshot(view)
    }
}
```
