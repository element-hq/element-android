# UnifiedPush

<!--- TOC -->

* [Introduction](#introduction)
* [Configuration in Element-Android and their forks](#configuration-in-element-android-and-their-forks)
  * [Enabling and disabling the feature](#enabling-and-disabling-the-feature)
    * [Override the configuration at runtime](#override-the-configuration-at-runtime)
    * [Enabling the feature](#enabling-the-feature)
    * [Disabling the feature](#disabling-the-feature)
  * [Useful links](#useful-links)

<!--- END -->

## Introduction

The recently started UnifiedPush project is an Android protocol and library for apps to be able to receive distributor-agnostic push notifications.

The *Gplay* variant of Element Android use the UnifiedPush library to still receive push notifications from FCM, but also alternatively from other non-Google distributor systems that the user can have installed on their device. Currently available are Gotify, a server and app that receives push notifications via a websocket, NoProvider2Push, a peer-to-peer system, and others. This would make it possible to have push notifications without depending on Google services or libraries.

The UnifiedPush library comes in two variations: the FCM-added version of the library, which basically comes with the FCM distributor built into the library (so a user doesn't need to do anything other than install the app to get FCM notifications), and the main version of the library, which doesn't come with FCM embedded (so a user has to separately install the FCM, Gotify, or other distributor as an app on their phone to get push notifications).

These two versions of the library are used in the Google Play version and F-Droid version of the app respectively, to be able to publish an easy-to-use no-setup-needed version of the app to Google Play, and a version that doesn't depend on any Google code to F-Droid.

## Configuration in Element-Android and their forks

### Enabling and disabling the feature

Allowing the user to use an alternative distributor can be changed in [Config](../vector-config/src/main/java/im/vector/app/config/Config.kt). The flag is named `ALLOW_EXTERNAL_UNIFIED_PUSH_DISTRIBUTORS`. Default value is `true`.

#### Override the configuration at runtime

On debug version, it is possible to override this configuration at runtime, using the `Feature` screen. The Feature is named `Allow external UnifiedPush distributors`.

#### Enabling the feature

This is the default behavior of Element Android.

If `ALLOW_EXTERNAL_UNIFIED_PUSH_DISTRIBUTORS` is set to true, it allows any available external UnifiedPush distributor to be chosen by the user.
- For Gplay variant it means that FCM will be used by default, but user can choose another UnifiedPush distributor;
- For F-Droid variant, it means that background polling will be used by default, but user can choose another UnifiedPush distributor.
- On the UI, the setting to choose an alternative distributor will be visible to the user, and some tests in the notification troubleshoot screen will shown.
- For F-Droid, if the user has chosen a distributor, the settings to configure the background polling will be hidden.

#### Disabling the feature

If `ALLOW_EXTERNAL_UNIFIED_PUSH_DISTRIBUTORS` is set to false, it prevents the usage of external UnifiedPush distributors.
- For Gplay variant it means that only FCM will be used;
- For F-Droid variant, it means that only background polling will be used.
- On the UI, the setting to choose an alternative distributor will be hidden to the user, and some tests in the notification troubleshoot screen will be hidden.

### Useful links

- UnifiedPush official website: [https://unifiedpush.org/](https://unifiedpush.org/)
- List of available distributors can be retrieved here: [https://unifiedpush.org/users/distributors/](https://unifiedpush.org/users/distributors/)
- UnifiedPush project discussion can occurs here: [#unifiedpush:matrix.org](https://matrix.to/#/#unifiedpush:matrix.org)
