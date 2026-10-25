# Anchor - Photos 1.1.1
Status: Finished    Opened: 2026-10-25    Finished: 2026-10-25

| ID    | Request | Phase | Acceptance | Status | Evidence |
|-------|---------|-------|------------|--------|----------|
| A-001 | rebuild and re publish to github with the new icon @[Assets/Icons/photo_6262632732634584666_y.jpg] | 9 | Replace the old launcher icon with the new dual-mountain icon (`Assets/Icons/photo_6262632732634584666_y.jpg`) across adaptive (`mipmap-anydpi-v26`, `drawable-nodpi/ic_launcher_foreground_art.png`, `drawable/ic_launcher_foreground.xml`, `drawable/ic_launcher_background.xml`) and all density mipmaps (`mdpi` through `xxxhdpi` square and round), rebuild the release APK, and re-publish to GitHub | Verified | `app/src/main/res/drawable-nodpi/ic_launcher_foreground_art.png`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `./gradlew verifyAndroidRules testDebugUnitTest assembleDebug assembleRelease`, `Output/Release/Photos_1.1.1_Android.apk` |
