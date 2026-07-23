#!/system/bin/sh

SERVICE_VERSION=2
DAEMON_MARKER=APKESU_FOREGROUND_TOOL_DAEMON=1
BASE=/data/adb/apkesu/foreground_tools
ENABLED="$BASE/enabled"
TARGETS="$BASE/targets.list"
TOOLS="$BASE/tools.list"
STATUS="$BASE/status.properties"
LOG="$BASE/events.log"
PID_FILE="$BASE/service.pid"
LOCK_DIR="$BASE/service.lock"
SERVICE=/data/adb/service.d/97-apkesu-foreground-tools.sh
POLL_SECONDS=2
RECHECK_SECONDS=10
STATUS_HEARTBEAT_SECONDS=30

mkdir -p "$BASE"
chmod 0700 "$BASE" 2>/dev/null

valid_package() {
    printf '%s' "$1" | grep -Eq '^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$'
}

is_running_service_pid() {
    pid="$1"
    case "$pid" in
        ''|*[!0-9]*) return 1 ;;
    esac
    [ -r "/proc/$pid/cmdline" ] || return 1
    tr '\000' ' ' < "/proc/$pid/cmdline" 2>/dev/null | grep -Fq "$SERVICE" && return 0
    [ -r "/proc/$pid/environ" ] || return 1
    tr '\000' '\n' < "/proc/$pid/environ" 2>/dev/null | grep -Fxq "$DAEMON_MARKER"
}

log_event() {
    printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$1" >> "$LOG"
    chmod 0600 "$LOG" 2>/dev/null
    lines=$(wc -l < "$LOG" 2>/dev/null)
    case "$lines" in
        ''|*[!0-9]*) return ;;
    esac
    if [ "$lines" -gt 240 ]; then
        tail -n 160 "$LOG" > "$LOG.tmp.$$" 2>/dev/null && mv -f "$LOG.tmp.$$" "$LOG"
    fi
}

# service.d must return promptly. The actual watcher runs in a detached child.
if [ "${1:-}" != "--daemon" ]; then
    [ -f "$ENABLED" ] || exit 0
    old_pid=$(cat "$PID_FILE" 2>/dev/null)
    is_running_service_pid "$old_pid" && exit 0
    if command -v nohup >/dev/null 2>&1 && command -v setsid >/dev/null 2>&1; then
        APKESU_FOREGROUND_TOOL_DAEMON=1 nohup setsid sh "$SERVICE" --daemon </dev/null >/dev/null 2>&1 &
    elif command -v nohup >/dev/null 2>&1; then
        APKESU_FOREGROUND_TOOL_DAEMON=1 nohup sh "$SERVICE" --daemon </dev/null >/dev/null 2>&1 &
    elif command -v setsid >/dev/null 2>&1; then
        APKESU_FOREGROUND_TOOL_DAEMON=1 setsid sh "$SERVICE" --daemon </dev/null >/dev/null 2>&1 &
    else
        APKESU_FOREGROUND_TOOL_DAEMON=1 sh "$SERVICE" --daemon </dev/null >/dev/null 2>&1 &
    fi
    exit 0
fi

# mkdir is used as an Android-compatible single-instance lock.
if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    old_pid=$(cat "$PID_FILE" 2>/dev/null)
    is_running_service_pid "$old_pid" && exit 0
    rmdir "$LOCK_DIR" 2>/dev/null || exit 0
    mkdir "$LOCK_DIR" 2>/dev/null || exit 0
fi

printf '%s\n' "$$" > "$PID_FILE"
chmod 0600 "$PID_FILE" 2>/dev/null

last_status_fingerprint=""
last_status_epoch=0

