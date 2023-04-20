## Overview

Until the final migration to [rust crypto sdk](https://github.com/matrix-org/matrix-rust-components-kotlin), the Element Android project will support two
different SDK as a product flavor.

The `matrix-sdk-android` module is defining a new flavor dimension `crypto`, with two flavors `kotlinCrypto` and `rustCrypto`.
The crypto module cannot be changed at runtime, it's a build time configuration. The app supports migration from kotlinCrypto to rustCrypto but not the other
way around.

The code that is not shared between the flavors is located in dedicated source sets (`src/kotlinCrypto/`, `src/rustCrypto/`). Some tests are also extracted
in different source sets because they were accessing internal API and won't work with the rust crypto sdk.

## Noticeable changes

As a general rule, if you stick to the `kotlinCrypto` the app should behave as it was before the integration of favours.
There is a noticeable exception though:
In order to integrate the rust crypto several APIs had to be migrated from callback code to suspendable code. This change
impacted a lot the key verification engine (user and device verification), so this part has been refactored for `kotlinCrypto`. The UI is also impacted,
the verification flows now match the web experience.

TLDR; Verification UI and engine has been refactored.

## Testing with a local rust aar

In order to run a custom rust SDK branch you can follow the direction in the [bindings repository](https://github.com/matrix-org/matrix-rust-components-kotlin) 
in order to build the `matrix-rust-sdk-crypto.aar`.

Copy this lib in `library/rustCrypto/`, and rename it `matrix-rust-sdk-crypto.aar`.

Then go to `matrix-sdk-android/build.gradle` and toggle the comments between the following lines.

````
 rustCryptoImplementation("org.matrix.rustcomponents:crypto-android:0.3.1")
 // rustCryptoApi project(":library:rustCrypto")
````

## Changes in CI

The workflow files have been updated to use the `kotlinCrypto` flavor, e.g

`assembleGplayNightly` => `assembleGplayKotlinCryptoNightly`

So building the unsigned release kotlin crypto apk is now:

`> ./gradlew assembleGplayKotlinCryptoRelease`

An additional workflow has been added to build the `rustCrypto` flavor (elementr.yml, ` Build debug APKs ER`).


## Database migration from kotlin to rust

With the kotlin flavor, the crypto information are persisted in the crypto realm database.
With the rust flavor, the crypto information are in a sqllite database.

The migration is handled when injecting `@SessionRustFilesDirectory` in the olmMachine. 
When launching the first time after migration, the app will detect that there is no rust data repository and it will
create one. If there is an existing realm database, the data will then migrated to rust. See `ExtractMigrationDataUseCase`.
This will extract your device keys, account secrets, active olm and megolm sessions.

There is no inverse migration for now, as there is not yet rust pickle to olm pickle support in the sdk.

If you migrate your app to rust, and want to revert to kotlin you have to logout then login again.

