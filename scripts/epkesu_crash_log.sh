#!/system/bin/sh
# Collect ApkeSU startup crash logs from Android/Termux or from a PC with adb.

PKG="io.github.fixz.apkesu"
WAIT_SECONDS=8
MODE="collect"
LAUNCH_APP=1

usage() {
    cat <<EOF
Usage: sh epkesu_crash_log.sh [options]

Options:
  -p, --package NAME   Package name, default: $PKG
  -w, --wait SECONDS   Seconds to wait after launching, default: $WAIT_SECONDS
      --no-launch      Do not force-stop or launch the app
      --live           Show filtered live logcat instead of saving files
  -h, --help           Show this help

Examples:
  sh epkesu_crash_log.sh
  sh epkesu_crash_log.sh --live
  sh epkesu_crash_log.sh --wait 15
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        -p|--package)
            PKG="$2"
            shift 2
            ;;
        -w|--wait)
            WAIT_SECONDS="$2"
            shift 2
            ;;
        --no-launch)
            LAUNCH_APP=0
            shift
            ;;
        --live)
            MODE="live"
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

FILTER='AndroidRuntime|FATAL EXCEPTION|Fatal signal|DEBUG|crash_dump|tombstone|SIGSYS|SIGSEGV|SIGABRT|libkernelsu|libc\.so|io\.github\.fixz\.apkesu|KernelSUApplication|MainActivity|MainActivityViewModel|HomeViewModel|SettingsViewModel|KsuCli|Build fingerprint|pid:|backtrace'

has_cmd() {
    command -v "$1" >/dev/null 2>&1
}

find_cmd() {
    NAME="$1"
    if has_cmd "$NAME"; then
        command -v "$NAME"
        return 0
    fi
    for DIR in /system/bin /system/xbin /vendor/bin /apex/com.android.runtime/bin /data/data/com.termux/files/usr/bin; do
        if [ -x "$DIR/$NAME" ]; then
            echo "$DIR/$NAME"
            return 0
        fi
    done
    echo "$NAME"
}

LOGCAT="$(find_cmd logcat)"
GETPROP="$(find_cmd getprop)"
AM="$(find_cmd am)"
MONKEY="$(find_cmd monkey)"
LS="$(find_cmd ls)"
TAIL="$(find_cmd tail)"
GREP="$(find_cmd grep)"

adb_ready() {
    has_cmd adb && adb get-state >/dev/null 2>&1
}

local_root_ready() {
    has_cmd su && su -c true >/dev/null 2>&1
}

run_device_shell() {
    if adb_ready; then
        adb shell "$@"
    elif local_root_ready; then
        su -c "$*"
    else
        "$@"
    fi
}

run_logcat_clear() {
    if adb_ready; then
        adb logcat -c
    elif local_root_ready; then
        su -c "$LOGCAT -c"
    else
        "$LOGCAT" -c
    fi
}

run_logcat_dump() {
    if adb_ready; then
        adb logcat -d -v time
    elif local_root_ready; then
        su -c "$LOGCAT -d -v time"
    else
        "$LOGCAT" -d -v time
    fi
}

run_logcat_live() {
    if adb_ready; then
        adb logcat -v time
    elif local_root_ready; then
        su -c "$LOGCAT -v time"
    else
        "$LOGCAT" -v time
    fi
}

run_getprop() {
    if adb_ready; then
        adb shell getprop
    elif [ -x "$GETPROP" ] || has_cmd "$GETPROP"; then
        "$GETPROP"
    else
        echo "getprop not available"
    fi
}

grep_filter() {
    if [ -x "$GREP" ] || has_cmd "$GREP"; then
        "$GREP" -iE "$FILTER|$PKG" "$FULL_LOG"
    else
        echo "grep not available; use logcat-full.txt"
    fi
}

tail_file() {
    if [ -x "$TAIL" ] || has_cmd "$TAIL"; then
        "$TAIL" -n 80 "$1"
    else
        cat "$1"
    fi
}

timestamp() {
    date +%Y%m%d-%H%M%S
}

make_out_dir() {
    TS="$(timestamp)"
    if adb_ready; then
        OUT_DIR="./epkesu-log-$TS"
    else
        OUT_DIR="/sdcard/Download/epkesu-log-$TS"
    fi
    mkdir -p "$OUT_DIR" || exit 1
    echo "$OUT_DIR"
}

if [ "$MODE" = "live" ]; then
    echo "Showing filtered live logcat for $PKG. Press Ctrl+C to stop."
    if [ -x "$GREP" ] || has_cmd "$GREP"; then
        run_logcat_live | "$GREP" -iE "$FILTER|$PKG"
    else
        run_logcat_live
    fi
    exit $?
fi

OUT_DIR="$(make_out_dir)"
FULL_LOG="$OUT_DIR/logcat-full.txt"
FILTERED_LOG="$OUT_DIR/logcat-crash-filtered.txt"
PROPS_LOG="$OUT_DIR/device-props.txt"
TOMBSTONE_LIST="$OUT_DIR/tombstones.txt"

echo "Output directory: $OUT_DIR"
echo "Clearing old logcat..."
run_logcat_clear >/dev/null 2>&1 || true

if [ "$LAUNCH_APP" = "1" ]; then
    echo "Launching $PKG ..."
    run_device_shell "$AM" force-stop "$PKG" >/dev/null 2>&1 || true
    run_device_shell "$MONKEY" -p "$PKG" 1 >/dev/null 2>&1 || true
    echo "Waiting $WAIT_SECONDS seconds for crash..."
    sleep "$WAIT_SECONDS"
fi

echo "Saving device properties..."
run_getprop > "$PROPS_LOG" 2>&1 || true

echo "Saving logcat..."
run_logcat_dump > "$FULL_LOG" 2>&1 || true
grep_filter > "$FILTERED_LOG" 2>/dev/null || true

echo "Saving tombstone list..."
if adb_ready; then
    {
        adb shell ls -lt /data/tombstones 2>/dev/null
        adb shell su -c "ls -lt /data/tombstones" 2>/dev/null
    } > "$TOMBSTONE_LIST" 2>&1 || true
elif local_root_ready; then
    su -c "$LS -lt /data/tombstones" > "$TOMBSTONE_LIST" 2>&1 || true
else
    "$LS" -lt /data/tombstones > "$TOMBSTONE_LIST" 2>&1 || true
fi

echo
echo "Done."
echo "Send these files back:"
echo "  $FILTERED_LOG"
echo "  $FULL_LOG"
echo "  $PROPS_LOG"
echo "  $TOMBSTONE_LIST"
echo
echo "Quick view:"
tail_file "$FILTERED_LOG" 2>/dev/null || true
