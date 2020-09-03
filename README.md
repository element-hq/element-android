[![Buildkite](https://badge.buildkite.com/ad0065c1b70f557cd3b1d3d68f9c2154010f83c4d6f71706a9.svg?branch=develop)](https://buildkite.com/matrix-dot-org/element-android/builds?branch=develop)
[![Weblate](https://translate.riot.im/widgets/element-android/-/svg-badge.svg)](https://translate.riot.im/engage/element-android/?utm_source=widget)
[![Element Android Matrix room #element-android:matrix.org](https://img.shields.io/matrix/element-android:matrix.org.svg?label=%23element-android:matrix.org&logo=matrix&server_fqdn=matrix.org)](https://matrix.to/#/#element-android:matrix.org)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=im.vector.app.android&metric=alert_status)](https://sonarcloud.io/dashboard?id=im.vector.app.android)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=im.vector.app.android&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=im.vector.app.android)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=im.vector.app.android&metric=bugs)](https://sonarcloud.io/dashboard?id=im.vector.app.android)

# Element Android

Element Android is an Android Matrix Client provided by [Element](https://element.io/).

It is a total rewrite of [Riot-Android](https://github.com/vector-im/riot-android) with a new user experience.

[<img src="resources/img/google-play-badge.png" alt="Get it on Google Play" height="60">](https://play.google.com/store/apps/details?id=im.vector.app)
[<img src="resources/img/f-droid-badge.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/app/im.vector.app)

Nightly build: [![Buildkite](https://badge.buildkite.com/ad0065c1b70f557cd3b1d3d68f9c2154010f83c4d6f71706a9.svg?branch=develop)](https://buildkite.com/matrix-dot-org/element-android/builds?branch=develop)

# New Android SDK

Element is based on a new Android SDK fully written in Kotlin (like Element). In order to make the early development as fast as possible, Element and the new SDK currently share the same git repository.

At each Element release, the SDK module is copied to a dedicated repository: https://github.com/matrix-org/matrix-android-sdk2. That way, third party apps can add a regular gradle dependency to use it. So more details on how to do that here: https://github.com/matrix-org/matrix-android-sdk2.

# Roadmap

The version 1.0.0 of Element still misses some features which was previously included in Riot-Android.
The team will work to add them on a regular basis.

## Contributing

Please refer to [CONTRIBUTING.md](https://github.com/vector-im/element-android/blob/develop/CONTRIBUTING.md) if you want to contribute on Matrix Android projects!

Come chat with the community in the dedicated Matrix [room](https://matrix.to/#/#element-android:matrix.org).
