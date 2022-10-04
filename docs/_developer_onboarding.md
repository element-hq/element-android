# Developer on boarding

<!--- TOC -->

* [Introduction](#introduction)
  * [Quick introduction to Matrix](#quick-introduction-to-matrix)
    * [Matrix data](#matrix-data)
      * [Room](#room)
      * [Event](#event)
    * [Sync](#sync)
      * [Glossary about syncs](#glossary-about-syncs)
  * [The Android project](#the-android-project)
  * [Matrix SDK](#matrix-sdk)
  * [Application](#application)
    * [MvRx](#mvrx)
      * [Behavior](#behavior)
    * [Epoxy](#epoxy)
    * [Other frameworks](#other-frameworks)
  * [Push](#push)
  * [Dependencies management](#dependencies-management)
  * [Test](#test)
  * [Other points](#other-points)
    * [Logging](#logging)
    * [Rageshake](#rageshake)
  * [Tips](#tips)
* [Happy coding!](#happy-coding)

<!--- END -->

## Introduction

This doc is a quick introduction about the project and its architecture.

It's aim is to help new developers to understand the overall project and where to start developing.

Other useful documentation:
- all the docs in this folder!
- the [contributing doc](../CONTRIBUTING.md), that you should also read carefully.

### Quick introduction to Matrix

Matrix website: [matrix.org](https://matrix.org), [discover page](https://matrix.org/discover).
*Note*: Matrix.org is also hosting a homeserver ([.well-known file](https://matrix.org/.well-known/matrix/client)).
The reference homeserver (this is how Matrix servers are called) implementation is [Synapse](https://github.com/matrix-org/synapse/). But other implementations exist. The Matrix specification is here to ensure that any Matrix client, such as Element Android and its SDK can talk to any Matrix server.

Have a quick look to the client-server API documentation: [Client-server documentation](https://spec.matrix.org/v1.3/client-server-api/). Other network API exist, the list is here: (https://spec.matrix.org/latest/)

Matrix is an open source protocol. Change are possible and are tracked using [this GitHub repository](https://github.com/matrix-org/matrix-doc/). Changes to the protocol are called MSC: Matrix Spec Change. These are PullRequest to this project.

Matrix object are Json data. Unstable prefixes must be used for Json keys when the MSC is not merged (i.e. accepted). 

#### Matrix data

There are many object and data in the Matrix worlds. Let's focus on the most important and used, `Room` and `Event`

##### Room

`Room` is a place which contains ordered `Event`s. They are identified with their `room_id`. Nearly all the data are stored in rooms, and shared using homeserver to all the Room Member.

*Note*: Spaces are also Rooms with a different `type`.

##### Event

`Events` are items of a Room, where data is embedded.

There are 2 types of Room Event:

- Regular Events: contain useful content for the user (message, image, etc.), but are not necessarily displayed as this in the timeline (reaction, message edition, call signaling).
- State Events: contain the state of the Room (name, topic, etc.). They have a non null value for the key `state_key`.

Also all the Room Member details are in State Events: one State Event per member. In this case, the `state_key` is the matrixId (= userId).

Important Fields of an Event:
- `event_id`: unique across the Matrix universe;
- `room_id`: the room the Event belongs to;
- `type`: describe what the Event contain, especially in the `content` section, and how the SDK should handle this Event;
- `content`: dynamic Event data; depends on the `type`.

So we have a triple `event_id`, `type`, `state_key` which uniquely defines an Event.

#### Sync

The `Sync` is a way for the Matrix client to be up to date regarding the user data hosted by the server. All the Events are coming through the sync response. More details can be found here: [spec.matrix.org/v1.3/client-server-api/#syncing](https://spec.matrix.org/v1.3/client-server-api/#syncing)
When the application is in foreground, this is a looping request. We are using Https requests, which offer the advantage to be compatible with any homeserver. A sync token is used as request parameter, to let the server know what the client knows.
The `SyncThread` is responsible to manage the sync request loop.

When the application is in background, a Push will trigger a sync request.

##### Glossary about syncs

- **initial sync**: a sync request without a token. This is the first request a client perform after login or after a clear cache. The server will include in the response all your rooms with the full state (all the room membership Event will not be present), with the latest messages for each room. We are in the process to replace this by version 3: sliding sync. All data are inserted to the Database (currently [Realm](https://www.mongodb.com/docs/realm/sdk/java/)).
- **incremental sync**: sync request with a token.
- **gappy sync**: sync request where all the new Events are not returned for one or several Rooms. Also called `limited sync`. It can be limited per Room. To get all the missing Events, a Room pagination API has to be called.
- **sync token**: `next_batch` value in the previous sync response. Will be provided as the `since` parameter for the next sync request.

### The Android project

The project should compile out of the box.

The project is split into several modules. The main ones are:
For the app
- `vector-app`: application entry point;
- `vector`: legacy application, but now a library. In the process of being split into several modules;
- `vector-config`: this is where all the configuration of the application should occurs. Should because we are in the process of migrating all the configuration here;
- `library/ui-strings`: this is where all the string resources are stored. Please refer to [contributing doc](../CONTRIBUTING.md) to know how to make change on this module;
- `library/ui-styles`: this is where the Android styles are defined.

For the SDK
- `matrix-sdk-android`: the main SDK module. The sources are in this project, but are also exported to [its own project](https://github.com/matrix-org/matrix-android-sdk2). All the PRs and issues related to the SDK take place in the Element Android project;
- `matrix-sdk-android-flow`: contains some wrapper to expose `Flow` to the application.

### Matrix SDK

SDK exposes `Services` to the client application. `Services` are public interface, and are defined in this parent package: `org.matrix.android.sdk.api`. Default implementation are internal to the SDK, in this parent package: `org.matrix.android.sdk.internal`. Note that you also have to declare the classes as `internal` when adding classes to the `org.matrix.android.sdk.internal` package.

Interface allows us to replace the implementation for testing purpose.

A generated documentation of the SDK is available [here](https://matrix-org.github.io/matrix-android-sdk2/). Updated after each release. Please ensure that the documentation (KDoc) of all the SDK Services is up to date, and is clear for a SDK user.
The SDK generated documentation also contains information about the entry points of the SDK.

[Dagger](https://dagger.dev/) is used to inject all the dependencies to the SDK classes.

SDK is exposing data as `LiveData`, but we are progressively migrating to `Flow`. Database is the source of truth.

Example:
- Client send an Event using the `SendService`;
- At the end a `SendEvent` task is used;
- Retrofit API is used to send data to the server;
- Goes to the server, which returns only the `event_id`;
- The `Event` is coming back from the `sync` response with eventually extra added data.

### Application

This is the UI part of the project.

There are two variants of the application: `Gplay` and `Fdroid`.

The main difference is about using Firebase on `Gplay` variant, to have Push from Google Services. `FDroid` variant cannot contain closed source dependency.

`Fdroid` is using background polling to lack the missing of Pushed. Now a solution using UnifiedPush has ben added to the project. See refer to [the dedicated documentation](./unifiedpush.md) for more details.

#### MvRx

[Maverick](https://airbnb.io/mavericks/#/README) (or MvRx) is an Android MVI framework that helps to develop Reactive application on Android.

- Activity: holder for Fragment. See the parent [VectorBaseActivity](../vector/src/main/java/im/vector/app/core/platform/VectorBaseActivity.kt);
- Fragment: manage screen of the application. See the parent [VectorBaseFragment](../vector/src/main/java/im/vector/app/core/platform/VectorBaseFragment.kt);
- BottomSheet: see the parent [VectorBaseBottomSheetDialogFragment](../vector/src/main/java/im/vector/app/core/platform/VectorBaseBottomSheetDialogFragment.kt);
- ViewModel: this is where the logic is placed. All our ViewModel has a `handle()` which takes action as parameter. See the parent [VectorViewModel](../vector/src/main/java/im/vector/app/core/platform/VectorViewModel.kt);
- VectorSharedActionViewModel: Specific ViewModel that can be used to communicate between Fragment(s) and the host Activity. See the parent [VectorSharedActionViewModel](../vector/src/main/java/im/vector/app/core/platform/VectorSharedActionViewModel.kt);
- ViewState: this are `data class`, and this represent the state of the View. Has to be copied and set to be updated. Fragment will update the UI regarding the current state (`invalidate()` method). `Async` class from MvRx can be used in the ViewState, especially for asynchronous data loading. Nullability can also be used for optional data. ViewStates have to implement `MavericksState`;
- ViewEvents: useful when the ViewModel asks the View to trigger a specific action: navigation, show dialog, etc. See the parent [VectorViewEvents](../vector/src/main/java/im/vector/app/core/platform/VectorViewEvents.kt);
- ViewAction (`VectorViewModelAction`): useful when the UI (generally the Fragment) asks the ViewModel to do something. See the parent [VectorViewModelAction](../vector/src/main/java/im/vector/app/core/platform/VectorViewModelAction.kt);
- Controller: see the `Epoxy` section just below.

##### Behavior

Fragment asks the ViewModel to perform an action (coming from the user, but not necessarily. ViewModel can then talk to the SDK, updates the state once or several times. Fragment update the UI regarding the new state.

When ViewModel is instantiated, it can subscribe using the SDK Services to get live state of the data.

`invalidate()` has to be used by default, but it's possible to listen to specific member(s) of the `ViewState` using `onEach`. TODO Add an example.
`awaitState()` method

#### Epoxy

[Epoxy](https://github.com/airbnb/epoxy) is an Android library for building complex screens in a RecyclerView. Please read [the introduction](https://github.com/airbnb/epoxy#epoxy).

- Controller declares items of the RecyclerView. Controller is injected in the Fragment. Controller extends `EpoxyController`, or one of its subclass, especially `TypedEpoxyController`;
- Fragment gives the state to the controller using `setData`;
- `buildModels` will be called by the framework;
- Controller will create ordered Items.

Epoxy does the diffing, and handle many other thing for us, like handling item type, etc.

See for instance the controller [AccountDataEpoxyController](../vector/src/main/java/im/vector/app/features/settings/devtools/AccountDataEpoxyController.kt)) for a simple example.

Warning: do not use twice the same item `id` or it will crash.

#### Other frameworks

- Dependency injection is managed by [Dagger](https://dagger.dev/) (SDK) and [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) (App);
- [Retrofit](https://square.github.io/retrofit/) and [OkHttp3](https://square.github.io/okhttp/): network requests;
- [Moshi](https://github.com/square/moshi) is used to parse and serialize Json object; 

### Push

Please see the dedicated documentation for more details.

This is the classical scenario:

- App receives a Push. Note: Push is ignored if app is in foreground;
- App asks the SDK to load Event data (fastlane mode). We have a change to get the data faster and display the notification faster;
- App asks the SDK to perform a sync request.

### Dependencies management

All the dependencies are declared in `build.gradle` files. But some versions are declared in [this dedicated file](../dependencies.gradle).

When adding a new dependency, you will have to update the file [dependencies_groups.gradle](../dependencies_groups.gradle) to allow the dependency to be downloaded from the artifact repository. Sometimes sub-dependencies need to be added too, until the project can compile.

[Dependabot](https://github.com/dependabot) is set up on the project. This tool will automatically create Pull Request to upgrade our dependencies one by one.
dependencies_group, gradle files, Dependabot, etc.

### Test

Please refer to [this dedicated document](./ui-tests.md).

TODO add link to the dedicated screenshot test documentation

### Other points

#### Logging

**Important warning: ** NEVER log private user data, or use the flag `LOG_PRIVATE_DATA`. Be very careful when logging `data class`, all the content will be output!

[Timber](https://github.com/JakeWharton/timber) is used to log data to logcat. We do not use directly the `Log` class. If possible please use a tag, as per

````kotlin
Timber.tag(loggerTag.value).d("my log")
````

because automatic tag (= class name) will not be available on the release version.

Also generally it is recommended to provide the `Throwable` to the Timber log functions.

Last point, not that `Timber.v` function may have no effect on some devices. Prefer using `Timber.d` and up.

#### Rageshake

Rageshake is a feature to send bug report directly from the application. Just shake your phone and you will be prompted to send a bug report.

Bug report can contain:
- a screenshot of the current application state
- the application logs from up to 15 application starts
- the logcat logs
- the key share history (crypto data)

The data will be sent to an internal server, which is not publicly accessible. A GitHub issue will also be created to a private GitHub repository.

Rageshake can be very useful to get logs from a release version of the application.

### Tips

- Element Android has a `developer mode` in the `Settings/Advanced settings`. Other useful options are available here;
- Show hidden Events can also help to debug feature. When developer mode is enabled, it is possible to view the source (= the Json content) of any Events;
- Type `/devtools` in a Room composer to access a developer menu. There are some other entry points. Developer mode has to be enabled;
- Hidden debug menu: when developer mode is enabled and on debug build, there are some extra screens that can be accessible using the green wheel. In those screens, it will be possible to toggle some feature flags;
- Using logcat, filtering with `onResume` can help you to understand what screen are currently displayed on your device. Searching for string displayed on the screen can also help to find the running code in the codebase.
- When this is possible, prefer using `sealed interface` instead of `sealed class`;
- When writing temporary code, using the string "DO NOT COMMIT" in a comment can help to avoid committing things by mistake. If committed and pushed, the CI will detect this String and will warn the user about it.

## Happy coding!

The team is here to support you, feel free to ask anything to other developers.

Also please feel to update this documentation, if incomplete/wrong/obsolete/etc.

**Thanks!**
