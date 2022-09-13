#!/bin/bash
./gradlew $CI_GRADLE_ARG_PROPERTIES -Pandroid.testInstrumentationRunnerArguments.class=org.matrix.android.sdk.session.room.timeline.ChunkEntityTest matrix-sdk-android:connectedAndroidTest
./gradlew $CI_GRADLE_ARG_PROPERTIES -Pandroid.testInstrumentationRunnerArguments.class=org.matrix.android.sdk.session.room.timeline.TimelineForwardPaginationTest matrix-sdk-android:connectedAndroidTest