write_status() {
    state="$1"
    foreground="$2"
    matched="$3"
    stopped="$4"
    failed="$5"
    event="$6"
    now_epoch=$(date +%s)
    fingerprint="$state|$foreground|$matched|$stopped|$failed|$event"
    if [ "$fingerprint" = "$last_status_fingerprint" ] &&
        [ $((now_epoch - last_status_epoch)) -lt "$STATUS_HEARTBEAT_SECONDS" ]; then
        return
    fi
    temp="$STATUS.tmp.$$"
    {
        printf 'version=%s\n' "$SERVICE_VERSION"
        printf 'state=%s\n' "$state"
        printf 'foreground=%s\n' "$foreground"
        printf 'matched=%s\n' "$matched"
        printf 'stopped=%s\n' "$stopped"
        printf 'failed=%s\n' "$failed"
        printf 'event=%s\n' "$event"
        printf 'updated=%s\n' "$(date '+%Y-%m-%d %H:%M:%S')"
        printf 'pid=%s\n' "$$"
    } > "$temp"
    chmod 0600 "$temp" 2>/dev/null
    mv -f "$temp" "$STATUS"
    last_status_fingerprint="$fingerprint"
    last_status_epoch="$now_epoch"
}

current_android_user() {
    user=$(am get-current-user 2>/dev/null |
        grep -oE '[0-9]+' | tail -n 1)
    if [ -z "$user" ]; then
        user=$(cmd activity get-current-user 2>/dev/null |
            grep -oE '[0-9]+' | tail -n 1)
    fi
    case "$user" in
        ''|*[!0-9]*) user=0 ;;
    esac
    printf '%s\n' "$user"
}

extract_foreground_record() {
    fallback_user="$1"
    while IFS= read -r line || [ -n "$line" ]; do
        package=$(printf '%s\n' "$line" |
            grep -oE '([A-Za-z][A-Za-z0-9_]*\.)+[A-Za-z][A-Za-z0-9_]*/' |
            head -n 1 | cut -d/ -f1)
        valid_package "$package" || continue
        user=$(printf '%s\n' "$line" |
            grep -oE '(^|[[:space:]])u[0-9]+([[:space:]]|$)' |
            head -n 1 | grep -oE '[0-9]+')
        case "$user" in
            ''|*[!0-9]*) user="$fallback_user" ;;
        esac
        printf '%s|%s\n' "$user" "$package"
        return 0
    done
    return 1
}

foreground_record() {
    fallback_user=$(current_android_user)
    activity_records=$(dumpsys activity activities 2>/dev/null |
        grep -E 'topResumedActivity|mResumedActivity')
    record=$(printf '%s\n' "$activity_records" |
        grep -E 'topResumedActivity' |
        extract_foreground_record "$fallback_user")
    if [ -z "$record" ]; then
        record=$(printf '%s\n' "$activity_records" |
            grep -E 'mResumedActivity' |
            extract_foreground_record "$fallback_user")
    fi
    if [ -z "$record" ]; then
        record=$(dumpsys activity top 2>/dev/null |
            grep -E '^[[:space:]]*ACTIVITY[[:space:]]' |
            extract_foreground_record "$fallback_user")
    fi
    if [ -z "$record" ]; then
        record=$(dumpsys window windows 2>/dev/null |
            grep -E 'mCurrentFocus|mFocusedApp' |
            extract_foreground_record "$fallback_user")
    fi
    [ -n "$record" ] || return 1
    printf '%s\n' "$record"
}

validate_config_file() {
    file="$1"
    [ -s "$file" ] || return 1
    found=0
    while IFS= read -r package || [ -n "$package" ]; do
        valid_package "$package" || return 1
        found=1
    done < "$file"
    [ "$found" -eq 1 ]
}

config_has_conflict() {
    while IFS= read -r package || [ -n "$package" ]; do
        grep -Fqx "$package" "$TOOLS" 2>/dev/null && return 0
    done < "$TARGETS"
    return 1
}

is_target() {
    valid_package "$1" || return 1
    grep -Fqx "$1" "$TARGETS" 2>/dev/null
}

