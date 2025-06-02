## Overview

Element Android [now](https://github.com/element-hq/element-android/pull/8656) only supports the [rust crypto SDK](https://github.com/matrix-org/matrix-rust-sdk/tree/main/crates/matrix-sdk-crypto).

## Testing with a local rust aar

In order to run a custom rust SDK branch you can follow the directions in the
[bindings repository](https://github.com/matrix-org/matrix-rust-components-kotlin?tab=readme-ov-file#testing-locally)
in order to build the AAR for the crypto crate.

Install the resulting file as `./library/rustCrypto/matrix-rust-sdk-crypto.aar`. For example:

```sh
ln -s <path_to_matrix-rust-components-kotlin>/crypto/crypto-android/build/outputs/aar/crypto-android-debug.aar ./library/rustCrypto/matrix-rust-sdk-crypto.aar
```

Then go to `matrix-sdk-android/build.gradle` and toggle the comments between the following lines.

````
 rustCryptoImplementation("org.matrix.rustcomponents:crypto-android:0.3.1")
 // rustCryptoApi project(":library:rustCrypto")
````

## Database migration from kotlin to rust

Crypto information is now persisted in a SQLite database.

The migration from the old Realm database is handled when injecting `@SessionRustFilesDirectory` in the olmMachine. 
When launching the first time after migration, the app will detect that there is no rust data repository and it will
create one. If there is an existing realm database, the data will then migrated to rust. See `ExtractMigrationDataUseCase`.
This will extract your device keys, account secrets, active olm and megolm sessions.

There is no inverse migration. If you migrate to a version of the app that uses
the Rust library, and want to revert to a Kotlin-crypto version, you will have
to logout then login again.

