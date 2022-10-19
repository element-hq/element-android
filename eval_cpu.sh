#!/bin/bash
#set -x
#INITIAL_CPU="`cat /proc/stat | grep 'cpu '`"
RESULT_CPU="`cat /proc/stat | grep 'cpu '`"

show() {
  NAME=$1
  OFFSET=$2
  BEFORE=`echo $INITIAL_CPU | cut -d\  -f $OFFSET`
  AFTER=`echo $RESULT_CPU | cut -d\  -f $OFFSET`
  echo "$NAME $BEFORE $AFTER ( $(($AFTER - $BEFORE)) )"
}

show "user   ", 1 
show "nice   ", 2 
show "system ", 3 
show "idle   ", 4 
show "iowait ", 5 
show "irq    ", 6 
show "softirq", 7 
show "steal  ", 8 
show "guest  ", 9 
show "guest_nice", 10 
