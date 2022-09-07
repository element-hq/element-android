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

The *F-Droid* and *Gplay* flavors of Element Android support UnifiedPush, so the user can use any distributor installed on their devices. This would make it possible to have push notifications without depending on Google services or libraries. Currently, the main distributors are [ntfy](https://ntfy.sh) which does not require any setup (like manual registration) to use the public server and [NextPush](https://github.com/UP-NextPush/android), available as a nextcloud application.

The *Gplay* variant uses a UnifiedPush library which basically embed a FCM distributor built into the application (so a user doesn't need to do anything other than install the app to get FCM notifications). This variant uses Google Services to receive notifications if the user has not installed any distributor. A [FCM Rewrite Proxy](https://unifiedpush.org/developers/embedded_fcm/#fcm-rewrite-proxy) is not required for Element Android's implementation of the FCM distributor - it will work with an existing Matrix push provider, such as [Sygnal](https://github.com/matrix-org/sygnal).

The *F-Droid* variant does not use this library to avoid any proprietary blob. It will use a polling service if the user has not installed any distributor.

In all cases, if there are other distributors available, the user will have to opt-in to one of them in the preferences.

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
