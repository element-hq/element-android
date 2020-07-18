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
    local file="$1"
    local color_shell="$2"
    local color_shell_dark="$3"
    # shell color
    sed -i "s|#199834|$color_shell|gi" "$file"
    sed -i "s|#044204|$color_shell_dark|gi" "$file"
}

logo_alternative() {
    logo_replace_color "$mydir/graphics/ic_launcher_foreground_sc.svg" "$@"
    logo_replace_color "$mydir/graphics/ic_launcher_sc.svg" "$@"
    "$mydir/graphics/icon_gen.sh"
}

case "$package_add" in
"a")
    # cyan
    logo_alternative "#00ACC1" "#006064"
    ;;
"b")
    # orange
    logo_alternative "#FB8C00" "#E65100"
    ;;
"c")
    # purple
    logo_alternative "#5E35B1" "#311B92"
    ;;
"d")
    # red
    logo_alternative "#E53935" "#B71C1C"
    ;;
esac

build_gradle="$mydir/vector/build.gradle"
sed -i "s|SchildiChat|SchildiChat.$name_add|g" "$build_gradle"
sed -i "s|de.spiritcroc.riotx|de.spiritcroc.riotx.$package_add|g" "$build_gradle"
