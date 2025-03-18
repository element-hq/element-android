Changes in Element v1.6.36 (2025-03-14)
=======================================

No significant changes.


Changes in Element v1.6.34 (2025-03-07)
=======================================

Security fixes 🔐
-----------------
- Fix for [GHSA-632v-9pm3-m8ch](https://github.com/element-hq/element-android/security/advisories/GHSA-632v-9pm3-m8ch) / [CVE-2025-27606](https://www.cve.org/CVERecord?id=CVE-2025-27606)

Changes in Element v1.6.32 (2025-02-18)
=======================================

Other changes
-------------
 - Add action to report room. ([#8998](https://github.com/element-hq/element-android/issues/8998))


Changes in Element v1.6.30 (2025-02-04)
=======================================

Dual licensing: AGPL + Element Commercial  ([#8990](https://github.com/element-hq/element-android/issues/8990))

Bugfixes 🐛
----------
 - Fix incoming call crash on Android 14+. ([#8964](https://github.com/element-hq/element-android/issues/8964))


Changes in Element v1.6.28 (2025-01-13)
=======================================

- Start sunsetting the application: prevent creation of new accounts on server with MAS support, and redirect users to Element X. ([#8983](https://github.com/element-hq/element-android/pull/8983))
- Sync strings. ([#8986](https://github.com/element-hq/element-android/pull/8986))


Changes in Element v1.6.26 (2024-12-20)
=======================================

Other changes
-------------
 - Bump org.matrix.rustcomponents:crypto-android from 0.5.0 to 0.6.0 based on matrix-sdk-crypto-0.9.0 ([#8960](https://github.com/element-hq/element-android/issues/8960))


Changes in Element v1.6.24 (2024-11-19)
=======================================

Bugfixes 🐛
----------
 - Extended file name support to include characters from multiple languages, including Cyrillic and Han scripts. ([#6449](https://github.com/element-hq/element-android/issues/6449)) ([#6449](https://github.com/element-hq/element-android/issues/6449))
 - Intercept mobile.element.io links with Element app ([#8904](https://github.com/element-hq/element-android/issues/8904))
 - Show a notice when a previously verified user is not anymore ([#8933](https://github.com/element-hq/element-android/issues/8933))

Other changes
-------------
 - Indicate when calls are unsupported in the timeline/notifications ([#8938](https://github.com/element-hq/element-android/issues/8938))


Changes in Element v1.6.22 (2024-09-23)
=======================================

Important: this version removes the dependency of the deprecated libolm library.
Application installations that have not been updated to the first version with the migration (v1.6.0 released at 2023-05-17) will not be able to migrate the account.
More details in ([#8901](https://github.com/element-hq/element-android/issues/8901))

Other changes
-------------
 - Remove legacy QR code login. ([#8889](https://github.com/element-hq/element-android/issues/8889))


Changes in Element v1.6.20 (2024-07-25)
=======================================

Other changes
-------------
- Bump compile and target SDK to 34 ([#8860](https://github.com/element-hq/element-android/pull/8860)
- Supports Authenticated media apis ([#8868](https://github.com/element-hq/element-android/pull/8868)

Changes in Element v1.6.18 (2024-06-25)
=======================================

Bugfixes 🐛
----------
 - Fix redacted events not grouped correctly when hidden events are inserted between. ([#8840](https://github.com/element-hq/element-android/issues/8840))
 - Element-Android session doesn't encrypt for a dehydrated device ([#8842](https://github.com/element-hq/element-android/issues/8842))
 - Intercept only links from `element.io` well known hosts. The previous behaviour broke OIDC login in Element X. ([#8849](https://github.com/element-hq/element-android/issues/8849))

Other changes
-------------
 - Posthog | report platform code for EA ([#8839](https://github.com/element-hq/element-android/issues/8839))


Changes in Element v1.6.16 (2024-05-29)
=======================================

Bugfixes 🐛
----------
 - Fix crash when accessing a local file and permission is revoked. ([#3616](https://github.com/element-hq/element-android/issues/3616))
 - Fixes Element on Android 12+ being ineligible for URL deeplinks ([#5748](https://github.com/element-hq/element-android/issues/5748))
 - Restore formatting when restoring a draft. Also keep formatting when switching composer mode. ([#7466](https://github.com/element-hq/element-android/issues/7466))

Other changes
-------------
 - Update posthog sdk to 3.2.0 ([#8820](https://github.com/element-hq/element-android/issues/8820))
 - Update Rust crypto SDK to version 0.4.1 ([#8838](https://github.com/element-hq/element-android/issues/8838))


Changes in Element v1.6.14 (2024-04-02)
=======================================

Bugfixes 🐛
----------
 - Fix send button blinking once for each character you are typing in RTE. ([#send_button_blinking](https://github.com/element-hq/element-android/issues/send_button_blinking))
 - Fix infinite loading on secure backup setup ("Re-Authentication needed" bottom sheet). ([#8786](https://github.com/element-hq/element-android/issues/8786))

Other changes
-------------
 - Improve UTD reporting by adding additional fields to the report. ([#8780](https://github.com/element-hq/element-android/issues/8780))
 - Add a report user action in the message bottom sheet and on the user profile page. ([#8796](https://github.com/element-hq/element-android/issues/8796))


Changes in Element v1.6.12 (2024-02-16)
=======================================

This update provides important security fixes, please update now.

Security fixes 🔐
-----------------
 - Add a check on incoming intent. [CVE-2024-26131](https://www.cve.org/CVERecord?id=CVE-2024-26131) / [GHSA-j6pr-fpc8-q9vm](https://github.com/element-hq/element-android/security/advisories/GHSA-j6pr-fpc8-q9vm)
 - Store temporary files created for Camera in a dedicated media folder. [CVE-2024-26132](https://www.cve.org/CVERecord?id=CVE-2024-26132) / [GHSA-8wj9-cx7h-pvm4](https://github.com/element-hq/element-android/security/advisories/GHSA-8wj9-cx7h-pvm4)

Bugfixes 🐛
----------
 - Switch the position and styles of the 'already have an account' and 'create account' buttons in the login splash screen. Also changes the 'already have an account one' to just say 'sign in'. ([#+update-login-splash-screen](https://github.com/element-hq/element-android/issues/+update-login-splash-screen))
 - Improve `Event.getClearContent()` and fix assignment issue that may help to decrypt last Event in the room list. ([#8744](https://github.com/element-hq/element-android/issues/8744))
 - Fix issues about location Event avatar rendering. ([#8749](https://github.com/element-hq/element-android/issues/8749))


Changes in Element v1.6.10 (2024-01-09)
=======================================

Features ✨
----------
 - Add support for Mobile Device Management.
  The keys are:
  - default homeserver URL `im.vector.app.serverConfigDefaultHomeserverUrlString`
  - push gateway URL `im.vector.app.serverConfigSygnalAPIUrlString`
  - permalink base URL `im.vector.app.clientPermalinkBaseUrl` ([#8698](https://github.com/element-hq/element-android/issues/8698))
 - Support Functional members (https://github.com/element-hq/element-meta/blob/develop/spec/functional_members.md) ([#3736](https://github.com/element-hq/element-android/issues/3736))

Bugfixes 🐛
----------
 - Fix some issues related to edition and reply of events. ([#5969](https://github.com/element-hq/element-android/issues/5969))
 - Fix crypto migration from kotlin to rust when an account has a single session and no backup. ([#8666](https://github.com/element-hq/element-android/issues/8666))


Changes in Element v1.6.8 (2023-11-28)
======================================

Bugfixes 🐛
----------
 - Stop incoming call ringing if the call is cancelled or answered on another session. ([#4066](https://github.com/element-hq/element-android/issues/4066))
 - Ensure the incoming call will not ring forever, in case the call is not ended by another way. ([#8178](https://github.com/element-hq/element-android/issues/8178))
 - Unified Push: Ignore the potential SSL error when the custom gateway is testing locally ([#8683](https://github.com/element-hq/element-android/issues/8683))
 - Fix issue with timeline message view reuse while rich text editor is enabled ([#8688](https://github.com/element-hq/element-android/issues/8688))

Other changes
-------------
 - Remove unused WebRTC dependency ([#8658](https://github.com/element-hq/element-android/issues/8658))
 - Take into account boolean "io.element.disable_network_constraint" from the .well-known file. ([#8662](https://github.com/element-hq/element-android/issues/8662))
 - Update regex for email address to be aligned on RFC 5322 ([#8671](https://github.com/element-hq/element-android/issues/8671))
 - Bump crypto sdk bindings to v0.3.16 ([#8679](https://github.com/element-hq/element-android/issues/8679))


Changes in Element v1.6.6 (2023-10-05)
======================================

Bugfixes 🐛
----------
 - Fixed JWT token for Jitsi openidtoken-jwt authentication ([#7758](https://github.com/element-hq/element-android/issues/7758))
 - Fix crash when max shortcuts count is exceeded ([#8644](https://github.com/element-hq/element-android/issues/8644))
 - Fix Login with QR code not working with rust crypto. ([#8653](https://github.com/element-hq/element-android/issues/8653))

Other changes
-------------
 - Use 3PID capability to show / hide email UI in settings ([#8615](https://github.com/element-hq/element-android/issues/8615))
 - If an external account manager is configured on the server, use it to delete other sessions and hide the multi session deletion. ([#8616](https://github.com/element-hq/element-android/issues/8616))
 - Hide account deactivation UI for account managed externally. ([#8619](https://github.com/element-hq/element-android/issues/8619))
 - Fix import of SAS Emoji string translations. ([#8623](https://github.com/element-hq/element-android/issues/8623))
 - Open external account manager for delete other sessions using Chrome custom tabs. ([#8645](https://github.com/element-hq/element-android/issues/8645))


Changes in Element v1.6.5 (2023-07-25)
======================================

Bugfixes 🐛
----------
 - Fix several crashes observed when the device cannot reach the homeserver ([#8578](https://github.com/element-hq/element-android/issues/8578))

Other changes
-------------
 - Update MSC3912 implementation: Redaction of related events ([#8481](https://github.com/element-hq/element-android/issues/8481))
 - Include some source code in our project to remove our dependency to artifact hosted by bintray (Jcenter). ([#8556](https://github.com/element-hq/element-android/issues/8556))


Changes in Element v1.6.3 (2023-06-27)
======================================

Features ✨
----------
 - **Element Android is now using the Crypto Rust SDK**. Migration of user's data should be done at first launch after application upgrade. ([#8390](https://github.com/element-hq/element-android/issues/8390))
 - [Rich text editor] Add mentions and slash commands ([#8440](https://github.com/element-hq/element-android/issues/8440))

Bugfixes 🐛
----------
 - Update rich text editor library to support pasting of images. ([#8270](https://github.com/element-hq/element-android/issues/8270))
 - Fix | Got asked twice about verification #8353 (and other verification banners problems) ([#8353](https://github.com/element-hq/element-android/issues/8353))
 - Prompt the user when the invited MatrixId is not recognized ([#8468](https://github.com/element-hq/element-android/issues/8468))
 - The correct title and options are now displayed When a poll that was edited is ended. ([#8471](https://github.com/element-hq/element-android/issues/8471))
 - In some conditions the room shield is not refreshed correctly ([#8507](https://github.com/element-hq/element-android/issues/8507))
 - Fix crypto config fallback key sharing strategy ([#8541](https://github.com/element-hq/element-android/issues/8541))

Other changes
-------------
 - MSC3987 implementation: the 'dont_notify' action for a push_rule is now deprecated and replaced by an empty action list. ([#8503](https://github.com/element-hq/element-android/issues/8503))
 - Update crypto rust sdk version to 0.3.10 ([#8554](https://github.com/element-hq/element-android/issues/8554))


Changes in Element v1.6.2 (2023-06-02)
======================================

Features ✨
----------
 - **Element Android is now using the Crypto Rust SDK**. Migration of user's data should be done at first launch after application upgrade. ([#8390](https://github.com/element-hq/element-android/issues/8390))
 - Marks WebP files as Animated and allows them to play ([#8120](https://github.com/element-hq/element-android/issues/8120))
 - Updates to protocol used for Sign in with QR code ([#8299](https://github.com/element-hq/element-android/issues/8299))
 - Updated rust crypto SDK to version 0.3.9 ([#8488](https://github.com/element-hq/element-android/issues/8488))

Bugfixes 🐛
----------
 - Fix: Allow users to sign out even if the sign out request fails. ([#4855](https://github.com/element-hq/element-android/issues/4855))
 - fix: Make some crypto calls suspendable to avoid reported ANR ([#8482](https://github.com/element-hq/element-android/issues/8482))

Other changes
-------------
 - Refactoring: Extract a new interface for common access to crypto store between kotlin and rust crypto ([#8470](https://github.com/element-hq/element-android/issues/8470))


Changes in Element v1.6.1 (2023-05-25)
======================================

Corrective release for 1.6.0

Bugfixes 🐛
----------
 - Allow stateloss on verification dialogfragment ([#8439](https://github.com/element-hq/element-android/issues/8439))
 - Fix: Update verification popup text when a re-verification is needed after rust migration (read only sessions) ([#8445](https://github.com/element-hq/element-android/issues/8445))
 - Fix several performance issues causing app non responsive issues. ([#8454](https://github.com/element-hq/element-android/issues/8454))
 - Fix: The device list screen from the member profile page was always showing the current user devices (rust crypto). ([#8457](https://github.com/element-hq/element-android/issues/8457))

Other changes
-------------
 - Remove UI option to manually verify a specific device of another user (deprecated behaviour) ([#8458](https://github.com/element-hq/element-android/issues/8458))


Changes in Element v1.6.0 (2023-05-17)
======================================

Features ✨
----------
 - **Element Android is now using the Crypto Rust SDK**. Migration of user's data should be done at first launch after application upgrade. ([#8390](https://github.com/element-hq/element-android/issues/8390))
 - Enable free style cropping for camera and gallery images ([#8325](https://github.com/element-hq/element-android/issues/8325))

Bugfixes 🐛
----------
 - User pills get lost at message editing ([#748](https://github.com/element-hq/element-android/issues/748))
 - Upgrade Jitsi SDK from 6.2.2 to 8.1.1. This fixes video call on some Android devices. ([#7619](https://github.com/element-hq/element-android/issues/7619))
 - Fix duplicate reactions when using full emoji picker. Contributed by @tulir @ Beeper. ([#8327](https://github.com/element-hq/element-android/issues/8327))
 - Fix: RustCrossSigning service API confusion (identity trusted vs own device trusted by identity) ([#8352](https://github.com/element-hq/element-android/issues/8352))
 - Allow custom push gateway to use non-default port ([#8376](https://github.com/element-hq/element-android/issues/8376))
 - Fix crash when opening "Protect access" screen, and various other issue with `repeatOnLifecycle` ([#8410](https://github.com/element-hq/element-android/issues/8410))
 - RustCrypto: Verification UX not refreshed after scanning a QR code ([#8418](https://github.com/element-hq/element-android/issues/8418))

SDK API changes ⚠️
------------------
 - First integration of rust crypto module. See documentation for details `docs/rust_crypto_integration.md` ([#7628](https://github.com/element-hq/element-android/issues/7628))
 - Add crypto database migration 22, that extract account and olm session to the new rust DB format ([#8405](https://github.com/element-hq/element-android/issues/8405))

Other changes
-------------
 - Add an audio alert when the voice broadcast recording is automatically paused ([#8339](https://github.com/element-hq/element-android/issues/8339))
 - Analytics: add crypto module to E2E events ([#8340](https://github.com/element-hq/element-android/issues/8340))
 - Bump rust crypto crate to 0.3.5 ([#8354](https://github.com/element-hq/element-android/issues/8354))
 - Expose Rust SDK Version in Help & About page and in Bug Reports ([#8364](https://github.com/element-hq/element-android/issues/8364))
 - Matrix-Ids are sometimes shown in notice events instead of display names ([#8365](https://github.com/element-hq/element-android/issues/8365))
 - CI: Add workflow to run test with crypto flavor ([#8366](https://github.com/element-hq/element-android/issues/8366))
 - Remove ability to migrate session from Riot to Element. ([#8402](https://github.com/element-hq/element-android/issues/8402))
 - Improve keyboard navigation and accessibility when using a screen reader. ([#8426](https://github.com/element-hq/element-android/issues/8426))
 - Updated posthog url (cosmetic, target same server) and added a new sentry env. ([#8436](https://github.com/element-hq/element-android/issues/8436))


Changes in Element v1.5.32 (2023-04-19)
=======================================

Bugfixes 🐛
----------
 - Fix multiple read receipts for the same user in timeline. ([#7882](https://github.com/element-hq/element-android/issues/7882))
 - The new permalink rendering is not applied on permalink created with the potential clientPermalinkBaseUrl ([#8307](https://github.com/element-hq/element-android/issues/8307))
 - Keep screen on while recording voicebroadcast ([#8313](https://github.com/element-hq/element-android/issues/8313))


Changes in Element v1.5.30 (2023-04-05)
=======================================

Features ✨
----------
 - Permalinks to a room/space are pillified ([#8219](https://github.com/element-hq/element-android/issues/8219))
 - Permalinks to a matrix user are pillified ([#8220](https://github.com/element-hq/element-android/issues/8220))
 - Permalinks to messages are pillified ([#8221](https://github.com/element-hq/element-android/issues/8221))

Bugfixes 🐛
----------
 - Custom sticker picker loads indefinitely ([#8026](https://github.com/element-hq/element-android/issues/8026))
 - Replace hardcoded colors by theming colors on save button. ([#8208](https://github.com/element-hq/element-android/issues/8208))
 - Add RTL support to RemoveJitsiWidgetView ([#8210](https://github.com/element-hq/element-android/issues/8210))
 - Add user completion for matrix ids ([#8217](https://github.com/element-hq/element-android/issues/8217))
 - Long name are truncated in the pills ([#8218](https://github.com/element-hq/element-android/issues/8218))
 - The read marker is stuck in the past ([#8268](https://github.com/element-hq/element-android/issues/8268))

Other changes
-------------
 - Replace Terms and Conditions with Acceptable Use Policy. ([#8286](https://github.com/element-hq/element-android/issues/8286))


Changes in Element v1.5.28 (2023-03-08)
=======================================

Features ✨
----------
 - [Poll] Error handling for push rules synchronization ([#8141](https://github.com/element-hq/element-android/issues/8141))
 - Add aggregated unread indicator for spaces in the new layout ([#8157](https://github.com/element-hq/element-android/issues/8157))
 - [Rich text editor] Add ability to insert GIFs from keyboard ([#8185](https://github.com/element-hq/element-android/issues/8185))

Bugfixes 🐛
----------
 - Space setting category doesn't show up ([#7989](https://github.com/element-hq/element-android/issues/7989))
 - Fix timeline loading a wrong room on permalink if a matching event id is found in a different room ([#8168](https://github.com/element-hq/element-android/issues/8168))
 - Reapply local push rules after event decryption ([#8170](https://github.com/element-hq/element-android/issues/8170))
 - [Rich text editor] Fix code appearance ([#8171](https://github.com/element-hq/element-android/issues/8171))
 - Extend workaround for extra new lines in timeline ([#8187](https://github.com/element-hq/element-android/issues/8187))
 - [Poll history] Fixing small issue about font style ([#8190](https://github.com/element-hq/element-android/issues/8190))
 - Update room member shields behavior ([#8195](https://github.com/element-hq/element-android/issues/8195))

Other changes
-------------
 - Direct Message: Manage encrypted DM in case of invite by email ([#6912](https://github.com/element-hq/element-android/issues/6912))


Changes in Element v1.5.26 (2023-02-22)
=======================================

Features ✨
----------
 - Adds MSC3824 OIDC-awareness when talking to an OIDC-enabled homeservers ([#6367](https://github.com/element-hq/element-android/issues/6367))
 - [Poll] Synchronize polls push rules with message push rules ([#8007](https://github.com/element-hq/element-android/issues/8007))
 - [Rich text editor] Add code block, quote and indentation actions ([#8045](https://github.com/element-hq/element-android/issues/8045))
 - [Poll] History list: details screen of a poll
 - [Poll] History list: enable the new settings entry in release mode ([#8056](https://github.com/element-hq/element-android/issues/8056))
 - [Location sharing] Show own location in map views ([#8110](https://github.com/element-hq/element-android/issues/8110))
 - Updates to protocol used for Sign in with QR code ([#8123](https://github.com/element-hq/element-android/issues/8123))
 - [Poll] Synchronize polls and message push rules ([#8130](https://github.com/element-hq/element-android/issues/8130))

Bugfixes 🐛
----------
 - Android app does not show correct poll data ([#6121](https://github.com/element-hq/element-android/issues/6121))
 - Fix timeline always jumps to the bottom when screen goes back to foreground. ([#8090](https://github.com/element-hq/element-android/issues/8090))
 - [Poll] Improve rendering of poll end message when poll start event isn't available ([#8129](https://github.com/element-hq/element-android/issues/8129))
 - Replace hardcoded colors by theming colors on send button. ([#8142](https://github.com/element-hq/element-android/issues/8142))
 - [Timeline]: Editing a reply from iOS breaks the "in reply to" rendering ([#8150](https://github.com/element-hq/element-android/issues/8150))

Other changes
-------------
 - Build unmerged APKs on pull request ([#8044](https://github.com/element-hq/element-android/issues/8044))
 - Replace 'Bots' with 'bots' inside terms_description_for_integration_manager ([#8115](https://github.com/element-hq/element-android/issues/8115))
 - Fix ktlint issue with fields and a new line. ([#8139](https://github.com/element-hq/element-android/issues/8139))


Changes in Element v1.5.25 (2023-02-15)
=======================================

Bugfixes 🐛
----------
 - CountUpTimer - Fix StackOverFlow exception when stop action is called within the tick event ([#8127](https://github.com/element-hq/element-android/issues/8127))


Changes in Element v1.5.24 (2023-02-08)
=======================================

Features ✨
----------
 - [Rich text editor] Add inline code to rich text editor ([#8011](https://github.com/element-hq/element-android/issues/8011))

Bugfixes 🐛
----------
 - If media cache is large, Settings > General takes a long time to open ([#5918](https://github.com/element-hq/element-android/issues/5918))
 - Fix that replies to @roomba would be highlighted as a room ping. Contributed by Nico. ([#6457](https://github.com/element-hq/element-android/issues/6457))
 - Cannot select text properly in plain text mode when using Rich Text Editor. ([#7801](https://github.com/element-hq/element-android/issues/7801))
 - Fix the next button disabled issue after going to change homeserver screen ([#7928](https://github.com/element-hq/element-android/issues/7928))
 - Fix extra new lines added to inline code ([#7975](https://github.com/element-hq/element-android/issues/7975))
 - [Voice Broadcast] Use internal playback timer to compute the current playback position ([#8012](https://github.com/element-hq/element-android/issues/8012))
 - Do not send any request to Posthog if no consent is provided. ([#8031](https://github.com/element-hq/element-android/issues/8031))
 - [Voice Broadcast] We should not be able to start broadcasting if there is already a live broadcast in the Room ([#8062](https://github.com/element-hq/element-android/issues/8062))

In development 🚧
----------------
 - [Poll] History list: unmock data ([#7864](https://github.com/element-hq/element-android/issues/7864))

SDK API changes ⚠️
------------------
 - [Poll] Adding PollHistoryService ([#7864](https://github.com/element-hq/element-android/issues/7864))
 - [Push rules] Call /actions api before /enabled api ([#8005](https://github.com/element-hq/element-android/issues/8005))

Other changes
-------------
 - Let the user know when we are not able to decrypt the voice broadcast chunks ([#7820](https://github.com/element-hq/element-android/issues/7820))
 - [Voice Broadcast] Show Live broadcast in the room list only if the feature flag is enabled in the lab ([#8042](https://github.com/element-hq/element-android/issues/8042))
 - Improve the `CountUpTimer` implementation ([#8058](https://github.com/element-hq/element-android/issues/8058))


Changes in Element v1.5.22 (2023-01-25)
=======================================

Features ✨
----------
 - [Poll] Warning message on decryption failure of some events ([#7824](https://github.com/element-hq/element-android/issues/7824))
 - [Poll] Render ended polls ([#7900](https://github.com/element-hq/element-android/issues/7900))
 - [Rich text editor] Update list item bullet appearance ([#7930](https://github.com/element-hq/element-android/issues/7930))
 - [Voice Broadcast] Handle connection errors while recording ([#7890](https://github.com/element-hq/element-android/issues/7890))
 - [Voice Broadcast] Use MSC3912 to delete server side all the related events ([#7967](https://github.com/element-hq/element-android/issues/7967))

Bugfixes 🐛
----------
- Fix OOM crashes. ([#7962](https://github.com/element-hq/element-android/issues/7962))
- Fix can't get out of a verification dialog ([#4025](https://github.com/element-hq/element-android/issues/4025))
- Fix rendering of edited polls ([#7938](https://github.com/element-hq/element-android/issues/7938))
- [Voice Broadcast] Fix unexpected "live broadcast" in the room list ([#7832](https://github.com/element-hq/element-android/issues/7832))
- Send voice message should not be allowed during a voice broadcast recording ([#7895](https://github.com/element-hq/element-android/issues/7895))
- Voice Broadcast - Fix playback scrubbing not working if the playback is in a stopped state ([#7961](https://github.com/element-hq/element-android/issues/7961))
- Handle exceptions when listening a voice broadcast ([#7829](https://github.com/element-hq/element-android/issues/7829))

In development 🚧
----------------
 - [Voice Broadcast] Only display a notification on the first voice chunk ([#7845](https://github.com/element-hq/element-android/issues/7845))
 - [Poll] History list: Load more UI mechanism ([#7864](https://github.com/element-hq/element-android/issues/7864))

SDK API changes ⚠️
------------------
 - Implement [MSC3912](https://github.com/matrix-org/matrix-spec-proposals/pull/3912): Relation-based redactions ([#7988](https://github.com/element-hq/element-android/issues/7988))

Other changes
-------------
 - Upgrade to Kotlin 1.8 ([#7936](https://github.com/element-hq/element-android/issues/7936))
 - Sentry: Report sync duration and metrics for initial sync and for sync after pause. Not for regular sync. ([#7960](https://github.com/element-hq/element-android/issues/7960))
 - [Voice Broadcast] Rework internal media players coordination ([#7979](https://github.com/element-hq/element-android/issues/7979))
 - Support reactions on Voice Broadcast ([#7807](https://github.com/element-hq/element-android/issues/7807))
 - Pause voice broadcast listening on new VB recording ([#7830](https://github.com/element-hq/element-android/issues/7830))
 - Tapping slightly left or right of the 30s buttons highlights the whole cell instead of registering as button presses ([#7929](https://github.com/element-hq/element-android/issues/7929))


Changes in Element v1.5.20 (2023-01-10)
=======================================

Features ✨
----------
 - "[Rich text editor] Add list formatting buttons to the rich text editor" ([#7887](https://github.com/element-hq/element-android/issues/7887))

Bugfixes 🐛
----------
 - ReplyTo are not updated if the original message is edited or deleted. ([#5546](https://github.com/element-hq/element-android/issues/5546))
 - Observe ViewEvents only when resumed and ensure ViewEvents are not lost. ([#7724](https://github.com/element-hq/element-android/issues/7724))
 - [Session manager] Missing info when a session does not support encryption ([#7853](https://github.com/element-hq/element-android/issues/7853))
 - Reduce number of crypto database transactions when handling the sync response ([#7879](https://github.com/element-hq/element-android/issues/7879))
 - [Voice Broadcast] Stop listening if we reach the last received chunk and there is no last sequence number ([#7899](https://github.com/element-hq/element-android/issues/7899))
 - Handle network error on API `rooms/{roomId}/threads` ([#7913](https://github.com/element-hq/element-android/issues/7913))

In development 🚧
----------------
 - [Poll] Render active polls list of a room
 - [Poll] Render past polls list of a room ([#7864](https://github.com/element-hq/element-android/issues/7864))

Other changes
-------------
 - fix: increase font size for messages ([#5717](https://github.com/element-hq/element-android/issues/5717))
 - Add trim to username input on the app side and SDK side when sign-in ([#7111](https://github.com/element-hq/element-android/issues/7111))


Changes in Element v1.5.18 (2023-01-02)
=======================================

This release fixes a bunch of recent regressions. Most of them were not pushed to production hopefully. Current production version is 1.5.11.
Threads are now enabled by default, and this may let the application perform an initial sync.
Testers on the PlayStore may have experimented some issues like empty room list, or incomplete room state (room name missing, etc.), or even crashing due to initial sync not using lazy loading of room members. All those issues have been fixed, but to fix your current state, please clear cache once you get the release 1.5.18.

Bugfixes 🐛
----------
 - Start DM will create a deadlock if user profile was never loaded ([#7870](https://github.com/element-hq/element-android/issues/7870))
 

Changes in Element v1.5.16 (2022-12-29)
======================================

Features ✨
----------
 - [Rich text editor] Add support for links ([#7746](https://github.com/element-hq/element-android/issues/7746))
 - [Poll] When a poll is ended, use /relations API to ensure poll results are correct ([#7767](https://github.com/element-hq/element-android/issues/7767))
 - [Session manager] Security recommendations cards: whole view should be tappable ([#7795](https://github.com/element-hq/element-android/issues/7795))
 - [Session manager] Other sessions list: header should not be sticky ([#7797](https://github.com/element-hq/element-android/issues/7797))

Bugfixes 🐛
----------
 - Do not show typing notification of ignored users. ([#2965](https://github.com/element-hq/element-android/issues/2965))
 - [Push Notifications, Threads] - quick reply to threaded notification now sent to thread except main timeline ([#7475](https://github.com/element-hq/element-android/issues/7475))
 - [Session manager] Other sessions list: filter option is displayed when selection mode is enabled ([#7784](https://github.com/element-hq/element-android/issues/7784))
 - [Session manager] Other sessions: Filter bottom sheet cut in landscape mode ([#7786](https://github.com/element-hq/element-android/issues/7786))
 - Automatically show keyboard after learn more bottom sheet is dismissed ([#7790](https://github.com/element-hq/element-android/issues/7790))
 - [Session Manager] Other sessions list: cannot select/deselect session by a long press when in select mode ([#7792](https://github.com/element-hq/element-android/issues/7792))
 - Fix current session ip address visibility ([#7794](https://github.com/element-hq/element-android/issues/7794))
 - Device Manager UI review fixes ([#7798](https://github.com/element-hq/element-android/issues/7798))

SDK API changes ⚠️
------------------
 - [Sync] Sync Filter params are moved to MatrixConfiguration and will not be stored in session realm to avoid bug when session cache is cleared ([#7843](https://github.com/element-hq/element-android/issues/7843))

Other changes
-------------
 - [Voice Broadcast] Replace the player timeline ([#7821](https://github.com/element-hq/element-android/issues/7821))
 - Increase session manager test coverage ([#7836](https://github.com/element-hq/element-android/issues/7836))


Changes in Element v1.5.14 (2022-12-20)
=======================================

Bugfixes 🐛
----------
- ActiveSessionHolder is not supposed to start syncing. Instead, the MainActivity does it, if necessary. Fixes a race condition when clearing cache.


Changes in Element v1.5.13 (2022-12-19)
=======================================

Bugfixes 🐛
----------
- Add `largeHeap=true` in the manifest since we are seeing more crashes (OOM) when handling sync response.


Changes in Element v1.5.12 (2022-12-15)
=======================================

Features ✨
----------
- [Threads] - Threads Labs Flag is enabled by default and forced to be enabled for existing users, but sill can be disabled manually ([#5503](https://github.com/element-hq/element-android/issues/5503))
 - [Session manager] Add action to signout all the other session ([#7693](https://github.com/element-hq/element-android/issues/7693))
 - Remind unverified sessions with a banner once a week ([#7694](https://github.com/element-hq/element-android/issues/7694))
 - [Session manager] Add actions to rename and signout current session ([#7697](https://github.com/element-hq/element-android/issues/7697))
 - Voice Broadcast - Update last message in the room list ([#7719](https://github.com/element-hq/element-android/issues/7719))
 - Delete unused client information from account data ([#7754](https://github.com/element-hq/element-android/issues/7754))

Bugfixes 🐛
----------
 - Fix bad pills color background. For light and dark theme the color is now 61708B (iso EleWeb) ([#7274](https://github.com/element-hq/element-android/issues/7274))
 - [Notifications] Fixed a bug when push notification was automatically dismissed while app is on background ([#7643](https://github.com/element-hq/element-android/issues/7643))
 - ANR when asking to select the notification method ([#7653](https://github.com/element-hq/element-android/issues/7653))
 - [Rich text editor] Fix design and spacing of rich text editor ([#7658](https://github.com/element-hq/element-android/issues/7658))
 - [Rich text editor] Fix keyboard closing after collapsing editor ([#7659](https://github.com/element-hq/element-android/issues/7659))
 - Rich Text Editor: fix several issues related to insets:
  * Empty space displayed at the bottom when you don't have permissions to send messages into a room.
  * Wrong insets being kept when you exit the room screen and the keyboard is displayed, then come back to it. ([#7680](https://github.com/element-hq/element-android/issues/7680))
 - Fix crash in message composer when room is missing ([#7683](https://github.com/element-hq/element-android/issues/7683))
 - Fix crash when invalid homeserver url is entered. ([#7684](https://github.com/element-hq/element-android/issues/7684))
 - Rich Text Editor: improve performance when entering reply/edit/quote mode. ([#7691](https://github.com/element-hq/element-android/issues/7691))
 - [Rich text editor] Add error tracking for rich text editor ([#7695](https://github.com/element-hq/element-android/issues/7695))
 - Fix E2EE set up failure whilst signing in using QR code ([#7699](https://github.com/element-hq/element-android/issues/7699))
 - Fix usage of unknown shield in room summary ([#7710](https://github.com/element-hq/element-android/issues/7710))
 - Fix crash when the network is not available. ([#7725](https://github.com/element-hq/element-android/issues/7725))
 - [Session manager] Sessions without encryption support should not prompt to verify ([#7733](https://github.com/element-hq/element-android/issues/7733))
 - Fix issue of Scan QR code button sometimes not showing when it should be available ([#7737](https://github.com/element-hq/element-android/issues/7737))
 - Verification request is not showing when verify session popup is displayed ([#7743](https://github.com/element-hq/element-android/issues/7743))
 - Fix crash when inviting by email. ([#7744](https://github.com/element-hq/element-android/issues/7744))
 - Revert usage of stable fields in live location sharing and polls ([#7751](https://github.com/element-hq/element-android/issues/7751))
 - [Poll] Poll end event is not recognized ([#7753](https://github.com/element-hq/element-android/issues/7753))
 - [Push Notifications] When push notification for threaded message is clicked, thread timeline will be opened instead of room's main timeline ([#7770](https://github.com/element-hq/element-android/issues/7770))

Other changes
-------------
 - [Threads] - added API to fetch threads list from the server instead of building it locally from events ([#5819](https://github.com/element-hq/element-android/issues/5819))
 - Add Z-Labs label for rich text editor and migrate to new label naming. ([#7477](https://github.com/element-hq/element-android/issues/7477))
 - Crypto database migration tests ([#7645](https://github.com/element-hq/element-android/issues/7645))
 - Add tracing Id for to device messages ([#7708](https://github.com/element-hq/element-android/issues/7708))
 - Disable nightly popup and add an entry point in the advanced settings instead. ([#7723](https://github.com/element-hq/element-android/issues/7723))
- Save m.local_notification_settings.<device-id> event in account_data ([#7596](https://github.com/element-hq/element-android/issues/7596))
- Update notifications setting when m.local_notification_settings.<device-id> event changes for current device ([#7632](https://github.com/element-hq/element-android/issues/7632))

SDK API changes ⚠️
------------------
- Handle account data removal ([#7740](https://github.com/element-hq/element-android/issues/7740))

Changes in Element 1.5.11 (2022-12-07)
======================================

Bugfixes 🐛
----------
 - Fix crash when the network is not available. ([#7725](https://github.com/element-hq/element-android/issues/7725))


Changes in Element v1.5.10 (2022-11-30)
=======================================

Features ✨
----------
 - Add setting to allow disabling direct share ([#2725](https://github.com/element-hq/element-android/issues/2725))
 - [Device Manager] Toggle IP address visibility ([#7546](https://github.com/element-hq/element-android/issues/7546))
 - New implementation of the full screen mode for the Rich Text Editor. ([#7577](https://github.com/element-hq/element-android/issues/7577))

Bugfixes 🐛
----------
 - Fix italic text is truncated when bubble mode and markdown is enabled ([#5679](https://github.com/element-hq/element-android/issues/5679))
 - Missing translations on "replyTo" messages ([#7555](https://github.com/element-hq/element-android/issues/7555))
 - ANR on session start when sending client info is enabled ([#7604](https://github.com/element-hq/element-android/issues/7604))
 - Make the plain text mode layout of the RTE more compact. ([#7620](https://github.com/element-hq/element-android/issues/7620))
 - Push notification for thread message is now shown correctly when user observes rooms main timeline ([#7634](https://github.com/element-hq/element-android/issues/7634))
 - Voice Broadcast - Fix playback stuck in buffering mode ([#7646](https://github.com/element-hq/element-android/issues/7646))

In development 🚧
----------------
 - Voice Broadcast - Handle redaction of the state events on the listener and recorder sides ([#7629](https://github.com/element-hq/element-android/issues/7629))
 - Voice Broadcast - Update the buffering display in the timeline ([#7655](https://github.com/element-hq/element-android/issues/7655))
 - Voice Broadcast - Remove voice messages related to a VB from the room attachments ([#7656](https://github.com/element-hq/element-android/issues/7656))

SDK API changes ⚠️
------------------
 - Added support for read receipts in threads. Now user in a room can have multiple read receipts (one per thread + one in main thread + one without threadId) ([#6996](https://github.com/element-hq/element-android/issues/6996))
 - Sync Filter now taking in account homeserver capabilities to not pass unsupported parameters.
  Sync Filter is now configured by providing SyncFilterBuilder class instance, instead of Filter to identify Filter changes related to homeserver capabilities ([#7626](https://github.com/element-hq/element-android/issues/7626))

Other changes
-------------
 - Remove usage of Buildkite. ([#7583](https://github.com/element-hq/element-android/issues/7583))
 - Better validation of edits ([#7594](https://github.com/element-hq/element-android/issues/7594))


Changes in Element v1.5.8 (2022-11-17)
======================================

Features ✨
----------
 - [Session manager] Multi-session signout ([#7418](https://github.com/element-hq/element-android/issues/7418))
 - Rich text editor: add full screen mode. ([#7436](https://github.com/element-hq/element-android/issues/7436))
 - [Rich text editor] Add plain text mode ([#7452](https://github.com/element-hq/element-android/issues/7452))
 - Move TypingView inside the timeline items. ([#7496](https://github.com/element-hq/element-android/issues/7496))
 - Push notifications toggle: align implementation for current session ([#7512](https://github.com/element-hq/element-android/issues/7512))
 - Voice messages - Persist the playback position across different screens ([#7582](https://github.com/element-hq/element-android/issues/7582))

Bugfixes 🐛
----------
 - [Voice Broadcast] Do not display the recorder view for a live broadcast started from another session ([#7431](https://github.com/element-hq/element-android/issues/7431))
 - [Session manager] Hide push notification toggle when there is no server support ([#7457](https://github.com/element-hq/element-android/issues/7457))
 - Fix rich text editor textfield not growing to fill parent on full screen. ([#7491](https://github.com/element-hq/element-android/issues/7491))
 - Fix duplicated mention pills in some cases ([#7501](https://github.com/element-hq/element-android/issues/7501))
 - Voice Broadcast - Fix duplicated voice messages in the internal playlist ([#7502](https://github.com/element-hq/element-android/issues/7502))
 - When joining a room, the message composer is displayed once the room is loaded. ([#7509](https://github.com/element-hq/element-android/issues/7509))
 - Voice Broadcast - Fix error on voice messages in unencrypted rooms ([#7519](https://github.com/element-hq/element-android/issues/7519))
 - Fix description of verified sessions ([#7533](https://github.com/element-hq/element-android/issues/7533))

In development 🚧
----------------
 - [Voice Broadcast] Improve timeline items factory and handle bad recording state display ([#7448](https://github.com/element-hq/element-android/issues/7448))
 - [Voice Broadcast] Stop recording when opening the room after an app restart ([#7450](https://github.com/element-hq/element-android/issues/7450))
 - [Voice Broadcast] Improve playlist fetching and player codebase ([#7478](https://github.com/element-hq/element-android/issues/7478))
 - [Voice Broadcast] Display an error dialog if the user fails to start a voice broadcast ([#7485](https://github.com/element-hq/element-android/issues/7485))
 - [Voice Broadcast] Add seekbar in listening tile ([#7496](https://github.com/element-hq/element-android/issues/7496))
 - [Voice Broadcast] Improve the live indicator icon rendering in the timeline ([#7579](https://github.com/element-hq/element-android/issues/7579))
 - Voice Broadcast - Add maximum length ([#7588](https://github.com/element-hq/element-android/issues/7588))

SDK API changes ⚠️
------------------
 - [Metrics] Add `SpannableMetricPlugin` to support spans within transactions. ([#7514](https://github.com/element-hq/element-android/issues/7514))
 - Fix a bug that caused messages with no formatted text to be quoted as "null". ([#7530](https://github.com/element-hq/element-android/issues/7530))
 - If message content has no `formattedBody`, default to `body` when editing. ([#7574](https://github.com/element-hq/element-android/issues/7574))


Changes in Element v1.5.7 (2022-11-07)
======================================

Bugfixes 🐛
----------
- Fix regression when syncing with homeserver < 1.4. ([#7534](https://github.com/element-hq/element-android/issues/7534))

Changes in Element v1.5.6 (2022-11-02)
======================================

Features ✨
----------
 - Add new UI for selecting an attachment ([#7429](https://github.com/element-hq/element-android/issues/7429))
 - Multi selection in sessions list ([#7396](https://github.com/element-hq/element-android/issues/7396))

Bugfixes 🐛
----------
 - New line and Enter hardware key presses deleting existing text in some keyboards. ([#7357](https://github.com/element-hq/element-android/issues/7357))
 - Fix share actions using share dialog. ([#7400](https://github.com/element-hq/element-android/issues/7400))
 - Fix crash by disabling Flipper on Android API 22 and below - only affects debug version of the application. ([#7428](https://github.com/element-hq/element-android/issues/7428))

In development 🚧
----------------
 - [Voice Broadcast] Live listening support ([#7419](https://github.com/element-hq/element-android/issues/7419))
 - [Voice Broadcast] Improve rendering in the timeline ([#7421](https://github.com/element-hq/element-android/issues/7421))
 - Add logic for sign in with QR code ([#7369](https://github.com/element-hq/element-android/issues/7369))

SDK API changes ⚠️
------------------
 - Add MetricPlugin interface to implement metrics in SDK clients. ([#7438](https://github.com/element-hq/element-android/issues/7438))

Other changes
-------------
 - Upgrade Jitsi SDK to 6.2.2 and WebRtc to 1.106.1-jitsi-12039821. ([#6195](https://github.com/element-hq/element-android/issues/6195))
 - Gets thread notifications from sync response ([#7424](https://github.com/element-hq/element-android/issues/7424))
 - Replace org.apache.sanselan:sanselan by org.apache.commons:commons-imaging ([#7454](https://github.com/element-hq/element-android/issues/7454))


Changes in Element v1.5.4 (2022-10-19)
======================================

Features ✨
----------
 - Add WYSIWYG editor, under a lab flag. ([#7288](https://github.com/element-hq/element-android/issues/7288))
 - New Device management, can be enabled in the labs settings.
 - Voice broadcast can be enabled in the labs settings (recording is possible only on Android 10 and up).

Bugfixes 🐛
----------
 - Fix wrong mic button direction to cancel on RTL languages ([#5968](https://github.com/element-hq/element-android/issues/5968))
 - Handle properly when getUser returns null - prefer using getUserOrDefault ([#7372](https://github.com/element-hq/element-android/issues/7372))
 - [Device Management] Long session names not handled well ([#7310](https://github.com/element-hq/element-android/issues/7310))
 - Fix editing formatted messages with plain text editor ([#7359](https://github.com/element-hq/element-android/issues/7359))

In development 🚧
----------------
 - [Device Management] Save "matrix_client_information" events on login/registration ([#7257](https://github.com/element-hq/element-android/issues/7257))
 - [Device management] Add lab flag for the feature ([#7336](https://github.com/element-hq/element-android/issues/7336))
 - [Device management] Add lab flag for matrix client info account data event ([#7344](https://github.com/element-hq/element-android/issues/7344))
 - [Device Management] Redirect to the new screen everywhere when lab flag is on ([#7374](https://github.com/element-hq/element-android/issues/7374))
 - [Device Management] Show correct device type icons ([#7277](https://github.com/element-hq/element-android/issues/7277))
 - [Device Management] Render extended device info ([#7294](https://github.com/element-hq/element-android/issues/7294))
 - [Device management] Improve the parsing for OS of Desktop/Web sessions ([#7321](https://github.com/element-hq/element-android/issues/7321))
 - [Device management] Hide the IP address and last activity date on current session ([#7324](https://github.com/element-hq/element-android/issues/7324))
 - [Device management] Update the unknown verification status icon ([#7327](https://github.com/element-hq/element-android/issues/7327))
 - [Voice Broadcast] Add the "io.element.voice_broadcast_info" state event with a minimalist timeline widget ([#7273](https://github.com/element-hq/element-android/issues/7273))
 - [Voice Broadcast] Aggregate state events in the timeline ([#7283](https://github.com/element-hq/element-android/issues/7283))
 - [Voice Broadcast] Record and send non aggregated voice messages to the room ([#7363](https://github.com/element-hq/element-android/issues/7363))
 - [Voice Broadcast] Start listening to a voice broadcast ([#7387](https://github.com/element-hq/element-android/issues/7387))
 - [Voice Broadcast] Enable the feature (behind a lab flag and only for Android 10 and up) ([#7393](https://github.com/element-hq/element-android/issues/7393))
 - [Voice Broadcast] Add additional data in events ([#7397](https://github.com/element-hq/element-android/issues/7397))
 - Implements MSC3881: Parses `enabled` and `device_id` fields from updated Pusher API ([#7217](https://github.com/element-hq/element-android/issues/7217))
 - Adds pusher toggle setting to device manager v2 ([#7261](https://github.com/element-hq/element-android/issues/7261))
 - Implement QR Code Login UI ([#7338](https://github.com/element-hq/element-android/issues/7338))
 - Implements client-side of local notification settings event ([#7300](https://github.com/element-hq/element-android/issues/7300))
 - Links "Enable Notifications for this session" setting to enabled value in pusher ([#7281](https://github.com/element-hq/element-android/issues/7281))

SDK API changes ⚠️
------------------
 - Stop using `original_event` field from `/relations` endpoint ([#7282](https://github.com/element-hq/element-android/issues/7282))
 - Add `formattedText` or similar optional parameters in several methods:
  * RelationService:
  	* editTextMessage
  	* editReply
  	* replyToMessage
  * SendService:
  	* sendQuotedTextMessage
  This allows us to send any HTML formatted text message without needing to rely on automatic Markdown > HTML translation. All these new parameters have a `null` value by default, so previous calls to these API methods remain compatible. ([#7288](https://github.com/element-hq/element-android/issues/7288))
 - Add support for `m.login.token` auth during QR code based sign in ([#7358](https://github.com/element-hq/element-android/issues/7358))
 - Allow getting the formatted or plain text body of a message for the fun `TimelineEvent.getTextEditableContent()`. ([#7359](https://github.com/element-hq/element-android/issues/7359))

Other changes
-------------
 - Refactor TimelineFragment, split it into MessageComposerFragment and VoiceRecorderFragment. ([#7285](https://github.com/element-hq/element-android/issues/7285))
 - Dependency to arrow has been removed. Please use `org.matrix.android.sdk.api.util.Optional` instead. ([#7335](https://github.com/element-hq/element-android/issues/7335))
 - Update WYSIWYG editor designs. ([#7354](https://github.com/element-hq/element-android/issues/7354))
 - Update WYSIWYG library to v0.2.1. ([#7384](https://github.com/element-hq/element-android/issues/7384))


Changes in Element v1.5.2 (2022-10-05)
======================================

Features ✨
----------
 - New App Layout is now enabled by default! Go to the Settings > Labs to toggle this ([#7166](https://github.com/element-hq/element-android/issues/7166))
 - Render inline images in the timeline ([#351](https://github.com/element-hq/element-android/issues/351))
 - Add privacy setting to disable personalized learning by the keyboard ([#6633](https://github.com/element-hq/element-android/issues/6633))

Bugfixes 🐛
----------
 - Disable emoji keyboard not applies in reply ([#5029](https://github.com/element-hq/element-android/issues/5029))
 - Fix animated images not autoplaying sometimes if only a thumbnail was fetched from the server ([#6215](https://github.com/element-hq/element-android/issues/6215))
 - Add Warning shield when a user previously verified rotated their cross signing keys ([#6702](https://github.com/element-hq/element-android/issues/6702))
 - Can't verify user when option to send keys to verified devices only is selected ([#6723](https://github.com/element-hq/element-android/issues/6723))
 - Add option to only send to verified devices per room (web parity) ([#6725](https://github.com/element-hq/element-android/issues/6725))
 - Delete pin code key and the key used for biometrics authentication on logout ([#6906](https://github.com/element-hq/element-android/issues/6906))
 - Fix crash on previewing images to upload on Android Pie. ([#7184](https://github.com/element-hq/element-android/issues/7184))
 - Fix app restarts in loop on Android 13 on the first run of the app. ([#7224](https://github.com/element-hq/element-android/issues/7224))

In development 🚧
----------------
 - [Device Management] Learn more bottom sheets ([#7100](https://github.com/element-hq/element-android/issues/7100))
 - [Device management] Verify current session ([#7114](https://github.com/element-hq/element-android/issues/7114))
 - [Device management] Verify another session ([#7143](https://github.com/element-hq/element-android/issues/7143))
 - [Device management] Rename a session ([#7158](https://github.com/element-hq/element-android/issues/7158))
 - [Device Manager] Unverified and inactive sessions list ([#7170](https://github.com/element-hq/element-android/issues/7170))
 - [Device management] Sign out a session ([#7190](https://github.com/element-hq/element-android/issues/7190))
 - [Device Manager] Parse user agents ([#7247](https://github.com/element-hq/element-android/issues/7247))
 - [Voice Broadcast] Add a feature flag with the composer action ([#7258](https://github.com/element-hq/element-android/issues/7258))

Improved Documentation 📚
------------------------
 - Draft onboarding documentation of the project at `./docs/_developer_onboarding.md` ([#7126](https://github.com/element-hq/element-android/issues/7126))

SDK API changes ⚠️
------------------
 - Allow the sync timeout to be configured (mainly useful for testing) ([#7198](https://github.com/element-hq/element-android/issues/7198))
 - Ports SDK instrumentation tests to use suspending functions instead of countdown latches ([#7207](https://github.com/element-hq/element-android/issues/7207))
 - [Device Manager] Extend user agent to include device information ([#7209](https://github.com/element-hq/element-android/issues/7209))

Other changes
-------------
 - Add support for `/tableflip` command ([#12](https://github.com/element-hq/element-android/issues/12))
 - Decreases the size of rounded corners and increases the maximum width of message bubbles to help avoid unnecessary unused space on screen ([#5712](https://github.com/element-hq/element-android/issues/5712))
 - Adds screenshot testing tooling ([#5798](https://github.com/element-hq/element-android/issues/5798))
 - [AppLayout]: added tracking of new analytics events ([#6508](https://github.com/element-hq/element-android/issues/6508))
 - Target API 12 and compile with Android SDK 32. ([#6929](https://github.com/element-hq/element-android/issues/6929))
 - Add basic integration of Sentry to capture errors and crashes if user has given consent. ([#7076](https://github.com/element-hq/element-android/issues/7076))
 - Add support to `/devtools` command. ([#7126](https://github.com/element-hq/element-android/issues/7126))
 - Fix lint warning, and cleanup the code ([#7159](https://github.com/element-hq/element-android/issues/7159))
 - Mutualize the pending auth handling ([#7193](https://github.com/element-hq/element-android/issues/7193))
 - CI: Prevent modification of translations by developer. ([#7211](https://github.com/element-hq/element-android/issues/7211))
 - Fix typo in strings.xml and make sure this is American English. ([#7287](https://github.com/element-hq/element-android/issues/7287))


Changes in Element v1.5.1 (2022-09-28)
======================================

Security ⚠️
----------

This update provides important security fixes, update now.
Ref: CVE-2022-39246 CVE-2022-39248

Changes in Element v1.5.0 (2022-09-23)
======================================

Features ✨
----------
 - Deferred DMs - Enable and move the feature to labs settings ([#7180](https://github.com/element-hq/element-android/issues/7180))

Bugfixes 🐛
----------
 - Fix text margin in QR code view when no display name is set ([#5424](https://github.com/element-hq/element-android/issues/5424))
 - [App Layout] Recents carousel now scrolled to first position when new item added to or moved to this position ([#6776](https://github.com/element-hq/element-android/issues/6776))
 - Fixed problem when room list's scroll did jump after rooms placeholders were replaced with rooms summary items ([#7079](https://github.com/element-hq/element-android/issues/7079))
 - Fixes crash when quickly double clicking FABs in the new app layout ([#7102](https://github.com/element-hq/element-android/issues/7102))
 - Fixes space list and new chat bottom sheets showing too small in New App Layout (especially evident in landscape) ([#7103](https://github.com/element-hq/element-android/issues/7103))
 - [App Layout] Room leaving prompt dialog now waits user to confirm leaving before do so ([#7122](https://github.com/element-hq/element-android/issues/7122))
 - Fix empty verification bottom sheet. ([#7130](https://github.com/element-hq/element-android/issues/7130))
 - [New Layout] Fixes new chat dialog not getting dismissed after selecting its actions ([#7132](https://github.com/element-hq/element-android/issues/7132))
 - Fixes Room List not getting updated when fragment is not in focus ([#7186](https://github.com/element-hq/element-android/issues/7186))

In development 🚧
----------------
 - Create DM room only on first message - Add a spinner when sending the first message ([#6970](https://github.com/element-hq/element-android/issues/6970))
 - [Device Manager] Filter Other Sessions ([#7045](https://github.com/element-hq/element-android/issues/7045))
 - [Device management] Session details screen ([#7077](https://github.com/element-hq/element-android/issues/7077))
 - Create DM room only on first message - Fix glitch in the room list ([#7121](https://github.com/element-hq/element-android/issues/7121))
 - Create DM room only on first message - Handle the local rooms within the new AppLayout ([#7153](https://github.com/element-hq/element-android/issues/7153))

Other changes
-------------
 - [Modules] Lifts the application variants to the app module ([#6779](https://github.com/element-hq/element-android/issues/6779))
 - Ensure that we do not expect all the Event fields when requesting `rooms/{roomId}/hierarchy` endpoint. ([#7035](https://github.com/element-hq/element-android/issues/7035))
 - Move some GitHub actions to buildjet runners, and remove the second attempt to run integration tests. ([#7108](https://github.com/element-hq/element-android/issues/7108))
 - Exclude legacy android support annotation library ([#7140](https://github.com/element-hq/element-android/issues/7140))
 - Pulling no longer hosted im.dlg:android-dialer directly into the repository and removing legacy support library usages ([#7142](https://github.com/element-hq/element-android/issues/7142))
 - Fixing build cache misses when compiling the vector module ([#7157](https://github.com/element-hq/element-android/issues/7157))

Changes in Element v1.4.36 (2022-09-10)
=======================================

New App Layout can be enabled in the Labs settings. Please give it a try!

Features ✨
----------
 - Adds New App Layout into Labs ([#7038](https://github.com/element-hq/element-android/issues/7038))
 - Try to detect devices that lack Opus encoder support, use bundled libopus library for those. ([#7010](https://github.com/element-hq/element-android/issues/7010))
 - Suggest @room when @channel, @everyone, or @here is typed in composer ([#6529](https://github.com/element-hq/element-android/issues/6529))

Bugfixes 🐛
----------
 - Fix long incremental sync. ([#6917](https://github.com/element-hq/element-android/issues/6917))
 - Fix push with FCM ([#7068](https://github.com/element-hq/element-android/issues/7068))
 - FTUE - Fixes optional email registration step always being mandatory ([#6969](https://github.com/element-hq/element-android/issues/6969))
 - Fixes /addToSpace and /joinSpace commands showing invalid syntax warnings ([#6844](https://github.com/element-hq/element-android/issues/6844))
 - Fix low occurrence crashes. ([#6967](https://github.com/element-hq/element-android/issues/6967))
 - Fix crash when opening an unknown room ([#6978](https://github.com/element-hq/element-android/issues/6978))
 - Fix crash on PIN code settings screen. ([#6979](https://github.com/element-hq/element-android/issues/6979))
 - Fix autoplayed animated stickers ([#6982](https://github.com/element-hq/element-android/issues/6982))
 - Catch race condition crash in voice recording ([#6989](https://github.com/element-hq/element-android/issues/6989))
 - Fix invite to room when in a space buttons not working. ([#7054](https://github.com/element-hq/element-android/issues/7054))

In development 🚧
----------------
 - Create DM room only on first message - Create the DM and navigate to the new room after sending an event ([#5525](https://github.com/element-hq/element-android/issues/5525))
 - [App Layout] New empty states for home screen ([#6835](https://github.com/element-hq/element-android/issues/6835))
 - [App Layout] Bottom navigation tabs are removed for new home screen ([#6565](https://github.com/element-hq/element-android/issues/6565))
 - [App Layout] fixed space switching dialog measured with wrong height sometimes ([#6750](https://github.com/element-hq/element-android/issues/6750))
 - [App Layout] Fabs doesn't go off screen anymore ([#6765](https://github.com/element-hq/element-android/issues/6765))
 - [New Layout] Adds back navigation through spaces ([#6877](https://github.com/element-hq/element-android/issues/6877))
 - [App Layout] new room invites screen ([#6889](https://github.com/element-hq/element-android/issues/6889))
 - [App Layout] - Invites now show empty screen after you reject last invite ([#6876](https://github.com/element-hq/element-android/issues/6876))
 - [App Layout] - space switcher now has empty state ([#6754](https://github.com/element-hq/element-android/issues/6754))
 - [App Layout] - Improves Developer Mode Debug Button UX and adds it to New App Layout ([#6871](https://github.com/element-hq/element-android/issues/6871))
 - [New Layout] Changes space sheet to accordion-style with expandable subspaces ([#6907](https://github.com/element-hq/element-android/issues/6907))
 - [New Layout] Adds space invites ([#6924](https://github.com/element-hq/element-android/issues/6924))
 - [App Layout] fixed invites count badge bottom margin on a home screen ([#6947](https://github.com/element-hq/element-android/issues/6947))
 - [New Layout] Improves talkback accessibility ([#7016](https://github.com/element-hq/element-android/issues/7016))
 - [New Layout] Changes space icon in fab and in release notes screen ([#7039](https://github.com/element-hq/element-android/issues/7039))
 - [New Layout] Adds header to spaces bottom sheet ([#7040](https://github.com/element-hq/element-android/issues/7040))
 - [App Layout] New App Layout is enabled by default (Edit: has to be enabled in Labs) ([#6958](https://github.com/element-hq/element-android/issues/6958))
 - [App Layout] Obsolete settings are not shown when App Layout flag is enabled ([#6646](https://github.com/element-hq/element-android/issues/6646))
 - [Devices Management] Session overview screen ([#6961](https://github.com/element-hq/element-android/issues/6961))
 - [Devices Management] Refactor some code to improve testability ([#7043](https://github.com/element-hq/element-android/issues/7043))
 - [Device Manager] Current Session Section ([#6902](https://github.com/element-hq/element-android/issues/6902))
 - [Device Manager] Other Sessions Section ([#6945](https://github.com/element-hq/element-android/issues/6945))
 - [Device Manager] Render Security Recommendations ([#6964](https://github.com/element-hq/element-android/issues/6964))

Improved Documentation 📚
------------------------
 - Clarify that setting up a FCM Rewrite Proxy is not necessary for use of the UnifiedPush FCM distributor. ([#6727](https://github.com/element-hq/element-android/issues/6727))

Other changes
-------------
 - Increase sticker size ([#6982](https://github.com/element-hq/element-android/issues/6982))
 - Focus input field when editing homeserver address to speed up login and registration. ([#6926](https://github.com/element-hq/element-android/issues/6926))
 - Log basic Http information in production. ([#6925](https://github.com/element-hq/element-android/issues/6925))
 - Converts the vector module to a library with a parent vector-app application module ([#6407](https://github.com/element-hq/element-android/issues/6407))
 - Creates a dedicated strings module ([#3955](https://github.com/element-hq/element-android/issues/3955))
 - Remove FragmentModule and the Fragment factory. No need to Inject the constructor on your Fragment, just add @AndroidEntryPoint annotation and @Inject class members. ([#6894](https://github.com/element-hq/element-android/issues/6894))
 - Small refactor of UnifiedPushHelper ([#6936](https://github.com/element-hq/element-android/issues/6936))
 - CI: only run sonarqube task when token is known ([#7057](https://github.com/element-hq/element-android/issues/7057))


Changes in Element v1.4.34 (2022-08-23)
=======================================

Features ✨
----------
 - [Notification] - Handle creation of notification for live location and poll start ([#6746](https://github.com/element-hq/element-android/issues/6746))

Bugfixes 🐛
----------
 - Fixes onboarding requiring matrix.org to be accessible on the first step, the server can now be manually changed ([#6718](https://github.com/element-hq/element-android/issues/6718))
 - Fixing sign in/up for homeservers that rely on the SSO fallback url ([#6827](https://github.com/element-hq/element-android/issues/6827))
 - Fixes uncaught exceptions in the SyncWorker to cause the worker to become stuck in the failure state ([#6836](https://github.com/element-hq/element-android/issues/6836))
 - Fixes onboarding captcha crashing when no WebView is available by showing an error with information instead ([#6855](https://github.com/element-hq/element-android/issues/6855))
 - Removes ability to continue registration after the app has been destroyed, fixes the next steps crashing due to missing information from the previous steps ([#6860](https://github.com/element-hq/element-android/issues/6860))
 - Fixes crash when exiting the login or registration entry screens whilst they're loading ([#6861](https://github.com/element-hq/element-android/issues/6861))
 - Fixes server selection being unable to trust certificates ([#6864](https://github.com/element-hq/element-android/issues/6864))
 - Ensure SyncThread is started when the app is launched after a Push has been received. ([#6884](https://github.com/element-hq/element-android/issues/6884))
 - Fixes missing firebase notifications after logging in when UnifiedPush distributor is installed ([#6891](https://github.com/element-hq/element-android/issues/6891))

In development 🚧
----------------
 - Create DM room only on first message - Trigger the flow when the "Direct Message" action is selected from the room member details screen ([#5525](https://github.com/element-hq/element-android/issues/5525))
 - added filter tabs for new App layout's Home screen ([#6505](https://github.com/element-hq/element-android/issues/6505))
 - [App Layout] added dialog to configure app layout ([#6506](https://github.com/element-hq/element-android/issues/6506))
 - Adds space list bottom sheet for new app layout ([#6749](https://github.com/element-hq/element-android/issues/6749))
 - [App Layout] Dialpad moved from bottom navigation tab to a separate activity accessed via home screen context menu ([#6787](https://github.com/element-hq/element-android/issues/6787))
 - Makes toolbar switch title based on space in New App Layout ([#6795](https://github.com/element-hq/element-android/issues/6795))
 - [Devices management] Add a feature flag and empty screen for future new layout ([#6798](https://github.com/element-hq/element-android/issues/6798))
 - Adds new chat bottom sheet as the click action of the main FAB in the new app layout ([#6801](https://github.com/element-hq/element-android/issues/6801))
 - [Devices management] Other sessions section in new layout ([#6806](https://github.com/element-hq/element-android/issues/6806))
 - [New Layout] Adds space settings accessible through clicking the toolbar ([#6859](https://github.com/element-hq/element-android/issues/6859))
 - Adds New App Layout FABs (hidden behind feature flag) ([#6693](https://github.com/element-hq/element-android/issues/6693))

SDK API changes ⚠️
------------------
 - Rename `DebugService.logDbUsageInfo` (resp. `Session.logDbUsageInfo`) to `DebugService.getDbUsageInfo` (resp. `Session.getDbUsageInfo`) and return a String instead of logging. The caller may want to log the String. ([#6884](https://github.com/element-hq/element-android/issues/6884))

Other changes
-------------
 - Removes the Login2 proof of concept - replaced by the FTUE changes ([#5974](https://github.com/element-hq/element-android/issues/5974))
 - Enable auto-capitalization for Room creation Title field ([#6645](https://github.com/element-hq/element-android/issues/6645))
 - Decouples the variant logic from the vector module ([#6783](https://github.com/element-hq/element-android/issues/6783))
 - Add a developer setting to enable LeakCanary at runtime ([#6786](https://github.com/element-hq/element-android/issues/6786))
 - [Create Room] Reduce some boilerplate with room state event contents ([#6799](https://github.com/element-hq/element-android/issues/6799))
 - [Call] Memory leak after a call ([#6808](https://github.com/element-hq/element-android/issues/6808))
 - Fix some string template ([#6843](https://github.com/element-hq/element-android/issues/6843))


Changes in Element v1.4.32 (2022-08-10)
=======================================

Features ✨
----------
 - [Location Share] Render fallback UI when map fails to load ([#6711](https://github.com/element-hq/element-android/issues/6711))

Bugfixes 🐛
----------
 - Fix message content sometimes appearing in the log ([#6706](https://github.com/element-hq/element-android/issues/6706))
 - Disable 'Enable biometrics' option if there are not biometric authenticators enrolled. ([#6713](https://github.com/element-hq/element-android/issues/6713))
 - Fix crash when biometric key is used when coming back to foreground and KeyStore reports that the device is still locked. ([#6768](https://github.com/element-hq/element-android/issues/6768))
 - Catch all exceptions on lockscreen system key migrations. ([#6769](https://github.com/element-hq/element-android/issues/6769))
 - Fixes crash when entering non ascii characters during account creation ([#6735](https://github.com/element-hq/element-android/issues/6735))
 - Fixes onboarding login/account creation errors showing after navigation ([#6737](https://github.com/element-hq/element-android/issues/6737))
 - [Location sharing] Invisible text on map symbol ([#6687](https://github.com/element-hq/element-android/issues/6687))

In development 🚧
----------------
  - Adds new app layout toolbar ([#6655](https://github.com/element-hq/element-android/issues/6655))

Other changes
-------------
 - [Modularization] Provides abstraction to avoid direct usages of BuildConfig ([#6406](https://github.com/element-hq/element-android/issues/6406))
 - Refactors SpaceStateHandler (previously AppStateHandler) and adds unit tests for it ([#6598](https://github.com/element-hq/element-android/issues/6598))
 - Setup Danger to the project ([#6637](https://github.com/element-hq/element-android/issues/6637))
 - [Location Share] Open maximized map on tapping on live sharing notification ([#6642](https://github.com/element-hq/element-android/issues/6642))
 - [Location sharing] Align naming of components for live location feature ([#6647](https://github.com/element-hq/element-android/issues/6647))
 - [Location share] Update minimum sending period to 5 seconds for a live ([#6653](https://github.com/element-hq/element-android/issues/6653))
 - [Location sharing] - Fix the memory leaks ([#6674](https://github.com/element-hq/element-android/issues/6674))
 - [Timeline] Memory leak in audio message playback tracker ([#6678](https://github.com/element-hq/element-android/issues/6678))
 - [FTUE] Memory leak on FtueAuthSplashCarouselFragment ([#6680](https://github.com/element-hq/element-android/issues/6680))
 - Link directly to DCO docs from danger message. ([#6739](https://github.com/element-hq/element-android/issues/6739))


Changes in Element v1.4.31 (2022-08-01)
=======================================

Bugfixes 🐛
----------
 - Fixes crash when returning to the app after backgrounding ([#6709](https://github.com/element-hq/element-android/issues/6709))
 - Fix message content sometimes appearing in the log ([#6706](https://github.com/element-hq/element-android/issues/6706))


Changes in Element v1.4.30 (2022-07-29)
=======================================

Features ✨
----------
 - [FTUE] - Enable improved login and register onboarding flows ([#2585](https://github.com/element-hq/element-android/issues/2585))
 - Adds settings screen to change app font scale or enable using system setting ([#5687](https://github.com/element-hq/element-android/issues/5687))
 - [Location sharing] - Delete action on a live message ([#6437](https://github.com/element-hq/element-android/issues/6437))
 - [Timeline] - Collapse redacted events ([#6487](https://github.com/element-hq/element-android/issues/6487))
 - Improve lock screen implementation with extra security measures ([#6522](https://github.com/element-hq/element-android/issues/6522))
 - Move initialization of the Session to a background thread. MainActivity is restoring the session now, instead of VectorApplication. Useful when for instance a long migration of a database is required. ([#6548](https://github.com/element-hq/element-android/issues/6548))
 - Share location with other apps ([#6567](https://github.com/element-hq/element-android/issues/6567))
 - Support element call widget ([#6616](https://github.com/element-hq/element-android/issues/6616))
 - [FTUE] Updates FTUE registration to include username availability check and update copy ([#6546](https://github.com/element-hq/element-android/issues/6546))
 - [FTUE] - Allows the email address to be changed during the verification process ([#6622](https://github.com/element-hq/element-android/issues/6622))
 - [FTUE] Updates the copy within the FTUE onboarding ([#6547](https://github.com/element-hq/element-android/issues/6547))
 - [FTUE] Test session feedback ([#6620](https://github.com/element-hq/element-android/issues/6620))
 - [FTUE] - Improved reset password error message ([#6621](https://github.com/element-hq/element-android/issues/6621))

Bugfixes 🐛
----------
 - Fixes wrong voice message being displayed and played on the timeline. ([#6213](https://github.com/element-hq/element-android/issues/6213))
 - Fixes the room list not taking into account the Show all rooms in Home preference ([#6665](https://github.com/element-hq/element-android/issues/6665))
 - Stop using unstable names for withheld codes ([#5115](https://github.com/element-hq/element-android/issues/5115))
 - Fixes room not being in space after upgrade ([#6200](https://github.com/element-hq/element-android/issues/6200))
 - Fixed issues with reporting sync state events from different threads ([#6341](https://github.com/element-hq/element-android/issues/6341))
 - Display specific message when verification QR code is malformed ([#6395](https://github.com/element-hq/element-android/issues/6395))
 - When there is no way to verify a device (no 4S nor other device) propose to reset verification keys ([#6466](https://github.com/element-hq/element-android/issues/6466))
 - Unwedging could cause the SDK to force creating a new olm session every hour ([#6534](https://github.com/element-hq/element-android/issues/6534))
 - [Location Share] - Wrong room live location status bar visibility in timeline ([#6537](https://github.com/element-hq/element-android/issues/6537))
 - Fix infinite loading when opening a DM when the current room is the same DM. ([#6549](https://github.com/element-hq/element-android/issues/6549))
 - Do not log the live location of the user ([#6579](https://github.com/element-hq/element-android/issues/6579))
 - Fix backup saving several times the same keys ([#6585](https://github.com/element-hq/element-android/issues/6585))
 - Check user power level before sharing live location ([#6587](https://github.com/element-hq/element-android/issues/6587))
 - [Location Share] - Live is considered as ended while still active ([#6596](https://github.com/element-hq/element-android/issues/6596))
 - Put EC permission shortcuts behind labs flag (PSG-630) ([#6634](https://github.com/element-hq/element-android/issues/6634))
 - ObjectAnimators are not canceled in TypingMessageDotsView ([#6663](https://github.com/element-hq/element-android/issues/6663))

SDK API changes ⚠️
------------------
 - Communities/Groups are removed completely ([#5733](https://github.com/element-hq/element-android/issues/5733))
 - SDK - The SpaceFilter is query parameter is no longer nullable, use SpaceFilter.NoFilter instead ([#6666](https://github.com/element-hq/element-android/issues/6666))

Other changes
-------------
 - Nightly build publication on Firebase ([#6478](https://github.com/element-hq/element-android/issues/6478))
 - Communities/Groups are removed completely ([#5733](https://github.com/element-hq/element-android/issues/5733))
 - Improves performance on search screen by replacing flattenParents with directParentName in RoomSummary ([#6314](https://github.com/element-hq/element-android/issues/6314))
 - Log durations of DB migration and migration steps. ([#6538](https://github.com/element-hq/element-android/issues/6538))
 - [Location Share] - Standardise "Stop" texts for live ([#6541](https://github.com/element-hq/element-android/issues/6541))
 - Adds NewAppLayoutEnabled feature flag ([#6584](https://github.com/element-hq/element-android/issues/6584))
 - [Location sharing] - Small improvements of UI for live ([#6607](https://github.com/element-hq/element-android/issues/6607))
 - Live Location Sharing - Reset zoom level while focusing a user ([#6609](https://github.com/element-hq/element-android/issues/6609))
 - Fix a typo in the terms and conditions step during registration. ([#6612](https://github.com/element-hq/element-android/issues/6612))
 - [Location sharing] - OnTap on the top live status bar, display the expanded map view ([#6625](https://github.com/element-hq/element-android/issues/6625))
 - [Location Share] - Expanded map state when no more live location shares ([#6635](https://github.com/element-hq/element-android/issues/6635))


Changes in Element v1.4.28 (2022-07-13)
=======================================

Features ✨
----------
 - Improve user experience when he is first invited to a room. Users will be able to decrypt and view previous messages ([#5853](https://github.com/element-hq/element-android/issues/5853))
 - [Location sharing] - Reply action on a live message ([#6401](https://github.com/element-hq/element-android/issues/6401))
 - Show a loader if all the Room Members are not yet loaded. ([#6413](https://github.com/element-hq/element-android/issues/6413))

Bugfixes 🐛
----------
 - Fixes numbered lists always starting from 1 ([#4777](https://github.com/element-hq/element-android/issues/4777))
 - Adds LoginType to SessionParams to fix soft logout form not showing for SSO and Password type ([#5398](https://github.com/element-hq/element-android/issues/5398))
 - Use stable endpoint for alias management instead of MSC2432. Contributed by Nico. ([#6288](https://github.com/element-hq/element-android/issues/6288))
 - [Poll] Fixes visible and wrong votes in closed poll after removing 2 previous polls ([#6430](https://github.com/element-hq/element-android/issues/6430))
 - Fix HTML entities being displayed in messages ([#6442](https://github.com/element-hq/element-android/issues/6442))
 - Gallery picker can pick external images ([#6450](https://github.com/element-hq/element-android/issues/6450))
 - Fixes crash when sharing plain text, such as a url ([#6451](https://github.com/element-hq/element-android/issues/6451))
 - Fix crashes on Timeline [Thread] due to range validation ([#6461](https://github.com/element-hq/element-android/issues/6461))
 - Fix crashes when opening Thread ([#6463](https://github.com/element-hq/element-android/issues/6463))
 - Fix ConcurrentModificationException on BackgroundDetectionObserver ([#6469](https://github.com/element-hq/element-android/issues/6469))
 - Fixes inconsistency with rooms within spaces showing or disappearing from home ([#6510](https://github.com/element-hq/element-android/issues/6510))

In development 🚧
----------------
 - FTUE - Adds support for resetting the password during the FTUE onboarding journey ([#5284](https://github.com/element-hq/element-android/issues/5284))
 - Create DM room only on first message - Design implementation & debug feature flag ([#5525](https://github.com/element-hq/element-android/issues/5525))

Other changes
-------------
 - Replacing Epoxy annotation layout id references with getDefaultLayoutId ([#6389](https://github.com/element-hq/element-android/issues/6389))
 - Ensure `RealmList<T>.clearWith()` extension is correctly used. ([#6392](https://github.com/element-hq/element-android/issues/6392))
 - [Poll] - Add a description under undisclosed poll when not ended ([#6423](https://github.com/element-hq/element-android/issues/6423))
 - Add `android:hasFragileUserData="true"` in the manifest ([#6429](https://github.com/element-hq/element-android/issues/6429))
 - Add code check to prevent modification of frozen class ([#6434](https://github.com/element-hq/element-android/issues/6434))
 - Let your Activity or Fragment implement `VectorMenuProvider` if they provide a menu. ([#6436](https://github.com/element-hq/element-android/issues/6436))
 - Rename Android Service to use `AndroidService` suffix ([#6458](https://github.com/element-hq/element-android/issues/6458))


Changes in Element v1.4.27 (2022-07-06)
=======================================

Bugfixes 🐛
----------
 - Fixes crash when sharing plain text, such as a url ([#6451](https://github.com/element-hq/element-android/issues/6451))
 - Fix crashes on Timeline [Thread] due to range validation ([#6461](https://github.com/element-hq/element-android/issues/6461))
 - Fix crashes when opening Thread ([#6463](https://github.com/element-hq/element-android/issues/6463))
 - Fix ConcurrentModificationException on BackgroundDetectionObserver ([#6469](https://github.com/element-hq/element-android/issues/6469))


Changes in Element v1.4.26 (2022-06-30)
=======================================

Features ✨
----------
 - Use UnifiedPush and allows user to have push without FCM. ([#3448](https://github.com/element-hq/element-android/issues/3448))
 - Replace ffmpeg-kit with libopus and libopusenc. ([#6203](https://github.com/element-hq/element-android/issues/6203))
 - Improve lock screen implementation. ([#6217](https://github.com/element-hq/element-android/issues/6217))
 - Allow sharing text based content via android's share menu (eg .ics files) ([#6285](https://github.com/element-hq/element-android/issues/6285))
 - Promote live location labs flag ([#6350](https://github.com/element-hq/element-android/issues/6350))
 - [Location sharing] - Stop any active live before starting a new one ([#6364](https://github.com/element-hq/element-android/issues/6364))
 - Expose pusher profile tag in advanced settings ([#6369](https://github.com/element-hq/element-android/issues/6369))

Bugfixes 🐛
----------
 - Fixes concurrent modification crash when signing out or launching the app ([#5821](https://github.com/element-hq/element-android/issues/5821))
 - Refactor - better naming, return native user id and not sip user id and create a dm with the native user instead of with the sip user. ([#6101](https://github.com/element-hq/element-android/issues/6101))
 - Fixed /upgraderoom command not doing anything ([#6154](https://github.com/element-hq/element-android/issues/6154))
 - Fixed crash when opening large images in the timeline ([#6290](https://github.com/element-hq/element-android/issues/6290))
 - [Location sharing] Fix crash when starting/stopping a live when offline ([#6315](https://github.com/element-hq/element-android/issues/6315))
 - Fix loop in timeline and simplify management of chunks and timeline events. ([#6318](https://github.com/element-hq/element-android/issues/6318))
 - Update design and behaviour on widget permission bottom sheet ([#6326](https://github.com/element-hq/element-android/issues/6326))
 - Fix | Some user verification requests couldn't be accepted/declined ([#6328](https://github.com/element-hq/element-android/issues/6328))
 - [Location sharing] Fix stop of a live not possible from another device ([#6349](https://github.com/element-hq/element-android/issues/6349))
 - Fix backslash escapes in formatted messages ([#6357](https://github.com/element-hq/element-android/issues/6357))
 - Fixes wrong error message when signing in with wrong credentials ([#6371](https://github.com/element-hq/element-android/issues/6371))
 - [Location Share] - Adding missing prefix "u=" for uncertainty in geo URI ([#6375](https://github.com/element-hq/element-android/issues/6375))

In development 🚧
----------------
 - FTUE - Adds automatic homeserver selection when typing a full matrix id during registration or login ([#6162](https://github.com/element-hq/element-android/issues/6162))

Improved Documentation 📚
------------------------
 - Update the PR process doc to come back to one reviewer with optional additional reviewers. ([#6396](https://github.com/element-hq/element-android/issues/6396))

SDK API changes ⚠️
------------------
 - Group all location sharing related API into LocationSharingService ([#5864](https://github.com/element-hq/element-android/issues/5864))
 - Add support for MSC2457 - opting in or out of logging out all devices when changing password ([#6191](https://github.com/element-hq/element-android/issues/6191))
 - Create `QueryStateEventValue` to do query on `stateKey` for State Event. Also remove the default parameter values for those type. ([#6319](https://github.com/element-hq/element-android/issues/6319))

Other changes
-------------
 - - Notify of the latest known location in LocationTracker to avoid multiple locations at start
  - Debounce location updates
  - Improve location providers access ([#5913](https://github.com/element-hq/element-android/issues/5913))
 - Add unit tests for LiveLocationAggregationProcessor code ([#6155](https://github.com/element-hq/element-android/issues/6155))
 - Making screenshots in bug reports opt in instead of opt out ([#6261](https://github.com/element-hq/element-android/issues/6261))
 - Setup [Flipper](https://fbflipper.com/) ([#6300](https://github.com/element-hq/element-android/issues/6300))
 - CreatePollViewModel unit tests ([#6320](https://github.com/element-hq/element-android/issues/6320))
 - Fix flaky test in voice recording feature. ([#6329](https://github.com/element-hq/element-android/issues/6329))
 - Poll view state unit tests ([#6366](https://github.com/element-hq/element-android/issues/6366))
 - Let LoadRoomMembersTask insert by chunk to release db. ([#6394](https://github.com/element-hq/element-android/issues/6394))


Changes in Element v1.4.25 (2022-06-27)
=======================================

Bugfixes 🐛
----------
- Second attempt to fix session database migration to version 30.

Changes in Element v1.4.24 (2022-06-22)
=======================================

Bugfixes 🐛
----------
- First attempt to fix session database migration to version 30.

Changes in Element v1.4.23 (2022-06-21)
=======================================

Bugfixes 🐛
----------
 - Fix loop in timeline and simplify management of chunks and timeline events. ([#6318](https://github.com/element-hq/element-android/issues/6318))


Changes in Element v1.4.22 (2022-06-14)
=======================================

Features ✨
----------
 - Make read receipt avatar list more compact ([#5970](https://github.com/element-hq/element-android/issues/5970))
 - Allow .well-known configuration to override key sharing mode ([#6147](https://github.com/element-hq/element-android/issues/6147))
 - Re-organize location settings flags ([#6244](https://github.com/element-hq/element-android/issues/6244))
 - Add report action for live location messages ([#6280](https://github.com/element-hq/element-android/issues/6280))

Bugfixes 🐛
----------
 - Fix cases of missing, swapped, or duplicated messages ([#5528](https://github.com/element-hq/element-android/issues/5528))
 - Fix wrong status of live location sharing in timeline ([#6209](https://github.com/element-hq/element-android/issues/6209))
 - Fix StackOverflowError while recording voice message ([#6222](https://github.com/element-hq/element-android/issues/6222))
 - Text cropped: "Secure backup" ([#6232](https://github.com/element-hq/element-android/issues/6232))
 - Fix copyright attributions of map views ([#6247](https://github.com/element-hq/element-android/issues/6247))
 - Fix flickering bottom bar of live location item ([#6264](https://github.com/element-hq/element-android/issues/6264))

In development 🚧
----------------
 - FTUE - Adds Sign Up tracking ([#5285](https://github.com/element-hq/element-android/issues/5285))

SDK API changes ⚠️
------------------
 - Some methods from `Session` have been moved to a new `SyncService`, that you can retrieve from a `Session`.
  - `SyncStatusService` method has been moved to the new `SyncService`
  - `InitSyncStep` have been moved and renamed to `InitialSyncStep`
  - `SyncStatusService.Status` has been renamed to `SyncRequestState`
  - The existing `SyncService` has been renamed to `SyncAndroidService` because of name clash with the new SDK Service ([#6029](https://github.com/element-hq/element-android/issues/6029))
 - Allows `AuthenticationService.getLoginFlow` to fail without resetting state from previously successful calls ([#6093](https://github.com/element-hq/element-android/issues/6093))
 - Allows new passwords to be passed at the point of confirmation when resetting a password ([#6169](https://github.com/element-hq/element-android/issues/6169))

Other changes
-------------
 - Adds support for parsing homeserver versions without a patch number ([#6017](https://github.com/element-hq/element-android/issues/6017))
 - Updating exit onboarding dialog copy formatting to match iOS ([#6087](https://github.com/element-hq/element-android/issues/6087))
 - Disables when arrow alignment in code style ([#6126](https://github.com/element-hq/element-android/issues/6126))


Changes in Element 1.4.20 (2022-06-13)
======================================

Bugfixes 🐛
----------
 - Fix: All rooms are shown in Home regardless of the switch state. ([#6272](https://github.com/element-hq/element-android/issues/6272))
 - Fix regression on EventInsertLiveObserver getting blocked so there is no event being processed anymore. ([#6278](https://github.com/element-hq/element-android/issues/6278))


Changes in Element 1.4.19 (2022-06-07)
======================================

Bugfixes 🐛
----------
 - Fix | performance regression on roomlist + proper display of space parents in explore rooms. ([#6233](https://github.com/element-hq/element-android/issues/6233))


Changes in Element v1.4.18 (2022-05-31)
=======================================

Features ✨
----------
 - Space explore screen changes: removed space card, added rooms filtering ([#5658](https://github.com/element-hq/element-android/issues/5658))
 - Adds space or user id as a subtitle under rooms in search ([#5860](https://github.com/element-hq/element-android/issues/5860))
 - Adds up navigation in spaces ([#6073](https://github.com/element-hq/element-android/issues/6073))
 - Labs flag for enabling live location sharing ([#6098](https://github.com/element-hq/element-android/issues/6098))
 - Added support for mandatory backup or passphrase from .well-known configuration. ([#6133](https://github.com/element-hq/element-android/issues/6133))
 - Security - Asking for user confirmation when tapping URLs which contain unicode directional overrides ([#6163](https://github.com/element-hq/element-android/issues/6163))
 - Add settings switch to allow autoplaying animated images ([#6166](https://github.com/element-hq/element-android/issues/6166))
 - Live Location Sharing - User List Bottom Sheet ([#6170](https://github.com/element-hq/element-android/issues/6170))

Bugfixes 🐛
----------
 - Fix some notifications not clearing when read ([#4862](https://github.com/element-hq/element-android/issues/4862))
 - Do not switch away from home space on notification when "Show all Rooms in Home" is selected. ([#5827](https://github.com/element-hq/element-android/issues/5827))
 - Use fixed text size in read receipt counter ([#5856](https://github.com/element-hq/element-android/issues/5856))
 - Revert: Use member name instead of room name in DM creation item ([#6032](https://github.com/element-hq/element-android/issues/6032))
 - Poll refactoring with unit tests ([#6074](https://github.com/element-hq/element-android/issues/6074))
 - Correct .well-known/matrix/client handling for server_names which include ports. ([#6095](https://github.com/element-hq/element-android/issues/6095))
 - Glide - Use current drawable while loading new static map image ([#6103](https://github.com/element-hq/element-android/issues/6103))
 - Fix sending multiple invites to a room reaching only one or two people ([#6109](https://github.com/element-hq/element-android/issues/6109))
 - Prevent widget web view from reloading on screen / orientation change ([#6140](https://github.com/element-hq/element-android/issues/6140))
 - Fix decrypting redacted event from sending errors ([#6148](https://github.com/element-hq/element-android/issues/6148))
 - Make widget web view request system permissions for camera and microphone (PSF-1061) ([#6149](https://github.com/element-hq/element-android/issues/6149))

In development 🚧
----------------
 - Adds email input and verification screens to the new FTUE onboarding flow ([#5278](https://github.com/element-hq/element-android/issues/5278))
 - FTUE - Adds the redesigned Sign In screen ([#5283](https://github.com/element-hq/element-android/issues/5283))
 - [Live location sharing] Update message in timeline during the live ([#5689](https://github.com/element-hq/element-android/issues/5689))
 - FTUE - Overrides sign up flow ordering for matrix.org only ([#5783](https://github.com/element-hq/element-android/issues/5783))
 - Live location sharing: navigation from timeline to map screen
  Live location sharing: show user pins on map screen ([#6012](https://github.com/element-hq/element-android/issues/6012))
 - FTUE - Adds homeserver login/register deeplink support ([#6023](https://github.com/element-hq/element-android/issues/6023))
 - [Live location sharing] Update entity in DB when a live is timed out ([#6123](https://github.com/element-hq/element-android/issues/6123))

SDK API changes ⚠️
------------------
- Notifies other devices when a verification request sent from an Android device is accepted.` ([#5724](https://github.com/element-hq/element-android/issues/5724))
- Some `val` have been changed to `fun` to increase their visibility in the generated documentation. Just add `()` if you were using them.
- `KeysBackupService.state` has been replaced by `KeysBackupService.getState()`
- `KeysBackupService.isStucked` has been replaced by `KeysBackupService.isStuck()`
- SDK documentation improved ([#5952](https://github.com/element-hq/element-android/issues/5952))
- Improve replay attacks and reduce duplicate message index errors ([#6077](https://github.com/element-hq/element-android/issues/6077))
- Remove `RoomSummaryQueryParams.roomId`. If you need to observe a single room, use the new API `RoomService.getRoomSummaryLive(roomId: String)`
- `ActiveSpaceFilter` has been renamed to `SpaceFilter`
- `RoomCategoryFilter.ALL` has been removed, just pass `null` to not filter on Room category. ([#6143](https://github.com/element-hq/element-android/issues/6143))

Other changes
-------------
 - leaving space experience changed to be aligned with iOS ([#5728](https://github.com/element-hq/element-android/issues/5728))
 - @Ignore a number of tests that are currently failing in CI. ([#6025](https://github.com/element-hq/element-android/issues/6025))
 - Remove ShortcutBadger lib and usage (it was dead code) ([#6041](https://github.com/element-hq/element-android/issues/6041))
 - Test: Ensure calling 'fail()' is not caught by the catch block ([#6089](https://github.com/element-hq/element-android/issues/6089))
 - Excludes transitive optional non FOSS google location dependency from fdroid builds ([#6100](https://github.com/element-hq/element-android/issues/6100))
 - Fixed grammar errors in /vector/src/main/res/values/strings.xml ([#6132](https://github.com/element-hq/element-android/issues/6132))
 - Downgrade gradle from 7.2.0 to 7.1.3 ([#6141](https://github.com/element-hq/element-android/issues/6141))
 - Add Lao language to the in-app settings. ([#6196](https://github.com/element-hq/element-android/issues/6196))
 - Remove the background location permission request ([#6198](https://github.com/element-hq/element-android/issues/6198))


Changes in Element v1.4.16 (2022-05-17)
=======================================

Features ✨
----------
 - Use key backup before requesting keys + refactor & improvement of key request/forward ([#5494](https://github.com/element-hq/element-android/issues/5494))
 - Screen sharing over WebRTC ([#5911](https://github.com/element-hq/element-android/issues/5911))
 - Allow using the latest user Avatar and name for all messages in the timeline ([#5932](https://github.com/element-hq/element-android/issues/5932))
 - Added themed launch icons for Android 13 ([#5936](https://github.com/element-hq/element-android/issues/5936))
 - Add presence indicator busy and away. ([#6047](https://github.com/element-hq/element-android/issues/6047))

Bugfixes 🐛
----------
 - Changed copy and list order in member profile screen. ([#5825](https://github.com/element-hq/element-android/issues/5825))
 - Fix for audio only being received in one direction after an un-hold during a sip call. ([#5865](https://github.com/element-hq/element-android/issues/5865))
 - Desynchronized 4S | Megolm backup causing Unusable backup ([#5906](https://github.com/element-hq/element-android/issues/5906))
 - If animations are disable on the System, chat effects and confetti will be disabled too ([#5941](https://github.com/element-hq/element-android/issues/5941))
 - Multiple threads improvement (mainly UI) ([#5959](https://github.com/element-hq/element-android/issues/5959))

Improved Documentation 📚
------------------------
 - Note public_baseurl requirement in integration tests documentation. ([#5973](https://github.com/element-hq/element-android/issues/5973))

SDK API changes ⚠️
------------------
 - - New API to enable/disable key forwarding CryptoService#enableKeyGossiping()
  - New API to limit room key request only to own devices MXCryptoConfig#limitRoomKeyRequestsToMyDevices
  - Event Trail API has changed, now using AuditTrail events
  - New API to manually accept an incoming key request CryptoService#manuallyAcceptRoomKeyRequest() ([#5559](https://github.com/element-hq/element-android/issues/5559))
 - Small change in the Matrix class: deprecated methods have been removed and the constructor is now public. Also the fun `workerFactory()` has been renamed to `getWorkerFactory()` ([#5887](https://github.com/element-hq/element-android/issues/5887))
 - Including SSL/TLS error handing when doing WellKnown lookups without a custom HomeServerConnectionConfig ([#5965](https://github.com/element-hq/element-android/issues/5965))

Other changes
-------------
 - Improve threads rendering in the main timeline ([#5151](https://github.com/element-hq/element-android/issues/5151))
 - Reformatted project code ([#5953](https://github.com/element-hq/element-android/issues/5953))
 - Update check for server-side threads support to match spec. ([#5997](https://github.com/element-hq/element-android/issues/5997))
 - Setup detekt ([#6038](https://github.com/element-hq/element-android/issues/6038))
 - Notify the user for each new message ([#4632](https://github.com/element-hq/element-android/issues/4632))


Changes in Element v1.4.14 (2022-05-05)
=======================================

Features ✨
----------
 - Improve management of ignored users ([#5772](https://github.com/element-hq/element-android/issues/5772))
 - VoIP Screen Sharing Permission ([#5811](https://github.com/element-hq/element-android/issues/5811))
 - Live location sharing: updating beacon state event content structure ([#5814](https://github.com/element-hq/element-android/issues/5814))

Bugfixes 🐛
----------
 - Fixes crash when accepting or receiving VOIP calls ([#5421](https://github.com/element-hq/element-android/issues/5421))
 - Improve/fix crashes on messages decryption ([#5592](https://github.com/element-hq/element-android/issues/5592))
 - Tentative fix of images crashing when being sent or shared from gallery ([#5652](https://github.com/element-hq/element-android/issues/5652))
 - Improving deactivation experience along with a crash fix ([#5721](https://github.com/element-hq/element-android/issues/5721))
 - Adds missing suggested tag for rooms in Explore Space ([#5826](https://github.com/element-hq/element-android/issues/5826))
 - Fixes missing call icons when threads are enabled ([#5847](https://github.com/element-hq/element-android/issues/5847))
 - Fix UX freezing when creating secure backup ([#5871](https://github.com/element-hq/element-android/issues/5871))
 - Fixes sign in via other requiring homeserver registration to be enabled ([#5874](https://github.com/element-hq/element-android/issues/5874))
 - Don't pause timer when call is held. ([#5885](https://github.com/element-hq/element-android/issues/5885))
 - Fix UISIDetector grace period bug ([#5886](https://github.com/element-hq/element-android/issues/5886))
 - Fix a crash with space invitations in the space list, and do not display space invitation twice. ([#5924](https://github.com/element-hq/element-android/issues/5924))
 - Fixes crash on android api 21/22 devices when opening messages due to Konfetti library ([#5925](https://github.com/element-hq/element-android/issues/5925))

In development 🚧
----------------
 - Reorders the registration steps to prioritise email, then terms for the FTUE onboarding ([#5783](https://github.com/element-hq/element-android/issues/5783))
 - [Live location sharing] Improve aggregation process of events ([#5862](https://github.com/element-hq/element-android/issues/5862))

Improved Documentation 📚
------------------------
 - Update the PR process doc with 2 reviewers and a new reviewer team. ([#5836](https://github.com/element-hq/element-android/issues/5836))
 - Improve documentation of the project and of the SDK ([#5854](https://github.com/element-hq/element-android/issues/5854))

SDK API changes ⚠️
------------------
 - Added registrationCustom into RegistrationWizard to send custom auth params for sign up
 - Moved terms converter into api package to make it accessible in sdk ([#5575](https://github.com/element-hq/element-android/issues/5575))
 - Move package `org.matrix.android.sdk.api.pushrules` to `org.matrix.android.sdk.api.session.pushrules` ([#5812](https://github.com/element-hq/element-android/issues/5812))
 - Some `Session` apis are now available by requesting the service first. For instance `Session.updateAvatar(...)` is now `Session.profileService().updateAvatar(...)`
 - The shortcut `Room.search()` has been removed, you have to use `Session.searchService().search()` ([#5816](https://github.com/element-hq/element-android/issues/5816))
 - Add return type to RoomApi.sendStateEvent() to retrieve the created event id ([#5855](https://github.com/element-hq/element-android/issues/5855))
 - `Room` apis are now available by requesting the service first. For instance `Room.updateAvatar(...)` is now `Room.stateService().updateAvatar(...)` ([#5858](https://github.com/element-hq/element-android/issues/5858))
 - Remove unecessary field `eventId` from `EventAnnotationsSummary` and `ReferencesAggregatedSummary` ([#5890](https://github.com/element-hq/element-android/issues/5890))
 - Replace usage of `System.currentTimeMillis()` by a `Clock` interface ([#5907](https://github.com/element-hq/element-android/issues/5907))

Other changes
-------------
 - Move "Ignored users" setting section into "Security & Privacy" ([#5773](https://github.com/element-hq/element-android/issues/5773))
 - Add a picto for ignored users in the room member list screen ([#5774](https://github.com/element-hq/element-android/issues/5774))
 - Autoformats entire project ([#5805](https://github.com/element-hq/element-android/issues/5805))
 - Add a GH workflow to push ElementX issues to the global board. ([#5832](https://github.com/element-hq/element-android/issues/5832))
 - Faster Olm decrypt when there is a lot of existing sessions ([#5872](https://github.com/element-hq/element-android/issues/5872))


Changes in Element 1.4.13 (2022-04-26)
======================================

Bugfixes 🐛
----------
 - Fix UI freeze observed after each incremental sync ([#5835](https://github.com/element-hq/element-android/issues/5835))


Changes in Element v1.4.12 (2022-04-20)
=======================================

Features ✨
----------
 - Add a setting to be able to always appear offline ([#5582](https://github.com/element-hq/element-android/issues/5582))
 - Adds the ability for audio attachments to be played in the timeline ([#5586](https://github.com/element-hq/element-android/issues/5586))
 - Do not cancel the current incremental sync request and treatment when the app goes to background ([#5719](https://github.com/element-hq/element-android/issues/5719))
 - Improve user experience when home servers do not yet support threads ([#5761](https://github.com/element-hq/element-android/issues/5761))

Bugfixes 🐛
----------
 - Added text next to spinner when loading information after user is clicked on space members screen ([#4305](https://github.com/element-hq/element-android/issues/4305))
 - The string `ftue_auth_carousel_workplace_body` was declared not translatable by mistake ([#5262](https://github.com/element-hq/element-android/issues/5262))
 - Fix some cases where the read marker line would not show up ([#5475](https://github.com/element-hq/element-android/issues/5475))
 - Fix sometimes read marker not properly updating ([#5481](https://github.com/element-hq/element-android/issues/5481))
 - Fix sometimes endless loading timeline ([#5554](https://github.com/element-hq/element-android/issues/5554))
 - Use member name instead of room name in DM creation item ([#5570](https://github.com/element-hq/element-android/issues/5570))
 - Align auto-reporting of decryption errors implementation with web client. ([#5596](https://github.com/element-hq/element-android/issues/5596))
 - Choosing "leave all rooms and spaces" while leaving Space won't cause leaving DMs in this Space anymore ([#5609](https://github.com/element-hq/element-android/issues/5609))
 - Fixes display name being changed when using /myroomnick ([#5618](https://github.com/element-hq/element-android/issues/5618))
 - Fix endless loading if the event from a permalink is not found ([#5659](https://github.com/element-hq/element-android/issues/5659))
 - Redacted events are no longer visible. ([#5707](https://github.com/element-hq/element-android/issues/5707))
 - Don't wrongly show non-space invites in the space panel. ([#5731](https://github.com/element-hq/element-android/issues/5731))
 - Fixes the onboarding confetti rendering behind the content instead of in-front ([#5735](https://github.com/element-hq/element-android/issues/5735))
 - Fixes crash when navigating the app whilst processing new room keys ([#5746](https://github.com/element-hq/element-android/issues/5746))
 - Fix sorting of uploads in encrypted rooms ([#5757](https://github.com/element-hq/element-android/issues/5757))
 - Fixing setting transfer title in call transfer. ([#5765](https://github.com/element-hq/element-android/issues/5765))
 - Changes destination after joining a space to Explore Space Rooms screen ([#5766](https://github.com/element-hq/element-android/issues/5766))
 - Unignoring a user will perform an initial sync ([#5767](https://github.com/element-hq/element-android/issues/5767))
 - Open a room by link: use the actual roomId instead of the alias ([#5786](https://github.com/element-hq/element-android/issues/5786))

In development 🚧
----------------
 - FTUE - Adds a new homeserver selection screen when creating an account ([#2396](https://github.com/element-hq/element-android/issues/2396))
 - FTUE - Updates the Captcha and T&Cs registration screens UI style ([#5279](https://github.com/element-hq/element-android/issues/5279))
 - FTUE - Adds error handling within the server selection screen ([#5749](https://github.com/element-hq/element-android/issues/5749))
 - Live Location Sharing - Send location data ([#5697](https://github.com/element-hq/element-android/issues/5697))
 - Live Location Sharing - Show message on start of a live ([#5710](https://github.com/element-hq/element-android/issues/5710))
 - Live Location Sharing - Attach location data to beacon info state event ([#5711](https://github.com/element-hq/element-android/issues/5711))
 - Live Location Sharing - Update beacon info state event when sharing is ended ([#5758](https://github.com/element-hq/element-android/issues/5758))


SDK API changes ⚠️
------------------
 - Include original event in live decryption listeners and update sync status naming to InitialSyncProgressing for clarity. ([#5639](https://github.com/element-hq/element-android/issues/5639))
 - KeysBackupService.getCurrentVersion takes a new type `KeysBackupLastVersionResult` in the callback. ([#5703](https://github.com/element-hq/element-android/issues/5703))
 - A lot of classes which were exposed to the clients and were located in the package `org.matrix.android.sdk.internal` have been moved to the package `org.matrix.android.sdk.api`.
  All the classes which are in the package `org.matrix.android.sdk.internal` should now be declared `internal`.
  Some unused code and classes have been removed. ([#5744](https://github.com/element-hq/element-android/issues/5744))
 - Some data classes are now immutable, using `val` instead of `var` ([#5762](https://github.com/element-hq/element-android/issues/5762))

Other changes
-------------
 - Upgrade konfetti lib from 1.3.2 to 2.0.2 ([#5079](https://github.com/element-hq/element-android/issues/5079))
 - Spaces feedback section is removed from left panel ([#5486](https://github.com/element-hq/element-android/issues/5486))
 - Reduce error logs ([#5703](https://github.com/element-hq/element-android/issues/5703))
 - Adds a complete editor config file for our current code style ([#5727](https://github.com/element-hq/element-android/issues/5727))
 - Updates the posthog dev environment url and api key ([#5732](https://github.com/element-hq/element-android/issues/5732))
 - Setup Dokka to be able to generate documentation of the SDK module. Run `./gradlew matrix-sdk-android:dokkaHtml` to do it. ([#5744](https://github.com/element-hq/element-android/issues/5744))


Changes in Element v1.4.11 (2022-04-07)
=======================================

Bugfixes 🐛
----------
 - Choosing "leave all rooms and spaces" while leaving Space won't cause leaving DMs in this Space anymore ([#5609](https://github.com/element-hq/element-android/issues/5609))


Changes in Element v1.4.10 (2022-04-05)
=======================================

Features ✨
----------
 - Allow scrolling position of Voice Message playback ([#5426](https://github.com/element-hq/element-android/issues/5426))
 - Users will be able to provide feedback for threads ([#5647](https://github.com/element-hq/element-android/issues/5647))
 - Update Jitsi lib from 3.10.0 to 5.0.2 ([#5654](https://github.com/element-hq/element-android/issues/5654))

Bugfixes 🐛
----------
 - Replace "open settings" button by "disable" action in RageShake dialog if there is no session ([#4445](https://github.com/element-hq/element-android/issues/4445))
 - Fixes room summaries showing encrypted content after verifying device ([#4867](https://github.com/element-hq/element-android/issues/4867))
 - Fixes polls being votable after being ended ([#5473](https://github.com/element-hq/element-android/issues/5473))
 - [Subscribing] Blank display name ([#5497](https://github.com/element-hq/element-android/issues/5497))
 - Fixes voice call button disappearing in DM rooms with more than 2 members ([#5548](https://github.com/element-hq/element-android/issues/5548))
 - Add loader in thread list ([#5562](https://github.com/element-hq/element-android/issues/5562))
 - Fixed key export when overwriting existing files ([#5663](https://github.com/element-hq/element-android/issues/5663))

In development 🚧
----------------
 - Adding combined account creation and server selection screen as part of the new FTUE ([#5277](https://github.com/element-hq/element-android/issues/5277))
 - Finalising FTUE onboarding account creation personalization steps but keeping feature disabled until other parts are complete ([#5519](https://github.com/element-hq/element-android/issues/5519))
 - Live Location Sharing - Foreground Service and Notification ([#5595](https://github.com/element-hq/element-android/issues/5595))
 - Send beacon info state event when live location sharing started ([#5651](https://github.com/element-hq/element-android/issues/5651))
 - Show a banner in timeline while location sharing service is running ([#5660](https://github.com/element-hq/element-android/issues/5660))
 - Location sharing: adding possibility to choose duration of live sharing ([#5667](https://github.com/element-hq/element-android/issues/5667))

Other changes
-------------
 - Improve main timeline thread summary rendering ([#5151](https://github.com/element-hq/element-android/issues/5151))
 - "Add space" copy is replaced with "create space" in left sliding panel ([#5516](https://github.com/element-hq/element-android/issues/5516))
 - Flattening the asynchronous onboarding state and passing all errors through the same pipeline ([#5517](https://github.com/element-hq/element-android/issues/5517))
 - Changed items order in space menu. Changed capitalization for "space" in those items ([#5524](https://github.com/element-hq/element-android/issues/5524))
 - Permalinks to root thread messages will now navigate you within the thread timeline ([#5567](https://github.com/element-hq/element-android/issues/5567))
 - Live location sharing: adding way to override feature activation in debug ([#5581](https://github.com/element-hq/element-android/issues/5581))
 - Adds unit tests around the login with matrix id flow ([#5628](https://github.com/element-hq/element-android/issues/5628))
 - Setup the plugin org.owasp.dependencycheck ([#5654](https://github.com/element-hq/element-android/issues/5654))
 - Implement threads beta opt-in mechanism to notify users about threads ([#5692](https://github.com/element-hq/element-android/issues/5692))


Changes in Element v1.4.8 (2022-03-28)
======================================

Other changes
-------------
 - Moving live location sharing permission to debug only builds whilst it is WIP ([#5636](https://github.com/element-hq/element-android/issues/5636))


Changes in Element v1.4.7 (2022-03-24)
======================================

Bugfixes 🐛
----------
 - Fix inconsistencies between the arrow visibility and the collapse action on the room sections ([#5616](https://github.com/element-hq/element-android/issues/5616))
 - Fix room list header count flickering

Changes in Element v1.4.6 (2022-03-23)
======================================

Features ✨
----------
 - Thread timeline is now live and much faster especially for large or old threads ([#5230](https://github.com/element-hq/element-android/issues/5230))
 - View all threads per room screen is now live when the home server supports threads ([#5232](https://github.com/element-hq/element-android/issues/5232))
 - Add a custom view to display a picker for share location options ([#5395](https://github.com/element-hq/element-android/issues/5395))
 - Add ability to pin a location on map for sharing ([#5417](https://github.com/element-hq/element-android/issues/5417))
 - Poll Integration Tests ([#5522](https://github.com/element-hq/element-android/issues/5522))
 - Live location sharing: adding build config field and show permission dialog ([#5536](https://github.com/element-hq/element-android/issues/5536))
 - Live location sharing: Adding indicator view when enabled ([#5571](https://github.com/element-hq/element-android/issues/5571))

Bugfixes 🐛
----------
 - Poll system notifications on Android are not user friendly ([#4780](https://github.com/element-hq/element-android/issues/4780))
 - Add colors for shield vector drawable ([#4860](https://github.com/element-hq/element-android/issues/4860))
 - Support both stable and unstable prefixes for Events about Polls and Location ([#5340](https://github.com/element-hq/element-android/issues/5340))
 - Fix missing messages when loading messages forwards ([#5448](https://github.com/element-hq/element-android/issues/5448))
 - Fix presence indicator being aligned to the center of the room image ([#5489](https://github.com/element-hq/element-android/issues/5489))
 - Read receipt in wrong order ([#5514](https://github.com/element-hq/element-android/issues/5514))
 - Fix mentions using matrix.to rather than client defined permalink base url ([#5521](https://github.com/element-hq/element-android/issues/5521))
 - Fixes crash when tapping the timeline verification surround box instead of the buttons ([#5540](https://github.com/element-hq/element-android/issues/5540))
 - [Notification mode] Wrong mode is displayed when the mention only is selected on the web client ([#5547](https://github.com/element-hq/element-android/issues/5547))
 - Fix local echos not being shown when re-opening rooms ([#5551](https://github.com/element-hq/element-android/issues/5551))
 - Fix crash when closing a room while decrypting timeline events ([#5552](https://github.com/element-hq/element-android/issues/5552))
 - Fix sometimes read marker not properly updating ([#5564](https://github.com/element-hq/element-android/issues/5564))

In development 🚧
----------------
 - Dynamically showing/hiding onboarding personalisation screens based on the users homeserver capabilities ([#5375](https://github.com/element-hq/element-android/issues/5375))
 - Introduces FTUE personalisation complete screen along with confetti celebration ([#5389](https://github.com/element-hq/element-android/issues/5389))

SDK API changes ⚠️
------------------
 - Adds support for MSC3440, additional threads homeserver capabilities ([#5271](https://github.com/element-hq/element-android/issues/5271))

Other changes
-------------
 - Refactoring for safer olm and megolm session usage ([#5380](https://github.com/element-hq/element-android/issues/5380))
 - Improve headers UI in Rooms/Messages lists ([#4533](https://github.com/element-hq/element-android/issues/4533))
 - Number of unread messages on space badge now include number of unread DMs ([#5260](https://github.com/element-hq/element-android/issues/5260))
 - Amend spaces menu to be consistent with iOS version ([#5270](https://github.com/element-hq/element-android/issues/5270))
 - Selected space highlight changed in left panel ([#5346](https://github.com/element-hq/element-android/issues/5346))
 - [Rooms list] Do not suggest collapse the unique section ([#5347](https://github.com/element-hq/element-android/issues/5347))
 - Add analytics support for threads ([#5378](https://github.com/element-hq/element-android/issues/5378))
 - Add top margin before our first message ([#5384](https://github.com/element-hq/element-android/issues/5384))
 - Improved onboarding registration unit test coverage ([#5408](https://github.com/element-hq/element-android/issues/5408))
 - Adds stable room hierarchy endpoint with a fallback to the unstable one ([#5443](https://github.com/element-hq/element-android/issues/5443))
 - Use ColorPrimary for attachmentGalleryButton tint ([#5501](https://github.com/element-hq/element-android/issues/5501))
 - Added online presence indicator attribute online to match offline styling ([#5513](https://github.com/element-hq/element-android/issues/5513))
 - Add a presence sync enabling build config ([#5563](https://github.com/element-hq/element-android/issues/5563))
 - Show stickers on click ([#5572](https://github.com/element-hq/element-android/issues/5572))


Changes in Element v1.4.4 (2022-03-09)
======================================

Features ✨
----------
 - Adds animated typing indicator to the bottom of the timeline ([#3296](https://github.com/element-hq/element-android/issues/3296))
 - Removes the topic and typing information from the room's top bar ([#4642](https://github.com/element-hq/element-android/issues/4642))
 - Add possibility to save media from Gallery + reorder choices in message context menu ([#5005](https://github.com/element-hq/element-android/issues/5005))
 - Improves settings error dialog messaging when changing avatar or display name fails ([#5418](https://github.com/element-hq/element-android/issues/5418))
 
Bugfixes 🐛
----------
 - Open direct message screen when clicking on DM button in the space members list ([#4319](https://github.com/element-hq/element-android/issues/4319))
 - Fix incorrect media cache size in settings ([#5394](https://github.com/element-hq/element-android/issues/5394))
 - Setting an avatar when creating a room had no effect ([#5402](https://github.com/element-hq/element-android/issues/5402))
 - Fix reactions summary crash when reopening a room ([#5463](https://github.com/element-hq/element-android/issues/5463))
 - Fixing room titles overlapping the room image in the room toolbar ([#5468](https://github.com/element-hq/element-android/issues/5468))

In development 🚧
----------------
 - Starts the FTUE account personalisation flow by adding an account created screen behind a feature flag ([#5158](https://github.com/element-hq/element-android/issues/5158))

SDK API changes ⚠️
------------------
 - Change name of getTimeLineEvent and getTimeLineEventLive methods to getTimelineEvent and getTimelineEventLive. ([#5330](https://github.com/element-hq/element-android/issues/5330))

Other changes
-------------
 - Improve Bubble layouts rendering ([#5303](https://github.com/element-hq/element-android/issues/5303))
 - Continue improving realm usage (potentially helping with storage and RAM usage) ([#5330](https://github.com/element-hq/element-android/issues/5330))
 - Update reaction button layout. ([#5313](https://github.com/element-hq/element-android/issues/5313))
 - Adds forceLoginFallback feature flag and usages to FTUE login and registration ([#5325](https://github.com/element-hq/element-android/issues/5325))
 - Override task affinity to prevent unknown activities running in our app tasks. ([#4498](https://github.com/element-hq/element-android/issues/4498))
 - Tentatively fixing the UI sanity test being unable to click on the space menu items ([#5269](https://github.com/element-hq/element-android/issues/5269))
 - Moves attachment-viewer, diff-match-patch, and multipicker modules to subfolders under library ([#5309](https://github.com/element-hq/element-android/issues/5309))
 - Log the `since` token used and `next_batch` token returned when doing an incremental sync. ([#5312](https://github.com/element-hq/element-android/issues/5312), [#5318](https://github.com/element-hq/element-android/issues/5318))
 - Upgrades material dependency version from 1.4.0 to 1.5.0 ([#5392](https://github.com/element-hq/element-android/issues/5392))
 - Using app name instead of hardcoded "Element" for exported keys filename ([#5326](https://github.com/element-hq/element-android/issues/5326))
 - Upgrade the plugin which generate strings with template from 1.2.2 to 2.0.0 ([#5348](https://github.com/element-hq/element-android/issues/5348))
 - Remove about 700 unused strings and their translations ([#5352](https://github.com/element-hq/element-android/issues/5352))
 - Creates dedicated VectorOverrides for forcing behaviour for local testing/development ([#5361](https://github.com/element-hq/element-android/issues/5361))
 - Cleanup unused threads build configurations ([#5379](https://github.com/element-hq/element-android/issues/5379))
 - Notify element-android channel each time a nightly build completes. ([#5314](https://github.com/element-hq/element-android/issues/5314))
 - Iterate on badge / unread indicator color ([#5456](https://github.com/element-hq/element-android/issues/5456))


Changes in Element v1.4.2 (2022-02-22 Palindrome Day!)
======================================================

Features ✨
----------
 - Open the room when user accepts an invite from the room list ([#3771](https://github.com/element-hq/element-android/issues/3771))
 - Add completion for @room to notify everyone in a room ([#5123](https://github.com/element-hq/element-android/issues/5123))
 - Improve UI of reactions in timeline, including quick add reaction. ([#5204](https://github.com/element-hq/element-android/issues/5204))
 - Support creating disclosed polls ([#5290](https://github.com/element-hq/element-android/issues/5290))

Bugfixes 🐛
----------
 - Remove redundant highlight on add poll option button ([#5178](https://github.com/element-hq/element-android/issues/5178))
 - Reliably display crash report prompt ([#5195](https://github.com/element-hq/element-android/issues/5195))
 - Fix for rooms with virtual rooms not showing call status events in the timeline. ([#5198](https://github.com/element-hq/element-android/issues/5198))
 - Fix for call transfer with consult failing to make outgoing consultation call. ([#5201](https://github.com/element-hq/element-android/issues/5201))
 - Fix crash during account registration when redirecting to Web View ([#5218](https://github.com/element-hq/element-android/issues/5218))
 - Analytics: Fixes missing use case identity values from within the onboarding flow ([#5234](https://github.com/element-hq/element-android/issues/5234))
 - Fixing crash when adding room by QR code after accepting the camera permission for the first time ([#5295](https://github.com/element-hq/element-android/issues/5295))

SDK API changes ⚠️
------------------
 - `join` and `leave` methods moved from MembershipService to RoomService and SpaceService to split logic for rooms and spaces ([#5183](https://github.com/element-hq/element-android/issues/5183))
 - Deprecates Matrix.initialize and Matrix.getInstance in favour of the client providing its own singleton instance via Matrix.createInstance ([#5185](https://github.com/element-hq/element-android/issues/5185))
 - Adds support for MSC3283, additional homeserver capabilities ([#5207](https://github.com/element-hq/element-android/issues/5207))

Other changes
-------------
 - Right align the notifications badge in the rooms list (and DMs) so that it's always in a consistent place on the screen. ([#4640](https://github.com/element-hq/element-android/issues/4640))
 - Collapse successive ACLs events in room timeline ([#2782](https://github.com/element-hq/element-android/issues/2782))
 - Home screen: Replacing search icon by filter icon in the top right menu ([#4643](https://github.com/element-hq/element-android/issues/4643))
 - Make Space creation screens more consistent ([#5104](https://github.com/element-hq/element-android/issues/5104))
 - Defensive coding to ensure encryption when room was once e2e ([#5136](https://github.com/element-hq/element-android/issues/5136))
 - Reduce verbosity of debug logging, ([#5209](https://github.com/element-hq/element-android/issues/5209))
 - Standardise emulator versions of GHA integration tests. ([#5210](https://github.com/element-hq/element-android/issues/5210))
 - Replacing color "vctr_unread_room_badge" by "vctr_content_secondary" ([#5225](https://github.com/element-hq/element-android/issues/5225))
 - Change preferred jitsi domain from `jitsi.riot.im` to `meet.element.io` ([#5254](https://github.com/element-hq/element-android/issues/5254))
 - Analytics screen events are now tracked on screen enter instead of screen leave ([#5256](https://github.com/element-hq/element-android/issues/5256))
 - Improves bitmap memory usage by caching the shortcut images ([#5276](https://github.com/element-hq/element-android/issues/5276))
 - Changes unread marker in room list from green to grey ([#5294](https://github.com/element-hq/element-android/issues/5294))
 - Improve some internal realm usages. ([#5297](https://github.com/element-hq/element-android/issues/5297))

Translations 🗣
--------------
 - Improved Japanese translations (special thanks to Suguru Hirahara!)


Changes in Element v1.4.0 (2022-02-09)
======================================

Features ✨
----------
 - Initial implementation of thread messages ([#4746](https://github.com/element-hq/element-android/issues/4746))
 - Support message bubbles in timeline. ([#4937](https://github.com/element-hq/element-android/issues/4937))
 - Support generic location pin ([#5146](https://github.com/element-hq/element-android/issues/5146))
 - Retrieve map style url from .well-known ([#5175](https://github.com/element-hq/element-android/issues/5175))

Bugfixes 🐛
----------
 - Fixes non sans-serif font weights being ignored ([#3907](https://github.com/element-hq/element-android/issues/3907))
 - Fixing missing/intermittent notifications on the google play variant when wifi is enabled ([#5038](https://github.com/element-hq/element-android/issues/5038))
 - Fixes call statuses in the timeline for missed/rejected calls and connected calls. ([#5088](https://github.com/element-hq/element-android/issues/5088))
 - Fix fallback permalink when threads are disabled ([#5128](https://github.com/element-hq/element-android/issues/5128))
 - Analytics: aligns use case identifying with iOS implementation ([#5142](https://github.com/element-hq/element-android/issues/5142))
 - Fix location rendering in timeline if map cannot be loaded ([#5143](https://github.com/element-hq/element-android/issues/5143))

Other changes
-------------
 - "Invite users to space" dialog now closed when user choose invite method ([#4295](https://github.com/element-hq/element-android/issues/4295))
 - Changed layout for space card and room card used at "explore room" screen and space/room invite dialogs ([#4304](https://github.com/element-hq/element-android/issues/4304))
 - Removed spaces restricted search hint dialogs ([#4315](https://github.com/element-hq/element-android/issues/4315))
 - Remove Search from room options if not available ([#4641](https://github.com/element-hq/element-android/issues/4641))
 - Qr code scanning fragments merged into one ([#4873](https://github.com/element-hq/element-android/issues/4873))
 - Fix CI/CD errors after merges for quality and integration tests ([#5118](https://github.com/element-hq/element-android/issues/5118))
 - Added automation for the Z-FTUE label to add issues to the FTUE Project Board ([#5120](https://github.com/element-hq/element-android/issues/5120))
 - Added automation for WTF labels to move to WTF project board ([#5148](https://github.com/element-hq/element-android/issues/5148))
 - Update WTF automation to fix it ([#5173](https://github.com/element-hq/element-android/issues/5173))


Changes in Element v1.3.18 (2022-02-03)
=======================================

Bugfixes 🐛
----------
 - Avoid deleting root event of CurrentState on gappy sync. In order to restore lost Events an initial sync may be triggered. ([#5137](https://github.com/element-hq/element-android/issues/5137))


Changes in Element v1.3.17 (2022-01-31)
=======================================

Bugfixes 🐛
----------
 - Display static map images in the timeline and improve Location sharing feature ([#5084](https://github.com/element-hq/element-android/issues/5084))
 - Show the legal mention of mapbox when sharing location ([#5062](https://github.com/element-hq/element-android/issues/5062))
 - Poll cannot end in some unencrypted rooms ([#5067](https://github.com/element-hq/element-android/issues/5067))
 - Selecting Transfer in a call should immediately put the other person on hold until the call connects or the Transfer is cancelled. ([#5081](https://github.com/element-hq/element-android/issues/5081))
 - Fixing crashes when quickly scrolling or restoring the room timeline ([#5091](https://github.com/element-hq/element-android/issues/5091))


Changes in Element 1.3.16 (2022-01-25)
======================================

Features ✨
----------
 - Static location sharing and rendering ([#2210](https://github.com/element-hq/element-android/issues/2210))
 - Enables the FTUE splash carousel ([#4584](https://github.com/element-hq/element-android/issues/4584))
 - Allow editing polls ([#5036](https://github.com/element-hq/element-android/issues/5036))

Bugfixes 🐛
----------
 - Fixing missing notifications in FDroid variants using `optimised for battery` background sync mode ([#5003](https://github.com/element-hq/element-android/issues/5003))
 - Fix for stuck local event messages at the bottom of the screen ([#516](https://github.com/element-hq/element-android/issues/516))
 - Notification does not take me to the room when another space was last viewed ([#3839](https://github.com/element-hq/element-android/issues/3839))
 - Explore Rooms overflow menu - content update include "Create room" ([#3932](https://github.com/element-hq/element-android/issues/3932))
 - Fix sync timeout after returning from background ([#4669](https://github.com/element-hq/element-android/issues/4669))
 - Fix a wrong network error issue in the Legals screen ([#4935](https://github.com/element-hq/element-android/issues/4935))
 - Prevent Alerts to be displayed in the automatically displayed analytics opt-in screen ([#4948](https://github.com/element-hq/element-android/issues/4948))
 - EmojiPopupDismissListener not being triggered after dismissing the EmojiPopup ([#4991](https://github.com/element-hq/element-android/issues/4991))
 - Fix an error in string resource ([#4997](https://github.com/element-hq/element-android/issues/4997))
 - Big messages taking inappropriately long to evaluate .m.rule.roomnotif push rules ([#5008](https://github.com/element-hq/element-android/issues/5008))
 - Improve auto rageshake lab feature ([#5021](https://github.com/element-hq/element-android/issues/5021))

In development 🚧
----------------
 - Updates the onboarding carousel images, copy and improves the handling of different device sizes ([#4880](https://github.com/element-hq/element-android/issues/4880))
 - Disabling onboarding automatic carousel transitions on user interaction ([#4914](https://github.com/element-hq/element-android/issues/4914))
 - Locking phones to portrait during the FTUE onboarding ([#4918](https://github.com/element-hq/element-android/issues/4918))
 - Adds a messaging use case screen to the FTUE onboarding ([#4927](https://github.com/element-hq/element-android/issues/4927))
 - Updating the FTUE use case icons ([#5025](https://github.com/element-hq/element-android/issues/5025))
 - Support undisclosed polls ([#5037](https://github.com/element-hq/element-android/issues/5037))

Other changes
-------------
 - Enabling native support for window resizing ([#4811](https://github.com/element-hq/element-android/issues/4811))
 - Analytics: send more Events ([#4734](https://github.com/element-hq/element-android/issues/4734))
 - Fix integration tests and add a comment with results (still not perfect due to github actions resource limitations) ([#4842](https://github.com/element-hq/element-android/issues/4842))
 - "/kick" command is replaced with "/remove". Also replaced all occurrences in string resources ([#4865](https://github.com/element-hq/element-android/issues/4865))
 - Toolbar management rework. Toolbar title's and subtitle's text appearance now controlled by theme without local overrides. Helper class introduced to
  help with toolbar configuration. Toolbar title, subtitle and navigation button widgets are removed where it is possible and replaced with built-in
  toolbar widgets. ([#4884](https://github.com/element-hq/element-android/issues/4884))
 - Add signing config for the release buildType. No secret added ([#4926](https://github.com/element-hq/element-android/issues/4926))
 - Remove unused module matrix-sdk-android-rx and do some cleanup ([#4942](https://github.com/element-hq/element-android/issues/4942))
 - Sync issue automation with element-web ([#4949](https://github.com/element-hq/element-android/issues/4949))
 - Improves local echo blinking when non room events received ([#4960](https://github.com/element-hq/element-android/issues/4960))
 - Including onboarding server options in the all screen sanity test suite ([#4975](https://github.com/element-hq/element-android/issues/4975))
 - Exclude dependabot upgrade for @github-script@v3 ([#4988](https://github.com/element-hq/element-android/issues/4988))
 - Small iteration on command parser and unit test it. ([#4998](https://github.com/element-hq/element-android/issues/4998))

SDK API changes ⚠️
------------------
 - `StateService.sendStateEvent()` now takes a non-nullable String for the parameter `stateKey`. If null was used, just now use an empty string. ([#4895](https://github.com/element-hq/element-android/issues/4895))
 - 429 are not automatically retried anymore in case of too long retry delay ([#4995](https://github.com/element-hq/element-android/issues/4995))


Changes in Element v1.3.15 (2022-01-18)
=======================================

Bugfixes 🐛
----------
 - Fix crash when viewing source which contains an emoji ([#4796](https://github.com/element-hq/element-android/issues/4796))
 - Prevent crash in Timeline and add more logs. ([#4959](https://github.com/element-hq/element-android/issues/4959))
 - Fix crash on API <24 and make sure this error will not occur again. ([#4962](https://github.com/element-hq/element-android/issues/4962))
 - Fixes sign in/up crash when selecting ems and other server types which use SSO ([#4969](https://github.com/element-hq/element-android/issues/4969))


Changes in Element v1.3.14 (2022-01-12)
=======================================

Bugfixes 🐛
----------
 - Fix sending events in encrypted rooms broken, and incremental sync broken in 1.3.13 ([#4924](https://github.com/element-hq/element-android/issues/4924))


Changes in Element v1.3.13 (2022-01-11)
=======================================

Features ✨
----------
 - Updates onboarding splash screen to have a dedicated sign in button and removes the dual purpose sign in/up stage ([#4382](https://github.com/element-hq/element-android/issues/4382))
 - Display Analytics opt-in screen at first start-up of the app ([#4892](https://github.com/element-hq/element-android/issues/4892))
 - New attachment picker UI ([#3444](https://github.com/element-hq/element-android/issues/3444))
 - Add labs support for rendering LaTeX maths (MSC2191) ([#2133](https://github.com/element-hq/element-android/issues/2133))
 - Allow changing nick colors from the member detail screen ([#2614](https://github.com/element-hq/element-android/issues/2614))
 - Analytics: Track Errors ([#4719](https://github.com/element-hq/element-android/issues/4719))
 - Change internal timeline management. ([#4405](https://github.com/element-hq/element-android/issues/4405))
 - Translate the error observed when the user is not allowed to join a room ([#4847](https://github.com/element-hq/element-android/issues/4847))

Bugfixes 🐛
----------
 - Stop using CharSequence as EpoxyAttribute because it can lead to crash if the CharSequence mutates during rendering. ([#4837](https://github.com/element-hq/element-android/issues/4837))
 - Better handling of misconfigured room encryption ([#4711](https://github.com/element-hq/element-android/issues/4711))
 - Fix message replies/quotes to respect newlines. ([#4540](https://github.com/element-hq/element-android/issues/4540))
 - Polls: unable to create a poll with more than 10 answers ([#4735](https://github.com/element-hq/element-android/issues/4735))
 - Fix for broken unread message indicator on the room list when there are no messages in the room. ([#4749](https://github.com/element-hq/element-android/issues/4749))
 - Fixes newer emojis rendering strangely when inserting from the system keyboard ([#4756](https://github.com/element-hq/element-android/issues/4756))
 - Fixing unable to change change avatar in some scenarios ([#4767](https://github.com/element-hq/element-android/issues/4767))
 - Tentative fix for the speaker being used instead of earpiece for the outgoing call ringtone on lineage os ([#4781](https://github.com/element-hq/element-android/issues/4781))
 - Fixing crashes when quickly scrolling or restoring the room timeline ([#4789](https://github.com/element-hq/element-android/issues/4789))
 - Fixing encrypted non message events showing up as notification messages (eg when a participant joins, mutes or leaves a voice call) ([#4804](https://github.com/element-hq/element-android/issues/4804))

SDK API changes ⚠️
------------------
 - Introduce method onStateUpdated on Timeline.Callback ([#4405](https://github.com/element-hq/element-android/issues/4405))
 - Support tagged events in Room Account Data (MSC2437) ([#4753](https://github.com/element-hq/element-android/issues/4753))

Other changes
-------------
 - Workaround to fetch all the pending toDevice events from a Synapse homeserver ([#4612](https://github.com/element-hq/element-android/issues/4612))
 - Toolbar is added to a views with QR code scan ([#4644](https://github.com/element-hq/element-android/issues/4644))
 - Open share UI provides by the system when sharing media or text. ([#4745](https://github.com/element-hq/element-android/issues/4745))
 - Cleaning rendering of state events in timeline ([#4747](https://github.com/element-hq/element-android/issues/4747))
 - Enabling new FTUE Auth onboarding base, includes the "I already have an account" button in the splash ([#4872](https://github.com/element-hq/element-android/issues/4872))
 - Olm lib is now hosted in MavenCentral - upgrade to 3.2.10 ([#4882](https://github.com/element-hq/element-android/issues/4882))
 - Remove deprecated experimental restricted space lab option ([#4889](https://github.com/element-hq/element-android/issues/4889))
 - Add ktlint results on github as a comment only on fail ([#4888](https://github.com/element-hq/element-android/issues/4888))
 - Fix github actions ktlint reports and publish results on PR as comment ([#4864](https://github.com/element-hq/element-android/issues/4864))


Changes in Element v1.3.12 (2021-12-20)
=======================================

Bugfixes 🐛
----------
 - Fixing emoji related crashes on android 8.1.1 and below ([#4769](https://github.com/element-hq/element-android/issues/4769))


Changes in Element v1.3.11 (2021-12-17)
=======================================

Bugfixes 🐛
----------
 - Fixing proximity sensor still being active after a call ([#2467](https://github.com/element-hq/element-android/issues/2467))
 - Fix name and shield are truncated in the room detail screen ([#4700](https://github.com/element-hq/element-android/issues/4700))
 - Call banner: center text vertically ([#4710](https://github.com/element-hq/element-android/issues/4710))
 - Fixes unable to render messages by allowing them to render whilst the emoji library is initialising ([#4733](https://github.com/element-hq/element-android/issues/4733))
 - Fix app crash uppon long press on a reply event ([#4742](https://github.com/element-hq/element-android/issues/4742))
 - Fixes crash when launching rooms which contain emojis in the emote content on android 12+ ([#4743](https://github.com/element-hq/element-android/issues/4743))

Other changes
-------------
 - Avoids leaking the activity windows when loading dialogs are displaying ([#4713](https://github.com/element-hq/element-android/issues/4713))


Changes in Element v1.3.10 (2021-12-14)
=======================================

Features ✨
----------
 - Poll Feature - Render in timeline ([#4653](https://github.com/element-hq/element-android/issues/4653))
 - Updates URL previews to match latest designs ([#4278](https://github.com/element-hq/element-android/issues/4278))
 - Setup Analytics framework using PostHog. Analytics are disabled by default. Opt-in screen not automatically displayed yet. ([#4559](https://github.com/element-hq/element-android/issues/4559))
 - Create a legal screen in the setting to group all the different policies. ([#4660](https://github.com/element-hq/element-android/issues/4660))
 - Add a help section in the settings. ([#4638](https://github.com/element-hq/element-android/issues/4638))
 - MSC2732: Olm fallback keys ([#3473](https://github.com/element-hq/element-android/issues/3473))

Bugfixes 🐛
----------
 - Fixes message menu showing when copying message urls ([#4324](https://github.com/element-hq/element-android/issues/4324))
 - Fix lots of integration tests by introducing TestMatrix class and MatrixWorkerFactory. ([#4546](https://github.com/element-hq/element-android/issues/4546))
 - Fix empty Dev Tools screen issue. ([#4592](https://github.com/element-hq/element-android/issues/4592))
 - Fix for outgoing voip call via sip bridge failing after 1 minute. ([#4621](https://github.com/element-hq/element-android/issues/4621))
 - Update log warning for call selection during voip calls. ([#4636](https://github.com/element-hq/element-android/issues/4636))
 - Fix possible crash when having identical subspaces in multiple root spaces ([#4693](https://github.com/element-hq/element-android/issues/4693))
 - Fix a crash in the timeline with some Emojis. Also migrate to androidx.emoji2 ([#4698](https://github.com/element-hq/element-android/issues/4698))
 - At the very first room search after opening the app sometimes no results are displayed ([#4600](https://github.com/element-hq/element-android/issues/4600))

Other changes
-------------
 - Upgrade OLM to v3.2.7 and get it from our maven repository. ([#4647](https://github.com/element-hq/element-android/issues/4647))
 - Add explicit dependency location, regarding the several maven repository. Also update some libraries (flexbox and alerter), and do some cleanup. ([#4670](https://github.com/element-hq/element-android/issues/4670))
 - Introducing feature flagging to the login and notification settings flows ([#4626](https://github.com/element-hq/element-android/issues/4626))
 - There is no need to call job.cancel() when we are using viewModelScope() ([#4602](https://github.com/element-hq/element-android/issues/4602))
 - Debounce some clicks ([#4645](https://github.com/element-hq/element-android/issues/4645))
 - Improve issue automation workflows ([#4617](https://github.com/element-hq/element-android/issues/4617))
 - Add automation to move message bubbles issues to message bubbles board. ([#4666](https://github.com/element-hq/element-android/issues/4666))
 - Fix graphql warning in issue workflow automation ([#4671](https://github.com/element-hq/element-android/issues/4671))
 - Cleanup the layout files ([#4604](https://github.com/element-hq/element-android/issues/4604))
 - Cleanup id ref. Use type views instead ([#4650](https://github.com/element-hq/element-android/issues/4650))


Changes in Element v1.3.9 (2021-12-01)
======================================

Features ✨
----------
 - Voice messages: Persist drafts of voice messages when navigating between rooms ([#3922](https://github.com/element-hq/element-android/issues/3922))
 - Make Element Android Thread aware ([#4246](https://github.com/element-hq/element-android/issues/4246))
 - Iterate on the consent dialog of the identity server. ([#4577](https://github.com/element-hq/element-android/issues/4577))

Bugfixes 🐛
----------
 - Fixes left over text when inserting emojis via the ':' menu and replaces the last typed ':' rather than the one at the end of the message ([#3449](https://github.com/element-hq/element-android/issues/3449))
 - Fixing queued voice message failing to send or retry ([#3833](https://github.com/element-hq/element-android/issues/3833))
 - Keeping device screen on whilst recording and playing back voice messages ([#4022](https://github.com/element-hq/element-android/issues/4022))
 - Allow voice messages to continue recording during device rotation ([#4067](https://github.com/element-hq/element-android/issues/4067))
 - Allowing users to hang up VOIP calls during the initialisation phase (avoids getting stuck in the call screen if something goes wrong) ([#4144](https://github.com/element-hq/element-android/issues/4144))
 - Make the verification shields the same in Element Web and Element Android ([#4338](https://github.com/element-hq/element-android/issues/4338))
 - Fix a display issue in the composer when the replied message is changed. ([#4343](https://github.com/element-hq/element-android/issues/4343))
 - Dismissing the Fdroid variant Listening for notifications on sign out, fixes crash when tapping the notification when signed out ([#4488](https://github.com/element-hq/element-android/issues/4488))
 - Fix a crash when displaying the bootstrap bottom sheet ([#4520](https://github.com/element-hq/element-android/issues/4520))
 - Remove duplicated settings declaration ([#4539](https://github.com/element-hq/element-android/issues/4539))
 - Fixes .ogg files failing to upload to rooms ([#4552](https://github.com/element-hq/element-android/issues/4552))
 - Add robustness when getting data from cursors ([#4605](https://github.com/element-hq/element-android/issues/4605))

Other changes
-------------
 - Upgrade Jitsi lib (and so webrtc) from Jitsi android-sdk-3.1.0 to android-sdk-3.10.0 ([#4504](https://github.com/element-hq/element-android/issues/4504))
 - Improve crypto logs to help debug decryption failures ([#4507](https://github.com/element-hq/element-android/issues/4507))
 - Voice recording mic button refactor with small animation tweaks in preparation for voice drafts ([#4515](https://github.com/element-hq/element-android/issues/4515))
 - Remove requestModelBuild() from epoxy Controllers init{} block ([#4591](https://github.com/element-hq/element-android/issues/4591))


Changes in Element v1.3.8 (2021-11-17)
======================================

Features ✨
----------
 - Android 12 support ([#4433](https://github.com/element-hq/element-android/issues/4433))
 - Make notification text spoiler aware ([#3477](https://github.com/element-hq/element-android/issues/3477))
 - Poll Feature - Create Poll Screen (Disabled for now) ([#4367](https://github.com/element-hq/element-android/issues/4367))
 - Adds support for images inside message notifications ([#4402](https://github.com/element-hq/element-android/issues/4402))

Bugfixes 🐛
----------
 - Render markdown in room list ([#452](https://github.com/element-hq/element-android/issues/452))
 - Fix incorrect cropping of conversation icons ([#4424](https://github.com/element-hq/element-android/issues/4424))
 - Fix potential NullPointerException crashes in Room and User account data sources ([#4428](https://github.com/element-hq/element-android/issues/4428))
 - Unable to establish Olm outbound session from fallback key ([#4446](https://github.com/element-hq/element-android/issues/4446))
 - Fixes intermittent crash on sign out due to the session being incorrectly recreated whilst being closed ([#4480](https://github.com/element-hq/element-android/issues/4480))

SDK API changes ⚠️
------------------
 - Add content scanner API from MSC1453
  API documentation : https://github.com/matrix-org/matrix-content-scanner#api ([#4392](https://github.com/element-hq/element-android/issues/4392))
 - Breaking SDK API change to PushRuleListener, the separated callbacks have been merged into one with a data class which includes all the previously separated push information ([#4401](https://github.com/element-hq/element-android/issues/4401))

Other changes
-------------
 - Finish migration from RxJava to Flow ([#4219](https://github.com/element-hq/element-android/issues/4219))
 - Remove redundant text in feature request issue form ([#4257](https://github.com/element-hq/element-android/issues/4257))
 - Add and improve issue triage workflows ([#4435](https://github.com/element-hq/element-android/issues/4435))
 - Update issue template to bring in line with element-web ([#4452](https://github.com/element-hq/element-android/issues/4452))


Changes in Element v1.3.7 (2021-11-04)
======================================

Features ✨
----------
 - Adding the room name to the invitation notification (if the room summary is available) ([#582](https://github.com/element-hq/element-android/issues/582))
 - Updating single sign on providers ordering to match priority/popularity ([#4277](https://github.com/element-hq/element-android/issues/4277))

Bugfixes 🐛
----------
 - Stops showing a dedicated redacted event notification, the message notifications will update accordingly ([#1491](https://github.com/element-hq/element-android/issues/1491))
 - Fixes marking individual notifications as read causing other notifications to be dismissed ([#3395](https://github.com/element-hq/element-android/issues/3395))
 - Fixing missing send button in light mode dev tools - send * event ([#3674](https://github.com/element-hq/element-android/issues/3674))
 - Fixing room search needing exact casing for non latin-1 character named rooms ([#3968](https://github.com/element-hq/element-android/issues/3968))
 - Fixing call ringtones only playing once when the ringtone doesn't contain looping metadata (android 9.0 and above) ([#4047](https://github.com/element-hq/element-android/issues/4047))
 - Tentatively fixing the doubled notifications by updating the group summary at specific points in the notification rendering cycle ([#4152](https://github.com/element-hq/element-android/issues/4152))
 - Do not show shortcuts if a PIN code is set ([#4170](https://github.com/element-hq/element-android/issues/4170))
 - Fixes being unable to join rooms by name ([#4255](https://github.com/element-hq/element-android/issues/4255))
 - Fixing missing F-Droid notifications when in background due to background syncs not triggering ([#4298](https://github.com/element-hq/element-android/issues/4298))
 - Fix video compression before upload ([#4353](https://github.com/element-hq/element-android/issues/4353))
 - Fixing QR code crashes caused by a known issue in the zxing library for older versions of android by downgrading to 3.3.3 ([#4361](https://github.com/element-hq/element-android/issues/4361))
 - Fixing timeline crash when rotating with the emoji window open ([#4365](https://github.com/element-hq/element-android/issues/4365))
 - Fix handling of links coming from web instance reported as malformed by mistake ([#4369](https://github.com/element-hq/element-android/issues/4369))

SDK API changes ⚠️
------------------
 - Add API `LoginWizard.loginCustom(data: JsonDict): Session` to be able to login to a homeserver using arbitrary request content ([#4266](https://github.com/element-hq/element-android/issues/4266))
 - Add optional deviceId to the login API ([#4334](https://github.com/element-hq/element-android/issues/4334))

Other changes
-------------
 - Migrate app DI framework to Hilt ([#3888](https://github.com/element-hq/element-android/issues/3888))
 - Limit supported TLS versions and cipher suites ([#4192](https://github.com/element-hq/element-android/issues/4192))
 - Fixed capitalisation of text on initial sync screen ([#4292](https://github.com/element-hq/element-android/issues/4292))


Changes in Element v1.3.6 (2021-10-26)
======================================

Bugfixes 🐛
----------
 - Correctly handle url of type https://mobile.element.io/?hs_url=…&is_url=…
  Skip the choose server screen when such URL are open when Element ([#2684](https://github.com/element-hq/element-android/issues/2684))


Changes in Element v1.3.5 (2021-10-25)
======================================

Bugfixes 🐛
----------
 - Fixing malformed link pop up when tapping on notifications ([#4267](https://github.com/element-hq/element-android/issues/4267))
 - Fix Broken EditText when using FromEditTextItem ([#4276](https://github.com/element-hq/element-android/issues/4276))
 - Fix crash when clicking on ViewEvent source actions ([#4279](https://github.com/element-hq/element-android/issues/4279))
 - Fix voice message record button wrong visibility ([#4283](https://github.com/element-hq/element-android/issues/4283))
 - Fix unread marker not showing ([#4313](https://github.com/element-hq/element-android/issues/4313))


Changes in Element v1.3.4 (2021-10-20)
======================================

Features ✨
----------
 - Implement /part command, with or without parameter ([#2909](https://github.com/element-hq/element-android/issues/2909))
 - Handle Presence support, for Direct Message room ([#4090](https://github.com/element-hq/element-android/issues/4090))
 - Priority conversations for Android 11+ ([#3313](https://github.com/element-hq/element-android/issues/3313))

Bugfixes 🐛
----------
 - Issue #908 Adding trailing space " " or ": " if the user started a sentence by mentioning someone, ([#908](https://github.com/element-hq/element-android/issues/908))
 - Fixes reappearing notifications when dismissing notifications from slow homeservers or delayed /sync responses ([#3437](https://github.com/element-hq/element-android/issues/3437))
 - Catching event decryption crash and logging when attempting to markOlmSessionForUnwedging fails ([#3608](https://github.com/element-hq/element-android/issues/3608))
 - Fixing notification sounds being triggered for every message, now they only trigger for the first, consistent with the vibrations ([#3774](https://github.com/element-hq/element-android/issues/3774))
 - Voice Message not sendable if recorded while flight mode was on ([#4006](https://github.com/element-hq/element-android/issues/4006))
 - Fixes push notification emails list not refreshing the first time seeing the notifications page.
  Also improves the error handling in the email notification toggling by using synchronous flows instead of the WorkManager ([#4106](https://github.com/element-hq/element-android/issues/4106))
 - Make MegolmBackupAuthData.signatures optional for robustness ([#4162](https://github.com/element-hq/element-android/issues/4162))
 - Fixing push notifications starting the looping background sync when the push notification causes the application to be created. ([#4167](https://github.com/element-hq/element-android/issues/4167))
 - Fix random crash when user logs out just after the log in. ([#4193](https://github.com/element-hq/element-android/issues/4193))
 - Make the font size selection dialog scrollable ([#4201](https://github.com/element-hq/element-android/issues/4201))
 - Fix conversation notification for sent messages ([#4221](https://github.com/element-hq/element-android/issues/4221))
 - Fixes the developer sync options being displayed in the home menu when developer mode is disabled ([#4234](https://github.com/element-hq/element-android/issues/4234))
 - Restore support for Android Auto as sent messages are no longer read aloud ([#4247](https://github.com/element-hq/element-android/issues/4247))
 - Fix crash on slash commands Exceptions ([#4261](https://github.com/element-hq/element-android/issues/4261))

Other changes
-------------
 - Scrub user sensitive data like gps location from images when sending on original quality ([#465](https://github.com/element-hq/element-android/issues/465))
 - Migrate to MvRx2 (Mavericks) ([#3890](https://github.com/element-hq/element-android/issues/3890))
 - Implement a new github action workflow to generate two PRs for emoji and sas string sync ([#4216](https://github.com/element-hq/element-android/issues/4216))
 - Improve wording around rageshakes in the defect issue template. ([#4226](https://github.com/element-hq/element-android/issues/4226))
 - Add automation to move incoming issues and X-Needs-Info into the right places on the issue triage board. ([#4250](https://github.com/element-hq/element-android/issues/4250))
 - Uppon sharing image compression fails, return the original image ([#4264](https://github.com/element-hq/element-android/issues/4264))


Changes in Element v1.3.3 (2021-10-11)
======================================

Bugfixes 🐛
----------
 - Disable Android Auto supports ([#4205](https://github.com/element-hq/element-android/issues/4205))


Changes in Element v1.3.2 (2021-10-08)
======================================

Features ✨
----------
 - Android Auto notification support ([#240](https://github.com/element-hq/element-android/issues/240))
 - Add a fallback for user displayName when this one is null or empty ([#3732](https://github.com/element-hq/element-android/issues/3732))
 - Add client base url config to customize permalinks ([#4027](https://github.com/element-hq/element-android/issues/4027))
 - Check if DM exists before creating a new one ([#4157](https://github.com/element-hq/element-android/issues/4157))
 - Handle 8 new slash commands: `/ignore`, `/unignore`, `/roomname`, `/myroomnick`, `/roomavatar`, `/myroomavatar`, `/lenny`, `/whois`. ([#4158](https://github.com/element-hq/element-android/issues/4158))
 - Display identity server policies in the Discovery screen ([#4184](https://github.com/element-hq/element-android/issues/4184))

Bugfixes 🐛
----------
 - Ensure initial sync progress dialog is hidden when the initial sync is over ([#983](https://github.com/element-hq/element-android/issues/983))
 - Avoid resending notifications that are already shown ([#1673](https://github.com/element-hq/element-android/issues/1673))
 - Room filter no results bad CTA in space mode when a space selected ([#3048](https://github.com/element-hq/element-android/issues/3048))
 - Fixes notifications not dismissing when reading messages on other devices ([#3347](https://github.com/element-hq/element-android/issues/3347))
 - Fixes the passphrase screen being incorrectly shown when pressing back on the key verification screen.
  When the user doesn't have a passphrase set we don't show the passphrase screen. ([#3898](https://github.com/element-hq/element-android/issues/3898))
 - App doesn't take you to a Space after choosing to Join it ([#3933](https://github.com/element-hq/element-android/issues/3933))
 - Validate public space addresses and room aliases length ([#3934](https://github.com/element-hq/element-android/issues/3934))
 - Save button for adding rooms to a space is hidden when scrolling through list of rooms ([#3935](https://github.com/element-hq/element-android/issues/3935))
 - Align new room encryption default to Web ([#4045](https://github.com/element-hq/element-android/issues/4045))
 - Fix Reply/Edit mode animation is broken when sending ([#4077](https://github.com/element-hq/element-android/issues/4077))
 - Added changes that will make SearchView in search bar focused by default on opening reaction picker.

  When tapping close icon of SearchView, the SearchView did not collapse therefore added the on close listener
  which will collapse the SearchView on close. ([#4092](https://github.com/element-hq/element-android/issues/4092))
 - Troubleshoot notification: Fix button not clickable ([#4109](https://github.com/element-hq/element-android/issues/4109))
 - Harmonize wording in the message bottom sheet and move up the View Reactions item ([#4155](https://github.com/element-hq/element-android/issues/4155))
 - Remove unused SendRelationWorker and related API call (3588) ([#4156](https://github.com/element-hq/element-android/issues/4156))
 - SIP user to native user mapping is wrong ([#4176](https://github.com/element-hq/element-android/issues/4176))

SDK API changes ⚠️
------------------
 - Create extension `String.isMxcUrl()` ([#4158](https://github.com/element-hq/element-android/issues/4158))

Other changes
-------------
 - Use ktlint plugin. See [the documentation](https://github.com/element-hq/element-android/blob/develop/CONTRIBUTING.md#ktlint) for more detail. ([#3957](https://github.com/element-hq/element-android/issues/3957))
 - Minimize the use of exported="true" in android Manifest (link: https://github.com/matrix-org/matrix-dinsic/issues/618) ([#4018](https://github.com/element-hq/element-android/issues/4018))
 - Fix redundancy in heading in the bug report issue form ([#4076](https://github.com/element-hq/element-android/issues/4076))
 - Fix release label in the release issue template ([#4113](https://github.com/element-hq/element-android/issues/4113))


Changes in Element v1.3.1 (2021-09-29)
======================================

Bugfixes 🐛
----------
 - Verifying exported E2E keys to provide user feedback when the output is malformed ([#4082](https://github.com/element-hq/element-android/issues/4082))
 - Fix settings crash when accelerometer not available ([#4103](https://github.com/element-hq/element-android/issues/4103))
 - Crash while rendering failed message warning ([#4110](https://github.com/element-hq/element-android/issues/4110))


Changes in Element v1.3.0 (2021-09-27)
======================================

Features ✨
----------
 - Spaces!
 - Adds email notification registration to Settings ([#2243](https://github.com/element-hq/element-android/issues/2243))
 - Spaces | M3.23 Invite by email in create private space flow ([#3678](https://github.com/element-hq/element-android/issues/3678))
 - Improve space invite bottom sheet ([#4057](https://github.com/element-hq/element-android/issues/4057))
 - Allow to also leave rooms when leaving a space ([#3692](https://github.com/element-hq/element-android/issues/3692))
 - Better expose adding spaces as Subspaces ([#3752](https://github.com/element-hq/element-android/issues/3752))
 - Push and syncs: add debug info on room list and on room detail screen and improves the log format. ([#4046](https://github.com/element-hq/element-android/issues/4046))

Bugfixes 🐛
----------
 - Remove the "Teammate spaces aren't quite ready" bottom sheet ([#3945](https://github.com/element-hq/element-android/issues/3945))
 - Restricted Room previews aren't working ([#3946](https://github.com/element-hq/element-android/issues/3946))
 - A removed room from a space can't be re-added as it won't be shown in add-room ([#3947](https://github.com/element-hq/element-android/issues/3947))
 - "Non-Admin" user able to invite others to Private Space (by default) ([#3951](https://github.com/element-hq/element-android/issues/3951))
 - Kick user dialog for spaces talks about rooms ([#3956](https://github.com/element-hq/element-android/issues/3956))
 - Messages are displayed as unable to decrypt then decrypted a few seconds later ([#4011](https://github.com/element-hq/element-android/issues/4011))
 - Fix DTMF not working ([#4015](https://github.com/element-hq/element-android/issues/4015))
 - Fix sticky end call notification ([#4019](https://github.com/element-hq/element-android/issues/4019))
 - Fix call screen stuck with some hanging up scenarios ([#4026](https://github.com/element-hq/element-android/issues/4026))
 - Fix other call not always refreshed when ended ([#4028](https://github.com/element-hq/element-android/issues/4028))
 - Private space invite bottomsheet only offering inviting by username not by email ([#4042](https://github.com/element-hq/element-android/issues/4042))
 - Spaces invitation system notifications don't take me to the join space toast ([#4043](https://github.com/element-hq/element-android/issues/4043))
 - Space Invites are not lighting up the drawer menu ([#4059](https://github.com/element-hq/element-android/issues/4059))
 - MessageActionsBottomSheet not being shown on local echos ([#4068](https://github.com/element-hq/element-android/issues/4068))

SDK API changes ⚠️
------------------
 - InitialSyncProgressService has been renamed to SyncStatusService and its function getInitialSyncProgressStatus() has been renamed to getSyncStatusLive() ([#4046](https://github.com/element-hq/element-android/issues/4046))

Other changes
-------------
 - Better support for Sdk2 version. Also slight change in the default user agent: `MatrixAndroidSDK_X` is replaced by `MatrixAndroidSdk2` ([#3994](https://github.com/element-hq/element-android/issues/3994))
 - Introduces ConferenceEvent to abstract usage of Jitsi BroadcastEvent class. ([#4014](https://github.com/element-hq/element-android/issues/4014))
 - Improve performances on RoomDetail screen ([#4065](https://github.com/element-hq/element-android/issues/4065))


Changes in Element v1.2.2 (2021-09-13)
======================================

Bugfixes 🐛
----------

- Fix a security issue with message key sharing. See https://matrix.org/blog/2021/09/13/vulnerability-disclosure-key-sharing for details.


Changes in Element v1.2.1 (2021-09-08)
======================================

Features ✨
----------
 - Support Android 11 Conversation features ([#1809](https://github.com/element-hq/element-android/issues/1809))
 - Introduces AutoAcceptInvites which can be enabled at compile time. ([#3531](https://github.com/element-hq/element-android/issues/3531))
 - New call designs ([#3599](https://github.com/element-hq/element-android/issues/3599))
 - Restricted Join Rule | Inform admins of new option ([#3631](https://github.com/element-hq/element-android/issues/3631))
 - Mention and Keyword Notification Settings: Turn on/off keyword notifications and edit keywords. ([#3650](https://github.com/element-hq/element-android/issues/3650))
 - Support accept 3pid invite when email is not bound to account ([#3691](https://github.com/element-hq/element-android/issues/3691))
 - Space summary pagination ([#3693](https://github.com/element-hq/element-android/issues/3693))
 - Update Email invite to be aware of spaces ([#3695](https://github.com/element-hq/element-android/issues/3695))
 - M11.12 Spaces | Default to 'Home' in settings ([#3754](https://github.com/element-hq/element-android/issues/3754))
 - Call: show dialog for some ended reasons. ([#3853](https://github.com/element-hq/element-android/issues/3853))
 - Add expired account error code in the matrix SDK ([#3900](https://github.com/element-hq/element-android/issues/3900))
 - Add password errors in the matrix SDK ([#3927](https://github.com/element-hq/element-android/issues/3927))
 - Upgrade AGP to 7.0.2.
  When compiling using command line, make sure to use the JDK 11 by adding for instance `-Dorg.gradle.java.home=/Applications/Android\ Studio\ Preview.app/Contents/jre/Contents/Home` or by setting JAVA_HOME. ([#3954](https://github.com/element-hq/element-android/issues/3954))
 - Check power level before displaying actions in the room details' timeline ([#3959](https://github.com/element-hq/element-android/issues/3959))

Bugfixes 🐛
----------
 - Add mxid to autocomplete suggestion if more than one user in a room has the same displayname ([#1823](https://github.com/element-hq/element-android/issues/1823))
 - Use WebView cache for widgets to avoid excessive data use ([#2648](https://github.com/element-hq/element-android/issues/2648))
 - Jitsi-hosted jitsi conferences not loading ([#2846](https://github.com/element-hq/element-android/issues/2846))
 - Space Explore Rooms no feedback on failed to join ([#3207](https://github.com/element-hq/element-android/issues/3207))
 - Notifications - Fix missing sound on notifications. ([#3243](https://github.com/element-hq/element-android/issues/3243))
 - the element-based domain permalinks (e.g. https://app.element.io/#/user/@chagai95:matrix.org) don't have the mxid in the first param (like matrix.to does - https://matrix.to/#/@chagai95:matrix.org) but rather in the second after /user/ so /user/mxid ([#3735](https://github.com/element-hq/element-android/issues/3735))
 - Update the AccountData with the users' matrix Id instead of their email for those invited by email in a direct chat ([#3743](https://github.com/element-hq/element-android/issues/3743))
 - Send an empty body for POST rooms/{roomId}/receipt/{receiptType}/{eventId} ([#3789](https://github.com/element-hq/element-android/issues/3789))
 - Fix order in which the items of the attachment menu appear ([#3793](https://github.com/element-hq/element-android/issues/3793))
 - Authenticated Jitsi not working in release ([#3841](https://github.com/element-hq/element-android/issues/3841))
 - Home: Dial pad lost entry when config changes ([#3845](https://github.com/element-hq/element-android/issues/3845))
 - Message edition is not rendered in e2e rooms after pagination ([#3887](https://github.com/element-hq/element-android/issues/3887))
 - Crash on opening a room on Android 5.0 and 5.1 - Regression with Voice message ([#3897](https://github.com/element-hq/element-android/issues/3897))
 - Fix a crash at start-up if translated string is empty ([#3910](https://github.com/element-hq/element-android/issues/3910))
 - PushRule enabling request is not following the spec ([#3911](https://github.com/element-hq/element-android/issues/3911))
 - Enable image preview in Android's share sheet (Android 11+) ([#3965](https://github.com/element-hq/element-android/issues/3965))
 - Voice Message - Cannot render voice message if the waveform data is corrupted ([#3983](https://github.com/element-hq/element-android/issues/3983))
 - Fix memory leak on RoomDetailFragment (ValueAnimator) ([#3990](https://github.com/element-hq/element-android/issues/3990))

Other changes
-------------
 - VoIP: Merge virtual room timeline in corresponding native room (call events only). ([#3520](https://github.com/element-hq/element-android/issues/3520))
 - Issue templates: modernise and sync with element-web ([#3883](https://github.com/element-hq/element-android/issues/3883))
 - Issue templates: modernise SDK and release checklists, and add homeserver question for bugs ([#3889](https://github.com/element-hq/element-android/issues/3889))
 - Issue templates: merge expected and actual results ([#3960](https://github.com/element-hq/element-android/issues/3960))


Changes in Element v1.2.0 (2021-08-12)
======================================

Features ✨
----------
 - Reorganise Advanced Notifications in to Default Notifications, Keywords and Mentions, Other (This feature is hidden in the release ui until a future release date.) ([#3646](https://github.com/element-hq/element-android/issues/3646))
 - Voice Message - Enable by default, remove from labs ([#3817](https://github.com/element-hq/element-android/issues/3817))

Bugfixes 🐛
----------
 - Voice Message - UI Improvements ([#3798](https://github.com/element-hq/element-android/issues/3798))
 - Stop VMs playing in the timeline if a new VM recording is started ([#3802](https://github.com/element-hq/element-android/issues/3802))


Changes in Element v1.1.16 (2021-08-09)
=======================================

Features ✨
----------
 - Spaces - Support Restricted Room via room capabilities API ([#3509](https://github.com/element-hq/element-android/issues/3509))
 - Spaces | Support restricted room access in room settings ([#3665](https://github.com/element-hq/element-android/issues/3665))

Bugfixes 🐛
----------
 - Fix crash when opening Troubleshoot Notifications ([#3778](https://github.com/element-hq/element-android/issues/3778))
 - Fix error when sending encrypted message if someone in the room logs out. ([#3792](https://github.com/element-hq/element-android/issues/3792))
 - Voice Message - Amplitude update java.util.ConcurrentModificationException ([#3796](https://github.com/element-hq/element-android/issues/3796))


Changes in Element v1.1.15 (2021-07-30)
=======================================

Features ✨
----------
 - Voice Message implementation (Currently under Labs Settings and disabled by default). ([#3598](https://github.com/element-hq/element-android/issues/3598))

SDK API changes ⚠️
------------------
 - updatePushRuleActions signature has been updated to more explicitly enabled/disable the rule and update the actions. It's behaviour has also been changed to match the web with the enable/disable requests being sent on every invocation and actions sent when needed(not null). ([#3681](https://github.com/element-hq/element-android/issues/3681))


Changes in Element 1.1.14 (2021-07-23)
======================================

Features ✨
----------
 - Add low priority section in DM tab ([#3463](https://github.com/element-hq/element-android/issues/3463))
 - Show missed call notification. ([#3710](https://github.com/element-hq/element-android/issues/3710))

Bugfixes 🐛
----------
 - Don't use the transaction ID of the verification for the request ([#3589](https://github.com/element-hq/element-android/issues/3589))
 - Avoid incomplete downloads in cache ([#3656](https://github.com/element-hq/element-android/issues/3656))
 - Fix a crash which can happen when user signs out ([#3720](https://github.com/element-hq/element-android/issues/3720))
 - Ensure OTKs are uploaded when the session is created ([#3724](https://github.com/element-hq/element-android/issues/3724))

SDK API changes ⚠️
------------------
 - Add initialState support to CreateRoomParams (#3713) ([#3713](https://github.com/element-hq/element-android/issues/3713))

Other changes
-------------
 - Apply grammatical fixes to the Server ACL timeline messages. ([#3721](https://github.com/element-hq/element-android/issues/3721))
 - Add tags in the log, especially for VoIP, but can be used for other features in the future ([#3723](https://github.com/element-hq/element-android/issues/3723))


Changes in Element v1.1.13 (2021-07-19)
=======================================

Features ✨
----------
 - Remove redundant mimetype (element-hq/element-web#2547) ([#3273](https://github.com/element-hq/element-android/issues/3273))
 - Room version capabilities and room upgrade support, better error feedback ([#3551](https://github.com/element-hq/element-android/issues/3551))
 - Add retry support in room addresses screen ([#3635](https://github.com/element-hq/element-android/issues/3635))
 - Better management of permission requests ([#3667](https://github.com/element-hq/element-android/issues/3667))

Bugfixes 🐛
----------
 - Standardise spelling and casing of homeserver, identity server, and integration manager. ([#491](https://github.com/element-hq/element-android/issues/491))
 - Perform .well-known request first, even if the entered URL is a valid homeserver base url ([#2843](https://github.com/element-hq/element-android/issues/2843))
 - Use different copy for self verification. ([#3624](https://github.com/element-hq/element-android/issues/3624))
 - Crash when opening room addresses screen with no internet connection ([#3634](https://github.com/element-hq/element-android/issues/3634))
 - Fix unread messages marker being hidden in collapsed membership item ([#3655](https://github.com/element-hq/element-android/issues/3655))
 - Ensure reaction emoji picker tabs look fine on small displays ([#3661](https://github.com/element-hq/element-android/issues/3661))

SDK API changes ⚠️
------------------
 - RawService.getWellknown() now takes a domain instead of a matrixId as parameter ([#3572](https://github.com/element-hq/element-android/issues/3572))


Changes in Element 1.1.12 (2021-07-05)
======================================

Features ✨
----------
 - Reveal password: use facility from com.google.android.material.textfield.TextInputLayout instead of manual handling. ([#3545](https://github.com/element-hq/element-android/issues/3545))
 - Implements new design for Jump to unread and quick fix visibility issues. ([#3547](https://github.com/element-hq/element-android/issues/3547))

Bugfixes 🐛
----------
 - Fix some issues with timeline cache invalidation and visibility. ([#3542](https://github.com/element-hq/element-android/issues/3542))
 - Fix call invite processed after call is ended because of fastlane mode. ([#3564](https://github.com/element-hq/element-android/issues/3564))
 - Fix crash after video call. ([#3577](https://github.com/element-hq/element-android/issues/3577))
 - Fix crash out of memory ([#3583](https://github.com/element-hq/element-android/issues/3583))
 - CryptoStore migration has to be object to avoid crash ([#3605](https://github.com/element-hq/element-android/issues/3605))


Changes in Element v1.1.11 (2021-06-22)
=======================================

Bugfixes 🐛
----------
 - Send button does not show up half of the time ([#3535](https://github.com/element-hq/element-android/issues/3535))
 - Fix crash on signout: release session at the end of clean up. ([#3538](https://github.com/element-hq/element-android/issues/3538))


Changes in Element v1.1.10 (2021-06-18)
=======================================

Features ✨
----------
 - Migrate DefaultTypingService, KeysImporter and KeysExporter to coroutines ([#2449](https://github.com/element-hq/element-android/issues/2449))
 - Update Message Composer design ([#3182](https://github.com/element-hq/element-android/issues/3182))
 - Cleanup Epoxy items, and debounce all the clicks ([#3435](https://github.com/element-hq/element-android/issues/3435))
 - Adds support for receiving MSC3086 Asserted Identity events. ([#3451](https://github.com/element-hq/element-android/issues/3451))
 - Migrate to new colors and cleanup the style and theme. Now exported in module :library:ui-styles
  Ref: https://material.io/blog/migrate-android-material-components ([#3459](https://github.com/element-hq/element-android/issues/3459))
 - Add option to set aliases for public spaces ([#3483](https://github.com/element-hq/element-android/issues/3483))
 - Add beta warning to private space creation flow ([#3485](https://github.com/element-hq/element-android/issues/3485))
 - User defined top level spaces ordering ([#3501](https://github.com/element-hq/element-android/issues/3501))

Bugfixes 🐛
----------
 - Fix new DMs not always marked as such ([#3333](https://github.com/element-hq/element-android/issues/3333))

SDK API changes ⚠️
------------------
 - Splits SessionAccountDataService and RoomAccountDataService and offers to query RoomAccountDataEvent at the session level. ([#3479](https://github.com/element-hq/element-android/issues/3479))

Other changes
-------------
 - Move the ability to start a call from dialpad directly to a dedicated tab in the home screen. ([#3457](https://github.com/element-hq/element-android/issues/3457))
 - VoIP: Change hold direction to send-only. ([#3467](https://github.com/element-hq/element-android/issues/3467))
 - Some improvements on DialPad (cursor edition, paste number, small fixes). ([#3516](https://github.com/element-hq/element-android/issues/3516))


Changes in Element v1.1.9 (2021-06-02)
======================================

Features ✨:
 - Upgrade Olm dependency to 3.2.4
 - Allow user to add custom "network" in room search (#1458)
 - Add Gitter.im as a default in the Change Network menu (#3196)
 - VoIP: support for virtual rooms (#3355)
 - Compress thumbnail: change Jpeg quality from 100 to 80 (#3396)
 - Inconsistent usage of the term homeserver in Settings (#3404)
 - VoIP: support attended transfer (#3420)
 - /snow -> /snowfall and update wording (iso Element Web) (#3430)

Bugfixes 🐛:
 - Fix | On Android it seems to be impossible to view the complete description of a Space (without dev tools) (#3401)
 - Fix | Suggest Rooms, Show a detailed view of the room on click (#3406)
 - Fix app crashing when signing out (#3424)
 - Switch to stable endpoint/fields for MSC2858 (#3442)

Changes in Element 1.1.8 (2021-05-25)
===================================================

Improvements 🙌:
 - Support Jitsi authentication (#3379)

Bugfix 🐛:
 - Space Invite by link not always displayed for public space (#3345)
 - Wrong copy in share space bottom sheet (#3346)
 - Fix a problem with database migration on nightly builds (#3335)
 - Implement a workaround to render &lt;del&gt; and &lt;u&gt; in the timeline (#1817)
 - Make sure the SDK can retrieve the secret storage if the system is upgraded (#3304)
 - Spaces | Explore room list: the RoomId is displayed instead of name (#3371)
 - Spaces | Personal spaces add DM - Web Parity (#3271)
 - Spaces | Improve 'Leave Space' UX/UI (#3359)
 - Don't create private spaces with encryption enabled (#3363)
 - #+ button on lower right when looking at an empty space goes to an empty 'Explore rooms' (#3327)

Build 🧱:
 - Compile with Kotlin 1.5.10.
 - Upgrade some dependencies: gradle wrapper, third party lib, etc.
 - Sign APK with build tools 30.0.3

Other changes:
 - Add documentation on LoginWizard and RegistrationWizard (#3303)
 - Setup towncrier tool (#3293)

 Security:
  - Element Android shares name of E2EE files with homeserver (#3387)

Changes in Element 1.1.7 (2021-05-12)
===================================================

Features ✨:
 - Spaces beta

Improvements 🙌:
 - Add ability to install APK from directly from Element (#2381)
 - Delete and react to stickers (#3250)
 - Compress video before sending (#442)
 - Improve file too big error detection (#3245)
 - User can now select video when selecting Gallery to send attachments to a room
 - Add option to record a video from the camera
 - Add the public icon on the rooms in the room list (#3292)

Bugfix 🐛:
 - Message states cosmetic changes (#3007)
 - Fix exception in rxSingle (#3180)
 - Do not invite the current user when creating a room (#3123)
 - Fix color issues when the system theme is changed (#2738)
 - Fix issues on Android 11 (#3067)
 - Fix issue when opening encrypted files (#3186)
 - Fix wording issue (#3242)
 - Fix missing sender information after edits (#3184)
 - Fix read marker not updating automatically (#3267)
 - Sent video does not contains duration (#3272)
 - Properly clean the back stack if the user cancel registration when waiting for email validation
 - Fix read marker visibility/position when filtering some events 
 - Fix user invitation in case of restricted profile api (#3306)

SDK API changes ⚠️:
 - RegistrationWizard.createAccount() parameters are now all optional, following Matrix spec (#3205)

Build 🧱:
 - Upgrade to gradle 7
 - https://github.com/Piasy/BigImageViewer is now hosted on mavenCentral()
 - Upgrade Realm to version 10.4.0

Other changes:
 - New store descriptions
 - `master` branch has been renamed to `main`. To apply change to your dev environment, run:
```sh
git branch -m master main
git fetch origin
git branch -u origin/main main
# And optionally
git remote prune origin
```
 - Allow cleartext (non-SSL) connections to Matrix servers on LAN hosts (#3166)

Changes in Element 1.1.6 (2021-04-16)
===================================================

Bugfix 🐛:
 - Fix crash on the timeline
 - App crashes on "troubleshoot notifications" button (#3187)

Changes in Element 1.1.5 (2021-04-15)
===================================================

Bugfix 🐛:
 - Fix crash during Realm migration
 - Fix crash when playing video (#3179)

Changes in Element 1.1.4 (2021-04-09)
===================================================

Improvements 🙌:
 - Split network request `/keys/query` into smaller requests (250 users max) (#2925)
 - Crypto improvement | Bulk send NO_OLM withheld code
 - Display the room shield in all room setting screens
 - Improve message with Emoji only detection (#3017)
 - Picture preview when replying. Also add the image preview in the message detail bottomsheet (#2916)
 - Api interceptor to allow app developers peek responses (#2986)
 - Update reactions to Unicode 13.1 (#2998)
 - Be more robust when parsing some enums
 - Improve timeline filtering (dissociate membership and profile events, display hidden events when highlighted, fix hidden item/read receipts behavior)
 - Add better support for empty room name fallback (#3106)
 - Room list improvements (paging)
 - Fix quick click action (#3127)
 - Get Event after a Push for a faster notification display in some conditions
 - Always try to retry Http requests in case of 429 (#1300)
 - registration availability endpoint added to matrix-sdk

Bugfix 🐛:
 - Fix bad theme change for the MainActivity
 - Handle encrypted reactions (#2509)
 - Disable URL preview for some domains (#2995)
 - Fix avatar rendering for DMs, after initial sync (#2693)
 - Fix mandatory parameter in API (#3065)
 - If signout request fails, do not start LoginActivity, but restart the app (#3099)
 - Retain keyword order in emoji import script, and update the generated file (#3147)

SDK API changes ⚠️:
 - Several Services have been migrated to coroutines (#2449)
 - Removes filtering options on Timeline.

Build 🧱:
 - Properly exclude gms dependencies in fdroid build flavour which were pulled in through the jitsi SDK (#3125)

Other changes:
 - Add version details on the login screen, in debug or developer mode
 - Migrate Retrofit interface to coroutine calls

Changes in Element 1.1.3 (2021-03-18)
===================================================

Bugfix 🐛:
 - Fix regression in UpdateTrustWorker (introduced in 1.1.2)
 - Timeline : Fix ripple effect on text item and fix background color of separators.

Changes in Element 1.1.2 (2021-03-16) (was not published tp GPlay prod)
===================================================

Improvements 🙌:
 - Lazy storage of ReadReceipts
 - Do not load room members in e2e after init sync

Bugfix 🐛:
 - Add option to cancel stuck messages at bottom of timeline see #516
 - Ensure message are decrypted in the room list after a clear cache
 - Regression: Video will not play upon tap, but only after swipe #2928
 - Cross signing now works with servers with an explicit port in the servername

Other changes:
 - Change formatting on issue templates to proper headings.

Changes in Element 1.1.1 (2021-03-10) (was not published tp GPlay prod)
===================================================

Improvements 🙌:
 - Allow non-HTTPS connections to homeservers on Tor (#2941)
 - Fetch homeserver type and version and display in a new setting screen and add info in rageshakes (#2831)
 - Improve initial sync performance - split into 2 transactions (#983)
 - PIP support for Jitsi call (#2418)
 - Add tooltip for room quick actions
 - Pre-share session keys when opening a room or start typing (#2771)
 - Sending is now queuing by room and not uniquely to the session
 - Improve Snackbar duration (#2929)
 - Improve sending message state (#2937)

Bugfix 🐛:
 - Try to fix crash about UrlPreview (#2640)
 - Be robust if Event.type is missing (#2946)
 - Snappier message send status
 - Fix MainActivity display (#2927)

Translations 🗣:
 - All string resources and translations have been moved to the application module. Weblate project for the SDK will be removed.

Build 🧱:
 - Update a lot of dependencies, with the help of dependabot.
 - Add a script to download and install APK from the CI

Other changes:
 - Rework edition of event management

Changes in Element 1.1.0 (2021-02-19)
===================================================

Features ✨:
 - VoIP : support for VoIP V1 protocol, transfer call and dial-pad
                                
Improvements 🙌:
 - VoIP : new tiles in timeline
 - Improve room profile UX
 - Upgrade Jitsi library from 2.9.3 to 3.1.0
 - a11y improvements

Bugfix 🐛:
 - VoIP : fix audio devices output
 - Fix crash after initial sync on Dendrite
 - Fix crash reported by PlayStore (#2707)
 - Ignore url override from credential if it is not valid (#2822)
 - Fix crash when deactivating an account

SDK API changes ⚠️:
 - Migrate AuthenticationService API to coroutines (#2449)

Other changes:
 - New Dev Tools panel for developers
 - Fix typos in CHANGES.md (#2811)
 - Colors rework: first step: merge file `colors_riot.xml` to file `colors_riotx.xml` and rename the file to `colors.xml`

Changes in Element 1.0.17 (2021-02-09)
===================================================

Improvements 🙌:
 - Create a WidgetItemFactory and use it for better rendering of Jitsi widget change (video conference)
 - Open image from URL Preview (#2705)

Bugfix 🐛:
 - Bug in WidgetContent.computeURL() (#2767)
 - Duplicate thumbs | Mobile reactions for 👍 and 👎 are not the same as web (#2776)
 - Join room by alias other federation error (#2778)
 - HTML unescaping for URL preview (#2766)
 - URL preview on reply fallback (#2756)
 - RTL: some arrows should be rotated in RTL (#2757)
 - Properly delete objects from Realm DB (#2765)

Build 🧱:
 - Upgrade build tools

Other changes:
 - Change app name from "Element (Riot.im)" to "Element"

Changes in Element 1.0.16 (2021-02-04)
===================================================

Bugfix 🐛:
 - Fix crash on API < 30 and light theme (#2774)

Changes in Element 1.0.15 (2021-02-03)
===================================================

Features ✨:
 - Social Login support

Improvements 🙌:
 - SSO support for cross signing (#1062)
 - Deactivate account when logged in with SSO (#1264)
 - SSO UIA doesn't work (#2754)

Bugfix 🐛:
 - Fix clear cache issue: sometimes, after a clear cache, there is still a token, so the init sync service is not started.
 - Sidebar too large in horizontal orientation or tablets (#475)
 - UrlPreview should be updated when the url is edited and changed (#2678)
 - When receiving a new pepper from identity server, use it on the next hash lookup (#2708)
 - Crashes reported by PlayStore (new in 1.0.14) (#2707)
 - Widgets: Support $matrix_widget_id parameter (#2748)
 - Data for Worker overload (#2721)
 - Fix multiple tasks
 - Object deletion in database is not complete (#2759)

SDK API changes ⚠️:
 - Increase targetSdkVersion to 30 (#2600)

Build 🧱:
 - Compile with Android SDK 30 (Android 11)

Other changes:
 - Update Dagger to 2.31 version so we can use the embedded AssistedInject feature 

Changes in Element 1.0.14 (2021-01-15)
===================================================

Features ✨:
 - Enable url previews for notices (#2562)
 - Edit room permissions (#2471)

Improvements 🙌:
 - Add System theme option and set as default (#904, #2387)
 - Store megolm outbound session to improve send time of first message after app launch.
 - Warn user when they are leaving a not public room (#1460)
 - Option to disable emoji keyboard (#2563)

Bugfix 🐛:
 - Unspecced msgType field in m.sticker (#2580)
 - Wait for all room members to be known before sending a message to a e2e room (#2518)
 - Url previews sometimes attached to wrong message (#2561)
 - Room Topic not displayed correctly after visiting a link (#2551)
 - Hiding membership events works the exact opposite (#2603)
 - Tapping drawer having more than 1 room in notifications gives "malformed link" error (#2605)
 - Sent image not displayed when opened immediately after sending (#409)
 - Initial sync is not retried correctly when there is some network error. (#2632)
 - Fix switch theme issue, and white field issue (#2599, #2528)
 - Fix request too large Uri error when joining a room

Translations 🗣:
 - New language supported: Hebrew

Build 🧱:
 - Remove dependency to org.greenrobot.eventbus library

Other changes:
 - Migrate to ViewBindings (#1072)

Changes in Element 1.0.13 (2020-12-18)
===================================================

Bugfix 🐛:
 - Fix MSC2858 implementation details (#2540)

Changes in Element 1.0.12 (2020-12-15)
===================================================

Features ✨:
 - Add room aliases management, and room directory visibility management in a dedicated screen (#1579, #2428)
 - Room setting: update join rules and guest access (#2442)
 - Url preview (#481)
 - Store encrypted file in cache and cleanup decrypted file at each app start (#2512)
 - Emoji Keyboard (#2520)
 - Social login (#2452)
 - Support for chat effects in timeline (confetti, snow) (#2535)

Improvements 🙌:
 - Add Setting Item to Change PIN (#2462)
 - Improve room history visibility setting UX (#1579)
 - Matrix.to deeplink custom scheme support
 - Homeserver history (#1933)

Bugfix 🐛:
 - Fix cancellation of sending event (#2438)
 - Double bottomsheet effect after verify with passphrase
 - EditText cursor jumps to the start while typing fast (#2469)
 - UTD for events before invitation if member state events are hidden (#2486)
 - No known servers error is given when joining rooms on new Gitter bridge (#2516)
 - Show preview when sending attachment from the keyboard (#2440)
 - Do not compress GIFs (#1616, #1254)

SDK API changes ⚠️:
 - StateService now exposes suspendable function instead of using MatrixCallback.
 - RawCacheStrategy has been moved and renamed to CacheStrategy
 - FileService: remove useless FileService.DownloadMode

Build 🧱:
 - Upgrade some dependencies and Kotlin version
 - Use fragment-ktx and preference-ktx dependencies (fix lint issue KtxExtensionAvailable)
 - Upgrade Realm dependency to 10.1.2

Other changes:
 - Remove "Status.im" theme #2424
 - Log HTTP requests and responses in production (level BASIC, i.e. without any private data)

Changes in Element 1.0.11 (2020-11-27)
===================================================

Features ✨:
 - Create DMs with users by scanning their QR code (#2025)
 - Add Invite friends quick invite actions (#2348)
 - Add friend by scanning QR code, show your code to friends (#2025)

Improvements 🙌:
 - New room creation tile with quick action (#2346)
 - Open an existing DM instead of creating a new one (#2319)
 - Use RoomMember instead of User in the context of a Room.
 - Ask for explicit user consent to send their contact details to the identity server (#2375)
 - Handle events of type "m.room.server_acl" (#890)
 - Room creation form: add advanced section to disable federation (#1314)
 - Move "Enable Encryption" from room setting screen to room profile screen (#2394)
 - Home empty screens quick design update (#2347)
 - Improve Invite user screen (seamless search for matrix ID)

Bugfix 🐛:
 - Fix crash on AttachmentViewer (#2365)
 - Exclude yourself when decorating rooms which are direct or don't have more than 2 users (#2370)
 - F-Droid version: ensure timeout of sync request can be more than 60 seconds (#2169)
 - Fix issue when restoring draft after sharing (#2287)
 - Fix issue when updating the avatar of a room (new avatar vanishing)
 - Discard change dialog displayed by mistake when avatar has been updated
 - Try to fix cropped image in timeline (#2126)
 - Registration: annoying error message scares every new user when they add an email (#2391)
 - Fix jitsi integration for those with non-vanilla dialler frameworks
 - Update profile has no effect if user is in zero rooms
 - Fix issues with matrix.to deep linking (#2349)

SDK API changes ⚠️:
 - AccountService now exposes suspendable function instead of using MatrixCallback (#2354).
   Note: We will incrementally migrate all the SDK API in a near future (#2449)

Test:
 - Add `allScreensTest` to cover all screens of the app

Other changes:
 - Upgrade Realm dependency to 10.0.0

Changes in Element 1.0.10 (2020-11-04)
===================================================

Improvements 🙌:
 - Rework sending Event management (#154)
 - New room creation screen: set topic and avatar in the room creation form (#2078)
 - Toggle Low priority tag (#1490)
 - Add option to send with enter (#1195)
 - Use Hardware keyboard enter to send message (use shift-enter for new line) (#1881, #1440)
 - Edit and remove icons are now visible on image attachment preview screen (#2294)
 - Room profile: BigImageViewerActivity now only display the image. Use the room setting to change or delete the room Avatar
 - Better visibility of text reactions in dark theme (#1118)
 - Room member profile: Add action to create (or open) a DM (#2310)
 - Prepare changelog for F-Droid (#2296)
 - Add graphic resources for F-Droid (#812, #2220)
 - Highlight text in the body of the displayed result (#2200)
 - Considerably faster QR-code bitmap generation (#2331)

Bugfix 🐛:
 - Fixed ringtone handling (#2100 & #2246)
 - Messages encrypted with no way to decrypt after SDK update from 0.18 to 1.0.0 (#2252)
 - Incoming call continues to ring if call is answered on another device (#1921)
 - Search Result | scroll jumps after pagination (#2238)
 - Badly formatted mentions in body (#1506)
 - KeysBackup: Avoid using `!!` (#2262)
 - Two elements in the task switcher (#2299)

Changes in Element 1.0.9 (2020-10-16)
===================================================

Features ✨:
 - Search messages in a room - phase 1 (#2110)
 - Hide encrypted history (before user is invited). Can be shown if wanted in developer settings
 - Changed rainbow algorithm

Improvements 🙌:
 - Wording differentiation for direct rooms (#2176)
 - PIN code: request PIN code if phone has been locked
 - Small optimisation of scrolling experience in timeline (#2114)
 - Allow user to reset cross signing if he has no way to recover (#2052)
 - Ability to share text
 - Create home shortcut for any room (#1525)
 - Can't confirm email due to killing by Android (#2021)
 - Add a menu item to open the setting in room list and in room (#2171)
 - Add a menu item in the timeline as a shortcut to invite user (#2171)
 - Drawer: move settings access and add sign out action (#2171)
 - Filter room member (and banned users) by name (#2184)
 - Implement "Jump to read receipt" and "Mention" actions on the room member profile screen
 - Direct share (#2029)
 - Add FAB to room members list (#2226)
 - Add Sygnal API implementation to test is Push are correctly received
 - Add PushGateway API implementation to test if Push are correctly received
 - Cross signing: shouldn't offer to verify with other session when there is not. (#2227)

Bugfix 🐛:
 - Improve support for image/audio/video/file selection with intent changes (#1376)
 - Fix Splash layout on small screens
 - Invalid popup when pressing back (#1635)
 - Simplifies draft management and should fix bunch of draft issues (#952, #683)
 - Very long topic cannot be fully visible (#1957)
 - Properly detect cross signing keys reset
 - Don't set presence when handling a push notification or polling (#2156)
 - Be robust against `StrandHogg` task injection
 - Clear alerts if user sign out
 - Fix rows are hidden in Textinput (#2234)
 - Uploading a file to a room caused it to have a info.size of -1 (#2141)

Translations 🗣:
 - Move store data to `/fastlane/metadata/android` (#812)
 - Weblate is now hosted at https://translate.element.io

SDK API changes ⚠️:
 - Search messages in a room by using Session.searchService() or Room.search()

Build 🧱:
 - Use Update Gradle Wrapper Action
 - Updates Gradle Wrapper from 5.6.4 to 6.6.1. (#2193)
 - Upgrade kotlin version from `1.3.72` to `1.4.10` and kotlin coroutines version from `1.3.8` to `1.3.9`
 - Upgrade build tools from `3.5.3` to `4.0.1`, then to `4.1.0`
 - Upgrade com.google.gms:google-services from `4.3.2` to `4.3.4`
 - Upgrade Moshi to `1.11.0`, Dagger to `2.29.1`, Epoxy to `4.1.0`

Other changes:
 - Added registration/verification automated UI tests
 - Create a script to help getting public information form any homeserver

Changes in Element 1.0.8 (2020-09-25)
===================================================

Improvements 🙌:
 - Add "show password" in import Megolm keys dialog
 - Visually disable call buttons in menu and prohibit calling when permissions are insufficient (#2112)
 - Better management of requested permissions (#2048)
 - Add a setting to show timestamp for all messages (#2123)
 - Use cache for user color
 - Allow using an outdated homeserver, at user's risk (#1972)
 - Restore small logo on login screens and fix scrolling issue on those screens
 - PIN Code Improvements: Add more settings: biometrics, grace period, notification content (#1985)

Bugfix 🐛:
 - Long message cannot be sent/takes infinite time & blocks other messages (#1397)
 - Fix crash when wellknown are malformed, or redirect to some HTML content (reported by rageshakes)
 - User Verification in DM not working
 - Manual import of Megolm keys does back up the imported keys
 - Auto scrolling to the latest message when sending (#2094)
 - Fix incorrect permission check when creating widgets (#2137)
 - Pin code: user has to enter pin code twice (#2005)

SDK API changes ⚠️:
 - Rename `tryThis` to `tryOrNull`

Other changes:
 - Add an advanced action to reset an account data entry

Changes in Element 1.0.7 (2020-09-17)
===================================================

Improvements 🙌:
 - Handle date formatting properly (show time am/pm if needed, display year when needed)
 - Improve F-Droid Notification (#2055)

Bugfix 🐛:
 - Clear the notification when the event is read elsewhere (#1822)
 - Speakerphone is not used for ringback tone (#1644, #1645)
 - Back camera preview is not mirrored anymore (#1776)
 - Various report of people that cannot play video (#2107)
 - Rooms incorrectly marked as unread (#588)
 - Allow users to show/hide room member state events (#1231) 
 - Fix stuck on loader when launching home

SDK API changes ⚠️:
 - Create a new RawService to get plain data from the server.

Other changes:
 - Performance: share Realm instance used on UI thread and improve SharedPreferences reading time.

Changes in Element 1.0.6 (2020-09-08)
===================================================

Features ✨:
 - List phone numbers and emails added to the Matrix account, and add emails and phone numbers to account (#44, #45)

Improvements 🙌:
 - You can now join room through permalink and within room directory search
 - Add long click gesture to copy userId, user display name, room name, room topic and room alias (#1774)
 - Fix several issues when uploading big files (#1889)
 - Do not propose to verify session if there is only one session and 4S is not configured (#1901)
 - Call screen does not use proximity sensor (#1735)

Bugfix 🐛:
 - Display name not shown under Settings/General (#1926)
 - Editing message forgets line breaks and markdown (#1939)
 - Words containing my name should not trigger notifications (#1781)
 - Fix changing language issue
 - Fix FontSize issue (#1483, #1787)
 - Fix bad color for settings icon on Android < 24 (#1786)
 - Change user or room avatar: when selecting Gallery, I'm not proposed to crop the selected image (#1590)
 - Loudspeaker is always used (#1685)
 - Fix uploads still don't work with room v6 (#1879)
 - Can't handle ongoing call events in background (#1992)
 - Handle room, user and group links by the Element app (#1795)
 - Update associated site domain (#1833)
 - Crash / Attachment viewer: Cannot draw a recycled Bitmap #2034
 - Login with Matrix-Id | Autodiscovery fails if identity server is invalid and Homeserver ok (#2027)
 - Support for image compression on Android 10
 - Verification popup won't show
 - Android 6: App crash when read Contact permission is granted (#2064)
 - JSON for verification events leaks in to the room list (#1246)
 - Replies to poll appears in timeline as unsupported events during sending (#1004)

Translations 🗣:
 - The SDK is now using SAS string translations from [Weblate Matrix-doc project](https://translate.element.io/projects/matrix-doc/) (#1909)
 - New translation to kabyle

Build 🧱:
 - Some dependencies have been upgraded (coroutine, recyclerView, appCompat, core-ktx, firebase-messaging)
 - Buildkite:
    New pipeline location: https://github.com/matrix-org/pipelines/blob/master/element-android/pipeline.yml
    New build location: https://buildkite.com/matrix-dot-org/element-android

Other changes:
 - Use File extension functions to make code more concise (#1996)
 - Create a script to import SAS strings (#1909)
 - Support `data-mx-[bg-]color` attributes on `<font>` tags.

Changes in Element 1.0.5 (2020-08-21)
===================================================

Features ✨:
 - Protect access to the app by a pin code (#1700)
 - Conference with Jitsi support (#43)

Improvements 🙌:
 - Share button in rooms gives room ID link without via parameters (#1927)
 - Give user the possibility to prevent accidental call (#1869)
 - Display device information (name, id and key) in Cryptography setting screen (#1784)
 - Ensure users do not accidentally ignore other users (#1890)
 - Better handling DM creation when invitees cannot be inviting due to denied federation
 - Support new config.json format and config.domain.json files (#1682)
 - Increase Font size on Calling screen (#1643)
 - Make the user's Avatar live in the general settings

Bugfix 🐛:
 - Fix incorrect date format for some Asian languages (#1928)
 - Fix invisible toolbar (Status.im theme) (#1746)
 - Fix relative date time formatting (#822)
 - Fix crash reported by RageShake
 - Fix refreshing of sessions list when another session is logged out
 - Fix IllegalArgumentException: Receiver not registered: NetworkInfoReceiver (#1960)
 - Failed to build unique file (#1954)
 - Highlighted Event when opening a permalink from another room (#1033)
 - A Kick appears has "someone has made no change" (#1959)
 - Avoid NetworkOnMainThreadException when setting a user avatar
 - Renew turnserver credentials when ttl runs out

Translations 🗣:
 - Add PlayStore description resources in the Triple-T format, to let Weblate handle them

SDK API changes ⚠️:
 - Rename package `im.vector.matrix.android` to `org.matrix.android.sdk`
 - Rename package `im.vector.matrix.rx` to `org.matrix.android.sdk.rx`

Build 🧱:
 - Fix RtlHardcoded issues (use `Start` and `End` instead of `Left` and `Right` layout attributes)

Other changes:
 - Use `Context#getSystemService` extension function provided by `core-ktx` (#1702)
 - Hide Flair settings, this is not implemented yet.
 - Rename package `im.vector.riotx.attachmentviewer` to `im.vector.lib.attachmentviewer`
 - Rename package `im.vector.riotx.multipicker` to `im.vector.lib.multipicker`
 - Rename package `im.vector.riotx` to `im.vector.app`
 - Remove old code that was used on devices with api level <21
 - Add Official Gradle Wrapper Validation Action

Changes in Element 1.0.4 (2020-08-03)
===================================================

Bugfix 🐛:
 - Fix Crash when opening invite to room user screen

Changes in Element 1.0.3 (2020-07-31)
===================================================

Features ✨:
 - Support server admin option to disable E2EE for DMs / private rooms [users can still enable] (#1794)

Bugfix 🐛:
 - Crash reported on playstore for HomeActivity launch (151 reports)

Changes in Element 1.0.2 (2020-07-29)
===================================================

Improvements 🙌:
 - Added Session Database migration to avoid unneeded initial syncs

Changes in Element 1.0.1 (2020-07-28)
===================================================

Improvements 🙌:
 - Sending events is now retried only 3 times, so we avoid blocking the sending queue too long.
 - Display warning when fail to send events in room list
 - Improve UI of edit role action in member profile
 - Moderation | New screen to display list of banned users in room settings, with unban action

Bugfix 🐛:
 - Fix theme issue on Room directory screen (#1613)
 - Fix notification not dismissing when entering a room
 - Fix uploads don't work with Room v6 (#1558)
 - Fix Requesting avatar thumbnails in Element uses wrong http "user-agent" string (#1725)
 - Fix 404 on EMS (#1761)
 - Fix Infinite loop at startup when migrating account from Riot (#1699)
 - Fix Element crashes in loop after initial sync (#1709)
 - Remove inner mx-reply tags before replying
 - Fix timeline items not loading when there are only filtered events
 - Fix "Voice & Video" grayed out in Settings (#1733)
 - Fix Allow VOIP call in all rooms with 2 participants (even if not DM)
 - Migration from old client does not enable notifications (#1723)

Other changes:
 - i18n deactivated account error

Changes in Element 1.0.0 (2020-07-15)
===================================================

Features ✨:
 - Re-branding: The app is now called Element. New name, new themes, new icons, etc. More details here: https://element.io/blog/welcome-to-element/ (#1691)

Bugfix 🐛:
 - Video calls are shown as a voice ones in the timeline (#1676)
 - Fix regression: not able to create a room without IS configured (#1679)

Changes in Riot.imX 0.91.5 (2020-07-11)
===================================================

Features ✨:
 - 3pid invite: it is now possible to invite people by email. An Identity Server has to be configured (#548)

Improvements 🙌:
 - Cleaning chunks with lots of events as long as a threshold has been exceeded (35_000 events in DB) (#1634)
 - Creating and listening to EventInsertEntity. (#1634)
 - Handling (almost) properly the groups fetching (#1634)
 - Improve fullscreen media display (#327)
 - Setup server recovery banner (#1648)
 - Set up SSSS from security settings (#1567)
 - New lab setting to add 'unread notifications' tab to main screen
 - Render third party invite event (#548)
 - Display three pid invites in the room members list (#548)

Bugfix 🐛:
 - Integration Manager: Wrong URL to review terms if URL in config contains path (#1606)
 - Regression Composer does not grow, crops out text (#1650)
 - Bug / Unwanted draft (#698)
 - All users seems to be able to see the enable encryption option in room settings (#1341)
 - Leave room only leaves the current version (#1656)
 - Regression |  Share action menu do not work (#1647)
 - verification issues on transition (#1555)
 - Fix issue when restoring keys backup using recovery key

SDK API changes ⚠️:
 - CreateRoomParams has been updated

Build 🧱:
 - Upgrade some dependencies
 - Revert to build-tools 3.5.3

Other changes:
 - Use Intent.ACTION_CREATE_DOCUMENT to save megolm key or recovery key in a txt file
 - Use `Context#withStyledAttributes` extension function (#1546)

Changes in Riot.imX 0.91.4 (2020-07-06)
===================================================

Features ✨:
 - Re-activate Wellknown support with updated UI (#1614)

Improvements 🙌:
 - Upload device keys only once to the homeserver and fix crash when no network (#1629)

Bugfix 🐛:
 - Fix crash when coming from a notification (#1601)
 - Fix Exception when importing keys (#1576)
 - File isn't downloaded when another file with the same name already exists (#1578)
 - saved images don't show up in gallery (#1324)
 - Fix reply fallback leaking sender locale (#429)

Build 🧱:
 - Fix lint false-positive about WorkManager (#1012)
 - Upgrade build-tools from 3.5.3 to 3.6.3
 - Upgrade gradle from 5.4.1 to 5.6.4

Changes in Riot.imX 0.91.3 (2020-07-01)
===================================================

Notes:
 - This version is the third beta version of RiotX codebase published as Riot-Android on the PlayStore.
 - Changelog below includes changes of v0.91.0, v0.91.1, and v0.91.2, because the first beta versions have been tagged and
 published from the branch feature/migration_from_legacy.
 - This version uses temporary name `Riot.imX`, to distinguish the app with RiotX app.

Features ✨:
 - Call with WebRTC support (##611)
 - Add capability to change the display name (#1529)

Improvements 🙌:
 - "Add Matrix app" menu is now always visible (#1495)
 - Handle `/op`, `/deop`, and `/nick` commands (#12)
 - Prioritising Recovery key over Recovery passphrase (#1463)
 - Room Settings: Name, Topic, Photo, Aliases, History Visibility (#1455)
 - Update user avatar (#1054)
 - Allow self-signed certificate (#1564)
 - Improve file download and open in timeline
 - Catchup tab is removed temporarily (#1565)
 - Render room avatar change (#1319)

Bugfix 🐛:
 - Fix dark theme issue on login screen (#1097)
 - Incomplete predicate in RealmCryptoStore#getOutgoingRoomKeyRequest (#1519)
 - User could not redact message that they have sent (#1543)
 - Use vendor prefix for non merged MSC (#1537)
 - Compress images before sending (#1333)
 - Searching by displayname is case sensitive (#1468)
 - Fix layout overlap issue (#1407)

Build 🧱:
 - Enable code optimization (Proguard)
 - SDK is now API level 21 minimum, and so RiotX (#405)

Other changes:
 - Use `SharedPreferences#edit` extension function consistently (#1545)
 - Use `retrofit2.Call.awaitResponse` extension provided by Retrofit 2. (#1526)
 - Fix minor typo in contribution guide (#1512)
 - Fix self-assignment of callback in `DefaultRoomPushRuleService#setRoomNotificationState` (#1520)
 - Random housekeeping clean-ups indicated by Lint (#1520, #1541)
 - Keys Backup API now use the unstable prefix (#1503)
 - Remove deviceId from /keys/upload/{deviceId} as not spec-compliant (#1502)

Changes in RiotX 0.22.0 (2020-06-15)
===================================================

Features ✨:
 - Integration Manager and Widget support (#48)
 - Send stickers (#51)

Improvements 🙌:
 - New wording for notice when current user is the sender
 - Hide "X made no changes" event by default in timeline (#1430)
 - Hide left rooms in breadcrumbs (#766)
 - Handle PowerLevel properly (#627)
 - Correctly handle SSO login redirection
 - SSO login is now performed in the default browser, or in Chrome Custom tab if available (#1400)
 - Improve checking of homeserver version support (#1442)
 - Add capability to add and remove a room from the favorites (#1217)

Bugfix 🐛:
 - Switch theme is not fully taken into account without restarting the app
 - Temporary fix to show error when user is creating an account on matrix.org with userId containing only digits (#1410)
 - Reply composer overlay stays on screen too long after send (#1169)
 - Fix navigation bar icon contrast on API in [21,27[ (#1342)
 - Fix status bar icon contrast on API in [21,23[
 - Wrong /query request (#1444)
 - Make Credentials.homeServer optional because it is deprecated (#1443)
 - Fix issue on dark themes, after alert popup dismiss

Other changes:
 - Send plain text in the body of events containing formatted body, as per https://matrix.org/docs/spec/client_server/latest#m-room-message-msgtypes
 - Update link to Modular url from "https://modular.im/" to "https://modular.im/services/matrix-hosting-riot" and open it using ChromeCustomTab

Changes in RiotX 0.21.0 (2020-05-28)
===================================================

Features ✨:
 - Identity server support (#607)
 - Switch language support (#41)
 - Display list of attachments of a room (#860)

Improvements 🙌:
 - Better connectivity lost indicator when airplane mode is on
 - Add a setting to hide redacted events (#951)
 - Render formatted_body for m.notice and m.emote (#1196)
 - Change icon to magnifying-glass to filter room (#1384)

Bugfix 🐛:
 - After jump to unread, newer messages are never loaded (#1008)
 - Fix issues with FontScale switch (#69, #645)
 - "Seen by" uses 12h time (#1378)
 - Enable markdown (if active) when sending emote (#734)
 - Screenshots for Rageshake now includes Dialogs such as BottomSheet (#1349)

SDK API changes ⚠️:
 - initialize with proxy configuration

Other changes:
 - support new key agreement method for SAS (#1374)

Changes in RiotX 0.20.0 (2020-05-15)
===================================================

Features ✨:
 - Add Direct Shortcuts (#652)

Improvements 🙌:
 - Invite member(s) to an existing room (#1276)
 - Improve notification accessibility with ticker text (#1226)
 - Support homeserver discovery from MXID (DISABLED: waiting for design) (#476)

Bugfix 🐛:
 - Fix | Verify Manually by Text crashes if private SSK not known (#1337)
 - Sometimes the same device appears twice in the list of devices of a user (#1329)
 - Random Crashes while doing sth with cross signing keys (#1364)
 - Crash | crash while restoring key backup (#1366)

SDK API changes ⚠️:
 - excludedUserIds parameter added to the UserService.getPagedUsersLive() function

Changes in RiotX 0.19.0 (2020-05-04)
===================================================

Features ✨:
 - Change password (#528)
 - Cross-Signing | Support SSSS secret sharing (#944)
 - Cross-Signing | Verify new session from existing session (#1134)
 - Cross-Signing | Bootstraping cross signing with 4S from mobile (#985)
 - Save media files to Gallery (#973)
 - Account deactivation (with password only) (#35)

Improvements 🙌:
 - Verification DM / Handle concurrent .start after .ready (#794)
 - Reimplementation of multiple attachment picker
 - Cross-Signing | Update Shield Logic for DM (#963)
 - Cross-Signing | Complete security new session design update (#1135)
 - Cross-Signing | Setup key backup as part of SSSS bootstrapping (#1201)
 - Cross-Signing | Gossip key backup recovery key (#1200)
 - Show room encryption status as a bubble tile (#1078)
 - UX/UI | Add indicator to home tab on invite (#957)
 - Cross-Signing | Restore history after recover from passphrase (#1214)
 - Cross-Sign | QR code scan confirmation screens design update (#1187)
 - Emoji Verification | It's not the same butterfly! (#1220)
 - Cross-Signing | Composer decoration: shields (#1077)
 - Cross-Signing | Migrate existing keybackup to cross signing with 4S from mobile (#1197)
 - Show a warning dialog if the text of the clicked link does not match the link target (#922)
 - Cross-Signing | Consider not using a spinner on the 'complete security' prompt (#1271)
 - Restart broken Olm sessions ([MSC1719](https://github.com/matrix-org/matrix-doc/pull/1719))
 - Cross-Signing | Hide Use recovery key when 4S is not setup (#1007)
 - Cross-Signing | Trust account xSigning keys by entering Recovery Key (select file or copy) #1199
 - E2E timeline decoration (#1279)
 - Manage Session Settings / Cross Signing update (#1295)
 - Cross-Signing | Review sessions toast update old vs new (#1293, #1306)

Bugfix 🐛:
 - Fix summary notification staying after "mark as read"
 - Missing avatar/displayname after verification request message (#841)
 - Crypto | RiotX sometimes rotate the current device keys (#1170)
 - RiotX can't restore cross signing keys saved by web in SSSS (#1174)
 - Cross- Signing | After signin in new session, verification paper trail in DM is off (#1191)
 - Failed to encrypt message in room (message stays in red), [thanks to pwr22] (#925)
 - Cross-Signing | web <-> riotX After QR code scan, gossiping fails (#1210)
 - Fix crash when trying to download file without internet connection (#1229)
 - Local echo are not updated in timeline (for failed & encrypted states)
 - Render image event even if thumbnail_info does not have mimetype defined (#1209)
 - RiotX now uses as many threads as it needs to do work and send messages (#1221)
 - Fix issue with media path (#1227)
 - Add user to direct chat by user id (#1065)
 - Use correct URL for SSO connection (#1178)
 - Emoji completion :tada: does not completes to 🎉 like on web (#1285)
 - Fix bad Shield Logic for DM (#963)

Translations 🗣:
 - Weblate now create PR directly to RiotX GitHub project

SDK API changes ⚠️:
 - Increase targetSdkVersion to 29

Build 🧱:
 - Compile with Android SDK 29 (Android Q)

Other changes:
 - Add a setting to prevent screenshots of the application, disabled by default (#1027)
 - Increase File Logger capacities ( + use dev log preferences)

Changes in RiotX 0.18.1 (2020-03-17)
===================================================

Improvements 🙌:
 - Implementation of /join command

Bugfix 🐛:
 - Message transitions in encrypted rooms are jarring #518
 - Images that failed to send are waiting to be sent forever #1145
 - Fix / Crashed when trying to send a gif from the Gboard #1136
 - Fix / Cannot click on key backup banner when new keys are available


Changes in RiotX 0.18.0 (2020-03-11)
===================================================

Improvements 🙌:
 - Share image and other media from e2e rooms (#677)
 - Add support for `/plain` command (#12)
 - Detect spaces in password if user fail to login (#1038)
 - FTUE: do not display a different color when encrypting message when not in developer mode.
 - Open room member profile from avatar of the room member state event (#935)
 - Restore the push rules configuration in the settings

Bugfix 🐛:
 - Fix crash on attachment preview screen (#1088)
 - "Share" option is not appearing in encrypted rooms for images (#1031)
 - Set "image/jpeg" as MIME type of images instead of "image/jpg" (#1075)
 - Self verification via QR code is failing (#1130)

SDK API changes ⚠️:
 - PushRuleService.getPushRules() now returns a RuleSet. Use getAllRules() on this object to get all the rules.

Build 🧱:
 - Upgrade ktlint to version 0.36.0
 - Pipeline file for Buildkite is now hosted on another Github repository: https://github.com/matrix-org/pipelines/blob/master/riotx-android/pipeline.yml

Other changes:
 - Restore availability to Chromebooks (#932)
 - Add a [documentation](./docs/integration_tests.md) to run integration tests

Changes in RiotX 0.17.0 (2020-02-27)
===================================================

Features ✨:
 - Secured Shared Storage Support (#984, #936)
 - It's now possible to select several rooms (with a possible mix of clear/encrypted rooms) when sharing elements to RiotX (#1010)
 - Media preview: media are previewed before being sent to a room (#1010)
 - Image edition: it's now possible to edit image before sending: crop, rotate, and delete actions are supported (#1010)
 - Sending image: image are sent to rooms with a reduced size. It's still possible to send original image file (#1010)

Improvements 🙌:
 - Migrate to binary QR code verification (#994)
 - Share action is added to room profile and room member profile (#858)
 - Display avatar in fullscreen (#861)
 - Fix some performance issues with crypto

Bugfix 🐛:
 - Account creation: wrongly hints that an email can be used to create an account (#941)
 - Fix crash in the room directory, when public room has no name (#1023)
 - Fix restoring keys backup with passphrase (#526)
 - Fix rotation of full-size image (#647)
 - Fix joining rooms from directory via federation isn't working. (#808)
 - Leaving a room creates a stuck "leaving room" loading screen. (#1041)
 - Fix some invitation handling issues (#1013)
 - New direct chat: selecting a participant sometimes results in two breadcrumbs (#1022)
 - New direct chat: selecting several participants was not adding the room to the direct chats list
 - Room overview shows deleted messages as “Encrypted message” (#758)

SDK API changes ⚠️:
 - Get crypto methods through Session.cryptoService()
 - ProgressListener.onProgress() function will be invoked on the background thread instead of UI thread
 - Improve CreateRoomParams API (#1070)

Changes in RiotX 0.16.0 (2020-02-14)
===================================================

Features ✨:
 - Polls and Bot Buttons (MSC 2192 matrix-org/matrix-doc#2192)

Improvements 🙌:
 - Show confirmation dialog before deleting a message (#967, #1003)
 - Open room member profile from reactions list and read receipts list (#875)

Bugfix 🐛:
 - Fix crash by removing all notifications after clearing cache (#878)
 - Fix issue with verification when other client declares it can only show QR code (#988)
 - Fix too errors in the code (1941862499c9ec5268cc80882512ced379cafcfd, a250a895fe0a4acf08c671e03434edcd29ccd84f)

SDK API changes ⚠️:
 - Javadoc improved for PushersService
 - PushersService.pushers() has been renamed to PushersService.getPushers()

Changes in RiotX 0.15.0 (2020-02-10)
===================================================

Improvements 🙌:
 - Improve navigation to the timeline (#789, #862)
 - Improve network detection. It is now based on the sync request status (#873, #882)

Other changes:
 - Support SSO login with Firefox account (#606)

Bugfix 🐛:
 - Ask for permission before opening the camera (#934)
 - Encrypt for invited users by default, if the room state allows it (#803)

Changes in RiotX 0.14.3 (2020-02-03)
===================================================

Bugfix 🐛:
 - Fix Exception in DeviceListManager

Changes in RiotX 0.14.2 (2020-02-02)
===================================================

Bugfix 🐛:
 - Fix RiotX not starting issue

Changes in RiotX 0.14.1 (2020-02-02)
===================================================

Bugfix 🐛:
 - Cross-signing: fix UX issue when closing the bottom sheet verification (#813)
 - Room and room member profile: fix issues on dark and black themes

Changes in RiotX 0.14.0 (2020-02-01)
===================================================

Features ✨:
 - First implementation of Cross-signing
 - Enable encryption in unencrypted rooms, from the room settings (#212)
 - Negotiate E2E by default for DMs (#907)

Improvements 🙌:
 - Sharing things to RiotX: sort list by recent room first (#771)
 - Hide the algorithm when turning on e2e (#897)
 - Sort room members by display names

Other changes:
 - Add support for /rainbow and /rainbowme commands (#879)

Build 🧱:
 - Ensure builds are reproducible (#842)
 - F-Droid: fix the "-dev" issue in version name (#815)

Changes in RiotX 0.13.0 (2020-01-17)
===================================================

Features ✨:
 - Send and render typing events (#564)
 - Create Room Profile screen (#54)
 - Create Room Member Profile screen (#59)

Improvements 🙌:
 - Render events m.room.encryption and m.room.guest_access in the timeline

Bugfix 🐛:
 - Fix broken background sync in F-Droid version
 - Fix issue with downloaded file on encrypted rooms. The file was not properly decrypted

Build 🧱:
 - Change the way versionCode is computed (#827)

Changes in RiotX 0.12.0 (2020-01-09)
===================================================

Improvements 🙌:
 - The initial sync is now handled by a foreground service
 - Render aliases and canonical alias change in the timeline
 - Introduce developer mode in the settings (#745, #796)
 - Improve devices list screen
 - Add settings for rageshake sensibility
 - Fix autocompletion issues and add support for rooms, groups, and emoji (#780)
 - Show skip to bottom FAB while scrolling down (#752)
 - Enable encryption on a room, SDK part (#212)

Other changes:
 - Change the way RiotX identifies a session to allow the SDK to support several sessions with the same user (#800)
 - Exclude play-services-oss-licenses library from F-Droid build (#814)
 - Email domain can be limited on some homeservers, i18n of the displayed error (#754)

Bugfix 🐛:
 - Fix crash when opening room creation screen from the room filtering screen
 - Fix avatar image disappearing (#777)
 - Fix read marker banner when permalink
 - Fix joining upgraded rooms (#697)
 - Fix matrix.org room directory not being browsable (#807)
 - Hide non working settings (#751)

Changes in RiotX 0.11.0 (2019-12-19)
===================================================

Features ✨:
 - Implement soft logout (#281)

Improvements 🙌:
 - Handle navigation to room via room alias (#201)
 - Open matrix.to link in RiotX (#57)
 - Limit sticker size in the timeline

Other changes:
 - Use same default room colors than Riot-Web

Bugfix 🐛:
 - Scroll breadcrumbs to top when opened
 - Render default room name when it starts with an emoji (#477)
 - Do not display " (IRC)" in display names https://github.com/element-hq/riot-android/issues/444
 - Fix rendering issue with HTML formatted body
 - Disable click on Stickers (#703)

Build 🧱:
 - Include diff-match-patch sources as dependency

Changes in RiotX 0.10.0 (2019-12-10)
===================================================

Features ✨:
 - Breadcrumbs: switch from one room to another quickly (#571)

Improvements 🙌:
 - Support entering a RiotWeb client URL instead of the homeserver URL during connection (#744)

Other changes:
 - Add reason for all membership events (https://github.com/matrix-org/matrix-doc/pull/2367)

Bugfix 🐛:
 - When automardown is ON, pills are sent as MD in body (#739)
 - "ban" event are not rendered correctly (#716)
 - Fix crash when rotating screen in Room timeline

Changes in RiotX 0.9.1 (2019-12-05)
===================================================

Bugfix 🐛:
 - Fix an issue with DB transaction (#740)

Changes in RiotX 0.9.0 (2019-12-05)
===================================================

Features ✨:
 - Account creation. It's now possible to create account on any homeserver with RiotX (#34)
 - Iteration of the login flow (#613)
 - [SDK] MSC2241 / verification in DMs (#707)

Improvements 🙌:
 - Send mention Pills from composer
 - Links in message preview in the bottom sheet are now active. 
 - Rework the read marker to make it more usable

Other changes:
 - Fix a small grammatical error when an empty room list is shown.

Bugfix 🐛:
 - Do not show long click help if only invitation are displayed
 - Fix emoji filtering not working
 - Fix issue of closing Realm in another thread (#725)
 - Attempt to properly cancel the crypto module when user signs out (#724)

Changes in RiotX 0.8.0 (2019-11-19)
===================================================

Features ✨:
 - Handle long click on room in the room list (#395)
 - Ignore/UnIgnore users, and display list of ignored users (#542, #617)

Improvements 🙌:
 - Search reaction by name or keyword in emoji picker
 - Handle code tags (#567)
 - Support spoiler messages
 - Support m.sticker and m.room.join_rules events in timeline

Other changes:
 - Markdown set to off by default (#412)
 - Accessibility improvements to the attachment file type chooser

Bugfix 🐛:
 - Fix issues with some member events rendering (#498)
 - Passphrase does not match (Export room keys) (#644)
 - Ask for permission to write external storage when uri comes from the keyboard (#658)
 - Fix issue with english US/GB translation (#671)

Changes in RiotX 0.7.0 (2019-10-24)
===================================================

Features:
 - Share elements from other app to RiotX (#58)
 - Read marker (#84)
 - Add ability to report content (#515)

Improvements:
 - Persist active tab between sessions (#503)
 - Do not upload file too big for the homeserver (#587)
 - Attachments: start using system pickers (#52)
 - Mark all messages as read (#396)


Other changes:
 - Accessibility improvements to read receipts in the room timeline and reactions emoji chooser

Bugfix:
 - Fix issue on upload error in loop (#587)
 - Fix opening a permalink: the targeted event is displayed twice (#556)
 - Fix opening a permalink paginates all the history up to the last event (#282)
 - after login, the icon in the top left is a green 'A' for (all communities) rather than my avatar (#267)
 - Picture uploads are unreliable, pictures are shown in wrong aspect ratio on desktop client (#517)
 - Invitation notifications are not dismissed automatically if room is joined from another client (#347)
 - Opening links from RiotX reuses browser tab (#599)

Changes in RiotX 0.6.1 (2019-09-24)
===================================================

Bugfix:
 - Fix crash: MergedHeaderItem was missing dimensionConverter

Changes in RiotX 0.6.0 (2019-09-24)
===================================================

Features:
 - Save draft of a message when exiting a room with non empty composer (#329)

Improvements:
 - Add unread indent on room list (#485)
 - Message Editing: Update notifications (#128)
 - Remove any notification of a redacted event (#563)

Other changes:
 - Fix a few accessibility issues

Bugfix:
 - Fix characters erased from the Search field when the result are coming (#545)
 - "No connection" banner was displayed by mistake
 - Leaving community (from another client) has no effect on RiotX (#497)
 - Push rules was not retrieved after a clear cache
 - m.notice messages trigger push notifications (#238)
 - Embiggen messages with multiple emojis also for edited messages (#458)

Build:
 - Fix (again) issue with bad versionCode generated by Buildkite (#553)

Changes in RiotX 0.5.0 (2019-09-17)
===================================================

Features:
 - Implementation of login to homeserver with SSO (#557)
 - Handle M_CONSENT_NOT_GIVEN error (#64)
 - Auto configure homeserver and identity server URLs of LoginActivity with a magic link

Improvements:
 - Reduce default release build log level, and lab option to enable more logs.
 - Display a no network indicator when there is no network (#559)

Bugfix:
 - Fix crash due to missing informationData (#535)
 - Progress in initial sync dialog is decreasing for a step and should not (#532)
 - Fix rendering issue of accepted third party invitation event
 - All current notifications were dismissed by mistake when the app is launched from the launcher

Build:
 - Fix issue with version name (#533)
 - Fix issue with bad versionCode generated by Buildkite (#553)

Changes in RiotX 0.4.0 (2019-08-30)
===================================================

Features:
 - Display read receipts in timeline (#81)

Improvements:
 - Reactions: Reinstate the ability to react with non-unicode keys (#307)

Bugfix:
 - Fix text diff linebreak display (#441)
 - Date change message repeats for each redaction until a normal message (#358)
 - Slide-in reply icon is distorted (#423)
 - Regression / e2e replies not encrypted
 - Some video won't play
 - Privacy: remove log of notifiable event (#519)
 - Fix crash with EmojiCompat (#530)

Changes in RiotX 0.3.0 (2019-08-08)
===================================================

Features:
 - Create Direct Room flow
 - Handle `/markdown` command

Improvements:
 - UI for pending edits (#193)
 - UX image preview screen transition (#393)
 - Basic support for resending failed messages (retry/remove)
 - Enable proper cancellation of suspending functions (including db transaction)
 - Enhances network connectivity checks in SDK
 - Add "View Edit History" item in the message bottom sheet (#401)
 - Cancel sync request on pause and timeout to 0 after pause (#404)

Other changes:
 - Show sync progress also in room detail screen (#403)

Bugfix:
 - Edited message: link confusion when (edited) appears in body (#398)
 - Close detail room screen when the room is left with another client (#256)
 - Clear notification for a room left on another client
 - Fix messages with empty `in_reply_to` not rendering (#447)
 - Fix clear cache (#408) and Logout (#205)
 - Fix `(edited)` link can be copied to clipboard (#402)
 - KeyBackup / SSSS | Should get the key from SSSS instead of asking recovery Key (#1163)

Build:
 - Split APK: generate one APK per arch, to reduce APK size of about 30%


Changes in RiotX 0.2.0 (2019-07-18)
===================================================

Features:
 - Message Editing: View edit history (#121)
 - Rooms filtering (#304)
 - Edit in encrypted room

Improvements:
 - Handle click on redacted events: view source and create permalink
 - Improve long tap menu: reply on top, more compact (#368)
 - Quick reply in timeline with swipe gesture (#167)
 - Improve edit of replies
 - Improve performance on Room Members and Users management (#381)

Other changes:
 - migrate from rxbinding 2 to rxbinding 3

Bugfix:
 - Fix regression on permalink click
 - Fix crash reported by the PlayStore (#341)
 - Fix Chat composer separator color in dark/black theme
 - Fix bad layout for room directory filter (#349)
 - Fix Copying link from a message shouldn't open context menu (#364)

Changes in RiotX 0.1.0 (2019-07-11)
===================================================

First release!

Mode details here: https://medium.com/@RiotChat/introducing-the-riotx-beta-for-android-b17952e8f771
