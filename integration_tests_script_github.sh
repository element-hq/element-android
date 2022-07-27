#!/bin/bash
./gradlew "-Porg.gradle.jvmargs=-XX:MaxRAM=7g -Xmx2g -XX:MaxMetaspaceSize=1g" -Porg.gradle.parallel=false -Dfile.encoding=UTF-8 -Pandroid.testInstrumentationRunnerArguments.class=org.matrix.android.sdk.session.room.timeline.ChunkEntityTest matrix-sdk-android:connectedAndroidTest
./gradlew "-Porg.gradle.jvmargs=-XX:MaxRAM=7g -Xmx2g -XX:MaxMetaspaceSize=1g" -Porg.gradle.parallel=false -Dfile.encoding=UTF-8 -Pandroid.testInstrumentationRunnerArguments.class=org.matrix.android.sdk.session.room.timeline.TimelineForwardPaginationTest matrix-sdk-android:connectedAndroidTest
