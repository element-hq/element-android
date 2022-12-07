<!--- TOC -->

* [Testing database migration](#testing-database-migration)
  * [Creating a reference database](#creating-a-reference-database)
  * [Testing](#testing)

<!--- END -->

## Testing database migration

### Creating a reference database

Databases are encrypted, the key to decrypt is needed to setup the test.
A special build property must be enabled to extract it. 

Set `vector.debugPrivateData=true` in `~/.gradle/gradle.properties` (to avoid committing by mistake)

Launch the app in your emulator, login and use the app to fill up the database.

Save the key for the tested database
```
RealmKeysUtils  W  Database key for alias `session_db_fe9f212a611ccf6dea1141777065ed0a`: 935a6dfa0b0fc5cce1414194ed190....
RealmKeysUtils  W  Database key for alias `crypto_module_fe9f212a611ccf6dea1141777065ed0a`: 7b9a21a8a311e85d75b069a343.....
```


Use the [Device File Explorer](https://developer.android.com/studio/debug/device-file-explorer) to extrat the database file from the emulator.

Go to `data/data/im.vector.app.debug/files/<hash>/`
Pick the database you want to test (name can be found in SessionRealmConfigurationFactory):
 - crypto_store.realm for crypto
 - disk_store.realm for session
 - etc... 

Download the file on your disk

### Testing

Copy the file in `src/AndroidTest/assets`

see `CryptoSanityMigrationTest` or `RealmSessionStoreMigration43Test` for sample tests.

There are already some databases in the assets folder.
The existing test will properly detect schema changes, and fail with such errors if a migration is missing:

```
io.realm.exceptions.RealmMigrationNeededException: Migration is required due to the following errors:
- Property 'CryptoMetadataEntity.foo' has been added.
```

If you want to test properly more complex database migration (dynamic transforms) ensure that the database contains
the entity you want to migrate.

You can explore the database with [realm studio](https://www.mongodb.com/docs/realm/studio/) if needed.

