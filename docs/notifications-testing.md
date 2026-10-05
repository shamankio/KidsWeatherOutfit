# Testing the morning notification

The reminder is a self-rescheduling chain of one-time WorkManager requests (unique name
`daily_outfit`): each run shows the notification, then enqueues the next run for tomorrow's
`departure − 30 min`. A debug "Test notification now" run uses its own unique name
(`daily_outfit_test`) and never touches the chain.

Package: `com.rustanovych.kidsoutfit`. `adb` lives in `/Volumes/DevSSD/Android/sdk/platform-tools`.

## 1. Check the notification content

1. Install a debug build, open Home, tap **Yes** on the reminder card, and allow notifications.
   (Or use Settings → "Morning outfit reminder".)
2. Settings → Notifications → **Test notification now (debug)**.
3. Pull down the shade: the title is `Today: <outfit>`, the text is the next outfit change after
   departure, or "Stable weather all day". It should match Home for today at the departure time.
   Tapping it opens the app.

The test run needs notifications to be allowed (otherwise it is silently skipped, by design).

## 2. Check the chain is scheduled

```bash
# What WorkManager has queued (state, next run time, constraints)
adb shell dumpsys jobscheduler | grep -B2 -A12 com.rustanovych.kidsoutfit

# WorkManager's own view — look for "daily_outfit" and its initial delay
adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS -p com.rustanovych.kidsoutfit
adb logcat -d | grep -i "WM-\|DiagnosticsWrkr"
```

A scheduled link appears as a job with `NetworkType.CONNECTED` and a minimum latency equal to the
time left until `departure − 30 min`.

## 3. Force a run and watch it re-schedule

Run the pending job immediately instead of waiting (the job id comes from the `dumpsys` output):

```bash
adb shell cmd jobscheduler run -f com.rustanovych.kidsoutfit <JOB_ID>
```

Expected: the notification appears, and a new `daily_outfit` job shows up in `dumpsys jobscheduler`
with ~24 h of latency. Repeating the command proves the chain keeps going.

## 4. Check the real timing

Set the departure time (Settings → "Leaves home") to **now + 31 minutes**; the reminder is due one
minute later. The job's latency should be ~1 min and the notification should arrive on schedule.
Changing the departure time or the switch re-queues the chain on the spot (no restart needed).

## 5. Reboot / clock changes

```bash
adb reboot
# wait for boot, then:
adb shell dumpsys jobscheduler | grep -A8 com.rustanovych.kidsoutfit
```

The `daily_outfit` job must still be there. `BootReceiver` also re-creates it after a time or time
zone change (`adb shell cmd alarm set-time ...`, or change the zone in system settings).

## 6. Automated

* JVM: `./gradlew :domain:test --tests "*ReminderScheduleTest"` (timing, incl. midnight) and
  `./gradlew :app:testDebugUnitTest --tests "*DailyOutfitRunTest"` (what the worker decides).
* Instrumented, with the WorkManager testing API (initial delay, constraints, replace/keep/append):
  `./gradlew :app:connectedDebugAndroidTest --tests "*DailyNotificationSchedulerTest"`.

## Notes

* In GPS mode a background run cannot always get a fresh fix (no background location permission);
  it then falls back to the cached forecast, which is still correct for today if it was fetched
  yesterday evening or this morning.
* If there is no usable forecast at all, the worker retries once with exponential backoff and then
  gives up for that day, but still schedules tomorrow's run.