force_stop_tools() {
    current_foreground="$1"
    android_user="$2"
    stopped_count=0
    failed_count=0
    while IFS= read -r package || [ -n "$package" ]; do
        valid_package "$package" || continue
        [ "$package" = "$current_foreground" ] && continue
        if ! pm path --user "$android_user" "$package" >/dev/null 2>&1 &&
            ! pm path "$package" >/dev/null 2>&1; then
            continue
        fi
        if am force-stop --user "$android_user" "$package" >/dev/null 2>&1; then
            stopped_count=$((stopped_count + 1))
        elif [ "$android_user" -eq 0 ] 2>/dev/null &&
            am force-stop "$package" >/dev/null 2>&1; then
            stopped_count=$((stopped_count + 1))
        else
            failed_count=$((failed_count + 1))
        fi
    done < "$TOOLS"
    printf '%s|%s\n' "$stopped_count" "$failed_count"
}

cleanup() {
    current_pid=$(cat "$PID_FILE" 2>/dev/null)
    [ "$current_pid" = "$$" ] && rm -f "$PID_FILE"
    rmdir "$LOCK_DIR" 2>/dev/null
    if [ -f "$ENABLED" ]; then
        write_status error "" "" 0 0 service_stopped
        log_event 'service stopped unexpectedly'
    else
        write_status disabled "" "" 0 0 disabled
    fi
}

trap cleanup EXIT
trap 'exit 0' INT TERM

if [ ! -f "$ENABLED" ]; then
    write_status disabled "" "" 0 0 disabled
    exit 0
fi

log_event 'foreground tool protection service started'
write_status waiting "" "" 0 0 starting
active_target=""
active_user=""
last_stop=0
misses=0
detection_failures=0
last_stopped=0
last_failed=0
config_error_logged=0

while [ -f "$ENABLED" ]; do
    if ! validate_config_file "$TARGETS" ||
        ! validate_config_file "$TOOLS" ||
        config_has_conflict; then
        active_target=""
        misses=0
        write_status error "" "" 0 0 invalid_config
        if [ "$config_error_logged" -eq 0 ]; then
            log_event 'invalid configuration: targets/tools are empty, malformed, or overlap'
            config_error_logged=1
        fi
        sleep "$POLL_SECONDS"
        continue
    fi
    config_error_logged=0

    record=$(foreground_record)
    foreground_user=${record%%|*}
    foreground=${record#*|}
    if [ "$foreground" = "$record" ] || ! valid_package "$foreground"; then
        foreground=""
        foreground_user=""
    fi
    if [ -z "$foreground" ]; then
        detection_failures=$((detection_failures + 1))
        if [ "$detection_failures" -eq 1 ] || [ $((detection_failures % 30)) -eq 0 ]; then
            log_event 'foreground detector returned no valid activity'
        fi
    else
        detection_failures=0
    fi
    if is_target "$foreground"; then
        misses=0
        now=$(date +%s)
        should_stop=0
        event=active
        if [ "$active_target" != "$foreground" ] || [ "$active_user" != "$foreground_user" ]; then
            should_stop=1
            event=target_enter
            log_event "target entered foreground: $foreground (user $foreground_user)"
        elif [ $((now - last_stop)) -ge "$RECHECK_SECONDS" ]; then
            should_stop=1
        fi
        active_target="$foreground"
        active_user="$foreground_user"
        if [ "$should_stop" -eq 1 ]; then
            result=$(force_stop_tools "$foreground" "$foreground_user")
            last_stopped=${result%%|*}
            last_failed=${result#*|}
            last_stop="$now"
            if [ "$event" = "target_enter" ] || [ "$last_failed" -gt 0 ]; then
                log_event "tools checked for $foreground: stopped=$last_stopped failed=$last_failed"
            fi
        fi
        write_status active "$foreground" "$active_target" "$last_stopped" "$last_failed" "$event"
    else
        if [ -n "$active_target" ]; then
            misses=$((misses + 1))
            if [ "$misses" -lt 2 ]; then
                write_status active "$foreground" "$active_target" "$last_stopped" "$last_failed" grace_period
                sleep "$POLL_SECONDS"
                continue
            fi
            log_event "target left foreground: $active_target"
        fi
        active_target=""
        active_user=""
        misses=0
        last_stopped=0
        last_failed=0
        write_status waiting "$foreground" "" 0 0 waiting
    fi
    sleep "$POLL_SECONDS"
done
