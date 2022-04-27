# Contributing code to Matrix

Please read https://github.com/matrix-org/synapse/blob/master/CONTRIBUTING.md

Element Android support can be found in this room: [![Element Android Matrix room #element-android:matrix.org](https://img.shields.io/matrix/element-android:matrix.org.svg?label=%23element-android:matrix.org&logo=matrix&server_fqdn=matrix.org)](https://matrix.to/#/#element-android:matrix.org).

# Specific rules for Matrix Android projects

## Android Studio settings

Please set the "hard wrap" setting of Android Studio to 160 chars, this is the setting we use internally to format the source code (Menu `Settings/Editor/Code Style` then `Hard wrap at`).
Please ensure that you're using the project formatting rules (which are in the project at .idea/codeStyles/), and format the file before committing them.

### Template

An Android Studio template has been added to the project to help creating all files needed when adding a new screen to the application. Fragment, ViewModel, Activity, etc.

To install the template (to be done only once):
- Go to folder `./tools/template`.
- Mac OSX: Run the script `./configure.sh`.

   Linux: Run `ANDROID_STUDIO=/path/to/android-studio ./configure`
    - e.g. `ANDROID_STUDIO=/usr/local/android-studio ./configure`

- Restart Android Studio.

To create a new screen:
- First create a new package in your code.
- Then right click on the package, and select `New/New Vector/Element Feature`.
- Follow the Wizard, especially replace `Main` by something more relevant to your feature.
- Click on `Finish`.
- Remaining steps are described as TODO in the generated files, or will be pointed out by the compiler, or at runtime :)

Note that if the templates are modified, the only things to do is to restart Android Studio for the change to take effect.

## Compilation

For now, the Matrix SDK and the Element application are in the same project. So there is no specific thing to do, this project should compile without any special action.

## I want to help translating Element

If you want to fix an issue with an English string, please submit a PR.
If you want to fix an issue in other languages, or add a missing translation, or even add a new language, please use [Weblate](https://translate.element.io/projects/element-android/).

## I want to submit a PR to fix an issue

Please have a look in the [dedicated documentation](./docs/pull_request.md) about pull request.

Please check if a corresponding issue exists. If yes, please let us know in a comment that you're working on it.
If an issue does not exist yet, it may be relevant to open a new issue and let us know that you're implementing it.

### Kotlin

This project is full Kotlin. Please do not write Java classes.

### Changelog

Please create at least one file under ./changelog.d containing details about your change. Towncrier will be used when preparing the release.

Towncrier says to use the PR number for the filename, but the issue number is also fine.

Supported filename extensions are:

- ``.feature``: Signifying a new feature in Element Android or in the Matrix SDK.
- ``.bugfix``: Signifying a bug fix.
- ``.wip``: Signifying a work in progress change, typically a component of a larger feature which will be enabled once all tasks are complete.
- ``.doc``: Signifying a documentation improvement.
- ``.sdk``: Signifying a change to the Matrix SDK, this could be an addition, deprecation or removal of a public API.
- ``.misc``: Any other changes.

See https://github.com/twisted/towncrier#news-fragments if you need more details.

### Code quality

Make sure the following commands execute without any error:

#### Internal tool

<pre>
./tools/check/check_code_quality.sh
</pre>

#### ktlint

<pre>
./gradlew ktlintCheck --continue
</pre>

Note that you can run

<pre>
./gradlew ktlintFormat
</pre>

For ktlint to fix some detected errors for you (you still have to check and commit the fix of course)

#### lint

<pre>
./gradlew lintGplayRelease
./gradlew lintFdroidRelease
</pre>

### Unit tests

Make sure the following commands execute without any error:

<pre>
./gradlew testGplayReleaseUnitTest
</pre>

### Tests

Element is currently supported on Android Lollipop (API 21+): please test your change on an Android device (or Android emulator) running with API 21. Many issues can happen (including crashes) on older devices.
Also, if possible, please test your change on a real device. Testing on Android emulator may not be sufficient.

You should consider adding Unit tests with your PR, and also integration tests (AndroidTest). Please refer to [this document](./docs/integration_tests.md) to install and run the integration test environment.

### Internationalisation

Translations are handled using an external tool: [Weblate](https://translate.element.io/projects/element-android/)

As a general rule, please never edit or add or remove translations to the project in a Pull Request. It can lead to merge conflict if the translations are also modified in Weblate side.

#### Adding new string

When adding new string resources, please only add new entries in file `value/strings.xml`. Translations will be added later by the community of translators using Weblate.

New strings can be added anywhere in the file `value/strings.xml`, not necessarily at the end of the file. Generally, it's even better to add the new strings in some dedicated section per feature, and not at the end of the file, to avoid merge conflict between 2 PR adding strings at the end of the same file.

Do not hesitate to use plurals when appropriate.

#### Editing existing strings

Two cases:
- If the meaning stays the same, it's OK to edit the original string (i.e. the English version).
- If the meaning is not the same, please create a new string and do not remove the existing string. See below for instructions to remove existing string.

#### Removing existing strings

If a string is not used anymore, it should be removed from the resource, but please do not remove the strings or its translations in the PR. It can lead to merge conflict with Weblate, and to lint error if new translations from deleted strings are added with Weblate.

Instead, please comment the original string with:
```xml
<!-- TODO TO BE REMOVED -->
```
And add `tools:ignore="UnusedResources"` to the string, to let lint ignore that the string is not used.

The string will be removed during the next sync with Weblate.

### Accessibility

Please consider accessibility as an important point. As a minimum requirement, in layout XML files please use attributes such as `android:contentDescription` and `android:importantForAccessibility`, and test with a screen reader if it's working well. You can add new string resources, dedicated to accessibility, in this case, please prefix theirs id with `a11y_`.

For instance, when updating the image `src` of an ImageView, please also consider updating its `contentDescription`. A good example is a play pause button.

### Layout

When adding or editing layouts, make sure the layout will render correctly if device uses a RTL (Right To Left) language.
You can check this in the layout editor preview by selecting any RTL language (ex: Arabic).

Also please check that the colors are ok for all the current themes of Element. Please use `?attr` instead of `@color` to reference colors in the layout. You can check this in the layout editor preview by selecting all the main themes (`AppTheme.Status`, `AppTheme.Dark`, etc.).

### Authors

Feel free to add an entry in file AUTHORS.md

## Thanks

Thanks for contributing to Matrix projects!
