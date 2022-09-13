#!/bin/bash
./gradlew  -Pandroid.testInstrumentationRunnerArguments.class=org.matrix.android.sdk.session.room.timeline.ChunkEntityTest matrix-sdk-android:connectedAndroidTest
./gradlew  -Pandroid.testInstrumentationRunnerArguments.class=org.matrix.android.sdk.session.room.timeline.TimelineForwardPaginationTest matrix-sdk-android:connectedAndroidTest
