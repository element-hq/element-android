#!/bin/bash
# Convert app to a different package with different icon and name,
# to allow multiple installations on the same device.

package_add="$1"
name_add="$2"
mydir="$(dirname "$(realpath "$0")")"

source "$mydir/merge_helpers.sh"

if [ -z "$package_add" ] || [ -z "$name_add" ]; then
    echo "Usage: $0 <package_add> <name_add>"
    exit 1
fi

require_clean_git

build_gradle="$mydir/vector/build.gradle"
src_dir="$mydir/vector/src"
fastlane_dir="$mydir/fastlane"

if grep -q "de.spiritcroc.riotx.$package_add" "$build_gradle"; then
    echo "Abort, $package_add already active"
    exit 0
fi

logo_replace_color() {
    local file="$1"
    local color_shell="$2"
    local color_shell_dark="$3"
    local color_bg="$4"
    # shell color
    sed -i "s|#8BC34A|$color_shell|gi" "$file"
    sed -i "s|#33691E|$color_shell_dark|gi" "$file"
    # bg color
    sed -i "s|#e2f0d2|$color_bg|gi" "$file"
}

logo_alternative() {
    logo_replace_color "$mydir/graphics/ic_launcher_foreground_sc.svg" "$@"
    logo_replace_color "$mydir/graphics/ic_launcher_sc.svg" "$@"
    logo_replace_color "$mydir/graphics/feature_image.svg" "$@"
    logo_replace_color "$mydir/graphics/store_icon.svg" "$@"
    logo_replace_color "$mydir/vector/src/main/res/mipmap-anydpi-v26/ic_launcher_background_sc.xml" "$@"
    "$mydir/graphics/icon_gen.sh"
}

logo_beta() {
    cp "$mydir/graphics/beta/"* "$mydir/graphics/"
    "$mydir/graphics/icon_gen.sh"
}

case "$package_add" in
"a")
    # blue
    logo_alternative "#2196F3" "#0D47A1" "#BBDEFB"
    ;;
"b")
    # orange: 900 color recuded in value
    logo_alternative "#FB8C00" "#7f2c00" "#FFE0B2"
    ;;
"c")
    # red: 900 color reduced in value
    logo_alternative "#E53935" "#4c0b0b" "#FFCDD2"
    ;;
"d")
    # purple
    logo_alternative "#5E35B1" "#311B92" "#D1C4E9"
    ;;
"e")
    # pink
    logo_alternative "#D81B60" "#880E4F" "#F8BBD0"
    ;;
"x")
    # cyan
    logo_alternative "#00ACC1" "#006064" "#B2EBF2"
    ;;
"z")
    # white
    logo_alternative "#ffffff" "#000000" "#eeeeee"
    ;;
esac

sed -i "s|SchildiChat|SchildiChat.$name_add|g" "$build_gradle"
sed -i "s|de.spiritcroc.riotx|de.spiritcroc.riotx.$package_add|g" "$build_gradle" `find "$src_dir" -name google-services.json`
sed -i "s|SchildiChat|SchildiChat.$name_add|g" `find "$fastlane_dir/metadata/android" -name "title.txt"`


if [ "$package_add" = "testing.foss" ]; then
    find "$fastlane_dir" -name full_description.txt -exec cp "$fastlane_dir/../fastlane_testing/foss_full_description.txt" '{}' \;
    find "$fastlane_dir" -name short_description.txt -exec cp "$fastlane_dir/../fastlane_testing/foss_short_description.txt" '{}' \;
    logo_beta
elif [ "$package_add" = "testing.fcm" ]; then
    find "$fastlane_dir" -name full_description.txt -exec cp "$fastlane_dir/../fastlane_testing/fcm_full_description.txt" '{}' \;
    find "$fastlane_dir" -name short_description.txt -exec cp "$fastlane_dir/../fastlane_testing/fcm_short_description.txt" '{}' \;
    logo_beta
fi

git add -A
git commit -m "Switch to alternative $name_add ($package_add)"
