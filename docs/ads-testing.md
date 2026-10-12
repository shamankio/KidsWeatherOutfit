# Ads & consent testing

Ads are Google AdMob (banner on Home only). Consent is Google UMP. Code lives in `app/.../ads/`.

## Ids

- **Debug** builds use Google's public sample App ID / banner unit, so debug can never serve or click a real ad.
- **Release** builds read `ADMOB_APP_ID` and `ADMOB_BANNER_ID` from `local.properties` (gitignored). A release
  task fails with a clear message if either is missing; debug builds do not need them.
- Optional `UMP_TEST_DEVICE_ID=<hashed id>` in `local.properties` is used by debug builds only.
- Optional `ADS_TEST_DEVICE_ID=<id>` in `local.properties` registers a phone as an ads test device in **every**
  build type, so real ad units serve it "Test Ad"s. Find it in logcat after the ads SDK starts:
  `adb logcat -d | grep "setTestDeviceIds"`. It is separate from the AdMob console's test-device list and from
  `UMP_TEST_DEVICE_ID` (different hash). Leave it out of the build that goes to Play.

## Flow

`MainActivity` calls `AdConsent.gatherConsent` when Home appears (never on Splash/onboarding).
`MobileAds.initialize` runs only once `canRequestAds` is true. `HomeRoute` shows `AdBanner` only then.
Settings -> About -> "Privacy settings" is shown only when UMP says privacy options are required (EEA/UK).

## Testing the consent form

Debug builds force `DEBUG_GEOGRAPHY_EEA`, so the form shows anywhere. The form is Google's sample message while the
debug App ID is in use. To see **your own** AdMob message, the GDPR message must be published in AdMob and the
app must run with the real App ID (a release build).

Reset consent: clear app data (`adb shell pm clear com.rustanovych.kidsoutfit`).

Hashed test-device id (for physical phones): run the app in debug and look in logcat for
`Use new ConsentDebugSettings.Builder().addTestDeviceHashedId("...")`, then put it in `local.properties`.

## Release build on a device

R8 is on for release. There is no release signing config in the repo; to try one locally:

```bash
./gradlew assembleRelease
apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android --out /path/to/release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

Careful: a release build with the real ad unit serves **real ads**. Do not tap them (invalid traffic can get the
AdMob account limited). Register the phone as an AdMob test device first, then confirm the banner carries "Test Ad".
