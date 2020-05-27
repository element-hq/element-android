#!/bin/bash

mydir="$(dirname "$(realpath "$0")")"

base_out="$mydir/../vector/src/main/res"

file="$mydir/riot_splash_0_green.svg"

dpi=96
base_folder="$mydir/../vector/src/main/res/drawable"

newfile="$(basename "$file" .svg).png"
mkdir -p $base_folder-mdpi
mkdir -p $base_folder-hdpi
mkdir -p $base_folder-xhdpi
mkdir -p $base_folder-xxhdpi
mkdir -p $base_folder-xxxhdpi
inkscape "$file" --export-filename="$base_folder-mdpi/$newfile" -C --export-dpi=$dpi
inkscape "$file" --export-filename="$base_folder-hdpi/$newfile" -C --export-dpi=$(($dpi*3/2))
inkscape "$file" --export-filename="$base_folder-xhdpi/$newfile" -C --export-dpi=$(($dpi*2))
inkscape "$file" --export-filename="$base_folder-xxhdpi/$newfile" -C --export-dpi=$(($dpi*3))
inkscape "$file" --export-filename="$base_folder-xxxhdpi/$newfile" -C --export-dpi=$(($dpi*4))
