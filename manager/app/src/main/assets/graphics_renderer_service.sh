#!/system/bin/sh

MODE_FILE=/data/adb/apkesu/graphics_renderer/mode
RESTART_MARKER=/data/adb/apkesu/graphics_renderer/restart_required

run_resetprop() {
  if [ -x /data/adb/ksu/bin/resetprop ]; then
    /data/adb/ksu/bin/resetprop "$@"
  elif [ -x /data/adb/ksud ]; then
    /data/adb/ksud resetprop "$@"
  else
    return 1
  fi
}

[ -r "$MODE_FILE" ] || exit 0
case "$(cat "$MODE_FILE" 2>/dev/null)" in
  vulkan)
    run_resetprop debug.hwui.renderer skiavk || exit 1
    run_resetprop debug.hwui.disable_vulkan false || exit 1
    ;;
  opengl)
    run_resetprop debug.hwui.renderer skiagl || exit 1
    run_resetprop debug.hwui.disable_vulkan true || exit 1
    ;;
  *)
    exit 0
    ;;
esac

rm -f "$RESTART_MARKER"
exit 0
