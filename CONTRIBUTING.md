# Contributing code to Matrix

Please read https://github.com/matrix-org/synapse/blob/master/CONTRIBUTING.rst

Android support can be found in this [![Riot Android Matrix room #riot-android:matrix.org](https://img.shields.io/matrix/riot-android:matrix.org.svg?label=%23riot-android:matrix.org&logo=matrix&server_fqdn=matrix.org)](https://matrix.to/#/#riot-android:matrix.org) room.

Dedicated room for RiotX: [![RiotX Android Matrix room #riot-android:matrix.org](https://img.shields.io/matrix/riotx:matrix.org.svg?label=%23RiotX:matrix.org&logo=matrix&server_fqdn=matrix.org)](https://matrix.to/#/#riotx:matrix.org)

# Specific rules for Matrix Android projects

## Android Studio settings

Please set the "hard wrap" setting of Android Studio to 160 chars, this is the setting we use internally to format the source code (Menu `Settings/Editor/Code Style` then `Hard wrap at`).

## Compilation

For now, the Matrix SDK and the RiotX application are in the same project. So there is no specific thing to do, this project should compile without any special action.

## I want to help translating RiotX

If you want to fix an issue with an English string, please submit a PR.
If you want to fix an issue in other languages, or add a missing translation, or even add a new language, please use [Weblate](https://translate.riot.im/projects/riot-android/).

For the moment, Strings from Riot will be used, there is no dedicated project in Weblate for RiotX.

## I want to submit a PR to fix an issue

Please check if a corresponding issue exists. If yes, please let us know in a comment that you're working on it.
If an issue does not exist yet, it may be relevant to open a new issue and let us know that you're implementing it.

### Kotlin

This project is full Kotlin. Please do not write Java classes.

### CHANGES.md

Please add a line to the top of the file `CHANGES.md` describing your change.

### Code quality

Make sure the following commands execute without any error:

> ./tools/check/check_code_quality.sh

> ./gradlew lintAppgplayRelease

### Unit tests

Make sure the following commands execute without any error:

> ./gradlew testAppgplayReleaseUnitTest

### Tests

RiotX is currently supported on Android Jelly Bean (API 16+): please test your change on an Android device (or Android emulator) running with API 16. Many issues can happen (including crashes) on older devices.
Also, if possible, please test your change on a real device. Testing on Android emulator may not be sufficient.

### Internationalisation

When adding new string resources, please only add new entries in file `value/strings.xml`. Translations will be added later by the community of translators with a specific tool named [Weblate](https://translate.riot.im/projects/riot-android/).
Do not hesitate to use plurals when appropriate.

### Layout

When adding or editing layouts, make sure the layout will render correctly if device uses a RTL (Right To Left) language.
You can check this in the layout editor preview by selecting any RTL language (ex: Arabic).

Also please check that the colors are ok for all the current themes of RiotX. Please use `?attr` instead of `@color` to reference colors in the layout. You can check this in the layout editor preview by selecting all the main themes (`AppTheme.Status`, `AppTheme.Dark`, etc.).

### Authors

Feel free to add an entry in file AUTHORS.md

## Thanks

Thanks for contributing to Matrix projects!
