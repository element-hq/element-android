#! /bin/bash
#=====================================================================
# Selects an android device
# Copyright (C) 2012-2020 Diego Torres Milano. All rights reserved.
#
# See:
#    - http://dtmilano.blogspot.ca/2013/01/android-select-device.html
#    - http://dtmilano.blogspot.ca/2012/03/selecting-adb-device.html
# for details on usage.
#=====================================================================

# BMA: GIST FROM https://gist.github.com/dtmilano/4537110

get_adb_devices() {
    adb $ADB_SERVER devices $LONG 2>&1 | tail -n +2 | sed '/^$/d'
}

git_pull() {
    [[ "$UNAME" == 'Linux' ]] && OPT=-e
    ASD=$(readlink $OPT $0)
    if [[ -n "$ASD" ]]
    then
        DIR=$(dirname $ASD)
        if [[ -n "$DIR" && -d "$DIR/.git" ]]
        then
            (cd $DIR && git pull)
        fi
    fi
}

PROGNAME=$(basename $0)
VERSION="3.3.0"
UNAME=$(uname)
DEVICE_OPT=
LONG=
ADB_SERVER=

git_pull > /dev/null

for opt in "$@"
do
    case "$opt" in
        -d|-e|-s|-t)
            DEVICE_OPT=$opt
            ;;

        -l|--long)
            LONG=-l
            ;;

        start-server|kill-server|connect|pair|-help)
            exit 0
            ;;

        -V|--version)
            echo "$PROGNAME version $VERSION"
            exit 0
            ;;
    esac
done
[ -n "$DEVICE_OPT" ] && exit 0
DEV=$(get_adb_devices)
if [ -z "$DEV" ]
then
    echo "$PROGNAME: ERROR: There's no locally connected devices." >&2
    read -p "Do you want to connect to a remote adb server? [Y/n]: " REPLY
    case "$REPLY" in
        n|N)
            exit 1
            ;;

        y|Y|"")
            read -p "ADB server IP: " IP
            ADB_SERVER="-H $IP"
            DEV=$(get_adb_devices)
            ;;
    esac
elif echo "$DEV" | grep -q 'daemon started successfully'
then
    # try again
    DEV=$(get_adb_devices)
fi
N=$(echo "$DEV" | wc -l | sed 's/ //g')

case $N in
1)
    # only one device detected
    D=$DEV
    ;;

*)
    # more than one device detected
    OLDIFS=$IFS
    IFS="
"
    PS3="Select the device to use, <Q> to quit: "
    select D in $DEV
    do
        [ "$REPLY" = 'q' -o "$REPLY" = 'Q' ] && exit 2
        [ -n "$D" ] && break
    done < /dev/tty

    IFS=$OLDIFS
    ;;
esac

if [ -z "$D" ]
then
    echo "$PROGNAME: ERROR: target device couldn't be determined" >&2
    exit 1
fi

# this didn't work on Darwin
# echo "-s ${D%% *}"
### BMA Modified printf -- '-s %s\n' "$(echo ${D} | sed 's/ .*$//')"
printf -- '%s' "$(echo ${D} | sed 's/ .*$//')"
