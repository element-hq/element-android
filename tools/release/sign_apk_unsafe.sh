#!/usr/bin/env bash

# Copy and adaptation of ./sign_apk.sh, which takes 2 more params: key store pass and key pass.
# It's unsafe to use it because it takes password as parameter, so passwords will
# remain in the terminal history.

set -e

if [[ -z "${ANDROID_HOME}" ]]; then
    echo "Env variable ANDROID_HOME is not set, should be set to something like ~/Library/Android/sdk"
    exit 1
fi

if [[ "$#" -ne 4 ]]; then
  echo "Usage: $0 KEYSTORE_PATH APK KS_PASS KEY_PASS" >&2
  exit 1
fi

# Get the command line parameters
PARAM_KEYSTORE_PATH=$1
PARAM_APK=$2
PARAM_KS_PASS=$3
PARAM_KEY_PASS=$4

# Other params
BUILD_TOOLS_VERSION="30.0.3"
MIN_SDK_VERSION=21

echo "Signing APK with build-tools version ${BUILD_TOOLS_VERSION} for min SDK version ${MIN_SDK_VERSION}..."

APK_SIGNER_PATH=${ANDROID_HOME}/build-tools/${BUILD_TOOLS_VERSION}

${APK_SIGNER_PATH}/apksigner sign \
    -v \
    --ks ${PARAM_KEYSTORE_PATH} \
    --ks-pass pass:${PARAM_KS_PASS} \
    --ks-key-alias riot.im \
    --key-pass pass:${PARAM_KEY_PASS} \
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
