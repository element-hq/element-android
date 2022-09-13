#!/usr/bin/env bash

echo "Deleted existing plan..."
rm vector/src/main/java/im/vector/app/features/analytics/plan/*.*

echo "Cloning analytics project..."
mkdir analytics_tmp
cd analytics_tmp
git clone https://github.com/matrix-org/matrix-analytics-events.git

echo "Copy plan..."
cp matrix-analytics-events/types/kotlin2/* ../vector/src/main/java/im/vector/app/features/analytics/plan/

echo "Cleanup."
cd ..
rm -rf analytics_tmp

echo "Done."
