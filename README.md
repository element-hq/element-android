[![Latest build](https://github.com/element-hq/element-android/actions/workflows/build.yml/badge.svg?query=branch%3Adevelop)](https://github.com/element-hq/element-android/actions/workflows/build.yml?query=branch%3Adevelop)
[![Weblate](https://translate.element.io/widgets/element-android/-/svg-badge.svg)](https://translate.element.io/engage/element-android/?utm_source=widget)
[![Element Android Matrix room #element-android:matrix.org](https://img.shields.io/matrix/element-android:matrix.org.svg?label=%23element-android:matrix.org&logo=matrix&server_fqdn=matrix.org)](https://matrix.to/#/#element-android:matrix.org)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=element-android&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=element-android)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=element-android&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=element-android)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=element-android&metric=bugs)](https://sonarcloud.io/summary/new_code?id=element-android)

# Element Android

Element Android is an Android Matrix Client provided by [Element](https://element.io/). The app can be run on every Android devices with Android OS Lollipop and more (API 21).

It is a total rewrite of [Riot-Android](https://github.com/element-hq/riot-android) with a new user experience.

[<img src="resources/img/google-play-badge.png" alt="Get it on Google Play" height="60">](https://play.google.com/store/apps/details?id=im.vector.app)
[<img src="resources/img/f-droid-badge.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/app/im.vector.app)

Build of develop branch: [![GitHub Action](https://github.com/element-hq/element-android/actions/workflows/build.yml/badge.svg?query=branch%3Adevelop)](https://github.com/element-hq/element-android/actions/workflows/build.yml?query=branch%3Adevelop) Nightly test status: [![allScreensTest](https://github.com/element-hq/element-android/actions/workflows/nightly.yml/badge.svg)](https://github.com/element-hq/element-android/actions/workflows/nightly.yml)


# New Android SDK

Element is based on a new Android SDK fully written in Kotlin (like Element). In order to make the early development as fast as possible, Element and the new SDK currently share the same git repository.

At each Element release, the SDK module is copied to a dedicated repository: https://github.com/matrix-org/matrix-android-sdk2. That way, third party apps can add a regular gradle dependency to use it. So more details on how to do that here: https://github.com/matrix-org/matrix-android-sdk2.

# Roadmap

The version 1.0.0 of Element still misses some features which was previously included in Riot-Android.
The team will work to add them on a regular basis.

# Releases to app stores

There is some delay between when a release is created and when it appears in the app stores (Google Play Store and F-Droid). Here are some of the reasons:

* Not all versioned releases that appear on GitHub are considered stable. Each release is first considered beta: this continues for at least two days. If the release is stable (no serious issues or crashes are reported), then it is released as a production release in Google Play Store, and a request is sent to F-Droid too.
* Each release on the Google Play Store undergoes review by Google before it comes out. This can take an unpredictable amount of time. In some cases it has taken several weeks.
* In order for F-Droid to guarantee that the app you receive exactly matches the public source code, they build releases themselves. When a release is considered stable, Element staff inform the F-Droid maintainers and it is added to the build queue. Depending on the load on F-Droid's infrastructure, it can take some time for releases to be built. This always takes at least 24 hours, and can take several days.

If you would like to receive releases more quickly (bearing in mind that they may not be stable) you have a number of options:

1. [Sign up to receive beta releases](https://play.google.com/apps/testing/im.vector.app) via the Google Play Store.
2. Install a [release APK](https://github.com/element-hq/element-android/releases) directly - download the relevant .apk file and allow installing from untrusted sources in your device settings.  Note: these releases are the Google Play version, which depend on some Google services.  If you prefer to avoid that, try the latest dev builds, and choose the F-Droid version.
3. If you're really brave, install the [very latest dev build](https://github.com/element-hq/element-android/actions/workflows/build.yml?query=branch%3Adevelop) - pick a build, then click on `Summary` to download the APKs from there: `vector-Fdroid-debug` and `vector-Gplay-debug` contains the APK for the desired store. Each file contains 5 APKs. 4 APKs for every supported specific architecture of device. In doubt you can install the `universal` APK.

## Contributing

Please refer to [CONTRIBUTING.md](./CONTRIBUTING.md) if you want to contribute on Matrix Android projects!

Come chat with the community in the dedicated Matrix [room](https://matrix.to/#/#element-android:matrix.org).

Also [this documentation](./docs/_developer_onboarding.md) can hopefully help developers to start working on the project.

## Triaging issues

Issues are triaged by community members and the Android App Team, following the [triage process](https://github.com/element-hq/element-meta/wiki/Triage-process).

We use [issue labels](https://github.com/element-hq/element-meta/wiki/Issue-labelling) to sort all incoming issues.

## Copyright and License

Copyright (c) 2018 - 2025 New Vector Ltd

This software is dual licensed by New Vector Ltd (Element). It can be used either:

(1) for free under the terms of the GNU Affero General Public License (as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version); OR

(2) under the terms of a paid-for Element Commercial License agreement between you and Element (the terms of which may vary depending on what you and Element have agreed to).

Unless required by applicable law or agreed to in writing, software distributed under the Licenses is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses.
