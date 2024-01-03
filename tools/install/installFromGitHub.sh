#!/usr/bin/env bash

# Exit on any error
set -e

if [[ "$#" -ne 1 ]]; then
  echo "Usage: $0 GitHub_Token" >&2
  echo "Read more about this script in the doc ./docs/installing_from_ci.md"
  exit 1
fi

gitHubToken=$1

# Path where the app is cloned (it's where this project has been cloned)
appPath=$(dirname $(dirname $(dirname $0)))
# Path where the APK will be downloaded from CI (it's a dir)
baseImportPath="${appPath}/tmp/DebugApks"

# Select device
serialNumber=$(${appPath}/tools/install/androidSelectDevice.sh)

# Detect device architecture
arch=$(adb -s ${serialNumber} shell getprop ro.product.cpu.abi)

echo
echo "Will install the application on device ${serialNumber} with arch ${arch}"

# Artifact URL
echo
read -p "Artifact url (ex: https://github.com/element-hq/element-android/suites/9293388174/artifacts/435942121)? " artifactUrl

## Example of default value for Gplay
#artifactUrl=${artifactUrl:-https://github.com/element-hq/element-android/suites/9293388174/artifacts/435942121}
## Example of default value for FDroid
# artifactUrl=${artifactUrl:-https://github.com/element-hq/element-android/suites/9293388174/artifacts/435942119}

artifactId=$(echo ${artifactUrl} | rev | cut -d'/' -f1 | rev)

# Download files
targetPath=${baseImportPath}/${artifactId}

filename="artifact.zip"

fullFilePath="${targetPath}/${filename}"

# Check if file already exists
if test -f "$fullFilePath"; then
    read -p "$fullFilePath already exists. Override (yes/no) default to no ? " download
    download=${download:-no}
else
    download="yes"
fi

# Ignore error from now
set +e

if [ ${download} == "yes" ]; then
    echo "Downloading ${filename} to ${targetPath}..."
    python3 ${appPath}/tools/release/download_github_artifacts.py \
        --token ${gitHubToken} \
        --artifactUrl ${artifactUrl} \
        --directory ${targetPath} \
        --filename ${filename} \
        --ignoreErrors
fi

echo "Unzipping ${filename}..."
unzip $fullFilePath -d ${targetPath}

## gplay or fdroid
if test -d "${targetPath}/gplay"; then
  variant="gplay"
elif test -d "${targetPath}/fdroid"; then
  variant="fdroid"
else
  echo "No variant found"
  exit 1
fi

fullApkPath="${targetPath}/${variant}/debug/vector-${variant}-${arch}-debug.apk"

echo "Installing ${fullApkPath} to device ${serialNumber}..."
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
