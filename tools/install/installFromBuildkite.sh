#!/usr/bin/env bash

# Exit on any error
set -e

if [[ "$#" -ne 1 ]]; then
  echo "Usage: $0 BUILDKITE_TOKEN" >&2
  exit 1
fi

buildkiteToken=$1

# Path where the app is cloned (it's where this project has been cloned)
appPath=$(dirname $(dirname $(dirname $0)))
# Path where the APK will be downloaded from Buildkite (it's a dir)
baseImportPath="${appPath}/tmp/DebugApks"

# Select device
serialNumber=$(${appPath}/tools/install/androidSelectDevice.sh)

# Detect device architecture

arch=$(adb -s ${serialNumber} shell getprop ro.product.cpu.abi)

# FDroid or Gplay ?
echo
read -p "fdroid or gplay (default to gplay)? " fdroidOrGplay
fdroidOrGplay=${fdroidOrGplay:-gplay}

echo
echo "Will install ${fdroidOrGplay} version on device ${serialNumber} with arch ${arch}"

# Buildkite build number
echo
read -p "Buildkite build number (ex: '1792')? " buildkiteBuildNumber

# Download files

targetPath=${baseImportPath}/${buildkiteBuildNumber}

filename="vector-${fdroidOrGplay}-${arch}-debug.apk"

fullApkPath="${targetPath}/${filename}"

# Check if file already exists
if test -f "$fullApkPath"; then
    read -p "$fullApkPath already exists. Override (yes/no) default to no ? " download
    download=${download:-no}
else
    download="yes"
fi

# Ignore error from now
set +e

if [ ${download} == "yes" ]; then
    echo "Downloading ${filename}..."
    python3 ${appPath}/tools/release/download_buildkite_artifacts.py \
        --token ${buildkiteToken} \
        --build ${buildkiteBuildNumber} \
        --directory ${targetPath} \
        --filename ${filename} \
        --ignoreErrors
fi

echo "Installing ${filename} to device ${serialNumber}..."
adb -s ${serialNumber} install -r ${fullApkPath}

# Check error and propose to uninstall and retry installing
if [[ "$?" -ne 0 ]]; then
    read -p "Error, do you want to uninstall the application then retry (yes/no) default to no ? " retry
    retry=${retry:-no}
    if [ ${retry} == "yes" ]; then
        echo "Uninstalling..."
        adb -s ${serialNumber} uninstall im.vector.app.debug
        echo "Installing again..."
        adb -s ${serialNumber} install -r ${fullApkPath}
    fi
fi
