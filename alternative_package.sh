#!/bin/bash
# Convert app to a different package with different icon and name,
# to allow multiple installations on the same device.

package_add="$1"
name_add="$2"
mydir="$(dirname "$(realpath "$0")")"

if [ -z "$package_add" ] || [ -z "$name_add" ]; then
    echo "Usage: $0 <package_add> <name_add>"
    exit 1
fi

logo_replace_color() {
    file="$1"
    color1="$2"
    color2="$3"
    # color 600
    sed -i "s|#7CB342|$color1|gi" "$file"
    # color 200
    sed -i "s|#C5E1A5|$color2|gi" "$file"
}

logo_alternative() {
    color1="$1"
    color2="$2"
    logo_replace_color "$mydir/graphics/ic_launcher_round.svg" "$color1" "$color2"
    logo_replace_color "$mydir/graphics/ic_launcher.svg" "$color1" "$color2"
    logo_replace_color "$mydir/graphics/riot_splash_0_green.svg" "$color1" "$color2"
    logo_replace_color "$mydir/vector/src/main/res/drawable-anydpi-v26/ic_launcher_foreground.xml" "$color1" "$color2"
    "$mydir/graphics/icon_gen.sh"
}

case "$package_add" in
"a")
    # cyan
    logo_alternative "#00ACC1" "#80DEEA"
    ;;
"b")
    # orange
    logo_alternative "#FB8C00" "#FFCC80"
    ;;
"c")
    # purple
    logo_alternative "#5E35B1" "#B39DDB"
    ;;
"d")
    # red
    logo_alternative "#E53935" "#EF9A9A"
    ;;
esac

build_gradle="$mydir/vector/build.gradle"
sed -i "s|SchildiChat|SchildiChat.$name_add|g" "$build_gradle"
sed -i "s|de.spiritcroc.riotx|de.spiritcroc.riotx.$package_add|g" "$build_gradle"
