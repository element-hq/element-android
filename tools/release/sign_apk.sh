#!/usr/bin/env bash

set -e

if [[ -z "${ANDROID_HOME}" ]]; then
    echo "Env variable ANDROID_HOME is not set, should be set to something like ~/Library/Android/sdk"
    exit 1
fi

if [[ "$#" -ne 2 ]]; then
  echo "Usage: $0 KEYSTORE_PATH APK" >&2
  exit 1
fi

# Get the command line parameters
PARAM_KEYSTORE_PATH=$1
PARAM_APK=$2

# Other params
BUILD_TOOLS_VERSION="30.0.3"
MIN_SDK_VERSION=21

echo "Signing APK with build-tools version ${BUILD_TOOLS_VERSION} for min SDK version ${MIN_SDK_VERSION}..."

APK_SIGNER_PATH=${ANDROID_HOME}/build-tools/${BUILD_TOOLS_VERSION}

${APK_SIGNER_PATH}/apksigner sign \
    -v \
    --ks ${PARAM_KEYSTORE_PATH} \
    --min-sdk-version ${MIN_SDK_VERSION} \
    ${PARAM_APK}

# Verify the signature
echo "Verifying the signature..."

# Note: we ignore warning on META-INF files
${APK_SIGNER_PATH}/apksigner verify \
    -v \
    --min-sdk-version ${MIN_SDK_VERSION} \
    ${PARAM_APK} \
    | grep -v "WARNING: META-INF/"

echo
echo "Congratulations! The APK ${PARAM_APK} is now signed!"
